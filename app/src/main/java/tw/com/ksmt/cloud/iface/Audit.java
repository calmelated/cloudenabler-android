package tw.com.ksmt.cloud.iface;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;

import tw.com.ksmt.cloud.R;

public class Audit implements Serializable {
    public static final int USER_LOGIN = 0;
    public static final int USER_LOGOUT = 1;
    public static final int NEW_USER = 2;
    public static final int EDIT_USER = 3;
    public static final int DELETE_USER = 4;
    public static final int USER_ACTIVATE = 5;
    public static final int CHG_LANG = 6;

    public static final int NEW_ANNOUNCE = 7;
    public static final int EDIT_ANNOUNCE = 8;
    public static final int DELETE_ANNOUNCE = 9;

    public static final int NEW_DEV = 10;
    public static final int EDIT_DEV = 11;
    public static final int DELETE_DEV = 12;
    public static final int DEV_IMPORT = 13;
    public static final int SND_FTP_LOG = 14;

    public static final int NEW_FLINK = 15;
    public static final int EDIT_FLINK = 16;
    public static final int DELETE_FLINK = 17;
    public static final int SND_FWUPG = 18;
    public static final int DEV_REBOOT = 19;

    public static final int NEW_REG = 20;
    public static final int DUP_REG = 21;
    public static final int EDIT_REG = 22;
    public static final int SET_REG = 23;
    public static final int DELETE_REG = 24;

    public static final int NEW_GROUP = 30;
    public static final int EDIT_GROUP = 31;
    public static final int DELETE_GROUP = 32;

    public static final int CLEAR_EVTLOG = 40;
    public static final int CLEAR_ALARM = 41;
    public static final int CLEAR_AUDIT = 42;
    public static final int CLEAR_IOSTLOG = 43;

    public static final int ANNOUNCE_SUB_CMP = 44;
    public static final int ANNOUNCE_ALL_CMP = 45;

    public static final int SWITCH_MAC = 46;
    public static final int SWITCH_BACK = 47;

    public int time;
    public String account;
    public int msgCode;
    public String message = "";

