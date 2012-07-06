package org.ros.android.view.visualization.shape;

/**
 * Implemented by shapes which require cleanup when they're no longer needed
 * @author azimmerman
 *
 */
public interface CleanableShape {
	public void cleanup();
}
