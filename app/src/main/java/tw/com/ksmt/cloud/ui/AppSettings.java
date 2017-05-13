package tw.com.ksmt.cloud.ui;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.avos.avoscloud.PushService;

import java.util.Locale;

import tw.com.ksmt.cloud.BuildConfig;
import tw.com.ksmt.cloud.MainApp;
import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Register;
import tw.com.ksmt.cloud.libs.DateUtils;
import tw.com.ksmt.cloud.libs.Dbg;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.StrUtils;
import tw.com.ksmt.cloud.libs.Utils;
import tw.com.ksmt.cloud.libs.WebUtils;

public class AppSettings extends PreferenceActivity {
    private final Context context = AppSettings.this;
    private static String lastLocale = null;
    private static int numClick = 0;
    private static ProgressDialog mDialog;

//    private String getArrayKey(int keyId, int valId, String target) {
//        String result = "";
//        Resources res = getResources();
//        String[] keys = res.getStringArray(keyId);
//        String[] vals = res.getStringArray(valId);
//        for(int i = 0; i < vals.length; i++) {
//            if(vals[i].equals(target) && keys[i] != null) {
//                return keys[i];
//            }
//        }
//        return result;
//    }

    private void strtSignInActivity(String servAddr) {
        mDialog = Kdialog.getProgress(context);
        Log.e(Dbg._TAG_(), "Set to another Cloud Server  -> " + servAddr);
        Utils.savePrefs(context, "CLOUD_URL_0", servAddr);
        new Thread(new Runnable() {
            @Override
            public void run() {
                WebUtils.logout(context);
            }
        }).start();
    }

