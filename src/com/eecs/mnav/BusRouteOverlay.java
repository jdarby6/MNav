package com.eecs.mnav;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class BusRouteOverlay extends Overlay {
	private Paint paint;
	private ArrayList<GeoPoint> routePoints;
	private boolean routeIsActive;  
	private int numberRoutePoints;

	// Constructor permitting the route array to be passed as an argument.
	public BusRouteOverlay(ArrayList<GeoPoint> routePoints) {
		this.routePoints = routePoints;
		numberRoutePoints  = routePoints.size();
		routeIsActive = true;
	}

	// Method to turn route display on and off
	public void setRouteView(boolean routeIsActive){
		this.routeIsActive = routeIsActive;
	}

	public void setColor(int c){
		color = c;
	}

	private int color = 0;

	Paint.Style paintStyle = Paint.Style.STROKE;

	public void setFillStyle(Paint.Style style){
		paintStyle = style;
	}

	@Override
	public void draw(Canvas canvas, MapView mapview, boolean shadow) {
		super.draw(canvas, mapview, shadow);
		if(! routeIsActive) return;


		if(paint == null){
			paint = new Paint();

			paint.setAntiAlias(true);
			paint.setStrokeWidth(1);
			paint.setStyle(paintStyle);
			paint.setAntiAlias(true);
			paint.setARGB(255, 0, 0, 255);
			paint.setColor(color);
		}

		if(bitmap == null){

			wMin = Integer.MAX_VALUE;
			wMax = Integer.MIN_VALUE;
			hMin = Integer.MAX_VALUE;
			hMax = Integer.MIN_VALUE;

			lonMin = Integer.MAX_VALUE;
			lonMax = Integer.MIN_VALUE;
			latMin = Integer.MAX_VALUE;
			latMax = Integer.MIN_VALUE;

			Boolean newSegment = true;
			Point pt = new Point();

			GeoPoint point = null;

			ArrayList<Point> points = new ArrayList<Point>();

			for(int i=0; i<numberRoutePoints; i++){

				point = routePoints.get(i);

				int tempLat = point.getLatitudeE6();
				int tempLon = point.getLongitudeE6();

				if(tempLon<lonMin)lonMin = tempLon;
				if(tempLon>lonMax)lonMax = tempLon;
				if(tempLat<latMin)latMin = tempLat;
				if(tempLat>latMax)latMax = tempLat;

				mapview.getProjection().toPixels(routePoints.get(i), pt);

				points.add(new Point(pt.x,pt.y));

				if(pt.x<wMin)wMin = pt.x;
				if(pt.x>wMax)wMax = pt.x;
				if(pt.y<hMin)hMin = pt.y;
				if(pt.y>hMax)hMax = pt.y;

			}

			topLeftIn = new GeoPoint(latMax, lonMin);
			bottomRightIn = new GeoPoint(latMin, lonMax);
			int width = (wMax-wMin);
			int height = (hMax-hMin);           

			bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
			Canvas c = new Canvas(bitmap);

			Path bitmapPath = new Path();
			bitmapPath.incReserve(numberRoutePoints);

			newSegment = true;
			for(Point p : points){

				if (newSegment) {
					bitmapPath.moveTo(p.x - wMin, p.y - hMin);
					newSegment = false;
				} else {
					bitmapPath.lineTo(p.x - wMin, p.y - hMin);
				}

			}
			c.drawPath(bitmapPath, paint);

		}
		mapview.getProjection().toPixels(topLeftIn, topLeftOut);
		mapview.getProjection().toPixels(bottomRightIn, bottomRightOut);

		int l = topLeftOut.x;
		int t = topLeftOut.y;
		int r = bottomRightOut.x;
		int b = bottomRightOut.y; 

		Rect rect = new Rect(l,t,r,b);

		canvas.drawBitmap(bitmap, new Rect(0,0,bitmap.getWidth(),bitmap.getHeight()),rect,null);

	}
	GeoPoint topLeftIn = null;
	GeoPoint bottomRightIn = null;
	Point topLeftOut = new Point();
	Point bottomRightOut = new Point();

	Bitmap bitmap = null;
	int wMin = Integer.MAX_VALUE;
	int wMax = 0;
	int hMin = Integer.MAX_VALUE;
	int hMax = 0;

	int lonMin = Integer.MAX_VALUE;
	int lonMax = 0;
	int latMin = Integer.MAX_VALUE;
	int latMax = 0;

}