package com.vladi.gae1;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WUtils {
	
	static int version = 9;

	static String [][] usefulEntries = new String[][] {
			{"Max TemperatureC", "MaxTemp"}, 
			{"Min TemperatureC", "MinTemp"},
			{"Max Wind SpeedKm/h", "MaxWind"},
			{"Mean Wind SpeedKm/h", "AvgWind"},
			{"Max Gust SpeedKm/h", "MaxGust"},
			{"Precipitationmm", "Rain"},
			{"Mean Sea Level PressurehPa", "AvgPressure"},
			{"CloudCover", "CloudCover"},
			{"Events", "Events"},
			};
    static HashMap<String, Integer> legendMap = new HashMap<String, Integer>();
    static {
    	int i = 0;
    	for (i = 0; i < usefulEntries.length; i++) legendMap.put(usefulEntries[i][1], i);  
    	legendMap.put("Snow", i++);
    	legendMap.put("Forecast", i++);
    }
    
	static void convertEntryFtoC(HashMap<String, String> entry) {
		if (!entry.containsKey("Max TemperatureF")) return;
		//System.err.println(Thread.currentThread() + " farenheit entry");
		HashSet<String> wuF = new HashSet<String>(Arrays.asList(new String[] {"Max TemperatureF","Mean TemperatureF","Min TemperatureF"}));
		HashSet<String> wuInPressure = new HashSet<String>(Arrays.asList(new String[] {"Max Sea Level PressureIn", "Mean Sea Level PressureIn", "Min Sea Level PressureIn"}));
		HashSet<String> wuInPrecip = new HashSet<String>(Arrays.asList(new String[] {"PrecipitationIn"}));
		HashSet<String> wuMph = new HashSet<String>(Arrays.asList(new String[] {"Max Wind SpeedMPH", "Mean Wind SpeedMPH", "Max Gust SpeedMPH"}));
		for (String s : wuF) {
			String newkey = s.replaceAll("ureF", "ureC");
			try {
				double f = Double.parseDouble(entry.get(s));
				double c = (f - 32) / 1.8f;
				entry.put(newkey, String.format("%.1f",c));
			} catch (Exception e) {
				entry.put(newkey, "");
			}
		}
		for (String s : wuInPressure) {
			String newkey = s.replaceAll("ureIn", "urehPa");
			try {
				double in = Double.parseDouble(entry.get(s));
				int c = (int)(in*33.86389f);
				entry.put(newkey, "" + c);
			} catch (Exception e) {
				entry.put(newkey, "");
			}
		}
		for (String s : wuInPrecip) {
			String newkey = s.replaceAll("ionIn", "ionmm");
			try {
				double in = Double.parseDouble(entry.get(s));
				int c = (int)(in*25.38f);
				entry.put(newkey, "" + c);
			} catch (Exception e) {
				entry.put(newkey, "");
			}
		}
		for (String s : wuMph) {
			String newkey = s.replaceAll("MPH", "Km/h");
			try {
				double in = Double.parseDouble(entry.get(s));
				int c = (int)(in*1.609f);
				entry.put(newkey, "" + c);
			} catch (Exception e) {
				entry.put(newkey, "");
			}
		}
	}
	
	public static void storeWeatherData(JSONObject weatherData) throws Exception {
		//System.err.println("Store weather data: serialize");
		boolean isMapped = false;
		String cityCode = weatherData.getString("icao");
		String mappedCity = cityCode;
		if (WebGAE_RealServlet.getMappingWuGsod().has(cityCode)) {
			isMapped = true;
			mappedCity = WebGAE_RealServlet.getMappingWuGsod().getString(cityCode);
		}
		weatherData.put("version", version);
		weatherData.put("icao", mappedCity);
		String xx = weatherData.toString();
		GAEUtils.storeGAEData(mappedCity, xx);
//		String fname = "data/" + weatherData.getString("icao") + ".json";
//		new File(fname).getParentFile().mkdirs();
//		FileOutputStream out = new FileOutputStream(fname);
//		out.write(weatherData.toString().getBytes());
	}

	public static JSONObject getLegend() throws JSONException {
		JSONObject ret = new JSONObject();
		for (Entry<String, Integer> e: legendMap.entrySet()) {
			ret.put(e.getKey(),  e.getValue());
			//System.err.println(e.getKey() + " " + e.getValue());
		}
//		for (int i = 0; i < legendMap..length; i++) {
//			ret.put(usefulEntries[i][1], i);
//		}
		return ret;
	}
	
	
	public static JSONObject loadWeatherData(String cityCode, JSONObject legend) throws Exception {
		String data = null;
		boolean isMapped = false;
		String mappedCity = cityCode;
		//System.err.println("city code is : " + cityCode);
		if (WebGAE_RealServlet.getMappingWuGsod().has(cityCode)) {
			isMapped = true;
			mappedCity = WebGAE_RealServlet.getMappingWuGsod().getString(cityCode);
			//System.err.println("Mapped city is : " + mappedCity);
		}
		if ((data = GAEUtils.readGAEData(mappedCity)) != null) {
			JSONObject j = new JSONObject(data);
			
			if (j.has("version") && j.getInt("version") == version || cityCode.length() > 4) {
				j.put("icao", cityCode);
				//if (isMapped) j.put("version", version + "m");
				return j;
			} else { 
				System.err.println("loadWeatherData. stored Version is old"); 
				System.err.println("loadWeatherData: version of archive is: " + j.optInt("version"));
				System.err.println("data is: " + data);
			}
		}

	    return createEmptyWeatherData(cityCode, legend);
	}
	
	public static JSONObject createEmptyWeatherData(String cityCode, JSONObject legend) throws Exception {
		JSONObject weatherData = new JSONObject();
		weatherData.put("icao", cityCode);
		weatherData.put("version", version);
		weatherData.put("legend", legend);
		weatherData.put("data", new JSONObject());
		return weatherData;
	}
	
	public static JSONArray getWeatherDataDay(JSONObject weatherData, String year, int month, int day) throws JSONException {
		JSONObject data = weatherData.getJSONObject("data");
		if (!data.has(year)) data.put(year, new JSONArray());
		JSONArray jy = data.getJSONArray(year);
		if (jy.isNull(month + 1)) jy.put(month+1, new JSONArray());
		JSONArray jm = jy.getJSONArray(month+1);
		if (jm.isNull(day)) jm.put(day, new JSONArray());
		JSONArray jd = jm.getJSONArray(day);
		
		return jd;
	}

	static Calendar getToday(TimeZone tz) {
		Calendar now = GregorianCalendar.getInstance(tz);
		now.set(Calendar.HOUR_OF_DAY, 0);
		now.set(Calendar.MINUTE, 0);
		now.set(Calendar.SECOND, 0);
		now.set(Calendar.MILLISECOND, 0);
		return now;
	}
	
	static Calendar getYesterday(TimeZone tz) {
		Calendar today = getToday(tz);
		today.add(Calendar.DAY_OF_MONTH, -1);
		return today;
	}
	
	static Calendar getDayStart(Calendar c) {
		Calendar c1 = (Calendar)c.clone();
		c1.set(Calendar.HOUR_OF_DAY, 0);
		c1.set(Calendar.MINUTE, 0);
		c1.set(Calendar.SECOND, 0);
		c1.set(Calendar.MILLISECOND, 0);
		return c1;
	}
	
	int cap(int val, int min, int max) {
		if (val < min) return min;
		if (val > max) return max;
		return val;
	}
	
	static public synchronized String d(Calendar c) throws Exception {
		if (c == null) return "null";
		return eetFormat_format(c.getTime()) + "_" +  c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE);
	}
	
	private static DateFormat eetFormat = new SimpleDateFormat("yyyy-M-d");

	static String eetFormat_format(Date time) {
		synchronized (eetFormat) {
			return eetFormat.format(time);
		}
	}

	static Date eetFormat_parse(String eet) throws ParseException {
		synchronized (eetFormat) {
			return eetFormat.parse(eet);
		}
	}
	
