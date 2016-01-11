package com.vladi.gae1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.apphosting.api.ApiProxy;



/*
 * data structure
 * { legend_data: {min:0, max:1, ....},
 *   legend_avg : {min:0, max:1, ....},
 *   data: {"2000": [[avg for year or empty][month_jan[0:avg for month or empty][(day1)1,3,5,2,4][(day2)3,2,]]
 *   										[month_feb[0:avg for month         ][(day1)....]]]
 *   }
 */
public class WunderGroundConnectorJSON extends WUtils {
	public static int finalYearTreshold = 250;

	private static final int MAX_WU_WORKERS = 2;
	private static Logger log = Logger.getLogger(WebGAE_RealServlet.class.getName());
	
	
	
//	public JSONObject getAll(String cityCode, boolean doUpdate) throws Exception {
//		JSONObject ret = new JSONObject();
//		JSONObject forecast = WUGForecastAdapter.retrieveForecast(cityCode, doUpdate, ); 
//		JSONObject allData = getStats(cityCode, doUpdate);
//		JSONObject avgData = AvgGenerator.computeAverage(allData);
//		
//		ret.put("forecast", forecast);
//		if (allData.getBoolean("hasMore") == false) {
//			ret.put("alldays",  allData);
//		}
//		ret.put("alldays",  allData);
//		ret.put("avg", avgData);
//		
//		return ret;
//	}
//	
	private StringBuffer logBuffer = new StringBuffer();
	public JSONObject getStats(String cityCode, TimeZone tz, boolean doUpdate, StringBuffer logBuffer) throws Exception {
		this.logBuffer = logBuffer;
		JSONObject weatherData = null;
		logBuffer.append("before load: \n");
		if (doUpdate) {
			weatherData = getHistory(cityCode, tz);
		} else {
			weatherData = loadWeatherData(cityCode, WUtils.getLegend());
		}
		logBuffer.append("after load: \n");
		return weatherData;
	}

