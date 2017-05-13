package tw.com.ksmt.cloud.ui;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Auth;
import tw.com.ksmt.cloud.iface.SearchViewAdapter;

public class AccountPermissionAdapter extends BaseAdapter implements SearchViewAdapter {
    private Context context;
    private AccountPermissionFragment accfragment;
    private List<Auth> authList = new ArrayList<Auth>();
    private List<Auth> filterList = new ArrayList<Auth>();
    private boolean queryMode = false;
    private String queryStr;

    public AccountPermissionAdapter(Context context, AccountPermissionFragment accfragment) {
        this.context = context;
        this.accfragment = accfragment;
    }

    public void addList(Auth auth)  {
        authList.add(auth);
        if(queryMode) {
            addFilterList(auth);
        }
    }

    public void clearList() {
        authList.clear();
        filterList.clear();
    }

    @Override
    public int getCount() {
        return (queryMode) ? filterList.size() : authList.size();
    }

    @Override
    public Object getItem(int position) {
        if(queryMode) {
            if(position < filterList.size()) {
                return filterList.get(position);
            }
        } else {
            if(position < authList.size()) {
                return authList.get(position);
            }
        }
        return null;
    }

    public List<Auth> getAllItem() {
        List<Auth> authList = new LinkedList<Auth>();
        for(int i = 0; i < getCount(); i++) {
            authList.add((Auth) getItem(i));
        }
        return authList;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        try {
            int listSz = (queryMode) ? filterList.size() : authList.size();
            if(listSz < 1) {
                return view;
            }
            final Auth auth = (queryMode) ? filterList.get(position) : authList.get(position);
            final LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            final ImageView imgIcon;
            final CheckBox checkBox;
            final TextView txtTitle;
            final TextView txtMessage;

            view = inflater.inflate(R.layout.list_rows_chbox_2, parent, false);
            imgIcon = (ImageView) view.findViewById(R.id.imgIcon);
            imgIcon.setImageResource(R.drawable.ic_editor);
            txtTitle = (TextView) view.findViewById(R.id.txtMessage);
            txtTitle.setText(auth.name);
            txtMessage = (TextView) view.findViewById(R.id.txtInfo);
            txtMessage.setText(auth.sn);

            checkBox = (CheckBox) view.findViewById(R.id.checkBox);
            checkBox.setChecked(auth.enable);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    auth.enable = isChecked;
                    accfragment.setSaveTimer(auth);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return view;
        }
    }

    @Override
    public void setQueryStr(String query) {
        if(query.length() < 1) {
            queryMode = false;
            return;
        }
        synchronized(this) {
            queryMode = true;
            queryStr = query;
            createFilterList(authList);
        }
    }

    private void addFilterList(Auth auth) {
        if(auth.name != null && auth.name.toLowerCase().contains(queryStr.toLowerCase())) {
            filterList.add(auth);
        } else if(auth.sn!= null && auth.sn.toLowerCase().contains(queryStr.toLowerCase())) {
            filterList.add(auth);
        }
    }

    private void createFilterList(List<Auth> authList) {
        filterList.clear();
        for(Auth auth: authList) {
            addFilterList(auth);
        }
    }
}