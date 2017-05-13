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
import tw.com.ksmt.cloud.iface.CloudStatus;
import tw.com.ksmt.cloud.iface.SearchViewAdapter;
import tw.com.ksmt.cloud.libs.Utils;

public class CloudStatusAdapter extends BaseAdapter implements SearchViewAdapter {
    private Context context;
    private List<CloudStatus> servtList = new ArrayList<CloudStatus>();
    private List<CloudStatus> filterList = new ArrayList<CloudStatus>();
    private boolean queryMode = false;
    private String queryStr;

    public CloudStatusAdapter(Context context) {
        this.context = context;
    }

    public List<CloudStatus> getList() {
        return (queryMode) ? filterList : servtList;
    }

    public void addList(CloudStatus cloudStatus) {
        servtList.add(cloudStatus);
        if(queryMode) {
            addFilterList(cloudStatus);
        }
    }

    public void setList(List<CloudStatus> statusList) {
        this.servtList = statusList;
        if(queryMode) {
            createFilterList(servtList);
        }
    }

    public void clearList() {
        this.servtList.clear();
        this.filterList.clear();
    }

    @Override
    public int getCount() {
        return (queryMode) ? filterList.size() : servtList.size();
    }

    @Override
    public Object getItem(int position) {
        if(queryMode) {
            if(position < filterList.size()) {
                return filterList.get(position);
            }
        } else {
            if(position < servtList.size()) {
                return servtList.get(position);
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
            int listSz = (queryMode) ? filterList.size() : servtList.size();
            if (listSz < 1) {
                return view;
            }
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            final CloudStatus cloudStatus = (queryMode) ? filterList.get(position) : servtList.get(position);
            if(cloudStatus.type == CloudStatus.TYPE_SERVICE) {
                view = inflater.inflate(R.layout.list_rows_service, parent, false);
                ImageView imgIcon = (ImageView) view.findViewById(R.id.imgIcon);
                TextView txtTitle = (TextView) view.findViewById(R.id.txtMessage);
                ImageView imgIcon2 = (ImageView) view.findViewById(R.id.imgIcon2);

                txtTitle.setText(context.getString(cloudStatus.servNameId));
                if(cloudStatus.servNameId == R.string.cloud) {
                    imgIcon.setImageResource(R.drawable.ic_cloud);
                } else if(cloudStatus.servNameId == R.string.notification) {
                    imgIcon.setImageResource(R.drawable.ic_notification);
                } else if(cloudStatus.servNameId == R.string.email) {
                    imgIcon.setImageResource(R.drawable.ic_email);
                } else if(cloudStatus.servNameId == R.string.net_quality) {
                    imgIcon.setImageResource(R.drawable.ic_network);
                } else if(cloudStatus.servNameId == R.string.app_download) {
                    imgIcon.setImageResource(R.drawable.ic_download_3);
                }
                if(cloudStatus.status == 9) {
                    imgIcon2.setImageResource(R.drawable.ic_warning_1);
                } else if(cloudStatus.status == 0) {
                    imgIcon2.setImageResource(R.drawable.ic_error_1);
                } else {
                    imgIcon2.setImageResource(R.drawable.ic_enable_1);
                }
            } else if(cloudStatus.type == CloudStatus.TYPE_TITLE){
                view = inflater.inflate(R.layout.list_title, parent, false);
                TextView txtTitle = (TextView) view.findViewById(R.id.txtInfo);
                txtTitle.setText(cloudStatus.title);
            } else {
                view = inflater.inflate(R.layout.list_rows, parent, false);
                ImageView imgIcon = (ImageView) view.findViewById(R.id.imgIcon);
                imgIcon.setImageResource(R.drawable.ic_event_log);
                TextView txtTitle = (TextView) view.findViewById(R.id.txtMessage);
                txtTitle.setText(cloudStatus.errMsg);

                TextView txtMessage = (TextView) view.findViewById(R.id.txtInfo);
                String durStr = "";
                if(cloudStatus.duration.length() > 0) {
                    durStr = ", "  + context.getString(R.string.duration) + ": " + cloudStatus.duration;
                }
                txtMessage.setText(context.getString(R.string.time) + ": " + Utils.unix2Datetime(cloudStatus.updateTime) + durStr);
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
            createFilterList(servtList);
        }
    }

    private void addFilterList(CloudStatus cloudStatus) {
        if(cloudStatus.errMsg != null && cloudStatus.errMsg.toLowerCase().contains(queryStr.toLowerCase())) {
            filterList.add(cloudStatus);
        }
    }

    private void createFilterList(List<CloudStatus> statusList) {
        filterList.clear();
        for(CloudStatus cloudStatus : statusList) {
            addFilterList(cloudStatus);
        }
    }
}