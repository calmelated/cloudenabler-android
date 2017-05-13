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
import android.text.InputFilter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.MainApp;
import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Device;
import tw.com.ksmt.cloud.iface.MstDev;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.StrUtils;
import tw.com.ksmt.cloud.libs.Utils;

public class NewSlaveDevActivity extends ActionBarActivity {
    private Context context = NewSlaveDevActivity.this;
    private ActionBar actionBar;
    private ProgressDialog mDialog;
    private Timer bgTimer;
    private MsgHandler mHandler;

    private LinearLayout layoutSerial;
    private LinearLayout layoutTCP;
    private Spinner  spinMbusType;
    private Spinner spinComPort;
    private Spinner spinSlvId;
    private Spinner spinTCPSlvId;
    private EditText txtDevName;
    private EditText txtIp;
    private EditText txtPort;
    private CheckBox cboxEnable;
    private Button btnConfirm;

    private ArrayAdapter<String> mbusTypeAdapter;
    private ArrayAdapter<String> comPortAdapter;
    private ArrayAdapter<String> slvIdAdapter;
    private ArrayAdapter<String> tcpSlvIdAdapter;
    private Device curDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("  " + getString(R.string.new_slave_device));
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        actionBar.setIcon(R.drawable.ic_new_device_1);
        actionBar.setDisplayShowHomeEnabled(true);

        AppSettings.setupLang(context);
        setContentView(R.layout.master_dev_manage);
        mHandler = new MsgHandler(this);
        curDevice = (Device) getIntent().getSerializableExtra("Device");

