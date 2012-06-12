/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.android.android_tutorial_pubsub;

import geometry_msgs.TransformStamped;

import org.ros.address.InetAddressFactory;
import org.ros.android.MessageCallable;
import org.ros.android.RosActivity;
import org.ros.android.view.RosTextView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import android.os.Bundle;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MainActivity extends RosActivity {

	private RosTextView<tf.tfMessage> rosTextView;

	// private Talker talker;

	public MainActivity() {
		// The RosActivity constructor configures the notification title and ticker
		// messages.
		super("Pubsub Tutorial", "Pubsub Tutorial");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		
//		rosTextView = (RosTextView<test_rospy.ArrayVal>) findViewById(R.id.text);
//		rosTextView.setTopicName("chatter");
//		rosTextView.setMessageType(test_rospy.ArrayVal._TYPE);
//		rosTextView.setMessageToStringCallable(new MessageCallable<String, test_rospy.ArrayVal>() {
//			@Override
//			public String call(test_rospy.ArrayVal message) {
//				System.out.println("Got message " + message.toString());
//				return message.toString();
//			}
//		});

		rosTextView = (RosTextView<tf.tfMessage>) findViewById(R.id.text);
		rosTextView.setTopicName("tf");
		rosTextView.setMessageType(tf.tfMessage._TYPE);
		rosTextView.setMessageToStringCallable(new MessageCallable<String, tf.tfMessage>() {
			@Override
			public String call(tf.tfMessage message) {
				System.out.println("Got message " + message.toString());
				for(TransformStamped t : message.getTransforms()) {
					System.out.println(t.toString());
				}
				return message.toString();
			}
		});
	}

	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {
		// talker = new Talker("TALKING");
		NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
		// At this point, the user has already been prompted to either enter the URI
		// of a master to use or to start a master locally.
		nodeConfiguration.setMasterUri(getMasterUri());
		// nodeMainExecutor.execute(talker, nodeConfiguration);
		// The RosTextView is also a NodeMain that must be executed in order to
		// start displaying incoming messages.
		nodeMainExecutor.execute(rosTextView, nodeConfiguration);
	}
}
