package org.ros.android.rviz_for_android.layers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.layer.DefaultLayer;
import org.ros.node.ConnectedNode;
import org.ros.rosjava_geometry.FrameTransformTree;

import android.os.Handler;

public class AxisLayer extends DefaultLayer {

	private static final float VERTICES[] = {
		0,0,0,
		0,0,1,
		0,.25f,.75f,
		0,-.25f,.75f,
		
		0,0,0,
		0,1,0,
		.25f,.75f,0,
		-.25f,.75f,0,
		
		0,0,0,
		1,0,0,
		.75f,.25f,0,
		.75f,-.25f,0
	};
	
	private static final float COLORS[] = {
		1,0,0,1,
		1,0,0,1,
		1,0,0,1,
		1,0,0,1,
		
		0,1,0,1,
		0,1,0,1,
		0,1,0,1,
		0,1,0,1,
		
		0,0,1,1,
		0,0,1,1,
		0,0,1,1,
		0,0,1,1
	};
	
	private static final byte INDEX[] = {
		0,1,
		1,2,
		1,3,
		
		4,5,
		5,6,
		5,7,
		
		8,9,
		9,10,
		9,11
	};
	
	private FloatBuffer vertexBuffer;
	private FloatBuffer colorBuffer;
	private ByteBuffer indexBuffer;
	
	@Override
	public void draw(GL10 gl) {		
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
		
		gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
		gl.glColorPointer(4, GL10.GL_FLOAT, 0, colorBuffer);
		gl.glDrawElements(GL10.GL_LINES, 18, GL10.GL_UNSIGNED_BYTE, indexBuffer);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		
		gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
	}

	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler, FrameTransformTree frameTransformTree, Camera camera) {
		ByteBuffer vbb = ByteBuffer.allocateDirect(VERTICES.length * 4);
		vbb.order(ByteOrder.nativeOrder());
		vertexBuffer = vbb.asFloatBuffer();
		vertexBuffer.put(VERTICES);
		vertexBuffer.position(0);
		
		vbb = ByteBuffer.allocateDirect(COLORS.length * 4);
		vbb.order(ByteOrder.nativeOrder());
		colorBuffer = vbb.asFloatBuffer();
		colorBuffer.put(COLORS);
		colorBuffer.position(0);

		indexBuffer = ByteBuffer.allocateDirect(INDEX.length);
		indexBuffer.put(INDEX);
		indexBuffer.position(0);
	}	
}
