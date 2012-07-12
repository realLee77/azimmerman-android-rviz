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

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.shapes.BufferedTrianglesShape;
import org.ros.android.renderer.shapes.Color;
import org.ros.android.rviz_for_android.urdf.UrdfDrawable;
import org.ros.rosjava_geometry.Transform;

public class Cube extends BufferedTrianglesShape implements UrdfDrawable {
	private static final Color baseColor = new Color(.5f,.5f,0f,1f);
	
	private static final float cubeVertices[] = {
		 -0.5f, -0.5f,  0.5f,	//[0]
		 0.5f, -0.5f,  0.5f,	//[1]
		 -0.5f,  0.5f,  0.5f,	//[2]
		 

	    0.5f, -0.5f,  0.5f,		//[1]
	    0.5f,  0.5f,  0.5f,		//[3]
	    -0.5f,  0.5f,  0.5f,	//[2]
	    
	    
	    -0.5f,  0.5f,  0.5f,	//[2]
	    0.5f,  0.5f,  0.5f,		//[3]
	    0.5f,  0.5f, -0.5f,		//[7]
	    
 
	    0.5f,  0.5f,  0.5f,		//[3]
	    0.5f, -0.5f,  0.5f,		//[1]
	    0.5f,  0.5f, -0.5f,		//[7]
	    
    
	    0.5f,  0.5f, -0.5f,		//[7]
	    0.5f, -0.5f,  0.5f,		//[1]
	    0.5f, -0.5f, -0.5f,		//[5]
	    

	    0.5f, -0.5f,  0.5f,		//[1]	
	    -0.5f, -0.5f, -0.5f,	//[4]
	    0.5f, -0.5f, -0.5f,		//[5]
	    
	    
	    0.5f, -0.5f, -0.5f,		//[5]
	    -0.5f, -0.5f, -0.5f,	//[4]
	    0.5f,  0.5f, -0.5f,		//[7]
	    

	    -0.5f, -0.5f, -0.5f,	//[4]
	    -0.5f,  0.5f, -0.5f,	//[6]
	    0.5f,  0.5f, -0.5f,		//[7]
	    
	    
	    0.5f,  0.5f, -0.5f,		//[7]		
	    -0.5f,  0.5f, -0.5f,	//[6]
	    -0.5f,  0.5f,  0.5f,	//[2]
	    		
	    
	    -0.5f,  0.5f, -0.5f,	//[6]		
	    -0.5f, -0.5f, -0.5f,	//[4]
	    -0.5f,  0.5f,  0.5f,	//[2]	
	    
	    
	    -0.5f,  0.5f,  0.5f,	//[2]		
	    -0.5f, -0.5f, -0.5f,	//[4]
	    -0.5f, -0.5f,  0.5f,	//[0]
	    		
	    
	    -0.5f, -0.5f, -0.5f,	//[4]		
	    0.5f, -0.5f,  0.5f,		//[1]
	    -0.5f, -0.5f,  0.5f,	//[0]		
	    
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

	public Cube(Camera cam) {
		super(cam, cubeVertices, cubeNormals, baseColor);
		super.setProgram(GLSLProgram.FlatShaded());
		super.setTransform(Transform.newIdentityTransform());
	}

	private float[] scale;
	
	public void draw(GL10 gl, Transform transform, float[] scale) {
		this.setTransform(transform);
		this.scale = scale;
		super.draw(gl);
	}

	@Override
	protected void scale(Camera cam) {
		cam.scaleM(scale[0], scale[1], scale[2]);
	}
}
