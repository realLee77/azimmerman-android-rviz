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

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.view.visualization.shape.TexturedTrianglesShape;

import android.opengl.ETC1Util.ETC1Texture;

public class Plane extends TexturedTrianglesShape {

	private static final float planeV[] = {
		0f,1f,0f,
		0f,0f,0f,
		1f,0f,0f,
		
		0f,1f,0f,
		1f,0f,0f,
		1f,1f,0f
	};
	
	private static final float planeN[] = {
		0f,0f,1f,
		0f,0f,1f,	
		0f,0f,1f,
		
		0f,0f,1f,
		0f,0f,1f,
		0f,0f,1f
	};
	
	private static final float planeUV[] = {
		0f,0f,
		0f,1f,
		1f,1f,
		
		0f,0f,
		1f,1f,
		1f,0f
	};

	public Plane(ETC1Texture tex) {
		super(planeV, planeN, planeUV, tex);
	}

	private float xScale = 1f;
	private float yScale = 1f;
	public void setScale(float xScale, float yScale) {
		this.xScale = xScale;
		this.yScale = yScale;
	}

	@Override
	public void draw(GL10 gl) {
	    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
	    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
	    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
	    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
		super.draw(gl);
	}

	@Override
	protected void scale(GL10 gl) {
		gl.glScalef(xScale, yScale, 1f);
	}

}
