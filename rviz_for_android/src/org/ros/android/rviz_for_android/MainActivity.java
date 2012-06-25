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
import java.util.Set;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.android.rviz_for_android.layers.AxisLayer;
import org.ros.android.rviz_for_android.layers.FPSLayer;
import org.ros.android.rviz_for_android.layers.GridLayer;
import org.ros.android.rviz_for_android.layers.ParentableOrbitCameraControlLayer;
import org.ros.android.rviz_for_android.layers.RobotModelLayer;
import org.ros.android.rviz_for_android.prop.LayerWithProperties;
import org.ros.android.rviz_for_android.prop.PropertyListAdapter;
import org.ros.android.rviz_for_android.urdf.MeshFileDownloader;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.layer.DefaultLayer;
import org.ros.android.view.visualization.layer.Layer;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.rosjava_geometry.FrameTransformTree;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
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
import android.widget.ProgressBar;
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

	// Tracking layers
	private CharSequence[] liveLayers;
	private CharSequence[] availableLayers = { "Axis", "Grid", "RobotModel" };
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

	// Enable/disable following
	boolean following = false;
	ParentableOrbitCameraControlLayer camControl;
	
	// Mesh downloader
	MeshFileDownloader mfd;
	
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
		menu.setGroupEnabled(R.id.unfollowGroup, following);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.menu_layertoggle:
			if(showLayers) {
				ll.setVisibility(LinearLayout.GONE);
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(ll.getWindowToken(), 0);
			} else {
				ll.setVisibility(LinearLayout.VISIBLE);
			}
			showLayers = !showLayers;
			break;
		case R.id.menu_follow:
			showTFSelectDialog();
			break;
		case R.id.menu_unfollow:
			camControl.setTargetFrame(null);
			item.setEnabled(false);
			following = false;
			break;
		case R.id.clear_model_cache:
			int clearedCount = mfd.clearCache();
			Toast.makeText(this, "Cleared " + clearedCount + " items in model cache", Toast.LENGTH_LONG).show();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void showTFSelectDialog() {
		FrameTransformTree ftt = visualizationView.getFrameTransformTree();

		Set<String> frameset = ftt.getFrameTracker().getAvailableFrames();
		final String[] tfFrames = (String[]) frameset.toArray(new String[frameset.size()]);

		if(tfFrames.length > 0) {
			AlertDialog.Builder selTfFrame = new AlertDialog.Builder(this);
			selTfFrame.setTitle("Select a frame");
			selTfFrame.setItems(tfFrames, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					camControl.setTargetFrame(tfFrames[item]);
					following = true;
					invalidateOptionsMenu();
				}
			});
			AlertDialog dialog = selTfFrame.create();
			dialog.show();
		} else {
			Toast.makeText(this, "No TF frames to follow!", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		MainActivity.context = this;

		createLayerDialogs();
		configureGUI();
		
		camControl = new ParentableOrbitCameraControlLayer(this);
		camControl.setName("Camera");
		layers.add(camControl);

		ll = ((LinearLayout) findViewById(R.id.layer_layout));
		ll.setVisibility(LinearLayout.GONE);

		visualizationView = (VisualizationView) findViewById(R.id.visualization);
		for(Layer l : layers)
			visualizationView.addLayer(l);

		elv = (ExpandableListView) findViewById(R.id.expandableListView1);
		propAdapter = new PropertyListAdapter(layers, getApplicationContext());
		elv.setAdapter(propAdapter);
		elv.setItemsCanFocus(true);
		
		// TODO: Add default layers. MAKE THESE LOADED FROM A CONFIG FILE
		addNewLayer(0);
		addNewLayer(1);
		
		visualizationView.addLayer(new FPSLayer());
	}

	public static Context getAppContext() {
		return MainActivity.context;
	}

	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {		
		mfd = MeshFileDownloader.getMeshFileDownloader("http://" + getMasterUri().getHost().toString() + ":44644", this);
		NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(), getMasterUri());
		nodeMainExecutor.execute(visualizationView, nodeConfiguration.setNodeName("android/map_view"));
	}

	private void addNewLayer(int layertype) {
		DefaultLayer newLayer = null;
		switch(layertype) {
		case 0:
			newLayer = new AxisLayer();
			break;
		case 1:
			newLayer = new GridLayer(10, 1f);
			break;
		case 2:
			newLayer = new RobotModelLayer(mfd);
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
		Layer toRemove = layers.get(item + 1);

		if(toRemove != null) {
			visualizationView.removeLayer(toRemove);
			layers.remove(toRemove);
			propAdapter.notifyDataSetChanged();
		} else {
			Toast.makeText(context, "Unable to remove selected layer " + liveLayers[item], Toast.LENGTH_LONG).show();
		}
	}

	private CharSequence[] listLiveLayers() {
		liveLayers = new CharSequence[layers.size() - 1];
		for(int i = 1; i < layers.size(); i++) {
			liveLayers[i - 1] = layers.get(i).getName();
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
		input.setText(liveLayers[item + 1]);
		input.setSelectAllOnFocus(true);
		input.setSingleLine(true);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String newName = input.getText().toString();
				((DefaultLayer) layers.get(selectedItem)).setName(newName);
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
