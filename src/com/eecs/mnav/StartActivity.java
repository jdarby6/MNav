package com.eecs.mnav;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

public class StartActivity extends Activity {
	//start page items;
	private Button search;
	private EditText address_box;

	public String address = "";


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);

		address_box = (EditText)findViewById(R.id.editText_address_box);
		search = (Button)findViewById(R.id.button_search);


		search.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				//do this correctly TODO
				address = address_box.getText().toString();

				//Save destination address
				Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
				editor.putString("DESTADDR", address);
				editor.commit();
				
				//hide the soft keyboard
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(address_box.getWindowToken(), 0);


				Intent searchIntent = new Intent(StartActivity.this, MNavMainActivity.class);
				StartActivity.this.startActivity(searchIntent);
			}

		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_start, menu);
		return true;
	}
}
