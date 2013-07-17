package com.eecs.mnav.google.directions;

import com.google.android.maps.GeoPoint;

public class TransitRoute {
	public String summary;
	public Leg leg;
	//public int[] waypointOrder;
	public String overviewPolyline;
	public Bounds routeBounds;
	public String copyright;
	public String warning;
}