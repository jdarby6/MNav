package com.eecs.mnav;


public class MEvent {
	private String label;
	private String location;
	private int index;//int representation of timebegin
	private String timeBegin;
	private String timeEnd;
	private String days;//string MOTUWETHFRSASU
	
	public MEvent(){
		this.label = "";
		this.location = "";
		this.index = 0;
		this.timeBegin = "";
		this.timeEnd = "";
		this.days = "";
	}
	
	public MEvent(String label2, String location2, int index2, String timeBegin2,
			String timeEnd2, String days2) {
		this.label = label2;
		this.location = location2;
		this.index = index2;
		this.timeBegin = timeBegin2;
		this.timeEnd = timeEnd2;
		this.days = days2;
	}
	public String getLabel() {return label;}
	public void setLabel(String label) {this.label = label;}
	public String getLocation() {return location;}
	public void setLocation(String location) {this.location = location;}
	public int getIndex() {return index;}
	public void setIndex(int index) {this.index = index;}
	public String getTimeBegin() {return timeBegin;}
	public void setTimeBegin(String timeBegin) {this.timeBegin = timeBegin;}
	public String getTimeEnd() {return timeEnd;}
	public void setTimeEnd(String timeEnd) {this.timeEnd = timeEnd;}
	public void restoreLabel(String label){this.label = label;}
	public String getDays() {return days;}
	public void setDays(String days) {this.days = days;}
}
