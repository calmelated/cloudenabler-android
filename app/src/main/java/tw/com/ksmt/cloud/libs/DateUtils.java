package tw.com.ksmt.cloud.libs;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DateUtils {
    public static String getDate(String dateFormat) {
        Calendar calendar = Calendar.getInstance();
        return new SimpleDateFormat(dateFormat, Locale.getDefault()).format(calendar.getTime());
    }

    public static String getDate(String dateFormat, long currenttimemillis) {
        return new SimpleDateFormat(dateFormat, Locale.getDefault()).format(currenttimemillis);
    }
}