    private void postLangConf(final String language) {
        // Pass language to server
        String cookie = Utils.loadPrefs(context, "Cookie");
        if(cookie != null && !cookie.equals("")) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    WebUtils.saveLanguage(context, language);
                }
            }).start();
        }
    }

    private void postPushType(final String pushType) {
        // Pass language to server
        new Thread(new Runnable() {
            @Override
            public void run() {
                WebUtils.savePushType(context, pushType);
            }
        }).start();
    }

    @Override
    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        registerReceiver(WebUtils.downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        addPreferencesFromResource(R.xml.app_settings);
        getListView().setBackgroundColor(Color.WHITE);

        // refresh activity if changed
        String lang = Utils.loadPrefs(context, "Language");
        ListPreference langPref = (ListPreference)findPreference("prefLangSettings");
        langPref.setValue(lang);
        langPref.setSummary(Utils.getArrayKey(context, R.array.langSelectKey, R.array.langSelectVal, lang));
        langPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.e(Dbg._TAG_(), "Reload Language settings -> " + newValue);
                postLangConf((String) newValue);
                Utils.savePrefs(context, "Language", (String) newValue);
                Register.clearTypes(); // make sure the reg type can be reloaded!
                setupLang(context);
                Utils.restartActivity(context);
                return true;
            }
        });

        final String sortRegList = Utils.loadPrefs(context, "SORT_REG_LIST");
        final ListPreference sortRegPref = (ListPreference)findPreference("prefSortRegList");
        sortRegPref.setValue(sortRegList);
        sortRegPref.setSummary(Utils.getArrayKey(context, R.array.sortRegKey, R.array.sortRegVal, sortRegList));
        sortRegPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.e(Dbg._TAG_(), "Sort Register List -> " + newValue);
                PrjCfg.SORT_REG_LIST = (String) newValue;
                Utils.savePrefs(context, "SORT_REG_LIST", PrjCfg.SORT_REG_LIST);
                sortRegPref.setValue(PrjCfg.SORT_REG_LIST);
                sortRegPref.setSummary(Utils.getArrayKey(context, R.array.sortRegKey, R.array.sortRegVal, PrjCfg.SORT_REG_LIST));
                return true;
            }
        });

        // Show build datetime
        Preference prefAppVer = (Preference)findPreference("prefAppVer");
        prefAppVer.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if(numClick++ > 6 && PrjCfg.USER_MODE == PrjCfg.MODE_ADMIN) {
                    PrjCfg.USER_MODE = PrjCfg.MODE_KSMT_DEBUG;
                    Kdialog.getDefInfoDialog(context).setCancelable(false).setMessage("Enter KSMT debug mode!").show();
                }
                return true;
            }
        });
        try {
            String appBuildDate = DateUtils.getDate("yyyy/MM/dd hh:mm", BuildConfig.buildTime);
            PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            prefAppVer.setSummary(PrjCfg.CUSTOMER + " " + pinfo.versionName + "." + pinfo.versionCode + " (" + appBuildDate + ")");
        } catch (Exception e) {
            e.printStackTrace();
        }

        final CheckBoxPreference prefReceiveNotify = (CheckBoxPreference)findPreference("prefReceiveNotify");
        prefReceiveNotify.setChecked(Utils.loadPrefsBool(context, "NOTIFICATION"));
        prefReceiveNotify.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Boolean isClicked = (Boolean) newValue;
                ((CheckBoxPreference)preference).setChecked(isClicked);
                Utils.savePrefs(context, "NOTIFICATION", isClicked.toString());
                PrjCfg.NOTIFICATION = isClicked.toString();
                return false;
            }
        });

        final CheckBoxPreference prefNotifySound = (CheckBoxPreference)findPreference("prefNotifySound");
        prefNotifySound.setChecked(Utils.loadPrefsBool(context, "NOTIFICATION_SOUND"));
        prefNotifySound.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Boolean isClicked = (Boolean) newValue;
                ((CheckBoxPreference)preference).setChecked(isClicked);
                Utils.savePrefs(context, "NOTIFICATION_SOUND", isClicked.toString());
                PrjCfg.NOTIFICATION_SOUND = isClicked.toString();
                return false;
            }
        });

        final CheckBoxPreference prefNotifyVibration = (CheckBoxPreference)findPreference("prefNotifyVibration");
        prefNotifyVibration.setChecked(Utils.loadPrefsBool(context, "NOTIFICATION_VIBRATION"));
        prefNotifyVibration.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Boolean isClicked = (Boolean) newValue;
                ((CheckBoxPreference)preference).setChecked(isClicked);
                Utils.savePrefs(context, "NOTIFICATION_VIBRATION", isClicked.toString());
                PrjCfg.NOTIFICATION_VIBRATION = isClicked.toString();
                return false;
            }
        });

        // Cloud URL
        final String cloudUrl = Utils.loadPrefs(context, "CLOUD_URL_0");
        final ListPreference prefCloudUrl = (ListPreference) findPreference("prefCloudUrl");
        String[] cloudUrlKey = new String[2];
        String[] cloudUrlVal = new String[2];
        cloudUrlKey[0] = getString(R.string.official_cloud_url);
        cloudUrlKey[1] = getString(R.string.private_cloud_url);
        cloudUrlVal[1] = "private";
        if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_YATEC)) {
            cloudUrlVal[0] = PrjCfg.CLOUD_LINK_YATEC;
        } else if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_LILU)) {
            cloudUrlVal[0] = PrjCfg.CLOUD_LINK_LILU;
        } else if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_HYEC)) {
            cloudUrlVal[0] = PrjCfg.CLOUD_LINK_HYEC;
        } else {
            cloudUrlVal[0] = PrjCfg.CLOUD_LINK_KSMT;
        }
        prefCloudUrl.setEntries(cloudUrlKey);
        prefCloudUrl.setEntryValues(cloudUrlVal);

        final String privUrl = Utils.loadPrefs(context, "PrivServAddr");
        if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_YATEC) && cloudUrl.equals(PrjCfg.CLOUD_LINK_YATEC)) {
            prefCloudUrl.setValue(cloudUrl);
            prefCloudUrl.setSummary(getString(R.string.official_cloud_url));
        } else if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_HYEC) && cloudUrl.equals(PrjCfg.CLOUD_LINK_HYEC)) {
            prefCloudUrl.setValue(cloudUrl);
            prefCloudUrl.setSummary(getString(R.string.official_cloud_url));
        } else if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_LILU) && cloudUrl.equals(PrjCfg.CLOUD_LINK_LILU)) {
            prefCloudUrl.setValue(cloudUrl);
            prefCloudUrl.setSummary(getString(R.string.official_cloud_url));
        } else if(cloudUrl.equals(PrjCfg.CLOUD_LINK_KSMT)) {
            prefCloudUrl.setValue(cloudUrl);
            prefCloudUrl.setSummary(getString(R.string.official_cloud_url));
        } else if(cloudUrl.equals(privUrl)) {
            prefCloudUrl.setValue("private");
            prefCloudUrl.setSummary(privUrl);
        } else {
            prefCloudUrl.setValue(cloudUrl);
            prefCloudUrl.setSummary(cloudUrl);
        }
        prefCloudUrl.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String newCloudUrl = (String) newValue;
                if(newCloudUrl.equals("private")) {
                    final LayoutInflater inflater = LayoutInflater.from(context);
                    final View layout = inflater.inflate(R.layout.input_url, null);
                    final EditText servAddr = (EditText) layout.findViewById(R.id.text);
                    servAddr.setText(privUrl);
                    servAddr.setHint(getString(R.string.private_url_hint));
                    servAddr.setFilters(Utils.arrayMerge(servAddr.getFilters(), new InputFilter[]{Utils.EMOJI_FILTER}));

                    final AlertDialog dialog = Kdialog.getDefInputDialog(context).create();
                    dialog.setView(layout);
                    dialog.setIcon(R.drawable.ic_cloud_server);
                    dialog.setTitle(getString(R.string.private_cloud_url));
                    dialog.setCancelable(false);
                    dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialogIface) {
                            Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                            btn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Boolean noError = true;
                                    String servAddrStr = servAddr.getText().toString().trim().toLowerCase();
                                    if (!StrUtils.validateInput(StrUtils.IN_TYPE_NONE_EMPTY, servAddrStr)) {
                                        noError = false;
                                        servAddr.setError(getString(R.string.err_msg_empty));
                                    } else if(servAddrStr.matches("^https://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
                                        noError = false;
                                        servAddr.setError(getString(R.string.err_msg_invalid_str));
                                    }
                                    if (noError) {
                                        dialog.dismiss();
                                        if(servAddrStr.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
                                            servAddrStr = "http://" + servAddrStr;
                                        } else if(!servAddrStr.matches("^(http|https).*")) {
                                            servAddrStr = "https://" + servAddrStr;
                                        }
                                        Utils.savePrefs(context, "PrivServAddr", servAddrStr);
                                        strtSignInActivity(servAddrStr);
                                    }
                                }
                            });
                        }
                    });
                    dialog.show();
                } else {
                    if(!cloudUrl.equals(newCloudUrl)) {
                        strtSignInActivity(newCloudUrl);
                    }
                }
                return true;
            }
        });

        // User interface
        final ListPreference prefInterface = (ListPreference) findPreference("prefInterface");
        prefInterface.setValue(Utils.loadPrefs(context, "CUSTOMER"));
        prefInterface.setSummary(Utils.loadPrefs(context, "CUSTOMER"));
        prefInterface.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String newInterface = (String) newValue;
                postPushType(PrjCfg.getLCType(MainApp.LC_SERVER, newInterface));
                prefInterface.setValue(newInterface);
                prefInterface.setSummary(newInterface);
                PrjCfg.CUSTOMER = newInterface;
                Utils.savePrefs(context, "CUSTOMER", newInterface);
                PrjCfg.loadSettings(PrjCfg.CUSTOMER);

                // Reset LeanCloud connection
                context.stopService(new Intent(context, PushService.class));
                MainApp.setPushService(context, newInterface);
                return true;
            }
        });
        if(PrjCfg.USER_MODE != PrjCfg.MODE_KSMT_DEBUG) {
            PreferenceCategory mSysPref = (PreferenceCategory) findPreference("CategorySysPref");
            mSysPref.removePreference(prefInterface);
        }

        Preference prefInstNewApp = (Preference)findPreference("prefInstNewApp");
        String curVerCode = Utils.loadPrefs(context, "AppCurVerCode");
        String latestVerCode = Utils.loadPrefs(context, "AppLatestVerCode");
        if(MainApp.FROM_GOOGLE_PLAY) { // Upgrade App By Google play
            PreferenceCategory mSysPref = (PreferenceCategory) findPreference("CategorySysPref");
            mSysPref.removePreference(prefInstNewApp);
        } else if(latestVerCode == null || curVerCode.equalsIgnoreCase(latestVerCode)) { // already latest
            PreferenceCategory mSysPref = (PreferenceCategory) findPreference("CategorySysPref");
            mSysPref.removePreference(prefInstNewApp);
        } else { // show update button
            prefInstNewApp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Kdialog.getDefInfoDialog(context)
                            .setMessage(context.getString(R.string.update_app_msg))
                            .setPositiveButton(context.getString(R.string.confirm), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Utils.savePrefs(context, "AppIgnoreVerCode", "");
                                    mDialog = Kdialog.getProgress(context, context.getString(R.string.updateding));
                                    WebUtils.installNewApp(context);
                                }
                            }).show();

                    return true;
                }
            });
        }

        // Setup language
        if(!Utils.loadPrefs(context, "Language", lastLocale).equals(lastLocale)) {
            lastLocale = Utils.loadPrefs(context, "Language");
            Utils.restartActivity(context);
            return;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        LinearLayout root = (LinearLayout)findViewById(android.R.id.list).getParent().getParent().getParent();
        Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.preference_toolbar, root, false);
        bar.setTitle(getString(R.string.app_settings));
        bar.setTitleTextColor(Color.WHITE);
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        root.addView(bar, 0); // insert at top
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AppSettings.reload(context);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mDialog != null) {
            mDialog.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(WebUtils.downloadReceiver);
    }

    public static String setupLang(final Context context) {
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();

        Locale curLocale = Locale.US; // default
        final String language = Utils.loadPrefs(context, "Language", Locale.US.toString());
        if (language.equals(Locale.TRADITIONAL_CHINESE.toString())) {
            curLocale = Locale.TRADITIONAL_CHINESE;
        }
//        Log.e(Dbg._TAG_(), "set locale " + language);

        Configuration conf = res.getConfiguration();
        conf.locale = curLocale;
        res.updateConfiguration(conf, dm);
        //activity.onConfigurationChanged(conf);
        return language;
    }

    public static void resetDefault(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("FirstTimeInit", "false");
        editor.putString("Language", Locale.getDefault().toString());
        editor.putString("CLOUD_URL_0", PrjCfg.CLOUD_URL);
        editor.putString("NOTIFICATION", PrjCfg.NOTIFICATION);
        editor.putString("NOTIFICATION_SOUND", PrjCfg.NOTIFICATION_SOUND);
        editor.putString("NOTIFICATION_VIBRATION", PrjCfg.NOTIFICATION_VIBRATION);
        editor.putString("CUSTOMER", PrjCfg.CUSTOMER);
        editor.putString("SORT_REG_LIST", PrjCfg.SORT_REG_LIST);
       editor.commit();
        setupLang(context);
    }

    public static void reload(Context context) {
        // First pollTime use APP, do some initialization
        if (Utils.loadPrefs(context, "FirstTimeInit", "true").equals("true")) {
            Log.e(Dbg._TAG_(), "Initialization APP settings !!");
            resetDefault(context);
            return;
        }

        String cloudUrl = Utils.loadPrefs(context, "CLOUD_URL_0");
        if (cloudUrl == null || cloudUrl.equals("")) {
            Log.e(Dbg._TAG_(), "Cloud URL address is missing! Re-initialization APP settings !!");
            resetDefault(context);
            return;
        }

        //Log.e(Dbg._TAG_(), "Reload APP settings Push=" + PrjCfg.NOTIFICATION);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        PrjCfg.CLOUD_URL = sharedPreferences.getString("CLOUD_URL_0", PrjCfg.CLOUD_URL);
        PrjCfg.NOTIFICATION = sharedPreferences.getString("NOTIFICATION", PrjCfg.NOTIFICATION);
        PrjCfg.NOTIFICATION_SOUND = sharedPreferences.getString("NOTIFICATION_SOUND", PrjCfg.NOTIFICATION_SOUND);
        PrjCfg.NOTIFICATION_VIBRATION = sharedPreferences.getString("NOTIFICATION_VIBRATION", PrjCfg.NOTIFICATION_VIBRATION);
        PrjCfg.CUSTOMER = sharedPreferences.getString("CUSTOMER", PrjCfg.CUSTOMER);
        PrjCfg.SORT_REG_LIST = sharedPreferences.getString("SORT_REG_LIST", PrjCfg.SORT_REG_LIST);
        PrjCfg.loadSettings(PrjCfg.CUSTOMER);
        setupLang(context);
    }
}
