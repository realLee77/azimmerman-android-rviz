package org.ros.android.view.visualization;

import org.ros.android.view.visualization.shape.Color;
import org.ros.rosjava_geometry.Vector3;

public final class Utility {
	private Utility() {
	}

	/**
	 * @param a1
	 * @param a2
	 * @return
	 * @return The input parameter with the smallest magnitude
	 */
	public static <T extends Number> T min_magitude(T a1, T a2) {
		if(Math.abs((Double) a1) < Math.abs((Double) a2)) {
			return a1;
		} else {
			return a2;
		}
	}

	// Common value manipulation and comparison functions
	/**
	 * @param val
	 *            value to test
	 * @param min
	 *            minimum acceptable value
	 * @param max
	 *            maximum acceptable value
	 * @return true if val is between min and max, false otherwise
	 */
	public static <T extends Comparable<T>> boolean inRange(T val, T min, T max) {
		if(val.compareTo(min) >= 0 && val.compareTo(max) <= 0)
			return true;
		return false;
	}

	/**
	 * @param val
	 *            value to test
	 * @param max
	 *            maximum acceptable value
	 * @return val if val is less than max, max otherwise
	 */
	public static <T extends Comparable<T>> T cap(T val, T max) {
		if(val.compareTo(max) < 0) {
			return val;
		} else {
			return max;
		}
	}

	/**
	 * @param val
	 *            value to test
	 * @param min
	 *            minimum acceptable value
	 * @param max
	 *            maximum acceptable value
	 * @return the value of val, capped between min and max
	 */
	public static <T extends Comparable<T>> T cap(T val, T min, T max) {
		if(val.compareTo(max) > 0) {
			return max;
		} else if(val.compareTo(min) < 0) {
			return min;
		} else {
			return val;
		}
	}

	public static float angleWrap(float angle) {
		float retval = angle % (float) (Math.PI * 2);
		if(retval > (float) (Math.PI * 2)) {
			retval = retval - (float) (Math.PI * 2);
		}
		if(retval < 0) {
			retval = retval + (float) (Math.PI * 2);
		}
		return retval;
	}

	// Convert a ROSJava Color object to/from an Android Color integer
	private static final android.graphics.Color androidColor = new android.graphics.Color();

	public static int ColorToInt(Color c) {
		// System.out.println("Color is: " + cap((int)(c.getRed()*255),0,255) + ", " + cap((int)(c.getGreen()*255),0,255) + ", " + cap((int)(c.getBlue()*255),0,255));
		return androidColor.argb(cap((int) (c.getAlpha() * 255), 0, 255), cap((int) (c.getRed() * 255), 0, 255), cap((int) (c.getGreen() * 255), 0, 255), cap((int) (c.getBlue() * 255), 0, 255));
	}

	public static Color IntToColor(int i) {
		float r, g, b, a;
		r = androidColor.red(i) / 255f;
		g = androidColor.green(i) / 255f;
		b = androidColor.blue(i) / 255f;
		a = androidColor.alpha(i) / 255f;
		// System.out.println("Color is: " + r + ", " + g + ", " + b + ", " + a);
		return new Color(r, g, b, a);
	}

	public static Vector3 newVector3FromString(String str) {
		double x, y, z;
		Vector3 retval = null;
		String[] parts = str.split("[ ,:/]+");
		
		if(parts.length != 3)
			return null;
		
		try {
			x = Double.parseDouble(parts[0]);
			y = Double.parseDouble(parts[1]);
			z = Double.parseDouble(parts[2]);
			retval = new Vector3(x, y, z);
		} catch(NumberFormatException e) {
			return null;
		}

		return retval;
	}
}
