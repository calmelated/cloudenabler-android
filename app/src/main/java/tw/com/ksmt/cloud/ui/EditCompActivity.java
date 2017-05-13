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

import com.avos.avoscloud.okhttp.RequestBody;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Company;
import tw.com.ksmt.cloud.iface.ListItem;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.StrUtils;
import tw.com.ksmt.cloud.libs.Utils;

public class EditCompActivity extends ActionBarActivity implements AdapterView.OnItemClickListener, PullToRefreshBase.OnRefreshListener2<ListView> {
    private Context context = EditCompActivity.this;
    private ActionBar actionBar;
    private Timer pollingTimer;
    private Timer saveTimer;
    private Timer removeTimer;
    private MsgHandler mHandler;
    private ProgressDialog mDialog;
    private EditCompAdapter adapter;
    private PullToRefreshListView prListView;
    private ListView listView;
    private String companyId;
    private Company company;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        setContentView(R.layout.pull_list_view);
        setTitle(getString(R.string.edit_company));
        mHandler = new MsgHandler(this);
        mDialog = Kdialog.getProgress(context, mDialog);

        // Decide Company ID
        String subCompID = Utils.loadPrefs(context, "SubCompID", "0");
        if(!subCompID.equals("0")) {
            companyId = subCompID;
        } else {
            companyId = Utils.loadPrefs(context, "CompanyID");
        }

        // Now find the PullToRefreshLayout to setup
        prListView = (PullToRefreshListView) findViewById(R.id.listView);
        prListView.setOnRefreshListener(this);

