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

import org.ros.android.rviz_for_android.prop.BoolProperty;
import org.ros.android.rviz_for_android.prop.LayerWithProperties;
import org.ros.android.rviz_for_android.prop.Property;
import org.ros.android.rviz_for_android.prop.PropertyUpdateListener;
import org.ros.android.rviz_for_android.prop.StringProperty;
import org.ros.android.rviz_for_android.urdf.UrdfReader;
import org.ros.android.view.visualization.layer.DefaultLayer;
import org.ros.android.view.visualization.layer.TfLayer;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;

public class RobotModel extends DefaultLayer implements LayerWithProperties, TfLayer {

	private BoolProperty prop = new BoolProperty("Enabled", true, null); 
	private UrdfReader reader;
	
	public RobotModel(final ConnectedNode node) {
		reader = new UrdfReader();
		prop.addSubProperty(new StringProperty("Parameter", "/robot_description", new PropertyUpdateListener<String>() {
			@Override
			public void onPropertyChanged(String newval) {
				reader.readUrdf(node.getParameterTree().getString(newval));
			}
		}));
		reader.readUrdf(node.getParameterTree().getString("/robot_description"));
	}
	
	@Override
	public Property<?> getProperties() {
		return prop;
	}

	@Override
	public GraphName getFrame() {
		return null;
	}

}
