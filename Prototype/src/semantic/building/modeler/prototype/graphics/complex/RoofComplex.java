package semantic.building.modeler.prototype.graphics.complex;

import java.util.ArrayList;
import java.util.List;

import processing.core.PApplet;
import semantic.building.modeler.configurationservice.model.enums.Side;
import semantic.building.modeler.math.Axis;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.prototype.enums.subdivisionType;
import semantic.building.modeler.prototype.graphics.interfaces.iGraphicComplex;
import semantic.building.modeler.prototype.graphics.primitives.AbstractQuad;
import semantic.building.modeler.prototype.graphics.primitives.PolygonalQuad;
import semantic.building.modeler.prototype.service.Texture;
import semantic.building.modeler.weightedstraightskeleton.algorithm.SkeletonRoofDescriptor;
import semantic.building.modeler.weightedstraightskeleton.controller.StraightSkeletonController;
import semantic.building.modeler.weightedstraightskeleton.result.AbstractResultElement;
import semantic.building.modeler.weightedstraightskeleton.result.ResultFace;
import semantic.building.modeler.weightedstraightskeleton.result.SkeletonResultComplex;

/**
 * 
 * @author Patrick Gunia Instanzen dieser Klasse modellieren Dachformen, die
 *         vorab durch den StraightSkeleton-Algorihtmus berechnet wurden. Zu
 *         diesem Zweck uebergibt man dem Konstruktor eine Roofcontroller, aus
 *         diesem extrahiert man die Ergebnisse der SS-Berechnung und erzeugt
 *         Ergebnisstrukturen innerhalb des Hauptsystems.
 * 
 */

public class RoofComplex extends AbstractComplex {

	/**
	 * Straight-Skeleton-Controller, aus dem das Berechnungsergebnis extrahiert
	 * werden soll
	 */
	private transient StraightSkeletonController mRoofController = null;

	/** Konfigurationsobjekt fuer die Kantengewichte */
	private transient SkeletonRoofDescriptor mRoofDescriptor = null;

