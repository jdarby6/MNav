package com.eecs.mnav;

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

	public Segment() {}

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
		return copy;
	}
}
