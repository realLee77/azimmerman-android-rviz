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
package org.ros.android.rviz_for_android.prop;

import org.ros.android.rviz_for_android.R;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ReadOnlyProperty extends Property<String> {
	
	private TextView textView;
	private TextView display;
	private int textColor = Color.WHITE;
	
	public ReadOnlyProperty(String name, String value, PropertyUpdateListener<String> updateListener) {
		super(name, value, updateListener);
	}

	public void setTextColor(int textColor) {
		this.textColor = textColor;
		if(display != null)
			display.setTextColor(textColor);
	}
	
	@Override
	public View getGUI(View convertView, ViewGroup parent, LayoutInflater inflater, String title) {
		convertView = inflater.inflate(R.layout.row_property_readonly, parent, false);
		textView = (TextView) convertView.findViewById(R.id.tvProp_ReadOnly_Name);
		if(title != null)
			textView.setText(title);
		else
			textView.setText(super.name);
		
		display = (TextView) convertView.findViewById(R.id.tvProp_ReadOnly_Value);
		display.setText(super.value);
		display.setTextColor(textColor);
		
		super.addUpdateListener(new PropertyUpdateListener<String>() {
			@Override
			public void onPropertyChanged(String newval) {
				display.setText(newval);
			}
		});
		return convertView;
	}

}
