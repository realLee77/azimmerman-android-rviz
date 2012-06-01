package org.ros.android.rviz_for_android.prop;

import org.ros.android.rviz_for_android.R;
import org.ros.android.view.visualization.Utility;

import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

public class IntProperty extends Property<Integer> {

	private int newInt;
	private int min = Integer.MIN_VALUE;
	private int max = Integer.MAX_VALUE;
	
	public IntProperty(String name, int value, PropertyUpdateListener updateListener) {
		super(name, value, updateListener);
		newInt = value;
	}

	@Override
	public View getGUI(View convertView, ViewGroup parent, LayoutInflater inflater) {
		convertView = inflater.inflate(R.layout.row_property_numericfield, parent, false);

		final TextView textView = (TextView) convertView.findViewById(R.id.tvProp_NumericField_Name);	
		textView.setText(super.name);

		// Show the numeric input field
		final EditText et = (EditText) convertView.findViewById(R.id.etProp_NumericField_DecimalValue);
		et.setVisibility(EditText.VISIBLE);		
		et.setText(Integer.toString(newInt));
		
		et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
		
		et.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				try {
					newInt = Integer.parseInt(et.getText().toString());
				} catch(NumberFormatException e) {
					newInt = value;
				}
				if(keyCode == KeyEvent.KEYCODE_ENTER) {
					setValue(newInt);
					return true;
				}
				return false;
			}
		});
		
		this.addUpdateListener(new PropertyUpdateListener<Integer>() {
			public void onPropertyChanged(Integer newval) {
				et.setText(Integer.toString(newval));
			}
		});
		
		return convertView;
	}
	
	public IntProperty setValidRange(int min, int max) {
		this.min = min;
		this.max = max;
		return this;
	}

	@Override
	public void setValue(Integer value) {
		if(Utility.inRange(value, min, max))
			super.setValue(value);
		else
			super.setValue(super.value);
	}

}
