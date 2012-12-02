package com.eecs.mnav;


import java.util.ArrayList;
import java.util.List;

import com.parse.FindCallback;
import com.parse.GetDataCallback;
import com.parse.ParseFile;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ProgressCallback;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class BuildingMapActivity extends Activity{
	ArrayList<Bitmap> floors = new ArrayList<Bitmap>();
	int curFloor = 0;
	int numFloors = 0;
	ParseObject buildingMapObject;
	TouchImageView touchImageMap;
	Button bStairsUp;
	Button bStairsDown;
	ProgressDialog gProgressDialog;

	String mBuildingName = "default"; //Default to saying we don't have the map

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("OnCreate()", "OnCreate() called");
		setContentView(R.layout.activity_building_map);

		gProgressDialog = new ProgressDialog(this);
		gProgressDialog.setMessage("Establishing Connection...");
		gProgressDialog.setCancelable(false);
		gProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		gProgressDialog.setProgress(0); // set percentage completed to 0%
		gProgressDialog.show();
		
		touchImageMap = (TouchImageView) findViewById(R.id.imageView_building_map);
		touchImageMap.setMaxZoom(3f);

		buildingMapObject = new ParseObject("BuildingMap");

		//Create query for my defined Parse class: BuildingMap
		ParseQuery query = new ParseQuery("BuildingMap");
		//Set constraints to look in COL name matching mBuildingName
		setBuildingName();
		query.whereEqualTo("name", mBuildingName);
		query.addAscendingOrder("name");
		query.findInBackground(new FindCallback() {
			public void done(List<ParseObject> mapList, ParseException e) {
				if (e == null) {
					Log.d("Parse Import", "Retrieved " + mapList.size() + " maps");
					numFloors = mapList.size();
					//Make the floors array big enough to hold our maps
					while(floors.size() < numFloors) {
						floors.add(null);
					}
					//Resolve the list of building maps into array of bitmaps
					curFloor = 0; //Initialize curFloor to 0 so we can use it as an index in resolveMaps();
					resolveMaps(mapList);
				} else {
					Log.d("score", "Error: " + e.getMessage());
				}
			}
		});


		bStairsUp = (Button) findViewById(R.id.button_stairsup);
		bStairsUp.setEnabled(false); //Disable until maps are loaded
		bStairsUp.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				goUpstairs();
			}
		});

		bStairsDown = (Button) findViewById(R.id.button_stairsdown);
		bStairsDown.setEnabled(false); //Disable until maps are loaded
		bStairsDown.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				goDownstairs();
			}
		});	
	}


	//Method to take the list of parseObjects and resolve them into the global array of bitmaps: floors
	private void resolveMaps(final List<ParseObject> parseMapList) {
		int tmp = curFloor+1;
		gProgressDialog.setMessage("Loading Map "+tmp+" of "+numFloors+"...");
		ParseObject tmpObj = parseMapList.get(curFloor);
		if(tmpObj.has("picture")) {
			Log.d("Parse Import", "The buildingMapObject has key: picture");
			final ParseFile tmpFile = (ParseFile)tmpObj.get("picture");
			Log.d("Parse Import", "Name of file: "+tmpFile.getName());
			tmpFile.getDataInBackground(new GetDataCallback() {
				public void done(byte[] data, ParseException e) {
					if (e == null) {
						// data has the bytes for the floor map
						addFloor(Character.getNumericValue(tmpFile.getName().charAt(tmpFile.getName().length()-5)), BitmapFactory.decodeByteArray(data, 0, data.length));
						curFloor++; //Increment the current floor after adding a floor
						if(curFloor == numFloors) {
							Log.d("Parse Import", "curFloor="+curFloor);
							setCurFloor();
							Log.d("Parse Import", "setCurFloor to: "+curFloor);
							touchImageMap.setImageBitmap(floors.get(curFloor));
							bStairsUp.setEnabled(true);
							bStairsDown.setEnabled(true);
							return;
						} else {
							resolveMaps(parseMapList); //Recursive call upon grabbing data and putting it in arrayList
						}
					} else {
						// something went wrong
						Log.d("Parse Import", "There was a ParseException!\n"+e);
					}
				}
			},  new ProgressCallback() {
				  public void done(Integer percentDone) {
					  	gProgressDialog.incrementProgressBy(percentDone - gProgressDialog.getProgress());
					  	if(gProgressDialog.getProgress() == 100){
					  		Log.d("Parse Import", "Loading Progress curFloor="+curFloor+" numFloors="+numFloors);
					  		if(curFloor == numFloors-1)					  				
					  			gProgressDialog.dismiss();
					  		else {
					  			gProgressDialog.setProgress(0);
					  		}
					  	}
					  }
			});
		}
	}

	/** Method to call which add floors to the floors arrayList and increments curFloor**/
	public void addFloor(int pos, Bitmap bmp) {
		Log.d("addFloor()", "Adding floor to pos="+pos+". This is map number "+curFloor);
		floors.set(pos, bmp);
	}

	/** Method to figure out what current floor we should display to the user **/
	private void setCurFloor() {
		curFloor = 1; //Start off on the first floor. TODO
	}

	/** Method to set the building name to look for a set of maps **/
	private void setBuildingName() {
		//mBuildingName = code to get name from previous activity; TODO
		mBuildingName = "eecs";
	}

	/**Method to call when up button pressed **/
	private void goUpstairs() {
		Log.d("BuildingMapActivity", "Trying to go Upstairs! curFloor="+curFloor+" floors size="+floors.size());
		if(curFloor == floors.size() - 2) {
			bStairsUp.setEnabled(false);
		}
		if(curFloor == (floors.size() - 1)){
			Log.d("BuildingMapActivity", "BUMP! Can't go further..");
			return;
		}
		else{
			if(!bStairsDown.isEnabled()) {
				bStairsDown.setEnabled(true);
			}
			curFloor++;
			touchImageMap.setImageBitmap(floors.get(curFloor));
			touchImageMap.invalidate();
			Log.d("BuildingMapActivity", "Went up! curFloor="+curFloor);
		}
	}

	/** Method to call when down button pressed **/
	private void goDownstairs() {
		Log.d("BuildingMapActivity", "Trying to go Downstairs! curFloor="+curFloor+" floors size="+floors.size());
		if(curFloor == 1) {
			bStairsDown.setEnabled(false);
		}
		if (curFloor == 0){
			Log.d("BuildingMapActivity", "BUMP! Can't go further..");
			return;
		}
		else{
			if(!bStairsUp.isEnabled()) {
				bStairsUp.setEnabled(true);
			}
			curFloor--;
			touchImageMap.setImageBitmap(floors.get(curFloor));
			touchImageMap.invalidate();
			Log.d("BuildingMapActivity", "Went down! curFloor="+curFloor);
		}
	}
}
