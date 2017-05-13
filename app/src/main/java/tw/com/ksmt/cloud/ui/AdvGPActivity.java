package tw.com.ksmt.cloud.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.MainApp;
import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.AdvGP;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.SearchViewTask;
import tw.com.ksmt.cloud.libs.Utils;

public class AdvGPActivity extends ActionBarActivity implements AdapterView.OnItemClickListener, PullToRefreshBase.OnRefreshListener2<ListView>, SearchView.OnQueryTextListener {
    private final Context context = AdvGPActivity.this;
    private boolean stopPolling = false;
    private PullToRefreshListView prListView;
    private ActionBar actionBar;
    private ListView listView;
    private AdvGPAdapter adapter;
    private ProgressDialog mDialog;
    private Timer deleteTimer;
    private Timer pollingTimer;
    private MsgHandler mHandler;
    private SearchViewTask searchTask;
    private SearchView mSearchView;
    private AdvGP curAdvGP;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        mDialog = Kdialog.getProgress(context, mDialog);
        AppSettings.setupLang(context);
        setContentView(R.layout.pull_list_view);
        mHandler = new MsgHandler(this);

        // set title
        curAdvGP = (AdvGP) getIntent().getSerializableExtra("AdvGP");
        if(curAdvGP != null) {
            setTitle(getString(R.string.child_group));
            actionBar.setSubtitle(Html.fromHtml("<small><small>" + getString(R.string.group_name) + ": " + curAdvGP.name + "</small></small>"));
        } else {
            setTitle(getString(R.string.advgp));
        }

