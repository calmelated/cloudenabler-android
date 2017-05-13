package tw.com.ksmt.cloud.ui;

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
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Device;
import tw.com.ksmt.cloud.iface.Register;
import tw.com.ksmt.cloud.libs.Dbg;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.StrUtils;
import tw.com.ksmt.cloud.libs.Utils;

public class NewRegisterActivity extends ActionBarActivity {
    private Context context = NewRegisterActivity.this;
    private ActionBar actionBar;
    private static final int IOSW = 1;
    private ProgressDialog mDialog;
    private Device curDevice;
    private Timer saveTimer;
    private Timer getUsedRegTimer;
    private MsgHandler mHandler;
    private EditText editDesc;
    private EditText editOnVal;
    private EditText editOffVal;
    private EditText editBtnTime;
    private TextView txtIOSW;
    private ImageView imgIOSW;
    private Spinner spinDevType;
    private Spinner spinFC;
    private Spinner spinHaddr;
    private Spinner spinLaddr;
    private Spinner spinFpt;
    private Button btnSave;
    private Button btnSaveAndNext;
    private TextView txtBtnTime;
    private TextView txtAddr;
    private TextView textFC;
    private TextView editHaddr;
    private TextView editLaddr;
    private CheckBox ckboxLogging;
    private LinearLayout layoutFpt;
    private LinearLayout layoutIOSW;
    private LinearLayout layoutSndVal;
    private ArrayAdapter<String> fcwAdapter;
    private ArrayAdapter<String> fcw4Adapter;
    private ArrayAdapter<String> fc4Adapter;
    private ArrayAdapter<String> typeAdapter;
    private ArrayAdapter<String> haddrAdapter;
    private ArrayAdapter<String> laddrAdapter;
    private ArrayAdapter<Integer> fptAdapter;
    private ArrayAdapter<Integer> fpt48Adapter;
    private Device ioswSrcDev;
    private Register ioswSrcReg;
    private int numFreeRegs = 0;
    private boolean isSlvDev;
    private int curTypeIdx = 0; //Modbus 16bits int

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("  " + getString(R.string.new_register));
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        actionBar.setIcon(R.drawable.ic_new_register_1);
        actionBar.setDisplayShowHomeEnabled(true);
        AppSettings.setupLang(context);
        setContentView(R.layout.device_register);

