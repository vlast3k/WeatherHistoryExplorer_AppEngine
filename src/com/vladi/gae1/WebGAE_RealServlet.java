package com.vladi.gae1;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

@SuppressWarnings("serial")
public class WebGAE_RealServlet extends HttpServlet {
	public WebGAE_RealServlet() {
		log.setLevel(Level.INFO);
	}
	private static Logger log = Logger.getLogger(WebGAE_RealServlet.class.getName());
	
	private boolean getBooleanArg(HttpServletRequest req, String arg) {
		if (req.getParameter(arg) == null || req.getParameter(arg).equals("false")) return false;
		return true ;
	}
	private static JSONObject mappingWuGsod;
	public static JSONObject initMappingGsod(ServletContext servletContext) throws Exception {
		if (mappingWuGsod != null) return mappingWuGsod;
		//mappingWuGsod = new JSONObject();
		mappingWuGsod = new JSONObject(	Utils.readTextInputStream(new FileInputStream(servletContext.getRealPath("/mapping_wu_gsod.json"))));
		return mappingWuGsod;
	}
	
	public static JSONObject getMappingWuGsod() {
		return mappingWuGsod;
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		StringBuffer logBuffer = new StringBuffer();
		String icao = req.getParameter("icao");
		if (icao == null) throw new IOException("Expected value for ICAO");
		MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
		syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
		try {
			if (req.getParameter("post") != null) {
				doPost(req, resp);
				return;
			}
			logBuffer.append("Servlet.doPost: icao=" + icao + ", avg=" + req.getParameter("avg") + ", fc=" + req.getParameter("fc") + "\n");
			//log.info("read1j: " + Utils.readTextInputStream(new FileInputStream(getServletContext().getRealPath("/AirportsLocations_hash.json"))));
			JSONObject data = null;
			initMappingGsod(getServletContext());
			syncCache.put("KFFO", 0L);
			long r = 0;
			boolean isForecast = getBooleanArg(req, "fc");
			if (!isForecast) {
				try {
					if ((r = syncCache.increment(icao, 1, 0L)) != 1L) {
						logBuffer.append("Another instance is already processing for : " + icao + ", r=" + r + "\n");
						while (syncCache.get(icao) != null) Thread.sleep(100);
						logBuffer.append("Proceeding for: " + icao);
					} else {
						logBuffer.append("Got Initial lock for: " + icao + ", r = " + r +"\n");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				logBuffer.append("Forecast call is with prio\n");
			}
			//syncCache.put(icao,  icao);
			resp.setContentType("application/json");
			boolean doUpdate = getBooleanArg(req, "doUpdate");
			boolean noForecast = getBooleanArg(req, "noForecast");
			boolean hasAvg = getBooleanArg(req, "hasAvg");
			String lastYear = req.getParameter("lastYear");
			String version = req.getParameter("version");
			int clientVersion = getClientVersion(req);
			JSONObject ret = new JSONObject();
			JSONObject forecast = noForecast ? new JSONObject() : WUGForecastAdapter.retrieveForecast(icao, getServletContext(), logBuffer, clientVersion);
			TimeZone tz = WUGForecastAdapter.getTimeZone(forecast);
			JSONObject alldays = null;
			if (icao.length() == 4) {
				alldays = new WunderGroundConnectorJSON().getStats(icao, tz, doUpdate, logBuffer);
			} else {
				logBuffer.append("GSOD Case \n");
				alldays = WunderGroundConnectorJSON.loadWeatherData(icao, WUtils.getLegend());
			}
			//JSONObject avgData = hasAvg ? null : AvgGenerator.computeAverage(alldays, logBuffer);
			//ako device-a podade tova, toi is misli che ima avg, no ralno sus wseki novi danni to se update-va
			//i osobeno ako oshte nqma nishto, pri purviq call, poluchava greshni danni i posle se poluchava che max
			// e po nisko ot tekushtatatemperatura ili pr..
			JSONObject avgData =  AvgGenerator.computeAverage(alldays, logBuffer); 
			logBuffer.append("after compute avg: \n");

			
			ret.put("forecast", forecast);
			if (doUpdate && icao.length() > 4) {
				//GSOD Case
				if (version != null && version.length() > 0 && version.equals(""+alldays.getString("version")))
					stripAllDays(alldays, lastYear);
				ret.put("alldays", alldays);
			}
			else if (doUpdate && (alldays.optBoolean("hasMore", true) == false)) {
				if (version != null && version.length() > 0 && version.equals(""+alldays.getString("version"))) {
					//logBuffer.append("WILL STRIP, version = " + version);
					stripAllDays(alldays, lastYear);
				} 
//				else {
//					logBuffer.append("WILL NOOOOOOOOOOOT STRIP, version=" +  version);
//				}
//					
				ret.put("alldays",  alldays);
			} else {
				ret.put("years", alldays.getJSONObject("data").length());
			}
			//System.err.println(">>>> 4");
			if (avgData != null) ret.put("avg", avgData);
			resp.getOutputStream().write(ret.toString().getBytes());
			logBuffer.append("after write resp: \n");

		} catch (Exception e) {
			e.printStackTrace(resp.getWriter());
			throw new IOException(e);
		} finally {
			if (req.getParameter("fc") == null)	{
				logBuffer.append("Deleting: " + icao + " fc=" + req.getParameter("fc") + ".\n");
				try {
					syncCache.delete(icao);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			logBuffer.append("Servlet.doPost: icao=" + icao + " finished\n");
			log.info(logBuffer.toString());
		}
	}

    private int getClientVersion(HttpServletRequest req) {
    	try {
    		return Integer.parseInt(req.getParameter("clVer"));
    	} catch (Exception e) {
    		return 0;
    	}
	}

	private void stripAllDays(JSONObject alldays, String lastYear) {
    	try {
	    	int iLastYear = Integer.parseInt(lastYear);
	    	JSONObject data = alldays.getJSONObject("data");
	    	for (String y : JSONObject.getNames(data)) {
	    		int iy = Integer.parseInt(y);
	    		if (iy < iLastYear) data.remove(y);
	    	}
    	} catch (Exception e) {
    		
    	}
	}
    
	public static byte[] readBinaryInputStream(InputStream inp) throws Exception {
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    byte[] buf = new byte[10000];
	    int len;
	    while ((len=inp.read(buf)) > -1) {
	      bos.write(buf, 0, len);
	    }
	    return bos.toByteArray();
    }
	

//	public static void main(String[] args) throws Exception {
//		byte zzz [] = GAEUtils.zipInput(new ByteArrayInputStream("sadsadsadsadsadsa".getBytes()));
//		ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zzz));
//		ZipEntry zipEntry = zip.getNextEntry();
//		byte data[] = readBinaryInputStream(zip);
//		log.warning("request stream size: " + data.length);
//		System.out.println(new String(data));
//	}
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			GAEUtils.storeGAEData(req.getParameter("icao"), req.getInputStream());
		} catch (Exception e) {
			e.printStackTrace(resp.getWriter());
			log.severe(e.toString());
			throw new ServletException(e);
		}
	}
}
