package tw.com.ksmt.cloud.ui;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Account;
import tw.com.ksmt.cloud.iface.Auth;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.SearchViewTask;
import tw.com.ksmt.cloud.libs.WebUtils;

public class AccountPermissionFragment extends Fragment implements AdapterView.OnItemClickListener, PullToRefreshBase.OnRefreshListener2<ListView>, SearchView.OnQueryTextListener {
    private AccountPager context;
    private Timer pollingTimer;
    private Timer saveTimer;
    private MsgHandler mHandler;
    private ProgressDialog mDialog;
    private AccountPermissionAdapter adapter;
    private PullToRefreshListView prListView;
    private ListView listView;
    protected Account editAcnt;
    private int authType;
    private SearchViewTask searchTask;
    private SearchView mSearchView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        editAcnt = (Account) getArguments().getSerializable("account");
        authType = getArguments().getInt("authType");
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
        adapter = new AccountPermissionAdapter(context, this);
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
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        context.getMenuInflater().inflate(R.menu.account_permission, menu);

        MenuItem searchItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setQuery("", false);
        mSearchView.clearFocus();
        mSearchView.setOnQueryTextListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.select_all) {
            setSaveTimer(adapter.getAllItem(), true);
        } else if(id == R.id.cancel_all) {
            setSaveTimer(adapter.getAllItem(), false);
        }
        return true;
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Log.e(Dbg._TAG_(), "pos: " + position + ", id: " + id);
        Auth auth = (Auth) adapter.getItem(position - 1);
        auth.enable = !auth.enable;
        setSaveTimer(auth);
    }


    public void setPollingTimer() {
        //Log.e(Dbg._TAG_(), "auth type=" + getString(authType));
        MHandler.exec(mHandler, MHandler.CLEAR_LIST);
        pollingTimer = new Timer();
        pollingTimer.schedule(new PollingTimerTask(), 0, PrjCfg.RUN_ONCE);
    }

    public void setSaveTimer(Auth auth) {
        //mDialog = Kdialog.getProgress(context, mDialog);
        //saveTimer = new Timer();
        new Timer().schedule(new SaveTimerTask(auth), 0, PrjCfg.RUN_ONCE);
    }

    public void setSaveTimer(List<Auth> authList, boolean enableAll) {
        mDialog = Kdialog.getProgress(context, mDialog);
        saveTimer = new Timer();
        saveTimer.schedule(new SaveTimerTask(authList, enableAll), 0, PrjCfg.RUN_ONCE);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        //Log.e(Dbg._TAG_(), "text changed: " + query);
        if (searchTask != null) {
            searchTask.cancel(true);
        }
        searchTask = new SearchViewTask(adapter, mHandler, query);
        searchTask.execute();
        return false;
    }

    private class SaveTimerTask extends TimerTask {
        private Auth auth;
        private List<Auth> authList;
        private boolean enableAll;

        public SaveTimerTask(Auth auth) {
            this.auth = auth;
            this.authList = null;
        }

        public SaveTimerTask(List<Auth> authList, boolean enableAll) {
            this.auth = null;
            this.authList = authList;
            this.enableAll = enableAll;
        }

        private boolean sendJson(Auth auth, boolean enable) throws Exception {
            String uri = PrjCfg.CLOUD_URL + "/api/user/auth";
            List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();
            params.add(new BasicNameValuePair("account", editAcnt.account));
            params.add(new BasicNameValuePair("deviceId", Integer.toString(auth.deviceId)));
            params.add(new BasicNameValuePair("enable", enable ? "1" : "0"));
            if(authType == Auth.ALARM) {
                params.add(new BasicNameValuePair("type", "enAlarm"));
            } else if(authType == Auth.CONTROL) {
                params.add(new BasicNameValuePair("type", "enControl"));
            } else if(authType == Auth.MONITOR) {
                params.add(new BasicNameValuePair("type", "enMonitor"));
            }
            JSONObject jObject = JSONReq.send(context, "PUT", uri, params);
            if (jObject == null || jObject.getInt("code") != 200) {
                MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_save));
                return false;
            } else {
                return true;
            }
        }

        public void run() {
            try {
                if(auth != null) {
                    if(sendJson(auth, auth.enable)) { // success
                        MHandler.exec(mHandler, MHandler.UPDATE);
                        MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_save));
                    }
                    return;
                } else {
                    for (Auth auth : authList) {
                        if (!sendJson(auth, this.enableAll)) {
                            return;
                        }
                    }
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_save));
                }
            } catch (Exception e) {
                if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
                }
                e.printStackTrace();
            } finally {
                //mDialog.dismiss();
                if(auth == null) { // select/unselect all
                    setPollingTimer();
                    saveTimer.cancel();
                }
            }
        }
    }

    private class PollingTimerTask extends TimerTask {
        public void run() {
            try {
                JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/user/auth/" + WebUtils.encode(editAcnt.account));
                int statCode = jObject.getInt("code");
                if(statCode == 404) { // no any registered device
                    return;
                } else if(statCode != 200) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_get_data));
                    return;
                }
                JSONArray jDevices = jObject.getJSONArray("devices");
                for (int i = 0; i < jDevices.length(); i++) {
                    MHandler.exec(mHandler, MHandler.ADD_LIST, (new Auth(authType, jDevices.getJSONObject(i))));
                }
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
        private AccountPermissionFragment fragment;

        public MsgHandler(AccountPermissionFragment fragment) {
            super(fragment.context);
            this.fragment = fragment;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MHandler.UPDATE: {
                    fragment.adapter.notifyDataSetChanged();
                    fragment.prListView.onRefreshComplete();
                    break;
                }
                case MHandler.CLEAR_LIST: {
                    fragment.adapter.clearList();
                    break;
                }
                case MHandler.ADD_LIST: {
                    fragment.adapter.addList((Auth) msg.obj);
                    break;
                }
                case MHandler.SRCH_QUERY: {
                    fragment.adapter.setQueryStr((String) msg.obj);
                    break;
                }
                default: {
                    super.handleMessage(msg);
                }
            }
        }
    }

}