package org.ua2.guantanamo.data;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;

import android.content.Context;
import android.util.Log;

/**
 * Encapsulate the logic for loading / saving and converting raw string data
 * into a useable object.
 * 
 * CacheItems should not be passed around for the non configuration instance as this
 * causes a refernce to the old activity to remain. Android doesn't like that
 * 
 * @author Techno
 * 
 * @param <T>
 */
public abstract class CacheItem<T> {
	
	private DatabaseHelper helper;

	private String type;
	private String id;
	private Date lastUpdate;
	private String data;
	private T item;
	private boolean isItemFromCache;

	private static final String TAG = CacheItem.class.getName();

	private static final DateFormat LASTUPDATE_FORMATTER = new SimpleDateFormat("dd/MM,HH:mm:ss");

	public CacheItem(Context context, String type, String itemId) {
		Log.d(TAG, "Creating " + type + "=" + itemId);

		this.helper = new DatabaseHelper(context);
		this.type = type;

		this.id = itemId;
		if(this.id == null) {
			this.id = "";
		}

		CacheRow row = helper.loadRow(type, id);
		if(row != null) {
			lastUpdate = row.lastUpdate;
			data = row.data;

			try {
				item = toItem(data);
			} catch(JSONException e) {
				throw new CacheException("Cannot convert data to item", e);
			}
		}
	}

	public void close() {
		helper.close();
	}
	
	/**
	 * Write the item to backing store
	 */
	public void save() {
		helper.saveRow(type, id, lastUpdate, data);
	}
	
	/**
	 * Retrieve the item
	 * This will trigger a refresh if there's no data cache or it's deemed out of date
	 * 
	 * @return
	 */
	public T getItem() {
		isItemFromCache = true;
		
		Log.d(TAG, "Checking " + type + "=" + id + ","
				+ " lastUpdate=" + (lastUpdate != null ? LASTUPDATE_FORMATTER.format(lastUpdate) : null)
				+ " " + (data != null ? "gotData" : "noData") + " "
				+ (item != null ? "gotItem" : "noItem"));

		// If there's no raw data, or it's out of date...
		if((data == null && item == null) || shouldRefresh()) {
			isItemFromCache = false;
			
			Log.d(TAG, "Refreshing " + type + "=" + id);
			// ...get the item
			lastUpdate = new Date(System.currentTimeMillis());
			try {
				item = refreshItem();
			} catch(JSONException e) {
				throw new CacheException("Cannot refresh " + type + "=" + id, e);
			}

			// Set the data up
			data = toData(item);

			save();
		} else {
			Log.d(TAG, "Using cache version");
		}

		if(item == null) {
			// Convert the existing raw data into an object
			Log.d(TAG, "Converting " + data);
			try {
				item = toItem(data);
			} catch(JSONException e) {
				throw new CacheException("Cannot convert " + type + "=" + id + " data to item", e);
			}
		}

		return item;
	}
	
	public T getAndClose(boolean refresh) {
		if(refresh) {
			clear();
		}
		
		getItem();
		
		close();
		
		return item;
	}

	/**
	 * Indicates whether the data was sourced from the cache or not
	 * This is automatically updated by called getItem
	 * 
	 * @return
	 */
	public boolean isItemFromCache() {
		return isItemFromCache;
	}

	protected boolean shouldRefresh() {
		if(lastUpdate == null) {
			return true;
		}

		Date check = new Date(lastUpdate.getTime() + 60000 * getStaleMinutes());
		Log.d(TAG, "Checking refresh against " + LASTUPDATE_FORMATTER.format(check));
		return System.currentTimeMillis() > check.getTime();
	}

	protected String toData(T item) {
		return item.toString();
	}

	/**
	 * Force the cache to empty
	 */
	public void clear() {
		Log.d(TAG, "Clearing " + type + "=" + id);

		data = null;
		lastUpdate = null;
		item = null;
	}

	protected abstract T refreshItem() throws JSONException;

	protected abstract T toItem(String data) throws JSONException;

	protected abstract long getStaleMinutes();
}
