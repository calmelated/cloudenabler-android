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
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Device;
import tw.com.ksmt.cloud.iface.ListItem;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.StrUtils;
import tw.com.ksmt.cloud.libs.Utils;

public class EditDeviceActivity extends ActionBarActivity implements AdapterView.OnItemClickListener, PullToRefreshBase.OnRefreshListener2<ListView> {
    private Context context = EditDeviceActivity.this;
    private ActionBar actionBar;
    private Timer pollingTimer;
    private Timer saveTimer;
    private Timer rebootTimer;
    private Timer sendLogTimer;
    private Timer fwupgTimer;
    private MsgHandler mHandler;
    private ProgressDialog mDialog;
    private EditDeviceAdapter adapter;
    private PullToRefreshListView prListView;
    private ListView listView;
    protected Device curDevice;  // put new setting in this structure
    protected Device origDevice; // keep the old setting

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        setContentView(R.layout.pull_list_view);
        setTitle(getString(R.string.edit_device));
        curDevice = (Device) getIntent().getSerializableExtra("Device");
        mHandler = new MsgHandler(this);
        mDialog = Kdialog.getProgress(context, mDialog);

        // Now find the PullToRefreshLayout to setup
        prListView = (PullToRefreshListView) findViewById(R.id.listView);
        prListView.setOnRefreshListener(this);

