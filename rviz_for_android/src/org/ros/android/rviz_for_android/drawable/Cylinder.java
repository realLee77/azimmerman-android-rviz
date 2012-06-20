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
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.view.visualization.OpenGlTransform;
import org.ros.android.view.visualization.Vertices;
import org.ros.android.view.visualization.shape.Color;
import org.ros.android.view.visualization.shape.Shape;
import org.ros.rosjava_geometry.Transform;

public class Cylinder implements Shape {

	private FloatBuffer sideVertices;
	private FloatBuffer sideNormals;

	private FloatBuffer topVertices;
	private FloatBuffer topNormals;
	
	private FloatBuffer bottomVertices;
	private FloatBuffer bottomNormals;

	private int stripTriangleCount = 0;
	private int fanTriangleCount = 0;

	private Color color = new Color(1, 1, 0, 1);

	public Cylinder() {
		initGeometry(14);
	}

	private void initGeometry(int sides) {
		double dTheta = (2.0 * Math.PI) / sides;

		float[] sideVertices = new float[(sides + 1) * 6];
		float[] sideNormals = new float[(sides + 1) * 6];

		int sideVidx = 0;
		int sideNidx = 0;

		float[] topVertices = new float[(sides + 2) * 3];
		float[] topNormals = new float[(sides + 2) * 3];
		float[] bottomVertices = new float[(sides + 2) * 3];
		float[] bottomNormals = new float[(sides + 2) * 3];

		int capVidx = 3;
		int capNidx = 3;

		topVertices[0] = 0f;
		topVertices[1] = 0f;
		topVertices[2] = .5f;
		topNormals[0] = 0f;
		topNormals[1] = 0f;
		topNormals[2] = 1f;
		bottomVertices[0] = 0f;
		bottomVertices[1] = 0f;
		bottomVertices[2] = -.5f;
		bottomNormals[0] = 0f;
		bottomNormals[1] = 0f;
		bottomNormals[2] = -1f;
		
		for(double theta = 0; theta <= (2.0 * Math.PI); theta += dTheta) {
			sideVertices[sideVidx++] = (float) Math.cos(theta); // X
			sideVertices[sideVidx++] = (float) Math.sin(theta); // Y
			sideVertices[sideVidx++] = 0.5f; // Z

			sideVertices[sideVidx++] = (float) Math.cos(theta); // X
			sideVertices[sideVidx++] = (float) Math.sin(theta); // Y
			sideVertices[sideVidx++] = -0.5f; // Z

			sideNormals[sideNidx++] = (float) Math.cos(theta); // X
			sideNormals[sideNidx++] = (float) Math.sin(theta); // Y
			sideNormals[sideNidx++] = 0f; // Z

			sideNormals[sideNidx++] = (float) Math.cos(theta); // X
			sideNormals[sideNidx++] = (float) Math.sin(theta); // Y
			sideNormals[sideNidx++] = 0f; // Z
			
			
			// X
			topVertices[capVidx] = (float) Math.cos(theta);
			bottomVertices[capVidx++] = (float) Math.sin(theta+(dTheta/2.0));
			// Y
			topVertices[capVidx] = (float) Math.sin(theta);
			bottomVertices[capVidx++] = (float) Math.cos(theta+(dTheta/2.0));
			// Z
			topVertices[capVidx] = 0.5f;
			bottomVertices[capVidx++] = -0.5f;
			
			// Normals
			topNormals[capNidx] = 0f;
			bottomNormals[capNidx++] = 0f;
			topNormals[capNidx] = 0f;
			bottomNormals[capNidx++] = 0f;
			topNormals[capNidx] = 1f;
			bottomNormals[capNidx++] = -1f;
		}
		stripTriangleCount = sideVertices.length / 3;
		fanTriangleCount = sides + 2;
		this.sideVertices = Vertices.toFloatBuffer(sideVertices);
		this.sideNormals = Vertices.toFloatBuffer(sideNormals);
		this.topVertices = Vertices.toFloatBuffer(topVertices);
		this.topNormals = Vertices.toFloatBuffer(topNormals);
		this.bottomVertices = Vertices.toFloatBuffer(bottomVertices);
		this.bottomNormals = Vertices.toFloatBuffer(bottomNormals);
	}

	@Override
	public void draw(GL10 gl) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setColor(Color color) {
		this.color = color;
	}

	@Override
	public Color getColor() {
		return color;
	}

	@Override
	public void setTransform(Transform transform) {
		// TODO Auto-generated method stub
	}

	@Override
	public Transform getTransform() {
		return null;
	}

	public void draw(GL10 gl, Transform transform, float length, float radius) {
		gl.glPushMatrix();
		OpenGlTransform.apply(gl, transform);
		gl.glScalef(radius, radius, length);
		gl.glColor4f(getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor().getAlpha());

		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
		
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, sideVertices);
		gl.glNormalPointer(GL10.GL_FLOAT, 0, sideNormals);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, stripTriangleCount);
		
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, topVertices);
		gl.glNormalPointer(GL10.GL_FLOAT, 0, topNormals);
		gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, fanTriangleCount);
		
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, bottomVertices);
		gl.glNormalPointer(GL10.GL_FLOAT, 0, topNormals);
		gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, fanTriangleCount);
		
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
		gl.glPopMatrix();
	}
}
