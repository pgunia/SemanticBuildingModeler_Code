package semantic.building.modeler.prototype.graphics.complex;

import semantic.building.modeler.math.MyPolygon;
import semantic.building.modeler.math.MyVector3f;

/**
 * 
 * @author Patrick Gunia Klasse berechnet Boundingboxes fuer Punktwolken im
 *         3d-Raum, die fuer eine grobe Bestimmung der Ausdehnungen und des
 *         Mittelpunktes des Objektes einegesetzt werden, zu der die Punktwolke
 *         gehoert
 * 
 */

public class AABB extends BoundingBox {

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet eine Bounding-Box fuer ein Quad. Abhaengig von der
	 * Ausrichtung des Quads muessen die Dimensionen unterschiedlich
	 * interpretiert werden, bsw. Ausrichtung RIGHT oder LEFT ist die z-Achse
	 * die Breite etc. 0: Hoehe (y-Achse), 1: Laenge (x-Achse), 2: Breite
	 * (z-Achse)
	 */
	public AABB() {
		super();

		// Hoehenachse
		mAxes.add(new MyVector3f(0.0f, 1.0f, 0.0f));

		// Laengenachse
		mAxes.add(new MyVector3f(1.0f, 0.0f, 0.0f));

		// Breitenachse
		mAxes.add(new MyVector3f(0.0f, 0.0f, 1.0f));

	}

	// ------------------------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "BoundingBox: Center: " + mCenter + ", Width: " + getWidth()
				+ ", Height: " + getHeight() + ", Length: " + getLength();
	}

	// ------------------------------------------------------------------------------------------
	@Override
	void update() {
		MyPolygon currentPoly = null;
		for (int i = 0; i < mFaces.size(); i++) {
			currentPoly = mFaces.get(i);

			// aktualisiere das Poly mit den eigenen transformierten Vertices
			// (ist erforderlich, da sich durch die Transformation alle Ebenen
			// und Strahlen etc. geaendert haben)
			currentPoly.update(currentPoly.getVertices());
		}
	}

	// ------------------------------------------------------------------------------------------

}
