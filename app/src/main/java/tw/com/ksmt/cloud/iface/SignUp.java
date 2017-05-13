package tw.com.ksmt.cloud.iface;

import com.avos.avoscloud.okhttp.MultipartBuilder;
import com.avos.avoscloud.okhttp.RequestBody;

import java.io.Serializable;

public class SignUp implements Serializable {
    public String name;
    public String account;
    public String password;
    public String parentId;

    public SignUp(String name, String account, String password) {
        this.name = name;
        this.account = account;
        this.password = password;
    }

    public RequestBody toMultiPart() {
        MultipartBuilder entityBuilder = new MultipartBuilder();
        entityBuilder.type(MultipartBuilder.FORM);
        entityBuilder.addFormDataPart("company", this.name);
        entityBuilder.addFormDataPart("account", this.account);
        entityBuilder.addFormDataPart("password", this.password);
        if(parentId != null) {
            entityBuilder.addFormDataPart("parentId", this.parentId);
        }
        return  entityBuilder.build();
    }
}
