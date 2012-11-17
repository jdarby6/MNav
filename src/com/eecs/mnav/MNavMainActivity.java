package com.eecs.mnav;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
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
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.parse.Parse;
import com.parse.ParseObject;

public class MNavMainActivity extends MapActivity {
	//Declare globals  //g is for global
	private float gBearing = 0;
	private float gSpeed = 0;
	private double gCurrentLong = 0.0;
	private double gCurrentLat = 0.0;
	private double gDestinationLong = 0.0;
	private double gDestinationLat = 0.0;
	private Location gBestLocation = null;
	private MapView gMapView = null;
	private MapController gMapController = null;
	private SharedPreferences gPreferences = null;
	private LocationManager gLocationManager;
	private boolean overlaysInitialized = false;
	private Button bPlotRoute;
	private Button bSatellite;
	private Button bReturn;
	private Button bZoomIn;
	private Button bZoomOut;
	private EditText tvDestination;
	private String gDestAddr = "the diag";
	private LocalDatabaseHandler local_db;
	private DataBaseHelper destination_db;
	private CurrentRouteOverlay gRouteOverlay = null;
	private ScaleBarOverlay gScaleBarOverlay = null;

	private static final int LONG = Toast.LENGTH_LONG;
	private static final int SHORT = Toast.LENGTH_SHORT;
	private static final int TWO_MINUTES = 1000 * 60 * 2;
	private static final int LAYER_TYPE_SOFTWARE = 1;
	private static final int ZOOM_LEVEL_SKY = 17;
	private static final int ZOOM_LEVEL_CAMPUS = 18;
	private static final int ZOOM_LEVEL_BUILDING = 19;

	//These are arbitrary numbers, used to call and remove the correct dialogs
	private static final int DIALOG_SAVE_CURRENT_LOC = 0;
	final static int DIALOG_DESTINATION_BLDG = 1;

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
		gDestAddr = gPreferences.getString("DESTADDR", "the diag");

		/** DATABASE STUFF **/
		if(gDestAddr != "the diag") { //checking against default string to see if we got a new destination address
			gDestAddr = gDestAddr.replaceAll("[0-9]", "").trim();
			Cursor cursor = destination_db.getBldgIdByName(gDestAddr);
			if(cursor.getCount() > 0 && cursor.moveToFirst()) {
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MNavMainActivity.this);

				// set title
				//alertDialogBuilder.setTitle("Invalid Destination");

				// set dialog message
				alertDialogBuilder
				.setMessage("Destination found! The map is now centered at your current location. " 
						+ "To get walking directions, click the pedestrian icon.")
						.setCancelable(true)
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
				
				int id_num_col = cursor.getColumnIndex("id_num");
				int num_doors_col = cursor.getColumnIndex("num_doors");
				int id_num = cursor.getInt(id_num_col);
				num_doors = cursor.getInt(num_doors_col);

				doors = new Coords[num_doors];
				for(int i = 0; i < num_doors; i++) {
					doors[i] = new Coords();
				}

				cursor.close();
				cursor = destination_db.getDoorsByBldgId(id_num);
				if(cursor.moveToFirst()) {
					int door_lat_col = cursor.getColumnIndex("door_lat");
					int door_long_col = cursor.getColumnIndex("door_long");
					for(int i = 0; i < num_doors; i++) {
						doors[i].latitude = cursor.getDouble(door_lat_col);
						doors[i].longitude = cursor.getDouble(door_long_col);
						cursor.moveToNext();
					}
				}
			}
			else {
				num_doors = -1;
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MNavMainActivity.this);

				// set title
				alertDialogBuilder.setTitle("Invalid Destination");

