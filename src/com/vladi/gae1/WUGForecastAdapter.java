package com.vladi.gae1;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;
import com.vladi.gae1.gsod.GSODConnector;
import com.vladi.gae1.gsod.GSODStation;


public class WUGForecastAdapter {
	private static Logger log = Logger.getLogger(WebGAE_RealServlet.class.getName());

	private static JSONObject airportsJson = null;

	static HashMap<String, String> condMapping = new HashMap<String, String>();
	static {
		condMapping.put("chanceflurries", "5s");
		condMapping.put("chancerain", "5r");
		condMapping.put("chancesleet", "5rs");
		condMapping.put("chancesleet", "5rs");
		condMapping.put("chancesnow", "5s");
		condMapping.put("chancetstorms", "5r");
		condMapping.put("chancetstorms", "5r");
		condMapping.put("clear", "1");
		condMapping.put("cloudy", "7");
		condMapping.put("flurries", "4s");
		condMapping.put("fog", "5f");
		condMapping.put("hazy", "2");
		condMapping.put("mostlycloudy", "5");
		condMapping.put("mostlysunny", "4");
		condMapping.put("partlycloudy", "4");
		condMapping.put("partlysunny", "5");
		condMapping.put("sleet", "4sr");
		condMapping.put("rain", "5r");
		condMapping.put("sleet", "4sr");
		condMapping.put("snow", "5s");
		condMapping.put("sunny", "1");
		condMapping.put("tstorms", "5t");
		condMapping.put("tstorms", "5t");
		condMapping.put("unknown", "1x");
		condMapping.put("cloudy", "7");
		condMapping.put("partlycloudy", "5");
	}

	private static JSONObject loadAirportsJSON(ServletContext servletContext) throws JSONException, IOException {
		return  new JSONObject(Utils.readTextInputStream(new FileInputStream(servletContext.getRealPath("/AirportsLocations_hash.json"))));		
	}

	static JSONObject gsod_id_rev = null, gsod_loc = null;
	private static void preloadGSODMeta(ServletContext servletContext) throws JSONException, IOException {
		if (gsod_id_rev == null) gsod_id_rev = new JSONObject(Utils.readTextInputStream(new FileInputStream(servletContext.getRealPath("/all_id_rev.js"))).substring(24));
		if (gsod_loc == null) gsod_loc = new JSONObject(Utils.readTextInputStream(new FileInputStream(servletContext.getRealPath("/all_loc.js"))).substring(21));		
	}
	
	private static String getGSODLocation(String station, ServletContext servletContext) throws Exception {
		preloadGSODMeta(servletContext);
		int idx = gsod_id_rev.getInt(station);
		return gsod_loc.getJSONArray("lat").getDouble(idx) + "," + gsod_loc.getJSONArray("lon").getDouble(idx);
	}

