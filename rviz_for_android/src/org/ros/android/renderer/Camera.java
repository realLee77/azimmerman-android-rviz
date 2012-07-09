package org.ros.android.renderer;

import javax.microedition.khronos.opengles.GL10;

import org.ros.namespace.GraphName;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import android.graphics.Point;

public interface Camera {

	public abstract void apply(GL10 gl);

	/**
	 * Moves the camera.
	 * 
	 * <p>
	 * The distances are given in viewport coordinates, not in world coordinates.
	 * 
	 * @param xDistance
	 *          distance in x to move
	 * @param yDistance
	 *          distance in y to move
	 */
	public abstract void moveCameraScreenCoordinates(float xDistance, float yDistance);

	public abstract void setCamera(Vector3 newCameraPoint);

	public abstract Vector3 getCamera();

	public abstract void zoomCamera(float factor);

	/**
	 * Returns the real world equivalent of the viewport coordinates specified.
	 * 
	 * @return the world coordinates of the provided screen coordinates
	 */
	public abstract Vector3 toWorldCoordinates(Point screenPoint);

	/**
	 * Returns the pose in the OpenGL world that corresponds to a screen
	 * coordinate and an orientation.
	 * 
	 * @param goalScreenPoint
	 *          the point on the screen
	 * @param orientation
	 *          the orientation of the pose on the screen
	 */
	public abstract Transform toOpenGLPose(Point goalScreenPoint, float orientation);

	public abstract GraphName getFixedFrame();

	public abstract void setFixedFrame(GraphName fixedFrame);

	public abstract void resetFixedFrame();

	public abstract void setTargetFrame(GraphName frame);

	public abstract void resetTargetFrame();

	public abstract GraphName getTargetFrame();

	public abstract Viewport getViewport();

	public abstract void setViewport(Viewport viewport);

	public abstract float getZoom();

	public abstract void setZoom(float zoom);

}