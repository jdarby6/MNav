package com.eecs.mnav;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class BusIconOverlay extends ItemizedOverlay<OverlayItem> {

	private ArrayList<OverlayItem> myOverlays;

	public BusIconOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
		myOverlays = new ArrayList<OverlayItem>();
		//populate();
	}

	public void addOverlay(OverlayItem overlay){
		myOverlays.add(overlay);
	}

	@Override
	protected OverlayItem createItem(int i) {
		return myOverlays.get(i);
	}

	// Removes overlay item i
	public void removeItem(int i){
		if(i >= 0) myOverlays.remove(i);
		//populate();
	}

	// Handle tap events on overlay icons
	@Override
	protected boolean onTap(int i){

		/*	In this case we will just put a transient Toast message on the screen indicating that we have
		captured the relevant information about the overlay item.  In a more serious application one
		could replace the Toast with display of a customized view with the title, snippet text, and additional
		features like an embedded image, video, or sound, or links to additional information. (The lat and
		lon variables return the coordinates of the icon that was clicked, which could be used for custom
		positioning of a display view.)*/

		GeoPoint  gpoint = myOverlays.get(i).getPoint();
		double lat = gpoint.getLatitudeE6()/1e6;
		double lon = gpoint.getLongitudeE6()/1e6;
		String toast = "Title: "+myOverlays.get(i).getTitle();
		toast += "\nText: "+myOverlays.get(i).getSnippet();
		toast += 	"\nSymbol coordinates: Lat = "+lat+" Lon = "+lon+" (microdegrees)";
		Toast.makeText(StartActivity.context, toast, Toast.LENGTH_LONG).show();
		return(true);
	}

	// Returns present number of items in list
	@Override
	public int size() {
		return myOverlays.size();
	}
	
	public void populateIt() {
		this.populate();
	}
}