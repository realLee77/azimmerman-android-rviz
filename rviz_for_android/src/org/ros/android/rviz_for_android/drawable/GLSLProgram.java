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
package org.ros.android.rviz_for_android.drawable;

import java.util.EnumMap;
import java.util.Map;

import android.opengl.GLES20;
import android.util.Log;

public class GLSLProgram {

	private String vertexProgram;
	private String fragmentProgram;
	private int programID = 0;
	private int fShader = 0;
	private int vShader = 0;
	
	public static enum ShaderVal {Position, Color, TexCoord, MVP, Time};
	private Map<ShaderVal, Integer> shaderValLocs = new EnumMap<ShaderVal, Integer>(ShaderVal.class);
	private Map<ShaderVal, String> shaderValNames = new EnumMap<ShaderVal, String>(ShaderVal.class);;

	public GLSLProgram(String vertex, String fragment) {
		if(vertex == null || fragment == null)
			throw new IllegalArgumentException("Vertex/fragment shader program cannot be null!");

		programID = GLES20.glCreateProgram();

		this.vertexProgram = vertex;
		this.fragmentProgram = fragment;
	}

	public boolean compile() {
		// Check that attributes are in place
		if(shaderValNames.isEmpty() || !shaderValNames.containsKey(ShaderVal.MVP))
			throw new IllegalArgumentException("Must program shader value names");
		
		// Load and compile
		vShader = loadShader(vertexProgram, GLES20.GL_VERTEX_SHADER);
		fShader = loadShader(fragmentProgram, GLES20.GL_FRAGMENT_SHADER);

		if(vShader == 0 || fShader == 0) {
			Log.e("GLSL", "Unable to compile shaders!");
			return false;
		}

		GLES20.glAttachShader(programID, vShader);
		GLES20.glAttachShader(programID, fShader);

		// Link
		int[] linkStatus = new int[1];
		GLES20.glLinkProgram(programID);
		GLES20.glGetProgramiv(programID, GLES20.GL_LINK_STATUS, linkStatus, 0);

		if(linkStatus[0] != GLES20.GL_TRUE) {
			Log.e("GLSL", "Unable to link program:");
			Log.e("GLSL", GLES20.glGetProgramInfoLog(programID));
			cleanup();
			return false;
		}

		// Attach to attribute and uniforms
		for(ShaderVal s : shaderValNames.keySet()) {
			int location = GLES20.glGetAttribLocation(programID, shaderValNames.get(s));
			shaderValLocs.put(s, location);
		}
		
		return true;
	}
	
	public void use() {
		GLES20.glUseProgram(programID);
	}
	
	public void setAttributeName(ShaderVal val, String name) {
		shaderValNames.put(val, name);
	}
	
	public int getAttributeLocation(ShaderVal toGet) {
		if(!shaderValLocs.containsKey(toGet)) {
			return 0;
		}
		return shaderValLocs.get(toGet);
	}

	/* load a Vertex or Fragment shader */
	private int loadShader(String source, int shaderType) {
		int shader = GLES20.glCreateShader(shaderType);
		if(shader != 0) {
			GLES20.glShaderSource(shader, source);
			GLES20.glCompileShader(shader);
			int[] compiled = new int[1];
			GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
			if(compiled[0] == 0) {
				Log.e("GLSL", "Could not compile shader " + shaderType + ":");
				Log.e("GLSL", GLES20.glGetShaderInfoLog(shader));
				GLES20.glDeleteShader(shader);
				shader = 0;
			}
		}
		Log.i("GLSL", "shader compiled: " + shader);
		return shader;
	}

	public void cleanup() {
		if(programID > 0)
			GLES20.glDeleteProgram(programID);
		if(vShader > 0)
			GLES20.glDeleteShader(vShader);
		if(fShader > 0)
			GLES20.glDeleteShader(fShader);

		fShader = 0;
		vShader = 0;
		programID = 0;
	}
}
