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

import org.ros.android.rviz_for_android.prop.GraphNameProperty;
import org.ros.android.rviz_for_android.prop.LayerWithProperties;
import org.ros.android.rviz_for_android.prop.Property;
import org.ros.android.rviz_for_android.prop.PropertyUpdateListener;
import org.ros.android.rviz_for_android.prop.ViewProperty;
import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.OrbitCamera;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.layer.OrbitCameraControlLayer;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.rosjava_geometry.FrameTransformTree;

import android.content.Context;
import android.os.Handler;
import android.view.MotionEvent;

public class ParentableOrbitCameraControlLayer extends OrbitCameraControlLayer implements LayerWithProperties {

	private ViewProperty prop = new ViewProperty("null", null, null);
	private OrbitCamera cam;
	
	public ParentableOrbitCameraControlLayer(Context context) {
		super(context);
		prop.addSubProperty(new GraphNameProperty("Fixed", null, null, null));
	}

	@Override
	public boolean onTouchEvent(VisualizationView view, MotionEvent event) {
		return super.onTouchEvent(view, event);
	}

	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler, FrameTransformTree frameTransformTree, final Camera camera) {
		if(!(camera instanceof OrbitCamera))
			throw new IllegalArgumentException("Can not use a ParentableOrbitCameraControlLayer with a Camera that isn't a subclass of OrbitCamera");

		cam = (OrbitCamera) camera;
		
		GraphNameProperty subprop = (GraphNameProperty) prop.getProperty("Fixed");
		subprop.setDefaultItem(camera.getFixedFrame().toString(), false);
		subprop.setValue(camera.getFixedFrame());
		subprop.setTransformTree(frameTransformTree);
		subprop.addUpdateListener(new PropertyUpdateListener<GraphName>() {
			public void onPropertyChanged(GraphName newval) {
				if(newval == null)
					camera.resetFixedFrame();
				else
					camera.setFixedFrame(newval);
			}
		});

		super.onStart(connectedNode, handler, frameTransformTree, camera);
	}
	
	public Property<?> getProperties() {
		return prop;
	}

	public void setTargetFrame(String newval) {
		if(newval == null) {
			cam.resetTargetFrame();
			super.enableScrolling = true;
		} else {
			cam.setTargetFrame(new GraphName(newval));
			super.enableScrolling = false;
		}
	}
}