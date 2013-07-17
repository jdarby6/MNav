package com.eecs.mnav.google.directions;

import com.google.android.maps.GeoPoint;

public class Step {
	public String htmlInstructions;
	public String distanceText;
	public int distanceMeters;
	public String durationText;
	public GeoPoint startLocation;
	public GeoPoint endLocation;
	public Step[] subSteps;
	public TransitDetails transDetails;
	public String polyline;
	public String travelMode;
}