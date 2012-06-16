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

package org.ros.android.rviz_for_android.drawable.loader;

import java.util.HashSet;
import java.util.Set;

import org.ros.android.rviz_for_android.drawable.ColladaGeometry;
import org.ros.android.rviz_for_android.urdf.XmlReader;
import org.w3c.dom.NodeList;

import android.util.Log;

public class ColladaLoader extends XmlReader {
	
	private Set<ColladaGeometry> geometries = new HashSet<ColladaGeometry>();
	
	public ColladaLoader() {
		super(false);
	}
	
	public void readDae(String contents) {
		if(contents == null)
			throw new IllegalArgumentException("Invalid DAE file contents passed to ColladaLoader");
		this.geometries.clear();
		buildDocument(contents);
		parseDae();
	}
	
	public Set<ColladaGeometry> getGeometries() {
		return geometries;
	}
	
	private void parseDae() {
		// Get the ID of each geometry section
		NodeList nodes = getExpression("/COLLADA/library_geometries/geometry/@id");

		for(int i = 0; i<nodes.getLength();i++) {
			String ID = nodes.item(i).getNodeValue();
			Log.d("DAE", "Parsing geometry " + ID);
			ColladaGeometry retval = parseGeometry(ID);
			if(retval != null)
				geometries.add(retval);
			Log.d("DAE", "Success!");
		}
	}

	private static String[] types = {"triangles", "tristrips", "trifans"};
	
	// Each geometry can have multiple associated triangles in different configurations using different materials
	// They all use the same vertices and normals though. If they don't, they aren't supported by this loader currently.
	private ColladaGeometry parseGeometry(String id) {
		String prefix = "/COLLADA/library_geometries/geometry[@id='"+id+"']/mesh";
		
		// If the selected geometry doesn't contain one of the types, return null. We're not interested in lines or any other shenanigans
		boolean acceptableGeometry = false;
		for(String type: types) {
			if(nodeExists(prefix,"",type)) {
				acceptableGeometry = true;
				break;
			}
		}
		if(!acceptableGeometry) {
			Log.e("DAE", "No acceptable geometry found!");
			return null;
		}
		
		short[] indices = null;
		float[] vertices = null;
		float[] normals = null;
		
		ColladaGeometry retval = null;
		
		// Find the ID of the vertices tag pointing to the POSITION and NORMAL locations
		String verticesID = getSingleNode(prefix,"vertices/@id").getNodeValue();
//		String positionID = getSingleNode(prefix,"vertices/input[@semantic='POSITION']/@source").getNodeValue().substring(1);
//		String normalID = getSingleNode(prefix,"vertices/input[@semantic='NORMAL']/@source").getNodeValue().substring(1);

		String positionID = getSingleNode(prefix,"/input[@semantic='POSITION']/@source").getNodeValue().substring(1);
		String normalID = getSingleNode(prefix,"/input[@semantic='NORMAL']/@source").getNodeValue().substring(1);
		Log.i("DAE", "Position ID: " + positionID + "   Normal ID: " + normalID);
		
		
		// Fetch the vertex and normal data
		vertices = toFloatArray(getSingleNode(prefix,"source[@id='"+positionID+"']/float_array").getTextContent());
		normals = toFloatArray(getSingleNode(prefix,"source[@id='"+normalID+"']/float_array").getTextContent());

		Log.i("DAE", "Loaded " + vertices.length + " vertices for drawable #");
		Log.i("DAE", "Loaded " + normals.length + " normals for drawable #");
		
		retval = new ColladaGeometry(normals, vertices);
		
		// For each type of drawing, add a part to the geometry with the corresponding indices
		for(String type : types) {
			NodeList nodes = getExpression(prefix, type);
			for(int i = 1; i <= nodes.getLength(); i++) {
				String inputSource = getSingleNode(prefix,type + "[" + i + "]/input[@semantic='VERTEX']/@source").getNodeValue().substring(1);
				if(!inputSource.equals(verticesID))
					throw new RuntimeException("DAE format error. Encountered drawables in the same geometry tag which reference different sets of vertices!");
				indices = toShortArray(getSingleNode(prefix,type + "[" + i + "]/p").getTextContent());
				Log.i("DAE", "Loaded " + indices.length + " indices for drawable #" + i);
				retval.addPart(indices, type);
			}			
		}		
		
		return retval;
	}
}
