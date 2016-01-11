package com.vladi.gae1;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/*
 * [0year avg data]
 * [1:month:jan 
 * 		[0:month avg]
 * 		[1:day avg [x.v.b.n]
 * 		[2:day avg [x.v.b.n]
 */

public class AvgGenerator {
	
	
	
	static String [] legend = new String[] {"numDays", "ev_sun", "ev_cloud", "ev_showers", "ev_snow", "avgMin",
			"avgMax", "absMin", "absMax", "avgRain", "avgWind", "minAvgMin", "maxAvgMax"};

	public static JSONObject computeAverage(JSONObject weatherData, StringBuffer logBuffer) throws Exception {
		if (weatherData.getJSONObject("data").length() == 0) return null;
		JSONObject jlegend = new JSONObject();
		for (int i=0; i < legend.length; i++) jlegend.put(legend[i], i);

		JSONArray data = new JSONArray();
		data.put(computeAverage(weatherData, -1, -1)); // avg for year
		for (int m = 1; m <= 12; m++) {
			JSONArray month = new JSONArray();
			month.put(computeAverage(weatherData, m, -1)); // avg for month  
			for (int d = 1; d <= 31; d++) {
				if (m == 2 && d == 29) break; //skip 29 feb
				JSONArray xx = computeAverage(weatherData, m, d);
				if (xx.length() > 0) month.put(computeAverage(weatherData, m, d)); //avoid 31th of smaller months
			}
			computeAvgMinMaxMonth(month, jlegend);
			data.put(month);
		}
		computeAvgMinMaxYear(data, jlegend);
		JSONObject ret = new JSONObject();
		ret.put("legend", jlegend);
		ret.put("data", data);
		return ret;
	}
	
	static private JSONArray computeAverage(JSONObject weatherData, int month, int day) throws Exception {
        int avgMax = 0;
        int avgMin = 0;
        int avgWind = 0;
        int avgRain = 0;
        int absMax = -100;
        int absMin = 100;
        int ev_sun = 0;
        int ev_cloud = 0;
        int ev_showers = 0;
        int ev_snow = 0;
        JSONObject legend = weatherData.getJSONObject("legend");
        JSONObject data   = weatherData.getJSONObject("data");
        //String jsonDataOrder[] = new String[] {"MaxTemp", "MinTemp", "AvgWind", "Rain", "MaxWind", "MaxGust", "AvgPressure", "CloudCover", "Events"};
    	int numDays = 0;
        for (String year : JSONObject.getNames(data)) {
        	JSONArray yearData = data.getJSONArray(year);
        	for (int m=1; m <= 12 ; m++) {
        		if (month > -1 && m != month || yearData.isNull(m) || yearData.getJSONArray(m).length() == 0) continue; //in case we need avg for the whole year - m is -1
        		int d=1;
	        	for (; d <= 31; d++) {
	        		if (day > -1 && d != day) continue; //in case we need avg for a specific day - skip all others
		        	if (yearData.getJSONArray(m).isNull(d) || yearData.getJSONArray(m).getJSONArray(d).length() == 0) continue;
		        	JSONArray dayData = yearData.getJSONArray(m).getJSONArray(d);
		        	//imashe ot gsod takiva dni kudeto lipsvat maxtemp ili min temp - dobre e da gi ignorirame.. ianche veche ne se generirat takiva
		        	if (dayData.isNull(legend.getInt("MaxTemp")) || dayData.isNull(legend.getInt("MinTemp"))) continue;
		        	if (dayData.getInt(legend.getInt("MaxTemp")) == 0 && dayData.getInt(legend.getInt("MinTemp")) == 0        		
		        		&& dayData.getInt(legend.getInt("CloudCover")) == 0) {
		        			continue; //invalid day - should be filtered elsewhere!
		        	}
		        		
		        	if (dayData.getInt(legend.getInt("MaxTemp")) == 55) {
		        		System.out.println("ere");
		        	}
		        	numDays ++;
		            avgMax += dayData.getInt(legend.getInt("MaxTemp"));
		            avgMin += dayData.getInt(legend.getInt("MinTemp"));
		            avgWind += dayData.getInt(legend.getInt("AvgWind"));
		            try {
		            	avgRain += dayData.getInt(legend.getInt("Rain"));
		            } catch (Exception e) {
		            	
		            }
		            absMax = Math.max(absMax, dayData.getInt(legend.getInt("MaxTemp")));
		            absMin = Math.min(absMin, dayData.getInt(legend.getInt("MinTemp")));
		            int cc = dayData.getInt(legend.getInt("CloudCover"));
		            String ev = dayData.getString(legend.getInt("Events"));
		            boolean hr = ev.indexOf("r") > -1;
		            boolean hs = ev.indexOf("s") > -1;
		            boolean ht = ev.indexOf("t") > -1;
		            if (cc <= 3) ev_sun++;
		            else if (ht) ev_showers ++;
		            else if (hs) ev_snow ++;
		            else if (hr) ev_showers ++;
		            else ev_cloud++;
	        	}
	            //if (day == -1) System.out.println("year: " + year + ", Month: " + month + ", d:" + d);
        	}
        }
        JSONArray res = new JSONArray();
        if (numDays == 0) return res;
        if (day == -1) System.out.println("Month: " + month + ", avgrain:" + avgRain);
        avgMax /= numDays;
        avgMin /= numDays;
        avgWind /= numDays;
        avgRain /= numDays;
        res.put(numDays);
        res.put(ev_sun);
        res.put(ev_cloud);
        res.put(ev_showers);
        res.put(ev_snow);
        res.put(avgMin);
        res.put(avgMax);
        res.put(absMin);
        res.put(absMax);
        res.put(avgRain);
        res.put(avgWind);
        //if (day == -1) System.out.println("Month: " + month + ", numDays:" + numDays);
        return res;
	}
	


