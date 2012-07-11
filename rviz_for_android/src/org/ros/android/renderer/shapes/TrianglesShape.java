package org.ros.android.renderer.shapes;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
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
	public void draw(GL10 glUnused, Camera cam) {
		super.draw(glUnused, cam);
		glUnused.glColor4f(getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor().getAlpha());

		glUnused.glVertexPointer(3, GL10.GL_FLOAT, 0, vertices);
		glUnused.glEnableClientState(GL10.GL_VERTEX_ARRAY);

		glUnused.glNormalPointer(GL10.GL_FLOAT, 0, normals);
		glUnused.glEnableClientState(GL10.GL_NORMAL_ARRAY);

		if(useIndices)
			glUnused.glDrawElements(GL10.GL_TRIANGLES, count, GL10.GL_UNSIGNED_SHORT, indices);
		else
			glUnused.glDrawArrays(GL10.GL_TRIANGLES, 0, count);

		glUnused.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		glUnused.glDisableClientState(GL10.GL_NORMAL_ARRAY);
	}
}
