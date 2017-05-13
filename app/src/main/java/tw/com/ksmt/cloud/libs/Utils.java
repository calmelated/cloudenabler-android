/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tw.com.ksmt.cloud.libs;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.ui.AnnounceActivity;
import tw.com.ksmt.cloud.ui.NotificationActivity;

public final class Utils {

    public static void bcastMessage(Context context, String message) {
        Intent intent = new Intent(PrjCfg.BRCAST_MSG);
        intent.putExtra(PrjCfg.BRCAST_MSG, message);
        context.sendBroadcast(intent);
    }

    public static String toUTFStr(String input) {
        String output = input;
        try {
            output = new String(input.getBytes("ISO-8859-1"), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }

    public static String unix2Datetime(long unixSec) {
        return unix2Datetime(unixSec, "yyyy-MM-dd HH:mm:ss");
    }

    public static String unix2Datetime(long unixSec, String format) {
        Date date = new Date(unixSec * 1000L); // *1000 is to convert seconds to milliseconds
        SimpleDateFormat sdf = new SimpleDateFormat(format); // the format of your date
        return sdf.format(date);
    }

    public static String unix2UTCDatetime(long unixSec, String format) {
        Date date = new Date(unixSec * 1000L); // *1000 is to convert seconds to milliseconds
        SimpleDateFormat sdf = new SimpleDateFormat(format); // the format of your date
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(date);
    }

    public static String toDuration(long seconds) {
        if(seconds < 60) {
            return String.format("%02d sec", seconds);
        } else if(seconds < 3600) {
            return String.format("%02d:%02d", (seconds % 3600) / 60, seconds % 60);
        } else if(seconds < 86400) {
            return String.format("%02d:%02d:%02d", (seconds % 86400) / 3600, (seconds % 3600) / 60, seconds % 60);
        } else {
            return String.format("%d d %02d:%02d:%02d", seconds / 86400, (seconds % 86400) / 3600, (seconds % 3600) / 60, seconds % 60);
        }
    }

    public static String getMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            String md5 = number.toString(16);
            while (md5.length() < 32) {
                md5 = "0" + md5;
            }
            return md5;
        } catch (NoSuchAlgorithmException e) {
            Log.e("MD5", e.getLocalizedMessage());
            return null;
        }
    }

    public static int[] getJSortedKeys(JSONObject jsonObject) {
        JSONArray jsonArray = jsonObject.names();
        if(jsonArray == null) {
            return new int[0];
        }

        int jlen = jsonArray.length();
        if(jlen == 0) {
            return new int[0];
        }
        int[] keys = new int[jlen];
        for(int i = 0; i < jlen; i++) {
            try {
                keys[i] = jsonArray.getInt(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Arrays.sort(keys);
        return keys;
    }

    public static String loadPrefs(Context context, String key, String defVal) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String data = sharedPreferences.getString(key, defVal);
        return data;
    }

    public static String loadPrefs(Context context, String key) {
        return loadPrefs(context, key, null);
    }

    public static boolean loadPrefsBool(Context context, String key) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String data = sharedPreferences.getString(key, "false");
        if(data.equals("true") || data.equals("1")) {
            return true;
        }
        return false;
    }

    public static void savePrefs(Context context, String key, String value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static void savePrefs(Context context, String key, Integer value) {
        savePrefs(context, key, Integer.toString(value));
    }

    // Issues a notification to inform the user that server has sent a message.
    public static void generateNotification(Context context, String message) {
        generateNotification(context, null, message);
    }

    public static void generateNotification(Context context, String title, String message) {
        if(!PrjCfg.NOTIFICATION.equals("true")) {
            return;
        }

        int icon;
        if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_FULLKEY)) {
            icon = R.mipmap.ic_launcher_fullkey;
            title = (title == null) ? context.getString(R.string.app_name_fullkey) : title;
        } else {
            icon = R.mipmap.ic_launcher;
            title = (title == null) ? context.getString(R.string.app_name) : title ;
        }

        long when = System.currentTimeMillis();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context)
                .setSmallIcon(icon)
                .setContentText(message)
                .setWhen(when);

