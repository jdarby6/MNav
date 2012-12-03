package com.eecs.mnav;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.parse.Parse;
import com.parse.ParseObject;

public class MNavMainActivity extends MapActivity {
	//Layout globals
	private MapView gMapView = null;
	private Button bPlotRoute;
	private Button bSatellite;
	private Button bTargetReticle;
	private Button bZoomIn;
	private Button bZoomOut;

	// Intro tips booleans
	private boolean hasSeenAlert1 = false;
	private boolean hasSeenAlert2 = false;
	private boolean hasSeenAlert3 = false;
	private boolean hasSeenAlert4 = false;

	//Our location globals
	private float gBearing = 0;
	private float gSpeed = 0;
	private double gCurrentLong = 0.0;
	private double gCurrentLat = 0.0;
	private Location gBestLocation = null;

	//Destination globals
	private EditText editTextDestination;
	private String gDestName = "the diag";
	private String gDestNum = "";
	private String gDestName_full ="";
	private String gDistanceToDest;
	private String gTimeToDest;
	private double gDestinationLong = 0.0;
	private double gDestinationLat = 0.0;

	//Helper globals
	private LocalDatabaseHandler local_db;
	private DataBaseHelper destination_db;
	private MapController gMapController = null;
	private LocationManager gLocationManager;
	private SharedPreferences gPreferences = null;

	//Overlay globals
	private PinOverlay gPinOverlay = null;
	private RouteOverlay gRouteOverlay = null;
	private MyLocationOverlay gMyLocationOverlay = null;
	private ScaleBarOverlay gScaleBarOverlay = null;

	//Constants
	private static final int LONG = Toast.LENGTH_LONG;
	private static final int SHORT = Toast.LENGTH_SHORT;
	private static final int FIVE_MINUTES = 1000 * 60 * 5;
	private static final int LAYER_TYPE_SOFTWARE = 1;
	private static final int ZOOM_LEVEL_SKY = 17;
	private static final int ZOOM_LEVEL_CAMPUS = 18;
	private static final int ZOOM_LEVEL_BUILDING = 19;
	//Alert IDs
	private static final int ALERT_INVALID_DEST = 0;
	private static final int ALERT_INTRO_PROMPT_1 = 1;
	private static final int ALERT_INTRO_PROMPT_2 = 2;
	private static final int ALERT_INTRO_PROMPT_3 = 3;
	private static final int ALERT_INTRO_PROMPT_4 = 4;
	//Dialog IDs
	private static final int DIALOG_SAVE_CURRENT_LOC = 0;
	public final static int DIALOG_DESTINATION_BLDG = 1;
	private final static int DIALOG_SETTINGS = 2;

	private class Coords {
		double latitude;
		double longitude;
	}
	private Coords doors[];
	int num_doors;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("OnCreate()", "OnCreate() called");
		setContentView(R.layout.activity_main);
		//Initialize local db
		local_db = new LocalDatabaseHandler(this);

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

		//Initialize Parse
		Parse.initialize(this, "kTygJWFcKh5a9OK7Pv58mTZtfkS7Sp91cpVyIiwc", "j8fsAwMny2P7y4iLRZNY8ABhK5oF2AV3rQe2MTdO");

		//Load stored data
		gPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		//Load last known latitude, longitude default is the Diag
		gCurrentLat = Double.parseDouble(gPreferences.getString("LASTLAT", "42.276956"));
		gCurrentLong = Double.parseDouble(gPreferences.getString("LASTLONG", "-83.738234"));
		//Load destination address, default is the Diag
		gDestName = gPreferences.getString("DESTNAME", "the diag");
		//DestName should be free of numbers and excess space now,  but just in case we'll keep the next line
		gDestName = gDestName.replaceAll("[0-9]", "").trim();
		gDestNum = gPreferences.getString("DESTNUM", "");

		hasSeenAlert1 = gPreferences.getBoolean("A1", false);
		hasSeenAlert2 = gPreferences.getBoolean("A2", false);
		hasSeenAlert3 = gPreferences.getBoolean("A3", false);
		hasSeenAlert4 = gPreferences.getBoolean("A4", false);



