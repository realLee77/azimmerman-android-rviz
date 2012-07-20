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

import geometry_msgs.Point32;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.VisualizationView;
import org.ros.android.renderer.layer.SubscriberLayer;
import org.ros.android.renderer.layer.TfLayer;
import org.ros.android.renderer.shapes.Color;
import org.ros.android.rviz_for_android.drawable.PCShaders;
import org.ros.android.rviz_for_android.drawable.PointCloudGL;
import org.ros.android.rviz_for_android.drawable.PCShaders.ColorMode;
import org.ros.android.rviz_for_android.prop.BoolProperty;
import org.ros.android.rviz_for_android.prop.ColorProperty;
import org.ros.android.rviz_for_android.prop.FloatProperty;
import org.ros.android.rviz_for_android.prop.LayerWithProperties;
import org.ros.android.rviz_for_android.prop.ListProperty;
import org.ros.android.rviz_for_android.prop.Property;
import org.ros.android.rviz_for_android.prop.Property.PropertyUpdateListener;
import org.ros.android.rviz_for_android.prop.StringProperty;
import org.ros.android.rviz_for_android.prop.StringProperty.StringPropertyValidator;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Subscriber;
import org.ros.rosjava_geometry.FrameTransformTree;

import sensor_msgs.PointCloud;
import android.os.Handler;

public class PointCloudLayer extends SubscriberLayer<sensor_msgs.PointCloud> implements LayerWithProperties, TfLayer {

	private BoolProperty prop;
	private int pointCount = -1;
	private GraphName frame;

	private ConnectedNode connectedNode;
	private MessageListener<PointCloud> subListener;
	private Subscriber<sensor_msgs.PointCloud> sub;