        adapter = new AdvGPAdapter(context);
        prListView = (PullToRefreshListView) findViewById(R.id.listView);
        prListView.setOnRefreshListener(this);
        listView = prListView.getRefreshableView();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        registerForContextMenu(listView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (deleteTimer != null) {
            deleteTimer.cancel();
        }
        if (pollingTimer != null) {
            pollingTimer.cancel();
        }
        if (mDialog != null) {
            mDialog.cancel();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        stopPolling = false;
        pollingTimer = new Timer();
        pollingTimer.schedule(new PollingTask(), 0, PrjCfg.GRUOP_POLLING);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AppSettings.reload(context);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.advgp, menu);

        stopPolling = true;
        MenuItem searchItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setOnQueryTextListener(this);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.new_group) {
            if(adapter.getCount() <= PrjCfg.MAX_GROUP_NUM) {
                Intent intent = new Intent(context, NewGroupMemberActivity.class);
                Bundle bundle = new Bundle();
                if(curAdvGP == null) {
                    bundle.putInt("isAdvGP", 1);
                } else {
                    bundle.putInt("isAdvGP", 2);
                    bundle.putSerializable("AdvGP", curAdvGP);
                }
                intent.putExtras(bundle);
                startActivity(intent);
            } else {
                MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_max_group_exceeded));
            }
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        stopPolling = true;
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        AdvGP announce = (AdvGP) adapter.getItem(info.position - 1);
        menu.setHeaderTitle(announce.name);
        menu.add(0, R.id.edit, 0, getString(R.string.edit));
        menu.add(0, R.id.remove, 0, getString(R.string.remove));
    }

    public void onContextMenuClosed(Menu menu) {
        stopPolling = false;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo itemInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final AdvGP advGP = (AdvGP) adapter.getItem(itemInfo.position - 1);
        if (advGP == null) {
            return true;
        }
        int itemId = item.getItemId();
        if (itemId == R.id.edit) {
            editAdvGP(advGP);
        } else if (itemId == R.id.remove) {
            Kdialog.getMakeSureDialog(context, getString(R.string.make_sure_delete))
            .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mDialog = Kdialog.getProgress(context, mDialog);
                    deleteTimer = new Timer();
                    deleteTimer.schedule(new DeleteTask(advGP), 0, PrjCfg.RUN_ONCE);
                }
            }).show();
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Log.e(Dbg._TAG_(), "pos: " + position + ", id: " + id);
        AdvGP advGP = (AdvGP) adapter.getItem(position - 1);
        Bundle bundle = new Bundle();
        bundle.putSerializable("AdvGP", advGP);
        bundle.putInt("Type", ViewStatusAdapter.GROUP);

        Intent intent = new Intent(context, ((advGP.gnext) ? AdvGPActivity.class : ViewStatusActivity.class));
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void editAdvGP(AdvGP advGP) {
        Intent intent = new Intent(context, EditGroupActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("AdvGP", advGP);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        //Log.e(Dbg._TAG_(), "text changed: " + query);
        if (searchTask != null) {
            searchTask.cancel(true);
        }
        searchTask = new SearchViewTask(adapter, mHandler, query);
        searchTask.execute();
        return false;
    }

    @Override
    public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
        //Log.e(Dbg._TAG_(), "onRefresh.. ");
        MHandler.exec(mHandler, MHandler.CLEAR_LIST);
        if(pollingTimer != null) {
            pollingTimer.cancel();
        }
        pollingTimer = new Timer();
        pollingTimer.schedule(new PollingTask(), 0, PrjCfg.RUN_ONCE);
    }

    @Override
    public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
    }

    private class PollingTask extends TimerTask {
        public void run() {
            try {
                if(stopPolling) { return; }
                getAdvGPList();

                if (Utils.loadPrefs(context, "Password", "").equals("")) {
                    MHandler.exec(mHandler, MHandler.BEEN_LOGOUT);
                    return;
                } else if(MainApp.SERV_MAINT > (new Date().getTime() / 1000)) {
                    MHandler.exec(mHandler, MHandler.SERV_MAINT);
                    return;
                }
                MHandler.exec(mHandler, MHandler.UPDATE);
            } catch (Exception e) {
                if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
                }
                e.printStackTrace();
            } finally {
                mDialog.dismiss();
            }
        }
    }

    private class DeleteTask extends TimerTask {
        public AdvGP advGP;

        public DeleteTask(AdvGP advGP) {
            this.advGP = advGP;
        }

        public void run() {
            try {
                deleteAdvGP(advGP);
            } catch (Exception e) {
                if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
                }
                e.printStackTrace();
            } finally {
                mDialog.dismiss();
                deleteTimer.cancel();
            }
        }
    }

    private void getAdvGPList() throws Exception {
        String url = (curAdvGP != null && curAdvGP.id > 0) ? "/api/advgp/sub/" + curAdvGP.id : "/api/advgp";
        JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + url);
        if (jObject.getInt("code") == 404) {  // Not found
            return;
        } else if (jObject.getInt("code") != 200) {
            MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_get_data));
            return;
        } else { // Successfully get a new list
            MHandler.exec(mHandler, MHandler.CLEAR_LIST);
        }
        JSONArray groups = jObject.getJSONArray("groups");
        for (int i = 0; i < groups.length(); i++) {
            MHandler.exec(mHandler, MHandler.ADD_LIST,(new AdvGP(groups.getJSONObject(i))));
        }
    }

    private void deleteAdvGP(AdvGP advGP) throws Exception {
        JSONObject jObject = JSONReq.send(context, "DELETE", PrjCfg.CLOUD_URL + "/api/advgp/" + advGP.id);
        if (jObject == null || jObject.getInt("code") != 200) {
            MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_remove));
        } else {
            MHandler.exec(mHandler, MHandler.DEL_LIST, advGP);
            MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_remove));
            MHandler.exec(mHandler, MHandler.UPDATE);
        }
    }

    private static class MsgHandler extends MHandler {
        public MsgHandler(AdvGPActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final AdvGPActivity activity = (AdvGPActivity) super.mActivity.get();
            switch (msg.what) {
                case MHandler.UPDATE: {
                    activity.adapter.notifyDataSetChanged();
                    activity.prListView.onRefreshComplete();
                    break;
                }
                case MHandler.CLEAR_LIST: {
                    activity.adapter.clearList();
                    break;
                }
                case MHandler.ADD_LIST: {
                    activity.adapter.addList((AdvGP) msg.obj);
                    break;
                }
                case MHandler.DEL_LIST: {
                    activity.adapter.remove((AdvGP) msg.obj);
                    break;
                }
                case MHandler.SRCH_QUERY: {
                    activity.adapter.setQueryStr((String) msg.obj);
                    break;
                }
                default: {
                    super.handleMessage(msg);
                }
            }
        }
    }
}