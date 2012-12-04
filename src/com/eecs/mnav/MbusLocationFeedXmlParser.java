package com.eecs.mnav;

import java.net.URL;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;

public class MbusLocationFeedXmlParser {
	// Item == bus
	public static class Item {
		public String id = "";
		public String latitude = "";
		public String longitude = "";
		public String heading = "";
		public String route = "";
		public String routeid = "";
		public String busroutecolor = "";
	}

	public static String parse(URL text) {
		// This pattern takes more than one param but we'll just use the first
		try {
			XmlPullParserFactory parserCreator;
			parserCreator = XmlPullParserFactory.newInstance();
			XmlPullParser parser = parserCreator.newPullParser();
			parser.setInput(text.openStream(), null);

			Log.d("MbusLocationFeedXmlParser", "Parsing XML...");

			int parserEvent = parser.getEventType();
			Item currentItem = new Item();
			BusRoutesActivity.items = new ArrayList<Item>();

			// Parse the XML returned on the network
			while (parserEvent != XmlPullParser.END_DOCUMENT) {
				switch (parserEvent) {
				case XmlPullParser.START_TAG:
					String tag = parser.getName();
					if(tag.compareTo("item") == 0) {
						parser.require(XmlPullParser.START_TAG, null, "item");
						while (parser.next() != XmlPullParser.END_TAG) {
							if (parser.getEventType() != XmlPullParser.START_TAG) {
								continue;
							}
							String name = parser.getName();
							if (name.equals("id")) {
								currentItem.id = BusRoutesActivity.readText(parser);
							} else if (name.equals("latitude")) {
								currentItem.latitude = BusRoutesActivity.readText(parser);
							} else if (name.equals("longitude")) {
								currentItem.longitude = BusRoutesActivity.readText(parser);
							} else if (name.equals("heading")) {
								currentItem.heading = BusRoutesActivity.readText(parser);
							} else if (name.equals("route")) {
								currentItem.route = BusRoutesActivity.readText(parser);
							} else if (name.equals("routeid")) {
								currentItem.routeid = BusRoutesActivity.readText(parser);
							} else if (name.equals("busroutecolor")) {
								currentItem.busroutecolor = BusRoutesActivity.readText(parser);
							}
						}
						BusRoutesActivity.items.add(currentItem);
						currentItem = new Item();
					}
					break;
				}
				parserEvent = parser.next();
			}
		} catch (Exception e) {
			Log.i("RouteLoader", "Failed in parsing XML", e);
			return "Finished with failure.";
		}

		return "Done...";
	}
}
