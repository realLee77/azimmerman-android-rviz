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

import java.io.FileNotFoundException;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.rviz_for_android.drawable.loader.StlLoader;
import org.ros.android.rviz_for_android.urdf.MeshFileDownloader;
import org.ros.android.rviz_for_android.urdf.UrdfDrawable;
import org.ros.android.view.visualization.shape.BufferedTrianglesShape;
import org.ros.android.view.visualization.shape.Color;
import org.ros.rosjava_geometry.Transform;

public class StlMesh extends BufferedTrianglesShape implements UrdfDrawable {

	private static final StlLoader loader = new StlLoader();
	
	public static StlMesh newFromFile(String filename, MeshFileDownloader mfd) {
		float[] v;
		float[] n;
		
		// Download the .DAE file if it doesn't exist
		String loadedFilename = mfd.getFile(filename);
		
		synchronized(loader) {
			try {
				loader.load(mfd.getContext().openFileInput(loadedFilename));
			} catch(FileNotFoundException e) {
				e.printStackTrace();
				return null;
			}
			v = loader.getVertices();
			n = loader.getNormals();
		}
		return new StlMesh(v, n, new Color(0,1,1,1));
	}
	
	private StlMesh(float[] vertices, float[] normals, Color color) {
		super(vertices, normals, color);
	}
	
	private float[] scale;
	
	public void draw(GL10 gl, Transform transform, float[] scale) {
		this.setTransform(transform);
		this.scale = scale;
		super.draw(gl);
	}

	@Override
	protected void scale(GL10 gl) {
		gl.glScalef(scale[0], scale[1], scale[2]);
	}

	@Override
	public void batchDraw(GL10 gl, Transform transform, float[] scale) {
		this.setTransform(transform);
		this.scale = scale;
		super.batchDraw(gl);
	}
}
