package com.eecs.mnav;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

public class MbusPublicFeedXmlParser {
	// We don't use namespaces
	private static final String ns = null;

	public List<Route> parse(InputStream in) throws XmlPullParserException, IOException {
		try {
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(in, null);
			parser.nextTag();
			return readFeed(parser);
		} finally {
			in.close();
		}
	}

	private List<Route> readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
		List<Route> routes = new ArrayList<Route>();

		parser.require(XmlPullParser.START_TAG, ns, "livefeed");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			// Starts by looking for the entry tag
			if (name.equals("route")) {
				routes.add(readRoute(parser));
			}
			else if (name.equals("routecount")) {
				//do something else
			}
			else {
				skip(parser);
			}
		}  
		return routes;
	}

	public static class Route {
		public final String routeName;
		public final String id;
		public final String topofloop;
		public final String busroutecolor;
		public final ArrayList<Stop> stops;
		public final String stopcount;

		private Route(String routeName, String id, String topofloop, String busroutecolor, 
				ArrayList<Stop> stops, String stopcount) {
			this.routeName = routeName;
			this.id = id;
			this.topofloop = topofloop;
			this.busroutecolor = busroutecolor;
			this.stops = stops;
			this.stopcount = stopcount;
		}
	}

	public static class Stop {
		public final String stopName;
		public final String stopName2;
		public final String stopName3;
		public final String latitude;
		public final String longitude;
		public final ArrayList<String> ids;
		public final ArrayList<String> toas;
		public final String toacount;

		private Stop(String stopName, String stopName2, String stopName3, String latitude, 
				String longitude, ArrayList<String> ids, ArrayList<String> toas, String toacount) {
			this.stopName = stopName;
			this.stopName2 = stopName2;
			this.stopName3 = stopName3;
			this.latitude = latitude;
			this.longitude = longitude;
			this.ids = ids;
			this.toas = toas;
			this.toacount = toacount;
		}
	}

	// Parses the contents of a route entry. When it comes across a "stop" tag, grabs its info
	// recursively before moving on
	private Route readRoute(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, "route");
		String routeName = null;
		String id = null;
		String topofloop = null;
		String busroutecolor = null;
		ArrayList<Stop> stops = new ArrayList<Stop>();
		String stopcount = null;

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (name.equals("name")) {
				routeName = readTag(parser, "name");
			} else if (name.equals("id")) {
				id = readTag(parser, "id");
			} else if (name.equals("topofloop")) {
				topofloop = readTag(parser, "topofloop");
			} else if (name.equals("busroutecolor")) {
				busroutecolor = readTag(parser, "busroutecolor");
			} else if (name.equals("stop")) {
				stops.add(readStop(parser));
			} else if (name.equals("stopcount")) {
				stopcount = readTag(parser, "stopcount");
			} else {
				skip(parser);
			}
		}
		return new Route(routeName, id, topofloop, busroutecolor, stops, stopcount);
	}

	// Parses the contents of a stop entry.
	private Stop readStop(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, "stop");
		String stopName = null;
		String stopName2 = null;
		String stopName3 = null;
		String latitude = null;
		String longitude = null;
		ArrayList<String> ids = new ArrayList<String>();
		ArrayList<String> toas = new ArrayList<String>();
		String toacount = null;

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (name.equals("name")) {
				stopName = readTag(parser, "name");
			} else if (name.equals("name2")) {
				stopName2 = readTag(parser, "name2");
			} else if (name.equals("name3")) {
				stopName3 = readTag(parser, "name3");
			} else if (name.equals("latitude")) {
				latitude = readTag(parser, "latitude");
			} else if (name.equals("longitude")) {
				longitude = readTag(parser, "longitude");
			} else if (name.matches("id[0-9]")) {
				ids.add(readTag(parser, name));
			} else if (name.matches("toa[0-9]")) {
				toas.add(readTag(parser, name));
			} else if (name.equals("toacount")) {
				toacount = readTag(parser, "toacount");
			} else {
				skip(parser);
			}
		}
		return new Stop(stopName, stopName2, stopName3, latitude, longitude, ids, toas, toacount);
	}
	
	// Processes basic tags in the feed.
	private String readTag(XmlPullParser parser, String tagName) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, ns, tagName);
		String tagText = readText(parser);
		parser.require(XmlPullParser.END_TAG, ns, tagName);
		return tagText;
	}

	// For all tags besides "stop", extracts text values.
	private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
		String result = "";
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.getText();
			parser.nextTag();
		}
		return result;
	}

	private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
		if (parser.getEventType() != XmlPullParser.START_TAG) {
			throw new IllegalStateException();
		}
		int depth = 1;
		while (depth != 0) {
			switch (parser.next()) {
			case XmlPullParser.END_TAG:
				depth--;
				break;
			case XmlPullParser.START_TAG:
				depth++;
				break;
			}
		}
	}
}
