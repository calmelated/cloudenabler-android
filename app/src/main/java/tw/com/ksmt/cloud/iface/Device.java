package tw.com.ksmt.cloud.iface;

import android.content.Context;
import android.util.Log;

import com.avos.avoscloud.okhttp.MultipartBuilder;
import com.avos.avoscloud.okhttp.RequestBody;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.libs.Dbg;

public class Device implements Serializable, Cloneable {
    public static final String MODEL_SIO           = "6101";
    public static final String MODEL_SIO_PLUS      = "61205W";
    public static final String MODEL_CE_63511      = "63511";
    public static final String MODEL_CE_63511W     = "63511W";
    public static final String MODEL_CE_63512      = "63512";
    public static final String MODEL_CE_63512W     = "63512W";
    public static final String MODEL_CE_63513      = "63513";
    public static final String MODEL_CE_63513W     = "63513W";
    public static final String MODEL_CE_63514      = "63514";
    public static final String MODEL_CE_63514W     = "63514W";
    public static final String MODEL_CE_63515      = "63515";
    public static final String MODEL_CE_63515W     = "63515W";
    public static final String MODEL_CE_63516      = "63516";
    public static final String MODEL_CE_63516W     = "63516W";

    // HYEC Model
    public static final String MODEL_CE_HY_63511   = "HY-63511";
    public static final String MODEL_CE_HY_63511W  = "HY-63511W";
    public static final String MODEL_CE_HY_63512   = "HY-63512";
    public static final String MODEL_CE_HY_63512W  = "HY-63512W";
    public static final String MODEL_CE_HY_63515   = "HY-63515";
    public static final String MODEL_CE_HY_63515W  = "HY-63515W";
    public static final String MODEL_CE_HY_63516   = "HY-63516";
    public static final String MODEL_CE_HY_63516W  = "HY-63516W";

    // YATEC Model
    public static final String MODEL_CE_YT_63511   = "YT-63511";
    public static final String MODEL_CE_YT_63511W  = "YT-63511W";
    public static final String MODEL_CE_YT_63512   = "YT-63512";
    public static final String MODEL_CE_YT_63512W  = "YT-63512W";
    public static final String MODEL_CE_YT_63515   = "YT-63515";
    public static final String MODEL_CE_YT_63515W  = "YT-63515W";
    public static final String MODEL_CE_YT_63516   = "YT-63516";
    public static final String MODEL_CE_YT_63516W  = "YT-63516W";

    public int status = 0;
    public int lastStatus = -1;
    public int fwVer = -1;
    public int fwVerNew = -1;
    public String model;
    public String name;
    public String sn;
    public String sn2;
    public String ip = "";
    public String ftpPswd = "";
    public int pollTime = -1;
    public int mbusTimeout = 30;
    public int logFreq = PrjCfg.DEV_LOG_FREQ;
    public boolean enlog = false;
    public boolean enServLog = false;
    public int storCapacity = 80;

    // FTP Client
    public boolean enFtpCli = false;
    public String ftpCliHost = "";
    public int ftpCliPort = 21;
    public String ftpCliAccount = "";
    public String ftpCliPswd = "";

    // Extra Setting
    public String url_1 = "";
    public String url_2 = "";
    public String url_3 = "";

    // Modbus Master
    public List<Integer> slvIdxList; // for the first
    public int slvIdx = 0;
    public MstDev slvDev;
    public JSONObject slvNames; //Used in GroupStatus, format -> "slvDev": {"1": "slv1", "2": "slv2", "3": "slv3"}

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    // New or Edit device
    public Device(String sn, String name, String model, int pollTime, boolean enlog) {
        this.sn = sn;
        this.name = name;
        this.model = model;
        this.pollTime = pollTime;
        this.enlog = enlog;
    }

    // Local (broadcast)
    public Device(String sn, String name, String model, int status) {
        this.sn = sn;
        this.name = name;
        this.model = model;
        this.status = status;
    }

    // Local (broadcast)
    public Device(String model, String name, String sn, String ip) {
        this.name = name;
        this.model = model;
        this.sn = sn;
        this.ip = ip;
    }

    // Local (user specifiy)
    public Device(String ip) {
        this.sn = null;
        this.ip = ip;
        this.name = null;
    }

