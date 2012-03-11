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
			worker.during();
		} catch(Exception e) {
			error = e;
		}

		return null;
	}

	protected void onPostExecute(Object obj) {
		done = true;
		
		dialog.dismiss();

		check();
	}
	
	private void reset(BackgroundWorker worker) {
		this.worker = worker;
		
		if(!done) {
			worker.start(msg);
			
			// Move to Activity
			dialog = ProgressDialog.show(context, "", msg + "...", true);
		}
	}
	
	public void pause() {
		dialog.dismiss();
		
		worker = null;
	}

	private void check() {
		if(done) {
			Log.i(TAG, msg + " done, error=" + (error != null));
			if(error == null) {
				worker.after();
			} else {
				worker.error(error);
				
				// Move to Activity
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
