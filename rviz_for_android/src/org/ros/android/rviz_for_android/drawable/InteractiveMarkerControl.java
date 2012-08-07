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
import org.ros.android.renderer.layer.InteractiveObject;
import org.ros.android.renderer.shapes.BaseShape;
import org.ros.android.renderer.shapes.BaseShapeInterface;
import org.ros.android.renderer.shapes.Cleanable;
import org.ros.android.renderer.shapes.Color;
import org.ros.android.rviz_for_android.urdf.MeshFileDownloader;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import visualization_msgs.InteractiveMarkerFeedback;
import android.opengl.Matrix;
import android.util.Log;

/**
 * @author azimmerman
 * 
 */
public class InteractiveMarkerControl implements InteractiveObject, Cleanable {

	public static enum InteractionMode {
		MENU(visualization_msgs.InteractiveMarkerControl.MENU, InteractiveMarkerFeedback.MENU_SELECT), MOVE_AXIS(visualization_msgs.InteractiveMarkerControl.MOVE_AXIS, InteractiveMarkerFeedback.POSE_UPDATE), ROTATE_AXIS(visualization_msgs.InteractiveMarkerControl.ROTATE_AXIS, InteractiveMarkerFeedback.POSE_UPDATE), MOVE_PLANE(visualization_msgs.InteractiveMarkerControl.MOVE_PLANE, InteractiveMarkerFeedback.POSE_UPDATE), MOVE_ROTATE(visualization_msgs.InteractiveMarkerControl.MOVE_ROTATE, InteractiveMarkerFeedback.POSE_UPDATE), NONE(visualization_msgs.InteractiveMarkerControl.NONE, InteractiveMarkerFeedback.KEEP_ALIVE);

		public byte val;
		public byte feedbackType;

		InteractionMode(byte val, byte feedbackType) {
			this.val = val;
			this.feedbackType = feedbackType;
		}

		public static InteractionMode fromByte(byte val) {
			for(InteractionMode im : InteractionMode.values())
				if(im.val == val)
					return im;
			return null;
		}
	};

	public static enum OrientationMode {
		FIXED(visualization_msgs.InteractiveMarkerControl.FIXED), INHERIT(visualization_msgs.InteractiveMarkerControl.INHERIT), VIEW_FACING(visualization_msgs.InteractiveMarkerControl.VIEW_FACING);

		public byte val;

		OrientationMode(byte val) {
			this.val = val;
		}

		public static OrientationMode fromByte(byte val) {
			for(OrientationMode im : OrientationMode.values())
				if(im.val == val)
					return im;
			return null;
		}
	}

	private List<Marker> markers = new ArrayList<Marker>();
	private final Camera cam;

	private InteractiveMarker parentControl;
	private InteractionMode interactionMode;
	private OrientationMode orientationMode;

	private Transform drawTransform = Transform.identity();
	private Quaternion myOrientation;

	private boolean captureScreenPosition = false;

