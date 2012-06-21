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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ros.android.rviz_for_android.urdf.MeshFileDownloader;
import org.ros.android.rviz_for_android.urdf.XmlReader;
import org.ros.android.view.visualization.shape.BaseShape;
import org.ros.android.view.visualization.shape.Color;
import org.ros.android.view.visualization.shape.TexturedTrianglesShape;
import org.ros.android.view.visualization.shape.TrianglesShape;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.tiffdecoder.TiffDecoder;

public class ColladaLoader extends XmlReader {
	private static enum semanticType {
		POSITION(3), NORMAL(3), TEXCOORD(2);

		private int mul;

		semanticType(int mul) {
			this.mul = mul;
		}

		public int numElements(int vertexCount) {
			return mul * vertexCount;
		}

	};

	private List<BaseShape> geometries;
	
	private MeshFileDownloader mfd;
	private String imgPrefix;
	private Context context;

	public ColladaLoader() {
		super(false);
	}

	public void setDownloader(MeshFileDownloader mfd) {
		if(mfd == null)
			throw new IllegalArgumentException("Passed a null MeshFileDownloader! Just what do you think you're doing?");
		this.mfd = mfd;
		this.context = mfd.getContext();
	}
	
	public void readDae(InputStream fileStream, String imgPrefix) {
		if(fileStream == null)
			throw new IllegalArgumentException("Invalid DAE file contents passed to ColladaLoader");
		this.imgPrefix = imgPrefix;
		buildDocument(fileStream);
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

	private static Color defaultColor = new Color(1, 1, 0, 1);

	private BaseShape parseSubMesh(String prefix, TYPES type, int submeshIndex) {
		// Load all necessary data (vertices, normals, texture coordinates, etc
		Map<String, InputData> data = getDataFromAllInputs(prefix, type.toString());

		// Load indices
		short[] indices = toShortArray(getSingleNode(prefix, type + "[" + submeshIndex + "]/p").getTextContent());

		// Find the triangle count
		int triCount = Integer.parseInt(getSingleNode(prefix, type.toString(), "@count").getNodeValue());

		Log.d("DAE", "I'm expecting " + triCount + " triangles.");

		boolean textured = false;
		Map<String, Bitmap> textures = null;

		// Load the images if the mesh is textured. Otherwise, if the normals and positions are the only
		// values included AND they have the same offset, there's no need to deindex, can return a mesh immediately
		if(data.containsKey("TEXCOORD")) {
			Log.d("DAE", "Mesh is textured!");
			textures = getTextures(prefix);
			textured = true;
		} else if(data.size() == 2 && data.containsKey("NORMAL") && data.containsKey("POSITION") && (data.get("NORMAL").getOffset() == data.get("POSITION").getOffset())) {
			Log.d("DAE", "I've detected that deindexing is not necessary for this dataset!");
			return new TrianglesShape(data.get("POSITION").getData().getArray(), data.get("NORMAL").getData().getArray(), indices, defaultColor);
		}

		// Deindex
		Map<String, FloatVector> results = deindex(data, indices, type.getVertexCount(triCount));

		Log.i("DAE", "For each vertex, I have the following information: " + results.keySet());

		if(!textured) {
			switch(type) {
			case triangles:
				return new TrianglesShape(results.get("POSITION").getArray(), results.get("NORMAL").getArray(), defaultColor);
			case tristrips:
			case trifans:
			default:
				return null;
			}
		} else {
			switch(type) {
			case triangles:
				return new TexturedTrianglesShape(results.get("POSITION").getArray(), results.get("NORMAL").getArray(), results.get("TEXCOORD").getArray(), textures);
			case tristrips:
			case trifans:
			default:
				return null;
			}
		}
	}

	private enum textureType {
		diffuse, bump
	};

	private Map<String, Bitmap> getTextures(String prefix) {
		Map<String, Bitmap> retval = new HashMap<String, Bitmap>();

		// Find which types of textures are present (diffuse, bump, etc)
		for(textureType t : textureType.values()) {
			if(nodeExists("/COLLADA/library_effects/", t.toString(), "texture/@texture")) {
				Log.i("DAE", "  Mesh has " + t.toString() + " texture component.");
				String texPointer = existResults.item(0).getNodeValue();

				System.out.println(t.toString() + " " + texPointer);

				// Locate the image ID from the texture pointer
				String imgID = getSingleNode("/COLLADA/library_effects//newparam[@sid='" + texPointer + "']/sampler2D/source").getTextContent();

				System.out.println(imgID);

				// Locate the image name
				String imgName = getSingleNode("/COLLADA/library_effects//newparam[@sid='" + imgID + "']/surface/init_from").getTextContent();
				System.out.println(imgName);

				// Locate the filename
				String filename = getSingleNode("/COLLADA/library_images/image[@id='" + imgName + "']/init_from").getTextContent();
				System.out.println(filename);

				retval.put(t.toString(), loadTextureFile(imgPrefix, filename));
			}
		}

		return retval;
	}
	
	private Bitmap loadTextureFile(String prefix, String filename) {
		Log.d("DAE", "Need to load an image: " + prefix + "   " + filename);
		return openTextureFile(mfd.getContext().getFilesDir().toString() + "/", mfd.getFile(prefix + filename));
//		if(mfd.fileExists(prefix + filename)) {
//			Log.d("DAE", "File already exists! Opening and returning the contents.");
//		
//		} else {
//			Log.d("DAE", "File doesn't exist! Downloading it from the server");
//			mfd.getFile(prefix + filename);
//		}
//		return null;
	}
	
	private Bitmap openTextureFile(String path, String filename) {
		Bitmap retval = null;
		if(filename.toLowerCase().endsWith(".tif")) {
			Log.d("DAE", "Loading TIF image: " + path + filename);
			TiffDecoder.nativeTiffOpen(path + filename);
			int[] pixels = TiffDecoder.nativeTiffGetBytes();
			retval = Bitmap.createBitmap(pixels, TiffDecoder.nativeTiffGetWidth(), TiffDecoder.nativeTiffGetHeight(), Bitmap.Config.ARGB_8888);
			TiffDecoder.nativeTiffClose();
		} else {
			retval = BitmapFactory.decodeFile(path + filename);
		}
		return retval;
	}

	private Map<String, FloatVector> deindex(Map<String, InputData> data, short[] indices, int vertexCount) {
		Map<String, FloatVector> retval = new HashMap<String, FloatVector>();

		List<InputData> sources = new ArrayList<InputData>(data.values());

		int inputCount = -99;
		for(InputData id : sources) {
			inputCount = Math.max(inputCount, id.getOffset());
			retval.put(id.getSemantic(), new FloatVector(id.getFloatElements(vertexCount)));
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

		public int getFloatElements(int vertexCount) {
			return sType.numElements(vertexCount);
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
				int offset = idx * 3;
				float x = data.get(offset++);
				float y = data.get(offset++);
				float z = data.get(offset++);
				float len = (float) Math.sqrt(x * x + y * y + z * z);

				destination.add(x / len);
				destination.add(y / len);
				destination.add(z / len);
				break;
			}
		}
	}

	public List<BaseShape> getGeometries() {
		return geometries;
	}
}
