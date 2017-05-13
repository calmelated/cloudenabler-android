package tw.com.ksmt.cloud.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.MainApp;
import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.CloudStatus;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.SearchViewTask;
import tw.com.ksmt.cloud.libs.Utils;

public class CloudStatusActivity extends ActionBarActivity implements PullToRefreshBase.OnRefreshListener2<ListView>, SearchView.OnQueryTextListener {
    private final Context context = CloudStatusActivity.this;
    private ActionBar actionBar;
    private PullToRefreshListView prListView;
    private ListView listView;
    private CloudStatusAdapter adapter;
    private ProgressDialog mDialog;
    private Timer bgTimer;
    private MsgHandler mHandler;
    private SearchViewTask searchTask;
    private SearchView mSearchView;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        mDialog = Kdialog.getProgress(context, mDialog);
        AppSettings.setupLang(context);
        setContentView(R.layout.pull_list_view);
        setTitle(getString(R.string.cloud_status));
        mHandler = new MsgHandler(this);

        adapter = new CloudStatusAdapter(context);
        prListView = (PullToRefreshListView) findViewById(R.id.listView);
        prListView.setOnRefreshListener(this);
        listView = prListView.getRefreshableView();
        listView.setAdapter(adapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bgTimer != null) {
            bgTimer.cancel();
        }
        if (mDialog != null) {
            mDialog.cancel();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bgTimer = new Timer();
        bgTimer.schedule(new BgTimerTask(), 0, PrjCfg.RUN_ONCE);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AppSettings.reload(context);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cloud_status, menu);

        MenuItem searchItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setOnQueryTextListener(this);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        //int id = item.getItemId();
        return true;
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
        if(bgTimer != null) {
            bgTimer.cancel();
        }
        bgTimer = new Timer();
        bgTimer.schedule(new BgTimerTask(), 0, PrjCfg.RUN_ONCE);
    }

    @Override
    public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
    }

    private class BgTimerTask extends TimerTask {
        public void run() {
            try {
                getCloudStatus();
                MHandler.exec(mHandler, MHandler.UPDATE);
            } catch (Exception e) {
                if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
                }
                e.printStackTrace();
            } finally {
                mDialog.dismiss();
                bgTimer.cancel();
            }
        }
    }

    private void getCloudStatus() throws Exception {
        JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/cloud/status");
        //Log.e(Dbg._TAG_(), jObject.toString());
        if (jObject.getInt("code") == 404) {  // Not found
            return;
        } else if (jObject.getInt("code") != 200) {
            MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_get_data));
            return;
        } else { // Successfully get a new list
            MHandler.exec(mHandler, MHandler.CLEAR_LIST);
        }
        int updateTime = jObject.getInt("time");
        MHandler.exec(mHandler, MHandler.UPDATE_TIME, getString(R.string.last_check_time)+ ": " + Utils.unix2Datetime(updateTime));
        MHandler.exec(mHandler, MHandler.ADD_LIST, new CloudStatus(CloudStatus.TYPE_TITLE, getString(R.string.services)));

        JSONObject servObject = jObject.getJSONObject("servStatus");
        // Show Cloud status
        MHandler.exec(mHandler, MHandler.ADD_LIST, new CloudStatus(CloudStatus.TYPE_SERVICE, R.string.cloud, servObject.getInt("cloud")));

        // Show Push status
        MHandler.exec(mHandler, MHandler.ADD_LIST, new CloudStatus(CloudStatus.TYPE_SERVICE, R.string.notification, servObject.getInt("push")));

        // Show Email status
        if(!PrjCfg.CLOUD_URL.equals("https://cloud.ksmt.co")) {
            // Hide if not KSMT Cloud
        } else {
            MHandler.exec(mHandler, MHandler.ADD_LIST, new CloudStatus(CloudStatus.TYPE_SERVICE, R.string.email, servObject.getInt("mail")));
        }

        // Show Network Quality
        MHandler.exec(mHandler, MHandler.ADD_LIST, new CloudStatus(CloudStatus.TYPE_SERVICE, R.string.net_quality, servObject.getInt("connection")));

        // Show APP download
        if(MainApp.FROM_GOOGLE_PLAY) {
            // Hide for Google Play APP
        } else {
            MHandler.exec(mHandler, MHandler.ADD_LIST, new CloudStatus(CloudStatus.TYPE_SERVICE, R.string.app_download, servObject.getInt("appDownload")));
        }

        // Show Recent errors
        MHandler.exec(mHandler, MHandler.ADD_LIST, new CloudStatus(CloudStatus.TYPE_TITLE, getString(R.string.recent_errors)));
        JSONArray errors = jObject.getJSONArray("recentErrors");
        for (int i = 0; i < errors.length(); i++) {
            JSONObject error = errors.getJSONObject(i);
            if(CloudStatus.isUnknownEvent(error.getInt("type"))) { //ignore timeout incidents
                continue;
            }
            MHandler.exec(mHandler, MHandler.ADD_LIST, new CloudStatus(CloudStatus.TYPE_HISTORY, context, error));
        }
    }

    private static class MsgHandler extends MHandler {
        public MsgHandler(CloudStatusActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final CloudStatusActivity activity = (CloudStatusActivity) super.mActivity.get();
            switch (msg.what) {
                case MHandler.UPDATE: {
                    activity.adapter.notifyDataSetChanged();
                    activity.prListView.onRefreshComplete();
                    break;
                }
                case MHandler.UPDATE_TIME: {
                    activity.getSupportActionBar().setSubtitle(Html.fromHtml("<small><small>" + msg.obj + "</small></small>"));
                    break;
                }
                case MHandler.CLEAR_LIST: {
                    activity.adapter.clearList();
                    break;
                }
                case MHandler.ADD_LIST: {
                    activity.adapter.addList((CloudStatus) msg.obj);
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