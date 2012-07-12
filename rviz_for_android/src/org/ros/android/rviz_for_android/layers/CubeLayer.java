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
package org.ros.android.rviz_for_android.layers;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.layer.DefaultLayer;
import org.ros.android.rviz_for_android.drawable.Cube;
import org.ros.android.rviz_for_android.drawable.Cylinder;
import org.ros.android.rviz_for_android.drawable.Sphere;
import org.ros.rosjava_geometry.Transform;

public class CubeLayer extends DefaultLayer {

	private Cube myCube;
	private Cube myCube2;
	private Sphere mySphere;
	private Cylinder myCyl;
	private Transform transform = Transform.newIdentityTransform();
	private float[] scale = new float[]{1f,1f,1f};
	private float[] scale2 = new float[]{.5f,1.5f,.75f};
	
	private long startTime;
	
	public CubeLayer(Camera cam) {
		super(cam);
		myCube = new Cube(cam);
		myCube2 = new Cube(cam);
		mySphere = new Sphere(cam);
		myCyl = new Cylinder(cam);
		startTime = System.currentTimeMillis();
	}

	@Override
	public void draw(GL10 glUnused) {
		//camera.rotateM(angle(8000), 0, 0, 1);
		myCube.draw(glUnused,transform,scale);
		camera.translateM(2f, 0f, 0f);
		myCube2.draw(glUnused,transform,scale);
		camera.translateM(1f, 0f, 1f);
		mySphere.draw(glUnused, transform, 1f);
		camera.translateM(0.5f, 1f, 0.5f);
		myCyl.draw(glUnused, transform, 1f, 1f);
	}	
	
	private int angle(int msecPerRev) {
		long now = System.currentTimeMillis();
		int rotTime = (int) ((now - startTime) % msecPerRev);
		return (int) (rotTime / (msecPerRev/360));
	}
}
