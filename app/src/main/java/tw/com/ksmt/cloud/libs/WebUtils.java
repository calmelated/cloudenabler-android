package tw.com.ksmt.cloud.libs;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.avos.avoscloud.PushService;
import com.avos.avoscloud.okhttp.MultipartBuilder;
import com.avos.avoscloud.okhttp.Response;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;

import tw.com.ksmt.cloud.MainApp;
import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.ui.LoginActivity;

public final class WebUtils {
    public static boolean logining = false;
    private static File newApkFile;
    private static boolean isFileExport = false;

    public static final JSONObject login(Context context) throws Exception {
        String company = Utils.loadPrefs(context, "Company");
        String account = Utils.loadPrefs(context, "Account");
        String password = Utils.loadPrefs(context, "Password");
        return login(context, company, account, password, false);
    }

    public static final JSONObject login(Context context, boolean force) throws Exception {
        String company = Utils.loadPrefs(context, "Company");
        String account = Utils.loadPrefs(context, "Account");
        String password = Utils.loadPrefs(context, "Password");
        return login(context, company, account, password, force);
    }

    public static final JSONObject login(Context context, String company, String account, String password, boolean force) throws Exception {
        Log.e(Dbg._TAG_(), "Session might timeout, re-authenticate again !!");
        JSONObject jLogin = null;
        if(!force && logining) {
            Log.e(Dbg._TAG_(), "Other thread is logining ... ");
            return jLogin;
        }
        logining = true;

        if (company == null || account == null || password == null || password.equals("")) {
            Log.e(Dbg._TAG_(), "Authentication data isn't enough !!");
            Utils.savePrefs(context, "Password", "");
            logining = false;
            return jLogin;
        }

        List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("company", company));
        params.add(new BasicNameValuePair("account", account));
        params.add(new BasicNameValuePair("password", password));
        params.add(new BasicNameValuePair("pushType", PrjCfg.getLCType(MainApp.LC_SERVER, PrjCfg.CUSTOMER)));
        params.add(new BasicNameValuePair("pushId", MainApp.LC_PUSH_ID));
        params.add(new BasicNameValuePair("lang", Utils.loadPrefs(context, "Language")));
        if (force) {
            params.add(new BasicNameValuePair("force", "1"));
        }
        jLogin = JSONReq.send(context, "POST", PrjCfg.CLOUD_URL + "/api/login", params);
        int code = jLogin.getInt("code");
        if (code == 200) { // Ok
            String parentId = jLogin.has("parentId") ? jLogin.getString("parentId") : "0";
            if(!parentId.equals("0")) { // subsidiary
                Log.e(Dbg._TAG_(), "IS Subsidiary company! Parent ID:" + parentId);
                Utils.savePrefs(context, "IsSubsidiary", "1");
                Utils.savePrefs(context, "ParentID", parentId);
            } else { // You're Parent company
                Utils.savePrefs(context, "IsSubsidiary", "0");
                String subCompID = Utils.loadPrefs(context, "SubCompID", "0");
                if(!subCompID.equals("0")) { // But you login in subcompany before, login subcompany automatically
                    JSONObject jObject = JSONReq.send(context, "PUT", PrjCfg.CLOUD_URL + "/api/company/login/" + subCompID);
                    if(jObject == null || jObject.getInt("code") != 200) {
                        Log.e(Dbg._TAG_(), "Failed to login " + subCompID);
                        Utils.savePrefs(context, "SubCompID", 0);
                        Utils.savePrefs(context, "Password", "");
                        jLogin.put("code", 401);
                    } else {
                        Log.e(Dbg._TAG_(), "Succeed to login " + subCompID);
                    }
                }
            }
        } else if (code == 401) { //Need Auth
            Log.e(Dbg._TAG_(), "Need Auth!");
        } else if (code == 504) {
            Log.e(Dbg._TAG_(), "Can't connect to Cloud !");
        } else if (code == 503) {
            Log.e(Dbg._TAG_(), "Server maintenance !");
        } else {
            Log.e(Dbg._TAG_(), "Authentication failed !!");
            Utils.savePrefs(context, "Password", "");
        }
        logining = false;
        return jLogin;
    }

    public static void saveCookies(Context context, Response response) {
        String rsltCookie = "";
        boolean found = false;
        List<String> cookies = response.headers("Set-Cookie");
        for(String cookie: cookies) {
//            Log.e(Dbg._TAG_(), "Cookie: " + cookie);
            String[] cookieValues = cookie.split(";");
            rsltCookie = rsltCookie.equals("") ? cookieValues[0].trim() : (rsltCookie + "; " + cookieValues[0].trim());
            if(cookie.indexOf("KCloud") >= 0 || cookie.indexOf("Datalogger") >= 0) {
                found = true;
                break;
            }
        }
        if(found) {
            //Log.e(Dbg._TAG_(), "Cookie: " + rsltCookie);
            Utils.savePrefs(context, "Cookie", rsltCookie);
        } else {
            //Log.e(Dbg._TAG_(), "Error: Can't find cookie KCloud!!");
        }
    }

    public static String encode(String url) {
        String result = url;
        try {
            result = URLEncoder.encode(url, "UTF-8")
                    .replaceAll("\\+", "%20")
                    .replaceAll("\\%21", "!")
                    .replaceAll("\\%27", "'")
                    .replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")")
                    .replaceAll("\\%7E", "~");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void saveLanguage(final Context context, String language) {
        String curAccount = Utils.loadPrefs(context, "Account");
        MultipartBuilder entityBuilder = new MultipartBuilder();
        entityBuilder.type(MultipartBuilder.FORM);
        entityBuilder.addFormDataPart("account", curAccount);
        entityBuilder.addFormDataPart("lang", language);
        try {
            JSONReq.multipart(context, "PUT", PrjCfg.CLOUD_URL + "/api/user/lang", entityBuilder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void savePushType(final Context context, String pushType) {
        String curAccount = Utils.loadPrefs(context, "Account");
        MultipartBuilder entityBuilder = new MultipartBuilder();
        entityBuilder.type(MultipartBuilder.FORM);
        entityBuilder.addFormDataPart("account", curAccount);
        entityBuilder.addFormDataPart("pushType", pushType);
        try {
            JSONReq.multipart(context, "PUT", PrjCfg.CLOUD_URL + "/api/user/pushType", entityBuilder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void logout(final Context context) {
        try {
            List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();
            params.add(new BasicNameValuePair("pushId", MainApp.LC_PUSH_ID));
            JSONObject jObject = JSONReq.send(context, "POST", PrjCfg.CLOUD_URL + "/api/logout", params);
            if (jObject == null || jObject.getInt("code") != 200) {
                Log.e(Dbg._TAG_(), "Account logout failed!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Clear authentication data
            Utils.savePrefs(context, "Cookie", "");
            Utils.savePrefs(context, "Password", "");
            Utils.savePrefs(context, "SubCompID", 0);

            Intent intent = new Intent(context, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            //((Activity) context).finish();

            // Unregister from parent company channel
            String parentId = Utils.loadPrefs(context, "ParentID", "0");
            Utils.savePrefs(context, "ParentID", 0);
            if(!parentId.equals("0")) { // subsidiary
                PushService.unsubscribe(context, "pa" + parentId);
            }

            // Unregister from company channel
            String companyId = Utils.loadPrefs(context, "CompanyID", "0");
            PushService.unsubscribe(context, "ca" + companyId);
            Utils.savePrefs(context, "CompanyID", 0);
        }
    }

    public static void download(Context context, String urlStr, String dstPath, String dstName) throws Exception {
        Log.e(Dbg._TAG_(), "Download from : " + urlStr + ", dstName: " + dstName);
        String fileName;
        if(dstName != null && !dstName.equals("")) {
            fileName = dstName;
        } else { // extracts file name from URL
            fileName = urlStr.substring(urlStr.lastIndexOf("/") + 1, urlStr.length());
        }
        isFileExport = true;
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(urlStr));
        request.setVisibleInDownloadsUi(true);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        request.addRequestHeader("Cookie", Utils.loadPrefs(context, "Cookie"));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        dm.enqueue(request);
    }

    public static byte[] tcpRequest(String host, byte[] reqData) throws Exception {
        Socket socket = null;
        DataInputStream din = null;
        DataOutputStream dout = null;
        try {
            socket = new Socket(host, PrjCfg.DEV_CONNECT_PORT);
            socket.setSoTimeout(PrjCfg.TCP_SO_TIMEOUT);
            dout = new DataOutputStream(socket.getOutputStream());
            dout.write(reqData);

            byte[] inputData = new byte[1280];
            din = new DataInputStream(socket.getInputStream());
            int dataLen = din.read(inputData);
            if (dataLen <= 0) {
                Log.e(Dbg._TAG_(), "The length of packet is less than zero!");
                return null;
            }

            int length = (inputData[9] & 0xff) + ((inputData[8] & 0xff) << 8);
            byte[] resData = new byte[length + 14];
            System.arraycopy(inputData, 0, resData, 0, resData.length);
            return resData;
        } finally {
            if(din != null) {
                din.close();
            }
            if(dout != null) {
                dout.close();
            }
            if(socket != null) {
                socket.close();
            }
        }
    }

    public static void checkNewApp(final Context context, final MHandler mHandler) {
        final AsyncTask<Void, Void, Void> mNewAppTask = new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                try {
                    String url = PrjCfg.APP_CHK_LINK;
                    JSONObject jReleases = JSONReq.send(context, "GET", url);
                    JSONArray jChangesets = jReleases.getJSONArray("changesets");
                    JSONObject jChangeset = jChangesets.getJSONObject(0);
                    String[] jMsgs = jChangeset.getString("message").trim().split("-"); // "app-debug-ver-XXX.apk "
                    //Log.e(Dbg._TAG_(), "jmsgs: " + jReleases.toString());

                    String webVerStr = jMsgs[jMsgs.length-1].split("\\.")[0];
                    int webVer = Integer.parseInt(webVerStr);
                    PackageInfo pinfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                    String igVerCode = Utils.loadPrefs(context, "AppIgnoreVerCode");

                    Log.e(Dbg._TAG_(), "Latest version: " + webVerStr + "; Now version: " + pinfo.versionCode + "; Ignore version: " + igVerCode);
                    Utils.savePrefs(context, "AppLatestVerCode", webVer);
                    Utils.savePrefs(context, "AppCurVerCode", pinfo.versionCode);
                    if(igVerCode != null && igVerCode.equalsIgnoreCase(webVerStr)) {
                        return null;
                    }
                    if(webVer > pinfo.versionCode) {
                        MHandler.exec(mHandler, MHandler.UPDATE_APP, context.getString(R.string.update_app_msg));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            protected void onPostExecute(Void result) {
            }
        };
        if(MainApp.FROM_GOOGLE_PLAY) {
            return; // don't check if the APP is from Google play
        }
        mNewAppTask.execute(null, null, null);
    }

    public static void installNewApp(final Context context) {
        final AsyncTask<Void, Void, Void> mNewAppTask = new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                try {
                    JSONObject jReleases = JSONReq.send(context, "GET", PrjCfg.APP_CHK_LINK);
                    JSONArray jChangesets = jReleases.getJSONArray("changesets");
                    JSONObject jChangeset = jChangesets.getJSONObject(0);

                    JSONArray jChgFiles = jChangeset.getJSONArray("files");
                    JSONObject jChgFile = jChgFiles.getJSONObject(0);
                    String apkName = jChgFile.getString("file");
                    Log.e(Dbg._TAG_(), "Download apk: " + apkName);
                    newApkFile = new File(Environment.getExternalStorageDirectory() + "/download/" + apkName);
                    if(newApkFile.exists()) {
                        newApkFile.delete();
                    }

                    String rawNode = jChangeset.getString("raw_node");
                    String branch = jChangeset.getString("branch");
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(PrjCfg.APP_RAW_LINK + rawNode + "/" + apkName + "?at=" + branch));
                    request.setVisibleInDownloadsUi(true);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, apkName);
                    request.setMimeType("application/vnd.android.package-archive");
                    DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                    dm.enqueue(request);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            protected void onPostExecute(Void result) {
            }
        };

        mNewAppTask.execute(null, null, null);
    }

    public static BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(isFileExport) { // profile exporting
                isFileExport = false;
            } else if(newApkFile == null) {
                Log.e(Dbg._TAG_(), "Cannot find new APK file!");
            } else if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                Intent promptInstall = new Intent(Intent.ACTION_VIEW);
                promptInstall.setDataAndType(Uri.fromFile(newApkFile), "application/vnd.android.package-archive");
                promptInstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(promptInstall);
            }
        }
    };
}
