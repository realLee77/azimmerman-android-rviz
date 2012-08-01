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

import java.util.HashSet;
import java.util.Set;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.VisualizationView;
import org.ros.android.renderer.layer.DefaultLayer;
import org.ros.android.rviz_for_android.drawable.InteractiveMarker;
import org.ros.android.rviz_for_android.layers.InteractiveMarkerSubscriptionManager.InteractiveMarkerCallback;
import org.ros.android.rviz_for_android.prop.BoolProperty;
import org.ros.android.rviz_for_android.prop.LayerWithProperties;
import org.ros.android.rviz_for_android.prop.Property;
import org.ros.android.rviz_for_android.prop.Property.PropertyUpdateListener;
import org.ros.android.rviz_for_android.prop.StringProperty;
import org.ros.android.rviz_for_android.urdf.MeshFileDownloader;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.rosjava_geometry.FrameTransformTree;

import visualization_msgs.InteractiveMarkerInit;
import visualization_msgs.InteractiveMarkerUpdate;
import android.os.Handler;

public class InteractiveMarkerLayer extends DefaultLayer implements LayerWithProperties {

	private InteractiveMarkerSubscriptionManager subscriber;

	private Set<InteractiveMarker> markers = new HashSet<InteractiveMarker>();
	
	private final Object lockObject = new Object();
	
	// Layer properties
	private final BoolProperty prop = new BoolProperty("Enabled", true, null);
	private final StringProperty propTopic = new StringProperty("Topic", "/basic_controls", new PropertyUpdateListener<String>() {
		@Override
		public void onPropertyChanged(String newval) {
			subscriber.setTopic(newval);
		}
	});
	
	private FrameTransformTree ftt;

	public InteractiveMarkerLayer(Camera cam, final MeshFileDownloader mfd) {
		super(cam);
		subscriber = new InteractiveMarkerSubscriptionManager("/basic_controls", cam, new InteractiveMarkerCallback() {
			@Override
			public void receiveUpdate(InteractiveMarkerUpdate msg) {
				// TODO Auto-generated method stub
			}
			
			@Override
			public void receiveInit(InteractiveMarkerInit msg) {
				synchronized(lockObject) {
					markers.clear();
					for(visualization_msgs.InteractiveMarker im : msg.getMarkers())
						markers.add(new InteractiveMarker(im, camera, mfd, ftt));
				}
			}

			@Override
			public void clear() {
				synchronized(lockObject) {
					markers.clear();
				}
			}
		});
		prop.addSubProperty(propTopic);
	}

	@Override
	public void draw(GL10 glUnused) {
		synchronized(lockObject) {
			for(InteractiveMarker marker : markers)
				marker.draw(glUnused);
		}
	}

	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler, FrameTransformTree frameTransformTree, Camera camera) {
		super.onStart(connectedNode, handler, frameTransformTree, camera);
		ftt = frameTransformTree;
		subscriber.onStart(connectedNode, handler, frameTransformTree, camera);
	}

	@Override
	public void onShutdown(VisualizationView view, Node node) {
		super.onShutdown(view, node);
		subscriber.onShutdown(view, node);
	}

	@Override
	public boolean isEnabled() {
		return prop.getValue();
	}

	@Override
	public Property<?> getProperties() {
		return prop;
	}

}
