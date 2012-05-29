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

package org.ros.android.rviz_for_android;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.layer.OrbitCameraControlLayer;
import org.ros.android.view.visualization.layer.OrthogonalCameraControlLayer;
import org.ros.android.view.visualization.layer.RobotLayer;
import org.ros.namespace.GraphName;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * An app that can be used to control a remote robot. This app also demonstrates how to use some of views from the rosjava android library.
 * 
 * @author munjaldesai@google.com (Munjal Desai)
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class MainActivity extends RosActivity {

	private VisualizationView visualizationView;
	private static Context context;

	public MainActivity() {
		super("Rviz", "Rviz");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.settings_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		MainActivity.context = getApplicationContext();
		visualizationView = (VisualizationView) findViewById(R.id.visualization);
		visualizationView.addLayer(new OrbitCameraControlLayer(this));
		visualizationView.addLayer(new GridLayer(10, 10, 0.5f, 0.5f));
		visualizationView.addLayer(new RobotLayer("base_footprint", this));
		visualizationView.addLayer(new TextLayer(new GraphName("test/stuff"), std_msgs.String._TYPE));
		visualizationView.addLayer(new AxisLayer());
	}

	public static Context getAppContext() {
		return MainActivity.context;
	}

	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {
		NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(), getMasterUri());
		// nodeMainExecutor.execute(virtualJoystickView, nodeConfiguration.setNodeName("virtual_joystick"));
		nodeMainExecutor.execute(visualizationView, nodeConfiguration.setNodeName("android/map_view"));
	}
}
