package com.eecs.mnav;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;

import android.widget.TextView;

public class MEventArrayAdapter extends ArrayAdapter<MEvent>{
	private final Context context;
	private final ArrayList<MEvent> events;
	private String destBldgName = "";
	private String destRoomNum = "";
	
	final String[] day_abbrs = new String[] {"NULL", "SU", "MO", "TU", "WE", 
			"TH", "FR", "SA"};
	private static final String REGEX_ROOM_NUM = "^[0-9]{1,4} [a-zA-Z]+ *";
	private static final String REGEX_BLDG_NAME = "^[a-zA-Z][a-zA-Z &]+";
	

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
		//do same for checkboxes
		
		//Set the textViews in the row_layout xml file
		textView_class.setText(events.get(position).getLabel());
		textView_location.setText(events.get(position).getLocation());
		textView_begin_time.setText(String.valueOf(events.get(position).getTimeBegin()));
		textView_end_time.setText(String.valueOf(events.get(position).getTimeEnd()));
		//remember checkboxes
		 rowView.setOnClickListener( new View.OnClickListener()
	        {
	                public void onClick(View v)
	                {
	                	Log.d("Schedule", "got to inside click");
	                	String tempAddress = events.get(position).getLocation();
	                	if(tempAddress.matches(REGEX_ROOM_NUM) || tempAddress.matches(REGEX_BLDG_NAME)) {
	    					if(tempAddress.matches(REGEX_ROOM_NUM)) {
	    						destRoomNum = tempAddress.substring(0,tempAddress.indexOf(" "));
	    						destBldgName = tempAddress.substring(tempAddress.indexOf(" ")).trim();
	    						Log.d("Schedule Item Click", "Matches REGEX_ROOM_NUM! RoomNum="+destRoomNum+" BldgName="+destBldgName);
	    					} else {//It should just be the name of the bldg
	    						destBldgName = tempAddress;
	    						destRoomNum = "";
	    						Log.d("Schedule Item Click", "Matches REGEX_BLDG_NAME! RoomNum="+destRoomNum+" BldgName="+destBldgName);
	    					}
	    				}
	    				
	    				Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
	    				editor.putString("DESTNAME", destBldgName);
	    				editor.putString("DESTROOM", destRoomNum);
	    				editor.commit();
	    				Intent searchIntent = new Intent(context, MNavMainActivity.class);
	    				context.startActivity(searchIntent);
	                }});

		return rowView;
	}
	
	public int size() {
		return events.size();
	}

	public ArrayList<MEvent> getEvents() {
		return events;
	}


}
