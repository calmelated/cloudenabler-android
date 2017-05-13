package tw.com.ksmt.cloud.iface;

import java.util.Calendar;
import java.util.Date;

public class TCPFrame {
    public static String CMD_BIND_REQ = "BIND-REQ";
    public static String CMD_BIND_RSP = "BIND_RSP";
    public static String CMD_APCFGREQ = "APCFGREQ";
    public static String CMD_APCFGRSP = "APCFGRSP";
    public static String CMD_FVER_REQ = "FVER-REQ";
    public static String CMD_FVER_RSP = "FVER-RSP";
    public static String CMD_FVER2REQ = "FVER2REQ";
    public static String CMD_FVER2RSP = "FVER2RSP";
    public static String CMD_TIME_REQ = "TIME-REQ";
    public static String CMD_TIME_RSP = "TIME-RSP";
    public static String CMD_IOSTSREQ = "IOSTSREQ";
    public static String CMD_IOSTSRSP = "IOSTSRSP";
    public static String CMD_UPFW_REQ = "UPFW-REQ";
    public static String CMD_REPLYCMD = "REPLYCMD";
    public static String CMD_CFSTSREQ = "CFSTSREQ";
    public static String CMD_CFSTSRSP = "CFSTSRSP";
    public static String CMD_RTCTLREQ = "RTCTLREQ";
    public static String CMD_GAECFREQ = "GAECFREQ";
    public static String CMD_GAECFRSP = "GAECFRSP";
    public static String CMD_MISC_REQ = "MISC-REQ";
    public static String CMD_MISC_RSP = "MISC-RSP";
    public static String CMD_NTPC_REQ = "NTPC-REQ";
    public static String CMD_NTPC_RSP = "NTPC-RSP";
    public static String CMD_UART_REQ = "UART-REQ";
    public static String CMD_UART_RSP = "UART-RSP";
    public static String CMD_WMAC_REQ = "WMAC-REQ";
    public static String CMD_WMAC_RSP = "WMAC-RSP";

    public static enum FileType {
        CB_BINARY_FILE,
        CB_DESCRIPTION_FILE
    }

    public static enum CFSTSREQ_OperationMode {
        GET_CB_HEADER,
        GET_THE_FULL_CB_FILE,
        SET_OPERATION_MODE,
        GET_CB_DESCRIPTION_FILE
    }

    public static enum DeviceOperationMode {
        CB_MODE,
        MODBUS_MODE
    }

    public static enum MISC_Type {
        RESERVED,
        RESET_SYSTEM
    }

    public static Boolean frameCheck(byte[] frame) {
        short checkSum = (short) ((short) ((frame[frame.length - 2] << 8) & 0xffff) + (short) (frame[frame.length - 1] & 0xff));
        short sum = 0;
        for (int index = 0; index < frame.length - 2; index++) {
            sum = (short) (sum + (short) (frame[index] & 0xff));
        }
        if (sum == checkSum) {
            return true;
        } else {
            return false;
        }
    }

    public static byte[] getBIND_REQ(byte bindingTimeInterval) {
        byte[] command = new byte[15];
        String cmd = CMD_BIND_REQ;
        byte[] tmp = cmd.getBytes();
        System.arraycopy(tmp, 0, command, 0, tmp.length);
        command[8] = 0;
        command[9] = 1;
        command[10] = bindingTimeInterval;
        short checkSum = 0;
        for (int index = 0; index < 11; index++) {
            checkSum = (short) (checkSum + (short) (command[index] & 0xff));
        }
        command[11] = (byte) (checkSum >> 8);
        command[12] = (byte) checkSum;
        command[command.length - 2] = 0x0D;
        command[command.length - 1] = 0x0A;
        return command;
    }

    public static byte[] getAPCFGREQ(int type) {
        byte[] command = new byte[15];
        String cmd = CMD_APCFGREQ;
        byte[] tmp = cmd.getBytes();
        System.arraycopy(tmp, 0, command, 0, tmp.length);
        command[8] = 0;
        command[9] = 1;
        command[10] = 0;
        short checkSum = 0;
        for (int index = 0; index < 11; index++) {
            checkSum = (short) (checkSum + (short) (command[index] & 0xff));
        }
        command[11] = (byte) (checkSum >> 8);
        command[12] = (byte) checkSum;
        command[command.length - 2] = 0x0D;
        command[command.length - 1] = 0x0A;
        return command;
    }

