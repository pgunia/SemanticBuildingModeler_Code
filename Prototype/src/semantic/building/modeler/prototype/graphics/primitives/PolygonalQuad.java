package semantic.building.modeler.prototype.graphics.primitives;

import java.util.ArrayList;
import java.util.List;

import semantic.building.modeler.math.MyVectormath;
import semantic.building.modeler.math.Vertex3d;

/**
 * 
 * @author Patrick Gunia Klasse beschreibt ein abstraktes Oberflaechenelement,
 *         das durch einen Polygonzug beschrieben wird, dessen Vertices im
 *         Uhrzeigersinn definiert werden
 * 
 * 
 */

public class PolygonalQuad extends AbstractQuad {

	// ------------------------------------------------------------------------------------------

	@Override
	/**
	 * Centerberechnung bei komplexen Polygonzuegen nicht sinnvoll moeglich, um Fehler zu vermeiden, 
	 * verwende einfach die Koordinaten eines Eckpunktes 
	 */
	public void calculateCenter() {

		// extrahiere den Linienzug
		List<Vertex3d> corners = new ArrayList<Vertex3d>(mIndices.length);
		List<Vertex3d> vertices = mComplexParent.getVertices();
		for (int i = 0; i < mIndices.length; i++) {
			corners.add(vertices.get(mIndices[i]));
		}

		mCenter = MyVectormath.getInstance().calculatePolygonCenter(corners);
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "polygonalquad";
	}
	// ------------------------------------------------------------------------------------------

}
