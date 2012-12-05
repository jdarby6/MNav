package com.eecs.mnav;


import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.jakewharton.DiskLruCache;
import com.parse.FindCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ProgressCallback;

public class BuildingMapActivity extends Activity{
	ArrayList<Bitmap> floors = new ArrayList<Bitmap>();
	private DiskLruCache bitmapCache;
	TouchImageView touchImageViewMap;

	int curFloor = 0;
	int numFloors = 0;
	String gDestName_full = "";
	String gDestName = "";
	String gDestRoom = "";
	TextView tvTitle;
	TextView tvFloorNum;
	TextView tvRoomNum;
	boolean mapsNotAvailable = true;
	ParseObject buildingMapObject;
	Button bStairsUp;
	Button bStairsDown;
	SharedPreferences gPreferences;
	ProgressDialog gProgressDialog;
	String mBuildingName = "default"; //Default to saying we don't have the map
	
	boolean curFloorSet = false;
	boolean hasBasement = false;


	private static final int DISK_CACHE_SIZE = 5 * 1024 * 1024; // 5MiB ~ This equates to about 5 sets of 5 building maps of EECS map quality
	private static final String DISK_CACHE_SUBDIR = "building_maps";
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("OnCreate()", "OnCreate() called");
		setContentView(R.layout.activity_building_map);

