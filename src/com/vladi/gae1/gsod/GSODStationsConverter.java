package com.vladi.gae1.gsod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.json.JSONArray;

import com.vladi.gae1.Utils;
import com.vladi.gae1.gsod.GSODUploader.UploadFunc;
import com.vladi.gae1.gsod.Parallelizer.P;

public class GSODStationsConverter {
	static class ConvertOneStation implements P {
		@Override
		public void process(List data, int idx) throws Exception {
			new GSODStation("c:/develop/gsod", data.get(idx).toString());
		}
	}
	
	public void updateCache() throws Exception {
		System.out.println("Update cache");
		JSONArray stationsId = new JSONArray(Utils.readTextFile("c:/develop/workspace_ad/webGAE_Real/war/all_id.js").substring(20));
		ArrayList<String> listStations = new ArrayList<String>();
		for (int i=0; i < stationsId.length(); i++) {
			if (stationsId.getString(i).length() == 4) continue;
			listStations.add(stationsId.getString(i));
		}
		Parallelizer.start(listStations, ConvertOneStation.class, 5);
	}
	
	public void buildZipFiles() throws Exception {
		System.out.println("Build Zip Files");
		File dir = new File("c:/develop/gsod/cache");
		int idx = 1;
		int allfiles = dir.listFiles().length;
		File files[] = dir.listFiles();
		int filesPerZip = 100;
		if (!new File("./files/ddd").exists()) new File("./files").mkdirs();
		for (int i=0; i < allfiles; i+= filesPerZip) {
			FileOutputStream out = new FileOutputStream("./files/zip" + i + ".zip");
			ZipOutputStream zip = new ZipOutputStream(out);
			//zip.setLevel(0);
			for (int j = i; j < i+filesPerZip && j < allfiles; j++) {
				ZipEntry zipEntry = new ZipEntry(files[j].getName() + ".json");
				zip.putNextEntry(zipEntry);
				
				String json = Utils.readTextInputStream(new GZIPInputStream(new FileInputStream(files[j].getAbsolutePath())));
				zip.write(json.toString().getBytes());
				zip.closeEntry();
			}
			zip.close();
			out.close();
			System.out.println((int)((float)i/allfiles * 100) + "% completed");
			
		}

	
		
//		stationsId = new JSONArray();
//		stationsId.put("994016-99999");
//		int len = stationsId.length();
//		int th = 7;
//		ArrayList<Thread> tl = new ArrayList<Thread>();
//		for (int i=0; i < th; i++) {
//			Thread t = new StationArchiver(stationsId, i * len/th, (i+1) * len/th, "t" + i + "_");
//			t.start();
//			tl.add(t);
//		}
//		for (Thread t : tl) t.join();
	}
}
