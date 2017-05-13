package tw.com.ksmt.cloud.iface;

import com.avos.avoscloud.okhttp.MultipartBuilder;
import com.avos.avoscloud.okhttp.RequestBody;

import java.io.Serializable;

public class GroupMember implements Serializable {
    public String name;
    public Device device;
    public Register register;

    public GroupMember(String name, Device device, Register register)  {
        this.name = name;
        this.device = device;
        this.register = register;
    }

    public RequestBody toMultiPart() {
        MultipartBuilder entityBuilder = new MultipartBuilder();
        entityBuilder.type(MultipartBuilder.FORM);
        entityBuilder.addFormDataPart("name", name);
        entityBuilder.addFormDataPart("sn", device.sn);
        if(device.isMbusMaster() && device.slvIdx > 0) {
            entityBuilder.addFormDataPart("addr", device.slvIdx + register.haddr);
        } else {
            entityBuilder.addFormDataPart("addr", register.haddr);
        }
        return entityBuilder.build();
    }
}
