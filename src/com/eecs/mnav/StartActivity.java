package com.eecs.mnav;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

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
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

public class StartActivity extends Activity implements TextWatcher {
	//start page items;
	private Button search;
	private AutoCompleteTextView address_box;
	private Button schedule;
	private Button button_bus_routes;
	private Button button_app_info;
	private TextView gInputFeedback;

	private String destBldgName = "";
	private String destRoomNum = "";

	final String[] day_abbrs = new String[] {"NULL", "SU", "MO", "TU", "WE", 
			"TH", "FR", "SA"};
	private static final String REGEX_ROOM_NUM = "^[0-9]{1,4} [a-zA-Z]+ *";
	private static final String REGEX_BLDG_NAME = "^[a-zA-Z][a-zA-Z &]+";

	private DataBaseHelper destination_db;
	private ScheduleDatabaseHandler schedule_db;

	static Context context;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);

		context = getApplicationContext();

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
		button_bus_routes = (Button)findViewById(R.id.button_bus_routes);
		button_app_info = (Button)findViewById(R.id.button_info);
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
		        int hour = calendar.get(Calendar.HOUR_OF_DAY);
		        int time = calendar.get(Calendar.MINUTE)+(hour*60);
		        
		        schedule_db = new ScheduleDatabaseHandler(v.getContext());
		        
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
				editor.putString("DESTNAME", destBldgName.toUpperCase());
				editor.putString("DESTROOM", destRoomNum);
				editor.commit();

				Intent searchIntent = new Intent(StartActivity.this, MNavMainActivity.class);
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

		button_app_info.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent scheduleIntent = new Intent(StartActivity.this, InfoActivity.class);
				StartActivity.this.startActivity(scheduleIntent);

			}
		});

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

	/** Helper function for displaying a toast. Takes the string to be displayed and the length: LONG or SHORT **/
/*	private void toastThis(String toast, int duration) {
		Toast t = Toast.makeText(context, toast, duration);
		t.show();
	}
*/
}
