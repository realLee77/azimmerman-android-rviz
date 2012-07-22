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
package org.ros.android.rviz_for_android.drawable;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.shapes.BaseShape;
import org.ros.android.renderer.shapes.Cleanable;
import org.ros.android.rviz_for_android.drawable.loader.ColladaLoader;
import org.ros.android.rviz_for_android.urdf.MeshFileDownloader;
import org.ros.android.rviz_for_android.urdf.UrdfDrawable;
import org.ros.rosjava_geometry.Transform;

import android.util.Log;

public class ColladaMesh implements UrdfDrawable, Cleanable {
	protected static final ColladaLoader loader = new ColladaLoader();
	
	/**
	 * @param filename The name of the DAE file to be loaded, parsed directly from the URDF, contains the "package://" or "html://" piece
	 * @param mfd The mesh file downloader instance
	 * @return a Collada mesh
	 */
	public static ColladaMesh newFromFile(String filename, MeshFileDownloader mfd, Camera cam) {
		long now = System.nanoTime();
		if(mfd == null)
			throw new IllegalArgumentException("Null mesh file downloader! Must have a valid MFD to download meshes.");
		
		List<BaseShape> retval = null;
		
		// Download the .DAE file if it doesn't exist
		String loadedFilename = mfd.getFile(filename);
		
		if(loadedFilename == null)
			throw new RuntimeException("Unable to download the file!");
		
		// Get the image prefix
		String imgPrefix = mfd.getPrefix(filename);
		
		synchronized(loader) {			
			loader.setDownloader(mfd);
			loader.setCamera(cam);
			try {				
				loader.readDae(mfd.getContext().openFileInput(loadedFilename), imgPrefix);
			} catch(IOException e) {
				return null;
			}
			retval = loader.getGeometries();
		}
		Log.i("Collada", "Load time for " + filename + " = " + (System.nanoTime() - now)/1000000000.0);
		return new ColladaMesh(cam, retval);
	}
	
	protected List<BaseShape> geometries;
	protected Camera cam;
	protected ColladaMesh(Camera cam, List<BaseShape> geometries) {
		this.cam = cam;
		this.geometries = geometries;	
	}
	
	private float[] scale;
	private Transform transform;
	@Override
	public void draw(GL10 glUnused, Transform transform, float[] scale) {
		cam.pushM();
		this.scale = scale;
		this.transform = transform;
		cam.scaleM(scale[0], scale[1], scale[2]);
		cam.applyTransform(transform);

		for(BaseShape g : geometries)
			g.draw(glUnused);
		
		cam.popM();
	}

	@Override
	public void selectionDraw(GL10 glUnused) {
		cam.pushM();		
		cam.scaleM(scale[0], scale[1], scale[2]);
		cam.applyTransform(transform);
		for(BaseShape g : geometries)
			g.selectionDraw(glUnused);
		
		cam.popM();
	}

	@Override
	public void cleanup() {
		for(BaseShape g : geometries) {
			if(g instanceof Cleanable) {
				Cleanable cs = (Cleanable) g;
				cs.cleanup();
			}
		}
	}

	@Override
	public void setSelected(boolean isSelected) {
		// TODO Auto-generated method stub
	}

	@Override
	public Map<String, String> getInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	public void registerSelectable() {
		for(BaseShape g : geometries)
			g.registerSelectable();
	}
}
