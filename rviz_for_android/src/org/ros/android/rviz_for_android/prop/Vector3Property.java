package org.ros.android.rviz_for_android.prop;

import org.ros.android.rviz_for_android.R;
import org.ros.android.view.visualization.Utility;
import org.ros.rosjava_geometry.Vector3;

import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

public class Vector3Property extends Property<Vector3> {
	
	private Vector3 newVector;

	public Vector3Property(String name, Vector3 value, PropertyUpdateListener<Vector3> updateListener) {
		super(name, value, updateListener);
		newVector = value;
	}

	@Override
	public View getGUI(View convertView, ViewGroup parent, LayoutInflater inflater) {
		convertView = inflater.inflate(R.layout.row_property_textfield, parent, false);
		final TextView textView = (TextView) convertView.findViewById(R.id.tvProp_TextField_Name);
		textView.setText(super.name);
		final EditText et = (EditText) convertView.findViewById(R.id.etProp_TextField_Value);
		et.setInputType(InputType.TYPE_CLASS_PHONE | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_TEXT);
		et.setText(newVector.getX() + ", " + newVector.getY() + ", " + newVector.getZ());
		et.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if(keyCode == KeyEvent.KEYCODE_ENTER) {
					newVector = Utility.newVector3FromString(et.getText().toString());
					if(newVector != null)
						setValue(newVector);
					else 
						newVector = value;
					return true;
				}
				return false;
			}
		});
		return convertView;
	}

}
