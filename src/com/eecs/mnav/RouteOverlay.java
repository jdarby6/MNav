package com.eecs.mnav;

import java.util.Iterator;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

class RouteOverlay extends Overlay {
    private final List<GeoPoint> routePoints;
    private int colour;
    private static final int ALPHA = 120;
    private static final float STROKE = 4.5f;
    private final Path path;
    private final Point p;
    private final Paint paint;

    public RouteOverlay(final Route route, final int defaultColour) {
            super();
            routePoints = route.getPoints();
            colour = defaultColour;
            path = new Path();
            p = new Point();
            paint = new Paint();
    }

    @Override
    public final void draw(final Canvas c, final MapView mv,
                    final boolean shadow) {
            super.draw(c, mv, shadow);

            paint.setColor(colour);
            paint.setAlpha(ALPHA);
            paint.setAntiAlias(true);
            paint.setStrokeWidth(STROKE);
            paint.setStyle(Paint.Style.STROKE);

            redrawPath(mv);
            c.drawPath(path, paint);
    }

    public final void setColor(final int c) {
            colour = c;
    }

    public final void clear() {
            routePoints.clear();
    }

    private void redrawPath(final MapView mv) {
            final Projection prj = mv.getProjection();
            path.rewind();
            final Iterator<GeoPoint> it = routePoints.iterator();
            prj.toPixels(it.next(), p);
            path.moveTo(p.x, p.y);
            while (it.hasNext()) {
                    prj.toPixels(it.next(), p);
                    path.lineTo(p.x, p.y);
            }
            path.setLastPoint(p.x, p.y);
    }

}
