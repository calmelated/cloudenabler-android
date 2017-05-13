package tw.com.ksmt.cloud.iface;

public class ListItem {
    public static final int VIEW            = 0;
    public static final int INPUT           = 1;
    public static final int CHECKBOX        = 2;
    public static final int NEW_PASSWORD    = 3;
    public static final int BUTTON          = 4;
    public static final int SWITCH          = 5;
    public static final int NUMBER          = 6;
    public static final int SELECTION       = 7;
    public static final int TITLE           = 8;
    public static final int PASSWORD        = 9;
    public static final int LONG_INPUT      = 10;

    public int type;
    public String key;
    public String value;

    public ListItem(String title) {
        this.type = TITLE;
        this.key = title;
        this.value = title;
    }

    public ListItem(String key, String value) {
        this.type = VIEW;
        this.key = key;
        this.value = value;
    }

    public ListItem(int type, String key, String value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }
}