//	int diff (Calendar c1, Date c2) {
//		if (c2 == null) return - 99;
//		return diff(c1.getTimeInMillis(), c2.getTime());
//	}
//
//	int diff (Calendar c1, Calendar c2) {
//		return diff(c1.getTimeInMillis(), c2.getTimeInMillis());
//	}
//
//	int diff (long c1, long c2) {
//		return (int) ((c1 - c2) / (22*60*60*1000));
//	}

	public static ArrayList<String> parseCSVLine(String line) {
		try {
			String[] split = line.split(",");
			ArrayList<String> ret = new ArrayList<String>();
			for (int i=0; i < split.length; i++) {
				int rem = split[i].indexOf("<br />");
				if (rem > -1) split[i] = split[i].substring(0, rem);
				if (split[i].length() > 0 && split[i].charAt(0) == '\"') split[i] = split[i].substring(1, split[i].length()-1); 
				ret.add(split[i].trim());		
			}
			return ret;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	synchronized Calendar getDateFromLine(HashMap<String, String> line) throws ParseException {
		String eet = line.get("Time");
		Date d = eetFormat_parse(eet);
		eetFormat_format(d);
		Calendar entryDate = new GregorianCalendar();
		entryDate.setTime(d);
		eetFormat_format(entryDate.getTime());
		entryDate.get(Calendar.MONTH);
		return entryDate;
	}
	
}
