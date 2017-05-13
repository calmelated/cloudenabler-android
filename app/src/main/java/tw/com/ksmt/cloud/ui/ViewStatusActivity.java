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
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.MainApp;
import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.AdvGP;
import tw.com.ksmt.cloud.iface.Device;
import tw.com.ksmt.cloud.iface.Group;
import tw.com.ksmt.cloud.iface.Register;
import tw.com.ksmt.cloud.iface.ViewStatus;
import tw.com.ksmt.cloud.libs.Dbg;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.SearchViewTask;
import tw.com.ksmt.cloud.libs.StrUtils;
import tw.com.ksmt.cloud.libs.Utils;
import tw.com.ksmt.cloud.libs.WebUtils;

public class ViewStatusActivity extends ActionBarActivity implements AdapterView.OnItemClickListener, SearchView.OnQueryTextListener {
    private final Context context = ViewStatusActivity.this;
    private static final int DEVICE_EDIT = 1;
    private final int MAX_FAIL_COUNT = 15; // 15 times waiting
    private int failCount = 0;
    private int nLastAddItems = -1;
    private ActionBar actionBar;
    private SearchViewTask searchTask;
    private SearchView mSearchView;
    private ListView listView;
    private ViewStatusAdapter adapter;
    private ProgressDialog mDialog;
    private Timer pollingTimer;
    private Timer bgTimer;
    private MsgHandler mHandler;
    private Device curDevice;
    private Group curGroup;
    private AdvGP curAdvGP;
    private int type;
    private boolean isSlvDev = false;
    private static boolean isPaused = false;
    private static boolean isPolling = false;
    private static int numUsedRegs = 0;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        mDialog = Kdialog.getProgress(context, mDialog);
        AppSettings.setupLang(context);
        setContentView(R.layout.list_view);
        mHandler = new MsgHandler(this);

