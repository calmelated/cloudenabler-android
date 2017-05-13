package tw.com.ksmt.cloud.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.MainApp;
import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Group;
import tw.com.ksmt.cloud.iface.Register;
import tw.com.ksmt.cloud.iface.ViewStatus;
import tw.com.ksmt.cloud.libs.Dbg;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.Utils;
import tw.com.ksmt.cloud.libs.WebUtils;

public class BarChartActivity extends ActionBarActivity {
    private final Context context = BarChartActivity.this;
    private ActionBar actionBar;
    private ProgressDialog mDialog;
    private Timer pollingTimer;
    private MsgHandler mHandler;
    private boolean isPolling = false;
    private BarChart mChart;
    private int orient;
    private int tzOffset;
    private YAxis leftAxis;
    private Group curGroup;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        AppSettings.setupLang(context);
        setContentView(R.layout.bar_chart);
        mChart = (BarChart) findViewById(R.id.bar_chart);
        mHandler = new MsgHandler(this);
        tzOffset = (Calendar.getInstance().getTimeZone().getRawOffset() / 1000);

        Intent intent = getIntent();
        curGroup = (Group) intent.getSerializableExtra("Group");
        if (curGroup == null) {
            MHandler.exec(mHandler, MHandler.GO_BACK, getString(R.string.err_get_data));
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AppSettings.reload(context);
    }

