package semantic.building.modeler.tesselation.math;

import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.tesselation.model.DrawingType;

/**
 * 
 * @author Patrick Gunia Klasse haelt alle Informationen vor, die fuer
 *         tesselierte Vertices benoetigt werden
 * 
 */

public class TesselationVertex extends Vertex3d {

	private DrawingType mDrawType = null;

	// ------------------------------------------------------------------------------------------

	/**
	 * Konstruktor
	 * 
	 * @param d1
	 *            x-Koordinate
	 * @param d2
	 *            y-Koordinate
	 * @param d3
	 *            z-Koordinate
	 * @param mDrawType
	 *            Art, mit der das Vertex gezeichnet bzw. mit anderen
	 *            Komponenten kombiniert wird
	 * 
	 */
	public TesselationVertex(double d1, double d2, double d3,
			DrawingType mDrawType) {
		super((float) d1, (float) d2, (float) d3);
		this.mDrawType = mDrawType;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Konstruktor
	 * 
	 * @param coords
	 *            Array mit den Koordinaten des Vertex
	 * @param mDrawType
	 *            Art, mit der das Vertex gezeichnet bzw. mit anderen
	 *            Komponenten kombiniert wird
	 */
	public TesselationVertex(double[] coords, DrawingType mDrawType) {
		super((float) coords[0], (float) coords[1], (float) coords[2]);
		this.mDrawType = mDrawType;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mDrawType
	 */
	public DrawingType getDrawType() {
		return mDrawType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return super.toString() + " DrawType=" + mDrawType;
	}

	// ------------------------------------------------------------------------------------------

}
