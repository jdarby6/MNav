package com.eecs.mnav;

import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ScheduleDatabaseHandler extends SQLiteOpenHelper {

	// All Static variables
	// Database Version
	private static final int DATABASE_VERSION = 1;

	// Database Name
	private static final String DATABASE_NAME = "scheduleManager";

	// Contacts table name
	private static final String TABLE_SAVEDEVENTS = "savedEvents";

	// Contacts Table Columns names
	public static final String KEY_ID = "_id"; // this is necessary to have, but it doesn't mean anything	
	public static final String KEY_EVENTINDEX = "eventindex";
	public static final String KEY_LABEL = "label";
	public static final String KEY_LOCATION = "location";
	public static final String KEY_TIMEBEGIN = "begin";
	public static final String KEY_TIMEEND = "end";
	public static final String KEY_DAYS = "days";

	public ScheduleDatabaseHandler() {
		super(ReportingApplication.getAppContext(), DATABASE_NAME, null, DATABASE_VERSION);
	}

	// Creating Tables
	@Override
	public void onCreate(SQLiteDatabase db) {		
		String CREATE_SAVEDEVENTS_TABLE = "CREATE TABLE " + TABLE_SAVEDEVENTS + "("
				+ KEY_ID + " INTEGER PRIMARY KEY," 
				+ KEY_EVENTINDEX + " INTEGER," 
				+ KEY_LABEL + " TEXT NOT NULL," 
				+ KEY_LOCATION + " TEXT NOT NULL," 
				+ KEY_TIMEBEGIN + " TEXT," 
				+ KEY_TIMEEND + " TEXT," 
				+ KEY_DAYS + " TEXT NOT NULL)";
		db.execSQL(CREATE_SAVEDEVENTS_TABLE);
	}

	//schedule name not used


	// Upgrading database
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older table if existed
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SAVEDEVENTS);

		// Create tables again
		onCreate(db);
	}

	/**
	 * All CRUD(Create, Read, Update, Delete) Operations
	 */

	//Adds a single row to the table, representing a single timer in the array
	void addEvent(MEvent event, String scheduleName) {
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(KEY_EVENTINDEX, event.getIndex());
		values.put(KEY_LABEL, event.getLabel());
		values.put(KEY_LOCATION, event.getLocation());
		values.put(KEY_TIMEBEGIN, event.getTimeBegin());
		values.put(KEY_TIMEEND, event.getTimeEnd());
		values.put(KEY_DAYS, event.getDays());


		// Inserting Row
		db.insert(TABLE_SAVEDEVENTS, null, values);

	}

	//Adds a collection of timers to the table
	public void addSchedule(ArrayList<MEvent> events, String scheduleName) {
		for(int i = 0; i < events.size(); i++) {
			addEvent(events.get(i), scheduleName);
		}
	}

	public ArrayList<MEvent> getDay(String day) {//returns list for a day
		ArrayList<MEvent> events = new ArrayList<MEvent>();
		// Query //don't really need days, since only looking at certain day, but w/e
		String query = "SELECT  " + KEY_LABEL + ", " + KEY_LOCATION + ", " 
				+ KEY_EVENTINDEX + ", " 
				+ KEY_TIMEBEGIN + ", " 
				+ KEY_TIMEEND + ", "
				+ KEY_DAYS + " FROM " 
				+ TABLE_SAVEDEVENTS
				+ " WHERE " + KEY_DAYS + " LIKE '%" + day
				+ "%'"+ " ORDER BY "
				+ KEY_EVENTINDEX + " ASC";


		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(query, null);

		// looping through all rows and adding to list
		//error check if no days
		if (cursor.moveToFirst()) {
			do {
				String label = cursor.getString(0);
				String location = cursor.getString(1);
				int index = cursor.getInt(2);
				String timeBegin = cursor.getString(3);
				String timeEnd = cursor.getString(4);
				String days = cursor.getString(5);
				MEvent event = new MEvent(label, location,index, timeBegin, timeEnd, days);
				event.restoreLabel(label);
				// Adding contact to list
				events.add(event);

			} while (cursor.moveToNext());

		}
		cursor.close();
		return events;
	}

	public void deleteEvent(String classname){

		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_SAVEDEVENTS, KEY_LABEL + " = ?", new String[] { classname });

	}


}
