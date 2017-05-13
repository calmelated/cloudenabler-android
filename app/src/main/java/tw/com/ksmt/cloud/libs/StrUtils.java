package tw.com.ksmt.cloud.libs;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StrUtils {
    static final String HEXES = "0123456789ABCDEF";
    public static final int IN_TYPE_NONE_EMPTY = 0x1;
    public static final int IN_TYPE_EMAIL = 0x2;
    public static final int IN_TYPE_IPV4 = 0x3;
    public static final int IN_TYPE_LEAST_6_LETTERS = 0x4;
    public static final int IN_TYPE_LEAST_8_LETTERS = 0x8;
    public static final int IN_TYPE_MAC = 0x5;
    public static final int IN_TYPE_STRING = 0x6;
    public static final int IN_TYPE_STRONG_PSWD = 0x7;
    public static final int IN_TYPE_IP_PORT = 0x9;
    public static final int IN_TYPE_MBUS_ADDR = 0xa;

    public static String getHex(byte[] raw) {
        if (raw == null) {
            return null;
        }
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }

    public static String toHexString(byte[] in) {
        BigInteger temp = new BigInteger(in);
        return temp.toString(16);
    }

    public static byte[] fromHexString(String in) {
        // BigInteger temp = new BigInteger(in, 16);
        byte[] tmp = new byte[in.length() / 2];
        for (int i = 0; i < in.length(); i += 2) {
            BigInteger temp = new BigInteger(in.substring(i, i + 2), 16);
            tmp[i / 2] = temp.byteValue();
        }
        // return temp.toByteArray();
        return tmp;
    }

    public static String getTimeString(long timeInterval) {
        java.util.Date utilDate = new java.util.Date();
        utilDate.setTime(timeInterval);
        long minutes = (timeInterval / 1000) / 60;
        long seconds = (timeInterval / 1000) % 60;
        long milliseconds = timeInterval % 1000;

        String m;
        String s;
        String ms;

        m = String.valueOf(minutes);
        if (seconds < 10) {
            s = "0" + seconds;
        } else {
            s = String.valueOf(seconds);
        }

        if (milliseconds < 10) {
            ms = "0" + milliseconds;
        } else {
            ms = String.valueOf(milliseconds);
        }
        String t = m + ":" + s + "." + ms;
        return t;
    }

    public static String intToIp(int i) {
        /*
		   ((i >> 24 ) & 0xFF ) + "." +
		               ((i >> 16 ) & 0xFF) + "." +
		               ((i >> 8 ) & 0xFF) + "." +
		               ( i & 0xFF) ;
		   */
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + ((i >> 24) & 0xFF);
    }

    public static boolean validateInput(int type, String input) {
        boolean result = true;
        if (type == IN_TYPE_EMAIL) {
            //final String emailPattern = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";
            final String emailPattern = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
            if (!input.matches(emailPattern)) {
                result = false;
            }
        } else if (type == IN_TYPE_NONE_EMPTY) {
            if (input.length() < 1 || input.equalsIgnoreCase("")) {
                result = false;
            }
        } else if (type == IN_TYPE_LEAST_6_LETTERS) {
            if (input.length() < 6) {
                result = false;
            }
        } else if (type == IN_TYPE_LEAST_8_LETTERS) {
            if (input.length() < 8) {
                result = false;
            }
        } else if (type == IN_TYPE_IPV4) {
            if(input.equals("0.0.0.0")) {
                return false;
            }
            final String PATTERN = "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
            Pattern pattern = Pattern.compile(PATTERN);
            Matcher matcher = pattern.matcher(input);
            result = matcher.matches();
        } else if (type == IN_TYPE_MAC) {
            final String PATTERN = "^([0-9A-F]{2}[:-]){5}([0-9A-F]{2})$";
            Pattern pattern = Pattern.compile(PATTERN);
            Matcher matcher = pattern.matcher(input.toUpperCase());
            result = matcher.matches();
        } else if (type == IN_TYPE_STRING) {
            if(input.contains("\"") || input.contains("'") || input.contains("\\")) {
                return false;
            }
            return true;
        } else if(type == IN_TYPE_STRONG_PSWD) {
            if(input.matches("^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])\\w{6,}$")) {
                return true;
            }
            return false;
        } else if(type == IN_TYPE_IP_PORT) {
            try {
                int port = Integer.parseInt(input);
                return (port > 0 && port < 65536);
            } catch (Exception e) {
                return false;
            }
        } else if(type == IN_TYPE_MBUS_ADDR) {
            return (validMbusAddr(input) < 0) ? false : true;
        }
        return result;
    }

    public static int validMbusAddr(String input) {
        try {
            int maddr = 0;
            if(input.matches("^0x[0-9abcdefABCDEF]{1,4}")) { // 0x123f
                String _input = (input.length() < 6) ? input.substring(2, input.length()) : input.substring(2, 6);
                maddr = Integer.parseInt(_input, 16);
            } else if(input.matches("[0-9abcdefABCDEF]{1,4}[hH]$")) { // 123fh or 123fH
                String _input = input.substring(0, input.length() - 1);
                maddr = Integer.parseInt(_input, 16);
            } else if(input.matches("[0-9]{1,5}")) {
                maddr = Integer.parseInt(input);
            } else {
                return -1;
            }
            return (maddr >= 0 && maddr < 65535) ? maddr : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    //465535 -> 4 + 65535 -> FC: 3
    public static String getFCByAddr(String addr) {
        try {
            int fcIdx = Integer.parseInt(addr) / 100000;
            if(fcIdx == 0) {
                return "01";
            } else if(fcIdx == 1) {
                return "02";
            } else if(fcIdx == 3) {
                return "04";
            } else if(fcIdx == 4) {
                return "03";
            } else if(fcIdx == 5) { // write coil
                return "05";
            } else if(fcIdx == 6) { // write holding
                return "06";
            } else if(fcIdx == 7) { // write multi-holding
                return "16";
            } else if(fcIdx == 8) { // write multi-coils
                return "15";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getMbusBaddr(String addr) {
        try {
            int decVal = (Integer.parseInt(addr) % 100000) - 1;
            return Integer.toHexString(decVal).toUpperCase();
        } catch (Exception e) {
        }
        return "-1";
    }

    public static String readableMbusAddr(String haddr, String laddr) {
        String fc = StrUtils.getFCByAddr(haddr);
        String addr = StrUtils.getMbusBaddr(haddr);
        if(laddr == null || laddr.equals("")) {
            return "FC-" + fc + ", 0x" + addr;
        } else {
            return "FC-" + fc + ", 0x" + addr + "-0x" + StrUtils.getMbusBaddr(laddr);
        }
    }

    public static void setPktStr(byte[] srcData, byte[] addData) {
        System.arraycopy(addData, 0, srcData, 0, addData.length);
    }

    public static void setPktStr(byte[] srcData, int pos, byte[] addData) {
        System.arraycopy(addData, 0, srcData, pos, addData.length);
    }

    public static String getPktStr(byte[] data, int start, int len) {
        String resultStr = "";
        try {
            byte[] info = new byte[len];
            System.arraycopy(data, start, info, 0, len);
            int index = -1;
            for (int i = 0; i < info.length; i++) {
                if (info[i] == 0) {
                    index = i;
                    break;
                }
            }
            if (index != -1) {
                byte[] t = new byte[index + 1];
                System.arraycopy(info, 0, t, 0, index);
                resultStr = new String(t, "UTF-8").trim();
            } else {
                resultStr = new String(info, "UTF-8").trim();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultStr;
    }
}