	static private void computeAvgMinMaxMonth(JSONArray arr, JSONObject jlegend) throws JSONException {
		//System.err.println("computeAvgMinMaxMonth");
		int min = 100;
		int max = -100;
		for (int i=1; i < arr.length(); i++) { // ALL DAYS
			if (arr.getJSONArray(i).length() == 0) continue; //the case for 31 when months do not have it
			//System.out.println("i=" + i + ", avgMin: " + arr.getJSONArray(i).getInt(jlegend.getInt("avgMin")) +
			//		", avgMax: " + arr.getJSONArray(i).getInt(jlegend.getInt("avgMax")) );
			min = Math.min(min, arr.getJSONArray(i).getInt(jlegend.getInt("avgMin")));
			max = Math.max(max, arr.getJSONArray(i).getInt(jlegend.getInt("avgMax")));
		}
		if (min == 100) return; //no data was recorded for this month
		//System.out.println("min max: " + min + ", " + max );
		arr.getJSONArray(0).put(jlegend.getInt("minAvgMin"), min);
		arr.getJSONArray(0).put(jlegend.getInt("maxAvgMax"), max);
	}

	static private void computeAvgMinMaxYear(JSONArray arr, JSONObject jlegend) throws JSONException {
		int min = 100;
		int max = -100;
		for (int i=1; i < arr.length(); i++) { //all months
			if (arr.getJSONArray(i).getJSONArray(0).length() == 0) continue; //in case there was no info for this month, e.g. data only for 2013, and not for the full year
			min = Math.min(min, arr.getJSONArray(i).getJSONArray(0).getInt(jlegend.getInt("avgMin")));
			max = Math.max(max, arr.getJSONArray(i).getJSONArray(0).getInt(jlegend.getInt("avgMax")));
		}
		arr.getJSONArray(0).put(jlegend.getInt("minAvgMin"), min);
		arr.getJSONArray(0).put(jlegend.getInt("maxAvgMax"), min);
	}
	
	public static void main(String[] args) throws Exception {
		//String wds = Utils.readTextInputStream(new URL("http://2.vlast3k-gae1.appspot.com/webgae_real?icao=156130-99999").openStream());
		//System.out.println("Received: " + wds);
		JSONObject wd = new JSONObject(Utils.readTextFile("./res/156140-99999.json"));
		JSONObject avg = computeAverage(wd, new StringBuffer());
		System.out.println(avg.getJSONArray("data").toString());
		
	}

}
