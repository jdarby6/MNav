package com.eecs.mnav;


import java.util.ArrayList;
import java.util.Calendar;

import android.os.Bundle;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;

import android.util.Log;
import android.view.Menu;
import android.view.View;

import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;

import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class Schedule extends Activity {

	private MEventArrayAdapter eventArrayAdapter;

	private ArrayList<MEvent> events_array = new ArrayList<MEvent>();
	final String[] day_names = new String[] {"Every Day","Sunday", "Monday", "Tuesday", "Wednesday", 
			"Thursday", "Friday", "Saturday" };
	final String[] day_abbrs = new String[] {"NULL", "SU", "MO", "TU", "WE", 
			"TH", "FR", "SA"};
	private ListView list_data;

	private MEvent curEvent;

	private Button button_add_event;

	private Button button_cancel_edit;
	private Button button_done;	
	private EditText editText_end_time;
	private EditText editText_begin_time;
	private EditText editText_location;
	private EditText editText_class;

	private CheckBox checkBoxMonday;
	private CheckBox checkBoxTuesday;
	private CheckBox checkBoxWednesday;
	private CheckBox checkBoxThursday;
	private CheckBox checkBoxFriday;
	private CheckBox checkBoxSaturday;
	private CheckBox checkBoxSunday;

	private TextView add_edit_title;
	private TextView textView_today;

	private static final int DIALOG_ADD_EVENT = 0;
	private static final int DIALOG_CHANGE_EVENT = 1;
	private static String CURRENTDAY = "NULL";

	public static int CDI = 0; //current days int;

	private ScheduleDatabaseHandler db;

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

		db = new ScheduleDatabaseHandler(this);

		eventArrayAdapter = new MEventArrayAdapter(this, events_array);
		list_data = (ListView) findViewById(R.id.list_data);
		list_data.setAdapter(eventArrayAdapter);
		list_data.setEmptyView(findViewById(R.id.textView_empty));//format problem keep button bottom



		loadEventsArray();
		if(eventArrayAdapter.isEnabled(0)) Log.d("Schedule Items", "enabled");

		button_add_event = (Button) findViewById(R.id.button_add_event);
		button_add_event.setOnClickListener(new Button.OnClickListener() { 
			public void onClick(View v) {
				showDialog(DIALOG_ADD_EVENT);
			}
		});



		//Set long-click listener to list items
		list_data.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int pos, long id) {
				Log.d("Schedule", "got to inside long click");

				curEvent.setLabel(eventArrayAdapter.getItem(pos).getLabel());
				curEvent.setLocation(eventArrayAdapter.getItem(pos).getLocation());
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
			editText_class.setText("");
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
	protected Dialog onCreateDialog(int id){


		final Dialog dialogEditEvent = new Dialog(this);
		dialogEditEvent.setContentView(R.layout.add_edit_event);

		button_done = (Button) dialogEditEvent.findViewById(R.id.button_done);
		button_cancel_edit = (Button) dialogEditEvent.findViewById(R.id.button_cancel_edit);

		editText_end_time = (EditText) dialogEditEvent.findViewById(R.id.editText_end_time);
		editText_begin_time = (EditText) dialogEditEvent.findViewById(R.id.editText_begin_time);
		editText_location = (EditText) dialogEditEvent.findViewById(R.id.editText_location);
		editText_class = (EditText) dialogEditEvent.findViewById(R.id.editText_class);

		checkBoxMonday = (CheckBox) dialogEditEvent.findViewById(R.id.checkBoxMonday);
		checkBoxTuesday = (CheckBox) dialogEditEvent.findViewById(R.id.checkBoxTuesday);
		checkBoxWednesday = (CheckBox) dialogEditEvent.findViewById(R.id.checkBoxWednesday);
		checkBoxThursday = (CheckBox) dialogEditEvent.findViewById(R.id.checkBoxThursday);
		checkBoxFriday = (CheckBox) dialogEditEvent.findViewById(R.id.checkBoxFriday);
		checkBoxSaturday = (CheckBox) dialogEditEvent.findViewById(R.id.checkBoxSaturday);
		checkBoxSunday = (CheckBox) dialogEditEvent.findViewById(R.id.checkBoxSunday);

		//add_edit_title = (TextView) dialogEditEvent.findViewById(R.id.textView_add_event);

		switch(id) {
		case DIALOG_ADD_EVENT:

			//add_edit_title.setText("Add Event");

			//set onKeylistener so use the enter key/dpad center to go to next field/remove keyboard

			button_done.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					String templocation = "";
					String templabel = "";
					String tempbegin_time= "";
					String tempend_time= "";
					String tempdays= "NULL";
					int finalbegin = 0;
					int finalend = 0;
					//checks for values

					templocation = editText_location.getText().toString();
					templabel = editText_class.getText().toString();
					tempbegin_time = editText_begin_time.getText().toString();
					tempend_time = editText_end_time.getText().toString();

					if(checkBoxMonday.isChecked()) tempdays+="MO";
					if(checkBoxTuesday.isChecked()) tempdays+="TU";
					if(checkBoxWednesday.isChecked()) tempdays+="WE";
					if(checkBoxThursday.isChecked()) tempdays+="TH";
					if(checkBoxFriday.isChecked()) tempdays+="FR";
					if(checkBoxSaturday.isChecked()) tempdays+="SA";
					if(checkBoxSunday.isChecked()) tempdays+="SU";

					if (!tempdays.equals("NULL")) addEvent(templabel,templocation,finalbegin,finalend,tempdays);

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
			//should come from long click on a list item

			//add_edit_title.setText("Edit Event");

			//editText_end_time.setText(curEvent.getTimeEnd());
			// editText_begin_time.setText(curEvent.getTimeEnd());
			editText_location.setText(curEvent.getLocation());
			editText_class.setText(curEvent.getLabel());

			if (curEvent.getDays().indexOf("MO") != -1) checkBoxMonday.setChecked(true);
			if (curEvent.getDays().indexOf("TU") != -1)checkBoxTuesday.setChecked(true);
			if (curEvent.getDays().indexOf("WE") != -1)checkBoxWednesday.setChecked(true);
			if (curEvent.getDays().indexOf("TH") != -1)checkBoxThursday.setChecked(true);
			if (curEvent.getDays().indexOf("FR") != -1)checkBoxFriday.setChecked(true);
			if (curEvent.getDays().indexOf("SA") != -1)checkBoxSaturday.setChecked(true);
			if (curEvent.getDays().indexOf("SU") != -1)checkBoxSunday.setChecked(true);


			button_done.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					String templocation = "";
					String templabel = "";
					String tempbegin_time= "";
					String tempend_time= "";
					String tempdays= "NULL";
					int finalbegin = 0;
					int finalend = 0;
					//checks for values

					templocation = editText_location.getText().toString();
					templabel = editText_class.getText().toString();
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

					if(tempdays.equals( "NULL") && templabel.equals(curEvent.getLabel())
							&& templocation.equals(curEvent.getLocation())
							&& finalbegin == curEvent.getTimeBegin()
							&& finalend == curEvent.getTimeEnd()){
						removeEvent(templabel);
					}

					else{
						removeEvent(templabel);
						addEvent(templabel,templocation,finalbegin,finalend,tempdays);

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

	private void addEvent(String classname, String location, int begin_time, int end_time, String days){
		//adds everything from the database now need to fix database stuff

		int index = 0;//index not really needed
		MEvent tmp = new MEvent(classname, location, index, begin_time, end_time, days);
		db.addEvent(tmp, "currentSchedule", index);

		loadEventsArray();
	}

	private void removeEvent(String classname){

		//search db for correct event and remove it
		db.deleteEvent(classname);
		loadEventsArray();

	}

	@Override
	public void onDestroy() {
		db.close();
		super.onDestroy();
	}
	public void loadEventsArray(){
		ArrayList<MEvent>tmp_array = new ArrayList<MEvent>();
		tmp_array = db.getDay(CURRENTDAY);
		eventArrayAdapter.clear();
		for(int i = 0; i<tmp_array.size(); i++){

			eventArrayAdapter.add(tmp_array.get(i));
		}

		eventArrayAdapter.notifyDataSetChanged();
	}

}
