package org.ua2.guantanamo.gui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

public abstract class BackgroundCaller extends AsyncTask<Object, Object, Object> {

	private String msg;
	private boolean done;
	private Exception error;

	private Context context;
	private Dialog dialog;
	
	private static final String TAG = BackgroundCaller.class.getName(); 
	
	public BackgroundCaller(Context context, String msg) {
		this.msg = msg;
		this.done = false;
		this.error = null;

		attach(context);
	}
	
	public static BackgroundCaller run(BackgroundCaller caller) {
		caller.execute();
		return caller;
	}
	
	@Override
	protected Object doInBackground(Object... args) {
		try {
			during();
		} catch(Exception e) {
			error = e;
		}

		return null;
	}

	protected abstract void during() throws Exception;
	protected abstract void after();

	protected void onPostExecute(Object obj) {
		done = true;
		
		dialog.dismiss();

		check();
	}
	
	protected Context getContext() {
		return context;
	}

	public void attach(Context context) {
		Log.i(TAG, "Attaching context " + context);
		
		this.context = context;
		
		check();
	}
	
	public void detach() {
		dialog.dismiss();
		context = null;
	}

	public void check() {
		if(context == null) {
			Log.i(TAG, "No context, unable to continue");
			return;
		}
		
		if(done) {
			Log.i(TAG, msg + " done, error=" + (error != null));
			if(error == null) {
				after();
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				String errMsg = msg + " failed";
				Log.e(TAG, errMsg, error);
				builder.setMessage(errMsg + ", " + error.getMessage()).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				});
				AlertDialog dialog = builder.create();
				dialog.show();
			}
		} else {
			dialog = ProgressDialog.show(context, "", msg + "...", true);
		}
	}
}
