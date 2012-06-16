package org.ros.android.rviz_for_android.drawable;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.rviz_for_android.drawable.loader.ColladaLoader;
import org.ros.android.view.visualization.Vertices;
import org.ros.android.view.visualization.shape.BaseShape;

public class ColladaGeometry {	
	public static enum GEOTYPE {
		triangles, tristrips, trifans
	};

	private List<ShortBuffer> indices = new ArrayList<ShortBuffer>();
	private List<GEOTYPE> type = new ArrayList<GEOTYPE>();
	private int parts = 0;

	private FloatBuffer normals;
	private FloatBuffer vertices;
	
	public ColladaGeometry(float[] normals, float[] vertices) {
		this.normals = Vertices.toFloatBuffer(normals);
		this.vertices = Vertices.toFloatBuffer(vertices);
	}

	public ShortBuffer getIndices(int i) {
		return indices.get(i);
	}

	public FloatBuffer getNormals() {
		return normals;
	}

	public FloatBuffer getVertices() {
		return vertices;
	}

	public GEOTYPE getType(int i) {
		return type.get(i);
	}

	public void addPart(short[] indices, String type) {
		this.indices.add(Vertices.toShortBuffer(indices));
		this.type.add(GEOTYPE.valueOf(type));
		parts ++;
	}
	
	public void draw(GL10 gl) {	
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertices);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glNormalPointer(GL10.GL_FLOAT, 0, normals);
		gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);

		for(int i = 0; i < parts; i++) {
			drawPart(gl, i);
		}
		
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);		
	}

	private void drawPart(GL10 gl, int i) {
		switch(type.get(i)) {
		case triangles:
			gl.glDrawElements(GL10.GL_TRIANGLES, indices.get(i).limit(), GL10.GL_UNSIGNED_SHORT, indices.get(i));
			break;
		case trifans:
			gl.glDrawElements(GL10.GL_TRIANGLE_FAN, indices.get(i).limit(), GL10.GL_UNSIGNED_SHORT, indices.get(i));
			break;
		case tristrips:
			gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, indices.get(i).limit(), GL10.GL_UNSIGNED_SHORT, indices.get(i));
		}
	}
}
