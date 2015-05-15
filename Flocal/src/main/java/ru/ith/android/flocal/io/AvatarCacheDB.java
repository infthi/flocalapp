package ru.ith.android.flocal.io;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by adminfthi on 29.06.13.
 */
public class AvatarCacheDB extends SQLiteOpenHelper {

	public static final String AVATAR_TABLE = "avatars";
	public static final String ROW_USER = "user";
	public static final String ROW_CACHED_FILE = "file";
	public static final String ROW_LAST_CHECKED = "updated";

	public AvatarCacheDB(Context context) {
		super(context, AVATAR_TABLE, null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("create table " + AVATAR_TABLE + " ("
				+ ROW_USER + " VARCHAR(100) not null unique primary key,"
				+ ROW_CACHED_FILE + " VARCHAR(32),"
				+ ROW_LAST_CHECKED + " integer" + ");");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}
}
