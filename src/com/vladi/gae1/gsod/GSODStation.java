package com.vladi.gae1.gsod;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.vladi.gae1.Utils;
import com.vladi.gae1.WUtils;

public class GSODStation extends Base {
	JSONObject json = null;
	public GSODStation(String dir, String stationName) throws Exception {
		json = processStation(dir, stationName);
	}
	
	private void processStationYear(String dir, String stationName, String year, JSONObject wdata) throws Exception {
		String gzName = String.format("%s/gsod_%s/%s-%s.op.gz", dir, year, stationName, year);
		if (new File(gzName).exists()) {
			parseGSODFile(stationName, Utils.readTextInputStream(new GZIPInputStream(new FileInputStream(gzName))), wdata);
		}
	}

	private JSONObject processStation(String dir, String stationName) throws Exception {
		JSONObject wdata = loadFromCache(dir, stationName);
		if (wdata == null) {
			wdata = loadFromSource(dir, stationName);
		} else {
			//processStationYear(dir, stationName, "2014", wdata);
			processStationYear(dir, stationName, "2016", wdata);
		}
		storeToCache(dir, stationName, wdata);
		return wdata;
	}
	
	private void storeToCache(String dir, String stationName, JSONObject wdata) throws Exception {
		String cacheDir = dir + "/cache";
		new File(cacheDir).mkdirs();
		String stationFile = cacheDir + "/" + stationName + ".gz";
		FileOutputStream fout = new FileOutputStream(stationFile);
		GZIPOutputStream gout = new GZIPOutputStream(fout);
		gout.write(wdata.toString().getBytes());
		gout.finish();
		gout.close();
		fout.close();
	}

	private JSONObject loadFromSource(String dir, String stationName) throws Exception {
		System.out.println("Load " + stationName + " from sources");
		JSONObject wdata = WUtils.createEmptyWeatherData(stationName, getLegendJSON());
		File [] files = new File(dir).listFiles();
		for (File f : files) {
			if (f.isDirectory() && f.getName().startsWith("gsod_")) {
				String year = f.getName().substring(5,9);
				processStationYear(dir, stationName, year, wdata);			
			}
		}
		//which stations are empty
		if (wdata.getJSONObject("data").toString().length() == 2) System.out.print("\"empty" + stationName + "\", ");
		return wdata;
	}

	private JSONObject loadFromCache(String dir, String stationName) throws Exception {
		FileInputStream fin = null;
		String cacheDir = dir + "/cache";
//		new File(cacheDir).mkdirs();
		String stationFile = cacheDir + "/" + stationName + ".gz";
		try {
			if (new File(stationFile).exists()) {
				fin = new FileInputStream(stationFile);
				String jsonString = Utils.readTextInputStream(new GZIPInputStream(fin));
				JSONObject json = new JSONObject(jsonString);
				fin.close();
				return json;
			}
		} catch (Exception e) {
			if (fin != null)fin.close();
			System.err.println("Error While processing: " + stationFile  + ", " + e.toString());
			//e.printStackTrace();
		}
		return null;
	}

	private Calendar getPrevDay(String year, int month, int day) {
		GregorianCalendar g = new GregorianCalendar(Integer.parseInt(year), month, day);
		g.add(Calendar.DAY_OF_MONTH, -1);
		return g;
	}
	
	private JSONArray getPrevWDataDay(JSONObject wdata, String year, int month, int day) throws JSONException {
		Calendar g = getPrevDay(year, month, day);
		return WUtils.getWeatherDataDay(wdata, "" + g.get(GregorianCalendar.YEAR), g.get(GregorianCalendar.MONTH), g.get(GregorianCalendar.DATE));
	}
	
