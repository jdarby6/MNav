package com.eecs.mnav;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

public class MEventArrayAdapter extends ArrayAdapter<MEvent>{
	private final Context context;
	private final ArrayList<MEvent> events;
	

	public MEventArrayAdapter(Context context, ArrayList<MEvent> events) {
		super(context, R.layout.event_row, events);//define row layout in xml
		this.context = context;
		this.events = events;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
	
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView;

		rowView = inflater.inflate(R.layout.event_row, parent, false);

		//Grab the textViews from the row_layout xml file
		TextView textView_class = (TextView) rowView.findViewById(R.id.textView_class);
		TextView textView_location = (TextView) rowView.findViewById(R.id.textView_location);
		TextView textView_begin_time = (TextView) rowView.findViewById(R.id.textView_begin);
		TextView textView_end_time = (TextView) rowView.findViewById(R.id.textView_end);
		Button button_schedule_search = (Button) rowView.findViewById(R.id.button_schedule_search);
		//do same for checkboxes
		
		//Set the textViews in the row_layout xml file
		textView_class.setText(events.get(position).getLabel());
		textView_location.setText(events.get(position).getLocation());
		textView_begin_time.setText(String.valueOf(events.get(position).getTimeBegin()));
		textView_end_time.setText(String.valueOf(events.get(position).getTimeEnd()));
		//remember checkboxes
		
		
		button_schedule_search.setOnClickListener(new Button.OnClickListener() { 
			public void onClick(View v) {
				
				//start activity with location as intent
				//String address = events.get(position).getLocation();
				

				//Save destination address
				//need a way to pass it over now that original way different

				Intent searchIntent = new Intent(context, MNavMainActivity.class);
				context.startActivity(searchIntent);
			}
		});

		return rowView;
	}
	
	public int size() {
		return events.size();
	}

	public ArrayList<MEvent> getEvents() {
		return events;
	}


}