	private PointCloudGL pc;

	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler, FrameTransformTree frameTransformTree, Camera camera) {
		super.onStart(connectedNode, handler, frameTransformTree, camera);
		sub = getSubscriber();
		subListener = new MessageListener<PointCloud>() {
			@Override
			public void onNewMessage(PointCloud msg) {
				pointCount = msg.getPoints().size();
				float[] vertices = new float[pointCount * 3];
				int i = 0;
				for(Point32 p : msg.getPoints()) {
					vertices[i++] = p.getX();
					vertices[i++] = p.getY();
					vertices[i++] = p.getZ();
				}
				pc.setData(vertices, msg.getChannels());
				prop.<ListProperty> getProperty("Channels").setList(pc.getChannelNames());

				if(frame == null || !frame.equals(msg.getHeader().getFrameId()))
					frame = GraphName.of(msg.getHeader().getFrameId());
			}
		};

		sub.addMessageListener(subListener);
		this.connectedNode = connectedNode;
	}

	@Override
	public void draw(GL10 glUnused) {
		pc.draw(glUnused);
	}

	public PointCloudLayer(Camera cam, GraphName topicName, String messageType) {
		super(topicName, messageType, cam);

		pc = new PointCloudGL(cam);

		// Enabled property
		prop = new BoolProperty("Enabled", true, null);

		// Channel selection property
		final ListProperty propChannels = new ListProperty("Channels", 0, new PropertyUpdateListener<Integer>() {
			@Override
			public void onPropertyChanged(Integer newval) {
				pc.setChannelSelection(newval);
			}
		}).setList(pc.getChannelNames());
		// Topic graph name property
		StringProperty propTopic = new StringProperty("Topic", "/lots_of_points", new PropertyUpdateListener<String>() {
			@Override
			public void onPropertyChanged(String newval) {
				clearSubscriber();
				initSubscriber(newval);
			}
		});
		propTopic.setValidator(new StringPropertyValidator() {
			@Override
			public boolean isAcceptable(String newval) {
				return true;//
				// TODO: Uh oh
				//GraphName.validate(newval);
			}
		});
		// Flat color selection property
		final ColorProperty propFlatColor = new ColorProperty("Flat Color", new Color(1f, 1f, 1f, 1f), new PropertyUpdateListener<Color>() {
			@Override
			public void onPropertyChanged(Color newval) {
				pc.setColor(newval);
			}
		});
		
		// Auto ranging input properties
		final FloatProperty propMaxRange = new FloatProperty("Maximum", 1f, null).setValidRange(0f+Float.MIN_VALUE, Float.POSITIVE_INFINITY);
		final FloatProperty propMinRange = new FloatProperty("Minimum", 0f, null).setValidRange(Float.NEGATIVE_INFINITY, 1f-Float.MIN_VALUE);
		
		pc.setAutoRanging(true);
		pc.setManualRange(propMinRange.getValue(), propMaxRange.getValue());
		
		propMinRange.addUpdateListener(new PropertyUpdateListener<Float>() {
			@Override
			public void onPropertyChanged(Float newval) {
				propMaxRange.setValidRange(newval+Float.MIN_VALUE, Float.POSITIVE_INFINITY);
				pc.setManualRange(newval, propMaxRange.getValue());
			}
		});
		propMaxRange.addUpdateListener(new PropertyUpdateListener<Float>() {
			@Override
			public void onPropertyChanged(Float newval) {
				propMinRange.setValidRange(Float.NEGATIVE_INFINITY, newval-Float.MIN_VALUE);
				pc.setManualRange(propMinRange.getValue(), newval);
			}
		});
		final BoolProperty propEnableAutorange = new BoolProperty("Auto-range", true, new PropertyUpdateListener<Boolean>() {
			@Override
			public void onPropertyChanged(Boolean newval) {
				propMinRange.setVisible(!newval);
				propMaxRange.setVisible(!newval);
				pc.setAutoRanging(newval);
			}
		});
		propEnableAutorange.setVisible(false);
		
		// Color mode selection property
		ListProperty propColorMode = new ListProperty("Color Mode", 0, new PropertyUpdateListener<Integer>() {
			@Override
			public void onPropertyChanged(Integer newval) {
				pc.setColorMode(newval);
				boolean isChannel = PCShaders.ColorMode.values()[newval] == PCShaders.ColorMode.CHANNEL;
				propChannels.setVisible(isChannel);
				propEnableAutorange.setVisible(isChannel);
				if(isChannel) {
					propMinRange.setVisible(!propEnableAutorange.getValue());
					propMaxRange.setVisible(!propEnableAutorange.getValue());
				} else {
					propMinRange.setVisible(false);
					propMaxRange.setVisible(false);
				}
				propFlatColor.setVisible(PCShaders.ColorMode.values()[newval] == PCShaders.ColorMode.FLAT_COLOR);
			}
		}).setList(PCShaders.shaderNames);
		
		propChannels.setVisible(PCShaders.ColorMode.values()[propChannels.getValue()] == PCShaders.ColorMode.CHANNEL);
		propFlatColor.setVisible(PCShaders.ColorMode.values()[propChannels.getValue()] == PCShaders.ColorMode.FLAT_COLOR);
		propMinRange.setVisible(!propEnableAutorange.getValue());
		propMaxRange.setVisible(!propEnableAutorange.getValue());
		
		prop.addSubProperty(propTopic);
		prop.addSubProperty(propColorMode);
		prop.addSubProperty(propChannels);
		prop.addSubProperty(propFlatColor);
		prop.addSubProperty(propEnableAutorange);
		prop.addSubProperty(propMinRange);
		prop.addSubProperty(propMaxRange);
		
		pc.setColor(propFlatColor.getValue());
	}

	private void clearSubscriber() {
		// TODO: Uh oh
		//sub.removeMessageListener(subListener);
		sub.shutdown();
	}

	private void initSubscriber(String topic) {
		sub = connectedNode.newSubscriber(topic, sensor_msgs.PointCloud._TYPE);
		sub.addMessageListener(subListener);
	}

	@Override
	public Property<?> getProperties() {
		return prop;
	}

	@Override
	public boolean isEnabled() {
		return prop.getValue();
	}

	@Override
	public void onShutdown(VisualizationView view, Node node) {
		super.onShutdown(view, node);

		// Clear the subscriber
		// TODO: Uh oh
		//getSubscriber().removeMessageListener(subListener);
	}

	@Override
	public GraphName getFrame() {
		return frame;
	}
}
