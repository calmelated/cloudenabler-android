package tw.com.ksmt.cloud.iface;

import android.content.Context;

import org.json.JSONObject;

import java.io.Serializable;

import tw.com.ksmt.cloud.R;

public class CloudStatus implements Serializable {
    public final static int TYPE_TITLE   = 0;
    public final static int TYPE_SERVICE = 1;
    public final static int TYPE_HISTORY = 2;
    public int type;

    public final static int ERR_TYPE_WEB_OK = 0;
    public final static int ERR_TYPE_WEB_FAIL = 1;
    public final static int ERR_TYPE_WEB_UNKNOWN = 2;
    public final static int ERR_TYPE_NET_OK = 3;
    public final static int ERR_TYPE_NET_FAIL = 4;
    public final static int ERR_TYPE_NET_UNKNOWN = 5;
    public final static int ERR_TYPE_APP_DOWN_OK = 6;
    public final static int ERR_TYPE_APP_DOWN_FAIL = 7;
    public final static int ERR_TYPE_APP_DOWN_UNKNOWN = 8;
    public final static int ERR_TYPE_PUSH_OK = 9;
    public final static int ERR_TYPE_PUSH_FAIL = 10;
    public final static int ERR_TYPE_PUSH_UNKNOWN = 11;
    public final static int ERR_TYPE_EMAIL_OK = 12;
    public final static int ERR_TYPE_EMAIL_FAIL = 13;
    public final static int ERR_TYPE_EMAIL_UNKNOWN = 14;
    public final static int ERR_TYPE_LINODE_OK = 15;
    public final static int ERR_TYPE_LINDOE_FAIL = 16;
    public final static int ERR_TYPE_LINODE_UNKNOWN = 17;

    // Title
    public String title;

    // Service (now)
    public int servNameId;
    public int status; // 0: failed, 1: ok, 9: timeout/unknown

    // History
    public int updateTime;
    public String errMsg;
    public String duration = "";

    // Service
    public CloudStatus(int type, int servNameId, int status) {
        this.type = type;
        this.servNameId = servNameId;
        this.status = status;
    }

    public CloudStatus(int type, String title) {
        this.type = type;
        this.title = title;
    }

    // History
    public CloudStatus(int type, Context context, JSONObject jsonObject) {
        try {
            this.type = type;
            updateTime = jsonObject.getInt("time");

            if(!jsonObject.isNull("duration")) {
                duration = jsonObject.getString("duration");
            }

            int errType = jsonObject.getInt("type");
            if(errType == ERR_TYPE_WEB_OK) {
                errMsg = context.getString(R.string.err_cloud_failed_back);
            } else if(errType == ERR_TYPE_WEB_FAIL) {
                errMsg = context.getString(R.string.err_cloud_failed);
            } else if(errType == ERR_TYPE_APP_DOWN_OK) {
                errMsg = context.getString(R.string.app_download_failed_back);
            } else if(errType == ERR_TYPE_APP_DOWN_FAIL) {
                errMsg = context.getString(R.string.app_download_failed);
            } else if(errType == ERR_TYPE_EMAIL_OK) {
                errMsg = context.getString(R.string.mail_failed_back);
            } else if(errType == ERR_TYPE_EMAIL_FAIL) {
                errMsg = context.getString(R.string.mail_failed);
            } else if(errType == ERR_TYPE_LINODE_OK) {
                errMsg = context.getString(R.string.err_cloud_failed_back);
            } else if(errType == ERR_TYPE_LINDOE_FAIL) {
                errMsg = context.getString(R.string.err_cloud_failed);
            } else if(errType == ERR_TYPE_PUSH_OK) {
                errMsg = context.getString(R.string.notifiaction_failed_back);
            } else if(errType == ERR_TYPE_PUSH_FAIL) {
                errMsg = context.getString(R.string.notifiaction_failed);
            } else if(errType == ERR_TYPE_NET_OK) {
                errMsg = context.getString(R.string.err_net_loss_rate_back);
            } else if(errType == ERR_TYPE_NET_FAIL) {
                errMsg = context.getString(R.string.err_net_loss_rate);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isUnknownEvent(int type) {
        if(type == ERR_TYPE_APP_DOWN_UNKNOWN) {
            return true;
        } else if(type == ERR_TYPE_EMAIL_UNKNOWN) {
            return true;
        } else if(type == ERR_TYPE_PUSH_UNKNOWN) {
            return true;
        } else if(type == ERR_TYPE_LINODE_UNKNOWN) {
            return true;
        } else if(type == ERR_TYPE_NET_UNKNOWN) {
            return true;
        } else if(type == ERR_TYPE_WEB_UNKNOWN) {
            return true;
        }
        return false;
    }
}
