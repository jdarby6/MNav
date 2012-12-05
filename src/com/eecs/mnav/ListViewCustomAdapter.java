package com.eecs.mnav;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ListViewCustomAdapter extends BaseAdapter{
	public Activity context;
	public LayoutInflater inflater;

	public ListViewCustomAdapter(Activity context) {
		super();

		this.context = context;

		this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public int getCount() {
		return BusRoutesActivity.routes.size();
	}

	public Object getItem(int position) {
		return BusRoutesActivity.routes.get(position);
	}

	public long getItemId(int position) {
		return Integer.parseInt(BusRoutesActivity.routes.get(position).id);
	}

	public static class ViewHolder
	{
		ImageView imgViewLogo;
		TextView textView_route;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;

		if(convertView == null) {
			holder = new ViewHolder();
			convertView = inflater.inflate(R.layout.routes_row_layout, null);
			holder.textView_route = (TextView) convertView.findViewById(R.id.textView_route);
			convertView.setTag(holder);
		}
		else
			holder = (ViewHolder) convertView.getTag();

		holder.textView_route.setText(BusRoutesActivity.routes.get(position).name);
		Log.d("Trying to set this as background color", BusRoutesActivity.routes.get(position).busroutecolor);
		convertView.setBackgroundColor(Color.parseColor('#'+BusRoutesActivity.routes.get(position).busroutecolor));


		return convertView;
	}

}