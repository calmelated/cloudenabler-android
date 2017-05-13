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
import tw.com.ksmt.cloud.iface.IoStLog;
import tw.com.ksmt.cloud.iface.SearchViewAdapter;
import tw.com.ksmt.cloud.libs.Utils;

public class IoStLogAdapter extends BaseAdapter implements SearchViewAdapter {
    private Context context;
    private List<IoStLog> iostList = new ArrayList<IoStLog>();
    private List<IoStLog> filterList = new ArrayList<IoStLog>();
    private boolean queryMode = false;
    private String queryStr;

    public IoStLogAdapter(Context context) {
        this.context = context;
    }

    public List<IoStLog> getList() {
        return (queryMode) ? filterList : iostList;
    }

    public void addList(IoStLog ioStLog) {
        iostList.add(ioStLog);
        if(queryMode) {
            addFilterList(ioStLog);
        }
    }

    public void clearList() {
        iostList.clear();
        filterList.clear();
    }

    @Override
    public int getCount() {
        return (queryMode) ? filterList.size() : iostList.size();
    }

    @Override
    public Object getItem(int position) {
        if(queryMode) {
            if(position < filterList.size()) {
                return filterList.get(position);
            }
        } else {
            if(position < iostList.size()) {
                return iostList.get(position);
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
            int listSz = (queryMode) ? filterList.size() : iostList.size();
            if(listSz < 1) {
                return view;
            }
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            view = inflater.inflate(R.layout.list_rows_input, parent, false);
            TextView txtMessage = (TextView) view.findViewById(R.id.txtMessage);
            TextView txtInfo = (TextView) view.findViewById(R.id.txtInfo);
            ImageView imgIcon = (ImageView) view.findViewById(R.id.imgIcon);

            IoStLog vs = (queryMode) ? filterList.get(position) : iostList.get(position);
            txtInfo.setText(Utils.unix2Datetime(vs.time, "yyyy-MM-dd HH:mm") + " - By " + vs.account);
            txtMessage.setText(vs.message);
            imgIcon.setImageResource(R.drawable.ic_event_log);
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
            createFilterList(iostList);
        }
    }

    private void addFilterList(IoStLog ioStLog) {
        if(ioStLog.message != null && ioStLog.message.toLowerCase().contains(queryStr.toLowerCase())) {
            filterList.add(ioStLog);
        } else if(ioStLog.account != null && ioStLog.account.toLowerCase().contains(queryStr.toLowerCase())) {
            filterList.add(ioStLog);
        }
    }

    private void createFilterList(List<IoStLog> ioStLogList) {
        filterList.clear();
        for(IoStLog ioStLog: ioStLogList) {
            addFilterList(ioStLog);
        }
    }
}