        // Actual ListView
        adapter = new EditDeviceAdapter(context, this);
        listView = prListView.getRefreshableView();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("curDevice", curDevice);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AppSettings.reload(context);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (pollingTimer != null) {
            pollingTimer.cancel();
        }
        if (mDialog != null) {
            mDialog.cancel();
        }
        if (saveTimer != null) {
            saveTimer.cancel();
        }
        if (sendLogTimer != null) {
            sendLogTimer.cancel();
        }
        if (fwupgTimer != null) {
            fwupgTimer.cancel();
        }
        if (rebootTimer != null) {
            rebootTimer.cancel();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setPollingTimer();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Log.e(Dbg._TAG_(), "pos: " + position + ", id: " + id);
        final ListItem listItem = (ListItem) adapter.getItem(position - 1);
        if(listItem.type == ListItem.INPUT || listItem.type == ListItem.NUMBER || listItem.type == ListItem.LONG_INPUT) {
            int layout;
            if(listItem.type == ListItem.INPUT) {
                layout = R.layout.input_text;
            } else if(listItem.type == ListItem.LONG_INPUT) {
                layout = R.layout.input_long_text;
            } else {
                layout = R.layout.input_number;
            }
            final LayoutInflater inflater = LayoutInflater.from(context);
            final View diagView = inflater.inflate(layout, null);
            final EditText editText = (EditText) diagView.findViewById(R.id.text);
            editText.setFilters(Utils.arrayMerge(editText.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
            editText.setText(listItem.value);
            if (listItem.key.equals(context.getString(R.string.logging_freq))) {
                if(curDevice.enlog) {
                    Kdialog.getDefInfoDialog(context).setMessage(getString(R.string.usb_logging)).show();
                    return;
                }
                editText.setHint(getString(R.string.logging_freq_hint));
            } else if (listItem.key.equals(context.getString(R.string.storage_capacity))) {
                editText.setHint(getString(R.string.storage_capacity_hint));
            } else if (listItem.key.equals(context.getString(R.string.ftp_cli_port))) {
                editText.setHint(getString(R.string.ftp_cli_port_hint));
            } else if (listItem.key.equals(context.getString(R.string.mbus_timeout))) {
                editText.setHint(getString(R.string.mbus_timeout_hint));
            }
            final AlertDialog dialog = Kdialog.getDefInputDialog(context).create();
            dialog.setView(diagView);
            dialog.setTitle(listItem.key);
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogIface) {
                    Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            boolean noError = true;
                            String input = editText.getText().toString().trim();
                            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, input)) {
                                noError = false;
                                editText.setError(getString(R.string.err_msg_empty));
                            }
                            if (listItem.key.equals(context.getString(R.string.device_extra_1)) ||
                                listItem.key.equals(context.getString(R.string.device_extra_2)) ||
                                listItem.key.equals(context.getString(R.string.device_extra_3))) {
                                if(!StrUtils.validateInput(StrUtils.IN_TYPE_STRING, input)) {
                                    noError = false;
                                    editText.setError(getString(R.string.err_msg_invalid_str));
                                }
                            }
                            if (noError) {
                                try {
                                    if (listItem.key.equals(context.getString(R.string.device_name))) {
                                        if(!StrUtils.validateInput(StrUtils.IN_TYPE_STRING, input)) {
                                            throw new Exception();
                                        }
                                        curDevice.name = input;
                                        setSaveTimer();
                                    } else if (listItem.key.equals(context.getString(R.string.storage_capacity))) {
                                        int capacity = Integer.parseInt(input);
                                        if(capacity >= 80 && capacity <= 100) {
                                            curDevice.storCapacity = capacity;
                                            setSaveTimer();
                                        } else {
                                            throw new Exception();
                                        }
                                    } else if (listItem.key.equals(context.getString(R.string.mbus_timeout))) {
                                        int mbusTimeout = Integer.parseInt(input);
                                        if(mbusTimeout >= 10 && mbusTimeout <= 3600) {
                                            curDevice.mbusTimeout = mbusTimeout;
                                            setSaveTimer();
                                        } else {
                                            throw new Exception();
                                        }
                                    } else if (listItem.key.equals(context.getString(R.string.device_polling))) {
                                        curDevice.pollTime = Integer.parseInt(input);
                                        setSaveTimer();
                                    } else if (listItem.key.equals(context.getString(R.string.ftp_cli_host))) {
                                        curDevice.ftpCliHost = input;
                                        setSaveTimer();
                                    } else if (listItem.key.equals(context.getString(R.string.device_extra_1))) {
                                        curDevice.url_1 = input;
                                        setSaveTimer();
                                    } else if (listItem.key.equals(context.getString(R.string.device_extra_2))) {
                                        curDevice.url_2 = input;
                                        setSaveTimer();
                                    } else if (listItem.key.equals(context.getString(R.string.device_extra_3))) {
                                        curDevice.url_3 = input;
                                        setSaveTimer();
                                    } else if (listItem.key.equals(context.getString(R.string.ftp_cli_account))) {
                                        curDevice.ftpCliAccount = input;
                                        setSaveTimer();
                                    } else if (listItem.key.equals(context.getString(R.string.ftp_cli_port))) {
                                        int port = Integer.parseInt(input);
                                        if(port > 0 && port < 65536) {
                                            curDevice.ftpCliPort = port;
                                            setSaveTimer();
                                        } else {
                                            throw new Exception();
                                        }
                                    } else if (listItem.key.equals(context.getString(R.string.logging_freq))) {
                                        curDevice.logFreq = Integer.parseInt(input);
                                        if(curDevice.logFreq < 1 || curDevice.logFreq > 3600) {
                                            throw new Exception();
                                        } else if(curDevice.logFreq < 10) {
                                            Kdialog.getMakeSureDialog(context, getString(R.string.log_freq_too_small))
                                            .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    setSaveTimer();
                                                }
                                            }).show();
                                        } else {
                                            setSaveTimer();
                                        }
                                    }
                                    dialog.dismiss();
                                } catch (Exception e) {
                                    if (listItem.key.equals(context.getString(R.string.logging_freq))) {
                                        editText.setError(getString(R.string.logging_freq_hint));
                                    } else if(listItem.key.equals(context.getString(R.string.mbus_timeout))) {
                                        editText.setError(getString(R.string.mbus_timeout_hint));
                                    } else if(listItem.key.equals(context.getString(R.string.device_name))) {
                                        editText.setError(getString(R.string.err_msg_invalid_str));
                                    } else {
                                        editText.setError(getString(R.string.invalid_val));
                                    }
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                }
            });
            dialog.show();
        } else if(listItem.type == ListItem.BUTTON) {
            if (listItem.key.equals(context.getString(R.string.ftp_cli_send_log))) {
                Kdialog.getMakeSureDialog(context, getString(R.string.make_sure_send_log))
                .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setSendLogTimer();
                    }
                }).show();
            } else if (listItem.key.equals(context.getString(R.string.reboot))) {
                Kdialog.getMakeSureDialog(context, getString(R.string.make_sure_dev_reboot))
                .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setRebootTimer();
                    }
                }).show();
            } else if (listItem.key.equals(context.getString(R.string.device_fw_upgrade))) {
                Kdialog.getMakeSureDialog(context, getString(R.string.make_sure_fwupg))
                .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setFwupgTimer("kt-6351x");
                    }
                }).show();
            } else if (listItem.key.equals(context.getString(R.string.stm32_upgrade))) {
                Kdialog.getMakeSureDialog(context, getString(R.string.make_sure_fwupg))
                .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setFwupgTimer("kt-stm32");
                    }
                }).show();
            }
        } else if(listItem.type == ListItem.CHECKBOX) {
            if (listItem.key.equals(context.getString(R.string.enable_usb_loggoing))) {
                curDevice.enlog = !curDevice.enlog;
                setSaveTimer();
            } else if (listItem.key.equals(context.getString(R.string.enable_server_loggoing))) {
                curDevice.enServLog = !curDevice.enServLog;
                setSaveTimer();
            } else if (listItem.key.equals(context.getString(R.string.ftp_cli_enable))) {
                curDevice.enFtpCli = !curDevice.enFtpCli;
                setSaveTimer();
            }
        } else if(listItem.type == ListItem.SELECTION && listItem.key.equals(context.getString(R.string.device_model))) {
            AlertDialog.Builder selectModel = new AlertDialog.Builder(context);
            selectModel.setIcon(R.drawable.ic_editor);
            selectModel.setTitle(listItem.key);

            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(context, android.R.layout.select_dialog_singlechoice);
            arrayAdapter.addAll(Device.getAllTypes(context));
            selectModel.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    curDevice.model = Device.getTypeIdx(context, arrayAdapter.getItem(which));
                    setSaveTimer();
                }
            }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).show();
        } else if(listItem.type == ListItem.PASSWORD) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            final View layout = inflater.inflate(R.layout.input_password, null);
            final EditText editPassword = (EditText) layout.findViewById(R.id.password);
            editPassword.setFilters(Utils.arrayMerge(editPassword.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));

            final AlertDialog dialog = Kdialog.getDefInputDialog(context).create();
            dialog.setView(layout);
            dialog.setTitle(listItem.key);
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogIface) {
                    Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Boolean noError = true;
                            String password = editPassword.getText().toString().trim();
                            if (noError) {
                                dialog.dismiss();
                                if (listItem.key.equals(context.getString(R.string.ftp_cli_password))) {
                                    curDevice.ftpCliPswd = password;
                                }
                                setSaveTimer();
                            }
                        }
                    });
                }
            });
            dialog.show();
        } else if(listItem.type == ListItem.NEW_PASSWORD) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            final View layout = inflater.inflate(R.layout.input_new_password, null);
            final EditText editPassword = (EditText) layout.findViewById(R.id.password);
            final EditText editConfimPassword = (EditText) layout.findViewById(R.id.confimPassword);
            editPassword.setFilters(Utils.arrayMerge(editPassword.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
            editConfimPassword.setFilters(Utils.arrayMerge(editConfimPassword.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));

            final AlertDialog dialog = Kdialog.getDefInputDialog(context).create();
            dialog.setView(layout);
            dialog.setTitle(listItem.key);
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
                            } else if(!password.equals(confirmPassword)) {
                                noError = false;
                                editPassword.setError(getString(R.string.err_msg_password_not_equal));
                            }
                            if (noError) {
                                dialog.dismiss();
                                if (listItem.key.equals(context.getString(R.string.ftp_srv_password))) {
                                    curDevice.ftpPswd = password;
                                }
                                setSaveTimer();
                            }
                        }
                    });
                }
            });
            dialog.show();
        }
    }

    @Override
    public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
        //Log.e(Dbg._TAG_(), "onRefresh.. ");
        if(pollingTimer != null) {
            pollingTimer.cancel();
        }
        setPollingTimer();
    }

    @Override
    public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
    }


    public void setPollingTimer() {
        MHandler.exec(mHandler, MHandler.CLEAR_LIST);
        pollingTimer = new Timer();
        pollingTimer.schedule(new PollingTimerTask(), 0, PrjCfg.RUN_ONCE);
    }

    public void setSaveTimer() {
        mDialog = Kdialog.getProgress(context, mDialog);
        saveTimer = new Timer();
        saveTimer.schedule(new SaveTimerTask(), 0, PrjCfg.RUN_ONCE);
    }

    public void setRebootTimer() {
        mDialog = Kdialog.getProgress(context, mDialog);
        rebootTimer = new Timer();
        rebootTimer.schedule(new DevRebootTimerTask(), 0, PrjCfg.RUN_ONCE);
    }

    public void setSendLogTimer() {
        mDialog = Kdialog.getProgress(context, mDialog);
        sendLogTimer = new Timer();
        sendLogTimer.schedule(new SendLogTimerTask(), 0, PrjCfg.RUN_ONCE);
    }

    public void setFwupgTimer(String type) {
        mDialog = Kdialog.getProgress(context, mDialog);
        fwupgTimer = new Timer();
        fwupgTimer.schedule(new FwupgTimerTask(type), 0, PrjCfg.RUN_ONCE);
    }

    private class DevRebootTimerTask extends TimerTask {
        public void run() {
            try {
                JSONObject jObject = JSONReq.send(context, "PUT", PrjCfg.CLOUD_URL + "/api/device/reboot/" + curDevice.sn, null);
                if (jObject.getInt("code") == 200) {
                    MHandler.exec(mHandler, MHandler.SHOW_MSG, getString(R.string.dev_received_reboot_req));
                } else if (jObject.getInt("code") == 408) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.dev_no_received_reboot_req));
                } else {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_unable_update));
                }
            } catch (Exception e) {
                if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
                }
                e.printStackTrace();
            } finally {
                mDialog.dismiss();
                rebootTimer.cancel();
            }
        }
    }

    private class SendLogTimerTask extends TimerTask {
        public void run() {
            try {
                JSONObject jObject = JSONReq.send(context, "PUT", PrjCfg.CLOUD_URL + "/api/device/ftplog/" + curDevice.sn, null);
                if (jObject.getInt("code") == 200) {
                    MHandler.exec(mHandler, MHandler.SHOW_MSG, getString(R.string.dev_received_ftp_req));
                } else if (jObject.getInt("code") == 408) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.dev_no_received_reboot_req));
                } else {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_unable_update));
                }
            } catch (Exception e) {
                if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
                }
                e.printStackTrace();
            } finally {
                mDialog.dismiss();
                sendLogTimer.cancel();
            }
        }
    }

    private class FwupgTimerTask extends TimerTask {
        private String type;

        public FwupgTimerTask(String type) {
            this.type = type;
        }

        public void run() {
            try {
                JSONObject jObject = JSONReq.send(context, "PUT", PrjCfg.CLOUD_URL + "/api/device/fwupg/" + curDevice.sn + "?type=" + type, null);
                if (jObject.getInt("code") == 200) {
                    MHandler.exec(mHandler, MHandler.SHOW_MSG, getString(R.string.received_fwupg_req));
                } else if (jObject.getInt("code") == 400) {
                    if(jObject.getString("desc").contains("Already the latest version")) {
                        MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_lastest_version));
                    } else {
                        MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_unable_update));
                    }
                } else if (jObject.getInt("code") == 408) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.no_received_fwupg_req));
                } else {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_unable_update));
                }
            } catch (Exception e) {
                if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
                }
                e.printStackTrace();
            } finally {
                mDialog.dismiss();
                fwupgTimer.cancel();
            }
        }
    }

    private class SaveTimerTask extends TimerTask {
        public void run() {
            try {
                JSONObject jObject = JSONReq.multipart(context, "PUT", PrjCfg.CLOUD_URL + "/api/device/edit", curDevice.toMultiPart(origDevice));
                int code = jObject.getInt("code");
                if(code == 200) {
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_save));
                } else {
                    mDialog.dismiss();
                    if(!jObject.isNull("desc") && jObject.getString("desc").contains("Device is logging")) {
                        MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.usb_logging));
                    } else if(!jObject.isNull("desc") && jObject.getString("desc").contains("No any logging register")) {
                        MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_no_logging_register));
                    } else {
                        MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_save));
                    }
                }
                setPollingTimer();
            } catch (Exception e) {
                if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
                }
                e.printStackTrace();
            } finally {
                saveTimer.cancel();
            }
        }
    }

    private class PollingTimerTask extends TimerTask {
        public void run() {
            try {
                JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/device/" + curDevice.sn);
                if (jObject == null || jObject.getInt("code") != 200) {
                    MHandler.exec(mHandler, MHandler.GO_BACK, getString(R.string.err_get_data));
                    return;
                }
                JSONObject jDevice = jObject.getJSONObject("device");
                Device newDev = new Device(jDevice);
                if(curDevice.isMbusMaster() && curDevice.slvIdx > 0) {
                    newDev.slvIdx = curDevice.slvIdx;
                    newDev.slvDev = curDevice.slvDev;
                }
                curDevice = newDev;
                origDevice = (Device) curDevice.clone();
                //Log.e(Dbg._TAG_(), "poll " + origDevice.pollTime + ", ftpps = " + origDevice.ftpCliPswd);
                MHandler.exec(mHandler, MHandler.UPDATE);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                pollingTimer.cancel();
                mDialog.dismiss();
            }
        }
    }

    private static class MsgHandler extends MHandler {
        public MsgHandler(EditDeviceActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            EditDeviceActivity activity = (EditDeviceActivity) super.mActivity.get();
            switch (msg.what) {
                case MHandler.UPDATE: {
                    activity.adapter.addList(activity.curDevice);
                    activity.adapter.notifyDataSetChanged();
                    activity.prListView.onRefreshComplete();
                    break;
                }
                case MHandler.CLEAR_LIST: {
                    activity.adapter.clearList();
                    break;
                }
                default: {
                    super.handleMessage(msg);
                }
            }
        }
    }

}