package com.vladi.gae1;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

@SuppressWarnings("serial")
public class GSODUploadServlet extends HttpServlet {
	public GSODUploadServlet() {
		log.setLevel(Level.INFO);
	}
	private static Logger log = Logger.getLogger(GSODUploadServlet.class.getName());
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)	throws ServletException, IOException {
		resp.getWriter().print("GSOD Uploader");
	}
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)	throws ServletException, IOException {
		try {
			System.err.println("GSOD UPloader: " + req.getParameter("GSOD_UPLOAD"));
			if (req.getParameter("GSOD_UPLOAD") == null) return;
			ZipInputStream zip = new ZipInputStream(req.getInputStream());
			ZipEntry zipEntry = null;
			while ((zipEntry = zip.getNextEntry()) != null) {
				String name = zipEntry.getName().split("\\.")[0];
				log.info("Uploading station: " + name);
				if (name.length() == 4) {
					String str = Utils.readTextInputStream(zip);
					JSONObject obj = new JSONObject(str);
					if (obj.has("alldays"))	obj = obj.getJSONObject("alldays");
					GAEUtils.storeGAEData(name, obj.toString());
				} else {
					GAEUtils.storeGAEData(name, zip);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(resp.getWriter());
			log.severe(e.toString());
			throw new ServletException(e);
		}
	}
}
