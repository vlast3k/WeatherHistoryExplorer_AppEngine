package com.vladi.gae1;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.vladi.gae1.AirportsDataBase.AirportData;


public class AveragesExporter {

	public void start() throws Exception {
		final WunderGroundConnectorJSON wunderGroundConnector = new WunderGroundConnectorJSON();
		//AirportsDataBase adb = new AirportsDataBase();
		JSONObject jsonap = new JSONObject(Utils.readTextFile("AirportsLocations_hash.json"));
		
		HashSet<String> ich = new HashSet<String>();
		int start = 0;
		//String order = "LEUBKCHO";
		String order = "U";
		HashSet<String> largePrefixes = new HashSet<String>();
		largePrefixes.add("CY");
		largePrefixes.add("LF");
		largePrefixes.add("EG");
		largePrefixes.add("KS");
		largePrefixes.add("KM");
		largePrefixes.add("LI");
		largePrefixes.add("KB");
		largePrefixes.add("KL");
		largePrefixes.add("EN");
		largePrefixes.add("KP");
		largePrefixes.add("KC");
		largePrefixes.add("KA");
		largePrefixes.add("LE");
		largePrefixes.add("KF");
		HashMap<String, JSONObject> avgMap = new HashMap<String, JSONObject>();
		String allap[] = JSONObject.getNames(jsonap);
	//	for (int a = 0; a < order.length(); a++) {
			//System.err.println("---------- Starting for : " + order.charAt(a));
			for (int i=start; i < allap.length; i++) {
				String icao = allap[i];
				//if (icao.charAt(0) != order.charAt(a)) continue;
				//if (order.indexOf(ad.icao.charAt(0)) > -1) continue;
				//if (ich.contains(icao)) continue;
//				System.err.println("\n---------------------------------------------------------");
				//System.err.println(order.charAt(a) + ":" + i +  "/" + adb.allAirports.size() + ". " + ad.icao + ", " + ad.continent +", " + ad.country + ", " + ad.city);
				System.err.println(i +  "/" + allap.length + ". " + icao + ", " + jsonap.getJSONObject(icao).getString("cont") +", " + jsonap.getJSONObject(icao).getString("cntr") + ", " + jsonap.getJSONObject(icao).getString("city"));
				//getData(icao);
				checkYears(icao);
				//Thread.sleep(10000);
				//JSONObject stats = wunderGroundConnector.getStats(ad.icao, false);
//				String prefix = ad.icao.substring(0,2);
//				if (largePrefixes.contains(prefix)) prefix = ad.icao.substring(0,3);
//				if (avgMap.containsKey(prefix) == false) avgMap.put(prefix, new JSONObject());
//				avgMap.get(prefix).put(ad.icao, computeAverage(stats));
				//ich.add(icao);
			}
	//	}
		
//		for (String k: avgMap.keySet()) {
//			FileOutputStream out = new FileOutputStream("avgExport/" + k + ".avg");
//			System.out.println("Exporting: " + k);
//			out.write((avgMap.get(k).toString()).getBytes());
//			out.close();
//		}
		
	}
	