	public InteractiveMarkerControl(visualization_msgs.InteractiveMarkerControl msg, Camera cam, MeshFileDownloader mfd, FrameTransformTree ftt, InteractiveMarker parentControl) {
		this.cam = cam;
		this.parentControl = parentControl;
		this.name = msg.getName();

		// Normalize control orientation
		myOrientation = Quaternion.fromQuaternionMessage(msg.getOrientation());
		Utility.correctQuaternion(myOrientation);
		myOrientation = Utility.normalize(myOrientation);

		myAxis = getRotatedQuaternionAxis(myOrientation);

		interactionMode = InteractionMode.fromByte(msg.getInteractionMode());
		orientationMode = OrientationMode.fromByte(msg.getOrientationMode());

		Log.d("InteractiveMarker", "Created interactive marker control " + name);

		isViewFacing = !msg.getIndependentMarkerOrientation() && orientationMode == OrientationMode.VIEW_FACING;

		// TODO: Figure out interactive marker "independent orientation" setting
		for(visualization_msgs.Marker marker : msg.getMarkers()) {
			Marker m = new Marker(marker, cam, mfd, ftt);
			m.setViewFacing(isViewFacing);
			if(msg.getInteractionMode() != visualization_msgs.InteractiveMarkerControl.NONE)
				m.setInteractive(this);
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
		// Generate control transform
		// Transform transform = new Transform(Vector3.zero(), myOrientation);

		// Generate a control marker corresponding to the control type
		switch(msg.getInteractionMode()) {
		case visualization_msgs.InteractiveMarkerControl.MOVE_ROTATE:
		case visualization_msgs.InteractiveMarkerControl.MOVE_PLANE:
		case visualization_msgs.InteractiveMarkerControl.ROTATE_AXIS:
			Log.i("InteractiveMarker", "Rotate axis (RING)");
			Ring ring = Ring.newRing(cam, .5f, .65f, 20);
			// ring.setTransform(transform);
			Marker marker = instantiateControlMarker(ring, generateColor(myOrientation), cam);
			markers.add(marker);
			break;
		case visualization_msgs.InteractiveMarkerControl.MOVE_AXIS:
			Log.i("InteractiveMarker", "Move axis (ARROWS)");
			BaseShape arrowOne = Arrow.newArrow(cam, .08f, .15f, .2f, .2f);
			// arrowOne.setTransform(transform.multiply(FIRST_ARROW_TRANSFORM));
			arrowOne.setTransform(FIRST_ARROW_TRANSFORM);
			BaseShape arrowTwo = Arrow.newArrow(cam, .08f, .15f, .2f, .2f);
			// arrowTwo.setTransform(transform.multiply(SECOND_ARROW_TRANSFORM));
			arrowTwo.setTransform(SECOND_ARROW_TRANSFORM);

			markers.add(instantiateControlMarker(arrowOne, generateColor(myOrientation), cam));
			markers.add(instantiateControlMarker(arrowTwo, generateColor(myOrientation), cam));
			break;
		case visualization_msgs.InteractiveMarkerControl.MENU:
			markers.add(instantiateControlMarker(new Cube(cam), generateColor(myOrientation), cam));
			break;
		default:
			return;
		}
	}

	private Marker instantiateControlMarker(BaseShapeInterface shape, Color color, Camera cam) {
		Marker m = new Marker(shape, color, cam, null);
		m.setInteractive(this);
		m.setViewFacing(isViewFacing);
		return m;
	}

	public void draw(GL10 glUnused) {
		cam.pushM();
		cam.applyTransform(drawTransform);

		if(captureScreenPosition)
			captureScreenPosition();
		for(Marker m : markers)
			m.draw(glUnused);
		cam.popM();
	}

	public void selectionDraw(GL10 glUnused) {
		cam.pushM();
		cam.applyTransform(drawTransform);

		captureScreenPosition();
		for(Marker m : markers)
			m.selectionDraw(glUnused);
		cam.popM();
	}

	private int[] position = new int[] { 0, 0 };
	private float[] MV = new float[16];
	private float[] MVP = new float[16];

	private void captureScreenPosition() {
		Matrix.multiplyMM(MV, 0, cam.getViewMatrix(), 0, cam.getModelMatrix(), 0);
		Matrix.multiplyMM(MVP, 0, cam.getViewport().getProjectionMatrix(), 0, MV, 0);
		float x = MVP[12];
		float y = MVP[13];
		float w = MVP[14];
		position[0] = (int) Math.round((x * cam.getViewport().getWidth()) / (2.0 * w) + (cam.getViewport().getWidth() / 2.0));
		position[1] = cam.getViewport().getHeight() - (int) Math.round((y * cam.getViewport().getHeight()) / (2.0 * w) + (cam.getViewport().getHeight() / 2.0));
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

	@Override
	public void cleanup() {
		for(Marker m : markers)
			m.cleanup();
	}

	// ******************
	// Selection handling
	// ******************
	@Override
	public void mouseDown() {
		parentControl.setSelected(true);
		Log.i("InteractiveMarker", "Mouse down!");
		if(interactionMode == InteractionMode.MENU) {
			cam.getSelectionManager().clearSelection();
			parentControl.showMenu(this);
		} else {
			captureScreenPosition = true;
		}
	}

	@Override
	public void mouseUp() {
		parentControl.setSelected(false);
		Log.i("InteractiveMarker", "Mouse up!");
		captureScreenPosition = false;
	}

	@Override
	public int[] getPosition() {
		return position;
	}

	@Override
	public InteractionMode getInteractionMode() {
		return interactionMode;
	}

	private Quaternion deltaQuaternion;
	private Vector3 myAxis;

	@Override
	public void rotate(float dTheta) {
		Log.i("InteractiveMarker", "Rotate by " + dTheta);
		deltaQuaternion = Quaternion.fromAxisAngle(myAxis, Math.toRadians(dTheta));
		parentControl.childRotate(deltaQuaternion);
		parentControl.publish(this, interactionMode.val);
	}

	/**
	 * Return a Vec3 representing the axis about which the disc rotates. This rotates by -90 degrees about the X axis.
	 */
	private Vector3 getRotatedQuaternionAxis(Quaternion q) {
		return new Vector3(q.getX(), -q.getZ(), q.getY());
	}

	public void setParentTransform(Transform transform) {
		drawTransform.setTranslation(transform.getTranslation());

		if(orientationMode != OrientationMode.FIXED)
			drawTransform.setRotation(transform.getRotation().multiply(myOrientation));
		else
			drawTransform.setRotation(myOrientation);

	}

	public void setParentRotate(Quaternion q) {
		if(orientationMode != OrientationMode.FIXED)
			myAxis = q.rotateVector(myAxis);
	}

	private String name;

	public String getName() {
		return name;
	}
}
