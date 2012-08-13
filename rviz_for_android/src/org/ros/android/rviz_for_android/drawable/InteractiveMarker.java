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
import org.ros.android.renderer.shapes.Cleanable;
import org.ros.android.rviz_for_android.drawable.InteractiveMarkerControl.InteractionMode;
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
import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseArray;

public class InteractiveMarker implements Cleanable {

	private List<InteractiveMarkerControl> controls = new LinkedList<InteractiveMarkerControl>();

	private float scale;
	private String name;

	private FrameTransformTree ftt;
	private GraphName frame;
	private String frameString;
	private Transform transform;

	private Camera cam;
	private final MeshFileDownloader mfd;
	private final MarkerFeedbackPublisher publisher;

	private volatile boolean isInAction = false; // Becomes true when a user is manipulating the marker via a child control
	private InteractiveMarkerPose latestPose = null; // Save the last received pose message if the marker is being manipulated and apply it when the user is done manipulating

	public InteractiveMarker(visualization_msgs.InteractiveMarker msg, Camera cam, MeshFileDownloader mfd, FrameTransformTree ftt, MarkerFeedbackPublisher pub) {
		this.mfd = mfd;
		this.publisher = pub;
		Log.d("InteractiveMarker", "Created interactive marker");

		name = msg.getName();
		transform = Transform.fromPoseMessage(msg.getPose());
		transform.setRotation(Utility.correctQuaternion(transform.getRotationAndScale()));

		frameString = msg.getHeader().getFrameId();
		frame = GraphName.of(frameString);

		// Create controls
		for(visualization_msgs.InteractiveMarkerControl control : msg.getControls()) {
			InteractiveMarkerControl imc = new InteractiveMarkerControl(control, cam, mfd, ftt, this);
			// imc.setParentTransform(transform);
			controls.add(imc);
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
		cam.applyTransform(Utility.newTransformIfPossible(ftt, frame, cam.getFixedFrame()));

		for(InteractiveMarkerControl control : controls)
			control.draw(glUnused);

		cam.popM();
	}

	public void selectionDraw(GL10 glUnused) {
		cam.pushM();
		cam.scaleM(scale, scale, scale);
		cam.applyTransform(Utility.newTransformIfPossible(ftt, frame, cam.getFixedFrame()));

		for(InteractiveMarkerControl control : controls)
			control.selectionDraw(glUnused);

		cam.popM();
	}

	public void publish(InteractiveMarkerControl control, byte type) {
		publisher.publishFeedback(this, control, type);
	}

	public void update(InteractiveMarkerPose p) {
		if(isInAction) {
			latestPose = p;
			return;
		}

		transform = Transform.fromPoseMessage(p.getPose());

		if(!frameString.equals(p.getHeader().getFrameId())) {
			frameString = p.getHeader().getFrameId();
			frame = GraphName.of(frameString);
		}

		updateControls();

		// If this marker is selected, move the control to match the new position
		if(isSelected)
			cam.getSelectionManager().signalCameraMoved();
	}

	public void childRotate(Quaternion q) {
		transform.setRotation(q.multiply(transform.getRotationAndScale()));
		updateControls();
		cam.getSelectionManager().signalCameraMoved();
	}

	public void childTranslate(Vector3 v) {
		transform.setTranslation(v);
		updateControls();
		cam.getSelectionManager().signalCameraMoved();
	}

	public void controlInAction(boolean inAction) {
		isInAction = inAction;
		if(inAction)
			latestPose = null;
		else if(latestPose != null) {
			update(latestPose);
		}
	}

	/**
	 * Update all child controls with the latest parent transform
	 */
	private void updateControls() {
		for(InteractiveMarkerControl imc : controls)
			imc.setParentTransform(transform);
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
			ArrayList<MenuItem> itemList = menuItems.get(parentId);
			if(itemList == null)
				itemList = new ArrayList<MenuItem>();
			itemList.add(mi);
			menuItems.put(parentId, itemList);
		}
	}

	private int menuSelection = 0;
	private InteractiveMarkerControl requestingControl;

	public void showMenu(InteractiveMarkerControl requestingControl) {
		this.requestingControl = requestingControl;
		showMenu(0);
	}

	// TODO: This feels like a hack, but it's the only way to get the dialog to launch on the UI thread
	private class ShowMenu extends AsyncTask<List<MenuItem>, List<MenuItem>, Void> {
		@Override
		protected void onProgressUpdate(List<MenuItem>... values) {
			showMenuDialog(values[0]);
			super.onProgressUpdate(values);
		}

		@Override
		protected Void doInBackground(List<MenuItem>... params) {
			this.publishProgress(params[0]);
			return null;
		}
	}

	private void showMenu(int parent) {
		List<MenuItem> children = menuItems.get(parent);
		if(children != null && !children.isEmpty()) {
			ShowMenu task = new ShowMenu();
			task.execute(children);
		} else {
			menuSelection = parent;
			publish(requestingControl, InteractionMode.MENU.feedbackType);
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

	@Override
	public void cleanup() {
		for(InteractiveMarkerControl imc : controls)
			imc.cleanup();
	}

	public String getFrame() {
		return frame.toString();
	}

	private boolean isSelected = false;

	public void setSelected(boolean b) {
		isSelected = b;
	}

	public String getName() {
		return name;
	}
}
