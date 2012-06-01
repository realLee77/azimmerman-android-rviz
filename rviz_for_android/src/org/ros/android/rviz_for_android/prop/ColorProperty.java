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
	
	private static final android.graphics.Color androidColor = new android.graphics.Color();
	
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
		btn.setBackgroundColor(androidColor.rgb((int)value.getRed()*255, (int)value.getGreen()*255, (int)value.getBlue()*255));

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
		btn.setTextColor(androidColor.BLACK);
		
		return convertView;
	}
	
	@Override
	protected void informListeners(Color newvalue) {
		if(btn != null)
			btn.setBackgroundColor(Utility.ColorToInt(newvalue));
		super.informListeners(newvalue);
	}
}