		tvTitle = (TextView) findViewById(R.id.textView_building_title);
		tvRoomNum = (TextView) findViewById(R.id.textView_class_num);
		tvFloorNum = (TextView) findViewById(R.id.textView_floor_num);
		touchImageViewMap = (TouchImageView) findViewById(R.id.imageView_building_map);
		touchImageViewMap.setMaxZoom(3f);
		
		
		gProgressDialog = new ProgressDialog(this);
		gProgressDialog.setMessage("Establishing Connection...");
		gProgressDialog.setCancelable(true);
		gProgressDialog.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				finish();
			}
		});
		gProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		gProgressDialog.setProgress(0); // set percentage completed to 0%
		gProgressDialog.show();
		
		//Load stored data
		gPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		//Load last known latitude, longitude default is the Diag
		gDestName_full = gPreferences.getString("DESTNAMEFULL", "");
		gDestName = gPreferences.getString("DESTNAME", "");
		gDestRoom = gPreferences.getString("DESTROOM", "");
		
		tvTitle.setText(gDestName_full);
		Log.d("BuildingMapActivity", "Name_full:"+gDestName_full+" Name:"+gDestName+" RoomNum:"+gDestRoom);
		if(!gDestRoom.equals(""))
			tvRoomNum.setText("Looking for "+gDestRoom+" "+gDestName);
		else
			tvRoomNum.setText("");
		
		/*XXX/Define the cache
		   File cacheDir = getDir(DISK_CACHE_SUBDIR, 0);
		   //We'll be using the destName abbreviation for the cache key
		    /**
		     * Opens the cache in {@code directory}, creating a cache if none exists
		     * there.
		     *
		     * @param directory a writable directory
		     * @param appVersion
		     * @param valueCount the number of values per cache entry. Must be positive.
		     * @param maxSize the maximum number of bytes this cache should use to store
		     * @throws IOException if reading or writing the cache directory fails
		     *
		   try {
			bitmapCache = DiskLruCache.open(cacheDir, 100, 1, DISK_CACHE_SIZE);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/

		buildingMapObject = new ParseObject("BuildingMap");

		//Create query for my defined Parse class: BuildingMap
		ParseQuery query = new ParseQuery("BuildingMap");
		//Set constraints to look in COL name matching mBuildingName
		setBuildingName();
		query.whereEqualTo("name", mBuildingName.toLowerCase());
		query.addAscendingOrder("name");
		query.setCachePolicy(ParseQuery.CachePolicy.CACHE_ELSE_NETWORK);
		query.findInBackground(new FindCallback() {
			public void done(List<ParseObject> mapList, ParseException e) {
				if (e == null) {
					Log.d("Parse Import", "Retrieved " + mapList.size() + " maps");
					numFloors = mapList.size();
					//Check to see if we got any maps from Parse. If not, set the bool and we'll display the default map later
					if (numFloors == 0) {
						mapsNotAvailable = true;
					}
					else 
						mapsNotAvailable = false;
					
					//Make the floors array big enough to hold our maps
					while(floors.size() < numFloors+1) { //numFloors + 1 will allow 0 basement floors
						floors.add(null);
					}
					//Resolve the list of building maps into array of bitmaps
					curFloor = 0; //Initialize curFloor to 0 so we can use it as an index in resolveMaps();
					resolveMaps(mapList);
				} else {
					Log.d("Parse Import", "Error: " + e.getMessage());
					mapsNotAvailable = true;
					resolveMaps(mapList);
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
		if(mapsNotAvailable) {
			touchImageViewMap.setBackgroundDrawable(getResources().getDrawable(R.drawable.defaultmap));
			gProgressDialog.dismiss();
			bStairsDown.setEnabled(false);
			bStairsUp.setEnabled(false);
			return;
		}
		gProgressDialog.setMessage("Loading Map "+(curFloor+1)+" of "+numFloors+"...");
		ParseObject tmpObj = parseMapList.get(curFloor);
		if(tmpObj.has("picture")) {
			final ParseFile tmpFile = (ParseFile)tmpObj.get("picture");
			final String filename = tmpFile.getName();
			final String key = filename.substring(filename.lastIndexOf('-')+1, filename.length()-4);
			Log.d("Parse Import", "Name of file: "+filename+"\nKey used: "+key);
			
			tmpFile.getDataInBackground(new GetDataCallback() {
				public void done(byte[] data, ParseException e) {
					if (e == null) {
						// data has the bytes for the floor map
						addFloor(Character.getNumericValue(filename.charAt(filename.length()-5)), BitmapFactory.decodeByteArray(data, 0, data.length), key);
						curFloor++; //Increment the current floor after adding a floor
						if(curFloor == numFloors) {
							//At this point, assumed all maps are in so prepare the UI accordingly and release to user
							Log.d("Parse Import", "curFloor="+curFloor);
							setCurFloor();
							Log.d("Parse Import", "setCurFloor to: "+curFloor);
							touchImageViewMap.setImageBitmap(floors.get(curFloor));
							bStairsUp.setEnabled(true);
							bStairsDown.setEnabled(true);
							//Check if the basement map was included by seeing if it was added or not
							if(floors.get(0) == null){
								hasBasement = false;
								bStairsDown.setEnabled(false);
							} else if (floors.get(floors.size()-1)==null) {
								floors.remove(floors.size()-1);
								hasBasement = true;
							}
							return;
						} else {
							//At this point, assumed 1 or more maps are still waiting to be grabbed
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
					  	if(gProgressDialog.getProgress() >= 100){
					  		Log.d("Parse Import", "Loading Progress curFloor="+curFloor+" numFloors="+numFloors);
					  		if(curFloor == numFloors-1 || (curFloor == 1 && curFloorSet))					  				
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
	public void addFloor(int pos, Bitmap bmp, String key) {
		Log.d("addFloor()", "Adding floor to pos="+pos+". This is map number "+curFloor);
		floors.set(pos, bmp);
	//XXX	addBitmapToMemoryCache(key, bmp);
	}

	/** Method to figure out what current floor we should display to the user **/
	private void setCurFloor() {
		curFloor = 1; //Start off on the first floor. TODO
		curFloorSet = true;
		setFloorTextView(curFloor);
	}

	/** Method to set the building name to look for a set of maps **/
	private void setBuildingName() {
		mBuildingName = gDestName;
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
			touchImageViewMap.setImageBitmap(floors.get(curFloor));
  			setFloorTextView(curFloor);
			touchImageViewMap.invalidate();
			Log.d("BuildingMapActivity", "Went up! curFloor="+curFloor);
		}
	}

	/** Method to call when down button pressed **/
	private void goDownstairs() {
		Log.d("BuildingMapActivity", "Trying to go Downstairs! curFloor="+curFloor+" floors size="+floors.size());
		if(curFloor == 2 && !hasBasement) { //when pressed, curFloor was at 2. disable going further down if no basement
			bStairsDown.setEnabled(false);
		} else if (curFloor == 1 && hasBasement) {
			bStairsDown.setEnabled(false);
		}
		if (curFloor == 0){ //safety check, should never get here
			Log.d("BuildingMapActivity", "BUMP! Can't go further..");
			return;
		}
		else{
			if(!bStairsUp.isEnabled()) {
				bStairsUp.setEnabled(true);
			}
			curFloor--;
			touchImageViewMap.setImageBitmap(floors.get(curFloor));
  			setFloorTextView(curFloor);
			touchImageViewMap.invalidate();
			Log.d("BuildingMapActivity", "Went down! curFloor="+curFloor);
		}
	}
	
	/*public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
	    if (getBitmapFromMemCache(key) == null) {
	    	Log.d("LRUCache","Added "+key+" to cache.");
	        bitmapCache.put(key, bitmap);
	    }
	    throws IOException, FileNotFoundException {
	        OutputStream out = null;
	        try {
	            out = new BufferedOutputStream( editor.newOutputStream( 0 ), 8*1024 );
	            return bitmap.compress( mCompressFormat, mCompressQuality, out );
	        } finally {
	            if ( out != null ) {
	                out.close();
	            }
	        }
	}

	public Bitmap getBitmapFromMemCache(String key) {
		 Bitmap bitmap = null;
	     DiskLruCache.Snapshot snapshot = null;
	     try {

	            snapshot = bitmapCache.get( key );
	            if ( snapshot == null ) {
	                return null;
	            }
	            final InputStream in = snapshot.getInputStream( 0 );
	            if ( in != null ) {
	                final BufferedInputStream buffIn = 
	                new BufferedInputStream( in, 8*1024 );
	                bitmap = BitmapFactory.decodeStream( buffIn );              
	            }   
	        } catch ( IOException e ) {
	            e.printStackTrace();
	        } finally {
	            if ( snapshot != null ) {
	                snapshot.close();
	            }
	        }

	        if ( BuildConfig.DEBUG ) {
	            Log.d( "cache_test_DISK_", bitmap == null ? "" : "image read from disk " + key);
	        }

	        return bitmap;
	}
*/	
	
	private void setFloorTextView(int floor) {
		String temp = "";
		if(floor == 0)
			temp = "Basement";
		else if(floor%10==1)
			temp = floor+"st Floor";
		else if(floor%10==2)
			temp = floor+"nd Floor";
		else if(floor%10==3)
			temp = floor+"rd Floor";
		else if(floor%10>=4 || floor%10==0)
			temp = floor+"th Floor";
		
		tvFloorNum.setText(temp);
	}
}
