package com.eecs.mnav;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.eecs.mnav.google.directions.Leg;
import com.eecs.mnav.google.directions.Line;
import com.eecs.mnav.google.directions.Step;
import com.eecs.mnav.google.directions.TransitDetails;
import com.eecs.mnav.google.directions.TransitRoute;
import com.google.android.maps.GeoPoint;

public class GoogleParser {
	private int distance;
	private URL feedURL;
	public GoogleParser(String feedUrl) {
		try {
			this.feedURL = new URL(feedUrl);
		} catch (MalformedURLException e) {
		}
	}

	/**
	 * Parses a url pointing to a Google JSON object to a Route object.
	 * @return a Route object based on the JSON object.
	 */

	public Route parseWalking() {
		// turn the stream into a string
		final String result = convertStreamToString(this.getInputStream());
		//Create an empty route
		final Route route = new Route();
		//Create an empty segment
		final Segment segment = new Segment();
		try {
			//Tranform the string into a json object
			final JSONObject json = new JSONObject(result);
			//Get the route object
			final JSONObject jsonRoute = json.getJSONArray("routes").getJSONObject(0);
			//Get the leg, only one leg as we don't support waypoints
			final JSONObject leg = jsonRoute.getJSONArray("legs").getJSONObject(0);
			//Get the steps for this leg
			final JSONArray steps = leg.getJSONArray("steps");
			//Number of steps for use in for loop
			final int numSteps = steps.length();
			//Set the name of this route using the start & end addresses
			route.setName(leg.getString("start_address") + " to " + leg.getString("end_address"));
			//Get google's copyright notice (tos requirement)
			route.setCopyright(jsonRoute.getString("copyrights"));
			//Get the total length of the route.
			route.setDistance(leg.getJSONObject("distance").getString("text"));
			//Get the total duration of the route.
			route.setDuration(leg.getJSONObject("duration").getString("text"));
			//Get any warnings provided (tos requirement)
			if (!jsonRoute.getJSONArray("warnings").isNull(0)) {
				route.setWarning(jsonRoute.getJSONArray("warnings").getString(0));
			}
			/* Loop through the steps, creating a segment for each one and
			 * decoding any polylines found as we go to add to the route object's
			 * map array. Using an explicit for loop because it is faster!
			 */
			Log.d("GoogleParser", "Number of steps:"+numSteps);
			for (int i = 0; i < numSteps; i++) {
				//Get the individual step
				final JSONObject step = steps.getJSONObject(i);
				//Get the start position for this step and set it on the segment
				final JSONObject start = step.getJSONObject("start_location");
				final GeoPoint position = new GeoPoint((int) (start.getDouble("lat")*1E6), 
						(int) (start.getDouble("lng")*1E6));
				segment.setStartPoint(position);
				//Set the length of this segment in metres
				final int length = step.getJSONObject("distance").getInt("value");
				distance += length;
				segment.setLength(length);
				segment.setDistance(distance/1000);
				//Strip html from google directions and set as turn instruction
				segment.setInstruction(step.getString("html_instructions").replaceAll("<(.*?)*>", ""));
				Log.d("GoogleParser", "Instruction "+i+":"+segment.getInstruction());
				//Retrieve & decode this segment's polyline and add it to the route.
				route.addPoints(decodePolyLine(step.getJSONObject("polyline").getString("points")));
				//Push a copy of the segment to the route
				route.addSegment(segment.copy());
			}
		} catch (JSONException e) {
			Log.e(e.getMessage(), "Google JSON Parser - " + feedURL);
		}
		return route;
	}


	/**
	 * Parses a url pointing to a Google JSON object to a Route object.
	 * @return a Route object based on the JSON object.
	 */

