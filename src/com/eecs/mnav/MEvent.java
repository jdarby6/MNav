package com.eecs.mnav;


public class MEvent {
	private String label;
	private String location;
	private int index;
	private int timeBegin;
	private int timeEnd;
	private String days;//string MOTUWETHFRSASU
	
	public MEvent(String label2, String location2, int index2, int timeBegin2,
			int timeEnd2, String days2) {
		label = label2;
		location = location2;
		index = index2;
		timeBegin = timeBegin2;
		timeEnd = timeEnd2;
		days = days2;
	}
	public String getLabel() {return label;}
	public void setLabel(String label) {this.label = label;}
	public String getLocation() {return location;}
	public void setLocation(String location) {this.location = location;}
	public int getIndex() {return index;}
	public void setIndex(int index) {this.index = index;}
	public int getTimeBegin() {return timeBegin;}
	public void setTimeBegin(int timeBegin) {this.timeBegin = timeBegin;}
	public int getTimeEnd() {return timeEnd;}
	public void setTimeEnd(int timeEnd) {this.timeEnd = timeEnd;}
	public void restoreLabel(String label){this.label = label;}
	public String getDays() {return days;}
	public void setDays(String days) {this.days = days;}
}
