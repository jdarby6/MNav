package com.eecs.mnav;


import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class PinOverlay extends ItemizedOverlay<OverlayItem> {
	//use m for "member" variables
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	Context mContext;
	MNavMainActivity gMainActivity = null;
	
	

	public PinOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
	}

	public PinOverlay(Drawable defaultMarker, Context context) {
		super(boundCenterBottom(defaultMarker));
		mContext = context;
	}

	public void setTapListener(MNavMainActivity m) {
		gMainActivity = m;
	}

	public void removeTapListener() {
		gMainActivity = null;
	}

	public void addOverlay(OverlayItem overlay) {
		mOverlays.add(overlay);
		populate();
	}

	public void addOverlayNoPopulate(OverlayItem overlay) {
		mOverlays.add(overlay);
	}
	
	public void populateOverlay(){
		populate();
	}
	
	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}
	
	public void replaceOverlayByIndex(OverlayItem overlay, int i) {
		mOverlays.remove(i);
		mOverlays.add(i, overlay);
		populate();
	}

	@Override
	public int size() {
		return mOverlays.size();
	}

	@Override
	protected boolean onTap(int index) {
		OverlayItem item = mOverlays.get(index);
		if(item.getTitle().equals("Current Location")) {
			AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
			dialog.setTitle(item.getTitle());
			dialog.setMessage(item.getSnippet());
			dialog.show();
			return true;
		}
		else {
			//Display the destination building dialog
			if(gMainActivity != null) {
				gMainActivity.showDialog(MNavMainActivity.DIALOG_DESTINATION_BLDG);
			}
			return true;
		}
	}



}