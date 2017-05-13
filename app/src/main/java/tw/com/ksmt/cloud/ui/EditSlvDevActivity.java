package tw.com.ksmt.cloud.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Device;
import tw.com.ksmt.cloud.iface.ListItem;
import tw.com.ksmt.cloud.iface.MstDev;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.StrUtils;
import tw.com.ksmt.cloud.libs.Utils;

public class EditSlvDevActivity extends ActionBarActivity implements AdapterView.OnItemClickListener, PullToRefreshBase.OnRefreshListener2<ListView> {
    private Context context = EditSlvDevActivity.this;
    private ActionBar actionBar;
    private Timer pollingTimer;
    private Timer saveTimer;
    private MsgHandler mHandler;
    private ProgressDialog mDialog;
    private EditSlvDevAdapter adapter;
    private PullToRefreshListView prListView;
    private ListView listView;
    protected Device curDevice;
    protected MstDev curMstDev;
    protected MstDev origMstDev;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        setContentView(R.layout.pull_list_view);
        setTitle(getString(R.string.edit_slave_device));
        curDevice = (Device) getIntent().getSerializableExtra("Device");
        curMstDev = (MstDev) getIntent().getSerializableExtra("MstDev");
        mHandler = new MsgHandler(this);
        mDialog = Kdialog.getProgress(context, mDialog);

        // Now find the PullToRefreshLayout to setup
        prListView = (PullToRefreshListView) findViewById(R.id.listView);
        prListView.setOnRefreshListener(this);

