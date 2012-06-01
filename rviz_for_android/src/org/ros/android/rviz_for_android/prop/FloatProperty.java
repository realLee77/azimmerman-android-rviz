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

public class FloatProperty extends Property<Float> {

	private float newFloat;
	private float min = Float.MIN_VALUE;
	private float max = Float.MAX_VALUE;

	private TextView textView;
	private EditText et;

	public FloatProperty(String name, Float value, PropertyUpdateListener<Float> updateListener) {
		super(name, value, updateListener);
		newFloat = value;
	}

	@Override
	public View getGUI(View convertView, ViewGroup parent, LayoutInflater inflater, String title) {
		convertView = inflater.inflate(R.layout.row_property_numericfield, parent, false);

		textView = (TextView) convertView.findViewById(R.id.tvProp_NumericField_Name);
		if(title != null)
			textView.setText(title);
		else
			textView.setText(super.name);

		// Show the numeric input field
		et = (EditText) convertView.findViewById(R.id.etProp_NumericField_DecimalValue);
		et.setVisibility(EditText.VISIBLE);

		et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);

		et.setText(Float.toString(newFloat));
		et.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				try {
					newFloat = Float.parseFloat(et.getText().toString());
				} catch(NumberFormatException e) {
					newFloat = value;
				}
				if(keyCode == KeyEvent.KEYCODE_ENTER) {
					setValue(newFloat);
					return true;
				}
				return false;
			}
		});

		this.addUpdateListener(new PropertyUpdateListener<Float>() {
			public void onPropertyChanged(Float newval) {
				et.setText(Float.toString(newval));
			}
		});

		return convertView;
	}

	public FloatProperty setValidRange(Float min, Float max) {
		this.min = min;
		this.max = max;
		return this;
	}

	@Override
	public void setValue(Float value) {
		if(Utility.inRange(value, min, max))
			super.setValue(value);
		else
			super.setValue(super.value);
	}

	@Override
	protected void informListeners(Float newvalue) {
		if(et != null)
			et.setText(newvalue.toString());
		super.informListeners(newvalue);
	}

}
