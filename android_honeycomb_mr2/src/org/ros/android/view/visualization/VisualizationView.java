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

package org.ros.android.view.visualization;

import java.util.List;

import org.ros.android.view.visualization.layer.Layer;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;
import org.ros.rosjava_geometry.FrameTransformTree;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class VisualizationView extends GLSurfaceView implements NodeMain {

	private RenderRequestListener renderRequestListener;
	
	private FrameTransformTree frameTransformTree;
	
	private Camera camera;
	private VisViewRenderer renderer;
	private List<Layer> layers;
	private ConnectedNode connectedNode;

	public VisualizationView(Context context) {
		super(context);
		init();
	}

	public VisualizationView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		renderRequestListener = new RenderRequestListener() {
			@Override
			public void onRenderRequest() {
				requestRender();
			}
		};
		frameTransformTree = new FrameTransformTree();
		camera = new OrbitCamera(frameTransformTree);
		renderer = new VisViewRenderer(frameTransformTree, camera);
		layers = Lists.newArrayList();
		setEGLConfigChooser(8, 8, 8, 8, 8, 8);
		// TODO: Support ES2:
//		setEGLContextClientVersion(2);
		getHolder().setFormat(PixelFormat.TRANSLUCENT);
		setRenderer(renderer);
	}

	@Override
	public GraphName getDefaultNodeName() {
		return new GraphName("android_honeycomb_mr2/visualization_view");
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		for(Layer layer : Iterables.reverse(layers)) {
			if(layer != null && layer.onTouchEvent(this, event)) {
				return true;
			}
		}
		return false;
	}

	public VisViewRenderer getRenderer() {
		return renderer;
	}

	/**
	 * Adds a new layer at the end of the layers collection. The new layer will be drawn last, i.e. on top of all other layers.
	 * 
	 * @param layer
	 *            layer to add
	 */
	public void addLayer(Layer layer) {
		synchronized(layers) {
			layers.add(layer);
		}
		layer.addRenderListener(renderRequestListener);
		if(connectedNode != null) {
			layer.onStart(connectedNode, getHandler(), frameTransformTree, camera);
		}
		requestRender();
	}

	public void removeLayer(Layer layer) {
		layer.onShutdown(this, connectedNode);
		synchronized(layers) {
			layers.remove(layer);
		}
	}

	@Override
	public void onStart(ConnectedNode connectedNode) {
		this.connectedNode = connectedNode;
		startTransformListener();
		startLayers();
	}

	private void startTransformListener() {
		String tfPrefix = connectedNode.getParameterTree().getString("~tf_prefix", "");
		if(!tfPrefix.isEmpty()) {
			frameTransformTree.setPrefix(tfPrefix);
		}
		Subscriber<tf.tfMessage> tfSubscriber = connectedNode.newSubscriber("tf", tf.tfMessage._TYPE);
		tfSubscriber.addMessageListener(new MessageListener<tf.tfMessage>() {
			@Override
			public void onNewMessage(tf.tfMessage message) {
				for(geometry_msgs.TransformStamped transform : message.getTransforms()) {
					frameTransformTree.updateTransform(transform);
				}
			}
		});
	}

	private void startLayers() {
		for(Layer layer : layers) {
			layer.onStart(connectedNode, getHandler(), frameTransformTree, camera);
		}
		renderer.setLayers(layers);
	}

	@Override
	public void onShutdown(Node node) {
		renderer.setLayers(null);
		for(Layer layer : layers) {
			layer.onShutdown(this, node);
		}
		this.connectedNode = null;
	}

	@Override
	public void onShutdownComplete(Node node) {
	}

	@Override
	public void onError(Node node, Throwable throwable) {
	}
	
	public FrameTransformTree getFrameTransformTree() {
		return frameTransformTree;
	}
}
