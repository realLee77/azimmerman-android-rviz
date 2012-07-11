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
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.Vertices;
import org.ros.android.renderer.layer.DefaultLayer;
import org.ros.android.renderer.layer.TfLayer;
import org.ros.android.rviz_for_android.drawable.GLSLProgram;
import org.ros.android.rviz_for_android.drawable.GLSLProgram.ShaderVal;
import org.ros.android.rviz_for_android.prop.BoolProperty;
import org.ros.android.rviz_for_android.prop.FloatProperty;
import org.ros.android.rviz_for_android.prop.GraphNameProperty;
import org.ros.android.rviz_for_android.prop.LayerWithProperties;
import org.ros.android.rviz_for_android.prop.Property;
import org.ros.android.rviz_for_android.prop.Property.PropertyUpdateListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.rosjava_geometry.FrameTransformTree;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;

public class AxisLayer extends DefaultLayer implements LayerWithProperties, TfLayer {

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
		0,0,1,1,
		0,0,1,1,
		0,0,1,1,
		0,0,1,1,
		
		0,1,0,1,
		0,1,0,1,
		0,1,0,1,
		0,1,0,1,
		
		1,0,0,1,
		1,0,0,1,
		1,0,0,1,
		1,0,0,1,
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
	
	private GLSLProgram axisShader = GLSLProgram.ColoredVertex();
	
	private FloatBuffer vertexBuffer;
	private FloatBuffer colorBuffer;
	private ByteBuffer indexBuffer;
	
	private BoolProperty prop;
	
	private int[] uniformHandles;
	private float[] MVP = new float[16];
	private float scale = 1f;
	
	private void calcMVP() {
		Matrix.multiplyMM(MVP, 0, camera.getViewMatrix(), 0, camera.getModelMatrix(), 0);
		Matrix.multiplyMM(MVP, 0, camera.getViewport().getProjectionMatrix(), 0, MVP, 0);
	}
	
	public AxisLayer(Camera cam) {
		super(cam);
		prop = new BoolProperty("enabled", true, null);
		prop.addSubProperty(new FloatProperty("Scale", scale, new PropertyUpdateListener<Float>() {
			@Override
			public void onPropertyChanged(Float newval) {
				scale = newval;
			}
		}).setValidRange(0.001f, 10000f));
		prop.addSubProperty(new GraphNameProperty("Parent", null, null, null));
		uniformHandles = axisShader.getUniformHandles();
	}
	
	@Override
	public void draw(GL10 glUnused) {		
		if(!axisShader.isCompiled()) {
			axisShader.compile(glUnused);
			uniformHandles = axisShader.getUniformHandles();
		}
		
		axisShader.use(glUnused);
		
		GLES20.glEnableVertexAttribArray(ShaderVal.POSITION.loc);
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
		
		GLES20.glEnableVertexAttribArray(ShaderVal.ATTRIB_COLOR.loc);
		GLES20.glVertexAttribPointer(ShaderVal.ATTRIB_COLOR.loc, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);
		
		camera.pushM();
		camera.scaleM(scale, scale, scale);
		calcMVP();
		GLES20.glUniformMatrix4fv(uniformHandles[ShaderVal.MVP_MATRIX.loc], 1, false, MVP, 0);
		GLES20.glDrawElements(GLES20.GL_LINES, 18, GLES20.GL_UNSIGNED_BYTE, indexBuffer);
		camera.popM();
	}

	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler, final FrameTransformTree frameTransformTree, final Camera camera) {
		(prop.<GraphNameProperty>getProperty("Parent")).setTransformTree(frameTransformTree);
		
		vertexBuffer = Vertices.toFloatBuffer(VERTICES);
		colorBuffer = Vertices.toFloatBuffer(COLORS);
		indexBuffer = Vertices.toByteBuffer(INDEX);
	}

	public Property<?> getProperties() {
		return prop;
	}

	public GraphName getFrame() {
		return prop.<GraphNameProperty>getProperty("Parent").getValue();
	}

	@Override
	public boolean isEnabled() {
		return prop.getValue();
	}	
}
