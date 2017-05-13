package tw.com.ksmt.cloud.iface;

import android.content.Context;
import android.util.Log;

import com.avos.avoscloud.okhttp.MultipartBuilder;
import com.avos.avoscloud.okhttp.RequestBody;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.libs.Dbg;
import tw.com.ksmt.cloud.libs.StrUtils;
import tw.com.ksmt.cloud.libs.Utils;

public class Register implements Serializable {
    public static final int HOLDING_SHIFT = 40000;
    private static List<String> TYPES_LL;
    public static TreeMap<String, Integer> TYPES;
    public int type;
    public int slvIdx = 0; // 0: slave mode, > 0: ID of slave device
    public String id;
    public String haddr = "";
    public String iaddr = "";
    public String jaddr = "";
    public String laddr = "";
    public String description =" ";
    public String unit = "";
    public String eq = "";
    public boolean enlog = false;
    public String virtReg = "";
    public String onVal = "";
    public String offVal = "";
    public String upBound = "";
    public String lowBound = "";
    public String sndAlmMail = "";
    public int almPri;
    public int almDur;
    public String maxVal = "";
    public String minVal = "";
    public int btnTime = 13;
    public int fpt = 0;
    public int display = 0;
    public String swSN = "";
    public String swAddr = "";
    public int swType;
    public Integer[] limitId;
    public String refReg = "";
    public String rr1 = "";
    public String rr2 = "";
    public String rr3 = "";
    public String rr4 = "";

    private static int[] DISPALY_TYPES = {
            R.string.display_default,                   // 0
            R.string.display_0_as_green,                // 1
            R.string.display_1_as_red,                  // 2
            R.string.display_2_as_orange,               // 3
            R.string.display_3_as_yellow,               // 4
            R.string.display_4_as_blue,                 // 5
            R.string.display_5_as_white,                // 6
            R.string.display_6_as_black,                // 7
            R.string.display_01_as_green_red,           // 8
            R.string.display_10_as_green_red,           // 9
            R.string.display_01_as_local_and_remote,    // 10 YATEC
            R.string.display_012_as_off_on_and_trip,    // 11 YATEC
            R.string.display_01_as_ok_and_err,          // 12 HYEC
            R.string.display_82_76_67_as_rlc,           // 13 HYEC
            R.string.display_01_as_putoff_on,           // 14 HYEC
            R.string.display_01_as_puton_off,           // 15 HYEC
            R.string.display_01_as_onsite_remote,       // 16 HYEC
            R.string.display_01_as_remote_onsite,       // 17 HYEC
            R.string.display_01_as_err_and_ok,          // 18 HYEC
            R.string.display_012_as_discharge,          // 19 HYEC
            R.string.display_01_as_on_off,              // 20 HYEC
            R.string.display_01_as_off_on,              // 21 HYEC
            R.string.display_01_as_run_stop,            // 22 HYEC
            R.string.display_01_as_stop_run,            // 23 HYEC
            R.string.display_01_as_open_close,          // 24 HYEC
            R.string.display_01_as_close_open,          // 25 HYEC
            R.string.display_01_as_normal_trip,         // 26 HYEC
            R.string.display_01_as_trip_normal,         // 27 HYEC
            R.string.display_01_as_test_connect,        // 28 HYEC
            R.string.display_01_as_connect_test,        // 29 HYEC
            R.string.display_01_as_manual_auto,         // 30 HYEC
            R.string.display_01_as_auto_manual,         // 31 HYEC
            R.string.display_01_as_normal_abnormal,     // 32 HYEC
            R.string.display_01_as_abnormal_normal,     // 33 HYEC
            R.string.display_01_as_green_off_and_red_on, // 34 YATEc
            R.string.display_01_as_red_off_and_green_on,              // 34 YATEc
    };

    public static final int MODBUS_INT16            = 0;
    public static final int MODBUS_UINT16           = 1;
    public static final int MODBUS_INT32            = 2;
    public static final int MODBUS_UINT32           = 3;
    public static final int MODBUS_IEEE754          = 4;
    public static final int MODBUS_FIXPT            = 5;
    public static final int MODBUS_FIXPT16          = 6;
    public static final int MODBUS_BINARY           = 7;
    public static final int MODBUS_SWITCH           = 8;
    public static final int MODBUS_FIXPT64          = 9;
    public static final int MODBUS_UNFIXPT16        = 20;
    public static final int MODBUS_UNFIXPT32        = 21;
    public static final int MODBUS_UNFIXPT48        = 22;

    public static final int MODBUS_GCM              = 10;
    public static final int MODBUS_EMAIL            = 11;
    public static final int MODBUS_GCM_EMAIL        = 12;
    public static final int MODBUS_GCM_CRITICAL     = 13;
    public static final int M2M_IOSW                = 14;
    public static final int M2M_IOSW32              = 15;
    public static final int M2M_IOSW48              = 16;
    public static final int M2M_IOSW64              = 17;

    public static final int APP_INT16               = 50;
    public static final int APP_UINT16              = 51;
    public static final int APP_INT32               = 52;
    public static final int APP_UINT32              = 53;
    public static final int APP_IEEE754             = 54;
    public static final int APP_FIXPT               = 55;
    public static final int APP_FIXPT16             = 56;
    public static final int APP_BINARY              = 57;
    public static final int APP_BTN                 = 58;
    public static final int APP_SWITCH              = 59;
    public static final int APP_FIXPT64             = 60;
    public static final int APP_UNFIXPT16           = 61;
    public static final int APP_UNFIXPT32           = 62;
    public static final int APP_UNFIXPT48           = 63;
    public static final int TYPE_ERROR              = 99;

