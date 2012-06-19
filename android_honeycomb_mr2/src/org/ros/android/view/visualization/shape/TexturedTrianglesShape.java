package org.ros.android.view.visualization.shape;

import java.nio.FloatBuffer;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.view.visualization.Vertices;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.opengl.GLUtils;

public class TexturedTrianglesShape extends TrianglesShape {
	private static final Color baseColor = new Color(1f, 1f, 1f, 1f);

	protected final FloatBuffer uv;
	private int[] texID;
	private Map<String, Bitmap> textures;

	private boolean texturesLoaded = false;

	public TexturedTrianglesShape(float[] vertices, float[] normals, float[] uvs, Map<String, Bitmap> textures) {
		super(vertices, normals, baseColor);
		uv = Vertices.toFloatBuffer(uvs);
		this.textures = textures;

		// TODO: Allow multiple textures for normal mapping
		//texID = new int[textures.keySet().size()];
		texID = new int[1];
	}

	@Override
	public void draw(GL10 gl) {
		super.setColor(baseColor);
		if(!texturesLoaded)
			loadTextures(gl);

		gl.glEnable(GL10.GL_TEXTURE_2D);
		for(Integer i : texID)
			gl.glBindTexture(GL10.GL_TEXTURE_2D, i);

		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, uv);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

		super.draw(gl);

		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glDisable(GL10.GL_TEXTURE_2D);
	}

	private void loadTextures(GL10 gl) {
		gl.glGenTextures(1, texID, 0);

		// texID[0] = diffuse, texID[1] = normals (if used)
		for(int i = 0; i < texID.length; i++) {
			gl.glBindTexture(GL10.GL_TEXTURE_2D, texID[i]);

			Bitmap tmpImage = null;
			switch(i) {
			case 0:
				tmpImage = textures.get("diffuse");
				break;
			case 1:
				tmpImage = textures.get("bump");
				break;
			}

			Matrix flip = new Matrix();
			flip.postScale(1f, -1f);
			Bitmap flipped = Bitmap.createBitmap(tmpImage, 0, 0, tmpImage.getWidth(), tmpImage.getHeight(), flip, true);
			tmpImage.recycle();

			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, flipped, 0);

			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);

			flipped.recycle();
		}
		texturesLoaded = true;
	}
}
