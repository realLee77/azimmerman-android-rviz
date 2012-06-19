package org.ros.android.view.visualization.shape;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.view.visualization.Vertices;
import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

public class TrianglesShape extends BaseShape {

	protected final FloatBuffer normals;
	protected final FloatBuffer vertices;
	protected final ShortBuffer indices;
	private boolean useIndices = false;
	protected int count;

	private FloatBuffer drawNormalsBuffer;

	/**
	 * @param vertices
	 *            an array of vertices as defined by OpenGL's GL_TRIANGLE_FAN method
	 * @param color
	 *            the {@link Color} of the {@link Shape}
	 */
	public TrianglesShape(float[] vertices, float[] normals, Color color) {
		this.vertices = Vertices.toFloatBuffer(vertices);
		this.normals = Vertices.toFloatBuffer(normals);
		this.indices = null;

		count = this.vertices.limit() / 3;
		
		createNormalsBuffer(vertices, normals);

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

	// TODO: Remove this, it's only for ensuring that meshes were loaded properly
	private void createNormalsBuffer(float[] vertices, float[] normals) {
		if(vertices.length != normals.length)
			throw new RuntimeException("MUST HAVE SAME NUMBER OF VERTICES AND NORMALS!");
		float[] output = new float[vertices.length * 2];
		for(int i = 0; i < vertices.length / 3; i++) {
			output[i * 6] = vertices[i * 3];
			output[i * 6 + 1] = vertices[i * 3 + 1];
			output[i * 6 + 2] = vertices[i * 3 + 2];

			output[i * 6 + 3] = vertices[i * 3] + 0.25f * normals[i * 3];
			output[i * 6 + 4] = vertices[i * 3 + 1] + 0.25f * normals[i * 3 + 1];
			output[i * 6 + 5] = vertices[i * 3 + 2] + 0.25f * normals[i * 3 + 2];
		}

		drawNormalsBuffer = Vertices.toFloatBuffer(output);
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

	public void drawNormals(GL10 gl) {
		gl.glColor4f(1f, 1f, 1f, 1f);

		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, drawNormalsBuffer);
		gl.glDrawArrays(GL10.GL_LINES, 0, drawNormalsBuffer.limit() / 3);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

	}
}
