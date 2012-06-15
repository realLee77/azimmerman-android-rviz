package org.ros.android.view.visualization.shape;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.view.visualization.Vertices;
import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

public class TriangleStripShape extends BaseShape {

	private final FloatBuffer normals;
	private final FloatBuffer vertices;
	private final ShortBuffer indices;

	/**
	 * @param vertices
	 *            an array of vertices as defined by OpenGL's GL_TRIANGLE_FAN method
	 * @param color
	 *            the {@link Color} of the {@link Shape}
	 */
	public TriangleStripShape(float[] vertices, short[] indices, float[] normals, Color color) {
		this.vertices = Vertices.toFloatBuffer(vertices);
		this.normals = Vertices.toFloatBuffer(normals);

		ByteBuffer bb_idx = ByteBuffer.allocateDirect(indices.length * 2);
		bb_idx.order(ByteOrder.nativeOrder());
		this.indices = bb_idx.asShortBuffer();
		this.indices.put(indices);
		this.indices.position(0);

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
		
		gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, indices.limit(), GL10.GL_UNSIGNED_SHORT, indices);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);		
	}
}
