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
import java.util.List;

import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.ListItem;
import tw.com.ksmt.cloud.iface.MstDev;

public class EditSlvDevAdapter extends BaseAdapter {
    private Context context;
    private EditSlvDevActivity editSlvDevActivity;
    private List<ListItem> mstDevList = new ArrayList<ListItem>();

    public EditSlvDevAdapter(Context context, EditSlvDevActivity editSlvDevActivity) {
        this.context = context;
        this.editSlvDevActivity = editSlvDevActivity;
    }

    public void addList(MstDev mstDev)  {
        mstDevList.addAll(mstDev.toListItems(context));
    }

    public void clearList() {
        mstDevList.clear();
    }

    @Override
    public int getCount() {
        return mstDevList.size();
    }

    @Override
    public Object getItem(int position) {
        if(position < mstDevList.size()) {
            return mstDevList.get(position);
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
            final CheckBox checkBox;
            final TextView txtTitle;
            final TextView txtMessage;
            final ListItem listItem = mstDevList.get(position);
            if(listItem.type == ListItem.CHECKBOX) {
                view = inflater.inflate(R.layout.list_rows_chbox, parent, false);
                imgIcon = (ImageView) view.findViewById(R.id.imgIcon);
                imgIcon.setImageResource(R.drawable.ic_editor);
                txtTitle = (TextView) view.findViewById(R.id.txtMessage);
                txtTitle.setText(listItem.key);
                checkBox = (CheckBox) view.findViewById(R.id.checkBox);
                checkBox.setChecked((listItem.value.equals("1")) ? true : false);
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (listItem.key.equals(context.getString(R.string.enable))) {
                            editSlvDevActivity.curMstDev.enable = isChecked;
                        }
                        editSlvDevActivity.setSaveTimer();
                    }
                });
            } else if(listItem.type == ListItem.TITLE) {
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
            } else if(listItem.type == ListItem.SELECTION) {
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