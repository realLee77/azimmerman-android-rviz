/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.android.renderer.shapes;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.Vertices;
import org.ros.android.rviz_for_android.drawable.GLSLProgram;
import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

public class TriangleFanShape extends BaseShape {

	private final FloatBuffer vertices;

	/**
	 * @param vertices
	 *            an array of vertices as defined by OpenGL's GL_TRIANGLE_FAN method
	 * @param color
	 *            the {@link Color} of the {@link Shape}
	 */
	public TriangleFanShape(float[] vertices, Color color) {
		this.vertices = Vertices.toFloatBuffer(vertices);
		setColor(color);
		setTransform(new Transform(new Vector3(0, 0, 0), new Quaternion(0, 0, 0, 1)));
		uniformHandles = shader.getUniformHandles();
	}

	@Override
	public void draw(GL10 glUnused, Camera cam) {
		super.draw(glUnused, cam);
		
		if(!shader.isCompiled()) {
			shader.compile(glUnused);
		}
		
		glUnused.glDisable(GL10.GL_CULL_FACE);
		glUnused.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		glUnused.glVertexPointer(3, GL10.GL_FLOAT, 0, vertices);
		glUnused.glColor4f(getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor().getAlpha());
		glUnused.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, vertices.limit() / 3);
		glUnused.glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}
}
