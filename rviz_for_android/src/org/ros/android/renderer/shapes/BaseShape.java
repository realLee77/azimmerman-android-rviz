package org.ros.android.renderer.shapes;

import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.SelectionManager;
import org.ros.android.renderer.layer.Selectable;
import org.ros.android.rviz_for_android.drawable.GLSLProgram;
import org.ros.android.rviz_for_android.drawable.GLSLProgram.ShaderVal;
import org.ros.rosjava_geometry.Transform;

import android.opengl.Matrix;

import com.google.common.base.Preconditions;

/**
 * Defines the getters and setters that are required for all {@link Shape} implementors.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 */
public abstract class BaseShape implements Shape, Selectable {

	private static final Transform defaultTransform = Transform.newIdentityTransform();
	private static final Color defaultColor = new Color(0.5f, 0.5f, 0f, 1f);
	
	protected Camera cam;
	protected Color color = defaultColor;
	protected Transform transform = defaultTransform;
	protected GLSLProgram shader;
	protected int[] uniformHandles;
	protected float[] MVP = new float[16];
	protected float[] MV = new float[16];
	public static float[] lightPosition = new float[]{3f, 4f, 5f};
	public static float[] lightVector = new float[]{0.4242f, 0.5656f, 0.7071f};
	
	public BaseShape(Camera cam) {
		this.cam = cam;
	}
	
	public void setProgram(GLSLProgram shader) {
		this.shader = shader;
		uniformHandles = shader.getUniformHandles();
	}
	
	protected int getUniform(ShaderVal s) {
		return uniformHandles[s.loc];
	}
	
	@Override
	public void draw(GL10 glUnused) {
		if(!shader.isCompiled()) {
			shader.compile(glUnused);
			uniformHandles = shader.getUniformHandles();
		}
		shader.use(glUnused);
		cam.applyTransform(transform);
		scale(cam);
	}

	/**
	 * Scales the coordinate system.
	 * 
	 * <p>
	 * This is called after transforming the surface according to {@link #transform}.
	 * 
	 * @param gl
	 */
	protected void scale(Camera cam) {
		// The default scale is in metric space.
	}

	@Override
	public Color getColor() {
		return color;
	}

	@Override
	public void setColor(Color color) {
		Preconditions.checkNotNull(color);
		this.color = color;
	}
	
	protected void calcMVP() {
		Matrix.multiplyMM(MV, 0, cam.getViewMatrix(), 0, cam.getModelMatrix(), 0);
		Matrix.multiplyMM(MVP, 0, cam.getViewport().getProjectionMatrix(), 0, MV, 0);
	}

	@Override
	public Transform getTransform() {
		Preconditions.checkNotNull(transform);
		return transform;
	}

	@Override
	public void setTransform(Transform pose) {
		this.transform = pose;
	}
	
	private Color selectedTemp = null;
	@Override
	public void setSelected(boolean isSelected) {
		if(isSelected) {
			selectedTemp = getColor();
			setColor(SelectionManager.selectedColor);
		} else {
			setColor(selectedTemp);
		}
	}
	
	private Color tmpColor;
	private GLSLProgram tmpShader;
	private Color selectionColor = SelectionManager.backgroundColor;

	public void registerSelectable() {
		selectionColor = cam.getSelectionManager().registerSelectable(this);
	}
	
	public void removeSelectable() {
		selectionColor = cam.getSelectionManager().removeSelectable(this); 
	}
	
	@Override
	public void selectionDraw(GL10 glUnused) {
		tmpColor = color;
		tmpShader = shader;
		shader = GLSLProgram.FlatColor();
		uniformHandles = shader.getUniformHandles();
		
		color = selectionColor;
		
		draw(glUnused);
	}
	
	protected void selectionDrawCleanup() {
		shader = tmpShader;
		color = tmpColor;
		uniformHandles = shader.getUniformHandles();
	}
	
	@Override
	public Map<String, String> getInfo() {
		// TODO Auto-generated method stub
		return null;
	}
}