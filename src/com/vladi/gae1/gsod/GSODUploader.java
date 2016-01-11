package com.vladi.gae1.gsod;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import com.vladi.gae1.gsod.Parallelizer.P;

public class GSODUploader {
	private static void uploadFile(String name) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) new URL("http://2.vlast3k-gae1.appspot.com/gsodupload?GSOD_UPLOAD=true").openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("content-type", "application/octet-stream");
		OutputStream out = conn.getOutputStream();
		InputStream in = new BufferedInputStream(new FileInputStream(name));
		byte buf[] = new byte[10000];
		int len = 0;
		int tot = 0;
		while ((len = in.read(buf)) > -1) {
			out.write(buf, 0, len);
			tot+= len;
			//System.out.println("sending: " + len);
		}
		System.out.println("Sent " + tot + " bytes");
		System.out.println("sent: " + conn.getResponseCode());
		out.close();
		in.close();
	}
	public static class UploadFunc implements P {
		@Override
		public void process(List data, int idx) throws Exception {
			File file = (File)data.get(idx);
			//if (args.length > 0 && file.getName().indexOf(args[0]) == -1) return;
			if (file.getName().endsWith(".zip")) {
				System.out.println(idx ++ + " / " + data.size() + " " + file.getName());
				uploadFile(file.getAbsolutePath ());
				//Thread.sleep(10);
			}
		}
	}
	static String[] args = null;
	public static void main(String[] args1) throws Exception {
		args = args1;
		//System.out.println("args: " + args.length);
		System.out.println("cache, zip, upload");
		if (args.length == 0 || "cache".equals(args[0])) {
			new GSODStationsConverter().updateCache();
		} 
		if (args.length == 0 || "zip".equals(args[0])) {
			new GSODStationsConverter().buildZipFiles();
		}
	    if (args.length == 0 || "upload".equals(args[0])) {
			File dir = new File("c:/develop/gsod/files");
			//File dir = new File("c:/develop/gsod/onlywu/zip");
			
			int idx = 1;
			int allfiles = dir.listFiles().length;
			Parallelizer.start(Arrays.asList(dir.listFiles()), UploadFunc.class, 4);
		} 
			
	    
	    System.exit(0);
			
//		for (File file : dir.listFiles()) {
//			if (args.length > 0 && file.getName().indexOf(args[0]) == -1) continue;
//			if (file.getName().endsWith(".zip")) {
//				System.out.println(idx ++ + " / " + allfiles + " " + file.getName());
//				uploadFile(file.getAbsolutePath ());
//				//Thread.sleep(10);
//			}
//		}
		
		
		
	}
}
