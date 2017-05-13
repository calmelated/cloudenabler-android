package tw.com.ksmt.cloud.ui;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.ContextMenu;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Account;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.SearchViewTask;
import tw.com.ksmt.cloud.libs.WebUtils;

public class AccountFragment extends Fragment implements AdapterView.OnItemClickListener, PullToRefreshBase.OnRefreshListener2<ListView>, SearchView.OnQueryTextListener {
    private MainPager context;
    private Timer bgTimer;
    private MsgHandler mHandler;
    private ProgressDialog mDialog;
    private AccountAdapter adapter;
    private PullToRefreshListView prListView;
    private ListView listView;
    private SearchViewTask searchTask;
    private SearchView mSearchView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = (MainPager) getActivity();
        mHandler = new MsgHandler(this);
        mDialog = Kdialog.getProgress(context, mDialog);

        // Now find the PullToRefreshLayout to setup
        View view = inflater.inflate(R.layout.pull_list_view, container, false);
        prListView = (PullToRefreshListView) view.findViewById(R.id.listView);
        prListView.setOnRefreshListener(this);

        // Actual ListView
        adapter = new AccountAdapter(context);
        listView = prListView.getRefreshableView();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        registerForContextMenu(listView);
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (bgTimer != null) {
            bgTimer.cancel();
        }
        if (mDialog != null) {
            mDialog.cancel();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        bgTimer = new Timer();
        bgTimer.schedule(new BgTimerTask(), 0, PrjCfg.RUN_ONCE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.account_manage, menu);

        MenuItem searchItem = menu.findItem(R.id.search);
        MenuItemCompat.collapseActionView(searchItem);
        mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setIconified(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.new_account) {
            startActivity(new Intent(context, NewAccountActivity.class));
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Log.e(Dbg._TAG_(), "pos: " + position + ", id: " + id);
        editAccount((Account) adapter.getItem(position - 1));
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

    @Override
    public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
        //Log.e(Dbg._TAG_(), "onRefresh.. ");
        MHandler.exec(mHandler, MHandler.CLEAR_LIST);
        if(bgTimer != null) {
            bgTimer.cancel();
        }
        bgTimer = new Timer();
        bgTimer.schedule(new BgTimerTask(), 0, PrjCfg.STANDARD_POLLING);
    }

    @Override
    public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Account account = (Account) adapter.getItem(info.position - 1);
        menu.setHeaderTitle(account.name);
        menu.add(0, R.id.edit, 0, getString(R.string.edit));
        menu.add(0, R.id.remove, 0, getString(R.string.remove));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo itemInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final Account account = (Account) adapter.getItem(itemInfo.position - 1);
        if (account == null) {
            return true;
        }
        int itemId = item.getItemId();
        if (itemId == R.id.edit) {
            editAccount(account);
        } else if (itemId == R.id.remove) {
            Kdialog.getMakeSureDialog(context, getString(R.string.make_sure_delete))
                    .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mDialog = Kdialog.getProgress(context, mDialog);
                            bgTimer = new Timer();
                            bgTimer.schedule(new BgTimerTask("DELETE", account.account), 0, PrjCfg.RUN_ONCE);
                        }
                    }).show();
        }
        return super.onContextItemSelected(item);
    }

    private void editAccount(Account account) {
        Intent intent = new Intent(context, AccountPager.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("account", account);
        bundle.putSerializable("accList", (ArrayList<Account>) adapter.getList());
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void getAccountList() throws Exception {
        JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/user");
        if (jObject.getInt("code") == 404) {  // Not found
            return;
        } else if (jObject.getInt("code") != 200) {
            MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_get_data));
            return;
        } else { // Successfully get a new list
            MHandler.exec(mHandler, MHandler.CLEAR_LIST);
        }
        JSONArray users = jObject.getJSONArray("users");
        for (int i = 0; i < users.length(); i++) {
            MHandler.exec(mHandler, MHandler.ADD_LIST, (new Account(users.getJSONObject(i))));
        }
    }

    private void deleteAccount(String account) throws Exception {
        JSONObject jObject = JSONReq.send(context, "DELETE", PrjCfg.CLOUD_URL + "/api/user/" + WebUtils.encode(account));
        if (jObject == null || jObject.getInt("code") != 200) {
            MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_remove));
        } else {
            MHandler.exec(mHandler, MHandler.DEL_LIST, account);
            MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_remove));
        }
    }

    private class BgTimerTask extends TimerTask {
        public String cmd;
        public String account;

        public BgTimerTask() {
            this.cmd = "GET";
        }

        public BgTimerTask(String cmd) {
            this.cmd = cmd;
        }

        public BgTimerTask(String cmd, String account) {
            this.cmd = cmd;
            this.account = account;
        }

        public void run() {
            try {
                if(cmd.equals("GET")) {
                    getAccountList();
                } else if(cmd.equals("DELETE")) {
                    deleteAccount(this.account);
                } else {
                    return;
                }
                MHandler.exec(mHandler, MHandler.UPDATE);
            } catch (Exception e) {
                if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
                }
                e.printStackTrace();
            } finally {
                mDialog.dismiss();
                bgTimer.cancel();
            }
        }
    }

    private static class MsgHandler extends MHandler {
        private AccountFragment fragment;

        public MsgHandler(AccountFragment fragment) {
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
                    fragment.adapter.addList((Account) msg.obj);
                    break;
                }
                case MHandler.DEL_LIST: {
                    fragment.adapter.remove((String) msg.obj);
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