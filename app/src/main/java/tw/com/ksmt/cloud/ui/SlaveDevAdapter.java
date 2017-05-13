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

import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.MstDev;
import tw.com.ksmt.cloud.iface.SearchViewAdapter;

public class SlaveDevAdapter extends BaseAdapter implements SearchViewAdapter {
    private Context context;
    private List<MstDev> mstDevList = new ArrayList<MstDev>();
    private List<MstDev> filterList = new ArrayList<MstDev>();
    private boolean queryMode = false;
    private String queryStr;

    public SlaveDevAdapter(Context context) {
        this.context = context;
    }

    public List<MstDev> getList() {
        return (queryMode) ? filterList : mstDevList;
    }

    public void addList(MstDev mstDev) {
        mstDevList.add(mstDev);
        if(queryMode) {
            addFilterList(mstDev);
        }
    }

    public void setList(List<MstDev> mstDevList) {
        this.mstDevList = mstDevList;
        if(queryMode) {
            createFilterList(mstDevList);
        }
    }

    public void clearList() {
        this.mstDevList.clear();
        this.filterList.clear();
    }

    @Override
    public int getCount() {
        return (queryMode) ? filterList.size() : mstDevList.size();
    }

    @Override
    public Object getItem(int position) {
        if(queryMode) {
            if(position < filterList.size()) {
                return filterList.get(position);
            }
        } else {
            if(position < mstDevList.size()) {
                return mstDevList.get(position);
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
            int listSz = (queryMode) ? filterList.size() : mstDevList.size();
            if (listSz < 1) {
                return view;
            }
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            view = inflater.inflate(R.layout.list_rows_input, parent, false);
            ImageView imgIcon = (ImageView) view.findViewById(R.id.imgIcon);
            TextView txtTitle = (TextView) view.findViewById(R.id.txtMessage);
            TextView txtMessage = (TextView) view.findViewById(R.id.txtInfo);

            MstDev mstDev = (queryMode) ? filterList.get(position) : mstDevList.get(position);
            txtTitle.setText(mstDev.name);
            if(mstDev.type.equals("TCP")) {
                String showStr = "TCP/IP: " + mstDev.ip + ":" + mstDev.port;
                if(mstDev.slvId > 0 && mstDev.slvId < 255) {
                    showStr += ", ID: " + mstDev.slvId;
                }
                txtMessage.setText(showStr);
            } else {
                txtMessage.setText(mstDev.comPort + ", ID: "+ mstDev.slvId);
            }
            imgIcon.setImageResource(R.drawable.ic_cloud_server);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return view;
        }
    }

    public void remove(int mstId) {
        remove(mstDevList, mstId);
        if(queryMode) {
            remove(filterList, mstId);
        }
    }

    private void remove(List<MstDev> list, int mstId) {
        Iterator<MstDev> it = list.iterator();
        while(it.hasNext()) {
            MstDev mstItem = it.next();
            if(mstItem.id == mstId) {
                it.remove();
                return;
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
            createFilterList(mstDevList);
        }
    }

    private void addFilterList(MstDev mstDev) {
        if(mstDev.name != null && mstDev.name.toLowerCase().contains(queryStr.toLowerCase())) {
            filterList.add(mstDev);
        }
    }

    private void createFilterList(List<MstDev> mstDevList) {
        filterList.clear();
        for(MstDev mstDev : mstDevList) {
            addFilterList(mstDev);
        }
    }
}