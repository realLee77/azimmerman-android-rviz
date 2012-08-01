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
import org.ros.android.renderer.shapes.BaseShapeInterface;
import org.ros.android.renderer.shapes.Color;
import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

public class DoubleArrow implements BaseShapeInterface {
	private static final Transform A_TRANSFORM = new Transform(new Vector3(0.5,0,0), Quaternion.identity());
	private static final Transform BTOA_TRANSFORM = A_TRANSFORM.multiply(new Transform(new Vector3(-1,0,0), Quaternion.fromAxisAngle(Vector3.zAxis(), Math.PI)));
	
	private Arrow arrowA;
	private Arrow arrowB;

	public DoubleArrow(Camera cam) {
		arrowA = Arrow.newArrow(cam, .08f, .11f, .2f, .2f);
		arrowA.setTransform(A_TRANSFORM);
		arrowB = Arrow.newArrow(cam, .08f, .11f, .2f, .2f);
		arrowB.setTransform(BTOA_TRANSFORM);
	}

	@Override
	public void setProgram(GLSLProgram shader) {
	}

	@Override
	public void draw(GL10 glUnused) {
		arrowA.draw(glUnused);
		arrowB.draw(glUnused);
	}

	@Override
	public Color getColor() {
		return arrowA.getColor();
	}

	@Override
	public void setColor(Color color) {
		arrowA.setColor(color);
		arrowB.setColor(color);
	}

	@Override
	public Transform getTransform() {
		return arrowA.getTransform();
	}

	@Override
	public void setTransform(Transform pose) {
		arrowA.setTransform(pose.multiply(A_TRANSFORM));

		// TODO: This might be the wrong multiplication direction
		arrowB.setTransform(pose.multiply(BTOA_TRANSFORM));
	}

	@Override
	public void setSelected(boolean isSelected) {
	}

}
