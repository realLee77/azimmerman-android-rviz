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
import org.ros.android.renderer.shapes.BaseShapeInterface;
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

		isViewFacing = (msg.getOrientationMode() == visualization_msgs.InteractiveMarkerControl.VIEW_FACING);

		// TODO: Figure out interactive marker "independent orientation" setting
		for(visualization_msgs.Marker marker : msg.getMarkers()) {
			Marker m = new Marker(marker, cam, mfd, ftt);
			m.setViewFacing(isViewFacing);
			markers.add(m);
		}

		if(msg.getMarkers().isEmpty())
			autoCompleteMarker(msg);
	}

	private static final Vector3 FIRST_ARROW_TRANSLATE = new Vector3(0.5, 0d, 0d);
	private static final Quaternion FIRST_ARROW_ROTATE = Quaternion.identity();
	private static final Vector3 SECOND_ARROW_TRANSLATE = new Vector3(-0.5, 0d, 0d);
	private static final Quaternion SECOND_ARROW_ROTATE = Quaternion.fromAxisAngle(Vector3.zAxis(), Math.PI);
	private static final Transform FIRST_ARROW_TRANSFORM = new Transform(FIRST_ARROW_TRANSLATE, FIRST_ARROW_ROTATE);
	private static final Transform SECOND_ARROW_TRANSFORM = new Transform(SECOND_ARROW_TRANSLATE, SECOND_ARROW_ROTATE);

	private boolean isViewFacing = false;

	private void autoCompleteMarker(visualization_msgs.InteractiveMarkerControl msg) {

		// Normalize control orientation
		Quaternion orientation = Quaternion.fromQuaternionMessage(msg.getOrientation());
		Utility.correctQuaternion(orientation);
		orientation = Utility.normalize(orientation);

		// Generate control transform
		Transform transform = null;

		// Determine what type of marker and its orientation based on orientation mode
		switch(msg.getOrientationMode()) {
		case visualization_msgs.InteractiveMarkerControl.FIXED:
			Utility.correctQuaternion(offset);
			transform = new Transform(Vector3.zero(), offset.invert().multiply(orientation));
			break;
		case visualization_msgs.InteractiveMarkerControl.VIEW_FACING:
			//transform = new Transform(Vector3.zero(), Quaternion.identity());
			//break;
		case visualization_msgs.InteractiveMarkerControl.INHERIT:
			transform = new Transform(Vector3.zero(), orientation);
			break;
		default:
			Log.e("InteractiveMarker", "Unexpected orientation mode: " + msg.getOrientationMode());
		}

		// Generate a control marker corresponding to the control type
		switch(msg.getInteractionMode()) {
		case visualization_msgs.InteractiveMarkerControl.MOVE_ROTATE:
		case visualization_msgs.InteractiveMarkerControl.MOVE_PLANE:
		case visualization_msgs.InteractiveMarkerControl.ROTATE_AXIS:
			Log.i("InteractiveMarker", "Rotate axis (RING)");
			Ring ring = Ring.newRing(cam, .5f, .65f, 20);
			ring.setTransform(transform);
			Marker marker = instantiateControlMarker(ring, generateColor(orientation), cam, ftt);
			markers.add(marker);
			break;
		case visualization_msgs.InteractiveMarkerControl.MOVE_AXIS:
			Log.i("InteractiveMarker", "Move axis (ARROWS)");
			BaseShape arrowOne = Arrow.newArrow(cam, .08f, .15f, .2f, .2f);
			arrowOne.setTransform(transform.multiply(FIRST_ARROW_TRANSFORM));
			BaseShape arrowTwo = Arrow.newArrow(cam, .08f, .15f, .2f, .2f);
			arrowTwo.setTransform(transform.multiply(SECOND_ARROW_TRANSFORM));

			markers.add(instantiateControlMarker(arrowOne, generateColor(orientation), cam, ftt));
			markers.add(instantiateControlMarker(arrowTwo, generateColor(orientation), cam, ftt));
			break;
		case visualization_msgs.InteractiveMarkerControl.MENU:
			markers.add(instantiateControlMarker(new Cube(cam), generateColor(orientation), cam, ftt));
			break;
		default:
			return;
		}
	}

	private Marker instantiateControlMarker(BaseShapeInterface shape, Color color, Camera cam, FrameTransformTree ftt) {
		Marker m = new Marker(shape, color, cam, ftt);
		m.setViewFacing(isViewFacing);
		return m;
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

		return new Color(mX / max_xyz, mY / max_xyz, mZ / max_xyz, 0.7f);
	}
}
