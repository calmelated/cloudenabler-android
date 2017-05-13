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
import tw.com.ksmt.cloud.iface.Company;
import tw.com.ksmt.cloud.iface.SearchViewAdapter;

public class SubCompAdapter extends BaseAdapter implements SearchViewAdapter {
    private Context context;
    private List<Company> compList = new ArrayList<Company>();
    private List<Company> filterList = new ArrayList<Company>();
    private boolean queryMode = false;
    private String queryStr;

    public SubCompAdapter(Context context) {
        this.context = context;
    }

    public List<Company> getList() {
        return (queryMode) ? filterList : compList;
    }

    public void addList(Company company) {
        compList.add(company);
        if(queryMode) {
            addFilterList(company);
        }
    }

    public void setList(List<Company> compList) {
        this.compList = compList;
        if(queryMode) {
            createFilterList(compList);
        }
    }

    public void clearList() {
        this.compList.clear();
        this.filterList.clear();
    }

    @Override
    public int getCount() {
        return (queryMode) ? filterList.size() : compList.size();
    }

    @Override
    public Object getItem(int position) {
        if(queryMode) {
            if(position < filterList.size()) {
                return filterList.get(position);
            }
        } else {
            if(position < compList.size()) {
                return compList.get(position);
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
            int listSz = (queryMode) ? filterList.size() : compList.size();
            if (listSz < 1) {
                return view;
            }
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            view = inflater.inflate(R.layout.list_rows_input, parent, false);
            ImageView imgIcon = (ImageView) view.findViewById(R.id.imgIcon);
            TextView txtTitle = (TextView) view.findViewById(R.id.txtMessage);
            TextView txtMessage = (TextView) view.findViewById(R.id.txtInfo);

            Company company = (queryMode) ? filterList.get(position) : compList.get(position);
            txtTitle.setText(company.name);
            txtMessage.setText("ID: " + company.id);
            imgIcon.setImageResource(R.drawable.ic_company);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return view;
        }
    }

    public void remove(Company company) {
        remove(compList, company.id);
        if(queryMode) {
            remove(filterList, company.id);
        }
    }

    private void remove(List<Company> list, String id) {
        Iterator<Company> it = list.iterator();
        while(it.hasNext()) {
            Company company = it.next();
            if(company.id.equals(id)) {
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
            createFilterList(compList);
        }
    }

    private void addFilterList(Company company) {
        if(company.name != null && company.name.toLowerCase().contains(queryStr.toLowerCase())) {
            filterList.add(company);
        }
    }

    private void createFilterList(List<Company> compList) {
        filterList.clear();
        for(Company account: compList) {
            addFilterList(account);
        }
    }
}