package semantic.building.modeler.prototype.algorithm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import semantic.building.modeler.math.MyPolygon;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.MyVectormath;
import semantic.building.modeler.math.Ray;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.objectplacement.model.AbstractComponent;
import semantic.building.modeler.prototype.graphics.complex.AbstractComplex;
import semantic.building.modeler.prototype.service.PrototypeHelper;

/**
 * 
 * Klasse implementiert die Logik, mittels derer aus einer Menge von Komponenten
 * ein einzelner Grundriss extrahiert wird. Dies erfolgt entweder mittels der
 * Footprint-Merger-Logik oder mittels einer Convex-Hull-Implementation
 * 
 * @author Patrick Gunia
 * 
 */

public class FootprintCreator {

	/** Logging-Instanz */
	protected static Logger LOGGER = Logger.getLogger(FootprintCreator.class);

	/** Set mit allen Footprint-Instanzen */
	private Set<Footprint> mAllFootprints = null;

	// ------------------------------------------------------------------------------------------
	/**
	 * Default-Konstruktor
	 */
	public FootprintCreator() {
		mAllFootprints = new HashSet<Footprint>();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Konstruktor mit Uebergabe einer AbstractComplex-Instanz, aus der ein
	 * Footprint abgeleitet wird
	 * 
	 * @param component
	 *            AbstractComplex-Instanz, aus der der Grundriss abgeleitet wird
	 */
	public void addComponent(final AbstractComplex component) {
		final Footprint currentFootprint = new Footprint(component);
		mAllFootprints.add(currentFootprint);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Konstruktor mit Uebergabe eines Polygons, aus dem direkt ein Footprint
	 * erstellt wird
	 * 
	 * @param poly
	 *            Polygon, das in die Footprintberechnungen integriert wird
	 */
	public void addComponent(final MyPolygon poly) {
		Footprint currentFootprint = new Footprint(poly);
		mAllFootprints.add(currentFootprint);
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Konstruktor mit einer Polygonliste, die zum Set geadded wird
	 * 
	 * @param polygons
	 *            Liste mit Polygonen, die zur Footprintberechnung hinzugefuegt
	 *            werden sollen
	 */
	public void addPolygons(final List<MyPolygon> polygons) {
		for (MyPolygon current : polygons) {
			Footprint currentFootprint = new Footprint(current);
			mAllFootprints.add(currentFootprint);
		}
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Konstruktor mit Uebergabe eine Liste mit Komponenten, die aus der
	 * Placementlogic stammen
	 * 
	 * @param components
	 *            Liste mit Placement-Components, die zur Footprintberechnung
	 *            hinzugefuegt werden sollen
	 */
	public void addComponents(final List<AbstractComponent> components) {
		for (AbstractComponent current : components) {
			Footprint currentFootprint = new Footprint(current.getPolygon());
			mAllFootprints.add(currentFootprint);
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Fassadenmethode, kapselt alle erforderlichen Berechnungsschritte des
	 * Algorithmus und gibt das Ergebnis an den Aufrufer zurueck
	 * 
	 * @param useConvexHull
	 *            Flag gibt an, ob fuer die Footprintberechnung der
	 *            Graham-Scan-Algorithmus verwendet werden soll
	 * 
	 * @return Das Ergebnis der Berechnung ist eine Liste mit Listen von
	 *         Vertex3d-Instanzen. Ein solcher Vertex3d-Vektor wird fuer jeden
	 *         gemergeten Grundriss erzeugt.
	 */
	public List<List<Vertex3d>> process(final Boolean useConvexHull) {

		// erster Schritt: Eimerchen berechnen
		List<FootprintBucket> buckets = computeBuckets();

		// zweiter Schritt: Berechne fuer jeden Eimer einen gemergeten Grundriss
		Iterator<FootprintBucket> bucketIter = buckets.iterator();
		List<List<Vertex3d>> resultFootprints = new ArrayList<List<Vertex3d>>();

		// durchlaufe alle Eimer und erzeuge fuer jeden Eimer einen Grundriss,
		// entweder ueber ConvexHull oder ueber das Standardverfahren
		while (bucketIter.hasNext()) {

			// Standardverfahren
			if (!useConvexHull)
				resultFootprints
						.add(createFootprintUsingFootprintMerger(bucketIter
								.next()));
			// ConvexHull
			else
				resultFootprints.add(createFootprintUsingConvexHull(bucketIter
						.next()));
		}

		// durchlaufe nun alle errechneten Footprints und entferne
		// ueberfluessige Vertices
		List<Vertex3d> vertices = null;
		for (int i = 0; i < resultFootprints.size(); i++) {
			vertices = resultFootprints.get(i);
			vertices = cleanVertices(vertices);
			resultFootprints.set(i, vertices);
		}

		String vertexDefinitions = null;
		String lineSeparator = System.getProperty("line.separator");

		for (int i = 0; i < resultFootprints.size(); i++) {
			vertices = resultFootprints.get(i);
			LOGGER.info("################# New Footprint #################");
			vertexDefinitions = lineSeparator;
			for (int j = 0; j < vertices.size(); j++) {

				MyVector3f pos = vertices.get(j).getPosition();
				vertexDefinitions += "mVertices.add(new Vertex3d(" + pos.x
						+ "f, " + pos.y + "f, " + pos.z + "f));"
						+ lineSeparator;
			}
			LOGGER.info(vertexDefinitions);
		}

		return resultFootprints;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode wird aufgerufen, um "Eimer" zu berechnen, die Grundrisse
	 * enthalten, welche sich schneiden. Nach der Berechnung verfuegt man ueber
	 * eine Menge von Buckets, deren Inhalte spaeter zu Grundrissen gemerget
	 * werden
	 * 
	 * @return Liste mit allen erzeugten Buckets
	 */
	private List<FootprintBucket> computeBuckets() {

		Footprint currentFootprint = null, otherFootprint = null;
		List<FootprintBucket> buckets = new ArrayList<FootprintBucket>();
		FootprintBucket currentBucket = null;
		final Footprint[] footprints = mAllFootprints
				.toArray(new Footprint[mAllFootprints.size()]);

		// teste alle moeglichen Paare von Grundrissen gegeneinander
		for (int i = 0; i < footprints.length; i++) {
			currentFootprint = footprints[i];

			// teste, ob es bereits einen Bucket gibt, in dem sich der aktuell e
			// Footprint befindet
			final Iterator<FootprintBucket> bucketIter = buckets.iterator();
			while (bucketIter.hasNext()) {
				currentBucket = bucketIter.next();

				// wenn der aktuelle Footprint nicht im Eimer ist, setze den
				// Eimer auf null
				if (!currentBucket.isFootprintInBucket(currentFootprint)) {
					currentBucket = null;
				}
				// sonst verlasse die Schleife
				else {
					break;
				}
			}

			// wenn kein Bucket fuer den aktuellen Footprint gefunden wurde,
			// erzeuge einen neuen
			if (currentBucket == null) {
				currentBucket = new FootprintBucket();
				buckets.add(currentBucket);

				// fuege den Footprint hinzu
				currentBucket.addFootprint(currentFootprint);
			}

			for (int j = i + 1; j < footprints.length; j++) {
				otherFootprint = footprints[j];
				if (currentFootprint.intersects(otherFootprint)) {
					// wenn ein Schnitt gefunden wurde, fuege den
					// Schnitt-Footprint hinzu
					currentBucket.addFootprint(otherFootprint);
				}
			}
		}

		LOGGER.info("Gesamtanzahl berechneter Eimer: " + buckets.size());

		// wenn nur 1 Bucket angelegt wurde, kann man sich die aufwendigen
		// Merging-Berechnungen sparen
		if (buckets.size() == 1)
			return buckets;

		// durch die Verarbeitung kann der Fall auftreten, dass zu viele Buckets
		// berechnet werden (ist abhaengig von der Position der Footprints
		// im Eingabevektor), evtl. ist es darum erforderlich, die Eimer zu
		// mergen, wenn gleiche Elemente vorkommen

		FootprintBucket otherBucket = null;

		// verwende eine innere und aeussere Schleife, dadurch werden alle
		// Kombinationen getestet (muss aufgrund des Merging-Ansatzes sein)
		for (int i = 0; i < buckets.size(); i++) {
			currentBucket = buckets.get(i);

			for (int j = 0; j < buckets.size(); j++) {
				otherBucket = buckets.get(j);
				if (otherBucket.equals(currentBucket))
					continue;

				// es muss gemerged werden, wenn ein Elemente gefunden wurde,
				// das in beiden Buckets vorkommt
				if (!currentBucket.isMergeNecessary(otherBucket))
					continue;
				else {
					currentBucket.mergeBuckets(otherBucket);

					// da nun alle Elemente des Other-Buckets in den Quellbucket
					// ueberfuehrt wurden, kann man diesen leeren
					otherBucket.clear();
				}
			}
		}

		// durch das Merging koennen leere Buckets entstanden sein, loesche
		// diese aus dem Rueckgabevektor
		Iterator<FootprintBucket> bucketIter = buckets.iterator();
		while (bucketIter.hasNext()) {
			currentBucket = bucketIter.next();
			if (currentBucket.isEmpty())
				bucketIter.remove();
		}
		LOGGER.info("Gesamtanzahl an Eimern nach dem Merging: "
				+ buckets.size());
		return buckets;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet fuer eine Menge von Eingabegrundrissen einen Grundriss,
	 * der durch die Verfolgung der Kantenverlaeufe ermittelt wird. Der
	 * resultierende Grundriss besteht somit nur aus Kantensegmenten der
	 * Eingabegrundrisse.
	 * 
	 * @param bucket
	 *            Eimer mit Grundrissen, die sich gegenseitig ueberschneiden
	 * @return Liste mit Vertices, die einen Polygonzug bilden, der den
	 *         Ergebnisgrundriss beschreibt
	 */
	private List<Vertex3d> createFootprintUsingFootprintMerger(
			final FootprintBucket bucket) {
		FootprintMerger mergingAlgorithm = new FootprintMerger();
		return mergingAlgorithm.computeMergedFootprint(bucket);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet fuer eine Menge von Eingabegrundrissen eine konvexe
	 * Huelle fuer alle Punkte, die in den Eingabegrundrissen vorkommen. Das
	 * Ergebnis dieser Berechnung ist somit ein Grundrisse, der saemtliche
	 * Vertices der Eingabe umschliesst.
	 * 
	 * @param bucket
	 *            Eimer mit Eingabegrundrissen
	 * @return Liste mit Vertices, die einen Linienzug beschreiben, der der
	 *         konvexen Huelle der Eingabevertices entspricht
	 */
	private List<Vertex3d> createFootprintUsingConvexHull(
			final FootprintBucket bucket) {

		PrototypeHelper helper = PrototypeHelper.getInstance();

		// sammle alle Vertices dieses Buckets auf, verwende ein
		// HashSet, um doppelte Vertices zu vermeiden
		Set<Vertex3d> vertices = new HashSet<Vertex3d>();

		Iterator<Footprint> footprintIter = bucket.getFootprints().iterator();
		Footprint currentFootprint = null;

		while (footprintIter.hasNext()) {
			currentFootprint = footprintIter.next();
			// nimm eine der Normalen, diese sind alle gleich, wurde bei
			// der Vorab-Berechnung bereits festgestellt
			vertices.addAll(currentFootprint.getFootprintPoly().getVertices());
		}

		List<Vertex3d> vertexList = new ArrayList<Vertex3d>(vertices.size());
		vertexList.addAll(vertices);

		// Normalenvektor der Ebene berechnen, in der die Punkte liegen
		MyVector3f normal = MyVectormath.getInstance().calculateNormalNewell(
				vertexList);
		return helper.computeConvexHullForUnprojectedVertices(vertexList,
				normal);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode entfernt aufeinanderfolgende Vertices aus dem Vertex-Vector,
	 * falls diese auf einer Geraden liegen, da dies zum Scheitern der
	 * Straight-Skeleton-Berechnung fuehrt
	 * 
	 * @param vertices
	 *            Saemtliche Vertices, die waehrend der Footprint-Bestimmung
	 *            erstellt wurden
	 * @return Bei Bedarf reduzierter Vektor, bei dem aufeinanderfolgende
	 *         Vertices auf einem Strahl liegen, der keine weiteren Vertices
	 *         enthaelt
	 */

	private List<Vertex3d> cleanVertices(final List<Vertex3d> vertices) {

		List<Vertex3d> toDelete = new ArrayList<Vertex3d>();
		Ray currentRay = null;
		Vertex3d currentVertex = null, currentNeighbourVertex = null, lastVertex = null;

		MyVectormath mathHelper = MyVectormath.getInstance();

		// berechne den Strahl zwischen den ersten beiden Vertices vor
		currentVertex = vertices.get(0);
		currentNeighbourVertex = vertices.get(1);
		lastVertex = currentNeighbourVertex;

		currentRay = new Ray(currentVertex, currentNeighbourVertex);
		boolean ueberlauf = false;

		float distanceToRay;

		// maximaler senkrechter Abstand eines Vertex von einem Strahl,
		// unterhalb dessen das Vertex als auf dem Strahl liegend betrachtet
		// wird
		float maxTolerableDistance = 0.5f;

		for (int i = 2; i < vertices.size(); i++) {
			currentVertex = vertices.get(i);

			// wenn das aktuelle Vertex auf dem Strahl liegt, loesche es
			distanceToRay = mathHelper.calculatePointEdgeDistance(
					currentVertex.getPosition(), currentRay);
			if (distanceToRay < maxTolerableDistance) {
				toDelete.add(lastVertex);
			}
			// sonst erzeuge einen neuen Strahl zwischen dem Vertex des
			// vorherigen Durchlaufs und dem aktuellen
			else {
				currentRay = new Ray(lastVertex, currentVertex);
			}
			// naechste Iteration vorbereiten
			lastVertex = currentVertex;

			// beruecksichtige die Moeglichkeit, dass das letzte und die ersten
			// beiden Vertices auf einer Kante liegen
			// erzeuge einen Ueberlauf auf die Startvertices
			if (i == vertices.size() - 1) {
				i = -1;
				ueberlauf = true;
			}
			if (i == 2 && ueberlauf)
				break;

		}

		// entferne alle Vertices aus dem toDelete-Buffer
		if (toDelete.size() > 0) {
			Iterator<Vertex3d> vertIter = toDelete.iterator();
			while (vertIter.hasNext()) {
				vertices.remove(vertIter.next());
			}
		}

		return vertices;
	}
	// ------------------------------------------------------------------------------------------

}
