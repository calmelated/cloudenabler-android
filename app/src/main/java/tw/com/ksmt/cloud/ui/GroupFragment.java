package tw.com.ksmt.cloud.ui;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
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

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Group;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.SearchViewTask;
import tw.com.ksmt.cloud.libs.Utils;
import tw.com.ksmt.cloud.libs.WebUtils;

public class GroupFragment extends Fragment implements AdapterView.OnItemClickListener, PullToRefreshBase.OnRefreshListener2<ListView>, SearchView.OnQueryTextListener {
    private MainPager context;
    private Timer pollingTimer;
    private Timer deleteTimer;
    private MsgHandler mHandler;
    private boolean stopPolling = false;
    private ProgressDialog mDialog;
    private GroupAdapter adapter;
    private PullToRefreshListView prListView;
    private ListView listView;
    private SearchViewTask searchTask;
    protected SearchView mSearchView;
    private boolean isVisibleToUser;

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        this.isVisibleToUser = isVisibleToUser;
        if(this.isVisibleToUser && context != null) {
            onResume();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = (MainPager) getActivity();
        mHandler = new MsgHandler(this);

        // Now find the PullToRefreshLayout to setup
        View view = inflater.inflate(R.layout.pull_list_view, container, false);
        prListView = (PullToRefreshListView) view.findViewById(R.id.listView);
        prListView.setOnRefreshListener(this);

        // Actual ListView
        adapter = new GroupAdapter(context);
        listView = prListView.getRefreshableView();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        registerForContextMenu(listView);
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (pollingTimer != null) {
            pollingTimer.cancel();
        }
        if (deleteTimer != null) {
            deleteTimer.cancel();
        }
        if (mDialog != null) {
            mDialog.cancel();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(!isVisibleToUser) {
            return;
        } else if (Utils.loadPrefs(context, "Password", "").equals("")) {
            return;
        }
        stopPolling = false;
        pollingTimer = new Timer();
        pollingTimer.schedule(new PollingTimerTask(), 0, PrjCfg.GRUOP_POLLING);
        mDialog = Kdialog.getProgress(context, mDialog);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(PrjCfg.USER_MODE == PrjCfg.MODE_ADMIN || PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
            inflater.inflate(R.menu.group, menu);
        } else {
            inflater.inflate(R.menu.main_pager_user, menu);
        }
        MenuItem searchItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setOnQueryTextListener(this);

        if(PrjCfg.EN_ADV_GRP) {
            MenuItem advGPItem = menu.findItem(R.id.advGP);
            if(advGPItem != null) {
                advGPItem.setVisible(true);
            }
        }
        if(PrjCfg.EN_ANNOUNCE) {
            MenuItem announceItem = menu.findItem(R.id.announce);
            if(announceItem != null) {
                announceItem.setVisible(true);
            }
        }
        if(PrjCfg.EN_ANNOUNCE) {
            MenuItem iostLogItem = menu.findItem(R.id.iostlog);
            if (iostLogItem != null) {
                iostLogItem.setVisible(true);
            }
        }
        if(PrjCfg.EN_FLINK) {
            MenuItem flinkItem = menu.findItem(R.id.flink);
            if (flinkItem != null) {
                flinkItem.setVisible(true);
            }
        }

        boolean isSubsidiary = Utils.loadPrefsBool(context, "IsSubsidiary");
        String subCompID = Utils.loadPrefs(context, "SubCompID", "0");
        MenuItem subCompItem = menu.findItem(R.id.subsidiary);
        if(subCompItem != null) {
            subCompItem.setVisible((isSubsidiary || !subCompID.equals("0")) ? false : true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.new_group) {
            if(adapter.getCount() < PrjCfg.MAX_GROUP_NUM) {
                startActivity(new Intent(context, NewGroupMemberActivity.class));
            } else {
                MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_max_group_exceeded));
            }
        } else if (id == R.id.account_manage) {
            startActivity(new Intent(context, AccountActivity.class));
        } else if (id == R.id.notification) {
            startActivity(new Intent(context, NotificationActivity.class));
        } else if (id == R.id.audit) {
            startActivity(new Intent(context, AuditActivity.class));
        } else if (id == R.id.iostlog) {
            startActivity(new Intent(context, IoStLogActivity.class));
        } else if (id == R.id.cloud_status) {
            startActivity(new Intent(context, CloudStatusActivity.class));
        } else if (id == R.id.company) {
            startActivity(new Intent(context, EditCompActivity.class));
        } else if (id == R.id.subsidiary) {
            startActivity(new Intent(context, SubCompActivity.class));
        } else if (id == R.id.announce) {
            startActivity(new Intent(context, AnnounceActivity.class));
        } else if (id == R.id.advGP) {
            startActivity(new Intent(context, AdvGPActivity.class));
        } else if (id == R.id.flink) {
            startActivity(new Intent(context, FlinkActivity.class));
        } else if (id == R.id.settings) {
            startActivity(new Intent(context, AppSettings.class));
        } else if (id == R.id.logout) {
            String subCompID = Utils.loadPrefs(context, "SubCompID");
            final boolean inSubComp = (subCompID == null || subCompID.equals("0") || subCompID.equals("")) ? false : true ;
            final int message = inSubComp ? R.string.logout_subsidiary_message : R.string.logout_message ;
            Kdialog.getDefInfoDialog(context)
            .setMessage(getString(message))
            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            })
            .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(inSubComp) {
                        context.logoutSubsidiary();
                    } else {
                        MHandler.exec(mHandler, MHandler.LOGOUT);
                    }
                }
            })
            .show();
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Log.e(Dbg._TAG_(), "pos: " + position + ", id: " + id);
        groupStatus(position - 1);
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
        if(pollingTimer != null) {
            pollingTimer.cancel();
        }
        pollingTimer = new Timer();
        pollingTimer.schedule(new PollingTimerTask(), 0, PrjCfg.GRUOP_POLLING);
    }

    @Override
    public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if(PrjCfg.USER_MODE == PrjCfg.MODE_USER) {
            return;
        }
        stopPolling = true;
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Group group = (Group) adapter.getItem(info.position - 1);
        menu.setHeaderTitle(group.name);
        menu.add(0, R.id.view, 0, getString(R.string.view));
        menu.add(0, R.id.edit, 0, getString(R.string.edit));
        menu.add(0, R.id.remove, 0, getString(R.string.remove));
    }

    public void onContextMenuClosed(Menu menu) {
        stopPolling = false;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo itemInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int itemId = item.getItemId();
        if (itemId == R.id.view) {
            groupStatus(itemInfo.position - 1);
        } else if (itemId == R.id.edit) {
            groupEdit(itemInfo.position - 1);
        } else if (itemId == R.id.remove) {
            Kdialog.getMakeSureDialog(context, getString(R.string.make_sure_delete))
            .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mDialog = Kdialog.getProgress(context, mDialog);
                    deleteTimer = new Timer();
                    deleteTimer.schedule(new DeleteTimerTask(itemInfo.position - 1), 0, PrjCfg.RUN_ONCE);
                }
            }).show();
        }
        return super.onContextItemSelected(item);
    }

    private class PollingTimerTask extends TimerTask {
        public void run() {
            try {
                if(stopPolling) { return; }
                getGroupList();
                MHandler.exec(mHandler, MHandler.UPDATE);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if(mDialog != null) {
                    mDialog.dismiss();
                }
            }
        }
    }

    private class DeleteTimerTask extends TimerTask {
        private int position;

        public DeleteTimerTask(int position) {
            this.position = position;
        }

        public void run() {
            try {
                deleteGrouop(position);
            } catch (Exception e) {
                if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
                }
                e.printStackTrace();
            } finally {
                mDialog.dismiss();
                deleteTimer.cancel();
            }
        }
    }

    private void getGroupList() throws Exception {
        // Group
        JSONObject jGroupObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/group");
        //Log.e(Dbg._TAG_(), jGroupObject.toString());
        int statCode = jGroupObject.getInt("code");
        if(statCode == 404 || statCode == 401) {
        } else if (statCode != 200) {
            MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.err_group_list));
            return;
        }
        //Log.e(Dbg._TAG_(), jGroupObject.toString());

        // Update group to list
        JSONArray groups = jGroupObject.getJSONArray("groups");
        if(groups == null) {
            return;
        }
        // Clear the last list
        MHandler.exec(mHandler, MHandler.CLEAR_LIST);

        // Add the last data
        for (int i = 0; i < groups.length(); i++) {
            String name = groups.getString(i);
            MHandler.exec(mHandler, MHandler.ADD_LIST, new Group(name));
        }
    }

    private void deleteGrouop(int position) throws Exception{
        List<Group> groupList = adapter.getList();
        Group group = groupList.get(position);
        if (group == null) {
            return;
        }
        JSONObject jDevObject = JSONReq.send(context, "DELETE", PrjCfg.CLOUD_URL + "/api/group/" + WebUtils.encode(group.name));
        if (jDevObject == null || jDevObject.getInt("code") != 200) {
            MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.err_remove));
        } else {
            MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_remove));
            MHandler.exec(mHandler, MHandler.DEL_LIST, group.name);
            MHandler.exec(mHandler, MHandler.UPDATE);
        }
    }

    private void groupStatus(int position) {
        List<Group> groupList = adapter.getList();
        Group group = groupList.get(position);
        if (group == null) {
            return;
        }
        Intent intent = new Intent(context, ViewStatusActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        Bundle bundle = new Bundle();
        bundle.putInt("Type", ViewStatusAdapter.GROUP);
        bundle.putSerializable("Group", group);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void groupEdit(int position) {
        List<Group> groupList = adapter.getList();
        Group group = groupList.get(position);
        if (group == null) {
            return;
        }
        Intent intent = new Intent(context, EditGroupActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        Bundle bundle = new Bundle();
        bundle.putSerializable("Group", group);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private static class MsgHandler extends MHandler {
        private GroupFragment fragment;

        public MsgHandler(GroupFragment fragment) {
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
                    fragment.adapter.addList((Group) msg.obj);
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