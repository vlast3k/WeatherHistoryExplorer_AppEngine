package com.vladi.gae1.gsod;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.appengine.repackaged.com.google.common.io.Files;
import com.vladi.gae1.CalendarDay;
import com.vladi.gae1.PreloadWeatherUnderground;
import com.vladi.gae1.Utils;
import com.vladi.gae1.WunderGroundConnectorJSON;
import com.vladi.gae1.gsod.Base.NearStation;

public class NewGSODProcess {

	public static void main(String[] args) throws Exception {
		
		 NewGSODProcess xx = new NewGSODProcess();
		 WunderGroundConnectorJSON.finalYearTreshold = 10000000;
		 xx.processWuAirportsNoMapping();
		// TODO Auto-generated method stub

	}
	
	private void processWuAirportsNoMapping() throws Exception {
    	JSONObject wuap = new JSONObject(Utils.readTextFile("c:/develop/workspace_ad/webGAE_Real/AirportsLocations_hash.json"));
		gs = new GSODStations();
		gs.localParse();
    	Object[] gsod_meta = gs.allData;
    	int count = 0;
    	HashSet<String> toSkipFromGSOD = new HashSet<String>();
    	String wuids[] = JSONObject.getNames(wuap);
    	Arrays.sort(wuids);
    	JSONObject mapping = new JSONObject();
    	int startIdx = 37;
    	for (String icao : wuids) {
    		ArrayList<NearStation> nearest = Base.getNearestStation(wuap.getJSONObject(icao).getDouble("lat"), wuap.getJSONObject(icao).getDouble("lon"), 10, gsod_meta);
    		if (nearest.size() == 0) {
        		System.out.println( count ++ + ". " + icao + ", preloading:");
        		if (count < 37)	PreloadWeatherUnderground.preloadStation(icao);
    		}
    	}
	}

	private int getMissingDays(String id, boolean isWu) throws Exception {
		String path = (isWu ? "c:/develop/gsod/allwu/" : "c:/develop/gsod/decomp/" ) + id + ".json";
		JSONObject jo;
		try {
			//System.out.println("Load: " + path);
			jo = new JSONObject(Utils.readTextFile(path));
		} catch (Exception e) {
			System.out.println("Err: " + e.toString());
			return -1;
		}
		return WunderGroundConnectorJSON.getAllMissingForPeriod(jo, new GregorianCalendar(1900, 0, 1), new GregorianCalendar()).size();
	}
	
