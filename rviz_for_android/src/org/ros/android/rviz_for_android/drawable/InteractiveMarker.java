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


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.Utility;
import org.ros.android.rviz_for_android.layers.InteractiveMarkerLayer.MarkerFeedbackPublisher;
import org.ros.android.rviz_for_android.urdf.MeshFileDownloader;
import org.ros.namespace.GraphName;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import visualization_msgs.InteractiveMarkerPose;
import visualization_msgs.MenuEntry;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.util.FloatMath;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

public class InteractiveMarker {

	private List<InteractiveMarkerControl> parentControls = new LinkedList<InteractiveMarkerControl>();
	private List<InteractiveMarkerControl> fixedControls = new LinkedList<InteractiveMarkerControl>();

	private float scale;
	private String name;
	
	private FrameTransformTree ftt;
	private GraphName frame;
	private String frameString;
	private Transform transform;
	
	private Camera cam;
	private final MeshFileDownloader mfd;
	private final MarkerFeedbackPublisher upperPublisher;
	
	public interface InteractiveMarkerFeedbackPublisher {
		public void publishFeedback(InteractiveMarkerControl control, byte type);
	}
	private InteractiveMarkerFeedbackPublisher lowerPublisher = new InteractiveMarkerFeedbackPublisher() {
		@Override
		public void publishFeedback(InteractiveMarkerControl control, byte type) {
			if(upperPublisher != null)
				upperPublisher.publishFeedback(InteractiveMarker.this, control, type);
		}
	};

	public InteractiveMarker(visualization_msgs.InteractiveMarker msg, Camera cam, MeshFileDownloader mfd, FrameTransformTree ftt, MarkerFeedbackPublisher pub) {
		this.mfd = mfd;
		this.upperPublisher = pub;
		Log.d("InteractiveMarker", "Created interactive marker");

		name = msg.getName();
		transform = Transform.fromPoseMessage(msg.getPose());

		frameString = msg.getHeader().getFrameId();
		frame = GraphName.of(frameString);
		
		// Create controls
		for(visualization_msgs.InteractiveMarkerControl control : msg.getControls()) {
			List<InteractiveMarkerControl> addList = (control.getOrientationMode() == visualization_msgs.InteractiveMarkerControl.FIXED) ? fixedControls : parentControls;
			addList.add(new InteractiveMarkerControl(control, cam, mfd, ftt, lowerPublisher));
		}

		// Construct menu
		buildMenuTree(msg.getMenuEntries());
		
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
		cam.applyTransform(ftt.newTransformIfPossible(frame, cam.getFixedFrame()));
		cam.applyTransform(transform);

		// Draw controls which rotate with the marker
		for(InteractiveMarkerControl control : parentControls)
			control.draw(glUnused);
		
		// Undo marker rotation
		rotateAxisAngle(glUnused, transform.getRotation().invert());
		
		// Draw controls which don't rotate with the marker
		for(InteractiveMarkerControl control : fixedControls)
			control.draw(glUnused);		

		cam.popM();
	}
	
	private void rotateAxisAngle(GL10 glUnused, Quaternion q) {
		// Use axis angle or matrix transformation??
		float angle = (float) Math.toDegrees(2*Math.acos(q.getW()));
		float l = FloatMath.sqrt(1 - (float)(q.getW()*q.getW()));
		float x = (float) (q.getX()/l);
		float y = (float) (q.getY()/l);
		float z = (float) (q.getZ()/l);
		cam.rotateM(angle, x, y, z);
	}

	public void update(InteractiveMarkerPose p) {
		// Trying to do this without causing a GC
		Quaternion q = Utility.correctQuaternion(transform.getRotation());
		q.setX(p.getPose().getOrientation().getX());
		q.setY(p.getPose().getOrientation().getY());
		q.setZ(p.getPose().getOrientation().getZ());
		q.setW(p.getPose().getOrientation().getW());
		
		Vector3 v = transform.getTranslation();
		v.setX(p.getPose().getPosition().getX());
		v.setY(p.getPose().getPosition().getY());
		v.setZ(p.getPose().getPosition().getZ());
		
		transform = Transform.fromPoseMessage(p.getPose());
		if(!frameString.equals(p.getHeader().getFrameId())) {
			frameString = p.getHeader().getFrameId();
			frame = GraphName.of(frameString);
		}
			
	}

	
	private class MenuItem {
		private String text;
		private int id;
		
		public MenuItem(MenuEntry entry) {
			this.text = entry.getTitle();
			this.id = entry.getId();
		}

		public String getText() {
			return text;
		}

		public int getId() {
			return id;
		}
	}
	
	private SparseArray<ArrayList<MenuItem>> menuItems = new SparseArray<ArrayList<MenuItem>>();
	
	private void buildMenuTree(List<MenuEntry> entries) {
		for(MenuEntry me : entries) {
			int parentId = me.getParentId();
			
			MenuItem mi = new MenuItem(me);
			ArrayList<MenuItem> itemList = menuItems.get(parentId, new ArrayList<MenuItem>());
//			if(!menuItems.(parentId))
//				itemList = new ArrayList<MenuItem>();
//			else
//				itemList = menuItems.get(parentId);
			itemList.add(mi);
			menuItems.put(parentId, itemList);
		}
	}

	public String getName() {
		return name;
	}
	
	private int menuSelection = 0;
	public void showMenu() {
		showMenu(0);
	}
	
	private void showMenu(int parent) {	
		List<MenuItem> children = menuItems.get(parent);
		if(children != null && !children.isEmpty()) {
			showMenuDialog(children);
		} else {
			menuSelection = parent;
			Toast.makeText(mfd.getContext(), "You selected " + parent, Toast.LENGTH_LONG).show();
		}
	}
	private void showMenuDialog(List<MenuItem> items) {
		int count = items.size();
		CharSequence[] names = new CharSequence[count];
		final int[] ids = new int[count];
		int idx = 0;
		for(MenuItem mi : items) {
			names[idx] = mi.getText();
			ids[idx++] = mi.getId(); 
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(mfd.getContext());	
		builder.setItems(names, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showMenu(ids[which]);
			}
		});
		AlertDialog dialog = builder.create();
		dialog.show();		
	}	
	
	public int getMenuSelection() {
		return menuSelection;
	}

	public Transform getTransform() {
		return transform;
	}
}
