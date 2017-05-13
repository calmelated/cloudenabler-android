package tw.com.ksmt.cloud.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Account;
import tw.com.ksmt.cloud.iface.ListItem;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.StrUtils;
import tw.com.ksmt.cloud.libs.Utils;
import tw.com.ksmt.cloud.libs.WebUtils;

public class AccountEditFragment extends Fragment implements AdapterView.OnItemClickListener, PullToRefreshBase.OnRefreshListener2<ListView> {
    private AccountPager context;
    private Timer pollingTimer;
    private Timer saveTimer;
    private Timer applyTimer;
    private MsgHandler mHandler;
    private ProgressDialog mDialog;
    private AccountEditAdapter adapter;
    private PullToRefreshListView prListView;
    private ListView listView;
    private List<Account> accountList;
    protected Account editAcnt;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        editAcnt = (Account) getArguments().getSerializable("account");
        accountList = (List<Account>) getArguments().getSerializable("accList");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = (AccountPager) getActivity();
        mHandler = new MsgHandler(this);
        mDialog = Kdialog.getProgress(context, mDialog);
        context.setTitle(editAcnt.name);

        // Now find the PullToRefreshLayout to setup
        View view = inflater.inflate(R.layout.pull_list_view, container, false);
        prListView = (PullToRefreshListView) view.findViewById(R.id.listView);
        prListView.setOnRefreshListener(this);

