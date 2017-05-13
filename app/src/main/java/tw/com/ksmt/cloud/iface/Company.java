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

public class Company implements Serializable, Cloneable {
    public String id;
    public String name;
    public String newName;
    public String parentId;
    public String agent;
    public int nAlarm;

    // Extra Setting
    public String ct_email = "";
    public String ct_company = "";
    public String ct_person = "";
    public String ct_phone = "";

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    // get company info from Web API
    public Company(JSONObject jObject) {
        try {
            this.newName = null;
            this.id = jObject.getString("id");
            this.name = jObject.has("company") ? jObject.getString("company") : "" ;
            this.parentId = jObject.has("parentId") ? jObject.getString("parentId") : "" ;
            this.agent = jObject.has("agent") ? jObject.getString("agent") : "" ;
            this.nAlarm = jObject.has("numAlarm") ? jObject.getInt("numAlarm") : 0 ;

            // Extra
            if (!jObject.isNull("extra")) {
                JSONObject extraObj = jObject.getJSONObject("extra");
                this.ct_company = (extraObj.has("ct_company")) ? extraObj.getString("ct_company") : "" ;
                this.ct_person = (extraObj.has("ct_name")) ? extraObj.getString("ct_name")  : "" ;
                this.ct_phone = (extraObj.has("ct_phone")) ? extraObj.getString("ct_phone") : "" ;
                this.ct_email = (extraObj.has("ct_email")) ? extraObj.getString("ct_email") : "" ;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RequestBody toMultiPart() {
        return toMultiPart("/api/company/edit");
    }

    public RequestBody toMultiPart(String url) {
        MultipartBuilder entityBuilder = new MultipartBuilder();
        entityBuilder.type(MultipartBuilder.FORM);
        if(url.equalsIgnoreCase("/api/company/rename")) {
            entityBuilder.addFormDataPart("companyId", id);
            entityBuilder.addFormDataPart("company", newName);
        } else {
            entityBuilder.addFormDataPart("companyId", id);
            entityBuilder.addFormDataPart("companyId", id);
            entityBuilder.addFormDataPart("ct_email", ct_email);
            entityBuilder.addFormDataPart("ct_company", ct_company);
            entityBuilder.addFormDataPart("ct_name", ct_person);
            entityBuilder.addFormDataPart("ct_phone", ct_phone);
        }
        return entityBuilder.build();
    }

    public List<ListItem> toListItems(Context context) {
        List<ListItem> listItems = new ArrayList<ListItem>();
        listItems.add(new ListItem(context.getString(R.string.company)));

        // Company info.
        Boolean isAdmin = (!Utils.loadPrefs(context, "Admin").equals("0")) ? true : false;
        String curUser = Utils.loadPrefs(context, "UserName");
        if(isAdmin && curUser.equalsIgnoreCase("Admin")) {
            listItems.add(new ListItem(ListItem.INPUT, context.getString(R.string.company_name), name));
        } else {
            listItems.add(new ListItem(ListItem.VIEW, context.getString(R.string.company_name), name));
        }

        if(PrjCfg.CUSTOMER.equals(PrjCfg.CUSTOMER_KSMT_TEST_ALL)) {
            listItems.add(new ListItem(ListItem.VIEW, context.getString(R.string.company_id), id));
            listItems.add(new ListItem(ListItem.VIEW, context.getString(R.string.parent_id), parentId));
            listItems.add(new ListItem(ListItem.VIEW, context.getString(R.string.agent_num), agent));
        }
        listItems.add(new ListItem(ListItem.VIEW, context.getString(R.string.num_used_alarm), String.valueOf(nAlarm)));

        // Button to remove company
        String subCompID = Utils.loadPrefs(context, "SubCompID", "0");
        if(isAdmin && curUser.equalsIgnoreCase("Admin") && subCompID.equals("0")) {
            listItems.add(new ListItem(ListItem.BUTTON, context.getString(R.string.company_remove), context.getString(R.string.company_remove_message)));
        }

        // Contact info for YATEC
        if(PrjCfg.EN_CONTACT_INFO) {
            listItems.add(new ListItem(context.getString(R.string.contact_info)));
            listItems.add(new ListItem(ListItem.INPUT, context.getString(R.string.contact_company), (ct_company == null || ct_company.equals("")) ? context.getString(R.string.none) : ct_company));
            listItems.add(new ListItem(ListItem.INPUT, context.getString(R.string.contact_person), (ct_person == null || ct_person.equals("")) ? context.getString(R.string.none) : ct_person));
            listItems.add(new ListItem(ListItem.INPUT, context.getString(R.string.contact_phone), (ct_phone == null || ct_phone.equals("")) ? context.getString(R.string.none) : ct_phone));
            listItems.add(new ListItem(ListItem.INPUT, context.getString(R.string.contact_email), (ct_email == null || ct_email.equals("")) ? context.getString(R.string.none) : ct_email));
        }
        return listItems;
    }
}
