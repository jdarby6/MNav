package com.eecs.mnav;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

import com.parse.Parse;

public class StartActivity extends Activity implements TextWatcher {
	//start page items;
	private Button search;
	private AutoCompleteTextView address_box;
	private Button schedule;
	private Button button_bus_routes;
	private Button button_building_finder;
	private Button button_app_info;
	private TextView gInputFeedback;

	private String destBldgName = "";
	private String destRoomNum = "";

	final String[] day_abbrs = new String[] {"NULL", "SU", "MO", "TU", "WE", "TH", "FR", "SA"};
	public static String sPref = null;

	private DataBaseHelper destination_db;
	private ScheduleDatabaseHandler schedule_db;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);

		//Initialize Parse
		Parse.initialize(this, Constants.parse_applicationId, Constants.parse_clientKey);

		//Initialize destination db
		destination_db = new DataBaseHelper("destination_db");
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
			String full_name = cursor.getString(0);
			String abbr_name = cursor.getString(1);
			strings.add(abbr_name+": " + full_name);
		}

		cursor.close();
		destination_db.close();
		String[] item = (String[]) strings.toArray(new String[strings.size()]);

		address_box = (AutoCompleteTextView)findViewById(R.id.autoCompleteTextView_address_box);
		search = (Button)findViewById(R.id.button_search);
		schedule = (Button)findViewById(R.id.button_schedule);
		button_bus_routes = (Button)findViewById(R.id.button_bus_routes);
		button_building_finder = (Button)findViewById(R.id.button_bldg_finder);
		button_app_info = (Button)findViewById(R.id.button_info);
		gInputFeedback = (TextView)findViewById(R.id.textView_input_feedback);

		address_box.addTextChangedListener(this);
		address_box.setTextColor(Color.BLACK);
		address_box.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, item));
		//Hide soft keyboard when an option is clicked
		address_box.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				in.hideSoftInputFromWindow(address_box.getWindowToken(), 0);

			}

		});
		//Programmatically press the search button when the search key is pressed on the soft keyboard
		address_box.setOnEditorActionListener(new AutoCompleteTextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEARCH) {
					search.performClick();
					return true;
				}
				return false;
			}
		});

		search.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//hide the soft keyboard
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(address_box.getWindowToken(), 0);

				String tempAddress = address_box.getText().toString();
				if(tempAddress.indexOf(':') != -1) {
					String destAbbr = tempAddress.substring(0, tempAddress.indexOf(':'));
					tempAddress = destAbbr;
				}
				gInputFeedback.setText("");
				if(tempAddress.matches(Constants.REGEX_ROOM_NUM) || tempAddress.matches(Constants.REGEX_BLDG_NAME)) {
					if(tempAddress.matches(Constants.REGEX_ROOM_NUM)) {
						destRoomNum = tempAddress.substring(0,tempAddress.indexOf(" "));
						destBldgName = tempAddress.substring(tempAddress.indexOf(" ")).trim();
						Log.d("Search Button", "Matches REGEX_ROOM_NUM! RoomNum="+destRoomNum+" BldgName="+destBldgName);
					} else {//It should just be the name of the bldg
						destBldgName = tempAddress;
						destRoomNum = "";
						Log.d("Search Button", "Matches REGEX_BLDG_NAME! RoomNum="+destRoomNum+" BldgName="+destBldgName);
					}
				} else if(tempAddress != null && tempAddress.length() > 0){
					//It doesn't match our regEx so it's an invalid entry.
					gInputFeedback.setText("Invalid destination entry.");
					return;
				}
				//Save destination address
				Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
				if(tempAddress == null || tempAddress.length() == 0) {
					editor.putString("DESTNAME", "the diag");
					editor.putString("DESTROOM", "");
				} else {
					editor.putString("DESTNAME", destBldgName);
					editor.putString("DESTROOM", destRoomNum);
				}
				editor.commit();
				Intent searchIntent = new Intent(StartActivity.this, MainMapActivity.class);
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
				int hour = calendar.get(Calendar.HOUR_OF_DAY);
				int time = calendar.get(Calendar.MINUTE)+(hour*60);

				schedule_db = new ScheduleDatabaseHandler();

				String tempAddress = "NULL";
				boolean found = false;
				int etime = 0;
				ArrayList<MEvent>tmp_array = new ArrayList<MEvent>();
				tmp_array = schedule_db.getDay(day_abbrs[day]);
				for(int i = 0; i < tmp_array.size(); i++){
					etime = tmp_array.get(i).getIndex();
					if(time > etime && etime >= (time - 15)){//if event started within past 15 minutes
						found = true;
						tempAddress=tmp_array.get(i).getLocation();
					}
					else if(time<etime && time +30 >= etime){
						found = true;
						tempAddress=tmp_array.get(i).getLocation();
					}
				}

				schedule_db.close();


				if(found){
					gInputFeedback.setText("");
					if(tempAddress.matches(Constants.REGEX_ROOM_NUM) || tempAddress.matches(Constants.REGEX_BLDG_NAME) || tempAddress.matches(Constants.REGEX_ROOM_NUM_AFTER)) {
						if(tempAddress.matches(Constants.REGEX_ROOM_NUM)) {
							destRoomNum = tempAddress.substring(0,tempAddress.indexOf(" "));
							destBldgName = tempAddress.substring(tempAddress.indexOf(" ")).trim();
							Log.d("Schedule Button", "Matches REGEX_ROOM_NUM! RoomNum="+destRoomNum+" BldgName="+destBldgName);
						} else if(tempAddress.matches(Constants.REGEX_ROOM_NUM_AFTER)) {
							destBldgName = tempAddress.substring(0,tempAddress.indexOf(" "));
							destRoomNum = tempAddress.substring(tempAddress.indexOf(" ")).trim();
							Log.d("Schedule Button", "Matches REGEX_ROOM_NUM_AFTER! RoomNum="+destRoomNum+" BldgName="+destBldgName);
						}else {//It should just be the name of the bldg
							destBldgName = tempAddress;
							destRoomNum = "";
							Log.d("Schedule Button", "Matches REGEX_BLDG_NAME! RoomNum="+destRoomNum+" BldgName="+destBldgName);
						}
					}

					Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
					editor.putString("DESTNAME", destBldgName.toUpperCase(Locale.US));
					editor.putString("DESTROOM", destRoomNum);
					editor.commit();

					Intent searchIntent = new Intent(StartActivity.this, MainMapActivity.class);
					StartActivity.this.startActivity(searchIntent);
				}
				return true;
			}
		});

		button_bus_routes.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(StartActivity.this, BusRoutesActivity.class);
				StartActivity.this.startActivity(intent);
			}
		});

		button_building_finder.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(StartActivity.this, BuildingFinderActivity.class);
				StartActivity.this.startActivity(intent);
			}
		});

		button_app_info.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent scheduleIntent = new Intent(StartActivity.this, InfoActivity.class);
				StartActivity.this.startActivity(scheduleIntent);

			}
		});

	}

	@Override
	protected void onResume() {
		super.onResume();
		HelperFunctions.checkGPS(this);
		//isGooglePlayServicesAvailable();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_start, menu);
		return true;
	}

	public void afterTextChanged(Editable arg0) {

	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {

	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}
}
