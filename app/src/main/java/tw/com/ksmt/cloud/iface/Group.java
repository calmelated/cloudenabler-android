package tw.com.ksmt.cloud.iface;

import android.content.Context;

import com.avos.avoscloud.okhttp.MultipartBuilder;
import com.avos.avoscloud.okhttp.RequestBody;

import org.json.JSONArray;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import tw.com.ksmt.cloud.R;

public class Group implements Serializable {
    public String name;
    public String origName;
    public JSONArray member;

    // Group List
    public Group(String name) {
        this.name = name;
    }

    public RequestBody toMultiPart() {
        MultipartBuilder entityBuilder = new MultipartBuilder();
        entityBuilder.type(MultipartBuilder.FORM);
        entityBuilder.addFormDataPart("name", name);
        entityBuilder.addFormDataPart("origName", (origName == null) ? name : origName);
        entityBuilder.addFormDataPart("member", (member != null) ? member.toString() : "");
        return entityBuilder.build();
    }

    public List<ListItem> toListItems(Context context) {
        List<ListItem> listItems = new ArrayList<ListItem>();
        //listItems.add(new ListItem(context.getString(R.string.group)));
        listItems.add(new ListItem(ListItem.INPUT, context.getString(R.string.group_name), name));
        return listItems;
    }
}
