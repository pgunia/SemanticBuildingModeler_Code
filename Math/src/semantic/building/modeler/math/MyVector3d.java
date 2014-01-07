package semantic.building.modeler.math;

import javax.vecmath.Tuple3d;

/**
 * 
 * @author Patrick Gunia Double-Implementation der Vektor-Klasse
 * 
 */

public class MyVector3d extends Tuple3d {

	/**
	 * 
	 */
	public MyVector3d() {
		super();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 */
	public MyVector3d(double arg0, double arg1, double arg2) {
		super(arg0, arg1, arg2);
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param arg0
	 */
	public MyVector3d(double[] arg0) {
		super(arg0);
	}
	// ------------------------------------------------------------------------------------------

}
