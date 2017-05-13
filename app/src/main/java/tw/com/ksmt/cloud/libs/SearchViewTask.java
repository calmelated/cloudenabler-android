package tw.com.ksmt.cloud.libs;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;

import tw.com.ksmt.cloud.iface.SearchViewAdapter;

public class SearchViewTask extends AsyncTask<Context, Integer, Boolean> {
    private String query;
    private SearchViewAdapter adapter;
    private android.os.Handler mHandler;

    public SearchViewTask(SearchViewAdapter adapter, Handler mHandler, String query) {
        this.query = query;
        this.adapter = adapter;
        this.mHandler = mHandler;
    }

    protected Boolean doInBackground(Context... contexts) {
        try {
            MHandler.exec(mHandler, MHandler.SRCH_QUERY, query);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    protected void onPostExecute(Boolean result) {
        if (result) {
            MHandler.exec(mHandler, MHandler.UPDATE);
        }
    }
}