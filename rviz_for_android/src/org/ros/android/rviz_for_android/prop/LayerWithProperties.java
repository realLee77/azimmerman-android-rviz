package org.ros.android.rviz_for_android.prop;

import org.ros.android.view.visualization.layer.Layer;

// This interface name probably violates some naming convention. Feel free to suggest alternate names! 
public interface LayerWithProperties extends Layer {
	public Property getProperties();
	public String getName();
}
