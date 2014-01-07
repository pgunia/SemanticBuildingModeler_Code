package semantic.building.modeler.prototype.graphics.complex;

import java.util.List;
import java.util.Map;

import processing.core.PApplet;
import semantic.building.modeler.configurationservice.model.enums.FloorPosition;
import semantic.building.modeler.configurationservice.model.enums.Side;
import semantic.building.modeler.math.MyPolygon;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.prototype.graphics.primitives.AbstractQuad;
import semantic.building.modeler.prototype.graphics.primitives.PolygonalQuad;

/**
 * 
 * @author Patrick Gunia Instanzen dieser Klasse erzeugen Stockwerke, Saeulen
 *         anstelle von Wanden verwenden. Solche Stockwerke werden bsw. bei
 *         Tempeln etc. eingesetzt.
 * 
 */

public class OpenFloorComplex extends FloorComplex {

	/**
	 * @param parent
	 *            PApplet-Instanz zum Zeichnen in die Renderare
	 * @param vertices
	 *            Polygonzug, der den Grundriss des Stockwerks definiert
	 * @param height
	 *            Hoehe des Stockwerks
	 * @param directionToSideMap
	 *            Map-Instanz, die einer Menge von Normalenvektoren die
	 *            jeweiligen Richtungsenums zuweist
	 * @param floorPosition
	 *            Enum-Instanz, die die Position des Stockwerks beschreibt
	 * @param floorPositionIndex
	 *            Index des Stockwerks im "Stockwerk-Stack"
	 * @param isTop
	 *            Flag zeigt an, ob das uebergebene Polygon die Deckenflaeche
	 *            beschreibt
	 */
	public OpenFloorComplex(PApplet parent, List<Vertex3d> vertices,
			Float height, Map<MyVector3f, Side> directionToSideMap,
			FloorPosition floorPosition, Integer floorPositionIndex,
			boolean isTop) {
		super(parent, vertices, height, directionToSideMap, floorPosition,
				floorPositionIndex, isTop);

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * @param parent
	 *            PApplet-Instanz zum Zeichnen in die Renderare
	 * @param footprint
	 *            Polygon, das den Grundriss des Stockwerks definiert
	 * @param height
	 *            Hoehe des Stockwerks
	 * @param directionToSideMap
	 *            Map-Instanz, die einer Menge von Normalenvektoren die
	 *            jeweiligen Richtungsenums zuweist
	 * @param floorPosition
	 *            Enum-Instanz, die die Position des Stockwerks beschreibt
	 * @param floorPositionIndex
	 *            Index des Stockwerks im "Stockwerk-Stack"
	 * @param isTop
	 *            Flag zeigt an, ob das uebergebene Polygon die Deckenflaeche
	 *            beschreibt
	 */
	public OpenFloorComplex(PApplet parent, MyPolygon footprint, Float height,
			Map<MyVector3f, Side> directionToSideMap,
			FloorPosition floorPosition, Integer floorPositionIndex,
			boolean isTop) {
		super(parent, footprint, height, directionToSideMap, floorPosition,
				floorPositionIndex, isTop);

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode extrudiert den gespeicherten Footprint in Richtung seines
	 * Normalenvektors. Die Methode geht dabei in der Berechnung davon aus, dass
	 * der gespeicherte Grundriss die Bodenflaeche des Objekts bildet, der
	 * Normalenvektor der Ebene definiert die Extrusionsrichtung, zeigt somit in
	 * Richtung der negativen y-Achse (vom Drehsinn her ist die Eingabe also ein
	 * Quad mit TOP-Ausrichtung). Im Gegensatz zu "normalen" Stockwerken werden
	 * fuer Saeulen-Stockwere keine Seitenwaende erzeugt, stattdessen werden
	 * Sauelen positioniert.
	 */
	@Override
	public void extrudeFootprint() {
		assert mFootprint != null : "FEHLER: Es wurde kein polygonaler Grundriss gesetzt!";
		assert mHeight != null : "FEHLER: Es ist keine Hoehe angegeben";

		// erzeuge die Vertices fuer das germergte Objekt
		// der uebergebene Grundriss beschreibt die Bodenflaeche, verschiebe die
		// Vertices in Richtung der Normale bis auf Zielhoehe
		MyVector3f translationDirection = mFootprint.getNormal();

		// Zielhoehe in die Normale einrechnen
		translationDirection.scale(mHeight);
		LOGGER.debug("Translationsvektor: " + translationDirection);

		// sorge dafuer, dass Vertices, die ein Quad bilden, immer aufeinander
		// folgen und die richtige Abfolge besitzen
		List<Vertex3d> vertices = mFootprint.getVertices();
		int vertCount = vertices.size();

		// Vertices der Bodenflaeche beschrieben durch den polygonalen Grundriss
		// hinzufuegen, Bottom Indices erzeugen
		Integer[] bottomIndices = new Integer[vertices.size()];
		for (int i = 0; i < vertices.size(); i++) {

			// Indices in umgekehrter Reihenfolge erzeugen, um konsistente
			// Normalenausrichtung zu erreichen
			bottomIndices[vertCount - i - 1] = i;
			mVertices.add(vertices.get(i));
		}

		// Boden-Quad erzeugen
		AbstractQuad bottomQuad = new PolygonalQuad();
		bottomQuad.setComplexParent(this);
		bottomQuad.setDirection(Side.BOTTOM);
		bottomQuad.setIndices(bottomIndices);
		bottomQuad.tesselate();
		mOutdoorQuads.add(bottomQuad);

		// erzeuge nun jeweils Quads fuer Decke und Boden
		Integer[] topIndices = new Integer[mFootprint.getVertices().size()];
		Vertex3d clonedVert = null;
		int index = -1;

		// Vertices der Deckenflaeche durch Translation erstellen, Top-Indices
		// erzeugen
		for (int i = 0; i < vertices.size(); i++) {
			clonedVert = vertices.get(i).clone();
			clonedVert.getPositionPtr().add(translationDirection);
			index = mVertices.size();
			mVertices.add(clonedVert);
			topIndices[i] = index;
		}

		AbstractQuad topQuad = new PolygonalQuad();
		topQuad.setComplexParent(this);
		topQuad.setDirection(Side.TOP);
		topQuad.setIndices(topIndices);
		topQuad.tesselate();
		mOutdoorQuads.add(topQuad);

		// Quadausrichtungen an Basisobjektausrichtungen anpassen
		alignDirectionsByNormals(mNormalToDirectionMap, mOutdoorQuads);

		// alle Berechnungen durchfuehren
		finalizeCreation();
	}

	// ------------------------------------------------------------------------------------------

	@Override
	/**
	 * Methode fuehrt eine Footpint-Extrusion aus und positioniert anschliessend geladene 3d-Saeulen-Modelle innerhalb des erzeugten Stockwerks
	 */
	public void create() {

		assert mFootprint != null : "FEHLER: Es wurde kein polygonaler Grundriss gesetzt!";
		assert mHeight != null : "FEHLER: Es ist keine Hoehe angegeben";

		extrudeFootprint();

	}

	// ------------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "openfloor";
	}

	// ------------------------------------------------------------------------------------------

}
