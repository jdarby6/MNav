package com.eecs.mnav.google.directions;

import com.google.android.maps.GeoPoint;

public class Leg {

	public Step[] steps;
	public int distanceMeters;
	public String distanceText;
	public int durationSeconds;
	public String durationText;
	public String arrivalTime;
	public String departureTime;
	public GeoPoint startLocation;
	public GeoPoint endLocation;
}