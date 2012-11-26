package com.eecs.mnav;

import java.util.ArrayList;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

public class MEventArrayAdapter extends ArrayAdapter<MEvent>{
	private final Context context;
	private final ArrayList<MEvent> events;

	public MEventArrayAdapter(Context context, ArrayList<MEvent> events) {
		super(context, R.layout.activity_schedule, events);//define row layout in xml
		this.context = context;
		this.events = events;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
	

		return convertView;//just to satisfy warning for now
	}


}