    public HashSet<String> processWuAirports() throws Exception {
    	JSONObject wuap = new JSONObject(Utils.readTextFile("c:/develop/workspace_ad/webGAE_Real/AirportsLocations_hash.json"));
		gs = new GSODStations();
		gs.localParse();
    	Object[] gsod_meta = gs.allData;
    	int count = 0;
    	HashSet<String> toSkipFromGSOD = new HashSet<String>();
    	String wuids[] = JSONObject.getNames(wuap);
    	Arrays.sort(wuids);
    	JSONObject mapping = new JSONObject();
    	for (String icao : wuids) {
    		//System.out.println("Processing : " + count ++);
    		//if (!icao.startsWith("LFP")) continue;
    		ArrayList<NearStation> nearest = Base.getNearestStation(wuap.getJSONObject(icao).getDouble("lat"), wuap.getJSONObject(icao).getDouble("lon"), 10, gsod_meta);
//    		for (NearStation n : nearest) {
//    			toSkipFromGSOD.add(n.name);
//    		}
    		if (nearest.size() > 0) {
    			int wuDays = getMissingDays(icao, true);
    			int mostDaysGsod = 10000000; NearStation mostDaysStation = null;
    			for (NearStation n : nearest) {
    				int missingDays = getMissingDays(n.name, false);
    				if (mostDaysGsod > missingDays) {
    					mostDaysGsod = missingDays;
    					mostDaysStation = n;
    				}
    			}
//    			int gsodDays = getMissingDays(nearest.get(0).name, false);
    			if (wuDays < mostDaysGsod) System.out.print(">>>>>> ");
	    		System.out.print(count+++ ". ");
	    		System.out.print("wu:" + (int)wuDays/356 + ", gs:" + (int)mostDaysGsod/356 + "; ");
	    		System.out.print("WU ICAO: " + icao + ", name: " + wuap.getJSONObject(icao).getString("city") + " -- " + nearest.size() + " -> " + mostDaysStation);
	    		mapping.put(icao, mostDaysStation.name);
	    		//for (NearStation n : nearest) System.out.print(n + ", ");
	    		System.out.println();
	    		//throw new Exception();
    			
    		}
    		
    		new FileOutputStream("mapping_wu_gsod.json").write(mapping.toString().getBytes());
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
	
	private void findNearStations() throws Exception {
		gs = new GSODStations();
		gs.localParse();
		File[] files = new File("c:/develop/gsod/decomp").listFiles();
		for (int i=0; i < files.length; i++) {
			String sid = files[i].getName().replaceAll(".json", "");
			ArrayList<NearStation> nearStations = gs.getNearStations(sid, 3);
			int h = gs.getHeight(nearStations.get(0).name);
			for (int n=1; n < nearStations.size(); n++) {
				int d = Math.abs(h - gs.getHeight(nearStations.get(n).name)); 
				if (d > 70) {
					System.out.println("height diff = " + d + ", " + sid + " > " + nearStations.size() + "  -  " + nearStations);
				}
			}
			if (nearStations.size() > 1) {
				System.out.println(sid + " > " + nearStations.size() + "  -  " + nearStations);
			}
		}
		// TODO Auto-generated method stub
		
	}

	private boolean isActive(File f) throws Exception {
		JSONObject j = new JSONObject(Utils.readTextFile(f.toString()));
		LinkedHashSet<CalendarDay> allmiss = WunderGroundConnectorJSON.getAllMissingForPeriod(j, new GregorianCalendar(2014, 4, 1), new GregorianCalendar(2014, 5, 14));
		if (allmiss.size() > 30) {
			return false;
		} else {
			return true;
		}
	}
	private void findActive() throws Exception {
		File[] files = new File("c:/develop/gsod/decomp").listFiles();
		String inactiveDir = "c:/develop/gsod/inactive/";
		for (int i=0; i < files.length; i++) {
			boolean b = isActive(files[i]);
			System.out.print(i + ". " + files[i].getName() + ", active=" + b );
			if (!b) {
				System.out.println(", moving..." + files[i].renameTo(new File(inactiveDir + files[i].getName())));
			} else {
				System.out.println("");
			}
		}
	}

	private void conv(int s, int e) throws Exception {
		for (int i=s; i <= e; i++ ){
			if (i >= gs.getStationIds().length()) break;
			System.out.println(i + "/" + gs.getStationIds().length());
			GSODStation st = new GSODStation("c:/develop/gsod", gs.getStationIds().getString(i));
			FileOutputStream fo = new FileOutputStream("c:/develop/gsod/decomp/" + gs.getStationIds().getString(i) + ".json");
			fo.write(st.getJSON().toString().getBytes());
			fo.close();
		}
		
	}
	GSODStations gs;
	private void go() throws Exception {
		gs = new GSODStations();
		gs.localParse();
	    new Thread() {
			public void run() {
				try {
					conv(0,3000);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			};
		}.start();
	    new Thread() {
			public void run() {
				try {
					conv(3000,6000);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			};
		}.start();
	    new Thread() {
			public void run() {
				try {
					conv(6000,9000);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			};
		}.start();
	    new Thread() {
			public void run() {
				try {
					conv(9000,gs.getStationIds().length());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			};
		}.start();
//		for (int i=0; i < gs.getStationIds().length(); i++ ){
//			System.out.println(i + "/" + gs.getStationIds().length());
//			GSODStation st = new GSODStation("c:/develop/gsod", gs.getStationIds().getString(i));
//			FileOutputStream fo = new FileOutputStream("c:/develop/gsod/decomp/" + gs.getStationIds().getString(i) + ".json");
//			fo.write(st.getJSON().toString().getBytes());
//			fo.close();
//		}
		
	}

}
