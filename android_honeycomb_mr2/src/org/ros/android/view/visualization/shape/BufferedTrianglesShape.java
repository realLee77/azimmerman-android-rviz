package org.ros.android.view.visualization.shape;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import org.ros.android.view.visualization.Vertices;
import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import android.opengl.GLES11;
import android.opengl.GLES20;

/**
 * A triangles shape which uses vertex buffers to cache geometry on the GPU. Vertices and normals are stored in a packed buffer: Xv,Yv,Zv, Xn,Yn,Zn, ...
 * @author azimmerman
 *
 */
public class BufferedTrianglesShape extends BaseShape implements BatchDrawable {
	private FloatBuffer packedBuffer;
	private boolean bufferPrepared = false;
	protected int count;

	public BufferedTrianglesShape(float[] vertices, float[] normals, Color color) {
		packedBuffer = packBuffer(vertices, normals, null);
		count = vertices.length / 3;

		setColor(color);
		setTransform(new Transform(new Vector3(0, 0, 0), new Quaternion(0, 0, 0, 1)));
	}

	public BufferedTrianglesShape(float[] vertices, float[] normals, short[] indices, Color color) {
		packedBuffer = packBuffer(vertices, normals, indices);
		
		count = indices.length;

		setColor(color);
		setTransform(new Transform(new Vector3(0, 0, 0), new Quaternion(0, 0, 0, 1)));
	}
	
	private FloatBuffer packBuffer(float[] vertices, float[] normals, short[] indices) {
		if(vertices.length != normals.length && indices == null)
			throw new IllegalArgumentException("Vertex array and normal array must be the same length!");
		
		bufferPrepared = false;
		float[] packedBuffer;
		
		if(indices == null) {		
			int bufferLength = vertices.length*2;
			int arrayElements = vertices.length/3;
			int vIdx = 0, nIdx = 0;
			
			packedBuffer = new float[bufferLength];
			for(int i = 0; i < arrayElements; i++) {
				int idx = i*6;
				packedBuffer[idx+0] = vertices[vIdx++];
				packedBuffer[idx+1] = vertices[vIdx++];
				packedBuffer[idx+2] = vertices[vIdx++];
				
				packedBuffer[idx+3] = normals[nIdx++];
				packedBuffer[idx+4] = normals[nIdx++];
				packedBuffer[idx+5] = normals[nIdx++];
			}
		} else {
			// TODO: Add deindexing
			throw new IllegalArgumentException("Packed buffers currently not supported, sorry!");
		}
		
		return Vertices.toFloatBuffer(packedBuffer);
	}

	private int bufferIdx = -1;
	private static final int FLOAT_SIZE = Float.SIZE/8;
	private static final int stride = 6*FLOAT_SIZE; 
	
	@Override
	public void draw(GL10 gl) {
		if(!bufferPrepared)
			bufferIdx = createVertexBuffer(gl);
		
		super.draw(gl);
		GLES11.glColor4f(getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor().getAlpha());

		GLES11.glBindBuffer(GL11.GL_ARRAY_BUFFER, bufferIdx);
		GLES11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		GLES11.glVertexPointer(3, GL11.GL_FLOAT, stride, 0);
		
        GLES11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
        GLES11.glNormalPointer(GL11.GL_FLOAT, stride, 3*FLOAT_SIZE);
        
        GLES11.glDrawArrays(GL10.GL_TRIANGLES, 0, count);
        
        GLES11.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		GLES11.glDisableClientState(GL10.GL_NORMAL_ARRAY);
		GLES11.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
	}

	private int createVertexBuffer(GL10 gl) {
		final int[] buffers = new int[1];
		GLES11.glGenBuffers(1, buffers, 0);
		GLES11.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);
		GLES11.glBufferData(GLES20.GL_ARRAY_BUFFER, packedBuffer.capacity()*FLOAT_SIZE, packedBuffer, GLES20.GL_STATIC_DRAW);
		
		bufferPrepared = true;
		return buffers[0];
	}

	@Override
	public void batchDraw(GL10 gl) {
		if(!bufferPrepared)
			bufferIdx = createVertexBuffer(gl);
		
		super.draw(gl);
		GLES11.glColor4f(getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor().getAlpha());

		GLES11.glBindBuffer(GL11.GL_ARRAY_BUFFER, bufferIdx);
		GLES11.glVertexPointer(3, GL11.GL_FLOAT, stride, 0);

        GLES11.glNormalPointer(GL11.GL_FLOAT, stride, 3*FLOAT_SIZE);
        
        GLES11.glDrawArrays(GL10.GL_TRIANGLES, 0, count);

		GLES11.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
	}
}
