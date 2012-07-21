/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.android.renderer;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.impl.WeakHashtable;
import org.ros.namespace.GraphName;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import android.graphics.Point;
import android.opengl.Matrix;

import com.google.common.base.Preconditions;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class OrbitCamera implements Camera {
	/**
	 * The default reference frame.
	 * 
	 * TODO(moesenle): make this the root of the TF tree.
	 */
	private static final GraphName DEFAULT_FIXED_FRAME = GraphName.of("/world");

	/**
	 * The default target frame is null which means that the renderer uses the user set camera.
	 */
	private static final GraphName DEFAULT_TARGET_FRAME = null;

	private float orbitRadius = 5.0f;
	private static final float MAX_FLING_VELOCITY = 25;
	private static final float MIN_FLING_VELOCITY = 0.05f;
	private static final float MAX_TRANSLATE_SPEED = 0.18f;

	private float angleTheta = (float) (Math.PI / 4);
	private float anglePhi = (float) (Math.PI / 4);
	private Vector3 location;
	private Vector3 lookTarget;

	private float[] mView = new float[16];

	private float vTheta = 0;
	private float vPhi = 0;

	/**
	 * Size of the viewport.
	 */
	private Viewport viewport;

	/**
	 * The TF frame the camera is locked on. If set, the camera point is set to the location of this frame in fixedFrame. If the camera is set or moved, the lock is removed.
	 */
	private GraphName targetFrame;

	/**
	 * The frame in which to render everything. The default value is /map which indicates that everything is rendered in map. If this is changed to, for instance, base_link, the view follows the robot and the robot itself is in the origin.
	 */
	private GraphName fixedFrame;

	private FrameTransformTree frameTransformTree;

	private SelectionManager sm = new SelectionManager();

	public OrbitCamera(FrameTransformTree frameTransformTree) {
		// Initialize the fixed frame
		this.frameTransformTree = frameTransformTree;
		fixedFrame = DEFAULT_FIXED_FRAME;

		// Initialize the location
		location = Vector3.newIdentityVector3();
		lookTarget = Vector3.newIdentityVector3();
		updateLocation();
		location = location.add(lookTarget);

		// Initialize the model matrix and stack
		loadIdentityM();
		Matrix.setIdentityM(stackM[0], 0);
	}

	public void apply() {
		velocityUpdate();

		synchronized(fixedFrame) {
			if(targetFrame != null) {
				lookTarget = frameTransformTree.newTransformIfPossible(targetFrame, fixedFrame).getTranslation();
				lookTarget.setX(lookTarget.getX() / 2);
				lookTarget.setY(lookTarget.getY() / 2);
				lookTarget.setZ(lookTarget.getZ() / 2);
				updateLocation();
			}
		}

		rotateOrbit();
	}

	private void rotateOrbit() {
		Matrix.setLookAtM(mView, 0, (float) location.getX(), (float) location.getY(), (float) location.getZ(), (float) lookTarget.getX(), (float) lookTarget.getY(), (float) lookTarget.getZ(), 0, 0, 1f);
		Matrix.translateM(mView, 0, -(float) location.getX(), -(float) location.getY(), -(float) location.getZ());
	}

	private void updateLocation() {
		location.setX((float) lookTarget.getX() + (orbitRadius * Math.sin(angleTheta) * Math.cos(anglePhi)));
		location.setY((float) lookTarget.getY() + (orbitRadius * Math.sin(angleTheta) * Math.sin(anglePhi)));
		location.setZ((float) lookTarget.getZ() + (orbitRadius * Math.cos(angleTheta)));
	}

	private void velocityUpdate() {
		if(vTheta != 0f || vPhi != 0f) {
			moveOrbitPosition(vPhi, vTheta);
			vTheta *= .9f;
			vPhi *= .9f;
		}

		if(Math.abs(vTheta) < MIN_FLING_VELOCITY)
			vTheta = 0;
		if(Math.abs(vPhi) < MIN_FLING_VELOCITY)
			vPhi = 0;
	}

	public void flingCamera(float vX, float vY) {
		vPhi = Utility.cap(-vX / 500, -MAX_FLING_VELOCITY, MAX_FLING_VELOCITY);
		vTheta = Utility.cap(-vY / 500, -MAX_FLING_VELOCITY, MAX_FLING_VELOCITY);
	}

	public void moveOrbitPosition(float xDistance, float yDistance) {
		anglePhi += Math.toRadians(xDistance);
		anglePhi = Utility.angleWrap(anglePhi);

		angleTheta += Math.toRadians(yDistance);
		angleTheta = Utility.cap(angleTheta, 0.00872664626f, 3.13286601f);

		updateLocation();
	}

	private float translationScaleFactor = 5f / 6f;

	@Override
	public void moveCameraScreenCoordinates(float xDistance, float yDistance) {
		float xDistCap = Utility.cap(xDistance, -MAX_TRANSLATE_SPEED, MAX_TRANSLATE_SPEED) * translationScaleFactor;
		float yDistCap = Utility.cap(yDistance, -MAX_TRANSLATE_SPEED, MAX_TRANSLATE_SPEED) * translationScaleFactor;

		lookTarget = lookTarget.subtract(new Vector3(Math.cos(anglePhi - Math.PI / 2) * xDistCap - Math.sin(anglePhi + Math.PI / 2) * yDistCap, Math.sin(anglePhi - Math.PI / 2) * xDistCap + Math.cos(anglePhi + Math.PI / 2) * yDistCap, 0));
		updateLocation();
	}

	public void setCamera(Vector3 newCameraPoint) {
		resetTargetFrame();
		lookTarget = newCameraPoint;
	}

	public Vector3 getCamera() {
		return location;
	}

	public void zoomCamera(float factor) {
		orbitRadius /= factor;
		translationScaleFactor = orbitRadius / 6.0f;
	}

	public GraphName getFixedFrame() {
		return fixedFrame;
	}

	public void setFixedFrame(GraphName fixedFrame) {
		Preconditions.checkNotNull(fixedFrame, "Fixed frame must be specified.");
		this.fixedFrame = fixedFrame;
	}

	public void resetFixedFrame() {
		synchronized(fixedFrame) {
			fixedFrame = DEFAULT_FIXED_FRAME;
		}
	}

	public void resetTargetFrame() {
		targetFrame = DEFAULT_TARGET_FRAME;
	}

	public void setTargetFrame(GraphName frame) {
		targetFrame = frame;
	}

	public GraphName getTargetFrame() {
		return targetFrame;
	}

	public Viewport getViewport() {
		return viewport;
	}

	public void setViewport(Viewport viewport) {
		this.viewport = viewport;
	}

	public Vector3 toWorldCoordinates(Point screenPoint) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Transform toOpenGLPose(Point goalScreenPoint, float orientation) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Set the camera look target to the fixed frame or the origin if one isn't set
	 */
	public void resetLookTarget() {
		lookTarget = Vector3.newIdentityVector3();
	}

	public void resetZoom() {
		this.orbitRadius = 5f;
		updateLocation();
	}

	@Override
	public float getZoom() {
		return 0;
	}

	@Override
	public void setZoom(float zoom) {
	}

	// Copy the contents of an array without instantiating a new object
	private void copyArray(float[] source, float[] dest) {
		for(int i = 0; i < 16; i++)
			dest[i] = source[i];
	}

	@Override
	public float[] getViewMatrix() {
		return mView;
	}

	private float[] modelM = new float[16];
	private float[][] stackM = new float[16][16];
	private int stackPointer = 0;

	@Override
	public void pushM() {
		stackPointer++;
		copyArray(modelM, stackM[stackPointer]);
	}

	@Override
	public void popM() {
		stackPointer--;
		copyArray(stackM[stackPointer], modelM);
		if(stackPointer < 0)
			throw new RuntimeException("Can not remove the last element in the model matrix stack!");
	}

	@Override
	public void translateM(float x, float y, float z) {
		Matrix.translateM(modelM, 0, x, y, z);
	}

	@Override
	public void scaleM(float sx, float sy, float sz) {
		Matrix.scaleM(modelM, 0, sx, sy, sz);
	}

	@Override
	public void rotateM(float a, float x, float y, float z) {
		Matrix.rotateM(modelM, 0, a, x, y, z);
	}

	@Override
	public void loadIdentityM() {
		Matrix.setIdentityM(modelM, 0);
	}

	@Override
	public void applyTransform(Transform transform) {
		translateM((float) transform.getTranslation().getX(), (float) transform.getTranslation().getY(), (float) transform.getTranslation().getZ());
		double angleDegrees = Math.toDegrees(transform.getRotation().getAngle());
		if(angleDegrees != 0) {
			Vector3 axis = transform.getRotation().getAxis();
			rotateM((float) angleDegrees, (float) axis.getX(), (float) axis.getY(), (float) axis.getZ());
		}
	}

	@Override
	public String toString() {
		return "Location: " + location.toString() + " Look target: " + lookTarget.toString();
	}

	@Override
	public float[] getModelMatrix() {
		return modelM;
	}

	@Override
	public SelectionManager getSelectionManager() {
		return sm;
	}

	
	private Set<TargetFrameListener> targetFrameListeners = new HashSet<TargetFrameListener>();
	
	@Override
	public void addTargetFrameChangeListener(TargetFrameListener l) {
		
	}
}
