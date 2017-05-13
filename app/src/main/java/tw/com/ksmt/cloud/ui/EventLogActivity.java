package tw.com.ksmt.cloud.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.MainApp;
import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Device;
import tw.com.ksmt.cloud.iface.EventLog;
import tw.com.ksmt.cloud.libs.Dbg;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.SearchViewTask;
import tw.com.ksmt.cloud.libs.Utils;

public class EventLogActivity extends ActionBarActivity implements PullToRefreshBase.OnRefreshListener2<ListView>, AbsListView.OnScrollListener, SearchView.OnQueryTextListener {
    private final Context context = EventLogActivity.this;
    private ActionBar actionBar;
    private PullToRefreshListView prListView;
    private ListView listView;
    private EventLogAdapter adapter;
    private ProgressDialog mDialog;
    private Timer pollingTimer;
    private Timer clearTimer;
    private MsgHandler mHandler;
    private SearchViewTask searchTask;
    private SearchView mSearchView;
    private Device curDevice;
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
        setTitle(getString(R.string.event_log));
        mHandler = new MsgHandler(this);

        Intent intent = getIntent();
        curDevice = (Device) intent.getSerializableExtra("Device");
        getSupportActionBar().setSubtitle(Html.fromHtml("<small><small><i>" + getString(R.string.device) + ": " + curDevice.name + "</i></small></small>"));

        prListView = (PullToRefreshListView) findViewById(R.id.listView);
        prListView.setOnRefreshListener(this);
        adapter = new EventLogAdapter(context);
        listView = prListView.getRefreshableView();
        listView.setAdapter(adapter);
        listView.setOnScrollListener(this);

        // Get Event log
        pollingTimer = new Timer();
        pollingTimer.schedule(new PollingTimerTask(), 0, PrjCfg.RUN_ONCE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (pollingTimer != null) {
            pollingTimer.cancel();
        }
        if (clearTimer != null) {
            clearTimer.cancel();
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
        getMenuInflater().inflate(R.menu.eventlog, menu);

        MenuItem searchItem = menu.findItem(R.id.search);
        MenuItemCompat.collapseActionView(searchItem);
        mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setIconified(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear_log) {
            clearEventLogsDialog();
        }
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

    private class ClearLogTimerTask extends TimerTask {
        private long startDate;
        private long endDate;

        public ClearLogTimerTask(long startDate, long endDate) {
            this.startDate = startDate / 1000;
            this.endDate = endDate / 1000;
        }

        public void run() {
            try {
                JSONObject jDevObject = JSONReq.send(context, "DELETE", PrjCfg.CLOUD_URL + "/api/device/" + curDevice.sn + "/evtlog/clear?st=" + startDate + "&et=" + endDate);
                if (jDevObject == null || jDevObject.getInt("code") != 200) {
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.err_clear));
                    mDialog.dismiss();
                } else {
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_clear));
                    onPullDownToRefresh(null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                clearTimer.cancel();
            }
        }
    }

    private class PollingTimerTask extends TimerTask {
        public void run() {
            try {
                getEventLog();
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

    private void getEventLog() throws Exception {
        if(isPolling) {
            return;
        } else {
            isPolling = true;
        }
        //Log.e(Dbg._TAG_(), "idxItem=" + idxItem + " total=" + totalItems);
        String uri = (totalItems < 0) ? "&from=0" : "&from=1&et=" + idxItem;
        //Log.e(Dbg._TAG_(), "/api/alarm?num=" + PrjCfg.LOAD_MORE_NUM + uri);
        JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/device/" + curDevice.sn + "/evtlog?num=" + PrjCfg.LOAD_MORE_NUM + uri);
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
        JSONArray evtlogs = jObject.getJSONArray("evtlogs");
        int evtlen = evtlogs.length();
        for (int i = 0; i < evtlen; i++) {
            EventLog evtlog = new EventLog(context, (JSONObject) evtlogs.get(i));
            MHandler.exec(mHandler, MHandler.ADD_LIST, evtlog);
            if (i == (evtlen - 1)) { // last one
                idxItem = (idxItem == evtlog.time) ? (evtlog.time - 1) : evtlog.time ;
            }
        }
        if (evtlen < PrjCfg.LOAD_MORE_NUM) { // no more data
            noMoreData = true;
        }
        isPolling = false;
    }

    private long DateToMillliSec(Boolean incToday, DatePicker datePicker) {
        try {
            int selectedYear = datePicker.getYear();
            int selectedMonth = datePicker.getMonth();
            int selectedDay = datePicker.getDayOfMonth();
            //Log.e(Dbg._TAG_(), "Date: " + selectedYear + "/" + (selectedMonth + 1) + "/" + selectedDay);
            if(incToday) {
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                formatter.setLenient(false);
                return formatter.parse(selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear + " 23:59:59").getTime();
            } else {
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
                formatter.setLenient(false);
                return formatter.parse(selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear).getTime();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return 0l;
    }

    private void clearEventLogsDialog() {
        LayoutInflater inflater = LayoutInflater.from(context);
        final View layout = inflater.inflate(R.layout.dialog_clear_log, null);
        final DatePicker sDate = (DatePicker) layout.findViewById(R.id.startDate);
        final DatePicker eDate = (DatePicker) layout.findViewById(R.id.endDate);

        AlertDialog.Builder builder = Kdialog.getDefInputDialog(context, layout);
        builder.setTitle(getString(R.string.clear_log));
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogIface) {
                Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        long startDate = DateToMillliSec(false, sDate);
                        long endDate = DateToMillliSec(true, eDate);
                        if(startDate == 0 && endDate == 0) { // nothing
                            return;
                        }
                        if(startDate > 0 && endDate > 0) {
                            if(startDate > endDate) {
                                long tmp = startDate;
                                startDate = endDate;
                                endDate = tmp;
                            }
                        }
                        mDialog = Kdialog.getProgress(context, mDialog);
                        clearTimer = new Timer();
                        clearTimer.schedule(new ClearLogTimerTask(startDate, endDate), 0, PrjCfg.RUN_ONCE);
                        dialog.dismiss();
                    }
                });
            }
        });
        dialog.show();
    }

    private static class MsgHandler extends MHandler {
        public MsgHandler(EventLogActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final EventLogActivity activity = (EventLogActivity) super.mActivity.get();
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
                    activity.adapter.addList((EventLog) msg.obj);
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
