package org.ros.android.rviz_for_android.prop;

import org.ros.android.rviz_for_android.R;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

public class StringProperty extends Property<String> {

	String newText;
	
	public StringProperty(String name, String value, PropertyUpdateListener updateListener) {
		super(name, value, updateListener);
		newText = value;
	}

	@Override
	public View getGUI(View convertView, ViewGroup parent, LayoutInflater inflater) {
		convertView = inflater.inflate(R.layout.row_property_textfield, parent, false);
		final TextView textView = (TextView) convertView.findViewById(R.id.tvProp_TextField_Name);
		textView.setText(super.name);
		final EditText et = (EditText) convertView.findViewById(R.id.etProp_TextField_Value);
		et.setText(newText);
		et.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				newText = et.getText().toString();
				if(keyCode == KeyEvent.KEYCODE_ENTER) {
					setValue(newText);
					return true;
				}
				return false;
			}
		});
		return convertView;
	}

}
