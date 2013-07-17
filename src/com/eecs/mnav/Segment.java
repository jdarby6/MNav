package com.eecs.mnav;

import java.util.ArrayList;
import java.util.List;

import com.google.android.maps.GeoPoint;

public class Segment {
	private GeoPoint start;
	/** Turn instruction to reach next segment. **/
	private String instruction;
	/** Length of segment. **/
	private int length;
	/** Distance covered. **/
	private double distance;
	/** Mode of transit **/
	private String transitMode;
	private final List<GeoPoint> points;

	public Segment() {
		points = new ArrayList<GeoPoint>();
	}

	public List<GeoPoint> getPoints() {return points;}
	public void addPoint(final GeoPoint p) {points.add(p);}
	public void addPoints(final List<GeoPoint> points) {this.points.addAll(points);}
	
	public String getTransitMode() {return transitMode;}
	public void setTransitMode(final String trans) {this.transitMode = trans;}

	public String getInstruction() {return instruction;}
	public void setInstruction(final String turn) {this.instruction = turn;}

	public void setDistance(double distance) {this.distance = distance;}

	public GeoPoint getStartPoint() {return start;}
	public void setStartPoint(final GeoPoint point) {start = point;}

	public int getLength() {return length;}
	public void setLength(final int length) {this.length = length;}

	public double getDistance() {return distance;}

	public Segment copy() {
		final Segment copy = new Segment();
		copy.start = start;
		copy.instruction = instruction;
		copy.length = length;
		copy.distance = distance;
		copy.transitMode = transitMode;
		copy.addPoints(points);
		return copy;
	}
}
