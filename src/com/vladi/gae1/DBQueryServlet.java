package com.vladi.gae1;

import static com.google.appengine.api.datastore.FetchOptions.Builder.withOffset;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Text;
import com.vladi.gae1.gsod.GSODStations;

@SuppressWarnings("serial")
public class DBQueryServlet extends HttpServlet {
	public DBQueryServlet() {
		log.setLevel(Level.INFO);
	}
	private static Logger log = Logger.getLogger(DBQueryServlet.class.getName());
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		//query1(req, resp);
		query2(req, resp);
	}
	
    private void query2(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    	try {
			resp.getWriter().print(GAEUtils.readGAEData(req.getParameter("icao")));
		} catch (Exception e) {
			throw new IOException(e);
		}
		
	}

	private void query1(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			Query q = new Query("Airport");
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			long now = System.currentTimeMillis();
			resp.setContentType("application/json");
			PreparedQuery pq = datastore.prepare(q);
			int total = 0;
			ArrayList<String> res = new ArrayList<String>(); 
			JSONArray arr = new JSONArray();
			for (int i=Integer.parseInt(req.getParameter("start")); i<Integer.parseInt(req.getParameter("end")); i+=500) {
				//int a = pq.countEntities(withOffset(i).limit(500));
				int a =0;
				for (Entity e: pq.asIterable(withOffset(i).limit(500))) {
					arr.put(e.getKey().getName());
					a++;
				}
				if (a==0) break;
			}
			resp.getWriter().write(arr.toString());
		} catch (Exception e) {
			e.printStackTrace(resp.getWriter());
		}
	}
    
    public static void main(String[] args) throws Exception {
    	JSONObject wuap = new JSONObject(Utils.readTextFile("c:/develop/workspace_ad/webGAE_Real/AirportsLocations_hash.json"));
    	String wuids[] = JSONObject.getNames(wuap);
    	Arrays.sort(wuids);
    	int i=0;
    	for (String icao:wuids) {
    		System.out.println(i + ", " + icao);
    		String data = Utils.readTextInputStream(new URL("http://2.vlast3k-gae1.appspot.com/dbquery?icao=" + icao).openStream());
    		FileOutputStream out = new FileOutputStream("c:\\develop\\gsod\\allwu\\" + icao + ".json");
    		out.write(data.getBytes());
    		out.close();
    		
    	}

//        JSONArray all = new JSONArray();
//		for (int i=0; i<30000; i+=500) {
//			System.out.println("i=" + i);
//			JSONArray xx = new JSONArray(Utils.readTextInputStream(new URL("http://2.vlast3k-gae1.appspot.com/dbquery?start="+i+"&end=" + (i+500)).openStream()));
//			for (int x=0; x<xx.length(); x++) {
//				all.put(xx.get(x));
//			}
//		}
//		new FileOutputStream("allIds.json").write(all.toString().getBytes());
	}
}
