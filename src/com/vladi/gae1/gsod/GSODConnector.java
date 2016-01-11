package com.vladi.gae1.gsod;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import org.json.JSONArray;
import org.json.JSONObject;
import com.vladi.gae1.Utils;

public class GSODConnector extends Base {

	static HashMap<String, String> countryNames = loadCountryNames("c:/develop/gsod/country-list.txt", false);
	static HashMap<String, String> stateNames = loadCountryNames("c:/develop/gsod/states.txt", false);

    public HashSet<String> processWuAirports() throws Exception {
    	JSONObject wuap = new JSONObject(Utils.readTextFile("c:/develop/workspace_ad/webGAE_Real/AirportsLocations_hash.json"));
    	Object[] gsod_meta = GSODStations.parseIshHistoryCSV(null, false);
    	int count = 0;
    	HashSet<String> toSkipFromGSOD = new HashSet<String>();
    	for (String icao : JSONObject.getNames(wuap)) {
    		System.out.println("Processing : " + count ++);
    		ArrayList<NearStation> nearest = getNearestStation(wuap.getJSONObject(icao).getDouble("lat"), wuap.getJSONObject(icao).getDouble("lon"), 15, gsod_meta);
    		for (NearStation n : nearest) {
    			toSkipFromGSOD.add(n.name);
    		}
//    		if (nearest.size() > 1) {
//	    		System.out.print(count+++ ". WU ICAO: " + icao + ", name: " + wuap.getJSONObject(icao).getString("city") + " -- " + nearest.size() + " -> ");
//	    		for (NearStation n : nearest) System.out.print(n + ", ");
//	    		System.out.println();
//    			
//    		}
//    		if ((Integer)nearest[1] < 3 
//    			//&&	!icao.equals(meta.getJSONArray((String)nearest[0]).getString(LEGEND_ISH.get("icao")))
//    			) {
//    			String wuCity = wuap.getJSONObject(icao).getString("city");
//    			String gCity  = meta.getJSONArray((String)nearest[0]).getString(LEGEND_ISH.get("name"));
//    			if (gCity.indexOf(wuCity) == -1) {
//		    		System.out.println(count+++ ". WU ICAO: " + icao + ", name: " + wuap.getJSONObject(icao).getString("city") + 
//		    				           ", dist: " + nearest[1] + ", station: " + nearest[0] + 
//		    				           ", icao: " + meta.getJSONArray((String)nearest[0]).getString(LEGEND_ISH.get("icao")) +
//		    				           ", name: " + meta.getJSONArray((String)nearest[0]).getString(LEGEND_ISH.get("name")));
//    			}
//    		}
    	}
    	System.out.println("Total to skip from GSOD: " + toSkipFromGSOD.size());
    	return toSkipFromGSOD;
    }
    
