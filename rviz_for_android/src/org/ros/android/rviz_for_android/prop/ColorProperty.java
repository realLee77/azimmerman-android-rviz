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
import org.ros.android.rviz_for_android.color.ColorPickerDialog;
import org.ros.android.view.visualization.Utility;
import org.ros.android.view.visualization.shape.Color;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class ColorProperty extends Property<Color> {
		
	public ColorProperty(String name, Color value, PropertyUpdateListener<Color> updateListener) {
		super(name, value, updateListener);
	}
	
	private TextView textView;
	private Button btn;

	@Override
	public View getGUI(View convertView, final ViewGroup parent, LayoutInflater inflater, String title) {
		convertView = inflater.inflate(R.layout.row_property_button, parent, false);
		textView = (TextView) convertView.findViewById(R.id.tvProp_Button_Name);
		if(title != null)
			textView.setText(title);
		else
			textView.setText(super.name);
		
		btn = (Button) convertView.findViewById(R.id.btProp_Button);
		btn.setText(" ");
		btn.setBackgroundColor(android.graphics.Color.rgb((int)value.getRed()*255, (int)value.getGreen()*255, (int)value.getBlue()*255));

		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(parent.getContext());
				final ColorPickerDialog d = new ColorPickerDialog(parent.getContext(), prefs.getInt("dialog", Utility.ColorToInt(value)));
				d.setAlphaSliderVisible(true);
				d.setButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						setValue(Utility.IntToColor(d.getColor()));	
					}
				});
				d.show();
			}
		});
		btn.setText("Pick Color");
		btn.setTextColor(android.graphics.Color.BLACK);
		
		return convertView;
	}
	
	@Override
	protected void informListeners(Color newvalue) {
		if(btn != null)
			btn.setBackgroundColor(Utility.ColorToInt(newvalue));
		super.informListeners(newvalue);
	}
}
