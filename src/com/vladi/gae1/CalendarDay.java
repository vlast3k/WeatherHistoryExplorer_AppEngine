package com.vladi.gae1;

import java.io.Serializable;
import java.util.Calendar;

public class CalendarDay implements Serializable {
	public static final long serialVersionUID = -2064264841232150768L;
	public CalendarDay(Calendar c) {
		cal = c;
	}
	private Calendar cal;
	
	@Override
	public int hashCode() {
		return cal.get(Calendar.YEAR)*1000 + cal.get(Calendar.DAY_OF_YEAR);
	}
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CalendarDay) {
			CalendarDay c2 = (CalendarDay) obj;
			return (cal.get(Calendar.YEAR) == c2.cal.get(Calendar.YEAR)) && (cal.get(Calendar.DAY_OF_YEAR) == c2.cal.get(Calendar.DAY_OF_YEAR));
		}
		return false;
	}
	
	public long getTime() {
		return cal.getTimeInMillis();
	}
	public Calendar getDate() {
		return cal;
	}
	
	public CalendarDay reuse(Calendar c) {
		cal = c;
		return this;
	}
	
	@Override
	public String toString() {
	  // TODO Auto-generated method stub
	  return WunderGroundConnectorJSON.eetFormat_format(cal.getTime());
	}
}