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
package org.ros.android.rviz_for_android;

import org.ros.android.renderer.AngleControlView;
import org.ros.android.renderer.AngleControlView.OnAngleChangeListener;
import org.ros.android.renderer.layer.InteractiveObject;

import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;

public class InteractiveControlManager {

	private static final int MSG_SHOW = 0;
	private static final int MSG_HIDE = 1;
	private static final int MSG_MOVE = 2;

	private static View activeControl;
	private static AngleControlView angleControl;

	private static final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case MSG_MOVE:
				activeControl.setX(msg.arg1 - (angleControl.getWidth() / 2));
				activeControl.setY(msg.arg2 - (angleControl.getHeight() / 2));
				break;
			case MSG_SHOW:
				activeControl.setVisibility(Button.VISIBLE);
				break;
			case MSG_HIDE:
				activeControl.setVisibility(Button.INVISIBLE);
				break;
			}
		}
	};
	
	private InteractiveObject activeObject;

	public InteractiveControlManager(AngleControlView view) {
		InteractiveControlManager.angleControl = view;
		view.setOnAngleChangeListener(new OnAngleChangeListener() {
			@Override
			public void angleChange(float newAngle, float delta) {
				activeObject.rotate(delta);
			}
		});
	}

	public void showInteractiveController(InteractiveObject activeObject) {
		this.activeObject = activeObject;
		switch(activeObject.getInteractionMode()) {
		case MENU:
			break;
		case ROTATE_AXIS:
			activeControl = angleControl;
			handler.sendEmptyMessage(MSG_SHOW);
			break;
		}
	}

	public void moveInteractiveController(int x, int y) {
		handler.obtainMessage(MSG_MOVE, x, y).sendToTarget();
	}

	public void hideInteractiveController() {
		handler.sendEmptyMessage(MSG_HIDE);
	}

}
