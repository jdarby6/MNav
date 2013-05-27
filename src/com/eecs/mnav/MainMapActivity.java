package com.eecs.mnav;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class MainMapActivity extends MapActivity implements TextWatcher {
	//Layout globals
	private MapView gMapView = null;
	private TextView tvDestInfo = null;
	private Button bPlotRoute;
	private Button bTargetReticle;
	private Button bZoomIn;
	private Button bZoomOut;

	// Intro tips booleans
	private boolean hasSeenAlert1 = false;
	private boolean hasSeenAlert2 = false;
	private boolean hasSeenAlert3 = false;
	private boolean hasSeenAlert4 = false;

	private boolean isTransit = false;


	//Destination globals
	private AutoCompleteTextView autoCompleteTextViewDestination;
	private String gDestName = "the diag";
	private String gDestNum = "";
	private String gDestName_full ="";
	private String gDistanceToDest;
	private String gTimeToDest;
	private double gDestinationLong = -83.738234;
	private double gDestinationLat = 42.276956;

	//Helper globals
	private LocalDatabaseHandler local_db;
	private DataBaseHelper destination_db;
	private MapController gMapController = null;
	private LocationManager gLocationManager;
	private SharedPreferences gPreferences = null;
	private GetDirectionsTask gOurGetDirectionsTask = null;
	private Handler uiHandler;

	//Overlay globals
	private PinOverlay gPinOverlay = null;
	private RouteOverlay gRouteOverlay = null;
	private MyLocationOverlay gMyLocationOverlay = null;
	private ScaleBarOverlay gScaleBarOverlay = null;

	//Directions globals
	private ProgressDialog gProgressDialog;
	private GeoPoint gStartGeo;
	private GeoPoint gDestGeo;

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
	//Overlay IDs
	private static final int OVERLAY_MYLOC_ID = 0;
	private static final int OVERLAY_SCALEBAR_ID = 1;
	private static final int OVERLAY_PIN_ID = 2;
	private static final int OVERLAY_ROUTE_ID = 3;


	private class Coords {
		double latitude;
		double longitude;
	}
	private Coords doors[];
	int num_doors = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("MainMapActivity", "OnCreate() called");
		setContentView(R.layout.activity_main);

		//Grab the mapView
		gMapView = (MapView)findViewById(R.id.mapview);

		//Turn off HW acceleration on new devices (map overlays are a bit buggy with it enabled)
		try {
			Method setLayerTypeMethod = gMapView.getClass().getMethod("setLayerType", new Class[] {int.class, Paint.class});
			setLayerTypeMethod.invoke(gMapView, new Object[] {Constants.LAYER_TYPE_SOFTWARE, null});
		} catch (NoSuchMethodException e) {
			// Older OS, no HW acceleration anyway
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		tvDestInfo = (TextView) findViewById(R.id.textView_destInfo);
		tvDestInfo.setVisibility(TextView.INVISIBLE);

		//Initialize local db
		local_db = new LocalDatabaseHandler(this);

		//Initialize destination db
		destination_db = new DataBaseHelper("destination_db");
		try {
			destination_db.createDataBase();
		} catch (IOException ioe) {
			throw new Error("Unable to create database");
		} try {
			destination_db.openDataBase();
		} catch(SQLException sqle) {	
			throw sqle;
		}


		//Initialize UI handler
		uiHandler = new Handler();

		//Load stored data
		gPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		gDestName = gPreferences.getString("DESTNAME", "the diag");
		gDestNum = gPreferences.getString("DESTNUM", "");
		hasSeenAlert1 = gPreferences.getBoolean("A1", false);
		hasSeenAlert2 = gPreferences.getBoolean("A2", false);
		hasSeenAlert3 = gPreferences.getBoolean("A3", false);
		hasSeenAlert4 = gPreferences.getBoolean("A4", false);

		//Check to see if user came without typing anything
		if(gDestName.equals("the diag")) {
			num_doors = -1;
			gDestName_full = "The Diag";
		} 
		else {
			digIntoDatabaseForBuildingInformationAndStuff();
		}

		buildAlertDialog(ALERT_INTRO_PROMPT_1);

		//Retrieve list of all buildings to use with the AutoCompleteTextView
		Cursor cursor = destination_db.getAllBldgs();

		ArrayList<String> strings = new ArrayList<String>();
		for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			String full_name = cursor.getString(0);
			String abbr_name = cursor.getString(1);
			strings.add(abbr_name + ": " + full_name);
		}

		cursor.close();
		String[] item = (String[]) strings.toArray(new String[strings.size()]);

		autoCompleteTextViewDestination = (AutoCompleteTextView)findViewById(R.id.autoCompleteTextView_map_destination);
		autoCompleteTextViewDestination.addTextChangedListener(this);
		autoCompleteTextViewDestination.setTextColor(Color.BLACK);
		autoCompleteTextViewDestination.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, item));
		/*
		//Hide soft keyboard when an option is clicked
		autoCompleteTextViewDestination.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				in.hideSoftInputFromWindow(autoCompleteTextViewDestination.getWindowToken(), 0);

			}

		});
		*/
		//Programmatically press the search button when the search key is pressed on the soft keyboard
		autoCompleteTextViewDestination.setOnEditorActionListener(new AutoCompleteTextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEARCH) {
					String tempAddress = autoCompleteTextViewDestination.getText().toString();
					if(tempAddress.indexOf(':') != -1) {
						String destAbbr = tempAddress.substring(0, tempAddress.indexOf(':'));
						tempAddress = destAbbr;
					}
					if(tempAddress.matches(Constants.REGEX_ROOM_NUM) || tempAddress.matches(Constants.REGEX_BLDG_NAME)) {
						if(tempAddress.matches(Constants.REGEX_ROOM_NUM)) {
							gDestNum = tempAddress.substring(0,tempAddress.indexOf(" "));
							gDestName = tempAddress.substring(tempAddress.indexOf(" ")).trim();
							Log.d("MainMapActivity", "Search Button Matches REGEX_ROOM_NUM! RoomNum=" + gDestNum + " BldgName=" + gDestName);
						} else {//It should just be the name of the bldg
							gDestName = tempAddress;
							gDestNum = "";
							Log.d("MainMapActivity", "Search Button Matches REGEX_BLDG_NAME! RoomNum=" + gDestNum + " BldgName=" + gDestName);
						}
					} else if(tempAddress != null && tempAddress.length() > 0){
						//It doesn't match our regEx so it's an invalid entry.
						return false;
					}
					digIntoDatabaseForBuildingInformationAndStuff();
					findClosestDoor();
					GeoPoint dest = new GeoPoint((int)(gDestinationLat * 1e6), (int)(gDestinationLong * 1e6));
					gMapView.getOverlays().remove(OVERLAY_PIN_ID);
					putPinOnMap(dest, gDestName_full);
					gMapView.invalidate();
					zoomTo(dest, Constants.ZOOM_LEVEL_BUILDING);
					InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					in.hideSoftInputFromWindow(autoCompleteTextViewDestination.getWindowToken(), 0);
					return true;
				}
				return false;
			}
		});

		if(!gDestName.equals("the diag"))
			autoCompleteTextViewDestination.setText(gDestName);

		//---------------------
		//PLOT ROUTE BUTTON
		//---------------------
		bPlotRoute = (Button) findViewById(R.id.button_plotroute);
		bPlotRoute.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.d("GetRouteClicked", "Calculating Route");
				//hide the soft keyboard
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(tvDestInfo.getWindowToken(), 0);

				tvDestInfo.setVisibility(TextView.INVISIBLE);
				//Grab the user input lat/long
				String input = autoCompleteTextViewDestination.getText().toString();
				String tempAddress = input;
				int colonIndex = input.indexOf(':');
				if(colonIndex != -1)
					tempAddress = input.substring(0, colonIndex);
				if(tempAddress.matches(Constants.REGEX_ROOM_NUM) || tempAddress.matches(Constants.REGEX_BLDG_NAME)) {
					if(tempAddress.matches(Constants.REGEX_ROOM_NUM)) {
						gDestNum = tempAddress.substring(0,tempAddress.indexOf(" "));
						gDestName = tempAddress.substring(tempAddress.indexOf(" ")).trim();
						Log.d("Search Button", "Matches REGEX_ROOM_NUM! RoomNum="+gDestNum+" BldgName="+gDestName);
					} else if(tempAddress.matches(Constants.REGEX_ROOM_NUM_AFTER)) {
						gDestName = tempAddress.substring(0,tempAddress.indexOf(" "));
						gDestNum = tempAddress.substring(tempAddress.indexOf(" ")).trim();
						Log.d("Schedule Button", "Matches REGEX_ROOM_NUM_AFTER! RoomNum="+gDestNum+" BldgName="+gDestName);
					} else {//It should just be the name of the bldg
						gDestName = tempAddress;
						gDestName.trim();
						gDestNum = "";
						Log.d("Search Button", "Matches REGEX_BLDG_NAME! RoomNum="+gDestNum+" BldgName="+gDestName);
					}
				} else{
					//It doesn't match our regEx so it's an invalid entry.
					tvDestInfo.setVisibility(TextView.VISIBLE);
					tvDestInfo.setText("Invalid destination entry.");
					tvDestInfo.setTextColor(getResources().getColor(R.color.fireBrickRed));
					return;
				}

				//The gDestName is set, so dig into the database for building information and stuff
				digIntoDatabaseForBuildingInformationAndStuff();
				//create geopoint for dest
				GeoPoint dest;
				
				//now find closest door
				findClosestDoor();
				if(HelperFunctions.checkGPS(MainMapActivity.this)){
					dest = getDirections(); //Query Google for directions to dest and return the dest
					gMapView.getOverlays().remove(OVERLAY_PIN_ID);
				} else {
					dest = new GeoPoint((int)(gDestinationLat * 1e6), (int)(gDestinationLong * 1e6));
					HelperFunctions.toastThisGravity("Enable GPS to get walking directions to this building.", Constants.LONG, 200);
				}
				
				putPinOnMap(dest, gDestName_full);
				gMapView.invalidate();
				zoomTo(dest, Constants.ZOOM_LEVEL_BUILDING);
			}
		});

		//---------------------
		//SATELLITE BUTTON
		//---------------------
