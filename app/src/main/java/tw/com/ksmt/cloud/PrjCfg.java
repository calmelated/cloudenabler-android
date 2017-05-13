package tw.com.ksmt.cloud;

import android.content.Context;
import android.util.Log;

import tw.com.ksmt.cloud.libs.Dbg;
import tw.com.ksmt.cloud.libs.Utils;

public final class PrjCfg {
    // configuration
    public static String NOTIFICATION = "true";
    public static String NOTIFICATION_SOUND = "true";
    public static String NOTIFICATION_VIBRATION = "true";

    // default setting
    public static String SORT_REG_LIST  = "func_code";
    public static final String BRCAST_MSG  = "broadcastMessage";
    public static final int CLOUD_DEV_POLLING = 14700; //MILLI_SECONDS
    public static final int GRUOP_POLLING = 15300; //MILLI_SECONDS
    public static final int MAIN_PAGE_POLLING = 3000; //MILLI_SECONDS
    public static final int LOCAL_DEV_POLLING = 15000; //MILLI_SECONDS
    public static final int STANDARD_POLLING = 15000; //MILLI_SECONDS
    public static final int TCP_SO_TIMEOUT = 15000; // 15sec

    public static final int VIEW_CHART_POLLING = 2000; //MILLI_SECONDS
    public static final int VIEW_STATUS_POLLING = 1000; //MILLI_SECONDS
    public static final int RUN_ONCE = 86400 * 1000; //MILLI_SECONDS
    public static final int LOAD_MORE_NUM = 50;
    public static final int LOCAL_DEVTIME_POLLING = 30000; //MILLI_SECONDS
    public static final int DEV_DISCOVER_PORT = 3550;
    public static final int DEV_DISCOVER_TIMEOUT = 1500;
    public static final int DEV_CONNECT_PORT = 9980;
    public static final int DEV_POLL_TIME = 50; // 50 * 100ms = 5 sec
    public static final int DEV_LOG_FREQ = 10;  // 10 sec
    public static final int MAX_ACCOUNT_NUM = 50;
    public static final int MAX_DEVICE_NUM = 100;
    public static int MAX_GROUP_NUM = 100;
    public static final int MAX_MASTER_NUM = 9;
    public static final int MAX_ANNOUNCE = 1000;
    public static final int MAX_FLINK = 30;

    // User mode
    public static int USER_MODE  = PrjCfg.MODE_USER;
    public static final int MODE_USER  = 0;
    public static final int MODE_ADMIN = 1;
    public static final int MODE_KSMT_DEBUG = 2;

    /***************************************************************************************************************************
     *  Customer Settings
     ***************************************************************************************************************************/
    //Customer Name List
    public static final String CUSTOMER_KSMT = "KSMT";
    public static final String CUSTOMER_KSMT_LOG = "KSMT-Log";
    public static final String CUSTOMER_KSMT_DEV = "KSMT-Dev";
    public static final String CUSTOMER_KSMT_TEST_ALL = "KSMT-Test-All";
    public static final String CUSTOMER_FULLKEY = "FullKey";
    public static final String CUSTOMER_YATEC = "YATEC";
    public static final String CUSTOMER_HYEC = "HYEC";
    public static final String CUSTOMER_LILU = "LILU";

    //Customer URL List
    public static final String CLOUD_LINK_YATEC = "https://yatec-cloud.ksmt.co";
    public static final String CLOUD_LINK_LILU = "https://lilu-cloud.ksmt.co";
    public static final String CLOUD_LINK_HYEC = "https://hyec-cloud.ksmt.co";
    public static final String CLOUD_LINK_KSMT = "https://cloud.ksmt.co";

    //Customer Name List
//    public static String CUSTOMER = PrjCfg.CUSTOMER_KSMT_DEV;
//    public static String CUSTOMER = PrjCfg.CUSTOMER_KSMT;
    public static String CUSTOMER = PrjCfg.CUSTOMER_KSMT_LOG;
//    public static String CUSTOMER = PrjCfg.CUSTOMER_KSMT_TEST_ALL;
//    public static String CUSTOMER = PrjCfg.CUSTOMER_FULLKEY;
//    public static String CUSTOMER = PrjCfg.CUSTOMER_YATEC;
//    public static String CUSTOMER = PrjCfg.CUSTOMER_HYEC;
//    public static String CUSTOMER = PrjCfg.CUSTOMER_LILU;

