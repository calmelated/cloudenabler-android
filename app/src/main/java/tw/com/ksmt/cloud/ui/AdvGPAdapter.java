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
import tw.com.ksmt.cloud.iface.AdvGP;
import tw.com.ksmt.cloud.iface.SearchViewAdapter;

public class AdvGPAdapter extends BaseAdapter implements SearchViewAdapter {
    private Context context;
    private List<AdvGP> advGPList = new ArrayList<AdvGP>();
    private List<AdvGP> filterList = new ArrayList<AdvGP>();
    private boolean queryMode = false;
    private String queryStr;

    public AdvGPAdapter(Context context) {
        this.context = context;
    }

    public List<AdvGP> getList() {
        return (queryMode) ? filterList : advGPList;
    }

    public void addList(AdvGP account) {
        advGPList.add(account);
        if(queryMode) {
            addFilterList(account);
        }
    }

    public void setList(List<AdvGP> accountList) {
        this.advGPList = accountList;
        if(queryMode) {
            createFilterList(accountList);
        }
    }

    public void clearList() {
        this.advGPList.clear();
        this.filterList.clear();
    }

    @Override
    public int getCount() {
        return (queryMode) ? filterList.size() : advGPList.size();
    }

    @Override
    public Object getItem(int position) {
        if(queryMode) {
            if(position < filterList.size()) {
                return filterList.get(position);
            }
        } else {
            if(position < advGPList.size()) {
                return advGPList.get(position);
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
            int listSz = (queryMode) ? filterList.size() : advGPList.size();
            if (listSz < 1) {
                return view;
            }
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            view = inflater.inflate(R.layout.list_rows_group, parent, false);
            ImageView imgIcon = (ImageView) view.findViewById(R.id.imgIcon);
            TextView txtTitle = (TextView) view.findViewById(R.id.txtMessage);

            AdvGP advGP = (queryMode) ? filterList.get(position) : advGPList.get(position);
            txtTitle.setText(advGP.name);
            imgIcon.setImageResource(R.drawable.ic_group);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return view;
        }
    }

    public void remove(AdvGP advGP) {
        remove(advGPList, advGP);
        if(queryMode) {
            remove(filterList, advGP);
        }
    }

    private void remove(List<AdvGP> list, AdvGP advGP) {
        Iterator<AdvGP> it = list.iterator();
        while(it.hasNext()) {
            AdvGP item = it.next();
            if(item.id == advGP.id) {
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
            createFilterList(advGPList);
        }
    }

    private void addFilterList(AdvGP announce) {
        if(announce.name != null && announce.name.toLowerCase().contains(queryStr.toLowerCase())) {
            filterList.add(announce);
        }
    }

    private void createFilterList(List<AdvGP> announceList) {
        filterList.clear();
        for(AdvGP announce : announceList) {
            addFilterList(announce);
        }
    }
}