/*		bSatellite = (Button) findViewById(R.id.button_satellite);
		if(!gMapView.isSatellite())
			bSatellite.setBackgroundResource(R.drawable.ic_road);
		else
			bSatellite.setBackgroundResource(R.drawable.ic_satellite);
		bSatellite.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(gMapView.isSatellite()) {
					bSatellite.setBackgroundResource(R.drawable.ic_road);
					gMapView.setSatellite(false);
				}
				else {
					gMapView.setSatellite(true);
					bSatellite.setBackgroundResource(R.drawable.ic_satellite);
				}

				Log.d("MAPSTUFF", "ZoomLevel=" + gMapView.getZoomLevel());
			}
		});*/

		//---------------------
		//TARGETING BUTTON
		//---------------------
		bTargetReticle = (Button) findViewById(R.id.button_return);
		bTargetReticle.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(HelperFunctions.checkGPS(MainMapActivity.this)){
					GeoPoint currentLoc = gMyLocationOverlay.getMyLocation();
					zoomTo(currentLoc, Constants.ZOOM_LEVEL_BUILDING);
					buildAlertDialog(ALERT_INTRO_PROMPT_3);
				} else {
					HelperFunctions.displayEnableGPSAlert(MainMapActivity.this);
				}
			}
		});
		bTargetReticle.setOnLongClickListener(new OnLongClickListener() {
			public boolean onLongClick(View v) {
				//create a geopoint for dest
				GeoPoint dest = new GeoPoint((int)(gDestinationLat * 1e6), (int)(gDestinationLong * 1e6));
				zoomTo(dest, Constants.ZOOM_LEVEL_BUILDING);
				buildAlertDialog(ALERT_INTRO_PROMPT_4);
				return true;
			}
		});

		//---------------------
		//Zoom IN BUTTON
		//---------------------
		bZoomIn = (Button) findViewById(R.id.button_zoomin);
		bZoomIn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(gMapController == null)
					gMapController = gMapView.getController();
				gMapController.zoomIn();
			}
		});

		//---------------------
		//Zoom OUT BUTTON
		//---------------------
		bZoomOut = (Button) findViewById(R.id.button_zoomout);
		bZoomOut.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(gMapController == null)
					gMapController = gMapView.getController();
				gMapController.zoomOut();
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		//HelperFunctions.checkGPS(this);
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
		editor.putBoolean("A1", hasSeenAlert1);
		editor.putBoolean("A2", hasSeenAlert2);
		editor.putBoolean("A3", hasSeenAlert3);
		editor.putBoolean("A4", hasSeenAlert4);
		editor.commit();

		//Turn off GPS
		if(gLocationManager != null)
			gLocationManager.removeUpdates(locationListener);

		destination_db.close();

		if(gOurGetDirectionsTask != null) {
			gOurGetDirectionsTask.cancel(true);
			gOurGetDirectionsTask = null;
		}
	}	


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public boolean onOptionsItemSelected (MenuItem item) {
		switch (item.getItemId()){
		case R.id.menu_satellite:
			if(gMapView.isSatellite()) {
				gMapView.setSatellite(false);
			}
			else {
				gMapView.setSatellite(true);
			}

			Log.d("MAPSTUFF", "ZoomLevel=" + gMapView.getZoomLevel());
			break;
		case R.id.menu_settings:
			showDialog(DIALOG_SETTINGS);
			break;
		case R.id.menu_show_all:
			putPinsOnMap(digIntoDatabaseForEverything());
			break;
		case R.id.menu_save_loc:
			showDialog(DIALOG_SAVE_CURRENT_LOC);
			break;
		case R.id.menu_clear_all:
			gPinOverlay.clearPins();
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

			final float curLat = (float)gMyLocationOverlay.getMyLocation().getLatitudeE6() * (float)1E-6;
			final float curLong = (float)gMyLocationOverlay.getMyLocation().getLongitudeE6() * (float)1E-6;

			textView_CurrentLat.setText("Current lat: " + curLat);
			textView_CurrentLong.setText("Current long: " + curLong);

			button_Save.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					String bldgAbbr = editText_BldgAbbr.getText().toString();
					String doorNick = editText_DoorNick.getText().toString();
					if((bldgAbbr == null || bldgAbbr.length() == 0) || (doorNick == null || doorNick.length() == 0)) {
						AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainMapActivity.this);

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
						local_db.addRow(bldgAbbr, doorNick, String.valueOf(curLat), String.valueOf(curLong));
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
					if(gDestName_full.equals("the diag")){
						removeDialog(DIALOG_DESTINATION_BLDG);
					}
					else{
						//Call intent to new activity
						Intent intent = new Intent(MainMapActivity.this, BuildingMapActivity.class);
						startActivity(intent);
						removeDialog(DIALOG_DESTINATION_BLDG);
					}
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
			final CheckBox useTransit = (CheckBox) dialogTemp.findViewById(R.id.checkBox_use_transit);
			useTransit.setChecked(isTransit);

			final Button bDone = (Button) dialogTemp.findViewById(R.id.button_done);
			bDone.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					if(resetTips.isChecked()){
						hasSeenAlert1 = false;
						hasSeenAlert2 = false;
						hasSeenAlert3 = false;
						hasSeenAlert4 = false;
						HelperFunctions.toastThis("Intro tips reset", Constants.LONG);
					}

					if(useTransit.isChecked())
						isTransit = true;
					else 
						isTransit = false;

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
		public void onLocationChanged(Location location) {}

		public void onStatusChanged(String provider, int status, Bundle extras) {}

		public void onProviderEnabled(String provider) {}

		public void onProviderDisabled(String provider) {}
	};

	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}
		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > Constants.FIVE_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -Constants.FIVE_MINUTES;
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
	@SuppressWarnings("unused")
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
	
	//Adds MyLocationOverlay, ScaleBarOverlay, and PinOverlay with single dest pin
	public void initOverlays() {
		//Remove all existing overlays
		List<Overlay> mapOverlays = gMapView.getOverlays();
		mapOverlays.clear();			
		//Start with putting our own location on the map
		gMyLocationOverlay = new MyLocationOverlay(this, gMapView);
		mapOverlays.add(OVERLAY_MYLOC_ID, gMyLocationOverlay);
		//Create the scalebar and add it to mapview
		gScaleBarOverlay = new ScaleBarOverlay(gMapView);
		gScaleBarOverlay.setImperial();
		mapOverlays.add(OVERLAY_SCALEBAR_ID, gScaleBarOverlay);
		if(HelperFunctions.checkGPS(this)){
			//Start with putting our own location on the map
			//gMyLocationOverlay = new MyLocationOverlay(this, gMapView);
			//mapOverlays.add(OVERLAY_MYLOC_ID, gMyLocationOverlay);
			gMyLocationOverlay.enableMyLocation();
			//Run this block of code after finding our first location fix
			gMyLocationOverlay.runOnFirstFix(new Runnable() {
				public void run() {
					//If we haven't seen alert1 or if no location was entered, center on self
					if(!hasSeenAlert1 || gDestName.equals("the diag")){
						GeoPoint p = gMyLocationOverlay.getMyLocation();
						if(gMapController == null)
							gMapController = gMapView.getController();
						gMapController.animateTo(p);
						gMapController.setZoom(Constants.ZOOM_LEVEL_SKY);
					}
					//Now query last known destination (or default destination)
					GeoPoint dest = new GeoPoint((int)(gDestinationLat * 1e6), (int)(gDestinationLong * 1e6));
					//Find the closest door
					if(findClosestDoor()) {
						Log.d("ClosestDoor", "Found closest door!");
						//If we found the closest door,
						//create a geopoint for that door destination
						dest = new GeoPoint((int)(gDestinationLat * 1e6), (int)(gDestinationLong * 1e6));
					}
					//Now take whatever destination we have now (either default, last known, or new closest door)
					//Put it in overlay item and add pin to map
					putPinOnMap(dest, gDestName_full);
					uiHandler.removeCallbacks(invalidateMapFromHandler);
					uiHandler.post(invalidateMapFromHandler);
	
					//If we have seen alert1 and there is a location, zoom to that location
					if(hasSeenAlert1 && !gDestName.equals("the diag"))
						zoomTo(dest, Constants.ZOOM_LEVEL_BUILDING);
				}
			});
		} else {
			//Now query last known destination (or default destination)
			GeoPoint dest = new GeoPoint((int)(gDestinationLat * 1e6), (int)(gDestinationLong * 1e6));
			//Find the closest door
			if(findClosestDoor()) {
				Log.d("ClosestDoor", "Found closest door!");
				//If we found the closest door,
				//create a geopoint for that door destination
				dest = new GeoPoint((int)(gDestinationLat * 1e6), (int)(gDestinationLong * 1e6));
			}
			//Now take whatever destination we have now (either default, last known, or new closest door)
			//Put it in overlay item and add pin to map
			putPinOnMap(dest, gDestName_full);
			uiHandler.removeCallbacks(invalidateMapFromHandler);
			uiHandler.post(invalidateMapFromHandler);

			//If we have seen alert1 and there is a location, zoom to that location
			if(hasSeenAlert1 && !gDestName.equals("the diag"))
				zoomTo(dest, Constants.ZOOM_LEVEL_BUILDING);
		}
	}

	private Route directionsWalking(final GeoPoint start, final GeoPoint dest) {
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
		Route r =  googleParser.parseWalking();
		return r;
	}

	private Route directionsTransit(final GeoPoint start, final GeoPoint dest) {
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
		//sBuf.append("&sensor=true&mode=walking");
		String currentTime = String.valueOf(System.currentTimeMillis());
		currentTime = currentTime.substring(0, currentTime.length()-3);
		sBuf.append("&sensor=true&departure_time="+currentTime+"&mode=transit");
		googleParser = new GoogleParser(sBuf.toString());
		Route r =  googleParser.parseTransit();
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

	private void buildAlertDialog(int ID) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainMapActivity.this);
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

	/**
	 * Get the distance (double) from current location as given by GPS using gMyLocationOverlay.
	 * @param latitude - latitude coordinate of destination
	 * @param longitude - longitude coordinate of destination
	 * @return Euclidean distance from current location to destination coords if GPS is enabled. -1 otherwise.
	 */
	private double getDistanceFromCurrentLoc(double latitude, double longitude) {
		if(HelperFunctions.checkGPS(MainMapActivity.this)){
			final float curLat = (float)gMyLocationOverlay.getMyLocation().getLatitudeE6() * (float)1E-6;
			final float curLong = (float)gMyLocationOverlay.getMyLocation().getLongitudeE6() * (float)1E-6;
	
			double lat_dist = latitude - curLat;
			double long_dist = longitude - curLong;
	
			return Math.sqrt(lat_dist*lat_dist + long_dist*long_dist);
		} else {
			return -1;
		}
	}
	/**
	 * Finds the closest door to current location using getDistanceFromCurrentLoc() and gDestinationLat & gDestinationLong.
	 * @return true if num_doors > != 1 && doors.length > 0
	 */
	private boolean findClosestDoor() {
		int currentBestIndex = 0;
		double currentBestDistance = 0;

		for(int i = 0; i < num_doors; i++) {
			double contenderDistance = getDistanceFromCurrentLoc(doors[i].latitude, doors[i].longitude);
			if(contenderDistance == -1){
				currentBestIndex = 0;
				Log.d("MNavMainActivity","findClosestDoor got -1");
				break;
			}
			if(contenderDistance < currentBestDistance)
				currentBestIndex = i;
		}

		if(num_doors != -1 && doors.length > 0) {
			gDestinationLat = doors[currentBestIndex].latitude;
			gDestinationLong = doors[currentBestIndex].longitude;
			return true;
		}

		return false;
	}

	// Implementation of AsyncTask used to get walking directions from current location to destination
	private class GetDirectionsTask extends AsyncTask<GeoPoint, Void, Route> {

		@Override
		protected Route doInBackground(GeoPoint... geopoints) {
			//Creates Url and queries google directions api
			if (isTransit)
				return directionsTransit(geopoints[0], geopoints[1]);
			return directionsWalking(geopoints[0], geopoints[1]);
		}

		@Override
		protected void onPostExecute(Route route) {
			List<Overlay> tmp = gMapView.getOverlays();
			//Remove the route if there's one there already
			if(tmp.contains(gRouteOverlay)) {
				tmp.remove(gRouteOverlay);
			}
			gRouteOverlay = new RouteOverlay(route, gStartGeo, gDestGeo, getResources().getColor(R.color.fireBrickRed));
			tmp.add(OVERLAY_ROUTE_ID, gRouteOverlay);

			gDistanceToDest = route.getDistance();
			gTimeToDest = route.getDuration();

			HelperFunctions.toastThis("Distance: "+gDistanceToDest + "\nTravel Duration: "+gTimeToDest, Constants.LONG);

			HelperFunctions.toastThis("TransitType:"+route.getSegments().get(0).getTransitMode(), Constants.LONG);

			tvDestInfo.setVisibility(TextView.VISIBLE);
			tvDestInfo.setText("Distance: "+gDistanceToDest+" Travel Duration: "+gTimeToDest);
			tvDestInfo.setTextColor(getResources().getColor(R.color.black));

			if(gProgressDialog.isShowing())
				gProgressDialog.dismiss();
			gMapView.invalidate();

			gOurGetDirectionsTask = null;
		}
	}

	private GeoPoint getDirections() {
		gProgressDialog = new ProgressDialog(MainMapActivity.this);
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
		gOurGetDirectionsTask = new GetDirectionsTask();
		gOurGetDirectionsTask.execute(start, dest);
		return dest;
	}

	//Method to grab the information from database pertaining to the building after gDestName is set.
	private void digIntoDatabaseForBuildingInformationAndStuff() {
		/** DATABASE STUFF **/
		//Grab a cursor	
		Cursor cursor = destination_db.getBldgIdByName(gDestName);
		if(cursor.getCount() > 0 && cursor.moveToFirst()) { //Destination is there, so show dialog and grab info
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
	}
		
	//Creates new pin overlay and places single pin on map
		private void putPinsOnMap(ArrayList<Coords> pinLocations) {
			OverlayItem tmpItem;
			GeoPoint doorPin;
			Coords curCoord = new Coords();
			int size = pinLocations.size();
			for(int idx = 1; idx < size; idx++) {
				curCoord = pinLocations.get(idx);
			//	Log.d("AllBldgs", "Pin#:"+idx+" at lat:"+curCoord.latitude+" long:"+curCoord.longitude);
				doorPin = new GeoPoint((int)(curCoord.latitude * 1e6), (int)(curCoord.longitude * 1e6));
				tmpItem = new OverlayItem(doorPin, "test", "This is one of our pins");
				gPinOverlay.addOverlayNoPopulate(tmpItem);
			}
			gPinOverlay.populateOverlay();
		}

		//Method to grab all building doors in the database
		private ArrayList<Coords> digIntoDatabaseForEverything() {
			Cursor bldgCursor = destination_db.getAllBldgIds();
			Cursor doorCursor;
			ArrayList<Coords> allDoors = new ArrayList<Coords>();
			int numBldgs = bldgCursor.getCount();
			if(numBldgs > 0 && bldgCursor.moveToFirst()) { //Destination is there, so show dialog and grab info
				while(numBldgs > 0){
				//	Log.d("AllBldgs", "numBldgs="+numBldgs);
					//find column containing bldg num
					int bldg_num_col = bldgCursor.getColumnIndex("bldg_num");
					if(bldg_num_col < 0) { numBldgs--; continue; } //The column doesn't exist
					int bldg_num = bldgCursor.getInt(bldg_num_col);
					//find column containing num_doors
					int num_doors_col = bldgCursor.getColumnIndex("num_doors");
					int num_doors = bldgCursor.getInt(num_doors_col);
					
					//get the lat/long of each door to put in doors[]
					doorCursor = destination_db.getDoorsByBldgId(bldg_num);
					if(doorCursor.moveToFirst()) {
						int door_lat_col = doorCursor.getColumnIndex("door_lat");
						int door_long_col = doorCursor.getColumnIndex("door_long");
						for(int i = 0; i < num_doors; i++) {
							Coords tmpDoorCoord = new Coords();
							tmpDoorCoord.latitude = doorCursor.getDouble(door_lat_col);
							tmpDoorCoord.longitude = doorCursor.getDouble(door_long_col);
							//Log.d("AllBldgs","Added door# "+i+" at lat:"+tmpDoorCoord.latitude+" long:"+tmpDoorCoord.longitude);
							allDoors.add(tmpDoorCoord);
							doorCursor.moveToNext();
						}
						doorCursor.close();
					}
					bldgCursor.moveToNext();
					numBldgs--;
				}
			} else {
				Log.d("AllBldgs", "This building doesn't exist");
				num_doors = -1;
			}
			bldgCursor.close();
			return allDoors;
		}
	
	//Creates new pin overlay and places single pin on map
	private void putPinOnMap(GeoPoint pinLocation, String name) {
		OverlayItem tmpItem;
		Drawable drawable = this.getResources().getDrawable(R.drawable.ic_pin);
		//Create our route overlay
		gPinOverlay = new PinOverlay(drawable);
		gPinOverlay.setTapListener(this);
		tmpItem = new OverlayItem(pinLocation, name, "This is your current destination");
		gPinOverlay.addOverlay(tmpItem);
		HelperFunctions.toastThis(name, Constants.SHORT);
		gMapView.getOverlays().add(OVERLAY_PIN_ID, gPinOverlay);
	}

	private Runnable invalidateMapFromHandler = new Runnable(){
		public void run() {
			gMapView.invalidate();
		}
	};

	public void afterTextChanged(Editable s) {


	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {


	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {


	}
}
