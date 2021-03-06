package com.eecs.mnav;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
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
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;

public class ScheduleActivity extends Activity implements TextWatcher {

	private MEventArrayAdapter eventArrayAdapter;

	private ArrayList<MEvent> events_array = new ArrayList<MEvent>();
	final String[] day_names = new String[] {"Every Day","Sunday", "Monday", "Tuesday", "Wednesday", 
			"Thursday", "Friday", "Saturday" };
	final String[] day_abbrs = new String[] {"NULL", "SU", "MO", "TU", "WE", 
			"TH", "FR", "SA"};
	private String destBldgName = "";
	private String destBldgFullName = "";
	private String destRoomNum = "";
	private ListView list_data;

	private MEvent curEvent;

	private Button button_add_event;

	private Button button_cancel_edit;
	private Button button_done;	
	private EditText editText_end_time;
	private EditText editText_begin_time;
	private EditText editText_location;
	private AutoCompleteTextView autoCompleteTextView_class;

	private CheckBox checkBoxMonday;
	private CheckBox checkBoxTuesday;
	private CheckBox checkBoxWednesday;
	private CheckBox checkBoxThursday;
	private CheckBox checkBoxFriday;
	private CheckBox checkBoxSaturday;
	private CheckBox checkBoxSunday;

	private TextView textView_today;

	private static final int DIALOG_ADD_EVENT = 0;
	private static final int DIALOG_CHANGE_EVENT = 1;
	private static final int TIME_PICK_BEGIN_DIALOG_ID = 2;
	private static final int TIME_PICK_END_DIALOG_ID = 3;
	private static final int DIALOG_OPTION = 4;
	//private static final int DIALOG_SECTION = 5;
	private static String CURRENTDAY = "NULL";

	public static int CDI = 0; //current days int;

	private DataBaseHelper all_classes_db;
	private ScheduleDatabaseHandler db;
	private Cursor cursor_all_classes;
	private ArrayAdapter<String> adapter;
	private String[] item;
	private ProgressDialog progressDialog;

