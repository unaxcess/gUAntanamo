package org.ua2.guantanamo.data;

import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

class DatabaseHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "cache.db";
	private static final int DATABASE_VERSION = 1;

	private SQLiteDatabase db;
	private static final String ITEM_TABLE = "item";
	private SQLiteStatement itemInsert;
	private static final String ITEM_INSERT = "insert into " + ITEM_TABLE + "(type,id,lastUpdate,data) values (?,?,?,?)";
	private SQLiteStatement itemDelete;
	private static final String ITEM_DELETE = "delete from " + ITEM_TABLE + " where type=? and id=?";

	private static final String TAG = DatabaseHelper.class.getName();

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	private void init() {
		if(db == null || !db.isOpen()) {
			Log.i(TAG, "Initialising database objects");

			db = getWritableDatabase();

			itemInsert = db.compileStatement(ITEM_INSERT);
			itemDelete = db.compileStatement(ITEM_DELETE);
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("create table " + ITEM_TABLE + "(" + " type text," + " id text," + " lastUpdate integer" + "," + " data text," + " primary key (type,id)" + " )");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	public void close() {
		Log.i(TAG, "Closing database objects");

		if(itemInsert != null) {
			itemInsert.close();
		}
		if(itemDelete != null) {
			itemDelete.close();
		}
		if(db != null) {
			db.close();
		}

		super.close();
	}

	public CacheRow loadRow(String type, String id) {
		init();

		Cursor cursor = db.rawQuery("select lastUpdate,data from " + ITEM_TABLE + " where type=? and id=?", new String[] { type, id });
		CacheRow row = null;
		if(cursor.moveToFirst()) {
			row = new CacheRow(new Date(cursor.getLong(0)), cursor.getString(1));
		}
		cursor.close();

		Log.d(TAG, "Loaded " + type + "=" + id + " " + (row != null ? "gotRow" : "noRow"));
		return row;
	}

	public void saveRow(String type, String id, Date lastUpdate, String data) {
		init();

		int col = 1;

		itemDelete.bindString(col++, type);
		itemDelete.bindString(col++, id);
		Log.d(TAG, "Deleting " + type + "=" + id);
		itemDelete.execute();

		col = 1;
		itemInsert.bindString(col++, type);
		itemInsert.bindString(col++, id);
		itemInsert.bindLong(col++, lastUpdate.getTime());
		itemInsert.bindString(col++, data);
		Log.d(TAG, "Inserting " + type + "=" + id + " " + data);
		itemInsert.executeInsert();
	}
}
