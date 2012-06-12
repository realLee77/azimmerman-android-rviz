package org.ros.android.rviz_for_android.vis;

import org.ros.android.view.visualization.Camera;
import org.ros.node.ConnectedNode;
import org.ros.rosjava.tf.TransformTree;
import org.ros.rosjava_geometry.Vector3;

import android.content.Context;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class OrbitCameraTFControlLayer extends DefaultLayerTF {

	private final Context context;
	protected boolean enableScrolling = true;
	private static final float TOUCH_ORBIT_COEFFICIENT = 0.25f;

	private GestureDetector gestureDetector;
	private ScaleGestureDetector scaleGestureDetector;

	private Vector3 prevScaleCenter = Vector3.newIdentityVector3();

	public OrbitCameraTFControlLayer(Context context) {
		this.context = context;
	}

	@Override
	public boolean onTouchEvent(VisualizationViewTF view, MotionEvent event) {
		if(gestureDetector != null) {
			if(gestureDetector.onTouchEvent(event)) {
				return true;
			}
			return scaleGestureDetector.onTouchEvent(event);
		}
		return false;
	}

	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler, TransformTree TransformTree, final Camera camera) {
		if(!(camera instanceof OrbitCameraTF))
			throw new IllegalArgumentException("OrbitCameraControlLayer can only be used with OrbitCamera objects!");
		final OrbitCameraTF cam = (OrbitCameraTF) camera;

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
						if(enableScrolling)
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
