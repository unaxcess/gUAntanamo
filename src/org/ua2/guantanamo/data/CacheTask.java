package org.ua2.guantanamo.data;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

public abstract class CacheTask<T> {
	
	private enum State { RUNNING, READY, ERROR };

	public interface Processor<T> {
		public void processItem(T item, boolean isNew);		
	}
	
	protected abstract String getType();
	protected abstract String getDescription();
	protected abstract int getRefreshMinutes();
	
	protected abstract T loadItem(String id) throws JSONException;
	protected abstract T convertDataToItem(String data) throws JSONException;

	protected String convertItemToData(T item) throws JSONException {
		return item.toString();
				
	}
	
	private static class CacheValue<T> {
		String id;
		Date lastUpdate;
		
		String data;
		T item;
		boolean isNew;
		
		public CacheValue(String id, Date lastUpdate, String data) {
			this.id = id;
			this.lastUpdate = lastUpdate;
			this.data = data;
		}
	}
	
	private State state;
	
	private Context context;
	private DatabaseHelper helper;
	private Processor<T> processor;
	
	private Dialog dialog;
	private CacheValue<T> value;
	private Exception error;
	
	private static Map<String, CacheValue> cacheMap = new ConcurrentHashMap<String, CacheValue>();
	
	private static final String TAG = CacheTask.class.getName();
	
	public CacheTask() {
		Log.i(TAG, "Creating task " + getName());
	}
	
	private String getName() {
		if(context != null) {
			return getClass().getName() + ":" + context.getClass().getName() + ":" + getType();
		}
		
		return getClass().getName() + ":" + getType();
	}
	
	private void setContext(Context context, Processor<T> processor) {
		synchronized(this) {
			if(this.context != context) {
				if(this.context != null) {
					detatch();
				}
				
				this.context = context;
				this.helper = new DatabaseHelper(context);
				
				Log.i(TAG, "Attached context to " + getName());
			}

			this.processor = processor;
		}
	}
	
	private void showRunner() {
		if(dialog != null) {
			Log.d(TAG, "Not showing runner", new Throwable());
			return;
		}
		
		if(context == null) {
			Log.i(TAG, "Can't show runner, no context for " + getName());
			return;
		}
		
		Log.i(TAG, "Showing runner for " + getName());
		dialog = ProgressDialog.show(context, "", "Loading " + getDescription() + "...", true);
	}
	
	private void hideRunner() {
		if(dialog == null) {
			return;
		}
		
		Log.i(TAG, "Hiding runner for " + getName());
		dialog.dismiss();
		dialog = null;
	}
	
	protected void doReady() {
		boolean isNew = value.isNew;
		
		if(value.isNew) {
			helper.saveRow(getType(), value.id != null ? value.id : "", value.lastUpdate, value.data);
			
			value.isNew = false;
		}

		processor.processItem(value.item, isNew);
	}
	
	private void doError() {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage("Cannot load " + getDescription() + ", " + error.getMessage()).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				hideRunner();
			}
		});
		
		dialog = builder.create();
		dialog.show();
	}
	
	public void attach(Context context, Processor<T> processor) {
		synchronized(this) {
			setContext(context, processor);
			
			Log.i(TAG, "Checking state " + state + " of " + getName());
			if(state == State.RUNNING) {
				showRunner();
				
			} else if(state == State.READY) {
				doReady();
				
			} else if(state == State.ERROR) {
				doError();
				
			}
		}
	}
	
	protected void _load(Context context, Processor<T> processor, final String id, final boolean forceRefresh) {
		synchronized(this) {
			setContext(context, processor);
			
			if(state == State.RUNNING) {
				return;
			}

			String key = getType();
			if(id != null) {
				key += ":" + id;
			}

			value = cacheMap.get(key);

			if(value == null) {
				CacheRow row = helper.loadRow(getType(), id != null ? id : "");
				if(row != null) {
					Log.d(TAG, "Creating cache value from row " + getType() + "=" + id + "(" + row.lastUpdate + ") for " + getName());
					value = new CacheValue<T>(id, row.lastUpdate, row.data);
				} else {
					Log.d(TAG, "Creating new cache value " + getType() + "=" + id + " for " + getName());
					value = new CacheValue<T>(id, null, null);
				}
				cacheMap.put(key, value);
			}
			
			if(forceRefresh) {
				value.lastUpdate = null;
				value.data = null;
				value.item = null;
			}
			
			state = State.RUNNING;
		}
		
		showRunner();
		
		AsyncTask<Object, Object, Object> task = new AsyncTask<Object, Object, Object>() {
			@Override
			protected Object doInBackground(Object... params) {
				try {
					if(value.lastUpdate == null
							|| (getRefreshMinutes() != -1 && value.lastUpdate.getTime() < System.currentTimeMillis() - 60000 * getRefreshMinutes())
							|| value.data == null) {
						
						Log.d(TAG, "Running " + value.id);
						long runStart = System.currentTimeMillis();
						value.item = loadItem(value.id);
						Log.d(TAG, "Ran " + value.id + " on " + getName() + " in " + (System.currentTimeMillis() - runStart) + " ms");
						
						Log.d(TAG, "Converting " + value.id);
						long convertStart = System.currentTimeMillis();
						value.data = convertItemToData(value.item);
						Log.d(TAG, "Converted " + value.id + " on " + getName() + " in " + (System.currentTimeMillis() - convertStart) + " ms");
						
						value.lastUpdate = new Date();
						
						value.isNew = true;
					}
					
					if(value.item == null) {
						value.item = convertDataToItem(value.data);
					}
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
				synchronized(CacheTask.this) {
					if(CacheTask.this.context == null) {
						state = State.READY;
						
						return;
					}
				}
			
				hideRunner();
					
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
			
			if(helper != null) {
				helper.close();
			}
			helper = null;
			
			processor = null;
			
			hideRunner();
		}
	}
}
