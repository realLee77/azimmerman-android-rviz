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

import org.ros.android.rviz_for_android.drawable.InteractiveMarkerControl.InteractionMode;

import android.os.Handler;
import android.os.Message;
import android.widget.Button;

public class InteractiveControlManager {
	
	private static final int MSG_SHOW = 0;
	private static final int MSG_HIDE = 1;
	private static final int MSG_MOVE = 2;
	
	private static Button btFollower;
	
	private static final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case MSG_MOVE:
				btFollower.setX(msg.arg1);
				btFollower.setY(msg.arg2);
				break;
			case MSG_SHOW:
				btFollower.setVisibility(Button.VISIBLE);
				break;
			case MSG_HIDE:
				btFollower.setVisibility(Button.INVISIBLE);
				break;			
			}
		}
	};
	public InteractiveControlManager(Button btFollower) {
		InteractiveControlManager.btFollower = btFollower;
	}
	
	public void showInteractiveController(InteractionMode type) {
		handler.sendEmptyMessage(MSG_SHOW);
	}
	
	public void moveInteractiveController(int x, int y) {
		handler.obtainMessage(MSG_MOVE, x, y).sendToTarget();
	}
	
	public void hideInteractiveController() {
		handler.sendEmptyMessage(MSG_HIDE);
	}


}
