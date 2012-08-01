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

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.Utility;
import org.ros.android.renderer.shapes.BaseShape;
import org.ros.android.renderer.shapes.Color;
import org.ros.android.rviz_for_android.urdf.MeshFileDownloader;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import android.util.Log;

public class InteractiveMarkerControl {

	private String name;
	private List<Marker> markers = new ArrayList<Marker>();
	private final Camera cam;
	private final FrameTransformTree ftt;
	private Quaternion offset;

	public InteractiveMarkerControl(visualization_msgs.InteractiveMarkerControl msg, Quaternion offset, Camera cam, MeshFileDownloader mfd, FrameTransformTree ftt) {
		this.cam = cam;
		this.ftt = ftt;
		this.name = msg.getName();
		this.offset = offset;
		Log.d("InteractiveMarker", "Created interactive marker control " + name);

		for(visualization_msgs.Marker marker : msg.getMarkers())
			markers.add(new Marker(marker, cam, mfd, ftt));

		if(msg.getMarkers().isEmpty())
			autoCompleteMarker(msg);
	}

	private static final Vector3 FIRST_ARROW_TRANSLATE = new Vector3(0.5,0d,0d);
	private static final Quaternion FIRST_ARROW_ROTATE = Quaternion.identity();
	private static final Vector3 SECOND_ARROW_TRANSLATE = new Vector3(-0.5,0d,0d);
	private static final Quaternion SECOND_ARROW_ROTATE = Quaternion.fromAxisAngle(Vector3.zAxis(), Math.PI);
	
	private static final Transform FIRST_ARROW_TRANSFORM = new Transform(FIRST_ARROW_TRANSLATE, FIRST_ARROW_ROTATE);
	private static final Transform SECOND_ARROW_TRANSFORM = new Transform(SECOND_ARROW_TRANSLATE, SECOND_ARROW_ROTATE);
	
	private static final Transform IDENTITY_TRANSFORM = Transform.identity();
	
	private void autoCompleteMarker(visualization_msgs.InteractiveMarkerControl msg) {
		
		// Normalize control orientation
		Quaternion orientation = Quaternion.fromQuaternionMessage(msg.getOrientation());
		if(orientation.getX() == 0 && orientation.getY() == 0 && orientation.getZ() == 0 && orientation.getW() == 0)
			orientation.setW(1d);
		orientation = Utility.normalize(orientation);
		
		Transform transform;
		if(msg.getOrientationMode() == visualization_msgs.InteractiveMarkerControl.FIXED)
			transform = new Transform(Vector3.zero(), offset.invert().multiply(orientation));
		else
			transform = new Transform(Vector3.zero(), orientation);
		
		switch(msg.getInteractionMode()) {
		case visualization_msgs.InteractiveMarkerControl.ROTATE_AXIS:
			Log.i("InteractiveMarker", "Rotate axis (RING)");
			Ring ring = Ring.newRing(cam, .5f, .65f, 20);
			ring.setTransform(transform);
			Marker marker = new Marker(ring, generateColor(orientation), cam, ftt);
			markers.add(marker);
			break;
		case visualization_msgs.InteractiveMarkerControl.MOVE_AXIS:
			Log.i("InteractiveMarker", "Move axis (ARROWS)");
			BaseShape arrowOne = Arrow.newArrow(cam, .08f, .15f, .2f, .2f);
			arrowOne.setTransform(transform.multiply(FIRST_ARROW_TRANSFORM));
			BaseShape arrowTwo = Arrow.newArrow(cam, .08f, .15f, .2f, .2f);
			arrowTwo.setTransform(transform.multiply(SECOND_ARROW_TRANSFORM));
			
			markers.add(new Marker(arrowOne, generateColor(orientation), cam, ftt));
			markers.add(new Marker(arrowTwo, generateColor(orientation), cam, ftt));
			break;
		case visualization_msgs.InteractiveMarkerControl.MENU:
			markers.add(new Marker(new Cube(cam), generateColor(orientation), cam, ftt));
			break;
		default:
			return;
		}
	}
	
	public void draw(GL10 glUnused) {
		for(Marker m : markers)
			m.draw(glUnused);
	}

	/**
	 * Generates a color based on the orientation of the marker
	 * 
	 * @param orientation
	 * @return Color based on marker orientation
	 */
	private Color generateColor(Quaternion orientation) {
		float x, y, z, w;
		x = (float) orientation.getX();
		y = (float) orientation.getY();
		z = (float) orientation.getZ();
		w = (float) orientation.getW();

		float mX, mY, mZ;

		mX = Math.abs(1 - 2 * y * y - 2 * z * z);
		mY = Math.abs(2 * x * y + 2 * z * w);
		mZ = Math.abs(2 * x * z - 2 * y * w);

		float max_xy = mX > mY ? mX : mY;
		float max_yz = mY > mZ ? mY : mZ;
		float max_xyz = max_xy > max_yz ? max_xy : max_yz;

		return new Color(mX / max_xyz, mY / max_xyz, mZ / max_xyz, 0.5f);
	}
}
