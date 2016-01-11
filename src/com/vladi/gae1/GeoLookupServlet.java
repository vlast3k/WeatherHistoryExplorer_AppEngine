package com.vladi.gae1;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

@SuppressWarnings("serial")
public class GeoLookupServlet extends HttpServlet {
	public GeoLookupServlet() {
		log.setLevel(Level.INFO);
	}
	private static Logger log = Logger.getLogger(GeoLookupServlet.class.getName());
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)	throws ServletException, IOException {
		String url = "http://api.wunderground.com/api/76ec67c0b351975f/geolookup/q/autoip.json?geo_ip=" + req.getRemoteAddr();
		String ret = Utils.readTextInputStream(new URL(url).openStream());
		System.out.println("Requested is: " + url);
		System.out.println("Response is: " + ret);
		resp.getWriter().print(ret);
	}
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)	throws ServletException, IOException {
		try {
			log.info(Utils.readTextInputStream(req.getInputStream()));
		} catch (Exception e) {
			e.printStackTrace(resp.getWriter());
			log.severe(e.toString());
			throw new ServletException(e);
		}
	}
}
