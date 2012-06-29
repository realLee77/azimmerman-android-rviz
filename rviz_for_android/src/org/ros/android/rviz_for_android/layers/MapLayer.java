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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.opengles.GL10;

import nav_msgs.OccupancyGrid;

import org.ros.android.rviz_for_android.drawable.Plane;
import org.ros.android.rviz_for_android.prop.BoolProperty;
import org.ros.android.rviz_for_android.prop.GraphNameProperty;
import org.ros.android.rviz_for_android.prop.LayerWithProperties;
import org.ros.android.rviz_for_android.prop.Property;
import org.ros.android.rviz_for_android.prop.ReadOnlyProperty;
import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.layer.SubscriberLayer;
import org.ros.android.view.visualization.layer.TfLayer;
import org.ros.android.view.visualization.shape.TexturedTrianglesShape;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.opengl.ETC1;
import android.opengl.ETC1Util.ETC1Texture;
import android.os.Handler;
import android.util.Log;

public class MapLayer extends SubscriberLayer<nav_msgs.OccupancyGrid> implements LayerWithProperties, TfLayer {

	private static int MAX_TEXTURE_WIDTH = 1024;
	private static int MAX_TEXTURE_HEIGHT = 1024;

	private int wTileCount;
	private int hTileCount;
	private float wTileScale;
	private float hTileScale;

	private Plane[][] tiles;

	private BoolProperty prop;

	private boolean isReady = false;

