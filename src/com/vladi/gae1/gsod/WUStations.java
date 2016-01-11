package com.vladi.gae1.gsod;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.vladi.gae1.Utils;
import com.vladi.gae1.gsod.Base.NearStation;

public class WUStations extends Base {
	static HashMap<String, String> stateNamesRev = loadCountryNames("c:/develop/gsod/states.txt", true);
	static HashMap<String, String> countryNamesRev = loadCountryNames("c:/develop/gsod/country-list.txt", true);

    private static String australiaStates = ",New South Wales,Victoria,Queensland,Western Australia,South Australia,Capital Territory,Tasmania,Northern Territory,Australian Islands,";
    private static String chinaProvinces = ",Anhui,Fujian,Gansu,Guangxi,Shanghai,Tianjin,Chongqing,Beijing,Nei Mongol Zizhiqu,Xinjiang Uygur,Guangdong,Guizhou,Hainan,Hebei,Heilongjiang,Henan,Hubei,Hunan,Jiangsu,Jiangxi,Jilin,Liaoning,Qinghai,Shaanxi,Shandong,Shanxi,Sichuan,Yunnan,Zhejiang,";
	Object[] allData = null;
	public void localParse() throws Exception {
		allData = convertWUAirports(new Object[] {new JSONArray(),new JSONArray(),new JSONArray(),new JSONArray(),new JSONArray(),new JSONArray()});
	}
	public Object[] convertWUAirports(Object[] gsod_meta) throws Exception {
    	JSONObject wuap = new JSONObject(Utils.readTextFile("c:/develop/workspace_ad/webGAE_Real/AirportsLocations_hash.json"));
    	//JSONObject ret = new JSONObject();
		JSONArray stationId = (JSONArray) gsod_meta[0];
		JSONArray stationCity = (JSONArray) gsod_meta[1];
		JSONArray stationCntr = (JSONArray) gsod_meta[2]; 
		JSONArray stationState = (JSONArray) gsod_meta[3];
		JSONObject stationLoc = (JSONObject) gsod_meta[4];
		JSONObject stationIdRev = (JSONObject) gsod_meta[6];

		int total = stationId.length();

//		JSONArray stationCity = new JSONArray();
//		JSONArray stationCntr = new JSONArray();
//		JSONArray stationState = new JSONArray();
//		
//		JSONObject stationLoc  = new JSONObject();
		JSONArray jLat = stationLoc.getJSONArray("lat");//new JSONArray();
		JSONArray jLon = stationLoc.getJSONArray("lon");//new JSONArray();
		//JSONArray jLon = new JSONArray();
//		stationLoc.put("lat", jLat);
//		stationLoc.put("lon", jLon);
//
//		JSONArray stationId   = new JSONArray();
//		JSONObject stationIdRev= new JSONObject();
//		//JSONArray stationOther = new JSONArray();
    	for (String icao : JSONObject.getNames(wuap)) {
			//JSONArray stationMeta = new JSONArray();
			JSONObject ap = wuap.getJSONObject(icao);
			stationCity.put(ap.getString("city"));
			if (ap.getString("cont").equals("United States")) {
				stationCntr.put("US");
				stationState.put(stateNamesRev.get(ap.getString("cntr")).toString());
			} else if (icao.startsWith("CZ") || icao.startsWith("CY")) {
				stationCntr.put("CA");
				stationState.put(stateNamesRev.get(ap.getString("cntr")).toString());				
			} else if (australiaStates.indexOf("," + ap.getString("cntr") + ",") > -1) {
				stationCntr.put("AU");
				stationState.put("");
			} else if (chinaProvinces.indexOf("," + ap.getString("cntr") + ",") > -1) {
				stationCntr.put("CH");
				stationState.put(ap.getString("cntr"));
			} else {
				stationCntr.put(countryNamesRev.get(ap.getString("cntr")).toString());
				stationState.put("");
			}
			//stationMeta.put(LEGEND_ISH.get("icao"), icao);
			jLat.put((Object)new Double(wuap.getJSONObject(icao).getString("lat")));
			jLon.put((Object)new Double(wuap.getJSONObject(icao).getString("lon")));
			stationId.put(icao);
			stationIdRev.put(icao, total);
			//ret.put(icao, stationMeta);
			total ++;
    	}
		System.out.println("Total WU Stations: " + total);
		return new Object[] {stationId, stationCity, stationCntr, stationState, stationLoc, stationIdRev};
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
