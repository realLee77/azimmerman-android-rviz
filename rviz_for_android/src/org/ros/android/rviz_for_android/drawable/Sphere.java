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

import org.ros.android.view.visualization.shape.BaseShape;
import org.ros.android.view.visualization.shape.Color;
import org.ros.rosjava_geometry.Transform;

public class Sphere extends BaseShape implements UrdfDrawable {

	private ColladaMesh sphere;
	
	public Sphere() {
		sphere = ColladaMesh.newFromFile("/sdcard/sphere.dae");
	}

	public void draw(GL10 gl, Transform transform, float radius) {
		gl.glFrontFace(GL10.GL_CW);
		sphere.draw(gl, transform, 2*radius);
		gl.glFrontFace(GL10.GL_CCW);
	}

	@Override
	public Color getColor() {
		return sphere.getColor();
	}

	@Override
	public void setColor(Color color) {
		sphere.setColor(color);
	}

	@Override
	public Transform getTransform() {
		return sphere.getTransform();
	}

	@Override
	public void setTransform(Transform pose) {
		sphere.setTransform(pose);
	}

	@Override
	public void draw(GL10 gl, Transform transform, float[] scale) {
		gl.glFrontFace(GL10.GL_CW);
		sphere.draw(gl, transform, scale);
		gl.glFrontFace(GL10.GL_CCW);
	}
}
