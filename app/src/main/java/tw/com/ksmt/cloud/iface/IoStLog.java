package tw.com.ksmt.cloud.iface;

import android.content.Context;

import org.json.JSONObject;

import java.io.Serializable;

import tw.com.ksmt.cloud.R;

public class IoStLog implements Serializable {
    private static final int[] MCODE = new int[]{
            R.string.unknown,
            R.string.unknown,
            R.string.local,
            R.string.remote,
            R.string.on,
            R.string.off,
            R.string.trip,
            R.string.normal,
            R.string.fault,
            R.string.putoff,
            R.string.puton,
            R.string.onsite,
            R.string.resistance,
            R.string.inductance,
            R.string.capacitance,
            R.string.remote,
            R.string.fault,
            R.string.operate,
            R.string.stop,
            R.string.running,
            R.string.test_position,
            R.string.connect_position,
            R.string.manual,
            R.string.automatic,
            R.string.abnormal,
            R.string.slight_partial_discharge,
            R.string.serious_partial_discharge,
            R.string.cutoff,
            R.string.off2,
            R.string.on,  //For YATEC-Viewer On-Red
            R.string.on,  //For YATEC-Viewer On-Green
            R.string.off, //For YATEC-Viewer Off-Red
            R.string.off, //For YATEC-Viewer Off-Green
    };

    public int time;
    public String account;
    public String regName;
    public int msgCode;
    public String message;
    public long accTime;
    public long accNum;

    public IoStLog(Context context, JSONObject jsonObject) {
        try {
            this.time = jsonObject.getInt("time");
            this.account = jsonObject.getString("account");
            this.regName = jsonObject.getString("regName");
            this.msgCode = jsonObject.getInt("msgCode");
            this.message = regName + " " + context.getString(MCODE[msgCode]);
            this.accNum  = jsonObject.has("accNum") ? jsonObject.getLong("accNum")   : 0 ;
            this.accTime = jsonObject.has("accTime") ? jsonObject.getLong("accTime") : 0 ;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
