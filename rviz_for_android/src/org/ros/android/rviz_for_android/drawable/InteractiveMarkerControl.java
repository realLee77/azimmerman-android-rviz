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
import org.ros.android.rviz_for_android.geometry.Ray;
import org.ros.android.rviz_for_android.geometry.Vector2;
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

	private Vector3 myAxis = Vector3.xAxis();
	private Vector3 myXaxis = Vector3.xAxis();

	private boolean captureScreenPosition = false;

	public InteractiveMarkerControl(visualization_msgs.InteractiveMarkerControl msg, Camera cam, MeshFileDownloader mfd, FrameTransformTree ftt, InteractiveMarker parentControl) {
		this.cam = cam;
		this.parentControl = parentControl;
		this.name = msg.getName();
		Log.d("InteractiveMarker", "Created interactive marker control " + name);

		interactionMode = InteractionMode.fromByte(msg.getInteractionMode());
		orientationMode = OrientationMode.fromByte(msg.getOrientationMode());
		isViewFacing = !msg.getIndependentMarkerOrientation() && orientationMode == OrientationMode.VIEW_FACING;

		// Normalize control orientation
		myOrientation = Quaternion.fromQuaternionMessage(msg.getOrientation());
		Utility.correctQuaternion(myOrientation);
		myOrientation = Utility.normalize(myOrientation);
		myXaxis = Utility.quatX(myOrientation);

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

		setParentTransform(parentControl.getTransform());
	}

	private static final Vector3 FIRST_ARROW_TRANSLATE = new Vector3(0.5, 0d, 0d);
	private static final Quaternion FIRST_ARROW_ROTATE = Quaternion.identity();
	private static final Vector3 SECOND_ARROW_TRANSLATE = new Vector3(-0.5, 0d, 0d);
	private static final Quaternion SECOND_ARROW_ROTATE = Quaternion.fromAxisAngle(Vector3.zAxis(), Math.PI);
	private static final Transform FIRST_ARROW_TRANSFORM = new Transform(FIRST_ARROW_TRANSLATE, FIRST_ARROW_ROTATE);
	private static final Transform SECOND_ARROW_TRANSFORM = new Transform(SECOND_ARROW_TRANSLATE, SECOND_ARROW_ROTATE);

	private boolean isViewFacing = false;

	private void autoCompleteMarker(visualization_msgs.InteractiveMarkerControl msg) {
		// Generate a control marker corresponding to the control type
		switch(msg.getInteractionMode()) {
		case visualization_msgs.InteractiveMarkerControl.MOVE_ROTATE:
		case visualization_msgs.InteractiveMarkerControl.MOVE_PLANE:
		case visualization_msgs.InteractiveMarkerControl.ROTATE_AXIS:
			Log.i("InteractiveMarker", "Rotate axis (RING)");
			Ring ring = Ring.newRing(cam, .5f, .65f, 20);
			Marker marker = instantiateControlMarker(ring, generateColor(myOrientation), cam);
			markers.add(marker);
			break;
		case visualization_msgs.InteractiveMarkerControl.MOVE_AXIS:
			Log.i("InteractiveMarker", "Move axis (ARROWS)");
			BaseShape arrowOne = Arrow.newArrow(cam, .08f, .15f, .2f, .2f);
			arrowOne.setTransform(FIRST_ARROW_TRANSFORM);
			BaseShape arrowTwo = Arrow.newArrow(cam, .08f, .15f, .2f, .2f);
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

	private float[] M = new float[16];
	private float[] MV = new float[16];
	private float[] MVP = new float[16];

	private void captureScreenPosition() {
		Utility.copyArray(cam.getModelMatrix(), M);
		Matrix.multiplyMM(MV, 0, cam.getViewMatrix(), 0, cam.getModelMatrix(), 0);
		Matrix.multiplyMM(MVP, 0, cam.getViewport().getProjectionMatrix(), 0, MV, 0);
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
		parentControl.publish(this, visualization_msgs.InteractiveMarkerFeedback.MOUSE_DOWN);
		parentControl.setSelected(true);
		Log.i("InteractiveMarker", "Mouse down!");
		if(interactionMode == InteractionMode.MENU) {
			cam.getSelectionManager().clearSelection();
			parentControl.showMenu(this);
		} else {
			captureScreenPosition = true;
			setMarkerSelection(true);
		}

		switch(orientationMode) {
		case FIXED:
			myAxis = myXaxis;
			break;
		case INHERIT:
			myAxis = parentControl.getTransform().getRotation().rotateVector(myXaxis);
			break;
		case VIEW_FACING:
			myAxis = getCameraVector(drawTransform.getTranslation()).invert();
			break;
		}

	}

	@Override
	public void mouseUp() {
		parentControl.publish(this, visualization_msgs.InteractiveMarkerFeedback.MOUSE_UP);
		parentControl.setSelected(false);
		Log.i("InteractiveMarker", "Mouse up!");
		captureScreenPosition = false;

		setMarkerSelection(false);
	}

	private static final Vector3 ORIGIN = Vector3.zero();
	private static final Vector3 XAXIS = Vector3.xAxis();
	private float[] resultVec = new float[4];
	private float[] positionVec = new float[4];
	private float x3d, y3d, w3d;

	private int[] getScreenPosition(Vector3 position) {
		positionVec[0] = (float) position.getX();
		positionVec[1] = (float) position.getY();
		positionVec[2] = (float) position.getZ();
		positionVec[3] = 1f;

		Matrix.multiplyMV(resultVec, 0, MVP, 0, positionVec, 0);
		x3d = resultVec[0];
		y3d = resultVec[1];
		w3d = resultVec[2];
		int[] retval = new int[2];
		retval[0] = (int) Math.round((x3d * cam.getViewport().getWidth()) / (2.0 * w3d) + (cam.getViewport().getWidth() / 2.0));
		retval[1] = cam.getViewport().getHeight() - (int) Math.round((y3d * cam.getViewport().getHeight()) / (2.0 * w3d) + (cam.getViewport().getHeight() / 2.0));
		return retval;
	}

	@Override
	public int[] getPosition() {
		return getScreenPosition(ORIGIN);
	}

	@Override
	public InteractionMode getInteractionMode() {
		return interactionMode;
	}

	private Quaternion deltaQuaternion;

	@Override
	public void rotate(float dTheta) {
		// Update axis of rotation
		if(orientationMode == OrientationMode.VIEW_FACING) {
			myAxis = getCameraVector(drawTransform.getTranslation()).invert();
		} else if(myAxis.dotProduct(getCameraVector(drawTransform.getTranslation())) > 0) {
			Log.d("InteractiveMarker", "Invert rotation axis");
			myAxis = myAxis.invert();
		}

		// Compute quaternion of rotation
		deltaQuaternion = Quaternion.fromAxisAngle(myAxis, Math.toRadians(dTheta));
		parentControl.childRotate(deltaQuaternion);
		parentControl.publish(this, visualization_msgs.InteractiveMarkerFeedback.POSE_UPDATE);
	}

	public void setParentTransform(Transform transform) {
		drawTransform.setTranslation(transform.getTranslation());

		if(orientationMode != OrientationMode.FIXED)
			drawTransform.setRotation(transform.getRotation().multiply(myOrientation));
		else
			drawTransform.setRotation(myOrientation);
	}

	private void setMarkerSelection(boolean selected) {
		for(Marker m : markers)
			m.setColorAsSelected(selected);
	}

	private String name;

	public String getName() {
		return name;
	}

	private Vector3 getCameraVector(Vector3 position) {
		return new Vector3(cam.getCamera().getX() * 2 - position.getX(), cam.getCamera().getY() * 2 - position.getY(), cam.getCamera().getZ() * 2 - position.getZ());
	}

	@Override
	public void translate(float X, float Y) {
		if(interactionMode == InteractionMode.MOVE_AXIS) {
			// Step 1: Project axis of motion to a ray on the screen
			int[] startpt = getScreenPosition(ORIGIN);
			int[] endpt = getScreenPosition(XAXIS);
			Vector2 screenRayDir = new Vector2(endpt[0] - startpt[0], endpt[1] - startpt[1]);
			Vector2 screenRayStart = new Vector2(startpt[0], startpt[1]);

			// If the axis isn't long enough, abort
			if(screenRayDir.length() <= 2) {
				Log.e("Move", "screen ray too short, aborting move axis");
				return;
			}

			// Step 2: Find nearest point on the screen ray to the mouse touch point
			Vector2 mousePt = new Vector2(X, Y);

			float num = (mousePt.subtract(screenRayStart)).dot(screenRayDir);
			float den = screenRayDir.dot(screenRayDir);
			Vector2 mouseActionPoint = screenRayStart.add(screenRayDir.scalarMultiply(num / den));

			// Step 3: Project the mouse action point into a 3D ray
			// Ray mouseRay = getMouseRay(X,Y);
			Ray mouseRay = getMouseRay(mouseActionPoint.getX(), mouseActionPoint.getY());

			if(Utility.containsNaN(mouseRay.getDirection()) || Utility.containsNaN(mouseRay.getStart())) {
				Log.e("Move", "NaN");
				return;
			}

			Vector3 axisRayStart = new Vector3(M[12], M[13], M[14]);
			Vector3 axisRayEnd = new Vector3(M[0] + M[12], M[1] + M[13], M[2] + M[14]);
			Ray axisRay = Ray.constructRay(axisRayStart, axisRayEnd); // new Ray(axisRayStart, axisRayEnd.subtract(axisRayStart));

			Vector3 result = axisRay.getClosestPoint(mouseRay);
			if(result == null)
				Log.e("Move", "Rays are parallel!");

			parentControl.childTranslate(result);
			parentControl.publish(this, visualization_msgs.InteractiveMarkerFeedback.POSE_UPDATE);
		} else if(interactionMode == InteractionMode.MOVE_PLANE) {
			// if(isViewFacing) {
			// Step 1: Construct the plane of motion

			// Find the camera view ray (ray from center of the camera forward)
			int centerX = cam.getViewport().getWidth() / 2;
			int centerY = cam.getViewport().getHeight() / 2;
			// float[] project_start = new float[3];
			// float[] project_end = new float[3];
			// int[] viewport = { 0, 0, cam.getViewport().getWidth(), cam.getViewport().getHeight() };
			// Utility.unProject(centerX, centerY, 0f, cam.getViewMatrix(), 0, cam.getViewport().getProjectionMatrix(), 0, viewport, 0, project_start, 0);
			// Utility.unProject(centerX, centerY, 1f, cam.getViewMatrix(), 0, cam.getViewport().getProjectionMatrix(), 0, viewport, 0, project_end, 0);
			//
			// Vector3 cameraRayDirection = new Vector3(project_end[0] - project_start[0], project_end[1] - project_start[1], project_end[2] - project_start[2]).normalized();

			// Log.i("Move", "Camera ray: " + centerX + ", " + centerY + " -> " + cameraRay);

			Ray cameraRay = getMouseRay(centerX, centerY);

			// Log.i("Move", "Camera ray: " + cameraRayDirection);

			Ray motionPlane = new Ray(parentControl.getTransform().getTranslation(), cameraRay.getDirection());

			// Step 2: Construct the mouse ray
			Ray mouseRay = getMouseRay(X, Y);

			// Step 3: Determine the intersection of the mouse ray and motion plane
			// I don't like the math rviz does, instead using math from
			// http://www.cs.princeton.edu/courses/archive/fall00/cs426/lectures/raycast/sld017.htm

			double t = motionPlane.getDirection().dotProduct(motionPlane.getStart().subtract(mouseRay.getStart())) / motionPlane.getDirection().dotProduct(mouseRay.getDirection());

			Vector3 pointInPlane = mouseRay.getPoint(t);
			Log.e("Move", "Motion point in plane: " + pointInPlane);

			parentControl.childTranslate(pointInPlane);
			parentControl.publish(this, visualization_msgs.InteractiveMarkerFeedback.POSE_UPDATE);
			// }
		}
	}

	@Override
	public Vector2 getScreenMotionVector() {
		int[] startpt = getScreenPosition(ORIGIN);
		int[] endpt = getScreenPosition(XAXIS);

		Vector2 screenRayDir = new Vector2(endpt[0] - startpt[0], endpt[1] - startpt[1]);
		return screenRayDir;
	}

	private int[] viewport = new int[4];
	private float[] project_start = new float[3];
	private float[] project_end = new float[3];

	private Ray getMouseRay(float x, float y) {
		// Step 3: Project the mouse action point into a 3D ray
		viewport[0] = 0;
		viewport[1] = 0;
		viewport[2] = cam.getViewport().getWidth();
		viewport[3] = cam.getViewport().getHeight();

		// Flip the Y coordinate of the mouse action point to put it in OpenGL pixel space
		Utility.unProject(x + cam.getScreenDisplayOffset()[0], viewport[3] - y + cam.getScreenDisplayOffset()[1], 0f, cam.getViewMatrix(), 0, cam.getViewport().getProjectionMatrix(), 0, viewport, 0, project_start, 0);
		Utility.unProject(x + cam.getScreenDisplayOffset()[0], viewport[3] - y + cam.getScreenDisplayOffset()[1], 1f, cam.getViewMatrix(), 0, cam.getViewport().getProjectionMatrix(), 0, viewport, 0, project_end, 0);

		Vector3 mouseray_start = new Vector3(project_start[0], project_start[1], project_start[2]);
		Vector3 mouseray_dir = new Vector3(project_end[0] - project_start[0], project_end[1] - project_start[1], project_end[2] - project_start[2]);

		return new Ray(mouseray_start, mouseray_dir.normalized());
	}

}
