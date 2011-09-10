package org.ua2.guantanamo.gui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

public class BackgroundCaller {
	
	private Context context;
	private String msg;
	private BackgroundWorker worker;
	
	private static final String TAG = BackgroundCaller.class.getName(); 
	
	private BackgroundCaller(Context context, String msg, BackgroundWorker worker) {
		this.context = context;
		this.msg = msg;
		this.worker = worker;
	}
	
	public static void run(Context context, String msg, BackgroundWorker worker) {
		new BackgroundCaller(context, msg, worker).run();
	}
	
	public void run() {
		final ProgressDialog progress = ProgressDialog.show(context, "", msg, true);

		new AsyncTask<String, Void, Exception>() {
			@Override
			protected Exception doInBackground(String... params) {
				try {
					worker.during();

					return null;
				} catch(Exception e) {
					return e;
				}
			}

			protected void onPostExecute(Exception e) {
				progress.dismiss();

				if(e == null) {
					worker.after();
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(context);
					String errMsg = worker.getError();
					Log.e(TAG, errMsg, e);
					builder.setMessage(errMsg + ", " + e.getMessage()).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
					AlertDialog dialog = builder.create();
					dialog.show();
				}
			}
		}.execute();
	}
}