	public TransitRoute parseTransit() {
		Log.d("GOOGLE PARSER - NEW", "I AM INSIDE THE PARSE TRANSIT!!!!");
		Log.d("GOOGLE PARSER - NEW", "I AM INSIDE THE PARSE TRANSIT!!!!");
		Log.d("GOOGLE PARSER - NEW", "I AM INSIDE THE PARSE TRANSIT!!!!");
		Log.d("GOOGLE PARSER - NEW", "I AM INSIDE THE PARSE TRANSIT!!!!");
		Log.d("GOOGLE PARSER - NEW", "I AM INSIDE THE PARSE TRANSIT!!!!");
		Log.d("GOOGLE PARSER - NEW", "I AM INSIDE THE PARSE TRANSIT!!!!");
		Log.d("GOOGLE PARSER - NEW", "I AM INSIDE THE PARSE TRANSIT!!!!");
		// turn the stream into a string
		final String result = convertStreamToString(this.getInputStream());
		//Create an empty route
		final TransitRoute route = new TransitRoute();

		try {
			final JSONObject json = new JSONObject(result);
			final JSONObject jsonRoute = json.getJSONArray("routes").getJSONObject(0);
			if(!jsonRoute.isNull("bounds")){
		/*		int swLat = (int) (jsonRoute.getJSONObject("bounds").getJSONObject("southwest").getDouble("lat")*1E6);
				int swLon = (int) (jsonRoute.getJSONObject("bounds").getJSONObject("southwest").getDouble("lng")*1E6);
				GeoPoint sw = new GeoPoint(swLat,swLon);
				int neLat = (int) (json.getJSONObject("bounds").getJSONObject("northeast").getDouble("lat")*1E6);
				int neLon = (int) (json.getJSONObject("bounds").getJSONObject("northeast").getDouble("lng")*1E6);
				GeoPoint ne = new GeoPoint(neLat,neLon);
				route.routeBounds.southwest = sw;
			
				route.routeBounds.northeast = ne;*/
			} else {
				route.routeBounds = null;
			}
			route.copyright = jsonRoute.getString("copyrights");
			route.overviewPolyline = jsonRoute.getJSONObject("overview_polyline").getString("points");
			if (!jsonRoute.getJSONArray("warnings").isNull(0)) {
				route.warning = jsonRoute.getJSONArray("warnings").getString(0);
			}
			//TODO: Support waypoints if this parser works....
			//Get the leg, only one leg as we don't support waypoints
			final JSONObject leg = jsonRoute.getJSONArray("legs").getJSONObject(0);
			final Leg transitLeg = new Leg();
			transitLeg.arrivalTime = leg.getJSONObject("arrival_time").getString("text");
			transitLeg.departureTime = leg.getJSONObject("departure_time").getString("text");
			transitLeg.distanceMeters = leg.getJSONObject("distance").getInt("value");
			transitLeg.distanceText = leg.getJSONObject("distance").getString("text");
			transitLeg.durationSeconds = leg.getJSONObject("duration").getInt("value");
			transitLeg.durationText = leg.getJSONObject("duration").getString("text");
			int startLat = (int) (leg.getJSONObject("start_location").getDouble("lat")*1E6);
			int startLon = (int) (leg.getJSONObject("start_location").getDouble("lng")*1E6);
			transitLeg.startLocation = new GeoPoint(startLat, startLon);
			int endLat = (int) (leg.getJSONObject("end_location").getDouble("lat")*1E6);
			int endLon = (int) (leg.getJSONObject("end_location").getDouble("lng")*1E6);
			transitLeg.endLocation = new GeoPoint(endLat, endLon);
			
			route.leg = transitLeg;
			
			//Get the steps for this leg
			final JSONArray steps = leg.getJSONArray("steps");
			//Number of steps for use in for loop
			final int numSteps = steps.length();
			/* Loop through the steps */
			Log.d("GoogleParser", "Number of steps:"+numSteps);
			ArrayList<Step> stepsArray = new ArrayList<Step>(numSteps);
			for (int i = 0; i < numSteps; i++) { //TODO
				Step step = new Step();
				//Get the individual step
				final JSONObject jStep = steps.getJSONObject(i);
				//Get the start position for this step and set it on the segment
				int startLatty = (int) (jStep.getJSONObject("start_location").getDouble("lat")*1E6);
				int startLonny = (int) (jStep.getJSONObject("start_location").getDouble("lng")*1E6);
				step.startLocation = new GeoPoint(startLatty, startLonny);
				int endLatty = (int) (jStep.getJSONObject("end_location").getDouble("lat")*1E6);
				int endLonny = (int) (jStep.getJSONObject("end_location").getDouble("lng")*1E6);
				step.endLocation = new GeoPoint(endLatty, endLonny);
				step.distanceText = jStep.getJSONObject("distance").getString("text");
				step.distanceMeters = jStep.getJSONObject("distance").getInt("value");
				step.durationText = jStep.getJSONObject("duration").getString("text");
				step.polyline = jStep.getJSONObject("polyline").getString("points");
				step.htmlInstructions = jStep.getString("html_instructions");
				Log.d("GOOGLE PARSER - NEW", "Step "+i+": "+step.htmlInstructions);
				step.travelMode = jStep.getString("travel_mode");
				//Strip html from google directions and set as turn instruction
				//segment.setInstruction(step.getString("html_instructions").replaceAll("<(.*?)*>", ""));
				if(!jStep.isNull("transit_details")){
					TransitDetails transDets = new TransitDetails();
					JSONObject jTransDets = jStep.getJSONObject("transit_details");
					
					transDets.arrivalStop = jTransDets.getJSONObject("arrival_stop").getString("name");
					int arrLat = (int) (jTransDets.getJSONObject("arrival_stop").getJSONObject("location").getDouble("lat")*1E6);
					int arrLon = (int) (jTransDets.getJSONObject("arrival_stop").getJSONObject("location").getDouble("lng")*1E6);
					transDets.arrivalCoord = new GeoPoint(arrLat, arrLon);
					transDets.departureStop = jTransDets.getJSONObject("departure_stop").getString("name");
					int depLat = (int) (jTransDets.getJSONObject("departure_stop").getJSONObject("location").getDouble("lat")*1E6);
					int depLon = (int) (jTransDets.getJSONObject("departure_stop").getJSONObject("location").getDouble("lng")*1E6);
					transDets.departureCoord = new GeoPoint(depLat,depLon);
					transDets.arrivalTime = jTransDets.getJSONObject("arrival_time").getString("text");
					transDets.departureTime = jTransDets.getJSONObject("departure_time").getString("text");
					transDets.headsign = jTransDets.getString("headsign");
					
					if(!jTransDets.isNull("line")){
						Line line = new Line();
						JSONObject jLine = jTransDets.getJSONObject("line");
						//TODO: allow for more than one agency
						try{
							line.agencyName = jLine.getJSONArray("agencies").getJSONObject(0).getString("name");
							line.agencyPhone = jLine.getJSONArray("agencies").getJSONObject(0).getString("phone");
							line.agencyUrl = jLine.getJSONArray("agencies").getJSONObject(0).getString("url");
							line.lineUrl = jLine.getString("url");
							line.name = jLine.getString("name");
							line.vehicleType = jLine.getJSONObject("vehicle").getString("type");
						} catch (JSONException ex) {
							Log.d("GOOGLE PARSER - NEW", "Line is missing "+ex);
						}
						transDets.transitLine = line;
					} else {
						transDets.transitLine = null;
					}
					
					step.transDetails = transDets;
				} else {
					step.transDetails = null;
				}
				
				if(!jStep.isNull("steps")) {
					JSONArray jSubSteps = jStep.getJSONArray("steps");
					int numSubSteps = jSubSteps.length();
					Step[] subSteps = new Step[numSubSteps];
					for(int j = 0; j < numSubSteps; ++j){
						subSteps[j] = parseStep(jSubSteps.getJSONObject(j));
					}
					step.subSteps = subSteps;
				} else {
					step.subSteps = null;
				}
				stepsArray.add(step);
			}
			route.leg.steps = stepsArray;
			
			Log.d("GOOGLE PARSER - NEW", "SUCCESSSSSSS!!!!");
			Log.d("GOOGLE PARSER - NEW", "SUCCESSSSSSS!!!!");
			Log.d("GOOGLE PARSER - NEW", "SUCCESSSSSSS!!!!");
			Log.d("GOOGLE PARSER - NEW", "SUCCESSSSSSS!!!!");
			Log.d("GOOGLE PARSER - NEW", "SUCCESSSSSSS!!!!");
			Log.d("GOOGLE PARSER - NEW", "SUCCESSSSSSS!!!!");
		} catch (JSONException e) {
			Log.e(e.getMessage(), "Google JSON Parser - " + feedURL);
		}
		
		return route;
	}

