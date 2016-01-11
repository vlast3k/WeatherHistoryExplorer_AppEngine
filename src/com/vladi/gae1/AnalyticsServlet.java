package com.vladi.gae1;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

@SuppressWarnings("serial")
public class AnalyticsServlet extends HttpServlet {
	public AnalyticsServlet() {
		log.setLevel(Level.INFO);
	}
	private static Logger log = Logger.getLogger(AnalyticsServlet.class.getName());
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		query2(req, resp);
	}
	
    private void query1(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			Query q = new Query("Analytics").addSort("time", SortDirection.DESCENDING);
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			long now = System.currentTimeMillis();
			HashSet<String> seenDeviceIds = new HashSet<String>();
			resp.getWriter().println("<html><body><hr/>");
			for (Entity e : datastore.prepare(q).asIterable()) {
				Date d = null;
				try {
					d = idFormat.parse((String)e.getProperty("time"));
				} catch (Exception ef) {
					continue;
				}
				d.setYear(114);
				int days = (int)((now - d.getTime())/(1000*60*60*24));
				resp.getWriter().println("Days is : " + days);
				String deviceId = (String)e.getProperty("deviceId");
				if (days < 4) {
					seenDeviceIds.add(deviceId);
					continue;
				}
				resp.getWriter().println("Days is : " + days);
				if (seenDeviceIds.contains(deviceId)) continue;
				if (e.getProperty("startCount") == null || (Long)e.getProperty("startCount") > 1) {
					seenDeviceIds.add(deviceId);
					continue;
				}
				
				String value = ((Text)e.getProperty("logs")).getValue();
				value = value.replaceAll("\\[", "<br/>\\[");
				resp.getWriter().println(((Text)e.getProperty("logs")).getValue());
				resp.getWriter().println("<hr/>");
			}
			
			resp.getWriter().write("</body></html>");
		} catch (Exception e) {
			e.printStackTrace(resp.getWriter());
		}
	}

    private void query2(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			Query q = new Query("Analytics").addSort("time", SortDirection.ASCENDING);
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			long now = System.currentTimeMillis();
			HashSet<String> seenDeviceIds = new HashSet<String>();
			HashMap<String, Integer> devStartCount = new HashMap<String, Integer>();
			HashMap<String, Integer> androidNewPerPeriod = new HashMap<String, Integer>();
			HashMap<String, Integer> iosNewPerPeriod = new HashMap<String, Integer>();
			HashMap<String, Integer> androidAllPerPeriod = new HashMap<String, Integer>();
			HashMap<String, Integer> iosAllPerPeriod = new HashMap<String, Integer>();
			HashMap<String, Integer> iosChartsAllPer = new HashMap<String, Integer>();
			HashMap<String, Integer> andChartsAllPer = new HashMap<String, Integer>();
			HashSet<String> periodId = new HashSet<String>();
			resp.getWriter().println("<html><head><meta name='viewport' content='width=device-width, height=device-height, initial-scale=1.0, maximum-scale=1.0, user-scalable=no' /></head><body><hr/>");
			FetchOptions opts = FetchOptions.Builder.withChunkSize(500);
			for (Entity e : datastore.prepare(q).asIterable(opts)) {
				Date d = null;
				try {
					d = idFormat.parse((String)e.getProperty("time"));
				} catch (Exception ef) {
					continue;
				}
				d.setYear(114);
				int days = (int)((now - d.getTime())/(1000*60*60*24));
				String type = e.getProperty("type") == null ? "xxx"  : (String)e.getProperty("type").toString().toLowerCase();
				boolean isIOS = type.indexOf("iphone") > -1 || type.indexOf("ipad") > -1;
				String period = String.format("%02d.%02d", d.getMonth() +1, d.getDate());
				Long startCount = (Long)e.getProperty("startCount");
				String deviceId = (String)e.getProperty("deviceId").toString().toLowerCase();
				
				if (startCount == null) continue;

				boolean isNew = !devStartCount.containsKey(deviceId);
				Boolean hadCharts = (Boolean)e.getProperty("charts");
				if (hadCharts != null) System.err.println("Had Charts: " + hadCharts);
				//resp.getWriter().println("new: " + isNew + " id: " + deviceId + " devs: " + devStartCount.containsKey(deviceId) + "<br>");
				boolean isReturning = !isNew && startCount != null && startCount > 2 && (int)devStartCount.get(deviceId) != (long)startCount;
				devStartCount.put(deviceId, (int)(long)startCount);
				//resp.getWriter().println("put: " + deviceId + "<br>");
				HashMap<String, Integer> mapNew = isIOS ? iosNewPerPeriod : androidNewPerPeriod;
				HashMap<String, Integer> mapRet = isIOS ? iosAllPerPeriod: androidAllPerPeriod;
				HashMap<String, Integer> mapCharts = isIOS ? iosChartsAllPer: andChartsAllPer;
				if (isNew) {
					if (mapNew.get(period) == null) mapNew.put(period, 0);
					mapNew.put(period, mapNew.get(period) + 1);
				} else if (isReturning) {					
					if (mapRet.get(period) == null) mapRet.put(period, 0);
					mapRet.put(period, mapRet.get(period) + 1);
				}
				if (hadCharts!= null && hadCharts == true) {
					if (mapCharts.get(period) == null) mapCharts.put(period, 0);
					mapCharts.put(period, mapCharts.get(period)+1);
				}
				//if (!periodId.contains(period)) resp.getWriter().print(".");
				periodId.add(period);
			}
			
			String [] periods = periodId.toArray(new String[1]);
			Arrays.sort(periods);
			/*
			 * 
 Andr  New Ret ios New Ret
01.03  412 444  -  222 111
			 */
			resp.getWriter().print("<table>");
			resp.getWriter().println("<tr><td>Date Devices</td> <td>A.New</td> <td>Ret</td> <td>Charts</td> <td>I.New</td> <td>Ret</td> <td>Charts</td></tr>");
			for (int i = periods.length -1; i >= 0; i--) {
				String p = periods[i];
				resp.getWriter().format("<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>", p,
						androidNewPerPeriod.get(p), androidAllPerPeriod.get(p), andChartsAllPer.get(p),
						iosNewPerPeriod.get(p), iosAllPerPeriod.get(p), iosChartsAllPer.get(p));
			}
			resp.getWriter().print("</table>");
						
			
			resp.getWriter().write("</body></html>");
		} catch (Exception e) {
			e.printStackTrace(resp.getWriter());
		}
	}

	public int getPositivIntParam(HttpServletRequest req, String name) {
    	try {
    	  return Integer.parseInt(req.getParameter(name));
    	} catch (Exception e) {
    		return -1;
    	}
    }
	DateFormat idFormat = new SimpleDateFormat("M.dd.HH.mm");
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)	throws ServletException, IOException {
		try {
			String data = Utils.readTextInputStream(req.getInputStream());
			log.info(data);
			//&_ISNEW_&_elapsed=113s_&_type=--samsung--GT-I9300--&_startCount=0&_runTimeMin=1&_localH=12&_deviceId=00880ef1-44bb-4c97-b01f-d34765ab15f5
			int startCount = getPositivIntParam(req, "_startCount");
			int runtimeMin = getPositivIntParam(req, "_runTimeMin");
			int localH     = getPositivIntParam(req, "_localH");
//			int elapsed    = Integer.parseInt(req.getParameter("_elapsed"));
			String deviceId= req.getParameter("_deviceId");
			if (deviceId == null) deviceId = "" + System.currentTimeMillis();
			String type    = req.getParameter("_type");
			if (type == null) type = "";
			type = type.replaceAll("--", " ").trim();
			DateFormat eetFormat = new SimpleDateFormat("M.d");
			GregorianCalendar now = new GregorianCalendar(TimeZone.getTimeZone("Europe/Istanbul"));
			now.add(Calendar.HOUR, 2);
			String date = eetFormat.format(now.getTime());
				
			boolean openedCharts = data.indexOf("ChartsWindow.generateTempChartData") > -1;
			System.err.println("Opeend Charts = " + openedCharts);

			String idDate = idFormat.format(now.getTime());

			
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			String keyName = "[" + (startCount > -1 ? startCount : idDate) +"] " + deviceId;
			Key icaoKey = KeyFactory.createKey("Analytics", keyName);
			//log.warning("storing data for: " + icao);
			Entity icaoData = new Entity(icaoKey);
			try {
				icaoData = datastore.get(icaoKey);
			} catch (EntityNotFoundException e) {
				//no problem as this would mean that the entity is not found, and the newly created one will be used
			}
			
			if (startCount > -1) icaoData.setProperty("startCount", startCount);
			if (runtimeMin > -1) icaoData.setProperty("runTimeMin", runtimeMin);
			if (localH     > -1) icaoData.setProperty("localH", localH);
			icaoData.setProperty("date", date);
			icaoData.setProperty("time", idDate);
			icaoData.setProperty("charts", openedCharts);
			if (type.length() > 0) icaoData.setProperty("type", type);
			icaoData.setProperty("deviceId", deviceId);
			
			
			if (icaoData.hasProperty("logs")) {
				String logs = ((Text)icaoData.getProperty("logs")).getValue();
				logs += "\n" + data;
				icaoData.setProperty("logs", new Text(logs));
			} else {
				icaoData.setProperty("logs", new Text(data));
			}
			
			datastore.put(icaoData);
			System.out.println("Entity written");

		} catch (Exception e) {
			e.printStackTrace(resp.getWriter());
			log.severe(e.toString());
			//throw new ServletException(e);
		}
	}
}
