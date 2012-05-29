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

package org.ros.android.view.visualization.layer;

import android.graphics.Bitmap;
import android.os.Handler;

import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.TextureBitmapUtilities;
import org.ros.android.view.visualization.TextureDrawable;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.rosjava_geometry.FrameTransformTree;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class OccupancyGridLayer extends SubscriberLayer<nav_msgs.OccupancyGrid> implements TfLayer {

  /**
   * Color of occupied cells in the map.
   */
  private static final int COLOR_OCCUPIED = 0xffcc1919;

  /**
   * Color of free cells in the map.
   */
  private static final int COLOR_FREE = 0xff7d7d7d;

  /**
   * Color of unknown cells in the map.
   */
  private static final int COLOR_UNKNOWN = 0xff000000;

  private final TextureDrawable occupancyGrid;

  private boolean ready;
  private GraphName frame;

  public OccupancyGridLayer(String topic) {
    this(new GraphName(topic));
  }

  public OccupancyGridLayer(GraphName topic) {
    super(topic, "nav_msgs/OccupancyGrid");
    occupancyGrid = new TextureDrawable();
    ready = false;
  }

  @Override
  public void draw(GL10 gl) {
    if (ready) {
      occupancyGrid.draw(gl);
    }
  }

  private static int[] occupancyGridToPixelArray(nav_msgs.OccupancyGrid occupancyGrid) {
    int pixels[] = new int[occupancyGrid.getData().length];
    for (int i = 0; i < pixels.length; i++) {
      if (occupancyGrid.getData()[i] == -1) {
        pixels[i] = COLOR_UNKNOWN;
      } else if (occupancyGrid.getData()[i] == 0) {
        pixels[i] = COLOR_FREE;
      } else {
        pixels[i] = COLOR_OCCUPIED;
      }
    }
    return pixels;
  }

  @Override
  public void onStart(ConnectedNode connectedNode, Handler handler, FrameTransformTree frameTransformTree,
      Camera camera) {
    super.onStart(connectedNode, handler, frameTransformTree, camera);
    getSubscriber().addMessageListener(new MessageListener<nav_msgs.OccupancyGrid>() {
      @Override
      public void onNewMessage(nav_msgs.OccupancyGrid occupancyGridMessage) {
        Bitmap occupancyGridBitmap =
            TextureBitmapUtilities.createSquareBitmap(
                occupancyGridToPixelArray(occupancyGridMessage), (int) occupancyGridMessage
                    .getInfo().getWidth(), (int) occupancyGridMessage.getInfo().getHeight(),
                COLOR_UNKNOWN);
        occupancyGrid.update(occupancyGridMessage.getInfo().getOrigin(), occupancyGridMessage
            .getInfo().getResolution(), occupancyGridBitmap);
        frame = new GraphName(occupancyGridMessage.getHeader().getFrameId());
        ready = true;
        requestRender();
      }
    });
  }

  @Override
  public GraphName getFrame() {
    return frame;
  }
}
