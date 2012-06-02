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

package org.ros.android.rviz_for_android;

import java.util.ArrayList;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.android.rviz_for_android.layers.AxisLayer;
import org.ros.android.rviz_for_android.layers.GridLayer;
import org.ros.android.rviz_for_android.layers.TextLayer;
import org.ros.android.rviz_for_android.prop.LayerWithProperties;
import org.ros.android.rviz_for_android.prop.PropertyListAdapter;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.layer.DefaultLayer;
import org.ros.android.view.visualization.layer.Layer;
import org.ros.android.view.visualization.layer.OrbitCameraControlLayer;
import org.ros.android.view.visualization.layer.RobotLayer;
import org.ros.namespace.GraphName;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * An app that can be used to control a remote robot. This app also demonstrates how to use some of views from the rosjava android library.
 * 
 * @author munjaldesai@google.com (Munjal Desai)
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class MainActivity extends RosActivity {

	private VisualizationView visualizationView;
	private static Context context;

	// GUI elements
	private ExpandableListView elv;
	private ArrayList<LayerWithProperties> layers = new ArrayList<LayerWithProperties>();
	private PropertyListAdapter propAdapter;

	// Default layers
	private TextLayer tl = new TextLayer(new GraphName("test/stuff"), std_msgs.String._TYPE);
	private GridLayer gl = new GridLayer(10, 10, 0.5f, 0.5f);
	private AxisLayer al = new AxisLayer();

	// Tracking layers
	private CharSequence[] liveLayers;
	private CharSequence[] availableLayers = { "Axis", "Grid", "Text" };
	private int[] counts;
	
	// Adding and removing layers
	private static AlertDialog.Builder addLayerDialogBuilder;
	private static AlertDialog.Builder remLayerDialogBuilder;
	private static AlertDialog addLayerDialog;
	private static AlertDialog remLayerDialog;
	private Button addLayer;
	private Button remLayer;
	private Button nameLayer;
	
	// Show and hide the layer selection panel
	private LinearLayout ll;
	private boolean showLayers = false;
	
	public MainActivity() {
		super("Rviz", "Rviz");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.settings_menu, menu);
		
		// Configure the action bar
		ActionBar ab = getActionBar();
		ab.setDisplayShowHomeEnabled(false);
		ab.setDisplayShowTitleEnabled(false);
		ab.setDisplayShowCustomEnabled(true);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.id.menu_layertoggle) {
			if(showLayers) {
				ll.setVisibility(LinearLayout.GONE);
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(ll.getWindowToken(), 0);
			} else {
				ll.setVisibility(LinearLayout.VISIBLE);				
			}
			showLayers = !showLayers;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		MainActivity.context = this;

		createLayerDialogs();
		configureGUI();
		
		tl.setName("Text");
		layers.add(tl);
		gl.setName("Grid");
		layers.add(gl);
		al.setName("Axis");
		layers.add(al);
		
		ll = ((LinearLayout) findViewById(R.id.layer_layout));
		ll.setVisibility(LinearLayout.GONE);
		
		visualizationView = (VisualizationView) findViewById(R.id.visualization);
		visualizationView.addLayer(new OrbitCameraControlLayer(this));
		visualizationView.addLayer(new RobotLayer("base_footprint", this));
		visualizationView.addLayer(gl);
		visualizationView.addLayer(tl);
		visualizationView.addLayer(al);

		elv = (ExpandableListView) findViewById(R.id.expandableListView1);
		propAdapter = new PropertyListAdapter(layers, getApplicationContext());
		elv.setAdapter(propAdapter);
		elv.setItemsCanFocus(true);
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
	
	private void addNewLayer(int layertype) {
		DefaultLayer newLayer = null;
		switch(layertype) {
		case 0:
			newLayer = new AxisLayer();
			break;
		case 1:
			newLayer = new GridLayer(10, 10, 1, 1);
			break;
		case 2:
			newLayer = new TextLayer(new GraphName("test/stuff"), std_msgs.String._TYPE);
			break;
		}

		if(newLayer != null) {
			newLayer.setName(availableLayers[layertype] + " " + counts[layertype]++);
			if(newLayer instanceof LayerWithProperties) {
				layers.add((LayerWithProperties) newLayer);
				propAdapter.notifyDataSetChanged();
			}
			visualizationView.addLayer(newLayer);
		} else {
			Toast.makeText(context, "Invalid selection!", Toast.LENGTH_LONG).show();
		}
	}
	
	private void removeLayer(int item) {
		Layer toRemove = layers.get(item);

		if(toRemove != null) {
			visualizationView.removeLayer(toRemove);
			layers.remove(toRemove);
			propAdapter.notifyDataSetChanged();
		} else {
			Toast.makeText(context, "Unable to remove selected layer " + liveLayers[item], Toast.LENGTH_LONG).show();
		}
	}

	private CharSequence[] listLiveLayers() {
		liveLayers = new CharSequence[layers.size()];
		for(int i = 0; i < layers.size(); i++) {
			liveLayers[i] = layers.get(i).getName();
		}
		return liveLayers;
	}

	private void createLayerDialogs() {
		// Initialize the number of instances of each layer to zero
		counts = new int[availableLayers.length];
		for(int i = 0; i < counts.length; i++) {
			counts[i] = 0;
		}

		// Build a layer selection dialog for adding layers
		addLayerDialogBuilder = new AlertDialog.Builder(context);
		addLayerDialogBuilder.setTitle("Select a Layer");
		addLayerDialogBuilder.setItems(availableLayers, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				addNewLayer(item);
			}
		});
		addLayerDialog = addLayerDialogBuilder.create();

		// Build a layer selection dialog for removing layers
		remLayerDialogBuilder = new AlertDialog.Builder(context);
		remLayerDialogBuilder.setTitle("Select a Layer");
	}
	
	private void configureGUI() {
		addLayer = (Button) findViewById(R.id.add_layer);
		remLayer = (Button) findViewById(R.id.remove_layer);
		nameLayer = (Button) findViewById(R.id.rename_layer);
		addLayer.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				addLayerDialog.show();
			}
		});
		remLayer.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(layers.size() > 0) {
					remLayerDialogBuilder.setItems(listLiveLayers(), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							removeLayer(item);
						}
					});
					remLayerDialog = remLayerDialogBuilder.create();
					remLayerDialog.show();
				} else {
					Toast.makeText(context, "No layers to delete!", Toast.LENGTH_LONG).show();
				}
			}
		});
		nameLayer.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				remLayerDialogBuilder.setItems(listLiveLayers(), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						renameLayer(item);
					}
				});
				remLayerDialog = remLayerDialogBuilder.create();
				remLayerDialog.show();
			}
		});
	}
	
	private void renameLayer(int item) {
		final int selectedItem = item;
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Rename Layer");
		alert.setMessage("New layer name");

		final EditText input = new EditText(this);
		input.setText(liveLayers[item]);
		input.setSelectAllOnFocus(true);
		input.setSingleLine(true);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String newName = input.getText().toString();
				((DefaultLayer)layers.get(selectedItem)).setName(newName);
				propAdapter.notifyDataSetChanged();
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});
		alert.show();
	}
}
