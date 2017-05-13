package tw.com.ksmt.cloud.iface;

import android.content.Context;

import org.json.JSONObject;

import java.io.Serializable;

import tw.com.ksmt.cloud.R;

public class EventLog implements Serializable {
    public final static int TYPE_OK = 0;
    public final static int TYPE_WARNING = 1;
    public final static int TYPE_ERROR = 2;

    public int time;
    public String evtId;
    public String message;
    public int type = TYPE_OK;

    public EventLog(Context context, JSONObject jsonObject) {
        try {
            this.time = jsonObject.getInt("time");
            this.evtId = jsonObject.getString("type");
            if(!jsonObject.has("extraMsg") || jsonObject.getString("extraMsg").equals("")) {
                this.message = getMessage(context, this.evtId, null);
            } else {
                this.message = getMessage(context, this.evtId, jsonObject.getJSONObject("extraMsg"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private String getMessage(Context context, String evtId, JSONObject extra) throws Exception {
        if     (evtId.equals("00")) { return context.getString(R.string.evtlog_00); }
        else if(evtId.equals("01")) { this.type = TYPE_ERROR; return context.getString(R.string.evtlog_01); }
        else if(evtId.equals("02")) { return context.getString(R.string.evtlog_02); }
        else if(evtId.equals("03")) { this.type = TYPE_ERROR; return context.getString(R.string.evtlog_03); }
        else if(evtId.equals("04")) { return context.getString(R.string.evtlog_04); }
        else if(evtId.equals("05")) { return context.getString(R.string.evtlog_05); }
        else if(evtId.equals("06")) { return context.getString(R.string.evtlog_06); }
        else if(evtId.equals("07")) { return context.getString(R.string.evtlog_07); }
        else if(evtId.equals("08")) { return context.getString(R.string.evtlog_08); }
        else if(evtId.equals("09")) { return context.getString(R.string.evtlog_09); }
        else if(evtId.equals("0A")) { return context.getString(R.string.evtlog_0A); }
        else if(evtId.equals("0B")) { return context.getString(R.string.evtlog_0B); }
        else if(evtId.equals("0C")) { return context.getString(R.string.evtlog_0C); }
        else if(evtId.equals("0D")) { return context.getString(R.string.evtlog_0D); }
        else if(evtId.equals("0E")) { return context.getString(R.string.evtlog_0E); }
        else if(evtId.equals("0F")) { return context.getString(R.string.evtlog_0F); }
        else if(evtId.equals("10")) { return context.getString(R.string.evtlog_10); }
        else if(evtId.equals("11")) { return context.getString(R.string.evtlog_11); }
        else if(evtId.equals("12")) { return context.getString(R.string.evtlog_12); }
        else if(evtId.equals("13")) { return context.getString(R.string.evtlog_13); }
        else if(evtId.equals("14")) { return context.getString(R.string.evtlog_14); }
        else if(evtId.equals("15")) { return context.getString(R.string.evtlog_15); }
        else if(evtId.equals("16")) { return context.getString(R.string.evtlog_16); }
        else if(evtId.equals("17")) { return context.getString(R.string.evtlog_17); }
        else if(evtId.equals("18")) { return context.getString(R.string.evtlog_18); }
        else if(evtId.equals("19")) { return context.getString(R.string.evtlog_19); }
        else if(evtId.equals("1A")) { return context.getString(R.string.evtlog_1A); }
        else if(evtId.equals("1B")) { return context.getString(R.string.evtlog_1B); }
        else if(evtId.equals("1C")) { return context.getString(R.string.evtlog_1C); }
        else if(evtId.equals("1D")) { return context.getString(R.string.evtlog_1D); }
        else if(evtId.equals("1E")) { return context.getString(R.string.evtlog_1E); }
        else if(evtId.equals("1F")) { return context.getString(R.string.evtlog_1F); }
        else if(evtId.equals("20")) { return context.getString(R.string.evtlog_20); }
        else if(evtId.equals("21")) { this.type = TYPE_WARNING; return context.getString(R.string.evtlog_21); }
        else if(evtId.equals("22")) { return context.getString(R.string.evtlog_22); }
        else if(evtId.equals("23")) { return context.getString(R.string.evtlog_23); }
        else if(evtId.equals("24")) { return context.getString(R.string.evtlog_24); }
        else if(evtId.equals("25")) { this.type = TYPE_WARNING;return context.getString(R.string.evtlog_25); }
        else if(evtId.equals("26")) { return context.getString(R.string.evtlog_26); }
        else if(evtId.equals("27")) { this.type = TYPE_ERROR; return context.getString(R.string.evtlog_27); }
        else if(evtId.equals("28")) { return context.getString(R.string.evtlog_28); }
        else if(evtId.equals("29")) { this.type = TYPE_WARNING;return context.getString(R.string.evtlog_29); }
        else if(evtId.equals("2A")) { this.type = TYPE_WARNING;return context.getString(R.string.evtlog_2A); }
        else if(evtId.equals("2B")) { this.type = TYPE_WARNING;return context.getString(R.string.evtlog_2B); }
        else if(evtId.equals("2C")) { this.type = TYPE_WARNING;return context.getString(R.string.evtlog_2C); }
        else if(evtId.equals("2D")) { this.type = TYPE_WARNING;return context.getString(R.string.evtlog_2D); }
        else if(evtId.equals("2E")) { return context.getString(R.string.evtlog_2E); }
        else if(evtId.equals("2F")) { this.type = TYPE_WARNING;return context.getString(R.string.evtlog_2F); }

        else if(evtId.equals("30")) { this.type = TYPE_WARNING;return context.getString(R.string.evtlog_30); }
        else if(evtId.equals("31")) { return context.getString(R.string.evtlog_31); }
        else if(evtId.equals("32")) { return context.getString(R.string.evtlog_32); }
        else if(evtId.equals("33")) { this.type = TYPE_WARNING;return context.getString(R.string.evtlog_33); }
        else if(evtId.equals("34")) { this.type = TYPE_WARNING;return context.getString(R.string.evtlog_34); }
        else if(evtId.equals("35")) { return context.getString(R.string.evtlog_35); }
        else if(evtId.equals("36")) { this.type = TYPE_ERROR; return context.getString(R.string.evtlog_36); }
        else if(evtId.equals("37")) { return context.getString(R.string.evtlog_37); }
        else if(evtId.equals("38")) { return context.getString(R.string.evtlog_38); }
        else if(evtId.equals("39")) { this.type = TYPE_ERROR; return context.getString(R.string.evtlog_39); }
        else if(evtId.equals("3A")) { return context.getString(R.string.evtlog_3A); }
        else if(evtId.equals("3B")) { this.type = TYPE_ERROR; return context.getString(R.string.evtlog_3B); }
        else if(evtId.equals("3C")) {
            String retStr = context.getString(R.string.evtlog_3C);
            return (extra == null) ? retStr : retStr + " " + context.getString(R.string.success) + ": " + extra.getLong("successCnt") + ", " + context.getString(R.string.failed) + ": " + extra.getLong("failCnt");
        } else if(evtId.equals("3D")) {
            String retStr = context.getString(R.string.evtlog_3D);
            return (extra == null) ? retStr : retStr + " " + context.getString(R.string.success) + ": " + extra.getLong("successCnt") + ", " + context.getString(R.string.failed) + ": " + extra.getLong("failCnt");
        } else if(evtId.equals("3E")) {
            String retStr = context.getString(R.string.evtlog_3E);
            return (extra == null) ? retStr : retStr + " " + context.getString(R.string.success) + ": " + extra.getLong("successCnt") + ", " + context.getString(R.string.failed) + ": " + extra.getLong("failCnt");
        }
        else if(evtId.equals("3F")) { this.type = TYPE_WARNING;return context.getString(R.string.evtlog_3F); }

        else if(evtId.equals("40")) { return context.getString(R.string.evtlog_40); }
        else if(evtId.equals("41")) { this.type = TYPE_ERROR; return context.getString(R.string.evtlog_41); }
        else if(evtId.equals("42")) { return context.getString(R.string.evtlog_42); }
        else if(evtId.equals("43")) {
            String retStr = context.getString(R.string.evtlog_43);
            if(extra == null) {
                return retStr;
            } else if(extra.getBoolean("normal")) {
                return retStr + context.getString(R.string.evtlog_back_normal);
            } else {
                this.type = TYPE_ERROR;
                if(extra.getBoolean("writeErr")) {
                    retStr += (" " + context.getString(R.string.evtlog_write_error));
                }
                if(extra.getBoolean("profErr")) {
                    retStr += (" " + context.getString(R.string.evtlog_profile_error));
                }
                if(extra.getBoolean("noUsbErr")) {
                    retStr += (" " + context.getString(R.string.evtlog_nousb_error));
                }
                return retStr;
            }
        } else if(evtId.equals("44") || evtId.equals("45")) {
            String retStr = context.getString(R.string.evtlog_44);
            boolean success = (extra.isNull("success")) ? false : extra.getBoolean("success");
            String logName = (extra.isNull("log")) ? "" :  extra.getString("log");
            if(success) {
                this.type = TYPE_OK;
                return retStr + " " + logName + " - " + context.getString(R.string.success);
            } else {
                this.type = TYPE_ERROR;
                return retStr + " " + logName + " - " + context.getString(R.string.failed);
            }
        } else if(evtId.equals("47")) {
            boolean offline = (extra.isNull("offline")) ? false : extra.getBoolean("offline");
            if(offline) {
                this.type = TYPE_ERROR;
                return context.getString(R.string.mbus_mst_offline);
            } else {
                this.type = TYPE_OK;
                return context.getString(R.string.mbus_mst_online);
            }
        } else {
            return context.getString(R.string.evtlog_unknown_type) + " 0x" + evtId;
        }
    }
}
