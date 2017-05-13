package tw.com.ksmt.cloud.ui;

import android.content.Context;
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
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import com.astuetz.PagerSlidingTabStrip;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import tw.com.ksmt.cloud.MainApp;
import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.iface.Account;
import tw.com.ksmt.cloud.iface.Auth;
import tw.com.ksmt.cloud.libs.Dbg;
import tw.com.ksmt.cloud.libs.MHandler;
import tw.com.ksmt.cloud.libs.Utils;

public class AccountPager extends ActionBarActivity {
    private ActionBar actionBar;
    private MainPagerAdapter adapter;
    private PagerSlidingTabStrip tabs;
    private ViewPager pager;
    private DisplayMetrics dm;

    private Context context = AccountPager.this;
    private MsgHandler mHandler;
    private Timer pollingTimer;
    private Account editAcnt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2E5E86")));
        AppSettings.reload(context);

        Account _editAcnt = (Account) getIntent().getSerializableExtra("account");
        try {
            editAcnt = (Account) _editAcnt.clone();
        } catch (Exception e) {
            editAcnt = _editAcnt;
        }
        if(editAcnt.admin) {
            setContentView(R.layout.view_pager_admin);
            tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
            tabs.setVisibility(View.GONE);
        } else {
            setContentView(R.layout.view_pager);
            tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
            dm = getResources().getDisplayMetrics();
            setTabsValue();
        }

        mHandler = new MsgHandler(this);
        pager = (ViewPager) findViewById(R.id.pager);
        adapter = new MainPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
        tabs.setViewPager(pager);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (pollingTimer != null) {
            pollingTimer.cancel();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        pollingTimer = new Timer();
        pollingTimer.schedule(new PollingTimerTask(), 0, PrjCfg.MAIN_PAGE_POLLING);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AppSettings.reload(context);
    }

    private void setTabsValue() {
        tabs.setShouldExpand(true);
        //tabs.setDividerColor(Color.TRANSPARENT);
        tabs.setUnderlineHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, dm));
        tabs.setIndicatorHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, dm));
        tabs.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, dm));
        tabs.setIndicatorColor(Color.parseColor("#ff33b5e5"));
        tabs.setTabBackground(0);
    }

    private static class MsgHandler extends MHandler {
        public MsgHandler(AccountPager activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final AccountPager activity = (AccountPager) super.mActivity.get();
            switch (msg.what) {
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
                    return;
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
        private final int[] tabTitles = { R.string.account, Auth.ALARM, Auth.CONTROL, Auth.MONITOR};

        public MainPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getString(tabTitles[position]);
        }

        @Override
        public int getCount() {
            if(editAcnt.admin) {
                return 1;
            }
            return tabTitles.length;
        }

        @Override
        public Fragment getItem(int position) {
            //Log.e(Dbg._TAG_(), "position=" + position + " AuthType = " + getString(tabTitles[position]));
            if(position == 0) {
                Fragment fragment = new AccountEditFragment();
                fragment.setArguments(getIntent().getExtras());
                return fragment;
            } else if(position == 1 || position == 2 || position == 3) {
                Bundle bundle = getIntent().getExtras();
                bundle.putInt("authType", tabTitles[position]);
                Fragment fragment = new AccountPermissionFragment();
                fragment.setArguments(bundle);
                return fragment;
            } else {
                Log.e(Dbg._TAG_(), "Unknown position " + position);
                return null;
            }
        }
    }
}
