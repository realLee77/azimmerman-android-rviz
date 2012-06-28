package org.ros.android.rviz_for_android.urdf;
import java.util.LinkedList;
import java.util.List;

import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

public abstract class VTDXmlReader {

	private AutoPilot ap;
	private VTDNav vn;

	public VTDXmlReader() {
	}

	protected void parse(String xml) throws Exception {
		final VTDGen vg = new VTDGen();
		vg.setDoc(xml.getBytes());
		vg.parse(false);

		vn = vg.getNav();
		ap = new AutoPilot(vn);
	}

	protected void getExpression(String... xPathExpression) {
		try {
			ap.selectXPath(Compose(xPathExpression));
		} catch(XPathParseException e) {
			e.printStackTrace();
		}
	}

	protected String Compose(String... pieces) {
		if(pieces.length == 1)
			return pieces[0];

		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < pieces.length; i++) {
			sb.append(pieces[i]);
			if(i < pieces.length - 1) {
				sb.append("/");
			}
		}
		return sb.toString();
	}

	protected List<String> getAttributeList(String... xPathExpression) {
		List<String> retval = new LinkedList<String>();
		getExpression(xPathExpression);
		int i;
		try {
			while((i = ap.evalXPath()) != -1) {
				retval.add(vn.toString(i + 1));
			}
		} catch(XPathEvalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(NavException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ap.resetXPath();
		return retval;
	}

	protected String getSingleAttribute(String... xPathExpression) {
		getExpression(xPathExpression);
		String result = null;
		try {
			result = vn.toString(ap.evalXPath() + 1);
			if(ap.evalXPath() != -1)
				throw new IllegalArgumentException("Expression returned multiple results!");
		} catch(XPathEvalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(NavException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ap.resetXPath();
		return result;
	}

	protected String getSingleContents(String... xPathExpression) {
		getExpression(xPathExpression);
		String result = null;
		try {
			result = vn.toString(ap.evalXPath());
			if(ap.evalXPath() != -1)
				throw new IllegalArgumentException("Expression returned multiple results!");
		} catch(XPathEvalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(NavException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ap.resetXPath();
		return result;
	}

	// TODO: There's probably a better way of doing this!
	protected int nodeCount(String... xPathExpression) {
		return getAttributeList(xPathExpression).size();
	}

	protected boolean nodeExists(String... xPathExpression) {
		boolean result = false;
		getExpression(xPathExpression);
		try {
			result = (ap.evalXPath() != -1);
			ap.resetXPath();
		} catch(XPathEvalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(NavException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	protected short[] toShortArray(String str) {
		String[] pieces = str.split(" ");
		short[] retval = new short[pieces.length];
		for(int i = 0; i < pieces.length; i++) {
			retval[i] = Short.parseShort(pieces[i]);
		}
		return retval;
	}

	protected float[] toFloatArray(String str) {
		String[] pieces = str.split(" ");
		float[] retval = new float[pieces.length];
		for(int i = 0; i < pieces.length; i++) {
			retval[i] = Float.parseFloat(pieces[i]);
		}
		return retval;
	}
}
