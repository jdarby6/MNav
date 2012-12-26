package com.eecs.mnav;


import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class CurrentRouteOverlay extends ItemizedOverlay<OverlayItem> {
	//use m for "member" variables
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	Context mContext;
	MainMapActivity gMainActivity = null;


	public CurrentRouteOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
	}

	public CurrentRouteOverlay(Drawable defaultMarker, Context context) {
		super(boundCenterBottom(defaultMarker));
		mContext = context;
	}

	public void setTapListener(MainMapActivity m) {
		gMainActivity = m;
	}

	public void removeTapListener() {
		gMainActivity = null;
	}

	public void addOverlay(OverlayItem overlay) {
		mOverlays.add(overlay);
		populate();
	}

	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	public void replaceOverlay(OverlayItem overlay, int i) {
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
				gMainActivity.showDialog(MainMapActivity.DIALOG_DESTINATION_BLDG);
			}
			return true;
		}
	}



}