    public static void initTypes(Context context) {
        TYPES = new TreeMap<String, Integer>();

        TYPES.put(context.getString(R.string.modbus_16_bit_int),  MODBUS_INT16);
        TYPES.put(context.getString(R.string.modbus_16_bit_uint), MODBUS_UINT16);
        TYPES.put(context.getString(R.string.modbus_32_bit_int),  MODBUS_INT32);
        TYPES.put(context.getString(R.string.modbus_32_bit_uint), MODBUS_UINT32);
        TYPES.put(context.getString(R.string.modbus_ieee_754),    MODBUS_IEEE754);
        TYPES.put(context.getString(R.string.modbus_fix_point),   MODBUS_FIXPT);
        TYPES.put(context.getString(R.string.modbus_fix_point_16),MODBUS_FIXPT16);
        TYPES.put(context.getString(R.string.modbus_fix_point_64),MODBUS_FIXPT64);
        TYPES.put(context.getString(R.string.modbus_unfpt_16),    MODBUS_UNFIXPT16);
        TYPES.put(context.getString(R.string.modbus_unfpt_32),    MODBUS_UNFIXPT32);
        TYPES.put(context.getString(R.string.modbus_unfpt_48),    MODBUS_UNFIXPT48);
        TYPES.put(context.getString(R.string.modbus_binary),      MODBUS_BINARY);
        TYPES.put(context.getString(R.string.modbus_switch),      MODBUS_SWITCH);
        TYPES.put(context.getString(R.string.modbus_iosw16),      M2M_IOSW);
        TYPES.put(context.getString(R.string.modbus_iosw32),      M2M_IOSW32);
//        TYPES.put(context.getString(R.string.modbus_iosw48),      M2M_IOSW48);
//        TYPES.put(context.getString(R.string.modbus_iosw64),      M2M_IOSW64);
        TYPES.put(context.getString(R.string.app_16_bit_int),     APP_INT16);
        TYPES.put(context.getString(R.string.app_16_bit_uint),    APP_UINT16);
        TYPES.put(context.getString(R.string.app_32_bit_int),     APP_INT32);
        TYPES.put(context.getString(R.string.app_32_bit_uint),    APP_UINT32);
        TYPES.put(context.getString(R.string.app_ieee_754),       APP_IEEE754);
        TYPES.put(context.getString(R.string.app_fix_point),      APP_FIXPT);
        TYPES.put(context.getString(R.string.app_fix_point_16),   APP_FIXPT16);
        TYPES.put(context.getString(R.string.app_fix_point_64),   APP_FIXPT64);
        TYPES.put(context.getString(R.string.app_unfpt_16),       APP_UNFIXPT16);
        TYPES.put(context.getString(R.string.app_unfpt_32),       APP_UNFIXPT32);
        TYPES.put(context.getString(R.string.app_unfpt_48),       APP_UNFIXPT48);
        TYPES.put(context.getString(R.string.app_binary),         APP_BINARY);
        TYPES.put(context.getString(R.string.app_btn),            APP_BTN);
        TYPES.put(context.getString(R.string.app_switch),         APP_SWITCH);

        TYPES.put(context.getString(R.string.modbus_gcm),         MODBUS_GCM);
        TYPES.put(context.getString(R.string.modbus_email),       MODBUS_EMAIL);
        TYPES.put(context.getString(R.string.modbus_gcm_email),   MODBUS_GCM_EMAIL);
        //TYPES.put(context.getString(R.string.modbus_gcm_email),   MODBUS_GCM_CRITICAL);

        TYPES_LL = new LinkedList<String>();
        TYPES_LL.add(context.getString(R.string.modbus_16_bit_int));
        TYPES_LL.add(context.getString(R.string.modbus_16_bit_uint));
        TYPES_LL.add(context.getString(R.string.modbus_fix_point_16));
        TYPES_LL.add(context.getString(R.string.modbus_unfpt_16));
        TYPES_LL.add(context.getString(R.string.modbus_32_bit_int));
        TYPES_LL.add(context.getString(R.string.modbus_32_bit_uint));
        TYPES_LL.add(context.getString(R.string.modbus_ieee_754));
        TYPES_LL.add(context.getString(R.string.modbus_fix_point));
        TYPES_LL.add(context.getString(R.string.modbus_unfpt_32));
        TYPES_LL.add(context.getString(R.string.modbus_unfpt_48));
        TYPES_LL.add(context.getString(R.string.modbus_fix_point_64));
        TYPES_LL.add(context.getString(R.string.modbus_binary));
        TYPES_LL.add(context.getString(R.string.modbus_switch));

        TYPES_LL.add(context.getString(R.string.modbus_gcm));
        TYPES_LL.add(context.getString(R.string.modbus_email));
        TYPES_LL.add(context.getString(R.string.modbus_gcm_email));
        //TYPES_LL.add(context.getString(R.string.modbus_gcm_email));

        TYPES_LL.add(context.getString(R.string.app_16_bit_int));
        TYPES_LL.add(context.getString(R.string.app_16_bit_uint));
        TYPES_LL.add(context.getString(R.string.app_fix_point_16));
        TYPES_LL.add(context.getString(R.string.app_unfpt_16));
        TYPES_LL.add(context.getString(R.string.app_32_bit_int));
        TYPES_LL.add(context.getString(R.string.app_32_bit_uint));
        TYPES_LL.add(context.getString(R.string.app_ieee_754));
        TYPES_LL.add(context.getString(R.string.app_fix_point));
        TYPES_LL.add(context.getString(R.string.app_unfpt_32));
        TYPES_LL.add(context.getString(R.string.app_unfpt_48));
        TYPES_LL.add(context.getString(R.string.app_fix_point_64));
        TYPES_LL.add(context.getString(R.string.app_binary));
        TYPES_LL.add(context.getString(R.string.app_btn));
        TYPES_LL.add(context.getString(R.string.app_switch));

        TYPES_LL.add(context.getString(R.string.modbus_iosw16));
        TYPES_LL.add(context.getString(R.string.modbus_iosw32));
//        TYPES_LL.add(context.getString(R.string.modbus_iosw48));
//        TYPES_LL.add(context.getString(R.string.modbus_iosw64));
    }

    public static void clearTypes() {
        if(TYPES == null) {
            return;
        }
        TYPES.clear();
        TYPES = null;
    }

    public static boolean enRtChart(int type) {
        if(isBinary(type) || isIOSW(type) || isAppWriteable(type)) {
            return false;
        }
        return true;
    }

