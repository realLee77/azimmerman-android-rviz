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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.layer.Layer;
import org.ros.android.renderer.layer.SelectableLayer;
import org.ros.android.renderer.layer.TfLayer;
import org.ros.android.renderer.shapes.Color;
import org.ros.namespace.GraphName;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Transform;

import android.graphics.Point;
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
		camera.loadIdentityM();
		
		GLES20.glClearColor(0f, 0f, 0f, 0f);
	}

	@Override
	public void onDrawFrame(GL10 glUnused) {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		camera.apply();
		camera.loadIdentityM();
		
		if(camera.getSelectionManager().isSelectionDraw())
			selectionDraw(glUnused);
		else
			drawLayers(glUnused);
		
		checkErrors(glUnused);
	}

	private void selectionDraw(GL10 glUnused) {
		if(layers == null) {
			return;
		}
		synchronized(layers) {
			for(Layer layer : getLayers()) {
				if(layer.isEnabled() && (layer instanceof SelectableLayer)) {
					camera.pushM();
					if(layer instanceof TfLayer) {
						GraphName layerFrame = ((TfLayer) layer).getFrame();
						if(layerFrame != null) {
							Transform t = frameTransformTree.newTransformIfPossible(layerFrame, camera.getFixedFrame());
							camera.applyTransform(t);
						}
					}
					((SelectableLayer)layer).selectionDraw(glUnused);
					camera.popM();
				}
			}
		}
		
		// is THIS your card?
		ByteBuffer colorBuf = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
		colorBuf.position(0);
		Point selected = camera.getSelectionManager().getSelectionCoordinates();
		selected.set(selected.x, (camera.getViewport().getHeight()-selected.y));
		GLES20.glReadPixels(selected.x, selected.y, 1,1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, colorBuf);
		colorBuf.position(0);		
		Color selectedColor = new Color((colorBuf.get() & 0xff)/255f, (colorBuf.get() & 0xff)/255f,(colorBuf.get() & 0xff)/255f, 1f);
		System.out.println("Selected color " + selectedColor);
		camera.getSelectionManager().selectItemWithColor(selectedColor);
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
}
