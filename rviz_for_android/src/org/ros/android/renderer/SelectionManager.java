/*
 * Copyright (c) 2012, Willow Garage, Inc.
 * All rights reserved.
 *
 * Willow Garage licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.ros.android.renderer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.ros.android.renderer.layer.Selectable;
import org.ros.android.renderer.shapes.Color;

import android.graphics.Point;

public class SelectionManager {
	public static final Color selectedColor = new Color(.25f, .55f, .35f, 1f);
	public static final Color backgroundColor = new Color(0f, 0f, 0f, 1f);
	
	// Color management: create a table of colors available to draw from. When the table runs out, generate more.
	private LinkedList<Color> colorPool = new LinkedList<Color>();
	private static final int COLOR_CHUNK_SIZE = 256;
	private int rGen = 1;
	private int gGen = 1;
	private int bGen = 1;
	
	// "Bidirectional" mapping between selectable and color
	private Map<Selectable, Color> forwardMap = new HashMap<Selectable, Color>();
	private Map<Color, Selectable> reverseMap = new HashMap<Color, Selectable>();
	
	// Selection tracking
	private boolean isSelectionDraw = false;
	private Selectable selected = null;
	
	private Point selectionPoint = new Point(-1, -1); 
	
	public SelectionManager() {
		generateColors(COLOR_CHUNK_SIZE);
	}
	
	public Color registerSelectable(Selectable s) {
		Color associatedColor = getNextColor();
		forwardMap.put(s, associatedColor);
		reverseMap.put(associatedColor, s);
		return associatedColor;
	}
	
	public Color removeSelectable(Selectable s) {
		Color toRemove = forwardMap.remove(s);
		reverseMap.remove(toRemove);
		colorPool.add(toRemove);
		return backgroundColor;
	}
	
	public void beginSelectionDraw(int selX, int selY) {
		isSelectionDraw = true;
		selectionPoint.set(selX, selY);
	}
	
	public Point getSelectionCoordinates() {
		return selectionPoint;
	}
	
	public boolean selectItemWithColor(Color c) {
		isSelectionDraw = false;
		if(reverseMap.containsKey(c)) {
			Selectable newSelected = reverseMap.get(c);
			if(selected != newSelected) {
				deselect(selected);
				selected = newSelected;			
				selected.setSelected(true);
			}
			return true;
		}
		deselect(selected);
		return false;
	}
	
	public Color getColor(Selectable s) {
		return forwardMap.get(s);
	}
	
	public Selectable getSelectedItem() {
		return selected;
	}
	
	public Map<String, String> getSelectedInfo() {
		if(selected != null)
			return selected.getInfo();
		else
			return null;
	}
	
	public boolean isSelectionDraw() {
		return isSelectionDraw;
	}
	
	private Color getNextColor() {
		if(colorPool.isEmpty())
			generateColors(COLOR_CHUNK_SIZE);
		return colorPool.removeFirst();
	}
	
	private void deselect(Selectable s) {
		if(s != null) {
			s.setSelected(false);
		}
	}
	
	private void generateColors(int nColors) {
		for(int i = 0; i < nColors; i++)
			colorPool.add(generateNextColor());
	}
	
	private Color generateNextColor() {
		rGen ++;
		if(rGen == 256) {
			gGen ++;
			rGen = 0;
		}
		if(gGen == 256) {
			bGen ++;
			gGen = 0;
		}
		if(bGen == 256)
			throw new RuntimeException("Selection manager is out of colors to generate.");
		
		return new Color(rGen/255f, gGen/255f, bGen/255f, 1f);
	}
	
}
