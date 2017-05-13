package tw.com.ksmt.cloud.iface;

import android.content.Context;

import com.avos.avoscloud.okhttp.MultipartBuilder;
import com.avos.avoscloud.okhttp.RequestBody;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import tw.com.ksmt.cloud.R;

public class AdvGP implements Serializable {
    public int id;
    public int parentId;
    public String name;
    public String sname;
    public Device device;
    public Register register;
    public boolean gnext;

    // new 2-level group
    public AdvGP(String name, String sname)  {
        this.name = name;
        this.sname = sname;
        this.id = 0;
        this.parentId = 0;
        this.gnext = false;
    }

    // new group + group member
    public AdvGP(String name, Device device, Register register)  {
        this.name = name;
        this.sname = null;
        this.device = device;
        this.register = register;
    }

    public AdvGP(JSONObject jObject) {
        try {
            this.id = jObject.has("id") ? jObject.getInt("id") : -1;
            this.parentId = jObject.has("parentId") ? jObject.getInt("parentId") : -1;
            this.name = jObject.has("name") ? jObject.getString("name") : null;
            this.gnext = jObject.has("gnext") ? jObject.getBoolean("gnext") : false ;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RequestBody toNameMultiPart() {
        MultipartBuilder entityBuilder = new MultipartBuilder();
        entityBuilder.type(MultipartBuilder.FORM);
        entityBuilder.addFormDataPart("name", name);
        return entityBuilder.build();
    }

    public RequestBody toMultiPart() {
        MultipartBuilder entityBuilder = new MultipartBuilder();
        entityBuilder.type(MultipartBuilder.FORM);

        if(id > 0) {
            entityBuilder.addFormDataPart("id", String.valueOf(id));
        } else {
            entityBuilder.addFormDataPart("name", name);
        }

        if(sname != null) {
            entityBuilder.addFormDataPart("sname", sname);
        }

        if(parentId > 0) { // new sub group
            entityBuilder.addFormDataPart("parentId", String.valueOf(parentId));
        }

        if(device != null && register != null) {
            if (device.isMbusMaster() && device.slvIdx > 0) {
                entityBuilder.addFormDataPart("addr", device.slvIdx + register.haddr);
            } else {
                entityBuilder.addFormDataPart("addr", register.haddr);
            }
            entityBuilder.addFormDataPart("sn", device.sn);
        }
        return entityBuilder.build();
    }

    public List<ListItem> toListItems(Context context) {
        List<ListItem> listItems = new ArrayList<ListItem>();
        //listItems.add(new ListItem(context.getString(R.string.group)));
        listItems.add(new ListItem(ListItem.INPUT, context.getString(R.string.group_name), name));
        return listItems;
    }

    public String toString() {
        return this.name;
    }
}
