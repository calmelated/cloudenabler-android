package tw.com.ksmt.cloud.libs;

import android.content.Context;
import android.util.Log;

import com.avos.avoscloud.okhttp.FormEncodingBuilder;
import com.avos.avoscloud.okhttp.MediaType;
import com.avos.avoscloud.okhttp.OkHttpClient;
import com.avos.avoscloud.okhttp.Request;
import com.avos.avoscloud.okhttp.RequestBody;
import com.avos.avoscloud.okhttp.Response;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import tw.com.ksmt.cloud.MainApp;
import tw.com.ksmt.cloud.PrjCfg;

public final class JSONReq {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static OkHttpClient okHttpClient = new OkHttpClient();

    private static JSONObject httpReq(Context context, String method, String url, List<BasicNameValuePair> params) {
        JSONObject jObject = null;
        String cookie = null;
        if(url.matches(PrjCfg.CLOUD_URL + ".*")) {
            cookie = Utils.loadPrefs(context, "Cookie");
        }
        try {
            Request.Builder builder = new Request.Builder();
            builder.addHeader("Content-Type", "charset=utf-8");
            if(cookie != null) {
                builder = builder.addHeader("Cookie", cookie);
            }

            Request request = null;
            if (method.equals("GET") && params == null) {
                request = builder.url(url).get().build();
            } else if (method.equals("GET") && params != null) { // request method is GET
                String paramString = URLEncodedUtils.format(params, "UTF-8");
                request = builder.url(url + paramString).get().build();
            } else if (method.equals("POST")) { // request method is POST
                FormEncodingBuilder formEncodingBuilder = new FormEncodingBuilder();
                if(params != null) {
                    for (BasicNameValuePair pair : params) {
                        formEncodingBuilder.addEncoded(pair.getName(), pair.getValue());
                    }
                }
                request = builder.url(url).post(formEncodingBuilder.build()).build();
            } else if (method.equals("PUT")) { // request method is PUT
                FormEncodingBuilder formEncodingBuilder = new FormEncodingBuilder();
                if(params != null) {
                    for (BasicNameValuePair pair : params) {
                        formEncodingBuilder.addEncoded(pair.getName(), pair.getValue());
                    }
                }
                request = builder.url(url).put(formEncodingBuilder.build()).build();
            } else if (method.equals("PUT-JSON") && params != null) { // request method is PUT-JSON
                builder.addHeader("Accept", "application/json");
                builder.addHeader("Content-type", "application/json");
                RequestBody body = RequestBody.create(JSON, params.get(0).getValue());
                request = builder.url(url).put(body).build();
            } else if (method.equals("DELETE")) { // request method is DELETE
                request = builder.url(url).delete().build();
            } else {
                Log.e(Dbg._TAG_(), "Non-support method " + method + ", params " + params);
                throw new Exception();
            }

            // Send HTTP/2 Request
            Response response = okHttpClient.newCall(request).execute();
            int statCode = response.code();
            String resStr = response.body().string();
            jObject = new JSONObject(resStr);
            jObject.put("code", statCode);
            if(url.matches(PrjCfg.CLOUD_URL + ".*")) {
                MainApp.INET_CONNECTED = true;
                if(statCode == 200 || (statCode == 401 && (jObject.has("activate") && !jObject.getBoolean("activate")))) {
                    WebUtils.saveCookies(context, response);
                }
            }
        } catch (Exception e) {
            jObject = new JSONObject("{code: 504}");
            MainApp.INET_CONNECTED = false;
            e.printStackTrace();
        } finally {
            return jObject;
        }
    }

