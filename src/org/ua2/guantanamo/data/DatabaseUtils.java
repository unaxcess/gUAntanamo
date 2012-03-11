package org.ua2.guantanamo.data;

import java.util.Date;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;

class DatabaseUtils {
	private static DatabaseHelper HELPER;
	private static Lock LOCK = new ReentrantLock();
	private static Condition HAS_CONTEXT = LOCK.newCondition();
	
	public static void setContext(Context context) {
		LOCK.lock();
		
		if(context != null) {
			HELPER = new DatabaseHelper(context);
			
			HAS_CONTEXT.signal();
		} else {
			HELPER = null;
		}
		
		LOCK.unlock();
	}

	public static CacheRow loadRow(String type, String id) {
		try {
			LOCK.lock();
			
			while(HELPER == null) {
				try {
					HAS_CONTEXT.await();
				} catch(InterruptedException e) {
				}
			}
			
			return HELPER.loadRow(type, id);
		} finally {
			LOCK.unlock();
		}
	}
	
	public static void saveRow(String type, String id, Date lastUpdate, String data) {
		try {
			LOCK.lock();
			
			while(HELPER == null) {
				try {
					HAS_CONTEXT.await();
				} catch(InterruptedException e) {
				}
			}
			
			HELPER.saveRow(type, id, lastUpdate, data);
		} finally {
			LOCK.unlock();
		}
	}
}
