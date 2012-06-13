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
package org.ros.android.rviz_for_android.vis;

import java.util.LinkedList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.vecmath.Quat4d;

import org.ros.android.view.visualization.Viewport;
import org.ros.android.view.visualization.layer.TfLayer;
import org.ros.rosjava.tf.Transform;
import org.ros.rosjava.tf.TransformTree;

import android.opengl.GLSurfaceView;
import android.util.Log;

public class VisViewRendererTF implements GLSurfaceView.Renderer {
	/**
	 * List of layers to draw. Layers are drawn in-order, i.e. the layer with index 0 is the bottom layer and is drawn first.
	 */
	private List<LayerTF> layers;

	private List<LayerTF> toAdd = new LinkedList<LayerTF>();

	private TransformTree transformTree;

	private OrbitCameraTF camera;

	public VisViewRendererTF(TransformTree transformTree, OrbitCameraTF camera) {
		this.transformTree = transformTree;
		this.camera = camera;
	}

	private void drawLayers(GL10 gl) {
		if(layers == null) {
			return;
		}
		synchronized(layers) {
			for(LayerTF layer : getLayers()) {
				if(layer.isEnabled()) {
					gl.glPushMatrix();
					if(layer instanceof TfLayerTF) {
						String layerFrame = ((TfLayerTF) layer).getFrame();
						String camFrame = camera.getFixedFrameString();
						
						if(layerFrame != null &&  transformTree.canTransform(camFrame, layerFrame)) {
							Transform transform = transformTree.lookupMostRecent(camFrame, layerFrame);
							
							if(transform != null) {
								applyTransformation(transform, gl);
							} else {
								Log.e("TF", "Transform is null in the renderer!");
							}
						}
					}
					layer.draw(gl);
					gl.glPopMatrix();
				}
			}
		}
	}
	
	private void applyTransformation(Transform transform, GL10 gl) {
		gl.glTranslatef((float)transform.getTranslation().x , (float)transform.getTranslation().y, (float)transform.getTranslation().z);
		
		Quat4d quat = transform.getRotation();
		float scale = (float)Math.sqrt(quat.x*quat.x + quat.y*quat.y + quat.z*quat.z);
		gl.glRotatef((float)Math.toDegrees(2*Math.acos(quat.w)), (float)(quat.x / scale), (float)(quat.y / scale), (float)(quat.z / scale));
	}
	
	private String fromGlobal(String frame) {
		if(frame.startsWith("/"))
			return frame.substring(1);
		return frame;
	}

	public List<LayerTF> getLayers() {
		return layers;
	}

	public void setLayers(List<LayerTF> layers) {
		this.layers = layers;
	}

	public void addLayer(LayerTF layer) {
		toAdd.add(layer);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		// Set the viewport
		Viewport viewport = new Viewport(width, height);
		viewport.apply(gl);
		camera.setViewport(viewport);
		// Set camera location transformation
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
		// Set texture rendering hints
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL10.GL_POINT_SMOOTH);
		gl.glHint(GL10.GL_POINT_SMOOTH_HINT, GL10.GL_NICEST);
		gl.glHint(GL10.GL_POLYGON_SMOOTH_HINT, GL10.GL_NICEST);
		gl.glDisable(GL10.GL_LIGHTING);
		gl.glDisable(GL10.GL_DEPTH_TEST);
		gl.glEnable(GL10.GL_COLOR_MATERIAL);
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		gl.glLoadIdentity();
		camera.apply(gl);
		drawLayers(gl);
		int error = gl.glGetError();
		if(error != GL10.GL_NO_ERROR) {
			System.err.println("OpenGL error: " + error);
		}
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
	}

}
