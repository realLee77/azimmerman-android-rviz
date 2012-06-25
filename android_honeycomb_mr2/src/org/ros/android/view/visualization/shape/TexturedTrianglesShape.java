package org.ros.android.view.visualization.shape;

import java.nio.FloatBuffer;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.view.visualization.Vertices;

import android.opengl.ETC1;
import android.opengl.ETC1Util;
import android.opengl.ETC1Util.ETC1Texture;
import android.opengl.GLES10;
import android.util.Log;

public class TexturedTrianglesShape extends TrianglesShape {
	private static final Color baseColor = new Color(1f, 1f, 1f, 1f);

	protected final FloatBuffer uv;
	private int[] texID;
	private Map<String, ETC1Texture> textures;

	private boolean texturesLoaded = false;

	public TexturedTrianglesShape(float[] vertices, float[] normals, float[] uvs, Map<String, ETC1Texture> textures) {
		super(vertices, normals, baseColor);
		uv = Vertices.toFloatBuffer(uvs);
		this.textures = textures;

		// TODO: Allow multiple textures for normal mapping
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

		// Remove the texture from the map. Once it's loaded to the GPU, it isn't needed anymore
		ETC1Texture tex = textures.remove("diffuse");
		
		// TODO: Determine why ETC1Util returns a corrupted texture
        //ETC1Util.loadTexture(GL10.GL_TEXTURE_2D, 0, 0, GL10.GL_RGB, GL10.GL_UNSIGNED_SHORT_5_6_5, tex);
		
        gl.glBindTexture(GL10.GL_TEXTURE_2D, texID[0]);
        gl.glCompressedTexImage2D(GL10.GL_TEXTURE_2D, 0, ETC1.ETC1_RGB8_OES, tex.getWidth(), tex.getHeight(), 0, tex.getData().capacity(), tex.getData());
        
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
		
		texturesLoaded = true;
	}
}
