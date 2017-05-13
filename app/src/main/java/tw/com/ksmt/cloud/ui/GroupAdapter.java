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
import tw.com.ksmt.cloud.iface.Group;
import tw.com.ksmt.cloud.iface.SearchViewAdapter;

public class GroupAdapter extends BaseAdapter implements SearchViewAdapter {
    private Context context;
    private List<Group> grouptList = new ArrayList<Group>();
    private List<Group> filterList = new ArrayList<Group>();
    private boolean queryMode = false;
    private String queryStr;

    public GroupAdapter(Context context) {
        this.context = context;
    }

    public List<Group> getList() {
        return (queryMode) ? filterList : grouptList;
    }

    public void addList(Group name) {
        grouptList.add(name);
        if(queryMode) {
            addFilterList(name);
        }
    }

    public void setList(List<Group> groupList) {
        this.grouptList = groupList;
        if(queryMode) {
            createFilterList(groupList);
        }
    }

    public void clearList() {
        grouptList.clear();
        filterList.clear();
    }

    @Override
    public int getCount() {
        return (queryMode) ? filterList.size() : grouptList.size();
    }

    @Override
    public Object getItem(int position) {
        if(queryMode) {
            if(position < filterList.size()) {
                return filterList.get(position);
            }
        } else {
            if(position < grouptList.size()) {
                return grouptList.get(position);
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
            int listSz = (queryMode) ? filterList.size() : grouptList.size();
            if(listSz < 1) {
                return view;
            }
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            view = inflater.inflate(R.layout.list_rows_group, parent, false);
            ImageView imgIcon = (ImageView) view.findViewById(R.id.imgIcon);
            TextView txtTitle = (TextView) view.findViewById(R.id.txtMessage);

            Group group = (queryMode) ? filterList.get(position) : grouptList.get(position);
            txtTitle.setText(group.name);
            imgIcon.setImageResource(R.drawable.ic_group);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return view;
        }
    }

    public void remove(String name) {
        remove(grouptList, name);
        if(queryMode) {
            remove(filterList, name);
        }
    }

    private void remove(List<Group> list, String name) {
        Iterator<Group> it = list.iterator();
        while(it.hasNext()) {
            Group group = it.next();
            if (group != null && group.name.equals(name)) {
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
            createFilterList(grouptList);
        }
    }

    private void addFilterList(Group group) {
        if(group.name != null && group.name.toLowerCase().contains(queryStr.toLowerCase())) {
            filterList.add(group);
        }
    }

    private void createFilterList(List<Group> groupList) {
        filterList.clear();
        for(Group group : groupList) {
            addFilterList(group);
        }
    }
}