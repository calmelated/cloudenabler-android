package tw.com.ksmt.cloud.ui;

import android.app.Activity;
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
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import org.json.JSONArray;
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
import tw.com.ksmt.cloud.iface.Register;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.StrUtils;
import tw.com.ksmt.cloud.libs.Utils;

public class EditRegActivity extends ActionBarActivity implements AdapterView.OnItemClickListener, PullToRefreshBase.OnRefreshListener2<ListView> {
    private Context context = EditRegActivity.this;
    private ActionBar actionBar;
    private static final int IOSW = 1;
    private Timer pollingTimer;
    private Timer saveTimer;
    private Timer authTimer;
    private Timer getRegTimer;
    private MsgHandler mHandler;
    private ProgressDialog mDialog;
    private EditRegAdapter adapter;
    private PullToRefreshListView prListView;
    private ListView listView;
    protected boolean enLog;
    protected String curSn;
    protected String curId;
    protected int curSlvIdx;
    protected int logFreq;
    protected Register curRegister;
    private Device ioswSrcDev;
    private Register ioswSrcReg;
    private JSONArray jAuthMembers = null;
    private JSONArray jModbus = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        setContentView(R.layout.pull_list_view);
        setTitle(getString(R.string.edit_register));
        Intent intent = getIntent();
        curId = intent.getStringExtra("curId");
        curSn = intent.getStringExtra("curSn");
        curSlvIdx = intent.getIntExtra("curSlvIdx", 0);
        logFreq = intent.getIntExtra("logFreq", PrjCfg.DEV_LOG_FREQ);
        mHandler = new MsgHandler(this);
        mDialog = Kdialog.getProgress(context, mDialog);

        // Now find the PullToRefreshLayout to setup
        prListView = (PullToRefreshListView) findViewById(R.id.listView);
        prListView.setOnRefreshListener(this);

