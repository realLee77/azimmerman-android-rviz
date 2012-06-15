package org.ros.android.view.visualization.shape;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.view.visualization.Vertices;
import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

public class TrianglesShape extends BaseShape {

	private final FloatBuffer normals;
	private final FloatBuffer vertices;

	/**
	 * @param vertices
	 *            an array of vertices as defined by OpenGL's GL_TRIANGLE_FAN method
	 * @param color
	 *            the {@link Color} of the {@link Shape}
	 */
	public TrianglesShape(float[] vertices, float[] normals, Color color) {
		this.vertices = Vertices.toFloatBuffer(vertices);
		this.normals = Vertices.toFloatBuffer(normals);

		setColor(color);
		setTransform(new Transform(new Vector3(0, 0, 0), new Quaternion(0, 0, 0, 1)));
	}

	@Override
	public void draw(GL10 gl) {
		super.draw(gl);
		gl.glColor4f(getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor().getAlpha());
		
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertices);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		
		gl.glNormalPointer(GL10.GL_FLOAT, 0, normals);
		gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
		
		gl.glDrawArrays(GL10.GL_TRIANGLES, 0, vertices.limit()/3);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);		
	}
}
