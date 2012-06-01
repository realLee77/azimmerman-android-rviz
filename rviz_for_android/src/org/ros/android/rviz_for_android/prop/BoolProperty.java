package org.ros.android.rviz_for_android.prop;

import org.ros.android.rviz_for_android.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class BoolProperty extends Property<Boolean> {

	public BoolProperty(String name, Boolean value, PropertyUpdateListener<Boolean> updateListener) {
		super(name, value, updateListener);
		// TODO Auto-generated constructor stub
	}

	private TextView textView;
	private CheckBox cb;

	@Override
	public View getGUI(View convertView, ViewGroup parent, LayoutInflater inflater, String title) {
		convertView = inflater.inflate(R.layout.row_property_boolean, parent, false);
		textView = (TextView) convertView.findViewById(R.id.tvProp_Boolean_Name);
		if(title != null)
			textView.setText(title);
		else
			textView.setText(super.name);
		cb = (CheckBox) convertView.findViewById(R.id.cbProp_Checkbox);
		cb.setFocusable(false);
		cb.setChecked(super.value);
		cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				setValue(isChecked);
			}
		});
		return convertView;
	}

	@Override
	protected void informListeners(Boolean newvalue) {
		if(cb != null)
			cb.setChecked(newvalue);
		super.informListeners(newvalue);
	}

}