    // Customized Settings by Customer
    public static String CLOUD_URL;
    public static String APP_CHK_LINK;
    public static String APP_RAW_LINK;
    public static boolean EN_CLOUD_LOG;
    public static boolean EN_EXPORT_CLOUD_LOG;
    public static boolean EN_CONTACT_INFO;
    public static boolean EN_ADV_GRP;
    public static boolean EN_IOST_LOG;
    public static boolean EN_FLINK;
    public static boolean EN_ANNOUNCE;
    public static boolean EN_ANNOUNCE_SUBCOMP;
    public static boolean EN_TRIAL_LOGIN;
    public static boolean EN_REG_RANGE;
    public static boolean EN_REG_4RR;
    public static boolean EN_REG_IMPORTANCE;
    public static boolean EN_IEEE754_MBUS_NUM;
    public static boolean EN_BARCHART;

    // Init Customize settings
    static {
        Log.e(Dbg._TAG_(), "Init Customer Settings = " + PrjCfg.CUSTOMER);
        PrjCfg.loadSettings(PrjCfg.CUSTOMER);
    }

    // Reload default settings
    public static void resetDefault() {
        PrjCfg.CLOUD_URL = (PrjCfg.CLOUD_URL == null) ? "https://cloud.ksmt.co" : PrjCfg.CLOUD_URL ;
        PrjCfg.APP_CHK_LINK = "https://bitbucket.org/api/1.0/repositories/cloudenabler/linoderelease/changesets?limit=1";
        PrjCfg.APP_RAW_LINK = "https://bitbucket.org/cloudenabler/linoderelease/raw/";
        PrjCfg.EN_CLOUD_LOG = false;
        PrjCfg.EN_EXPORT_CLOUD_LOG = false;
        PrjCfg.EN_CONTACT_INFO = false;
        PrjCfg.EN_ADV_GRP = false;
        PrjCfg.EN_IOST_LOG = false;
        PrjCfg.EN_FLINK = false;
        PrjCfg.EN_ANNOUNCE = false;
        PrjCfg.EN_ANNOUNCE_SUBCOMP = false;
        PrjCfg.EN_TRIAL_LOGIN = false;
        PrjCfg.EN_REG_RANGE = false;
        PrjCfg.EN_REG_4RR = false;
        PrjCfg.EN_REG_IMPORTANCE = false;
        PrjCfg.EN_IEEE754_MBUS_NUM = false;
        PrjCfg.EN_BARCHART = false;
        PrjCfg.MAX_GROUP_NUM = 100;
    }

