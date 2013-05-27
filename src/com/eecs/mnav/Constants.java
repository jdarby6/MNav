package com.eecs.mnav;

import android.widget.Toast;

public class Constants {
	public static final double DEFAULT_LONG = -83.72328;
	public static final double DEFAULT_LAT = 42.27880;
	public static final int FIVE_MINUTES = 1000 * 60 * 5;
	public static final int FOUR_SECONDS = 4000;
	public static final int LAYER_TYPE_SOFTWARE = 1;
	public static final int LONG = Toast.LENGTH_LONG;
	public static final int SHORT = Toast.LENGTH_SHORT;
	public static final int ZOOM_LEVEL_DEFAULT = 14;
	public static final int ZOOM_LEVEL_BUILDING = 19;
	public static final int ZOOM_LEVEL_SKY = 17;
	public static final String REGEX_BLDG_NAME = "^[a-zA-Z][a-zA-Z &]+";
	public static final String REGEX_ROOM_NUM = "^[0-9]{1,4} [a-zA-Z]+ *";
	public static final String REGEX_ROOM_NUM_AFTER = "^[a-zA-Z][a-zA-Z&]{1,6} [0-9]{1,4}";
	
	public static final String parse_applicationId = "kTygJWFcKh5a9OK7Pv58mTZtfkS7Sp91cpVyIiwc";
	public static final String parse_clientKey = "j8fsAwMny2P7y4iLRZNY8ABhK5oF2AV3rQe2MTdO";
}