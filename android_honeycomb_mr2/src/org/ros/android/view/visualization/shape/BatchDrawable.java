package org.ros.android.view.visualization.shape;

import javax.microedition.khronos.opengles.GL10;

public interface BatchDrawable extends Shape {
	/**
	 * A draw method called in place of draw(gl) which does as few glEnable and glDisable calls as possible to improve performance.
	 * Objects implementing this may assume that GL_VERTEX_ARRAY and GL_NORMAL_ARRAY have been enabled
	 * @param gl
	 */
	public void batchDraw(GL10 gl);
}
