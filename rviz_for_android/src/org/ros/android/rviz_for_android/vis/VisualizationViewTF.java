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

import java.util.Collection;
import java.util.List;

import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.OrbitCamera;
import org.ros.android.view.visualization.RenderRequestListener;
import org.ros.android.view.visualization.VisViewRenderer;
import org.ros.android.view.visualization.layer.Layer;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;
import org.ros.rosjava.tf.StampedTransform;
import org.ros.rosjava.tf.TransformFactory;
import org.ros.rosjava.tf.TransformTree;
import org.ros.rosjava.tf.pubsub.TransformListener;
import org.ros.rosjava_geometry.FrameTransformTree;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class VisualizationViewTF extends GLSurfaceView implements NodeMain {

	private RenderRequestListener renderRequestListener;

	private TransformTree transformTree;

	private OrbitCameraTF camera;
	private VisViewRendererTF renderer;
	private List<LayerTF> layers;
	private ConnectedNode connectedNode;

	public VisualizationViewTF(Context context) {
		super(context);
		init();
	}

	public VisualizationViewTF(Context context, AttributeSet attrs) {
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
		layers = Lists.newArrayList();
		transformTree = new TransformTree();
		camera = new OrbitCameraTF(transformTree);
		renderer = new VisViewRendererTF(transformTree, camera);
		setEGLConfigChooser(8, 8, 8, 8, 0, 0);
		getHolder().setFormat(PixelFormat.TRANSLUCENT);
		setRenderer(renderer);
	}

	@Override
	public GraphName getDefaultNodeName() {
		return new GraphName("android_honeycomb_mr2/visualization_view");
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		for(LayerTF layer : Iterables.reverse(layers)) {
			if(layer != null && layer.onTouchEvent(this, event)) {
				return true;
			}
		}
		return false;
	}

	public VisViewRendererTF getRenderer() {
		return renderer;
	}

	/**
	 * Adds a new layer at the end of the layers collection. The new layer will be drawn last, i.e. on top of all other layers.
	 * 
	 * @param layer
	 *            layer to add
	 */
	public void addLayer(LayerTF layer) {
		synchronized(layers) {
			layers.add(layer);
		}
		layer.addRenderListener(renderRequestListener);
		if(connectedNode != null) {
			layer.onStart(connectedNode, getHandler(), transformTree, camera);
		}
		requestRender();
	}

	public void removeLayer(LayerTF layer) {
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
		Subscriber<tf.tfMessage> sub = connectedNode.newSubscriber("/tf", tf.tfMessage._TYPE);
		sub.addMessageListener(new MessageListener<tf.tfMessage>() {
			@Override
			public void onNewMessage(final tf.tfMessage message) {
				// TODO: This could possibly use message.getTransforms() instead of creating a new Collection
				Collection<StampedTransform> transforms = TransformFactory.fromTfMessage(message);
				transformTree.add(transforms);
			}
		});
	}

	private void startLayers() {
		for(LayerTF layer : layers) {
			layer.onStart(connectedNode, getHandler(), transformTree, camera);
		}
		renderer.setLayers(layers);
	}

	@Override
	public void onShutdown(Node node) {
		renderer.setLayers(null);
		for(LayerTF layer : layers) {
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

	public TransformTree getTransformTree() {
		return transformTree;
	}
}