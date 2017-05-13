package tw.com.ksmt.cloud.iface;

import android.content.Context;

import org.json.JSONObject;

import java.io.Serializable;

import tw.com.ksmt.cloud.R;

public class Notification implements Serializable {
    // Alarm status
    public static final int PUSH_OK = 0;
    public static final int PUSH_NO_RECV = 1;
    public static final int PUSH_FAIL = 2;
    public static final int PUSH_PART_FAIL = 3;
    public static final int EMAIL_OK = 4;
    public static final int EMAIL_NO_RECV = 5;
    public static final int EMAIL_FAIL = 6;
    public static final int EMAIL_PART_FAIL = 7;
    public static final int DEV_DISABLED = 8;
    public static final int NO_DEV = 9;
    public static final int DB_FAIL = 10;

    // Message
    public static final int MCODE_DEFAULT = 0;
    public static final int MCODE_OFFLINE = 1;
    public static final int MCODE_RESET_PASSWORD = 2;
    public static final int MCODE_ONLINE = 3;
    public static final int MCODE_LOG_FAILED = 4;
    public static final int MCODE_FEW_ALARM = 5;
    public static final int MCODE_NO_ALARM = 6;
    public static final int MCODE_UPPER_ALARM = 7;
    public static final int MCODE_LOWER_ALARM = 8;
    public static final int MCODE_NEW_ANNOUNCE = 9;
    public static final int MCODE_SLVDEV_OFFLINE = 10;
    public static final int MCODE_SLVDEV_ONLINE = 11;
    public static final int MCODE_BACK_NORMAL_ALARM = 12;
    public static final int MCODE_MBUS_MST_ONLINE = 13;
    public static final int MCODE_MBUS_MST_OFFLINE = 14;
    public static final int MCODE_DEV_REBOOT = 15;

    public int time;
    public int status;
    public String account;
    public String message;
    public int msgCode;
    public int priority;
    public int done;
    public String company;

    // Extra info.
    public String sn;
    public String addr;
    public String refReg;
    public String desc;
    public String value;
    public String unit;
    public String slvName;
    public String almConf;
    public String duration;

    // rr1 ~ rr4
    public String rr1;
    public String rr1_desc;
    public String rr1_value;
    public String rr1_unit;
    public String rr2;
    public String rr2_desc;
    public String rr2_value;
    public String rr2_unit;
    public String rr3;
    public String rr3_desc;
    public String rr3_value;
    public String rr3_unit;
    public String rr4;
    public String rr4_desc;
    public String rr4_value;
    public String rr4_unit;

