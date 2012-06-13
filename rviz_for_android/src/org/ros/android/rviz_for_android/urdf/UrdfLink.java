package org.ros.android.rviz_for_android.urdf;
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

public class UrdfLink {
	private Component visual;
	private Component collision;
	private String name;

	public UrdfLink(Component visual, Component collision, String name) {
		this.visual = visual;
		this.collision = collision;
		this.name = name;
	}

	public Component getVisual() {
		return visual;
	}

	public Component getCollision() {
		return collision;
	}

	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((collision == null) ? 0 : collision.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((visual == null) ? 0 : visual.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		UrdfLink other = (UrdfLink) obj;
		if(collision == null) {
			if(other.collision != null)
				return false;
		} else if(!collision.equals(other.collision))
			return false;
		if(name == null) {
			if(other.name != null)
				return false;
		} else if(!name.equals(other.name))
			return false;
		if(visual == null) {
			if(other.visual != null)
				return false;
		} else if(!visual.equals(other.visual))
			return false;
		return true;
	}
}