    // Customize settings
    public static void loadSettings(String customer) {
        Log.e(Dbg._TAG_(), "Reload Customer Settings = " + customer);
        PrjCfg.resetDefault();

        if(customer.equals(PrjCfg.CUSTOMER_KSMT)) {
            PrjCfg.APP_CHK_LINK = "https://bitbucket.org/api/1.0/repositories/cloudenabler/linoderelease/changesets?limit=1";
            PrjCfg.APP_RAW_LINK = "https://bitbucket.org/cloudenabler/linoderelease/raw/";
        } else if(customer.equals(PrjCfg.CUSTOMER_KSMT_LOG)) {
            PrjCfg.APP_CHK_LINK = "https://bitbucket.org/api/1.0/repositories/cloudenabler/ksmt-log/changesets?limit=1";
            PrjCfg.APP_RAW_LINK = "https://bitbucket.org/cloudenabler/ksmt-log/raw/";
            PrjCfg.EN_CLOUD_LOG = true;
            PrjCfg.EN_EXPORT_CLOUD_LOG = true;
        } else if(customer.equals(PrjCfg.CUSTOMER_KSMT_DEV)) {
            PrjCfg.APP_CHK_LINK = "https://bitbucket.org/api/1.0/repositories/cloudenabler/development/changesets?limit=1";
            PrjCfg.APP_RAW_LINK = "https://bitbucket.org/cloudenabler/development/raw/";
            PrjCfg.EN_BARCHART = true;
        } else if(customer.equals(PrjCfg.CUSTOMER_KSMT_TEST_ALL)) {
            PrjCfg.APP_CHK_LINK = "https://bitbucket.org/api/1.0/repositories/cloudenabler/development/changesets?limit=1";
            PrjCfg.APP_RAW_LINK = "https://bitbucket.org/cloudenabler/development/raw/";
            PrjCfg.EN_CLOUD_LOG = true;
            PrjCfg.EN_EXPORT_CLOUD_LOG = true;
            PrjCfg.EN_CONTACT_INFO = true;
            PrjCfg.EN_REG_RANGE = true;
            PrjCfg.EN_REG_IMPORTANCE = true;
            PrjCfg.EN_REG_4RR = true;
            PrjCfg.EN_TRIAL_LOGIN = true;
            PrjCfg.EN_ADV_GRP = true;
            PrjCfg.EN_IOST_LOG = true;
            PrjCfg.EN_ANNOUNCE = true;
            PrjCfg.EN_ANNOUNCE_SUBCOMP = true;
            PrjCfg.EN_FLINK = true;
            PrjCfg.MAX_GROUP_NUM = 1000;
            PrjCfg.EN_BARCHART = true;
        } else if(customer.equals(PrjCfg.CUSTOMER_FULLKEY)) {
            PrjCfg.CLOUD_URL = (PrjCfg.CLOUD_URL == null) ? "https://cloud.ksmt.co" : PrjCfg.CLOUD_URL ;
            PrjCfg.APP_CHK_LINK = "https://bitbucket.org/api/1.0/repositories/cloudenabler/fullkey/changesets?limit=1";
            PrjCfg.APP_RAW_LINK = "https://bitbucket.org/cloudenabler/fullkey/raw/";
        } else if(customer.equals(PrjCfg.CUSTOMER_YATEC)) {
            PrjCfg.CLOUD_URL = (PrjCfg.CLOUD_URL == null) ? "https://yatec-cloud.ksmt.co" : PrjCfg.CLOUD_URL ;
            PrjCfg.APP_CHK_LINK = "https://bitbucket.org/api/1.0/repositories/cloudenabler/yatec/changesets?limit=1";
            PrjCfg.APP_RAW_LINK = "https://bitbucket.org/cloudenabler/yatec/raw/";
            PrjCfg.EN_CLOUD_LOG = true;
            PrjCfg.EN_CONTACT_INFO = true;
            PrjCfg.EN_REG_RANGE = true;
            PrjCfg.EN_TRIAL_LOGIN = true;
            PrjCfg.EN_IOST_LOG = true;
            PrjCfg.EN_FLINK = true;
            PrjCfg.MAX_GROUP_NUM = 1000;
        } else if(customer.equals(PrjCfg.CUSTOMER_HYEC)) {
            PrjCfg.CLOUD_URL = (PrjCfg.CLOUD_URL == null) ? "https://hyec-cloud.ksmt.co" : PrjCfg.CLOUD_URL ;
            PrjCfg.APP_CHK_LINK = "https://bitbucket.org/api/1.0/repositories/cloudenabler/hyec/changesets?limit=1";
            PrjCfg.APP_RAW_LINK = "https://bitbucket.org/cloudenabler/hyec/raw/";
            PrjCfg.EN_CLOUD_LOG = true;
            PrjCfg.EN_REG_RANGE = true;
            PrjCfg.EN_REG_IMPORTANCE = true;
            PrjCfg.EN_REG_4RR = true;
            PrjCfg.EN_TRIAL_LOGIN = true;
            PrjCfg.EN_IOST_LOG = true;
            PrjCfg.EN_ANNOUNCE = true;
            PrjCfg.EN_FLINK = true;
            PrjCfg.MAX_GROUP_NUM = 1000;
            PrjCfg.EN_IEEE754_MBUS_NUM = true;
        } else if(customer.equals(PrjCfg.CUSTOMER_LILU)) {
            PrjCfg.CLOUD_URL = (PrjCfg.CLOUD_URL == null) ? "https://lilu-cloud.ksmt.co" : PrjCfg.CLOUD_URL ;
            PrjCfg.APP_CHK_LINK = "https://bitbucket.org/api/1.0/repositories/cloudenabler/lilu/changesets?limit=1";
            PrjCfg.APP_RAW_LINK = "https://bitbucket.org/cloudenabler/lilu/raw/";
            PrjCfg.EN_IOST_LOG = true;
            PrjCfg.EN_ANNOUNCE = true;
            PrjCfg.EN_ANNOUNCE_SUBCOMP = true;
        }
    }

    /***************************************************************************************************************************
     *  LeanCloud Key (China)
     ***************************************************************************************************************************/
    // China KSMT
    public static final String LC_APP_ID = "";
    public static final String LC_APP_KEY = "";

    // China Site HYEC
    public static final String LC_APP_ID_HYEC = "";
    public static final String LC_APP_KEY_HYEC = "";

    // China Site YATEC
    public static final String LC_APP_ID_YATEC = "";
    public static final String LC_APP_KEY_YATEC = "";

