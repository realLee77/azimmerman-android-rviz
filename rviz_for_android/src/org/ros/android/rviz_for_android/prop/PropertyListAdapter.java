package org.ros.android.rviz_for_android.prop;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

public class PropertyListAdapter extends BaseExpandableListAdapter {
	
	private List<LayerWithProperties> layers;
	private List<ArrayList<Property>> props = new ArrayList<ArrayList<Property>>();
	private final Context context;
	private LayoutInflater inflater;
		
	public PropertyListAdapter(List<LayerWithProperties> layers, Context context) {
		super();
		this.context = context;
		inflater = LayoutInflater.from(context);
		this.layers = layers;
		generateContents();
	}
	
	private void generateContents() {
		for(int i = 0; i < layers.size(); i++) {
			LayerWithProperties lwp = layers.get(i);
			props.add(i, new ArrayList<Property>(lwp.getProperties().getPropertyCollection()));
		}
	}

	public Object getChild(int groupPosition, int childPosition) {
		return props.get(groupPosition).get(childPosition);
	}

	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		return props.get(groupPosition).get(childPosition).getGUI(convertView, parent, inflater);
	}

	public int getChildrenCount(int groupPosition) {
		System.out.println("Children count gp " + groupPosition + " = " + props.get(groupPosition).size());
		return props.get(groupPosition).size();
	}

	public Object getGroup(int groupPosition) {
		return layers.get(groupPosition);
	}

	public int getGroupCount() {
		return layers.size();
	}

	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		return layers.get(groupPosition).getProperties().getGUI(convertView, parent, inflater);
	}

	public boolean hasStableIds() {
		return true;
	}

	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

}
