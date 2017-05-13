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
import tw.com.ksmt.cloud.iface.Announce;
import tw.com.ksmt.cloud.iface.SearchViewAdapter;
import tw.com.ksmt.cloud.libs.Utils;

public class AnnounceAdapter extends BaseAdapter implements SearchViewAdapter {
    private Context context;
    private List<Announce> announceList = new ArrayList<Announce>();
    private List<Announce> filterList = new ArrayList<Announce>();
    private boolean queryMode = false;
    private String queryStr;

    public AnnounceAdapter(Context context) {
        this.context = context;
    }

    public List<Announce> getList() {
        return (queryMode) ? filterList : announceList;
    }

    public void addList(Announce account) {
        announceList.add(account);
        if(queryMode) {
            addFilterList(account);
        }
    }

    public void setList(List<Announce> accountList) {
        this.announceList = accountList;
        if(queryMode) {
            createFilterList(accountList);
        }
    }

    public void clearList() {
        this.announceList.clear();
        this.filterList.clear();
    }

    @Override
    public int getCount() {
        return (queryMode) ? filterList.size() : announceList.size();
    }

    @Override
    public Object getItem(int position) {
        if(queryMode) {
            if(position < filterList.size()) {
                return filterList.get(position);
            }
        } else {
            if(position < announceList.size()) {
                return announceList.get(position);
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
            int listSz = (queryMode) ? filterList.size() : announceList.size();
            if (listSz < 1) {
                return view;
            }
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            view = inflater.inflate(R.layout.list_rows_input, parent, false);
            ImageView imgIcon = (ImageView) view.findViewById(R.id.imgIcon);
            TextView txtTitle = (TextView) view.findViewById(R.id.txtMessage);
            TextView txtMessage = (TextView) view.findViewById(R.id.txtInfo);

            Announce announce = (queryMode) ? filterList.get(position) : announceList.get(position);
            txtTitle.setText(announce.message);
            txtMessage.setText(Utils.unix2Datetime(announce.time, "yyyy-MM-dd HH:mm"));
            imgIcon.setImageResource((PrjCfg.USER_MODE == PrjCfg.MODE_USER) ? R.drawable.ic_event_log : R.drawable.ic_editor);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return view;
        }
    }

    public void remove(Announce announce) {
        remove(announceList, announce);
        if(queryMode) {
            remove(filterList, announce);
        }
    }

    private void remove(List<Announce> list, Announce announce) {
        Iterator<Announce> it = list.iterator();
        while(it.hasNext()) {
            Announce item = it.next();
            if(item.time == announce.time) {
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
            createFilterList(announceList);
        }
    }

    private void addFilterList(Announce announce) {
        if(announce.message != null && announce.message.toLowerCase().contains(queryStr.toLowerCase())) {
            filterList.add(announce);
        }
    }

    private void createFilterList(List<Announce> announceList) {
        filterList.clear();
        for(Announce announce : announceList) {
            addFilterList(announce);
        }
    }
}