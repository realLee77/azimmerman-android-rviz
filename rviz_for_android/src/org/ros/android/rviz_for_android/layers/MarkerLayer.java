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
package org.ros.android.rviz_for_android.layers;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.shapes.BaseShape;
import org.ros.android.renderer.shapes.Color;
import org.ros.android.rviz_for_android.drawable.Cube;
import org.ros.android.rviz_for_android.drawable.Cylinder;
import org.ros.android.rviz_for_android.drawable.Sphere;
import org.ros.android.rviz_for_android.prop.LayerWithProperties;
import org.ros.android.rviz_for_android.prop.Property;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Transform;

import visualization_msgs.Marker;
import android.os.Handler;
import android.util.Log;

public class MarkerLayer extends EditableStatusSubscriberLayer<visualization_msgs.Marker> implements LayerWithProperties {

	private Map<String, HashMap<Integer, MarkerObj>> markers = new HashMap<String, HashMap<Integer, MarkerObj>>();
	private FrameTransformTree ftt;
	private long nextPruneTime;
	private static final long PRUNE_PERIOD = 1000; // Milliseconds
	
	public MarkerLayer(GraphName topicName, String messageType, String messageTypeName, Camera cam) {
		super(topicName, messageType, messageTypeName, cam);
		nextPruneTime = System.currentTimeMillis() + PRUNE_PERIOD;
	}

	@Override
	public void onMessageReceived(Marker msg) {
		super.onMessageReceived(msg);
		
		String ns = msg.getNs();
		int id = msg.getId();
		
		switch(msg.getAction()) {
		case Marker.ADD:
			Log.i("MarkerLayer", "Adding marker " + ns + ":" + id);
			if(!markers.containsKey(ns))
				markers.put(ns, new HashMap<Integer, MarkerObj>());
			markers.get(ns).put(id, new MarkerObj(msg, super.camera));
			break;
		case Marker.DELETE:
			Log.i("MarkerLayer", "Deleting marker " + ns + ":" + id);
			if(markers.containsKey(ns))
				markers.get(ns).remove(id);
			break;
		default:
			Log.e("MarkerLayer", "Received a message with action " + msg.getAction());
			return;
		}
	}
	
	@Override
	public void draw(GL10 glUnused) {
		for(HashMap<Integer, MarkerObj> hm : markers.values()) {
			for(MarkerObj mo : hm.values()) {
				camera.pushM();
				if(mo.getFrame() != null)
					camera.applyTransform(ftt.newTransformIfPossible(camera.getFixedFrame(), mo.getFrame()));
				mo.shape.draw(glUnused);
				camera.popM();
			}
		}
		if(System.currentTimeMillis() >= nextPruneTime)
			pruneMarkers();
	}
	
	private void pruneMarkers() {
		for(HashMap<Integer, MarkerObj> hm : markers.values()) {
			List<Integer> removeIds = new LinkedList<Integer>();
			for(Integer i : hm.keySet())
				if(hm.get(i).isExpired())
					removeIds.add(i);
			for(Integer i : removeIds)
				hm.remove(i);
		}
		nextPruneTime += PRUNE_PERIOD;
	}
	
	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler, FrameTransformTree frameTransformTree, Camera camera) {
		super.onStart(connectedNode, handler, frameTransformTree, camera);
		this.ftt = frameTransformTree;
	}

	@Override
	public Property<?> getProperties() {
		return super.prop;
	}

	@Override
	protected String getMessageFrameId(Marker msg) {
		return msg.getHeader().getFrameId();
	}
	
	private static class MarkerObj {
		private BaseShape shape;
		private GraphName frame;
		private Color color;
		private long createTime;
		private int duration;
		private float[] scale;
		private int type;
		
		public MarkerObj(Marker msg, Camera cam) {
			this.type = msg.getType();
			this.frame = msg.getFrameLocked()?GraphName.of(msg.getHeader().getFrameId()):null;
			this.scale = new float[] { (float) msg.getScale().getX(), (float) msg.getScale().getY(), (float) msg.getScale().getZ()};
			this.duration = msg.getLifetime().secs * 1000;
			this.createTime = System.currentTimeMillis();
			this.color = new Color(msg.getColor().getR(), msg.getColor().getG(), msg.getColor().getB(), msg.getColor().getA());
	
			switch(type) {
			case Marker.CUBE:
				shape = new Cube(cam);
				break;
			case Marker.SPHERE:
				shape = new Sphere(cam);
				break;
			case Marker.CYLINDER:
				shape = new Cylinder(cam);
				break;
			}

			shape.setColor(this.color);
			shape.setTransform(Transform.newFromPoseMessage(msg.getPose()));
		}
		
		public GraphName getFrame() {
			return frame;
		}
		
		public boolean isExpired() {
			if(duration == 0)
				return false;
			else
				return (System.currentTimeMillis() - createTime) < duration;
		}
		
		public float[] getScale() {
			return scale;
		}
	}
}
