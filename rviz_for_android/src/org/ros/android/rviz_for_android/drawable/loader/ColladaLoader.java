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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ros.android.rviz_for_android.urdf.XmlReader;
import org.ros.android.view.visualization.shape.BaseShape;
import org.ros.android.view.visualization.shape.Color;
import org.ros.android.view.visualization.shape.TrianglesShape;
import org.ros.rosjava_geometry.Vector3;
import org.w3c.dom.NodeList;

import android.util.Log;

public class ColladaLoader extends XmlReader {
	private static enum semanticType {
		POSITION, NORMAL, TEXCOORD
	};

	private List<BaseShape> geometries;

	public ColladaLoader() {
		super(false);
	}

	public void readDae(String contents) {
		if(contents == null)
			throw new IllegalArgumentException("Invalid DAE file contents passed to ColladaLoader");
		buildDocument(contents);
		parseDae();
	}

	private void parseDae() {
		// Get the ID of each geometry section
		NodeList nodes = getExpression("/COLLADA/library_geometries/geometry/@id");

		for(int i = 0; i < nodes.getLength(); i++) {
			String ID = nodes.item(i).getNodeValue();
			Log.d("DAE", "Parsing geometry " + ID);
			parseGeometry(ID);
		}
	}

	private static enum TYPES {
		triangles(3, 0), tristrips(1, -2), trifans(1, -2);
		private int mul;
		private int sub;

		TYPES(int b, int i) {
			this.mul = b;
			this.sub = i;
		}

		public int getVertexCount(int elementCount) {
			return (mul * elementCount) + sub;
		}
	};

	// Each geometry can have multiple associated triangles in different
	// configurations using different materials
	// They all use the same vertices and normals though. If they don't, they
	// aren't supported by this loader currently.
	private void parseGeometry(String id) {
		geometries = new ArrayList<BaseShape>();
		String prefix = "/COLLADA/library_geometries/geometry[@id='" + id + "']/mesh";

		// If the selected geometry doesn't contain one of the types, return
		// null. We're not interested in lines or any other shenanigans
		boolean acceptableGeometry = false;
		for(TYPES t : TYPES.values()) {
			if(nodeExists(prefix, "", t.toString())) {
				acceptableGeometry = true;
				break;
			}
		}
		if(!acceptableGeometry) {
			return;
		}

		// For each submesh inside the mesh tag, parse its vertices, normals, and texture data
		for(TYPES type : TYPES.values()) {
			NodeList nodes = getExpression(prefix, type.toString());
			for(int i = 1; i <= nodes.getLength(); i++) {
				Log.i("DAE", "Parsing submesh " + i);
				geometries.add(parseSubMesh(prefix, type, i));
			}
		}
	}
	
	private static Color defaultColor = new Color(1,1,0,1);

	private BaseShape parseSubMesh(String prefix, TYPES type, int submeshIndex) {
		// Load all necessary data (vertices, normals, texture coordinates, etc
		Map<String, InputData> data = getDataFromAllInputs(prefix, type.toString());

		// Load indices
		short[] indices = toShortArray(getSingleNode(prefix, type + "[" + submeshIndex + "]/p").getTextContent());

		// Find the triangle count
		int triCount = Integer.parseInt(getSingleNode(prefix, type.toString(), "@count").getNodeValue());

		Log.d("DAE", "I'm expecting " + triCount + " triangles.");

		// If the normals and positions are the only values included AND they have the same offset, there's no need to deindex
		if(data.size() == 2 && data.containsKey("NORMAL") && data.containsKey("POSITION") && (data.get("NORMAL").getOffset() == data.get("POSITION").getOffset())) {
			Log.d("DAE", "I've detected that deindexing is not necessary for this dataset!");
			return new TrianglesShape(data.get("POSITION").getData().getArray(), data.get("NORMAL").getData().getArray(), indices, defaultColor);
		}
		
		// Deindex
		Map<String, FloatVector> results = deindex(data, indices, type.getVertexCount(triCount));

		Log.i("DAE", "For each vertex, I have the following information: " + results.keySet());

		switch(type) {
		case triangles:
			return new TrianglesShape(results.get("POSITION").getArray(), results.get("NORMAL").getArray(), defaultColor);
		case tristrips:
		case trifans:
		default:
			return null;
		}
	}

