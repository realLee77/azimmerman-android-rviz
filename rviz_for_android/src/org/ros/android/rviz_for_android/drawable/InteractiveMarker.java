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

import java.util.LinkedList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.rviz_for_android.urdf.MeshFileDownloader;
import org.ros.namespace.GraphName;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Transform;

import android.util.Log;

public class InteractiveMarker {

	private List<InteractiveMarkerControl> controls = new LinkedList<InteractiveMarkerControl>();

	private float scale;

	private FrameTransformTree ftt;
	private GraphName frame;
	private Transform transform;
	private Camera cam;

	public InteractiveMarker(visualization_msgs.InteractiveMarker msg, Camera cam, MeshFileDownloader mfd, FrameTransformTree ftt) {
		Log.d("InteractiveMarker", "Created interactive marker");

		transform = Transform.fromPoseMessage(msg.getPose());
		frame = GraphName.of(msg.getHeader().getFrameId());

		// Create controls
		for(visualization_msgs.InteractiveMarkerControl control : msg.getControls())
			controls.add(new InteractiveMarkerControl(control, transform.getRotation(), cam, mfd, ftt));

		// Catch invalid scale
		scale = msg.getScale();
		if(scale <= 0)
			scale = 1;

		this.cam = cam;
		this.ftt = ftt;
	}

	public void draw(GL10 glUnused) {
		cam.pushM();
		cam.scaleM(scale, scale, scale);
		cam.applyTransform(ftt.newTransformIfPossible(cam.getFixedFrame(), frame));
		cam.applyTransform(transform);
		for(InteractiveMarkerControl control : controls)
			control.draw(glUnused);
		cam.popM();
	}

	public void update(visualization_msgs.InteractiveMarkerUpdate msg) {
		// TODO: Update each control based on the update message
	}
}