    public void removeGSODCloseStations(Object[] gsod_meta, HashSet<String> toSkipFromGSOD) throws Exception {
		JSONArray stationId = (JSONArray) gsod_meta[0];
		JSONArray stationCity = (JSONArray) gsod_meta[1];
		JSONArray stationCntr = (JSONArray) gsod_meta[2]; 
		JSONArray stationState = (JSONArray) gsod_meta[3];
		JSONObject stationLoc = (JSONObject) gsod_meta[4];
		JSONObject stationIdRev = (JSONObject) gsod_meta[6];

		int total = stationId.length();

		System.out.println("ToSkip Before: " + toSkipFromGSOD.size());
		for (int i=0; i < total; i++) {
    		System.out.println("Processing : " + i);
    		if (toSkipFromGSOD.contains(stationId.getString(i))) continue;
    		ArrayList<NearStation> nearest = getNearestStation(stationLoc.getJSONArray("lat").getDouble(i), stationLoc.getJSONArray("lon").getDouble(i), 15, gsod_meta);
    		nearest.add(new NearStation(i, 0, gsod_meta));
    		int mostYears = 0;
    		String mostYearsId = null;
    		for (NearStation n : nearest) {
    			if (n.years > mostYears) {
    				mostYearsId = n.name;
    				mostYears = n.years;
    			}
    		}
    		for (NearStation n : nearest) {
    			if (n.name.equals(mostYearsId) == false) toSkipFromGSOD.add(n.name);
    		}
		}
		System.out.println("ToSkip After: " + toSkipFromGSOD.size());
    }
	
    
	private String[] stationsMissingData = new String[] {"997266-99999", "997308-99999", "997316-99999", "997318-99999", "997319-99999", "997322-99999", "997323-99999", "997324-99999", "997326-99999", "997328-99999", "997330-99999", "997334-99999",
			"997394-99999", "997805-99999", "997807-99999", "997809-99999", "998223-99999", "998274-99999", "998276-99999", "998278-99999", "998283-99999", "998285-99999", "998288-99999", "998306-99999", "999999-23272", "014590-99999", "804103-99999", "804105-99999", "821450-99999", "824760-99999", "831790-99999", "833840-99999", "834420-99999", "837180-99999", 
			"841420-99999", "859680-99999", "721038-99999", "062480-99999", "062580-99999", "062850-99999", "943450-99999", "063080-99999", "063120-99999", "063130-99999", "944110-99999", 
			"944220-99999", "945130-99999", "945140-99999", "945210-99999", "945820-99999", "945860-99999", "945870-99999", "946230-99999", "946250-99999", "946270-99999", "946300-99999", "946360-99999", "946920-99999", "946940-99999", "074840-99999", "947040-99999", "074990-99999", "947320-99999", "947330-99999", "947350-99999", "947400-99999", "075890-99999", "075900-99999", "947720-99999", "947830-99999", "947920-99999", "076810-99999", "078760-99999", "948880-99999", "949010-99999", "949090-99999", "949280-99999", "949760-99999", "955120-99999",
			"955330-99999", "956140-99999", "956150-99999", "956160-99999", "956230-99999", "956260-99999", "956310-99999", "956430-99999", "957230-99999", "957270-99999", "957350-99999", 
			"957460-99999", "957520-99999", "957780-99999", "958430-99999", "140060-99999", "140090-99999", "140120-99999", "994035-99999", "994063-99999", "994064-99999", "677850-99999", "678910-99999"};
	//private HashSet<String> ss = new HashSet<String>(Arrays.asList(stationsMissingData));
	private void makeJSFiles() throws Exception {
		//HashSet<String> toSkipFromGSOD = processWuAirports();
		//System.out.println("994016-99999   "  + toSkipFromGSOD.contains("994016-99999"));
		//toSkipFromGSOD.addAll(Arrays.asList(stationsMissingData));
//		Object[] gsod_meta = GSODStations.parseIshHistoryCSV(toSkipFromGSOD, false);
		Object[] gsod_meta = GSODStations.parseIshHistoryCSV(null, false);
		//removeGSODCloseStations(gsod_meta, toSkipFromGSOD);
		//System.out.println("994016-99999   "  + toSkipFromGSOD.contains("994016-99999"));
		//toSkipFromGSOD.addAll(Arrays.asList(stationsMissingData));
		//gsod_meta = GSODStations.parseIshHistoryCSV(toSkipFromGSOD, false);
		//	return new Object[] {stationId, stationCity, stationCntr, stationState, stationLoc, stationOther};
		//new FileOutputStream("./res/all_stations.json").write(gsod_meta.toString().getBytes());
		new FileOutputStream("./res/gsod_id.js").write(("__stations.gsod_id = " + gsod_meta[0].toString()).getBytes());
		new FileOutputStream("./res/gsod_city.js").write(("__stations.gsod_city = " + gsod_meta[1].toString()).getBytes());
		new FileOutputStream("./res/gsod_cntr.js").write(("__stations.gsod_cntr = " + gsod_meta[2].toString()).getBytes());
		new FileOutputStream("./res/gsod_state.js").write(("__stations.gsod_state = " + gsod_meta[3].toString()).getBytes());
		new FileOutputStream("./res/gsod_loc.js").write(("__stations.gsod_loc = " + gsod_meta[4].toString()).getBytes());
		new FileOutputStream("./res/gsod_other.js").write(("__stations.gsod_other = " + gsod_meta[5].toString()).getBytes());
		new FileOutputStream("./res/gsod_id_rev.js").write(("__stations.gsod_id_rev = " + gsod_meta[6].toString()).getBytes());
	
		Object[] wu_meta = new WUStations().convertWUAirports(gsod_meta);
		new FileOutputStream("./res/all_id.js").write(("__stations.all_id = " + wu_meta[0].toString()).getBytes());
		new FileOutputStream("./res/all_city.js").write(("__stations.all_city = " + wu_meta[1].toString()).getBytes());
		new FileOutputStream("./res/all_cntr.js").write(("__stations.all_cntr = " + wu_meta[2].toString()).getBytes());
		new FileOutputStream("./res/all_state.js").write(("__stations.all_state = " + wu_meta[3].toString()).getBytes());
		new FileOutputStream("./res/all_loc.js").write(("__stations.all_loc = " + wu_meta[4].toString()).getBytes());
		new FileOutputStream("./res/all_id_rev.js").write(("__stations.all_id_rev = " + wu_meta[5].toString()).getBytes());

		JSONObject meta = new JSONObject();
		JSONObject jCountries = new JSONObject();
		meta.put("cntr", jCountries);
		for (String cid : countryNames.keySet()) jCountries.put(cid, countryNames.get(cid));
			
//		JSONObject jLegend = new JSONObject();
//		allStations.put("l", jLegend);
//		for (String cid : LEGEND_ISH.keySet()) jLegend.put(cid, LEGEND_ISH.get(cid));

		JSONObject jStates = new JSONObject();
		meta.put("state", jStates);
		for (String cid : stateNames.keySet()) jStates.put(cid, stateNames.get(cid));
		
		new FileOutputStream("./res/all_meta.js").write(("__stations.all_meta = " + meta.toString()).getBytes());
		System.out.println("done");
		
	}

	

