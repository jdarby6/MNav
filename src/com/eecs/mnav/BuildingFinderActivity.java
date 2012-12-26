package com.eecs.mnav;

import java.io.IOException;
import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

public class BuildingFinderActivity extends ListActivity {
	private DataBaseHelper destination_db;
	private EditText filterText = null;
	ArrayAdapter<String> adapter = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

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
			String full_name = cursor.getString(0);
			String abbr_name = cursor.getString(1);
			strings.add(abbr_name+": " + full_name);
		}

		cursor.close();
		destination_db.close();
		String[] item = (String[]) strings.toArray(new String[strings.size()]);

		setContentView(R.layout.activity_building_finder);

		filterText = (EditText) findViewById(R.id.search_box);
		filterText.addTextChangedListener(filterTextWatcher);

		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, 
				item);
		setListAdapter(adapter);
	}

	private TextWatcher filterTextWatcher = new TextWatcher() {

		public void afterTextChanged(Editable s) {
		}

		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			adapter.getFilter().filter(s);
		}

	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		filterText.removeTextChangedListener(filterTextWatcher);
	}
	
	@Override
	protected void onListItemClick (ListView l, View v, int position, long id) {
		String tempDest = l.getItemAtPosition(position).toString();
		String destAbbr = tempDest.substring(0, tempDest.indexOf(':'));
		
		//Save destination address
		Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
		editor.putString("DESTNAME", destAbbr);
		editor.commit();
		Intent intent = new Intent(BuildingFinderActivity.this, MainMapActivity.class);
		BuildingFinderActivity.this.startActivity(intent);
	}
}