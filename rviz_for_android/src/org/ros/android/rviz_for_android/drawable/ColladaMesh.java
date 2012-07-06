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

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.rviz_for_android.drawable.loader.ColladaLoader;
import org.ros.android.rviz_for_android.urdf.MeshFileDownloader;
import org.ros.android.rviz_for_android.urdf.UrdfDrawable;
import org.ros.android.view.visualization.shape.BaseShape;
import org.ros.android.view.visualization.shape.BatchDrawable;
import org.ros.rosjava_geometry.Transform;

import android.util.Log;

public class ColladaMesh extends BaseShape implements UrdfDrawable {
	protected static final ColladaLoader loader = new ColladaLoader();
	
	/**
	 * @param filename The name of the DAE file to be loaded, parsed directly from the URDF, contains the "package://" or "html://" piece
	 * @param mfd The mesh file downloader instance
	 * @return a Collada mesh
	 */
	public static ColladaMesh newFromFile(String filename, MeshFileDownloader mfd) {
		long now = System.nanoTime();
		if(mfd == null)
			throw new IllegalArgumentException("Null mesh file downloader! Must have a valid MFD to download meshes.");
		
		List<BatchDrawable> retval = null;
		
		// Download the .DAE file if it doesn't exist
		String loadedFilename = mfd.getFile(filename);
		
		// Get the image prefix
		String imgPrefix = mfd.getPrefix(filename);
		
		synchronized(loader) {			
			loader.setDownloader(mfd);	
			try {				
				loader.readDae(mfd.getContext().openFileInput(loadedFilename), imgPrefix);
			} catch(IOException e) {
				return null;
			}
			retval = loader.getGeometries();
		}
		Log.i("Collada", "Load time for " + filename + " = " + (System.nanoTime() - now)/1000000000.0);
		return new ColladaMesh(retval);
	}
	
	protected List<BatchDrawable> geometries;
	protected ColladaMesh(List<BatchDrawable> geometries) {
		this.geometries = geometries;
		
		if(super.color != null)
			for(BatchDrawable g : geometries)
				g.setColor(super.color);	
	}
	
	private float[] scale;

	public void draw(GL10 gl, Transform transform, float[] scale) {
		gl.glPushMatrix();
		this.setTransform(transform);
		this.scale = scale;

		super.draw(gl);
		
		for(BatchDrawable g : geometries) {
			g.draw(gl);
		}

		gl.glPopMatrix();
	}
	
	@Override
	protected void scale(GL10 gl) {
		gl.glScalef(scale[0], scale[1], scale[2]);
	}

	@Override
	public void batchDraw(GL10 gl, Transform transform, float[] scale) {
		gl.glPushMatrix();
		this.setTransform(transform);
		this.scale = scale;

		super.draw(gl);
		
		for(BatchDrawable g : geometries) {
			g.batchDraw(gl);
		}

		gl.glPopMatrix();
	}
}