    public static JSONObject multipart(Context context, String method, String url, RequestBody requestBody) {
        JSONObject jObject = null;
        String cookie = null;
        if(url.matches(PrjCfg.CLOUD_URL + ".*")) {
            cookie = Utils.loadPrefs(context, "Cookie");
        }
        try {
            Request.Builder builder = new Request.Builder();
            if(cookie != null) {
                builder = builder.addHeader("Cookie", cookie);
            }

            Request request = null;
            if(method.equals("POST")) {
                request = builder.url(url).post(requestBody).build();
            } else if(method.equals("PUT")) {
                request = builder.url(url).put(requestBody).build();
            }

            okHttpClient.setReadTimeout(600, TimeUnit.SECONDS);
            Response response = okHttpClient.newCall(request).execute();
            int statCode = response.code();
            String resStr = response.body().string();
            jObject = new JSONObject(resStr);
            jObject.put("code", statCode);
            if(url.matches(PrjCfg.CLOUD_URL + ".*")) {
                MainApp.INET_CONNECTED = true;
                if(statCode == 200) {
                    WebUtils.saveCookies(context, response);
                }
            }
        } catch (Exception e) {
            jObject = new JSONObject("{code: 504}");
            MainApp.INET_CONNECTED = false;
            e.printStackTrace();
        } finally {
            return jObject;
        }
    }

    public static final JSONObject send(Context context, String method, String url) {
        return send(context, method, url, null);
    }

    public static final JSONObject send(Context context, String method, String url, List<BasicNameValuePair> params) {
        JSONObject jObject = null;
        try {
            jObject = httpReq(context, method, url, params);
            int statusCode = jObject.getInt("code");
            if(statusCode == 200) { // save cookies
                MainApp.SERV_MAINT = 0;
            } else if(statusCode == 400) { // authentication failed
                if(!jObject.has("desc")) {
                    return jObject;
                }
                String desc = jObject.getString("desc").toLowerCase();
                if(desc.indexOf("authenication failed") > 0) {
                    Log.e(Dbg._TAG_(), "Bad request! Req: " + url + " Params: " + params + ", Res: " + jObject.toString());
                    Utils.savePrefs(context, "Password", "");
                }
                return jObject;
            } else if(statusCode == 401) { // session expired
                if(jObject.has("force") && jObject.getBoolean("force")) {
                    Log.e(Dbg._TAG_(), "You have been signed out!");
                    Utils.savePrefs(context, "Password", "");
                    return jObject;
                } else if (jObject.has("activate") && !jObject.getBoolean("activate")) { // No Auth & No activate
                    Log.e(Dbg._TAG_(), "You have been deactivated !");
                    Utils.savePrefs(context, "Password", "");
                    return jObject;
                }
                // End of trial period -> force logout
                boolean isTrialUser = Utils.loadPrefs(context, "TrialUser", "0").equals("1") ? true : false ;
                if(isTrialUser) {
                    Log.e(Dbg._TAG_(), "End of Trial period!");
                    Utils.savePrefs(context, "Password", "");
                    return jObject;
                }

                // login again if necessary
                if (WebUtils.login(context, false) != null) {
                    jObject = httpReq(context, method, url, params);
                } else {
                    // re-login failed! (Other thread is logging)
                }
            } else if(statusCode == 503 && url.contains(PrjCfg.CLOUD_URL)) { // our service unavailable, not bitbucket
                long curTime = new Date().getTime() / 1000;
                if(jObject.has("backTime")) {
                    long backTime = jObject.getLong("backTime");
                    MainApp.SERV_MAINT = (backTime > curTime) ? backTime : curTime + 10;
                } else {
                    MainApp.SERV_MAINT = (MainApp.SERV_MAINT > 2) ? (curTime + 10) : (MainApp.SERV_MAINT + 1);
//                    PrjCfg.SERV_MAINT = curTime + 10;
                    Log.e(Dbg._TAG_(), "Server 503 Error!! " + MainApp.SERV_MAINT);
                }
            } else {
                // ... other case
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return jObject;
        }
    }

    public static JSONArray sort(JSONArray array, Comparator c) {
        List asList = new ArrayList(array.length());
        for (int i = 0; i < array.length(); i++) {
            asList.add(array.opt(i));
        }
        Collections.sort(asList, c);
        JSONArray res = new JSONArray();
        for (Object o : asList) {
            res.put(o);
        }
        return res;
    }
}