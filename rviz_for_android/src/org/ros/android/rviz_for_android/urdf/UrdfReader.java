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

package org.ros.android.rviz_for_android.urdf;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ros.android.view.visualization.shape.Color;
import org.w3c.dom.NodeList;

public class UrdfReader extends XmlReader {

	private List<UrdfLink> urdf = new ArrayList<UrdfLink>();

	public UrdfReader() {
		super(true);

	}

	public void readUrdf(String urdf) {
		this.urdf.clear();
		buildDocument(new ByteArrayInputStream(urdf.getBytes()));
		parseUrdf();
		buildColors();
	}

	private void buildColors() {
		Map<String, Color> colors = new HashMap<String, Color>();
		for(UrdfLink ul : urdf) {
			for(Component c : ul.getComponents()) {
				if(c.getMaterial_color() != null) {
					colors.put(c.getMaterial_name(), c.getMaterial_color());
				}
			}
		}

		for(UrdfLink ul : urdf) {
			for(Component c : ul.getComponents()) {
				if(c.getMaterial_name() != null && c.getMaterial_color() == null) {				
					c.setMaterial_color(colors.get(c.getMaterial_name()));	
				}
			}
		}
	}	

	public List<UrdfLink> getUrdf() {
		return urdf;
	}

	private void parseUrdf() {
		NodeList nodes = getExpression("/robot/link/@name");

		for(int i = 0; i < nodes.getLength(); i++) {
			// Link name
			String name = nodes.item(i).getNodeValue();
			String prefix = "/robot/link[@name='" + name + "']";

			Component visual = null;
			Component collision = null;

			// Check for visual component
			if(nodeExists(prefix, "visual")) {
				String vprefix = prefix + "/visual";

				// Get geometry type
				String gtype = getSingleNode(vprefix, "geometry/*").getNodeName();

				Component.Builder visBuilder = new Component.Builder(gtype);

				switch(visBuilder.getType()) {
				case BOX: {
					String size = getSingleNode(vprefix, "/box/@size").getNodeValue();
					visBuilder.setSize(toFloatArray(size));
				}
					break;
				case CYLINDER: {
					float radius = Float.parseFloat(getSingleNode(vprefix, "/cylinder/@radius").getNodeValue());
					float length = Float.parseFloat(getSingleNode(vprefix, "/cylinder/@length").getNodeValue());
					visBuilder.setRadius(radius);
					visBuilder.setLength(length);
				}
					break;
				case SPHERE: {
					float radius = Float.parseFloat(getSingleNode(vprefix, "/sphere/@radius").getNodeValue());
					visBuilder.setRadius(radius);
				}
					break;
				case MESH:
					visBuilder.setMesh(getSingleNode(vprefix, "/mesh/@filename").getNodeValue());
					if(nodeExists(vprefix, "/mesh/@scale"))
						visBuilder.setMeshScale(Float.parseFloat(existResults.item(0).getNodeValue()));
					break;
				}

				// OPTIONAL - get origin
				if(nodeExists(vprefix, "/origin/@xyz")) {
					visBuilder.setOffset(toFloatArray(existResults.item(0).getNodeValue()));
				}
				if(nodeExists(vprefix, "/origin/@rpy")) {
					visBuilder.setRotation(toFloatArray(existResults.item(0).getNodeValue()));
				}

				// OPTIONAL - get material
				if(nodeExists(vprefix, "/material/@name")) {
					visBuilder.setMaterialName(existResults.item(0).getNodeValue());
				}
				if(nodeExists(vprefix, "/material/color/@rgba")) {
					visBuilder.setMaterialColor(toFloatArray(existResults.item(0).getNodeValue()));
				}
				visual = visBuilder.build();
			}

			// Check for collision component
			if(nodeExists(prefix, "collision")) {
				String vprefix = prefix + "/collision";

				// Get geometry type
				String gtype = getSingleNode(vprefix, "geometry/*").getNodeName();

				Component.Builder colBuilder = new Component.Builder(gtype);

				switch(colBuilder.getType()) {
				case BOX: {
					String size = getSingleNode(vprefix, "/box/@size").getNodeValue();
					colBuilder.setSize(toFloatArray(size));
				}
					break;
				case CYLINDER: {
					float radius = Float.parseFloat(getSingleNode(vprefix, "/cylinder/@radius").getNodeValue());
					float length = Float.parseFloat(getSingleNode(vprefix, "/cylinder/@length").getNodeValue());
					colBuilder.setRadius(radius);
					colBuilder.setLength(length);
				}
					break;
				case SPHERE: {
					float radius = Float.parseFloat(getSingleNode(vprefix, "/sphere/@radius").getNodeValue());
					colBuilder.setRadius(radius);
				}
					break;
				case MESH:
					colBuilder.setMesh(getSingleNode(vprefix, "/mesh/@filename").getNodeValue());
					break;
				}

				// OPTIONAL - get origin
				if(nodeExists(vprefix, "/origin/@xyz")) {
					colBuilder.setOffset(toFloatArray(existResults.item(0).getNodeValue()));
				}
				if(nodeExists(vprefix, "/origin/@rpy")) {
					colBuilder.setRotation(toFloatArray(existResults.item(0).getNodeValue()));
				}

				collision = colBuilder.build();
			}

			UrdfLink newLink = new UrdfLink(visual, collision, name);
			urdf.add(newLink);
		}
	}
}
