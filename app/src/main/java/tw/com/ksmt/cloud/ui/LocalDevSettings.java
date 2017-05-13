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
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Device;
import tw.com.ksmt.cloud.iface.TCPFrame;
import tw.com.ksmt.cloud.libs.Dbg;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.StrUtils;
import tw.com.ksmt.cloud.libs.Utils;
import tw.com.ksmt.cloud.libs.WebUtils;

public class LocalDevSettings extends PreferenceActivity {
    private final Context context = LocalDevSettings.this;
    private static final int DEVICE_REGISTER = 1;
    private boolean stopResume = false;
    private Timer getDevInfoTimer;
    private Timer getDevTimeTimer;
    private Timer getCntStatusTimer;
    private Timer setDevTimer;
    private Timer getFwVerTimer;
    private Timer rebootTimer;
    private Timer ntpTimer;
    private Timer uartTimer;

    private ProgressDialog mDialog;
    private MsgHandler mHandler;
    private DevSetting devSetting;
    private DevSetting origSetting;
    private Device curDev;
    private byte[] respApCfg;
    private byte[] ntpCfg;
    private byte[] uartCfg;

    private PreferenceCategory mCateCloud;
    private PreferenceCategory mCateDevSettings;
    private CheckBoxPreference enStaticIp;
    private CheckBoxPreference enCloud;
    private CheckBoxPreference enHTTPS;
    private EditTextPreference devName;
    private EditTextPreference cloudUrl;
    private EditTextPreference cloudPt;
    private Preference devReg;
    private Preference devPswd;
    private Preference devFwVer;
    private Preference cfgStaticIp;

    private ListPreference tZone;
    private CheckBoxPreference enNTPService;
    private Preference sioSyncTime;
    private Preference ceSyncTime;
    private EditTextPreference ntpServer1;
    private EditTextPreference ntpServer2;

    private CheckBoxPreference enTcpSlave;
    private Preference slvTcpPort;

    private ListPreference mbOpMode;
    private Preference mbUid;
    private ListPreference mbIface;
    private ListPreference mbBRate;
    private ListPreference mbDataLen;
    private ListPreference mbParity;
    private ListPreference mbStopBit;

    private ListPreference mbOpMode1;
    private Preference mbUid1;
    private ListPreference mbIface1;
    private ListPreference mbBRate1;
    private ListPreference mbDataLen1;
    private ListPreference mbParity1;
    private ListPreference mbStopBit1;

    @Override
    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        addPreferencesFromResource(R.xml.local_dev_settings);
        getListView().setBackgroundColor(Color.WHITE);
        devSetting = new DevSetting();
        mHandler = new MsgHandler(this);
        curDev = (Device) getIntent().getSerializableExtra("Device");
        Log.e(Dbg._TAG_(), "Connect to device " + curDev.ip + ", model " + curDev.model);

        enCloud = (CheckBoxPreference)findPreference("prefEnCloud");
        enHTTPS = (CheckBoxPreference)findPreference("prefEnHTTPS");
        cloudUrl = (EditTextPreference) findPreference("prefCloudUrl");
        cloudPt = (EditTextPreference) findPreference("prefCloudPt");

        devName = (EditTextPreference) findPreference("prefDevName");
        devPswd = (Preference) findPreference("prefDevPswd");
        devReg = (Preference) findPreference("prefDevReg");
        devFwVer = findPreference("prefDevFWVer");
        enStaticIp = (CheckBoxPreference)findPreference("prefStaticNetwork");
        cfgStaticIp = findPreference("prefStaticNetworkDialog");

        sioSyncTime = findPreference("prefSIOSyncDevTime");
        ceSyncTime = findPreference("prefCESyncDevTime");
        enNTPService = (CheckBoxPreference)findPreference("prefEnNTPService");
        ntpServer1 = (EditTextPreference) findPreference("prefNTPServer1");
        ntpServer2 = (EditTextPreference) findPreference("prefNTPServer2");
        tZone = (ListPreference)findPreference("prefTZone");
        enTcpSlave = (CheckBoxPreference)findPreference("prefEnTcpSlave");
        slvTcpPort = (Preference) findPreference("prefSlavTcpPort");

        mbOpMode = (ListPreference)findPreference("prefModbusOpMode");
        mbUid = (Preference)findPreference("prefModbusUid");
        mbIface = (ListPreference)findPreference("prefModbusIface");
        mbBRate = (ListPreference)findPreference("prefModbusBaudRate");
        mbDataLen = (ListPreference)findPreference("prefModbusDataLen");
        mbParity = (ListPreference)findPreference("prefModbusPriority");
        mbStopBit = (ListPreference)findPreference("prefModbusStopBit");

        mbOpMode1 = (ListPreference)findPreference("prefModbusOpMode1");
        mbUid1 = (Preference)findPreference("prefModbusUid1");
        mbIface1 = (ListPreference)findPreference("prefModbusIface1");
        mbBRate1 = (ListPreference)findPreference("prefModbusBaudRate1");
        mbDataLen1 = (ListPreference)findPreference("prefModbusDataLen1");
        mbParity1 = (ListPreference)findPreference("prefModbusPriority1");
        mbStopBit1 = (ListPreference)findPreference("prefModbusStopBit1");

        enTcpSlave.setOnPreferenceChangeListener(enTcpSlaveListener);
        slvTcpPort.setOnPreferenceClickListener(slvTcpPortListener);

        mbOpMode.setOnPreferenceChangeListener(mbOpModeListener);
        mbUid.setOnPreferenceClickListener(mbUidListener);
        mbIface.setOnPreferenceChangeListener(mbIfaceListener);
        mbBRate.setOnPreferenceChangeListener(mbBRateListener);
        mbDataLen.setOnPreferenceChangeListener(mbDataLenListener);
        mbParity.setOnPreferenceChangeListener(mbParityListener);
        mbStopBit.setOnPreferenceChangeListener(mbStopBitListener);

        mbOpMode1.setOnPreferenceChangeListener(mbOpMode1Listener);
        mbUid1.setOnPreferenceClickListener(mbUid1Listener);
        mbIface1.setOnPreferenceChangeListener(mbIface1Listener);
        mbBRate1.setOnPreferenceChangeListener(mbBRate1Listener);
        mbDataLen1.setOnPreferenceChangeListener(mbDataLen1Listener);
        mbParity1.setOnPreferenceChangeListener(mbParity1Listener);
        mbStopBit1.setOnPreferenceChangeListener(mbStopBit1Listener);

        sioSyncTime.setOnPreferenceClickListener(syncTimeListener);
        ceSyncTime.setOnPreferenceClickListener(syncTimeListener);
        enNTPService.setOnPreferenceChangeListener(enNTPListener);
        ntpServer1.setOnPreferenceChangeListener(ntpServ1Listener);
        ntpServer2.setOnPreferenceChangeListener(ntpServ2Listener);
        tZone.setOnPreferenceChangeListener(tzoneListener);

        enStaticIp.setOnPreferenceClickListener(enStaticIpClickListener);
        cfgStaticIp.setOnPreferenceClickListener(cfgStaticIpListener);
        enCloud.setOnPreferenceChangeListener(enCloudListener);
        enHTTPS.setOnPreferenceChangeListener(enHTTPSListener);
        cloudUrl.setOnPreferenceChangeListener(cloudUrlListener);
        cloudPt.setOnPreferenceChangeListener(cloudPtListener);
        devName.setOnPreferenceChangeListener(devNameListener);
        devPswd.setOnPreferenceClickListener(devPswdListener);
        devReg.setOnPreferenceClickListener(devRegListener);