    // get device info from Web API
    public Device(JSONObject jsonObject) {
        try {
            this.sn = jsonObject.getString("sn");
            this.sn2 = jsonObject.has("sn2") ? jsonObject.getString("sn2") : null;
            this.name = jsonObject.getString("name");
            this.model = jsonObject.getString("mo");
            this.fwVer = jsonObject.has("fwVer")  ? jsonObject.getInt("fwVer") : 0;
            this.fwVerNew = jsonObject.has("fwVerNew")  ? jsonObject.getInt("fwVerNew") : 0;
            this.status = jsonObject.has("status") ? jsonObject.getInt("status") : 0;
            this.pollTime = jsonObject.has("pollTime") ? jsonObject.getInt("pollTime") : 0;
            this.enlog = jsonObject.has("enLog") ? ((jsonObject.getInt("enLog") == 1) ? true : false) : false;
            this.enServLog = jsonObject.has("enServLog") ? ((jsonObject.getInt("enServLog") == 1) ? true : false) : false;
            this.logFreq = jsonObject.has("logFreq") ? jsonObject.getInt("logFreq") : PrjCfg.DEV_LOG_FREQ;
            this.ftpPswd = jsonObject.has("ftpPswd") ? jsonObject.getString("ftpPswd") : "admin";
            this.storCapacity = jsonObject.has("storCapacity") ? jsonObject.getInt("storCapacity") : 80;
            this.mbusTimeout = jsonObject.has("mbusTimeout") ? jsonObject.getInt("mbusTimeout") : 30;
            this.ip = "";

            // FTP
            this.enFtpCli = jsonObject.has("enFtpCli") ? ((jsonObject.getInt("enFtpCli") == 1) ? true : false) : false;
            this.ftpCliHost = jsonObject.has("ftpCliHost") ? jsonObject.getString("ftpCliHost") : "";
            this.ftpCliPort = jsonObject.has("ftpCliPort") ? jsonObject.getInt("ftpCliPort") : 21;
            this.ftpCliAccount = jsonObject.has("ftpCliAccount") ? jsonObject.getString("ftpCliAccount") : "";
            this.ftpCliPswd = jsonObject.has("ftpCliPswd") ? jsonObject.getString("ftpCliPswd") : "";

            // Extra
            if(!jsonObject.isNull("extra")) {
                JSONObject extraObj = jsonObject.getJSONObject("extra");
                if(extraObj.has("url_1")) {
                    this.url_1 = extraObj.getString("url_1");
                }
                if(extraObj.has("url_2")) {
                    this.url_2 = extraObj.getString("url_2");
                }
                if(extraObj.has("url_3")) {
                    this.url_3 = extraObj.getString("url_3");
                }
            }

            // Modbus Master
            if(isMbusMaster() && jsonObject.has("mstConf")) {
                JSONObject mstConfs = jsonObject.getJSONObject("mstConf");
                JSONArray ids = mstConfs.names();
                if(ids != null) {
                    slvIdxList = new ArrayList<Integer>();
                    for (int j = 0; j < ids.length(); j++) {
                        slvIdxList.add(ids.getInt(j));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static TreeMap<String, String> TYPES;

    public static void initTypes(Context context) {
        TYPES = new TreeMap<String, String>();
        TYPES.put(context.getString(R.string.device_kt_63511),  MODEL_CE_63511);
        TYPES.put(context.getString(R.string.device_kt_63511w), MODEL_CE_63511W);
        TYPES.put(context.getString(R.string.device_kt_63512),  MODEL_CE_63512);
        TYPES.put(context.getString(R.string.device_kt_63512w), MODEL_CE_63512W);
        TYPES.put(context.getString(R.string.device_kt_63513),  MODEL_CE_63513);
        TYPES.put(context.getString(R.string.device_kt_63513),  MODEL_CE_63513W);
        TYPES.put(context.getString(R.string.device_kt_63514),  MODEL_CE_63514);
        TYPES.put(context.getString(R.string.device_kt_63514w), MODEL_CE_63514W);
        TYPES.put(context.getString(R.string.device_kt_63515),  MODEL_CE_63515);
        TYPES.put(context.getString(R.string.device_kt_63515w), MODEL_CE_63515W);
        TYPES.put(context.getString(R.string.device_kt_63516),  MODEL_CE_63516);
        TYPES.put(context.getString(R.string.device_kt_63516w), MODEL_CE_63516W);

        //HYEC
        TYPES.put(context.getString(R.string.device_hy_63511),  MODEL_CE_HY_63511);
        TYPES.put(context.getString(R.string.device_hy_63511w), MODEL_CE_HY_63511W);
        TYPES.put(context.getString(R.string.device_hy_63512),  MODEL_CE_HY_63512);
        TYPES.put(context.getString(R.string.device_hy_63512w), MODEL_CE_HY_63512W);
        TYPES.put(context.getString(R.string.device_hy_63515),  MODEL_CE_HY_63515);
        TYPES.put(context.getString(R.string.device_hy_63515w), MODEL_CE_HY_63515W);
        TYPES.put(context.getString(R.string.device_hy_63516),  MODEL_CE_HY_63516);
        TYPES.put(context.getString(R.string.device_hy_63516w), MODEL_CE_HY_63516W);

        //YATEC
        TYPES.put(context.getString(R.string.device_yt_63511),  MODEL_CE_YT_63511);
        TYPES.put(context.getString(R.string.device_yt_63511w), MODEL_CE_YT_63511W);
        TYPES.put(context.getString(R.string.device_yt_63512),  MODEL_CE_YT_63512);
        TYPES.put(context.getString(R.string.device_yt_63512w), MODEL_CE_YT_63512W);
        TYPES.put(context.getString(R.string.device_yt_63515),  MODEL_CE_YT_63515);
        TYPES.put(context.getString(R.string.device_yt_63515w), MODEL_CE_YT_63515W);
        TYPES.put(context.getString(R.string.device_yt_63516),  MODEL_CE_YT_63516);
        TYPES.put(context.getString(R.string.device_yt_63516w), MODEL_CE_YT_63516W);
    }

    public static String getTypeName(Context context, String model) {
        if(TYPES == null) {
            initTypes(context);
        }
        String result = "";
        for(String typeName : TYPES.keySet()) {
            if(TYPES.get(typeName).equals(model)) {
                result = typeName;
                break;
            }
        }
        return result;
    }

    public static String getTypeIdx(Context context, String typeName) {
        if(TYPES == null) {
            initTypes(context);
        }
        return TYPES.get(typeName);
    }

    public static String[] getAllTypes(Context context) {
        if(TYPES == null) {
            initTypes(context);
        }
        Set typesSet = TYPES.keySet();
        return (String[]) typesSet.toArray(new String[typesSet.size()]);
    }

    public boolean isSIO() {
        return (model.equals(MODEL_SIO)) ? true : false ;
    }

    public boolean isSIOPlus() {
        return (model.equals(MODEL_SIO_PLUS)) ? true : false ;
    }

    public boolean isMbusMaster() {
        return (model.equals(MODEL_CE_63515)     ||
                model.equals(MODEL_CE_63515W)    ||
                model.equals(MODEL_CE_63516)     ||
                model.equals(MODEL_CE_63516W)    ||
                model.equals(MODEL_CE_HY_63515)  ||
                model.equals(MODEL_CE_HY_63515W) ||
                model.equals(MODEL_CE_HY_63516)  ||
                model.equals(MODEL_CE_HY_63516W) ||
                model.equals(MODEL_CE_YT_63515)  ||
                model.equals(MODEL_CE_YT_63515W) ||
                model.equals(MODEL_CE_YT_63516)  ||
                model.equals(MODEL_CE_YT_63516W)
        ) ? true : false ;
    }

    public boolean isCE() {
        return (model.equals(MODEL_CE_63511)     ||
                model.equals(MODEL_CE_63511W)    ||
                model.equals(MODEL_CE_63512)     ||
                model.equals(MODEL_CE_63512W)    ||
                model.equals(MODEL_CE_63513)     ||
                model.equals(MODEL_CE_63513W)    ||
                model.equals(MODEL_CE_63514)     ||
                model.equals(MODEL_CE_63514W)    ||
                model.equals(MODEL_CE_63515)     ||
                model.equals(MODEL_CE_63515W)    ||
                model.equals(MODEL_CE_63516)     ||
                model.equals(MODEL_CE_63516W)    ||
                model.equals(MODEL_CE_HY_63511)  ||
                model.equals(MODEL_CE_HY_63511W) ||
                model.equals(MODEL_CE_HY_63512)  ||
                model.equals(MODEL_CE_HY_63512W) ||
                model.equals(MODEL_CE_HY_63515)  ||
                model.equals(MODEL_CE_HY_63515W) ||
                model.equals(MODEL_CE_HY_63516)  ||
                model.equals(MODEL_CE_HY_63516W) ||
                model.equals(MODEL_CE_YT_63511)  ||
                model.equals(MODEL_CE_YT_63511W) ||
                model.equals(MODEL_CE_YT_63512)  ||
                model.equals(MODEL_CE_YT_63512W) ||
                model.equals(MODEL_CE_YT_63515)  ||
                model.equals(MODEL_CE_YT_63515W) ||
                model.equals(MODEL_CE_YT_63516)  ||
                model.equals(MODEL_CE_YT_63516W)
        ) ? true : false ;
    }

    public int getNumRs485() {
        if(model.equals(MODEL_CE_63513)     ||
           model.equals(MODEL_CE_63513W)    ||
           model.equals(MODEL_CE_63514)     ||
           model.equals(MODEL_CE_63514W)    ||
           model.equals(MODEL_CE_63515)     ||
           model.equals(MODEL_CE_63515W)    ||
           model.equals(MODEL_CE_63516)     ||
           model.equals(MODEL_CE_63516W)    ||
           model.equals(MODEL_CE_HY_63515)  ||
           model.equals(MODEL_CE_HY_63515W) ||
           model.equals(MODEL_CE_HY_63516)  ||
           model.equals(MODEL_CE_HY_63516W) ||
           model.equals(MODEL_CE_YT_63515)  ||
           model.equals(MODEL_CE_YT_63515W) ||
           model.equals(MODEL_CE_YT_63516)  ||
           model.equals(MODEL_CE_YT_63516W)) {
            return 2;
        } else {
            return 1;
        }
    }

    public int getNumRegs() {
        if(model.equals(MODEL_CE_63511)     ||
           model.equals(MODEL_CE_63511W)    ||
           model.equals(MODEL_CE_63513)     ||
           model.equals(MODEL_CE_63513W)    ||
           model.equals(MODEL_CE_63515)     ||
           model.equals(MODEL_CE_63515W)    ||
           model.equals(MODEL_CE_HY_63511)  ||
           model.equals(MODEL_CE_HY_63511W) ||
           model.equals(MODEL_CE_HY_63515)  ||
           model.equals(MODEL_CE_HY_63515W) ||
           model.equals(MODEL_CE_YT_63511)  ||
           model.equals(MODEL_CE_YT_63511W) ||
           model.equals(MODEL_CE_YT_63515)  ||
           model.equals(MODEL_CE_YT_63515W)) {
            return 128;
        } else if(
            model.equals(MODEL_CE_63512)     ||
            model.equals(MODEL_CE_63512W)    ||
            model.equals(MODEL_CE_63514)     ||
            model.equals(MODEL_CE_63514W)    ||
            model.equals(MODEL_CE_63516)     ||
            model.equals(MODEL_CE_63516W)    ||
            model.equals(MODEL_CE_HY_63512)  ||
            model.equals(MODEL_CE_HY_63512W) ||
            model.equals(MODEL_CE_HY_63516)  ||
            model.equals(MODEL_CE_HY_63516W) ||
            model.equals(MODEL_CE_YT_63512)  ||
            model.equals(MODEL_CE_YT_63512W) ||
            model.equals(MODEL_CE_YT_63516)  ||
            model.equals(MODEL_CE_YT_63516W)) {
            return 256;
        } else {
            Log.e(Dbg._TAG_(), "This function is only for Cloud Enabler!");
            return 0;
        }
    }

    public RequestBody toMultiPart(Device origDevice) {
        MultipartBuilder entityBuilder = new MultipartBuilder();
        entityBuilder.type(MultipartBuilder.FORM);
        entityBuilder.addFormDataPart("sn", this.sn);
        entityBuilder.addFormDataPart("mo", String.valueOf(this.model));
        if(!origDevice.name.equals(this.name)) {
            entityBuilder.addFormDataPart("name", this.name);
        }
        if(origDevice.enlog != this.enlog) {
            entityBuilder.addFormDataPart("enLog", "" + (this.enlog ? "1" : "0"));
        }
        if(origDevice.enServLog != this.enServLog) {
            entityBuilder.addFormDataPart("enServLog", "" + (this.enServLog ? "1" : "0"));
        }
        if(origDevice.logFreq != this.logFreq) {
            entityBuilder.addFormDataPart("logFreq", String.valueOf(this.logFreq));
        }

        Log.e(Dbg._TAG_(), "polltime = " + origDevice.pollTime + ", this.poll " + this.pollTime);
        if(origDevice.pollTime != this.pollTime) {
            entityBuilder.addFormDataPart("pollTime", String.valueOf(this.pollTime));
        }
        if(origDevice.storCapacity != this.storCapacity) {
            entityBuilder.addFormDataPart("storCapacity", String.valueOf(this.storCapacity));
        }
        if(origDevice.mbusTimeout != this.mbusTimeout) {
            entityBuilder.addFormDataPart("mbusTimeout", String.valueOf(this.mbusTimeout));
        }

        Log.e(Dbg._TAG_(), "ftpPswd = " + origDevice.ftpPswd + ", this.ftpPswd " + this.ftpPswd);
        if(!origDevice.ftpPswd.equals(this.ftpPswd)) {
            entityBuilder.addFormDataPart("ftpPswd", this.ftpPswd);
        }
        if(origDevice.enFtpCli != this.enFtpCli) {
            entityBuilder.addFormDataPart("enFtpCli", "" + (this.enFtpCli ? "1" : "0"));
        }
        if(!origDevice.ftpCliHost.equals(this.ftpCliHost)) {
            entityBuilder.addFormDataPart("ftpCliHost", this.ftpCliHost);
        }
        if(origDevice.ftpCliPort != this.ftpCliPort) {
            entityBuilder.addFormDataPart("ftpCliPort", String.valueOf(this.ftpCliPort));
        }
        if(!origDevice.ftpCliAccount.equals(this.ftpCliAccount)) {
            entityBuilder.addFormDataPart("ftpCliAccount", this.ftpCliAccount);
        }
        if(!origDevice.ftpCliPswd.equals(this.ftpCliPswd)) {
            entityBuilder.addFormDataPart("ftpCliPswd", this.ftpCliPswd);
        }

        if(!origDevice.url_1.equals(this.url_1) || !origDevice.url_2.equals(this.url_2) || !origDevice.url_3.equals(this.url_3) ) {
            try {
                JSONObject extra = new JSONObject();
                extra.put("url_1", this.url_1);
                extra.put("url_2", this.url_2);
                extra.put("url_3", this.url_3);
                entityBuilder.addFormDataPart("extra", extra.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return entityBuilder.build();
    }

    public RequestBody toMultiPart() {
        MultipartBuilder entityBuilder = new MultipartBuilder();
        entityBuilder.type(MultipartBuilder.FORM);
        entityBuilder.addFormDataPart("sn", this.sn);
        entityBuilder.addFormDataPart("mo", String.valueOf(this.model));
        entityBuilder.addFormDataPart("name", this.name);
        entityBuilder.addFormDataPart("enLog", "" + (this.enlog ? "1" : "0"));
        entityBuilder.addFormDataPart("enServLog", "" + (this.enServLog ? "1" : "0"));
        entityBuilder.addFormDataPart("logFreq", String.valueOf(this.logFreq));
        entityBuilder.addFormDataPart("pollTime", String.valueOf(this.pollTime));
        entityBuilder.addFormDataPart("storCapacity", String.valueOf(this.storCapacity));
        entityBuilder.addFormDataPart("ftpPswd", this.ftpPswd);
        entityBuilder.addFormDataPart("enFtpCli", "" + (this.enFtpCli ? "1" : "0"));
        entityBuilder.addFormDataPart("ftpCliHost", this.ftpCliHost);
        entityBuilder.addFormDataPart("ftpCliPort", String.valueOf(this.ftpCliPort));
        entityBuilder.addFormDataPart("ftpCliAccount", this.ftpCliAccount);
        entityBuilder.addFormDataPart("ftpCliPswd", this.ftpCliPswd);
        return entityBuilder.build();
    }

    public List<ListItem> toListItems(Context context) {
        List<ListItem> listItems = new ArrayList<ListItem>();

        // Device Info
        listItems.add(new ListItem(context.getString(R.string.device)));
        listItems.add(new ListItem(ListItem.VIEW, context.getString(R.string.device_sn), sn));

        // F/W
        if(fwVer > 0) {
            listItems.add(new ListItem(ListItem.VIEW, context.getString(R.string.device_version), String.valueOf(fwVer)));
            if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_KSMT_TEST_ALL)) {
                listItems.add(new ListItem(ListItem.BUTTON, context.getString(R.string.device_fw_upgrade), context.getString(R.string.lastest_version) + ": " + fwVerNew));
                listItems.add(new ListItem(ListItem.BUTTON, context.getString(R.string.stm32_upgrade), context.getString(R.string.stm32_upgrade_hint)));
            } else {
                if(fwVerNew > 0 && fwVer > 30 && fwVerNew > fwVer) {
                    listItems.add(new ListItem(ListItem.BUTTON, context.getString(R.string.device_fw_upgrade), context.getString(R.string.lastest_version) + ": " + fwVerNew));
                }
            }
        }

        // Polling time, Model
        if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_KSMT_TEST_ALL)){
            listItems.add(new ListItem(ListItem.VIEW, context.getString(R.string.device_model), getTypeName(context, model)));
            listItems.add(new ListItem(ListItem.NUMBER, context.getString(R.string.device_polling), String.valueOf(pollTime)));
        } else if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_FULLKEY)){
            // hide Device mode
        } else {
            listItems.add(new ListItem(ListItem.VIEW, context.getString(R.string.device_model), getTypeName(context, model)));
        }

        // Device Name
        listItems.add(new ListItem(ListItem.INPUT, context.getString(R.string.device_name), name));

        // Remote reboot
        if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_KSMT_TEST_ALL)) {
            listItems.add(new ListItem(ListItem.BUTTON, context.getString(R.string.reboot), context.getString(R.string.reboot_hinet)));
        } else if(fwVer > 31) {
            listItems.add(new ListItem(ListItem.BUTTON, context.getString(R.string.reboot), context.getString(R.string.reboot_hinet)));
        }

        // Modbus Timeout
        if(!isMbusMaster()) {
            listItems.add(new ListItem(ListItem.NUMBER, context.getString(R.string.mbus_timeout), String.valueOf(mbusTimeout)));
        }

        // Data Logging
        listItems.add(new ListItem(context.getString(R.string.logging)));

        // Cloud Logging
        if(PrjCfg.EN_CLOUD_LOG) {
            listItems.add(new ListItem(ListItem.CHECKBOX, context.getString(R.string.enable_server_loggoing), (enServLog ? "1" : "0")));
        }

        listItems.add(new ListItem(ListItem.CHECKBOX, context.getString(R.string.enable_usb_loggoing), (enlog ? "1" : "0")));
        listItems.add(new ListItem(ListItem.NUMBER, context.getString(R.string.logging_freq), String.valueOf(logFreq)));
        listItems.add(new ListItem(ListItem.NUMBER, context.getString(R.string.storage_capacity), String.valueOf(storCapacity)));
        listItems.add(new ListItem(ListItem.NEW_PASSWORD, context.getString(R.string.ftp_srv_password), ftpPswd));

        // FTP
        listItems.add(new ListItem(context.getString(R.string.ftp_client)));
        listItems.add(new ListItem(ListItem.CHECKBOX, context.getString(R.string.ftp_cli_enable), (enFtpCli ? "1" : "0")));
        if(enFtpCli) {
            listItems.add(new ListItem(ListItem.INPUT, context.getString(R.string.ftp_cli_host), ftpCliHost));
            listItems.add(new ListItem(ListItem.NUMBER, context.getString(R.string.ftp_cli_port), String.valueOf(ftpCliPort)));
            listItems.add(new ListItem(ListItem.INPUT, context.getString(R.string.ftp_cli_account), ftpCliAccount));
            listItems.add(new ListItem(ListItem.PASSWORD, context.getString(R.string.ftp_cli_password), ftpCliPswd));
            listItems.add(new ListItem(ListItem.BUTTON, context.getString(R.string.ftp_cli_send_log), context.getString(R.string.ftp_cli_send_log_hint)));
        }

        // Download Link (YATEC oringinal)
        if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_KSMT_TEST_ALL)) {
            listItems.add(new ListItem(context.getString(R.string.other_settings)));
            listItems.add(new ListItem(ListItem.LONG_INPUT, context.getString(R.string.device_extra_1), url_1));
            listItems.add(new ListItem(ListItem.LONG_INPUT, context.getString(R.string.device_extra_2), url_2));
            listItems.add(new ListItem(ListItem.LONG_INPUT, context.getString(R.string.device_extra_3), url_3));
        }
        return listItems;
    }

    public String toString() {
        String result = name + " (" + sn + ") ";
        return (isMbusMaster() && slvIdx > 0) ? (result + "- " + slvDev.name) : result ;
    }
}
