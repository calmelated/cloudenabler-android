package tw.com.ksmt.cloud.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.MainApp;
import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Notification;
import tw.com.ksmt.cloud.iface.Register;
import tw.com.ksmt.cloud.iface.ViewStatus;
import tw.com.ksmt.cloud.libs.Dbg;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.SearchViewTask;
import tw.com.ksmt.cloud.libs.Utils;

public class NotificationActivity extends ActionBarActivity implements PullToRefreshBase.OnRefreshListener2<ListView>, AbsListView.OnScrollListener, SearchView.OnQueryTextListener, AdapterView.OnItemClickListener {
    private final Context context = NotificationActivity.this;
    private ActionBar actionBar;
    private PullToRefreshListView prListView;
    private ListView listView;
    private NotificationAdapter adapter;
    private ProgressDialog mDialog;
    private Timer pollingTimer;
    private Timer getRegTimer;
    private MsgHandler mHandler;
    private SearchViewTask searchTask;
    private SearchView mSearchView;
    private boolean noMoreData = false;
    private int totalItems = -1;
    private int idxItem = 0;
    private static boolean isPolling = false;
    private int alarmTime = -1;
    private String showLogType = "average";
    private int tzOffset;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        mDialog = Kdialog.getProgress(context, mDialog);
        AppSettings.reload(context);
        setContentView(R.layout.pull_list_view);
        setTitle(getString(R.string.notification));
        mHandler = new MsgHandler(this);

        prListView = (PullToRefreshListView) findViewById(R.id.listView);
        prListView.setOnRefreshListener(this);
        adapter = new NotificationAdapter(context);
        listView = prListView.getRefreshableView();
        listView.setAdapter(adapter);
        listView.setOnScrollListener(this);
        tzOffset = (Calendar.getInstance().getTimeZone().getRawOffset() / 1000);

        // click -> linechart
        if(PrjCfg.EN_CLOUD_LOG) {
            listView.setOnItemClickListener(this);
        }

        // Get alarm
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
        if(getRegTimer != null) {
            getRegTimer.cancel();
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
        getMenuInflater().inflate(R.menu.notification, menu);

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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String addr = null;
        List<Notification> notifyList = adapter.getList();
        Notification notification = notifyList.get(position - 1);
        if(notification.sn != null && notification.refReg != null) {
            addr = notification.refReg;
            showLogType = "average";
        } else if(notification.msgCode == Notification.MCODE_LOWER_ALARM) {
            addr = notification.addr;
            showLogType = "min";
        } else if(notification.msgCode == Notification.MCODE_UPPER_ALARM) {
            addr = notification.addr;
            showLogType = "max";
        }
//        Log.e(Dbg._TAG_(), "refReg addr=" + addr);
        if(addr != null && !addr.equals("")) {
            alarmTime = notification.time + tzOffset;
            mDialog = Kdialog.getProgress(context, mDialog);
            getRegTimer = new Timer();
            getRegTimer.schedule(new GetRegInfoTask(notification.sn, addr), 0, PrjCfg.RUN_ONCE);
        }
    }

