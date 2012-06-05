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

import java.util.ArrayList;
import java.util.List;

import org.ros.android.rviz_for_android.R;
import org.ros.namespace.GraphName;
import org.ros.rosjava_geometry.FrameTransformTree;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class GraphNameProperty extends Property<GraphName> {

	private int selection = 0;
	private FrameTransformTree ftt;
	private TextView textView;
	private Spinner spin;
	
	public GraphNameProperty(String name, GraphName value, PropertyUpdateListener<GraphName> updateListener, FrameTransformTree ftt) {
		super(name, value, updateListener);
		this.ftt = ftt;
	}

	@Override
	public View getGUI(View convertView, ViewGroup parent, LayoutInflater inflater, String title) {
		convertView = inflater.inflate(R.layout.row_property_spinner, parent, false);
		textView = (TextView) convertView.findViewById(R.id.tvProp_Spinner_Name);
		if(title != null)
			textView.setText(title);
		else
			textView.setText(super.name);
		spin = (Spinner) convertView.findViewById(R.id.spProp_Spinner);
		if(ftt != null) {
			final List<GraphName> items = new ArrayList<GraphName>(ftt.getFrames());
			ArrayAdapter<GraphName> aa = new ArrayAdapter<GraphName>(parent.getContext(), android.R.layout.simple_spinner_item, items);
			aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spin.setAdapter(aa);
			spin.setOnItemSelectedListener(new OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> arg0, View v, int position, long id) {
					selection = position;
					setValue(items.get(position));
				}
				public void onNothingSelected(AdapterView<?> arg0) {
				}
			});
			spin.setSelection(selection);
		}
		return convertView;
	}
	
	public void setTransformTree(FrameTransformTree ftt) {
		this.ftt = ftt;
	}

}