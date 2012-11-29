package com.eecs.mnav;

import java.util.ArrayList;

import android.os.Bundle;
import android.app.Activity;
import android.app.Dialog;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class Schedule extends Activity {
	
	private MEventArrayAdapter eventArrayAdapter;
	private ArrayList<MEvent> events_array = new ArrayList<MEvent>();
	private ListView list_data;
	private MEvent curEvent;
	
	private Button button_add_event;
	
	private Button button_cancel_edit;
	private Button button_done;
	
	private EditText editText_end_time;
	private EditText editText_begin_time;
	private EditText editText_location;
	private EditText editText_class;
	
	private TextView textView_end_time;
	private TextView textView_begin_time;
	private TextView textView_location;
	private TextView textView_class;
	
	private CheckBox checkBoxMonday;
	private CheckBox checkBoxTuesday;
	private CheckBox checkBoxWednesday;
	private CheckBox checkBoxThursday;
	private CheckBox checkBoxFriday;
	private CheckBox checkBoxSaturday;
	private CheckBox checkBoxSunday;

	private static final int DIALOG_ADD_EVENT = 0;
	private static final int DIALOG_CHANGE_EVENT = 1;
	
	private ScheduleDatabaseHandler db;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
       
        
        db = new ScheduleDatabaseHandler(this);
        
        eventArrayAdapter = new MEventArrayAdapter(this, events_array);
		list_data = (ListView) findViewById(R.id.list_data);
		list_data.setAdapter(eventArrayAdapter);
		list_data.setEmptyView(findViewById(R.id.textView_empty));//format problem keep button bottom
		
		//test entry to pre load
		// MEvent tempo = new MEvent("class","location",5,5,5,"MOTUWETHFR");
	       // ArrayList<MEvent> test = new ArrayList<MEvent>();
	     //   events_array.add(tempo);
        addEvent("class","location",5,5,"MOTUWETHFR");
        
        button_add_event = (Button) findViewById(R.id.button_add_event);
		button_add_event.setOnClickListener(new Button.OnClickListener() { 
			public void onClick(View v) {
				showDialog(DIALOG_ADD_EVENT);
			}
		});
		
		//Set long-click listener to list items to display settings
		list_data.setOnItemLongClickListener(new OnItemLongClickListener() {
					public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
							int pos, long id) {
						
						curEvent = eventArrayAdapter.getItem(pos);
						showDialog(DIALOG_CHANGE_EVENT);
						return true;
					}
				});
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
    	Dialog dialog = null;
    	switch(id) {
    	case DIALOG_ADD_EVENT:
    		final Dialog dialogAddEvent = new Dialog(this);
			dialogAddEvent.setContentView(R.layout.add_edit_event);

			button_done = (Button) dialogAddEvent.findViewById(R.id.button_done);
			button_cancel_edit = (Button) dialogAddEvent.findViewById(R.id.button_cancel_edit);

			editText_end_time = (EditText) dialogAddEvent.findViewById(R.id.editText_end_time);
			editText_begin_time = (EditText) dialogAddEvent.findViewById(R.id.editText_begin_time);
			editText_location = (EditText) dialogAddEvent.findViewById(R.id.editText_location);
			editText_class = (EditText) dialogAddEvent.findViewById(R.id.editText_class);
			
			checkBoxMonday = (CheckBox) dialogAddEvent.findViewById(R.id.checkBoxMonday);
			checkBoxTuesday = (CheckBox) dialogAddEvent.findViewById(R.id.checkBoxTuesday);
			checkBoxWednesday = (CheckBox) dialogAddEvent.findViewById(R.id.checkBoxWednesday);
			checkBoxThursday = (CheckBox) dialogAddEvent.findViewById(R.id.checkBoxThursday);
			checkBoxFriday = (CheckBox) dialogAddEvent.findViewById(R.id.checkBoxFriday);
			checkBoxSaturday = (CheckBox) dialogAddEvent.findViewById(R.id.checkBoxSaturday);
			checkBoxSunday = (CheckBox) dialogAddEvent.findViewById(R.id.checkBoxSunday);
			
			//set onKeylistener so use the enter key/dpad center to go to next field/remove keyboard
			
			button_done.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					String templocation = "";
					String templabel = "";
					String tempbegin_time= "";
					String tempend_time= "";
					String tempdays= "";
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
					
					//set values and add event
					
					//check each field has values
					//do more checks
					
					if(templocation == "" || templabel == "" || finalbegin < 0 || finalend < 0 ||
							finalend < finalbegin || tempdays == "") //dialog pop up
					
					
					addEvent(templabel,templocation,finalbegin,finalend,tempdays);
					
					removeDialog(DIALOG_ADD_EVENT);
				}
			});
			
			button_cancel_edit.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					//Cancel the Dialog
					removeDialog(DIALOG_ADD_EVENT);
				}
			});
			dialog = dialogAddEvent;
    		break;
    	case DIALOG_CHANGE_EVENT:
    		//should come from long click on a list item
    		final Dialog dialogChangeEvent = new Dialog(this);
			dialogChangeEvent.setContentView(R.layout.add_edit_event);

			button_done = (Button) dialogChangeEvent.findViewById(R.id.button_done);
			button_cancel_edit = (Button) dialogChangeEvent.findViewById(R.id.button_cancel_edit);

			editText_end_time = (EditText) dialogChangeEvent.findViewById(R.id.editText_end_time);
			editText_begin_time = (EditText) dialogChangeEvent.findViewById(R.id.editText_begin_time);
			editText_location = (EditText) dialogChangeEvent.findViewById(R.id.editText_location);
			editText_class = (EditText) dialogChangeEvent.findViewById(R.id.editText_class);
			
			
			checkBoxMonday = (CheckBox) dialogChangeEvent.findViewById(R.id.checkBoxMonday);
			checkBoxTuesday = (CheckBox) dialogChangeEvent.findViewById(R.id.checkBoxTuesday);
			checkBoxWednesday = (CheckBox) dialogChangeEvent.findViewById(R.id.checkBoxWednesday);
			checkBoxThursday = (CheckBox) dialogChangeEvent.findViewById(R.id.checkBoxThursday);
			checkBoxFriday = (CheckBox) dialogChangeEvent.findViewById(R.id.checkBoxFriday);
			checkBoxSaturday = (CheckBox) dialogChangeEvent.findViewById(R.id.checkBoxSaturday);
			checkBoxSunday = (CheckBox) dialogChangeEvent.findViewById(R.id.checkBoxSunday);
			
			//set values from values already there
			//look at curEvent and set
			
			button_done.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					String templocation = "";
					String templabel = "";
					String tempbegin_time= "";
					String tempend_time= "";
					String tempdays= "";
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
					
					//set values and add event
					
					//check each field has values
					//do more checks
					
					if(templocation == "" || templabel == "" || finalbegin < 0 || finalend < 0 ||
							finalend < finalbegin || tempdays == "") //dialog pop up
					
					
					addEvent(templabel,templocation,finalbegin,finalend,tempdays);
					
					//Cancel the Dialog
					removeDialog(DIALOG_CHANGE_EVENT);
				}
			});
			
			button_cancel_edit.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					//Cancel the Dialog
					removeDialog(DIALOG_CHANGE_EVENT);
				}
			});
			dialog = dialogChangeEvent;
    		break;
    	default :
    		dialog = null;
    		break;
    	
    	}
    	
    	return dialog;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_schedule, menu);
        return true;
    }
    
    private void addEvent(String classname, String location, int begin_time, int end_time, String days){
    	//do stuff with database etc
    	int index = 0;//index not really needed
    	MEvent tmp = new MEvent(classname, location, index, begin_time, end_time, days);
		eventArrayAdapter.add(tmp);
		eventArrayAdapter.notifyDataSetChanged();
    }
    
	@Override
	public void onDestroy() {
		db.close();
		super.onDestroy();
	}
}