	public static JSONObject retrieveForecast(String icao, ServletContext servletContext, StringBuffer logBuffer, int clientVersion) throws Exception {
		log.setLevel(Level.INFO);
		JSONObject legend = WUtils.getLegend();
		JSONObject ret = null;
		String location = "";
		try {
			try {
				if (airportsJson == null) airportsJson = loadAirportsJSON(servletContext);
				logBuffer.append("Retrieving Forecast for: " + airportsJson.getJSONObject(icao).getString("cont") + "/" + airportsJson.getJSONObject(icao).getString("cntr") + "/" + airportsJson.getJSONObject(icao).getString("city") + "\n");
				location = getICAOLocation(icao);
			} catch (Exception e) {
				logBuffer.append("Retriving Forecast for GSOD station " + icao + "\n");
				location = getGSODLocation(icao,  servletContext);
				legend = GSODStation.getLegendJSON();
			}
		} catch (Exception e) {
			log.severe(e.toString());
			e.printStackTrace();
		}
		String s = getStoredForecast(icao, clientVersion);
		if (s != null && s.length() > 0) {
			logBuffer.append("Stored forecast retrieved\n");
			try {
				JSONObject jForecast = new JSONObject(s);
				if (!jForecast.getString("version").equals("" + WUtils.version)) {
					logBuffer.append("Stored forecase is old version: " + jForecast.getString("version"));
				} else {
					Calendar c = new GregorianCalendar(getTimeZone(jForecast));
					JSONArray ff = jForecast.getJSONArray("forecastDate");
	//				log.info("forcast.curent time in timezone: " + c + ", " + c.get(Calendar.MONTH));
					if (ff.getInt(0) == c.get(Calendar.YEAR) &&
						ff.getInt(1) == c.get(Calendar.MONTH) + 1 &&
						ff.getInt(2) == c.get(Calendar.DAY_OF_MONTH)) {
						logBuffer.append("Forecast is current. Timezone: " + ff.getString(3) + "\n");
						ret =  jForecast;
					}
				}
			} catch (Exception e) {			
				e.printStackTrace();
				//in case some error during loading forecast -  do not care
			}
		}
		//return new JSONObject();
		if (ret == null) ret =  loadAndStoreForecastFromWUG(icao, location, legend, logBuffer, clientVersion);
		if (ret != null) ret.remove("version");
		return ret;
	}
	
	private static String getICAOLocation(String icao) throws JSONException {
		return airportsJson.getJSONObject(icao).getString("lat") + "," + airportsJson.getJSONObject(icao).getString("lon"); 
	}

