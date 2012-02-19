package org.ua2.guantanamo.gui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

public class BackgroundCaller extends AsyncTask<Object, Object, Object> {

	private String msg;
	private boolean done;
	private Exception error;

	private Context context;
	private BackgroundWorker worker;
	private Dialog dialog;
	
	private static final String TAG = BackgroundCaller.class.getName(); 
	
	private BackgroundCaller(String msg) {
		this.msg = msg;
		this.done = false;
		this.error = null;
	}
	
	public static BackgroundCaller run(BackgroundCaller caller, Context context, String msg, BackgroundWorker worker) {
		if(caller == null) {
			Log.i(TAG, "Creating " + msg + " caller");
			caller = new BackgroundCaller(msg);
			caller.reset(context, worker);
			caller.execute();
		} else {
			Log.i(TAG, "Reusing existing " + caller.msg + " caller");
			caller.reset(context, worker);
			caller.check();
		}
		
		return caller;
	}
	
	@Override
	protected Object doInBackground(Object... args) {
		try {
			worker.during(context);
		} catch(Exception e) {
			error = e;
		}

		return null;
	}

	protected void onPostExecute(Object obj) {
		done = true;
		
		dialog.dismiss();

		if(context == null) {
			Log.e(TAG, "No context, unable to continue");
			return;
		}

		check();
	}
	
	private void reset(Context context, BackgroundWorker worker) {
		this.context = context;
		this.worker = worker;
		
		if(!done) {
			dialog = ProgressDialog.show(context, "", msg + "...", true);
		}
	}
	
	public void pause() {
		dialog.dismiss();
		
		context = null;
		worker = null;
	}

	private void check() {
		if(done) {
			Log.i(TAG, msg + " done, error=" + (error != null));
			if(error == null) {
				worker.after();
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
		}
	}
}
