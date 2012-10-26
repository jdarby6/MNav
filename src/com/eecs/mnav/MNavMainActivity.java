package com.eecs.mnav;

import java.util.List;

import org.xml.sax.Parser;

import com.eecs.mnav.R;
import com.google.android.maps.*;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.*;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.EditText;

public class MNavMainActivity extends MapActivity {
	//Declare globals  //g is for global
	private float gBearing = 0;
	private float gSpeed = 0;
	private double gLong = 0.0;
	private double gLat = 0.0;
	private Location gBestLocation = null;
	private MapView gMapView;
	private LocationManager gLocationManager;
	private boolean firstRun = true;
	private Button bGetLocation;
	
	//start page items;
	private Button search;
	private EditText address_box;
	
	private static final int LONG = Toast.LENGTH_LONG;
	private static final int SHORT = Toast.LENGTH_SHORT;
	private static final int TWO_MINUTES = 1000 * 60 * 2;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        bGetLocation = (Button) findViewById(R.id.button_getlocation);
        search = (Button)findViewById(R.id.button_search);
        address_box = (EditText)findViewById(R.id.editText_address_box);

        bGetLocation.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
		        GeoPoint dest = new GeoPoint((int)(42.276773 * 1e6), (int)(-83.740178 * 1e6));
		        GeoPoint start = new GeoPoint((int)(gLat * 1e6), (int)(gLong * 1e6));
		        Route route = directions(start, dest);
		        RouteOverlay routeOverlay = new RouteOverlay(route, Color.BLUE);
		        gMapView.getOverlays().add(routeOverlay);
			}
        	
        });

        
        gMapView = (MapView) findViewById(R.id.mapview);
        gMapView.setBuiltInZoomControls(true);
        
        gLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        //Check to see if GPS is enabled
        if(!gLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
        	//If not enabled, prompt user to enabled it
        	displayEnableGPSAlert();
        }
        
        //Start looking for location information
      	getCachedLocation();
      	gLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
      	gLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

	@Override
	protected boolean isRouteDisplayed() {
		
		return false;
	}
	
	
	LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			// Called when a new location is found by the network location provider.
			if(isBetterLocation(location, gBestLocation))
				gBestLocation = location;
			gBearing = gBestLocation.getBearing();
			gLat = gBestLocation.getLatitude();
			gLong = gBestLocation.getLongitude();
			gSpeed = gBestLocation.getSpeed();
			
			//if location reports accuracy and is accurate up to at least 100 meters - location is good
			if(gBestLocation.hasAccuracy() && gBestLocation.getAccuracy() < 100) {
				Log.d("LocationChanged", "Found accurate location fix! Accuracy="+gBestLocation.getAccuracy());
				gLocationManager.removeUpdates(locationListener);
			}
			
			
			String toast = "Speed: " + gSpeed + "m/s \nBearing: " + gBearing + " degrees E of N \nLong: "
					+ gLong + " \nLat: " + gLat;
			toastThis(toast, SHORT);
			if(firstRun)
			initOverlays(gBestLocation);
			else
			updateOverlays(gBestLocation);
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

		// If it's been more than two minutes since the current location, use the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
			return true;
		}
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
	private void getCachedLocation() {
		//Get cached location from GPS
		gBestLocation = gLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		//Compare cached GPS location to cached Network location, if it's better, use it
		if(isBetterLocation(gLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER), gBestLocation))
			gBestLocation = gLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
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
	
	public void updateOverlays(Location location) {
        // this is basically your code just a bit modified (removed unnecessary code and added new code)
        MapController mc = gMapView.getController();
        GeoPoint p = new GeoPoint((int)(gLat * 1e6), (int)(gLong * 1e6));

        //Remove all existing overlays
        List<Overlay> mapOverlays = gMapView.getOverlays();
        mapOverlays.clear();

      	Drawable drawable = this.getResources().getDrawable(R.drawable.ic_location);
      	CurrentLocationOverlay classOverlay = new CurrentLocationOverlay(drawable, this);
      	OverlayItem overlayitem = new OverlayItem(p, "Current Location", "You are here!");
      	
      	classOverlay.addOverlay(overlayitem);
      	mapOverlays.add(classOverlay);        
      	
      	mc.animateTo(p);
    }
	
	public void initOverlays(Location location) {
        // this is basically your code just a bit modified (removed unnecessary code and added new code)
        MapController mc = gMapView.getController();
        GeoPoint p = new GeoPoint((int)(gLat * 1e6), (int)(gLong * 1e6));

        //Remove all existing overlays
        List<Overlay> mapOverlays = gMapView.getOverlays();
        mapOverlays.clear();

      	Drawable drawable = this.getResources().getDrawable(R.drawable.ic_location);
      	CurrentLocationOverlay classOverlay = new CurrentLocationOverlay(drawable, this);
      	OverlayItem overlayitem = new OverlayItem(p, "Current Location", "You are here!");
      	
      	classOverlay.addOverlay(overlayitem);
      	mapOverlays.add(classOverlay);        
      	
      	mc.animateTo(p);
      	mc.zoomIn();
      	mc.zoomIn();
      	mc.zoomIn();
      	firstRun = false;
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
	
	
	
	
	
	
	/** Helper function for displaying a toast. Takes the string to be displayed and the length: LONG or SHORT **/
	private void toastThis(String toast, int duration) {
		Context context = getApplicationContext();
		Toast t = Toast.makeText(context, toast, duration);
		t.show();
	}
	
}