    public static byte[] getWMACREQ() {
        byte[] command = new byte[136];
        String cmd = CMD_WMAC_REQ;
        byte[] tmp = cmd.getBytes();
        System.arraycopy(tmp, 0, command, 0, tmp.length);
        command[8] = 0;
        command[9] = 0x7a;
        command[10] = 0; // GET
        short checkSum = 0;
        for (int index = 0; index < 132; index++) {
            checkSum = (short) (checkSum + (short) (command[index] & 0xff));
        }
        command[132] = (byte) (checkSum >> 8);
        command[133] = (byte) checkSum;
        command[command.length - 2] = 0x0D;
        command[command.length - 1] = 0x0A;
        return command;
    }

    public static byte[] getAPCFGREQ(byte type) {
        byte[] command = new byte[15];
        String cmd = CMD_APCFGREQ;
        byte[] tmp = cmd.getBytes();
        System.arraycopy(tmp, 0, command, 0, tmp.length);
        command[8] = 0;
        command[9] = 1;
        command[10] = type;
        short checkSum = 0;
        for (int index = 0; index < 11; index++) {
            checkSum = (short) (checkSum + (short) (command[index] & 0xff));
        }
        command[11] = (byte) (checkSum >> 8);
        command[12] = (byte) checkSum;
        command[command.length - 2] = 0x0D;
        command[command.length - 1] = 0x0A;
        return command;
    }

    public static byte[] getFVER_REQ() {
        byte[] command = new byte[14];
        String cmd = CMD_FVER_REQ;
        byte[] tmp = cmd.getBytes();
        System.arraycopy(tmp, 0, command, 0, tmp.length);
        command[8] = 0;
        command[9] = 0;
        short checkSum = 0;
        for (int index = 0; index < 10; index++) {
            checkSum = (short) (checkSum + (short) (command[index] & 0xff));
        }
        command[10] = (byte) (checkSum >> 8);
        command[11] = (byte) checkSum;
        command[command.length - 2] = 0x0D;
        command[command.length - 1] = 0x0A;
        return command;
    }

    public static byte[] getSET_TIME_REQ() {
        byte[] command = new byte[32];
        String cmd = CMD_TIME_REQ;
        byte[] tmp = cmd.getBytes();
        System.arraycopy(tmp, 0, command, 0, tmp.length);
        command[8] = 0;
        command[9] = 0x12;
        command[10] = 1;
        command[11] = 0;
        Calendar calendar = Calendar.getInstance();
        command[12] = (byte) ((calendar.get(Calendar.YEAR) & 0xff00) >> 8);
        command[13] = (byte) (calendar.get(Calendar.YEAR) & 0x00ff);
        command[14] = (byte) (calendar.get(Calendar.MONTH) + 1);
        command[15] = (byte) calendar.get(Calendar.DAY_OF_MONTH);
        command[16] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
        command[17] = (byte) calendar.get(Calendar.MINUTE);
        command[18] = (byte) calendar.get(Calendar.SECOND);
        command[19] = (byte) calendar.get(Calendar.DAY_OF_WEEK);
        command[20] = (byte) (((calendar.getTimeZone().getRawOffset() / 1000) & 0xff000000) >> 24);
        command[21] = (byte) (((calendar.getTimeZone().getRawOffset() / 1000) & 0x00ff0000) >> 16);
        command[22] = (byte) (((calendar.getTimeZone().getRawOffset() / 1000) & 0x0000ff00) >> 8);
        command[23] = (byte) ((calendar.getTimeZone().getRawOffset() / 1000) & 0x000000ff);
        command[24] = (byte) (((calendar.getTimeZone().getDSTSavings() / 1000) & 0xff000000) >> 24);
        command[25] = (byte) (((calendar.getTimeZone().getDSTSavings() / 1000) & 0x00ff0000) >> 16);
        command[26] = (byte) (((calendar.getTimeZone().getDSTSavings() / 1000) & 0x0000ff00) >> 8);
        command[27] = (byte) ((calendar.getTimeZone().getDSTSavings() / 1000) & 0x000000ff);

        short checkSum = 0;
        for (int index = 0; index < 28; index++) {
            checkSum = (short) (checkSum + (short) (command[index] & 0xff));
        }
        command[28] = (byte) (checkSum >> 8);
        command[29] = (byte) checkSum;
        command[command.length - 2] = 0x0D;
        command[command.length - 1] = 0x0A;
        return command;
    }

    public static byte[] getGET_TIME_REQ() {
        byte[] command = new byte[15];
        String cmd = CMD_TIME_REQ;
        byte[] tmp = cmd.getBytes();
        System.arraycopy(tmp, 0, command, 0, tmp.length);
        command[8] = 0;
        command[9] = 0x1;
        command[10] = 0;
        short checkSum = 0;
        for (int index = 0; index < 11; index++) {
            checkSum = (short) (checkSum + (short) (command[index] & 0xff));
        }
        command[11] = (byte) (checkSum >> 8);
        command[12] = (byte) checkSum;
        command[command.length - 2] = 0x0D;
        command[command.length - 1] = 0x0A;
        return command;
    }

