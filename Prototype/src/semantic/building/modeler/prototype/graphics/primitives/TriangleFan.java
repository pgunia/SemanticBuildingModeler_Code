package semantic.building.modeler.prototype.graphics.primitives;

import java.util.ArrayList;
import java.util.List;

import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.MyVectormath;
import semantic.building.modeler.math.Ray;
import semantic.building.modeler.math.Vertex3d;

/**
 * 
 * @author Patrick Gunia Instanzen dieser Klasse repraesentieren TriangleFans.
 *         Ein TriangleFan besteht dabei aus einer beliebigen Anzahl von
 *         Dreieecken, die sich alle den gleichen Mittelpunkt teilen. Wird
 *         beispielsweise für komplexe Zylinderobjekte zur Darstellung der
 *         Decken- und Bodenflaechen eingesetzt.
 * @deprecated Durch die Verwendung des Extrusionsansatzes zur Erzeugung von
 *             Zylindern etc. ist der TriangleFan nicht mehr erforderlich! Kann
 *             geloescht werden!
 * 
 */

public class TriangleFan extends AbstractQuad {

	// ------------------------------------------------------------------------------------------
	/**
	 * Default-Konstruktor: Callt ueber Superklasse Initroutine
	 */
	public TriangleFan() {
		super();
	}

	// ------------------------------------------------------------------------------------------
	@Override
	public String getType() {
		return "trianglefan";
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Aktuelle Implementation tut nichts, Instanzen dieser Klasse werden nur
	 * fuer Decke und Boden verwendet
	 */
	protected void createTextureCoords(float width, float height) {

	}

	// ------------------------------------------------------------------------------------------

	@Override
	/**
	 * Die Berechnung des Mittelpunktes basiert auf der Bestimmung von zwei Winkelhalbierenden und der Berechnung
	 * des Schnittpunktes dieser Halbierenden. Dieser Schnittpunkt ist der Mittelpunkt des Fans (zylindrische Grundkomponente).
	 */
	public void calculateCenter() {

		List<Vertex3d> vertices = mComplexParent.getVertices();

		List<Vertex3d> vertBuffer = new ArrayList<Vertex3d>(3);
		Ray winkelhalbierende0 = null, winkelhalbierende1 = null;
		Ray vert0To1 = null, vert1To2 = null;
		MyVector3f winkelhalbierendeDirection = null;

		MyVectormath mathHelper = MyVectormath.getInstance();

		// bestimme zwei Winkelhalbierende basierend auf den ersten Vertices im
		// Buffer
		for (int i = 0; i < mIndices.length; i++) {
			vertBuffer.add(vertices.get(mIndices[i]));
			if (vertBuffer.size() == 3) {
				vert0To1 = new Ray(vertBuffer.get(1), vertBuffer.get(0));
				vert1To2 = new Ray(vertBuffer.get(1), vertBuffer.get(2));
				winkelhalbierendeDirection = mathHelper
						.calculateWinkelhalbierende(vert0To1, vert1To2);

				if (winkelhalbierende0 == null) {
					winkelhalbierende0 = new Ray(vertBuffer.get(1)
							.getPosition(), winkelhalbierendeDirection);
					vertBuffer.clear();
					vert0To1 = null;
					vert1To2 = null;
				} else {
					winkelhalbierende1 = new Ray(vertBuffer.get(1)
							.getPosition(), winkelhalbierendeDirection);
					break;
				}
			}
		}

		// System.out.println(winkelhalbierende0 + " " + winkelhalbierende1);

		// berechne den Schnittpunkt der Winkelhalbierenden als Center
		mCenter = mathHelper.calculateRay2RayIntersectionApproximation(
				winkelhalbierende0, winkelhalbierende1);
		// System.out.println("CENTER TRIANGLEFAN: " + mCenter);
	}
	// ------------------------------------------------------------------------------------------

}
