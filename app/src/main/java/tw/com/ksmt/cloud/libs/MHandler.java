package tw.com.ksmt.cloud.libs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;

import tw.com.ksmt.cloud.MainApp;
import tw.com.ksmt.cloud.R;

public class MHandler extends Handler {
    public static final short UPDATE = 0;
    public static final short LOAD_MORE = 1;
    public static final short REFRESH = 2;
    public static final short SHOW_ERR_MSG = 3;
    public static final short SHOW_MSG = 4;
    public static final short LOGIN = 5;
    public static final short TOAST_MSG = 6;
    public static final short GO_BACK = 7;
    public static final short BEEN_LOGOUT = 8;
    public static final short UPDATE_VAL = 9;
    public static final short UPDATE_TIME = 10;
    public static final short UPDATE_APP = 11;
    public static final short UPDATE_APCFG = 12;
    public static final short UPDATE_FWVER = 13;
    public static final short CLEAR_LIST = 14;
    public static final short ADD_LIST = 15;
    public static final short DEL_LIST = 16;
    public static final short SRCH_QUERY = 17;
    public static final short UPDATE_NTP = 18;
    public static final short UPDATE_UART = 19;
    public static final short NEXT_ONE = 20;
    public static final short UPDATE_DEVICE_SN = 21;
    public static final short UPDATE_DEVICE_ADDR = 22;
    public static final short ADD_DEVICE_LIST = 23;
    public static final short ADD_REGISTER_LIST = 24;
    public static final short UPDATE_FROM_CACHE = 25;
    public static final short ACCOUNT_ACTIVE = 26;
    public static final short ASK_PASSWORD = 27;
    public static final short LOGOUT = 28;
    public static final short SERV_MAINT = 29;
    public static final short INET_FAILED = 30;
    public static final short CNT_STATUS = 31;
    public static final short AUTH_DIALG = 32;
    public static final short REFER_REG_DIALG = 33;
    public static final short UPDATE_PARGP_LIST = 34;
    public static final short ADD_PARGP_LIST = 35;
    public static final short DRAWLINE = 36;

    public final WeakReference mActivity;
    private static Toast toast;
    private AlertDialog mhDialog;

    public MHandler(Activity activity) {
        this.mActivity = new WeakReference(activity);
    }

    public static void exec(Handler mHandler, short msgHandle) {
        mHandler.sendMessage(Message.obtain(mHandler, msgHandle));
    }

    public static void exec(Handler mHandler, short msgHandle, String msgStr) {
        Message msg = Message.obtain(mHandler, msgHandle);
        msg.obj = new String(msgStr);
        mHandler.sendMessage(msg);
    }

    public static void exec(Handler mHandler, short msgHandle, Boolean msgBool) {
        Message msg = Message.obtain(mHandler, msgHandle);
        msg.obj = msgBool;
        mHandler.sendMessage(msg);
    }

    public static void exec(Handler mHandler, short msgHandle, Object obj) {
        Message msg = Message.obtain(mHandler, msgHandle);
        msg.obj = obj;
        mHandler.sendMessage(msg);
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            final Activity activity = (Activity)mActivity.get();
            switch (msg.what) {
                case MHandler.SHOW_ERR_MSG: {
                    Kdialog.show(activity, Kdialog.FAILED, (String) msg.obj);
                    break;
                }
                case MHandler.SHOW_MSG: {
                    Kdialog.show(activity, Kdialog.INFO, (String) msg.obj);
                    break;
                }
                case MHandler.TOAST_MSG: {
                    if(toast != null && toast.getView().isShown()) {
                        toast.cancel();
                    }
                    toast = Toast.makeText(activity, (String) msg.obj, Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                }
                case MHandler.GO_BACK: {
                    if(msg.obj != null) {
                        Kdialog.show(activity, Kdialog.BACK_MSG, (String) msg.obj);
                    } else {
                        activity.onBackPressed();
                    }
                    break;
                }
                case MHandler.BEEN_LOGOUT: {
                    Kdialog.getDefInfoDialog(activity)
                            .setCancelable(false)
                            .setMessage(activity.getString(R.string.been_signout))
                            .setPositiveButton(activity.getString(R.string.confirm), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Kdialog.getProgress(activity);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    WebUtils.logout(activity);
                                }
                            }).start();
                        }
                    }).show();
                    break;
                }
                case MHandler.SERV_MAINT: {
                    long curTime = new Date().getTime() / 1000;
                    String backStr = (new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")).format(new Date(MainApp.SERV_MAINT * 1000L));
                    //Log.e(Dbg._TAG_(), "backtime: " + PrjCfg.SERV_MAINT + ", curTime: " + curTime);
                    String alertMsg = ((MainApp.SERV_MAINT - curTime) < 11) ? activity.getString(R.string.serv_maint) : activity.getString(R.string.serv_maint_503) + backStr;
                    if(mhDialog != null) {
                        return;
                    }
                    mhDialog = Kdialog.getDefInfoDialog(activity)
                            .setTitle(activity.getString(R.string.serv_maint_title))
                            .setMessage(alertMsg)
                            .setCancelable(false)
                            .setPositiveButton(activity.getString(R.string.confirm), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mhDialog = null;
                                    Intent intent = new Intent(Intent.ACTION_MAIN);
                                    intent.addCategory(Intent.CATEGORY_HOME);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    activity.startActivity(intent);
                                }
                            }).show();
                    break;
                }
                case MHandler.INET_FAILED: {
                    Kdialog.show(activity, Kdialog.FAILED, activity.getString(R.string.inet_failed_msg));
                    break;
                }
                case MHandler.LOGOUT: {
                    Kdialog.getProgress(activity);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            WebUtils.logout(activity);
                        }
                    }).start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}