        Intent notificationIntent;
        if(message.equals(context.getString(R.string.received_new_announce))) {
            notificationIntent = new Intent(context, AnnounceActivity.class);
        } else {
            notificationIntent = new Intent(context, NotificationActivity.class);
        }

        // set intent so it does not start a new activity
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        notification.setContentTitle(title);
        notification.setContentIntent(intent);
        notification.setAutoCancel(true);
        if(PrjCfg.NOTIFICATION_SOUND.equals("true")) {
            notification.setDefaults(Notification.DEFAULT_SOUND);
            if(PrjCfg.NOTIFICATION_VIBRATION.equals("true")) {
                notification.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
            }
        }
        if(PrjCfg.NOTIFICATION_VIBRATION.equals("true")) {
            notification.setDefaults(Notification.DEFAULT_VIBRATE);
            if(PrjCfg.NOTIFICATION_SOUND.equals("true")) {
                notification.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
            }
        }
        notificationManager.notify((int) (Math.random() * 1024 + 1), notification.build());
    }

    public static void restartActivity(Context context) {
        Activity activity = (Activity)context;
        Intent intent = activity.getIntent();
        activity.finish();
        activity.startActivity(intent);
    }

    public static InputFilter EMOJI_FILTER = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            for (int index = start; index < end; index++) {
                int type = Character.getType(source.charAt(index));
                if (type == Character.SURROGATE) {
                    return "";
                }
            }
            return null;
        }
    };

    public static <T> T[] arrayMerge(T[] a, T[] b) {
        int aLen = a.length;
        int bLen = b.length;

        @SuppressWarnings("unchecked")
        T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }

    public static int getIpBytes(String ipAddr) {
        int result = 0x0;
        try {
            byte[] b = InetAddress.getByName(ipAddr).getAddress();
            result = ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16) | ((b[2] & 0xFF) << 8)  | ((b[3] & 0xFF) << 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String getArrayKey(Context context, int keyId, int valId, String target) {
        String result = "";
        Resources res = context.getResources();
        String[] keys = res.getStringArray(keyId);
        String[] vals = res.getStringArray(valId);
        for(int i = 0; i < vals.length; i++) {
            if(vals[i].equals(target) && keys[i] != null) {
                return keys[i];
            }
        }
        return result;
    }

    public static String realAddr(String addr) {
        if(addr == null || addr.equals("") || addr.length() == 5) {
            return addr;
        }
        String result = addr;
        try {
            int _addr = Integer.parseInt(addr);
            result = String.format("%06d", (_addr % 1000000));
        } catch (Exception e) {
            Log.e(Dbg._TAG_(), "result = " + result);
            e.printStackTrace();
        } finally {
            return result;
        }
    }

    public static int slvIdx(String addr) {
        int result = 0;
        if(addr == null || addr.equals("")) {
            return result;
        }
        try {
            int _addr = Integer.parseInt(addr);
            result = (_addr / 1000000);
        } catch (Exception e) {
            Log.e(Dbg._TAG_(), "result = " + result);
            e.printStackTrace();
        } finally {
            return result;
        }
    }

    /**
     * Get ISO 3166-1 alpha-2 country code for this device (or null if not available)
     *
     * @param context Context reference to get the TelephonyManager instance from
     * @return country code or null
     */
    public static String getCountryBySIM(Context context) {
        try {
            final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            final String simCountry = tm.getSimCountryIso();
            if (simCountry != null && simCountry.length() == 2) { // SIM country code is available
                return simCountry.toUpperCase(Locale.US);
            } else if (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) { // device is not 3G (would be unreliable)
                String networkCountry = tm.getNetworkCountryIso();
                if (networkCountry != null && networkCountry.length() == 2) { // network country code is available
                    return networkCountry.toUpperCase(Locale.US);
                }
            }
        } catch (Exception e) {
        }
        return null;
    }
}