	public static void main(String[] args) throws Exception {
		
//		convertAll();
//		String gsodDir = "c:/develop/Titanium_Studio_Workspace/WeatherHisyory_Titanium/gsod";
//		//gsod.processWuAirports();
		GSODConnector gsod = new GSODConnector();
		gsod.makeJSFiles();
//		JSONObject jst = gsod.processStation("c:/develop/gsod", "994016-99999");
//		new FileOutputStream("./res/conv.json").write(jst.toString().getBytes());
//		System.out.println("done");
//		for (String stname : JSONObject.getNames(gsod_meta)) {
//			System.out.print("Processing: "+ stname + ":");
//			JSONObject wdata = gsod.processStation("c:/develop/Titanium_Studio_Workspace/WeatherHisyory_Titanium/gsod", stname);
//			wdata.put("ctry", countryNames.get(gsod_meta.getJSONArray(stname).getString(LEGEND_ISH.get("cntr")).trim()));
//			wdata.put("city", gsod_meta.getJSONArray(stname).getString(LEGEND_ISH.get("city")));
//			new FileOutputStream("./res/" + stname + ".json").write(wdata.toString().getBytes());
//			System.out.println();
//			
//		}
//		TarInputStream tin = new TarInputStream(new FileInputStream("c:/develop/Titanium_Studio_Workspace/WeatherHisyory_Titanium/gsod/gsod_1970.tar"));
//		String stname = "./035623-992999-1970.op.gz";
//		TarEntry te = null;
//		while ((te = tin.getNextEntry()) != null) {
//			System.out.println(te.getName());
//			if (te.getName().equals(stname)) break;
//		}
		
	//	GZIPInputStream gzip = new GZIPInputStream(new FileInputStream("c:/develop/Titanium_Studio_Workspace/WeatherHisyory_Titanium/gsod/035623-99999-1972.op.gz"));
//		if (te != null) {
//			GZIPInputStream gzip = new GZIPInputStream(tin);
//			JSONObject wdata = gsod.parseGSODFile("010010-99999", Utils.readTextInputStream(gzip));
//			System.out.println(wdata.toString(1));
//		}
		//System.out.println(meta.toString());
		// TODO Auto-generated method stub

	}


}
;