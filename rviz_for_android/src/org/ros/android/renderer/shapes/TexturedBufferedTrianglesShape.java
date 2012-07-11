package org.ros.android.renderer.shapes;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.Vertices;
import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import android.opengl.ETC1;
import android.opengl.ETC1Util.ETC1Texture;
import android.opengl.GLES11;
import android.opengl.GLES20;

public class TexturedBufferedTrianglesShape extends BaseShape implements CleanableShape {
	public static enum TextureSmoothing {Linear, Nearest};
	private static final Color baseColor = new Color(1f, 1f, 1f, 1f);

	private List<Integer> texIDArray = new ArrayList<Integer>();
	private Map<String, ETC1Texture> textures;
	protected int count;

	private boolean texturesLoaded = false;
	private TextureSmoothing smoothing = TextureSmoothing.Linear;
	
	private boolean bufferPrepared = false;
	private FloatBuffer vertexBuffer;
	
	public TexturedBufferedTrianglesShape(float[] vertices, float[] normals, float[] uvs, ETC1Texture diffuseTexture) {
		super.setColor(baseColor);
		this.textures = new HashMap<String, ETC1Texture>();
		this.textures.put("diffuse", diffuseTexture);
		vertexBuffer = packBuffer(vertices,normals,uvs);
		setTransform(new Transform(new Vector3(0, 0, 0), new Quaternion(0, 0, 0, 1)));
	}
	
	public TexturedBufferedTrianglesShape(float[] vertices, float[] normals, float[] uvs, Map<String, ETC1Texture> textures) {
		super.setColor(baseColor);
		this.textures = textures;
		vertexBuffer = packBuffer(vertices,normals,uvs);
		setTransform(new Transform(new Vector3(0, 0, 0), new Quaternion(0, 0, 0, 1)));
	}
	
	private static final int FLOAT_SIZE = Float.SIZE/8;
	private static final int NUM_VERTEX = 3;
	private static final int NUM_NORMAL = 3;
	private static final int NUM_UV = 2;
	private static final int FLOATS_PER_VERTEX = NUM_VERTEX+NUM_NORMAL+NUM_UV;
	private static final int STRIDE = FLOATS_PER_VERTEX*FLOAT_SIZE;
	
	private FloatBuffer packBuffer(float[] vertices, float[] normals, float[] uvs) {
		if(vertices.length != normals.length || vertices.length/3 != uvs.length/2)
			throw new IllegalArgumentException("Vertex, normal, and UV arrays must describe the same number of vertices");
		
		bufferPrepared = false;
		int numVertices = vertices.length/3;
		count = vertices.length / 3;
		float[] packedBuffer = new float[numVertices*FLOATS_PER_VERTEX];

		int vIdx = 0, nIdx = 0, uIdx = 0;
		
		for(int i = 0; i < numVertices; i++) {
			int idx = i*FLOATS_PER_VERTEX;
			packedBuffer[idx+0] = vertices[vIdx++];
			packedBuffer[idx+1] = vertices[vIdx++];
			packedBuffer[idx+2] = vertices[vIdx++];
			
			packedBuffer[idx+3] = normals[nIdx++];
			packedBuffer[idx+4] = normals[nIdx++];
			packedBuffer[idx+5] = normals[nIdx++];
			
			packedBuffer[idx+6] = uvs[uIdx++];
			packedBuffer[idx+7] = uvs[uIdx++];
		}

		return Vertices.toFloatBuffer(packedBuffer);
	}
	
	public void setTextureSmoothing(TextureSmoothing s) {
		this.smoothing = s;
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
	
	private void unloadTextures(GL10 gl) {
		for(Integer i : texIDArray) {
			tmp[0] = i;
			gl.glDeleteTextures(1, tmp, 0); 
		}
		texIDArray.clear();
	}
	
	private int createVertexBuffer(GL10 gl) {
		final int[] buffers = new int[1];
		GLES11.glGenBuffers(1, buffers, 0);
		GLES11.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);
		GLES11.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexBuffer.capacity()*FLOAT_SIZE, vertexBuffer, GLES20.GL_STATIC_DRAW);
		
		bufferPrepared = true;
		return buffers[0];
	}
	
	private void destroyBuffer(GL10 gl) {
		int[] buffers = new int[1];
		buffers[0] = bufferIdx;
		GLES11.glDeleteBuffers(1, buffers, 0);
	}
	
	private int bufferIdx;
	private static final int VERTEX_OFFSET = 0;
	private static final int NORMAL_OFFSET = NUM_VERTEX*FLOAT_SIZE;
	private static final int UV_OFFSET = (NUM_NORMAL+NUM_VERTEX)*FLOAT_SIZE;
	private volatile boolean cleanUp = false;
	
	@Override
	public void draw(GL10 glUnused, Camera cam) {		
		if(cleanUp) {
			clearBuffers(glUnused);
			return;
		}	
		if(!bufferPrepared)
			bufferIdx = createVertexBuffer(glUnused);
		if(!texturesLoaded)
			loadTextures(glUnused);
		
		GLES11.glColor4f(getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor().getAlpha());

		GLES11.glBindBuffer(GL11.GL_ARRAY_BUFFER, bufferIdx);
		
		GLES11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		GLES11.glVertexPointer(3, GL11.GL_FLOAT, STRIDE, VERTEX_OFFSET);
		
        GLES11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
        GLES11.glNormalPointer(GL11.GL_FLOAT, STRIDE, NORMAL_OFFSET);
        
		glUnused.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		for(int i : texIDArray)
			glUnused.glBindTexture(GL10.GL_TEXTURE_2D, i);
		GLES11.glTexCoordPointer(2, GL11.GL_FLOAT, STRIDE, UV_OFFSET);
        
		super.draw(glUnused, cam);
        GLES11.glDrawArrays(GL10.GL_TRIANGLES, 0, count);
        
		glUnused.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        GLES11.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		GLES11.glDisableClientState(GL10.GL_NORMAL_ARRAY);
		GLES11.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
	}
	
	private boolean cleaned = false;
	
	private void clearBuffers(GL10 gl) {
		if(!cleaned) {
			int[] tmp = new int[0];
			tmp[0] = bufferIdx;
			GLES11.glDeleteBuffers(1,tmp,0);
			for(int i : texIDArray) {
				tmp[0] = i;
				GLES11.glDeleteTextures(1, tmp, 0);
			}
			cleaned = true;
		}
	}

	@Override
	public void cleanup() {
		cleanUp = true;
	}
}
