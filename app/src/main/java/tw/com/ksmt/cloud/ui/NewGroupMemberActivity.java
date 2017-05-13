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
import android.text.InputFilter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.avos.avoscloud.okhttp.RequestBody;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.MainApp;
import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.AdvGP;
import tw.com.ksmt.cloud.iface.Device;
import tw.com.ksmt.cloud.iface.Group;
import tw.com.ksmt.cloud.iface.GroupMember;
import tw.com.ksmt.cloud.iface.MstDev;
import tw.com.ksmt.cloud.iface.Register;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.StrUtils;
import tw.com.ksmt.cloud.libs.Utils;

public class NewGroupMemberActivity extends ActionBarActivity {
    private Context context = NewGroupMemberActivity.this;
    private ActionBar actionBar;
    private ProgressDialog mDialog;
    private Timer saveTimer;
    private Timer devInfoTimer;
    private MsgHandler mHandler;
    private EditText editGroupName;
    private EditText editSGroupName;
    private Spinner spinDevice;
    private Spinner spinDeviceReg;
    private TextView txtDevice;
    private TextView txtRegister;
    private CheckBox chBoxAdvGp;
    private Button btnSave;
    private Button btnSaveAndNext;
    private Group group;
    private AdvGP curAdvGP;
    private int isAdvGP = 0;
    private boolean addMember = true;
    private ArrayAdapter<Device> deviceAdapter;
    private ArrayAdapter<Register> registerAdapter;
    private HashMap<String, ArrayAdapter<Register>> registerMap;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        actionBar.setIcon(R.drawable.ic_new_group_1);
        actionBar.setDisplayShowHomeEnabled(true);

