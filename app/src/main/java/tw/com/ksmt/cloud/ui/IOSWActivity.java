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
import android.text.Html;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.MainApp;
import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Device;
import tw.com.ksmt.cloud.iface.MstDev;
import tw.com.ksmt.cloud.iface.Register;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.Utils;

public class IOSWActivity extends ActionBarActivity {
    private Context context = IOSWActivity.this;
    private ActionBar actionBar;
    private ProgressDialog mDialog;
    private Timer devInfoTimer;
    private MsgHandler mHandler;
    private Spinner spinDevice;
    private Spinner spinDeviceReg;
    private Button btnSave;
    private ArrayAdapter<Device> deviceAdapter;
    private ArrayAdapter<Register> registerAdapter;
    private HashMap<String, ArrayAdapter<Register>> registerMap;
    protected Register curRegister;
    private short numBits = 16;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("  " + getString(R.string.iosw_source));
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        actionBar.setIcon(R.drawable.ic_iosw_1);
        actionBar.setDisplayShowHomeEnabled(true);

        AppSettings.setupLang(context);
        setContentView(R.layout.io_switch);
        spinDevice = (Spinner) findViewById(R.id.spinDeviceSn);
        spinDeviceReg = (Spinner) findViewById(R.id.spinDeviceAddr);

        Intent intent = getIntent();
        if(intent != null) {
            curRegister = (Register) intent.getSerializableExtra("Register");
            if(curRegister != null) {
                actionBar.setSubtitle(Html.fromHtml("<small><small>&nbsp;&nbsp;&nbsp;&nbsp;" + getString(R.string.register) + ": " + curRegister.haddr + "</small></small>"));
                if(Register.is64Bits(curRegister.type)) {
                    numBits = 64;
                } else if(Register.is48Bits(curRegister.type)) {
                    numBits = 48;
                } else if(Register.is32Bits(curRegister.type)) {
                    numBits = 32;
                } else {
                    numBits = 16;
                }
            } else {
                boolean is32Bits = intent.getBooleanExtra("is32Bits", false);
                boolean is48Bits = intent.getBooleanExtra("is48Bits", false);
                boolean is64Bits = intent.getBooleanExtra("is64Bits", false);
                if(is64Bits) {
                    numBits = 64;
                } else if(is48Bits) {
                    numBits = 48;
                } else if(is32Bits) {
                    numBits = 32;
                } else {
                    numBits = 16;
                }
            }
        }
        mHandler = new MsgHandler(this);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnSave.setOnClickListener(btnSaveListener);