    private void setupChart() {
        try {
            actionBar.setTitle(curGroup.name);
            mChart.setDescription("");
            mChart.setNoDataTextDescription("You need to provide data for the chart.");

            // enable touch gestures
            mChart.setTouchEnabled(true);
            mChart.setDragEnabled(true);
            mChart.setScaleEnabled(true);
            mChart.setPinchZoom(false);
            mChart.setDoubleTapToZoomEnabled(false);
            mChart.setDrawGridBackground(true);
            mChart.clear();

            // modify the legend (only possible after setting data)
            Legend l = mChart.getLegend();
            // l.setPosition(LegendPosition.LEFT_OF_CHART);
            l.setForm(Legend.LegendForm.LINE);
            l.setTextColor(Color.BLACK);

            XAxis xl = mChart.getXAxis();
            xl.setTextColor(Color.BLACK);
            xl.setDrawGridLines(false);
            xl.setAvoidFirstLastClipping(true);
//            xl.setSpaceBetweenLabels(5);
            xl.setEnabled(true);
            xl.setPosition(XAxis.XAxisPosition.BOTTOM);

            YAxis rightAxis = mChart.getAxisRight();
            rightAxis.setEnabled(false);

            leftAxis = mChart.getAxisLeft();
            leftAxis.setDrawGridLines(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showChart(JSONArray jArray) {
        try {
            ArrayList<BarEntry> entries = new ArrayList<>();
            ArrayList<String> labels = new ArrayList<String>();
            for (int i = 0; i < jArray.length(); i++) {
                JSONObject iostat = (JSONObject) jArray.get(i);
                if(!iostat.has("haddr")) {
                    continue;
                }
                ViewStatus vs = new ViewStatus();
                vs.origId = iostat.has("id") ? iostat.getString("id") : "-1" ; // save original
                vs.id = vs.origId + i; // avoid two same id registers in the group

                // Get Device status
//                int hsIdx = Utils.slvIdx(iostat.getString("haddr"));
                vs.haddr  = Utils.realAddr(iostat.getString("haddr"));
                vs.hval   = iostat.has("hval") ? iostat.getString("hval") : "" ;
                vs.desc   = iostat.has("desc") ? iostat.getString("desc") : "" ;
//                vs.unit   = iostat.has("unit") ? iostat.getString("unit") : "" ;
//                vs.upVal  = iostat.has("up")   ? iostat.getString("up")   : "" ;
//                vs.lowVal = iostat.has("low")  ? iostat.getString("low")  : "" ;
                vs.fpt    = iostat.has("fpt")  ? iostat.getInt("fpt")     : 0;
//                vs.display= iostat.has("dt")   ? iostat.getInt("dt")      : 0;
//                vs.userControl  = iostat.has("userControl") ? ((iostat.getInt("userControl") == 1) ? true : false) : false ;
//                vs.enlog  = iostat.has("enlog")  ? ((iostat.getString("enlog").equals("1")) ? true : false) : false ;
                vs.swSN   = iostat.has("swSN")   ? iostat.getString("swSN")   : "" ;
                vs.swAddr = iostat.has("swAddr") ? iostat.getString("swAddr") : "" ;
                vs.swType = iostat.has("swType") ? iostat.getInt("swType")    : 0;

                vs.type = iostat.getInt("type");
                //Log.e(Dbg._TAG_(), "i=" + i + ", id=" + vs.id);
                if(vs.id.equals("") || vs.id.equals("-1")) {
                    continue;
                } else if (Register.is64Bits(vs.type) && vs.swType != Register.TYPE_ERROR) {
                    if(!iostat.has("iaddr") || !iostat.has("jaddr") || !iostat.has("laddr")) {
                        Log.e(Dbg._TAG_(), "Lost another byte for haddr: " + vs.haddr + " type: " + vs.type);
                        continue;
                    }
                    vs.iaddr = (iostat.has("iaddr")) ? Utils.realAddr(iostat.getString("iaddr")) : "" ;
                    vs.jaddr = (iostat.has("jaddr")) ? Utils.realAddr(iostat.getString("jaddr")) : "" ;
                    vs.laddr = (iostat.has("laddr")) ? Utils.realAddr(iostat.getString("laddr")) : "" ;
                    vs.ival = iostat.getString("ival");
                    vs.jval = iostat.getString("jval");
                    vs.lval = iostat.getString("lval");
                } else if (Register.is48Bits(vs.type) && vs.swType != Register.TYPE_ERROR) {
                    if(!iostat.has("iaddr") || !iostat.has("laddr")) {
                        Log.e(Dbg._TAG_(), "Lost another byte for haddr: " + vs.haddr + " type: " + vs.type);
                        continue;
                    }
                    vs.iaddr = (iostat.has("iaddr")) ? Utils.realAddr(iostat.getString("iaddr")) : "" ;
                    vs.laddr = (iostat.has("laddr")) ? Utils.realAddr(iostat.getString("laddr")) : "" ;
                    vs.ival = iostat.getString("ival");
                    vs.lval = iostat.getString("lval");
                } else if (Register.is32Bits(vs.type) && vs.swType != Register.TYPE_ERROR) {
                    vs.laddr = (iostat.has("laddr")) ? Utils.realAddr(iostat.getString("laddr")) : "" ;
                    if(vs.laddr.equals("")) {
                        Log.e(Dbg._TAG_(), "Lost another byte for  haddr: " + vs.haddr + " type: " + vs.type);
                        continue;
                    }
                    vs.lval = iostat.getString("lval");
                } else { // 16bit, alarm
                    //
                }
                // Add Entry
                labels.add(vs.desc);
                vs.showVal = Register.isIOSW(vs.type) ? vs.setShowVal(vs.swType) : vs.setShowVal(vs.type);
                if(vs.showVal != null && !vs.showVal.equals("")) {
                    entries.add(new BarEntry(Float.parseFloat(vs.showVal), i));
                }
            }
            BarDataSet dataset = new BarDataSet(entries, getString(R.string.real_time));
            dataset.setColors(ColorTemplate.COLORFUL_COLORS);
            dataset.setHighlightEnabled(false);
            BarData data = new BarData(labels, dataset);
            mChart.clear();
            mChart.setData(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        orient = getResources().getConfiguration().orientation;
        startPolling();
    }

    private void startPolling() {
        if (pollingTimer != null) {
            pollingTimer.cancel();
        }
        if (mDialog != null) {
            mDialog.cancel();
        }
        // Reset Chart
        setupChart();
        isPolling = false;
        mDialog = Kdialog.getProgress(context, mDialog);
        pollingTimer = new Timer();
        pollingTimer.schedule(new PollingTimerTask(), 0, PrjCfg.VIEW_CHART_POLLING);
    }

    private class PollingTimerTask extends TimerTask {
        public void run() {
            try {
                if (isPolling) { return; }
                isPolling = true;

                JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/group/status/" + WebUtils.encode(curGroup.name));
                int statCode = jObject.getInt("code");
                if (statCode != 200) {
                    return;
                }

                // draw chart
                JSONArray iostats = jObject.getJSONArray("iostats");
                if(iostats.length() == 0) {
                    MHandler.exec(mHandler, MHandler.GO_BACK, getString(R.string.err_get_data));
                }
                //Log.e(Dbg._TAG_(), iostats.toString());
                MHandler.exec(mHandler, MHandler.UPDATE, iostats);

                // Password check
                if (Utils.loadPrefs(context, "Password", "").equals("")) {
                    MHandler.exec(mHandler, MHandler.BEEN_LOGOUT);
                } else if (MainApp.SERV_MAINT > (new Date().getTime() / 1000)) {
                    MHandler.exec(mHandler, MHandler.SERV_MAINT);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isPolling = false;
                mDialog.dismiss();
            }
        }
    }

    private static class MsgHandler extends MHandler {
        public MsgHandler(BarChartActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            BarChartActivity activity = (BarChartActivity) super.mActivity.get();
            switch (msg.what) {
                case MHandler.UPDATE: {
                    activity.showChart((JSONArray) msg.obj);
                    break;
                }
                default: {
                    super.handleMessage(msg);
                }
            }
        }
    }
}
