package tw.com.ksmt.cloud.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Device;
import tw.com.ksmt.cloud.iface.TCPFrame;
import tw.com.ksmt.cloud.libs.Dbg;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.SearchViewTask;
import tw.com.ksmt.cloud.libs.StrUtils;
import tw.com.ksmt.cloud.libs.Utils;
import tw.com.ksmt.cloud.libs.WebUtils;

public class LocalDevFragment extends Fragment implements AdapterView.OnItemClickListener, PullToRefreshBase.OnRefreshListener2<ListView>, SearchView.OnQueryTextListener {
    private MainPager context;
    private Timer pollingTimer;
    private Timer getDevInfoTimer;
    private Timer getDevMACTimer;
    private MsgHandler mHandler;
    private boolean stopPolling = false;
    private ProgressDialog mDialog;
    private LocalDevAdapter adapter;
    private PullToRefreshListView prListView;
    private ListView listView;
    private SearchViewTask searchTask;
    protected SearchView mSearchView;
    private Device curDevice;
    private boolean isVisibleToUser;

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        context = (MainPager) getActivity();
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
        adapter = new LocalDevAdapter(context);
        listView = prListView.getRefreshableView();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
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
        if (getDevInfoTimer != null) {
            getDevInfoTimer.cancel();
        }
        if (getDevMACTimer!= null) {
            getDevMACTimer.cancel();
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
        pollingTimer.schedule(new PollingTimerTask(), 0, PrjCfg.LOCAL_DEVTIME_POLLING);
        mDialog = Kdialog.getProgress(context, mDialog);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(PrjCfg.USER_MODE == PrjCfg.MODE_ADMIN || PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
            inflater.inflate(R.menu.local_device, menu);
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
        if (id == R.id.local_connect) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            final View layout = inflater.inflate(R.layout.input_ip_addr, null);
            final EditText devIpAddr = (EditText) layout.findViewById(R.id.ip_addr);
            devIpAddr.setText(Utils.loadPrefs(context, "LocalDevAddr"));

            final AlertDialog dialog = Kdialog.getDefInputDialog(context).create();
            dialog.setView(layout);
            dialog.setIcon(R.drawable.ic_connect);
            dialog.setTitle(getString(R.string.local_connect));
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogIface) {
                    Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Boolean noError = true;
                            String localDevAddr = devIpAddr.getText().toString().trim();
                            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, localDevAddr)) {
                                noError = false;
                                devIpAddr.setError(getString(R.string.err_msg_empty));
                            } else if (!StrUtils.validateInput(StrUtils.IN_TYPE_IPV4, localDevAddr.toString())) {
                                noError = false;
                                devIpAddr.setError(getString(R.string.err_msg_invalid_ip));
                            }
                            if (noError) {
                                dialog.dismiss();
                                Utils.savePrefs(context, "LocalDevAddr", localDevAddr);
                                curDevice = new Device(localDevAddr);
                                getDevMAC(); // get mac -> get apconfig -> local device setting.
                            }
                        }
                    });
                }
            });
            dialog.show();
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
        mDialog = Kdialog.getProgress(context, mDialog);
        getDevInfo(position - 1);
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
        pollingTimer.schedule(new PollingTimerTask(), 0, PrjCfg.LOCAL_DEV_POLLING);
    }

    @Override
    public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
    }

    public void onContextMenuClosed(Menu menu) {
        stopPolling = false;
    }

    private class PollingTimerTask extends TimerTask {
        public void run() {
            try {
                if(stopPolling) { return; }
                Map<String, Device> deviceMap = new HashMap<String, Device>();
                devBrcast(deviceMap);
                if(deviceMap.size() > 0) {
                    MHandler.exec(mHandler, MHandler.CLEAR_LIST);
                    MHandler.exec(mHandler, MHandler.ADD_LIST, deviceMap);
                    MHandler.exec(mHandler, MHandler.UPDATE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if(mDialog != null) {
                    mDialog.dismiss();
                }
            }
        }
    }

    private byte[] respApCfg;

    private class DevCfgTask extends TimerTask {
        public void run() {
            try {
                byte[] reqData = TCPFrame.getAPCFGREQ((byte) 2); // 2: Get AP Mode Settings
                respApCfg = WebUtils.tcpRequest(curDevice.ip, reqData);
                if(respApCfg == null) {
                    return;
                }
                String typeStr = StrUtils.getPktStr(respApCfg, 0, 8);
                if (!typeStr.equals(TCPFrame.CMD_APCFGRSP)) {
                    Log.e(Dbg._TAG_(), "Frame " + typeStr + ", 9:" + respApCfg[9] + ", 10:" + respApCfg[10] + ", 11:" + respApCfg[11] + ", 12:" + respApCfg[12]);
                    return;
                }
                String devPawd = StrUtils.getPktStr(respApCfg, 390, 32);
                if(devPawd != null && devPawd.equals("")) {
                    MHandler.exec(mHandler, MHandler.NEXT_ONE);
                } else {
                    MHandler.exec(mHandler, MHandler.ASK_PASSWORD, devPawd);
                }
            } catch (Exception e) {
                e.printStackTrace();
                MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_unable_connect));
            } finally {
                mDialog.dismiss();
                getDevInfoTimer.cancel();
            }
        }
    }

    private class GetDevMACTask extends TimerTask {
        public void run() {
            try {
                byte[] reqData = TCPFrame.getWMACREQ(); // 2: Get AP Mode Settings
                byte[] respData = WebUtils.tcpRequest(curDevice.ip, reqData);
                if(respData == null) {
                    return;
                }
                String typeStr = StrUtils.getPktStr(respData, 0, 8);
                if (!typeStr.equals(TCPFrame.CMD_WMAC_RSP)) {
                    Log.e(Dbg._TAG_(), "Frame " + typeStr + ", 9:" + respData[9] + ", 10:" + respData[10] + ", 11:" + respData[11] + ", 12:" + respData[12]);
                    return;
                }
                String macAddr = String.format("%02X:%02X:%02X:%02X:%02X:%02X", respData[12], respData[13], respData[14], respData[15], respData[16], respData[17]);
                Log.e(Dbg._TAG_(), "Find MAC = " + macAddr);
                if(macAddr != null && !macAddr.equals("")) {
                    curDevice.sn = macAddr;
                    getDevInfoTimer = new Timer();
                    getDevInfoTimer.schedule(new DevCfgTask(), 0, PrjCfg.RUN_ONCE);
                }
            } catch (Exception e) {
                e.printStackTrace();
                MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_unable_connect));
            } finally {
                mDialog.dismiss();
                getDevMACTimer.cancel();
            }
        }
    }

    private void devBrcast(Map<String, Device> deviceMap) {
        DatagramSocket socket = null;
        try {
            //Log.e(Dbg._TAG_(), "Device discovering ...");
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            byte[] data = getSRCHREQ(); // new byte[]{0x53, 0x52, 0x43, 0x48, 0x2d, 0x52, 0x45, 0x51, 0x00, 0x02, (byte)0xff, 0x00, 0x03, 0x46, 0x0d, 0x0a};
            byte[] bcAddr = new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
            DatagramPacket request = new DatagramPacket(data, data.length, InetAddress.getByAddress(bcAddr), PrjCfg.DEV_DISCOVER_PORT);
            socket.send(request);
            socket.send(request); //NOTE: To make sure devices will receive this UDP request.
            socket.send(request); //NOTE: To make sure devices will receive this UDP request.
            socket.setSoTimeout(PrjCfg.DEV_DISCOVER_TIMEOUT); // 1.5 sec
            while(true) {
                byte[] buf = new byte[256];
                DatagramPacket receives = new DatagramPacket(buf, buf.length);
                socket.receive(receives);
                if (receives == null || receives.getLength() < 1) {
                    break;
                }
                try {
                    String action = new String(Arrays.copyOfRange(buf, 0, 8));
                    if (action.equals("SRCH-RSP")) {
                        String model = String.valueOf(((buf[10] & 0xff) << 8) + (buf[11] & 0xff));
                        String name = new String(Arrays.copyOfRange(buf, 20, 52)).split("\u0000")[0];
                        String macAddr = String.format("%02X:%02X:%02X:%02X:%02X:%02X", buf[84], buf[85], buf[86], buf[87], buf[88], buf[89]);
                        String ipAddr = receives.getAddress().toString().split("/")[1];
                        //Log.e(Dbg._TAG_(), "model=" + model + ", mac=" + macAddr + ", ip address=" + ipAddr);
                        deviceMap.put(macAddr, new Device(model, name, macAddr, ipAddr));
                    } else if (action.equals("SRCH2RSP")) {
                        String model = new String(Arrays.copyOfRange(buf, 160, 224)).split("\u0000")[0];
                        //int model = ((buf[159] & 0xff) << 24) + ((buf[158] & 0xff) << 16) + ((buf[157] & 0xff) << 8) + (buf[156] & 0xff);
                        String[] _model = model.split("-");
                        if(_model.length > 1) {
                            if(_model[0].equals("HY") || _model[0].equals("YT")) {
                                // no change;
                            } else { // KT-63511 -> 63511
                                model = _model[1];
                            }
                        }
                        String name = new String(Arrays.copyOfRange(buf, 20, 52)).split("\u0000")[0];
                        String macAddr = String.format("%02X:%02X:%02X:%02X:%02X:%02X", buf[84], buf[85], buf[86], buf[87], buf[88], buf[89]);
                        String ipAddr = receives.getAddress().toString().split("/")[1];
                        //Log.e(Dbg._TAG_(), "model=" + model + ", mac=" + macAddr + ", ip address=" + ipAddr);
                        deviceMap.put(macAddr, new Device(model, name, macAddr, ipAddr));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch(SocketTimeoutException e) {
            //  discovery timeout
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(socket != null) {
                socket.close();
            }
        }
    }

    private byte[] getSRCHREQ() {
        byte[] command = new byte[16];
        String cmd = "SRCH-REQ";
        byte[] tmp = cmd.getBytes();
        System.arraycopy(tmp, 0, command, 0, tmp.length);

        // len
        command[8] = 0x00;
        command[9] = 0x02;

        // search id
        command[10] = (byte)0xff;
        command[11] = 0x00;

        // checksum
        short checkSum = 0;
        for(int index = 0; index < 12; index++) {
            checkSum = (short)(checkSum + (short)(command[index] & 0xff));
        }
        command[12] = (byte)(checkSum>>8);
        command[13] = (byte)checkSum;

        // End of packet
        command[command.length-2] = 0x0D;
        command[command.length-1] = 0x0A;
        return command;
    }

    private void getDevMAC() {
        getDevMACTimer = new Timer();
        getDevMACTimer.schedule(new GetDevMACTask(), 0, PrjCfg.RUN_ONCE);
    }

    private void getDevInfo(int position) {
        if(position >= 0) {
            List<Device> devList = adapter.getList();
            Device device = devList.get(position);
            if (device == null) {
                return;
            }
            curDevice = device;
            getDevInfoTimer = new Timer();
            getDevInfoTimer.schedule(new DevCfgTask(), 0, PrjCfg.RUN_ONCE);
        }
    }

    private void askDevPassword(final String devPswd) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View layout = inflater.inflate(R.layout.input_new_password, null);
        final EditText editConfimPassword = (EditText) layout.findViewById(R.id.confimPassword);
        editConfimPassword.setVisibility(View.GONE);

        // Edit password
        final EditText editPassword = (EditText) layout.findViewById(R.id.password);
        editPassword.setHint(getString(R.string.device_default_password));
        editPassword.setFilters(Utils.arrayMerge(editPassword.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
        if(curDevice.sn != null && !curDevice.sn.equals("")) {
            editPassword.setText(Utils.loadPrefs(context, "LocalDevAddr-" + curDevice.sn));
        }

        final AlertDialog dialog = Kdialog.getDefInputDialog(context).create();
        dialog.setView(layout);
        dialog.setIcon(R.drawable.ic_password);
        dialog.setTitle(getString(R.string.device) + getString(R.string.password));
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogIface) {
                Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Boolean noError = true;
                        String enterPswd = editPassword.getText().toString().trim();
                        if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, enterPswd)) {
                            noError = false;
                            editPassword.setError(getString(R.string.err_msg_empty));
                        }
                        if(!enterPswd.equals(devPswd)) {
                            noError = false;
                            editPassword.setError(getString(R.string.err_msg_wrong_password));
                        }
                        if (noError) {
                            dialog.dismiss();
                            if(curDevice.sn != null && !curDevice.sn.equals("")) {
                                Utils.savePrefs(context, "LocalDevAddr-" + curDevice.sn, devPswd);
                            }
                            startLocalDev();
                        }
                    }
                });
            }
        });
        dialog.show();
    }

    private void startLocalDev() {
        Intent intent = new Intent(context, LocalDevSettings.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        Bundle bundle = new Bundle();
        bundle.putSerializable("Device", curDevice);
        //Log.e(Dbg._TAG_(), "sn="+curDevice.sn+", mo="+curDevice.model);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private static class MsgHandler extends MHandler {
        private LocalDevFragment fragment;

        public MsgHandler(LocalDevFragment fragment) {
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
                    Map<String, Device> deviceMap = (Map<String, Device>) msg.obj;
                    fragment.adapter.setList(new ArrayList<Device>(deviceMap.values()));
                    break;
                }
                case MHandler.SRCH_QUERY: {
                    fragment.adapter.setQueryStr((String) msg.obj);
                    break;
                }
                case MHandler.ASK_PASSWORD : {
                    fragment.askDevPassword((String) msg.obj);
                    break;
                }
                case MHandler.NEXT_ONE : {
                    fragment.startLocalDev();
                    break;
                }
                default: {
                    super.handleMessage(msg);
                }
            }
        }
    }

}