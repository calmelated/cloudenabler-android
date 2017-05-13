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
import tw.com.ksmt.cloud.iface.Account;
import tw.com.ksmt.cloud.iface.SearchViewAdapter;

public class AccountAdapter extends BaseAdapter implements SearchViewAdapter {
    private Context context;
    private List<Account> accountList = new ArrayList<Account>();
    private List<Account> filterList = new ArrayList<Account>();
    private boolean queryMode = false;
    private String queryStr;

    public AccountAdapter(Context context) {
        this.context = context;
    }

    public List<Account> getList() {
        return (queryMode) ? filterList : accountList;
    }

    public void addList(Account account) {
        accountList.add(account);
        if(queryMode) {
            addFilterList(account);
        }
    }

    public void setList(List<Account> accountList) {
        this.accountList = accountList;
        if(queryMode) {
            createFilterList(accountList);
        }
    }

    public void clearList() {
        this.accountList.clear();
        this.filterList.clear();
    }

    @Override
    public int getCount() {
        return (queryMode) ? filterList.size() : accountList.size();
    }

    @Override
    public Object getItem(int position) {
        if(queryMode) {
            if(position < filterList.size()) {
                return filterList.get(position);
            }
        } else {
            if(position < accountList.size()) {
                return accountList.get(position);
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
            int listSz = (queryMode) ? filterList.size() : accountList.size();
            if (listSz < 1) {
                return view;
            }
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            view = inflater.inflate(R.layout.list_rows_input, parent, false);
            ImageView imgIcon = (ImageView) view.findViewById(R.id.imgIcon);
            TextView txtTitle = (TextView) view.findViewById(R.id.txtMessage);
            TextView txtMessage = (TextView) view.findViewById(R.id.txtInfo);

            Account account = (queryMode) ? filterList.get(position) : accountList.get(position);
            txtTitle.setText(account.name);
            txtMessage.setText(account.account);
            if (account.admin) { //admin
                imgIcon.setImageResource(R.drawable.ic_admin_user);
            } else if(account.trial) {
                imgIcon.setImageResource(R.drawable.ic_trial_user);
            } else { // user
                imgIcon.setImageResource(R.drawable.ic_user);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return view;
        }
    }

    public void remove(String account) {
        remove(accountList, account);
        if(queryMode) {
            remove(filterList, account);
        }
    }

    private void remove(List<Account> list, String account) {
        Iterator<Account> it = list.iterator();
        while(it.hasNext()) {
            Account accItem = it.next();
            if(accItem.account.equals(account)) {
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
            createFilterList(accountList);
        }
    }

    private void addFilterList(Account account) {
        if(account.name != null && account.name.toLowerCase().contains(queryStr.toLowerCase())) {
            filterList.add(account);
        } else if(account.account != null && account.account.toLowerCase().contains(queryStr.toLowerCase())) {
            filterList.add(account);
        }
    }

    private void createFilterList(List<Account> accountList) {
        filterList.clear();
        for(Account account: accountList) {
            addFilterList(account);
        }
    }
}