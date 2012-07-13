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

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.Vertices;
import org.ros.android.renderer.shapes.BaseShape;
import org.ros.android.rviz_for_android.drawable.GLSLProgram.ShaderVal;

import android.opengl.GLES20;

public class PointCloudGL extends BaseShape {

	// Color mode and shaders
	public static enum ColorMode {
		FLAT_COLOR("Flat color", 0,0), GRADIENT_X("Gradient X", 1,1,0), GRADIENT_Y("Gradient Y", 1,2,1), GRADIENT_Z("Gradient Z", 1,3,2);
		private String name;
		public int shaderArrayPos;
		public int nameArrayPos;
		public int extraInfo = -1;

		ColorMode(String name, int shaderPos, int namePos) {
			this.name = name;
			this.shaderArrayPos = shaderPos;
			this.nameArrayPos = namePos;
		}

		ColorMode(String name, int shaderPos, int namePos, int extraInfo) {
			this.name = name;
			this.shaderArrayPos = shaderPos;
			this.nameArrayPos = namePos;
			this.extraInfo = extraInfo;
		}
		
		@Override
		public String toString() {
			return name;
		}
	}
	private ColorMode mode = ColorMode.FLAT_COLOR;
	public static final String[] colorModeNames = new String[ColorMode.values().length]; 
	private static GLSLProgram[] programs;
	static {
		int size = 0;
		for(ColorMode cm : ColorMode.values()) {
			colorModeNames[cm.nameArrayPos] = cm.toString();
			size = Math.max(size, cm.shaderArrayPos);
		}
		programs = new GLSLProgram[size+1];	
	}

	private static final String hToRGB = 
		    "vec4 hToRGB(float h) {		\n" +
		    "   float hs = 2.0*h;		\n" +
		    "	float hi = floor(hs);				\n"+
		    "   float f = (hs) - floor(hs);				\n"+			
		    "	float q = 1.0 - f;								\n"+
		    "	if (hi <= 0.0)										\n"+
		    "		return vec4(1.0, f, 0.0, 1.0);					\n"+
		    "	if (hi <= 1.0)								\n"+
		    "		return vec4(q, 1.0, 0.0, 1.0);					\n"+
		    "	if (hi <= 2.0)								\n"+
		    "		return vec4(0.0, 1.0, f, 1.0);					\n"+
		    "	if (hi <= 3.0)							\n"+
		    "		return vec4(0.0, q, 1.0, 1.0);					\n"+
		    "	if (hi <= 4.0)									\n"+
		    "		return vec4(f, 0.0, 1.0, 1.0);					\n"+
		    "	else										\n"+
		    "		return vec4(1.0, 0.0, q, 1.0);					\n"+
		    "}\n";
	private static final String vFlatColorShader = 
			"precision mediump float; 	\n" +
			"uniform mat4 uMvp;			\n" +
			"uniform vec4 uColor;		\n" +
			"attribute vec4 aPosition;	\n" +
			"varying vec4 vColor;		\n" +
			"void main() {				\n" +
		    "	gl_Position = uMvp * aPosition;	\n"+
		    "	vColor = uColor;								\n"+
		    "	gl_PointSize = 3.0;								\n"+
		    "}";
	private static final String vGradientShader = 
			"precision mediump float; 	\n" +
			"uniform mat4 uMvp;			\n" +
			"uniform vec4 uColor;		\n" +
			"uniform int uDirSelect;		\n" +
			"attribute vec4 aPosition;	\n" +
			"varying vec4 vColor;		\n" + hToRGB + 
			"void main() {				\n" +
		    "	gl_Position = uMvp * aPosition;					\n"+
		    "	vColor = hToRGB(mod(abs(aPosition[uDirSelect]),3.0));			\n"+
		    "	gl_PointSize = 3.0;								\n"+
		    "}";					
	private static final String fShader = 
			"precision mediump float; 	\n" +
			"varying vec4 vColor;		\n" +
			"void main()				\n" + 
			"{							\n" +
		    "	gl_FragColor = vColor;	\n" +
			"}";
	
	// Point cloud data
	private FloatBuffer points;
	private int cloudSize;
	private boolean draw = false;
	
	public PointCloudGL(Camera cam) {
		super(cam);
		programs[ColorMode.FLAT_COLOR.shaderArrayPos] = new GLSLProgram(vFlatColorShader, fShader);
		programs[ColorMode.FLAT_COLOR.shaderArrayPos].setAttributeName(ShaderVal.POSITION, "aPosition");
		programs[ColorMode.FLAT_COLOR.shaderArrayPos].setAttributeName(ShaderVal.UNIFORM_COLOR, "uColor");
		programs[ColorMode.FLAT_COLOR.shaderArrayPos].setAttributeName(ShaderVal.MVP_MATRIX, "uMvp");
		
		programs[ColorMode.GRADIENT_X.shaderArrayPos] = new GLSLProgram(vGradientShader, fShader);
		programs[ColorMode.GRADIENT_X.shaderArrayPos].setAttributeName(ShaderVal.POSITION, "aPosition");
		programs[ColorMode.GRADIENT_X.shaderArrayPos].setAttributeName(ShaderVal.UNIFORM_COLOR, "uColor");
		programs[ColorMode.GRADIENT_X.shaderArrayPos].setAttributeName(ShaderVal.MVP_MATRIX, "uMvp");
		programs[ColorMode.GRADIENT_X.shaderArrayPos].setAttributeName(ShaderVal.EXTRA, "uDirSelect");
		super.setProgram(programs[mode.shaderArrayPos]);
	}
	
	@Override
	public void draw(GL10 glUnused) {
		if(draw) {
			super.draw(glUnused);
			
			calcMVP();
			GLES20.glUniform4f(getUniform(ShaderVal.UNIFORM_COLOR), getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor().getAlpha());
			GLES20.glUniformMatrix4fv(getUniform(ShaderVal.MVP_MATRIX), 1, false, MVP, 0);
			GLES20.glUniform1i(getUniform(ShaderVal.EXTRA), mode.extraInfo);
			
			GLES20.glEnableVertexAttribArray(ShaderVal.POSITION.loc);
			GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, points);
			GLES20.glDrawArrays(GLES20.GL_POINTS, 0, cloudSize);
		}
	}

	public void setData(float[] points) {
		if(points == null) {
			draw = false;
			return;
		}
		this.points = Vertices.toFloatBuffer(points);
		cloudSize = points.length / 3;
		draw = (cloudSize > 0);
	}
	
	public void setColorMode(int selected) {
		this.mode = ColorMode.values()[selected];
		if(programs[mode.shaderArrayPos] != null)
			super.setProgram(programs[mode.shaderArrayPos]);
	}
	
	public ColorMode getColorMode() {
		return mode;
	}
}