	// ------------------------------------------------------------------------------------------
	@Override
	public String getType() {
		return "roof";
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public void create() {

		assert mRoofController != null : "FEHLER: Kein Roof-Controller gesetzt!";

		// hole das vollstaendige Berechnungsergebnis
		final SkeletonResultComplex skeletonResult = mRoofController
				.getResultComplex();

		// hole die einzelnen Seitenflaechen
		final List<ResultFace> faces = skeletonResult.getFaces();
		for (ResultFace currentFace : faces) {
			createQuadForFace(currentFace);
		}

		createQuadForBottom();

		// Berechne Ebenen, Texturkoordinaten etc.
		finalizeCreation();

		// setze die SS-Instanz zurueck, da diese nun nicht mehr benoetigt wird
		// und somit freigegeben werden kann
		mRoofController = null;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erzeugt ein Quad fuer die Bodenflaeche des Daches
	 */
	private void createQuadForBottom() {
		assert mRoofController != null : "FEHLER: Kein Roof-Controller gesetzt!";
		final List<Vertex3d> bottomVerts = mRoofController.getInputVertices();
		final String lineSeparator = System.getProperty("line.separator");

		final StringBuffer error = new StringBuffer("Vertices: "
				+ lineSeparator);
		for (int i = 0; i < mVertices.size(); i++) {
			error.append(i + ": " + mVertices.get(i) + lineSeparator);
		}

		final PolygonalQuad bottomQuad = new PolygonalQuad();
		final Integer[] indices = new Integer[bottomVerts.size()];

		int index;
		Vertex3d currentVertex = null;
		for (int i = 0; i < bottomVerts.size(); i++) {
			currentVertex = bottomVerts.get(i);
			index = -1;
			// teste mit Toleranz
			for (int k = 0; k < mVertices.size(); k++) {
				if (currentVertex.getPositionPtr().equalsWithinTolerance(
						mVertices.get(k).getPositionPtr())) {
					index = k;
					break;
				}
			}
			assert index != -1 : "FEHLER: Vertex " + currentVertex
					+ " befindet sich nicht im Vertexbuffer! Buffer: " + error;
			indices[i] = index;
		}

		bottomQuad.setIndices(indices);
		bottomQuad.setDirection(Side.BOTTOM_INDOOR);
		bottomQuad.setComplexParent(this);
		bottomQuad.tesselate();
		mOutdoorQuads.add(bottomQuad);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erzeugt fuer ein uebergebenes ResultFace-Element ein polygonales
	 * Quad und fuegt dieses zur Quadliste hinzu
	 * 
	 * @param face
	 *            Dachflaeche, fuer die ein polygonales Quad erstellt wird
	 */
	private void createQuadForFace(final ResultFace face) {

		final List<AbstractResultElement> elements = face.getElements();
		// erzeuge eine Polygonalquad-Datenstruktur fuer die Vertices
		final PolygonalQuad quad = new PolygonalQuad();

		// hole Vertice, Indices und Texturkoordinaten
		extractDataFromElements(quad, elements);

		// Gewicht der Grundkante => darauf basierend wird die Quad-Richtung
		// gewaehlt
		float weight = face.getBaseEdge().getWeight();

		if (mRoofDescriptor.getMainWeight() == weight) {
			quad.setDirection(Side.ROOF);
		} else {
			quad.setDirection(Side.ROOF_SIDE);
		}

		quad.setComplexParent(this);
		quad.tesselate();
		mOutdoorQuads.add(quad);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode bestimmt anhand der uebergebenen Elemente eine Abfolge von
	 * Vertices, die die Outline der Dachflaeche bilden. Diese Vertices werden
	 * zum Vertexbuffer des komplexen Objekts hinzugefuegt und die zugehoriegen
	 * Indices im uebergebenen Quad gespeichert.
	 * 
	 * @param targetQuad
	 *            Neu erstelltes Quad, fuer das Indices und Texturkoordinaten
	 *            bestimmt werden
	 * @param elements
	 *            Subelemente einer Dachflaeche, die als Ausgangspunkt fuer die
	 *            Berechnung der polygonalen Quads verwendet werden
	 */
	private void extractDataFromElements(final AbstractQuad targetQuad,
			final List<AbstractResultElement> elements) {

		// Starte die Berechnung mit dem ersten Element in der Elementliste
		final List<Vertex3d> localBuffer = new ArrayList<Vertex3d>(
				elements.size() * 2 + 1);
		final List<Integer> indices = new ArrayList<Integer>(
				elements.size() * 2 + 1);

		// starte die Berechnung mit dem ersten Element
		final AbstractResultElement start = elements.get(0);
		walkUp(start, indices, localBuffer);

		final Integer[] indicesArray = new Integer[indices.size()];
		final String lineSeparator = System.getProperty("line.separator");
		final StringBuffer message = new StringBuffer(lineSeparator);
		Vertex3d currentVertex = null;

		for (Integer currentIndex : indices) {
			currentVertex = getVertices().get(currentIndex);
			message.append("mVertices.add(new Vertex3d(" + currentVertex.getX()
					+ "f, " + currentVertex.getY() + "f, "
					+ currentVertex.getZ() + "f));" + lineSeparator);
		}

		LOGGER.debug("OUTLINE FUER TESSELATOR");
		LOGGER.debug(message);

		// kopiere die Indices in ein Integer-Array
		for (int i = 0; i < indices.size(); i++)
			indicesArray[i] = indices.get(i);

		// speichere Quadindices
		targetQuad.setIndices(indicesArray);

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode durchlaeuft die Hierarchie der Ergebniselemente aufsteigend und
	 * fuegt die linke Seite der Elemente zu den Ergebnisbuffern hinzu. Wird das
	 * oberste Element erreicht, wird die Richtung umgekehrt und der Baum wird
	 * absteigend durchlaufen.
	 * 
	 * @param currentElement
	 *            Element, dessen Vertices hinzugefuegt werden und von dem
	 *            ausgehend die naechste Rekursion aufgerufen wird
	 * @param indices
	 *            Integer-Liste, die alle Indices enthaelt, die waehrend der
	 *            Berechnung erzeugt wurden
	 * @param vertices
	 *            Vertex3d-Liste, die alle Vertices enthaelt, die waehrend der
	 *            Berechnung erzeugt wurden
	 */
	private void walkUp(final AbstractResultElement currentElement,
			final List<Integer> indices, final List<Vertex3d> vertices) {
		// adde die Vertices mit Indices 1 und 2 zum Buffer
		final List<Vertex3d> currentVertices = currentElement.getPoints();
		Vertex3d newVert = currentVertices.get(1).clone();
		Integer index = mVertices.indexOf(newVert);
		Integer indexLocal = vertices.indexOf(newVert);

		// nur adden, wenn das Vertex im lokalen Buffer noch nicht vorkam (es
		// kann sehr wohl sein, dass es im
		// globalen Buffer von einem vorherigen Quad bereits eingefuegt wurde)
		if (indexLocal == -1) {
			if (index == -1) {
				mVertices.add(newVert);
			}
			vertices.add(newVert);

			// adde den Index nur, wenn das Vertex noch nicht geadded wurde =>
			// aufgrund der Abfolge der Berechnung erhaelt man eine konsistente
			// Struktur
			indices.add(mVertices.indexOf(newVert));
		}

		newVert = currentVertices.get(2).clone();

		index = mVertices.indexOf(newVert);
		indexLocal = vertices.indexOf(newVert);

		if (indexLocal == -1) {
			if (index == -1)
				mVertices.add(newVert);
			vertices.add(newVert);

			// adde den Index nur, wenn das Vertex noch nicht geadded wurde =>
			// aufgrund der Abfolge der Berechnung erhaelt man eine konsistente
			// Struktur
			indices.add(mVertices.indexOf(newVert));
		}

		// bestimme nun das Element, mit dem die Rekursion gestartet wird
		final AbstractResultElement upperNeighbour = currentElement
				.getUpperNeighbour();

		// existiert kein oberer Nachbar mehr, beginne das Backtracking mit dem
		// aktuellen Element
		if (upperNeighbour == null) {
			walkDown(currentElement, indices, vertices);
		} else {
			// sonst teste, ob das aktuelle Element einen Nachbarn hat und
			// bestimme das linkeste Element in der Abfolge
			AbstractResultElement leftNeighbour = upperNeighbour;
			while (leftNeighbour.getLeftNeighbour() != null) {
				leftNeighbour = leftNeighbour.getLeftNeighbour();
			}

			// gehe weiter links herauf, sofern ein Nachbar existiert
			walkUp(leftNeighbour, indices, vertices);
		}

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode durchlaeuft die Hierarchie der Ergebniselemente absteigend und
	 * fuegt die rechte Seite der Elemente zu den Ergebnisbuffern hinzu. Wird
	 * das unterste Element erreicht, endet die Berechnung. Trifft man auf ein
	 * Element mit einem rechten Nachbarn, so wird die Hierarchie ausgehend von
	 * diesem Element erneut aufsteigend durchlaufen
	 * 
	 * @param currentElement
	 *            Element, dessen Vertices hinzugefuegt werden und von dem
	 *            ausgehend die naechste Rekursion aufgerufen wird
	 * @param indices
	 *            Integer-Liste, die alle Indices enthaelt, die waehrend der
	 *            Berechnung erzeugt wurden
	 * @param vertices
	 *            Vertex3d-Liste, die alle Vertices enthaelt, die waehrend der
	 *            Berechnung erzeugt wurden
	 */
	private void walkDown(final AbstractResultElement currentElement,
			final List<Integer> indices, final List<Vertex3d> vertices) {

		List<Vertex3d> currentVertices = currentElement.getPoints();
		Vertex3d newVert = null;
		Integer index = null, indexLocal = null;

		// wenn eine ResultEdge erreicht wurde, breche ab
		if (currentElement.getType().equals("ResultEdge")) {
			return;
		}

		// bei Dreiecken nur Vertex mit Index 0 adden
		if (!currentElement.getType().equals("ResultTriangle")) {
			newVert = currentVertices.get(3).clone();

			index = mVertices.indexOf(newVert);
			indexLocal = vertices.indexOf(newVert);

			if (indexLocal == -1) {
				if (index == -1)
					mVertices.add(newVert);
				vertices.add(newVert);

				// adde den Index nur, wenn das Vertex noch nicht geadded wurde
				// => aufgrund der Abfolge der Berechnung erhaelt man eine
				// konsistente Struktur
				indices.add(mVertices.indexOf(newVert));
			}
		}

		newVert = currentVertices.get(0).clone();
		index = mVertices.indexOf(newVert);
		indexLocal = vertices.indexOf(newVert);

		if (indexLocal == -1) {
			if (index == -1)
				mVertices.add(newVert);
			vertices.add(newVert);

			// adde den Index nur, wenn das Vertex noch nicht geadded wurde =>
			// aufgrund der Abfolge der Berechnung erhaelt man eine konsistente
			// Struktur
			indices.add(mVertices.indexOf(newVert));
		}

		AbstractResultElement rightNeighbour = currentElement
				.getRightNeighbour();

		// wenn ein rechter Nachbar existiert, gehe mit diesem wieder nach oben
		if (rightNeighbour != null)
			walkUp(rightNeighbour, indices, vertices);
		else {

			// sonst bestimme den darunterliegenden Nachbarn und rufe mit diesem
			// die Rekursion auf
			AbstractResultElement lowerNeighbour = currentElement
					.getLowerNeighbour();
			if (lowerNeighbour != null)
				walkDown(lowerNeighbour, indices, vertices);
			// sonst beende die Berechnung, man hat das unterste Element
			// erreicht
			else
				return;

		}
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public void setTexture(String category, Texture texture) {

		// speichere die Kategorie-Texturzuordnung im aktuellen Map-Objekt
		addTextureToMap(category, texture);
		for (AbstractQuad current : mOutdoorQuads) {
			current.setTextureForCategory(category, texture);
		}
	}
	
	// ------------------------------------------------------------------------------------------
	/**
	 * Standardkonstruktor fuer RoofComplex-Objekte
	 * 
	 * @param parent
	 *            PApplet-Instanz, ueber die das Objekt gezeichnet wird
	 * @param roofController
	 *            SS-Controller-Instanz, aus der die Vertices etc. errechnet
	 *            werden
	 */
	public RoofComplex(final PApplet parent,
			final StraightSkeletonController roofController,
			final SkeletonRoofDescriptor roofDescriptor) {
		super(parent);
		mRoofController = roofController;
		mRoofDescriptor = roofDescriptor;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Konstruktor wird bei Clone-Operationen aufgerufen und erzeugt ein leeres
	 * RoofComplex-Objekt, das nachfolgend mit kopierten Vertices, Quads etc.
	 * befuellt wird
	 */
	public RoofComplex(PApplet parent) {
		super(parent);
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public void extrude(Side whichFace, Axis extrudeAxis, float extrudeAmount) {

	}

	// ------------------------------------------------------------------------------------------

	@Override
	public void subdivideQuad(Side whichFace, subdivisionType type,
			float subdivisionFactor) {

	}

	// ------------------------------------------------------------------------------------------

	@Override
	public iGraphicComplex subdivide(subdivisionType type,
			float subdivisionFactor) {
		return null;
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public AbstractComplex cloneConcreteComponent() {
		return new RoofComplex(mParent);
	}
	// ------------------------------------------------------------------------------------------

}
