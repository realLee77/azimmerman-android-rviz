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

public class PointCloud extends BaseShape {

	// Color mode and shaders
	public static enum ColorMode {
		FLAT_COLOR("Flat color", 0), GRADIENT_X("Gradient X", 1), GRADIENT_Y("Gradient Y", 2), GRADIENT_Z("Gradient Z", 3);
		private String name;
		public int pos;

		ColorMode(String name, int pos) {
			this.name = name;
			this.pos = pos;
		}

		@Override
		public String toString() {
			return name;
		}
	}
	private ColorMode mode = ColorMode.FLAT_COLOR;
	private static GLSLProgram[] programs;
	static {
		int size = 0;
		for(ColorMode cm : ColorMode.values())
			size = Math.max(size, cm.pos);
		programs = new GLSLProgram[size+1];
	}

	private static final String vFlatColorShader = 
			"precision mediump float; 	\n" +
			"uniform mat4 uMvp;			\n" +
			"attribute vec4 aPosition;	\n" +
			"varying vec4 vColor;		\n" +
			"void main() {				\n" +
		    "	gl_Position = uMvp * vec4(aPosition.xyz, 1.0);	\n"+
		    "	vColor = vec4(0.5,0.6,0.9,1.0);					\n"+
		    "	gl_PointSize = 2.0;								\n"+
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
	
	public PointCloud(Camera cam) {
		super(cam);
		programs[ColorMode.FLAT_COLOR.pos] = new GLSLProgram(vFlatColorShader, fShader);
		super.setProgram(programs[0]);
	}
	
	@Override
	public void draw(GL10 glUnused) {
		super.draw(glUnused);
		
		GLES20.glUniform4f(getUniform(ShaderVal.UNIFORM_COLOR), getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor().getAlpha());
		
		GLES20.glEnableVertexAttribArray(ShaderVal.POSITION.loc);
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, points);
		GLES20.glDrawArrays(GLES20.GL_POINTS, 0, cloudSize);
	}

	public void setData(float[] points) {
		this.points = Vertices.toFloatBuffer(points);
		cloudSize = points.length / 3;
	}
	
	public void setColorMode(ColorMode cm) {
		this.mode = cm;
		if(programs[cm.pos] != null)
			super.setProgram(programs[cm.pos]);
	}
	
	public ColorMode getColorMode() {
		return mode;
	}
}
