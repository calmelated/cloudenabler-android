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
import tw.com.ksmt.cloud.iface.Device;
import tw.com.ksmt.cloud.iface.ListItem;

public class EditDeviceAdapter extends BaseAdapter {
    private Context context;
    private EditDeviceActivity editDeviceActivity;
    private List<ListItem> devList = new ArrayList<ListItem>();

    public EditDeviceAdapter(Context context, EditDeviceActivity editDeviceActivity) {
        this.context = context;
        this.editDeviceActivity = editDeviceActivity;
    }

    public void addList(Device device)  {
        devList.addAll(device.toListItems(context));
    }

    public void clearList() {
        devList.clear();
    }

    @Override
    public int getCount() {
        return devList.size();
    }

    @Override
    public Object getItem(int position) {
        if(position < devList.size()) {
            return devList.get(position);
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
            final TextView txtInfo;
            final ListItem listItem = devList.get(position);
            if(listItem.type == ListItem.CHECKBOX) {
                boolean isCloudLogging = listItem.key.equals(context.getString(R.string.enable_server_loggoing));
                int layout = isCloudLogging ? R.layout.list_rows_chbox_2 : R.layout.list_rows_chbox ;
                view = inflater.inflate(layout, parent, false);
                imgIcon = (ImageView) view.findViewById(R.id.imgIcon);
                imgIcon.setImageResource(R.drawable.ic_editor);
                txtTitle = (TextView) view.findViewById(R.id.txtMessage);
                txtTitle.setText(listItem.key);

                if(isCloudLogging) {
                    txtInfo = (TextView) view.findViewById(R.id.txtInfo);
                    txtInfo.setVisibility(View.VISIBLE);
                    txtInfo.setText(context.getString(R.string.enable_server_loggoing_hint));
                }

                checkBox = (CheckBox) view.findViewById(R.id.checkBox);
                checkBox.setChecked((listItem.value.equals("1")) ? true : false);
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (listItem.key.equals(context.getString(R.string.enable_usb_loggoing))) {
                            editDeviceActivity.curDevice.enlog = isChecked;
                        } else if (listItem.key.equals(context.getString(R.string.enable_server_loggoing))) {
                            editDeviceActivity.curDevice.enServLog = isChecked;
                        } else if (listItem.key.equals(context.getString(R.string.ftp_cli_enable))) {
                            editDeviceActivity.curDevice.enFtpCli = isChecked;
                        }
                        editDeviceActivity.setSaveTimer();
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
            } else if(listItem.type == ListItem.NEW_PASSWORD || listItem.type == ListItem.PASSWORD) {
                view = inflater.inflate(R.layout.list_rows_pass, parent, false);
                imgIcon = (ImageView) view.findViewById(R.id.imgIcon);
                imgIcon.setImageResource(R.drawable.ic_editor);
                txtTitle = (TextView) view.findViewById(R.id.txtMessage);
                txtTitle.setText(listItem.key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return view;
        }
    }
}