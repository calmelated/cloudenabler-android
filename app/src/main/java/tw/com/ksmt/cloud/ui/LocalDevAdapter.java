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
import java.util.List;

import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Device;
import tw.com.ksmt.cloud.iface.SearchViewAdapter;

public class LocalDevAdapter extends BaseAdapter implements SearchViewAdapter {
    private Context context;
    private List<Device> devList = new ArrayList<Device>();
    private List<Device> filterList = new ArrayList<Device>();
    private boolean queryMode = false;
    private String queryStr;

    public LocalDevAdapter(Context context) {
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
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            view = inflater.inflate(R.layout.list_rows_input, parent, false);
            ImageView imgIcon = (ImageView) view.findViewById(R.id.imgIcon);
            TextView txtTitle = (TextView) view.findViewById(R.id.txtMessage);
            TextView txtMessage = (TextView) view.findViewById(R.id.txtInfo);

            Device device = (queryMode) ? filterList.get(position) : devList.get(position);
            txtTitle.setText(device.name);
            txtMessage.setText(device.ip);
            if (device.isSIO()) {
                imgIcon.setImageResource(R.drawable.ic_device_sio);
            } else if (device.isSIOPlus()) {
                imgIcon.setImageResource(R.drawable.ic_device_sio_plus);
            } else if (device.model.equals(Device.MODEL_CE_63511) ||
                    device.model.equals(Device.MODEL_CE_HY_63511) ||
                    device.model.equals(Device.MODEL_CE_YT_63511)) {
                imgIcon.setImageResource(R.drawable.ic_device_63511);
            } else if (device.model.equals(Device.MODEL_CE_63511W) ||
                    device.model.equals(Device.MODEL_CE_HY_63511W) ||
                    device.model.equals(Device.MODEL_CE_YT_63511W)) {
                imgIcon.setImageResource(R.drawable.ic_device_63511w);
            } else if (device.model.equals(Device.MODEL_CE_63512) ||
                    device.model.equals(Device.MODEL_CE_HY_63512) ||
                    device.model.equals(Device.MODEL_CE_YT_63512)) {
                imgIcon.setImageResource(R.drawable.ic_device_63511);
            } else if (device.model.equals(Device.MODEL_CE_63512W) ||
                    device.model.equals(Device.MODEL_CE_HY_63512W) ||
                    device.model.equals(Device.MODEL_CE_YT_63512W)) {
                imgIcon.setImageResource(R.drawable.ic_device_63511w);
            } else if (device.model.equals(Device.MODEL_CE_63513)) {
                imgIcon.setImageResource(R.drawable.ic_device_63514);
            } else if (device.model.equals(Device.MODEL_CE_63513W)) {
                imgIcon.setImageResource(R.drawable.ic_device_63514w);
            } else if (device.model.equals(Device.MODEL_CE_63514)) {
                imgIcon.setImageResource(R.drawable.ic_device_63514);
            } else if (device.model.equals(Device.MODEL_CE_63514W)) {
                imgIcon.setImageResource(R.drawable.ic_device_63514w);
            } else if (device.model.equals(Device.MODEL_CE_63515) ||
                    device.model.equals(Device.MODEL_CE_HY_63515) ||
                    device.model.equals(Device.MODEL_CE_YT_63515)) {
                imgIcon.setImageResource(R.drawable.ic_device_63515);
            } else if (device.model.equals(Device.MODEL_CE_63515W) ||
                    device.model.equals(Device.MODEL_CE_HY_63515W) ||
                    device.model.equals(Device.MODEL_CE_YT_63515W)) {
                imgIcon.setImageResource(R.drawable.ic_device_63515w);
            } else if (device.model.equals(Device.MODEL_CE_63516) ||
                    device.model.equals(Device.MODEL_CE_HY_63516) ||
                    device.model.equals(Device.MODEL_CE_YT_63516)) {
                imgIcon.setImageResource(R.drawable.ic_device_63516);
            } else if (device.model.equals(Device.MODEL_CE_63516W) ||
                    device.model.equals(Device.MODEL_CE_HY_63516W) ||
                    device.model.equals(Device.MODEL_CE_YT_63516W)) {
                imgIcon.setImageResource(R.drawable.ic_device_63516w);
            } else {
                imgIcon.setImageResource(R.drawable.ic_device_local);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return view;
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