        // Actual ListView
        adapter = new AccountEditAdapter(context, this);
        listView = prListView.getRefreshableView();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        setPollingTimer();
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (pollingTimer != null) {
            pollingTimer.cancel();
        }
        if(applyTimer != null) {
            applyTimer.cancel();
        }
        if (mDialog != null) {
            mDialog.cancel();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Log.e(Dbg._TAG_(), "pos: " + position + ", id: " + id);
        final ListItem listItem = (ListItem) adapter.getItem(position - 1);
        if(listItem.type == ListItem.INPUT) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            final View layout = inflater.inflate(R.layout.input_text, null);
            final EditText editText = (EditText) layout.findViewById(R.id.text);
            editText.setFilters(Utils.arrayMerge(editText.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
            editText.setText(listItem.value);

            final AlertDialog dialog = Kdialog.getDefInputDialog(context).create();
            dialog.setView(layout);
            dialog.setTitle(listItem.key);
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogIface) {
                    Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Boolean noError = true;
                            String input = editText.getText().toString().trim();
                            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, input)) {
                                noError = false;
                                editText.setError(getString(R.string.err_msg_empty));
                            } else if (input.equalsIgnoreCase("Admin")) {
                                noError = false;
                                editText.setError(getString(R.string.err_msg_name_reserved));
                            } else if(!StrUtils.validateInput(StrUtils.IN_TYPE_STRING, input)) {
                                noError = false;
                                editText.setError(getString(R.string.err_msg_invalid_str));
                            }
                            if (noError) {
                                dialog.dismiss();
                                editAcnt.update(input, editAcnt.account, editAcnt.admin, editAcnt.activate);
                                setSaveTimer();
                            }
                        }
                    });
                }
            });
            dialog.show();
        } else if(listItem.type == ListItem.CHECKBOX) {
            if (listItem.key.equals(context.getString(R.string.activate))) {
                editAcnt.activate = !editAcnt.activate;
                setSaveTimer();
            } else if (listItem.key.equals(context.getString(R.string.admin))) {
                editAcnt.admin = !editAcnt.admin;
                setSaveTimer(true);
            }
        } else if(listItem.type == ListItem.NEW_PASSWORD) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            final View layout = inflater.inflate(R.layout.input_new_password, null);
            final EditText editPassword = (EditText) layout.findViewById(R.id.password);
            final EditText editConfimPassword = (EditText) layout.findViewById(R.id.confimPassword);
            editPassword.setFilters(Utils.arrayMerge(editPassword.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
            editConfimPassword.setFilters(Utils.arrayMerge(editConfimPassword.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));

            final AlertDialog dialog = Kdialog.getDefInputDialog(context).create();
            dialog.setView(layout);
            dialog.setTitle(listItem.key);
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogIface) {
                    Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Boolean noError = true;
                            String password = editPassword.getText().toString().trim();
                            String confirmPassword = editConfimPassword.getText().toString().trim();
                            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, password)) {
                                noError = false;
                                editPassword.setError(getString(R.string.err_msg_empty));
                            } else if (!StrUtils.validateInput(StrUtils.IN_TYPE_STRONG_PSWD, password)) {
                                noError = false;
                                editPassword.setError(getString(R.string.err_msg_strong_password));
                            }

                            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, confirmPassword)) {
                                noError = false;
                                editConfimPassword.setError(getString(R.string.err_msg_empty));
                            } else if(!password.equals(confirmPassword)) {
                                noError = false;
                                editPassword.setError(getString(R.string.err_msg_password_not_equal));
                            }
                            if (noError) {
                                dialog.dismiss();
                                editAcnt.update(editAcnt.name, editAcnt.account, password, editAcnt.admin, editAcnt.activate, editAcnt.trial);
                                setSaveTimer();
                            }
                        }
                    });
                }
            });
            dialog.show();
        } else if(listItem.type == ListItem.BUTTON && listItem.key.equals(getString(R.string.apply_permission))) {
            final List<CharSequence> accNameList = new LinkedList<CharSequence>();
            final List<CharSequence> accList = new LinkedList<CharSequence>();
            for(Account account: accountList) {
                if(!account.account.equals(editAcnt.account) && !account.admin) {
                    accList.add(account.account);
                    accNameList.add(account.name);
                }
            }
            final CharSequence[] accNameArray = new CharSequence[accNameList.size()];
            accNameList.toArray(accNameArray);

            final AlertDialog dialog;
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(getString(R.string.apply_permission));
            builder.setIcon(R.drawable.ic_refresh);
            builder.setSingleChoiceItems(accNameArray, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    mDialog = Kdialog.getProgress(context, mDialog);
                    applyTimer = new Timer();
                    applyTimer.schedule(new ApplyPermission(accList.get(which)), 0, PrjCfg.RUN_ONCE);
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
        setSaveTimer(false);
    }

    public void setSaveTimer(boolean resetActivity) {
        mDialog = Kdialog.getProgress(context, mDialog);
        saveTimer = new Timer();
        saveTimer.schedule(new SaveTimerTask(resetActivity), 0, PrjCfg.RUN_ONCE);
    }

    private class SaveTimerTask extends TimerTask {
        private boolean resetActivity = false;

        public SaveTimerTask() {
            this(false);
        }

        public SaveTimerTask(boolean resetActivity) {
            this.resetActivity = resetActivity;
        }

        public void run() {
            try {
                JSONObject jObject = JSONReq.multipart(context, "PUT", PrjCfg.CLOUD_URL + "/api/user/edit", editAcnt.toMultiPart());
                //Log.e(Dbg._TAG_(), jObject.toString());
                if (jObject == null || jObject.getInt("code") != 200) {
                    mDialog.dismiss();
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_save));
                    return;
                } else {
                    if(resetActivity) {
                        MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_save));
                        Utils.restartActivity(context);
                    } else {
                        setPollingTimer();
                        MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_save));
                    }
                }
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
                JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/user/" + WebUtils.encode(editAcnt.account));
                if (jObject == null || jObject.getInt("code") != 200) {
                    MHandler.exec(mHandler, MHandler.GO_BACK, getString(R.string.err_get_account));
                    return;
                }
                JSONObject jUser = jObject.getJSONObject("user");
                editAcnt.update(jUser);
				MHandler.exec(mHandler, MHandler.UPDATE);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                pollingTimer.cancel();
                mDialog.dismiss();
            }
        }
    }

    private class ApplyPermission extends TimerTask {
        private CharSequence account;

        public ApplyPermission(CharSequence account) {
            this.account = account;
        }

        public void run() {
            try {
                JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/user/auth/" + WebUtils.encode((String) account));
                if (jObject == null || jObject.getInt("code") != 200) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_get_account));
                    return;
                }
                JSONObject jUserAuth = new JSONObject();
                jUserAuth.put("account", editAcnt.account);
                jUserAuth.put("devices", jObject.getJSONArray("devices"));
                List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();
                params.add(new BasicNameValuePair("json", jUserAuth.toString()));

                jObject = JSONReq.send(context, "PUT-JSON", PrjCfg.CLOUD_URL + "/api/user/auth", params);
                if (jObject == null || jObject.getInt("code") != 200) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_save));
                } else {
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_save));
                    Utils.restartActivity(context);
                }
            } catch (Exception e) {
                if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
                }
                e.printStackTrace();
            } finally {
                applyTimer.cancel();
                mDialog.dismiss();
            }
        }
    }

    private static class MsgHandler extends MHandler {
        private AccountEditFragment fragment;

        public MsgHandler(AccountEditFragment fragment) {
            super(fragment.context);
            this.fragment = fragment;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MHandler.UPDATE: {
                    fragment.context.setTitle(fragment.editAcnt.name);
                    fragment.adapter.addList(fragment.editAcnt);
                    fragment.adapter.notifyDataSetChanged();
                    fragment.prListView.onRefreshComplete();
                    break;
                }
                case MHandler.CLEAR_LIST: {
                    fragment.adapter.clearList();
                    break;
                }
                default: {
                    super.handleMessage(msg);
                }
            }
        }
    }

}