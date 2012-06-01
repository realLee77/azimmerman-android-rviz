package org.ros.android.rviz_for_android.prop;

public interface PropertyUpdateListener<T> {
	void onPropertyChanged(T newval);
}
