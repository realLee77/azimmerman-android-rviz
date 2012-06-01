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

	@Override
	public View getGUI(View convertView, ViewGroup parent, LayoutInflater inflater) {
		convertView = inflater.inflate(R.layout.row_property_boolean, parent, false);
		final TextView textView = (TextView) convertView.findViewById(R.id.tvProp_TextField_Name);
		textView.setText(super.name);
		final CheckBox cb = (CheckBox) convertView.findViewById(R.id.cbProp_Checkbox);
		cb.setFocusable(false);
		cb.setChecked(super.value);
		cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				setValue(isChecked);
			}
		});
		
		this.addUpdateListener(new PropertyUpdateListener<Boolean>() {
			public void onPropertyChanged(Boolean newval) {
				cb.setChecked(newval);
			}
		});
		return convertView;
	}

}
