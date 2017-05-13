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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Account;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.StrUtils;
import tw.com.ksmt.cloud.libs.Utils;

public class NewAccountActivity extends ActionBarActivity {
    private Context context = NewAccountActivity.this;
    private ActionBar actionBar;
    private ProgressDialog mDialog;
    private Timer saveTimer;
    private MsgHandler mHandler;

    private EditText editAccount;
    private EditText editName;
    private EditText editPassword;
    private EditText editConfirmPswd;
    private CheckBox ckboxActivate;
    private Spinner  spinUserType;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("  " + getString(R.string.new_account));
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        actionBar.setIcon(R.drawable.ic_new_user_1);
        actionBar.setDisplayShowHomeEnabled(true);

        AppSettings.setupLang(context);
        setContentView(R.layout.account_manage);
        mHandler = new MsgHandler(this);

        editAccount = (EditText) findViewById(R.id.editAccount);
        editName = (EditText) findViewById(R.id.editName);
        editPassword = (EditText) findViewById(R.id.editPassword);
        editConfirmPswd = (EditText) findViewById(R.id.editConfirmPswd);
        editAccount.setFilters(Utils.arrayMerge(editAccount.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
        editName.setFilters(Utils.arrayMerge(editName.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
        editPassword.setFilters(Utils.arrayMerge(editPassword.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
        editConfirmPswd.setFilters(Utils.arrayMerge(editConfirmPswd.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));

        Button btnSave = (Button) findViewById(R.id.btnConfirm);
        btnSave.setOnClickListener(btnSaveListener);

        ckboxActivate = (CheckBox) findViewById(R.id.ckboxActivate);
        if(!PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_KSMT_TEST_ALL)){
            ckboxActivate.setVisibility(View.GONE);
        }

        ArrayAdapter<String> userTypeAdapterAdmin = new ArrayAdapter<String>(context, R.layout.spinner);
        userTypeAdapterAdmin.add(getString(R.string.user));
        userTypeAdapterAdmin.add(getString(R.string.admin));
        if(PrjCfg.EN_TRIAL_LOGIN) {
            userTypeAdapterAdmin.add(getString(R.string.trial_account));
        }
        userTypeAdapterAdmin.setDropDownViewResource(R.layout.spinner_item);

        ArrayAdapter<String> userTypeAdapter = new ArrayAdapter<String>(context, R.layout.spinner);
        userTypeAdapter.add(getString(R.string.user));
        if(PrjCfg.EN_TRIAL_LOGIN) {
            userTypeAdapter.add(getString(R.string.trial_account));
        }
        userTypeAdapter.setDropDownViewResource(R.layout.spinner_item);

        spinUserType = (Spinner) findViewById(R.id.spinUserType);
        String subCompID = Utils.loadPrefs(context, "SubCompID", "0");
        String curUser = Utils.loadPrefs(context, "UserName");
        if(curUser.equals("Admin") || !subCompID.equals("0")) { // First Admin, or Parent company Admin
            spinUserType.setAdapter(userTypeAdapterAdmin);
        } else { // general amdin
            spinUserType.setAdapter(userTypeAdapter);
        }
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
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDialog != null) {
            mDialog.dismiss();
        }
        if (saveTimer != null) {
            saveTimer.cancel();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AppSettings.reload(context);
    }

    View.OnClickListener btnSaveListener = new Button.OnClickListener() {
        public void onClick(View v) {
            String name = editName.getText().toString();
            String account = editAccount.getText().toString();
            String password = editPassword.getText().toString();
            String confirmPswd = editConfirmPswd.getText().toString();
            boolean chkActivate = ckboxActivate.isChecked();
            String userType = spinUserType.getSelectedItem().toString();

            Boolean noError = true;
            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, name)) {
                noError = false;
                editName.setError(getString(R.string.err_msg_empty));
            } else if (name.equalsIgnoreCase("Admin")) {
                noError = false;
                editName.setError(getString(R.string.err_msg_name_reserved));
            } else if (!StrUtils.validateInput(StrUtils.IN_TYPE_STRING, name)) {
                noError = false;
                editName.setError(getString(R.string.err_msg_invalid_str));
            }

            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, account)) {
                noError = false;
                editAccount.setError(getString(R.string.err_msg_empty));
            } else  if (!StrUtils.validateInput(StrUtils.IN_TYPE_EMAIL, account)) {
                noError = false;
                editAccount.setError(getString(R.string.err_msg_not_email));
            }

            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, password)) {
                noError = false;
                editPassword.setError(getString(R.string.err_msg_empty));
            } else if (!StrUtils.validateInput(StrUtils.IN_TYPE_STRONG_PSWD, password)) {
                noError = false;
                editPassword.setError(getString(R.string.err_msg_strong_password));
            }

            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, confirmPswd)) {
                noError = false;
                editConfirmPswd.setError(getString(R.string.err_msg_empty));
            } else if (!password.equals(confirmPswd)) {
                noError = false;
                editConfirmPswd.setError(getString(R.string.err_msg_password_not_equal));
            }

            boolean chkAdmin = true;
            boolean trial = false;
            if(userType.equals(getString(R.string.admin))) {
                chkAdmin = true;
            } else if(userType.equals(getString(R.string.user))) {
                chkAdmin = false;
                chkActivate = false;
            } else if(userType.equals(getString(R.string.trial_account))) {
                chkAdmin = false;
                trial = true;
                chkActivate = true;
            } else {
                noError = false;
            }

            if (noError) {
                mDialog = Kdialog.getProgress(context, mDialog);
                saveTimer = new Timer();
                saveTimer.schedule(new SaveTimerTask(new Account(name, account, password, chkAdmin, chkActivate, trial)), 0, PrjCfg.RUN_ONCE);
            }
        }
    };

    protected class SaveTimerTask extends TimerTask {
        private Account account;

        public SaveTimerTask(Account account) {
            this.account = account;
        }

        public void run() {
            try {
                JSONObject jObject = JSONReq.multipart(context, "POST", PrjCfg.CLOUD_URL + "/api/user/add", account.toMultiPart());
                if (jObject == null || jObject.getInt("code") != 200) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_save));
                    return;
                } else {
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_create));
                    MHandler.exec(mHandler, MHandler.GO_BACK);
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

    private static class MsgHandler extends MHandler {
        public MsgHandler(NewAccountActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            NewAccountActivity activity = (NewAccountActivity) super.mActivity.get();
            switch (msg.what) {
                case MHandler.UPDATE: {
                    break;
                } default: {
                    super.handleMessage(msg);
                }
            }
        }
    }
}
