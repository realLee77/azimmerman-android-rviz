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

import java.util.Random;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.VisualizationView;
import org.ros.android.renderer.layer.SubscriberLayer;
import org.ros.android.renderer.layer.TfLayer;
import org.ros.android.renderer.shapes.Color;
import org.ros.android.rviz_for_android.drawable.PointCloudGL;
import org.ros.android.rviz_for_android.prop.BoolProperty;
import org.ros.android.rviz_for_android.prop.ColorProperty;
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
import android.util.Log;

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
				Log.i("PCL", "Copying " + pointCount + " vertices to buffer");
				long now = System.nanoTime();
				float[] vertices = new float[pointCount * 3];
				int i = 0;
				for(Point32 p : msg.getPoints()) {
					vertices[i++] = p.getX();
					vertices[i++] = p.getY();
					vertices[i++] = p.getZ();
				}

				pc.setData(vertices);
				Log.i("PCL", "Done copying. Time: " + (System.nanoTime() - now) / 1000000000.0);

				if(frame == null || !frame.equals(msg.getHeader().getFrameId()))
					frame = new GraphName(msg.getHeader().getFrameId());
			}
		};

		sub.addMessageListener(subListener);
		this.connectedNode = connectedNode;
	}
	
	@Override
	public void draw(GL10 glUnused) {
		pc.draw(glUnused);
	}
	
	private float[] generateTestData() {
		Random rand = new Random();
		int pointCount = rand.nextInt(5000) + 5000;
		float[] retval = new float[pointCount * 3];
		
		for(int i = 0; i < retval.length; i += 3) {
			retval[i] = rand.nextFloat()*8 - 4;
			retval[i+1] = rand.nextFloat()*8 - 4;
			retval[i+2] = retval[i+2] = rand.nextFloat()*4 - 2;
		}
		
		return retval;		
	}

	public PointCloudLayer(Camera cam, GraphName topicName, String messageType) {
		super(topicName, messageType, cam);
		prop = new BoolProperty("Enabled", true, null);
		prop.addSubProperty(new StringProperty("Topic", "/lots_of_points", new PropertyUpdateListener<String>() {
			@Override
			public void onPropertyChanged(String newval) {
				clearSubscriber();
				initSubscriber(newval);
			}
		}));
		prop.<StringProperty> getProperty("Topic").setValidator(new StringPropertyValidator() {
			@Override
			public boolean isAcceptable(String newval) {
				return GraphName.validate(newval);
			}
		});
		prop.addSubProperty(new ColorProperty("Flat Color", new Color(1f,1f,1f,1f), new PropertyUpdateListener<Color>() {
			@Override
			public void onPropertyChanged(Color newval) {
				pc.setColor(newval);
			}
		}));
		
		prop.addSubProperty(new ListProperty("Color Mode", 0, new PropertyUpdateListener<Integer>() {
			@Override
			public void onPropertyChanged(Integer newval) {
				pc.setColorMode(newval);
			}
		}).setList(PointCloudGL.colorModeNames));
	
		pc = new PointCloudGL(cam);
		pc.setData(generateTestData());
		pc.setColor(prop.<ColorProperty>getProperty("Flat Color").getValue());
	}

	private void clearSubscriber() {
		sub.removeMessageListener(subListener);
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
		getSubscriber().removeMessageListener(subListener);
	}

	@Override
	public GraphName getFrame() {
		return frame;
	}
}
