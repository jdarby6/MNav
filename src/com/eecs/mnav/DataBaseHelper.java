package com.eecs.mnav;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DataBaseHelper extends SQLiteOpenHelper {
	
	//The Android's default system path of your application database.
	private static String DB_PATH;
	private static String DB_NAME;
	private SQLiteDatabase myDataBase; 

	/**
	 * Sets the DB_NAME field to copy the appropriate pre-existing database to the app's database directory to be used 
	 * @param name The filename of the pre-existing database
	 */
	public DataBaseHelper(String name) {
		
		super(ReportingApplication.getAppContext(), DB_NAME, null, 1);
		DB_NAME = name;
		
	    if(android.os.Build.VERSION.SDK_INT >= 4.2){
	        DB_PATH = ReportingApplication.getAppContext().getApplicationInfo().dataDir + "/databases/";         
	     }
	     else
	     {
	        DB_PATH = ReportingApplication.getAppContext().getFilesDir().getParentFile().getPath() + "/databases/";
	     }
	}	

	/**
	 * Creates a empty database on the system and rewrites it with your own database.
	 * @throws IOException
	 */
	public void createDataBase() throws IOException {

		boolean dbExists = checkDataBase();

		if(dbExists) {
			//do nothing - database already exists
		} 
		else {
			//By calling this method, an empty database will be created into the default system path of
			//our application, and we will be able to overwrite that database with our existing database.
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
	 * Check if the database already exists to avoid re-copying the file each time you open the application.
	 * @return true if it exists, false if it doesn't
	 */
	private boolean checkDataBase() {
		SQLiteDatabase checkDB = null;

		try {
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
	 * Copies your database from your local assets folder to the just0created empty database in the
	 * system folder, from where it can be accessed and handled.
	 * This is done by transferring bytestream.
	 * @throws IOException
	 * */
	private void copyDataBase() throws IOException {

		//Open your local db as the input stream
		InputStream myInput = ReportingApplication.getAppContext().getAssets().open(DB_NAME);

		try {
			// Path to the just created empty db
			String outFileName = DB_PATH + DB_NAME;

			//Open the empty db as the output stream
			OutputStream myOutput = new FileOutputStream(outFileName);

			//transfer bytes from the inputfile to the outputfile
			byte[] buffer = new byte[1024];
			int length;
			while ((length = myInput.read(buffer)) > 0) {
				myOutput.write(buffer, 0, length);
			}

			//Close the streams
			myOutput.flush();
			myOutput.close();
			myInput.close();
		}
		catch (IOException e) {
			Log.e("DataBaseHelper", "Exception thrown in copyDataBase()");
		}

	}

	/**
	 * Generic method to open the database referenced by this instance of DatabaseHelper
	 * @throws SQLException
	 */
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

	/**
	 * Retrieves relevant information for a building by name
	 * @param name The full or abbreviated name of the building for which you need info
	 * @return Cursor containing bldg_num, num_doors, and name_full for building whose
	 *         full name or abbreviation matches the name parameter
	 */
	public Cursor getBldgIdByName(String name) {
		name = name.toUpperCase(Locale.US);
		return myDataBase.rawQuery("SELECT bldg_num, num_doors, name_full FROM buildings " +
				"WHERE upper(name_abbr)='" + name + "' OR upper(name_full)='" + name + "'", null);
	}

	public Cursor getAllBldgIds() {
		return myDataBase.rawQuery("SELECT bldg_num, num_doors, name_full FROM buildings", null);
	}
	
	/**
	 * Retrieves all of the door locations for a building whose number is bldg_num
	 * @param bldg_num The building id for which we need the door locations 
	 * @return Cursor containing door_lat and door_long for each door of the building with
	 *         the id bldg_num 
	 */
	public Cursor getDoorsByBldgId(int bldg_num) {
		return myDataBase.rawQuery("SELECT door_lat, door_long FROM doors WHERE bldg_num=" + bldg_num, null);
	}

	/**
	 * Retrieves the full and abbreviated name of each building in the database
	 * @return Cursor containing name_full and name_abbr for each building
	 */
	public Cursor getAllBldgs() {
		return myDataBase.rawQuery("SELECT name_full, name_abbr FROM buildings", null);
	}

	/** 
	 * Retrieves just the abbreviated name of each building in the database
	 * @return Cursor containing name_abbr for each building
	 */
	public Cursor getAllBldgAbbrs() {
		return myDataBase.rawQuery("SELECT name_abbr FROM buildings", null);
	}

	public Cursor getSections(String classname) {

		return null;
	}

	/**
	 * Returns everything contained in our database of class info
	 * @return Cursor containing information for all classes
	 */
	public Cursor getAllClasses() {
		return myDataBase.rawQuery("SELECT * FROM class_info", null);
	}
}