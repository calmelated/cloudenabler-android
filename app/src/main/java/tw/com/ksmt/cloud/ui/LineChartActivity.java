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
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.MainApp;
import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Register;
import tw.com.ksmt.cloud.iface.ViewStatus;
import tw.com.ksmt.cloud.libs.Dbg;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.Utils;

public class LineChartActivity extends ActionBarActivity implements OnChartValueSelectedListener {
    private final Context context = LineChartActivity.this;
    private ActionBar actionBar;
    private ProgressDialog mDialog;
    private Timer pollingTimer;
    private MsgHandler mHandler;
    private boolean isPolling = false;
    private LineChart mChart;
    private int orient;
    private int tzOffset;
    private YAxis leftAxis;

    // from last activity
    private ViewStatus curVs;
    private String curSn;
    private String curSlvIdx;
    private String curAddr;

    // raw, day, week, month, quarter
    private String dataType = "raw";
    private String chDataType;
    private String chLogType;
    private String stUnit;
    private String timeUnit;
    private String unitTimeUnit;
    private int numReqData;

    //Highlight
    private Highlight hlMax = null;
    private Highlight hlMin = null;
    private Highlight hlSelect = null;

    // Remember current timestamp
    private int curTime;
    private int lastPosTime;
    private int startTime;
    private int endTime;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        AppSettings.setupLang(context);
        setContentView(R.layout.view_chart);
        mChart = (LineChart) findViewById(R.id.line_chart);
        mHandler = new MsgHandler(this);
        tzOffset = (Calendar.getInstance().getTimeZone().getRawOffset() / 1000);

        Intent intent = getIntent();
        curVs = (ViewStatus) intent.getSerializableExtra("curVs");
        curSn = curVs.sn;
        curAddr = curVs.haddr;
        curSlvIdx = (curVs.slvIdx > 0) ? (curVs.slvIdx + "") : "";
        if (curVs.type < 0 || curVs.id == null || curVs.id.equals("-1") || curVs.id.equals("")) { // unknown register
            MHandler.exec(mHandler, MHandler.GO_BACK, getString(R.string.err_register));
        }

        dataType = intent.getStringExtra("dataType");
        if(dataType != null && !dataType.equals("raw")) {
            reloadConfig("day");
            chLogType = intent.getStringExtra("logType");
            curTime = intent.getIntExtra("curTime", -1);
        } else {
            reloadConfig("raw");
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AppSettings.reload(context);
    }

    private void reloadConfig(String _dataType) {
        dataType = _dataType;
        chLogType = null;
        curTime = 0;

        if(_dataType.equals("day")) {
            stUnit = "MM/dd HH:mm";
            timeUnit = "HH:mm";
            unitTimeUnit = getString(R.string.unix_time_hhmm);
            numReqData = 360;
            chDataType = getString(R.string.day);
        } else if(_dataType.equals("week")) {
            stUnit = "yyyy/MM/dd";
            timeUnit = "MM/dd";
            unitTimeUnit = getString(R.string.unix_time_mmdd);
            numReqData = 360;
            chDataType = getString(R.string.week);
        } else if(_dataType.equals("month")) {
            stUnit = "yyyy/MM/dd";
            timeUnit = "MM/dd";
            unitTimeUnit = getString(R.string.unix_time_mmdd);
            numReqData = 360;
            chDataType = getString(R.string.month);
        } else if(_dataType.equals("year")) {
            stUnit = "yyyy/MM/dd";
            timeUnit = "MM/dd";
            unitTimeUnit = getString(R.string.unix_time_mmdd);
            numReqData = 360;
            chDataType = getString(R.string.year);
        } else { // raw
            timeUnit = "HH:mm:ss";
            unitTimeUnit = getString(R.string.unix_time_hhmmss);
            curTime = (int) (new Date().getTime() / 1000);
        }
    }

