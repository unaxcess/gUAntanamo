package org.ua2.guantanamo.data;

import java.util.Date;

/**
 * Structure for passing timestamp and raw data around
 * 
 * @author Michael
 * 
 */
public class CacheRow {
	Date lastUpdate;
	String data;

	public CacheRow(Date lastUpdate, String data) {
		this.lastUpdate = lastUpdate;
		this.data = data;
	}
}
