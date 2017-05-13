package tw.com.ksmt.cloud.iface;

import android.util.Log;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

import tw.com.ksmt.cloud.libs.Dbg;

public class ViewStatus implements Serializable {
    // Device configs
    public String  sn;
    public String  devName;
    public String  mo;
    public int     slvIdx;
    public int     devStat;
    public int     logFreq;
    public boolean devEnlog;
    public boolean devEnServlog;
    public String  slvNames = null;

    // Register configs
    public String id;
    public String origId; // original id (the same id on the server)
    public String desc;
    public String unit;
    public boolean userControl;
    public boolean wrOnce = false;
    public boolean isCoilReg = false;

    public String haddr;
    public String hval;
    public String iaddr;
    public String ival;
    public String jaddr;
    public String jval;
    public String laddr;
    public String lval;
    public String showVal;
    public String sortId;

    public String onVal;
    public String offVal;
    public String upVal;
    public String lowVal;
    public boolean enlog;
    public boolean isBtnOn = false;
    public int display = 0;
    public int type;
    public int fpt;

    // IOSW
    public String swSN;
    public String swAddr;
    public int swType;

    public ViewStatus() {
    }

    public ViewStatus(Device device) {
        this.sn           = device.sn;
        this.devName      = device.name;
        this.mo           = device.model;
        this.slvIdx       = device.slvIdx;
        this.devStat      = device.status;
        this.devEnlog     = device.enlog;
        this.devEnServlog = device.enServLog;
        this.logFreq      = device.logFreq;

        if(device.slvIdx > 0 && device.slvNames != null) {
            try {
                this.slvNames = device.slvNames.getString(String.valueOf(device.slvIdx));
                Log.e(Dbg._TAG_(), "this.slvNames=" + this.slvNames);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // only use for Register to valid value
    public ViewStatus(int type, int fpt) {
        this.type = type;
        this.fpt = fpt;
    }

    // for LineChart 16bits
    public ViewStatus(int type, String haddr, String hval, int fpt) {
        this.type = type;
        this.haddr = haddr;
        this.hval = hval;
        this.fpt = fpt;
        this.showVal = setShowVal(type);
    }

    // for LineChart 32bits
    public ViewStatus(int type, String haddr, String hval, String laddr, String lval, int fpt) {
        this.type = type;
        this.haddr = haddr;
        this.hval = hval;
        this.laddr = laddr;
        this.lval = lval;
        this.fpt = fpt;
        this.showVal = setShowVal(type);
    }

    // for LineChart 48bits
    public ViewStatus(int type, String haddr, String hval, String iaddr, String ival, String laddr, String lval, int fpt) {
        this.type = type;
        this.haddr = haddr;
        this.hval = hval;
        this.iaddr = iaddr;
        this.ival = ival;
        this.laddr = laddr;
        this.lval = lval;
        this.fpt = fpt;
        this.showVal = setShowVal(type);
    }

    // for LineChart 64bits
    public ViewStatus(int type, String haddr, String hval, String iaddr, String ival, String jaddr, String jval, String laddr, String lval, int fpt) {
        this.type = type;
        this.haddr = haddr;
        this.hval = hval;
        this.iaddr = iaddr;
        this.ival = ival;
        this.jaddr = jaddr;
        this.jval = jval;
        this.laddr = laddr;
        this.lval = lval;
        this.fpt = fpt;
        this.showVal = setShowVal(type);
    }

    public void setNewVal(String newVal) {
        try {
            String hex = "";
            if (this.type == Register.APP_INT16 || this.type == Register.MODBUS_INT16) {
                hex = Integer.toHexString(Integer.parseInt(newVal));
            } else if (this.type == Register.APP_UINT16 || this.type == Register.MODBUS_UINT16 || this.type == Register.MODBUS_SWITCH) {
                hex = Integer.toHexString(Integer.parseInt(newVal));
            } else if (this.type == Register.APP_INT32 || this.type == Register.MODBUS_INT32) {
                hex = Integer.toHexString(Integer.parseInt(newVal));
            } else if (this.type == Register.APP_UINT32 || this.type == Register.MODBUS_UINT32) {
                hex = Long.toHexString(Long.parseLong(newVal));
            } else if (this.type == Register.APP_IEEE754 || this.type == Register.MODBUS_IEEE754) {
                hex = Integer.toHexString(Float.floatToRawIntBits(Float.parseFloat(newVal)));
            } else if (this.type == Register.APP_FIXPT || this.type == Register.MODBUS_FIXPT) {
                hex = Integer.toHexString((int) (new BigDecimal(newVal)).multiply(new BigDecimal(Math.pow(10, fpt))).doubleValue());
            } else if (this.type == Register.APP_FIXPT16 || this.type == Register.MODBUS_FIXPT16) {
                hex = Integer.toHexString((int) (new BigDecimal(newVal)).multiply(new BigDecimal(Math.pow(10, fpt))).doubleValue());
            } else if (this.type == Register.APP_FIXPT64 || this.type == Register.MODBUS_FIXPT64) {
                hex = Long.toHexString((long)(new BigDecimal(newVal)).multiply(new BigDecimal(Math.pow(10, fpt))).doubleValue());
            } else if (this.type == Register.APP_UNFIXPT16 || this.type == Register.MODBUS_UNFIXPT16) {
                hex = Integer.toHexString((int) (new BigDecimal(newVal)).multiply(new BigDecimal(Math.pow(10, fpt))).doubleValue());
            } else if (this.type == Register.APP_UNFIXPT32 || this.type == Register.MODBUS_UNFIXPT32) {
                hex = Long.toHexString((long) (new BigDecimal(newVal)).multiply(new BigDecimal(Math.pow(10, fpt))).doubleValue());
            } else if (this.type == Register.APP_UNFIXPT48 || this.type == Register.MODBUS_UNFIXPT48) {
                long tmpVal = (new BigDecimal(newVal)).longValue();
                hval = Integer.toHexString((int) (tmpVal / 1000 % 1000));
                ival = Integer.toHexString((int) (tmpVal % 1000));

                int _fpt = (int)Math.pow(10, 3);
                tmpVal = (long)((new BigDecimal(newVal)).multiply(new BigDecimal(_fpt)).doubleValue());
                lval = Integer.toHexString((int) (tmpVal % _fpt));
                //Log.e(Dbg._TAG_(), "tmpVal="+tmpVal);
                //Log.e(Dbg._TAG_(),"_fpt=" + _fpt + ",fpt="+fpt+",hval="+hval+",ival="+ival+",lval="+lval);
            } else if (this.type == Register.APP_BINARY || this.type == Register.MODBUS_BINARY) {
                hex = Integer.toHexString(Integer.parseInt(newVal, 2));
            } else if (this.type == Register.APP_BTN) {
                hex = newVal;
            } else if (this.type == Register.APP_SWITCH) {
                hex = newVal;
            }

            if (Register.is64Bits(type)) {
                newVal = String.format("%16s", hex).replace(' ', '0');
                Log.e(Dbg._TAG_(), "Set newVal = 0x" + newVal);
                hval = newVal.substring(0, 4);
                ival = newVal.substring(4, 8);
                jval = newVal.substring(8, 12);
                lval = newVal.substring(12, 16);
            } else if(Register.is48Bits(type)) {
                // Do nothing
            } else {
                newVal = String.format("%8s", hex).replace(' ', '0');
                Log.e(Dbg._TAG_(), "Set newVal = 0x" + newVal);
                if (Register.is32Bits(type)) {
                    hval = newVal.substring(0, 4);
                    lval = newVal.substring(4, 8);
                } else {
                    hval = newVal.substring(4, 8);
                    lval = null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String setShowVal(int type) {
        String showVal = "";
        try {
            if(hval == null || hval.equals("")) {
                return showVal;
            }
            if (type == Register.MODBUS_INT16 || type == Register.APP_INT16) {
                showVal = Short.toString((short) Integer.parseInt(hval, 16));
            } else if (type == Register.MODBUS_UINT16 || type == Register.APP_UINT16 || type == Register.MODBUS_SWITCH) {
                showVal = Integer.toString(Integer.parseInt(hval, 16));
            } else if (type == Register.MODBUS_FIXPT16 || type == Register.APP_FIXPT16) {
                double fval = ((short) Long.parseLong(hval, 16)/ Math.pow(10, fpt));
                showVal = String.format("%." + fpt + "f", fval);
            } else if (type == Register.MODBUS_UNFIXPT16 || type == Register.APP_UNFIXPT16) {
                double fval = ((int) Long.parseLong(hval, 16)/ Math.pow(10, fpt));
                showVal = String.format("%." + fpt + "f", fval);
            } else if (type == Register.MODBUS_BINARY || type == Register.APP_BINARY) {
                showVal = Integer.toBinaryString(Integer.parseInt(hval, 16));
                showVal = String.format("%16s", showVal).replace(' ', '0');
                showVal = showVal.substring(0, 4) + " " + showVal.substring(4, 8) + "<br/>" + showVal.substring(8, 12) + " " + showVal.substring(12, 16);
            } else if (Register.isAlarm(type)) {
                showVal = Integer.toString(Integer.parseInt(hval, 16));
            } else if (type == Register.APP_SWITCH) {
                //showVal = Short.toString((short) Integer.parseInt(hval, 16));
                showVal = Integer.toString(Integer.parseInt(hval, 16));
            } else if (type == Register.APP_BTN) {
                //showVal = Short.toString((short) Integer.parseInt(hval, 16));
                showVal = Integer.toString(Integer.parseInt(hval, 16));
            } else if (iaddr != null && ival != null && jaddr != null && jval != null && laddr != null && lval != null) { // 64 Bits
                String values = String.format("%4s", hval).replace(' ', '0') + String.format("%4s", ival).replace(' ', '0') + String.format("%4s", jval).replace(' ', '0') + String.format("%4s", lval).replace(' ', '0');
                if (type == Register.MODBUS_FIXPT64 || type == Register.APP_FIXPT64) {
                    long lval = new BigInteger(values, 16).longValue();
                    BigDecimal fval = (new BigDecimal(lval).divide(BigDecimal.valueOf(Math.pow(10, fpt))));
                    showVal = String.format("%." + fpt + "f", fval);
                }
            } else if (iaddr != null && ival != null && laddr != null && lval != null) { // 48 Bits
                if (type == Register.MODBUS_UNFIXPT48 || type == Register.APP_UNFIXPT48) {
                    if(fpt > 0) {
                        double val = Integer.parseInt(hval, 16) * 1000 + Integer.parseInt(ival, 16) + (Integer.parseInt(lval, 16) / 1000.0);
                        int shift = (int) Math.pow(10, fpt);
                        val = (Math.floor(val * shift) / shift);
                        showVal = String.format("%." + fpt + "f", val);
                    } else {
                        int val = Integer.parseInt(hval, 16) * 1000 + Integer.parseInt(ival, 16);
                        showVal = String.valueOf(val);
                    }
                }
            } else if (laddr != null && lval != null){ // 32 Bits
                String values = String.format("%4s", hval).replace(' ', '0') + String.format("%4s", lval).replace(' ', '0');
                if (type == Register.MODBUS_INT32 || type == Register.APP_INT32) {
                    showVal = Integer.toString((int) Long.parseLong(values, 16));
                } else if (type == Register.MODBUS_UINT32 || type == Register.APP_UINT32) {
                    showVal = Long.toString(Long.parseLong(values, 16));
                } else if (type == Register.MODBUS_UNFIXPT32 || type == Register.APP_UNFIXPT32) {
                    double fval = ((long) Long.parseLong(values, 16)/ Math.pow(10, fpt));
                    showVal = String.format("%." + fpt + "f", fval);
                } else if (type == Register.MODBUS_IEEE754 || type == Register.APP_IEEE754) {
                    //Log.e(Dbg._TAG_(), "val=" + values + " vs.=" + vs.desc);
                    String firstBin = String.format("%4s", Integer.toBinaryString(Integer.valueOf(values.substring(0, 1), 16))).replace(' ', '0');
                    String restBin = String.format("%28s", Integer.toBinaryString(Integer.valueOf(values.substring(1, 8), 16))).replace(' ', '0');
                    String finalVal = Float.toString(Float.intBitsToFloat(Integer.valueOf(firstBin.substring(1, 4) + restBin, 2)));
                    showVal = (firstBin.charAt(0) == '1') ? "-" + finalVal : finalVal ;
                    float fval = Float.parseFloat(showVal);
                    showVal = (fval == (long)fval) ? String.format("%d", (long)fval) : String.format("%s", fval);
                } else if (type == Register.MODBUS_FIXPT || type == Register.APP_FIXPT) {
                    double fval = ((int) Long.parseLong(values, 16)/ Math.pow(10, fpt));
                    showVal = String.format("%." + fpt + "f", fval);
                }
            }
            if(this.type == Register.APP_BTN || this.type == Register.APP_SWITCH) {
                if(this.onVal != null && !this.onVal.equals("") && !this.hval.equals("")) {
                    this.isBtnOn = (Integer.parseInt(this.onVal, 16) == Integer.parseInt(this.hval, 16)) ? true : false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return showVal;
        }
    }
}