	public Step parseStep(JSONObject jStep) throws JSONException {
		Step step = new Step();
		//Get the start position for this step and set it on the segment
		int startLatty = (int) (jStep.getJSONObject("start_location").getDouble("lat")*1E6);
		int startLonny = (int) (jStep.getJSONObject("start_location").getDouble("lng")*1E6);
		step.startLocation = new GeoPoint(startLatty, startLonny);
		int endLatty = (int) (jStep.getJSONObject("end_location").getDouble("lat")*1E6);
		int endLonny = (int) (jStep.getJSONObject("end_location").getDouble("lng")*1E6);
		step.endLocation = new GeoPoint(endLatty, endLonny);
		step.distanceText = jStep.getJSONObject("distance").getString("text");
		step.distanceMeters = jStep.getJSONObject("distance").getInt("value");
		step.durationText = jStep.getJSONObject("duration").getString("text");
		step.polyline = jStep.getJSONObject("polyline").getString("points");
		try{
			step.htmlInstructions = jStep.getString("html_instructions");
			Log.d("GOOGLE PARSER - NEW", "substep: "+step.htmlInstructions);
		} catch (JSONException ex) {
			Log.d("GOOGLE PARSER -  NEW", "No html instructions for this substep");
		}
		step.travelMode = jStep.getString("travel_mode");
		//Strip html from google directions and set as turn instruction
		//segment.setInstruction(step.getString("html_instructions").replaceAll("<(.*?)*>", ""));
		if(!jStep.isNull("transit_details")){
			TransitDetails transDets = new TransitDetails();
			JSONObject jTransDets = jStep.getJSONObject("transit_details");
			
			transDets.arrivalStop = jTransDets.getJSONObject("arrival_stop").getString("name");
			int arrLat = (int) (jTransDets.getJSONObject("arrival_stop").getJSONObject("location").getDouble("lat")*1E6);
			int arrLon = (int) (jTransDets.getJSONObject("arrival_stop").getJSONObject("location").getDouble("lng")*1E6);
			transDets.arrivalCoord = new GeoPoint(arrLat, arrLon);
			transDets.departureStop = jTransDets.getJSONObject("departure_stop").getString("name");
			int depLat = (int) (jTransDets.getJSONObject("departure_stop").getJSONObject("location").getDouble("lat")*1E6);
			int depLon = (int) (jTransDets.getJSONObject("departure_stop").getJSONObject("location").getDouble("lng")*1E6);
			transDets.departureCoord = new GeoPoint(depLat,depLon);
			transDets.arrivalTime = jTransDets.getJSONObject("arrival_time").getString("text");
			transDets.departureTime = jTransDets.getJSONObject("departure_time").getString("text");
			transDets.headsign = jTransDets.getString("headsign");
			
			if(!jTransDets.isNull("line")){
				Line line = new Line();
				JSONObject jLine = jTransDets.getJSONObject("line");
				//TODO: allow for more than one agency
				try {
				line.agencyName = jLine.getJSONArray("agencies").getJSONObject(0).getString("name");
				line.agencyPhone = jLine.getJSONArray("agencies").getJSONObject(0).getString("phone");
				line.agencyUrl = jLine.getJSONArray("agencies").getJSONObject(0).getString("url");
				line.lineUrl = jLine.getString("url");
				line.name = jLine.getString("name");
				line.vehicleType = jLine.getJSONObject("vehicle").getString("type");
				} catch (JSONException ex) {
					Log.d("GOOGLE PARSER - NEW", "Line is missing "+ex);
				}
				transDets.transitLine = line;
			} else {
				transDets.transitLine = null;
			}
			
			step.transDetails = transDets;
		} else {
			step.transDetails = null;
		}
		return step;
	}
	
