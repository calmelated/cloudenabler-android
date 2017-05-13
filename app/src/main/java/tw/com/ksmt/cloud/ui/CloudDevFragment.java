package tw.com.ksmt.cloud.ui;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.text.InputFilter;
import android.util.Log;
import android.view.ContextMenu;
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
import android.widget.TextView;

import com.avos.avoscloud.okhttp.MediaType;
import com.avos.avoscloud.okhttp.MultipartBuilder;
import com.avos.avoscloud.okhttp.RequestBody;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Device;
import tw.com.ksmt.cloud.iface.MstDev;
import tw.com.ksmt.cloud.libs.Dbg;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.SearchViewTask;
import tw.com.ksmt.cloud.libs.StrUtils;
import tw.com.ksmt.cloud.libs.Utils;
import tw.com.ksmt.cloud.libs.WebUtils;

public class CloudDevFragment extends Fragment implements AdapterView.OnItemClickListener, PullToRefreshBase.OnRefreshListener2<ListView>, SearchView.OnQueryTextListener {
    private final int IMPORT_PROFILE = 100;
    private MainPager context;
    private Timer pollingTimer;
    private Timer deleteTimer;
    private Timer swDevTimer;
    private Timer importTimer;
    private Timer exportTimer;
    private MsgHandler mHandler;
    private boolean stopPolling = false;
    private boolean isExporting = false;
    private boolean isDeleting = false;
    private ProgressDialog mDialog;
    private CloudDevAdapter adapter;
    private PullToRefreshListView prListView;
    private ListView listView;
    private SearchViewTask searchTask;
    protected SearchView mSearchView;
    private Device selDev;
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
        adapter = new CloudDevAdapter(context);
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
        if (swDevTimer != null) {
            swDevTimer.cancel();
        }
        if (importTimer != null) {
            importTimer.cancel();
        }
        if (exportTimer != null) {
            exportTimer.cancel();
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
        pollingTimer.schedule(new PollingTimerTask(), 0, PrjCfg.CLOUD_DEV_POLLING);
        mDialog = Kdialog.getProgress(context, mDialog);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(PrjCfg.USER_MODE == PrjCfg.MODE_ADMIN || PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
            inflater.inflate(R.menu.cloud_device, menu);
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
        if (id == R.id.account_manage) {
            startActivity(new Intent(context, AccountActivity.class));
        } else if (id == R.id.notification) {
            startActivity(new Intent(context, NotificationActivity.class));
        } else if (id == R.id.audit) {
            startActivity(new Intent(context, AuditActivity.class));
        } else if (id == R.id.iostlog) {
            startActivity(new Intent(context, IoStLogActivity.class));
        } else if (id == R.id.settings) {
            startActivity(new Intent(context, AppSettings.class));
        } else if (id == R.id.cloud_status) {
            startActivity(new Intent(context, CloudStatusActivity.class));
        } else if (id == R.id.company) {
            startActivity(new Intent(context, EditCompActivity.class));
        } else if (id == R.id.subsidiary) {
            startActivity(new Intent(context, SubCompActivity.class));
        } else if (id == R.id.announce) {
            startActivity(new Intent(context, AnnounceActivity.class));
        } else if (id == R.id.flink) {
            startActivity(new Intent(context, FlinkActivity.class));
        } else if (id == R.id.advGP) {
            startActivity(new Intent(context, AdvGPActivity.class));
        } else if(id == R.id.logout) {
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
        Device device = (Device) adapter.getItem(position - 1);
        if (device == null) {
            return;
        }
        if(device.isMbusMaster() && device.slvIdx < 1) {
            if(PrjCfg.USER_MODE == PrjCfg.MODE_ADMIN || PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                startActivity(position - 1, SlaveDevActivity.class);
            }
        } else {
            startActivity(position - 1, ViewStatusActivity.class);
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

    @Override
    public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
        //Log.e(Dbg._TAG_(), "onRefresh.. ");
        MHandler.exec(mHandler, MHandler.CLEAR_LIST);
        if(pollingTimer != null) {
            pollingTimer.cancel();
        }
        pollingTimer = new Timer();
        pollingTimer.schedule(new PollingTimerTask(), 0, PrjCfg.CLOUD_DEV_POLLING);
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
        Device device = (Device) adapter.getItem(info.position - 1);
        if(device.isMbusMaster() && device.slvIdx > 0) { // slave device
            menu.setHeaderTitle(device.slvDev.name);
            menu.add(0, R.id.view, 0, getString(R.string.view));
            menu.add(0, R.id.edit, 0, getString(R.string.edit));
            menu.add(0, R.id.mbImport, 0, getString(R.string.import_slvdev_profile));
            menu.add(0, R.id.mbExport, 0, getString(R.string.export_slvdev_profile));
            menu.add(0, R.id.remove, 0, getString(R.string.remove));
        } else { // Normal CloudEnabler Device
            menu.setHeaderTitle(device.name);
            menu.add(0, R.id.view, 0, getString(R.string.view));
            menu.add(0, R.id.edit, 0, getString(R.string.edit));
            menu.add(0, R.id.evtlog, 0, getString(R.string.event_log));
            menu.add(0, R.id.mbImport, 0, getString(R.string.import_profile));
            menu.add(0, R.id.mbExport, 0, getString(R.string.export_profile));
            if(PrjCfg.EN_EXPORT_CLOUD_LOG) {
                menu.add(0, R.id.logExport, 0, getString(R.string.export_cloud_log));
            }
            if(device.sn2 == null) {
                menu.add(0, R.id.swmac, 0, getString(R.string.switch_device));
            } else {
                menu.add(0, R.id.swback, 0, getString(R.string.switch_back_device));
            }
            menu.add(0, R.id.remove, 0, getString(R.string.remove));
        }
    }

    public void onContextMenuClosed(Menu menu) {
        stopPolling = false;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo itemInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        selDev = (Device) adapter.getItem(itemInfo.position - 1);
        if (selDev == null) {
            return true;
        }
        int itemId = item.getItemId();
        if (itemId == R.id.view) {
            if(selDev.isMbusMaster() && selDev.slvIdx < 1) {
                startActivity(itemInfo.position - 1, SlaveDevActivity.class);
            } else {
                startActivity(itemInfo.position - 1, ViewStatusActivity.class);
            }
        } else if (itemId == R.id.edit) {
            if(selDev.isMbusMaster() && selDev.slvIdx > 0) {
                editSlvDev(itemInfo.position - 1);
            } else {
                startActivity(itemInfo.position - 1, EditDeviceActivity.class);
            }
        } else if (itemId == R.id.evtlog) {
            startActivity(itemInfo.position - 1, EventLogActivity.class);
        } else if (itemId == R.id.mbImport) {
            openProfile(itemInfo.position - 1);
        } else if (itemId == R.id.mbExport || itemId == R.id.logExport) {
            final int exportId = itemId;
            final LayoutInflater inflater = LayoutInflater.from(context);
            final View layout = inflater.inflate(R.layout.input_text, null);
            final String defName, fext;
            if(itemId == R.id.mbExport) {
                fext = ".json";
                defName = (selDev.slvIdx > 0)? "slvdev-" + selDev.slvDev.name : "profile-" + selDev.name;
            } else { // R.id.logExport
                fext = ".tar.gz";
                defName = "log-" + selDev.name;
            }
            final EditText editFname = (EditText) layout.findViewById(R.id.text);
            editFname.setFilters(Utils.arrayMerge(editFname.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
            editFname.setHint(getString(R.string.default_name) + ": " + defName);

            final AlertDialog dialog = Kdialog.getDefInputDialog(context).create();
            dialog.setView(layout);
            dialog.setIcon(R.drawable.ic_editor);
            dialog.setTitle(getString(R.string.export_fname));
            dialog.setCancelable(false);
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogIface) {
                    Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String fnameStr = editFname.getText().toString().trim();
                            if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, fnameStr)) {
                                fnameStr = defName;
                            }
                            if(!fnameStr.endsWith(fext)) {
                                fnameStr = fnameStr + fext;
                            }
                            dialog.dismiss();
                            mDialog = Kdialog.getProgress(context, mDialog);
                            exportTimer = new Timer();
                            exportTimer.schedule(new ExportTimer(exportId, selDev, fnameStr), 0, PrjCfg.RUN_ONCE);
                        }
                    });
                }
            });
            dialog.show();
        } else if (itemId == R.id.swmac) {
            switchDev(selDev);
        } else if (itemId == R.id.swback) {
            switchDevBack(selDev);
        } else if (itemId == R.id.remove) {
            if (selDev.enlog) {
                Kdialog.getDefInfoDialog(context).setMessage(getString(R.string.usb_logging)).show();
                return true;
            }
            Kdialog.getMakeSureDialog(context, getString(R.string.make_sure_delete_device))
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Log.e(Dbg._TAG_(), "req = " + requestCode + ", resultCode = " + requestCode);
        openProfile(requestCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private class PollingTimerTask extends TimerTask {
        public void run() {
            try {
                if(stopPolling) { return; }
                getDeviceList();
                MHandler.exec(mHandler, MHandler.UPDATE);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if(!(isExporting || isDeleting) && mDialog != null) {
                    mDialog.dismiss();
                }
            }
        }
    }

    private class SwDevTimerTask extends TimerTask {
        private RequestBody reqBody;

        public SwDevTimerTask(int action, String srcSN, String dstSN) {
            MultipartBuilder entityBuilder = new MultipartBuilder();
            entityBuilder.type(MultipartBuilder.FORM);
            entityBuilder.addFormDataPart("exchg", String.valueOf(action));
            entityBuilder.addFormDataPart("srcSN", srcSN);
            entityBuilder.addFormDataPart("dstSN", dstSN);
            reqBody = entityBuilder.build();
        }

        public void run() {
            try {
                JSONObject jObject = JSONReq.multipart(context, "PUT", PrjCfg.CLOUD_URL + "/api/device/swmac", reqBody);
                if (jObject == null || jObject.getInt("code") != 200) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_save));
                } else {
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_save));
                }
                int statCode = jObject.getInt("code");
                if(statCode == 200) {
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_save));
                    onPullDownToRefresh(null);
                    return;
                }
                String desc = jObject.isNull("desc") ? "" : jObject.getString("desc").trim();
                if(statCode == 400 && desc.matches("Already switch the mac")) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.already_switch_device));
                } else if(statCode == 400 && desc.matches("No such device")) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.no_switch_device));
                } else if(statCode == 400 && desc.matches("Invalid Model")) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_no_model));
                } else if(statCode == 400 && desc.matches("Invalid Model")) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_msg_no_model));
                } else {
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.err_save));
                }
            } catch (Exception e) {
                if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
                }
                e.printStackTrace();
            } finally {
                mDialog.dismiss();
                swDevTimer.cancel();
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
                isDeleting = true;
                deleteDevice(position);
            } catch (Exception e) {
                if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
                }
                e.printStackTrace();
            } finally {
                mDialog.dismiss();
                deleteTimer.cancel();
                isDeleting = false;
            }
        }
    }

    private class ExportTimer extends TimerTask {
        private int type;
        private Device dev;
        private String dstName;

        public ExportTimer(int type, Device dev, String dstName) {
            this.type = type;
            this.dev = dev;
            this.dstName = dstName;
        }

        public void run() {
            try {
                File download = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                String dstPath = download.getPath();
                Log.e(Dbg._TAG_(), "Download to " + dstPath);

                if(type == R.id.mbExport) {
                    String query = (dev.slvIdx > 0) ? "?slvIdx=" + dev.slvIdx : "";
                    WebUtils.download(context, PrjCfg.CLOUD_URL + "/api/device/profile/" + dev.sn + query, dstPath, dstName);
                    MHandler.exec(mHandler, MHandler.SHOW_MSG, getString(R.string.success_export_profile) + " " + dstPath);
                } else { // R.id.logExport
                    JSONObject jLogExport = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/device/datalog/" + dev.sn + "?tryRun=1");
                    int statCode = jLogExport.getInt("code");
                    if (statCode != 200) {
                        MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_export_cloud_log));
                        return;
                    }
                    WebUtils.download(context, PrjCfg.CLOUD_URL + "/api/device/datalog/" + dev.sn, dstPath, dstName);
                    MHandler.exec(mHandler, MHandler.SHOW_MSG, getString(R.string.success_export_cloud_log) + " " + dstPath);
                }
            } catch (Exception e) {
                if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
                }
                if(type == R.id.mbExport) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_export_profile));
                } else {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_export_cloud_log));
                }
                e.printStackTrace();
            } finally {
                exportTimer.cancel();
                mDialog.dismiss();
            }
        }
    }

    private class ImportTimer extends TimerTask {
        private Device selDev;
        private String path;

        public ImportTimer(Device selDev, String path) {
            this.selDev = selDev;
            this.path = path;
        }

        public void run() {
            try {
                isExporting = true;
                MultipartBuilder entityBuilder = new MultipartBuilder();
                entityBuilder.type(MultipartBuilder.FORM);
                entityBuilder.addFormDataPart("sn", selDev.sn);
                entityBuilder.addFormDataPart("profile", "profile", RequestBody.create(MediaType.parse("text/plain"), new File(path)));
                if(selDev.slvIdx > 0) {
                    entityBuilder.addFormDataPart("slvIdx", String.valueOf(selDev.slvIdx));
                }
                JSONObject jObject = JSONReq.multipart(context, "POST", PrjCfg.CLOUD_URL + "/api/device/import", entityBuilder.build());
                //Log.e(Dbg._TAG_(), jObject.toString());
                int statCode = jObject.getInt("code");
                if(statCode == 400 && !jObject.isNull("desc") && jObject.getString("desc").trim().matches("Device is logging")) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.usb_logging));
                } else if(statCode == 400 && !jObject.isNull("desc") && jObject.getString("desc").trim().matches("Invalid Model")) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_import) + " (" + getString(R.string.err_msg_no_model) + ")");
                } else if(statCode == 400 && !jObject.isNull("desc") && jObject.getString("desc").trim().matches("No more available registers")) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.reach_register_limit));
                } else if (statCode != 200) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.err_import));
                } else {
                    MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_import));
                }
            } catch (Exception e) {
                if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
                }
                e.printStackTrace();
            } finally {
                isExporting = false;
                importTimer.cancel();
                mDialog.dismiss();
            }
        }
    }

    private void getDeviceList() throws Exception {
        // Device
        JSONObject jDevObject = JSONReq.send(context, "GET", PrjCfg.CLOUD_URL + "/api/device");
        //Log.e(Dbg._TAG_(), jDevObject.toString());
        int statCode = jDevObject.getInt("code");
        if (statCode == 404 || statCode == 401) {
        } else if (statCode != 200) {
            MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.err_device_list));
            return;
        }
        //Log.e(Dbg._TAG_(), jDevObject.toString());

        // Update devices to list
        JSONArray devices = jDevObject.isNull("devices") ? null : jDevObject.getJSONArray("devices");
        if(devices == null) {
            return;
        }
        // Clear the last list
        MHandler.exec(mHandler, MHandler.CLEAR_LIST);

        // Add the last data
        for (int i = 0; i < devices.length(); i++) {
            JSONObject devObject = devices.getJSONObject(i);
            Device device = new Device(devObject);
            MHandler.exec(mHandler, MHandler.ADD_LIST, device);

            // Modbus Master
            if(device.isMbusMaster() && devObject.has("mstConf")) {
                JSONObject mstConfs = devObject.getJSONObject("mstConf");
                int[] ids = Utils.getJSortedKeys(mstConfs);
                for(int j = 0; j < ids.length; j++) {
                    Device _device = (Device) device.clone();
                    _device.slvIdx = ids[j];
                    _device.slvDev = new MstDev(ids[j], mstConfs.getJSONObject(String.valueOf(ids[j])));
                    if(_device.slvDev.enable) {
                        MHandler.exec(mHandler, MHandler.ADD_LIST, _device);
                    }
                }
            }
        }
    }

    private void editSlvDev(int position) {
        List<Device> devList = adapter.getList();
        Device device = devList.get(position);
        if (device == null) {
            return;
        }
        //Intent intent = new Intent(context, EditAccountActivity.class);
        Intent intent = new Intent(context, EditSlvDevActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("Device", device);
        bundle.putSerializable("MstDev", device.slvDev);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void deleteDevice(int position) throws Exception{
        List<Device> devList = adapter.getList();
        Device rt = devList.get(position);
        if (rt == null) {
            return;
        }
        String reqUrl = (rt.isMbusMaster() && rt.slvIdx > 0) ? ("/api/slvdev/" + rt.sn + "/" + rt.slvIdx) : ("/api/device/" + rt.sn);
        JSONObject jDevObject = JSONReq.send(context, "DELETE", PrjCfg.CLOUD_URL + reqUrl);
        int statCode = jDevObject.getInt("code");
        if(statCode == 200) {
            MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.success_remove));
            MHandler.exec(mHandler, MHandler.DEL_LIST, rt);
            MHandler.exec(mHandler, MHandler.UPDATE);
            return;
        }

        String desc = jDevObject.isNull("desc") ? "" : jDevObject.getString("desc").trim();
        if(statCode == 400 && desc.matches("Already switch the mac")) {
            MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.already_switch_device));
        } else if(statCode == 400 && desc.matches("Device is logging")) {
            MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, getString(R.string.usb_logging));
        } else {
            MHandler.exec(mHandler, MHandler.TOAST_MSG, getString(R.string.err_remove));
        }
    }

    private void openProfile(int position) {
        List<Device> devList = adapter.getList();
        selDev = devList.get(position);
        if (selDev == null) {
            return;
        } else if (selDev.enlog) {
            Kdialog.getDefInfoDialog(context).setMessage(getString(R.string.usb_logging)).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            File downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            intent.setDataAndType(Uri.fromFile(downloadPath), "*/*");
            Log.e(Dbg._TAG_(), "Open file chooser from " + downloadPath);
            startActivityForResult(Intent.createChooser(intent, getString(R.string.import_mdbus_profile)), IMPORT_PROFILE);
        } catch (android.content.ActivityNotFoundException ex) {
            Kdialog.getDefWarningDialog(context)
            .setMessage(getString(R.string.install_fm_hint))
            .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.topnet999.android.filemanager")));
                }
            }).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openProfile(int type, Intent data) {
        if(data == null || selDev == null) {
            Log.e(Dbg._TAG_(), "Selected file:  " + (data == null) + ", Selected device: " + (selDev == null));
            return;
        }
        try {
            Uri uri = data.getData();
            String path = getFileFromUri(context, uri);
            Log.e(Dbg._TAG_(), "Import Modbus file from : " + path);
            if(path != null) {
                mDialog = Kdialog.getProgress(context, mDialog);
                importTimer = new Timer();
                importTimer.schedule(new ImportTimer(selDev, path), 0, PrjCfg.RUN_ONCE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String queryAbsolutePath(final Context context, final Uri uri) {
        final String[] projection = {MediaStore.MediaColumns.DATA};
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                return cursor.getString(index);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public String getFileFromUri(final Context context, final Uri uri) {
        if (uri == null) {
            return null;
        }
        final boolean after44 = Build.VERSION.SDK_INT >= 19;
        if (after44 && DocumentsContract.isDocumentUri(context, uri)) {
            final String authority = uri.getAuthority();
            if ("com.android.externalstorage.documents".equals(authority)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] divide = docId.split(":");
                final String type = divide[0];
                if ("primary".equals(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + divide[1];
                } else if (System.getenv("SECONDARY_STORAGE") != null) {
                    String secondaryStorage = System.getenv("SECONDARY_STORAGE");
                    secondaryStorage = secondaryStorage.split(":")[0];
                    return secondaryStorage + "/" + divide[1];
                } else {
                    Log.e(Dbg._TAG_(), "getExternalStorageDirectory = " + Environment.getExternalStorageDirectory());
                    Log.e(Dbg._TAG_(), "SECONDARY_STORAGE = " + System.getenv("SECONDARY_STORAGE"));
                    Log.e(Dbg._TAG_(), "docId = " + docId);
                    Log.e(Dbg._TAG_(), "Can not find external mount points!");
                    return "/storage/MicroSD/" + divide[1];
                }
            } else if ("com.android.providers.downloads.documents".equals(authority)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final Uri downloadUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(docId));
                return queryAbsolutePath(context, downloadUri);
            } else if ("com.android.providers.media.documents".equals(authority)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] divide = docId.split(":");
                final String type = divide[0];
                Uri mediaUri = null;
                if ("image".equals(type)) {
                    mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    mediaUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    mediaUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                } else {
                    return null;
                }
                mediaUri = ContentUris.withAppendedId(mediaUri, Long.parseLong(divide[1]));
                return queryAbsolutePath(context, mediaUri);
            }
        } else {
            final String scheme = uri.getScheme();
            String path = null;
            if ("content".equals(scheme)) {
                return queryAbsolutePath(context, uri);
            } else if ("file".equals(scheme)) {
                return uri.getPath();
            }
        }
        return null;
    }

    private void startActivity(int position, Class<?> cls) {
        List<Device> devList = adapter.getList();
        Device device = devList.get(position);
        if (device == null) {
            return;
        }
        Intent intent = new Intent(context, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        Bundle bundle = new Bundle();
        bundle.putInt("Type", ViewStatusAdapter.DEVICE);
        bundle.putSerializable("Device", device);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void switchDevBack(final Device selDev) {
        Kdialog.getMakeSureDialog(context, getString(R.string.make_sure_swback))
        .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface _dialog, int which) {
                Log.e(Dbg._TAG_(), "Switch Back MAC, Src: " + selDev.sn + ", Dst: " + selDev.sn2);
                mDialog = Kdialog.getProgress(context, mDialog);
                swDevTimer = new Timer();
                swDevTimer.schedule(new SwDevTimerTask(0, selDev.sn, selDev.sn2), 0, PrjCfg.RUN_ONCE);
            }
        }).show();
    }

    private void switchDev(final Device selDev) {
        final List<Device> devList = adapter.getList();
        final List<CharSequence> snList = new LinkedList<CharSequence>();
        final List<CharSequence> swDevList = new LinkedList<CharSequence>();
        for(Device dev: devList) {
            if(dev.sn.equalsIgnoreCase(selDev.sn)) {
                continue;
            } else if(dev.sn2 != null) {
                continue;
            } else if(!dev.model.equalsIgnoreCase(selDev.model)) {
                continue;
            }
            snList.add(dev.sn);
            swDevList.add(dev.name);
        }
        if(swDevList.size() == 0 || snList.size() == 0) {
            Kdialog.getDefInfoDialog(context).setMessage(getString(R.string.no_switch_device)).show();
            return;
        }
        final CharSequence[] swDevArray = new CharSequence[swDevList.size()];
        swDevList.toArray(swDevArray);

        final AlertDialog dialog;
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getString(R.string.switch_device));
        builder.setIcon(R.drawable.ic_refresh);
        builder.setSingleChoiceItems(swDevArray, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                final String dstSN = (String) snList.get(which);
                final LayoutInflater inflater = LayoutInflater.from(context);
                final View layout = inflater.inflate(R.layout.input_validate, null);
                final TextView txtHint = (TextView) layout.findViewById(R.id.hint);
                final EditText editMac = (EditText) layout.findViewById(R.id.text);
                editMac.setFilters(Utils.arrayMerge(editMac.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));
                txtHint.setText(getString(R.string.make_sure_swmac_dev1) + selDev.sn + "\n" + getString(R.string.make_sure_swmac_dev2) + dstSN + "\n\n" + getString(R.string.make_sure_swmac));

                final AlertDialog macDialog = Kdialog.getDefInputDialog(context).create();
                macDialog.setView(layout);
                macDialog.setIcon(R.drawable.ic_warning);
                macDialog.setTitle(getString(R.string.caution));
                macDialog.setCancelable(false);
                macDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialogIface) {
                        Button btn = macDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                        btn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String input = editMac.getText().toString().trim();
                                if(!input.equalsIgnoreCase(selDev.sn)) {
                                    editMac.setError(getString(R.string.err_msg_invalid_str));
                                    return;
                                }
                                Log.e(Dbg._TAG_(), "Switch MAC, Src: " + selDev.sn + ", Dst: " + snList.get(which));
                                dialog.dismiss();
                                macDialog.dismiss();
                                mDialog = Kdialog.getProgress(context, mDialog);
                                swDevTimer = new Timer();
                                swDevTimer.schedule(new SwDevTimerTask(1, selDev.sn, dstSN), 0, PrjCfg.RUN_ONCE);
                            }
                        });
                    }
                });
                macDialog.show();
            }
        }).setNegativeButton(getString(R.string.cancel), null);
        dialog = builder.create();
        dialog.show();
    }

    private static class MsgHandler extends MHandler {
        private CloudDevFragment fragment;

        public MsgHandler(CloudDevFragment fragment) {
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
                    fragment.adapter.addList((Device) msg.obj);
                    break;
                }
                case MHandler.DEL_LIST: {
                    Device device = (Device) msg.obj;
                    if(device.isMbusMaster() && device.slvIdx > 0) {
                        fragment.adapter.remove(device.sn, device.slvIdx);
                    } else {
                        fragment.adapter.remove(device.sn);
                    }
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