        devName.getEditText().setFilters(new InputFilter[]{new InputFilter.LengthFilter(31), Utils.EMOJI_FILTER});
        cloudUrl.getEditText().setFilters(new InputFilter[]{new InputFilter.LengthFilter(256), Utils.EMOJI_FILTER});
        ntpServer1.getEditText().setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), Utils.EMOJI_FILTER});
        ntpServer2.getEditText().setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), Utils.EMOJI_FILTER});
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        AppSettings.reload(context);
        LinearLayout root = (LinearLayout)findViewById(android.R.id.list).getParent().getParent().getParent();
        Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.preference_toolbar, root, false);
        bar.setTitle(getString(R.string.local_dev_settings));
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        root.addView(bar, 0); // insert at top
    }

    @Override
    public void onResume() {
        super.onResume();
        if(stopResume) {
            return;
        }
        mDialog = Kdialog.getProgress(context, mDialog);
        getFwVerTimer = new Timer();
        getFwVerTimer.schedule(new GetFwVerTask(), 0, PrjCfg.RUN_ONCE);
    }

    private void onResumeAfter() {
        stopResume = false;
        mDialog = Kdialog.getProgress(context, mDialog);

        PreferenceScreen pfscreen = (PreferenceScreen) findPreference("preferenceScreen");
        mCateCloud = (PreferenceCategory) findPreference("CategoryCloud");
        mCateDevSettings = (PreferenceCategory) findPreference("CategoryDevSettings");
        PreferenceCategory mCateTime = (PreferenceCategory)findPreference("CategoryTime");
        PreferenceCategory mCateUart = (PreferenceCategory)findPreference("CategoryCom0");
        PreferenceCategory mCateUart1 = (PreferenceCategory)findPreference("CategoryCom1");
        PreferenceCategory mCateTcpSlave = (PreferenceCategory)findPreference("CategoryTcpSlave");

        // Not debug mode -> hide URL, HTTPS, PollTime
        if(!PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_KSMT_TEST_ALL)){
            mCateCloud.removePreference(cloudUrl);
            mCateCloud.removePreference(cloudPt);
        }

        // hide some preference if necessary
        if(!curDev.isCE()) { // Hide NTP, UART, HTTPS if not CE
            pfscreen.removePreference(mCateTime);
            pfscreen.removePreference(mCateTcpSlave);
            pfscreen.removePreference(mCateUart);
            pfscreen.removePreference(mCateUart1);
            mCateCloud.removePreference(devReg);
        } else { // Cloud Enabler
            mCateDevSettings.removePreference(sioSyncTime);
            if(curDev.getNumRs485() < 2 && mCateUart1 != null) {
                pfscreen.removePreference(mCateUart1);
            }
            if(curDev.isMbusMaster()) {
                if(mCateTcpSlave != null) {
                    pfscreen.removePreference(mCateTcpSlave);
                }
                mCateUart.removePreference(mbUid);
                mCateUart1.removePreference(mbUid1);
                mbOpMode.setEntries(R.array.proxyOpmodeKey);
                mbOpMode.setEntryValues(R.array.proxyOpmodeVal);
                mbOpMode1.setEntries(R.array.proxyOpmodeKey);
                mbOpMode1.setEntryValues(R.array.proxyOpmodeVal);
            } else {
                mbOpMode.setEntries(R.array.mbOpmodeKey);
                mbOpMode.setEntryValues(R.array.mbOpmodeVal);
                mbOpMode1.setEntries(R.array.mbOpmodeKey);
                mbOpMode1.setEntryValues(R.array.mbOpmodeVal);
            }
        }
        getDevInfoTimer = new Timer();
        getDevInfoTimer.schedule(new DevCfgTask(), 0, PrjCfg.RUN_ONCE);

        getDevTimeTimer = new Timer();
        getDevTimeTimer.schedule(new DevTimeTask(), 100, PrjCfg.LOCAL_DEVTIME_POLLING);

        getCntStatusTimer = new Timer();
        getCntStatusTimer.schedule(new CntStatusTask(), 600, 3000);

        if(curDev.isCE()) {
            ntpTimer = new Timer();
            ntpTimer.schedule(new NTPTask(), 150, PrjCfg.RUN_ONCE);

            uartTimer = new Timer();
            uartTimer.schedule(new UartTask(), 250, PrjCfg.RUN_ONCE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch(requestCode){
            case DEVICE_REGISTER: {
                stopResume = true;
                if(intent == null) {
                    //Log.e(Dbg._TAG_(), "Intent is empty!");
                    return;
                }
                boolean isRegDevice = intent.getBooleanExtra("isRegDevice", false);
                if(!isRegDevice) {
                    Log.e(Dbg._TAG_(), "Register Device isn't successful!");
                    return; // Register failed
                }
                int newCloudPt = 10; // set the polltime of the device to 1sec
                cloudPt.setText(String.valueOf(newCloudPt));
                cloudPt.setSummary(String.valueOf(newCloudPt));
                respApCfg[388] = (byte) (newCloudPt >> 8);
                respApCfg[389] = (byte) newCloudPt;
                respApCfg[11] = (byte) 2; // 4: HTTPs cloud 2: cloud, 0: local

                mDialog = Kdialog.getProgress(context, mDialog);
                setDevTimer = new Timer();
                setDevTimer.schedule(new DevCfgTask("SET"), 0, PrjCfg.RUN_ONCE);
                break;
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getDevInfoTimer != null) {
            getDevInfoTimer.cancel();
        }
        if (getDevTimeTimer != null) {
            getDevTimeTimer.cancel();
        }
        if (getFwVerTimer != null) {
            getFwVerTimer.cancel();
        }
        if(setDevTimer != null) {
            setDevTimer.cancel();
        }
        if(rebootTimer != null) {
            rebootTimer.cancel();
        }
        if(ntpTimer != null) {
            ntpTimer.cancel();
        }
        if (mDialog != null) {
            mDialog.cancel();
        }
        if(getCntStatusTimer != null) {
            getCntStatusTimer.cancel();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AppSettings.reload(context);
    }

    @Override
    public void onBackPressed() {
        boolean needReboot = false;
        if(origSetting != null) {
            if(origSetting.enStaticIp != devSetting.enStaticIp ||
               !origSetting.dns.equals(devSetting.dns) ||
               !origSetting.gateway.equals(devSetting.gateway) ||
               !origSetting.ipAddr.equals(devSetting.ipAddr) ||
               !origSetting.submask.equals(devSetting.submask)) {
                needReboot = true;
            }
        }
        if(!needReboot) {
            super.onBackPressed();
            return;
        }

        AlertDialog.Builder dialog = Kdialog.getDefInfoDialog(context);
        dialog.setMessage(getString(R.string.make_sure_reboot))
        .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                origSetting = null;
                mDialog = Kdialog.getProgress(context, mDialog);
                rebootTimer = new Timer();
                rebootTimer.schedule(new RebootTask(), 0, PrjCfg.RUN_ONCE);
            }
        })
        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                finish();
            }
        }).show();
    }

    private Preference.OnPreferenceChangeListener enTcpSlaveListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Boolean isClicked = (Boolean) newValue;
            ((CheckBoxPreference) preference).setChecked(isClicked);
            respApCfg[496] = (byte) ((isClicked) ? 1 : 0); // 0: RTU/ASCII, 1: TCP

            mDialog = Kdialog.getProgress(context, mDialog);
            setDevTimer = new Timer();
            setDevTimer.schedule(new DevCfgTask("SET"), 0, PrjCfg.RUN_ONCE);
            return true;
        }
    };

    private Preference.OnPreferenceClickListener slvTcpPortListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            final View layout = inflater.inflate(R.layout.input_number, null);
            final EditText editSlvTcpPort = (EditText) layout.findViewById(R.id.text);
            editSlvTcpPort.setHint(getString(R.string.slvtcp_port_range));
            editSlvTcpPort.setText(slvTcpPort.getSummary());

            final AlertDialog dialog = Kdialog.getDefInputDialog(context).create();
            dialog.setView(layout);
            dialog.setTitle(getString(R.string.pref_tcp_slave_port));
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogIface) {
                    Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                Boolean noError = true;
                                String newSlvTcpPort = editSlvTcpPort.getText().toString().trim();
                                if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, newSlvTcpPort)) {
                                    noError = false;
                                    editSlvTcpPort.setError(getString(R.string.err_msg_empty));
                                }

                                int newSlvTcpPortDec = Integer.parseInt(newSlvTcpPort);
                                if(!(newSlvTcpPortDec > 0 && newSlvTcpPortDec < 65536)) {
                                    noError = false;
                                    editSlvTcpPort.setError(getString(R.string.slvtcp_port_range));
                                } else if(newSlvTcpPortDec == 21 || newSlvTcpPortDec == 80 || newSlvTcpPortDec == 2222 || newSlvTcpPortDec == 2233 || newSlvTcpPortDec == 3322 || newSlvTcpPortDec == 9980) {
                                    noError = false;
                                    editSlvTcpPort.setError(getString(R.string.reserved_port));
                                }

                                if (noError) {
                                    respApCfg[497] = (byte) (newSlvTcpPortDec >> 8);
                                    respApCfg[498] = (byte) newSlvTcpPortDec;
                                    slvTcpPort.setSummary(newSlvTcpPort);
                                    dialog.dismiss();

                                    mDialog = Kdialog.getProgress(context, mDialog);
                                    setDevTimer = new Timer();
                                    setDevTimer.schedule(new DevCfgTask("SET"), 0, PrjCfg.RUN_ONCE);
                                }
                            } catch (Exception e) {
                                editSlvTcpPort.setError(getString(R.string.invalid_val));
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });
            dialog.show();
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener mbOpModeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String newMbOpMode = (String)newValue;
            if(newMbOpMode == null || newMbOpMode.length() == 0) {
                return false;
            }
            mbOpMode.setValue(newMbOpMode);
            mbOpMode.setSummary(mbOpMode.getEntry());
            uartCfg[17] = 1;
            uartCfg[19] = Byte.parseByte(newMbOpMode);

            mDialog = Kdialog.getProgress(context, mDialog);
            uartTimer = new Timer();
            uartTimer.schedule(new UartTask("SET"), 0, PrjCfg.RUN_ONCE);
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener mbOpMode1Listener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String newMbOpMode = (String)newValue;
            if(newMbOpMode == null || newMbOpMode.length() == 0) {
                return false;
            }
            mbOpMode1.setValue(newMbOpMode);
            mbOpMode1.setSummary(mbOpMode1.getEntry());
            uartCfg[25] = 1;
            uartCfg[27] = Byte.parseByte(newMbOpMode);

            mDialog = Kdialog.getProgress(context, mDialog);
            uartTimer = new Timer();
            uartTimer.schedule(new UartTask("SET-1"), 0, PrjCfg.RUN_ONCE);
            return true;
        }
    };

    private Preference.OnPreferenceClickListener mbUidListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            final View layout = inflater.inflate(R.layout.input_number, null);
            final EditText editMbUid = (EditText) layout.findViewById(R.id.text);
            editMbUid.setHint(getString(R.string.modbus_uid_range));
            editMbUid.setText(mbUid.getSummary());

            final AlertDialog dialog = Kdialog.getDefInputDialog(context).create();
            dialog.setView(layout);
            dialog.setTitle(getString(R.string.pref_modbus_uid));
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogIface) {
                    Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                Boolean noError = true;
                                String newMbUid = editMbUid.getText().toString().trim();
                                if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, newMbUid)) {
                                    noError = false;
                                    editMbUid.setError(getString(R.string.err_msg_empty));
                                }

                                int newMbUidDec = Integer.parseInt(newMbUid);
                                if(!(newMbUidDec > 0 && newMbUidDec < 255)) {
                                    noError = false;
                                    editMbUid.setError(getString(R.string.invalid_val));
                                }

                                if (noError) {
                                    uartCfg[12] = (byte)(newMbUidDec >> 0);
                                    mbUid.setSummary(newMbUid);
                                    dialog.dismiss();

                                    mDialog = Kdialog.getProgress(context, mDialog);
                                    uartTimer = new Timer();
                                    uartTimer.schedule(new UartTask("SET"), 0, PrjCfg.RUN_ONCE);
                                }
                            } catch (Exception e) {
                                editMbUid.setError(getString(R.string.err_msg_empty));
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });
            dialog.show();
            return true;
        }
    };

    private Preference.OnPreferenceClickListener mbUid1Listener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            final View layout = inflater.inflate(R.layout.input_number, null);
            final EditText editMbUid = (EditText) layout.findViewById(R.id.text);
            editMbUid.setHint(getString(R.string.modbus_uid_range));
            editMbUid.setText(mbUid1.getSummary());

            final AlertDialog dialog = Kdialog.getDefInputDialog(context).create();
            dialog.setView(layout);
            dialog.setTitle(getString(R.string.pref_modbus_uid));
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogIface) {
                    Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                Boolean noError = true;
                                String newMbUid = editMbUid.getText().toString().trim();
                                if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, newMbUid)) {
                                    noError = false;
                                    editMbUid.setError(getString(R.string.err_msg_empty));
                                }

                                int newMbUidDec = Integer.parseInt(newMbUid);
                                if(!(newMbUidDec > 0 && newMbUidDec < 255)) {
                                    noError = false;
                                    editMbUid.setError(getString(R.string.invalid_val));
                                }

                                if (noError) {
                                    uartCfg[20] = (byte)(newMbUidDec >> 0);
                                    mbUid.setSummary(newMbUid);
                                    dialog.dismiss();

                                    mDialog = Kdialog.getProgress(context, mDialog);
                                    uartTimer = new Timer();
                                    uartTimer.schedule(new UartTask("SET-1"), 0, PrjCfg.RUN_ONCE);
                                }
                            } catch (Exception e) {
                                editMbUid.setError(getString(R.string.err_msg_empty));
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });
            dialog.show();
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener mbIfaceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String newMbIface = (String)newValue;
            if(newMbIface == null || newMbIface.length() == 0) {
                return false;
            }
            mbIface.setValue(newMbIface);
            mbIface.setSummary(mbIface.getEntry());
            uartCfg[14] = Byte.parseByte(newMbIface);

            mDialog = Kdialog.getProgress(context, mDialog);
            uartTimer = new Timer();
            uartTimer.schedule(new UartTask("SET"), 0, PrjCfg.RUN_ONCE);
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener mbIface1Listener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String newMbIface = (String)newValue;
            if(newMbIface == null || newMbIface.length() == 0) {
                return false;
            }
            mbIface1.setValue(newMbIface);
            mbIface1.setSummary(mbIface1.getEntry());
            uartCfg[22] = Byte.parseByte(newMbIface);

            mDialog = Kdialog.getProgress(context, mDialog);
            uartTimer = new Timer();
            uartTimer.schedule(new UartTask("SET-1"), 0, PrjCfg.RUN_ONCE);
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener mbBRateListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String newMbBRate = (String)newValue;
            if(newMbBRate == null || newMbBRate.length() == 0) {
                return false;
            }
            mbBRate.setValue(newMbBRate);
            mbBRate.setSummary(mbBRate.getEntry());
            uartCfg[15] = Byte.parseByte(newMbBRate);

            mDialog = Kdialog.getProgress(context, mDialog);
            uartTimer = new Timer();
            uartTimer.schedule(new UartTask("SET"), 0, PrjCfg.RUN_ONCE);
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener mbBRate1Listener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String newMbBRate = (String)newValue;
            if(newMbBRate == null || newMbBRate.length() == 0) {
                return false;
            }
            mbBRate1.setValue(newMbBRate);
            mbBRate1.setSummary(mbBRate1.getEntry());
            uartCfg[23] = Byte.parseByte(newMbBRate);

            mDialog = Kdialog.getProgress(context, mDialog);
            uartTimer = new Timer();
            uartTimer.schedule(new UartTask("SET-1"), 0, PrjCfg.RUN_ONCE);
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener mbDataLenListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String newMbDataLen = (String)newValue;
            if(newMbDataLen == null || newMbDataLen.length() == 0) {
                return false;
            }
            mbDataLen.setValue(newMbDataLen);
            mbDataLen.setSummary(mbDataLen.getEntry());
            uartCfg[17] = Byte.parseByte(newMbDataLen);

            mDialog = Kdialog.getProgress(context, mDialog);
            uartTimer = new Timer();
            uartTimer.schedule(new UartTask("SET"), 0, PrjCfg.RUN_ONCE);
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener mbDataLen1Listener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String newMbDataLen = (String)newValue;
            if(newMbDataLen == null || newMbDataLen.length() == 0) {
                return false;
            }
            mbDataLen1.setValue(newMbDataLen);
            mbDataLen1.setSummary(mbDataLen.getEntry());
            uartCfg[25] = Byte.parseByte(newMbDataLen);

            mDialog = Kdialog.getProgress(context, mDialog);
            uartTimer = new Timer();
            uartTimer.schedule(new UartTask("SET-1"), 0, PrjCfg.RUN_ONCE);
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener mbParityListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String newMbParity = (String)newValue;
            if(newMbParity  == null || newMbParity .length() == 0) {
                return false;
            }
            mbParity.setValue(newMbParity);
            mbParity.setSummary(mbParity.getEntry());
            uartCfg[16] = Byte.parseByte(newMbParity);

            mDialog = Kdialog.getProgress(context, mDialog);
            uartTimer = new Timer();
            uartTimer.schedule(new UartTask("SET"), 0, PrjCfg.RUN_ONCE);
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener mbParity1Listener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String newMbParity = (String)newValue;
            if(newMbParity  == null || newMbParity .length() == 0) {
                return false;
            }
            mbParity1.setValue(newMbParity);
            mbParity1.setSummary(mbParity1.getEntry());
            uartCfg[24] = Byte.parseByte(newMbParity);

            mDialog = Kdialog.getProgress(context, mDialog);
            uartTimer = new Timer();
            uartTimer.schedule(new UartTask("SET-1"), 0, PrjCfg.RUN_ONCE);
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener mbStopBitListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String newMbStopBit = (String)newValue;
            if(newMbStopBit  == null || newMbStopBit .length() == 0) {
                return false;
            }
            mbStopBit.setValue(newMbStopBit);
            mbStopBit.setSummary(mbStopBit.getEntry());
            uartCfg[18] = Byte.parseByte(newMbStopBit);

            mDialog = Kdialog.getProgress(context, mDialog);
            uartTimer = new Timer();
            uartTimer.schedule(new UartTask("SET"), 0, PrjCfg.RUN_ONCE);
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener mbStopBit1Listener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String newMbStopBit = (String)newValue;
            if(newMbStopBit  == null || newMbStopBit .length() == 0) {
                return false;
            }
            mbStopBit1.setValue(newMbStopBit);
            mbStopBit1.setSummary(mbStopBit1.getEntry());
            uartCfg[26] = Byte.parseByte(newMbStopBit);

            mDialog = Kdialog.getProgress(context, mDialog);
            uartTimer = new Timer();
            uartTimer.schedule(new UartTask("SET-1"), 0, PrjCfg.RUN_ONCE);
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener enCloudListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Boolean isClicked = (Boolean) newValue;
            ((CheckBoxPreference) preference).setChecked(isClicked);
            respApCfg[11] = (byte) ((isClicked) ? 2 : 0); // 4: HTTPs cloud 2: cloud, 0: local

            mDialog = Kdialog.getProgress(context, mDialog);
            setDevTimer = new Timer();
            setDevTimer.schedule(new DevCfgTask("SET"), 0, PrjCfg.RUN_ONCE);

            if(!isClicked) {
                if(getCntStatusTimer != null) {
                    getCntStatusTimer.cancel();
                    getCntStatusTimer = null;
                }
                MHandler.exec(mHandler, MHandler.CNT_STATUS, getString(R.string.conf_not_enable));
            } else {
                if(getCntStatusTimer == null) {
                    getCntStatusTimer = new Timer();
                    getCntStatusTimer.schedule(new CntStatusTask(), 600, 3000);
                }
            }
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener enHTTPSListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Boolean isClicked = (Boolean) newValue;
            ((CheckBoxPreference) preference).setChecked(isClicked);
            respApCfg[11] = (byte) ((isClicked) ? 4 : 2); // 4: HTTPs cloud 2: cloud, 0: local

            mDialog = Kdialog.getProgress(context, mDialog);
            setDevTimer = new Timer();
            setDevTimer.schedule(new DevCfgTask("SET"), 0, PrjCfg.RUN_ONCE);
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener devNameListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String newDevName = (String)newValue;
            if(newDevName == null || newDevName.length() == 0) {
                MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_empty));
                return false;
            }
            // check byte length
            try {
                //Log.e(Dbg._TAG_(), String.valueOf(newDevName.getBytes("UTF-8").length));
                if(newDevName.getBytes("UTF-8").length > 31) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_max_string_exceeded));
                    return false;
                }
            } catch (Exception e) {
            }
            devName.setText(newDevName);
            devName.setSummary(newDevName);
            StrUtils.setPktStr(respApCfg, 16, (newDevName + "\u0000").getBytes());

            mDialog = Kdialog.getProgress(context, mDialog);
            setDevTimer = new Timer();
            setDevTimer.schedule(new DevCfgTask("SET"), 0, PrjCfg.RUN_ONCE);
            return true;
        }
    };

    private Preference.OnPreferenceClickListener devPswdListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            final View layout = inflater.inflate(R.layout.input_new_password, null);
            final EditText editPassword = (EditText) layout.findViewById(R.id.password);
            final EditText editConfimPassword = (EditText) layout.findViewById(R.id.confimPassword);

            final AlertDialog dialog = Kdialog.getDefInputDialog(context).create();
            dialog.setView(layout);
            dialog.setTitle(getString(R.string.reset) + getString(R.string.device) + getString(R.string.password));
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogIface) {
                    Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Boolean noError = true;
                            String password = editPassword.getText().toString().trim();
                            String confirmPassword = editConfimPassword.getText().toString().trim();
                            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, password)) {
                                noError = false;
                                editPassword.setError(getString(R.string.err_msg_empty));
                            } else if (!StrUtils.validateInput(StrUtils.IN_TYPE_STRONG_PSWD, password)) {
                                noError = false;
                                editPassword.setError(getString(R.string.err_msg_strong_password));
                            }

                            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, confirmPassword)) {
                                noError = false;
                                editConfimPassword.setError(getString(R.string.err_msg_empty));
                            } else if (!password.equals(confirmPassword)) {
                                noError = false;
                                editPassword.setError(getString(R.string.err_msg_password_not_equal));
                            }
                            if (noError) {
                                dialog.dismiss();
                                StrUtils.setPktStr(respApCfg, 390, (password + "\u0000").getBytes());
                                mDialog = Kdialog.getProgress(context, mDialog);
                                setDevTimer = new Timer();
                                setDevTimer.schedule(new DevCfgTask("SET"), 0, PrjCfg.RUN_ONCE);
                            }
                        }
                    });
                }
            });
            dialog.show();
            return true;
        }
    };

    private Preference.OnPreferenceClickListener devRegListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            final Intent intent = new Intent(context, NewDeviceActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

            Bundle bundle = new Bundle();
            bundle.putSerializable("Device", curDev);
            intent.putExtras(bundle);

            final String appUrl = PrjCfg.CLOUD_URL.split("//")[1] + "/ioreg"; // http://xxx.ksmt.co -> xxx.ksmt.co/ioreg
            Log.e(Dbg._TAG_(), "deive =" + cloudUrl.getText() + ", app = " + appUrl);
            if(cloudUrl.getText().equals(appUrl)) {
                startActivityForResult(intent, DEVICE_REGISTER);
                return true;
            }
            Kdialog.getMakeSureDialog(context, getString(R.string.cloud_different))
            .setPositiveButton(getString(R.string.change), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mDialog = Kdialog.getProgress(context, mDialog);
                    setCloudURL(appUrl);
                    startActivityForResult(intent, DEVICE_REGISTER);
                }
            }).setNegativeButton(getString(R.string.no_change), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivityForResult(intent, DEVICE_REGISTER);
                }
            }).show();
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener enNTPListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Boolean isClicked = (Boolean) newValue;
            ((CheckBoxPreference) preference).setChecked(isClicked);
            ntpCfg[11] = (byte) ((isClicked) ? 1 : 0); // 0: disable, 1: enable

            mDialog = Kdialog.getProgress(context, mDialog);
            ntpTimer = new Timer();
            ntpTimer.schedule(new NTPTask("SET"), 0, PrjCfg.RUN_ONCE);
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener ntpServ1Listener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String newNTPServer = (String)newValue;
            if(newNTPServer == null || newNTPServer.length() == 0) {
                return false;
            }
            ntpServer1.setText(newNTPServer);
            ntpServer1.setSummary(newNTPServer);
            StrUtils.setPktStr(ntpCfg, 20, (newNTPServer + "\u0000").getBytes());

            mDialog = Kdialog.getProgress(context, mDialog);
            ntpTimer = new Timer();
            ntpTimer.schedule(new NTPTask("SET"), 0, PrjCfg.RUN_ONCE);
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener ntpServ2Listener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String newNTPServer = (String)newValue;
            if(newNTPServer == null || newNTPServer.length() == 0) {
                return false;
            }
            ntpServer2.setText(newNTPServer);
            ntpServer2.setSummary(newNTPServer);
            StrUtils.setPktStr(ntpCfg, 84, (newNTPServer + "\u0000").getBytes());

            mDialog = Kdialog.getProgress(context, mDialog);
            ntpTimer = new Timer();
            ntpTimer.schedule(new NTPTask("SET"), 0, PrjCfg.RUN_ONCE);
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener tzoneListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String newTZone = (String)newValue;
            if(newTZone == null || newTZone.length() == 0) {
                return false;
            }
            tZone.setSummary(newTZone);
            int newTZVal = Integer.valueOf(newTZone);
            ntpCfg[148] = (byte) ((newTZVal >> 24) & 0xff);
            ntpCfg[149] = (byte) ((newTZVal >> 16) & 0xff);
            ntpCfg[150] = (byte) ((newTZVal >>  8) & 0xff);
            ntpCfg[151] = (byte) ((newTZVal      ) & 0xff);

            mDialog = Kdialog.getProgress(context, mDialog);
            ntpTimer = new Timer();
            ntpTimer.schedule(new NTPTask("SET"), 0, PrjCfg.RUN_ONCE);
            return true;
        }
    };

    private void setCloudURL(String newUrl) {
        this.cloudUrl.setText(newUrl);
        this.cloudUrl.setSummary(newUrl);
        StrUtils.setPktStr(respApCfg, 132, (newUrl + "\u0000").getBytes());

        setDevTimer = new Timer();
        setDevTimer.schedule(new DevCfgTask("SET"), 0, PrjCfg.RUN_ONCE);
    }

    private Preference.OnPreferenceChangeListener cloudUrlListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String newCloudUrl = (String)newValue;
            if(newCloudUrl == null || newCloudUrl.length() == 0) {
                return false;
            }
            mDialog = Kdialog.getProgress(context, mDialog);
            setCloudURL(newCloudUrl);
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener cloudPtListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String newStr = (String) newValue;
            if(newStr == null || newStr.length() == 0) {
                return false;
            }
            int newCloudPt = Integer.valueOf(newStr);
            cloudPt.setText(newStr);
            cloudPt.setSummary(newStr);
            respApCfg[388] = (byte) (newCloudPt >> 8);
            respApCfg[389] = (byte) newCloudPt;

            mDialog = Kdialog.getProgress(context, mDialog);
            setDevTimer = new Timer();
            setDevTimer.schedule(new DevCfgTask("SET"), 0, PrjCfg.RUN_ONCE);
            return true;
        }
    };

    private Preference.OnPreferenceClickListener syncTimeListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            new AlertDialog.Builder(context)
                    .setTitle(getString(R.string.pref_sync_time))
                    .setIcon(R.drawable.ic_time_sync)
                    .setMessage(getString(R.string.make_sure))
                    .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            mDialog = Kdialog.getProgress(context, mDialog);
                            setDevTimer = new Timer();
                            setDevTimer.schedule(new DevTimeTask("SET"), 0, PrjCfg.RUN_ONCE);
                        }
                    }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            }).show();
            return true;
        }
    };

    private Preference.OnPreferenceClickListener enStaticIpClickListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            if(enStaticIp.isChecked()) { // static ip
                setStaticIpAddr();
            } else { // DHCP
                respApCfg[423] = 1; // 1: DHCP, 0: Static IP
                mDialog = Kdialog.getProgress(context, mDialog);
                setDevTimer = new Timer();
                setDevTimer.schedule(new DevCfgTask("SET"), 0, PrjCfg.RUN_ONCE);
            }
            return true;
        }
    };

    private Preference.OnPreferenceClickListener cfgStaticIpListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            setStaticIpAddr();
            return true;
        }
    };

    private void setStaticIpAddr() {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View layout = inflater.inflate(R.layout.dialog_static_ip, null);
        final EditText ipAddr  = (EditText) layout.findViewById(R.id.ip_addr);
        final EditText submask = (EditText) layout.findViewById(R.id.submask);
        final EditText gateway = (EditText) layout.findViewById(R.id.gateway);
        final EditText dns	   = (EditText) layout.findViewById(R.id.dns);
        ipAddr.setText(devSetting.ipAddr);
        submask.setText(devSetting.submask);
        gateway.setText(devSetting.gateway);
        dns.setText(devSetting.dns);

        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(getString(R.string.pref_static_network_ip_addr));
        alert.setIcon(R.drawable.ic_editor);
        alert.setView(layout);
        alert.setCancelable(false);
        alert.setPositiveButton(getString(R.string.confirm), null);
        alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                enStaticIp.setChecked(origSetting.enStaticIp);
            }
        });

        final AlertDialog dialog = alert.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogIface) {
                Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Boolean noError = true;
                        if(ipAddr.getText().length() < 1) {
                            noError = false;
                            ipAddr.setError(getString(R.string.err_msg_empty));
                        }
                        if(!StrUtils.validateInput(StrUtils.IN_TYPE_IPV4, ipAddr.getText().toString())) {
                            noError = false;
                            ipAddr.setError(getString(R.string.err_msg_format));
                        }
                        if(submask.getText().length() < 1) {
                            noError = false;
                            submask.setError(getString(R.string.err_msg_empty));
                        }
                        if(!StrUtils.validateInput(StrUtils.IN_TYPE_IPV4, submask.getText().toString())) {
                            noError = false;
                            submask.setError(getString(R.string.err_msg_format));
                        }
                        if(gateway.getText().length() < 1) {
                            noError = false;
                            gateway.setError(getString(R.string.err_msg_empty));
                        }
                        if(!StrUtils.validateInput(StrUtils.IN_TYPE_IPV4, gateway.getText().toString())) {
                            noError = false;
                            gateway.setError(getString(R.string.err_msg_invalid_ip));
                        }
                        if(dns.getText().length() < 1) {
                            noError = false;
                            dns.setError(getString(R.string.err_msg_empty));
                        }
                        if(!StrUtils.validateInput(StrUtils.IN_TYPE_IPV4, dns.getText().toString())) {
                            noError = false;
                            dns.setError(getString(R.string.err_msg_format));
                        }

                        String ipAddrStr = ipAddr.getText().toString();
                        String netmaskStr = submask.getText().toString();
                        String gatewayStr = gateway.getText().toString();
                        int netmaskByte = Utils.getIpBytes(netmaskStr);
                        if(noError && (Utils.getIpBytes(ipAddrStr) & netmaskByte) != (Utils.getIpBytes(gatewayStr) & netmaskByte)) {
                            noError = false;
                            ipAddr.setError(getString(R.string.err_msg_invalid_ip_range));
                        }

                        if (noError) {
                            dialog.dismiss();
                            String[] ipa = ipAddrStr.split("\\.");
                            String[] netmaska = netmaskStr.split("\\.");
                            String[] gateways = gatewayStr.split("\\.");
                            String[] dnsa = dns.getText().toString().split("\\.");

                            respApCfg[423] = 0; // Static IP
                            respApCfg[424] = (byte) (int) Integer.valueOf(ipa[0]);
                            respApCfg[425] = (byte) (int) Integer.valueOf(ipa[1]);
                            respApCfg[426] = (byte) (int) Integer.valueOf(ipa[2]);
                            respApCfg[427] = (byte) (int) Integer.valueOf(ipa[3]);
                            respApCfg[428] = (byte) (int) Integer.valueOf(netmaska[0]);
                            respApCfg[429] = (byte) (int) Integer.valueOf(netmaska[1]);
                            respApCfg[430] = (byte) (int) Integer.valueOf(netmaska[2]);
                            respApCfg[431] = (byte) (int) Integer.valueOf(netmaska[3]);
                            respApCfg[432] = (byte) (int) Integer.valueOf(gateways[0]);
                            respApCfg[433] = (byte) (int) Integer.valueOf(gateways[1]);
                            respApCfg[434] = (byte) (int) Integer.valueOf(gateways[2]);
                            respApCfg[435] = (byte) (int) Integer.valueOf(gateways[3]);
                            respApCfg[436] = (byte) (int) Integer.valueOf(dnsa[0]);
                            respApCfg[437] = (byte) (int) Integer.valueOf(dnsa[1]);
                            respApCfg[438] = (byte) (int) Integer.valueOf(dnsa[2]);
                            respApCfg[439] = (byte) (int) Integer.valueOf(dnsa[3]);

                            mDialog = Kdialog.getProgress(context, mDialog);
                            setDevTimer = new Timer();
                            setDevTimer.schedule(new DevCfgTask("SET"), 0, PrjCfg.RUN_ONCE);
                        }
                    }
                });
            }
        });
        dialog.show();
    }

    private class DevCfgTask extends TimerTask {
        private String action;

        public DevCfgTask() {
            this.action = "GET";
        }

        public DevCfgTask(String action) {
            this.action = action;
        }

        public void run() {
            try {
                byte[] reqData;
                if(action.equals("SET")) {
                    StrUtils.setPktStr(respApCfg, 0, TCPFrame.CMD_APCFGREQ.getBytes());
                    respApCfg[10] = 4; // 1: set and reboot, 4: set
                    calcCheckSum(respApCfg);
                    reqData = respApCfg;
                } else {
                    reqData = TCPFrame.getAPCFGREQ((byte) 2); // 2: Get AP Mode Settings
                }

                //Log.e(Dbg._TAG_(), StrUtils.getPktStr(respApCfg, 0, 8) + ", req=" + StrUtils.getHex(reqData));
                byte[] respPkt = WebUtils.tcpRequest(curDev.ip, reqData);
                if(respPkt == null) {
                    Log.e(Dbg._TAG_(), "NO response packet !?");
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.err_res_packet));
                    return;
                }

                //Log.e(Dbg._TAG_(), StrUtils.getPktStr(respApCfg, 0, 8) + ", res=" + StrUtils.getHex(respApCfg));
                String typeStr = StrUtils.getPktStr(respPkt, 0, 8);
                if (!typeStr.equals(TCPFrame.CMD_APCFGRSP)) {
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.err_res_packet));
                    Log.e(Dbg._TAG_(), "Unexpected Frame " + typeStr + ", 9:" + respPkt[9] + ", 10:" + respPkt[10] + ", 11:" + respPkt[11] + ", 12:" + respPkt[12]);
                    return;
                } else {
                    respApCfg = respPkt;
                }

                // Ethernet
                devSetting.enStaticIp = (respApCfg[423] == 1) ? false : true ;
                devSetting.ipAddr  = ((int) respApCfg[424] & 0xff) + "." + ((int) respApCfg[425] & 0xff) + "." + ((int) respApCfg[426] & 0xff) + "." + ((int) respApCfg[427] & 0xff);
                devSetting.submask = ((int) respApCfg[428] & 0xff) + "." + ((int) respApCfg[429] & 0xff) + "." + ((int) respApCfg[430] & 0xff) + "." + ((int) respApCfg[431] & 0xff);
                devSetting.gateway = ((int) respApCfg[432] & 0xff) + "." + ((int) respApCfg[433] & 0xff) + "." + ((int) respApCfg[434] & 0xff) + "." + ((int) respApCfg[435] & 0xff);
                devSetting.dns 	= ((int) respApCfg[436] & 0xff) + "." + ((int) respApCfg[437] & 0xff) + "." + ((int) respApCfg[438] & 0xff) + "." + ((int) respApCfg[439] & 0xff);

                // Cloud Service
                devSetting.devName = StrUtils.getPktStr(respApCfg, 16, 32);
                devSetting.devPswd = StrUtils.getPktStr(respApCfg, 390, 32);
                devSetting.cloudUrl = StrUtils.getPktStr(respApCfg, 132, 256);
                devSetting.enCloudMode = (respApCfg[11] == 2 || respApCfg[11] == 4) ? true : false ;
                devSetting.enHttps = (respApCfg[11] == 4) ? true : false;
                devSetting.cloudPt = ((respApCfg[388] & 0xff) << 8) + (respApCfg[389] & 0xff);

                // TCP Slave setting
                devSetting.isSlvTcp = (respApCfg[496] == 1) ? true: false;
                devSetting.slvTcpPort = ((respApCfg[497] & 0xff) << 8) + (respApCfg[498] & 0xff);
                MHandler.exec(mHandler, MHandler.UPDATE_APCFG);
                if(origSetting == null) {
                    origSetting = new DevSetting();
                    copySettings(devSetting, origSetting);
                }
            } catch (Exception e) {
                e.printStackTrace();
                if(action.equals("SET")) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_update_val));
                } else {
                    MHandler.exec(mHandler, MHandler.GO_BACK, getString(R.string.err_msg_unable_connect));
                }
            } finally {
                if(action.equals("SET") && setDevTimer != null) {
                    setDevTimer.cancel();
                } else {
                    getDevInfoTimer.cancel();
                }
                runOnUiThread(new Runnable(){
                    public void run() {
                        if(stopResume) {
                            onResumeAfter();
                        } else {
                            mDialog.dismiss();
                        }
                    }
                });
            }
        }
    }

    private class DevTimeTask extends TimerTask {
        private String action;

        public DevTimeTask() {
            this.action = "GET";
        }

        public DevTimeTask(String action) {
            this.action = action;
        }

        public void run() {
            try {
                byte[] reqData = (action.equals("SET")) ? TCPFrame.getSET_TIME_REQ() : TCPFrame.getGET_TIME_REQ();
                byte[] resData = WebUtils.tcpRequest(curDev.ip, reqData);
                String typeStr = StrUtils.getPktStr(resData, 0, 8);
                if (!typeStr.equals(TCPFrame.CMD_TIME_RSP)) {
                    Log.e(Dbg._TAG_(), "Error response (Frame: " + typeStr + ")");
                    return;
                }
                String year = String.format("%04d", ((resData[12] << 8) & 0xff00) | (resData[13] & 0xff));
                String month = String.format("%02d", new Byte(resData[14]).intValue());
                String day = String.format("%02d", new Byte(resData[15]).intValue());
                String hour = String.format("%02d", new Byte(resData[16]).intValue());
                String min = String.format("%02d", new Byte(resData[17]).intValue());
                String sec = String.format("%02d", new Byte(resData[18]).intValue());
                String devTime = year + "-" + month + "-" + day + " " + hour + ":" + min + ":" + sec;
                MHandler.exec(mHandler, MHandler.UPDATE_TIME, devTime);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if(action.equals("SET") && setDevTimer != null) {
                    mDialog.dismiss();
                    setDevTimer.cancel();
                }
            }
        }
    }

    private class CntStatusTask extends TimerTask {
        public void run() {
            try {
                if(!devSetting.enCloudMode) {
                    return;
                }
                byte[] reqData = TCPFrame.getMISC_REQ(TCPFrame.MISC_Type.RESERVED, devSetting.devName);
                byte[] resData = WebUtils.tcpRequest(curDev.ip, reqData);
                String typeStr = StrUtils.getPktStr(resData, 0, 8);
                if (!typeStr.equals(TCPFrame.CMD_MISC_RSP)) {
                    Log.e(Dbg._TAG_(), "Error response (Frame: " + typeStr + ")");
                    return;
                }
                int cntStatus = new Byte(resData[11]).intValue();
                MHandler.exec(mHandler, MHandler.CNT_STATUS, (cntStatus == 0) ? getString(R.string.cloud_cnt_ok) : getString(R.string.cloud_cnt_failed));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class RebootTask extends TimerTask {
        public void run() {
            try {
                byte[] reqData = TCPFrame.getMISC_REQ(TCPFrame.MISC_Type.RESET_SYSTEM, devSetting.devName);
                byte[] resData = WebUtils.tcpRequest(curDev.ip, reqData);
                String typeStr = StrUtils.getPktStr(resData, 0, 8);
                if (!typeStr.equals(TCPFrame.CMD_MISC_RSP)) {
                    Log.e(Dbg._TAG_(), "Error response (Frame: " + typeStr + ")");
                    return;
                }
                Thread.sleep(70000);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mDialog.dismiss();
                rebootTimer.cancel();
                MHandler.exec(mHandler, MHandler.GO_BACK);
            }
        }
    }

    private class NTPTask extends TimerTask {
        private String action;

        public NTPTask() {
            this.action = "GET";
        }

        public NTPTask(String action) {
            this.action = action;
        }

        public void run() {
            try {
                byte[] resData;
                if(action.equals("SET")) {
                    StrUtils.setPktStr(ntpCfg, 0, TCPFrame.CMD_NTPC_REQ.getBytes());
                    ntpCfg[10] = 1;
                    calcCheckSum(ntpCfg);
                    resData = WebUtils.tcpRequest(curDev.ip, ntpCfg);
                } else {
                    resData = WebUtils.tcpRequest(curDev.ip, TCPFrame.getGET_NTPC_REQ());
                }
                String typeStr = StrUtils.getPktStr(resData, 0, 8);
                if (!typeStr.equals(TCPFrame.CMD_NTPC_RSP)) {
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.err_res_packet));
                    Log.e(Dbg._TAG_(), "Error response (Frame: " + typeStr + ")");
                    return;
                } else {
                    ntpCfg = resData;
                }
                MHandler.exec(mHandler, MHandler.UPDATE_NTP);
            } catch (Exception e) {
                MHandler.exec(mHandler, MHandler.GO_BACK, getString(R.string.err_msg_unable_connect));
                e.printStackTrace();
            } finally {
                if(action.equals("SET")) {
                    mDialog.dismiss();
                }
                ntpTimer.cancel();
            }
        }
    }

    private class GetFwVerTask extends TimerTask {
        public void run() {
            try {
                String curFwVer;
                byte[] resData = WebUtils.tcpRequest(curDev.ip, TCPFrame.getFVER_REQ());
                String typeStr = StrUtils.getPktStr(resData, 0, 8);
                if (typeStr.equals(TCPFrame.CMD_FVER_RSP)) {
                    curFwVer = StrUtils.getPktStr(resData, 10, 64) + "-" + resData[90] + String.format("%02d", resData[91]) + "b" + String.format("%02d", resData[92]) + String.format("%02d", resData[93]);
                    int model = (((resData[94] & 0xff) << 8) + (resData[95] & 0xff));
                    Log.e(Dbg._TAG_(), "Model ID: " +  model);
                    curDev.model = String.valueOf(model);
                } else if (typeStr.equals(TCPFrame.CMD_FVER2RSP)) {
                    curFwVer = StrUtils.getPktStr(resData, 10, 64) + "-" + resData[90] + String.format("%02d", resData[91]) + "b" + String.format("%02d", resData[92]) + String.format("%02d", resData[93]);
                    //int model = (((resData[94] & 0xff) << 24) + ((resData[95] & 0xff) << 16) + ((resData[96] & 0xff) << 8) + (resData[97] & 0xff));
                    String model = new String(Arrays.copyOfRange(resData, 98, 162)).split("\u0000")[0];
                    String[] _model = model.split("-");
                    if(_model.length > 1) {
                        if(_model[0].equals("HY") || _model[0].equals("YT")) {
                            // no change;
                        } else { // KT-63511 -> 63511
                            model = _model[1];
                        }
                    }
                    Log.e(Dbg._TAG_(), "Model ID: " + model);
                    curDev.model = String.valueOf(model);
                } else {
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.err_res_packet));
                    Log.e(Dbg._TAG_(), "Error response (Frame: " + typeStr + ")");
                    return;
                }
                MHandler.exec(mHandler, MHandler.UPDATE_FWVER, curFwVer);
            } catch (Exception e) {
                MHandler.exec(mHandler, MHandler.GO_BACK, getString(R.string.err_msg_unable_connect));
                e.printStackTrace();
            } finally {
                getFwVerTimer.cancel();
            }
        }
    }

    private void calcCheckSum(byte[] data) {
        short checkSum = 0;
        for (int index = 0; index < (data.length - 4); index++) {
            checkSum = (short) (checkSum + (short) (data[index] & 0xff));
        }
        data[data.length - 4] = (byte) (checkSum >> 8);
        data[data.length - 3] = (byte) checkSum;
    }

    private class UartTask extends TimerTask {
        private String action;

        public UartTask() {
            this.action = "GET";
        }

        public UartTask(String action) {
            this.action = action;
        }

        public void run() {
            try {
                byte[] resData;
                if(action.equals("SET") || action.equals("SET-0")) {
                    StrUtils.setPktStr(uartCfg, 0, TCPFrame.CMD_UART_REQ.getBytes());
                    uartCfg[10] = 1; // SET
                    uartCfg[11] = 0x1; // uart0 = 1
                    calcCheckSum(uartCfg);
                    resData = WebUtils.tcpRequest(curDev.ip, uartCfg);
                } else if(action.equals("SET-1")) {
                    StrUtils.setPktStr(uartCfg, 0, TCPFrame.CMD_UART_REQ.getBytes());
                    uartCfg[10] = 1; // SET
                    uartCfg[11] = 0x2; // uart0 = 1
                    calcCheckSum(uartCfg);
                    resData = WebUtils.tcpRequest(curDev.ip, uartCfg);
                } else {
                    resData = WebUtils.tcpRequest(curDev.ip, TCPFrame.getGET_UART_REQ());
                }
                String typeStr = StrUtils.getPktStr(resData, 0, 8);
                if (!typeStr.equals(TCPFrame.CMD_UART_RSP)) {
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.err_res_packet));
                    Log.e(Dbg._TAG_(), "Error response (Frame: " + typeStr + ")");
                    return;
                } else {
                    uartCfg = resData;
                }
                MHandler.exec(mHandler, MHandler.UPDATE_UART);
            } catch (Exception e) {
                MHandler.exec(mHandler, MHandler.GO_BACK, getString(R.string.err_msg_unable_connect));
                e.printStackTrace();
            } finally {
                if(action.equals("SET") || action.equals("SET-0") || action.equals("SET-1")) {
                    mDialog.dismiss();
                }
                uartTimer.cancel();
            }
        }
    }

    private static class MsgHandler extends MHandler {
        public MsgHandler(LocalDevSettings activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final LocalDevSettings activity = (LocalDevSettings) super.mActivity.get();
            switch (msg.what) {
                case MHandler.UPDATE_TIME: {
                    if(activity.sioSyncTime != null) {
                        activity.sioSyncTime.setSummary((CharSequence) msg.obj);
                    }
                    if(activity.ceSyncTime != null) {
                        activity.ceSyncTime.setSummary((CharSequence) msg.obj);
                    }
                    break;
                }
                case MHandler.CNT_STATUS: {
                    activity.enCloud.setSummary((CharSequence) msg.obj);
                    break;
                }
                case MHandler.UPDATE_FWVER: {
                    activity.devFwVer.setSummary((CharSequence) msg.obj);
                    activity.onResumeAfter();
                    break;
                }
                case MHandler.UPDATE_NTP: {
                    String ntpServ1 = StrUtils.getPktStr(activity.ntpCfg, 20, 64);
                    String ntpServ2 = StrUtils.getPktStr(activity.ntpCfg, 84, 64);
                    //int pTime = ((activity.ntpCfg[12] & 0xff) << 24) + ((activity.ntpCfg[13] & 0xff) << 16) + ((activity.ntpCfg[14] & 0xff) <<  8) + (activity.ntpCfg[15] & 0xff);
                    //int expire = ((activity.ntpCfg[16] & 0xff) << 24) + ((activity.ntpCfg[17] & 0xff) << 16) + ((activity.ntpCfg[18] & 0xff) <<  8) + (activity.ntpCfg[19] & 0xff);
                    int timezone = ((activity.ntpCfg[148] & 0xff) << 24) + ((activity.ntpCfg[149] & 0xff) << 16) + ((activity.ntpCfg[150] & 0xff) <<  8) + (activity.ntpCfg[151] & 0xff);
                    //int daylight = ((activity.ntpCfg[152] & 0xff) << 24) + ((activity.ntpCfg[153] & 0xff) << 16) + ((activity.ntpCfg[154] & 0xff) <<  8) + (activity.ntpCfg[155] & 0xff);
                    activity.enNTPService.setChecked((activity.ntpCfg[11] == 1) ? true : false);
                    activity.ntpServer1.setText(ntpServ1);
                    activity.ntpServer1.setSummary(ntpServ1);
                    activity.ntpServer2.setText(ntpServ2);
                    activity.ntpServer2.setSummary(ntpServ2);
                    activity.tZone.setValue(Integer.toString(timezone));
                    activity.tZone.setSummary(activity.tZone.getEntry());
                    break;
                }
                case MHandler.UPDATE_APCFG: {
                    activity.enCloud.setChecked(activity.devSetting.enCloudMode);
                    activity.enHTTPS.setChecked(activity.devSetting.enHttps);
                    activity.cloudUrl.setText(activity.devSetting.cloudUrl);
                    activity.cloudUrl.setSummary(activity.devSetting.cloudUrl);
                    activity.cloudPt.setText(String.valueOf(activity.devSetting.cloudPt));
                    activity.cloudPt.setSummary(String.valueOf(activity.devSetting.cloudPt));
                    activity.devName.setText(activity.devSetting.devName);
                    activity.devName.setSummary(activity.devSetting.devName);
                    activity.enStaticIp.setChecked(activity.devSetting.enStaticIp);
                    activity.enTcpSlave.setChecked(activity.devSetting.isSlvTcp);
                    activity.slvTcpPort.setSummary(String.valueOf(activity.devSetting.slvTcpPort));
                    if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_KSMT_TEST_ALL)){
                        activity.devPswd.setSummary(activity.devSetting.devPswd);
                    }
                    break;
                }
                case MHandler.UPDATE_UART: { // uart-1/com-1
                    // For data OP option
                    boolean isMasterDev = activity.curDev.isMbusMaster();
                    int proxyMode = -2; //0: COM0, 1: COM1, -1: none, -2: slave mode
                    if(activity.curDev.isMbusMaster()) {
                        if (activity.uartCfg[19] == 5 || activity.uartCfg[19] == 7) {
                            proxyMode = 0;
                        } else if (activity.uartCfg[27] == 5 || activity.uartCfg[27] == 7) { // Proxy mode
                            proxyMode = 1;
                        } else {
                            proxyMode = -1;
                        }
                    }

                    // COM-0
                    activity.mbUid.setSummary(String.valueOf(activity.uartCfg[12] & 0xff));
                    activity.mbIface.setValue(String.valueOf(activity.uartCfg[14]));
                    activity.mbIface.setSummary(activity.mbIface.getEntry());
                    activity.mbBRate.setValue(String.valueOf(activity.uartCfg[15]));
                    activity.mbBRate.setSummary(activity.mbBRate.getEntry());
                    activity.mbParity.setValue(String.valueOf(activity.uartCfg[16]));
                    activity.mbParity.setSummary(activity.mbParity.getEntry());
                    activity.mbStopBit.setValue(String.valueOf(activity.uartCfg[18]));
                    activity.mbStopBit.setSummary(activity.mbStopBit.getEntry());
                    // COM-0 Set Data OpMode
                    if(proxyMode == -1 || proxyMode == 0) { // COM0 is Proxy mode
                        activity.mbOpMode.setEntries(R.array.proxyOpmodeKey);
                        activity.mbOpMode.setEntryValues(R.array.proxyOpmodeVal);
                    } else if(proxyMode == 1) { // COM1 is Proxy mode
                        activity.mbOpMode.setEntries(R.array.mstOpmodeKey);
                        activity.mbOpMode.setEntryValues(R.array.mstOpmodeVal);
                    }
                    activity.mbOpMode.setValue(String.valueOf(activity.uartCfg[19]));
                    activity.mbOpMode.setSummary(activity.mbOpMode.getEntry());
                    // COM-0 Set DataLen
                    if(activity.uartCfg[19] == 0) { // RTU-Slave mode
                        activity.mbDataLen.setEntries(R.array.mbDataLenKey8);
                        activity.mbDataLen.setEntryValues(R.array.mbDataLenVal8);
                    } else {
                        activity.mbDataLen.setEntries(R.array.mbDataLenKey);
                        activity.mbDataLen.setEntryValues(R.array.mbDataLenVal);
                    }
                    activity.mbDataLen.setValue(String.valueOf(activity.uartCfg[17]));
                    activity.mbDataLen.setSummary(activity.mbDataLen.getEntry());

                    // COM-1
                    activity.mbUid1.setSummary(String.valueOf(activity.uartCfg[20] & 0xff));
                    activity.mbIface1.setValue(String.valueOf(activity.uartCfg[22]));
                    activity.mbIface1.setSummary(activity.mbIface1.getEntry());
                    activity.mbBRate1.setValue(String.valueOf(activity.uartCfg[23]));
                    activity.mbBRate1.setSummary(activity.mbBRate1.getEntry());
                    activity.mbParity1.setValue(String.valueOf(activity.uartCfg[24]));
                    activity.mbParity1.setSummary(activity.mbParity1.getEntry());
                    activity.mbStopBit1.setValue(String.valueOf(activity.uartCfg[26]));
                    activity.mbStopBit1.setSummary(activity.mbStopBit1.getEntry());

                    // COM-1 Set Data OPMode
                    if(proxyMode == 0) { // COM0 is Proxy mode
                        activity.mbOpMode1.setEntries(R.array.mstOpmodeKey);
                        activity.mbOpMode1.setEntryValues(R.array.mstOpmodeVal);
                    } else if(proxyMode == -1 || proxyMode == 1){ // COM1 is Proxy mode
                        activity.mbOpMode1.setEntries(R.array.proxyOpmodeKey);
                        activity.mbOpMode1.setEntryValues(R.array.proxyOpmodeVal);
                    }
                    activity.mbOpMode1.setValue(String.valueOf(activity.uartCfg[27]));
                    activity.mbOpMode1.setSummary(activity.mbOpMode1.getEntry());

                    // COM-1 Set Data Length
                    if(activity.uartCfg[27] == 0) { // RTU-Slave mode
                        activity.mbDataLen1.setEntries(R.array.mbDataLenKey8);
                        activity.mbDataLen1.setEntryValues(R.array.mbDataLenVal8);
                    } else {
                        activity.mbDataLen1.setEntries(R.array.mbDataLenKey);
                        activity.mbDataLen1.setEntryValues(R.array.mbDataLenVal);
                    }
                    activity.mbDataLen1.setValue(String.valueOf(activity.uartCfg[25]));
                    activity.mbDataLen1.setSummary(activity.mbDataLen.getEntry());
                    break;
                }
                default: {
                    super.handleMessage(msg);
                }
            }
        }
    }

    private void copySettings(DevSetting from, DevSetting to){
        to.devName = from.devName;
        to.enCloudMode = from.enCloudMode;
        to.enHttps = from.enHttps;
        to.enStaticIp = from.enStaticIp;
        to.ipAddr = from.ipAddr;
        to.submask = from.submask;
        to.gateway = from.gateway;
        to.dns = from.dns;
        to.cloudPt = from.cloudPt;
        to.cloudUrl = from.cloudUrl;
        to.isSlvTcp = from.isSlvTcp;
        to.slvTcpPort = from.slvTcpPort;
    }

    class DevSetting {
        public String devName;
        public String devPswd;
        public String cloudUrl;
        public int cloudPt;
        public boolean enCloudMode;
        public boolean enHttps;
        public boolean enStaticIp;
        public String ipAddr;
        public String submask;
        public String gateway;
        public String dns;
        public boolean isSlvTcp;
        public int slvTcpPort;
    }
}