        editDesc = (EditText) findViewById(R.id.editDesc);
        editDesc.setFilters(Utils.arrayMerge(editDesc.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
        spinDevType = (Spinner) findViewById(R.id.spinDevType);
        spinFC = (Spinner) findViewById(R.id.spinFC);
        spinHaddr = (Spinner) findViewById(R.id.spinHaddr);
        spinLaddr = (Spinner) findViewById(R.id.spinLaddr);
        txtAddr = (TextView) findViewById(R.id.txtAddr);
        textFC = (TextView) findViewById(R.id.textFC);
        txtBtnTime = (TextView) findViewById(R.id.txtBtnTime);
        editHaddr = (TextView) findViewById(R.id.editHaddr);
        editLaddr = (TextView) findViewById(R.id.editLaddr);
        layoutIOSW = (LinearLayout) findViewById(R.id.layoutIOSW);
        layoutFpt = (LinearLayout) findViewById(R.id.layoutFpt);
        layoutSndVal = (LinearLayout) findViewById(R.id.layoutSndVal);
        ckboxLogging = (CheckBox) findViewById(R.id.ckboxLogging);
        editOnVal = (EditText) findViewById(R.id.editOnVal);
        editOffVal = (EditText) findViewById(R.id.editOffVal);
        editBtnTime = (EditText) findViewById(R.id.editBtnTime);

        txtIOSW = (TextView) findViewById(R.id.txtIOSW);
        imgIOSW = (ImageView) findViewById(R.id.imgIOSW);
        imgIOSW.setOnClickListener(imgIOSWtListener);

        // fpt spinner only for 16bits, 32bits, 64bits
        fptAdapter = new ArrayAdapter<Integer>(context, R.layout.spinner);
        fptAdapter.addAll(new Integer[]{0, 1, 2, 3, 4});
        fptAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinFpt = (Spinner) findViewById(R.id.spinFpt);
        spinFpt.setAdapter(fptAdapter);

        // fpt spinner only for 48bits
        fpt48Adapter = new ArrayAdapter<Integer>(context, R.layout.spinner);
        fpt48Adapter.addAll(new Integer[]{0, 1, 2, 3});
        fpt48Adapter.setDropDownViewResource(R.layout.spinner_item);

        mHandler = new MsgHandler(this);
        curDevice = (Device) getIntent().getSerializableExtra("Device");
        isSlvDev = (curDevice != null && curDevice.slvDev != null && curDevice.slvIdx > 0) ? true : false ;
        String subTitleStr = (isSlvDev) ? (getString(R.string.slave_device) + ": " + curDevice.slvDev.name) : (getString(R.string.device) + ": " + curDevice.name);
        actionBar.setSubtitle(Html.fromHtml("<small><small>&nbsp;&nbsp;&nbsp;&nbsp;" + subTitleStr + "</small></small>"));
        ckboxLogging.setOnClickListener(ckboxListener);

        btnSave = (Button) findViewById(R.id.btnSave);
        btnSave.setOnClickListener(btnSaveListener);
        btnSaveAndNext = (Button) findViewById(R.id.saveAndNext);
        btnSaveAndNext.setOnClickListener(btnSaveAndNextListener);

        //mDialog = Kdialog.getProgress(context, mDialog);
        typeAdapter = new ArrayAdapter<String>(context, R.layout.spinner);
        haddrAdapter = new ArrayAdapter<String>(context, R.layout.spinner);
        laddrAdapter = new ArrayAdapter<String>(context, R.layout.spinner);

//        typeAdapter.addAll(Register.getAllTypes(context, false));
        typeAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinDevType.setAdapter(typeAdapter);
        spinDevType.setOnItemSelectedListener(spinDevTypeListener);

        // Function Code: only support holding registers now
        fcwAdapter = new ArrayAdapter<String>(context, R.layout.spinner);
        fcwAdapter.setDropDownViewResource(R.layout.spinner_item);
        fcwAdapter.add(getString(R.string.write_multi_holding_reigsters));

        fcw4Adapter = new ArrayAdapter<String>(context, R.layout.spinner);
        fcw4Adapter.setDropDownViewResource(R.layout.spinner_item);
        fcw4Adapter.add(getString(R.string.write_coils_status));
        fcw4Adapter.add(getString(R.string.write_holding_reigsters));
//        fcw4Adapter.add(getString(R.string.write_multi_coils_status));
        fcw4Adapter.add(getString(R.string.write_multi_holding_reigsters));

        fc4Adapter = new ArrayAdapter<String>(context, R.layout.spinner);
        fc4Adapter.setDropDownViewResource(R.layout.spinner_item);
        fc4Adapter.add(getString(R.string.read_coils_status));
        fc4Adapter.add(getString(R.string.read_input_status));
        fc4Adapter.add(getString(R.string.read_holoding_registers));
        fc4Adapter.add(getString(R.string.read_input_registers));

        if(isSlvDev) {
            spinFC.setOnItemSelectedListener(spinFCListener);
        } else {
            textFC.setVisibility(View.GONE);
            spinFC.setVisibility(View.GONE);
        }

        mDialog = Kdialog.getProgress(context, mDialog);
        getUsedRegTimer = new Timer();
        getUsedRegTimer.schedule(new GetUsedRegTimerTask(), 0, PrjCfg.RUN_ONCE);
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
        if (getUsedRegTimer != null) {
            getUsedRegTimer.cancel();
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

        String devName = (ioswSrcDev.slvIdx > 0) ? ioswSrcDev.slvDev.name : ioswSrcDev.name ;
        Log.e(Dbg._TAG_(), "M2M: Device/Register: " + ioswSrcDev.sn + " (" + devName + ") / " + ioswSrcReg.description);
        txtIOSW.setText(devName + ": " + ioswSrcReg.description );
    }

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

    View.OnClickListener imgIOSWtListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int type = Register.getTypeIdx(context, (String) spinDevType.getSelectedItem());
            Intent intent = new Intent(context, IOSWActivity.class);
            intent.putExtra("is32Bits", Register.is32Bits(type));
            intent.putExtra("is48Bits", Register.is48Bits(type));
            intent.putExtra("is64Bits", Register.is64Bits(type));
            startActivityForResult(intent, IOSW);
        }
    };

