package org.ros.android.rviz_for_android.prop;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author azimmerman
 * @param <T> The type of value stored by this property
 */
public abstract class Property<T> {
	
	protected T value;
	protected String name;
	protected String description = "Not implemented! ಠ_ಠ";
	protected LinkedList<PropertyUpdateListener<T>> updateListeners = new LinkedList<PropertyUpdateListener<T>>();
	
	protected HashMap<String, Property> subProps = new HashMap<String, Property>();

	public Property(String name, T value, PropertyUpdateListener<T> updateListener) {
		this.name = name;
		this.value = value;
		updateListeners.add(updateListener);
	}
	
	public void addUpdateListener(PropertyUpdateListener<T> updateListener) {
		updateListeners.add(updateListener);
	}
	
	public void setValue(T value) {
		if(this.value != value) this.value = value;
		informListeners(value);
	}
	
	private void informListeners(T newvalue) {
		for(PropertyUpdateListener<T> pul : updateListeners) {
			if(pul != null)	pul.onPropertyChanged(newvalue);
		}
	}

	public abstract View getGUI(View convertView, ViewGroup parent, LayoutInflater inflater);
	
	public T getValue() {
		return value;
	}
	
	public String getName() {
		return name;
	}
	
	public String getDescription() {
		return description;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Property other = (Property) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	// Nested property access functions
	public Property getProperty(String... levels) {
		Property cur = this;
		for(int i = 0; i < levels.length; i++) {
			cur = (Property) cur.subProps.get(levels[i]);
		}
		return cur;
	}
	public void addSubProperty(Property p, String... levels) {
		Property cur = this;
		for(int i = 0; i < levels.length; i ++) {
			cur = (Property) cur.subProps.get(levels[i]);
		}
		cur.subProps.put(p.getName(), p);
	}
	public Collection<Property> getPropertyCollection() {
		return subProps.values();
	}
}
