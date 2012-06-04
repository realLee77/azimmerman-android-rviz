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

import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

public class StringProperty extends Property<String> {

	String newText;
	
	private TextView textView;
	private EditText et;
	
	public StringProperty(String name, String value, PropertyUpdateListener<String> updateListener) {
		super(name, value, updateListener);
		newText = value;
	}

	@Override
	public View getGUI(View convertView, ViewGroup parent, LayoutInflater inflater, String title) {
		convertView = inflater.inflate(R.layout.row_property_textfield, parent, false);
		final InputMethodManager imm = (InputMethodManager) parent.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		textView = (TextView) convertView.findViewById(R.id.tvProp_TextField_Name);
		if(title != null)
			textView.setText(title);
		else
			textView.setText(super.name);
		et = (EditText) convertView.findViewById(R.id.etProp_TextField_Value);
		et.setText(newText);
		et.setSelectAllOnFocus(true);
		et.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				newText = et.getText().toString();
				if(keyCode == KeyEvent.KEYCODE_ENTER) {
					setValue(newText);
					imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
					return true;
				}
				return false;
			}
		});
		return convertView;
	}

}
