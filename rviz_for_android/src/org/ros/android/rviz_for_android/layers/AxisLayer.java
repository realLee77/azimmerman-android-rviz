/*
 * Copyright (c) 2012, Willow Garage, Inc.
 * All rights reserved.
 *
 * Willow Garage licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.ros.android.rviz_for_android.layers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.rviz_for_android.prop.BoolProperty;
import org.ros.android.rviz_for_android.prop.LayerWithProperties;
import org.ros.android.rviz_for_android.prop.Property;
import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.layer.DefaultLayer;
import org.ros.node.ConnectedNode;
import org.ros.rosjava_geometry.FrameTransformTree;

import android.os.Handler;

public class AxisLayer extends DefaultLayer implements LayerWithProperties {

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
	
	private BoolProperty prop = new BoolProperty("enabled", true, null);
	
	@Override
	public void draw(GL10 gl) {		
		if(prop.getValue()) {
			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
			
			gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
			gl.glColorPointer(4, GL10.GL_FLOAT, 0, colorBuffer);
			gl.glDrawElements(GL10.GL_LINES, 18, GL10.GL_UNSIGNED_BYTE, indexBuffer);
			gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
			
			gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
		}
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

	public Property getProperties() {
		return prop;
	}	
}
