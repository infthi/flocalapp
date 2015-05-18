package ru.ith.flocal.io;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by adminfthi on 29.06.13.
 */
public class UploadCacheDB extends SQLiteOpenHelper {

	public static final String UPLOAD_TABLE = "uploads";
	public static final String ROW_ID = "ID";
	public static final String ROW_CACHED_FILE = "file";
	public static final String ROW_LAST_CHECKED = "used";

	public UploadCacheDB(Context context) {
		super(context, UPLOAD_TABLE, null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("create table " + UPLOAD_TABLE + " ("
				+ ROW_ID + " VARCHAR(20) not null unique primary key,"
				+ ROW_CACHED_FILE + " VARCHAR(32),"
				+ ROW_LAST_CHECKED + " integer" + ");");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}
}
