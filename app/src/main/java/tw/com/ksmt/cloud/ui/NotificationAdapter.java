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
import tw.com.ksmt.cloud.iface.Notification;
import tw.com.ksmt.cloud.iface.SearchViewAdapter;
import tw.com.ksmt.cloud.libs.Utils;

public class NotificationAdapter extends BaseAdapter implements SearchViewAdapter {
    private Context context;
    private List<Notification> ntList = new ArrayList<Notification>();
    private List<Notification> filterList = new ArrayList<Notification>();
    private boolean queryMode = false;
    private String queryStr;

    public NotificationAdapter(Context context) {
        this.context = context;
    }

    public List<Notification> getList() {
        return (queryMode) ? filterList : ntList;
    }

    public void addList(Notification notification) {
        ntList.add(notification);
        if(queryMode) {
            addFilterList(notification);
        }
    }

    public void clearList() {
        ntList.clear();
        filterList.clear();
    }

    @Override
    public int getCount() {
        return (queryMode) ? filterList.size() : ntList.size();
    }

    @Override
    public Object getItem(int position) {
        if(queryMode) {
            if(position < filterList.size()) {
                return filterList.get(position);
            }
        } else {
            if(position < ntList.size()) {
                return ntList.get(position);
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
            int listSz = (queryMode) ? filterList.size() : ntList.size();
            if(listSz < 1) {
                return view;
            }
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            view = inflater.inflate(R.layout.list_rows_texts, parent, false);
            TextView txtMessage = (TextView) view.findViewById(R.id.txtMessage);
            TextView txtInfo = (TextView) view.findViewById(R.id.txtInfo);
            Notification vs = (queryMode) ? filterList.get(position) : ntList.get(position);
            txtMessage.setText(vs.message);
            String infoStr = Utils.unix2Datetime(vs.time, "yyyy-MM-dd HH:mm") + " - By " + vs.account ;
            infoStr = (vs.company == null) ? infoStr : context.getString(R.string.company) + ": " + vs.company;
            if(vs.company == null) {
                txtInfo.setText(Utils.unix2Datetime(vs.time, "yyyy-MM-dd HH:mm") + " - " + context.getString(R.string.by) + ": " + vs.account);
            } else {
                txtInfo.setText(Utils.unix2Datetime(vs.time, "yyyy-MM-dd HH:mm") + " - " + context.getString(R.string.company) + ": " + vs.company + ", " + context.getString(R.string.by) + ": " + vs.account);
            }

            ImageView imgIcon = (ImageView) view.findViewById(R.id.imgIcon);
            if(vs.done == 0) {
                if(vs.priority == 1) { // medium
                    imgIcon.setImageResource(R.drawable.ic_alarm_medium_yellow);
                } else if(vs.priority == 2) { // high
                    imgIcon.setImageResource(R.drawable.ic_alarm_high_red);
                } else { // == 0, low
                    imgIcon.setImageResource(R.drawable.ic_alarm_low_green);
                }
            } else {
                if(vs.priority == 1) { // medium
                    imgIcon.setImageResource(R.drawable.ic_alarm_medium_gray);
                } else if(vs.priority == 2) { // high
                    imgIcon.setImageResource(R.drawable.ic_alarm_high_gray);
                } else { // == 0, low
                    imgIcon.setImageResource(R.drawable.ic_alarm_low_gray);
                }
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
            createFilterList(ntList);
        }
    }

    private void addFilterList(Notification notification) {
        if(notification.message != null && notification.message.toLowerCase().contains(queryStr.toLowerCase())) {
            filterList.add(notification);
        } else if(notification.account != null && notification.account.toLowerCase().contains(queryStr.toLowerCase())) {
            filterList.add(notification);
        }
    }

    private void createFilterList(List<Notification> ntList) {
        filterList.clear();
        for(Notification notification: ntList) {
            addFilterList(notification);
        }
    }
}