		/** DATABASE STUFF **/

		//Grab a cursor
		Cursor cursor = destination_db.getBldgIdByName(gDestName);
		if(cursor.getCount() > 0 && cursor.moveToFirst()) { //Destination is there, so show dialog and grab info
			buildAlertDialog(ALERT_INTRO_PROMPT_1);
			//find column containing bldg num
			int bldg_num_col = cursor.getColumnIndex("bldg_num");
			int bldg_num = cursor.getInt(bldg_num_col);
			//find column containing num_doors
			int num_doors_col = cursor.getColumnIndex("num_doors");
			num_doors = cursor.getInt(num_doors_col);
			//doors is an array of coords with size "number of doors"
			doors = new Coords[num_doors];
			//create the Coords within doors to be populated later
			for(int i = 0; i < num_doors; i++) {
				doors[i] = new Coords();
			}
			//find column containing full names 
			try {
				int bldg_name_full_col = cursor.getColumnIndexOrThrow("name_full");
				gDestName_full = cursor.getString(bldg_name_full_col);
				toastThis(gDestName_full, LONG);
			} catch (IllegalArgumentException e) {
				Log.d("DATABASE SHIT", e.toString());
			}

			cursor.close();
			//grab a new cursor to get the lat/long of each door to put in doors[]
			cursor = destination_db.getDoorsByBldgId(bldg_num);
			if(cursor.moveToFirst()) {
				int door_lat_col = cursor.getColumnIndex("door_lat");
				int door_long_col = cursor.getColumnIndex("door_long");
				for(int i = 0; i < num_doors; i++) {
					doors[i].latitude = cursor.getDouble(door_lat_col);
					doors[i].longitude = cursor.getDouble(door_long_col);
					cursor.moveToNext();
				}
			}
		} else {
			num_doors = -1;
			buildAlertDialog(ALERT_INVALID_DEST);
		}

		Log.d("LOADED DATA", "Coords: "+String.valueOf(gCurrentLat)+","+String.valueOf(gCurrentLong)+
				" destAddr: "+gDestName+" roomNum: "+gDestNum);

		//Put last known info as current location
		Location location = new Location(LocationManager.GPS_PROVIDER);
		location.setLatitude(gCurrentLat);
		location.setLongitude(gCurrentLong);
		location.setTime(Long.parseLong(gPreferences.getString("LASTLOCTIME", "0")));
		gBestLocation = location;

		//Grab the mapView
		gMapView = (MapView)findViewById(R.id.mapview);
		try {
			Method setLayerTypeMethod = gMapView.getClass().getMethod("setLayerType", new Class[] {int.class, Paint.class});
			setLayerTypeMethod.invoke(gMapView, new Object[] {LAYER_TYPE_SOFTWARE, null});
		} catch (NoSuchMethodException e) {
			// Older OS, no HW acceleration anyway
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		//THIS IS WHERE WE PUT THE CLOSEST SEARCH RESULT INTO THE SEARCH BAR
		if(num_doors != -1) {
			int closestDoorIndex = getClosestDoorIndex();
			gDestinationLat = doors[closestDoorIndex].latitude;
			gDestinationLong = doors[closestDoorIndex].longitude;
		}

		editTextDestination = (EditText)findViewById(R.id.editText_map_destination);
		editTextDestination.setText(gDestName);

		bPlotRoute = (Button) findViewById(R.id.button_plotroute);
		bPlotRoute.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.d("GetRouteClicked", "Calculating Route");

				//Grab the user input lat/long
				//	String temp = editTextDestination.getText().toString();

				//	if(temp != null && temp.length() > 0) {
				//create a geopoint for dest
				GeoPoint dest = getDirections(); //Query Google for directions to dest and return the dest 

				zoomTo(dest, ZOOM_LEVEL_BUILDING);
			}