	private static JSONObject loadAndStoreForecastFromWUG(String icao, String location, JSONObject legend, StringBuffer logBuffer, int clientVersion)  {
		String t;
		icao = icao.toUpperCase();
		for (int i=0; i < 3; i++) {
			try {
				URL u = new URL("http://api.wunderground.com/api/76ec67c0b351975f/forecast10day/q/" + location + ".json");
				logBuffer.append("Requesting forecast: " + u  + "\n");
				URLConnection openConnection = u.openConnection();
				openConnection.setReadTimeout(5000);
				t = Utils.readTextInputStream(openConnection.getInputStream());
				//log.info("Forecast received: " + t );
				t = processForecast(icao, t, legend, clientVersion);
				//log.info("Forecast processed: " + t );
				storeForecast(icao, t, clientVersion);
				logBuffer.append("Forecast stored\n");
				return new JSONObject(t);
			} catch (Exception e) {
				logBuffer.append("TRY " + i + "Error while retriving forecast: " + e.toString());
				log.severe("TRY " + i + " Error while retriving forecast: " + e.toString());
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e2) {
				}
				if (i < 2) continue;
				log.severe("RETRY ENDED");
				e.printStackTrace();
				try {
					return WUtils.createEmptyWeatherData(icao, WUtils.getLegend());
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); 
					return new JSONObject();
				}
			}
		}
		try {
			return WUtils.createEmptyWeatherData(icao, WUtils.getLegend());
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace(); 
			return new JSONObject();
		}
	}

	private static String processForecast(String icao, String forecast, JSONObject legend, int clientVersion) throws Exception {
		JSONObject data = new JSONObject(forecast);
		JSONObject weatherData = WUtils.createEmptyWeatherData(icao, legend);
		if (data.optJSONObject("response") != null && data.getJSONObject("response").optJSONObject("error") != null) {
			log.severe("NO Forecast available on WU for ICAO: " + icao);
			return weatherData.toString();
		}

		JSONObject fday = data.getJSONObject("forecast").getJSONObject("simpleforecast").getJSONArray("forecastday").optJSONObject(0);
		JSONArray date = new JSONArray();
		date.put(fday.getJSONObject("date").getInt("year"));
		date.put(fday.getJSONObject("date").getInt("month"));
		date.put(fday.getJSONObject("date").getInt("day"));
		date.put(fday.getJSONObject("date").getString("tz_long"));
		weatherData.put("forecastDate", date);
		try {
			int numdays  = data.getJSONObject("forecast").getJSONObject("simpleforecast").getJSONArray("forecastday").length();
			for (int i=0; i < numdays; i++) {
				getDay(weatherData, data, i, legend, clientVersion);
			}
		} catch (Exception e) {
			e.printStackTrace();
			//ignore
		}
		return weatherData.toString();
	}

	private static void getDay(JSONObject weatherData, JSONObject fdata, int day, JSONObject legend, int clientVersion) throws JSONException {
		JSONObject fday = fdata.getJSONObject("forecast").getJSONObject("simpleforecast").getJSONArray("forecastday").optJSONObject(day);
		if (fday == null) return;
		JSONArray jd = WUtils.getWeatherDataDay(weatherData, fday.getJSONObject("date").getString("year"), 
															 fday.getJSONObject("date").getInt("month") - 1,
															 fday.getJSONObject("date").getInt("day"));
		try {
			System.out.println(fday);
			try {
				jd.put(legend.getInt("MaxTemp"), (int)Integer.parseInt(fday.getJSONObject("high").getString("celsius")));
			} catch (NumberFormatException n) {}
			
			try {
				jd.put(legend.getInt("MinTemp"), (int)Integer.parseInt(fday.getJSONObject("low").getString("celsius")));
			} catch (NumberFormatException n) {}
		} catch (JSONException e) { 
			jd.put(legend.getInt("MaxTemp"), (int)fday.getJSONObject("high").getInt("celsius"));
			jd.put(legend.getInt("MinTemp"), (int)fday.getJSONObject("low").getInt("celsius"));
			
		}
		
		String cond = condMapping.get(fday.getString("icon"));
		if (cond != null && cond.length() > 0) {
			int cc = Integer.parseInt("" + cond.charAt(0));
			String ev = cond.substring(1);
			jd.put(legend.getInt("CloudCover"), cc);
			jd.put(legend.getInt("Events"), ev);
		}
		int pop = fday.getInt("pop");
		int qpf = fday.getJSONObject("qpf_allday").getInt("mm");
		if (clientVersion > 0) { //support for forecast probbability added
			jd.put(legend.getInt("Rain"), new JSONArray(new int[] {qpf, pop}));
		} else {
			jd.put(legend.getInt("Rain"), qpf);
		}
		jd.put(legend.getInt("Forecast"), true);
	}
	
	
	private static String getStoredForecast(String icao, int clientVersion) {
		try {
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			String keyId = icao;
			if (clientVersion > 0) keyId += "v" + clientVersion;
			Key icaoKey = KeyFactory.createKey("Forecast", keyId);
			Entity icaoData = datastore.get(icaoKey);
			Object ff = icaoData.getProperty("forecast");
			if (ff instanceof Text) {
				ff = ((Text)ff).getValue();
			}
			return (String)ff;
		}  catch (EntityNotFoundException en) {
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static void storeForecast(String icao, String value, int clientVersion) {
		try {
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			String keyId = icao;
			if (clientVersion > 0) keyId += "v" + clientVersion;
			Key icaoKey = KeyFactory.createKey("Forecast", keyId);
			Entity icaoData = new Entity(icaoKey);
			try {
				icaoData = datastore.get(icaoKey);
			} catch (EntityNotFoundException e) {
				//no problem as this would mean that the enttiy is not found
			}
			icaoData.setProperty("forecast", new Text(value));
			datastore.put(icaoData);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static TimeZone getTimeZone(JSONObject jForecast) {
		try {
			if (jForecast == null) return TimeZone.getDefault();
			JSONArray ff = jForecast.getJSONArray("forecastDate");
			return TimeZone.getTimeZone(ff.getString(3));
		} catch (Exception e) {
			return TimeZone.getDefault();
		}
	}

//	public static void main(String[] args) throws Exception {
//		String t = Utils.readTextFile("slivenfc.json");
//		System.out.println(t);
//		System.out.println(new WUGForecastAdapter().processForecast("rrrr", t, GSODStation.getLegendJSON(), 2));
//	}
}