    private void startLineActivity(ViewStatus vs) {
        Intent intent = new Intent(context, LineChartActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        Bundle bundle = new Bundle();
        bundle.putSerializable("curVs", vs);
        bundle.putInt("curTime", alarmTime);
        bundle.putString("logType", showLogType);
        bundle.putString("dataType", "day");
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private class GetRegInfoTask extends TimerTask {
        private String sn;
        private String addr;

        public GetRegInfoTask(String sn, String addr) {
            this.sn = sn;
            this.addr = addr;
        }

        public void run() {
            try {
                JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/device/" + sn + "/status?addr=" + addr);
                int statCode = jObject.getInt("code");
                if (statCode != 200) {
                    return;
                }
                // draw chart
                JSONArray iostats = jObject.getJSONArray("iostats");
                if(iostats.length() == 0) {
                    MHandler.exec(mHandler, MHandler.GO_BACK, getString(R.string.err_register));
                }

                // Parsing data
                ViewStatus vs = new ViewStatus();
                vs.sn = sn;
                vs.devEnlog = jObject.has("enLog") ? jObject.getInt("enLog") == 1 ? true : false : false;
                vs.devEnServlog = jObject.has("enServLog") ? jObject.getInt("enServLog") == 1 ? true : false : false;
                vs.devStat = jObject.has("status") ? jObject.getInt("status") : 0;

                JSONObject jStats = (JSONObject) iostats.get(0);
                //Log.e(Dbg._TAG_(), jStats.toString());
                vs.id     = jStats.getString("id");
                vs.desc   = jStats.has("desc") ? jStats.getString("desc") : "" ;
                vs.unit   = jStats.has("unit") ? jStats.getString("unit") : "" ;
                vs.haddr  = Utils.realAddr(jStats.getString("haddr"));
                vs.slvIdx = Utils.slvIdx(addr);
                vs.hval   = jStats.getString("hval");
                vs.upVal  = jStats.has("up")  ? jStats.getString("up")  : "" ;
                vs.lowVal = jStats.has("low") ? jStats.getString("low") : "" ;
                vs.onVal  = jStats.has("on")  ? jStats.getString("on") : "" ;
                vs.offVal = jStats.has("off") ? jStats.getString("off") : "" ;
                vs.fpt    = jStats.has("fpt") ? jStats.getInt("fpt") : 0;
                vs.display= jStats.has("dt") ? jStats.getInt("dt") : 0;
                vs.enlog  = (jStats.getInt("enlog") == 1) ? true : false ;
                vs.swSN   = jStats.has("swSN") ? jStats.getString("swSN") : "" ;
                vs.swAddr = jStats.has("swAddr") ? jStats.getString("swAddr") : "" ;
                vs.swType = jStats.has("swType") ? jStats.getInt("swType") : 0;

                vs.type = jStats.getInt("type");
                if (Register.is64Bits(vs.type)) {
                    vs.iaddr = jStats.getString("iaddr");
                    vs.jaddr = jStats.getString("jaddr");
                    vs.laddr = jStats.getString("laddr");
                    vs.ival = jStats.getString("ival");
                    vs.jval = jStats.getString("jval");
                    vs.lval = jStats.getString("lval");
                    MHandler.exec(mHandler, MHandler.DRAWLINE, vs);
                } else if (Register.is48Bits(vs.type)) {
                    vs.iaddr = jStats.getString("iaddr");
                    vs.laddr = jStats.getString("laddr");
                    vs.ival = jStats.getString("ival");
                    vs.lval = jStats.getString("lval");
                    MHandler.exec(mHandler, MHandler.DRAWLINE, vs);
                } else if (Register.is32Bits(vs.type)) {
                    vs.laddr = jStats.getString("laddr");
                    vs.lval = jStats.getString("lval");
                    MHandler.exec(mHandler, MHandler.DRAWLINE, vs);
                } else { // 16bit, Alarm
                    MHandler.exec(mHandler, MHandler.DRAWLINE, vs);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mDialog.dismiss();
                if(getRegTimer != null) {
                    getRegTimer.cancel();
                    getRegTimer = null;
                }
            }
        }
    }

    private class PollingTimerTask extends TimerTask {
        public void run() {
            try {
                getNotifications();
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

    private void getNotifications() throws Exception {
        if(isPolling) {
            return;
        } else {
            isPolling = true;
        }
        //Log.e(Dbg._TAG_(), "idxItem=" + idxItem + " total=" + totalItems);
        String uri = (totalItems < 0) ? "&from=0" : "&from=1&et=" + idxItem;
        //Log.e(Dbg._TAG_(), "/api/alarm?num=" + PrjCfg.LOAD_MORE_NUM + uri);
        JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/alarm?num=" + PrjCfg.LOAD_MORE_NUM + uri);
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
        JSONArray almlogs = jObject.getJSONArray("almlogs");
        int almlen = almlogs.length();
        for (int i = 0; i < almlen; i++) {
            Notification almlog = new Notification(context, (JSONObject) almlogs.get(i));
            MHandler.exec(mHandler, MHandler.ADD_LIST, almlog);
            if (i == (almlen - 1)) { // last one
                idxItem = (idxItem == almlog.time) ? (almlog.time - 1) : almlog.time;
            }
        }
        if (almlen < PrjCfg.LOAD_MORE_NUM) { // no more data
            noMoreData = true;
        }
        isPolling = false;
    }

    private static class MsgHandler extends MHandler {
        public MsgHandler(NotificationActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final NotificationActivity activity = (NotificationActivity) super.mActivity.get();
            switch (msg.what) {
                case MHandler.DRAWLINE: {
                    activity.startLineActivity((ViewStatus) msg.obj);
                    break;
                }
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
                    activity.adapter.addList((Notification) msg.obj);
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
