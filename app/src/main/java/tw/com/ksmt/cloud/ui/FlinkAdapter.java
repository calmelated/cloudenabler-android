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
import tw.com.ksmt.cloud.iface.Flink;
import tw.com.ksmt.cloud.iface.SearchViewAdapter;

public class FlinkAdapter extends BaseAdapter implements SearchViewAdapter {
    private Context context;
    private List<Flink> flinkList = new ArrayList<Flink>();
    private List<Flink> filterList = new ArrayList<Flink>();
    private boolean queryMode = false;
    private String queryStr;

    public FlinkAdapter(Context context) {
        this.context = context;
    }

    public List<Flink> getList() {
        return (queryMode) ? filterList : flinkList;
    }

    public void addList(Flink account) {
        flinkList.add(account);
        if(queryMode) {
            addFilterList(account);
        }
    }

    public void setList(List<Flink> flinkList) {
        this.flinkList = flinkList;
        if(queryMode) {
            createFilterList(flinkList);
        }
    }

    public void clearList() {
        this.flinkList.clear();
        this.filterList.clear();
    }

    @Override
    public int getCount() {
        return (queryMode) ? filterList.size() : flinkList.size();
    }

    @Override
    public Object getItem(int position) {
        if(queryMode) {
            if(position < filterList.size()) {
                return filterList.get(position);
            }
        } else {
            if(position < flinkList.size()) {
                return flinkList.get(position);
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
            int listSz = (queryMode) ? filterList.size() : flinkList.size();
            if (listSz < 1) {
                return view;
            }
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            view = inflater.inflate(R.layout.list_rows_input, parent, false);
            ImageView imgIcon = (ImageView) view.findViewById(R.id.imgIcon);
            TextView txtTitle = (TextView) view.findViewById(R.id.txtMessage);
            TextView txtMessage = (TextView) view.findViewById(R.id.txtInfo);

            Flink flink = (queryMode) ? filterList.get(position) : flinkList.get(position);
            txtTitle.setText(flink.desc);
            txtMessage.setText(flink.url);
            imgIcon.setImageResource(R.drawable.ic_editor);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return view;
        }
    }

    public void remove(Flink flink) {
        remove(flinkList, flink);
        if(queryMode) {
            remove(filterList, flink);
        }
    }

    private void remove(List<Flink> list, Flink flink) {
        Iterator<Flink> it = list.iterator();
        while(it.hasNext()) {
            Flink item = it.next();
            if(item.id == flink.id) {
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
            createFilterList(flinkList);
        }
    }

    private void addFilterList(Flink flink) {
        if(flink.desc != null && flink.desc.toLowerCase().contains(queryStr.toLowerCase())) {
            filterList.add(flink);
        }
    }

    private void createFilterList(List<Flink> flinkList) {
        filterList.clear();
        for(Flink flink: flinkList) {
            addFilterList(flink);
        }
    }
}