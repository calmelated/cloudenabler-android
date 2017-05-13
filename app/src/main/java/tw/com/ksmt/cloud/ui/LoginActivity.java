package tw.com.ksmt.cloud.ui;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.MainApp;
import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.libs.Dbg;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.StrUtils;
import tw.com.ksmt.cloud.libs.Utils;
import tw.com.ksmt.cloud.libs.WebUtils;

public class LoginActivity extends ActionBarActivity {
    private Context context = LoginActivity.this;
    private ActionBar actionBar;
    private ProgressDialog mDialog;
    private Timer signInTimer;
    private Timer activateTimer;
    private Timer resetPassTimer;
    private Timer getTrialUserTimer;
    private MsgHandler mHandler;
    private EditText editComany;
    private EditText editAccount;
    private EditText editPassword;
    private EditText editNewPassword;
    private EditText editConfimNewPassword;
    private String account;
    private String company;
    private String password;
    private String newPassowrd;
    private Button btnLogin;
    private AlertDialog newAppDialog;
    private static String lastLocale;

    private void setDefaultVal() {
        String company = Utils.loadPrefs(context, "Company");
        if (company != null) {
            editComany.setText(company);
        }
        String account = Utils.loadPrefs(context, "Account");
        if (account != null) {
            editAccount.setText(account);
        }
        editPassword.setText("");
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        AppSettings.reload(context);
        if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_FULLKEY)) {
            setContentView(R.layout.login_fullkey);
            setTitle(getString(R.string.app_name_fullkey));
        } else { // KSMT
            setContentView(R.layout.login);
        }

        String cloudUrl = PrjCfg.CLOUD_URL;
        if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_YATEC) && PrjCfg.CLOUD_URL.equals(PrjCfg.CLOUD_LINK_YATEC)) {
            cloudUrl = getString(R.string.official_cloud_url);
        } else if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_LILU) && PrjCfg.CLOUD_URL.equals(PrjCfg.CLOUD_LINK_LILU)) {
            cloudUrl = getString(R.string.official_cloud_url);
        } else if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_HYEC) && PrjCfg.CLOUD_URL.equals(PrjCfg.CLOUD_LINK_HYEC)) {
            cloudUrl = getString(R.string.official_cloud_url);
        } else if(PrjCfg.CLOUD_URL.equals(PrjCfg.CLOUD_LINK_KSMT)) {
            cloudUrl = getString(R.string.official_cloud_url);
        }
        getSupportActionBar().setSubtitle(Html.fromHtml("<small><small><i>" + getString(R.string.location) + ": " + ((cloudUrl.equals("")) ? PrjCfg.CLOUD_URL : cloudUrl) + "</i></small></small>"));
        registerReceiver(WebUtils.downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        mHandler = new MsgHandler(this);

        editComany = (EditText) findViewById(R.id.editCompany);
        editAccount = (EditText) findViewById(R.id.editAccount);
        editPassword = (EditText) findViewById(R.id.editPassword);
        editComany.setFilters(Utils.arrayMerge(editComany.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
        editAccount.setFilters(Utils.arrayMerge(editAccount.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
        editPassword.setFilters(Utils.arrayMerge(editPassword.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
        setDefaultVal();

        btnLogin = (Button) findViewById(R.id.buttonEnter);
        btnLogin.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                company = editComany.getText().toString().trim();
                account = editAccount.getText().toString().trim();
                password = editPassword.getText().toString().trim();

                if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, company)) {
                    editComany.setError(getString(R.string.err_msg_empty));
                    return;
                }
                if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, account)) {
                    editAccount.setError(getString(R.string.err_msg_empty));
                    return;
                }
                if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, password)) {
                    editPassword.setError(getString(R.string.err_msg_empty));
                    return;
                }
                password = Utils.getMD5(password + account);
                Utils.savePrefs(context, "Company", company);
                Utils.savePrefs(context, "Account", account);

                mDialog = Kdialog.getProgress(context, mDialog);
                signInTimer = new Timer();
                signInTimer.schedule(new SignInTimerTask(), 0, PrjCfg.RUN_ONCE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();;

        // check language setting
        String lang = Utils.loadPrefs(context, "Language", "");
        if(lastLocale == null) { // first pollTime
            lastLocale = Utils.loadPrefs(context, "Language");
        } else if(!lang.equals(lastLocale)) {
            Log.e(Dbg._TAG_(), "Language changed!");
            lastLocale = Utils.loadPrefs(context, "Language");
            Utils.restartActivity(context);
            return;
        }

        // check lastest version
        WebUtils.checkNewApp(context, mHandler);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AppSettings.reload(context);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mDialog != null) {
            mDialog.dismiss();
        }
        if (signInTimer != null) {
            signInTimer.cancel();
        }
        if (activateTimer != null) {
            activateTimer.cancel();
        }
        if (resetPassTimer != null) {
            resetPassTimer.cancel();
        }
        if (getTrialUserTimer != null) {
            getTrialUserTimer.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(WebUtils.downloadReceiver);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.login, menu);
        if(PrjCfg.EN_TRIAL_LOGIN) {
            MenuItem trialItem = menu.findItem(R.id.trial_login);
            trialItem.setVisible(true);
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.new_company) {
            startActivity(new Intent(context, SignUpCompany.class));
            finish();
        } else if(id == R.id.cloud_status) {
            startActivity(new Intent(context, CloudStatusActivity.class));
        } else if(id == R.id.app_setting) {
            startActivity(new Intent(context, AppSettings.class));
        } else if(id == R.id.reset_password) {
            resetPassword();
        } else if(id == R.id.trial_login) {
            trialLogin();
        }
        return true;
    }

    private void startMainPager() {
        Toast.makeText(context, context.getString(R.string.success_sign_in), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(context, MainPager.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void resetPassword() {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View layout = inflater.inflate(R.layout.input_reset_password, null);
        final EditText editRstCompany = (EditText) layout.findViewById(R.id.editCompany);
        final EditText editRstAccount = (EditText) layout.findViewById(R.id.editAccount);
        editRstCompany.setText(editComany.getText().toString().trim());
        editRstAccount.setText(editAccount.getText().toString().trim());

        final AlertDialog dialog = Kdialog.getDefInputDialog(context).create();
        dialog.setView(layout);
        dialog.setTitle(getString(R.string.reset_password));
        dialog.setCancelable(false);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogIface) {
                Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Boolean noError = true;
                        String company = editRstCompany.getText().toString().trim();
                        if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, company)) {
                            noError = false;
                            editRstCompany.setError(getString(R.string.err_msg_empty));
                        }
                        String account = editRstAccount.getText().toString().trim();
                        if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, account)) {
                            noError = false;
                            editRstAccount.setError(getString(R.string.err_msg_empty));
                        }
                        if (noError) {
                            dialog.dismiss();
                            mDialog = Kdialog.getProgress(context, mDialog);
                            resetPassTimer = new Timer();
                            resetPassTimer.schedule(new ResetTimerTask(company, account), 0, PrjCfg.RUN_ONCE);
                        }
                    }
                });
            }
        });
        dialog.show();
    }

    private void trialLogin() {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View layout = inflater.inflate(R.layout.input_reset_password, null);
        final EditText editRstCompany = (EditText) layout.findViewById(R.id.editCompany);
        final EditText editRstAccount = (EditText) layout.findViewById(R.id.editAccount);
        editRstCompany.setText(editComany.getText().toString().trim());
        editRstAccount.setVisibility(View.GONE);

        final AlertDialog dialog = Kdialog.getDefInputDialog(context).create();
        dialog.setView(layout);
        dialog.setTitle(getString(R.string.trial_login));
        dialog.setCancelable(false);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogIface) {
                Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Boolean noError = true;
                        String company = editRstCompany.getText().toString().trim();
                        if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, company)) {
                            noError = false;
                            editRstCompany.setError(getString(R.string.err_msg_empty));
                        }
                        if (noError) {
                            dialog.dismiss();
                            mDialog = Kdialog.getProgress(context, mDialog);
                            getTrialUserTimer = new Timer();
                            getTrialUserTimer.schedule(new GetTrialUserTask(company), 0, PrjCfg.RUN_ONCE);
                        }
                    }
                });
            }
        });
        dialog.show();
    }

    private void activateAccount(){
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View layout = inflater.inflate(R.layout.input_new_password, null);
        editNewPassword = (EditText) layout.findViewById(R.id.password);
        editConfimNewPassword = (EditText) layout.findViewById(R.id.confimPassword);

        final AlertDialog dialog = Kdialog.getDefInputDialog(context).create();
        dialog.setView(layout);
        dialog.setTitle(getString(R.string.account_activate));
        dialog.setCancelable(false);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogIface) {
                Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Boolean noError = true;
                        String txtNewPassword = editNewPassword.getText().toString().trim();
                        String txtConfirmPassword = editConfimNewPassword.getText().toString().trim();
                        if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, txtNewPassword)) {
                            noError = false;
                            editNewPassword.setError(getString(R.string.err_msg_empty));
                        } else if (!StrUtils.validateInput(StrUtils.IN_TYPE_STRONG_PSWD, txtNewPassword)) {
                            noError = false;
                            editNewPassword.setError(getString(R.string.err_msg_strong_password));
                        }

                        if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, txtConfirmPassword)) {
                            noError = false;
                            editConfimNewPassword.setError(getString(R.string.err_msg_empty));
                        } else if (!txtNewPassword.equals(txtConfirmPassword)) {
                            noError = false;
                            editNewPassword.setError(getString(R.string.err_msg_password_not_equal));
                        }
                        if (noError) {
                            dialog.dismiss();
                            newPassowrd = txtNewPassword;
                            mDialog = Kdialog.getProgress(context, mDialog);
                            activateTimer = new Timer();
                            activateTimer.schedule(new ActivateTimerTask(), 0, PrjCfg.RUN_ONCE);
                        }
                    }
                });
            }
        });
        dialog.show();
    }

    private class SignInTimerTask extends TimerTask {
        public void run() {
            try {
                JSONObject jObject = WebUtils.login(context, company, account, password, true);
                if(jObject == null || !MainApp.INET_CONNECTED) {
                    MHandler.exec(mHandler, MHandler.INET_FAILED);
                    return;
                } else if(MainApp.SERV_MAINT > (new Date().getTime() / 1000)) {
                    MHandler.exec(mHandler, MHandler.SERV_MAINT);
                    return;
                }

                int statCode = jObject.getInt("code");
                if(statCode == 200 || statCode == 401) { // Ok
                    if (!jObject.isNull("rd")) { // No Auth & No activate
                        MHandler.exec(mHandler, MHandler.TOAST_MSG, context.getString(R.string.err_user_sign_in));
                        return;
                    } else if (jObject.has("activate") && !jObject.getBoolean("activate")) { // No Auth & No activate
                        MHandler.exec(mHandler, MHandler.ACCOUNT_ACTIVE);
                        return;
                    }
                    // Get Account information!
                    jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/user/" + WebUtils.encode(account));
                    if(MainApp.SERV_MAINT > (new Date().getTime() / 1000)) {
                        MHandler.exec(mHandler, MHandler.SERV_MAINT);
                    } else if (jObject.getInt("code") != 200) {
                        MHandler.exec(mHandler, MHandler.TOAST_MSG, context.getString(R.string.err_user_sign_in));
                    } else { // 200
                        JSONObject userObject = jObject.getJSONObject("user");
                        boolean isTrialUser = false;
                        int trial = userObject.has("trial") ? userObject.getInt("trial") : 0 ;
                        int admin = userObject.getInt("admin");
                        if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                        } else if(trial == 1){
                            PrjCfg.USER_MODE = PrjCfg.MODE_USER;
                            isTrialUser = true;
                        } else if(admin == 1 || admin == 2){
                            PrjCfg.USER_MODE = PrjCfg.MODE_ADMIN;
                        } else {
                            PrjCfg.USER_MODE = PrjCfg.MODE_USER;
                        }
                        Utils.savePrefs(context, "Password", password);
                        Utils.savePrefs(context, "CompanyID", String.valueOf(userObject.getLong("companyId")));
                        Utils.savePrefs(context, "UserName", userObject.getString("name"));
                        Utils.savePrefs(context, "Admin", admin);
                        Utils.savePrefs(context, "TrialUser", isTrialUser ? "1" : "0");
                        MHandler.exec(mHandler, MHandler.LOGIN);
                    }
                } else { // code: 400 Bad request
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, context.getString(R.string.err_user_sign_in));
                }
            } catch (Exception e) {
                if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
                }
                e.printStackTrace();
            } finally {
                mDialog.dismiss();
                signInTimer.cancel();
            }
        }
    }

    private class ResetTimerTask extends TimerTask {
        private String company;
        private String account;

        public ResetTimerTask(String company, String account) {
            this.company = company;
            this.account = account;
        }

        public void run() {
            try {
                List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();
                params.add(new BasicNameValuePair("company", company));
                params.add(new BasicNameValuePair("account", account));
                JSONObject jObject = JSONReq.send(context, "POST", PrjCfg.CLOUD_URL + "/api/password/reset", params);
                int statusCode = (jObject != null && !jObject.isNull("code")) ? jObject.getInt("code") : -1;
                if(statusCode == 200) {
                    MHandler.exec(mHandler, MHandler.SHOW_MSG, getString(R.string.success_reset_password));
                } else if(statusCode == 401) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_reset_pass_disallow));
                } else {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_no_admin));
                }
            } catch (Exception e) {
                if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
                } else {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_get_data));
                }
                e.printStackTrace();
            } finally {
                mDialog.dismiss();
                resetPassTimer.cancel();
            }
        }
    }

    private class GetTrialUserTask extends TimerTask {
        private String trialComp;

        public GetTrialUserTask(String trialComp) {
            this.trialComp = trialComp;
        }

        public void run() {
            try {
                JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/trials/" + trialComp);
                int statusCode = (jObject != null && !jObject.isNull("code")) ? jObject.getInt("code") : -1;
                if(statusCode == 200) { // Start to login
                    company = trialComp;
                    account = jObject.getString("account");
                    password = jObject.getString("password");
                    Utils.savePrefs(context, "Company", company);
                    Utils.savePrefs(context, "Account", account);

                    signInTimer = new Timer();
                    signInTimer.schedule(new SignInTimerTask(), 0, PrjCfg.RUN_ONCE);
                } else {
                    String desc = jObject.getString("desc");
                    if(statusCode == 404 && desc.matches("No any record")) {
                        MHandler.exec(mHandler, MHandler.SHOW_MSG, getString(R.string.err_noany_trials));
                    } else if(statusCode == 404 && desc.matches("No such company")) {
                        MHandler.exec(mHandler, MHandler.SHOW_MSG, getString(R.string.err_msg_no_such_company));
                    } else if(statusCode == 404 && desc.matches("No available accounts!")) {
                        MHandler.exec(mHandler, MHandler.SHOW_MSG, getString(R.string.err_nomore_trials));
                    } else {
                        MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_get_data));
                    }
                    mDialog.cancel();
                }
            } catch (Exception e) {
                if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
                } else {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_get_data));
                }
                e.printStackTrace();
                mDialog.cancel();
            } finally {
                getTrialUserTimer.cancel();
            }
        }
    }

    private class ActivateTimerTask extends TimerTask {
        public void run() {
            try {
                List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();
                params.add(new BasicNameValuePair("account", account));
                params.add(new BasicNameValuePair("origPswd", password));
                params.add(new BasicNameValuePair("newPswd", newPassowrd));
                JSONObject jObject = JSONReq.send(context, "POST", PrjCfg.CLOUD_URL + "/api/user/activate", params);
                if (jObject == null || jObject.getInt("code") != 200) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_activate_failed));
                } else {
                    MHandler.exec(mHandler, MHandler.SHOW_MSG, getString(R.string.success_activate) + getString(R.string.new_pswd_login));
                    MHandler.exec(mHandler, MHandler.UPDATE_VAL);
                }
            } catch (Exception e) {
                if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
                }
                e.printStackTrace();
            } finally {
                mDialog.dismiss();
                activateTimer.cancel();
            }
        }
    }

    private static class MsgHandler extends MHandler {
        public MsgHandler(LoginActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final LoginActivity activity = (LoginActivity) super.mActivity.get();
            switch (msg.what) {
                case MHandler.UPDATE_APP: {
                    if(activity.newAppDialog == null || !activity.newAppDialog.isShowing()) {
                        activity.newAppDialog = Kdialog.getDefInfoDialog(activity)
                                .setMessage((String) msg.obj)
                                .setNegativeButton(activity.getString(R.string.ignore), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Utils.savePrefs(activity, "AppIgnoreVerCode", Utils.loadPrefs(activity, "AppLatestVerCode"));
                                        activity.newAppDialog.dismiss();
                                    }
                                })
                                .setPositiveButton(activity.getString(R.string.confirm), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Utils.savePrefs(activity, "AppIgnoreVerCode", "");
                                        activity.mDialog = Kdialog.getProgress(activity, activity.getString(R.string.updateding));
                                        WebUtils.installNewApp(activity);
                                    }
                                }).show();
                    }
                    break;
                }
                case MHandler.LOGIN: {
                    activity.startMainPager();
                    break;
                }
                case MHandler.ACCOUNT_ACTIVE: {
                    activity.activateAccount();
                    break;
                }
                case MHandler.UPDATE_VAL: {
                    activity.setDefaultVal();
                    break;
                }
                default: {
                    super.handleMessage(msg);
                }
            }
        }
    }
}
