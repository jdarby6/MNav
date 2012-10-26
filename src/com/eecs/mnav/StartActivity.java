package com.eecs.mnav;

import com.google.android.maps.GeoPoint;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.Color;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class StartActivity extends Activity {
	//start page items;
		private Button search;
		private EditText address_box;

		public String address = address_box.getText().toString();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        
        search = (Button)findViewById(R.id.button_search);
        address_box = (EditText)findViewById(R.id.editText_address_box);
        
        search.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
		
			}
        	
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_start, menu);
        return true;
    }
}
