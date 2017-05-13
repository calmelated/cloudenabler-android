package tw.com.ksmt.cloud.libs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.ContextThemeWrapper;

import tw.com.ksmt.cloud.R;

public class DialogActivity extends Activity {
	private final Activity activity = DialogActivity.this;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String msgType = getIntent().getStringExtra("msgType");
		if(msgType.equals("NoInet")) {
			showNoInetMsg();
		} else if(msgType.equals("RetryInet")) {
			showRetryInetMsg();
		} else if(msgType.equals("ErrMsg")) {
			String title = getIntent().getStringExtra("title");
			String message = getIntent().getStringExtra("message");
			AlertDialog.Builder dialog = getDefDialog("Error", title, message);
			dialog.show();
		}
	}

	private AlertDialog.Builder getDefDialog(String type, String title, String message) {
		final Resources resources = getResources();
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(activity, android.R.style.Theme_Holo_Light_Dialog));
		if(type != null) {
            if(type.equalsIgnoreCase("error")) {
                builder.setIcon(R.drawable.ic_disable);
                builder.setCancelable(false);
            } else if(type.equalsIgnoreCase("info")) {
                builder.setIcon(R.drawable.ic_enable);
            }
		}
		if(title != null) {
			builder.setTitle(title);
		}
		if(message != null) {
			builder.setMessage(message);
		}
		builder.setPositiveButton(resources.getString(R.string.ok),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    finish();
                }
            });

		return builder;
	}

	private void showNoInetMsg() {
		final Resources resources = getResources();
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(activity, android.R.style.Theme_Holo_Light_Dialog));
		builder.setIcon(R.drawable.ic_disable)
		.setTitle(resources.getString(R.string.inet_failed))
		.setMessage(resources.getString(R.string.inet_failed_msg))
		.setPositiveButton(resources.getString(R.string.ok),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    if(dialog != null) {
                        dialog.cancel();
                    }
                    finish();
                }
            })
		.setNegativeButton(resources.getString(R.string.settings),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    if(dialog != null) {
                        dialog.cancel();
                    }
                    activity.startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
                    finish();
                }
            });

        // Show dialog
		builder.setCancelable(false)
		.create()
		.show();
	}

	private void showRetryInetMsg() {
		final Resources resources = getResources();
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(activity, android.R.style.Theme_Holo_Light_Dialog));
		builder.setIcon(R.drawable.ic_disable)
		.setTitle(resources.getString(R.string.inet_failed))
		.setMessage(resources.getString(R.string.inet_failed_msg))
		.setPositiveButton(resources.getString(R.string.retry),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    if(dialog != null) {
                        dialog.cancel();
                    }                    
                    finish();
                }
            })
		.setNegativeButton(resources.getString(R.string.settings),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    if(dialog != null) {
                        dialog.cancel();
                    }
                    activity.startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
                    finish();
                }
            });
        
        // Show dialog
		builder.setCancelable(false)
		.create()
		.show();
	}
}
