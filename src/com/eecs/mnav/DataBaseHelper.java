package com.eecs.mnav;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public class DataBaseHelper extends SQLiteOpenHelper{

	//The Android's default system path of your application database.
	private static String DB_PATH = "/data/data/com.eecs.mnav/databases/";

	private static String DB_NAME;

	private SQLiteDatabase myDataBase; 

	private final Context myContext;

	/**
	 * Constructor
	 * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
	 * @param context
	 */
	public DataBaseHelper(Context context, String name) {

		super(context, DB_NAME, null, 1);
		this.myContext = context;
		DB_NAME = name;
	}	

	/**
	 * Creates a empty database on the system and rewrites it with your own database.
	 * */
	public void createDataBase() throws IOException{

		boolean dbExists = checkDataBase();

		if(dbExists) {
			//do nothing - database already exists
		} 
		else {
			//By calling this method and empty database will be created into the default system path
			//of your application so we are gonna be able to overwrite that database with our database.
			this.getWritableDatabase();

			try {
				copyDataBase();
			} 
			catch (IOException e) {
				throw new Error("Error copying database");
			}
		}
	}

	/**
	 * Check if the database already exist to avoid re-copying the file each time you open the application.
	 * @return true if it exists, false if it doesn't
	 */
	private boolean checkDataBase()
	{
		SQLiteDatabase checkDB = null;

		try
		{
			String myPath = DB_PATH + DB_NAME;

			File DBFolder = new File(DB_PATH);
			if (!DBFolder.exists()) DBFolder.mkdir();

			checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
		}
		catch(SQLiteException e) {
			//database does't exist yet.
		}

		if(checkDB != null) {
			checkDB.close();
		}

		return checkDB != null ? true : false;
	}

	/**
	 * Copies your database from your local assets-folder to the just created empty database in the
	 * system folder, from where it can be accessed and handled.
	 * This is done by transfering bytestream.
	 * */
	private void copyDataBase() throws IOException {

		//Open your local db as the input stream
		InputStream myInput = myContext.getAssets().open(DB_NAME);

		// Path to the just created empty db
		String outFileName = DB_PATH + DB_NAME;

		//Open the empty db as the output stream
		OutputStream myOutput = new FileOutputStream(outFileName);

		//transfer bytes from the inputfile to the outputfile
		byte[] buffer = new byte[1024];
		int length;
		while ((length = myInput.read(buffer))>0){
			myOutput.write(buffer, 0, length);
		}

		//Close the streams
		myOutput.flush();
		myOutput.close();
		myInput.close();

	}

	public void openDataBase() throws SQLException {
		//Open the database
		String myPath = DB_PATH + DB_NAME;
		myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
	}

	@Override
	public synchronized void close() {

		if(myDataBase != null)
			myDataBase.close();

		super.close();

	}

	@Override
	public void onCreate(SQLiteDatabase db) {

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	public Cursor getBldgIdByName(String name) {
		name = name.toUpperCase();
		return myDataBase.rawQuery("SELECT bldg_num, num_doors, name_full FROM buildings " +
				"WHERE upper(name_abbr)='" + name + "' OR upper(name_full)='" + name + "'", null);
	}

	public Cursor getDoorsByBldgId(int bldg_num) {
		return myDataBase.rawQuery("SELECT door_lat, door_long FROM doors WHERE bldg_num=" + bldg_num, null);
	}
	
	public Cursor getAllBldgs() {
		return myDataBase.rawQuery("SELECT name_full, name_abbr FROM buildings", null);
	}
	
	public Cursor getAllBldgAbbrs() {
		return myDataBase.rawQuery("SELECT name_abbr FROM buildings", null);
	}

	public Cursor getSections(String classname) {
		// TODO Auto-generated method stub
		return null;
	}
}