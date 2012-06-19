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
import java.util.Set;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.rviz_for_android.drawable.ColladaMesh;
import org.ros.android.rviz_for_android.drawable.Cube;
import org.ros.android.rviz_for_android.drawable.Cylinder;
import org.ros.android.rviz_for_android.drawable.Sphere;
import org.ros.android.rviz_for_android.drawable.UrdfDrawable;
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
import org.ros.android.view.visualization.shape.Color;
import org.ros.node.ConnectedNode;
import org.ros.node.parameter.ParameterTree;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Transform;

import android.os.Handler;
import android.util.Log;

public class RobotModel extends DefaultLayer implements LayerWithProperties {

	private static final String DEFAULT_PARAM_VALUE = "/robot_description";
	private BoolProperty prop = new BoolProperty("Enabled", true, null);
	private FrameTransformTree ftt;
	private Camera cam;
	private UrdfReader reader;
	private MeshDownloader downloader;
	private ParameterTree params;

	private volatile boolean readyToDraw = false;
	private List<UrdfLink> urdf;
	
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

	private Cylinder cyl = new Cylinder();
	private Cube cube = new Cube();
	private Sphere sphere = new Sphere();
	private ColladaMesh test;
	
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
//			if(ftt.canTransform(cam.getFixedFrame(), ul.getName())) {
//				Transform t = ftt.newFrameTransform(cam.getFixedFrame(), ul.getName()).getTransform();
//				Log.i("RobotModel", t.toString());
//				OpenGlTransform.apply(gl, t);
//			}
		
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
		meshes.put(meshResourceName, test);
	}
	
	@Override
	public void onStart(final ConnectedNode node, Handler handler, FrameTransformTree frameTransformTree, Camera camera) {
		this.ftt = frameTransformTree;
		this.cam = camera;
		this.params = node.getParameterTree();
		
		readyToDraw = false;
		reloadUrdf(DEFAULT_PARAM_VALUE);
		readyToDraw = true;
	}

	private void reloadUrdf(String param) {
		readyToDraw = false;
		String urdf_xml = null;
		if(params.has(param))
			urdf_xml = params.getString(param);
		else
			return;
		reader.readUrdf(urdf_xml);
		this.urdf = reader.getUrdf();
		Log.d("RobotModel", "Parsed URDF! Size: " + urdf.size());
		readyToDraw = true;
	}
	
	@Override
	public boolean isEnabled() {
		return prop.getValue() && (drawVis || drawCol);
	}

	@Override
	public Property<?> getProperties() {
		return prop;
	}

}
