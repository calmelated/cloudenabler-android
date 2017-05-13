package tw.com.ksmt.cloud.ui;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Device;
import tw.com.ksmt.cloud.iface.SearchViewAdapter;

public class CloudDevAdapter extends BaseAdapter implements SearchViewAdapter {
    private Context context;
    private List<Device> devList = new ArrayList<Device>();
    private List<Device> filterList = new ArrayList<Device>();
    private boolean queryMode = false;
    private String queryStr;

    public CloudDevAdapter(Context context) {
        this.context = context;
    }

    public List<Device> getList() {
        return (queryMode) ? filterList : devList;
    }

    public void addList(Device rt) {
        devList.add(rt);
        if(queryMode) {
            addFilterList(rt);
        }
    }

    public void setList(List<Device> rtList) {
        this.devList = rtList;
        if(queryMode) {
            createFilterList(rtList);
        }
    }

    public void clearList() {
        devList.clear();
        filterList.clear();
    }

    @Override
    public int getCount() {
        return (queryMode) ? filterList.size() : devList.size();
    }

    @Override
    public Object getItem(int position) {
        if(queryMode) {
            if(position < filterList.size()) {
                return filterList.get(position);
            }
        } else {
            if(position < devList.size()) {
                return devList.get(position);
            }
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        try {
            int listSz = (queryMode) ? filterList.size() : devList.size();
            if(listSz < 1) {
                return view;
            }
            Device device = (queryMode) ? filterList.get(position) : devList.get(position);
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            if(device.isMbusMaster() && device.slvIdx > 0) { // slave devices
                view = inflater.inflate(R.layout.list_rows_slvdev, parent, false);
            } else {
                view = inflater.inflate(R.layout.list_rows_input, parent, false);
            }
            ImageView imgIcon = (ImageView) view.findViewById(R.id.imgIcon);
            TextView txtTitle = (TextView) view.findViewById(R.id.txtMessage);
            TextView txtMessage = (TextView) view.findViewById(R.id.txtInfo);
            ImageView rightIcon = (ImageView) view.findViewById(R.id.right_arrow);

            if(device.isMbusMaster() && device.slvIdx < 1) { // the first device
                txtTitle.setText(device.name);
                if(device.sn2 == null) {
                    txtMessage.setText(device.sn);
                } else {
                    txtMessage.setText(device.sn + " (" + context.getString(R.string.from) + ": " + device.sn2 + ")");
                }
                if(device.status == 0) {
                    imgIcon.setImageResource(R.drawable.ic_device_nosync);
                } else {
                    imgIcon.setImageResource(R.drawable.ic_device_sync);
                }
                if(PrjCfg.USER_MODE == PrjCfg.MODE_ADMIN || PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                    rightIcon.setImageResource(R.drawable.ic_settings_16);
                } else {
                    rightIcon.setVisibility(View.INVISIBLE);
                }
            } else if(device.isMbusMaster() && device.slvIdx > 0) { // slave devices
                txtTitle.setText(device.slvDev.name);
                if(device.slvDev.type.equals("TCP")) {
                    String showStr = "TCP/IP: " + device.slvDev.ip + ":" + device.slvDev.port;
                    if(device.slvDev.slvId > 0 && device.slvDev.slvId < 255) {
                        showStr += ", ID: " + device.slvDev.slvId;
                    }
                    txtMessage.setText(showStr);
                } else {
                    txtMessage.setText(device.slvDev.comPort + ", ID: "+ device.slvDev.slvId);
                }
                if(device.status == 0 || device.slvDev.status < 0) { // unknown
                    imgIcon.setImageResource(R.drawable.ic_init_slave);
                } else if(device.slvDev.status == 0) { // offline
                    imgIcon.setImageResource(R.drawable.ic_slave_offline);
                } else { // online
                    imgIcon.setImageResource(R.drawable.ic_slave_online);
                }
            } else { // 63511/63514W
                txtTitle.setText(device.name);
                if(device.sn2 == null) {
                    txtMessage.setText(device.sn);
                } else {
                    txtMessage.setText(device.sn + " (" + context.getString(R.string.from) + ": " + device.sn2 + ")");
                }
                if(device.status == 0) {
                    imgIcon.setImageResource(R.drawable.ic_device_nosync);
                } else {
                    imgIcon.setImageResource(R.drawable.ic_device_sync);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return view;
        }
    }

    public void remove(String sn) {
        remove(devList, sn);
        if(queryMode) {
            remove(filterList, sn);
        }
    }

    public void remove(String sn, int slvIdx) {
        remove(devList, sn, slvIdx);
        if(queryMode) {
            remove(filterList, sn, slvIdx);
        }
    }

    private void remove(List<Device> list, String sn) {
        Iterator<Device> it = list.iterator();
        while(it.hasNext()) {
            Device devItem = it.next();
            if(devItem.sn == null) {
                continue;
            }
            if (devItem.sn.equals(sn)) {
                it.remove();
            }
        }
    }

    private void remove(List<Device> list, String sn, int slvIdx) {
        Iterator<Device> it = list.iterator();
        while(it.hasNext()) {
            Device devItem = it.next();
            if(devItem.sn == null) {
                continue;
            }
            if (devItem.sn.equals(sn) && devItem.slvIdx == slvIdx) {
                it.remove();
            }
        }
    }

    public void setQueryStr(String query) {
        if(query.length() < 1) {
            queryMode = false;
            return;
        }
        synchronized(this) {
            queryMode = true;
            queryStr = query;
            createFilterList(devList);
        }
    }

    private void addFilterList(Device rt) {
        if(rt.name != null && rt.name.toLowerCase().contains(queryStr.toLowerCase())) {
            filterList.add(rt);
        } else if(rt.sn != null && rt.sn.toLowerCase().contains(queryStr.toLowerCase())) {
            filterList.add(rt);
        }
    }

    private void createFilterList(List<Device> rtList) {
        filterList.clear();
        for(Device rt : rtList) {
            addFilterList(rt);
        }
    }
}