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

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.layer.SubscriberLayer;
import org.ros.android.renderer.layer.TfLayer;
import org.ros.android.rviz_for_android.drawable.PointCloud2GL;
import org.ros.android.rviz_for_android.prop.BoolProperty;
import org.ros.android.rviz_for_android.prop.ButtonProperty;
import org.ros.android.rviz_for_android.prop.ColorProperty;
import org.ros.android.rviz_for_android.prop.FloatProperty;
import org.ros.android.rviz_for_android.prop.FrameCheckStatusPropertyController;
import org.ros.android.rviz_for_android.prop.LayerWithProperties;
import org.ros.android.rviz_for_android.prop.ListProperty;
import org.ros.android.rviz_for_android.prop.Property;
import org.ros.android.rviz_for_android.prop.ReadOnlyProperty;
import org.ros.android.rviz_for_android.prop.Property.PropertyUpdateListener;
import org.ros.android.rviz_for_android.prop.ReadOnlyProperty.StatusColor;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;
import org.ros.android.renderer.shapes.Color;
import org.ros.rosjava_geometry.FrameTransformTree;

import sensor_msgs.PointCloud2;
import android.content.Context;
import android.os.Handler;

public class PointCloud2Layer extends SubscriberLayer<sensor_msgs.PointCloud2> implements TfLayer, LayerWithProperties {
	private static final String[] COLOR_MODES = new String[]{"Flat Color", "Channel"};
	
	private PointCloud2GL pc;

	private GraphName frame;
	
	private ConnectedNode connectedNode;
	private MessageListener<PointCloud2> subListener;
	private Subscriber<PointCloud2> sub;
	
	private BoolProperty prop;
	private ReadOnlyProperty propStatus;
	private FrameCheckStatusPropertyController statusController;
	private final ListProperty propChannelSelect;
	
	public PointCloud2Layer(GraphName topicName, Camera cam, Context context) {
		super(topicName, sensor_msgs.PointCloud2._TYPE, cam);
		
		pc = new PointCloud2GL(cam, context);
		
		prop = new BoolProperty("Enabled", true, null);
		final ListProperty propColorMode = new ListProperty("Color Mode", 0, null).setList(COLOR_MODES);
		propChannelSelect = new ListProperty("Channel", 0, new PropertyUpdateListener<Integer>() {
			@Override
			public void onPropertyChanged(Integer newval) {
				if(pc != null)
					pc.setChannelColorMode(newval);
			}
		});
		final ColorProperty propColorSelect = new ColorProperty("Flat Color", pc.getColor(), new PropertyUpdateListener<Color>() {
			@Override
			public void onPropertyChanged(Color newval) {
				if(pc != null)
					pc.setFlatColorMode(newval);
			}
		});
		
		final FloatProperty propMinRange = new FloatProperty("Min", 0f, null);
		final FloatProperty propMaxRange = new FloatProperty("Max", 1f, null);
		
		final ButtonProperty propCalcRange = new ButtonProperty("Compute Range", "Compute", new PropertyUpdateListener<String>() {
			@Override
			public void onPropertyChanged(String newval) {
				if(pc != null) {
					float[] range = pc.computeRange();
					propMinRange.setValue(range[0]);
					propMaxRange.setValue(range[1]);
				}
			}
		});
		
		propMaxRange.addUpdateListener(new PropertyUpdateListener<Float>() {
			@Override
			public void onPropertyChanged(Float newval) {
				propMinRange.setValidRange(Float.NEGATIVE_INFINITY, newval - Float.MIN_NORMAL);
				if(pc != null)
					pc.setRange(propMinRange.getValue(), newval);
			}
		});		
		
		propMinRange.addUpdateListener(new PropertyUpdateListener<Float>() {
			@Override
			public void onPropertyChanged(Float newval) {
				propMinRange.setValidRange(newval + Float.MIN_NORMAL, Float.POSITIVE_INFINITY);
				if(pc != null)
					pc.setRange(newval, propMaxRange.getValue());
			}
		});
		
		propColorMode.addUpdateListener(new PropertyUpdateListener<Integer>() {
			@Override
			public void onPropertyChanged(Integer newval) {
				boolean isChannelColor = (newval == 1);
				propColorSelect.setVisible(!isChannelColor);
				if(!isChannelColor && pc != null) {
					pc.setFlatColorMode(propColorSelect.getValue());
				} else {
					propChannelSelect.setValue(0);
					pc.setChannelColorMode(0);
				}
				propCalcRange.setVisible(isChannelColor);
				propChannelSelect.setVisible(isChannelColor);
				propMinRange.setVisible(isChannelColor);
				propMaxRange.setVisible(isChannelColor);
			}
		});		
		
		propStatus = new ReadOnlyProperty("Status", "OK", null);
		
		prop.addSubProperty(propStatus);
		prop.addSubProperty(propColorMode);
		prop.addSubProperty(propChannelSelect);
		prop.addSubProperty(propColorSelect);
		prop.addSubProperty(propCalcRange);
		prop.addSubProperty(propMinRange);
		prop.addSubProperty(propMaxRange);
		
		// Set the initial visibilities
		boolean isChannelColor = false;
		propColorSelect.setVisible(!isChannelColor);
		propCalcRange.setVisible(isChannelColor);
		propChannelSelect.setVisible(isChannelColor);
		propMinRange.setVisible(isChannelColor);
		propMaxRange.setVisible(isChannelColor);
	}
	
	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler, FrameTransformTree frameTransformTree, Camera camera) {
		super.onStart(connectedNode, handler, frameTransformTree, camera);
		
		statusController = new FrameCheckStatusPropertyController(propStatus, camera, frameTransformTree);	
		statusController.setFrameChecking(false);
		statusController.setStatus("No PointCloud2 messages received", StatusColor.WARN);
		
		sub = getSubscriber();
		subListener = new MessageListener<PointCloud2>() {
			@Override
			public void onNewMessage(PointCloud2 msg) {
				pc.setData(msg);
				propChannelSelect.setList(pc.getChannelNames());
				
				if(frame == null || !frame.equals(msg.getHeader().getFrameId())) {
					frame = GraphName.of(msg.getHeader().getFrameId());					
					statusController.setTargetFrame(frame);
					statusController.setFrameChecking(true); // TODO: Optimize with message count?
				}
			}
		};

		sub.addMessageListener(subListener);
		this.connectedNode = connectedNode;
	}
	
	@Override
	public void draw(GL10 glUnused) {
		super.draw(glUnused);
		pc.draw(glUnused);
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
	public boolean isEnabled() {
		return prop.getValue();
	}
	@Override
	public GraphName getFrame() {
		return frame;
	}

	@Override
	public Property<?> getProperties() {
		return prop;
	}
}