        // Actual ListView
        adapter = new EditRegAdapter(context, this);
        listView = prListView.getRefreshableView();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
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
        if (authTimer != null) {
            authTimer.cancel();
        }
        if (getRegTimer != null) {
            getRegTimer.cancel();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setPollingTimer();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(requestCode != IOSW) {
            return;
        }
        if(intent == null) {
            return;
        }
        ioswSrcDev = (Device) intent.getSerializableExtra("ioswSrcDev");
        ioswSrcReg = (Register) intent.getSerializableExtra("ioswSrcReg");
        if(ioswSrcDev == null || ioswSrcReg == null) {
            return;
        }
        String slvIdx = (ioswSrcDev.slvIdx > 0) ? String.valueOf(ioswSrcDev.slvIdx) : "" ;
        curRegister.swSN = ioswSrcDev.sn;
        if(Register.is32Bits(curRegister.type)  ||
           Register.is48Bits(curRegister.type)  ||
           Register.is64Bits(curRegister.type)) {
            curRegister.swAddr = slvIdx + ioswSrcReg.haddr + "-" + slvIdx + ioswSrcReg.laddr;
        } else {
            curRegister.swAddr = slvIdx + ioswSrcReg.haddr;
        }
        setSaveTimer();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Log.e(Dbg._TAG_(), "pos: " + position + ", id: " + id);
        final ListItem listItem = (ListItem) adapter.getItem(position - 1);
        if(listItem.type == ListItem.INPUT || listItem.type == ListItem.NUMBER) {
            int layout = (listItem.type == ListItem.INPUT) ? R.layout.input_text : R.layout.input_number;
            final LayoutInflater inflater = LayoutInflater.from(context);
            final View diagView = inflater.inflate(layout, null);
            final EditText editText = (EditText) diagView.findViewById(R.id.text);
            editText.setFilters(Utils.arrayMerge(editText.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
            if (listItem.key.equals(context.getString(R.string.switch_on_hint))   ||
                listItem.key.equals(context.getString(R.string.switch_off_hint))  ||
                listItem.key.equals(context.getString(R.string.btn_press_hint))   ||
                listItem.key.equals(context.getString(R.string.btn_release_hint))) {
                //editText.setText(Short.toString((short) Integer.parseInt(listItem.value, 16)));
                editText.setText(Integer.toString(Integer.parseInt(listItem.value, 16)));
                editText.setHint(getString(R.string.range) + ": " + Register.getValidRange(curRegister));
            } else if(listItem.key.equals(context.getString(R.string.equation))) {
                if(!listItem.value.equals(getString(R.string.none))) {
                    editText.setText(listItem.value);
                }
                editText.setHint(getString(R.string.equation_message));
                final TextView txtNote = (TextView) diagView.findViewById(R.id.note);
                txtNote.setVisibility(View.VISIBLE);
                txtNote.setText(getString(R.string.equation_warning));
            } else if(listItem.key.equals(context.getString(R.string.floating_points))) {
                editText.setText(listItem.value);
                if(Register.is48Bits(curRegister.type)) {
                    editText.setHint(getString(R.string.floating_points_48_hint));
                } else {
                    editText.setHint(getString(R.string.floating_points_hint));
                }
            } else if(listItem.key.equals(context.getString(R.string.btn_time))) {
                editText.setText(listItem.value);
                editText.setHint(getString(R.string.range) + ": 1 ~ 60");
            } else if(listItem.key.equals(context.getString(R.string.iosw_source))) {
                Intent intent = new Intent(context, IOSWActivity.class);
                intent.putExtra("Register", curRegister);
                startActivityForResult(intent, IOSW);
                return;
            } else if(listItem.key.equals(context.getString(R.string.upbound_alarm))  ||
                      listItem.key.equals(context.getString(R.string.lowbound_alarm)) ||
                      listItem.key.equals(context.getString(R.string.max))            ||
                      listItem.key.equals(context.getString(R.string.min))) {
                if(!listItem.value.equals(getString(R.string.none))) {
                    editText.setText(listItem.value);
                }
                editText.setHint(getString(R.string.range) + ": " + Register.getValidRange(curRegister));
            } else {
                editText.setText(listItem.value);
                //editText.setHint(getString(R.string.device_name_hint));
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
                            boolean allowEmpty = false;
                            String input = editText.getText().toString().trim();
                            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, input)) {
                                // if empty input -> disable alarm
                                if (listItem.key.equals(context.getString(R.string.upbound_alarm))  ||
                                    listItem.key.equals(context.getString(R.string.lowbound_alarm)) ||
                                    listItem.key.equals(context.getString(R.string.max)) ||
                                    listItem.key.equals(context.getString(R.string.min)) ||
                                    listItem.key.equals(context.getString(R.string.equation))) {
                                    allowEmpty = true;
                                } else {
                                    noError = false;
                                    editText.setError(getString(R.string.err_msg_empty));
                                }
                            }
                            if (noError) {
                                try {
                                    if(allowEmpty) {
                                        if (listItem.key.equals(context.getString(R.string.upbound_alarm))) {
                                            curRegister.upBound = input;
                                        } else if (listItem.key.equals(context.getString(R.string.lowbound_alarm))) {
                                            curRegister.lowBound = input;
                                        } else if (listItem.key.equals(context.getString(R.string.min))) {
                                            curRegister.minVal = input;
                                        } else if (listItem.key.equals(context.getString(R.string.max))) {
                                            curRegister.maxVal = input;
                                        } else if (listItem.key.equals(getString(R.string.equation))){
                                            curRegister.eq = input.toLowerCase();
                                        }
                                    } else if (listItem.key.equals(context.getString(R.string.alarm_duration))) {
                                        final int almDur = Integer.parseInt(input);
                                        if (almDur < 0 || almDur > 600) {
                                            throw new Exception();
                                        }
                                        curRegister.almDur = almDur;
                                    } else if (listItem.key.equals(context.getString(R.string.btn_time))) { // check button duration
                                        final int btnTime = Integer.parseInt(input);
                                        if (btnTime < 1 || btnTime > 60) {
                                            throw new Exception();
                                        }
                                        if ((btnTime - logFreq) > 2) {
                                            curRegister.btnTime = btnTime;
                                        } else {
                                            Kdialog.getMakeSureDialog(context, getString(R.string.err_dur_better_lt_log_freq))
                                            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface _dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            }).setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface _dialog, int which) {
                                                    curRegister.btnTime = btnTime;
                                                    dialog.dismiss();
                                                    setSaveTimer();
                                                }
                                            }).show();
                                            return;
                                        }
                                    } else if (listItem.key.equals(context.getString(R.string.description))) {
                                        if (!StrUtils.validateInput(StrUtils.IN_TYPE_STRING, input)) {
                                            throw new Exception();
                                        }
                                        curRegister.description = input;
                                    } else if (listItem.key.equals(context.getString(R.string.floating_points))) {
                                        curRegister.fpt = Integer.parseInt(input);
                                        if(curRegister.fpt < 0) {
                                            throw new Exception();
                                        } else if(Register.is48Bits(curRegister.type) && curRegister.fpt > 3) {
                                            throw new Exception();
                                        } else if(curRegister.fpt > 4) {
                                            throw new Exception();
                                        }
                                    } else if (listItem.key.equals(context.getString(R.string.equation))) {
                                        if (!StrUtils.validateInput(StrUtils.IN_TYPE_STRING, input)) {
                                            throw new Exception();
                                        }
                                        curRegister.eq = input.toLowerCase();
                                    } else if (listItem.key.equals(context.getString(R.string.upbound_alarm))) {
                                        if(!Register.validVal(curRegister, input)) {
                                            throw new Exception();
                                        }
                                        curRegister.upBound = input;
                                    } else if (listItem.key.equals(context.getString(R.string.lowbound_alarm))) {
                                        if(!Register.validVal(curRegister, input)) {
                                            throw new Exception();
                                        }
                                        curRegister.lowBound = input;
                                    } else if (listItem.key.equals(context.getString(R.string.max))) {
                                        if(!Register.validVal(curRegister, input)) {
                                            throw new Exception();
                                        }
                                        curRegister.maxVal = input;
                                    } else if (listItem.key.equals(context.getString(R.string.min))) {
                                        if(!Register.validVal(curRegister, input)) {
                                            throw new Exception();
                                        }
                                        curRegister.minVal = input;
                                    } else if (listItem.key.equals(context.getString(R.string.switch_on_hint)) ||
                                               listItem.key.equals(context.getString(R.string.btn_press_hint))) {
                                        int offVal = Integer.parseInt(curRegister.offVal, 16);
                                        int onValInt = Integer.valueOf(input);
                                        if(offVal == onValInt && !curRegister.isCoil()) {
                                            editText.setError(getString(R.string.err_msg_cant_same_value));
                                            return;
                                        }
                                        if (Register.validVal(curRegister, input)) {
                                            curRegister.onVal = String.format("%8s", Integer.toHexString(Integer.valueOf(input))).replace(' ', '0').substring(4, 8);
                                        } else {
                                            throw new Exception();
                                        }
                                    } else if (listItem.key.equals(context.getString(R.string.switch_off_hint)) ||
                                               listItem.key.equals(context.getString(R.string.btn_release_hint))) {
                                        int onVal = Integer.parseInt(curRegister.onVal, 16);
                                        int offValInt = Integer.valueOf(input);
                                        if(onVal == offValInt && !curRegister.isCoil()) {
                                            editText.setError(getString(R.string.err_msg_cant_same_value));
                                            return;
                                        }
                                        if (Register.validVal(curRegister, input)) {
                                            curRegister.offVal = String.format("%8s", Integer.toHexString(Integer.valueOf(input))).replace(' ', '0').substring(4, 8);
                                        } else {
                                            throw new Exception();
                                        }
                                    }
                                    dialog.dismiss();
                                    setSaveTimer();
                                } catch (Exception e) {
                                    if (listItem.key.equals(context.getString(R.string.floating_points))) {
                                        if(Register.is48Bits(curRegister.type)) {
                                            editText.setError(getString(R.string.floating_points_48_hint));
                                        } else {
                                            editText.setError(getString(R.string.floating_points_hint));
                                        }
                                    } else if(listItem.key.equals(context.getString(R.string.description))) {
                                        editText.setError(getString(R.string.err_msg_invalid_str));
                                    } else if(listItem.key.equals(context.getString(R.string.alarm_duration))) {
                                        editText.setError(getString(R.string.range) + ": 0 ~ 600");
                                    } else if(listItem.key.equals(context.getString(R.string.btn_time))) {
                                        editText.setError(getString(R.string.range) + ": 1 ~ 60");
                                    } else if (listItem.key.equals(context.getString(R.string.upbound_alarm))  ||
                                               listItem.key.equals(context.getString(R.string.lowbound_alarm)) ||
                                               listItem.key.equals(context.getString(R.string.max))            ||
                                               listItem.key.equals(context.getString(R.string.min))) {
                                        editText.setError(getString(R.string.range) + ": " + Register.getValidRange(curRegister));
                                    } else if (listItem.key.equals(context.getString(R.string.switch_on_hint))   ||
                                               listItem.key.equals(context.getString(R.string.switch_off_hint))  ||
                                               listItem.key.equals(context.getString(R.string.btn_press_hint))   ||
                                               listItem.key.equals(context.getString(R.string.btn_release_hint))) {
                                        editText.setError(getString(R.string.range) + ": " + Register.getValidRange(curRegister));
                                    } else {
                                        editText.setError(getString(R.string.invalid_val));
                                    }
                                    if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                                        MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
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
            if (listItem.key.equals(getString(R.string.en_reg_logging))     ||
                listItem.key.equals(getString(R.string.en_reg_usb_logging)) ) {
                curRegister.enlog = !curRegister.enlog;
                setSaveTimer();
            } else if (listItem.key.equals(getString(R.string.virtual_register))) {
                if(curRegister.virtReg != null && curRegister.virtReg.equals("1")) {
                    curRegister.virtReg = "0";
                } else {
                    curRegister.virtReg = "1";
                }
                setSaveTimer();
            } else if (listItem.key.equals(getString(R.string.send_alarm_email))) {
                if(curRegister.sndAlmMail != null && curRegister.sndAlmMail.equals("1")) {
                    curRegister.sndAlmMail = "0";
                } else {
                    curRegister.sndAlmMail = "1";
                }
                setSaveTimer();
            }
        } else if(listItem.type == ListItem.BUTTON) {
            if (listItem.key.equals(getString(R.string.limited_access))) {
                mDialog = Kdialog.getProgress(context, mDialog);
                authTimer = new Timer();
                authTimer.schedule(new GetAuthUserTimerTask(), 0, PrjCfg.RUN_ONCE);
            } else if(listItem.key.equals(getString(R.string.refer_register)) ||
                listItem.key.equals(getString(R.string.refer_register_1))     ||
                listItem.key.equals(getString(R.string.refer_register_2))     ||
                listItem.key.equals(getString(R.string.refer_register_3))     ||
                listItem.key.equals(getString(R.string.refer_register_4))) {
                mDialog = Kdialog.getProgress(context, mDialog);
                getRegTimer = new Timer();
                getRegTimer.schedule(new GetRegTimerTask(listItem.key), 0, PrjCfg.RUN_ONCE);
            } else if(listItem.key.equals(getString(R.string.unit))) {
                selUnitDailog();
            } else if(listItem.key.equals(getString(R.string.importance))) {
                selImportanceDailog();
            } else if(listItem.key.equals(getString(R.string.display_type))) {
                selDisplayDailog();
            }
        }
    }

    private boolean[] chkArray;

    private void authDailog() {
        try {
            final List<CharSequence> accNameList = new LinkedList<CharSequence>();
            final List<Integer> accIdList = new LinkedList<Integer>();
            final List<Boolean> ableList = new LinkedList<Boolean>();
            for (int i = 0; i < jAuthMembers.length(); i++) {
                JSONObject jAuthMember = (JSONObject) jAuthMembers.get(i);
                if(!jAuthMember.has("admin")) { // ignore the first record
                    continue;
                }
                int admin = jAuthMember.getInt("admin");
                String accName = jAuthMember.getString("name");
                accNameList.add(accName);
                accIdList.add(jAuthMember.getInt("memberId"));
                if(accName.equalsIgnoreCase("Admin")) {
                    ableList.add(false);
                } else if(admin > 0) {
                    ableList.add(true);
                } else if(Register.isAlarm(curRegister.type)) {
                    int enAlarm = jAuthMember.getInt("enAlarm");
                    ableList.add((enAlarm == 1) ? true: false);
                } else {
                    int enMonitor = jAuthMember.getInt("enMonitor");
                    int enControl = jAuthMember.getInt("enControl");
                    ableList.add((enMonitor == 0 && enControl == 0) ? false : true);
                }
            }
            final String[] accNameArray = new String[accNameList.size()];
            accNameList.toArray(accNameArray);
            final Integer[] accIdArray = new Integer[accIdList.size()];
            accIdList.toArray(accIdArray);
            final Boolean[] availArray = new Boolean[ableList.size()];
            ableList.toArray(availArray);

            // Get checked item from limit list
            chkArray = new boolean[accIdArray.length];
            if(curRegister.limitId != null && curRegister.limitId.length > 0) {
                for (int i = 0; i < accIdArray.length; i++) {
                    boolean found = false;
                    for (int j = 0; j < curRegister.limitId.length; j++) {
                        if(accIdArray[i].intValue() == curRegister.limitId[j].intValue()) {
                            found = true;
                            break;
                        }
                    }
                    chkArray[i] = (found && availArray[i]) ? true : false;
                }
            } else {
                for (int i = 0; i < accIdArray.length; i++) {
                    chkArray[i] = false;
                }
            }
            final AlertDialog dialog;
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(getString(R.string.limited_access));
            builder.setIcon(R.drawable.ic_user);
            builder.setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final List<Integer> limitList = new LinkedList<Integer>();
                    for(int i = 0; i < chkArray.length; i++) {
                        if(chkArray[i] && availArray[i]) {
                            limitList.add(accIdArray[i]);
                        }
                    }
                    Integer[] newLimitId = new Integer[limitList.size()];
                    limitList.toArray(newLimitId);
                    if(Arrays.equals(curRegister.limitId, newLimitId)) {
                        MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.setting_no_change));
                    } else {
                        curRegister.limitId = newLimitId;
                        setSaveTimer();
                    }
                    dialog.dismiss();
                }
            }).setNegativeButton(getString(R.string.cancel), null);

            View view = getLayoutInflater().inflate(R.layout.list_view_default, null);
            dialog = builder.create();
            dialog.setView(view);

            ListView lv = (ListView) view.findViewById(R.id.listView);
            lv.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return accNameArray.length;
                }

                @Override
                public Object getItem(int position) {
                    return accNameArray[position];
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                public View getView(final int position, View view, ViewGroup parent) {
                    LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                    view = inflater.inflate(R.layout.limit_access, parent, false);
                    final CheckedTextView checkBox = (CheckedTextView) view.findViewById(R.id.checkBox);
                    checkBox.setCheckMarkDrawable(R.drawable.multichoice);
                    checkBox.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            CheckedTextView _checkBox = (CheckedTextView) v;
                            //Log.e(Dbg._TAG_(), "position " + position + " isChecked " + _checkBox.isChecked());
                            if(availArray[position]) {
                                chkArray[position] = !_checkBox.isChecked();
                                _checkBox.setChecked(chkArray[position]);
                            } else {
                                if(accNameArray[position].equals("Admin")) {
                                    _checkBox.setChecked(false);
                                } else {
                                    _checkBox.setChecked(true);
                                }
                            }
                        }
                    });
                    if(accNameArray[position].equals("Admin")) {
                        checkBox.setChecked(false);
                    } else if(chkArray[position] || !availArray[position]){
                        checkBox.setChecked(true);
                    } else {
                        checkBox.setChecked(false);
                    }
                    if(!availArray[position]) {
                        checkBox.setText(accNameArray[position]);
                        checkBox.setEnabled(false);
                        checkBox.setClickable(false);
                    } else {
                        checkBox.setText(Html.fromHtml("<font color='#000000'>" + accNameArray[position] + "</font>"));
                    }
                    return view;
                }
            });
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void inputUnitDialog() {
        try {
            final LayoutInflater inflater = LayoutInflater.from(context);
            final View layout = inflater.inflate(R.layout.input_text, null);
            final EditText unitText = (EditText) layout.findViewById(R.id.text);
            unitText.setFilters(Utils.arrayMerge(unitText.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER, new InputFilter.LengthFilter(5)}));

            final AlertDialog dialog = Kdialog.getDefInputDialog(context).create();
            dialog.setView(layout);
            dialog.setIcon(R.drawable.ic_editor);
            dialog.setTitle(getString(R.string.enter_unit));
            dialog.setCancelable(true);
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogIface) {
                    Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Boolean noError = true;
                            String unitStr = unitText.getText().toString().trim();
                            if (!StrUtils.validateInput(StrUtils.IN_TYPE_STRING, unitStr)) {
                                noError = false;
                                unitText.setError(getString(R.string.err_msg_invalid_str));
                            }
                            if (noError) {
                                dialog.dismiss();
                                curRegister.unit = unitStr;
                                setSaveTimer();
                            }
                        }
                    });
                }
            });
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void selImportanceDailog() {
        try {
            final String[] importances = getResources().getStringArray(R.array.importance);
            final AlertDialog dialog;
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(getString(R.string.importance));
            builder.setIcon(R.drawable.ic_editor);
            builder.setSingleChoiceItems(importances, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    curRegister.almPri = which;
                    dialog.dismiss();
                    setSaveTimer();
                }
            }).setNegativeButton(getString(R.string.cancel), null);
            dialog = builder.create();
            dialog.show();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void selUnitDailog() {
        try {
            final String[] units = getResources().getStringArray(R.array.units);
            final AlertDialog dialog;
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(getString(R.string.unit));
            builder.setIcon(R.drawable.ic_editor);
            builder.setSingleChoiceItems(units, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(which == 1) { // Enter unit
                        dialog.dismiss();
                        inputUnitDialog();
                    } else {
                        curRegister.unit = (which == 0) ? "" : units[which];
                        dialog.dismiss();
                        setSaveTimer();
                    }
                }
            }).setNegativeButton(getString(R.string.cancel), null);
            dialog = builder.create();
            dialog.show();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void referRegsterDailog(final String key) {
        if(jModbus == null) {
            return;
        }
        try {
            final List<CharSequence> regNameList = new LinkedList<CharSequence>();
            final List<CharSequence> regAddrList = new LinkedList<CharSequence>();
            regNameList.add(0, getString(R.string.none));
            regAddrList.add(0, "");
            for (int i = 0; i < jModbus.length(); i++) {
                JSONObject jMbusObj = jModbus.getJSONObject(i);
                String desc = jMbusObj.getString("desc");
                String haddr = jMbusObj.getString("haddr");
                String laddr = jMbusObj.getString("laddr");
                String addr = (laddr.equals("") ? haddr : (haddr + "-" + laddr));
                regNameList.add((i + 1), desc + " (" + addr + ")");
                regAddrList.add((i + 1), addr);
            }
            final CharSequence[] regNameArray = new CharSequence[regNameList.size()];
            regNameList.toArray(regNameArray);

            final AlertDialog dialog;
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(getString(R.string.select_register));
            builder.setIcon(R.drawable.ic_refresh);
            builder.setSingleChoiceItems(regNameArray, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    if(key.equals(getString(R.string.refer_register))) {
                        curRegister.refReg = (String) regAddrList.get(which);
                    } else if(key.equals(getString(R.string.refer_register_1))) {
                        curRegister.rr1 = (String) regAddrList.get(which);
                    } else if(key.equals(getString(R.string.refer_register_2))) {
                        curRegister.rr2 = (String) regAddrList.get(which);
                    } else if(key.equals(getString(R.string.refer_register_3))) {
                        curRegister.rr3 = (String) regAddrList.get(which);
                    } else if(key.equals(getString(R.string.refer_register_4))) {
                        curRegister.rr4 = (String) regAddrList.get(which);
                    }
                    setSaveTimer();
                }
            }).setNegativeButton(getString(R.string.cancel), null);
            dialog = builder.create();
            dialog.show();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void selDisplayDailog() {
        final String[] displayTypes = Register.getAllDisplayTypes(context);
        final AlertDialog dialog;
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getString(R.string.display_type));
        builder.setIcon(R.drawable.ic_refresh);
        builder.setSingleChoiceItems(displayTypes, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                curRegister.display = Register.getDisplayTypesIdx(context, displayTypes[which]);
                setSaveTimer();
            }
        }).setNegativeButton(getString(R.string.cancel), null);
        dialog = builder.create();
        dialog.show();
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
                JSONObject jObject = JSONReq.multipart(context, "PUT", PrjCfg.CLOUD_URL + "/api/device/edit", curRegister.toMultiPart("EDIT", curSn));
                int statCode = jObject.getInt("code");
                //Log.e(Dbg._TAG_(), jObject.toString());
                if(statCode == 400 && !jObject.isNull("desc") && jObject.getString("desc").trim().matches("Device is logging")) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.usb_logging));
                } else if(statCode == 400 && !jObject.isNull("errs") && jObject.getString("errs").trim().contains("mbusUp should larger")) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_upval_should_larger));
                } else if(statCode == 400 && !jObject.isNull("errs") && jObject.getString("errs").trim().contains("mbusLow should smaller")) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_lowval_should_smaller));
                } else if(statCode == 400 && !jObject.isNull("errs") && jObject.getString("errs").trim().contains("mbusMin should smaller than mbusMax")) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_minval_should_smaller));
                } else if(statCode == 400 && !jObject.isNull("errs") && jObject.getString("errs").trim().contains("mbusMax should larger than mbusMin")) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_maxval_should_larger));
                } else if(statCode == 400 && !jObject.isNull("desc") && jObject.getString("desc").trim().toLowerCase().contains("invalid")) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_invalid_str));
                } else if (statCode != 200) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_save));
                } else {
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_save));
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
                JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/device/" + curSn + "?mbusId=" + curId);
                if (jObject == null || jObject.getInt("code") != 200) {
                    MHandler.exec(mHandler, MHandler.GO_BACK, getString(R.string.err_get_data));
                    return;
                }
                if(jObject.has("enLog")) {
                    enLog = (jObject.getInt("enLog") == 1) ? true : false;
                } else {
                    enLog = false;
                }
                JSONObject jModbus = jObject.getJSONObject("modbus");
                curRegister = (curSlvIdx > 0) ? new Register(curSlvIdx, jModbus) : new Register(jModbus);
                MHandler.exec(mHandler, MHandler.UPDATE);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                pollingTimer.cancel();
                mDialog.dismiss();
            }
        }
    }

    private class GetAuthUserTimerTask extends TimerTask {
        public void run() {
            try {
                JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/device/auth/" + curSn);
                if (jObject == null || jObject.getInt("code") != 200) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_get_data));
                    return;
                }
                jAuthMembers = jObject.getJSONArray("members");
                if(jAuthMembers == null) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_get_data));
                    return;
                }
                MHandler.exec(mHandler, MHandler.AUTH_DIALG);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                pollingTimer.cancel();
                mDialog.dismiss();
            }
        }
    }

    private class GetRegTimerTask extends TimerTask {
        private String key;

        public GetRegTimerTask(String key) {
            this.key = key;
        }

        public void run() {
            try {
                JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/device/" + curSn);
                if (jObject.getInt("code") != 200) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_get_data));
                    return;
                }
                JSONObject jDevice = jObject.getJSONObject("device");
                jModbus = jDevice.getJSONArray("modbus");
                MHandler.exec(mHandler, MHandler.REFER_REG_DIALG, key);
            } catch (Exception e) {
                MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_get_data));
                e.printStackTrace();
            } finally {
                mDialog.dismiss();
                getRegTimer.cancel();
            }
        }
    }

    private static class MsgHandler extends MHandler {
        public MsgHandler(EditRegActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            EditRegActivity activity = (EditRegActivity) super.mActivity.get();
            switch (msg.what) {
                case MHandler.UPDATE: {
                    activity.adapter.addList(activity.curRegister);
                    activity.adapter.notifyDataSetChanged();
                    activity.prListView.onRefreshComplete();
                    break;
                }
                case MHandler.CLEAR_LIST: {
                    activity.adapter.clearList();
                    break;
                }
                case MHandler.AUTH_DIALG: {
                    activity.authDailog();
                    break;
                }
                case MHandler.REFER_REG_DIALG: {
                    activity.referRegsterDailog((String)msg.obj);
                    break;
                }
                default: {
                    super.handleMessage(msg);
                }
            }
        }
    }

}