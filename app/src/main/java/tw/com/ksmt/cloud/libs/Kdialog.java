package tw.com.ksmt.cloud.libs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;

import tw.com.ksmt.cloud.R;

public abstract class Kdialog {
    public static final short FAILED 		= 1;
    public static final short INFO			= 2;
    public static final short BACK_MSG		= 3;

    public static void show(Context context, short type) {
        show(context, type, null);
    }

    public static void show(Context context, short type, String msg) {
        if(type == Kdialog.FAILED) 			    { failed(context, msg);	}
        else if(type == Kdialog.INFO) 			{ info(context, msg);		}
        else if(type == Kdialog.BACK_MSG)		{ backMsg(context, msg);	}
    }

    public static AlertDialog.Builder getDefInfoDialog(Context context){
        return new AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.status))
            .setIcon(R.drawable.ic_info)
            .setPositiveButton(getString(context, R.string.confirm), null);
    }

    public static AlertDialog.Builder getDefErrDialog(Context context){
        return new AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.error))
            .setIcon(R.drawable.ic_disable)
            .setCancelable(false)
            .setPositiveButton(getString(context, R.string.confirm), null);
    }

    public static AlertDialog.Builder getDefWarningDialog(Context context){
        return new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.caution))
                .setIcon(R.drawable.ic_warning)
                .setPositiveButton(getString(context, R.string.confirm), null);
    }

    public static AlertDialog.Builder getDefInputDialog(Context context) {
        return new AlertDialog.Builder(context)
                .setIcon(R.drawable.ic_editor)
                .setCancelable(false)
                .setPositiveButton(getString(context, R.string.confirm), null)
                .setNegativeButton(getString(context, R.string.cancel), null);
    }

    public static AlertDialog.Builder getDefInputDialog(Context context, View view){
        LayoutInflater inflater = LayoutInflater.from(context);
        return new AlertDialog.Builder(context)
            .setIcon(R.drawable.ic_editor)
            .setView(view)
            .setCancelable(false)
            .setPositiveButton(getString(context, R.string.confirm), null)
            .setNegativeButton(getString(context, R.string.cancel), null);
    }

    private static final String getString(Context context, int id) {
        return context.getString(id);
    }

    private static final void failed(Context context, String msg) {
        getDefErrDialog(context)
            .setMessage(msg)
            .show();
    }

    private static final void info(Context context, String msg) {
        getDefInfoDialog(context)
            .setTitle(context.getString(R.string.status))
            .setMessage(msg)
            .show();
    }

    public static final AlertDialog.Builder getMakeSureDialog(Context context, String msg) {
        return new AlertDialog.Builder(context)
            .setTitle(getString(context, R.string.caution))
            .setIcon(R.drawable.ic_warning)
            .setMessage(msg)
            .setPositiveButton(getString(context, R.string.confirm), null)
            .setNegativeButton(getString(context, R.string.cancel), null);
    }

    private static final void backMsg(final Context context, String msg) {
        getDefInfoDialog(context)
        .setMessage(msg)
        .setCancelable(false)
        .setPositiveButton(getString(context, R.string.confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ((Activity)context).onBackPressed();
            }
        })
        .show();
    }

    public static final ProgressDialog getProgress(Context context){
        return getProgress(context, null, false,true);
    }

    public static final ProgressDialog getProgress(Context context, ProgressDialog mDialog){
        if(mDialog != null && mDialog.isShowing()) {
            return mDialog;
        }
        return getProgress(context, null, false, true);
    }

    public static final ProgressDialog getProgress(Context context, String msg){
        return getProgress(context, msg, false, true);
    }

    public static final ProgressDialog getProgress(final Context context, String msg, boolean hasCancelBtn, boolean showLoadingString){
        msg = (msg != null && !msg.equalsIgnoreCase("")) ? msg : getString(context, R.string.loading);
//        ProgressDialog pdialog = new ProgressDialog(context, R.style.ProgressDialog);
        ProgressDialog pdialog = new ProgressDialog(context);
        if(showLoadingString) {
            pdialog.setMessage(msg);
        }
        pdialog.setCancelable(false);
        if (hasCancelBtn) {
            pdialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(context, R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ((Activity)context).onBackPressed();
                }
            });
        }
        pdialog.show();
        return pdialog;
    }

    public static void disconnectAction(final Context context) {
        Intent intent = new Intent(context, DialogActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("msgType", "NoInet");
        context.startActivity(intent);
    }

    public static void retryInetAction(final Context context) {
        Intent intent = new Intent(context, DialogActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("msgType", "RetryInet");
        context.startActivity(intent);
    }
}