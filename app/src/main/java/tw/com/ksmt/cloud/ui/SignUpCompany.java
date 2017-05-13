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
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.MainApp;
import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.SignUp;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.StrUtils;
import tw.com.ksmt.cloud.libs.Utils;

public class SignUpCompany extends ActionBarActivity {
    private Context context = SignUpCompany.this;
    private ActionBar actionBar;
    private ProgressDialog mDialog;
    private Timer saveTimer;
    private MsgHandler mHandler;

    private EditText editCompany;
    private EditText editAccount;
    private EditText editPassword;
    private EditText editConfirmPswd;
    private String companyId;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AppSettings.reload(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        actionBar.setIcon(R.drawable.ic_new_company_1);
        actionBar.setDisplayShowHomeEnabled(true);
        AppSettings.setupLang(context);
        setContentView(R.layout.signup_company);
        mHandler = new MsgHandler(this);

        // Set title
        companyId = getIntent().getStringExtra("CompanyID");
        if(companyId == null || companyId.equals("")) {
            setTitle("  " + getString(R.string.signup_company));
            companyId = null;
        } else {
            setTitle("  " + getString(R.string.signup_subsidiary));
        }

        editCompany = (EditText) findViewById(R.id.editCompany);
        editAccount = (EditText) findViewById(R.id.editAccount);
        editPassword = (EditText) findViewById(R.id.editPassword);
        editConfirmPswd = (EditText) findViewById(R.id.editConfirmPswd);
        editCompany.setFilters(Utils.arrayMerge(editCompany.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
        editAccount.setFilters(Utils.arrayMerge(editAccount.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
        editPassword.setFilters(Utils.arrayMerge(editPassword.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
        editConfirmPswd.setFilters(Utils.arrayMerge(editConfirmPswd.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
        Button btnSave = (Button) findViewById(R.id.btnConfirm);
        btnSave.setOnClickListener(btnSaveListener);
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
    public void onBackPressed() {
        if(companyId == null) {
            Intent intent = new Intent(context, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        finish();
    }

    View.OnClickListener btnSaveListener = new Button.OnClickListener() {
        public void onClick(View v) {
            String company = editCompany.getText().toString().trim();
            String account = editAccount.getText().toString().trim();
            String password = editPassword.getText().toString();
            String confirmPswd = editConfirmPswd.getText().toString();
            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, company)) {
                editCompany.setError(getString(R.string.err_msg_empty));
                return;
            }

            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, account)) {
                editAccount.setError(getString(R.string.err_msg_empty));
                return;
            }
            if (!StrUtils.validateInput(StrUtils.IN_TYPE_LEAST_6_LETTERS, account)) {
                editAccount.setError(getString(R.string.err_msg_required_6));
                return;
            }
            if (!StrUtils.validateInput(StrUtils.IN_TYPE_EMAIL, account)) {
                editAccount.setError(getString(R.string.err_msg_not_email));
                return;
            }

            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, password)) {
                editPassword.setError(getString(R.string.err_msg_empty));
                return;
            }
            if (!StrUtils.validateInput(StrUtils.IN_TYPE_STRONG_PSWD, password)) {
                editPassword.setError(getString(R.string.err_msg_strong_password));
                return;
            }

            if (!password.equals(confirmPswd)) {
                editPassword.setError(getString(R.string.err_msg_password_not_equal));
                editConfirmPswd.setError(getString(R.string.err_msg_password_not_equal));
                return;
            }

            // Prepare Sign Up class
            SignUp signUp = new SignUp(company, account, password);
            if(companyId != null && !companyId.equals("")) {
                signUp.parentId = companyId;
            }

            mDialog = Kdialog.getProgress(context, mDialog);
            saveTimer = new Timer();
            saveTimer.schedule(new SaveTimerTask(signUp), 0, PrjCfg.RUN_ONCE);
        }
    };
    private class SaveTimerTask extends TimerTask {
        private SignUp signUp;

        public SaveTimerTask(SignUp signUp) {
            this.signUp = signUp;
        }

        public void run() {
            try {
                JSONObject jObject = JSONReq.multipart(context, "POST", PrjCfg.CLOUD_URL + "/api/company/add", signUp.toMultiPart());
                //Log.e(Dbg._TAG_(), jObject.toString());
                if(MainApp.SERV_MAINT > (new Date().getTime() / 1000)) {
                    MHandler.exec(mHandler, MHandler.SERV_MAINT);
                    return;
                } else if (!MainApp.INET_CONNECTED) {
                    MHandler.exec(mHandler, MHandler.INET_FAILED);
                    return;
                } else if (jObject.getInt("code") == 503) {
                    MHandler.exec(mHandler, MHandler.SERV_MAINT);
                } else if (jObject.getInt("code") == 200) {
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_sign_up));
                    if(companyId != null) { // subsidiary
                        MHandler.exec(mHandler, MHandler.GO_BACK);
                    } else { // company
                        Utils.savePrefs(context, "Company", signUp.name);
                        Utils.savePrefs(context, "Account", signUp.account);
                        Utils.savePrefs(context, "Password", "");
                        MHandler.exec(mHandler, MHandler.LOGIN);
                    }
                } else {
                    if(jObject.has("desc")) {
                        String descStr = jObject.getString("desc").toLowerCase();
                        if(descStr.indexOf("duplicate") > 0) {
                            MHandler.exec(mHandler, MHandler.TOAST_MSG, context.getString(R.string.err_sign_up_duplicate));
                        } else if(descStr.indexOf("company already exists") > 0) {
                            MHandler.exec(mHandler, MHandler.TOAST_MSG, context.getString(R.string.err_sign_up_duplicate));
                        } else {
                            MHandler.exec(mHandler, MHandler.TOAST_MSG, context.getString(R.string.invalid_val));
                        }
                    } else {
                        MHandler.exec(mHandler, MHandler.TOAST_MSG, context.getString(R.string.err_sign_up));
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

    private void startLogin() {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private static class MsgHandler extends MHandler {
        public MsgHandler(SignUpCompany activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            SignUpCompany activity = (SignUpCompany) super.mActivity.get();
            switch (msg.what) {
                case MHandler.LOGIN: {
                    activity.startLogin();
                    break;
                }
                 default: {
                    super.handleMessage(msg);
                }
            }
        }
    }

}