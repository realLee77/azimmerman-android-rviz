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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.rviz_for_android.prop.BoolProperty;
import org.ros.android.rviz_for_android.prop.ColorProperty;
import org.ros.android.rviz_for_android.prop.FloatProperty;
import org.ros.android.rviz_for_android.prop.GraphNameProperty;
import org.ros.android.rviz_for_android.prop.IntProperty;
import org.ros.android.rviz_for_android.prop.LayerWithProperties;
import org.ros.android.rviz_for_android.prop.Property;
import org.ros.android.rviz_for_android.prop.PropertyUpdateListener;
import org.ros.android.rviz_for_android.prop.Vector3Property;
import org.ros.android.view.visualization.layer.DefaultLayer;
import org.ros.android.view.visualization.layer.TfLayer;
import org.ros.android.view.visualization.shape.Color;
import org.ros.namespace.GraphName;
import org.ros.rosjava_geometry.Vector3;

public class GridLayer extends DefaultLayer implements LayerWithProperties, TfLayer {
	private int nLines;
	private float vertices[];
	private short indices[];
	private FloatBuffer vbb;
	private ShortBuffer ibb;

	private BoolProperty prop;
	private boolean ready = false;
	
	private float xOffset = 0f; 
	private float yOffset = 0f; 
	private float zOffset = 0f; 
	
	public GridLayer(int xCells, int yCells, float xSpacing, float ySpacing) {
		super();
		
		prop = new BoolProperty("enabled", true, null);
		prop.addSubProperty(new GraphNameProperty("Parent", null, null, null));
		prop.addSubProperty(new IntProperty("xCells", xCells, new PropertyUpdateListener<Integer>() {
			public void onPropertyChanged(Integer newval) {
				onValueChanged();
			}
		}).setValidRange(1, 1000));
		prop.addSubProperty(new IntProperty("yCells", yCells, new PropertyUpdateListener<Integer>() {
			public void onPropertyChanged(Integer newval) {
				onValueChanged();
			}
		}).setValidRange(1, 1000));
		
		prop.addSubProperty(new FloatProperty("xSpacing", xSpacing, new PropertyUpdateListener<Float>() {
			public void onPropertyChanged(Float newval) {
				onValueChanged();
			}
		}).setValidRange(0.01f, 10000f));
		prop.addSubProperty(new FloatProperty("ySpacing", ySpacing, new PropertyUpdateListener<Float>() {
			public void onPropertyChanged(Float newval) {
				onValueChanged();
			}
		}).setValidRange(0.01f, 10000f));
		prop.addSubProperty(new Vector3Property("offset", new Vector3(0,0,0), new PropertyUpdateListener<Vector3>() {

			public void onPropertyChanged(Vector3 newval) {
				xOffset = (float) newval.getX();
				yOffset = (float) newval.getY();
				zOffset = (float) newval.getZ();
			}
			
		}));
		prop.addSubProperty(new ColorProperty("color", new Color(1f, 1f, 1f, 1f), null));

		initGrid();
	}
	
	private void onValueChanged() {
		initGrid();
		requestRender();
	}

	private void initGrid() {
		ready = false;
		int xCells = (Integer) prop.getProperty("xCells").getValue();
		int yCells = (Integer) prop.getProperty("yCells").getValue();
		float xSpacing = (Float) prop.getProperty("xSpacing").getValue();
		float ySpacing = (Float) prop.getProperty("ySpacing").getValue();
		
		nLines = 2*xCells + 2*yCells + 2;
		vertices = new float[3*(2*nLines)];
		indices = new short[2*nLines];
		float yMin = -ySpacing*yCells;
		float yMax = ySpacing*yCells;
		float xMin = -xSpacing*xCells;
		float xMax = xSpacing*xCells;		
		
		int idx = -1;
		for(float x = xMin; x <= xMax; x += xSpacing) {
			// Edge start
			vertices[++idx] = x;
			vertices[++idx] = yMin;
			vertices[++idx] = 0;
			// Edge end 
			vertices[++idx] = x;
			vertices[++idx] = yMax;
			vertices[++idx] = 0;
		}
		for(float y = yMin; y <= yMax; y += ySpacing) {
			// Edge start
			vertices[++idx] = xMin;
			vertices[++idx] = y;
			vertices[++idx] = 0;
			// Edge end
			vertices[++idx] = xMax;
			vertices[++idx] = y;
			vertices[++idx] = 0;	
		}
		
		for(int i = 0; i < 2*nLines; i ++) {
			indices[i] = (short)i;
		}
		
		// Pack the vertices into a byte array
		ByteBuffer bb_vtx = ByteBuffer.allocateDirect(vertices.length * 4);
		bb_vtx.order(ByteOrder.nativeOrder());
		vbb = bb_vtx.asFloatBuffer();
		vbb.put(vertices);
		vbb.position(0);

		ByteBuffer bb_idx = ByteBuffer.allocateDirect(indices.length * 2);
		bb_idx.order(ByteOrder.nativeOrder());
		ibb = bb_idx.asShortBuffer();
		ibb.put(indices);
		ibb.position(0);
		
		ready = true;
		requestRender();
	}
	
	@Override
	public void draw(GL10 gl) {		
		if(prop.getValue() && ready) {
					
			Color c = (Color) prop.getProperty("color").getValue();
			gl.glColor4f(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			
			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vbb);
			gl.glPushMatrix();
			gl.glTranslatef(xOffset, yOffset, zOffset);
			gl.glDrawElements(GL10.GL_LINES, 2*nLines, GL10.GL_UNSIGNED_SHORT, ibb);
			gl.glPopMatrix();
			gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		}
	}

	public Property<?> getProperties() {
		return prop;
	}

	public GraphName getFrame() {
		return (GraphName) prop.getProperty("Parent").getValue();
	}
	
	@Override
	public boolean isEnabled() {
		return prop.getValue();
	}	
}
