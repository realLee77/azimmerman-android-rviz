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

package org.ros.android.view.visualization.layer;

import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.OrbitCamera;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.node.ConnectedNode;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Vector3;

import android.content.Context;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class OrbitCameraControlLayer extends DefaultLayer {

	private final Context context;

	private static final float TOUCH_ORBIT_COEFFICIENT = 0.25f;

	private GestureDetector gestureDetector;
	private ScaleGestureDetector scaleGestureDetector;

	private Vector3 prevScaleCenter = Vector3.newIdentityVector3();

	public OrbitCameraControlLayer(Context context) {
		this.context = context;
	}

	@Override
	public boolean onTouchEvent(VisualizationView view, MotionEvent event) {
		if(gestureDetector.onTouchEvent(event)) {
			return true;
		}
		return scaleGestureDetector.onTouchEvent(event);
	}

	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler, FrameTransformTree frameTransformTree, final Camera camera) {
		if(!(camera instanceof OrbitCamera))
			throw new RuntimeException("OrbitCameraControlLayer can only be used with OrbitCamera objects!");
		final OrbitCamera cam = (OrbitCamera) camera;

		handler.post(new Runnable() {
			@Override
			public void run() {
				gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
					@Override
					public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
						cam.moveOrbitPosition(distanceX * TOUCH_ORBIT_COEFFICIENT, distanceY * TOUCH_ORBIT_COEFFICIENT);
						requestRender();
						return true;
					}

					@Override
					public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
						cam.flingCamera(velocityX, velocityY);
						return true;
					}

				});

				scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
					@Override
					public boolean onScaleBegin(ScaleGestureDetector detector) {
						prevScaleCenter.setX(detector.getFocusX());
						prevScaleCenter.setY(detector.getFocusY());
						return true;
					}

					@Override
					public boolean onScale(ScaleGestureDetector detector) {
						Vector3 diff = prevScaleCenter.subtract(new Vector3(detector.getFocusX(), detector.getFocusY(), 0));
						cam.moveCameraScreenCoordinates((float) diff.getX() / 50, (float) diff.getY() / 50);

						prevScaleCenter.setX(detector.getFocusX());
						prevScaleCenter.setY(detector.getFocusY());

						camera.zoomCamera(detector.getScaleFactor());
						requestRender();

						return true;
					}
				});
			}
		});
	}
}
