package com.eecs.mnav;


import java.util.List;

import com.parse.FindCallback;
import com.parse.GetDataCallback;
import com.parse.ParseFile;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;

public class BuildingMapActivity extends Activity{
	Bitmap floors[] = null;
	int curFloor = 0;
	ParseObject buildingMapObject;
	ParseFile buildingMap;
	TouchImageView touchImageMap;
	
	String mBuildingName = "adserv"; //Default because it was the first one I uploaded
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("OnCreate()", "OnCreate() called");
		setContentView(R.layout.activity_building_map);
		
		touchImageMap = (TouchImageView) findViewById(R.id.imageView_building_map);
		touchImageMap.setMaxZoom(3f);
	
		buildingMapObject = new ParseObject("BuildingMap");
		
		//Create query for my defined Parse class: BuildingMap
		ParseQuery query = new ParseQuery("BuildingMap");
		//Set constraints to look in COL name matching mBuildingName
		query.whereEqualTo("name", mBuildingName);
		query.addAscendingOrder("name");
		//query.whereExists("picture");
		query.findInBackground(new FindCallback() {
		    public void done(List<ParseObject> mapList, ParseException e) {
		        if (e == null) {
		            Log.d("Parse Import", "Retrieved " + mapList.size() + " maps");
		            //Resolve the list of building maps into array of bitmaps
		            resolveMaps(mapList);
		        } else {
		            Log.d("score", "Error: " + e.getMessage());
		        }
		    }
		});
	}
	
	
	//Method to take the list of parseObjects and resolve them into the global array of bitmaps: floors
	private void resolveMaps(List<ParseObject> parseMapList) {
		curFloor = 0;
		for(int i = 0; i < parseMapList.size(); i++){
		ParseObject tmpObj = parseMapList.get(i);
		if(tmpObj.has("picture")) {
			Log.d("Parse Import", "The buildingMapObject has key: picture");
		}
		ParseFile tmpFile = (ParseFile)tmpObj.get("picture");
		tmpFile.getDataInBackground(new GetDataCallback() {
		  public void done(byte[] data, ParseException e) {
		    if (e == null) {
		      // data has the bytes for the resume
		    	addFloor(BitmapFactory.decodeByteArray(data, 0, data.length));
		    } else {
		      // something went wrong
		    	Log.d("Parse Import", "There was a ParseException!\n"+e);
		    }
		  }
		});
		
		}
	
		touchImageMap.setImageBitmap(floors[curFloor]);
		Log.d("Parse Import", "Name of file: "+buildingMap.getName());
	}
	
	public void addFloor(Bitmap bmp) {
		floors[curFloor] = bmp;
		curFloor++;
	}
}
