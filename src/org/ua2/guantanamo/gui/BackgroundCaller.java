package org.ua2.guantanamo.gui;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.ua2.guantanamo.data.DatabaseUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

public class BackgroundCaller extends AsyncTask<Object, Object, Object> {

	private enum RunState { BEFORE, DURING, AFTER };
	
	private String msg;
	private Exception error;
	private RunState state;

	private BackgroundWorker worker;
	private Dialog dialog;

	private Lock LOCK = new ReentrantLock();
	private Condition HAS_WORKER = LOCK.newCondition();

	private static final String TAG = BackgroundCaller.class.getName(); 
	
	private BackgroundCaller(String msg) {
		this.msg = msg;
		this.error = null;
		
		this.state = RunState.BEFORE;
	}
	
	public static BackgroundCaller run(BackgroundCaller caller, String msg, BackgroundWorker worker) {
		DatabaseUtils.setContext(worker.getContext());
		
		if(caller == null) {
			Log.i(TAG, "Creating " + msg + " caller");
			caller = new BackgroundCaller(msg);
		} else {
			Log.i(TAG, "Reusing existing " + caller.msg + " caller");
		}
		caller.reset(worker);
		
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
		LOCK.lock();

		state = RunState.AFTER;
		
		dialog.dismiss();

		check();
		
		LOCK.unlock();
	}
	
	private void reset(BackgroundWorker worker) {
		LOCK.lock();

		Log.i(TAG, "Setting worker to " + worker + " on " + Thread.currentThread().getName());
		this.worker = worker;
		
		if(state != RunState.AFTER) {
			dialog = ProgressDialog.show(worker.getContext(), "", msg + "...", true);
		}
		
		if(state == RunState.BEFORE) {
			execute();
		}

		Log.i(TAG, "Signalling worker on " + Thread.currentThread().getName());
		HAS_WORKER.signal();

		LOCK.unlock();
	}
	
	public void pause() {
		LOCK.lock();
		
		dialog.dismiss();
		
		Log.i(TAG, "Setting worker to null on " + Thread.currentThread().getName());
		worker = null;
		
		LOCK.unlock();
	}

	private void check() {
		LOCK.lock();
		
		long start = System.currentTimeMillis();
		while(worker == null) {
			Log.i(TAG, "Waiting for worker on " + Thread.currentThread().getName());
			try {
				HAS_WORKER.await();
			} catch(InterruptedException e) {
			}
		}
		long diff = System.currentTimeMillis() - start;
		Log.i(TAG, "Checking after " + diff + "ms");
		
		if(state == RunState.AFTER) {
			Log.i(TAG, msg + " done, error=" + (error != null));
			if(error == null) {
				worker.after();
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(worker.getContext());
				String errMsg = msg + " failed";
				Log.e(TAG, errMsg, error);
				builder.setMessage(errMsg + ", " + error.getMessage()).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				});
				dialog = builder.create();
				dialog.show();
			}
		}
		
		LOCK.unlock();
	}
}
