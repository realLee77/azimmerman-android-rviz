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

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.rviz_for_android.drawable.loader.StlLoader;
import org.ros.android.view.visualization.shape.Color;
import org.ros.android.view.visualization.shape.TrianglesShape;
import org.ros.rosjava_geometry.Transform;

public class Mesh extends TrianglesShape {

	private static final StlLoader loader = new StlLoader();
	
	public static Mesh newFromFile(String filename) {
		float[] v;
		float[] n;
		synchronized(loader) {
			loader.load(filename);
			v = loader.getVertices();
			n = loader.getNormals();
		}
		return new Mesh(v, n, new Color(0,1,1,1));
	}
	
	private Mesh(float[] vertices, float[] normals, Color color) {
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
}
