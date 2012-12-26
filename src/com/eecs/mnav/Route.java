package com.eecs.mnav;

import java.util.ArrayList;
import java.util.List;

import com.google.android.maps.GeoPoint;

public class Route {
	private String name;
	private final List<GeoPoint> points;
	private List<Segment> segments;
	private String copyright;
	private String warning;
	private String country;
	private String distance;
	private String duration;
	private String polyline;
	//Transit specific
	private String arrivalTime;
	private String departureTime;

	public Route() {
		points = new ArrayList<GeoPoint>();
		segments = new ArrayList<Segment>();
	}

	public void addPoint(final GeoPoint p) {
		points.add(p);
	}

	public void addPoints(final List<GeoPoint> points) {
		this.points.addAll(points);
	}

	public List<GeoPoint> getPoints() {
		return points;
	}

	public void addSegment(final Segment s) {
		segments.add(s);
	}

	public List<Segment> getSegments() {
		return segments;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setCopyright(String copyright) {
		this.copyright = copyright;
	}

	public String getCopyright() {
		return copyright;
	}

	public void setWarning(String warning) {
		this.warning = warning;
	}

	public String getWarning() {
		return warning;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCountry() {
		return country;
	}

	public void setDistance(String distance) {
		this.distance = distance;
	}

	public String getDistance() {
		return distance;
	}
	
	public void setDuration(String time) {
		this.duration = time;
	}
	
	public String getDuration() {
		return duration;
	}

	public void setPolyline(String polyline) {
		this.polyline = polyline;
	}

	public String getPolyline() {
		return polyline;
	}
	
	public void setArrivalTime(String arrTime) {
		this.arrivalTime = arrTime;
	}
	
	public String getArrivalTime() {
		return arrivalTime;
	}

	public void setDepartureTime(String deptTime){
		this.departureTime = deptTime;
	}
	
	
}