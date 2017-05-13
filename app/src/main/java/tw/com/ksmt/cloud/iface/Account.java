package tw.com.ksmt.cloud.iface;

import android.content.Context;

import com.avos.avoscloud.okhttp.MultipartBuilder;
import com.avos.avoscloud.okhttp.RequestBody;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import tw.com.ksmt.cloud.PrjCfg;
import tw.com.ksmt.cloud.R;
import tw.com.ksmt.cloud.libs.Utils;

public class Account implements Serializable, Cloneable {
    public boolean superAdmin;
    public boolean admin;
    public boolean activate;
    public boolean trial;
    public boolean allowUp;
    public boolean allowDown;
    private boolean passChanged = false;
    public String name;
    public String account;
    public String password;
    public String newpasswoed;

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public Account(JSONObject jsonObject) throws Exception {
        update(jsonObject);
    }

    public Account(String name, String account, boolean admin, boolean activate) {
        update(name, account, admin, activate);
    }

    public Account(String name, String account, String password, boolean admin, boolean activate, boolean trial) {
        update(name, account, password, admin, activate, trial);
    }

    public Account(String account,String origPswd,String newPswd){
        this.account = account;
        this.password = origPswd;
        this.newpasswoed = newPswd;
    }

    public void update(JSONObject jsonObject) {
        try {
            this.superAdmin = ((jsonObject.getInt("admin") == 2) ? true : false);
            this.admin = ((jsonObject.getInt("admin") == 1 || superAdmin) ? true : false);
            this.activate = ((jsonObject.getInt("activate") == 1) ? true : false);
            this.allowDown = ((jsonObject.getInt("allowDown") == 1) ? true : false);
            this.allowUp = ((jsonObject.getInt("allowUp") == 1) ? true : false);
            this.trial = ((jsonObject.getInt("trial") == 1) ? true : false);
            this.account = jsonObject.getString("account");
            this.name = jsonObject.getString("name");
            if (jsonObject.has("password")) {
                this.password = jsonObject.getString("password");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void update(String name, String account, boolean admin, boolean activate) {
        this.admin = admin;
        this.activate = activate;
        this.allowDown = true;
        this.allowUp = true;
        this.account = account;
        this.name = name;
    }

    public void update(String name, String account, String password, boolean admin, boolean activate, boolean trial) {
        update(name, account, admin, activate);
        this.password = password;
        this.passChanged = true;
        this.trial = trial;
    }

    public RequestBody toMultiPart() {
        MultipartBuilder entityBuilder = new MultipartBuilder();
        entityBuilder.type(MultipartBuilder.FORM);
        entityBuilder.addFormDataPart("account", this.account);
        entityBuilder.addFormDataPart("name", this.name);
        entityBuilder.addFormDataPart("activate", (this.activate ? "1" : "0"));
        entityBuilder.addFormDataPart("admin", (this.superAdmin ? "2" : (this.admin ? "1" : "0")));
        entityBuilder.addFormDataPart("trial", (this.trial ? "1" : "0"));
        entityBuilder.addFormDataPart("allowDown", (this.allowDown ? "1" : "0"));
        entityBuilder.addFormDataPart("allowUp", (this.allowUp ? "1" : "0"));
        if(passChanged) {
            entityBuilder.addFormDataPart("password", this.password);
        }
        return entityBuilder.build();
    }

    public List<ListItem> toListItems(Context context) {
        List<ListItem> listItems = new ArrayList<ListItem>();
        String subCompID = Utils.loadPrefs(context, "SubCompID", "0");
        String curUser = (subCompID.equals("0")) ? Utils.loadPrefs(context, "UserName") : "Admin";
        String curAccount = Utils.loadPrefs(context, "Account");
        Boolean isAdmin = (!Utils.loadPrefs(context, "Admin").equals("0")) ? true : false;
        if(admin) {
            listItems.add(new ListItem(ListItem.VIEW, context.getString(R.string.account), account));
            if(isAdmin && (curAccount.equals(account) || curUser.equalsIgnoreCase("Admin") || PrjCfg.USER_MODE == PrjCfg.MODE_KSMT_DEBUG)) {
                if(name.equalsIgnoreCase("Admin")) {
                    listItems.add(new ListItem(ListItem.VIEW, context.getString(R.string.name), name));
                } else {
                    listItems.add(new ListItem(ListItem.INPUT, context.getString(R.string.name), name));
                }
                listItems.add(new ListItem(ListItem.NEW_PASSWORD, context.getString(R.string.password), password));
            } else {
                listItems.add(new ListItem(ListItem.VIEW, context.getString(R.string.name), name));
            }
        } else if(trial) { // trial user
            listItems.add(new ListItem(ListItem.VIEW, context.getString(R.string.trial_account), account));
            listItems.add(new ListItem(ListItem.INPUT, context.getString(R.string.name), name));
            listItems.add(new ListItem(ListItem.NEW_PASSWORD, context.getString(R.string.password), password));
        } else { // normal user
            listItems.add(new ListItem(ListItem.VIEW, context.getString(R.string.account), account));
            listItems.add(new ListItem(ListItem.INPUT, context.getString(R.string.name), name));
            listItems.add(new ListItem(ListItem.NEW_PASSWORD, context.getString(R.string.password), password));
            listItems.add(new ListItem(ListItem.CHECKBOX, context.getString(R.string.activate), (activate ? "1" : "0")));
        }
        if(isAdmin && !name.equalsIgnoreCase("Admin")) {
            if(curUser.equals("Admin") || !subCompID.equals("0")) {
                listItems.add(new ListItem(ListItem.CHECKBOX, context.getString(R.string.admin), (superAdmin ? "2" : (admin ? "1" : "0"))));
            }
        }
        return listItems;
    }
}
