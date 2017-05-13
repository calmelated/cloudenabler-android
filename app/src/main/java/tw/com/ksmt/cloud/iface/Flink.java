package tw.com.ksmt.cloud.iface;

import com.avos.avoscloud.okhttp.MultipartBuilder;
import com.avos.avoscloud.okhttp.RequestBody;

import org.json.JSONObject;

import java.io.Serializable;

public class Flink implements Serializable, Cloneable {
    public int id;
    public String desc = "";
    public String url = "";

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public Flink(String desc, String url) {
        this.desc = desc;
        this.url = url;
    }

    public Flink(JSONObject jsonObject) {
        try {
            this.id = jsonObject.getInt("id");
            this.desc = jsonObject.getString("desc");
            this.url = jsonObject.getString("url");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RequestBody toMultiPart() {
        MultipartBuilder entityBuilder = new MultipartBuilder();
        entityBuilder.type(MultipartBuilder.FORM);
        entityBuilder.addFormDataPart("desc", this.desc);
        entityBuilder.addFormDataPart("url", this.url);
        return entityBuilder.build();
    }
}