	private Map<String, FloatVector> deindex(Map<String, InputData> data, short[] indices, int vertexCount) {
		Map<String, FloatVector> retval = new HashMap<String, FloatVector>();

		List<InputData> sources = new ArrayList<InputData>(data.values());

		int inputCount = -99;
		for(InputData id : sources) {
			inputCount = Math.max(inputCount, id.getOffset());
			retval.put(id.getSemantic(), new FloatVector(3 * vertexCount));
		}

		Log.d("DAE", "BEGINNING DEINDEXING");
		Log.d("DAE", "The indices point to " + sources.size() + " sources.");
		Log.d("DAE", "There are " + (inputCount + 1) + " pieces of information per vertex, " + vertexCount + " vertices");
		Log.d("DAE", "There are " + indices.length + " mixed type indices");

		int curOffset = 0;
		for(Short s : indices) {
			for(InputData id : sources) {
				if(curOffset == id.getOffset()) {
					FloatVector reciever = retval.get(id.getSemantic());
					id.appendData(reciever, s);
				}
			}

			if(curOffset == inputCount)
				curOffset = 0;
			else
				curOffset++;
		}

		return retval;
	}

	private Map<String, InputData> getDataFromAllInputs(String prefix, String subMeshType) {
		Map<String, InputData> retval = new HashMap<String, InputData>();

		NodeList inputs = getExpression(prefix, subMeshType, "input");
		for(int i = 0; i < inputs.getLength(); i++) {
			String semantic = inputs.item(i).getAttributes().getNamedItem("semantic").getNodeValue();
			String sourceID = inputs.item(i).getAttributes().getNamedItem("source").getNodeValue().substring(1);
			int offset = Integer.parseInt(inputs.item(i).getAttributes().getNamedItem("offset").getNodeValue());
			List<InputData> returned = getDataFromInput(prefix, semantic, sourceID);
			for(InputData id : returned) {
				id.setOffset(offset);
				retval.put(id.getSemantic(), id);
			}
		}

		return retval;
	}

	private List<InputData> getDataFromInput(String prefix, String semantic, String sourceID) {
		List<InputData> retval = new ArrayList<InputData>();

		// Find whatever node has the requested ID
		String nodetype = getSingleNode(prefix, "/*[@id='" + sourceID + "']").getNodeName();

		// If it's a vertices node, get the data from the inputs it references
		if(nodetype.equals("vertices")) {
			List<String> inputs = getValuesAsList(prefix, "/vertices[@id='" + sourceID + "']/input/@semantic");
			for(String subSemantic : inputs) {
				retval.addAll(getDataFromInput(prefix, subSemantic, getSingleNode(prefix, "/vertices[@id='" + sourceID + "']/input[@semantic='" + subSemantic + "']/@source").getNodeValue().substring(1)));
			}

		} else
		// If it's a source, grab its float_array data
		if(nodetype.equals("source")) {
			retval.add(new InputData(semantic, new FloatVector(toFloatArray(getSingleNode(prefix, "/source[@id='" + sourceID + "']/float_array").getTextContent()))));
			return retval;
		} else {
			Log.e("DAE", "ERR! UNKNOWN NODE TYPE: " + nodetype);
		}

		return retval;
	}

	private class InputData {
		private semanticType sType;
		private int offset = -1;
		private FloatVector data;

		public InputData(String semantic, FloatVector data) {
			super();
			this.sType = semanticType.valueOf(semantic);
			this.data = data;
		}

		public void setOffset(int offset) {
			this.offset = offset;
		}

		public String getSemantic() {
			return sType.toString();
		}

		public int getOffset() {
			return offset;
		}

		public FloatVector getData() {
			return data;
		}

		@Override
		public String toString() {
			return "InputData [semantic=" + sType.toString() + ", offset=" + offset + ", data size=" + data.getIdx() + "]";
		}
				
		public void appendData(FloatVector destination, int idx) {
			switch(sType) {
			case TEXCOORD:
				for(int b = (idx * 2); b < (idx * 2) + 2; b++)
					destination.add(data.get(b));
				break;
			case POSITION:
				for(int b = (idx * 3); b < (idx * 3) + 3; b++)
					destination.add(data.get(b));
				break;
			case NORMAL:
				// Normalize the loaded normal
				int offset = idx*3;
				float x = data.get(offset++);
				float y = data.get(offset++);
				float z = data.get(offset++);
				float len = (float)Math.sqrt(x*x + y*y + z*z);

				destination.add(x/len);
				destination.add(y/len);
				destination.add(z/len);
				break;
			}
		}

		private ColladaLoader getOuterType() {
			return ColladaLoader.this;
		}

	}

	public List<BaseShape> getGeometries() {
		return geometries;
	}
}