	public JSONObject getHistory(String cityCode1, TimeZone tz) throws Exception {
		
		Calendar startDate = new GregorianCalendar(1985, 0, 1);
		if (WebGAE_RealServlet.getMappingWuGsod().has(cityCode1)) {
			startDate = new GregorianCalendar(2014, 0, 1);
		}
		Calendar endDate = getYesterday(tz);
		final JSONObject weatherData;
		boolean reachedEnd = false;
		boolean receivedData = false;
		logBuffer.append(Thread.currentThread() + " Requesting History for:" + cityCode1 + ", from:" + eetFormat_format(startDate.getTime()) + ", to:" + eetFormat_format(endDate.getTime()) + "\n");

		weatherData = loadWeatherData(cityCode1, WUtils.getLegend()); // load weatherData after the synchronized block is entered

		ArrayList<Thread> startedBackgroundThreads = new ArrayList<Thread>();
		try {
			startDate = (Calendar)startDate.clone();
			endDate = (Calendar)endDate.clone();
	
			int daysAdd = 0;
			Calendar yesterday = getYesterday(tz);
			if (yesterday.before(startDate)) startDate = (Calendar)yesterday.clone();
			//Note: WUG supports query for only ONE day, but NOT if this is for yesterday
			// so for simplicity - we will ensure that startDate is always at least one day before endDate
			//but perhaps here is not the best place
//				if (diff(startDate, endDate) > -1) {
//					//In case result for Yesterday is requested, only 1 day - then wunderground returns bad data
//					startDate.add(Calendar.DAY_OF_MONTH, -2);
//				}
//				//LinkedHashSet<CalendarDay> allMissing;
			for (Calendar currStart = (Calendar)endDate.clone(); 
					startedBackgroundThreads.size() <MAX_WU_WORKERS ;
					currStart.add(Calendar.YEAR, -1)) {
				if (currStart.before(startDate)) {
					reachedEnd = true;
					break;
				}

				final Calendar startBeforeOneYear = (Calendar)currStart.clone();
				startBeforeOneYear.add(Calendar.YEAR, -1);
				//substract 1 day, so that we always retrieve a full year and not more, so that the years do not overlap
				//if they do - then when writing to the db in paralell there may be problems
				startBeforeOneYear.add(Calendar.DAY_OF_MONTH, 1);
				
				final LinkedHashSet<CalendarDay> allMissing = getAllMissingForPeriod(weatherData, startBeforeOneYear, currStart);
				//logBuffer.append("allmissing contains: " + allMissing.size());
				// in case too much NULLs are found in the DB -this means that the last day has been found
				if (allMissing == null) {
					logBuffer.append(Thread.currentThread() + " found too many nulls, assuming DB contains end: " + d(startBeforeOneYear) + " - " + d(currStart) + "\n");
					reachedEnd = true;
					break;
				}
				//in case nothing to retrieve, then no thread needs to be started
				if (allMissing.size() == 0) continue;
				startedBackgroundThreads.add(startRetrieveCoreInBackground(weatherData, (Calendar)startBeforeOneYear.clone(), (Calendar)currStart.clone(), allMissing));
				//Thread.sleep(2000);
			}
			logBuffer.append("start waiting for end: ");
			while (startedBackgroundThreads.size() > 0) {
				boolean hasAlive = false;
				for (Thread t : startedBackgroundThreads) if (t.isAlive()) hasAlive = true;
				if (!hasAlive) break;
				receivedData = true;
				if (getRemainingSeconds() < 5) {
					logBuffer.append("\nLess than 5 seconds remaining, breaking wait!\n");
					break;
				}
				logBuffer.append(getRemainingSeconds() + ",");
				Thread.sleep(1000);
				//startedBackgroundThreads.get(0).join();
				//startedBackgroundThreads.remove(0);
//				if (ApiProxy.getCurrentEnvironment() != null && ApiProxy.getCurrentEnvironment().getRemainingMillis() < 5) {
//					logBuffer.append("Remaining ms: " + ApiProxy.getCurrentEnvironment().getRemainingMillis() + "\n");
//				}
			}
		}  catch (Exception e) {
			log.severe("WundegroundConnetor.getHistory: " + e);
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			logBuffer.append("Exception in WundergroundConnector.getHistory: " + sw.getBuffer().toString());
		}
	
		weatherData.put("hasMore", reachedEnd == false);
		weatherData.put("receivedData", receivedData);
		weatherData.put("icao", cityCode1);
		if (receivedData) storeWeatherData(weatherData);
		logBuffer.append("done\n");
		return weatherData;
		
    }
	
	private int getRemainingSeconds() {
		if (ApiProxy.getCurrentEnvironment() != null) {
			return (int)(ApiProxy.getCurrentEnvironment().getRemainingMillis()/1000);
		}
		return 100000;
	}

	private Thread startRetrieveCoreInBackground(final JSONObject weatherData,
			final Calendar startBeforeOneYear, final Calendar currStart,
			final LinkedHashSet<CalendarDay> allMissing) {
		
		Runnable r = new Runnable() {
			public void run() {
				String data = null;
				try {
					data = retrieveCore(weatherData, startBeforeOneYear, currStart, allMissing);
				} catch (Exception e) {
					e.printStackTrace();
					log.severe("WundegroundConnetor.startRetrieveCoreInBackground: " + e);
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					logBuffer.append("data was: " + data + "\n");
				} 

			}
		};
		Thread t =  null;
		try {
			t = com.google.appengine.api.ThreadManager.currentRequestThreadFactory().newThread(r);
		} catch (Exception e) {
			t = new Thread(r);
		}
		t.start();

		return t;
	}

