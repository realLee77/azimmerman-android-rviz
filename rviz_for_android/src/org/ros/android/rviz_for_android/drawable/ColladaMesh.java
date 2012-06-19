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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.rviz_for_android.drawable.loader.ColladaLoader;
import org.ros.android.view.visualization.shape.BaseShape;
import org.ros.android.view.visualization.shape.Color;
import org.ros.android.view.visualization.shape.TrianglesShape;
import org.ros.rosjava_geometry.Transform;

import com.google.common.io.Files;

public class ColladaMesh extends BaseShape {
	protected static final ColladaLoader loader = new ColladaLoader();

	public static ColladaMesh newFromFile(String filename) {
		List<BaseShape> retval = null;
		synchronized(loader) {
			String contents = null;
			try {
				contents = Files.toString(new File(filename), Charset.defaultCharset());
			} catch(IOException e) {
				e.printStackTrace();
			}
			loader.readDae(contents);
			retval = loader.getGeometries();
		}
		return new ColladaMesh(retval);
	}
	
	protected List<BaseShape> geometries;
	protected ColladaMesh(List<BaseShape> geometries) {
		this.geometries = geometries;
	}
	
	private float[] scale;
	
	public void draw(GL10 gl, Transform transform, float[] scale) {
		gl.glPushMatrix();
		this.setTransform(transform);
		this.scale = scale;

		super.draw(gl);
		
		for(BaseShape g : geometries) {
			g.setColor(super.color);
			g.draw(gl);
		}

		gl.glPopMatrix();
	}
	
	@Override
	protected void scale(GL10 gl) {
		gl.glScalef(scale[0], scale[1], scale[2]);
	}
}
