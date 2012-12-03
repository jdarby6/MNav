package com.eecs.mnav;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class BusRoutesActivity extends MapActivity {

	private MapController mapControl;
	private MapView mapView;
	private MyLocationOverlay myLocationOverlay;

	private List<Overlay> mapOverlays;
	private BusIconOverlay itemizedOverlay;

	private String location_feed_url = "http://mbus.pts.umich.edu/shared/location_feed.xml";
	List<MbusLocationFeedXmlParser.Item> items = null;

	private int m_interval = 4000;
	private Handler m_handler;

	String TAG = "GPStest";
	BusRouteOverlay route;   // This will hold the route segments
	boolean routeIsDisplayed = false;


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);  // Suppress title bar for more space
		setContentView(R.layout.activity_bus_routes);

		// Add map controller with zoom controls
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setSatellite(false);
		mapView.setTraffic(false);
		mapView.setBuiltInZoomControls(true);   // Set android:clickable=true in main.xml
		int maxZoom = mapView.getMaxZoomLevel();
		int initZoom = maxZoom-2;
		mapControl = mapView.getController();
		mapControl.setZoom(initZoom);

		myLocationOverlay = new MyLocationOverlay(this, mapView);
		mapView.getOverlays().add(myLocationOverlay);
		myLocationOverlay.enableMyLocation();

		m_handler = new Handler();
		startRepeatingTask();

	}

	@Override
	protected void onPause() {
		super.onPause();
		myLocationOverlay.disableMyLocation();
	}

	public void setOverlays() {
		/*URL url = new URL("http://mbus.pts.umich.edu/shared/"+)
		InputStream inputStream = url.openStream();
		Bitmap b = BitmapFactory.decodeStream(inputStream);
		b.setDensity(Bitmap.DENSITY_NONE);
		Drawable d = new BitmapDrawable(b);
		http://stackoverflow.com/questions/7361976/how-to-create-a-drawable-from-a-stream-without-resizing-it
		*/
		mapOverlays = mapView.getOverlays();
		itemizedOverlay = new BusIconOverlay(this.getResources().getDrawable(R.drawable.busred));
		for(int i = 0; i < items.size(); i++) {
			int lat = (int)(Integer.valueOf(items.get(i).latitude) * 1E6);
			int lon = (int)(Integer.valueOf(items.get(i).longitude) * 1E6);
			itemizedOverlay.addOverlay(new OverlayItem( new GeoPoint(lat, lon), "", ""));
		}
		mapOverlays.add(itemizedOverlay);
		itemizedOverlay.populateIt();
		mapView.postInvalidate();

	}

	// Required method since class extends MapActivity
	@Override
	protected boolean isRouteDisplayed() {
		return false;  // Don't display a route
	}

	Runnable m_statusChecker = new Runnable()
	{ 
		public void run() {
			Log.d("Ya", "I'm all runnable");
			new FetchTask(StartActivity.context).execute((Object)null);
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

	public class FetchTask extends AsyncTask<Object, Object, Object > {

		//private ProgressDialog dlg;
		private Context ctx;

		public FetchTask(Context context) {
			ctx = context;
		}

		@Override
		protected void onPreExecute() {
			/*dlg = new ProgressDialog(ctx);
			dlg.setMessage("Loading....");
			dlg.show();
			*/
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(Object result) {
			//dlg.dismiss();
			if ( result instanceof Exception ) {
				// show error message
			} else {
				// display data
			}
			super.onPostExecute(result);
		}

		@Override
		protected Object doInBackground(Object... params) {
			try {
				InputStream stream = null;
				// Instantiate the parser
				MbusLocationFeedXmlParser mbusLocationFeedXmlParser = new MbusLocationFeedXmlParser();

				try {
					Log.d("ya", "Trying to download locations");
					stream = downloadUrl(location_feed_url);        
					items = mbusLocationFeedXmlParser.parse(stream);
					// Makes sure that the InputStream is closed after the app is
					// finished using it.
				} finally {
					if (stream != null) {
						stream.close();
					} 
				}
			} catch (IOException e) {
				return "Connection error";
			} catch (XmlPullParserException e) {
				return "XML error";
			}
			return null;
		}
	}

	// Given a string representation of a URL, sets up a connection and gets
	// an input stream.
	private InputStream downloadUrl(String urlString) throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000 /* milliseconds */);
		conn.setConnectTimeout(15000 /* milliseconds */);
		conn.setRequestMethod("GET");
		conn.setDoInput(true);
		// Starts the query
		conn.connect();
		InputStream stream = conn.getInputStream();
		return stream;
	}
}