	private void getData(String icao) {
		try {
			boolean hasMore = true;
			while (hasMore) {
				String data = Utils.readTextInputStream(new URL("http://1.vlast3k-gae1.appspot.com/webgae_real?icao=" + icao + "&doUpdate=true&noForecast=true").openStream());
				JSONObject j = new JSONObject(data);
				hasMore = j.has("years");
				if (hasMore) System.err.print(j.get("years") + ".");
			} 
			System.err.println();

		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	private void checkYears(String icao) {
		try {
				String data = Utils.readTextInputStream(new URL("http://1.vlast3k-gae1.appspot.com/webgae_real?icao=" + icao + "&noForecast=true").openStream());
				JSONObject j = new JSONObject(data);
				String years = j.optString("years");
				if (years != null) { 
					System.err.println("== years: " + years);
				} else {
					System.err.println("-----------------------------------------> no data");
				}

		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	

	private JSONArray computeAverage(JSONObject weatherData, int month, int day) throws Exception {
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
        String jsonDataOrder[] = new String[] {"MaxTemp", "MinTemp", "AvgWind", "Rain", "MaxWind", "MaxGust", "AvgPressure", "CloudCover", "Events"};
    	int numDays = 0;
        for (String year : JSONObject.getNames(weatherData)) {
        	JSONObject yearData = weatherData.getJSONObject(year);
        	for (int m=0; m < 12 ; m++) {
        		String smonth = "" + m;
        		if (month > -1 && m != month || !yearData.has(smonth)) continue; //in case we need avg for the whole year - m is -1
        	
	        	for (int d=1; d <= 31; d++) {
	        		String sday = "" + d;
	        		if (day > -1 && d != day) continue; //in case we need avg for a specific day - skip all others
		        	if (!yearData.getJSONObject(smonth).has(sday)) continue;
		        	JSONArray dayData = yearData.getJSONObject(smonth).getJSONArray(sday);
		        	if (dayData.getInt(legend.getInt("MaxTemp")) == 0 && dayData.getInt(legend.getInt("MinTemp")) == 0        		
		        		&& dayData.getInt(legend.getInt("CloudCover")) == 0) {
		        			continue; //invalid day - should be filtered elsewhere!
		        	}
		        		
		        	numDays ++;
		            avgMax += dayData.getInt(legend.getInt("MaxTemp"));
		            avgMin += dayData.getInt(legend.getInt("MinTemp"));
		            avgWind += dayData.getInt(legend.getInt("AvgWind"));
		            avgRain += dayData.getInt(legend.getInt("Rain"));
		            absMax = Math.max(absMax, dayData.getInt(legend.getInt("MaxTemp")));
		            absMin = Math.min(absMin, dayData.getInt(legend.getInt("MinTemp")));
		            int ev = dayData.getInt(legend.getInt("CloudCover"));
		            if (ev < 3) ev_sun++;
		            else if (ev < 9 || ev == 10 && dayData.getInt(legend.getInt("Rain")) == 0) ev_cloud++; // ponqkoga pishat che e imalo dujd obache 0 mm percipation, i realno v takiva dni ne e jasno kde e valqlo
		            else if (ev < 11) ev_showers ++;
		            else ev_snow ++;
	        	}
        	}
        }
        JSONArray res = new JSONArray();
        if (numDays == 0) return res;

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
        return res;
	}
	
	String [] legend = new String[] {"numDays", "ev_sun", "ev_cloud", "ev_showers", "ev_snow", "avgMin",
			"avgMax", "absMin", "absMax", "avgRain", "avgWind", "minAvgMin", "maxAvgMax"};

	public JSONObject computeAverage(JSONObject weatherData) throws Exception {
		JSONObject jlegend = new JSONObject();
		for (int i=0; i < legend.length; i++) jlegend.put(legend[i], i);

		JSONArray data = new JSONArray();
		data.put(computeAverage(weatherData, -1, -1)); // avg for year
		for (int m = 0; m < 12; m++) {
			JSONArray month = new JSONArray();
			month.put(computeAverage(weatherData, m, -1)); // avg for month  
			for (int d = 1; d <= 31; d++) {
				if (m == 1 && d == 29) break; //skip 29 feb
				month.put(computeAverage(weatherData, m, d));
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

	private void computeAvgMinMaxMonth(JSONArray arr, JSONObject jlegend) throws JSONException {
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

	private void computeAvgMinMaxYear(JSONArray arr, JSONObject jlegend) throws JSONException {
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
//		WunderGroundConnectorJDBC wunderGroundConnector = new WunderGroundConnectorJDBC();
//		JSONObject w = wunderGroundConnector.getStats("LBSF", false);
//		JSONObject r = new AveragesExporter().computeAverage(w);
//		System.out.println(r.toString(0));
		new AveragesExporter().start();
		// TODO Auto-generated method stub

	}

}