	public MapLayer(GraphName topicName, String messageType) {
		super(topicName, messageType);

		prop = new BoolProperty("Enabled", true, null);
		prop.addSubProperty(new GraphNameProperty("Parent", null, null, null));
		prop.addSubProperty(new ReadOnlyProperty("Status", "OK", null));
	}

	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler, FrameTransformTree frameTransformTree, Camera camera) {
		super.onStart(connectedNode, handler, frameTransformTree, camera);
		Subscriber<nav_msgs.OccupancyGrid> sub = getSubscriber();
		sub.addMessageListener(new MessageListener<OccupancyGrid>() {
			@Override
			public void onNewMessage(OccupancyGrid arg0) {
				isReady = false;
				generateMapTiles(arg0);
				isReady = true;
			}
		});
		prop.<GraphNameProperty> getProperty("Parent").setTransformTree(frameTransformTree);
	}

	private Bitmap mapImage;
	private Bitmap tileImage;
	private Canvas canvas;

	private void generateMapTiles(OccupancyGrid msg) {
		int u = msg.getInfo().getWidth();
		int v = msg.getInfo().getHeight();
		float density = msg.getInfo().getResolution();

		wTileCount = (u / MAX_TEXTURE_WIDTH) + 1;
		hTileCount = (v / MAX_TEXTURE_HEIGHT) + 1;

		wTileScale = density * MAX_TEXTURE_WIDTH;
		hTileScale = density * MAX_TEXTURE_HEIGHT;

		Log.d("Map", "Tile grid is " + wTileCount + " x " + hTileCount + " with " + wTileScale + " x " + hTileScale + " tiles.");

		tiles = new Plane[hTileCount][wTileCount];

		initTextures(u, v, msg.getData());

		for(int col = 0; col < wTileCount; col++) {
			for(int row = 0; row < hTileCount; row++) {
				tiles[row][col] = new Plane(getTileTexture(row, col));
				Transform tileTransform = new Transform(new Vector3(wTileScale * col, hTileScale * row, 0), Quaternion.newIdentityQuaternion());
				tiles[row][col].setTransform(tileTransform);
				tiles[row][col].setScale(wTileScale, hTileScale);
				tiles[row][col].setTextureSmoothing(TexturedTrianglesShape.TextureSmoothing.Nearest);
			}
		}
	}

	private static final int BLACK = Color.argb(255, 0, 0, 0);
	private static final Paint paint = new Paint();

	/**
	 * Currently this uses ETC1 compressed textures with black for any unused portions of the tile. Transparency isn't supported by ETC1 compression, which is the only compression mode guaranteed to work on all Android devices with OpenGL ES 2.0 support
	 */
	private ETC1Texture getTileTexture(int row, int col) {
		// Fill the tile with black (background color)
		canvas.clipRect(0, 0, canvas.getWidth(), canvas.getHeight());
		canvas.drawColor(BLACK);

		// Copy the section of the map image into the tile
		int top = mapImage.getHeight() - Math.min((row + 1) * MAX_TEXTURE_HEIGHT, mapImage.getHeight());
		int bottom = mapImage.getHeight() - row * MAX_TEXTURE_HEIGHT;
		int left = col * MAX_TEXTURE_WIDTH;
		int right = Math.min(left + MAX_TEXTURE_WIDTH, mapImage.getWidth());

		Rect src = new Rect(left, top, right, bottom);
		Rect dst = new Rect(0, 0, src.width(), src.height());
		dst.offset(0, MAX_TEXTURE_HEIGHT - dst.height());

		canvas.drawBitmap(mapImage, src, dst, paint);

		// Compress the tile
		return compressBitmap(tileImage);
	}

	private ETC1Texture compressBitmap(Bitmap uncompressedBitmap) {
		// Copy the bitmap to a byte buffer
		ByteBuffer uncompressedBytes = ByteBuffer.allocateDirect(uncompressedBitmap.getByteCount()).order(ByteOrder.nativeOrder());
		uncompressedBitmap.copyPixelsToBuffer(uncompressedBytes);
		uncompressedBytes.position(0);

		int width = uncompressedBitmap.getWidth();
		int height = uncompressedBitmap.getHeight();

		// Compress the texture
		int encodedSize = ETC1.getEncodedDataSize(width, height);
		ByteBuffer compressed = ByteBuffer.allocateDirect(encodedSize).order(ByteOrder.nativeOrder());
		ETC1.encodeImage(uncompressedBytes, width, height, 2, 2 * width, compressed);

		ETC1Texture retval = new ETC1Texture(width, height, compressed);

		return retval;
	}

	private void initTextures(int width, int height, byte[] data) {
		if(mapImage != null)
			mapImage.recycle();
		if(tileImage != null)
			tileImage.recycle();

		// Map image must be stored using 2 bytes/pixel because of compression constraints
		mapImage = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		tileImage = Bitmap.createBitmap(MAX_TEXTURE_WIDTH, MAX_TEXTURE_HEIGHT, Bitmap.Config.RGB_565);
		canvas = new android.graphics.Canvas(tileImage);

		// Copy the message data into a bitmap, flipping vertically
		for(int u = 0; u < width; u++) {
			for(int v = 0; v < height; v++) {
				int color = data[v * width + u];
				if(color == 100)
					color = 0;
				else if(color == 0)
					color = 255;
				else
					color = 127;

				mapImage.setPixel(u, height - v - 1, Color.argb(255, color, color, color));
			}
		}
	}

	/*	private void saveBitmap(String filename, Bitmap image) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			image.compress(CompressFormat.JPEG, 80, baos);

			File f = new File(Environment.getExternalStorageDirectory() + File.separator + filename);
			try {
				f.createNewFile();
				FileOutputStream out = new FileOutputStream(f);
				out.write(baos.toByteArray());
				out.close();
			} catch(IOException e) {
				Log.e("Map", "Error saving JPG!");
				e.printStackTrace();
			}
		}*/

	@Override
	public void draw(GL10 gl) {
		if(isReady) {
			super.draw(gl);
			for(Plane[] pRow : tiles) {
				for(Plane p : pRow)
					p.draw(gl);
			}
		}
	}

	@Override
	public boolean isEnabled() {
		return prop.getValue();
	}

	@Override
	public Property<?> getProperties() {
		return prop;
	}

	@Override
	public GraphName getFrame() {
		return prop.<GraphNameProperty> getProperty("Parent").getValue();
	}

}