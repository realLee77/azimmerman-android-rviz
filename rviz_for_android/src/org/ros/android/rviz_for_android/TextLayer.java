package org.ros.android.rviz_for_android;

import java.io.IOException;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.layer.SubscriberLayer;
import org.ros.android.view.visualization.layer.TfLayer;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;
import org.ros.rosjava_geometry.FrameTransformTree;

import android.os.Handler;

public class TextLayer extends SubscriberLayer<std_msgs.String> implements TfLayer {
	
	private String toWrite;
	
	private GraphName frame;
	private TexFont txt;
	private boolean isLoaded = false;

	public TextLayer(GraphName topicName, String messageType) {
		super(topicName, messageType);
		
		toWrite = "I'm listening for " + messageType + " messages on " + topicName.toString();
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
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			isLoaded = true;
		}
		txt.SetScale(1);
		txt.PrintAt(gl, toWrite, 0, 0);
	}

	public GraphName getFrame() {
		return frame;
	}
}