				// set dialog message
				alertDialogBuilder
				.setMessage("We couldn't recognize the destination you entered. Make sure everything is spelled correctly"
						+ "and try again.")
						.setCancelable(false)
						.setNegativeButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,int id) {
								// if this button is clicked, just close
								// the dialog box and do nothing

								MNavMainActivity.this.finish();
								dialog.cancel();
							}
						});

				// create alert dialog
				AlertDialog alertDialog = alertDialogBuilder.create();

				// show it
				alertDialog.show();
			}
		}

		Log.d("LOADED DATA", "Coords: "+String.valueOf(gCurrentLat)+","+String.valueOf(gCurrentLong)+
				" time: "+" destAddr: "+gDestAddr);
		//Put last known info as current location
		Location location = new Location(LocationManager.GPS_PROVIDER);
		location.setLatitude(gCurrentLat);
		location.setLongitude(gCurrentLong);
		location.setTime(Long.parseLong(gPreferences.getString("LASTLOCTIME", "0")));
		gBestLocation = location;


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
			
		tvDestination = (EditText)findViewById(R.id.editText_map_destination);

		bPlotRoute = (Button) findViewById(R.id.button_plotroute);
		bPlotRoute.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.d("GetRouteClicked", "Calculating Route");
				//Grab the user input lat/long
				String temp = tvDestination.getText().toString();
				if(temp != null && temp.length() > 0) {
					gDestinationLat = Double.parseDouble(temp.substring(0, temp.indexOf(",")));
					gDestinationLong = Double.parseDouble(temp.substring(temp.indexOf(",")+1,temp.length()));
					//create a geopoint for dest
					GeoPoint dest = new GeoPoint((int)(gDestinationLat * 1e6), (int)(gDestinationLong * 1e6));

					
					//XXX Set "Desination" to full building name
					OverlayItem overlayitem = new OverlayItem(dest, "Destination", "This is your current destination");

					gRouteOverlay.addOverlay(overlayitem);
					gMapView.getOverlays().add(gRouteOverlay); 

					zoomTo(dest, ZOOM_LEVEL_BUILDING);
					GeoPoint start = new GeoPoint((int)(gCurrentLat * 1e6), (int)(gCurrentLong * 1e6));
					if(start.equals(dest))
						return;
					new GetDirectionsTask().execute(start, dest);
				}
			}
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

		bReturn = (Button) findViewById(R.id.button_return);
		bReturn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				GeoPoint currentLoc = new GeoPoint((int)(gCurrentLat * 1e6), (int)(gCurrentLong * 1e6));
				zoomTo(currentLoc, ZOOM_LEVEL_BUILDING);
				startGPS();
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

		//THIS IS WHERE WE PUT THE CLOSEST SEARCH RESULT INTO THE SEARCH BAR
		if(num_doors != -1) {
			int closestDoorIndex = getClosestDoorIndex();
			gDestinationLat = doors[closestDoorIndex].latitude;
			gDestinationLong = doors[closestDoorIndex].longitude;
			tvDestination.setText(String.valueOf(gDestinationLat) + "," + String.valueOf(gDestinationLong));
		}
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
		editor.commit();
		Log.d("SAVED DATA", "Coords: "+String.valueOf(gCurrentLat)+","+String.valueOf(gCurrentLong));	
		//Turn off GPS
		if(gLocationManager != null)
			gLocationManager.removeUpdates(locationListener);
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
		default:
			return false;
		}
		return true;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		switch(id) {
		case DIALOG_SAVE_CURRENT_LOC:
			final Dialog dialog_saveCurrentLoc = new Dialog(this);
			dialog_saveCurrentLoc.setContentView(R.layout.dialog_save_current_loc);
			dialog_saveCurrentLoc.setTitle("Save Current Location");

			final EditText editText_BldgAbbr = (EditText)dialog_saveCurrentLoc.findViewById(R.id.editText_BldgAbbr);
			final EditText editText_DoorNick = (EditText)dialog_saveCurrentLoc.findViewById(R.id.editText_DoorNick);
			final TextView textView_CurrentLat = (TextView)dialog_saveCurrentLoc.findViewById(R.id.textView_CurrentLat);
			final TextView textView_CurrentLong = (TextView)dialog_saveCurrentLoc.findViewById(R.id.textView_CurrentLong);
			final Button button_Save = (Button)dialog_saveCurrentLoc.findViewById(R.id.button_Save);
			final Button button_Cancel = (Button)dialog_saveCurrentLoc.findViewById(R.id.button_Cancel);

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

			dialog = dialog_saveCurrentLoc;
			break;
			case DIALOG_DESTINATION_BLDG:
				final Dialog dialog_destinationBldg = new Dialog(this);
				dialog_destinationBldg.requestWindowFeature(Window.FEATURE_NO_TITLE);
				dialog_destinationBldg.setContentView(R.layout.dialog_destination_building);
				dialog_destinationBldg.setTitle("DESTINATIO NNAME HERE"); //TODO Put the destination name here

				final Button bViewMap = (Button)dialog_destinationBldg.findViewById(R.id.button_viewmap);
				final Button bGetDirections = (Button)dialog_destinationBldg.findViewById(R.id.button_getdirections);

				bViewMap.setOnClickListener(new Button.OnClickListener() {
					public void onClick(View v) {
							//Call intent to new activity XXX
							Intent intent = new Intent(MNavMainActivity.this, BuildingMapActivity.class);
							startActivity(intent);
							removeDialog(DIALOG_DESTINATION_BLDG);
							}
				});

				bGetDirections.setOnClickListener(new Button.OnClickListener() {
					public void onClick(View v) {
						//Call method to get directions XXX
						GeoPoint dest = new GeoPoint((int)(gDestinationLat * 1e6), (int)(gDestinationLong * 1e6));
						GeoPoint start = new GeoPoint((int)(gCurrentLat * 1e6), (int)(gCurrentLong * 1e6));
						if(start.equals(dest))
							return;
						new GetDirectionsTask().execute(start, dest);
						removeDialog(DIALOG_DESTINATION_BLDG);
						}
				});

				dialog = dialog_destinationBldg;
				break;
		}
		return dialog;
	}

	@Override
	protected boolean isRouteDisplayed() {

		return false;
	}


	LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) { // Called when a new location is found by the network location provider.
			Log.d("LocationChanged", "Found You: "+location.getLatitude()+","+location.getLongitude());
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
			if(!overlaysInitialized)
				initOverlays(gBestLocation);
			else
				updateUserPosition(gBestLocation);
		}

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
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;
		/** People will probably not be moving as much, since they will be in class majority of time
		 * 
		 * 
		 *
		// If it's been more than two minutes since the current location, use the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			Log.d("isBetter", "true: is significantly newer");
			return true;


			// If the new location is more than two minutes older, it must be worse
		} else 
		 */
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
			Log.d("isBetter", "true: is newer, not less accurate");
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
		Location cachedLoc;
		//Get cached location from GPS
		cachedLoc = gLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		//Compare cached GPS location to cached Network location, if it's better, use it
		if(isBetterLocation(gLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER), gBestLocation))
			cachedLoc = gLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

		return cachedLoc;
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

	//
	public void updateUserPosition(Location location) {
		GeoPoint p = new GeoPoint((int)(gCurrentLat * 1e6), (int)(gCurrentLong * 1e6));

		//Remove all existing overlays
		List<Overlay> mapOverlays = gMapView.getOverlays();
		mapOverlays.clear();

		OverlayItem overlayitem = new OverlayItem(p, "Current Location", "You are here!");

		gRouteOverlay.replaceOverlay(overlayitem, 0);
		mapOverlays.add(gRouteOverlay);
	}

	public void initOverlays(Location location) {
		GeoPoint p = new GeoPoint((int)(gCurrentLat * 1e6), (int)(gCurrentLong * 1e6));

		//Remove all existing overlays
		List<Overlay> mapOverlays = gMapView.getOverlays();
		mapOverlays.clear();

		Drawable drawable = this.getResources().getDrawable(R.drawable.ic_pin);
		//Create our route overlay
		gRouteOverlay = new CurrentRouteOverlay(drawable, this);
		gRouteOverlay.setTapListener(this);
		OverlayItem overlayitem = new OverlayItem(p, "Current Location", "You are here!");

		gRouteOverlay.addOverlay(overlayitem);
		mapOverlays.add(gRouteOverlay);       
		
		//Create the scalebar and add it to mapview
		gScaleBarOverlay = new ScaleBarOverlay(this.getBaseContext(), gMapView);
		gScaleBarOverlay.setImperial();
		mapOverlays.add(gScaleBarOverlay);

		zoomTo(p, ZOOM_LEVEL_CAMPUS);

		overlaysInitialized = true;
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
		googleParser = new GoogleParser(sBuf.toString());
		Route r =  googleParser.parse();
		return r;
	}

	private void zoomTo(GeoPoint p, int level) {
		if(gMapController == null)
			gMapController = gMapView.getController();
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
			Log.d("zoomTo()", "Caught exception"+e);
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

		//Check the cahced location, see if it's better than the best one we are using now
		/*	Location cachedLoc = getCachedLocation();
		if(isBetterLocation(cachedLoc, gBestLocation))
			//Since it's better, use it then start querying gps sources
		gBestLocation = cachedLoc;*/	
		gLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
		gLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
	}

	/** Helper function for displaying a toast. Takes the string to be displayed and the length: LONG or SHORT **/
	private void toastThis(String toast, int duration) {
		Context context = getApplicationContext();
		Toast t = Toast.makeText(context, toast, duration);
		t.show();
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

			//catch exception here or something TODODODODODODO

		}

		@Override
		protected void onPostExecute(Route route) {  
			RouteOverlay routeOverlay = new RouteOverlay(route, Color.BLUE);
			gMapView.getOverlays().add(routeOverlay);
		}
	}
}
