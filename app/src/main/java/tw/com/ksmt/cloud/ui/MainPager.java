package tw.com.ksmt.cloud.ui;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;

import com.astuetz.PagerSlidingTabStrip;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.MainApp;
import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.libs.Dbg;
import tw.com.ksmt.cloud.libs.JSONReq;
import tw.com.ksmt.cloud.libs.Kdialog;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.Utils;
import tw.com.ksmt.cloud.libs.WebUtils;

public class MainPager extends ActionBarActivity {
    private PagerSlidingTabStrip tabs;
    private MainPagerAdapter adapter;
    private ViewPager pager;
    private DisplayMetrics dm;
    private Fragment[] fragments = new Fragment[3];

    private Context context = MainPager.this;
    private static String lastLocale;
    private static String lastSubCompID;
    private static String lastCustomer;
    private ProgressDialog mDialog;
    private AlertDialog newAppDialog;
    private MsgHandler mHandler;
    private Timer pollingTimer;
    private ActionBar actionBar;
    private boolean isTrialUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_pager);
        AppSettings.reload(context);
        registerReceiver(WebUtils.downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        isTrialUser = Utils.loadPrefs(context, "TrialUser", "0").equals("1") ? true : false ;
        String accountStr = (isTrialUser) ? getString(R.string.trial_account) : getString(R.string.account);
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        actionBar.setSubtitle(Html.fromHtml("<small><small><i>" + accountStr + ": " + Utils.loadPrefs(context, "UserName") + "</i></small></small>"));
        mHandler = new MsgHandler(this);

        String admin = Utils.loadPrefs(context, "Admin");
        if (Utils.loadPrefs(context, "Password", "").equals("") || admin == null) {
            startLogin();
            return;
        } else if(MainApp.SERV_MAINT > (new Date().getTime() / 1000)) {
            MHandler.exec(mHandler, MHandler.SERV_MAINT);
            return;
        }

        // Determine Mode
        int _admin = Integer.valueOf(admin);
        if(_admin == PrjCfg.MODE_USER) {
            PrjCfg.USER_MODE = PrjCfg.MODE_USER;
        } else { // Admin
            PrjCfg.USER_MODE = (PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) ? PrjCfg.MODE_KSMT_DEBUG : PrjCfg.MODE_ADMIN;
        }

        dm = getResources().getDisplayMetrics();
        pager = (ViewPager) findViewById(R.id.pager);
        adapter = new MainPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
        setTabsValue();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AppSettings.reload(context);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mDialog != null) {
            mDialog.cancel();
        }
        if (pollingTimer != null) {
            pollingTimer.cancel();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // check language setting
        String lang = Utils.loadPrefs(context, "Language", "");
        if(lastLocale == null) { // first pollTime
            lastLocale = Utils.loadPrefs(context, "Language");
        } else if(!lang.equals(lastLocale)) {
            Log.e(Dbg._TAG_(), "Language changed!");
            lastLocale = lang;
            Utils.restartActivity(context);
            return;
        }

        // check if login subsidiary or not
        String subCompID = Utils.loadPrefs(context, "SubCompID", "0");
        if(lastSubCompID == null) { // first time
            lastSubCompID = Utils.loadPrefs(context, "SubCompID", "0");
        } else if(!subCompID.equals(lastSubCompID)) {
            Log.e(Dbg._TAG_(), "Subsidiary ID changed!");
            lastSubCompID = subCompID;
            Utils.restartActivity(context);
            return;
        }

        // check if change UI
        if(lastCustomer == null) { // first time
            lastCustomer = PrjCfg.CUSTOMER;
        } else if(!PrjCfg.CUSTOMER.equals(lastCustomer)) {
            Log.e(Dbg._TAG_(), "Customer changed! " + lastCustomer + " -> " + PrjCfg.CUSTOMER);
            lastCustomer = PrjCfg.CUSTOMER;
            Utils.restartActivity(context);
            return;
        }

        // check authentication
        if (Utils.loadPrefs(context, "Password", "").equals("")) {
            startLogin();
            return;
        } else if(MainApp.SERV_MAINT > (new Date().getTime() / 1000)) {
            MHandler.exec(mHandler, MHandler.SERV_MAINT);
            return;
        }
        pollingTimer = new Timer();
        pollingTimer.schedule(new PollingTimerTask(), 0, PrjCfg.MAIN_PAGE_POLLING);

        // check lastest version
        WebUtils.checkNewApp(context, mHandler);

        // Debug Mode
        if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
            actionBar.setTitle("Debug Mode");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(WebUtils.downloadReceiver);
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        super.onContextMenuClosed(menu);
        Fragment fragment = adapter.getItem(pager.getCurrentItem());
        if(fragment instanceof CloudDevFragment) {
            ((CloudDevFragment)fragment).onContextMenuClosed(menu);
        } else if(fragment instanceof LocalDevFragment) {
            ((LocalDevFragment)fragment).onContextMenuClosed(menu);
        } else if(fragment instanceof GroupFragment) {
            ((GroupFragment)fragment).onContextMenuClosed(menu);
        }
    }

    private void setTabsValue() {
        tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        tabs.setShouldExpand(true);
        //tabs.setDividerColor(Color.TRANSPARENT);
        tabs.setUnderlineHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, dm));
        tabs.setIndicatorHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, dm));
        tabs.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, dm));
        tabs.setIndicatorColor(Color.parseColor("#ff33b5e5"));
        tabs.setTabBackground(0);
        tabs.setViewPager(pager);
        tabs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                Fragment fragment = fragments[position];
                SearchView searchView = null;
                if(fragment == null) {
                    return;
                }
                if (fragment instanceof CloudDevFragment) {
                    searchView = ((CloudDevFragment) fragments[position]).mSearchView;
                } else if (fragment instanceof LocalDevFragment) {
                    searchView = ((LocalDevFragment) fragments[position]).mSearchView;
                } else if (fragment instanceof GroupFragment) {
                    searchView = ((GroupFragment) fragments[position]).mSearchView;
                }
                // FIX BUG: Search bar can't be collapsed.
                if (searchView != null) {
                    searchView.setIconified(true);
                    searchView.setIconifiedByDefault(true);
                    actionBar.collapseActionView();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    private void startLogin() {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    protected void logoutSubsidiary() {
        mDialog = Kdialog.getProgress(context, getString(R.string.logouting));
        new Timer().schedule(new LogoutSubCompTask(), 0, PrjCfg.RUN_ONCE);
    }

    private class LogoutSubCompTask extends TimerTask {
        public void run() {
            try {
                JSONObject jObject = JSONReq.send(context, "PUT", PrjCfg.CLOUD_URL + "/api/company/logout");
                MHandler.exec(mHandler, MHandler.TOAST_MSG, (jObject == null || jObject.getInt("code") != 200) ? getString(R.string.already_logout) : getString(R.string.success_logout));
                Utils.savePrefs(context, "SubCompID", 0);
                Utils.restartActivity(context);

                if (Utils.loadPrefs(context, "Password", "").equals("")) {
                    MHandler.exec(mHandler, MHandler.BEEN_LOGOUT);
                    return;
                } else if(MainApp.SERV_MAINT > (new Date().getTime() / 1000)) {
                    MHandler.exec(mHandler, MHandler.SERV_MAINT);
                    return;
                }
            } catch (Exception e) {
                if(PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                    MHandler.exec(mHandler, MHandler.SHOW_ERR_MSG, Arrays.toString(e.getStackTrace()));
                }
                e.printStackTrace();
            } finally {
                mDialog.dismiss();
                this.cancel();
            }
        }
    }

    private static class MsgHandler extends MHandler {
        public MsgHandler(MainPager activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final MainPager activity = (MainPager) super.mActivity.get();
            switch (msg.what) {
                case MHandler.UPDATE_APP: {
                    if(activity.newAppDialog == null || !activity.newAppDialog.isShowing()) {
                        activity.newAppDialog = Kdialog.getDefInfoDialog(activity)
                                .setMessage((String) msg.obj)
                                .setNegativeButton(activity.getString(R.string.ignore), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Utils.savePrefs(activity, "AppIgnoreVerCode", Utils.loadPrefs(activity, "AppLatestVerCode"));
                                        activity.newAppDialog.dismiss();
                                    }
                                })
                                .setPositiveButton(activity.getString(R.string.confirm), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Utils.savePrefs(activity, "AppIgnoreVerCode", "");
                                        activity.mDialog = Kdialog.getProgress(activity, activity.getString(R.string.updateding));
                                        WebUtils.installNewApp(activity);
                                    }
                                }).show();
                    }
                    break;
                }
                default: {
                    super.handleMessage(msg);
                }
            }
        }
    }

    private class PollingTimerTask extends TimerTask {
        public void run() {
            try {
                if (Utils.loadPrefs(context, "Password", "").equals("")) {
                    MHandler.exec(mHandler, MHandler.BEEN_LOGOUT);
                    pollingTimer.cancel();
                } else if(MainApp.SERV_MAINT > (new Date().getTime() / 1000)) {
                    MHandler.exec(mHandler, MHandler.SERV_MAINT);
                    pollingTimer.cancel();
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class MainPagerAdapter extends FragmentPagerAdapter {
        private int[] tabTitles;

        public MainPagerAdapter(FragmentManager fm) {
            super(fm);
            if(PrjCfg.USER_MODE == PrjCfg.MODE_ADMIN || PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG) {
                tabTitles = new int[]{R.string.cloud_device, R.string.local_device, R.string.group};
                fragments[0] = new CloudDevFragment();
                fragments[1] = new LocalDevFragment();
                fragments[2] = new GroupFragment();
            } else {
                tabTitles = new int[]{R.string.cloud_device, R.string.group};
                fragments[0] = new CloudDevFragment();
                fragments[1] = new GroupFragment();
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getString(tabTitles[position]);
        }

        @Override
        public int getCount() {
            return tabTitles.length;
        }

        @Override
        public Fragment getItem(int position) {
            return fragments[position];
        }
    }
}
