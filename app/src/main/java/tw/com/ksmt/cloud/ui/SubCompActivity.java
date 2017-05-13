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
import tw.com.ksmt.cloud.iface.Company;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.SearchViewTask;
import tw.com.ksmt.cloud.libs.Utils;

public class SubCompActivity extends ActionBarActivity implements AdapterView.OnItemClickListener, PullToRefreshBase.OnRefreshListener2<ListView>, SearchView.OnQueryTextListener {
    private final Context context = SubCompActivity.this;
    private PullToRefreshListView prListView;
    private ActionBar actionBar;
    private ListView listView;
    private SubCompAdapter adapter;
    private ProgressDialog mDialog;
    private Timer bgTimer;
    private MsgHandler mHandler;
    private SearchViewTask searchTask;
    private SearchView mSearchView;
    private String companyId;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        mDialog = Kdialog.getProgress(context, mDialog);
        AppSettings.setupLang(context);
        setContentView(R.layout.pull_list_view);
        setTitle(getString(R.string.subsidiary));
        mHandler = new MsgHandler(this);

        // Decide Company ID
        boolean isSubsidiary = Utils.loadPrefsBool(context, "IsSubsidiary");
        String subCompID = Utils.loadPrefs(context, "SubCompID", "0");
        if(isSubsidiary || !subCompID.equals("0")) { // Now, APP in subsidiary
            MHandler.exec(mHandler, MHandler.GO_BACK, getString(R.string.err_in_subsidiary));
            return;
        } else {
            companyId = Utils.loadPrefs(context, "CompanyID");
        }

        adapter = new SubCompAdapter(context);
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
        getMenuInflater().inflate(R.menu.sub_company, menu);
        if(PrjCfg.EN_ANNOUNCE_SUBCOMP) {
            MenuItem announceAllItem = menu.findItem(R.id.announce_all_subsidiary);
            announceAllItem.setVisible(true);
        }

        MenuItem searchItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setOnQueryTextListener(this);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.new_company) {
            Intent intent = new Intent(context, SignUpCompany.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

            Bundle bundle = new Bundle();
            bundle.putSerializable("CompanyID", companyId);
            intent.putExtras(bundle);
            startActivity(intent);
        } else if(id == R.id.announce_all_subsidiary) {
            Intent intent = new Intent(context, NewAnnounceActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

            Bundle bundle = new Bundle();
            bundle.putString("CompanyName", "AllSubsidiary");
            intent.putExtras(bundle);
            startActivity(intent);
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Company company = (Company) adapter.getItem(info.position - 1);
        menu.setHeaderTitle(company.name);
        menu.add(0, R.id.login, 0, getString(R.string.login));
        if(PrjCfg.EN_ANNOUNCE_SUBCOMP) {
            menu.add(0, R.id.new_announce, 0, getString(R.string.new_announce));
        }
        menu.add(0, R.id.remove, 0, getString(R.string.remove));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo itemInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final Company company = (Company) adapter.getItem(itemInfo.position - 1);
        if (company == null) {
            return true;
        }
        int itemId = item.getItemId();
        if (itemId == R.id.login) {
            loginSubsidiary(company);
        } else if (itemId == R.id.new_announce) {
            Intent intent = new Intent(context, NewAnnounceActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

            Bundle bundle = new Bundle();
            bundle.putString("CompanyName", company.name);
            bundle.putSerializable("Company", company);
            intent.putExtras(bundle);
            startActivity(intent);
        } else if (itemId == R.id.remove) {
            Kdialog.getMakeSureDialog(context, getString(R.string.make_sure_delete))
            .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mDialog = Kdialog.getProgress(context, mDialog);
                    bgTimer = new Timer();
                    bgTimer.schedule(new BgTimerTask("DELETE", company), 0, PrjCfg.RUN_ONCE);
                }
            }).show();
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Log.e(Dbg._TAG_(), "pos: " + position + ", id: " + id);
        loginSubsidiary((Company) adapter.getItem(position - 1));
    }

    private void loginSubsidiary(final Company company) {
        Kdialog.getMakeSureDialog(context, getString(R.string.make_sure_login))
        .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mDialog = Kdialog.getProgress(context, mDialog);
                bgTimer = new Timer();
                bgTimer.schedule(new BgTimerTask("LOGIN", company), 0, PrjCfg.RUN_ONCE);
            }
        }).show();
    }

    private void loginPost(Company company) {
        try {
            JSONObject jObject = JSONReq.send(context, "PUT", PrjCfg.CLOUD_URL + "/api/company/login/" + company.id);
            if (jObject == null || jObject.getInt("code") != 200) {
                MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_login));
            } else {
                Utils.savePrefs(context, "SubCompID", company.id);
                MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_login));
                MHandler.exec(mHandler, MHandler.GO_BACK);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        MHandler.exec(mHandler, MHandler.CLEAR_LIST);
        bgTimer = new Timer();
        bgTimer.schedule(new BgTimerTask(), 0, PrjCfg.RUN_ONCE);
    }

    @Override
    public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
    }

    private class BgTimerTask extends TimerTask {
        public String cmd;
        public Company company;

        public BgTimerTask() {
            this.cmd = "GET";
        }

        public BgTimerTask(String cmd) {
            this.cmd = cmd;
        }

        public BgTimerTask(String cmd, Company company) {
            this.cmd = cmd;
            this.company = company;
        }

        public void run() {
            try {
                if(cmd.equals("GET")) {
                    getSubsidiaryList();
                } else if(cmd.equals("DELETE")) {
                    deleteSubsidiary(this.company);
                } else if(cmd.equals("LOGIN")) {
                    loginPost(this.company);
                } else {
                    return;
                }
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
                bgTimer.cancel();
            }
        }
    }

    private void getSubsidiaryList() throws Exception {
        JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/company/sub");
        if (jObject.getInt("code") == 404) {  // Not found
            return;
        } else if (jObject.getInt("code") != 200) {
            MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_get_data));
            return;
        } else { // Successfully get a new list
            MHandler.exec(mHandler, MHandler.CLEAR_LIST);
        }
        JSONArray companies = jObject.getJSONArray("companies");
        for (int i = 0; i < companies.length(); i++) {
            MHandler.exec(mHandler, MHandler.ADD_LIST,(new Company(companies.getJSONObject(i))));
        }
    }

    private void deleteSubsidiary(Company company) throws Exception {
        JSONObject jObject = JSONReq.send(context, "DELETE", PrjCfg.CLOUD_URL + "/api/company/id/" + company.id);
        if (jObject == null || jObject.getInt("code") != 200) {
            MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_remove));
        } else {
            MHandler.exec(mHandler, MHandler.DEL_LIST, company);
            MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_remove));
        }
    }

    private static class MsgHandler extends MHandler {
        public MsgHandler(SubCompActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final SubCompActivity activity = (SubCompActivity) super.mActivity.get();
            if(activity.adapter == null) {
                return;
            }
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
                    activity.adapter.addList((Company) msg.obj);
                    break;
                }
                case MHandler.DEL_LIST: {
                    activity.adapter.remove((Company) msg.obj);
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