    public static boolean enCloudLogging(int type) {
        if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_YATEC)) {
            return (isNumber(type) || isAlarm(type)) ? true : false;
        } else if(PrjCfg.EN_CLOUD_LOG) {
            return enRtChart(type);
        } else { // Other customers
            return false;
        }
    }

    public static boolean is64Bits(int type) {
        return (type == MODBUS_FIXPT64 || type == APP_FIXPT64 || type == M2M_IOSW64) ? true : false;
    }

    public static boolean is48Bits(int type) {
        return (type == MODBUS_UNFIXPT48 || type == APP_UNFIXPT48 || type == M2M_IOSW48) ? true : false;
    }

    public static boolean is32Bits(int type) {
        if(type == MODBUS_INT32 || type == MODBUS_UINT32 || type == MODBUS_IEEE754 || type == MODBUS_FIXPT ||
           type == APP_INT32    || type ==    APP_UINT32 || type ==    APP_IEEE754 || type == APP_FIXPT    ||
           type == M2M_IOSW32   || type == MODBUS_UINT32 || type == APP_UNFIXPT32  || type == MODBUS_UNFIXPT32) {
            return true;
        }
        return false;
    }

    public static boolean is16Bits(int type) {
        return (is32Bits(type) || is48Bits(type) || is64Bits(type)) ? false : true;
    }

    public static boolean isIOSW(int type) {
        if(type == M2M_IOSW32 || type == M2M_IOSW || type == M2M_IOSW48 || type == M2M_IOSW64) {
            return true;
        }
        return false;
    }

    public static boolean isSigned(int type) {
        if(type == MODBUS_INT16 || type == MODBUS_INT32   || type == MODBUS_IEEE754 || type == MODBUS_FIXPT ||
           type == APP_INT16    || type == APP_INT32      || type == APP_IEEE754    || type == APP_FIXPT    ||
           type == APP_FIXPT64  || type == MODBUS_FIXPT64 || type == MODBUS_FIXPT16 || type == APP_FIXPT16) {
            return true;
        }
        return false;
    }

    public static boolean isAppWriteable(int type) {
        if(type >= APP_INT16 && type <= APP_UNFIXPT48) {
            return true;
        }
        return false; // You can't write Modbus register
    }

    public static boolean isAlarm(int type) {
        if (type == MODBUS_GCM || type == MODBUS_EMAIL || type == MODBUS_GCM_EMAIL || type == MODBUS_GCM_CRITICAL) {
            return true;
        }
        return false;
    }

    public static boolean isFloatVal(int type) {
        if (type == MODBUS_IEEE754 || type == APP_IEEE754 || isFixPtVal(type)) {
            return true;
        }
        return false;
    }

    public static boolean isFixPtVal(int type) {
        if (type == MODBUS_FIXPT16      ||
            type == MODBUS_FIXPT        ||
            type == MODBUS_FIXPT64      ||
            type == MODBUS_UNFIXPT16    ||
            type == MODBUS_UNFIXPT32    ||
            type == MODBUS_UNFIXPT48    ||
            type == APP_FIXPT16         ||
            type == APP_FIXPT           ||
            type == APP_FIXPT64         ||
            type == APP_UNFIXPT16       ||
            type == APP_UNFIXPT32       ||
            type == APP_UNFIXPT48) {
            return true;
        }
        return false;
    }

    public static boolean isBinary(int type) {
        return (type == APP_BINARY || type == MODBUS_BINARY) ? true : false ;
    }

    public static boolean isEventData(int type) {
        if(isAlarm(type) || type == MODBUS_SWITCH || type == APP_SWITCH || type == APP_BTN) {
            return true;
        }
        return false;
    }

    public static boolean isAppNumber(int type) {
        if(type == APP_FIXPT        ||
           type == APP_FIXPT16      ||
           type == APP_FIXPT64      ||
           type == APP_UNFIXPT16    ||
           type == APP_UNFIXPT32    ||
           type == APP_UNFIXPT48    ||
           type == APP_INT16        ||
           type == APP_INT32        ||
           type == APP_UINT16       ||
           type == APP_UINT32) {
            return true;
        }
        return false;
    }

    public static boolean isMbusNumber(int type) {
        if(type == MODBUS_FIXPT         ||
           type == MODBUS_FIXPT16       ||
           type == MODBUS_FIXPT64       ||
           type == MODBUS_UNFIXPT16     ||
           type == MODBUS_UNFIXPT32     ||
           type == MODBUS_UNFIXPT48     ||
           type == MODBUS_INT16         ||
           type == MODBUS_INT32         ||
           type == MODBUS_UINT16        ||
           type == MODBUS_UINT32        ||
           type == MODBUS_SWITCH) {
            return true;
        }
        if(PrjCfg.EN_IEEE754_MBUS_NUM) {
            if(type == MODBUS_IEEE754) {
                return true;
            }
        }
        return false;
    }

    public static boolean isWriteOnce(ViewStatus vs) {
        if((vs.type >= MODBUS_INT16     && vs.type <= MODBUS_FIXPT64) ||
           (vs.type >= MODBUS_UNFIXPT16 && vs.type <= MODBUS_UNFIXPT48)) {
            if(vs.slvIdx < 1) { //ce-slave device
                return true;
            }
            String fc = StrUtils.getFCByAddr(vs.haddr);
            if(fc.equals("01") || fc.equals("03")) { // only coil and holding registers
                return true;
            }
        }
        return false;
    }

    public static boolean isMathEq(int type) {
        if(type == MODBUS_IEEE754) {
            return false;
        } else if(isMbusNumber(type) || isAlarm(type)) {
            return true;
        }
        return false;
    }

    public static boolean isNumber(int type) {
        return (isAppNumber(type) || isMbusNumber(type) || type == APP_IEEE754 || type == MODBUS_IEEE754) ?  true : false ;
    }

    public static boolean isDisplay(int type) {
        if(type == MODBUS_INT16  || type == MODBUS_UINT16  ||
           type == MODBUS_INT32  || type == MODBUS_UINT32  ||
           type == MODBUS_SWITCH) {
            return true;
        }
        return false;
    }

    public Register(JSONObject jsonObject) {
        this(0, jsonObject);
    }

    public Register(int slvIdx, JSONObject jsonObject) {
        try {
            this.slvIdx = slvIdx;
            this.type = jsonObject.getInt("type");
            this.description = (jsonObject.has("desc")) ? jsonObject.getString("desc") : "";
            this.unit = (jsonObject.has("unit")) ? jsonObject.getString("unit") : null;
            this.haddr = (jsonObject.has("haddr")) ? Utils.realAddr(jsonObject.getString("haddr")) : "";
            this.iaddr = (jsonObject.has("iaddr")) ? Utils.realAddr(jsonObject.getString("iaddr")) : "";
            this.jaddr = (jsonObject.has("jaddr")) ? Utils.realAddr(jsonObject.getString("jaddr")) : "";
            this.laddr = (jsonObject.has("laddr")) ? Utils.realAddr(jsonObject.getString("laddr")) : "";
            this.enlog = (jsonObject.getString("enlog").equals("1")) ? true : false;
            this.id = jsonObject.getString("id");
            this.eq = (jsonObject.has("eq")) ? jsonObject.getString("eq") : null;
            this.onVal = (jsonObject.has("on")) ? jsonObject.getString("on") : "";
            this.offVal = (jsonObject.has("off")) ? jsonObject.getString("off") : "";
            this.fpt = (jsonObject.has("fpt")) ? jsonObject.getInt("fpt") : 0;
            this.display = (jsonObject.has("dt")) ? jsonObject.getInt("dt") : 0;
            this.upBound = (jsonObject.has("up")) ? jsonObject.getString("up") : null;
            this.lowBound = (jsonObject.has("low")) ? jsonObject.getString("low") : null;
            this.maxVal = (jsonObject.has("max")) ? jsonObject.getString("max") : null;
            this.minVal = (jsonObject.has("min")) ? jsonObject.getString("min") : null;
            this.virtReg = (jsonObject.has("virt")) ? jsonObject.getString("virt") : "";

            //Log.e(Dbg._TAG_(), String.valueOf(jsonObject.has("sam")));
            this.sndAlmMail = (jsonObject.has("sam")) ? jsonObject.getString("sam") : "";
            this.almPri = (jsonObject.has("pri")) ? jsonObject.getInt("pri") : 0;
            this.almDur = (jsonObject.has("dur")) ? jsonObject.getInt("dur") : 0;

            this.btnTime = (jsonObject.has("btnTime")) ? jsonObject.getInt("btnTime") : 13;
            this.swSN = (jsonObject.has("swSN")) ? jsonObject.getString("swSN") : "";
            this.swAddr = (jsonObject.has("swAddr")) ? jsonObject.getString("swAddr") : "";
            this.refReg = (jsonObject.has("refReg")) ? jsonObject.getString("refReg") : "";
            this.rr1 = (jsonObject.has("rr1")) ? jsonObject.getString("rr1") : "";
            this.rr2 = (jsonObject.has("rr2")) ? jsonObject.getString("rr2") : "";
            this.rr3 = (jsonObject.has("rr3")) ? jsonObject.getString("rr3") : "";
            this.rr4 = (jsonObject.has("rr4")) ? jsonObject.getString("rr4") : "";

            JSONArray _limitId = (jsonObject.has("limitId")) ? jsonObject.getJSONArray("limitId") : null;
            if(_limitId != null) {
                limitId = new Integer[_limitId.length()];
                for (int i = 0; i < _limitId.length(); ++i) {
                    limitId[i] = _limitId.optInt(i);
                }
            } else {
                limitId = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // New Register
    public Register(int type, String haddr, String laddr) {
        this.haddr = haddr;
        this.laddr = laddr;
        this.type = type;

        String rand = Long.toString((new Date()).getTime()).substring(7);
        this.id = (this.laddr.equals("")) ? this.haddr.substring(2) + rand : this.haddr.substring(2) + this.laddr.substring(2) + rand;
        if(Register.isAlarm(type)) { // Set high importance at default
            this.almPri = 2;
        }
    }

    // for delete register
    public Register(String id, ViewStatus vs) {
        this.id = id;
        this.description = vs.desc;
        this.type = vs.type;
        this.haddr = vs.haddr;
        this.iaddr = vs.iaddr;
        this.jaddr = vs.jaddr;
        this.laddr = vs.laddr;
        this.enlog = vs.enlog;
    }

    public static boolean isCoil(int slvIdx, String haddr) {
        if (slvIdx > 0) {
            String fc = StrUtils.getFCByAddr(haddr);
            if(fc.equalsIgnoreCase("01") || fc.equalsIgnoreCase("05")) {
                return true;
            }
        }
        return false;
    }

    public boolean isCoil() {
        return Register.isCoil(slvIdx, haddr);
    }

    public static String getValidRange(int type) {
        return getValidRange(new ViewStatus(type, 0));
    }

    public static String getValidRange(Register register) {
        ViewStatus vs = new ViewStatus();
        vs.type = register.type;
        vs.slvIdx = register.slvIdx;
        vs.haddr = register.haddr;
        vs.fpt = register.fpt;
        vs.isCoilReg = Register.isCoil(vs.slvIdx, vs.haddr);
        return getValidRange(vs);
    }

    public static String getValidRange(ViewStatus vs) {
        int type = vs.type;
        if (vs.isCoilReg) {
            return 0 + " ~ " + 1;
        } else if (type == Register.APP_INT16 || type == Register.MODBUS_INT16) {
            return (-Short.MAX_VALUE - 1) + " ~ " + Short.MAX_VALUE;
        } else if (type == Register.APP_UINT16 || type == Register.APP_BTN || type == Register.APP_SWITCH || type == Register.MODBUS_UINT16 || type == Register.MODBUS_SWITCH) {
            return "0" + " ~ " + (Short.MAX_VALUE * 2 + 1);
        } else if (type == Register.APP_INT32 || type == Register.MODBUS_INT32) {
            return (-Integer.MAX_VALUE - 1) + " ~ " + Integer.MAX_VALUE;
        } else if (type == Register.APP_UINT32 || type == Register.MODBUS_UINT32) {
            return "0 ~ " + (Integer.MAX_VALUE * 2L + 1);
        } else if (type == Register.APP_IEEE754 || type == Register.MODBUS_IEEE754) {
            return (-Float.MAX_VALUE - 1) + " ~ " + Float.MAX_VALUE;
        } else if (type == Register.APP_FIXPT || type == Register.MODBUS_FIXPT) {
            String minStr = String.format("%." + vs.fpt + "f", ((-Integer.MAX_VALUE - 1) / Math.pow(10, vs.fpt)));
            String maxStr = String.format("%." + vs.fpt + "f", (Integer.MAX_VALUE / Math.pow(10, vs.fpt)));
            return minStr + " ~ " + maxStr;
        } else if (type == Register.APP_FIXPT16 || type == Register.MODBUS_FIXPT16) {
            String minStr = String.format("%." + vs.fpt + "f", ((-Short.MAX_VALUE - 1) / Math.pow(10, vs.fpt)));
            String maxStr = String.format("%." + vs.fpt + "f", (Short.MAX_VALUE / Math.pow(10, vs.fpt)));
            return minStr + " ~ " + maxStr;
        } else if (type == Register.APP_UNFIXPT16 || type == Register.MODBUS_UNFIXPT16) {
            String minStr = String.format("%." + vs.fpt + "f", 0.0);
            String maxStr = String.format("%." + vs.fpt + "f", ((Short.MAX_VALUE * 2 + 1) / Math.pow(10, vs.fpt)));
            return minStr + " ~ " + maxStr;
        } else if (type == Register.APP_UNFIXPT32 || type == Register.MODBUS_UNFIXPT32) {
            String minStr = String.format("%." + vs.fpt + "f", 0.0);
            String maxStr = String.format("%." + vs.fpt + "f", ((Integer.MAX_VALUE * 2L + 1) / Math.pow(10, vs.fpt)));
            return minStr + " ~ " + maxStr;
        } else if (type == Register.APP_UNFIXPT48 || type == Register.MODBUS_UNFIXPT48) {
            String minStr = String.format("%." + vs.fpt + "f", 0.0);
            String maxStr;
            if (vs.fpt == 0) {
                maxStr = "999999";
            } else if (vs.fpt == 1) {
                maxStr = String.format("%." + vs.fpt + "f", 999999.9);
            } else if (vs.fpt == 2) {
                maxStr = String.format("%." + vs.fpt + "f", 999999.99);
            } else {
                maxStr = String.format("%." + vs.fpt + "f", 999999.999);
            }
            return minStr + " ~ " + maxStr;
        } else if (type == Register.APP_FIXPT64 || type == Register.MODBUS_FIXPT64) {
            BigDecimal bigDecimal = new BigDecimal(2).pow(52);
            String minStr = String.format("%." + vs.fpt + "f", (bigDecimal.multiply(BigDecimal.valueOf(-1))).divide(new BigDecimal(10).pow(vs.fpt)));
            String maxStr = String.format("%." + vs.fpt + "f", bigDecimal.subtract(BigDecimal.ONE).divide(new BigDecimal(10).pow(vs.fpt)));
            return minStr + " ~ " + maxStr;
        } else {
            return "Invlid type!";
        }
    }

    public static boolean validVal(Register register, String input) {
        ViewStatus vs = new ViewStatus();
        vs.type = register.type;
        vs.slvIdx = register.slvIdx;
        vs.haddr = register.haddr;
        vs.fpt = register.fpt;
        vs.isCoilReg = Register.isCoil(vs.slvIdx, vs.haddr);
        return validVal(vs, input);
    }

    public static boolean validVal(int type, String input) {
        return validVal(new ViewStatus(type, 0), input);
    }

    public static boolean validVal(ViewStatus vs, String input) {
        boolean valid = false;
        try {
            int type = vs.type;
            if(vs.isCoilReg) {
                long val = Long.parseLong(input);
                valid = (val >= 0 && val <= 1) ? true : false;
            } else if (type == Register.APP_INT16 || type == Register.MODBUS_INT16) {
                long val = Long.parseLong(input);
                valid = (val >= (-Short.MAX_VALUE - 1) && val <= Short.MAX_VALUE) ? true : false;
            } else if (type == Register.APP_UINT16 || type == Register.APP_BTN || type == Register.APP_SWITCH || type == Register.MODBUS_UINT16 || type == Register.MODBUS_SWITCH) {
                long val = Long.parseLong(input);
                valid = (val >= 0 && val <= (Short.MAX_VALUE * 2 + 1)) ? true : false;
            } else if (type == Register.APP_INT32 || type == Register.MODBUS_INT32) {
                long val = Long.parseLong(input);
                valid = (val >= (-Integer.MAX_VALUE - 1) && val <= Integer.MAX_VALUE) ? true : false;
            } else if (type == Register.APP_UINT32 || type == Register.MODBUS_UINT32) {
                long val = Long.parseLong(input);
                valid = (val >= 0 && val <= (Integer.MAX_VALUE * 2L + 1)) ? true : false;
            } else if (type == Register.APP_IEEE754 || type == Register.MODBUS_IEEE754) {
                Double val = Double.parseDouble(input);
                valid = (val >= (-Float.MAX_VALUE - 1) && val <= Float.MAX_VALUE) ? true : false;
            } else if (type == Register.APP_FIXPT || type == Register.MODBUS_FIXPT) {
                long val = (long)(Double.parseDouble(input) * (int)Math.pow(10, vs.fpt));
                //Log.e(Dbg._TAG_(), "input=" + input + ", val=" + val + ", max=" + Integer.MAX_VALUE);
                valid = (val >= (-Integer.MAX_VALUE - 1) && val <= Integer.MAX_VALUE) ? true : false;
            } else if (type == Register.APP_FIXPT16 || type == Register.MODBUS_FIXPT16) {
                int val = (int)(Double.parseDouble(input) * (int)Math.pow(10, vs.fpt));
                //Log.e(Dbg._TAG_(), "input=" + input + ", val=" + val + ", max=" + Short.MAX_VALUE);
                valid = (val >= (-Short.MAX_VALUE - 1) && val <= Short.MAX_VALUE) ? true : false;
            } else if (type == Register.APP_UNFIXPT16 || type == Register.MODBUS_UNFIXPT16) {
                int val = (int)(Double.parseDouble(input) * (int)Math.pow(10, vs.fpt));
                valid = (val >= 0 && val <= (Short.MAX_VALUE * 2 + 1)) ? true : false;
            } else if (type == Register.APP_UNFIXPT32 || type == Register.MODBUS_UNFIXPT32) {
                long val = (long)(Double.parseDouble(input) * (int)Math.pow(10, vs.fpt));
                valid = (val >= 0 && val <= (Integer.MAX_VALUE * 2L + 1)) ? true : false;
            } else if (type == Register.APP_UNFIXPT48 || type == Register.MODBUS_UNFIXPT48) {
                long val = (long)(Double.parseDouble(input) * 1000);
                valid = (val >= 0 && val <= 999999999) ? true : false;
            } else if (type == Register.APP_FIXPT64 || type == Register.MODBUS_FIXPT64) {
                long val = (long)(Double.parseDouble(input) * Math.pow(10, vs.fpt));
                BigDecimal bigDecimal = new BigDecimal(2).pow(52);
                long minVal = bigDecimal.multiply(BigDecimal.valueOf(-1)).longValue();
                long maxVal = bigDecimal.subtract(BigDecimal.ONE).longValue();
                //Log.e(Dbg._TAG_(), "input val = " + val + ", min ~ max " + minVal + " ~ " + maxVal);
                valid = (val >= minVal && val <= maxVal) ? true : false;
            } else if (type == Register.APP_BINARY) {
                valid = true;
            } else if (type == Register.APP_BTN) {
                valid = true;
            } else if (type == Register.APP_SWITCH) {
                valid = true;
            } else {
                Log.e(Dbg._TAG_(), "invalid type " + type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return valid;
        }
    }

    public static String getTypeName(Context context, int type) {
        if(TYPES == null) {
            initTypes(context);
        }
        Set<String> typeNames = TYPES.keySet();
        for(String typeName: typeNames) {
            if(TYPES.get(typeName) == type) {
                return typeName;
            }
        }
        return "";
    }

    public static int getTypeIdx(Context context, String typeName) {
        if(TYPES == null) {
            initTypes(context);
        }
        return TYPES.get(typeName);
    }

    public static String[] getAllTypes(Context context, int numFreeRegs) {
        if(TYPES == null || TYPES_LL == null) {
            initTypes(context);
        }
        if(numFreeRegs < 4) {
            List<String> typeList = new ArrayList<String>();
            for(String typeName: TYPES_LL) {
                if(numFreeRegs < 4 && Register.is64Bits(TYPES.get(typeName))) {
                    continue;
                } else if(numFreeRegs < 3 && Register.is48Bits(TYPES.get(typeName))) {
                    continue;
                } else if(numFreeRegs < 2 && Register.is32Bits(TYPES.get(typeName))) {
                    continue;
                } else {
                   typeList.add(typeName);
                }
            }
            return (String[]) typeList.toArray(new String[typeList.size()]);
        } else {
            return (String[]) TYPES_LL.toArray(new String[TYPES_LL.size()]);
        }
    }

    public static String[] getMasterTypes(Context context, int numFreeRegs) {
        if(TYPES == null || TYPES_LL == null) {
            initTypes(context);
        }
        List<String> typeList = new ArrayList<String>();
        for(String typeName: TYPES_LL) {
            int type = TYPES.get(typeName);
            if((numFreeRegs < 4) && Register.is64Bits(type)) {
                continue;
            } else if((numFreeRegs < 3) && Register.is48Bits(type)) {
                continue;
            } else if((numFreeRegs < 2) && Register.is32Bits(type)) {
                continue;
            } else if(Register.isAlarm(type)){
                continue;
            } else {
                typeList.add(typeName);
            }
        }
        return (String[]) typeList.toArray(new String[typeList.size()]);
    }

    public static String[] getAllRegs(Device device) {
        int numRegs = device.getNumRegs();
        String[] addrs = new String[numRegs];
        for (int i = 0 ; i < numRegs; i++) {
            addrs[i] = String.valueOf(HOLDING_SHIFT + i + 1);
        }
        return addrs;
    }

    public String toString() {
        if(is32Bits(type) || is48Bits(type) || is64Bits(type)) {
            return description  + " (" + haddr + " - " + laddr + ") ";
        } else {
            return description  + " (" + haddr + ") ";
        }
    }

    public RequestBody toMultiPart(String action, String sn) {
        MultipartBuilder entityBuilder = new MultipartBuilder();
        entityBuilder.type(MultipartBuilder.FORM);

        entityBuilder.addFormDataPart("sn", sn);
        if(action.equals("ADD") || action.equals("EDIT")) {
            if(action.equals("EDIT")) {
                entityBuilder.addFormDataPart("mbusId", id);
            }
            entityBuilder.addFormDataPart("mbusAction", action);
            entityBuilder.addFormDataPart("mbusType", String.valueOf(type));
            entityBuilder.addFormDataPart("mbusDesc", description);
            entityBuilder.addFormDataPart("mbusEnlog", (enlog ? "1" : "0"));

            if(isNumber(type) && unit != null) {
                entityBuilder.addFormDataPart("mbusUnit", unit);
            }

            if(isMathEq(type) && eq != null) {
                entityBuilder.addFormDataPart("mbusEq", eq);
            }

            if(isDisplay(type)) {
                entityBuilder.addFormDataPart("mbusDt", String.valueOf(display));
            }

            if(isMbusNumber(type)) {
                if(upBound != null) {
                    entityBuilder.addFormDataPart("mbusUp", upBound);
                }
                if(lowBound != null) {
                    entityBuilder.addFormDataPart("mbusLow", lowBound);
                }
                if(sndAlmMail != null) {
                    entityBuilder.addFormDataPart("mbusSAM", sndAlmMail);
                }
                if(maxVal != null) {
                    entityBuilder.addFormDataPart("mbusMax", maxVal);
                }
                if(minVal != null) {
                    entityBuilder.addFormDataPart("mbusMin", minVal);
                }
                if(almPri > 0) {
                    entityBuilder.addFormDataPart("mbusPri", String.valueOf(almPri));
                }
                if(almDur > 0) {
                    entityBuilder.addFormDataPart("mbusDur", String.valueOf(almDur));
                }
                if(rr1 != null) {
                    entityBuilder.addFormDataPart("mbusRR1", rr1);
                }
                if(rr2 != null) {
                    entityBuilder.addFormDataPart("mbusRR2", rr2);
                }
                if(rr3 != null) {
                    entityBuilder.addFormDataPart("mbusRR3", rr3);
                }
                if(rr4 != null) {
                    entityBuilder.addFormDataPart("mbusRR4", rr4);
                }
            }

            if(limitId != null && limitId.length > 0) {
                JSONArray jsonArray = new JSONArray();
                for (int i = 0; i < limitId.length; i++) {
                    jsonArray.put(limitId[i]);
                }
                entityBuilder.addFormDataPart("mbusLimitId", jsonArray.toString());
            }

            if(slvIdx > 0) {
                entityBuilder.addFormDataPart("mbusHaddr", slvIdx + haddr);
                entityBuilder.addFormDataPart("mbusLaddr", ((laddr == null || laddr.equals("")) ? "" : slvIdx + laddr));
                if(Register.is48Bits(type)) {
                    entityBuilder.addFormDataPart("mbusIaddr", ((iaddr == null || iaddr.equals("")) ? "" : slvIdx + iaddr));
                } else if(Register.is64Bits(type)) {
                    entityBuilder.addFormDataPart("mbusIaddr", ((iaddr == null || iaddr.equals("")) ? "" : slvIdx + iaddr));
                    entityBuilder.addFormDataPart("mbusJaddr", ((jaddr == null || jaddr.equals("")) ? "" : slvIdx + jaddr));
                }
            } else {
                entityBuilder.addFormDataPart("mbusHaddr", haddr);
                entityBuilder.addFormDataPart("mbusLaddr", ((laddr == null || laddr.equals("")) ? "" : laddr));
                if(Register.is48Bits(type)) {
                    entityBuilder.addFormDataPart("mbusIaddr", ((iaddr == null || iaddr.equals("")) ? "" : iaddr));
                } else if(Register.is64Bits(type)) {
                    entityBuilder.addFormDataPart("mbusIaddr", ((iaddr == null || iaddr.equals("")) ? "" : iaddr));
                    entityBuilder.addFormDataPart("mbusJaddr", ((jaddr == null || jaddr.equals("")) ? "" : jaddr));
                }
            }

            if(Register.isFloatVal(type)) {
                entityBuilder.addFormDataPart("mbusFpt", String.valueOf(fpt));
            }

            if(type == APP_SWITCH || type == APP_BTN) {
                entityBuilder.addFormDataPart("mbusOnVal",  (onVal  == null || onVal.equals(""))  ? "1" : onVal);
                entityBuilder.addFormDataPart("mbusOffVal", (offVal == null || offVal.equals("")) ? "0" : offVal);
            }

            if(type == APP_BTN) {
                entityBuilder.addFormDataPart("mbusBtnTime", String.valueOf(btnTime));
            }

            if(isIOSW(type) && swSN != null && swAddr != null) {
                entityBuilder.addFormDataPart("mbusSwSN", swSN);
                entityBuilder.addFormDataPart("mbusSwAddr", swAddr);
            }

            if(Register.isAlarm(type)) {
                if(refReg != null) {
                    entityBuilder.addFormDataPart("mbusRefReg", refReg);
                }
                if(almPri > 0) {
                    entityBuilder.addFormDataPart("mbusPri", String.valueOf(almPri));
                }
            }

            if(Register.isNumber(type)) {
                if(virtReg != null) {
                    entityBuilder.addFormDataPart("mbusVirt", virtReg);
                }
            }
        } else if(action.equals("DELETE")) {
            entityBuilder.addFormDataPart("mbusAction", action);
            entityBuilder.addFormDataPart("mbusId", id);
        }
        return entityBuilder.build();
    }

    public static int getDisplayTypesIdx(Context context, String typeName) {
        for(int i = 0; i < DISPALY_TYPES.length; i++) {
            if(context.getString(DISPALY_TYPES[i]).equals(typeName)) {
                return i;
            }
        }
        return -1;
    }

    public static String[] getAllDisplayTypes(Context context) {
        List<String> list = new LinkedList<String>();
        for(int i = 0; i < DISPALY_TYPES.length; i++) {
            if(i >= 3 && i <= 7) {
                if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_HYEC)) {
                    // HYEC don't want these types!
                } else {
                    list.add(context.getString(DISPALY_TYPES[i]));
                }
            } else if((i >= 10 && i <= 11)) {
                if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_YATEC) ||
                   PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_KSMT_TEST_ALL)) {
                    list.add(context.getString(DISPALY_TYPES[i]));
                }
            } else if((i >= 12 && i <= 33)) {
                if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_HYEC) ||
                   PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_KSMT_TEST_ALL)) {
                    list.add(context.getString(DISPALY_TYPES[i]));
                }
            } else if((i >= 34 && i <= 35)) {
                if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_YATEC) ||
                   PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_KSMT_TEST_ALL)) {
                    list.add(context.getString(DISPALY_TYPES[i]));
                }
            } else {
                list.add(context.getString(DISPALY_TYPES[i]));
            }
        }
        String[] array = new String[list.size()];
        list.toArray(array);
        return array;
    }

    public static String getDisplayType(Context context, int idx) {
        return context.getString(DISPALY_TYPES[idx]);
    }

    public List<ListItem> toListItems(Context context) {
        final String[] almPriString = context.getResources().getStringArray(R.array.importance);
        List<ListItem> listItems = new ArrayList<ListItem>();
        listItems.add(new ListItem(context.getString(R.string.register)));
        if(slvIdx > 0) {
            String combinedAddr = (laddr == null || laddr.equals("")) ? (slvIdx + haddr) : ((slvIdx + haddr) + "-" + (slvIdx + laddr));
            String readableMbusAddr = StrUtils.readableMbusAddr(haddr, laddr);
            listItems.add(new ListItem(ListItem.VIEW, context.getString(R.string.register_addr), readableMbusAddr + " (" + combinedAddr + ")"));
        } else {
            String combinedAddr = (laddr == null || laddr.equals("")) ? haddr : haddr + "-" + laddr;
            listItems.add(new ListItem(ListItem.VIEW, context.getString(R.string.register_addr), combinedAddr));
        }

        listItems.add(new ListItem(ListItem.VIEW, context.getString(R.string.type), getTypeName(context, type)));
        listItems.add(new ListItem(ListItem.INPUT, context.getString(R.string.description), description));

        if(isNumber(type)) {
            String _unit = (unit == null || unit.equals("") || unit.equals("null")) ? context.getString(R.string.default_name) : unit;
            listItems.add(new ListItem(ListItem.BUTTON, context.getString(R.string.unit), _unit));
        }

        // MathEQ: only support in Master Mode
        if(slvIdx > 0 && isMathEq(type)) {
            String _eq = (eq == null || eq.equals("") || eq.equals("null")) ? context.getString(R.string.none) : eq;
            listItems.add(new ListItem(ListItem.INPUT, context.getString(R.string.equation), _eq));
        }

        if(isDisplay(type)) {
            listItems.add(new ListItem(ListItem.BUTTON, context.getString(R.string.display_type), getDisplayType(context, display)));
        }

        if(Register.isFixPtVal(type)) {
            listItems.add(new ListItem(ListItem.NUMBER, context.getString(R.string.floating_points), String.valueOf(fpt)));
        } else if(type == APP_SWITCH) {
            listItems.add(new ListItem(ListItem.NUMBER, context.getString(R.string.switch_on_hint),  onVal));
            listItems.add(new ListItem(ListItem.NUMBER, context.getString(R.string.switch_off_hint), offVal));
        } else if(type == APP_BTN) {
            listItems.add(new ListItem(ListItem.NUMBER, context.getString(R.string.btn_press_hint),   onVal));
            listItems.add(new ListItem(ListItem.NUMBER, context.getString(R.string.btn_release_hint), offVal));
            listItems.add(new ListItem(ListItem.NUMBER, context.getString(R.string.btn_time), String.valueOf(btnTime)));
        } else if(isIOSW(type)) {
            listItems.add(new ListItem(ListItem.INPUT, context.getString(R.string.iosw_source), swSN + ": " + swAddr));
        } else if(Register.isAlarm(type)) {
            String _refReg = (refReg.equals("") || refReg.equals("null")) ? context.getString(R.string.none) : refReg;
            listItems.add(new ListItem(ListItem.BUTTON, context.getString(R.string.refer_register), _refReg));

            // High/Medium/Low priority
            if(PrjCfg.EN_REG_IMPORTANCE) {
                listItems.add(new ListItem(ListItem.BUTTON, context.getString(R.string.importance), almPriString[almPri]));
            }
        }
        listItems.add(new ListItem(ListItem.BUTTON, context.getString(R.string.limited_access), context.getString(R.string.limited_access_hint)));

        if(isNumber(type)) {
            listItems.add(new ListItem(ListItem.CHECKBOX, context.getString(R.string.virtual_register), virtReg));
        }

        if(Register.enCloudLogging(type)) {
            listItems.add(new ListItem(ListItem.CHECKBOX, context.getString(R.string.en_reg_logging), (enlog ? "1" : "0")));
        } else {
            listItems.add(new ListItem(ListItem.CHECKBOX, context.getString(R.string.en_reg_usb_logging), (enlog ? "1" : "0")));
        }

        // Upper/Lower Alarm: only support in master mode
        if(isMbusNumber(type) && slvIdx > 0) {
            listItems.add(new ListItem(context.getString(R.string.alarm)));
            String _upBound = (upBound == null || upBound.equals("") || upBound.equals("null")) ? context.getString(R.string.none) : upBound;
            String _lowBound = (lowBound == null || lowBound.equals("") || lowBound.equals("null")) ? context.getString(R.string.none) : lowBound;
            listItems.add(new ListItem(ListItem.NUMBER, context.getString(R.string.upbound_alarm), _upBound));
            listItems.add(new ListItem(ListItem.NUMBER, context.getString(R.string.lowbound_alarm), _lowBound));
            listItems.add(new ListItem(ListItem.NUMBER, context.getString(R.string.alarm_duration), String.valueOf(almDur)));

            // High/Medium/Low priority
            if(PrjCfg.EN_REG_IMPORTANCE) {
                listItems.add(new ListItem(ListItem.BUTTON, context.getString(R.string.importance), almPriString[almPri]));
            }

            // Send registers
            String _rr = (rr1.equals("") || rr1.equals("null")) ? context.getString(R.string.none) : rr1;
            listItems.add(new ListItem(ListItem.BUTTON, context.getString(R.string.refer_register_1), _rr));

            // Send Email
            listItems.add(new ListItem(ListItem.CHECKBOX, context.getString(R.string.send_alarm_email), sndAlmMail));

            // Send registers
            if(PrjCfg.EN_REG_4RR) {
                listItems.add(new ListItem(context.getString(R.string.refer_register_title)));

                _rr = (rr2.equals("") || rr2.equals("null")) ? context.getString(R.string.none) : rr2;
                listItems.add(new ListItem(ListItem.BUTTON, context.getString(R.string.refer_register_2), _rr));
                _rr = (rr3.equals("") || rr3.equals("null")) ? context.getString(R.string.none) : rr3;
                listItems.add(new ListItem(ListItem.BUTTON, context.getString(R.string.refer_register_3), _rr));
                _rr = (rr4.equals("") || rr4.equals("null")) ? context.getString(R.string.none) : rr4;
                listItems.add(new ListItem(ListItem.BUTTON, context.getString(R.string.refer_register_4), _rr));
            }

            // Register Range
            if(PrjCfg.EN_REG_RANGE) {
                listItems.add(new ListItem(context.getString(R.string.register_range)));
                String _maxVal = (maxVal == null || maxVal.equals("") || maxVal.equals("null")) ? context.getString(R.string.none) : maxVal;
                String _minVal = (minVal == null || minVal.equals("") || minVal.equals("null")) ? context.getString(R.string.none) : minVal;
                listItems.add(new ListItem(ListItem.NUMBER, context.getString(R.string.max), _maxVal));
                listItems.add(new ListItem(ListItem.NUMBER, context.getString(R.string.min), _minVal));
            }
        }
        return listItems;
    }
}
