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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.ros.android.rviz_for_android.MainActivity;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

public class MeshFileDownloader {

	private String host;
	private Activity context;
	private Object lock = new Object();

	// TODO:
	private MeshFileDownloader.QueryServer qs;

	private static MeshFileDownloader instance;

	private ProgressDialog mProgressDialog;

	public static MeshFileDownloader getMeshFileDownloader(String host, Activity context) {
		if(instance == null) {
			// Initialize the new instance
			if(isDownloadManagerAvailable(context))
				instance = new MeshFileDownloader(host, context);
			else
				throw new RuntimeException("Download manager not available!");
		}
		return instance;
	}

	public static boolean isDownloadManagerAvailable(Context context) {
		try {
			if(Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
				return false;
			}
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			intent.setClassName("com.android.providers.downloads.ui", "com.android.providers.downloads.ui.DownloadList");
			List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
			return list.size() > 0;
		} catch(Exception e) {
			return false;
		}
	}

	public String queryServerBackground(String url, String filename) {
		DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
		request.setDescription("Some descrition");
		request.setTitle("Some title");
		// in order for this if to run, you must use the android 3.2 to compile your app
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			request.allowScanningByMediaScanner();
			request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		}
		request.setDestinationUri(Uri.parse(context.getFilesDir().toString() + "/" + filename));
		// request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "name-of-the-file.ext");

		// get download service and enqueue file
		DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
		manager.enqueue(request);
		return filename;
	}

	public MeshFileDownloader(String host, final Activity context) {
		Log.i("Downloader", "Creating a mesh file downloader object");
		Log.i("Downloader", "Host IP is " + host);
		this.context = context;
		this.host = host;

		clearCache();

		context.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mProgressDialog = new ProgressDialog(context);
				mProgressDialog.setMessage("Download progress");
				mProgressDialog.setIndeterminate(false);
				mProgressDialog.setMax(100);
				mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			}
		});
	}

	public void clearCache() {
		synchronized(lock) {
			for(String file : context.fileList()) {
				Log.i("Downloader", "Cleared file " + file);
				context.deleteFile(file);
			}
			Log.d("Downloader", "Cleared cache");
		}
	}

	public Context getContext() {
		return context;
	}

	// Given a package name, give the string prefix to attach to prepend to image names when fetching them from the server
	public String getPrefix(String filename) {
		if(filename != null)
			return filename.substring(0, filename.lastIndexOf("/") + 1);
		else
			return null;
	}

	public boolean fileExists(String filename) {
		if(filename != null)
			return Arrays.binarySearch(context.fileList(), sanitizeFilename(filename)) >= 0;
		else
			throw new IllegalArgumentException("Filename can't be null!");
	}

	private String sanitizeFilename(String filename) {
		if(filename.contains("/")) {
			filename = filename.substring(filename.indexOf("//") + 2).replace("/", "-");
		}
		return filename;
	}

	public String getFile(final String path) {
		synchronized(lock) {
			if(fileExists(path))
				return sanitizeFilename(path);
			final String filename = sanitizeFilename(path);
			try {
				if(path.startsWith("http://"))
					return queryServer(new URL(path), filename);
				else if(path.toLowerCase().startsWith("package://")) {
					qs = new QueryServer();
					context.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							qs.execute(host + "/PKG" + path.substring(9), filename, path.substring(path.lastIndexOf('/') + 1, path.length()));
						}
					});
					return qs.get(10, TimeUnit.SECONDS);
					// return queryServer(new URL(host + "/PKG" + path.substring(9)), filename);
				} else {
					Log.e("Downloader", "Unexpected scheme: " + path);
					return null;
				}
			} catch(MalformedURLException e) {
				Log.e("Downloader", "Path and URL are malformed: " + host + "    " + path);
				e.printStackTrace();
			} catch(InterruptedException e) {
				Log.e("Downloader", "Interrupted exception");
				e.printStackTrace();
			} catch(ExecutionException e) {
				Log.e("Downloader", "Execution exception");
				e.printStackTrace();
			} catch(TimeoutException e) {
				Log.e("Downloader", "Timed out!!");
				e.printStackTrace();
			}
			return null;
		}
	}

	private String queryServer(URL url, String filename) {
		Log.i("Downloader", "Fetching file with URL " + url.toString() + " to save to " + filename);

		int retries = 0;
		while(retries < 5) {
			try {
				Log.d("Downloader", "Connecting...");
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();

				int bytesRead = 0;
				BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
				BufferedOutputStream bout = new BufferedOutputStream(context.openFileOutput(filename, Context.MODE_PRIVATE), 1024);

				if(conn.getResponseCode() != HttpURLConnection.HTTP_NOT_FOUND) {
					int toRead = conn.getContentLength();
					byte[] data = new byte[1024];
					int read = 0;
					while((read = in.read(data, 0, 1024)) >= 0) {
						bout.write(data, 0, read);
						bytesRead += read;
					}
					Log.d("Downloader", "Should have " + toRead + " bytes");
					Log.d("Downloader", "Wrote " + bytesRead + " bytes to sdcard");
					if(bytesRead < toRead)
						Log.e("Downloader", "Download incomplete!");
					bout.close();
					return filename;
				} else {
					Log.e("Downloader", "404'd!");
					retries++;
				}
			} catch(IOException e) {
				Log.e("Downloader", "IO Error!");
				retries++;
				e.printStackTrace();
			}
		}
		return null;
	}

	private class QueryServer extends AsyncTask<String, Integer, String> {

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			mProgressDialog.dismiss();
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgressDialog.show();
			mProgressDialog.setProgress(0);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			mProgressDialog.setProgress(values[0]);
		}

		@Override
		protected String doInBackground(String... params) {
			Log.d("Downloader", "Beginning execution...");
			mProgressDialog.setMessage("Downloading " + params[2]);
			URL url = null;
			try {
				url = new URL(params[0]);
			} catch(MalformedURLException e1) {
				e1.printStackTrace();
				return null;
			}
			String filename = params[1];

			int retries = 0;
			while(retries < 5) {
				try {
					Log.d("Downloader", "Connecting...");
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();

					int bytesRead = 0;
					BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
					BufferedOutputStream bout = new BufferedOutputStream(context.openFileOutput(filename, Context.MODE_PRIVATE), 1024);

					if(conn.getResponseCode() != HttpURLConnection.HTTP_NOT_FOUND) {
						int toRead = conn.getContentLength();
						byte[] data = new byte[1024];
						int read = 0;
						while((read = in.read(data, 0, 1024)) >= 0) {
							bout.write(data, 0, read);
							bytesRead += read;
							publishProgress((int) ((100.0 * bytesRead) / toRead));
						}
						Log.d("Downloader", "Should have " + toRead + " bytes");
						Log.d("Downloader", "Wrote " + bytesRead + " bytes to sdcard");
						if(bytesRead < toRead)
							Log.e("Downloader", "Download incomplete!");
						bout.close();
						publishProgress(100);
						return filename;
					} else {
						Log.e("Downloader", "404'd!");
						retries++;
					}
				} catch(IOException e) {
					Log.e("Downloader", "IO Error!");
					retries++;
					e.printStackTrace();
				}
			}
			return null;
		}

	}
}
