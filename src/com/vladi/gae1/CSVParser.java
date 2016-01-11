package com.vladi.gae1;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

public class CSVParser {
	public static ArrayList<String> parseCSVLine(String line) {
		String[] split = line.split(",");
		ArrayList<String> ret = new ArrayList<String>();
		for (int i=0; i < split.length; i++) {
			if (split[i].length() > 0 && split[i].charAt(0) == '\"') split[i] = split[i].substring(1, split[i].length()-1); 
			ret.add(split[i].trim());		
		}
		return ret;
	}
	
	public ArrayList<HashMap<String, String>> parse(String data) throws Exception {
		  ArrayList<HashMap<String, String>> entries = new ArrayList<HashMap<String,String>>();
		  String line;

		  BufferedReader br = new BufferedReader(new StringReader(data));
		  while ((line = br.readLine()).length() == 0);
		  ArrayList<String> headers = parseCSVLine(line);
		  
		  while ((line = br.readLine()) != null) {
		  	if (line.trim().length() == 0) continue;
		  	ArrayList<String> lineValues = parseCSVLine(line);
		  	HashMap<String, String> entry = new HashMap<String, String>();
		  	for (int i=0; i < headers.size(); i++) entry.put(headers.get(i), lineValues.get(i));
		  	entries.add(entry);
		  }
		  return entries;
	}
}