	public Route parseDriving() {
		// turn the stream into a string
		final String result = convertStreamToString(this.getInputStream());
		//Create an empty route
		//final ArrayList<Route> routeList = new ArrayList<Route>();
		final Route route = new Route();
		//Create an empty segment
		final Segment segment = new Segment();
		try {
			//Tranform the string into a json object
			final JSONObject json = new JSONObject(result);
			//Get the route object
			final JSONObject jsonRoute = json.getJSONArray("routes").getJSONObject(0);
			//Get the leg, only one leg as we don't support waypoints
			final JSONArray legs = jsonRoute.getJSONArray("legs");
			for(int j = 0; j < legs.length(); j++) {
				//Get the steps for this leg
				final JSONArray steps = ((JSONObject)legs.get(j)).getJSONArray("steps");
				//final JSONArray arrival_time = leg.getJSONArray("arrival_time");
				//final JSONArray departure_time = leg.getJSONArray("departure_time");
				//Number of steps for use in for loop
				final int numSteps = steps.length();
				//Set the name of this route using the start & end addresses
				route.setName(((JSONObject)legs.get(j)).getString("start_address") + " to " + ((JSONObject)legs.get(j)).getString("end_address"));
				//Get google's copyright notice (tos requirement)
				route.setCopyright(jsonRoute.getString("copyrights"));
				//Get the total length of the route.
				route.setDistance(((JSONObject)legs.get(j)).getJSONObject("distance").getString("text"));
				//Get the total duration of the route.
				route.setDuration(((JSONObject)legs.get(j)).getJSONObject("duration").getString("text"));

				//Get any warnings provided (tos requirement)
				if (!jsonRoute.getJSONArray("warnings").isNull(0)) {
					route.setWarning(jsonRoute.getJSONArray("warnings").getString(0));
				}
				/* Loop through the steps, creating a segment for each one and
				 * decoding any polylines found as we go to add to the route object's
				 * map array. Using an explicit for loop because it is faster!
				 */
				for (int i = 0; i < numSteps; i++) {
					//Get the individual step
					final JSONObject step = steps.getJSONObject(i);
					//Get the start position for this step and set it on the segment
					final JSONObject start = step.getJSONObject("start_location");
					final GeoPoint position = new GeoPoint((int) (start.getDouble("lat")*1E6), 
							(int) (start.getDouble("lng")*1E6));
					segment.setStartPoint(position);
					//Set the length of this segment in metres
					final int length = step.getJSONObject("distance").getInt("value");
					distance += length;
					segment.setLength(length);
					segment.setDistance(distance/1000);
					//Strip html from google directions and set as turn instruction
					segment.setInstruction(step.getString("html_instructions").replaceAll("<(.*?)*>", ""));
					//Retrieve & decode this segment's polyline and add it to the route.
					route.addPoints(decodePolyLine(step.getJSONObject("polyline").getString("points")));

					//Grab the type of transit this describes
					segment.setTransitMode(step.getString("travel_mode"));

					//Push a copy of the segment to the route
					route.addSegment(segment.copy());
				}
			}
		} catch (JSONException e) {
			Log.e(e.getMessage(), "Google JSON Parser - " + feedURL);
		}
		return route;
	}


