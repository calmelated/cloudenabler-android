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
import android.text.Html;
import android.text.InputFilter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.MainApp;
import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Announce;
import tw.com.ksmt.cloud.iface.Company;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.StrUtils;
import tw.com.ksmt.cloud.libs.Utils;

public class NewAnnounceActivity extends ActionBarActivity {
    private Context context = NewAnnounceActivity.this;
    private ActionBar actionBar;
    private ProgressDialog mDialog;
    private Timer saveTimer;
    private Timer getDataTimer;
    private MsgHandler mHandler;
    private Announce editAnnounce;
    private boolean isEdit;
    private EditText editMessage;
    private String companyName;
    private Company company;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        actionBar.setDisplayShowHomeEnabled(true);

        AppSettings.setupLang(context);
        setContentView(R.layout.announce);
        editAnnounce = (Announce) getIntent().getSerializableExtra("Announce");
        isEdit = (editAnnounce == null) ? false : true;
        setTitle(getString((isEdit) ? R.string.edit_announce : R.string.new_announce));
        mHandler = new MsgHandler(this);

        companyName = getIntent().getStringExtra("CompanyName");
        company = (Company) getIntent().getSerializableExtra("Company");
        LinearLayout companyLayout = (LinearLayout) findViewById(R.id.companyPart);
        EditText editCompName = (EditText) findViewById(R.id.editCompName);
        if(companyName != null && !companyName.isEmpty()) {
            companyLayout.setVisibility(View.VISIBLE);
            if("AllSubsidiary".equals(companyName)) {
                editCompName.setText(getString(R.string.all_subsidiary));
            } else {
                editCompName.setText(companyName);
            }
        }

        editMessage = (EditText) findViewById(R.id.editMessage);
        editMessage.setFilters(Utils.arrayMerge(editMessage.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
        Button btnSave = (Button) findViewById(R.id.btnConfirm);
        btnSave.setOnClickListener(btnSaveListener);

        if(PrjCfg.USER_MODE == PrjCfg.MODE_USER) {
            btnSave.setVisibility(View.GONE);
            setTitle(getString(R.string.announcement));
            actionBar.setIcon(0);
            editMessage.setEnabled(false);
            editMessage.setTextColor(Color.BLACK);
        }

        if(isEdit) {
            actionBar.setSubtitle(Html.fromHtml("<small><small>" + Utils.unix2Datetime(editAnnounce.time, "yyyy-MM-dd HH:mm") + "</small></small>"));
            mDialog = Kdialog.getProgress(context, mDialog);
            getDataTimer = new Timer();
            getDataTimer.schedule(new GetDataTimer(), 0, PrjCfg.RUN_ONCE);
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AppSettings.reload(context);
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
        if (getDataTimer != null) {
            getDataTimer.cancel();
        }
    }

    View.OnClickListener btnSaveListener = new Button.OnClickListener() {
        public void onClick(View v) {
            String message = editMessage.getText().toString();
            Boolean noError = true;
            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, message)) {
                noError = false;
                editMessage.setError(getString(R.string.err_msg_empty));
//            } else if (!StrUtils.validateInput(StrUtils.IN_TYPE_STRING, message)) {
//                noError = false;
//                editMessage.setError(getString(R.string.err_msg_invalid_str));
            }
            if (noError) {
                mDialog = Kdialog.getProgress(context, mDialog);
                saveTimer = new Timer();
                if(isEdit) {
                    editAnnounce.message = message;
                } else {
                    editAnnounce = new Announce(message);
                }
                if(companyName != null && !companyName.isEmpty()) {
                    if("AllSubsidiary".equals(companyName)) {
                        editAnnounce.toAllSubsidiary = true;
                    } else if(company != null) {
                        editAnnounce.toSubsidiary = true;
                        editAnnounce.company = company;
                    }
                }
                editAnnounce.pushType = PrjCfg.getLCType(context);
                saveTimer.schedule(new SaveTimerTask(), 0, PrjCfg.RUN_ONCE);
            }
        }
    };

    protected class SaveTimerTask extends TimerTask {
        public void run() {
            try {
                String method = (isEdit) ? "PUT" : "POST";
                String action = (isEdit) ? "/api/announce/" + editAnnounce.time : "/api/announce" ;
                JSONObject jObject = JSONReq.multipart(context, method, PrjCfg.CLOUD_URL + action, editAnnounce.toMultiPart());
                if (jObject == null || jObject.getInt("code") != 200) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_save));
                    return;
                } else {
                    int success_msg = (isEdit) ? R.string.success_save : R.string.success_create;
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(success_msg));
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

    private class GetDataTimer extends TimerTask {
        public void run() {
            try {
                JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/announce/" + editAnnounce.time);
                if (Utils.loadPrefs(context, "Password", "").equals("")) {
                    MHandler.exec(mHandler, MHandler.BEEN_LOGOUT);
                    return;
                } else if(MainApp.SERV_MAINT > (new Date().getTime() / 1000)) {
                    MHandler.exec(mHandler, MHandler.SERV_MAINT);
                    return;
                }
                if (jObject == null || jObject.getInt("code") != 200) {
                    MHandler.exec(mHandler, MHandler.GO_BACK, getString(R.string.err_get_data));
                    return;
                }
                JSONObject jAnnounce = jObject.getJSONObject("announce");
                editAnnounce = new Announce(jAnnounce);
                MHandler.exec(mHandler, MHandler.UPDATE);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mDialog.dismiss();
                getDataTimer.cancel();
            }
        }
    }

    private static class MsgHandler extends MHandler {
        public MsgHandler(NewAnnounceActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            NewAnnounceActivity activity = (NewAnnounceActivity) super.mActivity.get();
            switch (msg.what) {
                case MHandler.UPDATE: {
                    activity.editMessage.setText(activity.editAnnounce.message);
                    break;
                } default: {
                    super.handleMessage(msg);
                }
            }
        }
    }
}