	private DataBaseHelper destination_db;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_schedule);

		textView_today = (TextView) findViewById(R.id.textView_today);

		autoSetDay(textView_today);

		textView_today.setOnClickListener(new TextView.OnClickListener() { 
			public void onClick(View v) {
				CDI++; CDI %=8;
				CURRENTDAY = day_abbrs[CDI];
				textView_today.setText(day_names[CDI]);
				loadEventsArray();
			}
		});
		textView_today.setOnLongClickListener(new TextView.OnLongClickListener() { 
			public boolean onLongClick(View v) {
				Calendar calendar = Calendar.getInstance();
				int day = calendar.get(Calendar.DAY_OF_WEEK);
				CDI = day;
				CURRENTDAY = day_abbrs[day];
				textView_today.setText("Today's Schedule");
				loadEventsArray();
				return true;
			}
		});


		curEvent = new MEvent();

		db = new ScheduleDatabaseHandler();

		eventArrayAdapter = new MEventArrayAdapter(this, events_array);
		list_data = (ListView) findViewById(R.id.list_data);
		list_data.setAdapter(eventArrayAdapter);
		list_data.setEmptyView(findViewById(R.id.textView_empty));//format problem keep button bottom



		loadEventsArray();
		if(eventArrayAdapter.isEnabled(0)) Log.d("Schedule Items", "enabled");

		button_add_event = (Button) findViewById(R.id.button_add_event);
		button_add_event.setOnClickListener(new Button.OnClickListener() { 
			public void onClick(View v) {
				new GetClassDataTask().execute();
			}
		});
		list_data.setOnItemClickListener(new OnItemClickListener(){

			public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
					long id) {

				curEvent.setLabel(eventArrayAdapter.getItem(pos).getLabel());
				curEvent.setLocation(eventArrayAdapter.getItem(pos).getLocation());
				curEvent.setIndex(eventArrayAdapter.getItem(pos).getIndex());
				curEvent.setTimeBegin(eventArrayAdapter.getItem(pos).getTimeBegin());
				curEvent.setTimeEnd(eventArrayAdapter.getItem(pos).getTimeEnd());
				curEvent.setDays(eventArrayAdapter.getItem(pos).getDays());

				showDialog(DIALOG_OPTION);


			}});

		//Set long-click listener to list items
		list_data.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int pos, long id) {
				Log.d("Schedule", "got to inside long click");

				curEvent.setLabel(eventArrayAdapter.getItem(pos).getLabel());
				curEvent.setLocation(eventArrayAdapter.getItem(pos).getLocation());
				curEvent.setIndex(eventArrayAdapter.getItem(pos).getIndex());
				curEvent.setTimeBegin(eventArrayAdapter.getItem(pos).getTimeBegin());
				curEvent.setTimeEnd(eventArrayAdapter.getItem(pos).getTimeEnd());
				curEvent.setDays(eventArrayAdapter.getItem(pos).getDays());

				showDialog(DIALOG_CHANGE_EVENT);
				return true;
			}
		});
	}

	private void autoSetDay(TextView title) {

		Calendar calendar = Calendar.getInstance();
		int day = calendar.get(Calendar.DAY_OF_WEEK);
		CDI = day;
		CURRENTDAY = day_abbrs[day];
		title.setText("Today's Schedule");

	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog){
		switch(id) {
		case DIALOG_ADD_EVENT:
			editText_end_time.setText("");
			editText_begin_time.setText("");
			editText_location.setText("");
			autoCompleteTextView_class.setText("");
			checkBoxMonday.setChecked(false);
			checkBoxTuesday.setChecked(false);
			checkBoxWednesday.setChecked(false);
			checkBoxThursday.setChecked(false);
			checkBoxFriday.setChecked(false);
			checkBoxSaturday.setChecked(false);
			checkBoxSunday.setChecked(false);
			break;
		case DIALOG_CHANGE_EVENT:
			//set all those to prev values? in create instead?
			break;
		default:
			break;
		}
	}

	@Override
	protected Dialog onCreateDialog(final int id){
		final Dialog dialogEditEvent = new Dialog(this);		
		switch(id) {
		case DIALOG_ADD_EVENT:
			dialogEditEvent.setContentView(R.layout.add_edit_event);
			dialogEditEvent.setTitle("Add Class");

			//----------------------- grab all the view elements
			button_done = (Button) dialogEditEvent.findViewById(R.id.button_done);
			button_cancel_edit = (Button) dialogEditEvent.findViewById(R.id.button_cancel_edit);
			editText_end_time = (EditText) dialogEditEvent.findViewById(R.id.editText_end_time);
			editText_begin_time = (EditText) dialogEditEvent.findViewById(R.id.editText_begin_time);
			editText_location = (EditText) dialogEditEvent.findViewById(R.id.editText_location);
			autoCompleteTextView_class = (AutoCompleteTextView) dialogEditEvent.findViewById(R.id.autoCompleteTextView_class);
			checkBoxMonday = (CheckBox) dialogEditEvent.findViewById(R.id.checkBoxMonday);
			checkBoxTuesday = (CheckBox) dialogEditEvent.findViewById(R.id.checkBoxTuesday);
			checkBoxWednesday = (CheckBox) dialogEditEvent.findViewById(R.id.checkBoxWednesday);
			checkBoxThursday = (CheckBox) dialogEditEvent.findViewById(R.id.checkBoxThursday);
			checkBoxFriday = (CheckBox) dialogEditEvent.findViewById(R.id.checkBoxFriday);
			checkBoxSaturday = (CheckBox) dialogEditEvent.findViewById(R.id.checkBoxSaturday);
			checkBoxSunday = (CheckBox) dialogEditEvent.findViewById(R.id.checkBoxSunday);
			//-------------------		

			autoCompleteTextView_class.addTextChangedListener(this);
			autoCompleteTextView_class.setTextColor(Color.BLACK);
			autoCompleteTextView_class.setOnItemClickListener(new OnItemClickListener() { 

				public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
					String text = autoCompleteTextView_class.getText().toString();
					int adj_pos = getArrayIndex(item, text);
					cursor_all_classes.moveToFirst();
					cursor_all_classes.move(adj_pos);

					if(cursor_all_classes.getString(7).toString() != null && cursor_all_classes.getString(7).toString().length() != 0) {
						checkBoxMonday.setChecked(true);
					}
					else checkBoxMonday.setChecked(false);
					if(cursor_all_classes.getString(8).toString() != null && cursor_all_classes.getString(8).toString().length() != 0) {
						checkBoxTuesday.setChecked(true);
					}
					else checkBoxTuesday.setChecked(false);
					if(cursor_all_classes.getString(9).toString() != null && cursor_all_classes.getString(9).toString().length() != 0) {
						checkBoxWednesday.setChecked(true);
					}
					else checkBoxWednesday.setChecked(false);
					if(cursor_all_classes.getString(10).toString() != null && cursor_all_classes.getString(10).toString().length() != 0) {
						checkBoxThursday.setChecked(true);
					} else checkBoxThursday.setChecked(false);
					if(cursor_all_classes.getString(11).toString() != null && cursor_all_classes.getString(11).toString().length() != 0) {
						checkBoxFriday.setChecked(true);
					}else checkBoxFriday.setChecked(false);
					if(cursor_all_classes.getString(12).toString() != null && cursor_all_classes.getString(12).toString().length() != 0) {
						checkBoxSaturday.setChecked(true);
					} else checkBoxSaturday.setChecked(false);
					if(cursor_all_classes.getString(13).toString() != null && cursor_all_classes.getString(13).toString().length() != 0) {
						checkBoxSunday.setChecked(true);
					}else checkBoxSunday.setChecked(false);
					editText_location.setText(cursor_all_classes.getString(15));
					int period = autoCompleteTextView_class.getText().toString().indexOf('.');
					autoCompleteTextView_class.setText(autoCompleteTextView_class.getText().toString().substring(0, period));
					timeParse(cursor_all_classes.getString(14).toString());

				}
			});

			//Allow click to produce time picker
			editText_end_time.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					showDialog(TIME_PICK_END_DIALOG_ID);
				}
			});
			editText_begin_time.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					showDialog(TIME_PICK_BEGIN_DIALOG_ID);
				}
			});

			button_done.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					String templocation = "";
					String templabel = "";
					String tempbegin_time= "";
					String tempend_time= "";
					String tempdays= "NULL";
					int tempindex = 0;

					//checks for values					
					templocation = editText_location.getText().toString();
					templabel = autoCompleteTextView_class.getText().toString();
					tempbegin_time = editText_begin_time.getText().toString();
					tempend_time = editText_end_time.getText().toString();

					if(checkBoxMonday.isChecked()) tempdays+="MO";
					if(checkBoxTuesday.isChecked()) tempdays+="TU";
					if(checkBoxWednesday.isChecked()) tempdays+="WE";
					if(checkBoxThursday.isChecked()) tempdays+="TH";
					if(checkBoxFriday.isChecked()) tempdays+="FR";
					if(checkBoxSaturday.isChecked()) tempdays+="SA";
					if(checkBoxSunday.isChecked()) tempdays+="SU";

					tempindex = makeIndex(tempbegin_time);

					if (!tempdays.equals("NULL")) addEvent(templabel, templocation, tempindex, tempbegin_time, tempend_time, tempdays);

					removeDialog(DIALOG_ADD_EVENT);
				}
			});

			button_cancel_edit.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					//Cancel the Dialog
					removeDialog(DIALOG_ADD_EVENT);
				}
			});

			break;
		case DIALOG_CHANGE_EVENT:
			dialogEditEvent.setContentView(R.layout.add_edit_event);
			dialogEditEvent.setTitle("Edit Class");

			//----------------------- grab all the view elements
			button_done = (Button) dialogEditEvent.findViewById(R.id.button_done);
			button_cancel_edit = (Button) dialogEditEvent.findViewById(R.id.button_cancel_edit);
			editText_end_time = (EditText) dialogEditEvent.findViewById(R.id.editText_end_time);
			editText_begin_time = (EditText) dialogEditEvent.findViewById(R.id.editText_begin_time);
			editText_location = (EditText) dialogEditEvent.findViewById(R.id.editText_location);
			autoCompleteTextView_class = (AutoCompleteTextView) dialogEditEvent.findViewById(R.id.autoCompleteTextView_class);
			checkBoxMonday = (CheckBox) dialogEditEvent.findViewById(R.id.checkBoxMonday);
			checkBoxTuesday = (CheckBox) dialogEditEvent.findViewById(R.id.checkBoxTuesday);
			checkBoxWednesday = (CheckBox) dialogEditEvent.findViewById(R.id.checkBoxWednesday);
			checkBoxThursday = (CheckBox) dialogEditEvent.findViewById(R.id.checkBoxThursday);
			checkBoxFriday = (CheckBox) dialogEditEvent.findViewById(R.id.checkBoxFriday);
			checkBoxSaturday = (CheckBox) dialogEditEvent.findViewById(R.id.checkBoxSaturday);
			checkBoxSunday = (CheckBox) dialogEditEvent.findViewById(R.id.checkBoxSunday);
			//-------------------	

			//Allow click to produce time picker
			editText_end_time.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					showDialog(TIME_PICK_END_DIALOG_ID);
				}
			});
			editText_begin_time.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					showDialog(TIME_PICK_BEGIN_DIALOG_ID);
				}
			});

			//Restore dialog values based on event info
			editText_location.setText(curEvent.getLocation());
			autoCompleteTextView_class.setText(curEvent.getLabel());
			editText_begin_time.setText(curEvent.getTimeBegin());
			editText_end_time.setText(curEvent.getTimeEnd());
			if (curEvent.getDays().indexOf("MO") != -1) checkBoxMonday.setChecked(true);
			else checkBoxMonday.setChecked(false);
			if (curEvent.getDays().indexOf("TU") != -1)checkBoxTuesday.setChecked(true);
			else checkBoxTuesday.setChecked(false);
			if (curEvent.getDays().indexOf("WE") != -1)checkBoxWednesday.setChecked(true);
			else checkBoxWednesday.setChecked(false);
			if (curEvent.getDays().indexOf("TH") != -1)checkBoxThursday.setChecked(true);
			else checkBoxThursday.setChecked(false);
			if (curEvent.getDays().indexOf("FR") != -1)checkBoxFriday.setChecked(true);
			else checkBoxFriday.setChecked(false);
			if (curEvent.getDays().indexOf("SA") != -1)checkBoxSaturday.setChecked(true);
			else checkBoxSaturday.setChecked(false);
			if (curEvent.getDays().indexOf("SU") != -1)checkBoxSunday.setChecked(true);
			else checkBoxSunday.setChecked(false);

			button_done.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					String templocation = "";
					String templabel = "";
					String tempbegin_time= "";
					String tempend_time= "";
					String tempdays= "NULL";
					int tempindex = 0;
					//checks for values

					templocation = editText_location.getText().toString();
					templabel = autoCompleteTextView_class.getText().toString();
					tempbegin_time = editText_begin_time.getText().toString();
					tempend_time = editText_end_time.getText().toString();

					if(checkBoxMonday.isChecked()) tempdays+="MO";
					if(checkBoxTuesday.isChecked()) tempdays+="TU";
					if(checkBoxWednesday.isChecked()) tempdays+="WE";
					if(checkBoxThursday.isChecked()) tempdays+="TH";
					if(checkBoxFriday.isChecked()) tempdays+="FR";
					if(checkBoxSaturday.isChecked()) tempdays+="SA";
					if(checkBoxSunday.isChecked()) tempdays+="SU";

					//remove old entry then add if different
					//if only thing changed was taking away all days, dont add

					if(tempdays.equals("NULL")) {
						removeEvent(templabel);
					}
					else {
						removeEvent(curEvent.getLabel());
						Log.d("schedule", tempbegin_time);
						tempindex = makeIndex(tempbegin_time);
						addEvent(templabel, templocation, tempindex, tempbegin_time, tempend_time, tempdays);

					}
					removeDialog(DIALOG_CHANGE_EVENT);
				}
			});

			button_cancel_edit.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					//Cancel the Dialog
					removeDialog(DIALOG_CHANGE_EVENT);
				}
			});

			break;
		case TIME_PICK_END_DIALOG_ID:
		case TIME_PICK_BEGIN_DIALOG_ID:
			dialogEditEvent.setContentView(R.layout.dialog_pick_time);
			//Recycling the dialog code by using explicit if check on ID
			if(id == TIME_PICK_END_DIALOG_ID)
				dialogEditEvent.setTitle("End Time");
			else
				dialogEditEvent.setTitle("Begin Time");

			final TimePicker timePicker = (TimePicker) dialogEditEvent.findViewById(R.id.timePicker);
			timePicker.setCurrentHour(12);
			timePicker.setCurrentMinute(0);

			Button button_set = (Button) dialogEditEvent.findViewById(R.id.button_set_time);
			button_set.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					Integer hour = timePicker.getCurrentHour();
					Integer min = timePicker.getCurrentMinute();
					String time = "";
					time = timeToString(hour, min);
					if(id == TIME_PICK_END_DIALOG_ID){
						editText_end_time.setText(time);
						removeDialog(TIME_PICK_END_DIALOG_ID);
					} else {
						editText_begin_time.setText(time);
						removeDialog(TIME_PICK_BEGIN_DIALOG_ID);
					}
				}
			});
			break;
		case DIALOG_OPTION:

			dialogEditEvent.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialogEditEvent.setContentView(R.layout.dialog_destination_building);
			final TextView title = (TextView)dialogEditEvent.findViewById(R.id.textView_dialog_title);
			title.setText(curEvent.getLabel());
			final Button bViewMap = (Button)dialogEditEvent.findViewById(R.id.button_viewmap);
			final Button bGetDirections = (Button)dialogEditEvent.findViewById(R.id.button_getdirections);

			bViewMap.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					//Call intent to new activity
					String tempAddress = curEvent.getLocation();
					if(tempAddress.matches(Constants.REGEX_ROOM_NUM) || tempAddress.matches(Constants.REGEX_BLDG_NAME)) {
						if(tempAddress.matches(Constants.REGEX_ROOM_NUM)) {
							destRoomNum = tempAddress.substring(0,tempAddress.indexOf(" "));
							destBldgName = tempAddress.substring(tempAddress.indexOf(" ")).trim();
							Log.d("Schedule Item Click", "Matches REGEX_ROOM_NUM! RoomNum="+destRoomNum+" BldgName="+destBldgName);
						} 
						else { //It should just be the name of the bldg
							destBldgName = tempAddress;
							destRoomNum = "";
							Log.d("Schedule Item Click", "Matches REGEX_BLDG_NAME! RoomNum="+destRoomNum+" BldgName="+destBldgName);
						}

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
						Cursor bldgs_cursor = destination_db.getBldgIdByName(destBldgName);
						if(bldgs_cursor.getCount() > 0) {
							bldgs_cursor.moveToFirst();
							destBldgFullName = bldgs_cursor.getString(2);
						}
						destination_db.close();
					}

					Editor editor = PreferenceManager.getDefaultSharedPreferences(ReportingApplication.getAppContext()).edit();
					editor.putString("DESTNAMEFULL", destBldgFullName);
					editor.putString("DESTNAME", destBldgName);
					editor.putString("DESTROOM", destRoomNum);
					editor.commit();
					Intent intent = new Intent(ScheduleActivity.this, BuildingMapActivity.class);
					startActivity(intent);
					removeDialog(DIALOG_OPTION);
				}
			});

			bGetDirections.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					Log.d("Schedule", "got to inside click");
					String tempAddress = curEvent.getLocation();
					if(tempAddress.matches(Constants.REGEX_ROOM_NUM) || tempAddress.matches(Constants.REGEX_BLDG_NAME)) {
						if(tempAddress.matches(Constants.REGEX_ROOM_NUM)) {
							destRoomNum = tempAddress.substring(0,tempAddress.indexOf(" "));
							destBldgName = tempAddress.substring(tempAddress.indexOf(" ")).trim();
							Log.d("Schedule Item Click", "Matches REGEX_ROOM_NUM! RoomNum="+destRoomNum+" BldgName="+destBldgName);
						} 
						else { //It should just be the name of the bldg
							destBldgName = tempAddress;
							destRoomNum = "";
							Log.d("Schedule Item Click", "Matches REGEX_BLDG_NAME! RoomNum="+destRoomNum+" BldgName="+destBldgName);
						}
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
						Cursor bldgs_cursor = destination_db.getBldgIdByName(destBldgName);
						if(bldgs_cursor.getCount() > 0) {
							bldgs_cursor.moveToFirst();
							destBldgFullName = bldgs_cursor.getString(2);
						}
						destination_db.close();
					}

					Editor editor = PreferenceManager.getDefaultSharedPreferences(ReportingApplication.getAppContext()).edit();
					editor.putString("DESTNAMEFULL", destBldgFullName);
					editor.putString("DESTNAME", destBldgName);
					editor.putString("DESTROOM", destRoomNum);
					editor.commit();
					Intent searchIntent = new Intent(ScheduleActivity.this, MainMapActivity.class);
					startActivity(searchIntent);
					removeDialog(DIALOG_OPTION);
				}
			});
			break;
		default :
			break;

		}

		return dialogEditEvent;

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_schedule, menu);
		return true;
	}

	private void addEvent(String classname, String location, int index, String tempbegin_time, String tempend_time, String days){
		//adds everything from the database now need to fix database stuff


		MEvent tmp = new MEvent(classname, location, index, tempbegin_time, tempend_time, days);
		db.addEvent(tmp, "currentSchedule");

		loadEventsArray();
	}

	private void removeEvent(String classname){

		//search db for correct event and remove it
		db.deleteEvent(classname);
		loadEventsArray();

	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (cursor_all_classes != null)
			cursor_all_classes.close();
		if(all_classes_db != null)
			all_classes_db.close();
		if(db != null)
			db.close();
	}
	public void loadEventsArray(){
		ArrayList<MEvent>tmp_array = new ArrayList<MEvent>();
		tmp_array = db.getDay(CURRENTDAY);
		eventArrayAdapter.clear();
		for(int i = 0; i<tmp_array.size(); i++){
			eventArrayAdapter.add(tmp_array.get(i));
			Log.d("Addition", tmp_array.get(i).getTimeBegin());
		}

		eventArrayAdapter.notifyDataSetChanged();
	}

	public int makeIndex(String time){
		int tempi = 0;

		if (time.matches(""))return 0;

		int int1 = Integer.parseInt(time.substring(0, time.indexOf(':')));
		int int2 = Integer.parseInt(time.substring(time.indexOf(':')+1, time.length()-2));
		String str = time.substring(time.length()-2, time.length());

		if(str.matches("pm") && int1 != 12) int1+=12;
		int1*=60;
		tempi = int1+int2;

		return tempi;
	}

	private String timeToString(int hour, int min) {
		String am_pm = "";
		String time = "";
		if(hour < 12 && hour >= 0) { //It's morning
			am_pm = "am";
		} else if(hour >= 12) { //It's night
			hour -= 12;
			am_pm = "pm";
		}
		if(hour == 0) {
			hour = 12; 
		}
		time += hour+":";
		if(min < 10) {
			time+="0"+min+am_pm;
		} else
			time+=min+am_pm;
		return time;

	}

	public void afterTextChanged(Editable arg0) {

	}

	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
			int arg3) {

	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	public final int getArrayIndex(String[] myArray, String myObject) {
		int ArraySize = Array.getLength(myArray);// get the size of the array
		for (int i = 0; i < ArraySize; i++) {
			if (myArray[i].equals(myObject)) {
				return (i);
			}
		}
		return (-1);// These aren't the droids you are looking for
	}

	public void timeParse(String time) {
		if(time.equalsIgnoreCase("ARR")) return;

		int length = time.length();
		int hyphenLoc = time.indexOf('-');
		String startString = time.substring(0, hyphenLoc);
		String endString = time.substring(hyphenLoc+1, length-2);
		String AMorPM = time.substring(length-2);
		Time startTime = new Time();
		Time endTime = new Time();
		String formattedStartTime;
		String formattedEndTime;

		if(startString.length() > 2 && startString.substring(startString.length()-2).equals("30")) {
			startTime.hour = Integer.parseInt(startString.substring(0, startString.length()-2));
			startTime.minute = 30;
			formattedStartTime = startString.substring(0, startString.length()-2) + ":30";
		}
		else {
			startTime.hour = Integer.parseInt(startString);
			startTime.minute = 00;
			formattedStartTime = startString + ":00";
		}

		if(endString.length() > 2 && endString.substring(endString.length()-2).equals("30")) {
			endTime.hour = Integer.parseInt(endString.substring(0, endString.length()-2));
			endTime.minute = 00;
			formattedEndTime = endString.substring(0, endString.length()-2) + ":30";
		}
		else {
			endTime.hour = Integer.parseInt(endString);
			endTime.minute = 00;
			formattedEndTime = endString + ":00";
		}

		if(AMorPM.equalsIgnoreCase("PM")) {
			formattedEndTime = formattedEndTime.concat("pm");
			if(endTime.hour == 12 || ((endTime.hour < startTime.hour) && (startTime.hour != 12))) {
				formattedStartTime = formattedStartTime.concat("am");
			}
			else formattedStartTime = formattedStartTime.concat("pm");
		}
		else {
			formattedEndTime = formattedEndTime.concat("am");
			formattedStartTime = formattedStartTime.concat("am");
		}

		editText_begin_time.setText(formattedStartTime);
		editText_end_time.setText(formattedEndTime);

	}

	/**
	 * Fetch all class info from local database in background. Shows progressDialog when loading,
	 * then launches the add event dialog
	 *
	 */
	private class GetClassDataTask extends AsyncTask<String, String, Cursor> {

		// can use UI thread here
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog = ProgressDialog.show(ScheduleActivity.this, "Loading class data. Please wait...", "", true);
			progressDialog.show();
		}

		// automatically done on worker thread (separate from UI thread)
		@Override
		protected Cursor doInBackground(final String... args) {
			if(all_classes_db == null) {
				//Initialize classes db
				all_classes_db = new DataBaseHelper("all_classes_db");
				try {
					publishProgress("Creating database...");
					all_classes_db.createDataBase();
				} 
				catch (IOException ioe) {
					throw new Error("Unable to create database");
				}
				try {
					publishProgress("Opening database...");
					all_classes_db.openDataBase();
				} 
				catch(SQLException sqle) {	
					throw sqle;

				}
			}
			publishProgress("Querying for all classes...");
			return all_classes_db.getAllClasses();
		}

		//update progress dialog here
		@Override
		protected void onProgressUpdate(String... progress) {
			super.onProgressUpdate( progress[0]);

			Log.d("ScheduleActivity", "Progress Update: " + progress[0]);

			progressDialog.setMessage(progress[0]);
		}

		// can use UI thread here
		@Override
		protected void onPostExecute(final Cursor cursor) {

			showDialog(DIALOG_ADD_EVENT);

			cursor_all_classes = cursor;

			ArrayList<String> strings = new ArrayList<String>();
			for(cursor_all_classes.moveToFirst(); !cursor_all_classes.isAfterLast(); cursor_all_classes.moveToNext()) {
				String subject = cursor_all_classes.getString(2).trim();
				int startParen = subject.indexOf('(');
				int endParen = subject.indexOf(')');
				String department = subject.substring(startParen+1, endParen);
				String catalog_num = cursor_all_classes.getString(3).trim();
				String section = cursor_all_classes.getString(4).trim();
				strings.add(department + " " + catalog_num + "." + section);
			}

			item = (String[]) strings.toArray(new String[strings.size()]);
			adapter = new ArrayAdapter<String>(ScheduleActivity.this, android.R.layout.simple_dropdown_item_1line, item);
			autoCompleteTextView_class.setAdapter(adapter);


			if (progressDialog.isShowing()) {
				progressDialog.dismiss();
			}
		}
	}
}
