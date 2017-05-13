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
import tw.com.ksmt.cloud.iface.EventLog;
import tw.com.ksmt.cloud.iface.SearchViewAdapter;
import tw.com.ksmt.cloud.libs.Utils;

public class EventLogAdapter extends BaseAdapter implements SearchViewAdapter {
    private Context context;
    private List<EventLog> evtList = new ArrayList<EventLog>();
    private List<EventLog> filterList = new ArrayList<EventLog>();
    private boolean queryMode = false;
    private String queryStr;

    public EventLogAdapter(Context context) {
        this.context = context;
    }

    public List<EventLog> getList() {
        return (queryMode) ? filterList : evtList;
    }

    public void addList(EventLog evtlog) {
        evtList.add(evtlog);
        if(queryMode) {
            addFilterList(evtlog);
        }
    }

    public void clearList() {
        evtList.clear();
        filterList.clear();
    }

    @Override
    public int getCount() {
        return (queryMode) ? filterList.size() : evtList.size();
    }

    @Override
    public Object getItem(int position) {
        if(queryMode) {
            if(position < filterList.size()) {
                return filterList.get(position);
            }
        } else {
            if(position < evtList.size()) {
                return evtList.get(position);
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
            int listSz = (queryMode) ? filterList.size() : evtList.size();
            if(listSz < 1) {
                return view;
            }
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            view = inflater.inflate(R.layout.list_rows, parent, false);
            TextView txtMessage = (TextView) view.findViewById(R.id.txtMessage);
            TextView txtInfo = (TextView) view.findViewById(R.id.txtInfo);
            ImageView imgIcon = (ImageView) view.findViewById(R.id.imgIcon);

            EventLog evtlog = (queryMode) ? filterList.get(position) : evtList.get(position);
            txtMessage.setText(evtlog.message);
            txtInfo.setText(Utils.unix2Datetime(evtlog.time, "yyyy-MM-dd HH:mm"));
            if(evtlog.type == EventLog.TYPE_ERROR) {
                imgIcon.setImageResource(R.drawable.ic_disable_1);
            } else if(evtlog.type == EventLog.TYPE_WARNING) {
                imgIcon.setImageResource(R.drawable.ic_warning_1);
            } else {
                imgIcon.setImageResource(R.drawable.ic_enable_1);
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
            createFilterList(evtList);
        }
    }

    private void addFilterList(EventLog evtlog) {
        if(evtlog.message != null && evtlog.message.toLowerCase().contains(queryStr.toLowerCase())) {
            filterList.add(evtlog);
        }
    }

    private void createFilterList(List<EventLog> ntList) {
        filterList.clear();
        for(EventLog evtlog: ntList) {
            addFilterList(evtlog);
        }
    }
}