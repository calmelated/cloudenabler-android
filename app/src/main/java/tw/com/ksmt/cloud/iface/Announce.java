package tw.com.ksmt.cloud.iface;

import com.avos.avoscloud.okhttp.MultipartBuilder;
import com.avos.avoscloud.okhttp.RequestBody;

import org.json.JSONObject;

import java.io.Serializable;

public class Announce implements Serializable, Cloneable {
    public int time;
    public String message = "";
    public String pushType = "";
    public boolean toAllSubsidiary = false;
    public boolean toSubsidiary = false;
    public Company company;

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public Announce(String message) {
        this.message = message;
    }

    public Announce(JSONObject jsonObject) {
        try {
            this.time = jsonObject.getInt("time");
            this.message = jsonObject.getString("message");
            this.message = this.message.replaceAll("<br>", "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // To Current company
    public RequestBody toMultiPart() {
        MultipartBuilder entityBuilder = new MultipartBuilder();
        entityBuilder.type(MultipartBuilder.FORM);
        if(toAllSubsidiary) {
            entityBuilder.addFormDataPart("toAllSubsidiary", "1");
        } else if(toSubsidiary && company != null) {
            entityBuilder.addFormDataPart("toSubsidiary", "1");
            entityBuilder.addFormDataPart("subCompanyId", company.id);
        }
        entityBuilder.addFormDataPart("message", this.message.replaceAll("\n|\r|\r\n", "<br>"));
        entityBuilder.addFormDataPart("pushType", this.pushType);
        return entityBuilder.build();
    }
}
