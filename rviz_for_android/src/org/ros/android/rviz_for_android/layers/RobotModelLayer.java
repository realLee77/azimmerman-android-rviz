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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.rviz_for_android.MainActivity;
import org.ros.android.rviz_for_android.drawable.ColladaMesh;
import org.ros.android.rviz_for_android.drawable.Cube;
import org.ros.android.rviz_for_android.drawable.Cylinder;
import org.ros.android.rviz_for_android.drawable.StlMesh;
import org.ros.android.rviz_for_android.drawable.Sphere;
import org.ros.android.rviz_for_android.drawable.UrdfDrawable;
import org.ros.android.rviz_for_android.prop.BoolProperty;
import org.ros.android.rviz_for_android.prop.LayerWithProperties;
import org.ros.android.rviz_for_android.prop.Property;
import org.ros.android.rviz_for_android.prop.PropertyUpdateListener;
import org.ros.android.rviz_for_android.prop.StringProperty;
import org.ros.android.rviz_for_android.urdf.Component;
import org.ros.android.rviz_for_android.urdf.MeshFileDownloader;
import org.ros.android.rviz_for_android.urdf.UrdfLink;
import org.ros.android.rviz_for_android.urdf.UrdfReader;
import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.OpenGlTransform;
import org.ros.android.view.visualization.layer.DefaultLayer;
import org.ros.node.ConnectedNode;
import org.ros.node.parameter.ParameterTree;
import org.ros.rosjava_geometry.FrameTransformTree;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

public class RobotModelLayer extends DefaultLayer implements LayerWithProperties {

	private static final String DEFAULT_PARAM_VALUE = "/robot_description";
	private BoolProperty prop = new BoolProperty("Enabled", true, null);
	private FrameTransformTree ftt;
	private Camera cam;
	private UrdfReader reader;
	private ParameterTree params;

	private volatile boolean readyToDraw = false;
	private List<UrdfLink> urdf;

	// The visual and collision draw options exist both as properties and booleans in the RobotModel layer
	// Boolean access times are required to properly draw the model
	private volatile boolean drawVis = true;
	private volatile boolean drawCol = false;

	private Activity context;
	private MeshFileDownloader mfd;

	public RobotModelLayer(MeshFileDownloader mfd) {
		if(mfd == null)
			throw new IllegalArgumentException("MFD is null!");

		this.context = mfd.getContext();
		this.mfd = mfd;

		reader = new UrdfReader();

		prop.addSubProperty(new StringProperty("Parameter", DEFAULT_PARAM_VALUE, new PropertyUpdateListener<String>() {
			@Override
			public void onPropertyChanged(String newval) {
				if(params.has(newval)) {
					reloadUrdf(newval);
				}
			}
		}));
		prop.addSubProperty(new BoolProperty("Visual", drawVis, new PropertyUpdateListener<Boolean>() {
			@Override
			public void onPropertyChanged(Boolean newval) {
				drawVis = newval;
				requestRender();
			}
		}));
		prop.addSubProperty(new BoolProperty("Collision", drawCol, new PropertyUpdateListener<Boolean>() {
			@Override
			public void onPropertyChanged(Boolean newval) {
				drawCol = newval;
				requestRender();
			}
		}));
	}

	private Component vis;
	private Component col;

	private Cylinder cyl = new Cylinder();
	private Cube cube = new Cube();
	private Sphere sphere = new Sphere();

	private Map<String, UrdfDrawable> meshes = new HashMap<String, UrdfDrawable>();

	@Override
	public void draw(GL10 gl) {
		if(!readyToDraw || ftt == null || urdf == null) {
			return;
		}

		for(UrdfLink ul : urdf) {
			vis = ul.getVisual();
			col = ul.getCollision();

			gl.glPushMatrix();

			// Transform to the URDF link's frame
			// if(ftt.canTransform(cam.getFixedFrame(), ul.getName())) {
			// Transform t = ftt.newFrameTransform(cam.getFixedFrame(), ul.getName()).getTransform();
			// Log.i("RobotModel", t.toString());
			// OpenGlTransform.apply(gl, t);
			// }

			OpenGlTransform.apply(gl, ftt.newTransformIfPossible(ul.getName(), cam.getFixedFrame()));

			// Draw the shape
			if(drawVis && vis != null) {
				drawComponent(gl, vis);
			}
			if(drawCol && col != null) {
				drawComponent(gl, col);
			}

			gl.glPopMatrix();
		}
	}

	private void drawComponent(GL10 gl, Component com) {
		switch(com.getType()) {
		case BOX:
			cube.setColor(com.getMaterial_color());
			cube.draw(gl, com.getOrigin(), com.getSize());
			break;
		case CYLINDER:
			cyl.setColor(com.getMaterial_color());
			cyl.draw(gl, com.getOrigin(), com.getLength(), com.getRadius());
			break;
		case SPHERE:
			sphere.setColor(com.getMaterial_color());
			sphere.draw(gl, com.getOrigin(), com.getRadius());
			break;
		case MESH:
			UrdfDrawable ud = meshes.get(com.getMesh());
			if(ud != null)
				ud.draw(gl, com.getOrigin(), com.getSize());
			else
				loadMesh(com.getMesh());
			break;
		}
	}

	private void loadMesh(String meshResourceName) {
		if(meshResourceName.toLowerCase().endsWith(".dae"))
			meshes.put(meshResourceName, ColladaMesh.newFromFile(meshResourceName, mfd));
		else if(meshResourceName.toLowerCase().endsWith(".stl"))
			meshes.put(meshResourceName, StlMesh.newFromFile(meshResourceName, mfd));
		else
			Log.e("Downloader", "Unknown mesh type! " + meshResourceName);
	}

	@Override
	public void onStart(final ConnectedNode node, Handler handler, FrameTransformTree frameTransformTree, Camera camera) {
		this.ftt = frameTransformTree;
		this.cam = camera;
		this.params = node.getParameterTree();

		reloadUrdf(DEFAULT_PARAM_VALUE);
	}

	private void reloadUrdf(final String param) {
		final LoadUrdf lu = new LoadUrdf();
		context.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				lu.execute(param);
			}
		});
	}

	@Override
	public boolean isEnabled() {
		return prop.getValue() && (drawVis || drawCol);
	}

	@Override
	public Property<?> getProperties() {
		return prop;
	}

	private class LoadUrdf extends AsyncTask<String, String, Void> {

		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			Toast.makeText(context, values[0], Toast.LENGTH_SHORT).show();
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			readyToDraw = false;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			readyToDraw = true;
		}

		@Override
		protected Void doInBackground(String... parameters) {
			publishProgress("Parsing URDF...");
			String param = parameters[0];
			
			// Parse the URDF
			String urdf_xml = null;
			if(params.has(param))
				urdf_xml = params.getString(param);
			else
				return null;
			reader.readUrdf(urdf_xml);
			urdf = reader.getUrdf();
			publishProgress("Downloading geometry...");
			// Load any referenced models
			for(UrdfLink ul : urdf) {
				for(Component c : ul.getComponents()) {
					if(c.getType() == Component.GEOMETRY.MESH) {
						loadMesh(c.getMesh());
					}
				}
			}
			publishProgress("Done!");
			return null;
		}

	}
}
