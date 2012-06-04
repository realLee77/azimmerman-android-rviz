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

package org.ros.android.view.visualization;

import javax.microedition.khronos.opengles.GL10;

import org.ros.namespace.GraphName;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import android.graphics.Point;

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
	private static final GraphName DEFAULT_FIXED_FRAME = new GraphName("/world");

	/**
	 * The default target frame is null which means that the renderer uses the user set camera.
	 */
	private static final GraphName DEFAULT_TARGET_FRAME = null;

	/**
	 * Most the user can zoom in.
	 */
	private static final float MINIMUM_ZOOM = 1.0f;

	/**
	 * Most the user can zoom out.
	 */
	private static final float MAXIMUM_ZOOM = 500.0f;

	// TODO: Make this variable (possibly changing with pinch instead of zoom?)
	private float orbitRadius = 5.0f;
	private static final float MAX_FLING_VELOCITY = 25;
	private static final float MIN_FLING_VELOCITY = 0.05f;
	private static final float MAX_TRANSLATE_SPEED = 0.18f;
	
	private float angleTheta = (float) (Math.PI / 4);
	private float anglePhi = (float) (Math.PI / 4);
	private Vector3 location;
	private Vector3 lookTarget;
	
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

	public OrbitCamera(FrameTransformTree frameTransformTree) {
		this.frameTransformTree = frameTransformTree;
		fixedFrame = DEFAULT_FIXED_FRAME;
		lookTarget = Vector3.newIdentityVector3();
		location = Vector3.newIdentityVector3();
		updateLocation();
	}

	public void apply(GL10 gl) {
		viewport.zoom(gl);
		velocityUpdate();
		rotateOrbit(gl);
	}

	private void rotateOrbit(GL10 gl) {
		android.opengl.GLU.gluLookAt(gl, (float) location.getX(), (float) location.getY(), (float) location.getZ(), (float) lookTarget.getX(), (float) lookTarget.getY(), (float) lookTarget.getZ(), 0, 0, 1f);
		gl.glTranslatef(-(float) location.getX(), -(float) location.getY(), -(float) location.getZ());
	}
	
	private void velocityUpdate() {
		if(vTheta != 0f || vPhi != 0f) {
			moveOrbitPosition(vPhi, vTheta);
			vTheta *= .9f;
			vPhi *= .9f;
		}		
		
		if(Math.abs(vTheta) < MIN_FLING_VELOCITY) vTheta = 0;
		if(Math.abs(vPhi) < MIN_FLING_VELOCITY) vPhi = 0;
	}

	private void updateLocation() {
		location.setX((float) lookTarget.getX() + (orbitRadius * Math.sin(angleTheta) * Math.cos(anglePhi)));
		location.setY((float) lookTarget.getY() + (orbitRadius * Math.sin(angleTheta) * Math.sin(anglePhi)));
		location.setZ((float) lookTarget.getZ() + (orbitRadius * Math.cos(angleTheta)));
	}
	
	public void flingCamera(float vX, float vY) {
		vPhi = Utility.cap(-vX/500, -MAX_FLING_VELOCITY, MAX_FLING_VELOCITY);
		vTheta = Utility.cap(-vY/500, -MAX_FLING_VELOCITY, MAX_FLING_VELOCITY);
	}
	
	public void moveOrbitPosition(float xDistance, float yDistance) {
		anglePhi += Math.toRadians(xDistance);
		anglePhi = Utility.angleWrap(anglePhi);

		angleTheta += Math.toRadians(yDistance);
		angleTheta = Utility.cap(angleTheta, 0.00872664626f, 3.13286601f);

		updateLocation();
	}
	

	@Override
	public void moveCameraScreenCoordinates(float xDistance, float yDistance) {
		float xDistCap = Utility.cap(xDistance, -MAX_TRANSLATE_SPEED, MAX_TRANSLATE_SPEED);
		float yDistCap = Utility.cap(yDistance, -MAX_TRANSLATE_SPEED,MAX_TRANSLATE_SPEED);
			
		lookTarget = lookTarget.subtract(new Vector3(Math.cos(anglePhi-Math.PI/2)*xDistCap - Math.sin(anglePhi+Math.PI/2)*yDistCap, Math.sin(anglePhi-Math.PI/2)*xDistCap + Math.cos(anglePhi+Math.PI/2)*yDistCap, 0));
		updateLocation();
	}

	public void setCamera(Vector3 newCameraPoint) {
		resetTargetFrame();
		// location = newCameraPoint;
	}

	public Vector3 getCamera() {
		return location;
	}

	public void zoomCamera(float factor) {
		orbitRadius *= factor;
//		float zoom = viewport.getZoom() * factor;
//		if (zoom < MINIMUM_ZOOM) {
//			zoom = MINIMUM_ZOOM;
//		} else if (zoom > MAXIMUM_ZOOM) {
//			zoom = MAXIMUM_ZOOM;
//		}
//		viewport.setZoom(zoom);
	}

	/**
	 * Returns the real world equivalent of the viewport coordinates specified.
	 * 
	 * @return the world coordinates of the provided screen coordinates
	 */
	public Vector3 toWorldCoordinates(Point screenPoint) {
		// Top left corner of the view is the origin.
		double x = 2.0d * screenPoint.x / viewport.getWidth() - 1.0d;
		double y = 1.0d - 2.0d * screenPoint.y / viewport.getHeight();
		// Apply the viewport transformation.
		x *= viewport.getWidth() / 2.0d / viewport.getZoom();
		y *= viewport.getHeight() / 2.0d / viewport.getZoom();
		// Exchange x and y for the rotation and add the translation.
		// return new Vector3(y + location.getX(), -x + location.getY(), 0);
		return new Vector3(-1, -1, -1);
	}

	/**
	 * Returns the pose in the OpenGL world that corresponds to a screen coordinate and an orientation.
	 * 
	 * @param goalScreenPoint
	 *            the point on the screen
	 * @param orientation
	 *            the orientation of the pose on the screen
	 */
	public Transform toOpenGLPose(Point goalScreenPoint, float orientation) {
		return new Transform(toWorldCoordinates(goalScreenPoint), Quaternion.newFromAxisAngle(new Vector3(0, 0, -1), orientation + Math.PI / 2));
	}

	public GraphName getFixedFrame() {
		return fixedFrame;
	}

	public void setFixedFrame(GraphName fixedFrame) {
		Preconditions.checkNotNull(fixedFrame, "Fixed frame must be specified.");
		this.fixedFrame = fixedFrame;
		// To prevent camera jumps, we always center on the fixedFrame when
		// it is reset.
		// location = Vector3.newIdentityVector3();
	}

	public void resetFixedFrame() {
		fixedFrame = DEFAULT_FIXED_FRAME;
	}

	public void setTargetFrame(GraphName frame) {
		targetFrame = frame;
	}

	public void resetTargetFrame() {
		targetFrame = DEFAULT_TARGET_FRAME;
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

	public float getZoom() {
		return viewport.getZoom();
	}

	public void setZoom(float zoom) {
		viewport.setZoom(zoom);
	}
}
