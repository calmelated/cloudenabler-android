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
import tw.com.ksmt.cloud.iface.Audit;
import tw.com.ksmt.cloud.iface.SearchViewAdapter;
import tw.com.ksmt.cloud.libs.Utils;

public class AuditAdapter extends BaseAdapter implements SearchViewAdapter {
    private Context context;
    private List<Audit> adtList = new ArrayList<Audit>();
    private List<Audit> filterList = new ArrayList<Audit>();
    private boolean queryMode = false;
    private String queryStr;

    public AuditAdapter(Context context) {
        this.context = context;
    }

    public List<Audit> getList() {
        return (queryMode) ? filterList : adtList;
    }

    public void addList(Audit audit) {
        adtList.add(audit);
        if(queryMode) {
            addFilterList(audit);
        }
    }

    public void clearList() {
        adtList.clear();
        filterList.clear();
    }

    @Override
    public int getCount() {
        return (queryMode) ? filterList.size() : adtList.size();
    }

    @Override
    public Object getItem(int position) {
        if(queryMode) {
            if(position < filterList.size()) {
                return filterList.get(position);
            }
        } else {
            if(position < adtList.size()) {
                return adtList.get(position);
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
            int listSz = (queryMode) ? filterList.size() : adtList.size();
            if(listSz < 1) {
                return view;
            }
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            view = inflater.inflate(R.layout.list_rows_small_texts, parent, false);
            TextView txtMessage = (TextView) view.findViewById(R.id.txtMessage);
            TextView txtInfo = (TextView) view.findViewById(R.id.txtInfo);
            ImageView imgIcon = (ImageView) view.findViewById(R.id.imgIcon);

            Audit vs = (queryMode) ? filterList.get(position) : adtList.get(position);
            txtMessage.setText(vs.message);
            txtInfo.setText(Utils.unix2Datetime(vs.time, "yyyy-MM-dd HH:mm") + " - By " + vs.account);

            if (vs.msgCode == Audit.CLEAR_ALARM || vs.msgCode == Audit.CLEAR_AUDIT || vs.msgCode == Audit.CLEAR_EVTLOG || vs.msgCode == Audit.CLEAR_IOSTLOG) {
                imgIcon.setImageResource(R.drawable.ic_clear_1);
            } else if(vs.msgCode == Audit.DELETE_DEV || vs.msgCode == Audit.DELETE_GROUP || vs.msgCode == Audit.DELETE_REG || vs.msgCode == Audit.DELETE_USER || vs.msgCode == Audit.DELETE_ANNOUNCE || vs.msgCode == Audit.DELETE_FLINK) {
                imgIcon.setImageResource(R.drawable.ic_delete);
            } else if(vs.msgCode == Audit.DEV_IMPORT || vs.msgCode == Audit.DUP_REG || vs.msgCode == Audit.EDIT_DEV || vs.msgCode == Audit.EDIT_GROUP || vs.msgCode == Audit.EDIT_REG || vs.msgCode == Audit.EDIT_USER || vs.msgCode == Audit.SND_FTP_LOG || vs.msgCode == Audit.CHG_LANG || vs.msgCode == Audit.EDIT_ANNOUNCE || vs.msgCode == Audit.EDIT_FLINK) {
                imgIcon.setImageResource(R.drawable.ic_editor);
            } else if(vs.msgCode == Audit.NEW_DEV) {
                imgIcon.setImageResource(R.drawable.ic_new_device);
            } else if(vs.msgCode == Audit.NEW_GROUP) {
                imgIcon.setImageResource(R.drawable.ic_new_group);
            } else if(vs.msgCode == Audit.NEW_REG) {
                imgIcon.setImageResource(R.drawable.ic_new_register);
            } else if(vs.msgCode == Audit.NEW_USER) {
                imgIcon.setImageResource(R.drawable.ic_new_user);
            } else if(vs.msgCode == Audit.SET_REG) {
                imgIcon.setImageResource(R.drawable.ic_warning);
            } else if(vs.msgCode == Audit.USER_ACTIVATE) {
                imgIcon.setImageResource(R.drawable.ic_user);
            } else if(vs.msgCode == Audit.USER_LOGOUT || vs.msgCode == Audit.USER_LOGIN) {
                imgIcon.setImageResource(R.drawable.ic_login);
            } else if(vs.msgCode == Audit.NEW_ANNOUNCE || vs.msgCode == Audit.ANNOUNCE_ALL_CMP || vs.msgCode == Audit.ANNOUNCE_SUB_CMP) {
                imgIcon.setImageResource(R.drawable.ic_file_add);
            } else if(vs.msgCode == Audit.NEW_FLINK) {
                imgIcon.setImageResource(R.drawable.ic_file_add);
            } else {
                imgIcon.setImageResource(R.drawable.ic_warning);
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
            createFilterList(adtList);
        }
    }

    private void addFilterList(Audit notification) {
        if(notification.message != null && notification.message.toLowerCase().contains(queryStr.toLowerCase())) {
            filterList.add(notification);
        } else if(notification.account != null && notification.account.toLowerCase().contains(queryStr.toLowerCase())) {
            filterList.add(notification);
        }
    }

    private void createFilterList(List<Audit> ntList) {
        filterList.clear();
        for(Audit notification: ntList) {
            addFilterList(notification);
        }
    }
}