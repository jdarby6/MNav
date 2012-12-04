package com.eecs.mnav;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;
import android.util.Xml;

public class MbusPublicFeedXmlParser {
	// We don't use namespaces
	private static final String ns = null;
	
	public static class Route {
		public String routeName;
		public String id;
		public String topofloop;
		public String busroutecolor;
		public ArrayList<Stop> stops;
		public String stopcount;
	}

	public static class Stop {
		public String stopName;
		public String stopName2;
		public String stopName3;
		public String latitude;
		public String longitude;
		public ArrayList<String> ids;
		public ArrayList<String> toas;
		public String toacount;
	}

	public void parse(URL text) throws XmlPullParserException, IOException {
		try {
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(text.openStream(), null);
			parser.nextTag();
			readFeed(parser);
		} catch(Exception e) {
			Log.d("MbusPublicFeedXmlParser", "whups");
		}
	}

	private void readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
		BusRoutesActivity.routes = new ArrayList<Route>();

		parser.require(XmlPullParser.START_TAG, ns, "livefeed");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			// Starts by looking for the entry tag
			if (name.equals("route")) {
				BusRoutesActivity.routes.add(readRoute(parser));
			}
			else if (name.equals("routecount")) {
				//do something else
			}
			else {
				skip(parser);
			}
		}
	}

	// Parses the contents of a route entry. When it comes across a "stop" tag, grabs its info
	// recursively before moving on
	private Route readRoute(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, "route");
		
		Route currentRoute = new Route();

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (name.equals("name")) {
				currentRoute.routeName = readTag(parser, "name");
			} else if (name.equals("id")) {
				currentRoute.id = readTag(parser, "id");
			} else if (name.equals("topofloop")) {
				currentRoute.topofloop = readTag(parser, "topofloop");
			} else if (name.equals("busroutecolor")) {
				currentRoute.busroutecolor = readTag(parser, "busroutecolor");
			} else if (name.equals("stop")) {
				currentRoute.stops.add(readStop(parser));
			} else if (name.equals("stopcount")) {
				currentRoute.stopcount = readTag(parser, "stopcount");
			} else {
				skip(parser);
			}
		}
		return currentRoute;
	}

	// Parses the contents of a stop entry.
	private Stop readStop(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, "stop");
		
		Stop currentStop = new Stop();

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (name.equals("name")) {
				currentStop.stopName = readTag(parser, "name");
			} else if (name.equals("name2")) {
				currentStop.stopName2 = readTag(parser, "name2");
			} else if (name.equals("name3")) {
				currentStop.stopName3 = readTag(parser, "name3");
			} else if (name.equals("latitude")) {
				currentStop.latitude = readTag(parser, "latitude");
			} else if (name.equals("longitude")) {
				currentStop.longitude = readTag(parser, "longitude");
			} else if (name.matches("id[0-9]")) {
				currentStop.ids.add(readTag(parser, name));
			} else if (name.matches("toa[0-9]")) {
				currentStop.toas.add(readTag(parser, name));
			} else if (name.equals("toacount")) {
				currentStop.toacount = readTag(parser, "toacount");
			} else {
				skip(parser);
			}
		}
		return currentStop;
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
