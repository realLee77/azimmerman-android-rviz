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

import java.util.Set;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.layer.DefaultLayer;
import org.ros.android.renderer.layer.Selectable;
import org.ros.android.renderer.layer.SelectableLayer;
import org.ros.android.rviz_for_android.drawable.Arrow;
import org.ros.android.rviz_for_android.drawable.Cube;
import org.ros.android.rviz_for_android.drawable.Cylinder;
import org.ros.android.rviz_for_android.drawable.Ring;
import org.ros.android.rviz_for_android.drawable.Sphere;
import org.ros.rosjava_geometry.Transform;

public class CubeLayer extends DefaultLayer implements SelectableLayer {

	private Cube myCube;
	private Cube myCube2;
	private Sphere mySphere;
	private Cylinder myCyl;
	private Ring myRing;
	private Ring myRing2;
	private Arrow myArrow;
	private Transform transform = Transform.identity();
	private float[] scale = new float[]{1f,1f,1f};
	
	private long startTime;
	
	public CubeLayer(Camera cam) {
		super(cam);
		myCube = new Cube(cam);
		myCube2 = new Cube(cam);
		mySphere = new Sphere(cam, 1f);
		myCyl = new Cylinder(cam, 1f, 1f);
		myRing = Ring.newRing(cam, 0.7f, 1f, 3);
		myRing2 = Ring.newRing(cam, 0.7f, 1f, 20);
		myArrow = Arrow.newArrow(cam, 0.2f, 0.4f, .5f, .6f);
		startTime = System.currentTimeMillis();
		
		myCube.registerSelectable();
		myCube2.registerSelectable();
		mySphere.registerSelectable();
		myCyl.registerSelectable();
		myArrow.registerSelectable();
	}

	@Override
	public void draw(GL10 glUnused) {
		camera.pushM();
		myRing.draw(glUnused);
		camera.rotateM(angle(8000), 0, 0, 1);
		camera.translateM(1f, 1f, 0f);
		myCube.draw(glUnused,transform,scale);
		camera.translateM(2f, 0f, .5f);
		myRing2.draw(glUnused);
		camera.rotateM(angle(1000), 0, 0.707f, 0.707f);
		myCube2.draw(glUnused,transform,scale);
		camera.popM();
		
		
		camera.translateM(1f, 1.5f, 1f);
		mySphere.draw(glUnused, transform, 1f);
		camera.rotateM(angle(12000), 1, 0, 0);
		camera.translateM(1.5f, 1.5f, 1.5f);
		myCyl.draw(glUnused, transform, .5f, .25f);
		camera.translateM(2f, 0f, 1f);
		camera.rotateM(angle(2500), .1f, 0f, .1f);
		myArrow.draw(glUnused);
	}	
	
	private float angle(int msecPerRev) {
		long now = System.currentTimeMillis();
		int rotTime = (int) ((now - startTime) % msecPerRev);
		return (rotTime / (msecPerRev/360f));
	}

	@Override
	public void selectionDraw(GL10 glUnused) {
		camera.pushM();
		myRing.selectionDraw(glUnused);
		camera.rotateM(angle(8000), 0, 0, 1);
		camera.translateM(1f, 1f, 0f);
		myCube.selectionDraw(glUnused);
		camera.translateM(2f, 0f, .5f);
		myRing2.selectionDraw(glUnused);
		camera.rotateM(angle(1000), 0, 0.707f, 0.707f);
		myCube2.selectionDraw(glUnused);
		camera.popM();

		camera.translateM(1f, 1.5f, 1f);
		mySphere.selectionDraw(glUnused);
		camera.rotateM(angle(12000), 1, 0, 0);
		camera.translateM(1.5f, 1.5f, 1.5f);
		myCyl.selectionDraw(glUnused);
		camera.translateM(2f, 0f, 1f);
		camera.rotateM(angle(2500), 1, 0f, 1f);
		myArrow.selectionDraw(glUnused);
	}

	@Override
	public Set<Selectable> getSelectables() {
		// TODO Auto-generated method stub
		return null;
	}
}