	/**
	 * Convert an inputstream to a string.
	 * @param input inputstream to convert.
	 * @return a String of the inputstream.
	 */

	private static String convertStreamToString(final InputStream input) {
		final BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		final StringBuilder sBuf = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sBuf.append(line);
			}
		} catch (IOException e) {
			Log.e(e.getMessage(), "Google parser, stream2string");
		} finally {
			try {
				input.close();
			} catch (IOException e) {
				Log.e(e.getMessage(), "Google parser, stream2string");
			}
		}
		return sBuf.toString();
	}

	/**
	 * Decode a polyline string into a list of GeoPoints.
	 * @param poly polyline encoded string to decode.
	 * @return the list of GeoPoints represented by this polystring.
	 */

	public static List<GeoPoint> decodePolyLine(final String poly) {
		int len = poly.length();
		int index = 0;
		List<GeoPoint> decoded = new ArrayList<GeoPoint>();
		int lat = 0;
		int lng = 0;

		while (index < len) {
			int b;
			int shift = 0;
			int result = 0;
			do {
				b = poly.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lat += dlat;

			shift = 0;
			result = 0;
			do {
				b = poly.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lng += dlng;

			decoded.add(new GeoPoint(
					(int) (lat*1E6 / 1E5), (int) (lng*1E6 / 1E5)));
		}

		return decoded;
	}

	protected InputStream getInputStream() {
		try {
			return feedURL.openConnection().getInputStream();
		} catch (IOException e) {
			//Log.e(e.getMessage(), "XML parser - " + feedUrl);
			return null;
		}
	}
}
