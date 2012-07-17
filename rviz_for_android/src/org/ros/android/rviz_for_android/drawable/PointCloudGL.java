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

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.Utility;
import org.ros.android.renderer.Vertices;
import org.ros.android.renderer.shapes.BaseShape;
import org.ros.android.rviz_for_android.drawable.GLSLProgram.ShaderVal;

import sensor_msgs.ChannelFloat32;
import android.opengl.GLES20;

public class PointCloudGL extends BaseShape {

	// Color mode and shaders
	public static enum ColorMode {
		FLAT_COLOR("Flat color", 0,0), GRADIENT_X("Gradient X", 1,1,0), GRADIENT_Y("Gradient Y", 1,2,1), GRADIENT_Z("Gradient Z", 1,3,2), CHANNEL("Channel",2,4);
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

	private static final String vChannelShader =
			"attribute vec2 aChannel;	\n" +
			"attribute vec4 aPosition;	\n" +
			"uniform mat4 uMvp;			\n" +
			"uniform float minVal;      \n" +
			"uniform float maxVal;      \n" +
			"varying vec4 vColor;		\n" + 
			"void main() {				\n" +
		    "	gl_Position = uMvp * aPosition;												\n"+
		    "	float mixlevel = max(min((aChannel.x - minVal)/(maxVal-minVal),1.0),0.0);	\n" +
		    "	vColor = mix(vec4(0.0, 0.0, 0.0, 1.0), vec4(1.0,1.0,1.0,1.0), mixlevel);	\n"+
		    "	gl_PointSize = 3.0;															\n"+
		    "}";
	private static final String hToRGB = 
		    "vec4 hToRGB(float h) {		\n" +
		    "   float hs = 2.0*h;		\n" +
		    "	float hi = floor(hs);				\n"+
		    "   float f = (hs) - floor(hs);				\n"+			
		    "	float q = 1.0 - f;							\n"+
		    "	if (hi <= 0.0)								\n"+
		    "		return vec4(1.0, f, 0.0, 1.0);			\n"+
		    "	if (hi <= 1.0)								\n"+
		    "		return vec4(q, 1.0, 0.0, 1.0);			\n"+
		    "	if (hi <= 2.0)								\n"+
		    "		return vec4(0.0, 1.0, f, 1.0);			\n"+
		    "	if (hi <= 3.0)								\n"+
		    "		return vec4(0.0, q, 1.0, 1.0);			\n"+
		    "	if (hi <= 4.0)								\n"+
		    "		return vec4(f, 0.0, 1.0, 1.0);			\n"+
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
	private Buffer selectedChannelBuffer;
	private float[][] channels;
	private float[] channelMin;
	private float[] channelMax;
	private int channelSelected = 0;
	private List<String> channelNames = new ArrayList<String>();
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
		
		programs[ColorMode.CHANNEL.shaderArrayPos] = new GLSLProgram(vChannelShader, fShader);
		programs[ColorMode.CHANNEL.shaderArrayPos].setAttributeName(ShaderVal.POSITION, "aPosition");
		programs[ColorMode.CHANNEL.shaderArrayPos].setAttributeName(ShaderVal.MVP_MATRIX, "uMvp");
		programs[ColorMode.CHANNEL.shaderArrayPos].setAttributeName(ShaderVal.ATTRIB_COLOR, "aChannel");
		programs[ColorMode.CHANNEL.shaderArrayPos].setAttributeName(ShaderVal.EXTRA, "minVal");
		programs[ColorMode.CHANNEL.shaderArrayPos].setAttributeName(ShaderVal.EXTRA_2, "maxVal");
				
		super.setProgram(programs[mode.shaderArrayPos]);
	}
	
	@Override
	public void draw(GL10 glUnused) {
		if(draw) {
			super.draw(glUnused);
			
			calcMVP();
			
			if(mode == ColorMode.CHANNEL) {
				GLES20.glEnableVertexAttribArray(ShaderVal.ATTRIB_COLOR.loc);
				GLES20.glVertexAttribPointer(ShaderVal.ATTRIB_COLOR.loc, 1, GLES20.GL_FLOAT, false, 0, selectedChannelBuffer);
				GLES20.glUniform1f(getUniform(ShaderVal.EXTRA), channelMin[channelSelected]);
				GLES20.glUniform1f(getUniform(ShaderVal.EXTRA_2), channelMax[channelSelected]);
			} else {
				GLES20.glUniform4f(getUniform(ShaderVal.UNIFORM_COLOR), getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor().getAlpha());
				GLES20.glUniform1i(getUniform(ShaderVal.EXTRA), mode.extraInfo);
			}
			
			GLES20.glUniformMatrix4fv(getUniform(ShaderVal.MVP_MATRIX), 1, false, MVP, 0);
			GLES20.glEnableVertexAttribArray(ShaderVal.POSITION.loc);
			GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, points);
			GLES20.glDrawArrays(GLES20.GL_POINTS, 0, cloudSize);
		}
	}

	public void setData(float[] points, List<sensor_msgs.ChannelFloat32> channels) {
		if(points == null || points.length == 0) {
			draw = false;
			return;
		}
		
		// Determine which channels of data are available
		channelNames.clear();
		if(channels != null && channels.size() > 0) {
			this.channels = new float[channels.size()][points.length/3];
			channelMin = new float[channels.size()];
			channelMax = new float[channels.size()];
			
			int idx = 0;
			for(ChannelFloat32 cf : channels) {
				channelNames.add(cf.getName());
				this.channels[idx] = cf.getValues();
				channelMin[idx] = Utility.arrayMin(this.channels[idx]);
				channelMax[idx] = Utility.arrayMax(this.channels[idx]);
				idx++;
			}
			
			selectedChannelBuffer = Vertices.toFloatBuffer(this.channels[channelSelected]);
		} else {
			// If no channels are available
			this.channels = new float[0][0];
			mode = ColorMode.FLAT_COLOR;
			channelSelected = 0;
		}
		
		if(channelSelected > this.channels.length) {
			mode = ColorMode.FLAT_COLOR;
			channelSelected = 0;
		}
		
		this.points = Vertices.toFloatBuffer(points);
		cloudSize = points.length / 3;
		draw = (cloudSize > 0);
	}
	
	public void setChannelSelection(int selected) {
		channelSelected = selected;
		selectedChannelBuffer = Vertices.toFloatBuffer(channels[channelSelected]);
	}
	
	public void setColorMode(int selected) {
		this.mode = ColorMode.values()[selected];
		if(programs[mode.shaderArrayPos] != null)
			super.setProgram(programs[mode.shaderArrayPos]);
		
		if(channelSelected > channels.length)
			channelSelected = 0;
		
		if(mode == ColorMode.CHANNEL && channels.length == 0) {
			this.mode = ColorMode.FLAT_COLOR;
			super.setProgram(programs[mode.shaderArrayPos]);
		}
	}
	
	public ColorMode getColorMode() {
		return mode;
	}
	
	public List<String> getChannelNames() {
		return channelNames;
	}
}
