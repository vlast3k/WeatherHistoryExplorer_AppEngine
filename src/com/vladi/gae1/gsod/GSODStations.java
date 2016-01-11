package com.vladi.gae1.gsod;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.vladi.gae1.Utils;
import com.vladi.gae1.WUtils;
import com.vladi.gae1.gsod.Base.NearStation;

public class GSODStations {
	
	private static int colUSAF = 0;
	private static int colWBAN = 1;
	private static int colName = 2;
	private static int colCtryx = 3;
	private static int colCtry = 4;
	private static int colState = 5;
	private static int colISAO = 6;
	private static int colLat = 7;
	private static int colLon = 8;
	private static int colElev = 9;
	private static int colStart = 10;
	private static int colEnd = 11;
	
	Object[] allData = null;
	public void localParse() throws Exception {
		allData = parseIshHistoryCSV(null,  false);
	}
	
	static boolean isStationActive(String id) {
		return new File("c:/develop/gsod/decomp/" + id + ".json").exists();
	}

	static Object[] parseIshHistoryCSV(HashSet<String> toSkipFromGSOD, boolean processAll) throws Exception {
		String data = Utils.readTextFile("c:/develop/gsod/ish-history.csv");
		BufferedReader rd = new BufferedReader(new StringReader(data));
		rd.readLine();
		String line = null;
		JSONObject ret = new JSONObject();
		int total = 0;
		
		JSONArray stationCity = new JSONArray();
		JSONArray stationCntr = new JSONArray();
		JSONArray stationState = new JSONArray();
		
		JSONObject stationLoc  = new JSONObject();
		JSONArray jLat = new JSONArray();
		JSONArray jLon = new JSONArray();
		stationLoc.put("lat", jLat);
		stationLoc.put("lon", jLon);

		JSONArray stationId   = new JSONArray();
		JSONObject stationIdRev   = new JSONObject();
		JSONArray stationOther = new JSONArray();
		while ((line = rd.readLine()) != null) {
			ArrayList<String> items = WUtils.parseCSVLine(line);
			if (items.get(colCtry).length() == 0) continue;
			if (items.get(colLat).length() == 0) continue;
			if (items.get(colLat).equals("0")) continue;
			if (items.get(colLat).equals("-99999")) continue;
			if (!(items.get(colEnd).equals("20130705") || items.get(colEnd).equals("20130706"))) continue;
			String stationIdstr = items.get(colUSAF) + "-" + items.get(colWBAN);
			if (toSkipFromGSOD != null && toSkipFromGSOD.contains(stationIdstr)) continue;
			//if (isStationActive(stationIdstr) == false) continue;
			
			int yearsHistory = (Integer.parseInt(items.get(colEnd)) - Integer.parseInt(items.get(colStart)) ) / 10000; 

			stationCity.put(Base.toCamelCase(items.get(colName).trim()));
			stationCntr.put(items.get(colCtry).trim());
			stationState.put(items.get(colState).trim());
			
			jLat.put((Object)new Float((float)Integer.parseInt(items.get(colLat).trim()) / 1000));
			jLon.put((Object)new Float((float)Integer.parseInt(items.get(colLon).trim()) / 1000));
			
			stationId.put(stationIdstr);
			
			JSONArray jOther = new JSONArray();
			jOther.put(0, (Object)yearsHistory);
			jOther.put(1,(Object)new Integer((int)(Integer.parseInt(items.get(colElev).trim())/10)));
			if (items.get(colISAO).trim().length() > 0) jOther.put(2, items.get(colISAO).trim());
			stationOther.put(jOther);
			stationIdRev.put(stationIdstr, total);
			total ++;
		}
		

		System.out.println("Total GSOD Stations: " + total);
		return new Object[] {stationId, stationCity, stationCntr, stationState, stationLoc, stationOther, stationIdRev};
	}
	
	public JSONArray getStationIds() {
		return (JSONArray)allData[0];
	}
	
	private JSONObject getLoc() {
		return (JSONObject)allData[4];
	}

	public double[] getLatlon(String sid) throws JSONException {
		int idx = ((JSONObject)allData[6]).getInt(sid);
		return new double[] {getLoc().getJSONArray("lat").getDouble(idx), getLoc().getJSONArray("lon").getDouble(idx)}; 
	}
	
	public ArrayList<NearStation> getNearStations(String sid, int tresholdKm) throws Exception {
		double[] latLon = getLatlon(sid);
		return Base.getNearestStation(latLon[0], latLon[1], tresholdKm, allData);
		
	}

	public int getHeight(String sid) throws Exception {
		int idx = ((JSONObject)allData[6]).getInt(sid);
		return ((JSONArray)allData[5]).getJSONArray(idx).getInt(1);
	}
	


}