    public static byte[] getGET_NTPC_REQ() {
        byte[] command = new byte[160];
        String cmd = CMD_NTPC_REQ;
        byte[] tmp = cmd.getBytes();
        System.arraycopy(tmp, 0, command, 0, tmp.length);
        command[8] = 0;
        command[9] = (byte) 0x92; // len: 146
        command[10] = 0; //Type: 0: Get, 1: Set
        short checkSum = 0;
        for (int index = 0; index < 156; index++) {
            checkSum = (short) (checkSum + (short) (command[index] & 0xff));
        }
        command[156] = (byte) (checkSum >> 8);
        command[157] = (byte) checkSum;
        command[command.length - 2] = 0x0D;
        command[command.length - 1] = 0x0A;
        return command;
    }

    public static byte[] getGET_UART_REQ() {
        byte[] command = new byte[80];
        String cmd = CMD_UART_REQ;
        byte[] tmp = cmd.getBytes();
        System.arraycopy(tmp, 0, command, 0, tmp.length);
        command[8] = 0;
        command[9] = (byte) 0x42; // len: 66
        command[10] = 0; //Type: 0: Get, 1: Set
        short checkSum = 0;
        for (int index = 0; index < 76; index++) {
            checkSum = (short) (checkSum + (short) (command[index] & 0xff));
        }
        command[76] = (byte) (checkSum >> 8);
        command[77] = (byte) checkSum;
        command[command.length - 2] = 0x0D;
        command[command.length - 1] = 0x0A;
        return command;
    }

    public static byte[] getSET_APCFGREQ(String SSID, byte encryption, String encryption_key, byte WPA_cipher_suit, byte WPA2_cipher_suit) {
        byte[] command = new byte[392];
        String cmd = CMD_APCFGREQ;
        byte[] tmp = cmd.getBytes();
        System.arraycopy(tmp, 0, command, 0, tmp.length);
        command[8] = 0x01;
        command[9] = 0x7A;
        command[10] = 1;
        command[11] = 1;
        command[12] = 0;
        command[13] = 11;
        command[14] = 0;
        command[15] = 0;
        //command[16]
        byte[] bSSID = SSID.getBytes();
        System.arraycopy(bSSID, 0, command, 16, bSSID.length);
        command[48] = 0;
        command[49] = encryption;
        command[50] = 0;
        command[51] = 2;
        command[52] = WPA_cipher_suit;
        command[53] = WPA2_cipher_suit;
        byte[] bKey = encryption_key.getBytes();
        command[54] = (byte) bKey.length;
        //command[55]
        System.arraycopy(bKey, 0, command, 55, bKey.length);
        command[119] = 1;
        command[120] = 0;
        command[124] = 0;
        command[128] = 0;
        command[132] = 0;

        short checkSum = 0;
        for (int index = 0; index < 388; index++) {
            checkSum = (short) (checkSum + (short) (command[index] & 0xff));
        }
        command[388] = (byte) (checkSum >> 8);
        command[389] = (byte) checkSum;
        command[command.length - 2] = 0x0D;
        command[command.length - 1] = 0x0A;
        return command;
    }

    public static byte[] getIOSTSREQ() {
        byte[] command = new byte[14];
        String cmd = CMD_IOSTSREQ;
        byte[] tmp = cmd.getBytes();
        System.arraycopy(tmp, 0, command, 0, tmp.length);
        command[8] = 0;
        command[9] = 0;
        short checkSum = 0;
        for (int index = 0; index < 10; index++) {
            checkSum = (short) (checkSum + (short) (command[index] & 0xff));
        }
        command[10] = (byte) (checkSum >> 8);
        command[11] = (byte) checkSum;
        command[command.length - 2] = 0x0D;
        command[command.length - 1] = 0x0A;
        return command;
    }

    public static byte[] getCFSTSREQ() {
        byte[] command = new byte[14];
        String cmd = CMD_CFSTSREQ;
        byte[] tmp = cmd.getBytes();
        System.arraycopy(tmp, 0, command, 0, tmp.length);
        command[8] = 0;
        command[9] = 0;
        short checkSum = 0;
        for (int index = 0; index < 10; index++) {
            checkSum = (short) (checkSum + (short) (command[index] & 0xff));
        }
        command[10] = (byte) (checkSum >> 8);
        command[11] = (byte) checkSum;
        command[command.length - 2] = 0x0D;
        command[command.length - 1] = 0x0A;
        return command;
    }

