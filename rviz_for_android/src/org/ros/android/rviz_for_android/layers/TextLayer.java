package org.ros.android.rviz_for_android.layers;

import java.io.IOException;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.rviz_for_android.MainActivity;
import org.ros.android.rviz_for_android.prop.BoolProperty;
import org.ros.android.rviz_for_android.prop.LayerWithProperties;
import org.ros.android.rviz_for_android.prop.Property;
import org.ros.android.rviz_for_android.prop.PropertyUpdateListener;
import org.ros.android.rviz_for_android.prop.StringProperty;
import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.layer.SubscriberLayer;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;
import org.ros.rosjava_geometry.FrameTransformTree;

import android.os.Handler;

public class TextLayer extends SubscriberLayer<std_msgs.String> implements LayerWithProperties {
	
	private String toWrite;
	
	private GraphName frame;
	private TexFont txt;
	private boolean isLoaded = false;
	
	private BoolProperty prop;

	public TextLayer(GraphName topicName, String messageType) {
		super(topicName, messageType);
		initProperties();
	}
	
	private void initProperties() {
		prop = new BoolProperty("enabled", true, null);
		
		prop.addSubProperty(new StringProperty("toWrite", "I'm listening...", new PropertyUpdateListener<String>() {
			public void onPropertyChanged(String newval) {
				toWrite = newval;				
			}
		}));
		
		toWrite = (String) prop.getProperty("toWrite").getValue();
	}

	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler, FrameTransformTree frameTransformTree, Camera camera) {
		super.onStart(connectedNode, handler, frameTransformTree, camera);
		isLoaded = false;
		Subscriber<std_msgs.String> sub = getSubscriber();
		sub.addMessageListener(new MessageListener<std_msgs.String>() {
			public void onNewMessage(std_msgs.String arg0) {
				toWrite = "I heard: " + arg0.getData();
				requestRender();
			}
		});
	}

	@Override
	public void draw(GL10 gl) {
		super.draw(gl);
		if(!isLoaded) {
			txt = new TexFont(MainActivity.getAppContext(), gl);
			try {
				txt.LoadFont("TestFont.bff", gl);
				isLoaded = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(prop.getValue()) {
			txt.SetScale(1);
			txt.PrintAt(gl, toWrite, 0, 0);
		}
	}

	public GraphName getFrame() {
		return frame;
	}

	public Property getProperties() {
		return prop;
	}
}
