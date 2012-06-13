package org.ros.android.rviz_for_android.urdf;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class UrdfReader {

	private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	private DocumentBuilder builder;
	private final XPath xpath = XPathFactory.newInstance().newXPath();

	private Set<UrdfLink> urdf = new HashSet<UrdfLink>();

	public UrdfReader() {
		factory.setNamespaceAware(true);
		try {
			builder = factory.newDocumentBuilder();
		} catch(ParserConfigurationException e) {
			e.printStackTrace();
		}
	}
	
	public void readUrdf(String urdf) {
		buildDocument(urdf);
		parseUrdf();
	}
	
	public Set<UrdfLink> getUrdf() {
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
					visBuilder.setMaterialColor(existResults.item(0).getNodeValue());
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
			
			System.out.println(visual);
			System.out.println(collision);
		}
	}

	private Document doc = null;

	private void buildDocument(String toParse) {
		try {
			doc = builder.parse(toParse);
		} catch(SAXException e1) {
			e1.printStackTrace();
		} catch(IOException e1) {
			e1.printStackTrace();
		}
	}

	private NodeList existResults;
	private boolean nodeExists(String... xPathExpression) {
		existResults = getExpression(xPathExpression); 
		return existResults.getLength() > 0;
	}

	private <T> List<T> getValuesAsList(String... xPathExpression) {
		List<T> retval = new ArrayList<T>();
		NodeList nl = getExpression(xPathExpression);

		for(int i = 0; i < nl.getLength(); i++) {
			retval.add((T) nl.item(i).getNodeValue());
		}
		return retval;
	}

	private <T> List<T> getNamesAsList(String... xPathExpression) {
		List<T> retval = new ArrayList<T>();
		NodeList nl = getExpression(xPathExpression);

		for(int i = 0; i < nl.getLength(); i++) {
			retval.add((T) nl.item(i).getNodeName());
		}
		return retval;
	}

	private Node getSingleNode(String... xPathExpression) {
		NodeList nl = getExpression(xPathExpression);
		if(nl.getLength() > 1)
			throw new IllegalArgumentException("Expression returned multiple results!");
		return nl.item(0);
	}

	private NodeList getExpression(String... xPathExpression) {
		try {
			XPathExpression expr = xpath.compile(Compose(xPathExpression));
			return (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
		} catch(XPathExpressionException e) {
			e.printStackTrace();
		}
		return null;
	}

	private String Compose(String... pieces) {
		if(pieces.length == 1)
			return pieces[0];

		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < pieces.length; i++) {
			sb.append(pieces[i]);
			if(i < pieces.length - 1)
				sb.append("/");
		}
		return sb.toString();
	}

	private float[] toFloatArray(String str) {
		String[] pieces = str.split(" ");
		float[] retval = new float[pieces.length];
		for(int i = 0; i < pieces.length; i++) {
			retval[i] = Float.parseFloat(pieces[i]);
		}
		return retval;
	}
}
