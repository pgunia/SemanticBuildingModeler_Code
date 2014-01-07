package semantic.building.modeler.prototype.graphics.complex;

import java.util.Iterator;
import java.util.List;

import processing.core.PApplet;
import semantic.building.modeler.configurationservice.model.enums.Side;
import semantic.building.modeler.math.Axis;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.prototype.algorithm.BoundingBoxCreator;
import semantic.building.modeler.prototype.enums.subdivisionType;
import semantic.building.modeler.prototype.graphics.interfaces.iGraphicComplex;
import semantic.building.modeler.prototype.graphics.primitives.AbstractQuad;

/**
 * 
 * @author Patrick Gunia Klasse verwaltet Geometrie-Daten fuer 3d-Objekte, die
 *         aus externen 3d-Programmen geladen und in das System importiert
 *         wurden.
 * 
 */

public class ImportedComplex extends AbstractComplex {

	// ------------------------------------------------------------------------------------------
	/**
	 * Standardkonstruktor mit Uebergabe des Applets zum Zeichnen des Objekts
	 * 
	 * @param parentApplet
	 *            Instanz der PApplet-Klasse, die fuer das Zeichnen der Objekte
	 *            benoetigt wird
	 */
	public ImportedComplex(PApplet parentApplet, List<Vertex3d> vertices,
			List<AbstractQuad> quadFaces) {
		super(parentApplet);
		mVertices = vertices;
		mOutdoorQuads = quadFaces;

		// berechne eine OBB fuer das aktuelle Objekt
		final BoundingBoxCreator bbCreator = new BoundingBoxCreator();

		// zunaechst reichen AABBs aus, da die Vorgabe fuer importierte Objekte
		// darin besteht, dass sie immer im Front-ViewPort erzeugt werden
		// muessen
		// dadurch sind sie achsenausgerichtet
		mBB = bbCreator.computeAABB(mVertices);
		// mBB = bbCreator.computeOBBBruteForce(mVertices);

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Konstruktor wird bei Clone-Operationen aufgerufen, erstellt zunaechst nur
	 * eine Instanz der ImportedComplex-Klasse, die weiteren
	 * Verarbeitungsschritte werden dann durch die clone-Operation der
	 * Basisklasse durchgefuehrt
	 */
	public ImportedComplex(PApplet parentApplet) {
		super(parentApplet);
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "imported";
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public void create() {

		assert mVertices != null : "FEHLER: Keine Vertices gesetzt.";
		assert mOutdoorQuads != null : "FEHLER: Keine Quads gesetzt.";

		Iterator<AbstractQuad> quadIter = mOutdoorQuads.iterator();
		AbstractQuad currentQuad = null;

		// fuehre die abschliessenden Verarbeitungsschritte durch
		while (quadIter.hasNext()) {
			currentQuad = quadIter.next();
			currentQuad.setComplexParent(this);
			currentQuad.tesselate();
		}

		finalizeCreation();
		// evtl. hier noch Direction-Aligment basierend auf existierenden
		// Normalenvektoren...koennte bsw. eine Standardausrichtung speichern
		// und diese verwenden
	}

	// ------------------------------------------------------------------------------------------
	@Override
	/** 
	 * Ueberschreibt regulaere Abschlussberechnungen von anderen komplexen Objekten, damit importierte Objekte nicht automatisch bei der Objektverwaltung
	 * angemeldet werden. Da Objekte dieser Art nicht alleine stehen koennen, macht auch eine Anmeldung bei der Verwaltung keinen Sinn.
	 */
	public void finalizeCreation() {

		// berechne Mittelpunkte, Normalen etc. fuer alle berechneten faces
		update();

		// erzeuge IDs fuer das Objekt und alle Kindelemente
		generateID(null, null);
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public void extrude(Side whichFace, Axis extrudeAxis, float extrudeAmount) {
		// TODO Auto-generated method stub

	}

	// ------------------------------------------------------------------------------------------

	@Override
	public void subdivideQuad(Side whichFace, subdivisionType type,
			float subdivisionFactor) {
		// TODO Auto-generated method stub

	}

	// ------------------------------------------------------------------------------------------

	@Override
	public iGraphicComplex subdivide(subdivisionType type,
			float subdivisionFactor) {
		// TODO Auto-generated method stub
		return null;
	}

	// ------------------------------------------------------------------------------------------

	@Override
	protected AbstractComplex cloneConcreteComponent() {
		return new ImportedComplex(mParent);
	}

	// ------------------------------------------------------------------------------------------

}
