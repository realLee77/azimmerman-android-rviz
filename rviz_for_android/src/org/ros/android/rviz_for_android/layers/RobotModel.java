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

import java.util.Set;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.rviz_for_android.drawable.ColladaMesh;
import org.ros.android.rviz_for_android.drawable.Cube;
import org.ros.android.rviz_for_android.drawable.Sphere;
import org.ros.android.rviz_for_android.prop.BoolProperty;
import org.ros.android.rviz_for_android.prop.LayerWithProperties;
import org.ros.android.rviz_for_android.prop.Property;
import org.ros.android.rviz_for_android.prop.PropertyUpdateListener;
import org.ros.android.rviz_for_android.prop.StringProperty;
import org.ros.android.rviz_for_android.urdf.Component;
import org.ros.android.rviz_for_android.urdf.MeshDownloader;
import org.ros.android.rviz_for_android.urdf.UrdfLink;
import org.ros.android.rviz_for_android.urdf.UrdfReader;
import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.OpenGlTransform;
import org.ros.android.view.visualization.layer.DefaultLayer;
import org.ros.android.view.visualization.layer.TfLayer;
import org.ros.android.view.visualization.shape.Color;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.parameter.ParameterTree;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Transform;

import android.os.Handler;
import android.util.Log;

public class RobotModel extends DefaultLayer implements LayerWithProperties, TfLayer {

	private static final String DEFAULT_PARAM_VALUE = "/robot_description";
	private BoolProperty prop = new BoolProperty("Enabled", true, null);
	private FrameTransformTree ftt;
	private Camera cam;
	private UrdfReader reader;
	private MeshDownloader downloader;
	private ParameterTree params;

	private Set<UrdfLink> urdf;
	
	// The visual and collision draw options exist both as properties and booleans in the RobotModel layer
	// Boolean access times are required to properly draw the model 
	private volatile boolean drawVis = true;
	private volatile boolean drawCol = false;

	public RobotModel(MeshDownloader downloader) {
		this.downloader = downloader;
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
		 
		
		test = ColladaMesh.newFromFile("/sdcard/base.dae");
	}
	
	private Component vis;
	private Component col;

	private Cube cube = new Cube(new Color(0, 1, 0, 1));
	private Sphere sphere = new Sphere();
	private ColladaMesh test;
	
	@Override
	public void draw(GL10 gl) {
		if(ftt == null || urdf == null || urdf.size() == 0) {
			Log.e("RobotModel", "FTT or URDF is null or empty. Aborting drawing.");
			return;
		}
			
		for(UrdfLink ul : urdf) {			
			vis = ul.getVisual();
			col = ul.getCollision();
			
			gl.glPushMatrix();
			
			// Transform to the URDF link's frame
			if(ftt.canTransform(cam.getFixedFrame(), ul.getName()))
				OpenGlTransform.apply(gl, ftt.newFrameTransform(cam.getFixedFrame(), ul.getName()).getTransform());

			// Draw the shape
			if(drawVis && vis != null) {
				switch(vis.getType()) {
				case BOX:					
					cube.setColor(vis.getMaterial_color());
					cube.draw(gl, Transform.newIdentityTransform(), vis.getSize());
					break;
				case CYLINDER:
					break;
				case SPHERE:
					sphere.setColor(vis.getMaterial_color());
					sphere.draw(gl, vis.getOrigin(), vis.getSize());
					break;
				case MESH:
					test.setColor(vis.getMaterial_color());
					test.draw(gl, vis.getOrigin(), vis.getSize());
					break;
				}
			}
			
			if(drawCol && col != null) {
				// TODO: Duplicate the vis drawing code for col		
			}

			gl.glPopMatrix();
		}
	}

	@Override
	public void onStart(final ConnectedNode node, Handler handler, FrameTransformTree frameTransformTree, Camera camera) {
		this.ftt = frameTransformTree;
		this.cam = camera;
		this.params = node.getParameterTree();

		reloadUrdf(DEFAULT_PARAM_VALUE);
	}

	private void reloadUrdf(String param) {
		String urdf_xml = null;
		if(params.has(param))
			urdf_xml = params.getString(param);
		else
			return;
		reader.readUrdf(urdf_xml);
		this.urdf = reader.getUrdf();
		Log.d("RobotModel", "Parsed URDF! Size: " + urdf.size());
	}
	
	@Override
	public boolean isEnabled() {
		return prop.getValue() && (drawVis || drawCol);
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