    public static byte[] getRTCTLREQ(int mode, int remoteId, int value) {
        byte[] command = new byte[17];
        String cmd = CMD_RTCTLREQ;
        byte[] tmp = cmd.getBytes();
        System.arraycopy(tmp, 0, command, 0, tmp.length);
        command[8] = 0;
        command[9] = 3;
        command[10] = (byte) mode;
        command[11] = (byte) remoteId;
        command[12] = (byte) value;
        short checkSum = 0;
        for (int index = 0; index < 13; index++) {
            checkSum = (short) (checkSum + (short) (command[index] & 0xff));
        }
        command[13] = (byte) (checkSum >> 8);
        command[14] = (byte) checkSum;
        command[command.length - 2] = 0x0D;
        command[command.length - 1] = 0x0A;
        return command;
    }

    public static byte[] getCFSTSREQ_1(CFSTSREQ_OperationMode mode, DeviceOperationMode type) {
        byte[] command = new byte[16];
        String cmd = CMD_CFSTSREQ;
        byte[] tmp = cmd.getBytes();
        System.arraycopy(tmp, 0, command, 0, tmp.length);
        command[8] = 0;
        command[9] = 2;
        command[10] = (byte) mode.ordinal();
        command[11] = (byte) type.ordinal();
        short checkSum = 0;
        for (int index = 0; index < 12; index++) {
            checkSum = (short) (checkSum + (short) (command[index] & 0xff));
        }
        command[12] = (byte) (checkSum >> 8);
        command[13] = (byte) checkSum;
        command[command.length - 2] = 0x0D;
        command[command.length - 1] = 0x0A;
        return command;
    }

    public static byte[] getGAECFREQ_GET() {
        byte[] command = new byte[16];
        String cmd = CMD_GAECFREQ;
        byte[] tmp = cmd.getBytes();
        System.arraycopy(tmp, 0, command, 0, tmp.length);
        command[8] = 0;
        command[9] = 2;
        command[10] = (byte) 0;
        command[11] = (byte) 0;
        short checkSum = 0;
        for (int index = 0; index < 12; index++) {
            checkSum = (short) (checkSum + (short) (command[index] & 0xff));
        }
        command[12] = (byte) (checkSum >> 8);
        command[13] = (byte) checkSum;
        command[command.length - 2] = 0x0D;
        command[command.length - 1] = 0x0A;
        return command;
    }

    public static byte[] getGAECFREQ_SET(String password, String deviceInfo) {
        byte[] command = new byte[368];
        String cmd = CMD_GAECFREQ;
        byte[] tmp = cmd.getBytes();
        System.arraycopy(tmp, 0, command, 0, tmp.length);
        command[8] = 0x01;
        command[9] = 0x62;
        command[10] = (byte) 1;
        command[11] = (byte) 0;
        byte[] tmp1 = new byte[32];
        String updateTime = String.valueOf((new Date()).getTime());
        byte[] tmpTime = updateTime.getBytes();
        System.arraycopy(tmpTime, 0, tmp1, 0, tmpTime.length);
        System.arraycopy(tmp1, 0, command, 12, tmp1.length);

        byte[] tmp2 = new byte[64];
        byte[] tmpPwd = password.getBytes();
        System.arraycopy(tmpPwd, 0, tmp2, 0, tmpPwd.length);
        System.arraycopy(tmp2, 0, command, 44, tmp2.length);
        byte[] tmp3 = new byte[256];
        byte[] tmpInfo = deviceInfo.getBytes();
        System.arraycopy(tmpInfo, 0, tmp3, 0, tmpInfo.length);
        System.arraycopy(tmp3, 0, command, 108, tmp3.length);
        short checkSum = 0;
        for (int index = 0; index < 364; index++) {
            checkSum = (short) (checkSum + (short) (command[index] & 0xff));
        }
        command[364] = (byte) (checkSum >> 8);
        command[365] = (byte) checkSum;
        command[command.length - 2] = 0x0D;
        command[command.length - 1] = 0x0A;
        return command;
    }

    public static byte[] getMISC_REQ(MISC_Type type, String SSID) {
        byte[] command = new byte[112];
        String cmd = CMD_MISC_REQ;
        byte[] tmp = cmd.getBytes();
        System.arraycopy(tmp, 0, command, 0, tmp.length);
        command[8] = 0x00;
        command[9] = 0x62;
        command[10] = (byte) type.ordinal();
        byte[] bSSID = SSID.getBytes();
        System.arraycopy(bSSID, 0, command, 11, bSSID.length);

        short checkSum = 0;
        for (int index = 0; index < 108; index++) {
            checkSum = (short) (checkSum + (short) (command[index] & 0xff));
        }
        command[108] = (byte) (checkSum >> 8);
        command[109] = (byte) checkSum;
        command[command.length - 2] = 0x0D;
        command[command.length - 1] = 0x0A;
        return command;
    }
}
