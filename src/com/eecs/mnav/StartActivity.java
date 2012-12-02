package com.eecs.mnav;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class StartActivity extends Activity implements TextWatcher {
	//start page items;
	private Button search;
	private AutoCompleteTextView address_box;
	private Button schedule;
	private TextView gInputFeedback;

	private String destBldgName = "";
	private String destRoomNum = "";
	
	final String[] day_abbrs = new String[] {"NULL", "SU", "MO", "TU", "WE", 
			"TH", "FR", "SA"};
	private static final String REGEX_ROOM_NUM = "^[0-9]{1,4} [a-zA-Z]+ *";
	private static final String REGEX_BLDG_NAME = "^[a-zA-Z][a-zA-Z &]+";
	private static final int LONG = Toast.LENGTH_LONG;
	private static final int SHORT = Toast.LENGTH_SHORT;	
	public static final String WIFI = "Wi-Fi";
	public static final String ANY = "Any";
	private static final String URL = "http://mbus.pts.umich.edu/shared/public_feed.xml";

	// Whether there is a Wi-Fi connection.
	private static boolean wifiConnected = false; 
	// Whether there is a mobile connection.
	private static boolean mobileConnected = false;
	// Whether the display should be refreshed.
	public static boolean refreshDisplay = true; 
	public static String sPref = null;
	
	private DataBaseHelper destination_db;
	private ScheduleDatabaseHandler schedule_db;

	String item[]={
			"January", "February", "March", "April",
			"May", "June", "July", "August",
			"September", "October", "November", "December"
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);
		
		//Initialize destination db
		destination_db = new DataBaseHelper(this, "destination_db");
		try {
			destination_db.createDataBase();
		} 
		catch (IOException ioe) {
			throw new Error("Unable to create database");
		}
		try {
			destination_db.openDataBase();
		} 
		catch(SQLException sqle) {	
			throw sqle;

		}
		
		Cursor cursor = destination_db.getAllBldgs();
		
		ArrayList<String> strings = new ArrayList<String>();
		for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
		   String mTitleRaw = cursor.getString(0);
		   strings.add(mTitleRaw);
		}
		
		cursor.close();
		destination_db.close();
		String[] item = (String[]) strings.toArray(new String[strings.size()]);

		address_box = (AutoCompleteTextView)findViewById(R.id.autoCompleteTextView_address_box);
		search = (Button)findViewById(R.id.button_search);
		schedule = (Button)findViewById(R.id.button_schedule);
		gInputFeedback = (TextView)findViewById(R.id.textView_input_feedback);
		
		address_box.addTextChangedListener(this);
		address_box.setTextColor(Color.BLACK);
		address_box.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, item));

		search.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//hide the soft keyboard
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(address_box.getWindowToken(), 0);
				
				String tempAddress = address_box.getText().toString();
				gInputFeedback.setText("");
				if(tempAddress.matches(REGEX_ROOM_NUM) || tempAddress.matches(REGEX_BLDG_NAME)) {
					if(tempAddress.matches(REGEX_ROOM_NUM)) {
						destRoomNum = tempAddress.substring(0,tempAddress.indexOf(" "));
						destBldgName = tempAddress.substring(tempAddress.indexOf(" ")).trim();
						Log.d("Search Button", "Matches REGEX_ROOM_NUM! RoomNum="+destRoomNum+" BldgName="+destBldgName);
					} else {//It should just be the name of the bldg
						destBldgName = tempAddress;
						destRoomNum = "";
						Log.d("Search Button", "Matches REGEX_BLDG_NAME! RoomNum="+destRoomNum+" BldgName="+destBldgName);
					}
				} else {
					//It doesn't match our regEx so it's an invalid entry.
					gInputFeedback.setText("Invalid destination entry.");
					return;
				}
				//Save destination address
				Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
				editor.putString("DESTNAME", destBldgName);
				editor.putString("DESTROOM", destRoomNum);
				editor.commit();
				
				Intent searchIntent = new Intent(StartActivity.this, MNavMainActivity.class);
				StartActivity.this.startActivity(searchIntent);
			}

		});

		schedule.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				
				Intent scheduleIntent = new Intent(StartActivity.this, ScheduleActivity.class);
				StartActivity.this.startActivity(scheduleIntent);

			}


		});
		schedule.setOnLongClickListener(new OnLongClickListener() {

			public boolean onLongClick(View v) {
				//reach into db and get current time day
				//for now, get first on list
				
				
				Calendar calendar = Calendar.getInstance();
		        int day = calendar.get(Calendar.DAY_OF_WEEK);
		        
		        schedule_db = new ScheduleDatabaseHandler(v.getContext());
		        
		        String tempAddress = "";
		        ArrayList<MEvent>tmp_array = new ArrayList<MEvent>();
		    	tmp_array = schedule_db.getDay(day_abbrs[day]);
		    	tempAddress = tmp_array.get(0).getLocation();
		    	//future loop events to see if time correct, right now just first event
		    	schedule_db.close();
		    	
				
				
				gInputFeedback.setText("");
				if(tempAddress.matches(REGEX_ROOM_NUM) || tempAddress.matches(REGEX_BLDG_NAME)) {
					if(tempAddress.matches(REGEX_ROOM_NUM)) {
						destRoomNum = tempAddress.substring(0,tempAddress.indexOf(" "));
						destBldgName = tempAddress.substring(tempAddress.indexOf(" ")).trim();
						Log.d("Schedule Button", "Matches REGEX_ROOM_NUM! RoomNum="+destRoomNum+" BldgName="+destBldgName);
					} else {//It should just be the name of the bldg
						destBldgName = tempAddress;
						destRoomNum = "";
						Log.d("Schedule Button", "Matches REGEX_BLDG_NAME! RoomNum="+destRoomNum+" BldgName="+destBldgName);
					}
				}
				
				Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
				editor.putString("DESTNAME", destBldgName);
				editor.putString("DESTROOM", destRoomNum);
				editor.commit();
				
				Intent searchIntent = new Intent(StartActivity.this, MNavMainActivity.class);
				StartActivity.this.startActivity(searchIntent);
				
				return true;

			}


		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_start, menu);
		return true;
	}


	// Uses AsyncTask to download the XML feed from mbus.pts.umich.edu.
	public void loadPage() {  

		if((sPref.equals(ANY)) && (wifiConnected || mobileConnected)) {
			new DownloadXmlTask().execute(URL);
		}
		else if ((sPref.equals(WIFI)) && (wifiConnected)) {
			new DownloadXmlTask().execute(URL);
		} else {
			// show error
		}  
	}

	// Implementation of AsyncTask used to download XML feed from mbus.pts.umich.edu.
	private class DownloadXmlTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {
			try {
				return loadXmlFromNetwork(urls[0]);
			} catch (IOException e) {
				return "Connection error";
			} catch (XmlPullParserException e) {
				return "XML error";
			}
		}

		@Override
		protected void onPostExecute(String result) {  
			/*
			setContentView(R.layout.main);
			// Displays the HTML string in the UI via a WebView
			WebView myWebView = (WebView) findViewById(R.id.webview);
			myWebView.loadData(result, "text/html", null);
			 */
		}
	}

	// Uploads XML from mbus.pts.umich.edu and parses it
	private String loadXmlFromNetwork(String urlString) throws XmlPullParserException, IOException {
		InputStream stream = null;
		// Instantiate the parser
		MbusPublicFeedXmlParser mbusPublicFeedXmlParser = new MbusPublicFeedXmlParser();
		List<MbusPublicFeedXmlParser.Route> routes = null;

		try {
			stream = downloadUrl(urlString);        
			routes = mbusPublicFeedXmlParser.parse(stream);
			// Makes sure that the InputStream is closed after the app is
			// finished using it.
		} finally {
			if (stream != null) {
				stream.close();
			} 
		}

		// StackOverflowXmlParser returns a List (called "entries") of Entry objects.
		// Each Entry object represents a single post in the XML feed.
		// This section processes the entries list to combine each entry with HTML markup.
		// Each entry is displayed in the UI as a link that optionally includes
		// a text summary.
		for (MbusPublicFeedXmlParser.Route route : routes) {

		}
		return "Something";
	}

	// Given a string representation of a URL, sets up a connection and gets
	// an input stream.
	private InputStream downloadUrl(String urlString) throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000 /* milliseconds */);
		conn.setConnectTimeout(15000 /* milliseconds */);
		conn.setRequestMethod("GET");
		conn.setDoInput(true);
		// Starts the query
		conn.connect();
		InputStream stream = conn.getInputStream();
		return stream;
	}

	public void afterTextChanged(Editable arg0) {
		// TODO Auto-generated method stub

	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// TODO Auto-generated method stub

	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub

	}
	
	/** Helper function for displaying a toast. Takes the string to be displayed and the length: LONG or SHORT **/
	private void toastThis(String toast, int duration) {
		Context context = getApplicationContext();
		Toast t = Toast.makeText(context, toast, duration);
		t.show();
	}
	
	
}
