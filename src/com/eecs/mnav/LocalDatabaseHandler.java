package com.eecs.mnav;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class LocalDatabaseHandler extends SQLiteOpenHelper {

	// All Static variables
	// Database Version
	private static final int DATABASE_VERSION = 1;

	// Database Name
	private static final String DATABASE_NAME = "doorGpsCoordsManager";

	// Door GPS Coordinates table name
	private static final String TABLE_DOORGPSCOORDS = "buildingGpsCoords";

	// Contacts Table Columns names
	public static final String KEY_ID = "_id"; // this is necessary to have, but it doesn't mean anything
	public static final String KEY_BUILDINGNAME_ABBR = "buildingname_abbr";
	public static final String KEY_DOOR_NICKNAME = "door_nickname";
	public static final String KEY_LATITUDE = "latitude";
	public static final String KEY_LONGITUDE = "longitude";

	public LocalDatabaseHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	// Creating Tables
	@Override
	public void onCreate(SQLiteDatabase db) {		
		String CREATE_DOORGPSCOORDS_TABLE = "CREATE TABLE " + TABLE_DOORGPSCOORDS + "("
				+ KEY_ID + " INTEGER PRIMARY KEY," + KEY_BUILDINGNAME_ABBR + " TEXT,"
				+ KEY_DOOR_NICKNAME + " TEXT," + KEY_LATITUDE + " TEXT," 
				+ KEY_LONGITUDE + " TEXT)";
		db.execSQL(CREATE_DOORGPSCOORDS_TABLE);
	}

	// Upgrading database
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older table if existed
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_DOORGPSCOORDS);

		// Create tables again
		onCreate(db);
	}

	/**
	 * All CRUD(Create, Read, Update, Delete) Operations
	 */

	//Adds a single row to the table, representing a single door of a building
	void addRow(String buildingname_abbr, String door_nickname, String latitude, String longitude) {
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(KEY_BUILDINGNAME_ABBR, buildingname_abbr);
		values.put(KEY_DOOR_NICKNAME, door_nickname);
		values.put(KEY_LATITUDE, latitude);
		values.put(KEY_LONGITUDE, longitude);

		// Inserting Row
		db.insert(TABLE_DOORGPSCOORDS, null, values);

		Log.d("DatabaseHandler", "Added coords (" + latitude + "," + longitude + " for building \"" + buildingname_abbr + "\"");
	}
}