        mDialog = Kdialog.getProgress(context, mDialog);
        devInfoTimer = new Timer();
        devInfoTimer.schedule(new DevInfoTimer(), 0, PrjCfg.RUN_ONCE);
        deviceAdapter = new ArrayAdapter<Device>(context, R.layout.spinner);
        registerMap = new HashMap<String, ArrayAdapter<Register>>();
        spinDevice.setOnItemSelectedListener(spinDevListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mDialog != null) {
            mDialog.dismiss();
        }
        if (devInfoTimer != null) {
            devInfoTimer.cancel();
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

    View.OnClickListener btnSaveListener = new Button.OnClickListener() {
        public void onClick(View v) {
            onSaveBtnClick(false);
        }
    };

    private void onSaveBtnClick(boolean addMore) {
        Device device = (Device) spinDevice.getSelectedItem();
        Register register = (Register) spinDeviceReg.getSelectedItem();
        if (device != null && register != null) {
            Intent intent = new Intent();
            intent.putExtra("ioswSrcDev", device);
            intent.putExtra("ioswSrcReg", register);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    AdapterView.OnItemSelectedListener spinDevListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
            Device device = deviceAdapter.getItem(position);
            //Toast.makeText(context, device.sn, Toast.LENGTH_SHORT).show();
            if (registerMap.containsKey(device.sn + "-" + device.slvIdx)) {
                MHandler.exec(mHandler, MHandler.UPDATE_FROM_CACHE, device.sn + "-" + device.slvIdx);
            } else {
                registerAdapter = new ArrayAdapter<Register>(context, R.layout.spinner);
                mDialog = Kdialog.getProgress(context, mDialog);
                devInfoTimer = new Timer();
                devInfoTimer.schedule(new DevRegInfoTimer(device), 0, PrjCfg.RUN_ONCE);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
        }
    };

    private class DevInfoTimer extends TimerTask {
        public void run() {
            try {
                JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/device");
                if (Utils.loadPrefs(context, "Password", "").equals("")) {
                    MHandler.exec(mHandler, MHandler.BEEN_LOGOUT);
                    return;
                } else if(MainApp.SERV_MAINT > (new Date().getTime() / 1000)) {
                    MHandler.exec(mHandler, MHandler.SERV_MAINT);
                    return;
                }
                if (jObject == null || jObject.getInt("code") != 200) {
                    MHandler.exec(mHandler, MHandler.GO_BACK, getString(R.string.err_msg_no_devices));
                    return;
                }
                JSONArray jsonArray = jObject.getJSONArray("devices");
                for(int i = 0; i < jsonArray.length(); i++) {
                    JSONObject devObject = (JSONObject) jsonArray.get(i);
                    Device device = new Device(devObject);
                    if(device.isMbusMaster() && devObject.has("mstConf")) {
                        JSONObject mstConfs = devObject.getJSONObject("mstConf");
                        int[] ids = Utils.getJSortedKeys(mstConfs);
                        for(int j = 0; j < ids.length; j++) {
                            Device _device = (Device) device.clone();
                            _device.slvIdx = ids[j];
                            _device.slvDev = new MstDev(ids[j], mstConfs.getJSONObject(String.valueOf(ids[j])));
                            MHandler.exec(mHandler, MHandler.ADD_DEVICE_LIST, _device);
                        }
                    } else {
                        MHandler.exec(mHandler, MHandler.ADD_DEVICE_LIST, device);
                    }
                }
                MHandler.exec(mHandler, MHandler.UPDATE_DEVICE_SN);
            } catch (Exception e) {
                e.printStackTrace();
                MHandler.exec(mHandler, MHandler.GO_BACK, getString(R.string.err_device_list));
            } finally {
                mDialog.dismiss();
                devInfoTimer.cancel();
            }
        }
    }

    private class DevRegInfoTimer extends TimerTask {
        private Device device;
        private boolean isSlvDev;

        public DevRegInfoTimer(Device device) {
            this.device = device;
            this.isSlvDev = (device.isMbusMaster() && device.slvIdx > 0);
        }

        public void run() {
            try {
                String params = (isSlvDev) ? ("?slvIdx=" + device.slvIdx) : "" ;
                JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/device/" + device.sn + params);
                if (Utils.loadPrefs(context, "Password", "").equals("")) {
                    MHandler.exec(mHandler, MHandler.BEEN_LOGOUT);
                    return;
                } else if(MainApp.SERV_MAINT > (new Date().getTime() / 1000)) {
                    MHandler.exec(mHandler, MHandler.SERV_MAINT);
                    return;
                }
                int statCode = jObject.getInt("code");
                if(statCode == 404) {
                    return;
                } else if (statCode != 200) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_get_data));
                    return;
                }
                JSONArray jModbus = (isSlvDev) ? jObject.getJSONArray("modbus") : jObject.getJSONObject("device").getJSONArray("modbus");
                for(int i = 0; i < jModbus.length(); i++) {
                    JSONObject jRegister = (JSONObject) jModbus.get(i);
                    int type = jRegister.getInt("type");
                    if(Register.isIOSW(type)) {
                        continue;
                    }
                    if(numBits == 64 && Register.is64Bits(type)) { // Only 64 bits
                        MHandler.exec(mHandler, MHandler.ADD_REGISTER_LIST, (new Register(device.slvIdx, jRegister)));
                    } else if(numBits == 48 && Register.is48Bits(type)) { // Only 64 bits
                        MHandler.exec(mHandler, MHandler.ADD_REGISTER_LIST, (new Register(device.slvIdx, jRegister)));
                    } else if(numBits == 32 && Register.is32Bits(type)) { // Only 32 bits
                        MHandler.exec(mHandler, MHandler.ADD_REGISTER_LIST, (new Register(device.slvIdx, jRegister)));
                    } else if(numBits == 16 && Register.is16Bits(type)) { // Only 16 bits
                        MHandler.exec(mHandler, MHandler.ADD_REGISTER_LIST, (new Register(device.slvIdx, jRegister)));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace(); // usually, no any modbus profiles
            } catch (Exception e) {
                e.printStackTrace();
                MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_get_data));
            } finally {
                MHandler.exec(mHandler, MHandler.UPDATE_DEVICE_ADDR, device);
                mDialog.dismiss();
                devInfoTimer.cancel();
            }
        }
    }

    private static class MsgHandler extends MHandler {
        public MsgHandler(IOSWActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            IOSWActivity activity = (IOSWActivity) super.mActivity.get();
            switch (msg.what) {
                case MHandler.ADD_DEVICE_LIST: {
                    activity.deviceAdapter.add((Device) msg.obj);
                    break;
                } case MHandler.ADD_REGISTER_LIST: {
                    activity.registerAdapter.add((Register) msg.obj);
                    break;
                } case MHandler.UPDATE_DEVICE_SN: {
                    activity.deviceAdapter.setDropDownViewResource(R.layout.spinner_item);
                    activity.spinDevice.setAdapter(activity.deviceAdapter);
                    break;
                } case MHandler.UPDATE_DEVICE_ADDR: {
                    Device device = (Device) msg.obj;
                    activity.registerMap.put(device.sn + "-" + device.slvIdx , activity.registerAdapter);
                    activity.registerAdapter.setDropDownViewResource(R.layout.spinner_item);
                    activity.spinDeviceReg.setAdapter(activity.registerAdapter);
                    break;
                } case MHandler.UPDATE_FROM_CACHE: {
                    activity.spinDeviceReg.setAdapter(activity.registerMap.get(msg.obj));
                    break;
                } default: {
                    super.handleMessage(msg);
                }
            }
        }
    }
}
