package tw.com.ksmt.cloud.iface;

import org.json.JSONObject;

import java.io.Serializable;

import tw.com.ksmt.cloud.R;

public class Auth implements Serializable {
    public static final int ALARM = R.string.permission_alarm;
    public static final int CONTROL = R.string.permission_control;
    public static final int MONITOR = R.string.permission_monitor;
    public int type;
    public String sn;
    public String name;
    public int deviceId;
    public boolean enable;

    public Auth(int type, JSONObject jsonObject) throws Exception {
        this.type =  type;
        update(jsonObject);
    }

    public void update(JSONObject jsonObject) {
        try {
            this.sn = jsonObject.getString("sn");
            this.name = jsonObject.getString("name");
            this.deviceId = jsonObject.getInt("deviceId");
            if (type == ALARM) {
                this.enable = ((jsonObject.getInt("enAlarm") == 1) ? true : false);
            } else if (type == CONTROL) {
                this.enable = ((jsonObject.getInt("enControl") == 1) ? true : false);
            } else if (type == MONITOR) {
                this.enable = ((jsonObject.getInt("enMonitor") == 1) ? true : false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
