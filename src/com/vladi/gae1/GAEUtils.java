package com.vladi.gae1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public class GAEUtils {
	private static Logger log = Logger.getLogger(WebGAE_RealServlet.class.getName());
	private static byte[] zipInput(InputStream in) throws Exception {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ZipOutputStream zip = new ZipOutputStream(bout);
		//log.warning("zip1");
		ZipEntry zipEntry = new ZipEntry("data");
		//log.warning("zip2");
		zip.putNextEntry(zipEntry);
		//log.warning("zip3");
		byte buf[] = new byte[10000];
		int len;
		while ((len=in.read(buf)) > -1) {
			//log.warning("read len: " + len);
			zip.write(buf, 0, len);
		}
		zip.closeEntry();
		zip.close();
		bout.close();
		//log.warning("bout size stream size: " + bout.toByteArray().length);
		return bout.toByteArray();
	}
	
	public static void storeGAEData(String icao, String data) throws Exception {
		storeGAEData(icao, new ByteArrayInputStream(data.getBytes()));
	}
		
   public static void storeGAEData(String icao, InputStream in) throws Exception {
	
		try {
			//log.severe("storing data for: " + icao);
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Key icaoKey = KeyFactory.createKey("Station", icao);
			//log.warning("storing data for: " + icao);
			Entity icaoData = new Entity(icaoKey);
			try {
				icaoData = datastore.get(icaoKey);
			} catch (EntityNotFoundException e) {
				//no problem as this would mean that the entity is not found
			}	
			Blob blob = new Blob(zipInput(in));
			//log.warning("zip entry size: " + blob.getBytes().length);
			icaoData.setProperty("data", blob);
			datastore.put(icaoData);
			//log.warning("data stored: " + icao);
		} catch (Exception e) {
			log.severe(e.toString());
			//throw e;
		}
	}
   

   public static String readGAEData(String icao) throws Exception {
	   try {
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			log.warning("requesting data for: " + icao);
			Key icaoKey = KeyFactory.createKey("Station", icao);
			Entity icaoData = null;
			try {
				icaoData = datastore.get(icaoKey);
			} catch (EntityNotFoundException e) {
				System.err.println("ReadGAEData - Entity NotFound: " + icao);
				//no problem as this would mean that the enttiy is not found
				return null;
			}
			if (icaoData.getProperty("data") == null) {
				System.err.println("ReadGAEData - Missing data field: " + icao);
				return null; // entity exists but there is no data field - e.g. forecast was created
			}
			Blob blob = (Blob)icaoData.getProperty("data");
			ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(blob.getBytes()));
			ZipEntry zipEntry = zip.getNextEntry();
			byte data[] = Utils.readBinaryInputStream(zip);
			return new String(data);
	   } catch (Exception e) {
		   log.severe(e.toString());
		   return null;
	   }
   }
}