        // Actual ListView
        adapter = new EditCompAdapter(context, this);
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
        if (removeTimer != null) {
            removeTimer.cancel();
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
        final ListItem listItem = (ListItem) adapter.getItem(position - 1);
        if(listItem.type == ListItem.INPUT) {
            int layout = R.layout.input_text;
            final LayoutInflater inflater = LayoutInflater.from(context);
            final View diagView = inflater.inflate(layout, null);
            final EditText editText = (EditText) diagView.findViewById(R.id.text);
            editText.setFilters(Utils.arrayMerge(editText.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
            if(!listItem.value.equals(getString(R.string.none))) {
                editText.setText(listItem.value);
            }
            //editText.setHint(getString(R.string.name));

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
                            if(!StrUtils.validateInput(StrUtils.IN_TYPE_STRING, input)) {
                                editText.setError(getString(R.string.err_msg_invalid_str));
                                return;
                            }
                            if(listItem.key.equals(context.getString(R.string.company_name))) {
                                if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, input)) {
                                    editText.setError(getString(R.string.err_msg_empty));
                                    return;
                                } else if(input.equals(company.name)) {
                                    dialog.dismiss();
                                    return;
                                }
                            }
                            if (noError) {
                                try {
                                    if (listItem.key.equals(context.getString(R.string.contact_company))) {
                                        company.ct_company = input;
                                    } else if(listItem.key.equals(context.getString(R.string.contact_person))) {
                                        company.ct_person = input;
                                    } else if(listItem.key.equals(context.getString(R.string.contact_phone))) {
                                        company.ct_phone = input;
                                    } else if(listItem.key.equals(context.getString(R.string.contact_email))) {
                                        company.ct_email = input;
                                    } else if(listItem.key.equals(context.getString(R.string.company_name))) {
                                        if(!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, input)) {
                                            throw new Exception();
                                        }
                                        company.newName = input;
                                    }
                                    if(listItem.key.equals(context.getString(R.string.company_name))) {
                                        makeSureRename();
                                    } else {
                                        setSaveTimer();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                } finally {
                                    dialog.dismiss();
                                }
                            }
                        }
                    });
                }
            });
            dialog.show();
        } else if(listItem.type == ListItem.BUTTON) {
            if (listItem.key.equals(context.getString(R.string.company_remove))) {
                Kdialog.getMakeSureDialog(context, getString(R.string.make_sure_delete))
                .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeCompTimer();
                    }
                }).show();
            }
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


    public void makeSureRename() {
        Kdialog.getMakeSureDialog(context, getString(R.string.make_sure_rename_company))
        .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mDialog = Kdialog.getProgress(context, mDialog);
                saveTimer = new Timer();
                saveTimer.schedule(new SaveTimerTask("/api/company/rename"), 0, PrjCfg.RUN_ONCE);
            }
        })
        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                company.newName = null;
            }
        })
        .show();
    }

    public void setPollingTimer() {
        MHandler.exec(mHandler, MHandler.CLEAR_LIST);
        pollingTimer = new Timer();
        pollingTimer.schedule(new PollingTimerTask(), 0, PrjCfg.RUN_ONCE);
    }

    public void setSaveTimer() {
        mDialog = Kdialog.getProgress(context, mDialog);
        saveTimer = new Timer();
        saveTimer.schedule(new SaveTimerTask("/api/company/edit"), 0, PrjCfg.RUN_ONCE);
    }

    private class SaveTimerTask extends TimerTask {
        RequestBody reqData;
        boolean doRename;
        String url;

        public SaveTimerTask(String url) {
            this.url = url;
            reqData = company.toMultiPart(url);
            doRename = url.equalsIgnoreCase("/api/company/rename") ? true : false;
        }

        public void run() {
            try {
                JSONObject jObject = JSONReq.multipart(context, "PUT", PrjCfg.CLOUD_URL + url, reqData);
                int code = jObject.getInt("code");
                if(code == 200 && doRename) {
                    String subCompID = Utils.loadPrefs(context, "SubCompID");
                    final boolean inSubComp = (subCompID == null || subCompID.equals("0") || subCompID.equals("")) ? false : true;
                    if(!inSubComp) { // Admin in subcompany -> No need logout
                        Utils.savePrefs(context, "Company", company.newName);
                        MHandler.exec(mHandler, MHandler.BEEN_LOGOUT);
                        MHandler.exec(mHandler, MHandler.SHOW_MSG, getString(R.string.success_save) + ", " + getString(R.string.company_name) + ": " + company.newName);
                        return;
                    }
                } else if(code == 200) {
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_save));
                } else if(code == 400 && !jObject.isNull("desc") && jObject.getString("desc").trim().matches("The company already exists")) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_company_existed));
                } else {
                    mDialog.dismiss();
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_save));
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
                JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/company/id/" + companyId);
                if (jObject == null || jObject.getInt("code") != 200) {
                    MHandler.exec(mHandler, MHandler.GO_BACK, getString(R.string.err_get_data));
                    return;
                }
                JSONObject jCompany = jObject.getJSONObject("company");
                company = new Company(jCompany);
                MHandler.exec(mHandler, MHandler.UPDATE);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                pollingTimer.cancel();
                mDialog.dismiss();
            }
        }
    }

    public void removeCompTimer() {
        mDialog = Kdialog.getProgress(context, mDialog);
        removeTimer = new Timer();
        removeTimer.schedule(new RemoveCompanyTimerTask(), 0, PrjCfg.RUN_ONCE);
    }

    private class RemoveCompanyTimerTask extends TimerTask {
        public void run() {
            try {
                JSONObject jObject = JSONReq.send(context, "DELETE", PrjCfg.CLOUD_URL + "/api/company/id/" + companyId);
                if (jObject == null || jObject.getInt("code") != 200) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_remove));
                } else {
                    MHandler.exec(mHandler, MHandler.LOGOUT);
                }
            } catch (Exception e) {
                if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
                }
                e.printStackTrace();
            } finally {
                mDialog.dismiss();
                removeTimer.cancel();
            }
        }
    }

    private static class MsgHandler extends MHandler {
        public MsgHandler(EditCompActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            EditCompActivity activity = (EditCompActivity) super.mActivity.get();
            switch (msg.what) {
                case MHandler.UPDATE: {
                    activity.adapter.addList(activity.company);
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