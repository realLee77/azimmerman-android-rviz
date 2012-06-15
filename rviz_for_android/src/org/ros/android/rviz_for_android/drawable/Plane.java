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

import org.ros.android.view.visualization.shape.Color;
import org.ros.android.view.visualization.shape.TriangleStripShape;

public class Plane extends TriangleStripShape {

	private static final float planeV[] = {
	-1f,-1f,0f,
	-1f,1f,0f,
	1f,-1f,0f,
	1f,1f,0f
	};
	
	private static final float planeN[] = {
		0f,0f,1f,
		0f,0f,1f,	
	};
	
	private static final short planeI[] = {
	    3,2,1,0//0, 1, 2, 3
	};

	public Plane(Color color) {
		super(planeV, planeI, planeN, color);
	}

}