	public String retrieveCore(JSONObject weatherData, final Calendar currStart, final Calendar endAfterOneYear, final LinkedHashSet<CalendarDay> allMissing) throws Exception, IOException, ParseException, SQLException {
		  String data;
		  //JSONObject wdata = weatherData.getJSONObject("data");
		  //start retrieving from the first missing date. This way we will avoid retrieving data for a full year if only the last week is missing
		  //if there are some days in between for which there is data - they will be skipped
		  data = retrieveFromWunderground(weatherData.getString("icao"), allMissing.iterator().next().getDate(), endAfterOneYear);
		  //System.err.println(data);
		  BufferedReader br = new BufferedReader(new StringReader(data));
		  String line;
		  while ((line = br.readLine()).length() == 0);
		  ArrayList<String> headers = parseCSVLine(line);
		  weatherData.put("tz", headers.get(0));
		  headers.set(0, "Time");//needed because time depends on time zone of the location and summer or winter
		  CalendarDay cd = new CalendarDay(null);
		  while ((line = br.readLine()) != null) {
		  	if (line.trim().length() == 0) continue;
		  //	logBuffer.append(line);
		  	ArrayList<String> lineValues = parseCSVLine(line);
		  	HashMap<String, String> entry = new HashMap<String, String>();
		  	for (int i=0; i < headers.size(); i++) {
		  		entry.put(headers.get(i), lineValues.get(i));
		  	}
		  	convertEntryFtoC(entry);
		  	if (entry.get("Max TemperatureC") == null || entry.get("Max TemperatureC").length() == 0 || Double.parseDouble(entry.get("Max TemperatureC")) < -100) {
		  		//it is possible that an invalid date is returned. In this case skip to next line and ignore this one, it will be marked as non-data-day afterwards
		  		continue;
		  	}
		  	cd.reuse(getDateFromLine(entry));
		  	//System.err.println("allmissing size is: " + allMissing.size() + " cd: " + eetFormat_format(cd.getDate().getTime()));
		  	if (allMissing.contains(cd)) { //here we skip 29.Feb as it not even listed in allMissing
		  		//System.err.println("will insert: " + eetFormat_format(cd.getDate().getTime()));
		  		insertDayEntry(weatherData, cd, entry);
		  	}
		  }
		  int totalEmpty = fillEmptyDaysForPeriod(weatherData, currStart, endAfterOneYear);
		  if (totalEmpty > finalYearTreshold) {
			  logBuffer.append(Thread.currentThread() + " found empty days: " + totalEmpty + ", " + d(currStart) + " - " + d(endAfterOneYear) + ", data length: " + data.length() + "\n");
		  	//semaphors.get(weatherData).foundEnd = true;
		  }
		  return data;
    }

