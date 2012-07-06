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

/**
 * @author azimmerman
 *
 */
public class TexturedTrianglesShape extends TrianglesShape implements CleanableShape {
	public static enum TextureSmoothing {Linear, Nearest};
	private static final Color baseColor = new Color(1f, 1f, 1f, 1f);

	protected final FloatBuffer uv;
	private List<Integer> texIDArray = new ArrayList<Integer>();
	private Map<String, ETC1Texture> textures;

	private boolean texturesLoaded = false;
	private TextureSmoothing smoothing = TextureSmoothing.Linear;

	private boolean cleanUp = false;
	
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
	
	/**
	 * Determines which texture smoothing (TEXTURE_MIN_FILTER and TEXTURE_MAG_FILTER) modes are used for this shape.
	 * This must be called before the shape is first drawn to have any effect.
	 * @param s The smoothing mode to use
	 */
	public void setTextureSmoothing(TextureSmoothing s) {
		this.smoothing = s;
	}

	@Override
	public void draw(GL10 gl) {		
		if(cleanUp) {
			clearBuffers(gl);
			return;
		}
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
		        if(smoothing == TextureSmoothing.Linear) {
		        	gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
					gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
		        } else if(smoothing == TextureSmoothing.Nearest) {
		    	    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
		    	    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
		        }
			    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
			    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
			}
		}
		
		texturesLoaded = true;
		textures = null;
	}
	
	public void cleanup() {
		cleanUp = true;
	}
	
	/**
	 * Clear the buffered textures which have been loaded
	 * @param gl
	 */
	private boolean cleaned = false;
	private void clearBuffers(GL10 gl) {
		if(!cleaned) {
			for(Integer i : texIDArray) {
				tmp[0] = i;
				gl.glDeleteTextures(1, tmp, 0); 
			}
			texIDArray.clear();
			cleaned = true;
		}
	}
}