        txtDevName = (EditText) findViewById(R.id.editDevName);
        cboxEnable = (CheckBox) findViewById(R.id.enable);
        btnConfirm = (Button) findViewById(R.id.btnDevConfirm);
        txtDevName.setFilters(Utils.arrayMerge(txtDevName.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
        btnConfirm.setOnClickListener(btnDeviceListener);

        // Master Mode Layout
        layoutSerial = (LinearLayout) findViewById(R.id.layoutSerial);
        layoutTCP = (LinearLayout) findViewById(R.id.layoutTCP);

        // Master Mode
        spinMbusType = (Spinner) findViewById(R.id.spinMbusType);
        mbusTypeAdapter = new ArrayAdapter<String>(context, R.layout.spinner);
        mbusTypeAdapter.add("Serial");
        mbusTypeAdapter.add("TCP");
        mbusTypeAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinMbusType.setAdapter(mbusTypeAdapter);
        spinMbusType.setOnItemSelectedListener(mbusTypeListener);

        // Modbus Master TCP mode
        txtIp = (EditText) findViewById(R.id.editIp);
        txtPort = (EditText) findViewById(R.id.editPort);
        spinTCPSlvId = (Spinner) findViewById(R.id.spinTCPSlvId);
        tcpSlvIdAdapter = new ArrayAdapter<String>(context, R.layout.spinner);
        tcpSlvIdAdapter.add(getString(R.string.none));
        for(int i = 1; i < 255; i++) {
            tcpSlvIdAdapter.add(String.valueOf(i));
        }
        tcpSlvIdAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinTCPSlvId.setAdapter(tcpSlvIdAdapter);

        // Master Serial Mode - COM Port
        spinComPort = (Spinner) findViewById(R.id.spinComPort);
        comPortAdapter = new ArrayAdapter<String>(context, R.layout.spinner);
        comPortAdapter.add("COM0");
        comPortAdapter.add("COM1");
        comPortAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinComPort.setAdapter(comPortAdapter);

        // Master Serial Mode - Slave ID
        spinSlvId = (Spinner) findViewById(R.id.spinSlvId);
        slvIdAdapter = new ArrayAdapter<String>(context, R.layout.spinner);
        for(int i = 1; i < 255; i++) {
            slvIdAdapter.add(String.valueOf(i));
        }
        slvIdAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinSlvId.setAdapter(slvIdAdapter);
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

    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AppSettings.reload(context);
    }

    AdapterView.OnItemSelectedListener mbusTypeListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if(position == 0) { // serial port
                layoutSerial.setVisibility(View.VISIBLE);
                layoutTCP.setVisibility(View.GONE);
            } else { // tcp
                layoutSerial.setVisibility(View.GONE);
                layoutTCP.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    View.OnClickListener btnDeviceListener = new Button.OnClickListener() {
        public void onClick(View v) {
            String name = txtDevName.getText().toString().trim();
            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, name)) {
                txtDevName.setError(getString(R.string.err_msg_empty));
                return;
            } else if (!StrUtils.validateInput(StrUtils.IN_TYPE_STRING, name)) {
                txtDevName.setError(getString(R.string.err_msg_invalid_str));
                return;
            }
            String type = (String) spinMbusType.getSelectedItem();
            boolean enable = cboxEnable.isChecked();
            MstDev mstDev = new MstDev(name, type, enable);
            if(type.equals("TCP")) {
                String ip = txtIp.getText().toString().trim();
                if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, ip)) {
                    txtIp.setError(getString(R.string.err_msg_empty));
                    return;
                } else if(!StrUtils.validateInput(StrUtils.IN_TYPE_IPV4, ip)) {
                    txtIp.setError(getString(R.string.err_msg_format));
                    return;
                }
                String port = txtPort.getText().toString().trim();
                if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, port)) {
                    port = "502"; // default port
                } else {
                    if(!StrUtils.validateInput(StrUtils.IN_TYPE_IP_PORT, port)) {
                        txtPort.setError(getString(R.string.err_msg_format));
                        return;
                    }
                }
                int slvId = 255;
                String _slvId = (String) spinTCPSlvId.getSelectedItem();
                if(!_slvId.equals(getString(R.string.none))) {
                    slvId = Integer.parseInt(_slvId);
                }
                mstDev.setTCP(ip, Integer.parseInt(port), slvId);
            } else {
                String comPort = (String) spinComPort.getSelectedItem();
                String slvId = (String) spinSlvId.getSelectedItem();
                mstDev.setSerial(comPort, Integer.parseInt(slvId));
            }
            mDialog = Kdialog.getProgress(context, mDialog);
            bgTimer = new Timer();
            bgTimer.schedule(new BgTimerTask("SAVE", mstDev), 0, PrjCfg.RUN_ONCE);
        }
    };

    private class BgTimerTask extends TimerTask {
        public String cmd;
        public MstDev mstDev;

        public BgTimerTask(String cmd, MstDev device) {
            this.cmd = cmd;
            this.mstDev = device;
        }

        public void run() {
            try {
                if (cmd.equals("GET")) {
                    getDeviceInfo();
                } else if (cmd.equals("SAVE")) {
                    saveMstDev(mstDev);
                }
                if (Utils.loadPrefs(context, "Password", "").equals("")) {
                    MHandler.exec(mHandler, MHandler.BEEN_LOGOUT);
                    return;
                } else if(MainApp.SERV_MAINT > (new Date().getTime() / 1000)) {
                    MHandler.exec(mHandler, MHandler.SERV_MAINT);
                    return;
                }
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

    private void getDeviceInfo() throws Exception {
        JSONObject jDevObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/slvdev/" + curDevice.sn);
        if (jDevObject == null || jDevObject.getInt("code") != 200) {
            MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.err_device_list));
            return;
        }
        MHandler.exec(mHandler, MHandler.UPDATE, new Device(jDevObject.getJSONObject("device")));
    }

    private void saveMstDev(MstDev mstDev) throws Exception {
        JSONObject jObject = JSONReq.multipart(context, "POST", PrjCfg.CLOUD_URL + "/api/slvdev/update", mstDev.toMultiPart(curDevice));
        //Log.e(Dbg._TAG_(), jObject.toString());
        int statCode = jObject.getInt("code");
        if(statCode == 500) {
            MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, context.getString(R.string.err_msg_server_error));
        } else if(statCode == 400) {
            if(!jObject.isNull("desc") && jObject.getString("desc").contains("Device is logging")) {
                MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.usb_logging));
            } else if(!jObject.isNull("desc") && jObject.getString("desc").trim().matches("Invalid Data")) {
                MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, context.getString(R.string.err_msg_invalid_str));
            } else if(!jObject.isNull("desc") && jObject.getString("desc").trim().matches("maximum number")) {
                MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, context.getString(R.string.err_msg_max_device_exceeded));
            } else if(!jObject.isNull("desc") && jObject.getString("desc").trim().matches("The slave setting has been used")) {
                MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, context.getString(R.string.err_msg_dup_slvid));
            } else {
                MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, context.getString(R.string.err_save));
            }
        } else if (statCode != 200) {
            MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, context.getString(R.string.err_save));
        } else {
            MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_save));
            finish();
        }
    }

    private static class MsgHandler extends MHandler {
        public MsgHandler(NewSlaveDevActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final NewSlaveDevActivity activity = (NewSlaveDevActivity) super.mActivity.get();
            switch (msg.what) {
                case MHandler.UPDATE: {
                    break;
                }
                default: {
                    super.handleMessage(msg);
                }
            }
        }
    }
}