	private String retrieveFromWunderground(String cityCode, Calendar start1, Calendar end) throws Exception {
		//Note: WUG supports query for only ONE day, but NOT if this is for yesterday
		// so for simplicity - we will ensure that startDate is always at least one day before endDate
		Calendar start = (Calendar)start1.clone();
		start.add(Calendar.DAY_OF_MONTH, -1);
		logBuffer.append(Thread.currentThread() + " Requesting Wunderground for:" + cityCode + ", from:" + d(start) + ", to:" + d(end) + "\n");

		String url = String.format("http://www.wunderground.com/history/airport/%s/%d/%d/%d/CustomHistory.html?dayend=%d&monthend=%d&yearend=%d&req_city=NA&req_state=NA&req_statename=NA&format=1", 
				cityCode, 
				start.get(Calendar.YEAR), start.get(Calendar.MONTH) + 1, start.get(Calendar.DAY_OF_MONTH),
				end.get(Calendar.DAY_OF_MONTH), end.get(Calendar.MONTH) + 1, end.get(Calendar.YEAR));
		logBuffer.append(Thread.currentThread() + " Requesting wunderground: " + url + "\n");
		long a = System.currentTimeMillis();
		String data = null;
		for (int i=0; i < 3; i++ ) {
			try { 
				logBuffer.append(Thread.currentThread() + " Requesting wundergroundm try#" + i + ": " + url + "\n");
				URL url2 = new URL(url);
				HttpURLConnection conn = (HttpURLConnection)url2.openConnection();
				conn.setReadTimeout(30000);
				conn.setConnectTimeout(10000);
				conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
				conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:19.0) Gecko/20100101 Firefox/19.0");
				//conn.setRequestProperty("cookie", "DT=1359786187:31154:365-b9; ASC=1364041369:22; __utma=203569430.373297991.1359786179.1361607978.1361723337.11; __utmz=203569430.1361593810.9.3.utmcsr=google|utmccn=(organic)|utmcmd=organic|utmctr=(not%20provided); __gads=ID=32272df0c0131411:T=1359786189:S=ALNI_Mb-S59-57pALHEMIJbupm_85zIh3g; JS=ON; __qca=P0-1725992421-1359786180232; cto_wunderus=; Prefs=FAVS:1|WXSN:1|PWSOBS:1|WPHO:1|PHOT:1|RADC:0|RADALL:0|HIST0:NULL|GIFT:1|PHOTOTHUMBS:50|HISTICAO:KJBR*LBSF*NULL|SHOWMETAR:1|; __atuvc=10%7C6%2C5%7C7; rc=%2C00000.15549%2C00000.04013%2C00000.04038%2C00000.04082%2C00000.15552; EmailCookie=Vlast3k%40gmail.com; EmailAuth=6358935; NoAdsCookie=0012025e7e2b762f663b26382a3d7263795a5a5b");
				if (conn.getResponseCode() == 200) {
					String encoding = conn.getHeaderField("Content-encoding");
					encoding = "deflate";
					if ("deflate".equals(encoding)) {
						data = Utils.readTextInputStream(new InflaterInputStream(conn.getInputStream(), new Inflater(true)));
					} else if ("gzip".equals(encoding)) {
						data = Utils.readTextInputStream(new GZIPInputStream(conn.getInputStream()));
					} else {
						data = Utils.readTextInputStream(conn.getInputStream());
					}
				}
				conn.disconnect();
				break;
			} catch (Exception e) {
				e.printStackTrace(System.err);
				System.err.println(Thread.currentThread() + " While reading: " + e.toString());
			}
		}
		logBuffer.append(Thread.currentThread() + " Request took: " + (System.currentTimeMillis() - a) + "\n");
		//System.err.println(data);
		return data;
	}
	 
	public static synchronized LinkedHashSet<CalendarDay> getAllMissingForPeriod(JSONObject weatherData, Calendar start, Calendar end) throws Exception {
		// used for periods long about 1 year
		start = getDayStart(start);
		end = getDayStart(end);

		LinkedHashSet<CalendarDay> allmissing = new LinkedHashSet<CalendarDay>();
		int countNull = 0;
		end.add(Calendar.DAY_OF_MONTH, 1);
		JSONObject data = weatherData.getJSONObject("data");
		for (; start.before(end); start.add(Calendar.DAY_OF_MONTH, 1)) {
			String year = "" + start.get(Calendar.YEAR);
			int month = start.get(Calendar.MONTH);
			int day = start.get(Calendar.DAY_OF_MONTH);
			if (month == 1 && day == 29) continue; // skip 29 feb
			if (data.has(year)) {
				JSONArray jy = data.getJSONArray(year);
				if (jy.length() - 1 > month && !jy.isNull(month + 1)) {
					JSONArray jm = jy.getJSONArray(month + 1);
					if (jm.length() > day && !jm.isNull(day)) {
							if (jm.getJSONArray(day).length() == 0) {
								if (("2015".equals(year) && month == 11 && day == 31) || 
									("2016".equals(year) && month == 0  && day < 12)) {
									  //there was a temporary outage during this time and those days are wrong
										// so for now in case they are found to be null - they will always be requested
									allmissing.add(new CalendarDay((Calendar) start.clone()));
								} else {								
									countNull++;
									if (countNull > finalYearTreshold)
										return null;
								}
							}
							continue;
						
					}
				}
			}
			allmissing.add(new CalendarDay((Calendar) start.clone()));
		}
		return allmissing;
	}
	
	private synchronized int fillEmptyDaysForPeriod(JSONObject weatherData, Calendar currStart, Calendar endAfterOneYear) throws Exception {
		currStart = getDayStart(currStart);
		endAfterOneYear = getDayStart(endAfterOneYear);
		LinkedHashSet<CalendarDay> allMissingForPeriod = getAllMissingForPeriod(weatherData, currStart, endAfterOneYear);

		if (allMissingForPeriod == null) return 0;
		//System.err.println(Thread.currentThread() + " Filling empty days for:" +  d(currStart) + " " + d(endAfterOneYear) + " " + allMissingForPeriod.size());
		for (CalendarDay c : allMissingForPeriod) 	insertDayEntry(weatherData, c, null);
		return allMissingForPeriod.size();
	}

	private synchronized void insertDayEntry(JSONObject weatherData, CalendarDay date, HashMap<String, String> entry) throws SQLException, JSONException {
		String year  =  "" + date.getDate().get(Calendar.YEAR);
		int month = date.getDate().get(Calendar.MONTH);
		int day   = date.getDate().get(Calendar.DAY_OF_MONTH);

		JSONArray jd = getWeatherDataDay(weatherData, year, month, day);
		
		if (entry == null) return;
		
		String ev = entry.get("Events");
		ev = ev == null ? "" : ev.replaceAll("Rain", "r").replaceAll("Thunderstorm", "t").replaceAll("Hail", "r")
								 .replaceAll("Snow", "s").replaceAll("Fog", "f").replaceAll("-", "");
		entry.put("Events", ev);
		
		for (String e[] : usefulEntries) {
			try {
				int iv;
				if (e[0].equals("Events")) {
					jd.put(entry.get(e[0]));
					continue;
				}
				if (e[0].equals("CloudCover") && entry.get(e[0]).length() == 0) iv = 0;
				else iv = (int)Math.round(Double.parseDouble(entry.get(e[0])));
				
				if (e[0].equals("Precipitationmm")) iv = cap(iv, 0, 255);
				if (e[0].equals("Max Gust SpeedKm/h")) iv = cap(iv, 0, 255);
				if (e[0].equals("Max Wind SpeedKm/h")) iv = cap(iv, 0, 255);
				if (e[0].equals("Mean Wind SpeedKm/h")) iv = cap(iv, 0, 255);
				if (e[0].equals("CloudCover")) iv = cap(iv, 0, 15);
				jd.put(iv);
			} catch (Exception ex) {
				jd.put(0);
			}
			
		}
		
		if (jd.isNull(legendMap.get("MaxGust")) ||
			   	 jd.getInt(legendMap.get("MaxGust")) <  jd.getInt(legendMap.get("MaxWind"))) 
			jd.put((int)legendMap.get("MaxGust"), jd.getInt(legendMap.get("MaxWind")));
	}



	

	public static void main(String[] args) throws Exception {
//		String xx = "a-b-c-d";
//		String[] split = xx.split("-");
//		System.out.println(split);
//		List<String> asList = Arrays.asList(split);
//		HashSet<String> ev = new HashSet<String>(asList);
	//	System.out.println("" + Math.floor((double)-20/100) + " " + Math.floor((double)(20/100)) + " " + Math.floor((double)(120/100)) + " " + Math.floor((double)(-120/100)));
//	GregorianCalendar gg = new GregorianCalendar(2011, 0, 1);
//	String dd = "2011-2-10";
//	Date eet = eetFormat.parse(dd);
//	Calendar xx = GregorianCalendar.getInstance();
//	//System.err.println(eetFormat_format(eet));
//	xx.setTime(eet);
//	System.err.println(xx.get(Calendar.MONTH));
//	GregorianCalendar g2 = new GregorianCalendar(eet.getYear() + 1900, eet.getMonth(), eet.getDate());
//	gg.compareTo(g2);
//		Calendar c = new GregorianCalendar(2011, 1,1);
//		System.out.println(d(c));
//		System.out.println(d(getDayStart(c)));
		//Thread.sleep(1000000);
//
		
	final WunderGroundConnectorJSON wunderGroundConnector = new WunderGroundConnectorJSON();
	//wunderGroundConnector.getAll("LBSF", true);
	System.out.println(wunderGroundConnector.loadWeatherData("LBSF", WUtils.getLegend()).toString());
	System.out.println(wunderGroundConnector.getHistory("LBSF", TimeZone.getDefault()).toString(1));
//	   JSONObject wd = wunderGroundConnector.loadWeatherData("LBSF");
//	   LinkedHashSet<CalendarDay> allMissingForPeriod = wunderGroundConnector.getAllMissingForPeriod(wd, new GregorianCalendar(2013, 10, 12), new GregorianCalendar(2013, 10, 13));
////	   System.out.println(allMissingForPeriod);
////	   System.out.println(allMissingForPeriod.size());
//	   wunderGroundConnector.retrieveCore(wd, new GregorianCalendar(2013, 10, 12), new GregorianCalendar(2013, 10, 13), allMissingForPeriod);
//	   System.out.println(wd.toString(2));
//
//	   allMissingForPeriod = wunderGroundConnector.getAllMissingForPeriod(wd, new GregorianCalendar(2009, 0, 5), new GregorianCalendar(2009, 0,25));
//	   System.out.println(allMissingForPeriod);
//	   System.out.println(allMissingForPeriod.size());
//	   wunderGroundConnector.retrieveCore(wd, new GregorianCalendar(2009, 0, 5), new GregorianCalendar(2009, 0,25), allMissingForPeriod);
//	   System.out.println(wd.toString(2));
//		JSONObject jobj = wunderGroundConnector.getStats("EGLL");
//		new FileOutputStream("res.json").write(jobj.toString().getBytes());
		//wunderGroundConnector.retrieveForAllAirports();
		//49
		//wunderGroundConnector.getHistory("KJBR");
		//wunderGroundConnector.test();
		//wunderGroundConnector.getHistory("KMXF");
//	System.err.println("Expected to retrieve from july 96");
//	wunderGroundConnector.getHistory("LBSF", new GregorianCalendar(1996, 0, 1), GregorianCalendar.getInstance());
//	System.err.println("two requests expected");
		//wunderGroundConnector.getHistory("LBSF", new GregorianCalendar(2003, 2, 27), new GregorianCalendar(2004, 3, 2));
//	System.err.println("data already exists");
		//wunderGroundConnector.getHistory("LBSF", new GregorianCalendar(1996, 0, 1), new GregorianCalendar(1996, 8, 1));
//	System.err.println("one request expected");
//		wunderGroundConnector.getHistory("ETOR", new GregorianCalendar(2009, 0, 1), new GregorianCalendar(2010, 11, 30));
	//wunderGroundConnector.getHistory("LBSF");
//		wunderGroundConnector.getHistory("LBSF", new GregorianCalendar(1995, 1, 15), new GregorianCalendar(1995, 5, 11));
//	wunderGroundConnector.saveDB();
		//get maxTemp for last 5 years, for last 5 days
		//get avgWeekTemp for last 5 years, for last 5 weeks, weekN = [ 7*(n-1); 7n)
		//wunderGroundConnector.getStats("LBSF", "Max TemperatureC", 2, -1, 0); // 2013.jan
//		KAFW 
//		KAEL 
//		KAHH 
//		new Thread() {
//			public void run() {
//				try {
//	        wunderGroundConnector.getStats("KAEL", "AVGTEMP", 10, -1, 1);
//        } catch (Exception e) {
//	        // TODO Auto-generated catch block
//	        e.printStackTrace();
//        } // 2013.feb - partial
//			};
//		}.start();
//		new Thread() {
//			public void run() {
//				try {
//					wunderGroundConnector.getStats("KAEL", "AVGTEMP", 10, -1, 1); // 2013.feb - partial
//        } catch (Exception e) {
//	        // TODO Auto-generated catch block
//	        e.printStackTrace();
//        } // 2013.feb - partial
//			};
//		}.start();
//		
//		Thread.sleep(100000);
		//wunderGroundConnector.getStats("LBSF", "yearly_period", 5, -1, 0); // 2013.feb - partial
		//wunderGroundConnector.getStats("LBSF", "AVGTEMP", 1, -1, 0); // 2013.feb - partial
//		wunderGroundConnector.getStats("LBSF", "AVGTEMP", 1, -1, 1); // 2013.feb - partial
//		wunderGroundConnector.getStats("LBSF", "AVGTEMP", 1, -1, 1); // 2013.feb - partial
//		wunderGroundConnector.getStats("LBSF", "AvgTemp", 1, -1, 2); // 2012.march
//		wunderGroundConnector.getStats("LBSF", "AvgTemp", 1, 1,-1);
//		wunderGroundConnector.getStats("LBSF", "AvgTemp", 2, 1,-1);
//		wunderGroundConnector.getStats("LBSF", "AvgTemp", 2, 2,-1);
//		wunderGroundConnector.getStats("LBSF", "Mean TemperatureC", 10, 5);
		//wunderGroundConnector.getStats("EHEH", "yearly_period", 20, 30, -1);
	}

