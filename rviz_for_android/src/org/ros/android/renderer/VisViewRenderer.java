/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.android.renderer;

import java.util.LinkedList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.layer.Layer;
import org.ros.android.renderer.layer.TfLayer;
import org.ros.namespace.GraphName;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Transform;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

/**
 * Renders all layers of a navigation view.
 * 
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class VisViewRenderer implements GLSurfaceView.Renderer {
	/**
	 * List of layers to draw. Layers are drawn in-order, i.e. the layer with index 0 is the bottom layer and is drawn first.
	 */
	private List<Layer> layers;

	private List<Layer> toAdd = new LinkedList<Layer>();

	private FrameTransformTree frameTransformTree;

	private Camera camera;
	
	public VisViewRenderer(FrameTransformTree frameTransformTree, Camera camera) {
		this.frameTransformTree = frameTransformTree;
		this.camera = camera;
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		// Set the viewport
		Viewport viewport = new Viewport(width, height);
		viewport.apply(gl);
		camera.setViewport(viewport);
		
		// Set camera location transformation
//		gl.glMatrixMode(GL10.GL_MODELVIEW);
//		gl.glLoadIdentity();
		camera.loadIdentityM();
	}

	@Override
	public void onDrawFrame(GL10 glUnused) {	    
/*		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();
		camera.apply(gl);
		
		initLighting(gl);
		
		drawLayers(gl);
		int error = gl.glGetError();
		if(error != GL10.GL_NO_ERROR) {
			System.err.println("OpenGL error: " + error);
		}*/
		
		GLES20.glClearColor(0f, 0f, 0f, 0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		camera.apply();
		camera.loadIdentityM();
		
		drawLayers(glUnused);
		
		checkErrors(glUnused);
	}

	private void checkErrors(GL10 glUnused) {
		int error = GLES20.glGetError();
		if(error != GLES20.GL_NO_ERROR) {
			String err;
			switch(error) {
			case GLES20.GL_INVALID_ENUM:
				err = "Invalid enum";
				break;
			case GLES20.GL_INVALID_FRAMEBUFFER_OPERATION:
				err = "Invalid framebuffer operation";
				break;
			case GLES20.GL_INVALID_OPERATION:
				err = "Invalid operation";
				break;
			case GLES20.GL_INVALID_VALUE:
				err = "Invalid value";
				break;
			case GLES20.GL_OUT_OF_MEMORY:
				err = "Out of memory";
			default:
				err = "Unknown error " + error;
			}
			
			System.err.println("OpenGL error: " + err); 
		}
	}

	public static final int SUNLIGHT = GL10.GL_LIGHT0;
	
	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
		// Set rendering options 
		// TODO: Are these needed at all???
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		GLES20.glDisable(GLES20.GL_DITHER);
		
        // Face culling
		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glFrontFace(GL10.GL_CCW);
		GLES20.glCullFace(GLES20.GL_BACK);
		
		// Depth
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glDepthMask(true);
	}

	private void drawLayers(GL10 glUnused) {
		if(layers == null) {
			return;
		}
		synchronized(layers) {
			for(Layer layer : getLayers()) {
				if(layer.isEnabled()) {
					camera.pushM();
					if(layer instanceof TfLayer) {
						GraphName layerFrame = ((TfLayer) layer).getFrame();
						if(layerFrame != null) {
							Transform t = frameTransformTree.newTransformIfPossible(layerFrame, camera.getFixedFrame());
							camera.applyTransform(t);
						}
					}
					layer.draw(glUnused);
					camera.popM();
				}
			}
		}
	}

	public List<Layer> getLayers() {
		return layers;
	}

	public void setLayers(List<Layer> layers) {
		this.layers = layers;
	}

	public void addLayer(Layer layer) {
		toAdd.add(layer);
	}
}