	private JSONObject parseGSODFile(String stationCode, String data, JSONObject wdata) throws Exception {	
		//System.out.println("Parsing gsod file");
		BufferedReader rd = new BufferedReader(new StringReader(data));
		rd.readLine();
		String line = null;
		while ((line = rd.readLine()) != null) {
			String sMeanTemp = line.substring(24, 30);
			String sYear 	 = line.substring(14, 18);
			String sMonth 	 = line.substring(18, 20);
			String sDay 	 = line.substring(20, 22);
			String sWindSpd  = line.substring(78, 83);
			String sMaxWind  = line.substring(88, 93);
			String sGust     = line.substring(95, 100);
			String sMaxTemp  = line.substring(102, 108);
			String sMinTemp  = line.substring(110, 116);
			String sPrcp     = line.substring(118, 123);
			String sSndp     = line.substring(125, 130);
			char sFog	     = line.charAt(132);
			char sRain	  	 = line.charAt(133);
			char sSnow		 = line.charAt(134);
			char sHail		 = line.charAt(135);
			char sThunder  	 = line.charAt(136);
			char sTornado 	 = line.charAt(137);
			
			Integer iAvgTemp = tempToC(sMeanTemp);
			Integer iMaxTemp  = tempToC(sMaxTemp);
			Integer iMinTemp  = tempToC(sMinTemp);
			if (iMaxTemp == null || iMinTemp == null) continue;
			int iMonth = Integer.parseInt(sMonth) - 1;
			int iDay   = Integer.parseInt(sDay);
			Integer iWindSpd  = speedToKmh(sWindSpd);
			Integer iMaxWind  = speedToKmh(sMaxWind);
			Integer iGust     = speedToKmh(sGust);
			Integer iPrcp 	 = inchToMM(sPrcp);
			Integer iSndp	 = inchToMM(sSndp);
			String events = (sFog == '1' ? "f" : "") +
					(sRain == '1' ? "r" : "") +
					(sSnow == '1' ? "s" : "") +
					(sHail == '1' ? "h" : "") +
					(sThunder == '1' ? "t" : "") +
					(sTornado == '1' ? "o" : "") ;
			JSONArray dayData = WUtils.getWeatherDataDay(wdata, sYear, iMonth, iDay);
			//dayData.put(LEGEND.get("))
			int cloudCover = 1;
			if (sRain == '1' || sSnow == '1' || sThunder == '1' || sTornado == '1' || sHail == '1') cloudCover = 4; 
			putInteger(dayData, "MaxTemp", iMaxTemp);
			putInteger(dayData, "MaxTemp", iMaxTemp); 
			putInteger(dayData, "MinTemp", iMinTemp); 
			putInteger(dayData, "AvgTemp", iAvgTemp); 
			putInteger(dayData, "AvgWind", iWindSpd); 
			putInteger(dayData, "MaxWind", iMaxWind); 
			putInteger(dayData, "MaxGust", iGust); 
			putInteger(dayData, "Rain",    0);
			putInteger(getPrevWDataDay(wdata, sYear, iMonth, iDay), "Rain",    iPrcp);
//			} catch (Exception e) {
//				// in case we could not get hte previous date
//				e.printStackTrace();
//			}
			putInteger(dayData, "Snow",    iSndp); 
			putInteger(dayData, "CloudCover", cloudCover); 			
			dayData.put(LEGEND.get("Events"),  events); 
		}
		return wdata;
		
	}
	
	private static Integer tempToC(String ftemp) {
		ftemp = ftemp.trim();
		if (ftemp.equals("9999.9") || ftemp.length() == 0) return null;
		double f = Double.parseDouble(ftemp);
		int c = (int)Math.round((f - 32)/1.8);
		return c;
	}
	
	private static Integer speedToKmh(String knotsSpeed) {
		knotsSpeed = knotsSpeed.trim();
		if (knotsSpeed.equals("999.9") || knotsSpeed.length() == 0) return 0;
		double knots = Double.parseDouble(knotsSpeed);
		return (int)Math.round(knots * 1.852);
	}
	
	private static Integer inchToMM(String inch) {
		inch = inch.trim();
		if (inch.equals("99.99") || inch.equals(".00") || inch.equals("999.9") || inch.length() == 0) return 0;
		double dinch = Double.parseDouble(inch);
		return (int)Math.round(dinch * 2.54 * 10);
	}
	

	private static void putInteger(JSONArray arr, String key, Integer value) throws JSONException {
		arr.put(LEGEND.get(key), value == null? JSONObject.NULL : (int)value);
	}
	private static final HashMap<String, Integer> LEGEND = createLegend();
	private static HashMap<String, Integer> createLegend() {
		HashMap<String, Integer> legend = new HashMap<String, Integer>();
		int idx = 0;
		legend.put("MaxTemp", idx++); 
		legend.put("MinTemp", idx++);
		legend.put("MaxWind", idx++);
		legend.put("AvgWind", idx++);
		legend.put("MaxGust", idx++);
		legend.put("Rain", idx++);
		legend.put("AvgTemp", idx++);
		legend.put("CloudCover", idx++);
		legend.put("Events", idx++);
		legend.put("Snow", idx++);
		legend.put("Forecast", idx++);
		return legend;
	}
	
	public static JSONObject getLegendJSON() throws JSONException {
		JSONObject ret = new JSONObject();
		for (String key : LEGEND.keySet())  ret.put(key, LEGEND.get(key));	
		return ret;
	}

	public JSONObject getJSON() {
		return json;
	}
	
	public static void main(String[] args) throws Exception {
		FileOutputStream fout = new FileOutputStream("sofia.json");
		fout.write(new GSODStation("c:/develop/gsod", "156140-99999").getJSON().toString().getBytes());
		fout.close();
	}
}
