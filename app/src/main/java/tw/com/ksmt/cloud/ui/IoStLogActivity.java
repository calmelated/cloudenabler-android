package tw.com.ksmt.cloud.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.MainApp;
import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.IoStLog;
import tw.com.ksmt.cloud.libs.Dbg;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.SearchViewTask;
import tw.com.ksmt.cloud.libs.Utils;

public class IoStLogActivity extends ActionBarActivity implements PullToRefreshBase.OnRefreshListener2<ListView>, AbsListView.OnScrollListener, SearchView.OnQueryTextListener, AdapterView.OnItemClickListener {
    private final Context context = IoStLogActivity.this;
    private PullToRefreshListView prListView;
    private ActionBar actionBar;
    private ListView listView;
    private IoStLogAdapter adapter;
    private ProgressDialog mDialog;
    private Timer pollingTimer;
    private MsgHandler mHandler;
    private SearchViewTask searchTask;
    private SearchView mSearchView;
    private boolean noMoreData = false;
    private int totalItems = -1;
    private int idxItem = 0;
    private static boolean isPolling = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        mDialog = Kdialog.getProgress(context, mDialog);
        AppSettings.reload(context);
        setContentView(R.layout.pull_list_view);
        setTitle(getString(R.string.iostlog));
        mHandler = new MsgHandler(this);

        prListView = (PullToRefreshListView) findViewById(R.id.listView);
        prListView.setOnRefreshListener(this);
        adapter = new IoStLogAdapter(context);
        listView = prListView.getRefreshableView();
        listView.setAdapter(adapter);
        listView.setOnScrollListener(this);
        listView.setOnItemClickListener(this);

        // Get Log
        pollingTimer = new Timer();
        pollingTimer.schedule(new PollingTimerTask(), 0, PrjCfg.RUN_ONCE);
    }

    @Override
    protected void onPause() {
        super.onPause();
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
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AppSettings.reload(context);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.audit, menu);

        MenuItem searchItem = menu.findItem(R.id.search);
        MenuItemCompat.collapseActionView(searchItem);
        mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setIconified(true);
        return true;
    }

    @Override
    public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
        Log.e(Dbg._TAG_(), "onRefresh.. ");
        MHandler.exec(mHandler, MHandler.CLEAR_LIST);
        totalItems = -1;
        idxItem = 0;
        noMoreData = false;
        pollingTimer = new Timer();
        pollingTimer.schedule(new PollingTimerTask(), 0, PrjCfg.RUN_ONCE);
    }

    @Override
    public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
        //Log.e(Dbg._TAG_(), "Pull Up Event");
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        //Log.e(Dbg._TAG_(), "onScrollStateChanged");
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        //Log.e(Dbg._TAG_(), "first=" + firstVisibleItem + ", visible=" + visibleItemCount + ", total=" + totalItems);
        if (firstVisibleItem == 0) { // on the top
            //Log.e(Dbg._TAG_(), "On the top of list");
            prListView.setMode(PullToRefreshBase.Mode.PULL_FROM_START);
        } else if (firstVisibleItem + visibleItemCount >= totalItemCount) { // on the bottom
            if (noMoreData) {
                prListView.setMode(PullToRefreshBase.Mode.PULL_FROM_START);
                prListView.onRefreshComplete();
                return;
            }
            //Log.e(Dbg._TAG_(), "Loading more...");
            if(pollingTimer == null) {
                prListView.setMode(PullToRefreshBase.Mode.PULL_FROM_END);
                prListView.setRefreshing(true);
                pollingTimer = new Timer();
                pollingTimer.schedule(new PollingTimerTask(), 0, PrjCfg.RUN_ONCE);
            }
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
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        List<IoStLog> iostlogList = adapter.getList();
        IoStLog ioStLog = iostlogList.get(position - 1);
        String msg = "";
        msg += getString(R.string.time)     + ": " + Utils.unix2Datetime(ioStLog.time, "yyyy-MM-dd HH:mm")  + "\n";
        msg += getString(R.string.from)     + ": " + ioStLog.account + "\n";
        msg += getString(R.string.status)   + ": " + ioStLog.message + "\n";
        msg += getString(R.string.acc_num)  + ": " + ioStLog.accNum  + "\n";
        msg += getString(R.string.acc_time) + ": " + Utils.toDuration(ioStLog.accTime);
        Kdialog.getDefInfoDialog(context).setMessage(msg).show();
    }

    private class PollingTimerTask extends TimerTask {
        public void run() {
            try {
                getIoStLogs();
                if (Utils.loadPrefs(context, "Password", "").equals("")) {
                    MHandler.exec(mHandler, MHandler.BEEN_LOGOUT);
                    return;
                } else if(MainApp.SERV_MAINT > (new Date().getTime() / 1000)) {
                    MHandler.exec(mHandler, MHandler.SERV_MAINT);
                    return;
                }
                MHandler.exec(mHandler, MHandler.UPDATE);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mDialog.dismiss();
                pollingTimer.cancel();
                pollingTimer = null;
            }
        }
    }

    private void getIoStLogs() throws Exception {
        if(isPolling) {
            return;
        } else {
            isPolling = true;
        }
        //Log.e(Dbg._TAG_(), "idxItem=" + idxItem + " total=" + totalItems);
        String uri = (totalItems < 0) ? "&from=0" : "&from=1&et=" + idxItem;
        //Log.e(Dbg._TAG_(), "/api/alarm?num=" + PrjCfg.LOAD_MORE_NUM + uri);
        JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/iostlog?num=" + PrjCfg.LOAD_MORE_NUM + uri);
        if (jObject.getInt("code") == 404) {  // Not found
            isPolling = false;
            return;
        } else if (jObject.getInt("code") != 200) {
            isPolling = false;
            MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_get_data));
            return;
        }
        totalItems = jObject.getInt("total");
        //idxItem = ((from + num) > totalItems) ? totalItems : (from + num);
        JSONArray iostlogs = jObject.getJSONArray("iostLogs");
        int adtlen = iostlogs.length();
        for (int i = 0; i < adtlen; i++) {
            IoStLog ioStLog = new IoStLog(context, (JSONObject) iostlogs.get(i));
            MHandler.exec(mHandler, MHandler.ADD_LIST, ioStLog);
            if (i == (adtlen - 1)) { // last one
                idxItem = (idxItem == ioStLog.time) ? (ioStLog.time - 1) : ioStLog.time ;
            }
        }
        if (adtlen < PrjCfg.LOAD_MORE_NUM) { // no more data
            noMoreData = true;
        }
        isPolling = false;
    }

    private static class MsgHandler extends MHandler {
        public MsgHandler(IoStLogActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final IoStLogActivity activity = (IoStLogActivity) super.mActivity.get();
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
                    activity.adapter.addList((IoStLog) msg.obj);
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
