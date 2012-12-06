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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

import com.eecs.mnav.MbusLocationFeedXmlParser.Item;
import com.eecs.mnav.MbusPublicFeedXmlParser.Route;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.slidingmenu.lib.app.SlidingMapActivity;

public class BusRoutesActivity extends SlidingMapActivity {
	//Layout globals
	private MapView gMapView = null;
	private Button bSlider;
	private Button bSatellite;
	private Button bTargetReticle;
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
	private static final int FOUR_SECONDS = 4000;
	private static final int LAYER_TYPE_SOFTWARE = 1;
	private static final int ZOOM_LEVEL_BUILDING = 19;

	static ArrayList<Item> items = new ArrayList<Item>();
	static ArrayList<Route> routes = new ArrayList<Route>();

	static final String locationFeedLink = "http://mbus.pts.umich.edu/shared/location_feed.xml";
	static final String publicFeedLink = "http://mbus.pts.umich.edu/shared/public_feed.xml";
	static final String stopsLink = "http://mbus.pts.umich.edu/shared/stop.xml";

	//Number of points to interpolate a move by (chop up the update of bus icon into this many parts
	//before fetching the XML again) - not implemented yet
	private int splitMove = 10;
	static BusIconOverlay busIconOverlay;

	private int m_interval = FOUR_SECONDS;
	private Handler m_handler;
	private ListView listView;
	private ListViewCustomAdapter routesListViewAdapter;


	@Override
	public void onCreate(Bundle savedInstanceState) { 
		super.onCreate(savedInstanceState);
		
		listView = new ListView(StartActivity.context);
		routesListViewAdapter = new ListViewCustomAdapter(this);
		listView.setAdapter(routesListViewAdapter);
		setContentView(R.layout.activity_bus_routes);
		setBehindContentView(listView);
		getSlidingMenu().setBehindOffset(100);
		
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
		
		bSlider = (Button) findViewById(R.id.button_slider);
		bSlider.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				BusRoutesActivity.this.getSlidingMenu().toggle();
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

			}
		});

		bTargetReticle = (Button) findViewById(R.id.button_return);
		bTargetReticle.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startGPS();
				//		GeoPoint currentLoc = new GeoPoint((int)(gCurrentLat * 1e6), (int)(gCurrentLong * 1e6));
				GeoPoint currentLoc = gMyLocationOverlay.getMyLocation();
				zoomTo(currentLoc, ZOOM_LEVEL_BUILDING);
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
		new GetXmlDataTask().execute((String[])null);
		m_handler = new Handler();
		startRepeatingTask();

	}

	@Override
	protected void onResume() {
		super.onResume();
		//Initialize the map overlays (scale, currentLocation indicator)
		initOverlays();
		gMyLocationOverlay.enableMyLocation();
		startRepeatingTask();
		//start gps;
	}

	@Override
	protected void onPause() {
		super.onPause();
		// when our activity pauses, we want to remove listening for location updates
		gMyLocationOverlay.disableMyLocation();
		stopRepeatingTask();
		//stop gps
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

	}	


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_bus_routes, menu);
		return true;
	}

	public boolean onOptionsItemSelected (MenuItem item) {
		switch (item.getItemId()){
		case R.id.menu_reset_map:
			zoomTo(new GeoPoint((int)(gDefaultLat*1E6), (int)(gDefaultLong*1E6)), gDefaultZoom);
			break;
		default:
			return false;
		}
		return true;
	}

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

	Runnable m_statusChecker = new Runnable()
	{ 
		public void run() {
			Log.d("Ya", "I'm all runnable");

			new GetXmlDataTask().execute((String[]) null);

			m_handler.postDelayed(m_statusChecker, m_interval);
		}
	};

	void startRepeatingTask()
	{
		m_statusChecker.run(); 
	}

	void stopRepeatingTask()
	{
		m_handler.removeCallbacks(m_statusChecker);
	}

	public void setBusOverlays() {
		//clear and re-add all overlays
		List<Overlay> mapOverlays = gMapView.getOverlays();
		mapOverlays.clear();
		mapOverlays.add(gMyLocationOverlay);
		mapOverlays.add(gScaleBarOverlay);
		Drawable busIcon = this.getResources().getDrawable(R.drawable.busred);
		//this overlay needs a drawable in its constructor, but we change it later
		busIconOverlay = new BusIconOverlay(busIcon); 

		try {
			// Display all buses at once
			for(int i = 0; i < items.size(); i++) {
				int lat = (int)(Double.parseDouble(items.get(i).latitude)*1E6);
				int lon = (int)(Double.parseDouble(items.get(i).longitude)*1E6);
				int heading = Integer.parseInt(items.get(i).heading);
				String routeid = items.get(i).routeid;
				String heading2 = getHeading2(heading);
				String imageString = "bus_route_"+routeid+"_heading_"+heading2+".png";
				busIcon = Drawable.createFromStream(getAssets().open("icons/"+imageString), imageString);
				busIcon.setBounds(-busIcon.getIntrinsicWidth(), -busIcon.getIntrinsicHeight(),
						busIcon.getIntrinsicWidth(), busIcon.getIntrinsicHeight());
				OverlayItem overlayItem = new OverlayItem(new GeoPoint(lat, lon), "", "");
				overlayItem.setMarker(busIcon);
				busIconOverlay.addOverlay(overlayItem);
			}
		} catch (Exception e) {
			Log.d("setBusOverlays", e.getMessage());
		}

		mapOverlays.add(busIconOverlay);
		busIconOverlay.populateIt();

		// Added symbols will be displayed when map is redrawn so force redraw now
		gMapView.postInvalidate(); 
	}

	/* Chooses the closest 45 degree angle at which to orient the bus based
	 * on its actual heading. MBus uses this value to choose the appropriate
	 * icon file.
	 */
	private String getHeading2(int heading) {
		int heading2 = (heading + 90) % 360;

		if(heading2 > 25 && heading2 < 65)
			return "45";
		else if(heading2 > 65 && heading2 < 115)
			return "90";
		else if(heading2 > 115 && heading2 < 155)
			return "135";
		else if(heading2 > 155 && heading2 < 205)
			return "180";
		else if(heading2 > 205 && heading2 < 245)
			return "225";
		else if(heading2 > 245 && heading2 < 290)
			return "270";
		else if(heading2 > 290 && heading2 < 340)
			return "315";
		else
			return "";
	}

	//Generalized AsyncTask for any XML parsing that needs to be done. 
	private class GetXmlDataTask extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... params) {
			String result = "";
			URL locationFeedURL;
			URL publicFeedURL;
			try {
				locationFeedURL = new URL(locationFeedLink);
				publicFeedURL = new URL(publicFeedLink);
			} catch (MalformedURLException e1) {
				Log.d("GetXmlDataTask", "Error with opening URL");
				e1.printStackTrace();

				return "Error with opening URL";
			}

			result = MbusLocationFeedXmlParser.parse(locationFeedURL);
			result = MbusPublicFeedXmlParser.parse(publicFeedURL);

			return result;
		}

		protected void onCancelled() {
			Log.i("GetXmlDataTask", "GetXml task Cancelled");
		}

		// Now that route data are loaded, execute the method to overlay the route on the map
		protected void onPostExecute(String result) {
			Log.i("GetXmlDataTask", "Locations and public feed XML data transfer complete");
			setBusOverlays();
			routesListViewAdapter.notifyDataSetChanged();
		}

		protected void onPreExecute() {
			Log.i("GetXmlDataTask","Ready to load URL");
		}

		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
		}

	}

	static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
		String result = "";
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.getText();
			parser.nextTag();
		}
		return result;
	}
}
