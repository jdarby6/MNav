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
    private int length;
    private String polyline;

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

    public void setLength(int length) {
            this.length = length;
    }

    public int getLength() {
            return length;
    }

    public void setPolyline(String polyline) {
            this.polyline = polyline;
    }

    public String getPolyline() {
            return polyline;
    }

}