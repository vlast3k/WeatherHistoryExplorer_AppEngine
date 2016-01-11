package com.vladi.gae1.gsod;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.vladi.gae1.Utils;

public class Base {
	protected static HashMap<String, String> loadCountryNames(String file, boolean reverse) {
		try {
			String data = Utils.readTextFile(file);
			BufferedReader rd = new BufferedReader(new StringReader(data));
			rd.readLine();rd.readLine();
			String line = null;
			HashMap<String, String> res = new HashMap<String, String>();
			while ((line = rd.readLine()) != null) {
				if (reverse) {
					res.put(toCamelCase(line.substring(2).trim()), line.substring(0, 2).trim());
				} else {
					res.put(line.substring(0, 2).trim(), toCamelCase(line.substring(2).trim()));
				}
			}
			return res;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
    static String toCamelCase(String src) {
		String[] spl = src.split(" ");
		String camelcase = "";
		for (String s : spl) {
			if (s.length() == 0) continue;
			camelcase += s.charAt(0);
			camelcase += s.toLowerCase().substring(1);
			camelcase += " ";
		}
		
		return camelcase.trim();
	}
    
	public static class NearStation {
		public NearStation(int idx, int km, Object[] gsod_meta) throws Exception {
			JSONArray gsod_city = (JSONArray) gsod_meta[1];
			//JSONArray gsod_cntr = (JSONArray) gsod_meta[2];
			JSONArray gsod_id   = (JSONArray) gsod_meta[0];
			JSONArray gsod_other= (JSONArray) gsod_meta[5];
			this.km = km;
			this.name = gsod_id.getString(idx);
			city = gsod_city.getString(idx);
			icao = gsod_other.getJSONArray(idx).optString(2);
			icao = icao == null ? "" : icao;
			years= gsod_other.getJSONArray(idx).getInt(0);
		}
		String city;
		int km;
		public String name;
		String icao;
		int years;
		@Override
		public String toString() {
			return "'" + city + "' - " + km + " [" + icao + "](" + name + "),y:" + years; 
		}
	}
    public static ArrayList<NearStation> getNearestStation(double lat1, double lon1, int tresholdKm, Object[] gsod_meta) throws Exception {
    	JSONObject gsod_loc = (JSONObject) gsod_meta[4];
    	JSONObject gsod_id_rev = (JSONObject) gsod_meta[6];
    	int gsodCount = gsod_id_rev.length();
    	//System.out.println("Total GSOD stations :"  + gsodCount);
    	
    	ArrayList<NearStation> stations = new ArrayList<GSODConnector.NearStation>();
    	for (int i = 0; i < gsodCount; i++) {
    	//for (String s : JSONObject.getNames(gsod_meta)) {
    		//if (!Character.isDigit(s.charAt(0))) continue;
            double lat2 = gsod_loc.getJSONArray("lat").getDouble(i); //.getDouble(LEGEND_ISH.get("lat"));
            double lon2 = gsod_loc.getJSONArray("lon").getDouble(i);
	        int distkm = distance(lat1, lon1, lat2, lon2, "K");
            if (distkm < tresholdKm) {
            	stations.add(new NearStation(i, distkm, gsod_meta));
            }
    	}
    	return stations;
    }

    private static int distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        double radlat1 = Math.PI * lat1/180;
        double radlat2 = Math.PI * lat2/180;
        double theta = lon1-lon2;
        double radtheta = Math.PI * theta/180;
        double dist = Math.sin(radlat1) * Math.sin(radlat2) + Math.cos(radlat1) * Math.cos(radlat2) * Math.cos(radtheta);
        dist = Math.acos(dist);
        dist = dist * 180/Math.PI;
        dist = dist * 60 * 1.1515;
        if (unit=="K") { dist = dist * 1.609344; }
        if (unit=="N") { dist = dist * 0.8684; }
        return (int)dist;
    }
    
	

}