        AppSettings.setupLang(context);
        setContentView(R.layout.group_member);
        editGroupName = (EditText) findViewById(R.id.editName);
        editGroupName.setFilters(Utils.arrayMerge(editGroupName.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
        editSGroupName = (EditText) findViewById(R.id.editSname);
        editSGroupName.setFilters(Utils.arrayMerge(editSGroupName.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
        spinDevice = (Spinner) findViewById(R.id.spinDeviceSn);
        spinDeviceReg = (Spinner) findViewById(R.id.spinDeviceAddr);
        txtDevice = (TextView) findViewById(R.id.txtDevice);
        txtRegister = (TextView) findViewById(R.id.txtRegister);
        chBoxAdvGp = (CheckBox) findViewById(R.id.ckboxAdvGp);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnSave.setOnClickListener(btnSaveListener);
        btnSaveAndNext = (Button) findViewById(R.id.saveAndNext);
        btnSaveAndNext.setOnClickListener(btnSaveAndNextListener);
        mHandler = new MsgHandler(this);

        Intent intent = getIntent();
        isAdvGP = intent.getIntExtra("isAdvGP", 0);
//        Log.e(Dbg._TAG_(), "isAdvGP = " + isAdvGP);
        if(isAdvGP == 0) {
            group = (Group) intent.getSerializableExtra("Group");
            if(group == null) {
                setTitle("  " + getString(R.string.new_group));
            } else {
                setTitle("  " + group.name);
                editGroupName.setText(group.name);
                editGroupName.setEnabled(false);
                editGroupName.setVisibility(View.GONE);
            }
        } else if(isAdvGP == 1) { // new gruop
            setTitle("  " + getString(R.string.new_group));
            chBoxAdvGp.setVisibility(View.VISIBLE);
            chBoxAdvGp.setOnClickListener(chkBoxListener);
        } else if(isAdvGP == 2) { // new sub group
            curAdvGP = (AdvGP) intent.getSerializableExtra("AdvGP");
            setTitle("  " + getString(R.string.new_group));
            actionBar.setSubtitle(Html.fromHtml("<small><small>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + curAdvGP.name + "</small></small>"));
        } else if(isAdvGP == 3) { // add group member
            curAdvGP = (AdvGP) intent.getSerializableExtra("AdvGP");
            setTitle("  " + getString(R.string.new_group_member));
            actionBar.setSubtitle(Html.fromHtml("<small><small>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + curAdvGP.name + "</small></small>"));
            editGroupName.setVisibility(View.GONE);
        }

        // Get Device/Register list
        mDialog = Kdialog.getProgress(context, mDialog);
        devInfoTimer = new Timer();
        devInfoTimer.schedule(new DevInfoTimer(), 0, PrjCfg.RUN_ONCE); // timer delay start
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
        if (saveTimer != null) {
            saveTimer.cancel();
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

    View.OnClickListener chkBoxListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            boolean isChecked = chBoxAdvGp.isChecked();
            if(isChecked) {
                addMember = false;
                editSGroupName.setVisibility(View.VISIBLE);
                spinDeviceReg.setVisibility(View.GONE);
                spinDevice.setVisibility(View.GONE);
                txtDevice.setVisibility(View.GONE);
                txtRegister.setVisibility(View.GONE);
                btnSaveAndNext.setVisibility(View.GONE);
            } else {
                addMember = true;
                editSGroupName.setVisibility(View.GONE);
                spinDeviceReg.setVisibility(View.VISIBLE);
                spinDevice.setVisibility(View.VISIBLE);
                txtDevice.setVisibility(View.VISIBLE);
                txtRegister.setVisibility(View.VISIBLE);
                btnSaveAndNext.setVisibility(View.VISIBLE);
            }
        }
    };

    View.OnClickListener btnSaveListener = new Button.OnClickListener() {
        public void onClick(View v) {
            onSaveBtnClick(false);
        }
    };

    View.OnClickListener btnSaveAndNextListener = new View.OnClickListener() {
        public void onClick(View v) {
            onSaveBtnClick(true);
        }
    };

    private void onSaveBtnClick(boolean addMore) {
        String name = "";
        if(isAdvGP == 0 || isAdvGP == 2 || isAdvGP == 1) {
            name = editGroupName.getText().toString();
            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, name)) {
                editGroupName.setError(getString(R.string.err_msg_empty));
                return;
            } else if (!StrUtils.validateInput(StrUtils.IN_TYPE_STRING, name)) {
                editGroupName.setError(getString(R.string.err_msg_invalid_str));
                return;
            }
        }

        String sname = null; // subgroup name
        if(isAdvGP == 1 && !addMember) {
            sname = editSGroupName.getText().toString();
            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, sname)) {
                editSGroupName.setError(getString(R.string.err_msg_empty));
                return;
            } else if (!StrUtils.validateInput(StrUtils.IN_TYPE_STRING, sname)) {
                editSGroupName.setError(getString(R.string.err_msg_invalid_str));
                return;
            }
        }

        // spin device
        Device device = (Device) spinDevice.getSelectedItem();
        Register register = (Register) spinDeviceReg.getSelectedItem();
        if (addMember && (device == null || register == null)) {
            return;
        }

        mDialog = Kdialog.getProgress(context, mDialog);
        saveTimer = new Timer();
        if(isAdvGP == 0) { // old group
            saveTimer.schedule(new SaveTimerTask(new GroupMember(name, device, register), addMore), 0, PrjCfg.RUN_ONCE);
        } else { // new group
            AdvGP newAdvGp = (addMember) ?  new AdvGP(name, device, register) : new AdvGP(name, sname);
            if(isAdvGP == 2) { // new group, new sub group
                newAdvGp.parentId = curAdvGP.id;
            } else if(isAdvGP == 3) { // new group member
                newAdvGp.id = curAdvGP.id;
                newAdvGp.parentId = curAdvGP.parentId;
            }
            saveTimer.schedule(new SaveTimerTask(newAdvGp, addMore), 0, PrjCfg.RUN_ONCE);
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

    protected class SaveTimerTask extends TimerTask {
        private GroupMember groupMember;
        private AdvGP advGP;
        private boolean addMore;

        public SaveTimerTask(GroupMember groupMember, boolean addMore) {
            this.groupMember = groupMember;
            this.addMore = addMore;
        }

        public SaveTimerTask(AdvGP advGP, boolean addMore) {
            this.advGP = advGP;
            this.addMore = addMore;
        }

        public void run() {
            try {
                String action = (groupMember == null) ? "/api/advgp/add" : "/api/group/add" ;
                RequestBody reqBody = (groupMember == null) ? advGP.toMultiPart() : groupMember.toMultiPart();
                JSONObject jObject = JSONReq.multipart(context, "POST", PrjCfg.CLOUD_URL + action, reqBody);
                //Log.e(Dbg._TAG_(), jObject.toString());
                int code = jObject.getInt("code");
                if (code == 500) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_server_error));
                    return;
                } else if(code == 201) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_group_existed));
                    return;
                } else if(code == 404) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_wrong_data));
                    return;
                } else if(code == 400) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_max_group_exceeded));
                    return;
                } else if(code != 200) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, context.getString(R.string.err_save));
                    return;
                } else {
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_save));
                    if(addMore && groupMember == null) {
                        advGP.id = jObject.has("id") ? jObject.getInt("id") : 0;
                        MHandler.exec(mHandler, MHandler.NEXT_ONE, advGP);
                    } else if(addMore) {
                        MHandler.exec(mHandler, MHandler.NEXT_ONE, groupMember);
                    } else {
                        MHandler.exec(mHandler, MHandler.GO_BACK);
                    }
                }
            } catch (Exception e) {
                if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
                }
                e.printStackTrace();
            } finally {
                mDialog.dismiss();
                saveTimer.cancel();
            }
        }
    }

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
                    MHandler.exec(mHandler, MHandler.ADD_REGISTER_LIST, (new Register(device.slvIdx, jRegister)));
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
        public MsgHandler(NewGroupMemberActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            NewGroupMemberActivity activity = (NewGroupMemberActivity) super.mActivity.get();
            switch (msg.what) {
                case MHandler.NEXT_ONE: {
                    if(activity.isAdvGP == 0) {
                        GroupMember groupMember = (GroupMember) msg.obj;
                        activity.setTitle("  " + groupMember.name);
                        activity.editGroupName.setEnabled(false);
                        activity.editGroupName.setVisibility(View.GONE);
                        activity.spinDeviceReg.setSelection(0);
                    } else if(activity.isAdvGP != 3){
                        AdvGP advGP = (AdvGP) msg.obj;
                        activity.curAdvGP = advGP;
                        activity.isAdvGP = 3;
                        activity.setTitle("  " + activity.getString(R.string.new_group_member));
                        activity.actionBar.setSubtitle(Html.fromHtml("<small><small>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + advGP.name + "</small></small>"));
                        activity.chBoxAdvGp.setVisibility(View.GONE);
                        activity.editGroupName.setVisibility(View.GONE);
                        activity.spinDeviceReg.setSelection(0);
                    }
                    break;
                } case MHandler.ADD_DEVICE_LIST: {
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
