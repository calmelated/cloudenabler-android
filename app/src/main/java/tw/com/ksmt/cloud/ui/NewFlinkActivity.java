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
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.MainApp;
import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Flink;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.StrUtils;
import tw.com.ksmt.cloud.libs.Utils;

public class NewFlinkActivity extends ActionBarActivity {
    private Context context = NewFlinkActivity.this;
    private ActionBar actionBar;
    private ProgressDialog mDialog;
    private Timer saveTimer;
    private Timer getDataTimer;
    private MsgHandler mHandler;
    private Flink editFlink;
    private boolean isEdit;
    private EditText editDesc;
    private EditText editUrl;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        actionBar.setIcon(R.drawable.ic_file_add_32);
        actionBar.setDisplayShowHomeEnabled(true);

        AppSettings.setupLang(context);
        setContentView(R.layout.flink);
        editFlink = (Flink) getIntent().getSerializableExtra("Flink");
        isEdit = (editFlink == null) ? false : true;
        setTitle("  " + getString((isEdit) ? R.string.edit_flink: R.string.new_flink));
        mHandler = new MsgHandler(this);

        editDesc = (EditText) findViewById(R.id.editDesc);
        editUrl = (EditText) findViewById(R.id.editUrl);
        editDesc.setFilters(Utils.arrayMerge(editDesc.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
        editUrl.setFilters(Utils.arrayMerge(editUrl.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
        Button btnSave = (Button) findViewById(R.id.btnConfirm);
        btnSave.setOnClickListener(btnSaveListener);
        if(isEdit) {
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
            Boolean noError = true;
            String desc = editDesc.getText().toString();
            String url = editUrl.getText().toString();
            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, desc)) {
                noError = false;
                editDesc.setError(getString(R.string.err_msg_empty));
            }
            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, url)) {
                noError = false;
                editUrl.setError(getString(R.string.err_msg_empty));
            }
            if (noError) {
                mDialog = Kdialog.getProgress(context, mDialog);
                saveTimer = new Timer();
                if(isEdit) {
                    editFlink.desc = desc;
                    editFlink.url = url;
                } else {
                    editFlink = new Flink(desc, url);
                }
                saveTimer.schedule(new SaveTimerTask(), 0, PrjCfg.RUN_ONCE);
            }
        }
    };

    protected class SaveTimerTask extends TimerTask {
        public void run() {
            try {
                String method = (isEdit) ? "PUT" : "POST";
                String action = (isEdit) ? "/api/flink/" + editFlink.id : "/api/flink" ;
                JSONObject jObject = JSONReq.multipart(context, method, PrjCfg.CLOUD_URL + action, editFlink.toMultiPart());
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
                JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/flink?id=" + editFlink.id);
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
                JSONArray jArray = jObject.getJSONArray("flinks");
                if(jArray.length() == 0) {
                    MHandler.exec(mHandler, MHandler.GO_BACK, getString(R.string.err_get_data));
                    return;
                }
                editFlink = new Flink(jArray.getJSONObject(0));
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
        public MsgHandler(NewFlinkActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            NewFlinkActivity activity = (NewFlinkActivity) super.mActivity.get();
            switch (msg.what) {
                case MHandler.UPDATE: {
                    activity.editDesc.setText(activity.editFlink.desc);
                    activity.editUrl.setText(activity.editFlink.url);
                    break;
                } default: {
                    super.handleMessage(msg);
                }
            }
        }
    }
}
