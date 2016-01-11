package com.vladi.gae1.gsod;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;

public class StationArchiver extends Thread {
	private JSONArray stationsId;
	private int endIdx;
	private String prefix;
	private int startIdx;
	GSODConnector gsod = new GSODConnector();

	public StationArchiver(JSONArray stationsId, int startIdx, int endIdx, String prefix) {
		this.stationsId = stationsId;
		this.startIdx = startIdx;
		this.endIdx = endIdx;
		this.prefix = prefix;
		System.out.println("StationArchiver: " + startIdx + " - " + endIdx);
	}
	
	public void run() {
		try {
			int filesPerZip = 100;
			ZipOutputStream zip = null;// = new ZipOutputStream(out)
			int idx = 0, filesInZip = 1;
			FileOutputStream fout = null;
			for (int i = startIdx; i < endIdx; i++) {
				if ((filesInZip % filesPerZip) == 0 || i == startIdx) {
					if (fout != null) {
						System.out.println("Closing: " + "./files/" + prefix + idx + ".zip" + ", i= " + i + ", si: " + startIdx);
//						zip.close();
//						fout.close();
						idx++;
						filesInZip = 1;
					}
					if (!new File("./files/ddd").exists()) new File("./files").mkdirs();
//					fout = new FileOutputStream("./files/" + prefix + idx + ".zip");
//					zip = new ZipOutputStream(fout);
					System.out.println("Writing ZIP: " + "./files/" + prefix + idx + ".zip");
				}
				String name = stationsId.getString(i);
				if (name.length() == 4) continue;
				
				new GSODStation("c:/develop/gsod", name);
//				ZipEntry zipEntry = new ZipEntry(name + ".json");
//				zip.putNextEntry(zipEntry);
//				//System.out.println("Processing station : " + name + " ");
//				JSONObject wdata = new GSODStation("c:/develop/gsod", name).getJSON();
//				zip.write(wdata.toString().getBytes());
//				zip.closeEntry();
				filesInZip ++;
				//new FileOutputStream(name + ".json").write(wdata.toString().getBytes());
			}
			if (filesInZip > 1) {
//				zip.close();
			}
//			fout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
