package com.eecs.mnav;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;

public class MbusPublicFeedXmlParser {

	public static class Route {
		public String name;
		public String id;
		public String topofloop;
		public String busroutecolor;
		public ArrayList<Stop> stops;
		public String stopcount;

		//Override the equals() method for Route to compare the route name
		@Override
		public boolean equals(Object o) {
			return (o instanceof Route) && ((Route)o).name.equals(this.name);
		}

		//Override the hashCode() method so that Route objects are hashed by their route name
		@Override
		public int hashCode() {
			return name.hashCode();
		}
	}

	public static class Stop {
		public String name;
		public String name2;
		public String name3;
		public String latitude;
		public String longitude;
		public ArrayList<String> ids;
		public ArrayList<String> toas;
		public String toacount;
	}
	
	private static Set<Route> currentRoutesFromFeed;

	public static String parse(URL text) {
		try {
			XmlPullParserFactory parserCreator;
			parserCreator = XmlPullParserFactory.newInstance();
			XmlPullParser parser = parserCreator.newPullParser();
			parser.setInput(text.openStream(), null);

			Log.d("MbusPublicFeedXmlParser", "Parsing XML...");

			int parserEvent = parser.getEventType();
			Route currentRoute;
			Stop currentStop;
			currentRoutesFromFeed = new HashSet<Route>();

			// Parse the XML returned on the network
			while (parserEvent != XmlPullParser.END_DOCUMENT) {
				switch (parserEvent) {
				case XmlPullParser.START_TAG:
					String tag = parser.getName();
					if(tag.compareTo("route") == 0) {
						parser.require(XmlPullParser.START_TAG, null, "route");
						currentRoute = new Route();
						currentRoute.stops = new ArrayList<Stop>();
						while (parser.next() != XmlPullParser.END_TAG) {
							if (parser.getEventType() != XmlPullParser.START_TAG) {
								continue;
							}
							String name = parser.getName();
							if (name.equals("name")) {
								currentRoute.name = BusRoutesActivity.readText(parser);
							} 
							else if (name.equals("id")) {
								currentRoute.id = BusRoutesActivity.readText(parser);
							} 
							else if (name.equals("topofloop")) {
								currentRoute.topofloop = BusRoutesActivity.readText(parser);
							} 
							else if (name.equals("busroutecolor")) {
								currentRoute.busroutecolor = BusRoutesActivity.readText(parser);
							} 
							else if (name.equals("stop")) {
								parser.require(XmlPullParser.START_TAG, null, "stop");
								currentStop = new Stop();
								currentStop.toas = new ArrayList<String>();
								currentStop.ids = new ArrayList<String>();
								while (parser.next() != XmlPullParser.END_TAG) {
									if (parser.getEventType() != XmlPullParser.START_TAG)
										continue;

									name = parser.getName();
									if (name.equals("name")) 
										currentStop.name = BusRoutesActivity.readText(parser);
									else if (name.equals("name2")) 
										currentStop.name2 = BusRoutesActivity.readText(parser);
									else if (name.equals("name3"))
										currentStop.name3 = BusRoutesActivity.readText(parser);
									else if (name.equals("latitude"))
										currentStop.latitude = BusRoutesActivity.readText(parser);
									else if (name.equals("longitude"))
										currentStop.longitude = BusRoutesActivity.readText(parser);
									else if (name.regionMatches(0, "toa", 0, 3))
										currentStop.toas.add(BusRoutesActivity.readText(parser));
									else if (name.regionMatches(0, "id", 0, 2))
										currentStop.ids.add(BusRoutesActivity.readText(parser));
									else if (name.equals("toacount"))
										currentStop.toacount = BusRoutesActivity.readText(parser);

									parserEvent = parser.next();
								}

								currentRoute.stops.add(currentStop);
							} 
							else if (name.equals("stopcount")) {
								currentRoute.stopcount = BusRoutesActivity.readText(parser);
							}

							parserEvent = parser.next();
						}
						currentRoutesFromFeed.add(currentRoute);
					}

					break;
				}
				parserEvent = parser.next();
			}
		} 
		catch (Exception e) {
			Log.i("MbusPublicFeedXmlParser", "Failed in parsing XML", e);
			return "Finished with failure.";
		}
		
		updateRoutesList();

		return "Done...";
	}
	
	/**
	 * Go through each element in the currentRoutesFromFeed set and see if BusRoutesActivity.routes
	 * contains it, and add it to that set if it doesn't. Then go through BusRoutesActivity.routes
	 * and see if it contains something that isn't in currentRoutesFromFeed, and remove it from 
	 * BusRoutesActivity.routes if it does. 
	 */
	private static void updateRoutesList() {
		for(Route r : currentRoutesFromFeed) {
			if(!BusRoutesActivity.routes.contains(r))
				BusRoutesActivity.routes.add(r);
		}
		for(Route r : BusRoutesActivity.routes) {
			if(!currentRoutesFromFeed.contains(r))
				BusRoutesActivity.routes.remove(r);
		}		
	}
}
