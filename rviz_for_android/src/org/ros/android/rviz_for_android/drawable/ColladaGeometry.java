package org.ros.android.rviz_for_android.drawable;
import java.util.ArrayList;
import java.util.List;

public class ColladaGeometry {
	public static enum GEOTYPE {
		triangles, tristrips, trifans
	};

	private List<short[]> indices = new ArrayList<short[]>();
	private List<GEOTYPE> type = new ArrayList<GEOTYPE>();

	private float[] normals;
	private float[] vertices;

	public ColladaGeometry(float[] normals, float[] vertices) {
		this.normals = normals;
		this.vertices = vertices;
	}

	public short[] getIndices(int i) {
		return indices.get(i);
	}

	public float[] getNormals() {
		return normals;
	}

	public float[] getVertices() {
		return vertices;
	}

	public GEOTYPE getType(int i) {
		return type.get(i);
	}

	public void addPart(short[] indices, String type) {
		this.indices.add(indices);
		this.type.add(GEOTYPE.valueOf(type));
	}
}
