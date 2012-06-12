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
package org.ros.android.rviz_for_android.layersTF;

import org.ros.android.rviz_for_android.prop.Property;
import org.ros.android.rviz_for_android.prop.PropertyUpdateListener;
import org.ros.android.rviz_for_android.prop.TfFrameProperty;
import org.ros.android.rviz_for_android.prop.ViewProperty;
import org.ros.android.rviz_for_android.vis.LayerWithPropertiesTF;
import org.ros.android.rviz_for_android.vis.OrbitCameraTF;
import org.ros.android.rviz_for_android.vis.OrbitCameraTFControlLayer;
import org.ros.android.rviz_for_android.vis.VisualizationViewTF;
import org.ros.android.view.visualization.Camera;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.rosjava.tf.TransformTree;

import android.content.Context;
import android.os.Handler;
import android.view.MotionEvent;

public class ParentableOrbitCameraTFControlLayer extends OrbitCameraTFControlLayer implements LayerWithPropertiesTF {

	private ViewProperty prop = new ViewProperty("null", null, null);
	private OrbitCameraTF cam;
	
	public ParentableOrbitCameraTFControlLayer(Context context) {
		super(context);
		prop.addSubProperty(new TfFrameProperty("Fixed", null, null, null));
	}

	@Override
	public boolean onTouchEvent(VisualizationViewTF view, MotionEvent event) {
		return super.onTouchEvent(view, event);
	}

	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler, TransformTree transformTree, final Camera camera) {
		if(!(camera instanceof OrbitCameraTF))
			throw new IllegalArgumentException("Can not use a ParentableOrbitCameraControlLayer with a Camera that isn't a subclass of OrbitCamera");

		cam = (OrbitCameraTF) camera;
		
		TfFrameProperty subprop = prop.<TfFrameProperty>getProperty("Fixed");
		subprop.setDefaultItem(camera.getFixedFrame().toString(), false);
		subprop.setValue(cam.getFixedFrameString());
		subprop.setTransformTree(transformTree);
		subprop.addUpdateListener(new PropertyUpdateListener<String>() {
			public void onPropertyChanged(String newval) {
				if(newval == null)
					cam.resetFixedFrame();
				else
					cam.setFixedFrameString(newval);
			}
		});

		super.onStart(connectedNode, handler, transformTree, camera);
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