    public Audit(Context context, JSONObject jsonObject) {
        try {
            this.time = jsonObject.getInt("time");
            this.msgCode = jsonObject.getInt("msgCode");
            this.account = jsonObject.getString("account");

            JSONObject msgObj = (jsonObject.isNull("message")) ? null : jsonObject.getJSONObject("message");
            if(msgCode == USER_LOGIN) {
                this.message = context.getString(R.string.login) + " (" + context.getString(R.string.user) + ": " + this.account + ")";
            } else if(msgCode == USER_LOGOUT) {
                this.message = context.getString(R.string.logout) + " (" + context.getString(R.string.user) + ": " + this.account + ")";
            } else if(msgCode == NEW_USER) {
                this.message = context.getString(R.string.new_account) + " - " + msgObj.getString("userName");
            } else if(msgCode == EDIT_USER) {
                this.message = context.getString(R.string.edit_account) + " - " + msgObj.getString("userName");
            } else if(msgCode == DELETE_USER) {
                this.message = context.getString(R.string.remove) + context.getString(R.string.account) + " - " + msgObj.getString("userName");
            } else if(msgCode == USER_ACTIVATE) {
                this.message = context.getString(R.string.account_activate) + " - " + this.account;
            } else if(msgCode == CHG_LANG) {
                this.message = context.getString(R.string.change_lang) + " - " + msgObj.getString("lang");
            } else if(msgCode == NEW_DEV) {
                this.message = context.getString(R.string.new_device) + " - " + msgObj.getString("devName");
            } else if(msgCode == EDIT_DEV && msgObj.has("slvDevName")) {
                String slvDev = msgObj.getString("slvDevName");
                String extra = "";
                if(msgObj.has("origName")) {
                    extra += " (" + context.getString(R.string.new_name) + ": " + slvDev + ")";
                } else if(msgObj.has("ip")) {
                    extra += " (" + context.getString(R.string.slave_ip) + ": " + msgObj.getString("ip") + ")";
                } else if(msgObj.has("port")) {
                    extra += " (" + context.getString(R.string.slave_port) + ": " + msgObj.getString("port") + ")";
                } else if(msgObj.has("slvId")) {
                    extra += " (" + context.getString(R.string.slave_id) + ": " + msgObj.getString("slvId") + ")";
                } else if(msgObj.has("comPort")) {
                    extra += " (" + context.getString(R.string.serial_port) + ": " + msgObj.getString("comPort") + ")";
                } else if(msgObj.has("type")) {
                    extra += " (" + context.getString(R.string.type) + ": " + msgObj.getString("type") + ")";
                } else if(msgObj.has("timeout")) {
                    extra += " (" + context.getString(R.string.poll_timeout) + ": " + msgObj.getString("timeout") + ")";
                } else if(msgObj.has("delayPoll")) {
                    extra += " (" + context.getString(R.string.delay_poll) + ": " + msgObj.getString("delayPoll") + ")";
                } else if(msgObj.has("maxRetry")) {
                    extra += " (" + context.getString(R.string.max_retry) + ": " + msgObj.getString("maxRetry") + ")";
                }
                if(extra.equals("")) {
                    this.message = context.getString(R.string.new_slave_device) + " - " + slvDev;
                } else {
                    this.message = context.getString(R.string.edit_slave_device) + " - " + slvDev + extra;
                }
            } else if(msgCode == EDIT_DEV && !msgObj.has("slvDevName")) {
                String extra = "";
                if(!msgObj.isNull("newDevName")) {
                    extra += " (" + context.getString(R.string.device) + ": " + msgObj.getString("newDevName") + ")";
                } else if(!msgObj.isNull("enLog")) {
                    if(msgObj.getString("enLog").equals("1")) {
                        extra += " (" + context.getString(R.string.enable_usb_loggoing) + ": " + context.getString(R.string.enable) + ")";
                    } else {
                        extra += " (" + context.getString(R.string.enable_usb_loggoing) + ": " + context.getString(R.string.disable) + ")";
                    }
                } else if(!msgObj.isNull("logFreq")) {
                    extra += " (" + context.getString(R.string.logging_freq) + ": " + msgObj.getString("logFreq") + ")";
                } else if(!msgObj.isNull("storCapacity")) {
                    extra += " (" + context.getString(R.string.storage_capacity) + ": " + msgObj.getString("storCapacity") + ")";
                } else if(!msgObj.isNull("enFtpCli")) {
                    if(msgObj.getString("enFtpCli").equals("1")) {
                        extra += " (" + context.getString(R.string.ftp_cli_enable) + ": " + context.getString(R.string.enable) + ")";
                    } else {
                        extra += " (" + context.getString(R.string.ftp_cli_enable) + ": " + context.getString(R.string.disable) + ")";
                    }
                } else if(!msgObj.isNull("ftpPswd")) {
                    extra += " (" + context.getString(R.string.ftp_cli_password) + ": " + msgObj.getString("ftpPswd") + ")";
                } else if(!msgObj.isNull("ftpCliHost")) {
                    extra += " (" + context.getString(R.string.ftp_cli_host) + ": " + msgObj.getString("ftpCliHost") + ")";
                } else if(!msgObj.isNull("ftpCliPort")) {
                    extra += " (" + context.getString(R.string.ftp_cli_port) + ": " + msgObj.getString("ftpCliPort") + ")";
                } else if(!msgObj.isNull("ftpCliAccount")) {
                    extra += " (" + context.getString(R.string.ftp_cli_account) + ": " + msgObj.getString("ftpCliAccount") + ")";
                } else if(!msgObj.isNull("ftpCliPswd")) {
                    extra += " (" + context.getString(R.string.ftp_cli_password) + ": " + msgObj.getString("ftpCliPswd") + ")";
                } else if(!msgObj.isNull("mbusTimeout")) {
                    extra += " (" + context.getString(R.string.mbus_timeout) + ": " + msgObj.getString("mbusTimeout") + ")";
                }
                this.message = context.getString(R.string.edit_device) + " - " + msgObj.getString("devName") + extra;
            } else if(msgCode == DELETE_DEV) {
                this.message = context.getString(R.string.remove) + context.getString(R.string.device) + " - " + msgObj.getString("devName");
            } else if(msgCode == DEV_IMPORT) {
                String devId = "";
                if(msgObj.has("slvIdx")) {
                    devId = " (" + context.getString(R.string.device_id) + ": " + msgObj.getInt("slvIdx") + ")";
                }
                this.message = context.getString(R.string.import_profile) + devId + " (" + context.getString(R.string.device) + ": " + msgObj.getString("devName") + ")";
            } else if(msgCode == SND_FTP_LOG) {
                this.message = context.getString(R.string.ftp_cli_send_log) + " (" + context.getString(R.string.device) + ": " + msgObj.getString("devName") + ")";
            } else if(msgCode == SND_FWUPG) {
                String _msg = context.getString(R.string.device_fw_upgrade) + " (" + context.getString(R.string.device) + ": " + msgObj.getString("devName");
                _msg = msgObj.has("fwVer") ? _msg + ", " + context.getString(R.string.device_version) + ": " + msgObj.getString("fwVer") : _msg ;
                this.message = _msg + ")";
            } else if(msgCode == DEV_REBOOT) {
                this.message = context.getString(R.string.reboot) + " (" + context.getString(R.string.device) + ": " + msgObj.getString("devName") + ")";
            } else if(msgCode == NEW_REG) {
                this.message = context.getString(R.string.new_register) + " - " + msgObj.getString("regName") + " (" + context.getString(R.string.device) + ": " + msgObj.getString("devName") + ")";
            } else if(msgCode == DUP_REG) {
                this.message = context.getString(R.string.copy) + context.getString(R.string.register) + " - " + msgObj.getString("regName") + " (" + context.getString(R.string.device) + ": " + msgObj.getString("devName") + ")";
            } else if(msgCode == EDIT_REG) {
                this.message = context.getString(R.string.edit_register) + ": " + msgObj.getString("regName") + " (" + context.getString(R.string.device) + ": " + msgObj.getString("devName") + ")";
            } else if(msgCode == SET_REG) {
                String hval = (msgObj.isNull("hval")) ? "" : msgObj.getString("hval");
                String ival = (msgObj.isNull("ival")) ? "" : msgObj.getString("ival");
                String jval = (msgObj.isNull("jval")) ? "" : msgObj.getString("jval");
                String lval = (msgObj.isNull("lval")) ? "" : msgObj.getString("lval");
                int fpt  = (msgObj.isNull("fpt"))  ? -1 : msgObj.getInt("fpt");
                int type = (msgObj.isNull("type")) ? -1 : msgObj.getInt("type");
                if(type < 0) { // unknown type
                    this.message = context.getString(R.string.set_reg_val) + " - " + msgObj.getString("regName") + " (" + context.getString(R.string.device) + ": " + msgObj.getString("devName") + ", " + context.getString(R.string.value) + ": 0x" + hval + ival + jval + lval + ") ";
                } else {
                    ViewStatus vs = new ViewStatus();
                    vs.type = type;
                    vs.fpt = fpt;
                    vs.haddr = (msgObj.isNull("haddr") ? null : msgObj.getString("haddr")) ; vs.hval = hval;
                    vs.iaddr = (msgObj.isNull("iaddr") ? null : msgObj.getString("iaddr")) ; vs.ival = ival;
                    vs.jaddr = (msgObj.isNull("jaddr") ? null : msgObj.getString("jaddr")) ; vs.jval = jval;
                    vs.laddr = (msgObj.isNull("laddr") ? null : msgObj.getString("laddr")) ; vs.lval = lval;
                    vs.showVal = Register.isIOSW(vs.type) ? vs.setShowVal(vs.swType) : vs.setShowVal(vs.type);
                    if(type == Register.APP_BINARY || type == Register.MODBUS_BINARY) {
                        vs.showVal =  vs.showVal.replace("<br/>", " ");
                    }
                    this.message = context.getString(R.string.set_reg_val) + " - " + msgObj.getString("regName") + " (" + context.getString(R.string.device) + ": " + msgObj.getString("devName") + ", " + context.getString(R.string.value) + ": " + vs.showVal + ") ";
                }
            } else if(msgCode == DELETE_REG) {
                this.message = context.getString(R.string.remove) + context.getString(R.string.register) + " - " + msgObj.getString("regName") + " (" + context.getString(R.string.device) + ": " + msgObj.getString("devName") + ") ";
            } else if(msgCode == NEW_GROUP) {
                String devList = "";
                if(!msgObj.isNull("groupMember")) {
                    JSONArray jsonArray = msgObj.getJSONArray("groupMember");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        String sn = obj.getString("sn");
                        String addr = obj.getString("addr");
                        devList = (devList.length() > 0) ? (devList + ", " + sn + ":" + addr) : (sn + ":" + addr);
                    }
                    devList = (devList.length() > 0) ? " (" + devList + ")" : devList;
                }
                this.message = context.getString(R.string.new_group_member) + " - " + msgObj.getString("groupName") + devList;
            } else if(msgCode == EDIT_GROUP) {
                String devList = "";
                if(!msgObj.isNull("groupMember")) {
                    JSONArray jsonArray = msgObj.getJSONArray("groupMember");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        String sn = obj.getString("sn");
                        String addr = obj.getString("addr");
                        devList = (devList.length() > 0) ? (devList + ", " + sn + ":" + addr) : (sn + ":" + addr);
                    }
                    devList = (devList.length() > 0) ? " (" + devList + ")" : devList;
                }
                this.message = context.getString(R.string.edit) + context.getString(R.string.group) + " - " + msgObj.getString("groupName") + devList;
            } else if(msgCode == DELETE_GROUP) {
                String devList = "";
                if(!msgObj.isNull("groupMember")) {
                    JSONArray jsonArray = msgObj.getJSONArray("groupMember");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        String sn = obj.getString("sn");
                        String addr = obj.getString("addr");
                        devList = (devList.length() > 0) ? (devList + ", " + sn + ":" + addr) : (sn + ":" + addr);
                    }
                    devList = (devList.length() > 0) ? " (" + devList + ")" : devList;
                }
                this.message = context.getString(R.string.remove) + context.getString(R.string.group) + " - " + msgObj.getString("groupName") + devList;
            } else if(msgCode == CLEAR_EVTLOG) {
                this.message = context.getString(R.string.clear_log) + " - " + context.getString(R.string.event_log);
            } else if(msgCode == CLEAR_ALARM) {
                this.message = context.getString(R.string.clear_log) + " - " + context.getString(R.string.notification);
            } else if(msgCode == CLEAR_AUDIT) {
                this.message = context.getString(R.string.clear_log) + " - " + context.getString(R.string.audit_log);
            } else if(msgCode == CLEAR_IOSTLOG) {
                this.message = context.getString(R.string.clear_log) + " - " + context.getString(R.string.iostlog);
            } else if(msgCode == NEW_ANNOUNCE) {
                this.message = context.getString(R.string.new_announce) + " - " + msgObj.getString("title");
            } else if(msgCode == ANNOUNCE_SUB_CMP) {
                String companyName = msgObj.has("companyName") ? " (" + context.getString(R.string.company) + ": " + msgObj.getString("companyName") + ")" : "";
                this.message = context.getString(R.string.new_announce) + " - " + msgObj.getString("title") + companyName;
            } else if(msgCode == ANNOUNCE_ALL_CMP) {
                this.message = context.getString(R.string.new_announce_all) + " - " + msgObj.getString("title");
            } else if(msgCode == EDIT_ANNOUNCE) {
                this.message = context.getString(R.string.edit_announce) + " - " + msgObj.getString("title");
            } else if(msgCode == DELETE_ANNOUNCE) {
                this.message = context.getString(R.string.delete_announce) + " - " + msgObj.getString("title");
            } else if(msgCode == NEW_FLINK) {
                this.message = context.getString(R.string.new_flink) + " - " + msgObj.getString("desc");
            } else if(msgCode == EDIT_FLINK) {
                this.message = context.getString(R.string.edit_flink) + " - " + msgObj.getString("desc");
            } else if(msgCode == DELETE_FLINK) {
                this.message = context.getString(R.string.delete_flink) + " - " + msgObj.getString("desc");
            } else if(msgCode == SWITCH_MAC) {
                this.message = context.getString(R.string.switch_device) + " - " +
                    context.getString(R.string.device) + ": "  + msgObj.getString("srcSN") + ", " +
                    context.getString(R.string.device) + ": " + msgObj.getString("dstSN");
            } else if(msgCode == SWITCH_BACK) {
                this.message = context.getString(R.string.switch_back_device) + " - " +
                    context.getString(R.string.device) + ": "  + msgObj.getString("srcSN") + ", " +
                    context.getString(R.string.device) + ": " + msgObj.getString("dstSN");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
