package com.eecs.mnav;

import java.util.ArrayList;

import android.os.Bundle;
import android.app.Activity;
import android.app.Dialog;
import android.view.Menu;
import android.view.View;
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
	private CheckBox checkBoxWednesDay;
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
    		break;
    	case DIALOG_CHANGE_EVENT:
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
			
			//set onKeylistener so use the enter key/dpad center to go to next field/remove keyboard
			
			button_done.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					
					//checks for values
					
					//set values and add event
					
					//Cancel the Dialog
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
			
			//set onKeylistener so use the enter key/dpad center to go to next field/remove keyboard
			
			
			//set values from values already there
			
			button_done.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					
					//checks for values
					
					//set values and add event
					
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
    	//do stuff
    }
    
	@Override
	public void onDestroy() {
		db.close();
		super.onDestroy();
	}
}