        Intent intent = getIntent();
        type = intent.getIntExtra("Type", ViewStatusAdapter.DEVICE);
        curGroup = (Group) intent.getSerializableExtra("Group");
        curAdvGP = (AdvGP) intent.getSerializableExtra("AdvGP");
        curDevice = (Device) intent.getSerializableExtra("Device");
        isSlvDev = (curDevice != null && curDevice.isMbusMaster() && curDevice.slvIdx > 0) ? true : false;
        if(type == ViewStatusAdapter.DEVICE) {
            actionBar.setTitle((isSlvDev) ? curDevice.slvDev.name : curDevice.name);
            adapter = new ViewStatusAdapter(context, ViewStatusAdapter.DEVICE);
            if(curDevice.status == 0) {
                MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.device_offline_message));
            }
        } else if(curAdvGP != null){
            actionBar.setTitle(curAdvGP.name);
            adapter = new ViewStatusAdapter(context, ViewStatusAdapter.GROUP);
        } else {
            actionBar.setTitle(curGroup.name);
            adapter = new ViewStatusAdapter(context, ViewStatusAdapter.GROUP);
        }

        listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        registerForContextMenu(listView);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch(requestCode){
            case DEVICE_EDIT: {
                if(intent == null) {
                    //Log.e(Dbg._TAG_(), "Intent is empty!");
                    return;
                }
                curDevice = (Device) intent.getSerializableExtra("curDevice");
                if(curDevice != null) {
                    actionBar.setTitle(curDevice.name);
                    return;
                }
                break;
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AppSettings.reload(context);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (pollingTimer != null) {
            pollingTimer.cancel();
        }
        if (bgTimer != null) {
            bgTimer.cancel();
        }
        if (mDialog != null) {
            mDialog.cancel();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isPaused = false;
        isSlvDev = (curDevice != null && curDevice.isMbusMaster() && curDevice.slvIdx > 0) ? true : false;
        pollingTimer = new Timer();
        pollingTimer.schedule(new PollingTimerTask(), 0, PrjCfg.VIEW_STATUS_POLLING);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.view_status, menu);

        MenuItem searchItem = menu.findItem(R.id.search);
        MenuItemCompat.collapseActionView(searchItem);
        mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setIconified(true);

        MenuItem editItem = menu.findItem(R.id.edit);
        MenuItem editSlvDevItem = menu.findItem(R.id.editSlvDev);
        if(type == ViewStatusAdapter.DEVICE) {
            editItem.setVisible(true);

            // Slave Device
            if(isSlvDev) {
                editSlvDevItem.setVisible(true);
            }
        }

        if(PrjCfg.USER_MODE == PrjCfg.MODE_USER) {
            MenuItem addItem = menu.findItem(R.id.add);
            addItem.setVisible(false);
            editItem.setVisible(false);
            editSlvDevItem.setVisible(false);
        }

        if(PrjCfg.EN_BARCHART) {
            if (type == ViewStatusAdapter.GROUP && curAdvGP == null) {
                MenuItem rtBarChart = menu.findItem(R.id.rtBarChart);
                rtBarChart.setVisible(true);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.add) {
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            if(type == ViewStatusAdapter.DEVICE) {
                if(curDevice.enlog) {
                    Kdialog.getDefInfoDialog(context).setMessage(getString(R.string.usb_logging)).show();
                    return true;
                }
                if(adapter.getNumUsedReg() >= curDevice.getNumRegs()) {
                    Kdialog.getDefInfoDialog(context).setMessage(getString(R.string.reach_register_limit)).show();
                    return true;
                }
                intent.setClass(context, NewRegisterActivity.class);
                bundle.putSerializable("Device", curDevice);
                bundle.putInt("numUsedRegs", numUsedRegs);
            } else {
                intent.setClass(context, NewGroupMemberActivity.class);
                if(curAdvGP != null) {
                    bundle.putSerializable("AdvGP", curAdvGP);
                    bundle.putInt("isAdvGP", 3);
                } else {
                    bundle.putSerializable("Group", curGroup);
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.putExtras(bundle);
            startActivity(intent);
        } else if(id == R.id.edit) {
            Intent intent = new Intent(context, EditDeviceActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

            Bundle bundle = new Bundle();
            bundle.putSerializable("Device", curDevice);
            intent.putExtras(bundle);
            startActivityForResult(intent, DEVICE_EDIT);
        } else if(id == R.id.editSlvDev) {
            Intent intent = new Intent(context, EditSlvDevActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

            Bundle bundle = new Bundle();
            bundle.putSerializable("Device", curDevice);
            bundle.putSerializable("MstDev", curDevice.slvDev);
            intent.putExtras(bundle);
            startActivity(intent);
        } else if(id == R.id.rtBarChart) {
            Intent intent = new Intent(context, BarChartActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

            Bundle bundle = new Bundle();
            if(curAdvGP == null) {
                bundle.putSerializable("Group", curGroup);
            } else {
                bundle.putSerializable("Group", curAdvGP);
            }
            intent.putExtras(bundle);
            startActivity(intent);
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        isPaused = true;

        List<ViewStatus> vsList = adapter.getList();
        ViewStatus vs = vsList.get(info.position);
        menu.setHeaderTitle(vs.desc);

        // Admin, Debug, User with permission
        if(vs.devStat == 1 && vs.userControl) {
            if (Register.isAppWriteable(vs.type)) {
                menu.add(0, R.id.setval, 0, getString(R.string.setval));
            } else if(Register.isWriteOnce(vs)) {
                menu.add(0, R.id.wronce, 0, getString(R.string.write_once));
            }
        }

        // show chart
        if(Register.enRtChart(vs.type)) {
            menu.add(0, R.id.chart, 0, getString(R.string.line_chart));
        }

        // Admin, Debug Only
        if(PrjCfg.USER_MODE == PrjCfg.MODE_ADMIN || PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
            menu.add(0, R.id.edit, 0, getString(R.string.edit));
            // Device Only, Not for Group, Not for 64 bits
            if(type == ViewStatusAdapter.DEVICE &&
               !curDevice.isMbusMaster()        &&
               !Register.is48Bits(vs.type)      &&
               !Register.is64Bits(vs.type)) {
                menu.add(0, R.id.copy, 0, getString(R.string.copy));
            }
            menu.add(0, R.id.remove, 0, getString(R.string.remove));
        }
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        isPaused = false;
        super.onContextMenuClosed(menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo itemInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int itemId = item.getItemId();
        if (itemId == R.id.setval || itemId == R.id.wronce) {
            setNewVal(itemId, itemInfo.position);
        } else if (itemId == R.id.edit) {
            Intent intent = new Intent(context, EditRegActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

            ViewStatus vs = (ViewStatus) adapter.getItem(itemInfo.position);
            Bundle bundle = new Bundle();
            bundle.putString("curId", vs.origId);
            bundle.putString("curSn", vs.sn);
            bundle.putInt("curSlvIdx", vs.slvIdx);
            bundle.putInt("logFreq", vs.logFreq);
            intent.putExtras(bundle);
            startActivity(intent);
        } else if (itemId == R.id.copy) {
            ViewStatus vs = (ViewStatus) adapter.getItem(itemInfo.position);
            final int availRegs;
            if(Register.is64Bits(vs.type)) {
                availRegs = (curDevice.getNumRegs() - numUsedRegs) / 4;
            } else if(Register.is48Bits(vs.type)) {
                availRegs = (curDevice.getNumRegs() - numUsedRegs) / 3;
            } else if(Register.is32Bits(vs.type)) {
                availRegs = (curDevice.getNumRegs() - numUsedRegs) / 2;
            } else {
                availRegs = curDevice.getNumRegs() - numUsedRegs;
            }
            if(type == ViewStatusAdapter.DEVICE) {
                if (vs.devEnlog) {
                    Kdialog.getDefInfoDialog(context).setMessage(getString(R.string.usb_logging)).show();
                    return true;
                }
            }
            if(availRegs < 1) {
                Kdialog.getDefInfoDialog(context).setMessage(getString(R.string.reach_register_limit)).show();
                return true;
            }
            final LayoutInflater inflater = LayoutInflater.from(context);
            final View layout = inflater.inflate(R.layout.input_dup_reg, null);
            final EditText editNumDupReg = (EditText) layout.findViewById(R.id.text);
            editNumDupReg.setHint(getString(R.string.dup_reg_hint) + " " + availRegs + ")");

            final AlertDialog dialog = Kdialog.getDefInputDialog(context).create();
            dialog.setView(layout);
            dialog.setIcon(R.drawable.ic_editor);
            dialog.setTitle(getString(R.string.copy));
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogIface) {
                    Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Boolean noError = true;
                            String numDupReg = editNumDupReg.getText().toString().trim();
                            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, numDupReg)) {
                                numDupReg = "1";
                            }
                            int _numDupReg = Integer.parseInt(numDupReg);
                            if(_numDupReg < 1 || _numDupReg > availRegs) {
                                noError = false;
                                editNumDupReg.setError(getString(R.string.range) + ": " + ((availRegs > 1) ? ("1 ~ " + availRegs) : "1"));
                            }
                            if (noError) {
                                ViewStatus vs = (ViewStatus) adapter.getItem(itemInfo.position);
                                mDialog = Kdialog.getProgress(context, mDialog);
                                bgTimer = new Timer();
                                bgTimer.schedule(new DupRegTimerTask(vs, numDupReg), 0, PrjCfg.RUN_ONCE);
                                dialog.dismiss();
                            }
                        }
                    });
                }
            });
            dialog.show();
        } else if (itemId == R.id.remove) {
            String removeStr = getString(R.string.make_sure_delete);
            if(type == ViewStatusAdapter.DEVICE) {
                removeStr = getString(R.string.make_sure_delete_register);
                ViewStatus vs = (ViewStatus) adapter.getItem(itemInfo.position);
                if (vs.devEnlog) {
                    Kdialog.getDefInfoDialog(context).setMessage(getString(R.string.usb_logging)).show();
                    return true;
                }
            }
            Kdialog.getMakeSureDialog(context, removeStr)
            .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mDialog = Kdialog.getProgress(context, mDialog);
                    bgTimer = new Timer();
                    bgTimer.schedule(new DeleteTimerTask(itemInfo.position), 0, PrjCfg.RUN_ONCE);
                }
            }).show();
        } else if (itemId == R.id.chart) {
            ViewStatus vs = (ViewStatus) adapter.getItem(itemInfo.position);
            startLineChart(vs);
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Log.e(Dbg._TAG_(), "pos: " + position + ", id: " + id);
        List<ViewStatus> vsList = adapter.getList();
        ViewStatus vs = vsList.get(position);
        if(vs.devStat == 1) {
            if (Register.isAppWriteable(vs.type) && vs.userControl) {
                setNewVal(R.string.setval, position);
            }
        }
        if(Register.enRtChart(vs.type)) {
            startLineChart(vs);
        }
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

    private class PollingTimerTask extends TimerTask {
        public void run() {
            try {
                if(isPaused) {
                    return;
                }
                if(isPolling) {
                    Log.e(Dbg._TAG_(), "last polling not finished yet!");
                    return;
                } else {
                    isPolling = true;
                }
                if(type == ViewStatusAdapter.DEVICE) {
                    getDevStatus();
                } else {
                    getGroupStatus();
                }
                if (Utils.loadPrefs(context, "Password", "").equals("")) {
                    MHandler.exec(mHandler, MHandler.BEEN_LOGOUT);
                    pollingTimer.cancel();
                    return;
                } else if(MainApp.SERV_MAINT > (new Date().getTime() / 1000)) {
                    MHandler.exec(mHandler, MHandler.SERV_MAINT);
                    pollingTimer.cancel();
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isPolling = false;
                mDialog.dismiss();
            }
        }
    }

    private class DupRegTimerTask extends TimerTask {
        private ViewStatus vs;
        private String numDupReg;

        public DupRegTimerTask(ViewStatus vs, String numDupReg) {
            this.vs = vs;
            this.numDupReg = numDupReg;
        }

        public void run() {
            if(type != ViewStatusAdapter.DEVICE) {
                return;
            }
            try {
                JSONObject jObject = JSONReq.send(context, "PUT", PrjCfg.CLOUD_URL + "/api/device/reg/dup/" + vs.sn + "?mbusId=" + vs.origId + "&ndup=" + numDupReg);
                int statCode = jObject.getInt("code");
                if(statCode == 400 && !jObject.isNull("desc") && jObject.getString("desc").trim().matches("Device is logging")) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.usb_logging));
                } else if (statCode != 200) {
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.err_copy));
                } else {
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_copy));
                }
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

    private class DeleteTimerTask extends TimerTask {
        private int position;

        public DeleteTimerTask(int position) {
            this.position = position;
        }

        public void run() {
            try {
                if(type == ViewStatusAdapter.DEVICE) {
                    deleteRegister(position);
                } else {
                    deleteGroupMember(position);
                }
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

    private class UpdateTimerTask extends TimerTask {
        private ViewStatus vs;

        public UpdateTimerTask(ViewStatus vs) {
            this.vs = vs;
        }

        private boolean sendVal(String uri, List<BasicNameValuePair> params) throws Exception {
            JSONObject jObject = JSONReq.send(context, "PUT", uri, params);
            if (jObject == null || jObject.getInt("code") != 200) {
                MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_update_val));
                return false;
            }
            return true;
        }

        public void run() {
            try {
                String uri = PrjCfg.CLOUD_URL + "/api/device/" + vs.sn + "/status";
                List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();
                params.add(new BasicNameValuePair("sn", vs.sn));
                params.add(new BasicNameValuePair("wronce", vs.wrOnce ? "1" : "0"));
                if (Register.is64Bits(vs.type) && vs.laddr != null && vs.lval != null && vs.iaddr != null && vs.ival != null && vs.jaddr != null && vs.jval != null) {
                    Log.e(Dbg._TAG_(), "Addr: " + vs.haddr + "-" + vs.laddr + ", Val: 0x" + vs.hval + "," + vs.ival + "," + vs.jval + "," + vs.lval);
                    params.add(new BasicNameValuePair("addr", ((vs.slvIdx > 0) ? (vs.slvIdx + vs.haddr) : vs.haddr)));
                    params.add(new BasicNameValuePair("iaddr", ((vs.slvIdx > 0) ? (vs.slvIdx + vs.iaddr) : vs.iaddr)));
                    params.add(new BasicNameValuePair("jaddr", ((vs.slvIdx > 0) ? (vs.slvIdx + vs.jaddr) : vs.jaddr)));
                    params.add(new BasicNameValuePair("laddr", ((vs.slvIdx > 0) ? (vs.slvIdx + vs.laddr) : vs.laddr)));
                    params.add(new BasicNameValuePair("val", vs.hval));
                    params.add(new BasicNameValuePair("ival", vs.ival));
                    params.add(new BasicNameValuePair("jval", vs.jval));
                    params.add(new BasicNameValuePair("lval", vs.lval));
                    sendVal(uri, params);
                } else if (Register.is48Bits(vs.type) && vs.laddr != null && vs.lval != null && vs.iaddr != null && vs.ival != null) {
                    Log.e(Dbg._TAG_(), "Addr: " + vs.haddr + "-" + vs.laddr + ", Val: 0x" + vs.hval + "," + vs.ival + "," + vs.lval);
                    params.add(new BasicNameValuePair("addr", ((vs.slvIdx > 0) ? (vs.slvIdx + vs.haddr) : vs.haddr)));
                    params.add(new BasicNameValuePair("iaddr", ((vs.slvIdx > 0) ? (vs.slvIdx + vs.iaddr) : vs.iaddr)));
                    params.add(new BasicNameValuePair("laddr", ((vs.slvIdx > 0) ? (vs.slvIdx + vs.laddr) : vs.laddr)));
                    params.add(new BasicNameValuePair("val", vs.hval));
                    params.add(new BasicNameValuePair("ival", vs.ival));
                    params.add(new BasicNameValuePair("lval", vs.lval));
                    sendVal(uri, params);
                } else if (Register.is32Bits(vs.type) && vs.laddr != null && vs.lval != null) {
                    Log.e(Dbg._TAG_(), "addr: " + vs.haddr + "-" + vs.laddr + " hval: " + vs.hval + ", lval: " + vs.lval);
                    params.add(new BasicNameValuePair("addr", ((vs.slvIdx > 0) ? (vs.slvIdx + vs.haddr) : vs.haddr)));
                    params.add(new BasicNameValuePair("laddr", ((vs.slvIdx > 0) ? (vs.slvIdx + vs.laddr) : vs.laddr)));
                    params.add(new BasicNameValuePair("val", vs.hval));
                    params.add(new BasicNameValuePair("lval", vs.lval));
                    sendVal(uri, params);
                } else { // 16 bits
                    Log.e(Dbg._TAG_(), "addr: " + vs.haddr + ", val: " + vs.hval);
                    params.add(new BasicNameValuePair("addr", ((vs.slvIdx > 0) ? (vs.slvIdx + vs.haddr) : vs.haddr)));
                    params.add(new BasicNameValuePair("val", vs.hval));
                    sendVal(uri, params);
                }
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

    private void deleteGroupMember(int position) throws Exception {
        List<ViewStatus> vsList = adapter.getList();
        ViewStatus vs = vsList.get(position);
        if (vs == null || (curGroup == null && curAdvGP == null)) {
            return;
        }
        //Log.e(Dbg._TAG_(), "sn=" + vs.sn + ", haddr=" + vs.haddr + ", laddr="+ vs.laddr + ", slvIdx=" + vs.slvIdx);
        String action = (curAdvGP == null) ? ("/api/group/" + WebUtils.encode(curGroup.name)) : ("/api/advgp/" + curAdvGP.id);
        JSONObject jObject = JSONReq.send(context, "DELETE", PrjCfg.CLOUD_URL + action + "?sn=" + vs.sn + "&addr=" + ((vs.slvIdx > 0) ? vs.slvIdx : "") + vs.haddr);
        if (jObject == null || jObject.getInt("code") != 200) {
            MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.err_remove));
        } else {
            MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_remove));
            MHandler.exec(mHandler, MHandler.DEL_LIST, vs);
            MHandler.exec(mHandler, MHandler.UPDATE);
        }
    }

    private void getGroupStatus() throws Exception {
        if(failCount > MAX_FAIL_COUNT) { // server disconnect, stop connecting
            return;
        } else if(failCount == MAX_FAIL_COUNT) {
            failCount++;
            MHandler.exec(mHandler, MHandler.GO_BACK, getString(R.string.err_get_ioval));
            return;
        }

        String action = (curAdvGP == null) ? ("/api/group/status/" + WebUtils.encode(curGroup.name)) : ("/api/advgp/status/" + curAdvGP.id);
        JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + action);
        int statCode = jObject.getInt("code");
        if (statCode == 404) {
            failCount = 0;
            return;
        } else if (statCode == 200) {
            failCount = 0;
        } else if (statCode != 200) {
            failCount++;
            return;
        }
        //Log.e(Dbg._TAG_(), jObject.toString());

        List<String> idList = new ArrayList<String>();
        JSONObject jDevConfs = jObject.getJSONObject("devcnfs");
        JSONArray iostats = jObject.getJSONArray("iostats");

        // some registers had been removed
        if(nLastAddItems >= 0 && nLastAddItems != adapter.getCount()) {
            Log.e(Dbg._TAG_(), "Number of ADD items changed! Last: " + nLastAddItems + ", Now: " + adapter.getCount());
            MHandler.exec(mHandler, MHandler.CLEAR_LIST);
        }

        // iterator group registers
        int nAddMember = 0;
        for (int i = 0; i < iostats.length(); i++) {
            JSONObject iostat = (JSONObject) iostats.get(i);

            // Get Device config
            String sn = iostat.getString("sn");
            JSONObject jDevConf = jDevConfs.getJSONObject(sn);
            String model = jDevConf.getString("mo");
            String devName = jDevConf.getString("name");
            int status = jDevConf.has("status") ?  jDevConf.getInt("status") : 0 ;
            Device device = new Device(sn, devName, model, status);
            device.enlog = (jDevConf.getInt("enLog") == 1) ? true : false ;
            device.logFreq = (jDevConf.has("logFreq")) ? jDevConf.getInt("logFreq") : PrjCfg.DEV_LOG_FREQ;
            device.slvNames = jDevConf.has("slvDev") ? jDevConf.getJSONObject("slvDev") : null;

            int hsIdx = -1;
            if(!iostat.has("haddr")) {
                continue;
            } else {
                hsIdx = Utils.slvIdx(iostat.getString("haddr"));
                device.slvIdx = hsIdx;
            }

            ViewStatus vs = new ViewStatus(device);
            vs.origId = iostat.has("id") ? iostat.getString("id") : "-1" ; // save original
            vs.id = vs.origId + i; // avoid two same id registers in the group
            idList.add(vs.id);

            // Get Device status
            vs.haddr  = Utils.realAddr(iostat.getString("haddr"));
            vs.hval   = iostat.has("hval") ? iostat.getString("hval") : "" ;
            vs.desc   = iostat.has("desc") ? iostat.getString("desc") : "" ;
            vs.unit   = iostat.has("unit") ? iostat.getString("unit") : "" ;
            vs.upVal  = iostat.has("up")   ? iostat.getString("up")   : "" ;
            vs.lowVal = iostat.has("low")  ? iostat.getString("low")  : "" ;
            vs.onVal  = iostat.has("on")   ? iostat.getString("on")   : "" ;
            vs.offVal = iostat.has("off")  ? iostat.getString("off")  : "" ;
            vs.fpt    = iostat.has("fpt")  ? iostat.getInt("fpt")     : 0;
            vs.display= iostat.has("dt")   ? iostat.getInt("dt")      : 0;
            vs.userControl  = iostat.has("userControl") ? ((iostat.getInt("userControl") == 1) ? true : false) : false ;
            vs.enlog  = iostat.has("enlog")  ? ((iostat.getString("enlog").equals("1")) ? true : false) : false ;
            vs.swSN   = iostat.has("swSN")   ? iostat.getString("swSN")   : "" ;
            vs.swAddr = iostat.has("swAddr") ? iostat.getString("swAddr") : "" ;
            vs.swType = iostat.has("swType") ? iostat.getInt("swType")    : 0;
            vs.type   = iostat.getInt("type");

            // sort list by sortId
            if(hsIdx > 0 && PrjCfg.SORT_REG_LIST.equals("mbus_addr")) {
                String fc = vs.haddr.substring(0, 1);
                if(fc.equals("3")) {
                    fc = "4";
                } else if(fc.equals("4")) {
                    fc = "3";
                }
                vs.sortId = vs.haddr.substring(1, vs.haddr.length()) + fc;
            } else {
                vs.sortId = vs.haddr;
            }

            //Log.e(Dbg._TAG_(), "i=" + i + ", id=" + vs.id);
            if(vs.id.equals("") || vs.id.equals("-1")) {
                MHandler.exec(mHandler, MHandler.ADD_LIST, vs);
                nAddMember++;
                continue;
            } else if (Register.is64Bits(vs.type) && vs.swType != Register.TYPE_ERROR) {
                if(!iostat.has("iaddr") || !iostat.has("jaddr") || !iostat.has("laddr")) {
                    Log.e(Dbg._TAG_(), "Lost another byte for  haddr: " + vs.haddr + " type: " + vs.type);
                    continue;
                }
                vs.iaddr = Utils.realAddr(iostat.getString("iaddr"));
                vs.jaddr = Utils.realAddr(iostat.getString("jaddr"));
                vs.laddr = Utils.realAddr(iostat.getString("laddr"));
                vs.ival = iostat.getString("ival");
                vs.jval = iostat.getString("jval");
                vs.lval = iostat.getString("lval");

                int lsIdx = (iostat.has("laddr")) ? Utils.slvIdx(iostat.getString("laddr")) : 0;
                device.slvIdx = (hsIdx > 0) ? hsIdx : ((lsIdx > 0) ? lsIdx : 0) ;
                vs.slvIdx = device.slvIdx;
            } else if (Register.is48Bits(vs.type) && vs.swType != Register.TYPE_ERROR) {
                if(!iostat.has("iaddr") || !iostat.has("laddr")) {
                    Log.e(Dbg._TAG_(), "Lost another byte for  haddr: " + vs.haddr + " type: " + vs.type);
                    continue;
                }
                vs.iaddr = Utils.realAddr(iostat.getString("iaddr"));
                vs.laddr = Utils.realAddr(iostat.getString("laddr"));
                vs.ival = iostat.getString("ival");
                vs.lval = iostat.getString("lval");

                int lsIdx = (iostat.has("laddr")) ? Utils.slvIdx(iostat.getString("laddr")) : 0;
                device.slvIdx = (hsIdx > 0) ? hsIdx : ((lsIdx > 0) ? lsIdx : 0) ;
                vs.slvIdx = device.slvIdx;
            } else if (Register.is32Bits(vs.type) && vs.swType != Register.TYPE_ERROR) {
                if(!iostat.has("laddr")) {
                    Log.e(Dbg._TAG_(), "Lost another byte for  haddr: " + vs.haddr + " type: " + vs.type);
                    continue;
                }
                vs.laddr = Utils.realAddr(iostat.getString("laddr"));
                vs.lval = iostat.getString("lval");

                int lsIdx = (iostat.has("laddr")) ? Utils.slvIdx(iostat.getString("laddr")) : 0;
                device.slvIdx = (hsIdx > 0) ? hsIdx : ((lsIdx > 0) ? lsIdx : 0) ;
                vs.slvIdx = device.slvIdx;
            } else { // 16bit, alarm
                device.slvIdx = hsIdx;
                vs.slvIdx = device.slvIdx;
            }
            vs.isCoilReg = Register.isCoil(vs.slvIdx, vs.haddr);
            vs.showVal = Register.isIOSW(vs.type) ? vs.setShowVal(vs.swType) : vs.setShowVal(vs.type);
            MHandler.exec(mHandler, MHandler.ADD_LIST, vs);
            nAddMember++;
        }
        nLastAddItems = nAddMember;

        // some registers had been removed
        //Log.e(Dbg._TAG_(), "nAddItem=" + nAddItem + ", adapter.getCount=" + adapter.getCount());
        if(nAddMember != adapter.getCount()) {
            for(int i = 0; i < adapter.getCount(); i++) {
                ViewStatus vs = (ViewStatus) adapter.getItem(i);
                if(idList.indexOf(vs.id) < 0) { // not found
                    MHandler.exec(mHandler, MHandler.DEL_LIST, vs);
                }
            }
        }

        // Update UI
        MHandler.exec(mHandler, MHandler.UPDATE);
    }

    private void startLineChart(ViewStatus vs) {
        Intent intent = new Intent(context, LineChartActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        Bundle bundle = new Bundle();
        bundle.putSerializable("curVs", vs);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void deleteRegister(int position) throws Exception {
        List<ViewStatus> vsList = adapter.getList();
        ViewStatus vs = vsList.get(position);
        if (vs == null || curDevice == null) {
            return;
        }
        Register register = new Register(vs.origId, vs);
        register.slvIdx = vs.slvIdx;
        JSONObject jObject = JSONReq.multipart(context, "PUT", PrjCfg.CLOUD_URL + "/api/device/edit", register.toMultiPart("DELETE", curDevice.sn));
        int statCode = jObject.getInt("code");
        if(statCode == 400 && !jObject.isNull("desc") && jObject.getString("desc").trim().matches("Device is logging")) {
            MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.usb_logging));
        } else if (statCode != 200) {
            MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.err_remove));
        } else {
            if(Register.is64Bits(register.type)) {
                numUsedRegs = numUsedRegs - 4;
            } else if(Register.is48Bits(register.type)) {
                numUsedRegs = numUsedRegs - 3;
            } else if(Register.is32Bits(register.type)) {
                numUsedRegs = numUsedRegs - 2;
            } else { // 16 Bits
                numUsedRegs = numUsedRegs - 1;
            }
            MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_remove));
            MHandler.exec(mHandler, MHandler.DEL_LIST, vs);
            MHandler.exec(mHandler, MHandler.UPDATE);
        }
    }

    private void getDevStatus() throws Exception {
        if(failCount > MAX_FAIL_COUNT) { // server disconnect, stop connecting
            return;
        } else if(failCount == MAX_FAIL_COUNT) {
            failCount++;
            MHandler.exec(mHandler, MHandler.GO_BACK, getString(R.string.err_get_ioval));
            return;
        }

        String url = (curDevice.slvIdx > 0) ? "/api/device/" + curDevice.sn + "/status?slvIdx=" + curDevice.slvIdx : "/api/device/" + curDevice.sn + "/status";
        JSONObject jObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + url);
        if (jObject == null || jObject.getInt("code") != 200) {
            failCount++;
            return;
        }
        failCount = 0; // reset if connected

        // Get device status
        curDevice.status = jObject.has("status") ?  jObject.getInt("status") : 0 ;
        if(curDevice.lastStatus < 0) {
            // first time, ignore the check
        } else if(curDevice.status != curDevice.lastStatus) {
            if(curDevice.status == 1) { // online
                MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.device_online));
            } else { // offline
                MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.device_offline));
            }
        }
        curDevice.lastStatus = curDevice.status;

        if (jObject.has("lastUpdate") && !(jObject.get("lastUpdate") + "").equals("null")) {
            long lastUpdate = jObject.getLong("lastUpdate");
            MHandler.exec(mHandler, MHandler.UPDATE_TIME, curDevice.sn + " - " + Utils.unix2Datetime(lastUpdate / 1000));
        }
        if(!jObject.has("iostats")) {
            return; // this device don't have any address
        }
        JSONArray iostats = jObject.getJSONArray("iostats");

        // some registers had been removed
        if(nLastAddItems >= 0 && nLastAddItems != adapter.getCount()) {
            Log.e(Dbg._TAG_(), "Number of ADD items changed! Last: " + nLastAddItems + ", Now: " + adapter.getCount());
            MHandler.exec(mHandler, MHandler.CLEAR_LIST);
        }

        // show value
        List<String> idList = new ArrayList<String>();
        int nAddItem = 0;
        int count = iostats.length();
        boolean userControl = jObject.has("userControl") ? (jObject.getInt("userControl") == 1 ? true : false) : false;
        curDevice.enlog = jObject.isNull("enLog") ? curDevice.enlog : (jObject.getInt("enLog") == 1 ? true : false);
        for(int i = 0; i < iostats.length(); i++) {
            if(iostats.isNull(i)) {
                continue;
            }
            ViewStatus vs = new ViewStatus(curDevice);
            JSONObject jmbProf = (JSONObject) iostats.get(i);
            //Log.e(Dbg._TAG_(), jmbProf.toString());
            vs.id = jmbProf.getString("id");
            vs.origId = vs.id;
            idList.add(vs.id);

            vs.userControl = userControl;
            vs.desc   = jmbProf.has("desc") ? jmbProf.getString("desc") : "" ;
            vs.unit   = jmbProf.has("unit") ? jmbProf.getString("unit") : "" ;
            vs.haddr  = Utils.realAddr(jmbProf.getString("haddr"));
            vs.hval   = jmbProf.getString("hval");
            vs.upVal  = jmbProf.has("up")  ? jmbProf.getString("up")  : "" ;
            vs.lowVal = jmbProf.has("low") ? jmbProf.getString("low") : "" ;
            vs.onVal  = jmbProf.has("on")  ? jmbProf.getString("on") : "" ;
            vs.offVal = jmbProf.has("off") ? jmbProf.getString("off") : "" ;
            vs.fpt    = jmbProf.has("fpt") ? jmbProf.getInt("fpt") : 0;
            vs.display= jmbProf.has("dt") ? jmbProf.getInt("dt") : 0;
            vs.enlog  = (jmbProf.getInt("enlog") == 1) ? true : false ;
            vs.swSN   = jmbProf.has("swSN") ? jmbProf.getString("swSN") : "" ;
            vs.swAddr = jmbProf.has("swAddr") ? jmbProf.getString("swAddr") : "" ;
            vs.swType = jmbProf.has("swType") ? jmbProf.getInt("swType") : 0;

            // sort list by sortId
            if(curDevice.slvIdx > 0 && PrjCfg.SORT_REG_LIST.equals("mbus_addr")) {
                String fc = vs.haddr.substring(0, 1);
                if(fc.equals("3")) {
                    fc = "4";
                } else if(fc.equals("4")) {
                    fc = "3";
                }
                vs.sortId = vs.haddr.substring(1, vs.haddr.length()) + fc;
            } else {
                vs.sortId = vs.haddr;
            }
            //Log.e(Dbg._TAG_(),  "sort reg list = " + PrjCfg.SORT_REG_LIST + ", sortId = " + vs.sortId);

            vs.type = jmbProf.getInt("type");
            if (Register.is64Bits(vs.type)) {
                vs.iaddr = (jmbProf.has("iaddr")) ? Utils.realAddr(jmbProf.getString("iaddr")) : "" ;
                vs.jaddr = (jmbProf.has("jaddr")) ? Utils.realAddr(jmbProf.getString("jaddr")) : "" ;
                vs.laddr = (jmbProf.has("laddr")) ? Utils.realAddr(jmbProf.getString("laddr")) : "" ;
                vs.ival = jmbProf.getString("ival");
                vs.jval = jmbProf.getString("jval");
                vs.lval = jmbProf.getString("lval");
                count = count + 3;
            } else if (Register.is48Bits(vs.type)) {
                vs.iaddr = (jmbProf.has("iaddr")) ? Utils.realAddr(jmbProf.getString("iaddr")) : "" ;
                vs.laddr = (jmbProf.has("laddr")) ? Utils.realAddr(jmbProf.getString("laddr")) : "" ;
                vs.ival = jmbProf.getString("ival");
                vs.lval = jmbProf.getString("lval");
                count = count + 2;
            } else if (Register.is32Bits(vs.type)) {
                vs.laddr = (jmbProf.has("laddr")) ? Utils.realAddr(jmbProf.getString("laddr")) : "" ;
                vs.lval = jmbProf.getString("lval");
                count = count + 1;
            }
            //Log.e(Dbg._TAG_(), "slvIdx=" + vs.slvIdx + ", vs.haddr=" + vs.haddr);
            vs.isCoilReg = Register.isCoil(vs.slvIdx, vs.haddr);
            vs.showVal = Register.isIOSW(vs.type) ? vs.setShowVal(vs.swType) : vs.setShowVal(vs.type);
            MHandler.exec(mHandler, MHandler.ADD_LIST, vs);
            nAddItem++;
        }
        numUsedRegs = count;
        nLastAddItems = nAddItem;

        // some registers had been removed
        //Log.e(Dbg._TAG_(), "numUsedRegs=" + numUsedRegs + ", len = " + iostats.length());
        //Log.e(Dbg._TAG_(), "nAddItem=" + nAddItem + ", adapter.getCount=" + adapter.getCount());
        if(nAddItem != adapter.getCount()) {
            for(int i = 0; i < adapter.getCount(); i++) {
                ViewStatus vs = (ViewStatus) adapter.getItem(i);
                if(idList.indexOf(vs.id) < 0) { // not found
                    MHandler.exec(mHandler, MHandler.DEL_LIST, vs);
                }
            }
        }

        // Update UI
        MHandler.exec(mHandler, MHandler.UPDATE);
    }

    private void setNewVal(int type, int position) {
        final ViewStatus vs = (ViewStatus) adapter.getItem(position);
        if (vs == null) {
            return;
        }
        try {
            vs.wrOnce = (type == R.id.wronce) ? true : false;
            if (vs.type == Register.APP_BTN || vs.type == Register.APP_SWITCH) {
                setBtnVal(vs);
            } else if (Register.isBinary(vs.type)) {
                setBinVal(vs);
            } else {
                setEditVal(vs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setBinVal(final ViewStatus vs) throws  Exception {
        String hval = vs.hval;
        if (vs.hval == null || vs.hval.equals("")) {
            hval = "0";
        }

        final boolean[] selections = new boolean[16];
        char[] showVals = Integer.toBinaryString(Integer.parseInt(hval, 16)).toCharArray();
        for (int i = showVals.length - 1, j = 0; i >= 0; i--, j++) {
            selections[j] = (showVals[i] == '1') ? true : false;
        }

        final CharSequence[] bits = {
            getString(R.string.bit_1),
            getString(R.string.bit_2),
            getString(R.string.bit_3),
            getString(R.string.bit_4),
            getString(R.string.bit_5),
            getString(R.string.bit_6),
            getString(R.string.bit_7),
            getString(R.string.bit_8),
            getString(R.string.bit_9),
            getString(R.string.bit_10),
            getString(R.string.bit_11),
            getString(R.string.bit_12),
            getString(R.string.bit_13),
            getString(R.string.bit_14),
            getString(R.string.bit_15),
            getString(R.string.bit_16),
        };

        final AlertDialog dialog;
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getString(R.string.setval));
        builder.setIcon(R.drawable.ic_editor);
        builder.setMultiChoiceItems(bits, null, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                //Log.e(Dbg._TAG_(), "Which = " + which + ", isChecked=" + isChecked);
                selections[which] = isChecked;
            }
        });
        builder.setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newVal = "";
                for (int i = selections.length - 1; i >= 0; i--) {
                    newVal += (selections[i]) ? "1" : "0";
                }
                //Log.e(Dbg._TAG_(), "Binary newVal = " + newVal);
                vs.setNewVal(newVal);
                dialog.dismiss();

                mDialog = Kdialog.getProgress(context, mDialog);
                bgTimer = new Timer();
                bgTimer.schedule(new UpdateTimerTask(vs), 0, PrjCfg.RUN_ONCE);
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), null);
        dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                for (int i = 0 ; i < selections.length ; i++){
                    if(selections[i]){
                        ((AlertDialog) dialog).getListView().setItemChecked(i, true);
                    } else {
                        ((AlertDialog) dialog).getListView().setItemChecked(i, false);
                    }
                }
            }
        });
        dialog.show();
    }

    private void setBtnVal(final ViewStatus vs) {
        int msgStr = (vs.type == Register.APP_BTN) ? R.string.make_sure_press_btn : R.string.make_sure_press_sw ;
        Kdialog.getMakeSureDialog(context, getString(msgStr))
        .setTitle(vs.desc)
        .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            if(vs.isBtnOn) {
                vs.setNewVal(vs.offVal);
            } else {
                vs.setNewVal(vs.onVal);
            }
            mDialog = Kdialog.getProgress(context, mDialog);
            bgTimer = new Timer();
            bgTimer.schedule(new UpdateTimerTask(vs), 0, PrjCfg.RUN_ONCE);
            }
        }).show();
    }

    private void setEditVal(final ViewStatus vs) {
        isPaused = true;
        int layout = vs.wrOnce ? R.layout.input_wronce : R.layout.input_number;
        final View view = View.inflate(this, layout, null);
        final EditText input = (EditText)view.findViewById(R.id.text);
        input.setText(vs.showVal);
        input.setHint(getString(R.string.range) + ": " + Register.getValidRange(vs));

        final AlertDialog dialog = Kdialog.getDefInputDialog(context).create();
        dialog.setView(view);
        dialog.setIcon(R.drawable.ic_editor);
        dialog.setTitle(vs.desc);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                isPaused = false;
            }
        });
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogIface) {
                Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Boolean noError = true;
                        String inputVal = input.getText().toString().trim();
                        if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, inputVal)) {
                            noError = false;
                            input.setError(getString(R.string.err_msg_empty));
                        }
                        if (!Register.validVal(vs, inputVal)) {
                            noError = false;
                            //input.setError(getString(R.string.invalid_val));
                            input.setError(getString(R.string.range) + ": " + Register.getValidRange(vs));
                        }
                        if (noError) {
                            try {
                                vs.setNewVal(input.getText().toString());
                                mDialog = Kdialog.getProgress(context, mDialog);
                                bgTimer = new Timer();
                                bgTimer.schedule(new UpdateTimerTask(vs), 0, PrjCfg.RUN_ONCE);
                            } catch (Exception e) {
                                MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_update_val));
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
    }

    private static class MsgHandler extends MHandler {
        public MsgHandler(ViewStatusActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            ViewStatusActivity activity = (ViewStatusActivity) super.mActivity.get();
            switch (msg.what) {
                case MHandler.UPDATE: {
                    int curIdx = activity.listView.getFirstVisiblePosition();
                    View topView = activity.listView.getChildAt(0);
                    int topIdx = (topView == null) ? 0 : topView.getTop();

                    activity.adapter.notifyDataSetChanged();
                    activity.listView.setSelectionFromTop(curIdx, topIdx);
                    break;
                }
                case MHandler.UPDATE_TIME: {
                    activity.getSupportActionBar().setSubtitle(Html.fromHtml("<small><small>" + (String) msg.obj + "</small></small>"));
                    break;
                }
                case MHandler.CLEAR_LIST: {
                    activity.adapter.clearList();
                    break;
                }
                case MHandler.ADD_LIST: {
                    activity.adapter.addItem((ViewStatus) msg.obj);
                    break;
                }
                case MHandler.DEL_LIST: {
                    activity.adapter.remove((ViewStatus) msg.obj);
                    break;
                }
                case MHandler.SRCH_QUERY: {
                    activity.adapter.setQueryStr((String) msg.obj);
                    break;
                }
                default: {
                    super.handleMessage(msg);
                }
            }
        }
    }
}
