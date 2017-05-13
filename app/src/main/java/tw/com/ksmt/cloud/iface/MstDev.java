package tw.com.ksmt.cloud.iface;

import android.content.Context;

import com.avos.avoscloud.okhttp.MultipartBuilder;
import com.avos.avoscloud.okhttp.RequestBody;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import tw.com.ksmt.cloud.R;

public class MstDev implements Serializable, Cloneable {
    public int id = -1;
    public String name = "";
    public String type = "";
    public String comPort = "";
    public int slvId = -1;
    public String ip = "";
    public int port = 502;
    public int timeout = 200;
    public int delayPoll = 100;
    public int maxRetry = 10;
    public boolean enable;
    public int status = -1; // -1: unknown, 0: offline, 1: online

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public MstDev(String name, String type, boolean enable) {
        this.name = name;
        this.type = type;
        this.enable = enable;
    }

    public void setSerial(String comPort, int slvId) {
        this.comPort = comPort;
        this.slvId = slvId;
    }

    public void setTCP(String ip, int port, int slvId) {
        this.ip = ip;
        this.port = port;
        this.slvId = slvId;
    }

    public MstDev(int id, JSONObject jsonObject) {
        try {
            this.id = id;
            this.name = jsonObject.has("name") ? jsonObject.getString("name") : "" ;
            this.type = jsonObject.has("type") ? jsonObject.getString("type") : "" ;
            this.comPort = jsonObject.has("comPort") ? jsonObject.getString("comPort") : "" ;
            this.slvId = jsonObject.has("slvId") ? jsonObject.getInt("slvId") : -1 ;
            this.ip = jsonObject.has("ip") ? jsonObject.getString("ip") : "" ;
            this.port = jsonObject.has("port") ? jsonObject.getInt("port") : 502 ;
            this.timeout = jsonObject.has("timeout") ? jsonObject.getInt("timeout") : 200 ;
            this.delayPoll = jsonObject.has("delayPoll") ? jsonObject.getInt("delayPoll") : 100 ;
            this.maxRetry = jsonObject.has("maxRetry") ? jsonObject.getInt("maxRetry") : 10 ;
            this.enable = jsonObject.has("enable") ? ((jsonObject.getInt("enable") == 1) ? true : false) : false;
            this.status = jsonObject.has("status") ? jsonObject.getInt("status") : -1;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RequestBody toMultiPart(Device device) {
        MultipartBuilder entityBuilder = new MultipartBuilder();
        entityBuilder.type(MultipartBuilder.FORM);
        entityBuilder.addFormDataPart("sn", device.sn);
        entityBuilder.addFormDataPart("name", this.name);
        entityBuilder.addFormDataPart("type", this.type);
        entityBuilder.addFormDataPart("comPort", this.comPort);
        entityBuilder.addFormDataPart("slvId", String.valueOf(this.slvId));
        entityBuilder.addFormDataPart("ip", this.ip);
        entityBuilder.addFormDataPart("port", String.valueOf(this.port));
        entityBuilder.addFormDataPart("timeout", String.valueOf(this.timeout));
        entityBuilder.addFormDataPart("delayPoll", String.valueOf(this.delayPoll));
        entityBuilder.addFormDataPart("maxRetry", String.valueOf(this.maxRetry));
        entityBuilder.addFormDataPart("enable", "" + (this.enable ? "1" : "0"));
        return  entityBuilder.build();
    }

    public RequestBody toMultiPart(Device device, MstDev origMstDev) {
        MultipartBuilder entityBuilder = new MultipartBuilder();
        entityBuilder.type(MultipartBuilder.FORM);
        entityBuilder.addFormDataPart("sn", device.sn);
        entityBuilder.addFormDataPart("id", String.valueOf(this.id));
        if(!name.equals(origMstDev.name)) {
            entityBuilder.addFormDataPart("name", this.name);
        }
        if(!type.equals(origMstDev.type)) {
            entityBuilder.addFormDataPart("type", this.type);
        }
        if(!comPort.equals(origMstDev.comPort)) {
            entityBuilder.addFormDataPart("comPort", this.comPort);
        }
        if(slvId != origMstDev.slvId) {
            entityBuilder.addFormDataPart("slvId", String.valueOf(this.slvId));
        }
        if(!ip.equals(origMstDev.ip)) {
            entityBuilder.addFormDataPart("ip", this.ip);
        }
        if(port != origMstDev.port) {
            entityBuilder.addFormDataPart("port", String.valueOf(this.port));
        }
        if(delayPoll != origMstDev.delayPoll) {
            entityBuilder.addFormDataPart("delayPoll", String.valueOf(this.delayPoll));
        }
        if(timeout != origMstDev.timeout) {
            entityBuilder.addFormDataPart("timeout", String.valueOf(this.timeout));
        }
        if(maxRetry != origMstDev.maxRetry) {
            entityBuilder.addFormDataPart("maxRetry", String.valueOf(this.maxRetry));
        }
        if(enable != origMstDev.enable) {
            entityBuilder.addFormDataPart("enable", "" + (this.enable ? "1" : "0"));
        }
        return entityBuilder.build();
    }

    public List<ListItem> toListItems(Context context) {
        List<ListItem> listItems = new ArrayList<ListItem>();
        listItems.add(new ListItem(context.getString(R.string.slave_config)));

        listItems.add(new ListItem(ListItem.VIEW, context.getString(R.string.connect_type), type));
        listItems.add(new ListItem(ListItem.INPUT, context.getString(R.string.slave_dev_name), name));
        if(type.equals("TCP")) {
            listItems.add(new ListItem(ListItem.INPUT, context.getString(R.string.slave_ip_hinet), ip));
            listItems.add(new ListItem(ListItem.NUMBER, context.getString(R.string.slave_port), String.valueOf(port)));
            listItems.add(new ListItem(ListItem.SELECTION, context.getString(R.string.slave_id), String.valueOf(slvId)));
        } else {
            listItems.add(new ListItem(ListItem.SELECTION, context.getString(R.string.serial_port), comPort));
            listItems.add(new ListItem(ListItem.SELECTION, context.getString(R.string.slave_id), String.valueOf(slvId)));
        }
        listItems.add(new ListItem(ListItem.CHECKBOX, context.getString(R.string.enable), (enable ? "1" : "0")));

        listItems.add(new ListItem(context.getString(R.string.other_settings)));
        listItems.add(new ListItem(ListItem.NUMBER, context.getString(R.string.delay_poll), String.valueOf(delayPoll)));
        listItems.add(new ListItem(ListItem.NUMBER, context.getString(R.string.poll_timeout), String.valueOf(timeout)));
        listItems.add(new ListItem(ListItem.NUMBER, context.getString(R.string.max_retry), String.valueOf(maxRetry)));
        return listItems;
    }
}
