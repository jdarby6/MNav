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
				segment.setPoint(position);
				//Set the length of this segment in metres
				final int length = step.getJSONObject("distance").getInt("value");
				distance += length;
				segment.setLength(length);
				segment.setDistance(distance/1000);
				//Strip html from google directions and set as turn instruction
				segment.setInstruction(step.getString("html_instructions").replaceAll("<(.*?)*>", ""));
				Log.d("GoogleParser", "Instruction "+numSteps+":"+segment.getInstruction());
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

	public Route parseTransit() {
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
			final JSONObject leg = jsonRoute.getJSONArray("legs").getJSONObject(0);
			//Get the steps for this leg
			final JSONArray steps = leg.getJSONArray("steps");
			//final JSONArray arrival_time = leg.getJSONArray("arrival_time");
			//final JSONArray departure_time = leg.getJSONArray("departure_time");
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
			for (int i = 0; i < numSteps; i++) { //TODO
				//Get the individual step
				final JSONObject step = steps.getJSONObject(i);
				//Get the start position for this step and set it on the segment
				final JSONObject start = step.getJSONObject("start_location");
				final GeoPoint position = new GeoPoint((int) (start.getDouble("lat")*1E6), 
						(int) (start.getDouble("lng")*1E6));
				segment.setPoint(position);
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

	private List<GeoPoint> decodePolyLine(final String poly) {
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
