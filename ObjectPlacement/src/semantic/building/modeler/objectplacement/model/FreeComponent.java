package semantic.building.modeler.objectplacement.model;

import java.util.List;

import semantic.building.modeler.math.Vertex3d;

public class FreeComponent extends AbstractComponent {

	// ------------------------------------------------------------------------------------------
	/**
	 * Konstruktor mit Uebergabe der Vertices, die die Komponente beschreiben,
	 * definiert im Uhrzeigersinn.
	 * 
	 * @param corners
	 *            Liste von Vertex-Strukturen, definiert im Uhrzeigersinn
	 */
	public FreeComponent(List<Vertex3d> corners) {
		super(corners);
		mCenter = mPolygon.getCenter();

	}

	// ------------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "FreeComponent";
	}
	// ------------------------------------------------------------------------------------------

}
