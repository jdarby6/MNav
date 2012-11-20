package com.eecs.mnav;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class Schedule extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_schedule, menu);
        return true;
    }
}