    private void setupChart() {
        try {
            actionBar.setSubtitle("");
            mChart.setOnChartValueSelectedListener(this);

            // no description text
            mChart.setDescription("");
            mChart.setNoDataTextDescription("You need to provide data for the chart.");
            mChart.setMarkerView(new MyMarkerView(this, R.layout.custom_marker_view));

            // enable touch gestures
            mChart.setTouchEnabled(true);
            mChart.setDragEnabled(true);
            mChart.setScaleEnabled(true);
            mChart.setPinchZoom(false);
            mChart.setDoubleTapToZoomEnabled(false);
            mChart.setDrawGridBackground(true);

            // set an alternative background color
            //mChart.setBackgroundColor(Color.WHITE);
            LineData data = new LineData();
            data.setValueTextColor(Color.BLUE);

            // add empty data
            mChart.clear();
            mChart.setData(data);

            // modify the legend (only possible after setting data)
            Legend l = mChart.getLegend();
            // l.setPosition(LegendPosition.LEFT_OF_CHART);
            l.setForm(Legend.LegendForm.LINE);
            l.setTextColor(Color.BLACK);

            XAxis xl = mChart.getXAxis();
            xl.setTextSize(12f);
            xl.setTextColor(Color.BLACK);
            xl.setDrawGridLines(false);
            xl.setAvoidFirstLastClipping(true);
            xl.setSpaceBetweenLabels(5);
            xl.setEnabled(true);
            xl.setPosition(XAxis.XAxisPosition.BOTTOM);

            leftAxis = mChart.getAxisLeft();
            leftAxis.resetAxisMaxValue();
            leftAxis.resetAxisMinValue();
            leftAxis.setTextSize(12f);
            leftAxis.setTextColor(Color.BLACK);
            leftAxis.setDrawGridLines(true);
            leftAxis.setStartAtZero(false);
//            leftAxis.setValueFormatter(new YAxisValueFormatter() {
//                @Override
//                public String getFormattedValue(float v, YAxis yAxis) {
//                    DecimalFormat mFormat = null;
//                    if(curVs.fpt == 0) {
//                        mFormat = new DecimalFormat("#");
//                    } else if(curVs.fpt == 1) {
//                        mFormat = new DecimalFormat("#.0");
//                    } else if(curVs.fpt == 2) {
//                        mFormat = new DecimalFormat("#.00");
//                    } else if(curVs.fpt == 3) {
//                        mFormat = new DecimalFormat("#.000");
//                    } else if(curVs.fpt == 4) {
//                        mFormat = new DecimalFormat("#.0000");
//                    }
//                    return (mFormat == null) ? "" : mFormat.format(v);
//                }
//            });

            YAxis rightAxis = mChart.getAxisRight();
            rightAxis.setEnabled(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawLimitLine(int limitType, String limitVal) {
        if(limitVal.equals("")) {
            return;
        }
        try {
            float limVal = Float.parseFloat(limitVal);
            LimitLine lltmp = new LimitLine(limVal, getString(limitType) + ": " + limitVal);
            lltmp.enableDashedLine(10f, 10f, 0);
//            lltmp.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
            lltmp.setTextSize(12f);
            lltmp.setTextColor(Color.parseColor("#FF1010"));
            leftAxis.addLimitLine(lltmp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private LineDataSet createSet() {
        return createSet(null, String.valueOf(Html.fromHtml("<small>" + getString(R.string.real_time) + " (" + unitTimeUnit + ") </small>")));
    }

    private LineDataSet createSet(List<Entry> yVals, String setName) {
        LineDataSet set = new LineDataSet(yVals, setName);
        set.setColor(ColorTemplate.getHoloBlue());
//        set.setLineWidth(3f);
//        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setHighLightColor(Color.argb(0, 0, 255, 0));
        set.setHighlightEnabled(true);
        set.setValueTextColor(Color.BLACK);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        set.setDrawFilled(true);
        set.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
//        set.setDrawCircles(false);
//        set.setDrawCircleHole(false);
//        set.setDrawValues(true);
        return set;
    }

    private void showRtChart(ViewStatus vs) {
        LineData data = mChart.getData();
        if (data == null || vs.showVal == null || vs.showVal.equals("")) {
            return;
        }
        try {
            LineDataSet set = (LineDataSet) data.getDataSetByIndex(0);
            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            // add a new x-value first
            float newVal = Float.parseFloat(vs.showVal);
            String xLable = Utils.unix2Datetime((new Date().getTime() / 1000), "HH:mm:ss");
            data.addXValue(xLable);
            data.addEntry(new Entry(newVal, set.getEntryCount()), 0);

            // Draw limit line for upper/lower bound
            leftAxis.removeAllLimitLines();
            drawLimitLine(R.string.upbound_alarm, curVs.upVal);
            drawLimitLine(R.string.lowbound_alarm, curVs.lowVal);

            // let the chart know it's data has changed
            mChart.notifyDataSetChanged();

            // limit the number of visible entries
            int maxRange = (orient == Configuration.ORIENTATION_LANDSCAPE) ? 30 : 15;
            mChart.setVisibleXRangeMaximum(maxRange);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            int numData = data.getXValCount();
            mChart.moveViewToX(numData - maxRange);

            // highlight max/min value whithin latest max-range data
            if(numData > 0) {
                LineDataSet lineDataSet = (LineDataSet) data.getDataSetByIndex(0);
                int maxIdx = 0, minIdx = 0;
                float maxVal = Float.MIN_VALUE, minVal = Float.MAX_VALUE;
                for(int i = (numData > maxRange ? (numData - maxRange) : 0); i < numData; i++) {
                    Entry entry = lineDataSet.getEntryForXIndex(i);
                    float iVal = entry.getVal();
                    //Log.e(Dbg._TAG_(),  i + " = " + iVal);
                    if(iVal > maxVal) {
                        maxIdx = i;
                        maxVal = iVal;
                    } else if(iVal < minVal) {
                        minIdx = i;
                        minVal = iVal;
                    }
                }
                //Log.e(Dbg._TAG_(), "minIdx=" + minIdx + ", minVal=" + minVal + ", maxIdx=" + maxIdx + ", maxVal=" + maxVal);
                hlMax = new Highlight(maxIdx, 0);
                hlMin = new Highlight(minIdx, 0);
                drawHighlight();
            }

            // this automatically refreshes the chart (calls invalidate())
            // mChart.moveViewTo(data.getXValCount()-7, 55f, AxisDependency.LEFT);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void showChart(JSONObject jObject) {
        LineData data = mChart.getData();
        if (data == null) {
            return;
        }
        try {
            int num = jObject.getInt("num");
            startTime = jObject.getInt("start");
            endTime = jObject.getInt("end");
            actionBar.setSubtitle(Html.fromHtml("<small><small>" + getString(R.string.time_range) + ": " + Utils.unix2Datetime(startTime, stUnit) + " - " + Utils.unix2Datetime(endTime, stUnit) + "</small></small>"));
            JSONArray timeArray = jObject.getJSONArray("time");
            JSONArray dataArray = jObject.getJSONArray("data");
            if(timeArray == null || dataArray == null) {
                return;
            }

            int maxIdx = 0, minIdx = 0;
            ArrayList<String> xVals = new ArrayList<String>();
            ArrayList<Entry> yVals = new ArrayList<Entry>();
            for (int i = 0; i < num; i++) {
                float curVal = (float) dataArray.getDouble(i);
                xVals.add(Utils.unix2Datetime(timeArray.getInt(i), timeUnit));
                yVals.add(new Entry(curVal, i));
                if(curVal > yVals.get(maxIdx).getVal()) {
                    maxIdx = i;
                }
                if(curVal < yVals.get(minIdx).getVal()) {
                    minIdx = i;
                }
            }
            hlMax = new Highlight(maxIdx, 0);
            hlMin = new Highlight(minIdx, 0);

            String logTypeStr = ", ";
            if(chLogType.equals("max")) {
                logTypeStr += getString(R.string.max);
            } else if(chLogType.equals("min")) {
                logTypeStr += getString(R.string.min);
            } else {
                logTypeStr += getString(R.string.average);
            }

            LineDataSet yDataSet = createSet(yVals, chDataType + logTypeStr + " (" + unitTimeUnit + ")");
            ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
            dataSets.add(yDataSet); // add the datasets
            LineData xDataSet = new LineData(xVals, yDataSet);

            mChart.clear();
            mChart.setData(xDataSet);
            mChart.setVisibleXRangeMaximum(numReqData);

            // Hightlight and Draw limit line for upper/lower bound
            leftAxis.removeAllLimitLines();
            drawLimitLine(R.string.upbound_alarm, curVs.upVal);
            drawLimitLine(R.string.lowbound_alarm, curVs.lowVal);
            drawHighlight();
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
        int newOrient = getResources().getConfiguration().orientation;
        if(orient != newOrient) {
            invalidateOptionsMenu();
            orient = newOrient;
        }
        LineData ldata = mChart.getLineData();
        if(ldata != null) {
            //Log.e(Dbg._TAG_(), "count=" + ldata.getXValCount());
            int nEntries = ldata.getXValCount();
            if(dataType.equals("raw")) {
//                if(nEntries == 0) {
//                    setupChart();
//                }
                startPolling();
            } else { // other case
                if(nEntries == 0) {
                    startPolling();
                }
            }
        } else { // no any data
            startPolling();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(!Register.enCloudLogging(curVs.type)) {
            return false;
        }
        getMenuInflater().inflate(R.menu.view_chart, menu);
        MenuItem prev = menu.findItem(R.id.prev);
        MenuItem next = menu.findItem(R.id.next);
        MenuItem average = menu.findItem(R.id.average);
        MenuItem max = menu.findItem(R.id.max);
        MenuItem min = menu.findItem(R.id.min);
        if (dataType.equals("raw")) {
            prev.setVisible(false);
            next.setVisible(false);
            average.setVisible(false);
            min.setVisible(false);
            max.setVisible(false);
        } else {
            if(chLogType.equals("average")) {
                average.setVisible(false);
                min.setVisible(true);
                max.setVisible(true);
            } else if(chLogType.equals("min")) {
                min.setVisible(false);
                average.setVisible(true);
                max.setVisible(true);
            } else if(chLogType.equals("max")) {
                average.setVisible(true);
                max.setVisible(false);
                min.setVisible(true);
            }
            prev.setVisible(true);
            next.setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.realTime:
                dataType = "raw";
                timeUnit = "HH:mm:ss";
                unitTimeUnit = getString(R.string.unix_time_hhmmss);
                chLogType = null;
                chDataType = null;
                curTime = (int) (new Date().getTime() / 1000);
                startPolling();
                break;

            case R.id.search:
                chooseLog();
                break;

            case R.id.prev:
                prevOrNext(true);
                break;

            case R.id.next:
                prevOrNext(false);
                break;

            case R.id.average:
                chLogType = "average";
                startPolling();
                break;

            case R.id.min:
                chLogType = "min";
                startPolling();
                break;

            case R.id.max:
                chLogType = "max";
                startPolling();
                break;
        }
        return true;
    }

    private void setLogType(int type) {
        if(type == R.string.average) {
            chLogType = "average";
        } else if(type == R.string.max) {
            chLogType = "max";
        } else if(type == R.string.min) {
            chLogType = "min";
        } else {
            Log.e(Dbg._TAG_(), "Unknown Log type!");
            return;
        }
        startPolling();
    }

    private void prevOrNext(boolean isPrev) {
        lastPosTime = curTime;
        if(dataType.equals("day")) {
            curTime = (isPrev) ? (startTime - 36000) : (endTime + 36000);
        } else if(dataType.equals("week")) {
            curTime = (isPrev) ? (startTime - 86400) : (endTime + 86400);
        } else if(dataType.equals("month")) {
            curTime = (isPrev) ? (startTime - 86400) : (endTime + 172800);
        } else if(dataType.equals("year")) {
            curTime = (isPrev) ? (startTime - 86400) : (endTime + 172800);
        }
        //Log.e(Dbg._TAG_(), "new curtime = " + curTime);
        startPolling();
    }

    private void chooseLog() {
        final View chLogLayout = LayoutInflater.from(context).inflate(R.layout.dialog_choose_log, null);
        final DatePicker datePicker = (DatePicker) chLogLayout.findViewById(R.id.datePicker);

        // Date Range (day, month, year)
        final Spinner spinDataType= (Spinner) chLogLayout.findViewById(R.id.spinDataRange);
        ArrayAdapter<String> dataRangeAdapter = new ArrayAdapter<String>(context, R.layout.spinner);
        dataRangeAdapter.addAll(new String[]{getString(R.string.day), getString(R.string.week), getString(R.string.month), getString(R.string.year)});
        dataRangeAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinDataType.setAdapter(dataRangeAdapter);

        // log type (average, min, max)
        final Spinner spinLogType = (Spinner) chLogLayout.findViewById(R.id.spinDataType);
        ArrayAdapter<String> logTypeAdapter = new ArrayAdapter<String>(context, R.layout.spinner);
        logTypeAdapter.addAll(new String[]{getString(R.string.average), getString(R.string.max), getString(R.string.min)});
        logTypeAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinLogType.setAdapter(logTypeAdapter);

        final AlertDialog dialog = Kdialog.getDefInputDialog(context).create();
        dialog.setView(chLogLayout);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogIface) {
                Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        chDataType = (String) spinDataType.getSelectedItem();
                        if (chDataType.equals(getString(R.string.day))) {
                            reloadConfig("day");
                        } else if (chDataType.equals(getString(R.string.week))) {
                            reloadConfig("week");
                        } else if (chDataType.equals(getString(R.string.month))) {
                            reloadConfig("month");
                        } else if (chDataType.equals(getString(R.string.year))) {
                            reloadConfig("year");
                        }

                        chLogType = (String) spinLogType.getSelectedItem();
                        if(chLogType.equals(getString(R.string.average))) {
                            chLogType = "average";
                        } else if(chLogType.equals(getString(R.string.max))) {
                            chLogType = "max";
                        } else if(chLogType.equals(getString(R.string.min))) {
                            chLogType = "min";
                        }

                        Calendar calendar = new GregorianCalendar(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
                        curTime = (int) (calendar.getTimeInMillis() / 1000);
                        //Log.e(Dbg._TAG_(), "dataType=" + dataType + ", logType=" + chLogType + ", y/m/d" + curYear + "/" + curMonth + "/" + curDay);
                        startPolling();
                    }
                });
            }
        });
        dialog.show();
    }

    private void startPolling() {
        if (pollingTimer != null) {
            pollingTimer.cancel();
        }
        if (mDialog != null) {
            mDialog.cancel();
        }

        // Reset Chart
        invalidateOptionsMenu();
        setupChart();
        hlSelect = null;
        isPolling = false;
        actionBar.setTitle(curVs.desc);

        int pollTime = PrjCfg.RUN_ONCE;
        if(dataType.equals("raw")) {
            pollTime = PrjCfg.VIEW_CHART_POLLING;
            hlMax = new Highlight(0, 0);
            hlMin = new Highlight(0, 0);
        } else {
            hlMin = null;
            hlMax = null;
        }

        mDialog = Kdialog.getProgress(context, mDialog);
        pollingTimer = new Timer();
        pollingTimer.schedule(new PollingTimerTask(), 0, pollTime);
    }

    @Override
    public void onValueSelected(Entry entry, int i, Highlight highlight) {
        hlSelect = highlight;
        drawHighlight();
    }

    @Override
    public void onNothingSelected() {
        hlSelect = null;
        drawHighlight();
    }

    private void drawHighlight() {
        if(hlSelect == null) {
            mChart.highlightValues(new Highlight[]{hlMax, hlMin});
        } else {
            mChart.highlightValues(new Highlight[]{hlMax, hlMin, hlSelect});
        }
    }

    private static int lastStatus = -1;

    private class PollingTimerTask extends TimerTask {
        public void run() {
            try {
                if (isPolling) { return; }
                isPolling = true;

                if(dataType.equals("raw")) { // Real-time log
                    JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/device/" + curSn + "/status?addr=" + curSlvIdx + curAddr);
                    int statCode = jObject.getInt("code");
                    if (statCode != 200) {
                        return;
                    }

                    // Check device status
                    int status = jObject.has("status") ?  jObject.getInt("status") : 0 ;
                    if(lastStatus < 0) { // first time, ignore the check
                        if(status == 0) {
                            MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.device_offline));
                        }
                    } else if(status != lastStatus) {
                        if(status == 1) { // online
                            MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.device_online));
                        } else { // offline
                            MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.device_offline));
                        }
                    }
                    lastStatus = status;
                    if(status == 0) {
                        return;
                    }

                    // draw chart
                    JSONArray iostats = jObject.getJSONArray("iostats");
                    if(iostats.length() == 0) {
                        MHandler.exec(mHandler, MHandler.GO_BACK, getString(R.string.err_register));
                    }

                    // Parsing data
                    JSONObject jStats = (JSONObject) iostats.get(0);
                    //Log.e(Dbg._TAG_(), jStats.toString());
                    String haddr = jStats.getString("haddr");
                    String hval = jStats.getString("hval");
                    int fpt = jStats.has("fpt") ? jStats.getInt("fpt") : 0;
                    int type = jStats.getInt("type");
                    if (Register.is64Bits(type)) {
                        String iaddr = jStats.getString("iaddr");
                        String jaddr = jStats.getString("jaddr");
                        String laddr = jStats.getString("laddr");
                        String ival = jStats.getString("ival");
                        String jval = jStats.getString("jval");
                        String lval = jStats.getString("lval");
                        MHandler.exec(mHandler, MHandler.UPDATE, new ViewStatus(type, haddr, hval, iaddr, ival, jaddr, jval, laddr, lval, fpt));
                    } else if (Register.is48Bits(type)) {
                        String iaddr = jStats.getString("iaddr");
                        String laddr = jStats.getString("laddr");
                        String ival = jStats.getString("ival");
                        String lval = jStats.getString("lval");
                        MHandler.exec(mHandler, MHandler.UPDATE, new ViewStatus(type, haddr, hval, iaddr, ival, laddr, lval, fpt));
                    } else if (Register.is32Bits(type)) {
                        String laddr = jStats.getString("laddr");
                        String lval = jStats.getString("lval");
                        MHandler.exec(mHandler, MHandler.UPDATE, new ViewStatus(type, haddr, hval, laddr, lval, fpt));
                    } else { // 16bit, Alarm
                        MHandler.exec(mHandler, MHandler.UPDATE, new ViewStatus(type, haddr, hval, fpt));
                    }
                } else { // History log
                    String logTypeStr = (chLogType != null) ? ("&type=" + chLogType) : "";
                    String timeStr = (curTime > 0) ? "&t=" + curTime : "";
                    JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/device/" + curSn + "/" + curSlvIdx + curAddr + "/dt/" + dataType + "?num=" + numReqData + "&tz=" + tzOffset + timeStr + logTypeStr);
//                    Log.e(Dbg._TAG_(), jObject.toString());
                    if (!(jObject.has("num") && jObject.getInt("num") > 0)) {
                        Log.e(Dbg._TAG_(), "load back last=" + lastPosTime + ", cur=" + curTime);
                        curTime = (lastPosTime > 0) ? lastPosTime : curTime;
                        lastPosTime = -1;
                        MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.no_cloud_logging_data));
                        return; //  don't have anything
                    }
                    MHandler.exec(mHandler, MHandler.UPDATE, jObject);
                }
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
                if (!dataType.equals("raw")) {
                    pollingTimer.cancel();
                }
            }
        }
    }

    class MyMarkerView extends MarkerView {
        private TextView tvContent;
        private RelativeLayout tvrLayout;

        public MyMarkerView(Context context, int layoutResource) {
            super(context, layoutResource);
            tvrLayout = (RelativeLayout) findViewById(R.id.tvrLayout);
            tvContent = (TextView) findViewById(R.id.tvContent);
        }

        @Override
        public void refreshContent(Entry e, Highlight highlight) {
            if (e instanceof CandleEntry) {
                CandleEntry ce = (CandleEntry) e;
                tvContent.setText("" + ce.getHigh());
            } else {
                tvContent.setText("" + e.getVal());
            }
            if (hlSelect != null && e.getXIndex() == hlSelect.getXIndex()) {
                tvrLayout.setBackgroundResource(R.drawable.marker2);
            } else if (e.getXIndex() == hlMin.getXIndex() || e.getXIndex() == hlMax.getXIndex()) {
                tvrLayout.setBackgroundResource(R.drawable.marker1);
            }
        }

        @Override
        public int getXOffset(float xpos) {
            return -(getWidth() / 2);
        }

        @Override
        public int getYOffset(float ypos) {
            return -getHeight();
        }
    }

    private static class MsgHandler extends MHandler {
        public MsgHandler(LineChartActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            LineChartActivity activity = (LineChartActivity) super.mActivity.get();
            switch (msg.what) {
                case MHandler.UPDATE: {
                    if(msg.obj instanceof  ViewStatus) {
                        activity.showRtChart((ViewStatus) msg.obj);
                    } else {
                        activity.showChart((JSONObject) msg.obj);
                    }
                    break;
                }
                default: {
                    super.handleMessage(msg);
                }
            }
        }
    }
}
