package com.vladi.gae1;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

public class AirportsDataBase {
	static class AirportData {
		public  String icao;
		public String city;
		public String country;
		public String continent;
		private double lat;
		private double lon;
		private String earliestHistoryData = "";
		public AirportData(String csvLine) {
			ArrayList<String> items = WunderGroundConnectorJSON.parseCSVLine(csvLine);
			icao = items.get(0).trim();
			city = items.get(1).trim();
			country = items.get(2).trim();
			continent = items.get(3).trim();
			lat = Double.parseDouble(items.get(4).trim());
			lon = Double.parseDouble(items.get(5).trim());
			if (items.size() >= 7) earliestHistoryData = items.get(6).trim();
		}
	}
	
	private String souceCSV = "AirportsLocations.csv";
	public  ArrayList<AirportData> allAirports = new ArrayList<AirportsDataBase.AirportData>();
	
	public AirportsDataBase() throws Exception {
		initAirportsFromCSV();
		
	}
	
	private void initAirportsFromCSV() throws Exception {
		long a = System.currentTimeMillis();
		FileInputStream fin = new FileInputStream(souceCSV);
		BufferedReader br = new BufferedReader(new InputStreamReader(fin));
		String line = null;
		while ((line = br.readLine()) != null) {
			if (line.startsWith("#")) continue;
			allAirports.add(new AirportData(line));
		}
		fin.close();
		System.out.println("Airports loaded in: " + (System.currentTimeMillis() -a ) + " ms, " + allAirports.size() + " airports loaded");
  }
	
	private HashMap<String, String> alreadyRequestedICAO = new HashMap<String, String>();

	private void storeAirportsToCSV() throws IOException {
		PrintStream ps = new PrintStream(souceCSV);
		for (AirportData ad : allAirports) {
			ps.format("%s,%s,%s,%s,%f,%f,%s\n", ad.icao, ad.city, ad.country, ad.continent, ad.lat, ad.lon, ad.earliestHistoryData);
		}
		ps.close();
	  // TODO Auto-generated method stub
	  
  }


}
