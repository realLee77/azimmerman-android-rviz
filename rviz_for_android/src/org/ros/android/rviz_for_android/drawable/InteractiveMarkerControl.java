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
import org.ros.android.renderer.shapes.GenericColoredShape;
import org.ros.android.rviz_for_android.urdf.MeshFileDownloader;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import visualization_msgs.InteractiveMarkerFeedback;
import android.opengl.GLES20;
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

	private GenericColoredShape axis;

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

		// TODO:
		axis = new GenericColoredShape(cam, GLES20.GL_LINES, new float[] { 0f, 0f, 0f, 0f, 0f, 0f });
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
		axis.draw(glUnused);
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
		}

		// if(interactionMode == InteractionMode.ROTATE_AXIS) {
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
		// }
		
//		axis = new GenericColoredShape(cam, GLES20.GL_LINES, new float[] { 0f, 0f, 0f, 1f, 0f, 0f});
	}

	@Override
	public void mouseUp() {
		parentControl.publish(this, visualization_msgs.InteractiveMarkerFeedback.MOUSE_UP);
		parentControl.setSelected(false);
		Log.i("InteractiveMarker", "Mouse up!");
		captureScreenPosition = false;
	}

	private static final Vector3 ORIGIN = Vector3.zero();
	private static final Vector3 XAXIS = Vector3.xAxis();
	private float[] resultVec = new float[4];
	private float[] positionVec = new float[4];
	private float x3d,y3d,w3d;
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
//		if(interactionMode == InteractionMode.ROTATE_AXIS)
			return getScreenPosition(ORIGIN);
//		else
//			return getScreenPosition(XAXIS);
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

	private String name;

	public String getName() {
		return name;
	}

	private Vector3 getCameraVector(Vector3 position) {
		return new Vector3(cam.getCamera().getX() * 2 - position.getX(), cam.getCamera().getY() * 2 - position.getY(), cam.getCamera().getZ() * 2 - position.getZ());
	}

	@Override
	public void translate(float dX, float dY) {
		if(interactionMode == InteractionMode.MOVE_AXIS) {
			int[] center = getScreenPosition(ORIGIN);
			int[] vecCenter = getScreenPosition(Vector3.xAxis());
			int[] vector = new int[] {center[0] - vecCenter[0], center[1] - vecCenter[1]};
			double length = Math.sqrt(vector[0]*vector[0] + vector[1]*vector[1]);
			double[] screenVectorN = new double[] {vector[0]/length, vector[1]/length};
			
			length = Math.sqrt(dX*dX + dY*dY);
			if(length == 0 || length == Double.NaN)
				return;
				
			double[] dragVectorN = new double[] {dX/length, dY/length};
			
			// Normalize the vectors
//			length = Math.sqrt(center[0]*center[0] + center[1]*center[1]);
//			double[] centerN = new double[] {center[0]/length, center[1]/length};
//			length =Math.sqrt(vecCenter[0]*vecCenter[0] + vecCenter[1]*vecCenter[1]);
//			double[] vecCenterN = new double[] {vecCenter[0]/length, vecCenter[1]/length};
			
			double dp = (screenVectorN[0]*dragVectorN[0] + screenVectorN[1]*dragVectorN[1]);			
			
			Log.i("InteractiveMarker", "DP = " + dp + "   scale = " + w3d);
			dp = dp/w3d;
			Vector3 amt = new Vector3(myAxis.getX()*dp, myAxis.getY()*dp, myAxis.getZ()*dp);
			parentControl.childTranslate(amt);
			parentControl.publish(this, visualization_msgs.InteractiveMarkerFeedback.POSE_UPDATE);
			// Screen vector = projection of my axis onto the screen
			// Dot product of delta vector and screen vector = percent of axis to add to position
		}
	}
}
