package org.ros.android.renderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.ros.android.renderer.shapes.Color;
import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Vector3;

import android.content.Context;

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
	
	public static Quaternion normalize(Quaternion q) {
		double length = Math.sqrt(Math.pow(q.getX(), 2) + Math.pow(q.getY(), 2) + Math.pow(q.getZ(), 2) + Math.pow(q.getW(), 2));
		q.setX(q.getX()/length);
		q.setY(q.getY()/length);
		q.setZ(q.getZ()/length);
		q.setW(q.getW()/length);
		return q;
	}
	
	public static Quaternion correctQuaternion(Quaternion q) {
		if(q.getX() == 0 && q.getY() == 0 && q.getZ() == 0 && q.getW() == 0)
			q.setW(1d);
		return q;
	}
	
	public static Vector3 getQuaternionAxis(Quaternion q) {
		return new Vector3(q.getX(), q.getY(), q.getZ());
	}
	
	public static Color colorFromMessage(std_msgs.ColorRGBA c) {
		return new Color(c.getR(), c.getG(), c.getB(), c.getA());
	}
	
	
	/**
	 * @param q
	 * @return Angle in radians
	 */
	public static float getAngle(Quaternion q) {
		return (float) (2*Math.acos(q.getW()));
	}
	
	public static Vector3 getAxis(Quaternion q) {
		double l = Math.sqrt(1 - (q.getW()*q.getW()));
		if(l > 1e-9) {
			double x = q.getX() / l;
			double y = q.getY() / l;
			double z = q.getZ() / l;
			return new Vector3(x,y,z);
		} else {
			return Vector3.zero();
		}
	}

	public static float arrayMax(float[] arr) {
		float max = Float.MIN_VALUE;
		for(float f : arr)
			max = Math.max(f, max);
		return max;
	}

	public static float arrayMin(float[] arr) {
		float min = Float.MIN_VALUE;
		for(float f : arr)
			min = Math.min(f, min);
		return min;
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
	public static int ColorToIntARGB(Color c) {
		return android.graphics.Color.argb(cap((int) (c.getAlpha() * 255), 0, 255), cap((int) (c.getRed() * 255), 0, 255), cap((int) (c.getGreen() * 255), 0, 255), cap((int) (c.getBlue() * 255), 0, 255));
	}

	public static int ColorToIntRGB(Color c) {
		return android.graphics.Color.rgb(cap((int) (c.getRed() * 255), 0, 255), cap((int) (c.getGreen() * 255), 0, 255), cap((int) (c.getBlue() * 255), 0, 255));
	}

	public static float ColorToBrightness(Color c) {
		float[] hsv = new float[3];
		android.graphics.Color.colorToHSV(ColorToIntRGB(c), hsv);
		return hsv[2];
	}

	public static Color IntToColor(int i) {
		float r, g, b, a;
		r = android.graphics.Color.red(i) / 255f;
		g = android.graphics.Color.green(i) / 255f;
		b = android.graphics.Color.blue(i) / 255f;
		a = android.graphics.Color.alpha(i) / 255f;
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

	public static String readStream(InputStream is) throws IOException {
		byte[] buffer = new byte[is.available()];
		is.read(buffer);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		os.write(buffer);
		os.close();
		is.close();
		return os.toString();
	}

	public static String assetToString(Context context, String filename) {
		try {
			return readStream(context.getAssets().open(filename));
		} catch(IOException e) {
			return null;
		}
	}
}
