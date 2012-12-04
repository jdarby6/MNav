package com.eecs.mnav;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class BusRoutesActivity extends MapActivity {
	//Layout globals
	private MapView gMapView = null;
	private Button bSatellite;
	private Button bZoomIn;
	private Button bZoomOut;

	//Our location globals
	private double gDefaultLong = -83.72328;
	private double gDefaultLat = 42.27880;
	private int gDefaultZoom = 14;

	//Helper globals
	private MapController gMapController = null;
	private LocationManager gLocationManager;

	//Overlay globals
	private PinOverlay gPinOverlay = null;
	private RouteOverlay gRouteOverlay = null;
	private MyLocationOverlay gMyLocationOverlay = null;
	private ScaleBarOverlay gScaleBarOverlay = null;

	//Constants
	private static final int LONG = Toast.LENGTH_LONG;
	private static final int SHORT = Toast.LENGTH_SHORT;
	private static final int FOUR_SECONDS = 4000;
	private static final int LAYER_TYPE_SOFTWARE = 1;

	// Item == bus
	public static class Item {
		public String id = "";
		public String latitude = "";
		public String longitude = "";
		public String heading = "";
		public String route = "";
		public String routeid = "";
		public String busroutecolor = "";
	}
	static ArrayList<Item> items;
	static final String locationFeedURL = "http://mbus.pts.umich.edu/shared/location_feed.xml";
	private List<Overlay> mapOverlays;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bus_routes);

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
		zoomTo(new GeoPoint((int)(gDefaultLat*1E6), (int)(gDefaultLong*1E6)), gDefaultZoom);

		try {
			new GetLocationsTask().execute(new URL(locationFeedURL));
		} catch (MalformedURLException e) {
			Log.i("NETWORK", "Failed to generate valid URL");
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		//Initialize the map overlays (scale, currentLocation indicator)
		initOverlays();
		gMyLocationOverlay.enableMyLocation();
		//start gps;
	}

	@Override
	protected void onPause() {
		super.onPause();
		// when our activity pauses, we want to remove listening for location updates
		gMyLocationOverlay.disableMyLocation();
		//stop gps
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

	}	


	/*@Override
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
	}*/

	@Override
	protected boolean isRouteDisplayed() {
		return true;
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

		gMyLocationOverlay = new MyLocationOverlay(this, gMapView);
		mapOverlays.add(gMyLocationOverlay);

		gMyLocationOverlay.enableMyLocation();


		//Create the scalebar and add it to mapview
		gScaleBarOverlay = new ScaleBarOverlay(this.getBaseContext(), gMapView);
		gScaleBarOverlay.setImperial();
		mapOverlays.add(gScaleBarOverlay);
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
	}

	/** Helper function for displaying a toast. Takes the string to be displayed and the length: LONG or SHORT **/
	private void toastThis(String toast, int duration) {
		Context context = getApplicationContext();
		Toast t = Toast.makeText(context, toast, duration);
		t.show();
	}

	public void setBusOverlays() {
		mapOverlays = gMapView.getOverlays();	
		Drawable busIcon = this.getResources().getDrawable(R.drawable.busred); 
		BusIconOverlay busIconOverlay = new BusIconOverlay(busIcon); 
		// Display all buses at once
		for(int i = 0; i < items.size(); i++) {
			int lat = (int)(Double.parseDouble(items.get(i).latitude)*1E6);
			int lon = (int)(Double.parseDouble(items.get(i).longitude)*1E6);
			busIconOverlay.addOverlay(new OverlayItem(new GeoPoint(lat, lon), "", ""));
		}
		mapOverlays.add(busIconOverlay);
		busIconOverlay.populateIt();

		// Added symbols will be displayed when map is redrawn so force redraw now
		gMapView.postInvalidate(); 
	}

	private class GetLocationsTask extends AsyncTask<URL, String, String> {

		@Override
		protected String doInBackground(URL... params) {
			// This pattern takes more than one param but we'll just use the first
			try {
				URL text = params[0];

				XmlPullParserFactory parserCreator;

				parserCreator = XmlPullParserFactory.newInstance();

				XmlPullParser parser = parserCreator.newPullParser();

				parser.setInput(text.openStream(), null);

				publishProgress("Parsing XML...");

				int parserEvent = parser.getEventType();
				Item currentItem = new Item();
				BusRoutesActivity.items = new ArrayList<Item>();

				// Parse the XML returned on the network
				while (parserEvent != XmlPullParser.END_DOCUMENT) {
					switch (parserEvent) {
					case XmlPullParser.START_TAG:
						String tag = parser.getName();
						if(tag.compareTo("item") == 0) {
							parser.require(XmlPullParser.START_TAG, null, "item");
							while (parser.next() != XmlPullParser.END_TAG) {
								if (parser.getEventType() != XmlPullParser.START_TAG) {
									continue;
								}
								String name = parser.getName();
								if (name.equals("id")) {
									currentItem.id = readText(parser);
								} else if (name.equals("latitude")) {
									currentItem.latitude = readText(parser);
								} else if (name.equals("longitude")) {
									currentItem.longitude = readText(parser);
								} else if (name.equals("heading")) {
									currentItem.heading = readText(parser);
								} else if (name.equals("route")) {
									currentItem.route = readText(parser);
								} else if (name.equals("routeid")) {
									currentItem.routeid = readText(parser);
								} else if (name.equals("busroutecolor")) {
									currentItem.busroutecolor = readText(parser);
								}
							}
							BusRoutesActivity.items.add(currentItem);
							currentItem = new Item();
						}
						break;
					}
					parserEvent = parser.next();
				}
			} catch (Exception e) {
				Log.i("RouteLoader", "Failed in parsing XML", e);
				return "Finished with failure.";
			}

			return "Done...";
		}

		protected void onCancelled() {
			Log.i("GetLocationsTask", "GetLocations task Cancelled");
		}

		// Now that route data are loaded, execute the method to overlay the route on the map
		protected void onPostExecute(String result) {
			Log.i("GetLocationsTask", "Locations XML data transfer complete");
			setBusOverlays();
		}

		protected void onPreExecute() {
			Log.i("GetLocationsTask","Ready to load URL");
		}

		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
		}

	}

	private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
		String result = "";
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.getText();
			parser.nextTag();
		}
		return result;
	}

	/*
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
	 */
}