    private void onSaveBtnClick(final boolean addMore) {
        try {
            String desc = editDesc.getText().toString().replaceAll("\\n", "").trim();
            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, desc)) {
                editDesc.setError(getString(R.string.err_msg_empty));
                return;
            } else if (!StrUtils.validateInput(StrUtils.IN_TYPE_STRING, desc)) {
                editDesc.setError(getString(R.string.err_msg_invalid_str));
                return;
            }

            String haddr = "";
            String iaddr = "";
            String jaddr = "";
            String laddr = "";
            int type = Register.getTypeIdx(context, (String) spinDevType.getSelectedItem());
            if(isSlvDev) {
                int fcBaseAddr = 400001; // Default: Read Holding registersdr
                String spinFCstr = (String) spinFC.getSelectedItem();
                if(spinFCstr.equals(getString(R.string.read_coils_status))) { //FC=1, 00001 ~ 65535
                    fcBaseAddr = 1;
                } else if(spinFCstr.equals(getString(R.string.read_input_status))) { //FC=2, 10001 ~ 75535
                    fcBaseAddr = 100001;
                } else if(spinFCstr.equals(getString(R.string.read_holoding_registers))) { //FC=3, 40001 ~ 105536
                    fcBaseAddr = 400001;
                } else if(spinFCstr.equals(getString(R.string.read_input_registers))) { //FC=4, 30001 ~ 95536
                    fcBaseAddr = 300001;
                } else if(spinFCstr.equals(getString(R.string.write_coils_status))) { //FC=5,  50001 ~
                    fcBaseAddr = 500001;
                } else if(spinFCstr.equals(getString(R.string.write_holding_reigsters))) { //FC=6,  60001 ~
                    fcBaseAddr = 600001;
                } else if(spinFCstr.equals(getString(R.string.write_multi_holding_reigsters))) { //FC=16, 70001 ~
                    fcBaseAddr = 700001;
                }

                haddr = editHaddr.getText().toString();
                if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, haddr)) {
                    editHaddr.setError(getString(R.string.err_msg_empty));
                    return;
                }

                int _haddr = StrUtils.validMbusAddr(haddr);
                if(_haddr < 0) {
                    editHaddr.setError(getString(R.string.invalid_val));
                    return;
                }
                haddr = String.valueOf(curDevice.slvIdx * 1000000 + fcBaseAddr + _haddr);

                if(Register.is32Bits(type) || Register.is64Bits(type) || Register.is48Bits(type)) {
                    laddr = editLaddr.getText().toString();
                    if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, laddr)) {
                        editLaddr.setError(getString(R.string.err_msg_empty));
                        return;
                    }
                    int _laddr = StrUtils.validMbusAddr(laddr);
                    if(_laddr < 0) {
                        editHaddr.setError(getString(R.string.invalid_val));
                        return;
                    }
                    if(Register.is64Bits(type)) {
                        laddr = String.valueOf(curDevice.slvIdx * 1000000 + fcBaseAddr + _laddr);
                        if (_haddr > _laddr) {
                            iaddr = String.valueOf(curDevice.slvIdx * 1000000 + fcBaseAddr + _haddr - 1);
                            jaddr = String.valueOf(curDevice.slvIdx * 1000000 + fcBaseAddr + _haddr - 2);
                        } else {
                            iaddr = String.valueOf(curDevice.slvIdx * 1000000 + fcBaseAddr + _laddr - 2);
                            jaddr = String.valueOf(curDevice.slvIdx * 1000000 + fcBaseAddr + _laddr - 1);
                        }
                    } else if(Register.is48Bits(type)) {
                        laddr = String.valueOf(curDevice.slvIdx * 1000000 + fcBaseAddr + _laddr);
                        if (_haddr > _laddr) {
                            iaddr = String.valueOf(curDevice.slvIdx * 1000000 + fcBaseAddr + _haddr - 1);
                        } else {
                            iaddr = String.valueOf(curDevice.slvIdx * 1000000 + fcBaseAddr + _laddr - 1);
                        }
                    } else { // 32 bits
                        laddr = String.valueOf(curDevice.slvIdx * 1000000 + fcBaseAddr + _laddr);
                    }
                }
            } else {
                haddr = (String) spinHaddr.getSelectedItem();
                if(Register.is32Bits(type)) {
                    laddr = (String) spinLaddr.getSelectedItem();
                } else if(Register.is48Bits(type)) {
                    laddr = (String) spinLaddr.getSelectedItem();
                    int _laddr = Integer.parseInt(laddr);
                    int _haddr = Integer.parseInt(haddr);
                    if(_haddr < _laddr) {
                        iaddr = String.valueOf(_haddr + 1);
                    } else {
                        iaddr = String.valueOf(_haddr - 1);
                    }
                } else if(Register.is64Bits(type)) {
                    laddr = (String) spinLaddr.getSelectedItem();
                    int _laddr = Integer.parseInt(laddr);
                    int _haddr = Integer.parseInt(haddr);
                    if(_haddr < _laddr) {
                        iaddr = String.valueOf(_haddr + 1);
                        jaddr = String.valueOf(_haddr + 2);
                    } else {
                        iaddr = String.valueOf(_haddr - 1);
                        jaddr = String.valueOf(_haddr - 2);
                    }
                } else {
                    laddr = "";
                }
            }
            if (haddr.equals(laddr)) {
                MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_same_addr));
                return;
            }

            String onVal = null;
            String offVal = null;
            if (type == Register.APP_SWITCH || type == Register.APP_BTN) {
                onVal = editOnVal.getText().toString().trim();
                if(onVal.equals("")) {
                    onVal = "0001";
                } else if(Register.validVal(Register.APP_UINT16, onVal)) {
                    onVal = String.format("%8s", Integer.toHexString(Integer.valueOf(onVal))).replace(' ', '0').substring(4, 8);
                } else {
                    editOnVal.setError(getString(R.string.range) + ": " + Register.getValidRange(Register.APP_UINT16));
                    return;
                }
                offVal = editOffVal.getText().toString().trim();
                if(offVal.equals("")) {
                    offVal = "0000";
                } else if(Register.validVal(Register.APP_UINT16, offVal)) {
                    offVal = String.format("%8s", Integer.toHexString(Integer.valueOf(offVal))).replace(' ', '0').substring(4, 8);
                } else {
                    editOffVal.setError(getString(R.string.range) + ": " + Register.getValidRange(Register.APP_UINT16));
                    return;
                }
                if(onVal.equals(offVal)) {
                    editOnVal.setError(getString(R.string.err_msg_cant_same_value));
                    editOffVal.setError(getString(R.string.err_msg_cant_same_value));
                    return;
                }
            }

            int btnTime = 13;
            if (type == Register.APP_BTN) {
                String _btnTime = editBtnTime.getText().toString().trim();
                if(!_btnTime.equals("")) {
                    btnTime = Integer.parseInt(_btnTime);
                    if(btnTime < 1 || btnTime > 60) {
                        editBtnTime.setError(getString(R.string.range) + ": 1 ~ 60");
                        return;
                    }
                }
            }

            String swSN = null;
            String swAddr = null;
            if (Register.isIOSW(type)) {
                if(ioswSrcDev == null || ioswSrcReg == null || txtIOSW.getText().equals("")) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_no_src_dev));
                    return;
                }
                String slvIdx = (ioswSrcDev.slvIdx > 0) ? String.valueOf(ioswSrcDev.slvIdx) : "" ;
                swSN = ioswSrcDev.sn;
                if(Register.is16Bits(ioswSrcReg.type) ||
                   Register.is32Bits(ioswSrcReg.type) ||
                   Register.is64Bits(ioswSrcReg.type)) {
                    swAddr = slvIdx + ioswSrcReg.haddr + "-" + slvIdx + ioswSrcReg.laddr;
                } else {
                    swAddr = slvIdx + ioswSrcReg.haddr;
                }
            }

            boolean enEnlog = ckboxLogging.isChecked();
            int fpt = (Register.isFixPtVal(type)) ? (int) spinFpt.getSelectedItem() : 0;
            final Register register = new Register(type, haddr, laddr);
            register.description = desc;
            register.iaddr = iaddr;
            register.jaddr = jaddr;
            register.enlog = enEnlog;
            register.fpt = fpt;
            register.onVal = onVal;
            register.offVal = offVal;
            register.btnTime = btnTime;
            register.swSN = swSN;
            register.swAddr = swAddr;

            if(Register.is64Bits(type)) {
                int _haddr = Integer.parseInt(haddr);
                int _laddr = Integer.parseInt(laddr);
                if ((_haddr - _laddr + 1) == 4 || (_laddr - _haddr + 1) == 4) {
                    setSaveTimerTask(register, addMore);
                } else {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.need_4_continuous_addrs));
                }
            } else if(Register.is48Bits(type)) {
                int _haddr = Integer.parseInt(haddr);
                int _laddr = Integer.parseInt(laddr);
                if ((_haddr - _laddr + 1) == 3 || (_laddr - _haddr + 1) == 3) {
                    setSaveTimerTask(register, addMore);
                } else {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.need_3_continuous_addrs));
                }
            } else if(Register.is32Bits(type)) {
                int _haddr = Integer.parseInt(haddr);
                int _laddr = Integer.parseInt(laddr);
                if (!((_haddr - _laddr) == 1 || (_laddr - _haddr) == 1)) {
                    Kdialog.getMakeSureDialog(context, getString(R.string.err_consecutive_addr))
                            .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    setSaveTimerTask(register, addMore);
                                }
                            }).show();
                } else {
                    setSaveTimerTask(register, addMore);
                }
            } else { // 16 bits
                setSaveTimerTask(register, addMore);
            }
        } catch (Exception e) {
            if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
            }
            e.printStackTrace();
        }
    }

    private void setSaveTimerTask(Register register, boolean addMore) {
        mDialog = Kdialog.getProgress(context, mDialog);
        saveTimer = new Timer();
        saveTimer.schedule(new SaveTimerTask(register, addMore), 0, PrjCfg.RUN_ONCE);
    }

    View.OnClickListener ckboxListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(curDevice.enlog) {
                Kdialog.getDefInfoDialog(context).setMessage(getString(R.string.usb_logging)).show();
                ckboxLogging.setChecked(false);
            }
        }
    };

    AdapterView.OnItemSelectedListener spinFCListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            String fcStr = (String) adapterView.getSelectedItem();
            if(fcStr.equals(getString(R.string.write_coils_status))) {
                editOnVal.setText("");
                editOffVal.setText("");
                layoutSndVal.setVisibility(View.GONE);
            } else if(curTypeIdx == Register.APP_SWITCH || curTypeIdx == Register.APP_BTN) {
                layoutSndVal.setVisibility(View.VISIBLE);
                editOnVal.setText("");
                editOffVal.setText("");
                editOnVal.setHint(getString(R.string.btn_press_hint));
                editOffVal.setHint(getString(R.string.btn_release_hint));
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };

    AdapterView.OnItemSelectedListener spinDevTypeListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
            String typeStr = typeAdapter.getItem(position);
            curTypeIdx = Register.getTypeIdx(context, typeStr);
            if(Register.is32Bits(curTypeIdx) || Register.is48Bits(curTypeIdx) || Register.is64Bits(curTypeIdx)) {
                editBtnTime.setVisibility(View.GONE);
                txtBtnTime.setVisibility(View.GONE);
                txtAddr.setText(getString(R.string.reg_address));
                if(Register.is64Bits(curTypeIdx)) {
                    btnSaveAndNext.setVisibility((numFreeRegs < 8) ? View.INVISIBLE : View.VISIBLE);
                } else if(Register.is48Bits(curTypeIdx)) {
                    btnSaveAndNext.setVisibility((numFreeRegs < 6) ? View.INVISIBLE : View.VISIBLE);
                } else {
                    btnSaveAndNext.setVisibility((numFreeRegs < 3) ? View.INVISIBLE : View.VISIBLE);
                }
                if(isSlvDev) {
                    spinHaddr.setVisibility(View.GONE);
                    spinLaddr.setVisibility(View.GONE);
                    editHaddr.setVisibility(View.VISIBLE);
                    editLaddr.setVisibility(View.VISIBLE);
                } else {
                    spinHaddr.setVisibility(View.VISIBLE);
                    spinLaddr.setVisibility(View.VISIBLE);
                    editHaddr.setVisibility(View.GONE);
                    editLaddr.setVisibility(View.GONE);
                }
            } else {
                txtAddr.setText(getString(R.string.address));
                spinLaddr.setVisibility(View.INVISIBLE);
                btnSaveAndNext.setVisibility((numFreeRegs < 2) ? View.INVISIBLE : View.VISIBLE);

                if(isSlvDev) {
                    spinHaddr.setVisibility(View.GONE);
                    spinLaddr.setVisibility(View.GONE);
                    editHaddr.setVisibility(View.VISIBLE);
                    editLaddr.setVisibility(View.INVISIBLE);
                } else {
                    spinHaddr.setVisibility(View.VISIBLE);
                    spinLaddr.setVisibility(View.INVISIBLE);
                    editHaddr.setVisibility(View.GONE);
                    editLaddr.setVisibility(View.GONE);
                }
            }

            if(Register.is48Bits(curTypeIdx)) {
                spinFpt.setAdapter(fpt48Adapter);
            } else {
                spinFpt.setAdapter(fptAdapter);
            }

            if(Register.isIOSW(curTypeIdx)) {
                layoutSndVal.setVisibility(View.GONE);
                editBtnTime.setVisibility(View.GONE);
                txtBtnTime.setVisibility(View.GONE);
                layoutIOSW.setVisibility(View.VISIBLE);
                txtIOSW.setText("");
                txtIOSW.setHint(getString(R.string.device) + ": " + getString(R.string.register));
            } else if(curTypeIdx == Register.APP_SWITCH || curTypeIdx == Register.APP_BTN){
                layoutIOSW.setVisibility(View.GONE);
                editBtnTime.setVisibility(View.GONE);
                txtBtnTime.setVisibility(View.GONE);
                layoutSndVal.setVisibility(View.VISIBLE);
                editOnVal.setText("");
                editOffVal.setText("");
                editBtnTime.setText("");
                editOnVal.setHint(getString(R.string.btn_press_hint));
                editOffVal.setHint(getString(R.string.btn_release_hint));
                editBtnTime.setHint(getString(R.string.btn_time_hint));
            } else {
                layoutIOSW.setVisibility(View.GONE);
                layoutSndVal.setVisibility(View.GONE);
                editBtnTime.setVisibility(View.GONE);
                txtBtnTime.setVisibility(View.GONE);
            }

            if(Register.isFixPtVal(curTypeIdx)) {
                layoutFpt.setVisibility(View.VISIBLE);
            } else {
                layoutFpt.setVisibility(View.GONE);
            }

            if(Register.enCloudLogging(curTypeIdx)) {
                ckboxLogging.setText(getString(R.string.en_reg_logging));
            } else {
                ckboxLogging.setText(getString(R.string.en_reg_usb_logging));
            }

            if(Register.isAppWriteable(curTypeIdx) || Register.isIOSW(curTypeIdx)) {
                if(Register.is32Bits(curTypeIdx) || Register.is48Bits(curTypeIdx) || Register.is64Bits(curTypeIdx)) {
                    spinFC.setAdapter(fcwAdapter);
                } else{
                    spinFC.setAdapter(fcw4Adapter);
                    spinFC.setSelection(1);
                }
            } else {
                spinFC.setAdapter(fc4Adapter);
                spinFC.setSelection(2);
            }

            int numLaddrs = spinLaddr.getCount();
            if(numLaddrs > 3 && Register.is64Bits(curTypeIdx)) {
                spinLaddr.setSelection(3);
            } else if(numLaddrs > 2 && Register.is48Bits(curTypeIdx)) {
                spinLaddr.setSelection(2);
            } else if(numLaddrs > 1 && Register.is32Bits(curTypeIdx)) {
                spinLaddr.setSelection(1);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
        }
    };

    private class SaveTimerTask extends TimerTask {
        private Register register;
        private boolean addMore;

        public SaveTimerTask(Register register, boolean addMore) {
            this.register = register;
            this.addMore = addMore;
        }

        public void run() {
            try {
                JSONObject jObject = JSONReq.multipart(context, "PUT", PrjCfg.CLOUD_URL + "/api/device/edit", register.toMultiPart("ADD", curDevice.sn));
                //Log.e(Dbg._TAG_(), jObject.toString());
                if (jObject == null) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_save));
                } else if (jObject.getInt("code") == 400) {
                    if(!jObject.isNull("desc") && jObject.getString("desc").trim().matches("The register address has been used")) {
                        MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_dup_register));
                    } else if(!jObject.isNull("desc") && jObject.getString("desc").trim().matches("Device is logging")) {
                        MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.usb_logging));
                    } else if(!jObject.isNull("desc") && jObject.getString("desc").trim().matches("No more available registers")) {
                        MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.reach_register_limit));
                    } else {
                        MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_invalid_str));
                    }
                } else if (jObject.getInt("code") != 200) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_save));
                    return;
                } else {
                    if(Register.is64Bits(register.type)) {
                        numFreeRegs = numFreeRegs - 4;
                    } else if(Register.is48Bits(register.type)) {
                        numFreeRegs = numFreeRegs - 3;
                    } else if(Register.is32Bits(register.type)) {
                        numFreeRegs = numFreeRegs - 2;
                    } else {
                        numFreeRegs = numFreeRegs - 1;
                    }
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_save));
                    if (addMore) {
                        MHandler.exec(mHandler, MHandler.NEXT_ONE, register);
                    } else {
                        MHandler.exec(mHandler, MHandler.GO_BACK);
                    }
                }
            } catch (Exception e) {
                if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
                } else {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_save));
                }
                e.printStackTrace();
            } finally {
                mDialog.dismiss();
                saveTimer.cancel();
            }
        }
    }

    private class GetUsedRegTimerTask extends TimerTask {
        public void run() {
            try {
                JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/device/reg/used/" + curDevice.sn);
                //Log.e(Dbg._TAG_(), jObject.toString());
                if (jObject.getInt("code") == 404) {  // Not found
                    MHandler.exec(mHandler, MHandler.UPDATE);
                    return;
                } else if (jObject.getInt("code") != 200) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_get_data));
                    return;
                } else {
                    JSONArray regs = jObject.getJSONArray("regs");
                    String[] usedRegs = new String[regs.length()];
                    for (int i = 0; i < regs.length(); i++) {
                        usedRegs[i] = regs.getString(i);
                    }
                    MHandler.exec(mHandler, MHandler.UPDATE, usedRegs);
                }
            } catch (Exception e) {
                MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_get_data));
                e.printStackTrace();
            } finally {
                mDialog.dismiss();
                getUsedRegTimer.cancel();
            }
        }
    }

    private static class MsgHandler extends MHandler {
        public MsgHandler(NewRegisterActivity activity) {
            super(activity);
        }

        public void updateSpinType(NewRegisterActivity activity) {
            // hide some type if no enough free register
            //Log.e(Dbg._TAG_(), "Num Free Reg=" + activity.numFreeRegs);
            activity.typeAdapter.clear();
            if(activity.isSlvDev) {
                activity.typeAdapter.addAll(Register.getMasterTypes(activity.context, activity.numFreeRegs));
            } else {
                activity.typeAdapter.addAll(Register.getAllTypes(activity.context, activity.numFreeRegs));
            }
            //Log.e(Dbg._TAG_(), "num free reg = " + activity.numFreeRegs);
            if(Register.is64Bits(activity.curTypeIdx) && activity.numFreeRegs < 4) {
                activity.btnSaveAndNext.setVisibility(View.INVISIBLE);
            } else if(Register.is48Bits(activity.curTypeIdx) && activity.numFreeRegs < 3) {
                activity.btnSaveAndNext.setVisibility(View.INVISIBLE);
            } else if(Register.is32Bits(activity.curTypeIdx) && activity.numFreeRegs < 2) {
                activity.btnSaveAndNext.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public void handleMessage(Message msg) {
            NewRegisterActivity activity = (NewRegisterActivity) super.mActivity.get();
            switch (msg.what) {
                case MHandler.UPDATE: {
                    activity.haddrAdapter.addAll(Register.getAllRegs(activity.curDevice));
                    activity.laddrAdapter.addAll(Register.getAllRegs(activity.curDevice));
                    if(msg.obj != null) {
                        String[] usedRegs = (String[]) msg.obj;
                        for (int i = 0; i < usedRegs.length; i++) {
                            activity.haddrAdapter.remove(usedRegs[i]);
                            activity.laddrAdapter.remove(usedRegs[i]);
                        }
                        activity.numFreeRegs = activity.curDevice.getNumRegs() - usedRegs.length;
                    } else {
                        activity.numFreeRegs = activity.curDevice.getNumRegs();
                    }
                    activity.haddrAdapter.setDropDownViewResource(R.layout.spinner_item);
                    activity.laddrAdapter.setDropDownViewResource(R.layout.spinner_item);
                    activity.spinHaddr.setAdapter(activity.haddrAdapter);
                    activity.spinLaddr.setAdapter(activity.laddrAdapter);
                    updateSpinType(activity);
                    break;
                }
                case MHandler.NEXT_ONE: {
                    Register register = (Register) msg.obj;
                    activity.editDesc.setText("");
                    activity.ckboxLogging.setChecked(false);
                    updateSpinType(activity);

                    if(activity.isSlvDev) {
                        activity.editHaddr.setText("");
                        activity.editLaddr.setText("");
                    } else {
                        // update high/low adapter for error proof
                        activity.haddrAdapter.remove(register.haddr);
                        activity.laddrAdapter.remove(register.haddr);
                        if (register.laddr != null && !register.laddr.equals("")) {
                            activity.haddrAdapter.remove(register.laddr);
                            activity.laddrAdapter.remove(register.laddr);
                        }
                        if (register.iaddr != null && !register.iaddr.equals("")) {
                            activity.haddrAdapter.remove(register.iaddr);
                            activity.laddrAdapter.remove(register.iaddr);
                        }
                        if (register.jaddr != null && !register.jaddr.equals("")) {
                            activity.haddrAdapter.remove(register.jaddr);
                            activity.laddrAdapter.remove(register.jaddr);
                        }
                    }
                    activity.spinDevType.setSelection(0);
                    activity.txtIOSW.setText("");
                    break;
                } default: {
                    super.handleMessage(msg);
                }
            }
        }
    }
}
