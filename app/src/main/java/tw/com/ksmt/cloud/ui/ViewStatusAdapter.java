package tw.com.ksmt.cloud.ui;

import android.app.Activity;
import android.content.Context;
import android.text.Html;
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
import tw.com.ksmt.cloud.iface.Register;
import tw.com.ksmt.cloud.iface.SearchViewAdapter;
import tw.com.ksmt.cloud.iface.ViewStatus;
import tw.com.ksmt.cloud.libs.StrUtils;

public class ViewStatusAdapter extends BaseAdapter implements SearchViewAdapter {
    public static final int DEVICE = 0;
    public static final int GROUP  = 1;

    private Context context;
    private List<ViewStatus> vsList = new ArrayList<ViewStatus>();
    private List<ViewStatus> filterList = new ArrayList<ViewStatus>();
    private boolean queryMode = false;
    private String queryStr;
    private int type;

    public ViewStatusAdapter(Context context, int type) {
        this.context = context;
        this.type = type;
    }

    public List<ViewStatus> getList() {
        return (queryMode) ? filterList : vsList;
    }

    public void addItem(ViewStatus vs) {
        addItemIfNoExist(vsList, vs);
        if(queryMode) {
            addFilterList(vs);
        }
    }

    private long getSnAddrIdx(ViewStatus vs) { //String sn, String haddr) {
        try {
            String sortId = vs.sortId;
            if(type == DEVICE) { // Device
                return Integer.parseInt(sortId);
            }
            // Group
            sortId = vs.slvIdx + sortId;
            String addrHex = Integer.toHexString(Integer.valueOf(sortId));
            String[] snArray = vs.sn.split(":");
            String snStr = snArray[3] + snArray[4] + snArray[5] + addrHex;
            return Long.valueOf(snStr, 16);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void addItemIfNoExist(List<ViewStatus> vsList, ViewStatus vs) {
        int position = 0;
        boolean found = false;
        for(int i = 0; i < vsList.size(); i++) {
            ViewStatus _vs = vsList.get(i);
            if(_vs.id.equalsIgnoreCase(vs.id) && vs.haddr.equals(_vs.haddr)) {
                vsList.set(i, vs);
                return;
            }
            if(getSnAddrIdx(vs) > getSnAddrIdx(_vs)) {
                position = i + 1;
            }
        }
        if(!found) {
            vsList.add(position, vs);
        }
    }

    public void clearList() {
        vsList.clear();
        filterList.clear();
    }

    @Override
    public int getCount() {
        return (queryMode) ? filterList.size() : vsList.size();
    }

    @Override
    public Object getItem(int position) {
        if(queryMode) {
            if(position < filterList.size()) {
                return filterList.get(position);
            }
        } else {
            if(position < vsList.size()) {
                return vsList.get(position);
            }
        }
        return null;
    }

    public int getNumUsedReg() {
        int sum = 0;
        for(ViewStatus vs : vsList) {
            if(Register.is64Bits(vs.type)) {
                sum = sum + 4;
            } else if(Register.is48Bits(vs.type)) {
                sum = sum + 3;
            } else if(Register.is32Bits(vs.type)) {
                sum = sum + 2;
            } else {
                sum = sum + 1;
            }
        }
        return sum;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public void setQueryStr(String query) {
        if(query.length() < 1) {
            queryMode = false;
            return;
        }
        synchronized(this) {
            queryMode = true;
            queryStr = query;
            createFilterList(vsList);
        }
    }

    private void addFilterList(ViewStatus vs) {
        String queryStr = this.queryStr.toLowerCase();
        if(vs.desc != null && vs.desc.toLowerCase().contains(queryStr)) {
            addItemIfNoExist(filterList, vs);
        } else if(vs.haddr != null && vs.haddr.toLowerCase().contains(queryStr)) {
            addItemIfNoExist(filterList, vs);
        } else if(vs.laddr != null && vs.laddr.toLowerCase().contains(queryStr)) {
            addItemIfNoExist(filterList, vs);
        } else if(type == GROUP) {
            if (vs.sn != null && vs.sn.toLowerCase().contains(queryStr)) {
                addItemIfNoExist(filterList, vs);
            } else if (vs.devName != null && vs.devName.toLowerCase().contains(queryStr)) {
                addItemIfNoExist(filterList, vs);
            }
        }
    }

    private void createFilterList(List<ViewStatus> vsList) {
        filterList.clear();
        for(ViewStatus vs: vsList) {
            addFilterList(vs);
        }
    }

    public void remove(ViewStatus vs) {
        remove(vsList, vs);
        if(queryMode) {
            remove(filterList, vs);
        }
    }

    private boolean setDisplayType(ViewStatus vs, TextView txtStatVal) {
        boolean showValue = false;
        if(vs.display == 1 && vs.showVal.equals("0")) {
            txtStatVal.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_led_green, 0, 0, 0);
        } else if(vs.display == 2 && vs.showVal.equals("1")) {
            txtStatVal.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_led_red, 0, 0, 0);
        } else if(vs.display == 3 && vs.showVal.equals("2")) {
            txtStatVal.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_led_orange, 0, 0, 0);
        } else if(vs.display == 4 && vs.showVal.equals("3")) {
            txtStatVal.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_led_yellow, 0, 0, 0);
        } else if(vs.display == 5 && vs.showVal.equals("4")) {
            txtStatVal.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_led_blue, 0, 0, 0);
        } else if(vs.display == 6 && vs.showVal.equals("5")) {
            txtStatVal.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_led_white, 0, 0, 0);
        } else if(vs.display == 7 && vs.showVal.equals("6")) {
            txtStatVal.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_led_black, 0, 0, 0);
        } else if(vs.display == 8) {
            if(vs.showVal.equals("0")) {
                txtStatVal.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_led_green, 0, 0, 0);
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_led_red, 0, 0, 0);
            } else {
                showValue = true;
            }
        } else if(vs.display == 9) {
            if(vs.showVal.equals("0")) {
                txtStatVal.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_led_red, 0, 0, 0);
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_led_green, 0, 0, 0);
            } else {
                showValue = true;
            }
        } else if(vs.display == 10) {
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>Local</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>Remote</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 11) {
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>OFF</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>ON</font></small>"));
            } else if(vs.showVal.equals("2")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>TRIP</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 12) {
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.normal) + "</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#f90909'>" + context.getString(R.string.fault) + "</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 13) {
            if (vs.showVal.equals("82")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.resistance) + "</font></small>"));
            } else if (vs.showVal.equals("76")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.inductance) + "</font></small>"));
            } else if (vs.showVal.equals("67")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.capacitance) + "</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 14) {
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.putoff) + "</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.puton) + "</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 15) {
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.puton) + "</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.putoff) + "</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 16) {
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.onsite) + "</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.remote) + "</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 17) {
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.remote) + "</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.onsite) + "</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 18) { //Show Fault/Normal when the value is 0/1
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#f90909'>" + context.getString(R.string.fault) + "</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.normal) + "</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 19) { //Show Normal/Slight Partial/Serious Partial discharge when the value is 0/1/2
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.normal) + "</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#f90909'>" + context.getString(R.string.slight_partial_discharge) + "</font></small>"));
            } else if(vs.showVal.equals("2")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#f90909'>" + context.getString(R.string.serious_partial_discharge) + "</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 20) { //Show ON/OFF when the value is 0/1
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.operate) + "</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.stop) + "</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 21) { //Show OFF/ON when the value is 0/1
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.stop) + "</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.operate) + "</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 22) { //Show Running/Stop when the value is 0/1
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.running) + "</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.off2) + "</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 23) { //Show Stop/Running when the value is 0/1
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.off2) + "</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.running) + "</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 24) { //Show Open/Close when the value is 0/1
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.cutoff) + "</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.puton) + "</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 25) { //Show Close/Open when the value is 0/1
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.puton) + "</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.cutoff) + "</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 26) { //Show Normal/Trip when the value is 0/1
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.normal) + "</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#f90909'>" + context.getString(R.string.trip) + "</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 27) { //Show Trip/Normal when the value is 0/1
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#f90909'>" + context.getString(R.string.trip) + "</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.normal) + "</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 28) { //Show Test/Connect Position when the value is 0/1
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.test_position) + "</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.connect_position) + "</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 29) { //Show Connect/Test Position when the value is 0/1
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.connect_position) + "</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.test_position) + "</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 30) { //Show Manual/Automatic when the value is 0/1
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.manual) + "</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.automatic) + "</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 31) { //Show Automatic/Manual when the value is 0/1
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.automatic) + "</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.manual) + "</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 32) { //Show Normal/Abnormal when the value is 0/1
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.normal) + "</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#f90909'>" + context.getString(R.string.abnormal) + "</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 33) { //Show Abnormal/Normal when the value is 0/1
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#f90909'>" + context.getString(R.string.abnormal) + "</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#808080'>" + context.getString(R.string.normal) + "</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 34) { //Show Green-Off/Red-On when the value is 0/1
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#00FF00'>" + context.getString(R.string.off) + "</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#f90909'>" + context.getString(R.string.on) + "</font></small>"));
            } else {
                showValue = true;
            }
        } else if(vs.display == 35) { //Show Red-OFF/Green-On when the value is 0/1
            if(vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#f90909'>" + context.getString(R.string.off) + "</font></small>"));
            } else if(vs.showVal.equals("1")) {
                txtStatVal.setText(Html.fromHtml("<small><font color='#00FF00'>" + context.getString(R.string.on) + "</font></small>"));
            } else {
                showValue = true;
            }
        } else { // default -> show value
            showValue = true;
        }
        return showValue; // show value or display icon/text
    }

    private void remove(List<ViewStatus> list, ViewStatus vs) {
        Iterator<ViewStatus> it = list.iterator();
        while(it.hasNext()) {
            ViewStatus vss = it.next();
            if(vss.sn.equals(vs.sn) && vss.haddr.equals(vs.haddr)) {
                it.remove();
                return;
            }
        }
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        try {
            int listSz = (queryMode) ? filterList.size() : vsList.size();
            if(listSz < 1) {
                return view;
            }
            ViewStatus vs = (queryMode) ? filterList.get(position) : vsList.get(position);
            String unit = (vs.unit != null && !vs.unit.equals("")) ? vs.unit : "";
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            view = inflater.inflate((type == DEVICE) ? R.layout.view_status_rows : R.layout.group_status_rows, parent, false);
            TextView txtMessage = (TextView) view.findViewById(R.id.txtMessage);
            TextView txtInfo = (TextView) view.findViewById(R.id.txtInfo);
            TextView txtStatVal = (TextView) view.findViewById(R.id.txtStatus);
            ImageView imgEdit = (ImageView) view.findViewById(R.id.imgEdit);

            // show edit button
            if(vs.devStat == 0) { // device is offline
                imgEdit.setImageResource(R.drawable.ic_warning);
            } else if(vs.type < 0 || vs.id == null || vs.id.equals("-1") || vs.id.equals("")) { // unknown register
                imgEdit.setImageResource(R.drawable.ic_error);
            } else if(Register.enRtChart(vs.type)) {
                imgEdit.setImageResource(R.drawable.ic_right_arrow);
                imgEdit.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            } else if(Register.isIOSW(vs.type)) {
                if(vs.swType == Register.TYPE_ERROR) {
                    imgEdit.setImageResource(R.drawable.ic_error);
                } else {
                    imgEdit.setImageResource(R.drawable.ic_iosw);
                }
            } else if (Register.isAppWriteable(vs.type) && vs.userControl) {
                if(vs.type == Register.APP_SWITCH) {
                    if(vs.isBtnOn) {
                        imgEdit.setImageResource(R.drawable.ic_switch_on);
                    } else {
                        imgEdit.setImageResource(R.drawable.ic_switch_off);
                    }
                } else if(vs.type == Register.APP_BTN) {
                    if(vs.isBtnOn) {
                        imgEdit.setImageResource(R.drawable.ic_btn_push);
                    } else {
                        imgEdit.setImageResource(R.drawable.ic_btn_release);
                    }
                } else if(vs.type == Register.APP_BINARY) {
                    imgEdit.setImageResource(R.drawable.ic_binary);
                } else {
                    imgEdit.setImageResource(R.drawable.ic_editor);
                }
            }

            // show status
            if (type == DEVICE) {
                txtMessage.setText(vs.desc);
                String desc = "<font color='#808080'>" + context.getString(R.string.register) + ": ";
                if(vs.slvIdx > 0) {
                    desc += StrUtils.readableMbusAddr(vs.haddr, vs.laddr);
                } else {
                    desc += (vs.laddr == null) ? (vs.haddr + "</font>") : (vs.haddr + "-" + vs.laddr + "</font>");
                }
                if(vs.enlog) {
                    if(vs.devEnlog || vs.devEnServlog) {
                        desc += "<font color='#808080'>, " + context.getString(R.string.logging_on) + "</font>";
                    } else {
                        desc += "<font color='#808080'>, " + context.getString(R.string.logging_paused) + "</font>";
                    }
                }
                txtInfo.setText(Html.fromHtml(desc));
            } else { // Group
                TextView txtTitle = (TextView) view.findViewById(R.id.txtTitle);
                if(vs.slvIdx > 0 && vs.slvNames != null) {
                    txtTitle.setText(vs.devName + " -> " + vs.slvNames);
                } else {
                    txtTitle.setText(vs.devName);
                }
                txtMessage.setText(vs.desc);
                String desc = "<font color='#808080'>";
                desc += vs.sn + ", ";
                if(vs.slvIdx > 0) {
                    desc += StrUtils.readableMbusAddr(vs.haddr, vs.laddr);
                } else {
                    desc += (vs.laddr == null) ? (vs.haddr + "</font>") : (vs.haddr + "-" + vs.laddr + "</font>");
                }
                txtInfo.setText(Html.fromHtml(desc));
            }

            // Show LED if used, show real value otherwise.
            boolean showValue = false;
            if(vs.display > 0) {
                showValue = setDisplayType(vs, txtStatVal);
            } else if((Register.isAlarm(vs.type) || Register.isAlarm(vs.swType)) && !vs.showVal.equals("") && !vs.showVal.equals("0")) {
                txtStatVal.setText(Html.fromHtml("<small><small><font color='#ff0000'> " + vs.showVal + " " + unit + "</font></small></small>"));
                txtStatVal.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_alarm, 0, 0, 0);
            } else {
                showValue = true;
            }

            // Show value if available (Notice: highlight upper/lower bound value)
            String showVal = (vs.showVal == null || vs.showVal.equals("")) ? null : vs.showVal;
            if(showValue && showVal != null) {
                String colorCode = "#808080";
                if(Register.isMbusNumber(vs.type) || (Register.isIOSW(vs.type) && Register.isMbusNumber(vs.swType))) {
                    Double _showVal = Double.parseDouble(showVal);
                    String upVal = (vs.upVal == null || vs.upVal.equals("")) ? null : vs.upVal;
                    if (upVal != null) {
                        Double _upVal = Double.parseDouble(upVal);
                        if(_showVal >= _upVal) {
                            colorCode = "#f90909";
                        }
                    }

                    String lowVal = (vs.lowVal == null || vs.lowVal.equals("")) ? null : vs.lowVal;
                    if(lowVal != null) {
                        Double _lowVal = Double.parseDouble(lowVal);
                        if(_showVal <= _lowVal) {
                            colorCode = "#f90909";
                        }
                    }
                }
                txtStatVal.setText(Html.fromHtml("<font color='" + colorCode + "'>" + showVal + " " + unit + "</font>"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return view;
        }
    }
}