        // Actual ListView
        adapter = new EditSlvDevAdapter(context, this);
        listView = prListView.getRefreshableView();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
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
    }

    @Override
    public void onResume() {
        super.onResume();
        setPollingTimer();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AppSettings.reload(context);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Log.e(Dbg._TAG_(), "pos: " + position + ", id: " + id);
        if(curDevice.enlog) {
            Kdialog.getDefInfoDialog(context).setMessage(getString(R.string.usb_logging)).show();
            return;
        }
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
            if (listItem.key.equals(context.getString(R.string.slave_port))) {
                editText.setHint(getString(R.string.slave_port_hint));
            } else if (listItem.key.equals(context.getString(R.string.delay_poll))) {
                editText.setHint(getString(R.string.range) + ": 0 ~ 60000");
            } else if (listItem.key.equals(context.getString(R.string.poll_timeout))) {
                editText.setHint(getString(R.string.range) + ": 100 ~ 60000");
            } else if (listItem.key.equals(context.getString(R.string.max_retry))) {
                editText.setHint(getString(R.string.range) + ": 1 ~ 1000");
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
                            if (noError) {
                                try {
                                    if (listItem.key.equals(context.getString(R.string.slave_dev_name))) {
                                        if(!StrUtils.validateInput(StrUtils.IN_TYPE_STRING, input)) {
                                            throw new Exception();
                                        }
                                        curMstDev.name = input;
                                        setSaveTimer();
                                    } else if (listItem.key.equals(context.getString(R.string.slave_ip_hinet))) {
                                        if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, input)) {
                                            throw new Exception();
                                        } else if(!StrUtils.validateInput(StrUtils.IN_TYPE_IPV4, input)) {
                                            throw new Exception();
                                        }
                                        curMstDev.ip = input;
                                        setSaveTimer();
                                    } else if (listItem.key.equals(context.getString(R.string.slave_port))) {
                                        if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, input)) {
                                            throw new Exception();
                                        } else if(!StrUtils.validateInput(StrUtils.IN_TYPE_IP_PORT, input)) {
                                            throw new Exception();
                                        }
                                        curMstDev.port = Integer.parseInt(input);
                                        setSaveTimer();
                                    } else if (listItem.key.equals(context.getString(R.string.poll_timeout))) {
                                        if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, input)) {
                                            throw new Exception();
                                        }
                                        int pollTimeout = Integer.parseInt(input);
                                        if (pollTimeout < 100 || pollTimeout > 60000) {
                                            throw new Exception();
                                        }
                                        curMstDev.timeout = pollTimeout;
                                        setSaveTimer();
                                    } else if (listItem.key.equals(context.getString(R.string.delay_poll))) {
                                        if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, input)) {
                                            throw new Exception();
                                        }
                                        int delayPoll = Integer.parseInt(input);
                                        if (delayPoll < 0 || delayPoll > 60000) {
                                            throw new Exception();
                                        }
                                        curMstDev.delayPoll = delayPoll;
                                        setSaveTimer();
                                    } else if (listItem.key.equals(context.getString(R.string.max_retry))) {
                                        if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, input)) {
                                            throw new Exception();
                                        }
                                        int maxRetry = Integer.parseInt(input);
                                        if (maxRetry < 1 || maxRetry > 1000) {
                                            throw new Exception();
                                        }
                                        curMstDev.maxRetry = maxRetry;
                                        setSaveTimer();
                                    }
                                    dialog.dismiss();
                                } catch (Exception e) {
                                    if (listItem.key.equals(context.getString(R.string.slave_dev_name))) {
                                        editText.setError(getString(R.string.err_msg_invalid_str));
                                    } else if (listItem.key.equals(context.getString(R.string.slave_ip_hinet))) {
                                        editText.setError(getString(R.string.err_msg_invalid_ip));
                                    } else if (listItem.key.equals(context.getString(R.string.slave_port))) {
                                        editText.setError(getString(R.string.slave_port_hint));
                                    } else if (listItem.key.equals(context.getString(R.string.delay_poll))) {
                                        editText.setError(getString(R.string.range) + ": 100 ~ 60000");
                                    } else if (listItem.key.equals(context.getString(R.string.poll_timeout))) {
                                        editText.setError(getString(R.string.range) + ": 100 ~ 60000");
                                    } else if (listItem.key.equals(context.getString(R.string.max_retry))) {
                                        editText.setError(getString(R.string.range) + ": 1 ~ 1000");
                                    }
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                }
            });
            dialog.show();
        } else if(listItem.type == ListItem.CHECKBOX) {
            if (listItem.key.equals(context.getString(R.string.enable))) {
                curMstDev.enable = !curMstDev.enable;
                setSaveTimer();
            }
        } else if(listItem.type == ListItem.SELECTION) {
            final List<String> itemList = new LinkedList<String>();
            if (listItem.key.equals(context.getString(R.string.connect_type))) {
                if(curMstDev.type.equals("TCP")) {
                    itemList.add("Serial");
                } else {
                    itemList.add("TCP");
                }
            } else if (listItem.key.equals(context.getString(R.string.serial_port))) {
                if(curMstDev.comPort.equals("COM0")) {
                    itemList.add("COM1");
                } else if(curMstDev.comPort.equals("COM1")){
                    itemList.add("COM0");
                }
            } else if (listItem.key.equals(context.getString(R.string.slave_id))) {
                if(curMstDev.type.equals("TCP")) {
                    itemList.add(getString(R.string.none));
                }
                for(int i = 1; i < 255; i++) {
                    if(curMstDev.slvId == i) {
                        continue;
                    }
                    itemList.add(String.valueOf(i));
                }
            }
            final String[] itemArray = new String[itemList.size()];
            itemList.toArray(itemArray);

            final AlertDialog dialog;
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setIcon(R.drawable.ic_editor);
            builder.setTitle(listItem.key);
            builder.setSingleChoiceItems(itemArray, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (listItem.key.equals(context.getString(R.string.connect_type))) {
                        curMstDev.name = itemArray[which];
                    } else if (listItem.key.equals(context.getString(R.string.serial_port))) {
                        curMstDev.comPort = itemArray[which];
                    } else if (listItem.key.equals(context.getString(R.string.slave_id))) {
                        if(itemArray[which].equals(getString(R.string.none))) {
                            curMstDev.slvId = 255;
                        } else {
                            curMstDev.slvId = Integer.parseInt(itemArray[which]);
                        }
                    }
                    setSaveTimer();
                    dialog.dismiss();
                }
            }).setNegativeButton(getString(R.string.cancel), null);
            dialog = builder.create();
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

    private class SaveTimerTask extends TimerTask {
        public void run() {
            try {
                JSONObject jObject = JSONReq.multipart(context, "POST", PrjCfg.CLOUD_URL + "/api/slvdev/update", curMstDev.toMultiPart(curDevice, origMstDev));
                int code = jObject.getInt("code");
                //Log.e(Dbg._TAG_(), jObject.toString());
                if(code == 200) {
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_save));
                } else {
                    mDialog.dismiss();
                    if(!jObject.isNull("desc") && jObject.getString("desc").contains("Device is logging")) {
                        MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.usb_logging));
                    } else if(!jObject.isNull("desc") && jObject.getString("desc").trim().matches("Invalid Data")) {
                        MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, context.getString(R.string.err_msg_invalid_str));
                    } else if(!jObject.isNull("desc") && jObject.getString("desc").trim().matches("The slave setting has been used")) {
                        MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, context.getString(R.string.err_msg_dup_slvid));
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
                JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/slvdev/" + curDevice.sn + "?id=" + curMstDev.id);
                if (jObject == null || jObject.getInt("code") != 200) {
                    MHandler.exec(mHandler, MHandler.GO_BACK, getString(R.string.err_get_account));
                    return;
                }
                JSONObject jMstDev = jObject.getJSONObject("slvDev");
                curMstDev = new MstDev(curMstDev.id, jMstDev);
                origMstDev = (MstDev) curMstDev.clone();
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
        public MsgHandler(EditSlvDevActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            EditSlvDevActivity activity = (EditSlvDevActivity) super.mActivity.get();
            switch (msg.what) {
                case MHandler.UPDATE: {
                    activity.adapter.addList(activity.curMstDev);
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