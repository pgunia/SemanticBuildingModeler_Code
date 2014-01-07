package semantic.building.modeler.prototype.graphics.complex;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import processing.core.PApplet;
import semantic.building.modeler.configurationservice.model.enums.FloorPosition;
import semantic.building.modeler.configurationservice.model.enums.Side;
import semantic.building.modeler.math.MyPolygon;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.prototype.graphics.primitives.AbstractQuad;

/**
 * Klasse beschreibt Zwischenebenen zwischen Stockwerken. diese enthalten keine
 * Boden- bzw. Deckenquads, wodurch z-Fighting an diesen Komponenten verhindert
 * wird. Ansonsten verhalten sie sich wie normale Stockwerke
 * 
 * @author Patrick Gunia
 * 
 */

public class IntermediateFloor extends FloorComplex {
	// ------------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "intermediatefloor";
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Standard-Konstruktor
	 * 
	 * @param parent
	 *            Applet-Instanz, die fuer das Zeichnen des komplexen Objekts
	 *            benoetigt wird
	 * @param vertices
	 *            Liste mit Vertices, die den Grundriss im Uhrzeigersinn
	 *            definieren
	 * @param height
	 *            Hoehe des komplexen Objekts
	 * @param directionToSideMap
	 *            Zuordnung von Face-Normalen zu Ausrichtungen, darueber kann
	 *            man automatisiert Richtungen fuer die Faces setzen
	 * @param floorPosition
	 *            Position des Stockwerks innerhalb des Gebaeudes
	 * @param floorPositioningIndex
	 *            Numerischr Index, der die Position des Stockwerks innerhalb
	 *            des Gebaeudes festlegt
	 * @param isTop
	 *            Flag zeigt an, ob das uebergebene Polygon die Deckenflaeche
	 *            beschreibt
	 */
	public IntermediateFloor(PApplet parent, List<Vertex3d> vertices,
			Float height, Map<MyVector3f, Side> directionToSideMap,
			FloorPosition floorPosition, Integer floorPositionIndex,
			boolean isTop) {
		super(parent, vertices, height, directionToSideMap, floorPosition,
				floorPositionIndex, isTop);
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Konstruktor mit Uebergabe eines Polygons anstelle einer Vertexliste
	 * 
	 * @param parent
	 *            Applet-Instanz, die fuer das Zeichnen des komplexen Objekts
	 *            benoetigt wird
	 * @param vertices
	 *            Liste mit Vertices, die den Grundriss im Uhrzeigersinn
	 *            definieren
	 * @param height
	 *            Hoehe des komplexen Objekts
	 * @param directionToSideMap
	 *            Zuordnung von Face-Normalen zu Ausrichtungen, darueber kann
	 *            man automatisiert Richtungen fuer die Faces setzen
	 * @param floorPosition
	 *            Position des Stockwerks innerhalb des Gebaeudes
	 * @param floorPositioningIndex
	 *            Numerischr Index, der die Position des Stockwerks innerhalb
	 *            des Gebaeudes festlegt
	 * @param isTop
	 *            Flag zeigt an, ob das uebergebene Polygon die Deckenflaeche
	 *            beschreibt
	 * @return
	 */
	public IntermediateFloor(PApplet parent, MyPolygon poly, Float height,
			Map<MyVector3f, Side> directionToSideMap,
			FloorPosition floorPosition, Integer floorPositionIndex,
			boolean isTop) {
		super(parent, poly, height, directionToSideMap, floorPosition,
				floorPositionIndex, isTop);

	}

	// ------------------------------------------------------------------------------------------

	@Override
	/**
	 * Methode muss ueberschrieben werden, da MergedComplex-Instanzen aus den berechneten Umrissen von TOP-Quads erzeugt werden, die
	 * darum zunaechst auf die Bodenhoehe verschoben werden. Dies ist bei FloorComplex-Instanzen nicht der Fall.
	 */
	public void create() {

		assert mFootprint != null : "FEHLER: Es wurde kein polygonaler Grundriss gesetzt!";
		assert mHeight != null : "FEHLER: Es ist keine Hoehe angegeben";

		extrudeFootprint();

		// Entferne das TOP- und das BOTTOM-Quad aus der Struktur =>
		// Intermediate-Strukturen besitzen keine Boden- bzw. Deckenflaechen
		Iterator<AbstractQuad> quadIter = mOutdoorQuads.iterator();
		AbstractQuad currentQuad = null;
		while (quadIter.hasNext()) {
			currentQuad = quadIter.next();
			if (currentQuad.getDirection().equals(Side.TOP)
					|| currentQuad.getDirection().equals(Side.BOTTOM)) {
				quadIter.remove();
			}
		}

		// alle Berechnungen durchfuehren
		finalizeCreation();
	}

	// ------------------------------------------------------------------------------------------
	@Override
	protected AbstractComplex cloneConcreteComponent() {
		// isTop ist immer false, da beim Cloning bereits die Verschiebung
		// bereits durchgefuehrt wurde
		IntermediateFloor newObject = new IntermediateFloor(mParent,
				mFootprint, mHeight, mNormalToDirectionMap, mFloorPosition,
				mFloorPositionIndex, false);
		return newObject;
	}

	// ------------------------------------------------------------------------------------------

}
