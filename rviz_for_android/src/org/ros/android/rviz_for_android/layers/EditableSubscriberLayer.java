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

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.VisualizationView;
import org.ros.android.renderer.layer.SubscriberLayer;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Subscriber;
import org.ros.rosjava_geometry.FrameTransformTree;

import android.os.Handler;

public abstract class EditableSubscriberLayer<T extends org.ros.internal.message.Message> extends SubscriberLayer<T> {

	private String messageType;
	protected ConnectedNode connectedNode;
	private Subscriber<T> sub;
	private final MessageListener<T> subListener = new MessageListener<T>() {
		@Override
		public void onNewMessage(T msg) {
			messageCount ++;
			onMessageReceived(msg);
		}
	};
	
	protected int messageCount = 0;
	private String topic;
	
	public EditableSubscriberLayer(GraphName topicName, String messageType, Camera cam) {
		super(topicName, messageType, cam);
		this.messageType = messageType;
	}

	protected abstract void onMessageReceived(T msg);

	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler, FrameTransformTree frameTransformTree, Camera camera) {
		super.onStart(connectedNode, handler, frameTransformTree, camera);
		sub = getSubscriber();
		sub.addMessageListener(subListener);
		this.connectedNode = connectedNode;
	}

	@Override
	public void onShutdown(VisualizationView view, Node node) {
		clearSubscriber();
		super.onShutdown(view, node);
	}
	
	protected void clearSubscriber() {
		sub.shutdown();
	}

	protected void initSubscriber(String topic) {
		this.topic = topic;
		sub = connectedNode.newSubscriber(topic, messageType);
		sub.addMessageListener(subListener);
		messageCount = 0;
	}
	
	protected void changeTopic(String topic) {
		if(!topic.equals(this.topic)) {
			clearSubscriber();
			initSubscriber(topic);
		}
	}
}
