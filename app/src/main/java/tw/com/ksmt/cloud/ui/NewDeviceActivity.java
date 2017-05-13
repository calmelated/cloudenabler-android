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
import android.text.InputFilter;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.MainApp;
import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Device;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.StrUtils;
import tw.com.ksmt.cloud.libs.Utils;

public class NewDeviceActivity extends ActionBarActivity {
    private Context context = NewDeviceActivity.this;
    private ActionBar actionBar;
    private ProgressDialog mDialog;
    private Timer bgTimer;
    private MsgHandler mHandler;

    private Spinner  spinDevTypes;
    private TextView txtDevTypes;
    private EditText txtDevSn;
    private EditText txtDevName;
    private EditText txtPolling;
    private CheckBox cboxEnlog;
    private Button btnConfirm;

    private ArrayAdapter<String> devTypeAdapter;
    private boolean isEdit;
    private Device curDevice;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AppSettings.reload(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("  " + getString(R.string.new_device));
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        actionBar.setIcon(R.drawable.ic_new_device_1);
        actionBar.setDisplayShowHomeEnabled(true);

        AppSettings.setupLang(context);
        setContentView(R.layout.device_manage);
        mHandler = new MsgHandler(this);

        txtDevSn    = (EditText) findViewById(R.id.editDevSn);
        txtDevName  = (EditText) findViewById(R.id.editDevName);
        txtPolling  = (EditText) findViewById(R.id.editDevPolling);
        cboxEnlog   = (CheckBox) findViewById(R.id.ckboxEnlog);
        btnConfirm  = (Button) findViewById(R.id.btnDevConfirm);
        txtDevSn.setFilters(Utils.arrayMerge(txtDevSn.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
        txtDevName.setFilters(Utils.arrayMerge(txtDevName.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
        btnConfirm.setOnClickListener(btnDeviceListener);

        txtDevTypes = (TextView) findViewById(R.id.txtDevType);
        spinDevTypes = (Spinner) findViewById(R.id.spinDevType);
        devTypeAdapter = new ArrayAdapter<String>(context, R.layout.spinner);
        devTypeAdapter.addAll(Device.getAllTypes(context));
        devTypeAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinDevTypes.setAdapter(devTypeAdapter);

        Intent intent = getIntent();
        curDevice = (Device) intent.getSerializableExtra("Device");
        if(curDevice != null) {
            MHandler.exec(mHandler, MHandler.UPDATE, curDevice);
        }

        // debug mode
        if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_KSMT_TEST_ALL)){
            spinDevTypes.setEnabled(true);
        } else { // admin or user
            spinDevTypes.setEnabled(false);
            txtPolling.setVisibility(View.GONE);
            txtDevSn.setVisibility(View.GONE);

            // For Fullkey
            if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_FULLKEY)) {
                spinDevTypes.setVisibility(View.GONE);
                txtDevTypes.setVisibility(View.GONE);
            }
        }
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

    View.OnClickListener btnDeviceListener = new Button.OnClickListener() {
        public void onClick(View v) {
            String sn = txtDevSn.getText().toString().trim();
            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, sn)) {
                txtDevSn.setError(getString(R.string.err_msg_empty));
                return;
            } else if (!StrUtils.validateInput(StrUtils.IN_TYPE_MAC, sn)) {
                txtDevSn.setError(getString(R.string.invalid_val));
                return;
            }

            String name = txtDevName.getText().toString().trim();
            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, name)) {
                txtDevName.setError(getString(R.string.err_msg_empty));
                return;
            } else if (!StrUtils.validateInput(StrUtils.IN_TYPE_STRING, name)) {
                txtDevName.setError(getString(R.string.err_msg_invalid_str));
                return;
            }

            int pollingTime = PrjCfg.DEV_POLL_TIME;
            if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_KSMT_TEST_ALL)){
                String tmp = txtPolling.getText().toString().trim();
                if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, tmp)) {
                    txtPolling.setError(getString(R.string.err_msg_empty));
                    return;
                }
                pollingTime = Integer.parseInt(tmp);
            }

            String model = Device.getTypeIdx(context, (String) spinDevTypes.getSelectedItem());
            boolean enlog = cboxEnlog.isChecked();
            mDialog = Kdialog.getProgress(context, mDialog);
            bgTimer = new Timer();
            bgTimer.schedule(new BgTimerTask("SAVE", new Device(sn, name, model, pollingTime, enlog)), 0, PrjCfg.RUN_ONCE);
        }
    };

    private class BgTimerTask extends TimerTask {
        public String cmd;
        public Device device;

        public BgTimerTask(String cmd, Device device) {
            this.cmd = cmd;
            this.device = device;
        }

        public void run() {
            try {
                if (cmd.equals("GET")) {
                    getDeviceInfo(device);
                } else if (cmd.equals("SAVE")) {
                    saveDevice(device);
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

    private void getDeviceInfo(Device device) throws Exception {
        JSONObject jDevObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/device/" + device.sn);
        if (jDevObject == null || jDevObject.getInt("code") != 200) {
            MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.err_device_list));
            return;
        }
        MHandler.exec(mHandler, MHandler.UPDATE, new Device(jDevObject.getJSONObject("device")));
    }

    private void saveDevice(Device device) throws Exception {
        JSONObject jObject = JSONReq.multipart(context, "POST", PrjCfg.CLOUD_URL + "/api/device/add", device.toMultiPart());
        //Log.e(Dbg._TAG_(), jObject.toString());

        Intent intent = new Intent();
        intent.putExtra("isRegDevice", false);
        int statCode = jObject.getInt("code");
        //Log.e(Dbg._TAG_(), jObject.toString());
        if(statCode == 500) {
            MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, context.getString(R.string.err_msg_server_error));
        } else if(statCode == 400) {
            if(!jObject.isNull("desc") && jObject.getString("desc").contains("Invalid Data")) {
                String errStr =  jObject.toString();
//                Log.e(Dbg._TAG_(), "errStr = " + errStr);
                if(errStr.contains("unacceptable model")) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, context.getString(R.string.err_msg_unacceptable_model));
                } else {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, context.getString(R.string.err_msg_invalid_str));
                }
            } else if(!jObject.isNull("desc") && jObject.getString("desc").trim().matches("maximum number")) {
                MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, context.getString(R.string.err_msg_max_device_exceeded));
            } else {
                MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, context.getString(R.string.err_msg_dup_device));
            }
        } else if (statCode != 200) {
            MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, context.getString(R.string.err_save));
        } else {
            MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_save));
            intent.putExtra("isRegDevice", true);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private static class MsgHandler extends MHandler {
        public MsgHandler(NewDeviceActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final NewDeviceActivity activity = (NewDeviceActivity) super.mActivity.get();
            switch (msg.what) {
                case MHandler.UPDATE: {
                    Device device = (Device) msg.obj;
                    activity.spinDevTypes.setSelection(activity.devTypeAdapter.getPosition(Device.getTypeName(activity, device.model)));
                    if(device.sn != null && !device.sn.equals("")) {
                        activity.txtDevSn.setText(device.sn);
                        activity.txtDevSn.setEnabled(false);
                    }
                    if(activity.isEdit) {
                        activity.txtDevName.setText(device.name);
                    }
                    activity.cboxEnlog.setChecked(device.enlog);
                    if(device.pollTime > 0) {
                        activity.txtPolling.setText(Integer.toString(device.pollTime));
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