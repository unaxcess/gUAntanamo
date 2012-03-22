package org.ua2.guantanamo.data;

import java.util.Date;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.util.Log;

public class DatabaseUtils {
	private static DatabaseHelper HELPER;
	private static Lock LOCK = new ReentrantLock();
	private static Condition HAS_HELPER = LOCK.newCondition();
	
	private static final String TAG = DatabaseUtils.class.getName();
	
	public static void setContext(Context context) {
		LOCK.lock();
		
		if(HELPER != null) {
			Log.i(TAG, "Closing helper on " + Thread.currentThread().getName());
			HELPER.close();
		}
		
		if(context != null) {
			HELPER = new DatabaseHelper(context);
			
			Log.i(TAG, "Signalling helper on " + Thread.currentThread().getName());
			HAS_HELPER.signal();
		} else {
			Log.i(TAG, "Setting helper to null");
			HELPER = null;
		}
		
		LOCK.unlock();
	}

	public static CacheRow loadRow(String type, String id) {
		try {
			LOCK.lock();

			long start = System.currentTimeMillis();
			while(HELPER == null) {
				Log.i(TAG, "Waiting for helper on " + Thread.currentThread().getName());
				try {
					HAS_HELPER.await();
				} catch(InterruptedException e) {
				}
			}
			long diff = System.currentTimeMillis() - start;
			Log.i(TAG, "Loading row " + type + "=" + id + " after " + diff + "ms");			
			return HELPER.loadRow(type, id);
		} finally {
			LOCK.unlock();
		}
	}
	
	public static void saveRow(String type, String id, Date lastUpdate, String data) {
		try {
			LOCK.lock();
			
			long start = System.currentTimeMillis();
			while(HELPER == null) {
				Log.i(TAG, "Waiting for helper on " + Thread.currentThread().getName());
				try {
					HAS_HELPER.await();
				} catch(InterruptedException e) {
				}
			}
			long diff = System.currentTimeMillis() - start;
			Log.i(TAG, "Saving row " + type + "=" + id + " after " + diff + "ms");			
			HELPER.saveRow(type, id, lastUpdate, data);
		} finally {
			LOCK.unlock();
		}
	}
}
