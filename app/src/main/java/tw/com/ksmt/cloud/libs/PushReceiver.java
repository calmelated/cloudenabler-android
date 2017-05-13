package tw.com.ksmt.cloud.libs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.json.JSONObject;

import tw.com.ksmt.cloud.ui.AppSettings;

public class PushReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String action = intent.getAction();
            if(!action.equals("tw.com.ksmt.cloud.action.PUSH_RECEIVER")) {
                return;
            }

            //  String channel = intent.getExtras().getString("com.avos.avoscloud.Channel");
            JSONObject json = new JSONObject(intent.getExtras().getString("com.avos.avoscloud.Data"));
            String message = json.getString("message");
            AppSettings.reload(context);
            Utils.generateNotification(context, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
