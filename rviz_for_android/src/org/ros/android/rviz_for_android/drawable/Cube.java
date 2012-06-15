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

import org.ros.android.view.visualization.shape.Color;
import org.ros.android.view.visualization.shape.TriangleStripShape;
import org.ros.android.view.visualization.shape.TrianglesShape;
import org.ros.rosjava_geometry.Transform;

public class Cube extends TrianglesShape {
	private static final float cubeVertices[] = {
		 -1.0f, -1.0f,  1.0f,	//[0]
		 -1.0f,  1.0f,  1.0f,	//[2]
		 1.0f, -1.0f,  1.0f,	//[1]

	    1.0f, -1.0f,  1.0f,		//[1]
	    -1.0f,  1.0f,  1.0f,	//[2]
	    1.0f,  1.0f,  1.0f,		//[3]
	    
	    -1.0f,  1.0f,  1.0f,	//[2]
	    1.0f,  1.0f, -1.0f,		//[7]
	    1.0f,  1.0f,  1.0f,		//[3]
 
	    1.0f,  1.0f,  1.0f,		//[3]
	    1.0f,  1.0f, -1.0f,		//[7]
	    1.0f, -1.0f,  1.0f,		//[1]
    
	    1.0f,  1.0f, -1.0f,		//[7]
	    1.0f, -1.0f, -1.0f,		//[5]
	    1.0f, -1.0f,  1.0f,		//[1]

	    1.0f, -1.0f,  1.0f,		//[1]	
	    1.0f, -1.0f, -1.0f,		//[5]
	    -1.0f, -1.0f, -1.0f,	//[4]
	    
	    1.0f, -1.0f, -1.0f,		//[5]
	    1.0f,  1.0f, -1.0f,		//[7]
	    -1.0f, -1.0f, -1.0f,	//[4]

	    -1.0f, -1.0f, -1.0f,	//[4]
	    1.0f,  1.0f, -1.0f,		//[7]
	    -1.0f,  1.0f, -1.0f,	//[6]
	    
	    1.0f,  1.0f, -1.0f,		//[7]		
	    -1.0f,  1.0f,  1.0f,	//[2]
	    -1.0f,  1.0f, -1.0f,	//[6]		
	    
	    -1.0f,  1.0f, -1.0f,	//[6]		
	    -1.0f,  1.0f,  1.0f,	//[2]	
	    -1.0f, -1.0f, -1.0f,	//[4]
	    
	    -1.0f,  1.0f,  1.0f,	//[2]		
	    -1.0f, -1.0f,  1.0f,	//[0]
	    -1.0f, -1.0f, -1.0f,	//[4]		
	    
	    -1.0f, -1.0f, -1.0f,	//[4]		
	    -1.0f, -1.0f,  1.0f,	//[0]		
	    1.0f, -1.0f,  1.0f,		//[1]
	};

	private static final float cubeNormals[] = {
		0f,0f,1f,0f,0f,1f,0f,0f,1f,
		0f,0f,1f,0f,0f,1f,0f,0f,1f,
		0f,1f,0f,0f,1f,0f,0f,1f,0f,
		1f,0f,0f,1f,0f,0f,1f,0f,0f,
		1f,0f,0f,1f,0f,0f,1f,0f,0f,
		0f,-1f,0f,0f,-1f,0f,0f,-1f,0f,
		0f,0f,-1f,0f,0f,-1f,0f,0f,-1f,
		0f,0f,-1f,0f,0f,-1f,0f,0f,-1f,
		0f,1f,0f,0f,1f,0f,0f,1f,0f,
		-1f,0f,0f,-1f,0f,0f,-1f,0f,0f,
		-1f,0f,0f,-1f,0f,0f,-1f,0f,0f,
		0f,-1f,0f,0f,-1f,0f,0f,-1f,0f
	};

	public Cube(Color color) {
		super(cubeVertices, cubeNormals, color);
	}

	private float[] scale;
	
	public void draw(GL10 gl, Transform transform, float[] scale) {
		this.setTransform(transform);
		this.scale = scale;
		super.draw(gl);
	}

	@Override
	protected void scale(GL10 gl) {
		gl.glScalef(scale[0], scale[1], scale[2]);
	}
}
