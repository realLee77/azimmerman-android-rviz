package org.ros.android.renderer.shapes;

/**
 * Implemented by shapes which require cleanup when they're no longer needed
 * @author azimmerman
 *
 */
public interface CleanableShape {
	public void cleanup();
}
