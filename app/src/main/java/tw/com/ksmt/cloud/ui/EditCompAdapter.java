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
import tw.com.ksmt.cloud.iface.Company;
import tw.com.ksmt.cloud.iface.ListItem;

public class EditCompAdapter extends BaseAdapter {
    private Context context;
    private EditCompActivity editCompActivity;
    private List<ListItem> compList = new ArrayList<ListItem>();

    public EditCompAdapter(Context context, EditCompActivity editCompActivity) {
        this.context = context;
        this.editCompActivity = editCompActivity;
    }

    public void addList(Company company)  {
        compList.addAll(company.toListItems(context));
    }

    public void clearList() {
        compList.clear();
    }

    @Override
    public int getCount() {
        return compList.size();
    }

    @Override
    public Object getItem(int position) {
        if(position < compList.size()) {
            return compList.get(position);
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
            final LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            final ImageView imgIcon;
            final TextView txtTitle;
            final TextView txtMessage;
            final ListItem listItem = compList.get(position);
            if(listItem.type == ListItem.TITLE) {
                view = inflater.inflate(R.layout.list_title, parent, false);
                txtMessage = (TextView) view.findViewById(R.id.txtInfo);
                txtMessage.setText(listItem.key);
            } else if(listItem.type == ListItem.VIEW) {
                view = inflater.inflate(R.layout.list_rows, parent, false);
                imgIcon = (ImageView) view.findViewById(R.id.imgIcon);
                imgIcon.setImageResource(R.drawable.ic_view_data);
                txtTitle = (TextView) view.findViewById(R.id.txtMessage);
                txtTitle.setText(listItem.key);
                txtMessage = (TextView) view.findViewById(R.id.txtInfo);
                txtMessage.setText(listItem.value);
            } else if(listItem.type == ListItem.INPUT || listItem.type == ListItem.NUMBER || listItem.type == ListItem.LONG_INPUT ) {
                view = inflater.inflate(R.layout.list_rows_input, parent, false);
                imgIcon = (ImageView) view.findViewById(R.id.imgIcon);
                imgIcon.setImageResource(R.drawable.ic_editor);
                txtTitle = (TextView) view.findViewById(R.id.txtMessage);
                txtTitle.setText(listItem.key);
                txtMessage = (TextView) view.findViewById(R.id.txtInfo);
                txtMessage.setText(listItem.value);
            } else if(listItem.type == ListItem.BUTTON) {
                view = inflater.inflate(R.layout.list_rows, parent, false);
                imgIcon = (ImageView) view.findViewById(R.id.imgIcon);
                imgIcon.setImageResource(R.drawable.ic_editor);
                txtTitle = (TextView) view.findViewById(R.id.txtMessage);
                txtTitle.setText(listItem.key);
                txtMessage = (TextView) view.findViewById(R.id.txtInfo);
                txtMessage.setText(listItem.value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return view;
        }
    }
}