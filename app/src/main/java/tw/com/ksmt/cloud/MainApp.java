package tw.com.ksmt.cloud;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVInstallation;
import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.PushService;
import com.avos.avoscloud.SaveCallback;

import tw.com.ksmt.cloud.libs.Dbg;
import tw.com.ksmt.cloud.libs.Utils;
import tw.com.ksmt.cloud.ui.NotificationActivity;

public class MainApp extends Application {
    public static boolean INET_CONNECTED = true;
    public static boolean FROM_GOOGLE_PLAY = false;

    public static long SERV_MAINT = 0;
    public static String LC_SERVER;
    public static String LC_PUSH_ID;

    @Override
    public void onCreate() {
        super.onCreate();
        final Context context = getApplicationContext();
        FROM_GOOGLE_PLAY = (getPackageManager().getInstallerPackageName(getPackageName()) == null) ? false : true ;

        final String customer = Utils.loadPrefs(context, "CUSTOMER", PrjCfg.CUSTOMER);
        setPushService(context, customer);
    }

    public static void setPushService(final Context context, final String customer) {
        // Choose Server (China or US)
        String country = Utils.getCountryBySIM(context);
        if(country != null && country.equals("CN")) {
            LC_SERVER = "CN";
            AVOSCloud.useAVCloudCN();
        } else {
            LC_SERVER = "US";
            AVOSCloud.useAVCloudCN();
        }
        AVOSCloud.initialize(context, PrjCfg.getLCAppId(LC_SERVER, customer), PrjCfg.getLCAppKey(LC_SERVER, customer));
        AVInstallation.getCurrentInstallation().saveInBackground(new SaveCallback() {
            public void done(AVException e) {
                if (e != null) {
                    Log.e(Dbg._TAG_(), e.toString());
                }
                LC_PUSH_ID = AVInstallation.getCurrentInstallation().getInstallationId();
                Log.e(Dbg._TAG_(), "Push ID = " + LC_PUSH_ID + ", LC_SERVER = " + LC_SERVER + ", CUSTOMER = " + customer);

                // subscribe push channel (for public )
                PushService.setDefaultPushCallback(context, NotificationActivity.class);
                PushService.subscribe(context, "public", NotificationActivity.class);
            }
        });
    }
}
