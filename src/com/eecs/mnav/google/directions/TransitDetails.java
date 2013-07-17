package com.eecs.mnav.google.directions;

import com.google.android.maps.GeoPoint;

public class TransitDetails {
	public GeoPoint arrivalCoord;
	public String arrivalStop;
	public GeoPoint departureCoord;
	public String departureStop;
	public String arrivalTime;
	public String departureTime;
	public String headsign;
	public Line transitLine;
}
