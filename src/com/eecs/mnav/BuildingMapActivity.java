package com.eecs.mnav;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class BuildingMapActivity extends Activity{

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("OnCreate()", "OnCreate() called");
		setContentView(R.layout.activity_building_map);
	}
}