    public Notification(Context context, JSONObject jsonObject) {
        try {
            this.time    = jsonObject.getInt("time");
            this.status  = jsonObject.getInt("status");
            this.msgCode = jsonObject.getInt("msgCode");
            this.account = jsonObject.getString("account");
            String _message = jsonObject.getString("message");
            this.priority = (jsonObject.isNull("priority")) ? 0 : jsonObject.getInt("priority");
            this.done = (jsonObject.isNull("done")) ? 0 : jsonObject.getInt("done");

            // Get extra info.
            JSONObject extra = jsonObject.has("extra") ? jsonObject.getJSONObject("extra") : null ;
            if(extra != null) {
                sn     = (extra.has("sn")) ? extra.getString("sn") : null;
                addr   = (extra.has("addr")) ? extra.getString("addr").split("-")[0] : null;
                refReg = (extra.has("refReg")) ? extra.getString("refReg").split("-")[0] : null;
                desc   = (extra.has("desc")) ? extra.getString("desc") : null;
                value  = (extra.has("value")) ? extra.getString("value") : null;
                unit   = (extra.has("unit")) ? extra.getString("unit") : null;
                company= (extra.has("company")) ? extra.getString("company") : null;
                slvName= (extra.has("slvName")) ? extra.getString("slvName") : null;
                almConf= (extra.has("conf")) ? extra.getString("conf") : null;
                almConf= (almConf != null && almConf.equals("")) ? null : almConf;
                rr1 = (extra.has("rr1")) ? extra.getString("rr1").split("-")[0] : null;
                rr2 = (extra.has("rr2")) ? extra.getString("rr2").split("-")[0] : null;
                rr3 = (extra.has("rr3")) ? extra.getString("rr3").split("-")[0] : null;
                rr4 = (extra.has("rr4")) ? extra.getString("rr4").split("-")[0] : null;
                rr1_desc = (extra.has("rr1_desc")) ? extra.getString("rr1_desc") : null;
                rr2_desc = (extra.has("rr2_desc")) ? extra.getString("rr2_desc") : null;
                rr3_desc = (extra.has("rr3_desc")) ? extra.getString("rr3_desc") : null;
                rr4_desc = (extra.has("rr4_desc")) ? extra.getString("rr4_desc") : null;
                rr1_value = (extra.has("rr1_value")) ? extra.getString("rr1_value") : null;
                rr2_value = (extra.has("rr2_value")) ? extra.getString("rr2_value") : null;
                rr3_value = (extra.has("rr3_value")) ? extra.getString("rr3_value") : null;
                rr4_value = (extra.has("rr4_value")) ? extra.getString("rr4_value") : null;
                rr1_unit = (extra.has("rr1_unit")) ? extra.getString("rr1_unit") : null;
                rr2_unit = (extra.has("rr2_unit")) ? extra.getString("rr2_unit") : null;
                rr3_unit = (extra.has("rr3_unit")) ? extra.getString("rr3_unit") : null;
                rr4_unit = (extra.has("rr4_unit")) ? extra.getString("rr4_unit") : null;
                duration = (extra.has("duration")) ? extra.getString("duration") : null;
            }

            // Adjust messages from msgCode
            if(msgCode == Notification.MCODE_OFFLINE) {
                message = context.getString(R.string.device) + " " + account + " - " + context.getString(R.string.offline);
            } else if(msgCode == Notification.MCODE_RESET_PASSWORD) {
                message = context.getString(R.string.reset_password);
            } else if(msgCode == Notification.MCODE_LOG_FAILED) {
                message = context.getString(R.string.device) + " " + account + " - " + context.getString(R.string.logging_falied);
            } else if(msgCode == Notification.MCODE_ONLINE) {
                if (_message != null) {
                    String[] durs = _message.split("duration:");
                    if (durs.length > 1) {
                        message = context.getString(R.string.device) + " " + account + " - " + context.getString(R.string.online) + "  (" + context.getString(R.string.offline_time) + ": " + durs[1] + ")";
                    } else {
                        message = context.getString(R.string.device) + " " + account + " - " + context.getString(R.string.online);
                    }
                } else {
                    message = context.getString(R.string.device) + " " + account + " - " + context.getString(R.string.online);
                }
            } else if(msgCode == Notification.MCODE_FEW_ALARM) {
                message = context.getString(R.string.few_alarm_left);
            } else if(msgCode == Notification.MCODE_NO_ALARM) {
                message = context.getString(R.string.no_avaliable_alarm);
            } else if(msgCode == Notification.MCODE_UPPER_ALARM ||
                      msgCode == Notification.MCODE_LOWER_ALARM ||
                      msgCode == Notification.MCODE_BACK_NORMAL_ALARM) {
                if(msgCode == Notification.MCODE_UPPER_ALARM) {
                    message = desc + " " + context.getString(R.string.upper_limit_alarm);
                } else if(msgCode == Notification.MCODE_LOWER_ALARM) {
                    message = desc + " " + context.getString(R.string.lower_limit_alarm);
                } else if(msgCode == Notification.MCODE_BACK_NORMAL_ALARM) {
                    message = desc + " " + context.getString(R.string.evtlog_back_normal);
                }
                if(value != null) {
                    message += " " + context.getString(R.string.value) + ": "  + value + ((unit == null) ? "" : unit);
                }
                if(almConf != null) {
                    message += ((value == null) ? " " : ", ") + context.getString(R.string.settings) + ": "  + almConf;
                }

                boolean first = true;
                if(rr1 != null) {
                    message += ((first) ? " (" : ", ") + rr1_desc + ": " + rr1_value + rr1_unit;
                    first = false;
                }
                if(rr2 != null) {
                    message += ((first) ? " (" : ", ") + rr2_desc + ": " + rr2_value + rr2_unit;
                    first = false;
                }
                if(rr3 != null) {
                    message += ((first) ? " (" : ", ") + rr3_desc + ": " + rr3_value + rr3_unit;
                    first = false;
                }
                if(rr4 != null) {
                    message += ((first) ? " (" : ", ") + rr4_desc + ": " + rr4_value + rr4_unit;
                    first = false;
                }
                if(!first) {
                    message += ")";
                }
            } else if(msgCode == Notification.MCODE_SLVDEV_ONLINE) {
                message = context.getString(R.string.slave_device) + " " + slvName + " - " + context.getString(R.string.online);
            } else if(msgCode == Notification.MCODE_SLVDEV_OFFLINE) {
                message = context.getString(R.string.slave_device) + " " + slvName + " - " + context.getString(R.string.offline);
            } else if(msgCode == Notification.MCODE_NEW_ANNOUNCE) {
                message = context.getString(R.string.received_new_announce);
            } else if(msgCode == Notification.MCODE_MBUS_MST_OFFLINE) {
                message = context.getString(R.string.device) + " " + account + " - " + context.getString(R.string.mbus_mst_offline);
            } else if(msgCode == Notification.MCODE_MBUS_MST_ONLINE) {
                message = context.getString(R.string.device) + " " + account + " - " + context.getString(R.string.mbus_mst_online);
            } else if(msgCode == Notification.MCODE_DEV_REBOOT) {
                if(duration == null || duration.equals("")) {
                    message = context.getString(R.string.device) + " " + account + " - " + context.getString(R.string.rebooted);
                } else {
                    message = context.getString(R.string.device) + " " + account + " - " + context.getString(R.string.reboot) + "  (" + context.getString(R.string.offline_time) + ": " + duration + ")";
                }
            } else {
                message = _message;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
