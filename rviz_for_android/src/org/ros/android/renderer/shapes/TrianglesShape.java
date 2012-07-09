package org.ros.android.renderer.shapes;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Vertices;
import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

public class TrianglesShape extends BaseShape {

	protected final FloatBuffer normals;
	protected final FloatBuffer vertices;
	protected final ShortBuffer indices;
	private boolean useIndices = false;
	protected int count;

	/**
	 * @param vertices
	 *            an array of vertices as defined by OpenGL's GL_TRIANGLES method
	 * @param color
	 *            the {@link Color} of the {@link Shape}
	 */
	public TrianglesShape(float[] vertices, float[] normals, Color color) {
		this.vertices = Vertices.toFloatBuffer(vertices);
		this.normals = Vertices.toFloatBuffer(normals);
		this.indices = null;

		count = this.vertices.limit() / 3;

		setColor(color);
		setTransform(new Transform(new Vector3(0, 0, 0), new Quaternion(0, 0, 0, 1)));
	}

	public TrianglesShape(float[] vertices, float[] normals, short[] indices, Color color) {
		this.vertices = Vertices.toFloatBuffer(vertices);
		this.normals = Vertices.toFloatBuffer(normals);
		this.indices = Vertices.toShortBuffer(indices);
		useIndices = true;
		
		count = this.indices.limit();

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

		if(useIndices)
			gl.glDrawElements(GL10.GL_TRIANGLES, count, GL10.GL_UNSIGNED_SHORT, indices);
		else
			gl.glDrawArrays(GL10.GL_TRIANGLES, 0, count);

		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
	}
}
