package org.ros.android.rviz_for_android;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.view.visualization.layer.DefaultLayer;

public class GridLayer extends DefaultLayer {
	private int xCells, yCells;
	private float xSpacing, ySpacing;
	private int nLines;
	private float vertices[];
	private short indices[];
	private FloatBuffer vbb;
	private ShortBuffer ibb;

	public GridLayer(int xCells, int yCells, float xSpacing, float ySpacing) {
		super();
		this.xCells = xCells;
		this.yCells = yCells;
		this.xSpacing = xSpacing;
		this.ySpacing = ySpacing;
		initGrid();
	}

	private void initGrid() {
		nLines = 2*xCells + 2*yCells + 2;
		vertices = new float[3*(2*nLines)];
		indices = new short[2*nLines];
		float yMin = -ySpacing*yCells;
		float yMax = ySpacing*yCells;
		float xMin = -xSpacing*xCells;
		float xMax = xSpacing*xCells;
		
		int idx = -1;
		for(float x = xMin; x <= xMax; x += xSpacing) {
			// Edge start
			vertices[++idx] = x;
			vertices[++idx] = yMin;
			vertices[++idx] = 0;
			// Edge end
			vertices[++idx] = x;
			vertices[++idx] = yMax;
			vertices[++idx] = 0;
		}
		for(float y = yMin; y <= yMax; y += ySpacing) {
			// Edge start
			vertices[++idx] = xMin;
			vertices[++idx] = y;
			vertices[++idx] = 0;
			// Edge end
			vertices[++idx] = xMax;
			vertices[++idx] = y;
			vertices[++idx] = 0;	
		}
		
		for(int i = 0; i < 2*nLines; i ++) {
			indices[i] = (short)i;
		}
		
		System.out.println(Arrays.toString(vertices));
		System.out.println(Arrays.toString(indices));
		
		// Pack the vertices into a byte array
		ByteBuffer bb_vtx = ByteBuffer.allocateDirect(vertices.length * 4);
		bb_vtx.order(ByteOrder.nativeOrder());
		vbb = bb_vtx.asFloatBuffer();
		vbb.put(vertices);
		vbb.position(0);

		ByteBuffer bb_idx = ByteBuffer.allocateDirect(indices.length * 2);
		bb_idx.order(ByteOrder.nativeOrder());
		ibb = bb_idx.asShortBuffer();
		ibb.put(indices);
		ibb.position(0);
	}
	
	@Override
	public void draw(GL10 gl) {
		// TODO Auto-generated method stub
		super.draw(gl);
		
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vbb);
		
		gl.glDrawElements(GL10.GL_LINES, 2*nLines, GL10.GL_UNSIGNED_SHORT, ibb);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}
	
	
}
