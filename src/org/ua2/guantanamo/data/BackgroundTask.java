package org.ua2.guantanamo.data;

import org.json.JSONException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

public abstract class BackgroundTask<T> {
	
	private enum State { LOADING, READY, ERROR };
	
	protected abstract String getDescription();
	
	protected abstract T load() throws JSONException;

	private static final String TAG = BackgroundTask.class.getName();
	
	private State state;
	
	private Context context;
	
	private Dialog dialog;
	private Exception error;
	
	public BackgroundTask() {
		Log.i(TAG, "Creating task " + getName());
	}
	
	private String getName() {
		if(context != null) {
			return getClass().getName() + ":" + context.getClass().getName();
		}
		
		return getClass().getName();
	}
	
	private void setContext(Context context) {
		synchronized(this) {
			if(this.context != context) {
				if(this.context != null) {
					detatch();
				}
				
				this.context = context;
				
				Log.i(TAG, "Attached context to " + getName());
			}
		}
	}
	
	private void showLoader() {
		if(dialog != null) {
			Log.d(TAG, "Not showing loader", new Throwable());
			return;
		}
		
		if(context == null) {
			Log.i(TAG, "Can't show loader, no context for " + getName());
			return;
		}
		
		Log.i(TAG, "Showing loader for " + getName());
		dialog = ProgressDialog.show(context, "", "Loading " + getDescription() + "...", true);
	}
	
	private void hideLoader() {
		if(dialog == null) {
			return;
		}
		
		Log.i(TAG, "Hiding loader for " + getName());
		dialog.dismiss();
		dialog = null;
	}
	
	protected void doReady() {
		
	}
	
	private void doError() {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage("Cannot load " + getDescription() + ", " + error.getMessage()).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				hideLoader();
			}
		});
		
		dialog = builder.create();
		dialog.show();
	}
	
	public void attach(Context context) {
		synchronized(this) {
			setContext(context);
			
			Log.i(TAG, "Checking state " + state + " of " + getName());
			if(state == State.LOADING) {
				showLoader();
				
			} else if(state == State.READY) {
				doReady();
				
			} else if(state == State.ERROR) {
				doError();
				
			}
		}
	}
	
	protected void _load(Context context) {
		synchronized(this) {
			setContext(context);
			
			if(state == State.LOADING) {
				return;
			}
			
			state = State.LOADING;
		}
		
		showLoader();
		
		AsyncTask<Object, Object, Object> task = new AsyncTask<Object, Object, Object>() {
			@Override
			protected Object doInBackground(Object... params) {
				try {
					Log.d(TAG, "Loading");
					long loadStart = System.currentTimeMillis();
					load();
					Log.d(TAG, "Loaded " + " on " + getName() + " in " + (System.currentTimeMillis() - loadStart) + " ms");
				} catch(Exception e) {
					Log.e(TAG, getName() + " failed", e);
					
					error = e;
					state = State.ERROR;
					
					return null;
				}
				
				state = State.READY;
				
				return null;
			}

			@Override
			protected void onPostExecute(Object result) {
				synchronized(BackgroundTask.this) {
					if(BackgroundTask.this.context == null) {
						state = State.READY;
						
						return;
					}
				}
			
				hideLoader();
					
				if(state == State.READY) {
					doReady();
					
				} else if(state == State.ERROR) {
					doError();
					
				}
			}
		};

		task.execute();
	}
	
	public void detatch() {
		synchronized(this) {
			Log.i(TAG, "Detatching context from " + getName());
			
			context = null;
			
			hideLoader();
		}
	}
}
