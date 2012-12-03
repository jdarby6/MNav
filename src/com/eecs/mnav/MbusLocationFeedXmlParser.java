package com.eecs.mnav;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

public class MbusLocationFeedXmlParser {
	// We don't use namespaces
	private static final String ns = null;
	
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

	public List<Item> parse(InputStream in) throws XmlPullParserException, IOException {
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

	private List<Item> readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
		List<Item> items = new ArrayList<Item>();

		parser.require(XmlPullParser.START_TAG, ns, "livefeed");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			//each bus is an 'item'
			if (name.equals("item")) {
				items.add(readItem(parser));
			}
		}  
		return items;
	}

	private Item readItem(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, "item");
		Item item = new Item();

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			
			String name = parser.getName();
			
			if (name.equals("id")) {
				item.id = parser.getText();
			} else if (name.equals("latitude")) {
				item.latitude = parser.getText();
			} else if (name.equals("longitude")) {
				item.longitude = parser.getText();
			} else if (name.equals("heading")) {
				item.heading = parser.getText();
			} else if (name.equals("route")) {
				item.route = parser.getText();
			} else if (name.equals("routeid")) {
				item.routeid = parser.getText();
			} else if (name.equals("busroutecolor")) {
				item.busroutecolor = parser.getText();
			}
			
			parser.nextTag();
		}
		return item;
	}
}
