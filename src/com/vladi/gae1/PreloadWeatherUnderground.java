package com.vladi.gae1;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

public class PreloadWeatherUnderground {
	public static void preloadStation(String id) throws Exception {
		for (;;) {
			try {
				URL u = new URL("https://2-dot-vlast3k-gae1.appspot.com/webgae_real?icao=" + id + "&doUpdate=true&version=8&noForecast=true");
//				URL u = new URL("http://localhost:8888/webgae_real?icao=" + id + "&doUpdate=true&version=8&noForecast=true");
				System.out.print("Requesting: " + u + " ... ");
				String res = Utils.readTextInputStream(u.openStream());
				JSONObject jr = new JSONObject(res);
				String years = jr.optString("years");
				if (years == null || years.length() == 0) {
					System.out.println("done");
					new FileOutputStream("c:\\develop\\gsod\\onlywu\\" + id + ".json").write(res.getBytes());;
					break;
				}
				else System.out.println(years + " years");
				//Thread.sleep(5000);
			} catch (Exception e) {
				System.err.println(e.toString());
			}
		}
		
	}
	public static void main(String[] args) throws JSONException, IOException {
		JSONObject stations = new JSONObject(Utils.readTextFile("c:/develop/workspace_ad/webGAE_Real/AirportsLocations_hash.json"));
		//int toSkip = 2;
		String[] allStations = JSONObject.getNames(stations);
		for (int i=1023; i < allStations.length; i++) {
			JSONObject station = stations.getJSONObject(allStations[i]);
			if (!station.getString("cont").equals("United States")) continue; 
			for (;;) {
				try {
					URL u = new URL("https://2-dot-vlast3k-gae1.appspot.com/webgae_real?icao=" + allStations[i] + "&doUpdate=true&version=8&noForecast=true");
					System.out.print(i + ". Requesting: " + station.getString("cntr") + ", " + station.getString("city") + ", " + u + " ... ");
					String res = Utils.readTextInputStream(u.openStream());
					JSONObject jr = new JSONObject(res);
					String years = jr.optString("years");
					if (years == null || years.length() == 0) {
						System.out.println("done");
						break;
					}
					else System.out.println(years + " years");
					Thread.sleep(5000);
				} catch (Exception e) {
					System.err.println(e.toString());
				}
			}
			//break;
			
		}
	}
}
