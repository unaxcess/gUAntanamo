package org.ua2.guantanamo.data;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

public abstract class CacheTask<T> {
	
	private enum State { RUNNING, STOPPED, ERROR };
	
	protected abstract String getType();
	protected abstract String getDescription();
	protected abstract int getRefreshMinutes();
	
	protected abstract T loadItem(String id) throws JSONException;
	protected abstract T convertDataToItem(String data) throws JSONException;

	protected interface ItemProcessor<T> {
		public void processItem(T item);		
	}
	
	private static class CacheValue<T> {
		String id;
		Date lastUpdate;
		boolean needsSave;
		
		String data;
		T item;
		
		public CacheValue(String id, Date lastUpdate, String data) {
			this.id = id;
			this.lastUpdate = lastUpdate;
			this.data = data;
		}
			
	}
	
	private static Map<String, CacheValue> cacheMap = new ConcurrentHashMap<String, CacheValue>();
	
	private State state;
	private DatabaseHelper helper;
	private Context context;
	private ItemProcessor<T> processor;
	
	private Dialog dialog;
	private CacheValue<T> value;
	private Exception error;
	
	public void init(Context context, ItemProcessor<T> processor) {
		synchronized(this) {
			helper = new DatabaseHelper(context);
			this.processor = processor;

			if(state == State.STOPPED || state == State.ERROR) {
				load(null, false);
			}
		}
	}
	
	public void load(final String id, final boolean refresh) {
		synchronized(this) {
			dialog = ProgressDialog.show(context, "", getDescription() + "...", true);
			
			if(state == null) {
				state = State.RUNNING;
			}
			
			String key = getType();
			if(id != null) {
				key += ":" + id;
			}

			value = cacheMap.get(key);

			if(value == null) {
				CacheRow row = helper.loadRow(getType(), id);
				if(row != null) {
					value = new CacheValue<T>(id, row.lastUpdate, row.data);
				} else {
					value = new CacheValue<T>(id, null, null);
				}
				cacheMap.put(key, value);
			}
			
			if(refresh) {
				value.lastUpdate = null;
			}
		}
		
		AsyncTask<Object, Object, Object> task = new AsyncTask<Object, Object, Object>() {
			@Override
			protected Object doInBackground(Object... params) {
				_load();
				
				return null;
			}
		};

		task.execute();
	}
	
	private void _load() {
		if(state == State.RUNNING) {
			if(value.lastUpdate == null || value.lastUpdate.getTime() < System.currentTimeMillis() - 60000 * getRefreshMinutes() || value.data == null) {
				if(isLoadable()) {
					try {
						value.item = loadItem(value.id);
						value.data = convertItemToData(value.item);
						
						value.lastUpdate = new Date();
						
						value.needsSave = true;
					} catch(JSONException e) {
						error = e;
						state = State.ERROR;
					}
				} else {
					// TODO: Can't get the value
				}
			}
			
			if(value.item == null) {
				try {
					value.item = convertDataToItem(value.data);
				} catch(JSONException e) {
					error = e;
					state = State.ERROR;
				}
			}
		}
		
		synchronized(CacheTask.this) {
			if(helper == null) {
				state = State.STOPPED;
				
				return;
			}
			
			if(value.needsSave) {
				helper.saveRow(getType(), value.id, value.lastUpdate, value.data);
				
				value.needsSave = false;
			}
		}
		
		CacheTask.this.processor.processItem(value.item);
		
		state = State.STOPPED;
	}
	
	public void unload() {
		synchronized(this) {
			helper.close();
			helper = null;
			
			context = null;
			processor = null;
			
			if(dialog != null) {
				dialog.dismiss();
				dialog = null;
			}
		}
	}

	protected String convertItemToData(T item) throws JSONException {
		return item.toString();
				
	}

	public static boolean isLoadable() {
		return true;
	}
}
