package com.eecs.mnav;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Toast;

public class HelperFunctions {
	public static boolean checkGPS(Activity callingActivity) {
		LocationManager mLocationManager = (LocationManager) ReportingApplication.getAppContext().getSystemService(Context.LOCATION_SERVICE);
		//Check to see if GPS is enabled
		if(!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
			//If not enabled, return false
			return false;
		}
		return true;
	}

	public static void displayEnableGPSAlert(final Activity callingActivity) {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(callingActivity);
		alertDialog.setTitle("Enable GPS?");
		alertDialog.setMessage("MNav requires GPS to use this feature. Enable?");
		alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int which) {
				Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				callingActivity.startActivity(intent);
			}
		});
		alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {		
				//callingActivity.finish();
				//Do nothing
			}
		});
		alertDialog.show();
	}
	

	/** 
	 * Helper function for displaying a toast. Takes the string to be displayed and the length: LONG or SHORT 
	 * @param toast The string that should be displayed
	 * @param duration Constants.LONG or Constants.SHORT
	 */
	public static void toastThis(String toast, int duration) {
		Toast t = Toast.makeText(ReportingApplication.getAppContext(), toast, duration);
		t.show();
	}
	
	/** 
	 * Helper function for displaying a toast. Takes the string to be displayed and the length: LONG or SHORT 
	 * @param toast The string that should be displayed
	 * @param duration Constants.LONG or Constants.SHORT
	 * @param gravity Offset from top to position the toast
	 */
	public static void toastThisGravity(String toast, int duration, int gravity) {
		Toast t = Toast.makeText(ReportingApplication.getAppContext(), toast, duration);
		t.setGravity(Gravity.TOP, 0, gravity);
		t.show();
	}

}
