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
import tw.com.ksmt.cloud.iface.AdvGP;
import tw.com.ksmt.cloud.iface.Group;
import tw.com.ksmt.cloud.iface.ListItem;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.StrUtils;
import tw.com.ksmt.cloud.libs.Utils;

public class EditGroupActivity extends ActionBarActivity implements AdapterView.OnItemClickListener, PullToRefreshBase.OnRefreshListener2<ListView> {
    private Context context = EditGroupActivity.this;
    private ActionBar actionBar;
    private Timer pollingTimer;
    private Timer saveTimer;
    private MsgHandler mHandler;
    private ProgressDialog mDialog;
    private EditGroupAdapter adapter;
    private PullToRefreshListView prListView;
    private ListView listView;
    protected Group curGroup;  // put new setting in this structure
    protected AdvGP curAdvGP;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        setContentView(R.layout.pull_list_view);
        setTitle(getString(R.string.edit_group));
        Intent intent = getIntent();
        curGroup = (Group) intent.getSerializableExtra("Group");
        curAdvGP = (AdvGP) intent.getSerializableExtra("AdvGP");
        mHandler = new MsgHandler(this);
        mDialog = Kdialog.getProgress(context, mDialog);

        // Now find the PullToRefreshLayout to setup
        prListView = (PullToRefreshListView) findViewById(R.id.listView);
        prListView.setOnRefreshListener(this);

        // Actual ListView
        adapter = new EditGroupAdapter(context, this);
        listView = prListView.getRefreshableView();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AppSettings.reload(context);
    }

//    @Override
//    public void onBackPressed() {
//        Intent intent = new Intent();
//        intent.putExtra("curGroup", curGroup);
//        setResult(RESULT_OK, intent);
//        finish();
//    }

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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Log.e(Dbg._TAG_(), "pos: " + position + ", id: " + id);
        final ListItem listItem = (ListItem) adapter.getItem(position - 1);
        if(listItem.type == ListItem.INPUT) {
            int layout = R.layout.input_text_64;
            final LayoutInflater inflater = LayoutInflater.from(context);
            final View diagView = inflater.inflate(layout, null);
            final EditText editText = (EditText) diagView.findViewById(R.id.text);
            editText.setFilters(Utils.arrayMerge(editText.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
            editText.setText(listItem.value);
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
                            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, input)) {
                                noError = false;
                                editText.setError(getString(R.string.err_msg_empty));
                            }
                            if (noError) {
                                try {
                                    if (listItem.key.equals(context.getString(R.string.group_name))) {
                                        if(!StrUtils.validateInput(StrUtils.IN_TYPE_STRING, input)) {
                                            throw new Exception();
                                        }
                                        if(curGroup == null) {
                                            curAdvGP.name = input;
                                        } else {
                                            curGroup.origName = curGroup.name;
                                            curGroup.name = input;
                                        }
                                        setSaveTimer();
                                    }
                                    dialog.dismiss();
                                } catch (Exception e) {
                                    if(listItem.key.equals(context.getString(R.string.group_name))) {
                                        editText.setError(getString(R.string.err_msg_invalid_str));
                                    }
                                    e.printStackTrace();
                                }
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

    private class SaveTimerTask extends TimerTask {
        public void run() {
            try {
                JSONObject jObject;
                if(curAdvGP == null) {
                    jObject = JSONReq.multipart(context, "PUT", PrjCfg.CLOUD_URL + "/api/group/rename", curGroup.toMultiPart());
                } else {
                    jObject = JSONReq.multipart(context, "PUT", PrjCfg.CLOUD_URL + "/api/advgp/edit/" + curAdvGP.id, curAdvGP.toNameMultiPart());
                }
//                Log.e(Dbg._TAG_(), jObject.toString());
                int code = jObject.getInt("code");
                if(code == 200) {
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_save));
                } else {
                    if(curGroup == null) {
                    } else {
                        curGroup.name = curGroup.origName; // reset
                    }
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
//                curGroup.origName = curGroup.name;
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
        public MsgHandler(EditGroupActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            EditGroupActivity activity = (EditGroupActivity) super.mActivity.get();
            switch (msg.what) {
                case MHandler.UPDATE: {
                    if(activity.curGroup == null) {
                        activity.adapter.addList(activity.curAdvGP);
                    } else {
                        activity.adapter.addList(activity.curGroup);
                    }
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