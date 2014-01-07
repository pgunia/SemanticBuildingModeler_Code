package semantic.building.modeler.prototype.graphics.primitives;

import java.util.List;

import semantic.building.modeler.math.MyVectormath;
import semantic.building.modeler.math.Vertex3d;

/**
 * 
 * @author Patrick Gunia Klasse modelliert Quads, die nur aus 3 Punkten
 *         bestehen, also eigentlich Dreiecke sind, allerdings wird fuer die
 *         Verarbeitung die Vorstufe der Quads benoetigt. Quads dieser Art
 *         entstehen bsw. beim Import von Modells.
 * 
 */

public class TriangleQuad extends AbstractQuad {

	// ------------------------------------------------------------------------------------------
	@Override
	public void calculateCenter() {
		List<Vertex3d> vertices = getQuadVertices();
		setCenter(MyVectormath.getInstance().calculateTriangleCenter(vertices));
	}

	// ------------------------------------------------------------------------------------------
	@Override
	public String getType() {
		return "trianglequad";
	}

	// ------------------------------------------------------------------------------------------
	@Override
	/**
	 * Bei TriangleQuads ist eine Tesselation ueber GLU nicht erforderlich, da die Quads selber bereits Dreiecke sind.
	 * Reiche darum einfach die Quad-Indices durch und erzgeuge eine Triangle-Instanz fuer diese.
	 */
	public void tesselate() {
		mTriangles.clear();
		Triangle triangle = new Triangle(mIndices, this);
		mTriangles.add(triangle);

	}
	// ------------------------------------------------------------------------------------------

}
