/*
 * Copyright (c) 2012, Willow Garage, Inc.
 * All rights reserved.
 *
 * Willow Garage licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.ros.android.rviz_for_android.urdf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import android.content.Context;
import android.util.Log;

public class MeshFileDownloader {

	private String host;
	private Context context;
	private Object lock = new Object();
	
	private static MeshFileDownloader instance;
	public static MeshFileDownloader getMeshFileDownloader(String host, Context context) {
		if(instance == null) {
			// Initialize the new instance
			instance = new MeshFileDownloader(host, context);			
		}
		return instance;
	}

	public MeshFileDownloader(String host, Context context) {
		Log.i("Downloader", "Creating a mesh file downloader object");
		Log.i("Downloader", "Host IP is " + host);
		this.context = context;
		this.host = host;
		
		clearCache();
	}
	
	public void clearCache() {
		for(String file : context.fileList()) {
			context.deleteFile(file);
		}
		
		Log.d("Downloader", "Cleared cache");
	}
	
	public Context getContext() {
		return context;
	}
	
	// Given a package name, give the string prefix to attach to prepend to image names when fetching them from the server
	public String getPrefix(String filename) {
		return filename.substring(0,filename.lastIndexOf("/")+1);
	}
	
	public boolean fileExists(String filename) {
		return Arrays.binarySearch(context.fileList(), sanitizeFilename(filename)) >= 0;
	}
	
	private String sanitizeFilename(String filename) {
		if(filename.contains("/")) {
			filename = filename.substring(filename.indexOf("//")+2).replace("/", "-");
		}
		return filename;
	}

	public String getFile(String path) {
		synchronized(lock) {
			if(fileExists(path))
				return sanitizeFilename(path);
			String filename = sanitizeFilename(path); //path.substring(path.indexOf("//")+2).replace("/", "-");
			try {
				if(path.startsWith("http://"))
					return queryServer(new URL(path), filename);
				else if(path.toLowerCase().startsWith("package://"))
					return queryServer(new URL(host + "/" + "PKG" + path.substring(9)), filename);
				else {
					Log.e("Downloader", "Unexpected scheme: " + path);
					return null;
				}
			} catch(MalformedURLException e) {
				Log.e("Downloader", "Path and URL are malformed: " + host + "    " + path);
				e.printStackTrace();
			}
			return null;
		}
	}

	private String queryServer(URL url, String filename) {
		Log.i("Downloader", "Fetching file with URL " + url.toString() + " to save to " + filename);
		try {
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoInput(true);
			Log.d("Downloader", "Connecting...");
			conn.connect();
			
			if(conn.getResponseCode() != HttpURLConnection.HTTP_NOT_FOUND) {
				Log.d("Downloader", "Response code " + conn.getResponseCode());
				int size = conn.getContentLength();
				byte[] data = new byte[size];
				InputStream is = conn.getInputStream();
				is.read(data, 0, size);
				Log.d("Downloader", "Got file data in " + size + " bytes!");
				
				byte sum = 0;
				for(int i = 0; i < data.length; i++)
					sum += data[i];
				Log.i("Downloader", "Data checksum: " + sum);
				
				
				FileOutputStream fOut = context.openFileOutput(filename, Context.MODE_PRIVATE);				
				fOut.write(data);				
				fOut.close();
				
				FileOutputStream fos = new FileOutputStream("/sdcard/" + filename);
				fos.write(data);
				fos.close();
				
				Log.d("Downloader", "Wrote file to " + context.getFilesDir());
				conn.disconnect();
				return filename;
			} else {
				Log.e("Downloader", "404'd!");
			}
		} catch(IOException e) {
			Log.e("Downloader", "IO Error!");
			e.printStackTrace();
		}
		
		return null;
	}
}
