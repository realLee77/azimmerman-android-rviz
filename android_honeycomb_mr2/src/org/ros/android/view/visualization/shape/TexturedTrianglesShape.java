package org.ros.android.view.visualization.shape;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.view.visualization.Vertices;

import android.opengl.ETC1;
import android.opengl.ETC1Util.ETC1Texture;

public class TexturedTrianglesShape extends TrianglesShape {
	private static final Color baseColor = new Color(1f, 1f, 1f, 1f);

	protected final FloatBuffer uv;
	private List<Integer> texIDArray = new ArrayList<Integer>();
	private Map<String, ETC1Texture> textures;

	private boolean texturesLoaded = false;

	public TexturedTrianglesShape(float[] vertices, float[] normals, float[] uvs, ETC1Texture diffuseTexture) {
		super(vertices, normals, baseColor);
		uv = Vertices.toFloatBuffer(uvs);
		this.textures = new HashMap<String, ETC1Texture>();
		this.textures.put("diffuse", diffuseTexture);
	}
	
	public TexturedTrianglesShape(float[] vertices, float[] normals, float[] uvs, Map<String, ETC1Texture> textures) {
		super(vertices, normals, baseColor);
		uv = Vertices.toFloatBuffer(uvs);
		this.textures = textures;
	}

	@Override
	public void draw(GL10 gl) {
		gl.glPushMatrix();
		super.setColor(baseColor);
		if(!texturesLoaded)
			loadTextures(gl);

		gl.glEnable(GL10.GL_TEXTURE_2D);
		for(Integer i : texIDArray)
			gl.glBindTexture(GL10.GL_TEXTURE_2D, i);

		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, uv);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

		super.draw(gl);

		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glPopMatrix();
	}

	private int[] tmp = new int[1];
	private void loadTextures(GL10 gl) {	
		for(String s : textures.keySet()) {
			// Remove the texture from the map. Once it's loaded to the GPU, it isn't needed anymore
			ETC1Texture tex = textures.remove(s);
			
			if(tex != null) {
				// Generate a texture ID, append it to the list
				gl.glGenTextures(1, tmp, 0);
				texIDArray.add(tmp[0]);
				
				// Bind and load the texture
		        gl.glBindTexture(GL10.GL_TEXTURE_2D, tmp[0]);
		        gl.glCompressedTexImage2D(GL10.GL_TEXTURE_2D, 0, ETC1.ETC1_RGB8_OES, tex.getWidth(), tex.getHeight(), 0, tex.getData().capacity(), tex.getData());
		        
		        // UV mapping parameters
				gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
				gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
				gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
				gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
			}
		}
		
		texturesLoaded = true;
		textures = null;
	}
}