//	private void test() throws IOException, ParseException, SQLException, Exception {
//		final LinkedHashSet<CalendarDay> allMissing = getAllMissingForPeriod("KCHD", new GregorianCalendar(2011, 1,1), new GregorianCalendar(2011, 11, 20));
//		retrieveCore("KCHD", new GregorianCalendar(2011, 1,1), new GregorianCalendar(2011, 11, 20), allMissing);
//  }

//	public int retrieveOneYearWUInBackground(final JSONObject weatherData, final Calendar currStart0, final Calendar endAfterOneYear0, ArrayList<Thread> startedBackgroundThreads) throws Exception {
//	final Calendar currStart = getDayStart(currStart0);
//	final Calendar endAfterOneYear = getDayStart(endAfterOneYear0);
//	final LinkedHashSet<CalendarDay> allMissing = getAllMissingForPeriod(weatherData, currStart, endAfterOneYear);
//	// in case too much NULLs are found in the DB -this means that the last day has been found
//	if (allMissing == null) {
//		System.err.println(Thread.currentThread() + " found too many nulls, assuming DB contains end: " + d(currStart) + " - " + d(endAfterOneYear));
//		return STATUS_REACHED_END;
//	}
//	//in case nothing to retrieve, then no thread needs to be started
//	if (allMissing.size() == 0) return STATUS_ALL_DATA_EXISTS;
//	synchronized (semaphors.get(weatherData)) {
//		while (semaphors.get(weatherData).currentWorkers >= MAX_WU_WORKERS) {
//			semaphors.get(weatherData).wait();
//		}
//		if (semaphors.get(weatherData).foundEnd) return STATUS_REACHED_END;
//		semaphors.get(weatherData).currentWorkers ++;
//		Thread.sleep(100);
//	}
//	Runnable r = new Runnable() {
//		public void run() {
//			String data = null;
//			try {
//				data = retrieveCore(weatherData, currStart, endAfterOneYear, allMissing);
//			} catch (Exception e) {
//				e.printStackTrace();
//				System.err.println("data was: " + data);
//			} finally {
//				synchronized (semaphors.get(weatherData)) {
//					if (ApiProxy.getCurrentEnvironment() != null && ApiProxy.getCurrentEnvironment().getRemainingMillis() < 20000) {
//						System.err.println("Remaining ms: " + ApiProxy.getCurrentEnvironment().getRemainingMillis());
//						semaphors.get(weatherData).foundEnd = true;
//					}
//					semaphors.get(weatherData).currentWorkers --;
//					semaphors.get(weatherData).notify();
//				}
//			}
//		}
//	};
//	Thread t =  null;
//	try {
//		t = com.google.appengine.api.ThreadManager.currentRequestThreadFactory().newThread(r);
//	} catch (Exception e) {
//		t = new Thread(r);
//	}
//	t.start();
//	if (startedBackgroundThreads != null)	startedBackgroundThreads.add(t);
//	return STATUS_STARTED_THREAD;
//}
	
}
