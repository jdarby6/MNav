package com.eecs.mnav;


import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

public class CurrentLocationOverlay extends ItemizedOverlay {
	//use m for "member" variables
    	  private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
    	  Context mContext;
    	  
    	  public CurrentLocationOverlay(Drawable defaultMarker) {
    		  super(boundCenterBottom(defaultMarker));
    		}
    	  
    	  public CurrentLocationOverlay(Drawable defaultMarker, Context context) {
    		  super(boundCenterBottom(defaultMarker));
    		  mContext = context;
    	  }
    	  
    	  public void addOverlay(OverlayItem overlay) {
    		    mOverlays.add(overlay);
    		    populate();
    		}
    	  
    	  @Override
    	  protected OverlayItem createItem(int i) {
    	    return mOverlays.get(i);
    	  }
    	  
    	  @Override
    	  public int size() {
    	    return mOverlays.size();
    	  }
    	  
    	  @Override
    	  protected boolean onTap(int index) {
    	    OverlayItem item = mOverlays.get(index);
    	    AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
    	    dialog.setTitle(item.getTitle());
    	    dialog.setMessage(item.getSnippet());
    	    dialog.show();
    	    return true;
    	  }
    
}