    // China Site LILU
    public static final String LC_APP_ID_LILU = "";
    public static final String LC_APP_KEY_LILU = "";

    /***************************************************************************************************************************
     *  LeanCloud Key (US)
     ***************************************************************************************************************************/
    // US site KSMT
    public static final String LCUS_APP_ID = "";
    public static final String LCUS_APP_KEY = "";

    // US Site Test
    public static final String LCUS_APP_ID_TEST = "";
    public static final String LCUS_APP_KEY_TEST = "";

    // US Site HYEC
    public static final String LCUS_APP_ID_HYEC = "";
    public static final String LCUS_APP_KEY_HYEC = "";

    // US Site YATEC
    public static final String LCUS_APP_ID_YATEC = "";
    public static final String LCUS_APP_KEY_YATEC = "";

    // US Site LILU
    public static final String LCUS_APP_ID_LILU = "";
    public static final String LCUS_APP_KEY_LILU = "";

    /***************************************************************************************************************************
     *  Get LC Push Setttings by Customer and Region
     ***************************************************************************************************************************/
    public static String getLCAppId(String region, String customer) {
        if("US".equals(region)) {
            if(PrjCfg.CUSTOMER_HYEC.equals(customer)) {
                return PrjCfg.LCUS_APP_ID_HYEC;
            } else if(PrjCfg.CUSTOMER_YATEC.equals(customer)) {
                return PrjCfg.LCUS_APP_ID_YATEC;
            } else if(PrjCfg.CUSTOMER_LILU.equals(customer)) {
                return PrjCfg.LCUS_APP_ID_LILU;
            } else { // KSMT
                return PrjCfg.LCUS_APP_ID;
            }
        } else { // CN
            if(PrjCfg.CUSTOMER_HYEC.equals(customer)) {
                return PrjCfg.LC_APP_ID_HYEC;
            } else if(PrjCfg.CUSTOMER_YATEC.equals(customer)) {
                return PrjCfg.LC_APP_ID_YATEC;
            } else if(PrjCfg.CUSTOMER_LILU.equals(customer)) {
                return PrjCfg.LC_APP_ID_LILU;
            } else { // KSMT
                return PrjCfg.LC_APP_ID;
            }
        }
    }

    public static String getLCAppKey(String region, String customer) {
        if("US".equals(region)) {
            if(PrjCfg.CUSTOMER_HYEC.equals(customer)) {
                return PrjCfg.LCUS_APP_KEY_HYEC;
            } else if(PrjCfg.CUSTOMER_YATEC.equals(customer)) {
                return PrjCfg.LCUS_APP_KEY_YATEC;
            } else if(PrjCfg.CUSTOMER_LILU.equals(customer)) {
                return PrjCfg.LCUS_APP_KEY_LILU;
            } else { // KSMT
                return PrjCfg.LCUS_APP_KEY;
            }
        } else { // CN
            if(PrjCfg.CUSTOMER_HYEC.equals(customer)) {
                return PrjCfg.LC_APP_KEY_HYEC;
            } else if(PrjCfg.CUSTOMER_YATEC.equals(customer)) {
                return PrjCfg.LC_APP_KEY_YATEC;
            } else if(PrjCfg.CUSTOMER_LILU.equals(customer)) {
                return PrjCfg.LC_APP_KEY_LILU;
            } else { // KSMT
                return PrjCfg.LC_APP_KEY;
            }
        }
    }

    public static String getLCType(Context context) {
        String customer = Utils.loadPrefs(context, "CUSTOMER", PrjCfg.CUSTOMER);
        return getLCType(MainApp.LC_SERVER, customer);
    }

    public static String getLCType(String region, String customer) {
        if("US".equals(region)) {
            if(PrjCfg.CUSTOMER_HYEC.equals(customer)) {
                return "9";
            } else if(PrjCfg.CUSTOMER_YATEC.equals(customer)) {
                return "15";
            } else if(PrjCfg.CUSTOMER_LILU.equals(customer)) {
                return "21";
            } else { // KSMT
                return "3";
            }
        } else { // CN
            if(PrjCfg.CUSTOMER_HYEC.equals(customer)) {
                return "6";
            } else if(PrjCfg.CUSTOMER_YATEC.equals(customer)) {
                return "12";
            } else if(PrjCfg.CUSTOMER_LILU.equals(customer)) {
                return "18";
            } else { // KSMT
                return "0";
            }
        }
    }
}
