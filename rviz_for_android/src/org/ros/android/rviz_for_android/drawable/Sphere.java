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

import org.ros.android.view.visualization.Vertices;
import org.ros.android.view.visualization.shape.BaseShape;
import org.ros.rosjava_geometry.Transform;

import android.util.FloatMath;

public class Sphere extends BaseShape {

	FloatBuffer m_VertexData;
	FloatBuffer m_NormalData;

	float m_Scale;
	float m_Squash;
	float m_Radius;
	int m_Stacks, m_Slices, elementsToDraw;

	public Sphere() {
		init(17, 14, 0.98f, 1.0f);
		elementsToDraw = (m_Slices + 1) * 2 * (m_Stacks - 1) + 2;
	}

	// Sphere generation code from the book OpenGL ES 2.0 Programming in Android
	private void init(int stacks, int slices, float radius, float squash) {
		float[] vertexData;
		float[] normalData;

		int vIndex = 0; // vertex index
		int nIndex = 0; // normal index
		m_Scale = radius;
		m_Squash = squash;

		m_Stacks = stacks;
		m_Slices = slices;

		// vertices

		vertexData = new float[3 * ((m_Slices * 2 + 2) * m_Stacks)];

		// Normalize data

		normalData = new float[(3 * (m_Slices * 2 + 2) * m_Stacks)];

		int phiIdx, thetaIdx;

		// latitude

		for(phiIdx = 0; phiIdx < m_Stacks; phiIdx++) {
			// starts at -90 degrees (-1.57 radians) goes up to +90 degrees (or +1.57 radians)

			// the first circle

			float phi0 = (float) Math.PI * ((float) (phiIdx + 0) * (1.0f / (float) (m_Stacks)) - 0.5f);

			// the next, or second one.

			float phi1 = (float) Math.PI * ((float) (phiIdx + 1) * (1.0f / (float) (m_Stacks)) - 0.5f);

			float cosPhi0 = FloatMath.cos(phi0);
			float sinPhi0 = FloatMath.sin(phi0);
			float cosPhi1 = FloatMath.cos(phi1);
			float sinPhi1 = FloatMath.sin(phi1);

			float cosTheta, sinTheta;

			// longitude

			for(thetaIdx = 0; thetaIdx < m_Slices; thetaIdx++) {
				// increment along the longitude circle each "slice"

				float theta = (float) (2.0f * (float) Math.PI * ((float) thetaIdx) * (1.0 / (float) (m_Slices - 1)));
				cosTheta = FloatMath.cos(theta);
				sinTheta = FloatMath.sin(theta);

				// we're generating a vertical pair of points, such
				// as the first point of stack 0 and the first point of stack 1
				// above it. This is how TRIANGLE_STRIPS work,
				// taking a set of 4 vertices and essentially drawing two triangles
				// at a time. The first is v0-v1-v2 and the next is v2-v1-v3. Etc.

				// get x-y-z for the first vertex of stack

				vertexData[vIndex + 0] = m_Scale * cosPhi0 * cosTheta;
				vertexData[vIndex + 1] = m_Scale * (sinPhi0 * m_Squash);
				vertexData[vIndex + 2] = m_Scale * (cosPhi0 * sinTheta);

				vertexData[vIndex + 3] = m_Scale * cosPhi1 * cosTheta;
				vertexData[vIndex + 4] = m_Scale * (sinPhi1 * m_Squash);
				vertexData[vIndex + 5] = m_Scale * (cosPhi1 * sinTheta);

				// Normalize data pointers for lighting.
				normalData[nIndex + 0] = cosPhi0 * cosTheta;
				normalData[nIndex + 1] = sinPhi0;
				normalData[nIndex + 2] = cosPhi0 * sinTheta;

				normalData[nIndex + 3] = cosPhi1 * cosTheta;
				normalData[nIndex + 4] = sinPhi1;
				normalData[nIndex + 5] = cosPhi1 * sinTheta;

				vIndex += 2 * 3;
				nIndex += 2 * 3;
			}

			// create a degenerate triangle to connect stacks and maintain winding order

			vertexData[vIndex + 0] = vertexData[vIndex + 3] = vertexData[vIndex - 3];
			vertexData[vIndex + 1] = vertexData[vIndex + 4] = vertexData[vIndex - 2];
			vertexData[vIndex + 2] = vertexData[vIndex + 5] = vertexData[vIndex - 1];
		}

		m_VertexData = Vertices.toFloatBuffer(vertexData);
		m_NormalData = Vertices.toFloatBuffer(normalData);
	}

	private float radius;

	public void draw(GL10 gl, Transform transform, float radius) {
		this.radius = radius;
		super.setTransform(transform);
		super.draw(gl);
		gl.glColor4f(getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor().getAlpha());
		gl.glNormalPointer(GL10.GL_FLOAT, 0, m_NormalData);
		gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);

		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, m_VertexData);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, elementsToDraw);

		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
	}
	
	public void batchDraw(GL10 gl, Transform transform, float radius) {
		this.radius = radius;
		super.setTransform(transform);
		super.draw(gl);
		gl.glColor4f(getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor().getAlpha());
		gl.glNormalPointer(GL10.GL_FLOAT, 0, m_NormalData);

		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, m_VertexData);

		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, elementsToDraw);
	}

	@Override
	protected void scale(GL10 gl) {
		gl.glScalef(radius, radius, radius);
	}

}