			//	}
		});


		bSatellite = (Button) findViewById(R.id.button_satellite);
		if(!gMapView.isSatellite())
			bSatellite.setBackgroundResource(R.drawable.ic_road);
		else
			bSatellite.setBackgroundResource(R.drawable.ic_satellite);

		bSatellite.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(gMapView.isSatellite()){
					bSatellite.setBackgroundResource(R.drawable.ic_road);
					gMapView.setSatellite(false);
				}else{
					gMapView.setSatellite(true);
					bSatellite.setBackgroundResource(R.drawable.ic_satellite);
				}

				Log.d("MAPSTUFF", "ZoomLevel="+gMapView.getZoomLevel());
			}
		});

		bTargetReticle = (Button) findViewById(R.id.button_return);
		bTargetReticle.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startGPS();
				//		GeoPoint currentLoc = new GeoPoint((int)(gCurrentLat * 1e6), (int)(gCurrentLong * 1e6));
				GeoPoint currentLoc = gMyLocationOverlay.getMyLocation();
				zoomTo(currentLoc, ZOOM_LEVEL_BUILDING);
				buildAlertDialog(ALERT_INTRO_PROMPT_3);
			}
		});
		bTargetReticle.setOnLongClickListener(new OnLongClickListener() {
			public boolean onLongClick(View v) {
				//create a geopoint for dest
				GeoPoint dest = new GeoPoint((int)(gDestinationLat * 1e6), (int)(gDestinationLong * 1e6));
				zoomTo(dest, ZOOM_LEVEL_BUILDING);
				buildAlertDialog(ALERT_INTRO_PROMPT_4);
				return true;
			}
		});

		bZoomIn = (Button) findViewById(R.id.button_zoomin);
		bZoomIn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(gMapController == null)
					gMapController = gMapView.getController();
				gMapController.zoomIn();
			}
		});

		bZoomOut = (Button) findViewById(R.id.button_zoomout);
		bZoomOut.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(gMapController == null)
					gMapController = gMapView.getController();
				gMapController.zoomOut();
			}
		});

		startGPS();
	}

	@Override
	protected void onResume() {
		super.onResume();
		//Initialize the map overlays (scale, currentLocation indicator, and destination bldg if applicable)
		//Also zooms to destination building pin or current location depending on if intro has been seen
		initOverlays();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// when our activity pauses, we want to remove listening for location updates
		gMyLocationOverlay.disableMyLocation();

		Editor editor = gPreferences.edit();
		editor.putString("DESTNAMEFULL", gDestName_full);
		editor.putString("DESTNAME", gDestName);
		editor.commit();
		Log.d("MNavMainActivity", "gDestName_full:"+gDestName_full+" gDestName:"+gDestName);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d("OnDestroy()", "OnDestroy() called");
		//Save stored data
		Editor editor = gPreferences.edit();
		//Save last known latitude
		editor.putString("LASTLAT", String.valueOf(gCurrentLat));
		editor.putString("LASTLONG", String.valueOf(gCurrentLong));
		editor.putString("LASTLOCTIME", String.valueOf(gBestLocation.getTime()));


		editor.putBoolean("A1", hasSeenAlert1);
		editor.putBoolean("A2", hasSeenAlert2);
		editor.putBoolean("A3", hasSeenAlert3);
		editor.putBoolean("A4", hasSeenAlert4);

		editor.commit();
		Log.d("SAVED DATA", "Coords: "+String.valueOf(gCurrentLat)+","+String.valueOf(gCurrentLong));	
		//Turn off GPS
		if(gLocationManager != null)
			gLocationManager.removeUpdates(locationListener);
		destination_db.close();
	}	


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public boolean onOptionsItemSelected (MenuItem item) {
		switch (item.getItemId()){
		case R.id.menu_satellite:
			showDialog(DIALOG_SAVE_CURRENT_LOC);
			break;
		case R.id.menu_settings:
			showDialog(DIALOG_SETTINGS);
			break;
		default:
			return false;
		}
		return true;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		final Dialog dialogTemp = new Dialog(this);
		switch(id) {
		case DIALOG_SAVE_CURRENT_LOC:
			dialogTemp.setContentView(R.layout.dialog_save_current_loc);
			dialogTemp.setTitle("Save Current Location");

			final EditText editText_BldgAbbr = (EditText)dialogTemp.findViewById(R.id.editText_BldgAbbr);
			final EditText editText_DoorNick = (EditText)dialogTemp.findViewById(R.id.editText_DoorNick);
			final TextView textView_CurrentLat = (TextView)dialogTemp.findViewById(R.id.textView_CurrentLat);
			final TextView textView_CurrentLong = (TextView)dialogTemp.findViewById(R.id.textView_CurrentLong);
			final Button button_Save = (Button)dialogTemp.findViewById(R.id.button_Save);
			final Button button_Cancel = (Button)dialogTemp.findViewById(R.id.button_Cancel);

			textView_CurrentLat.setText("Current lat: " + gCurrentLat);
			textView_CurrentLong.setText("Current long: " + gCurrentLong);

			button_Save.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					String bldgAbbr = editText_BldgAbbr.getText().toString();
					String doorNick = editText_DoorNick.getText().toString();
					if((bldgAbbr == null || bldgAbbr.length() == 0) || (doorNick == null || doorNick.length() == 0)) {
						AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MNavMainActivity.this);

						// set title
						alertDialogBuilder.setTitle("Error!");

						// set dialog message
						alertDialogBuilder
						.setMessage("You have to fill out values for building name and door nickname")
						.setCancelable(false)
						.setNegativeButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,int id) {
								// if this button is clicked, just close
								// the dialog box and do nothing
								dialog.cancel();
							}
						});

						// create alert dialog
						AlertDialog alertDialog = alertDialogBuilder.create();

						// show it
						alertDialog.show();
					}
					else {
						local_db.addRow(bldgAbbr, doorNick, String.valueOf(gCurrentLat), String.valueOf(gCurrentLong));
						removeDialog(DIALOG_SAVE_CURRENT_LOC);
					}
				}
			});

			button_Cancel.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					removeDialog(DIALOG_SAVE_CURRENT_LOC);
				}
			});
			break;

		case DIALOG_DESTINATION_BLDG:
			dialogTemp.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialogTemp.setContentView(R.layout.dialog_destination_building);
			final TextView title = (TextView)dialogTemp.findViewById(R.id.textView_dialog_title);
			title.setText(gDestName_full);
			final Button bViewMap = (Button)dialogTemp.findViewById(R.id.button_viewmap);
			final Button bGetDirections = (Button)dialogTemp.findViewById(R.id.button_getdirections);

			bViewMap.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					//Call intent to new activity
					Intent intent = new Intent(MNavMainActivity.this, BuildingMapActivity.class);
					startActivity(intent);
					removeDialog(DIALOG_DESTINATION_BLDG);
				}
			});

			bGetDirections.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					getDirections();
					removeDialog(DIALOG_DESTINATION_BLDG);
				}
			});
			break;

		case DIALOG_SETTINGS:
			dialogTemp.setContentView(R.layout.dialog_settings);
			dialogTemp.setTitle("Settings");

			final CheckBox resetTips = (CheckBox) dialogTemp.findViewById(R.id.checkBox_restore_tips);
			final Button bDone = (Button) dialogTemp.findViewById(R.id.button_done);
			bDone.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					if(resetTips.isChecked()){
						hasSeenAlert1 = false;
						hasSeenAlert2 = false;
						hasSeenAlert3 = false;
						hasSeenAlert4 = false;
						toastThis("Intro tips reset", LONG);
					}
					removeDialog(DIALOG_SETTINGS);
				}
			});
			break;
		}
		dialog = dialogTemp;
		return dialog;
	}

	@Override
	protected boolean isRouteDisplayed() {
		return true;
	}


	LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) { // Called when a new location is found by the network location provider.
			//		Log.d("LocationChanged", "Found You: "+location.getLatitude()+","+location.getLongitude());
			//Check to see if the new location is better than our best location so far
			if(isBetterLocation(location, gBestLocation))
				gBestLocation = location;
			gBearing = gBestLocation.getBearing();
			gCurrentLat = gBestLocation.getLatitude();
			gCurrentLong = gBestLocation.getLongitude();
			gSpeed = gBestLocation.getSpeed();
			/*	String toast = "Speed: " + gSpeed + "m/s \nBearing: " + gBearing + " degrees E of N \nLong: "
					+ gCurrentLong + " \nLat: " + gCurrentLat;
			toastThis(toast, SHORT); */

			//If it's our first found location, initialize overlays.
			/*	if(!overlaysInitialized)
				initOverlays(gBestLocation);
			else */
			//		updateUserPosition(gBestLocation);
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {}

		public void onProviderEnabled(String provider) {}

		public void onProviderDisabled(String provider) {}
	};
	private ProgressDialog gProgressDialog;
	private GeoPoint gStartGeo;
	private GeoPoint gDestGeo;

	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}
		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > FIVE_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -FIVE_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			Log.d("isBetter", "true: is significantly newer");
			return true;


			// If the new location is more than two minutes older, it must be worse
		} else
			if (isSignificantlyOlder) {
				Log.d("isBetter", "false: is significantly older");
				return false;
			}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 5;
		boolean isMoreAccurate = accuracyDelta < -5;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and accuracy
		if (isMoreAccurate) {
			Log.d("isBetter", "true: is more accurate");
			return true;
		} else if (isNewer && !isLessAccurate) {
			//		Log.d("isBetter", "true: is newer, not less accurate");
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
			Log.d("isBetter", "true: is newer, !significantlyLessAccurate, fromSameProvider");
			return true;
		}

		Log.d("isBetter", "false: default catch");
		return false;
	}

	/** Checks whether two providers are the same. Called in isBetterLocation()*/
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	/**Gets best cached location from GPS and NETWORK. Called before searching for location */
	private Location getCachedLocation() {
		//Get cached location from GPS
		Location cachedGpsLoc = gLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		//Get cached location from Network
		Location cachedNetworkLoc = gLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

		//If one or both of these are null, just return the other without calling isBetterLocation to avoid time and
		//null pointer exceptions
		if(cachedGpsLoc == null) return cachedNetworkLoc;
		if(cachedNetworkLoc == null) return cachedGpsLoc;

		//If it hasn't returned by now, check which one is actually better and return it.
		if(isBetterLocation(cachedGpsLoc, cachedNetworkLoc))
			return cachedGpsLoc;
		else return cachedNetworkLoc;
	}

	private void displayEnableGPSAlert() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(getApplicationContext());
		alertDialog.setTitle("Enable GPS?");
		alertDialog.setMessage("GPS is not enabled. Would you like to enable it in settings?");
		alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int which) {
				Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				startActivity(intent);
			}
		});
		alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		alertDialog.show();
	}

	public void initOverlays() {
		//Remove all existing overlays
		List<Overlay> mapOverlays = gMapView.getOverlays();
		mapOverlays.clear();

		Drawable drawable = this.getResources().getDrawable(R.drawable.ic_pin);
		//Create our route overlay
		gPinOverlay = new PinOverlay(drawable, this);
		gPinOverlay.setTapListener(this);
		/**
		 * OverlayItem overlayitem = new OverlayItem(p, "Current Location", "You are here!");
		 * gRouteOverlay.addOverlay(overlayitem);
		 * mapOverlays.add(gRouteOverlay);
		 *
		 **/

		gMyLocationOverlay = new MyLocationOverlay(this, gMapView);
		mapOverlays.add(gMyLocationOverlay);

		gMyLocationOverlay.enableMyLocation();


		//Create the scalebar and add it to mapview
		gScaleBarOverlay = new ScaleBarOverlay(this.getBaseContext(), gMapView);
		gScaleBarOverlay.setImperial();
		mapOverlays.add(gScaleBarOverlay);

		if(hasSeenAlert1){
			//create a geopoint for dest
			GeoPoint dest = new GeoPoint((int)(gDestinationLat * 1e6), (int)(gDestinationLong * 1e6));

			OverlayItem overlayitem = new OverlayItem(dest, "gDestName_full", "This is your current destination");
			gPinOverlay.addOverlay(overlayitem);
			gMapView.getOverlays().add(gPinOverlay); 

			zoomTo(dest, ZOOM_LEVEL_BUILDING);
		} else {
			gProgressDialog = new ProgressDialog(this);
			gProgressDialog.setMessage("Finding you...");
			gProgressDialog.setCancelable(false);
			gProgressDialog.setIndeterminate(true);
			gProgressDialog.show();

			gMyLocationOverlay.runOnFirstFix(new Runnable() {
				public void run() {
					gProgressDialog.dismiss();
					GeoPoint p = gMyLocationOverlay.getMyLocation();
					if(gMapController == null)
						gMapController = gMapView.getController();
					gMapController.animateTo(p);
					gMapController.setZoom(ZOOM_LEVEL_BUILDING);
				}
			});
		}
	}

	private Route directions(final GeoPoint start, final GeoPoint dest) {
		GoogleParser googleParser;
		String jsonURL = "http://maps.google.com/maps/api/directions/json?";
		final StringBuffer sBuf = new StringBuffer(jsonURL);
		sBuf.append("origin=");
		sBuf.append(start.getLatitudeE6()/1E6);
		sBuf.append(',');
		sBuf.append(start.getLongitudeE6()/1E6);
		sBuf.append("&destination=");
		sBuf.append(dest.getLatitudeE6()/1E6);
		sBuf.append(',');
		sBuf.append(dest.getLongitudeE6()/1E6);
		sBuf.append("&sensor=true&mode=walking");
		//sBuf.append("&sensor=true&mode=transit&departure_time="+(System.currentTimeMillis()+FIVE_MINUTES));
		googleParser = new GoogleParser(sBuf.toString());
		Route r =  googleParser.parse();
		return r;
	}

	private void zoomTo(GeoPoint p, int level) {
		if(gMapController == null)
			gMapController = gMapView.getController();
		gMapController.stopPanning();
		gMapController.animateTo(p);
		int zoomLevel = gMapView.getZoomLevel();
		try{
			if(zoomLevel < level) {
				for(int i = zoomLevel; i < level; i++)
					gMapController.zoomIn();
			} else {
				for(int i = zoomLevel; i > level; i--) {
					gMapController.zoomOut();
				}
			}
		} catch (IllegalArgumentException e) {
			Log.d("zoomTo()", e.toString());
			gMapController.setZoom(level);
		}
	}

	private void startGPS() {
		gLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		//Check to see if GPS is enabled
		if(!gLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
			//If not enabled, prompt user to enabled it
			displayEnableGPSAlert();
		}

		//Start looking for location information

		//Check the cached location, see if it's better than the best one we are using now
		Location cachedLoc = getCachedLocation();
		if(!isBetterLocation(gBestLocation, cachedLoc))
			//Since it's better, use it then start querying gps sources
			gBestLocation = cachedLoc;
		gLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
		gLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
	}

	/** Helper function for displaying a toast. Takes the string to be displayed and the length: LONG or SHORT **/
	private void toastThis(String toast, int duration) {
		Context context = getApplicationContext();
		Toast t = Toast.makeText(context, toast, duration);
		t.show();
	}

	private void buildAlertDialog(int ID) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MNavMainActivity.this);
		switch(ID){
		case ALERT_INVALID_DEST:
			//set title message
			alertDialogBuilder.setTitle("Error: Destination Not Found");
			// set dialog message
			alertDialogBuilder.setMessage("We couldn't recognize the destination you entered. Make sure everything is spelled correctly"
					+ " and try again.")
					.setCancelable(false)
					.setNegativeButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,int id) {
							// if this button is clicked, just close
							// the dialog box and do nothing

							MNavMainActivity.this.finish();
							dialog.cancel();
						}
					});
			break;
		case ALERT_INTRO_PROMPT_1:
			if(hasSeenAlert1)
				return;
			// set dialog message
			alertDialogBuilder.setTitle("Destination Found!");
			alertDialogBuilder.setMessage("The map is now centered at your location. " 
					+ "To get walking directions, press the pedestrian icon above.")
					.setCancelable(true)
					.setNegativeButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,int id) {
							// if this button is clicked, just close
							// the dialog box and do nothing
							dialog.cancel();
						}
					});
			break;
		case ALERT_INTRO_PROMPT_2:
			if(hasSeenAlert2)
				return;
			// set dialog message
			alertDialogBuilder.setTitle("Note:");
			alertDialogBuilder.setMessage("You can press the targeting icon in the top right corner to return to" +
					" your current location or tap on the building pin for more options.")
					.setCancelable(true)
					.setNegativeButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,int id) {
							// if this button is clicked, just close
							// the dialog box and do nothing
							dialog.cancel();
						}
					});
			hasSeenAlert2 = true;
			break;
		case ALERT_INTRO_PROMPT_3:
			if(hasSeenAlert3)
				return;
			alertDialogBuilder.setTitle("Note:");
			alertDialogBuilder.setMessage("You can long-press the targeting icon to return to your destination building pin.")
			.setCancelable(true)
			.setNegativeButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,int id) {
					// if this button is clicked, just close
					// the dialog box and do nothing
					dialog.cancel();
				}
			});
			hasSeenAlert3 = true;
			break;		
		case ALERT_INTRO_PROMPT_4:
			if(hasSeenAlert4)
				return;
			alertDialogBuilder.setTitle("Note:");
			alertDialogBuilder.setMessage("To find a new building, simply type it in the search bar above and press the pedestrian icon.")
			.setCancelable(true)
			.setNegativeButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,int id) {
					// if this button is clicked, just close
					// the dialog box and do nothing
					dialog.cancel();
				}
			});
			hasSeenAlert4 = true;
			break;
		}
		// create and show it
		alertDialogBuilder.create().show();
	}

	private double getDistanceFromCurrentLoc(double latitude, double longitude) {
		double lat_dist = latitude - gCurrentLat;
		double long_dist = longitude - gCurrentLong;

		return Math.sqrt(lat_dist*lat_dist + long_dist*long_dist);
	}

	private int getClosestDoorIndex() {
		int currentBestIndex = 0;
		double currentBestDistance = 0;

		for(int i = 0; i < num_doors; i++) {
			double contenderDistance = getDistanceFromCurrentLoc(doors[i].latitude, doors[i].longitude);
			if(contenderDistance < currentBestDistance)
				currentBestIndex = i;
		}

		return currentBestIndex;
	}

	// Implementation of AsyncTask used to get walking directions from current location to destination
	private class GetDirectionsTask extends AsyncTask<GeoPoint, Void, Route> {

		@Override
		protected Route doInBackground(GeoPoint... geopoints) {
			//Creates Url and queries google directions api
			return directions(geopoints[0], geopoints[1]);
		}

		@Override
		protected void onPostExecute(Route route) {
			List<Overlay> tmp = gMapView.getOverlays();
			//Remove the route if there's one there already
			if(tmp.contains(gRouteOverlay)) {
				tmp.remove(gRouteOverlay);
			}
			gRouteOverlay = new RouteOverlay(route, gStartGeo, gDestGeo, getResources().getColor(R.color.fireBrickRed));
			tmp.add(gRouteOverlay);

			gDistanceToDest = route.getDistance();
			gTimeToDest = route.getDuration();
			
			toastThis("Distance: "+gDistanceToDest + "\nTravel Duration: "+gTimeToDest, LONG);

			if(gProgressDialog.isShowing())
				gProgressDialog.dismiss();
			gMapView.invalidate();
		}
	}

	private GeoPoint getDirections() {
		gProgressDialog = new ProgressDialog(MNavMainActivity.this);
		gProgressDialog.setMessage("Calculating Route...");
		gProgressDialog.setCancelable(true);
		gProgressDialog.setIndeterminate(true);
		gProgressDialog.show();
		hasSeenAlert1 = true;
		//create a geopoint for dest
		GeoPoint dest = new GeoPoint((int)(gDestinationLat * 1e6), (int)(gDestinationLong * 1e6));
		GeoPoint start;
		if(gMyLocationOverlay != null)
			start = gMyLocationOverlay.getMyLocation();
		else //if MyLocation isn't working, use default from diag
			start = new GeoPoint((int)(42.276956 * 1e6), (int)(-83.738234 * 1e6));

		buildAlertDialog(ALERT_INTRO_PROMPT_2);
		if(start == null || start.equals(dest))
			return dest;

		gStartGeo = start;
		gDestGeo = dest;
		new GetDirectionsTask().execute(start, dest);
		return dest;
	}

}
