package semantic.building.modeler.prototype.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import semantic.building.modeler.math.MyPolygon;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.MyVectormath;
import semantic.building.modeler.math.Plane;
import semantic.building.modeler.math.Ray;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.prototype.service.PrototypeHelper;

/**
 * 
 * @author Patrick Gunia 
 * 		   Klasse implementiert den QuickHull-Algorithmus fuer
 *         3d-Punktwolken.
 *         http://www.yaldex.com/game-programming/0131020099_ch22lev1sec6.html
 * 
 */

public class Quickhull3d {

	/** Logging-Instanz */
	protected static Logger LOGGER = Logger.getLogger(Quickhull3d.class);

	/** Eingabepunktwolke, fuer die die konvexe Huelle berechnet werden soll */
	private List<Vertex3d> mVertices = null;

	/**
	 * Punktebuffer waehrend der Berechnung des Verfahrens, enthaelt zu Beginn
	 * der Berechnung alle Eingabepunkte, aus diesem Buffer werden bei jeder
	 * Iteration weitere Vertices entfernt
	 */
	private List<Vertex3d> mVertexBuffer = null;

	/** Datenstruktur fuer die Outside-Sets fuer jedes Dreieck */
	private Map<QuickHullTriangle, List<Vertex3d>> mOutsideSets = null;

	/** Instanz der Mathebibliothek */
	private MyVectormath mMathHelper = MyVectormath.getInstance();

	/**
	 * Liste mit Dreiecken, die waehrend der Berechnung verwaltet werden,
	 * verwendet die Polygonstrukturen, da diese nicht indexbasiert arbeiten und
	 * darum besser geeignet sind
	 */
	private List<QuickHullTriangle> mTriangles = null;

	/**
	 * Buffer dient der Aufnahme von Triangles, die geloescht werden, muss extra
	 * ausgelagert werden, um Concurrent Modifications zu verhindern
	 */
	private List<QuickHullTriangle> mTrianglesToDelete = null;

	/**
	 * Mapstruktur speichert fuer einen Punkt alle Dreiecke, die von diesem aus
	 * sichtbar sind
	 */
	private Map<Vertex3d, List<QuickHullTriangle>> mVisibleTriangles = null;

	/** Index fuer Dreiecks-ID-Vergabe */
	private long mLastTriangleIndex = 0;

	// ------------------------------------------------------------------------------------------

	/**
	 * 
	 * @param vertices
	 *            Vertexliste, fuer die eine konvexe Huelle berechnet wird
	 */
	public Quickhull3d(List<Vertex3d> vertices) {
		mVertices = vertices;
		mTriangles = new ArrayList<QuickHullTriangle>();
		mVertexBuffer = new ArrayList<Vertex3d>(mVertices.size());
		mVertexBuffer.addAll(mVertices);
		mOutsideSets = new HashMap<QuickHullTriangle, List<Vertex3d>>();
		mTrianglesToDelete = new ArrayList<QuickHullTriangle>();
	}

	// ------------------------------------------------------------------------------------------
	public long getTriangleIndex() {
		long temp = mLastTriangleIndex;
		mLastTriangleIndex++;
		return temp;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * 
	 * @return Liste mit Dreiecken, die die konvexe Huelle fuer die
	 *         Eingabepunktwolke beschreiben
	 */
	public List<MyPolygon> computeConvexHull() {

		// PrototypeHelper helper = PrototypeHelper.getInstance();

		LOGGER.trace("#Vertices im Vertexbuffer: " + mVertexBuffer.size());
		init();

		QuickHullTriangle currentTri = null, currentMaxOutsideVertsTri = null;

		int numberOfIterations = 1;
		int maxNumberOfVertsInOutsideSet = 0;
		List<Vertex3d> currentOutsideSet = null;
		float maxDistance = 0, currentDistance = 0;
		Vertex3d currentVert = null, maxDistanceVert = null;
		MyVectormath mathHelper = MyVectormath.getInstance();

		while (computeVisibility()) {

			LOGGER.info("Iteration: " + numberOfIterations);
			LOGGER.info("Anzahl Vertices: " + mVertexBuffer.size());
			LOGGER.info("Anzahl Dreiecke: " + mTriangles.size());

			// reset
			maxNumberOfVertsInOutsideSet = 0;
			currentOutsideSet = null;
			currentMaxOutsideVertsTri = null;
			maxDistanceVert = null;

			// bestimme das Outside-Set mit den meisten Vertices und verarbeite
			// dieses weiter
			for (int i = 0; i < mTriangles.size(); i++) {
				currentTri = mTriangles.get(i);
				currentOutsideSet = mOutsideSets.get(currentTri);

				// Dreiecke, die zur finalen konvexen Hueller gehoeren, besitzen
				// keine Outside-Sets
				if (currentOutsideSet == null)
					continue;
				if (currentOutsideSet.size() > maxNumberOfVertsInOutsideSet) {
					currentMaxOutsideVertsTri = currentTri;
					maxNumberOfVertsInOutsideSet = currentOutsideSet.size();
				}
			}

			maxDistance = -Float.MAX_VALUE;
			currentDistance = 0;

			// bestimme im Outsideset mit den meisten Vertices das Vertex mit
			// der groessten Entfernung vom jeweiligen Face
			currentOutsideSet = mOutsideSets.get(currentMaxOutsideVertsTri);
			for (int i = 0; i < currentOutsideSet.size(); i++) {
				currentVert = currentOutsideSet.get(i);
				currentDistance = mathHelper.calculatePointPlaneDistance(
						currentVert.getPositionPtr(), currentMaxOutsideVertsTri
								.getPoly().getPlane());
				if (currentDistance > maxDistance) {
					maxDistance = currentDistance;
					maxDistanceVert = currentVert;
				}
			}

			// berechne die Horizon-Lines fuer das am weitesten entfernte Vertex
			computeHorizonForPoint(maxDistanceVert);

			LOGGER.trace("#Zu loeschende Dreiecke: "
					+ mTrianglesToDelete.size());
			LOGGER.trace("#Dreiecke vor Loeschen: " + mTriangles.size());
			// loesche nun alle nicht mehr sichtbaren Dreiecke aus dem
			// Dreiecksbuffer
			for (int i = 0; i < mTrianglesToDelete.size(); i++) {
				mTriangles.remove(mTrianglesToDelete.get(i));
				LOGGER.trace("Geloeschtes Dreieck: "
						+ mTrianglesToDelete.get(i));
			}
			LOGGER.trace("#Dreiecke nach Loeschen: " + mTriangles.size());

			// Normalenvektoren ausrichten
			normalizeNormals();
			initNextIteration();
			numberOfIterations++;

		}

		return getPolygons();

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode liefert die waehrend des Algorithmus berechneten Polygone
	 * 
	 * @return Liste mit Ergebnispolygonen
	 */
	public List<MyPolygon> getPolygons() {
		// mache wieder Polygone aus der verwendeten propietaeren Struktur
		List<MyPolygon> result = new ArrayList<MyPolygon>(mTriangles.size());
		for (int i = 0; i < mTriangles.size(); i++)
			result.add(mTriangles.get(i).getPoly());
		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode bereitet die naechste Iteration vor
	 */
	private void initNextIteration() {

		mTrianglesToDelete.clear();
		mOutsideSets.clear();

		// Rekursionsabbruchkriterium zuruecksetzen
		for (int i = 0; i < mTriangles.size(); i++)
			mTriangles.get(i).setVisited(false);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode prueft saemtliche Normalenvektor der Polyeder-Faces und aendert
	 * bei Bedarf die Vertexorder der Polygone, um einheitlich ausgerichtete
	 * Normalen zu erhalten
	 */
	private void normalizeNormals() {
		List<MyPolygon> polys = new ArrayList<MyPolygon>(mTriangles.size());
		for (int i = 0; i < mTriangles.size(); i++) {
			polys.add(mTriangles.get(i).getPoly());
		}

		PrototypeHelper.getInstance().normalizeNormals(polys);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet eine Liste von NeighbourHelper-Instanzen, die fuer jede
	 * Kante die adjazenten Dreiecke speichert
	 * 
	 * @param triangles
	 *            Liste mit Dreiecken, fuer die die Struktur erstellt werden
	 *            soll
	 * @return Liste mit NeighbourHelper-Instanzen
	 */
	private List<QuickHullNeighbourHelper> createNeighbourStructure(
			List<QuickHullTriangle> triangles) {
		List<Ray> triangleRays = null;
		QuickHullTriangle currentTri = null;
		QuickHullNeighbourHelper currentHelper = null;
		Ray currentRay = null;

		List<QuickHullNeighbourHelper> edgeBuffer = new ArrayList<QuickHullNeighbourHelper>(
				triangles.size() * 3 / 2);

		for (int i = 0; i < triangles.size(); i++) {
			currentTri = triangles.get(i);
			triangleRays = currentTri.getPoly().getRays();

			// Kanten durchlaufen, fuer jede Kante die beiden Dreiecke suchen,
			// die sich diese teilen
			for (int k = 0; k < triangleRays.size(); k++) {
				currentRay = triangleRays.get(k);

				// Helper-Instanz fuer Ray erzeugen, damit auf die Map zugreifen
				currentHelper = new QuickHullNeighbourHelper(
						currentRay.getStartVertex(), currentRay.getEndVertex());
				if (edgeBuffer.contains(currentHelper)) {
					int index = edgeBuffer.indexOf(currentHelper);
					currentHelper = edgeBuffer.get(index);
					currentHelper.addNeighbour(currentTri);
				} else {
					currentHelper.addNeighbour(currentTri);
					edgeBuffer.add(currentHelper);
				}
			}
		}
		return edgeBuffer;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet die Datenstrukturen zur Bestimmung der Sichtbarkeit von
	 * Faces und Vertices. Vertices, die kein Face sehen, werden aus dem
	 * Vertexbuffer geloescht.
	 * 
	 * @return True, so lange noch weitere Vertices im Buffer sind und das
	 *         Verfahren weiterlaufen muss, False sonst
	 */
	private boolean computeVisibility() {

		// durchlaufe alle Punkte im aktuellen Punktebuffer
		Iterator<Vertex3d> vertIter = mVertexBuffer.iterator();
		mVisibleTriangles = new HashMap<Vertex3d, List<QuickHullTriangle>>();

		Vertex3d currentVert = null;
		boolean isPointOutside = false;
		List<QuickHullTriangle> currentTriList = null;
		List<Vertex3d> outsideVerts = null;

		while (vertIter.hasNext()) {
			isPointOutside = false;
			currentVert = vertIter.next();

			// teste das Vertex gegen alle vorhandenen Dreiecke, sobald ein
			// Dreieck gefunden wird,
			// dass vom aktuellen Vertex aus sichtbar ist, fuege das Vertex zum
			// Outside-Set dieses Dreiecks hinzu
			for (int i = 0; i < mTriangles.size(); i++) {

				// Dreieck ist vom aktuellen Punkt aus sichtbar => fuege den
				// Punkt zum OutsideSet hinzu
				if (isFaceVisible(mTriangles.get(i), currentVert)) {

					// Punkt liegt ausserhalb
					isPointOutside = true;

					// adde Punkt und Triangle zur Mapstruktur
					currentTriList = mVisibleTriangles.get(currentVert);
					if (currentTriList == null)
						currentTriList = new ArrayList<QuickHullTriangle>();
					currentTriList.add(mTriangles.get(i));
					mVisibleTriangles.put(currentVert, currentTriList);

					// und aktualisiere gleichzeitig die Outsidesets
					outsideVerts = mOutsideSets.get(mTriangles.get(i));
					if (outsideVerts == null)
						outsideVerts = new ArrayList<Vertex3d>();
					outsideVerts.add(currentVert);
					mOutsideSets.put(mTriangles.get(i), outsideVerts);
				}
			}

			// liegt der aktuelle Punkt innerhalb des Polyeders, sieht also kein
			// Face, entferne ihn
			if (!isPointOutside)
				vertIter.remove();
		}

		// Abbruch, wenn kein Vertex mehr ausserhalb liegt
		if (mVertexBuffer.size() > 0)
			return true;
		else {
			return false;
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode testet die Sichtbarkeit des uebergebenen Faces fuer den
	 * uebergebenen Punkt, indem alle Faceeckpunkte auf Sichtbarkeit getestet
	 * werden. Sobald ein Test positiv ausfaellt, wird das Face als sichtbar
	 * angesehen
	 * 
	 * @param face
	 *            Polygon, das auf Sichtbarkeit getestet wird
	 * @param point
	 *            Punkt, fuer den die Sichtbarkeit ueberprueft wird
	 * @return True, falls das Polygon sichtbar ist (Winkel zwischen
	 *         Blickrichtugnsvektor und Normale <= 90°), False sonst
	 */
	private boolean isFaceVisible(QuickHullTriangle face, Vertex3d point) {

		MyVector3f faceToPoint = null;
		float angle = 0;
		Vertex3d current = null;
		MyVectormath mathHelper = MyVectormath.getInstance();

		List<Vertex3d> points = face.getPoly().getVertices();
		for (int i = 0; i < points.size(); i++) {
			current = points.get(i);
			faceToPoint = new MyVector3f();
			faceToPoint.sub(point.getPositionPtr(), current.getPositionPtr());
			angle = mathHelper.calculateAngle(faceToPoint, face.getPoly()
					.getNormalPtr());
			if (angle <= 90.0f)
				return true;
		}
		return false;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet den Horizont fuer den uebergebenen Punkt, also die
	 * Kanten der Faces, die den Uebergang zwischen sichtbaren und unsichtbaren
	 * Faces definieren.
	 * 
	 * @param point
	 *            Punkt, fuer den der Horizont berechnet wird
	 */
	private void computeHorizonForPoint(Vertex3d point) {

		// List<Vertex3d> outline =
		// computeOutlineForVisibleTrianglesByConvexHull(point);
		List<Ray> outlineRays = computeOutlineForVisibleTrianglesByNeighbourStructure(point);
		List<QuickHullTriangle> visibleTriangles = mVisibleTriangles.get(point);

		assert visibleTriangles != null : "FEHLER: Fuer den Punkt " + point
				+ " sind keine sichtbaren Faces vorhanden!";

		// alle Dreiecke, die vom aktuellen Punkt aus sichtbar waren, entfernen
		mTrianglesToDelete.addAll(visibleTriangles);

		// und erzeuge neue Faces ueber die HorizonLines und den Startpunkt
		createNewTriangleByHorizonLines(new HashSet<Ray>(outlineRays), point);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet fuer eine uebergebene Menge von Faces saemtliche
	 * Kanten, die ausserhalb der Facemenge verlaufen, indem solche Kanten
	 * gesucht werden, die nur ein adjazentes Face in der Menge aufweisen
	 * 
	 * @param point
	 *            Punkt, fuer den der Horizont bestimmt wird
	 * @return Liste mit Strahlen, die die Horizontkanten beschreiben
	 */
	private List<Ray> computeOutlineForVisibleTrianglesByNeighbourStructure(
			Vertex3d point) {

		List<QuickHullTriangle> tris = mVisibleTriangles.get(point);
		assert tris != null : "FEHLER: Fuer Punkt " + point
				+ " existieren keine sichtbaren Faces!";

		LOGGER.trace("DREIECKE");
		for (int i = 0; i < tris.size(); i++)
			LOGGER.trace(tris.get(i));

		// berechne die Neighbour-Structure
		List<QuickHullNeighbourHelper> neighbourStruct = createNeighbourStructure(tris);
		List<Ray> result = new ArrayList<Ray>();

		// finde die Instanzen, die nur einen einzigen Nachbarn haben, dies sind
		// die Outlines
		QuickHullNeighbourHelper currentHelper = null;
		for (int i = 0; i < neighbourStruct.size(); i++) {
			currentHelper = neighbourStruct.get(i);
			if (currentHelper.getNeighbourCount() == 1)
				result.add(currentHelper.getEdgeAsRay());
		}

		return result;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet neue Dreiecke basierend auf einem durch Vertices
	 * beschriebenen Linienzug und dem uebergebenen Referenzpunkt. Dreiecke
	 * werden dabei jeweils aus dem Referenzpunkt und einem Segment des
	 * Linienzuges gebildet
	 * 
	 * @param outline
	 *            Linienzug, fuer den neue Dreiecke berechnet werden
	 * @param start
	 *            Referenzpunkt, der in allen neu gebildeten Dreiecken vorkommt
	 * @deprecated Wird nur verwendet, wenn der Convex-Hull-Ansatz zur
	 *             Horizontbestimmung eingesetzt wird
	 */
	private void createNewTrianglesByVertexList(List<Vertex3d> outline,
			Vertex3d start) {

		List<Vertex3d> polyVerts = null;
		QuickHullTriangle resultTri = null;

		Vertex3d currentVertex = null, nextVertex = null;

		for (int i = 0; i < outline.size(); i++) {

			currentVertex = outline.get(i);
			if (i < outline.size() - 1)
				nextVertex = outline.get(i + 1);
			else
				nextVertex = outline.get(0);

			// Dreieck erzeugen
			polyVerts = new ArrayList<Vertex3d>(3);

			// erzeuge ein neues Dreieck und loesche direkt die Eckpunkte des
			// Dreiecks aus dem VertexBuffer
			polyVerts.add(currentVertex);
			polyVerts.add(nextVertex);
			polyVerts.add(start);

			resultTri = new QuickHullTriangle(polyVerts);

			// Dreiecksvertices aus dem Buffer loeschen
			for (int k = 0; k < polyVerts.size(); k++)
				mVertexBuffer.remove(polyVerts.get(k));
			LOGGER.trace("Neues Dreieck: " + resultTri);

			mTriangles.add(resultTri);
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Ansatz berechnet den Horizont fuer den uebergebenen Punkt, indem vom
	 * Punkt ausgehend zunaechst alle Punkte der sichtbaren Faces auf eine Ebene
	 * projiziert werden. Fuer diese Punkte wird dann die konvexe Huelle
	 * bestimmt, die genau die gesuchte Horizontlinie darstellt
	 * 
	 * @param point
	 *            Puntk, fuer den der Horizont berechnet wird
	 * @return Liste mit Vertices, die einen Linienzug beschreiben, der die
	 *         konvexe Huelle der projizierten Punkte darstellt
	 * @deprecated Wird durch die Nachbarstruktur direkter und schneller
	 *             ermittelt
	 */
	private List<Vertex3d> computeOutlineForVisibleTrianglesByConvexHull(
			Vertex3d point) {

		List<QuickHullTriangle> visibleTriangles = mVisibleTriangles.get(point);
		if (visibleTriangles == null)
			return new ArrayList<Vertex3d>(0);

		LOGGER.trace("Sichtbares Dreiecke fuer Punkt " + point);

		// bestimme die Horizontlinien als konvexe Huelle um alle Eckpunkte der
		// sichtbaren Dreiecke
		Set<Vertex3d> triangleVertices = new HashSet<Vertex3d>();
		for (int i = 0; i < visibleTriangles.size(); i++) {
			LOGGER.trace("Sichtbares Dreieck: "
					+ visibleTriangles.get(i).getID());
			triangleVertices.addAll(visibleTriangles.get(i).getPoly()
					.getVertices());
		}
		List<Vertex3d> vertexList = new ArrayList<Vertex3d>(triangleVertices);

		/*
		 * List<Vertex3d> vertexList = new ArrayList<Vertex3d>(mVertexBuffer);
		 * vertexList.remove(point);
		 */
		Plane triPlane = null, projectionPlane = null;
		MyVector3f normal = null, stuetzvektor = null, scaledNormal = null;
		List<Vertex3d> projectedVertices = null;

		for (int i = 0; i < visibleTriangles.size(); i++) {
			// die Verwendung des GrahamScan-Algorithmus erfordert die
			// Konstruktion einer Projektionsebene, auf die die TriangleVerts
			// projiziert werden
			// verwende dafuer die Ebene eines sichtbaren Dreiecks
			triPlane = visibleTriangles.get(i).getPoly().getPlane();

			// verschiebe den Stuetzvektor in entgegengesetzter Richtung der
			// Ebenennormalen, um die Plane moeglichst hinter den Polyeder zu
			// schieben
			normal = triPlane.getNormal();
			stuetzvektor = triPlane.getStuetzvektor();

			scaledNormal = normal.clone();
			scaledNormal.scale(-1000.0f);

			stuetzvektor.add(scaledNormal);
			projectionPlane = new Plane(normal, stuetzvektor);
			projectedVertices = null;

			// rechne so lange, bis eine Ebene gefunden wird, bei der die
			// Projektion vollstaendig erfolgt
			// hier kann der Fall auftreten, dass die Projektionsstrahlen die
			// Ebene nicht treffen, in diesem Fall muss eine andere Ebene
			// verwendet werden
			projectedVertices = MyVectormath.getInstance()
					.projectPointsOntoPlaneFromVanishingPoint(projectionPlane,
							point, vertexList);
			if (projectedVertices != null)
				break;
		}

		assert projectedVertices != null : "FEHLER: Es konnte keine Projektion berechnet werden";

		// Mapstruktur verwenden, um nach Berechnung der konvexen Huelle wieder
		// auf die Ausgangsvertices zurueck zu kommen
		Map<Vertex3d, Vertex3d> vertToVert = new HashMap<Vertex3d, Vertex3d>(
				vertexList.size());
		for (int i = 0; i < vertexList.size(); i++)
			vertToVert.put(projectedVertices.get(i), vertexList.get(i));

		List<Vertex3d> convexHullPolyProjected = PrototypeHelper.getInstance()
				.computeConvexHullForUnprojectedVertices(projectedVertices,
						projectionPlane.getNormalPtr());

		List<Vertex3d> result = new ArrayList<Vertex3d>(
				convexHullPolyProjected.size());
		// projizierte Vertices zurueckrechnen
		for (int i = 0; i < convexHullPolyProjected.size(); i++) {
			result.add(vertToVert.get(convexHullPolyProjected.get(i)));
		}

		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet ausgehend von dem Punkt aus dem Outside-Set eines Faces
	 * Kanten, die den Uebergang zwischen von dem Punkt aus sichtbaren und
	 * unsichtbaren Faces definieren, hierfuer verwendet man einen Suchansatz,
	 * der von dem Face aus startet, zu dessen Outside-Set der Punkt gehoert
	 * 
	 * @param face
	 *            Oberflaechensegment, fuer dessen OutsideSet die Horizontlinie
	 *            berechnet werden soll
	 */
	private void computeHorizonForOutsideSet(QuickHullTriangle face) {

		// bestimme den Punkt innerhalb des OutsideSets, der den groessten
		// Abstand zum Face besitzt
		Float maxDistance = -Float.MAX_VALUE, currentDistance = null;
		Vertex3d currentVertex = null, currentMaxVertex = null;

		List<Vertex3d> outsideSet = mOutsideSets.get(face);

		LOGGER.trace("VERTS in OUTSIDESET: " + outsideSet.size());

		for (int i = 0; i < outsideSet.size(); i++) {
			currentVertex = outsideSet.get(i);
			currentDistance = mMathHelper.calculatePointPlaneDistance(
					currentVertex.getPositionPtr(), face.getPoly().getPlane());
			LOGGER.trace("CURRENT DISTACNE: " + currentDistance);
			if (currentDistance > maxDistance) {
				maxDistance = currentDistance;
				currentMaxVertex = currentVertex;
			}
		}

		// ausgehend von dem Punkt mit maximalem Abstand innerhalb des
		// OutsideSets bestimmt man nun die Horizontkanten, indem man die
		// Nachbarn des Vertex durchlaeuft
		// berechne die Horizontlinien rekursiv ausgehend vom aktuellen Dreieck
		Set<Ray> horizonLines = new HashSet<Ray>();
		horizonLines.addAll(checkNeighboursForHorizon(face, null,
				currentMaxVertex));

		// loesche nicht mehr sichtbare Faces
		deleteFacesVisibleForPoint(currentMaxVertex);

		// und erzeuge neue Faces ueber die HorizonLines und den Startpunkt
		createNewTriangleByHorizonLines(horizonLines, currentMaxVertex);

		// alle Dreiecke fuer naechste Berechnung zuruecksetzen
		for (int i = 0; i < mTriangles.size(); i++)
			mTriangles.get(i).setVisited(false);

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Als Abschluss der Iteration fuer einen Punkt innerhalb eines Outside-Sets
	 * loescht man alle Faces aus der Triangle-Menge, die vom Uebergabepunkt aus
	 * sichtbar waren
	 * 
	 * @param point
	 *            Uebergabepunkt, fuer den alle sichtbaren Faces geloescht
	 *            werden
	 */
	private void deleteFacesVisibleForPoint(Vertex3d point) {

		MyVector3f centerToPoint = null;
		QuickHullTriangle currentTri = null;
		float angle = 0;
		for (int i = 0; i < mTriangles.size(); i++) {
			currentTri = mTriangles.get(i);
			centerToPoint = new MyVector3f();
			centerToPoint.sub(point.getPositionPtr(), currentTri.getCenter());
			angle = MyVectormath.getInstance().calculateAngle(centerToPoint,
					currentTri.getPoly().getNormalPtr());
			if (angle < 90.0f) {
				mTrianglesToDelete.add(currentTri);
			}
		}

		LOGGER.trace("Fuer Punkt " + point
				+ " werden folgende Dreiecke geloescht: ");
		for (int i = 0; i < mTrianglesToDelete.size(); i++) {
			LOGGER.trace(mTrianglesToDelete.get(i).getID());
		}

		/*
		 * // durchlaufe alle Faces, die vom Punkt aus sichtbar waren und
		 * loesche sie aus der Outside-Map-Struktur und dem Triangle-Buffer
		 * Set<QuickHullTriangle> triKeys = mOutsideSets.keySet();
		 * List<Vertex3d> visibleVerts = null;
		 * 
		 * Iterator<QuickHullTriangle> triIter = triKeys.iterator();
		 * QuickHullTriangle currentTriangle = null;
		 * 
		 * while(triIter.hasNext()) { currentTriangle = triIter.next();
		 * visibleVerts = mOutsideSets.get(currentTriangle);
		 * if(visibleVerts.contains(point)) { // aus der Mapstruktur loeschen
		 * mTrianglesToDelete.add(currentTriangle); } }
		 */
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet neue Dreiecke auf der konvexen Huelle basierend auf den
	 * berechneten Hoizontlinien und dem berechneten Startpunkt und fuegt diese
	 * zum Triangle-Buffer hinzu
	 * 
	 * @param horizonLines
	 *            Set mit einer Menge von Linien, die fuer den uebergebenen
	 *            Startpunkt den Horizont bilden
	 * @param startPoint
	 *            Startpunkt, fuer den mit jeder Linie im Set ein neues Dreieck
	 *            berechnet wird
	 */
	private void createNewTriangleByHorizonLines(Set<Ray> horizonLines,
			Vertex3d startPoint) {

		LOGGER.trace("#HORIZON LINES: " + horizonLines.size()
				+ " Ausgangspunkt: " + startPoint);
		Iterator<Ray> lineIter = horizonLines.iterator();
		int index = 0;
		while (lineIter.hasNext()) {
			LOGGER.trace(index + ": " + lineIter.next());
			index++;
		}

		lineIter = horizonLines.iterator();
		Ray currentRay = null;
		List<Vertex3d> polyVerts = null;
		QuickHullTriangle resultTri = null;

		while (lineIter.hasNext()) {
			polyVerts = new ArrayList<Vertex3d>(3);
			currentRay = lineIter.next();

			// erzeuge ein neues Dreieck und loesche direkt die Eckpunkte des
			// Dreiecks aus dem VertexBuffer
			polyVerts.add(new Vertex3d(currentRay.getStart()));
			polyVerts.add(new Vertex3d(currentRay.getEnd()));
			polyVerts.add(startPoint);

			resultTri = new QuickHullTriangle(polyVerts);

			// Dreiecksvertices aus dem Buffer loeschen
			for (int i = 0; i < polyVerts.size(); i++) {
				if (mVertexBuffer.contains(polyVerts.get(i))) {
					mVertexBuffer.remove(polyVerts.get(i));
					// logger.trace("Vertex " + polyVerts.get(i) +
					// " aus dem VertexBuffer entfernt!");
				} else {
					// logger.error("Vertex " + polyVerts.get(i) +
					// " befindet sich nicht im VertexBuffer");
				}
			}

			LOGGER.trace("Neues Dreieck: " + resultTri);
			mTriangles.add(resultTri);
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode durchsucht rekursiv alle vorhandenen Dreiecke nach Nachbarn, die
	 * vom aktuellen Startpunkt aus nicht mehr sichtbar sind.
	 * 
	 * @param currentStart
	 *            Dreieck, von dem ausgehend nach Horizontlinien gesucht werden
	 *            soll
	 * @param lastStart
	 *            Dreieck, von dem aus die letzte Rekursion ausgeloest wurde
	 * @param startPoint
	 *            Punkt, von dem ausgehend Dreiecke auf Sichtbarkeit getestet
	 *            werden
	 * @return Liste mit allen entdeckten Hoirzontkanten
	 */
	private Set<Ray> checkNeighboursForHorizon(QuickHullTriangle currentStart,
			QuickHullTriangle lastStart, Vertex3d startPoint) {

		Set<Ray> horizonLines = new HashSet<Ray>();

		// Abbruchkriterium testen, bereits besuchte Dreiecke skippen
		if (currentStart.isVisited())
			return horizonLines;

		// noch nicht besuchte Dreiecke als besucht markieren
		else
			currentStart.setVisited(true);

		QuickHullTriangle[] neighbours = currentStart.getNeighbours();
		QuickHullTriangle currentTriangle = null;
		MyVector3f pointToTriangle = null, currentPolyPoint = null;
		Float currentAngle = null;
		Ray horizonRay = null;

		for (int i = 0; i < neighbours.length; i++) {

			currentTriangle = neighbours[i];

			// teste die direkten Dreiecke
			currentPolyPoint = currentTriangle.getCenter();
			pointToTriangle = new MyVector3f();

			// Vektor vom Polygonmittelpunkt auf den Startpunkt
			pointToTriangle.sub(startPoint.getPositionPtr(), currentPolyPoint);
			currentAngle = mMathHelper.calculateAngle(pointToTriangle,
					currentTriangle.getPoly().getNormalPtr());

			// Dreieck ist nicht sichtbar
			// berechne die gemeinsame Kante
			if (currentAngle > 90.0f) {
				horizonRay = getSharedEdge(currentStart, currentTriangle);
				LOGGER.trace("Geteilte Kante: " + horizonRay);
				horizonLines.add(horizonRay);
			} else {
				// starte Rekursion, allerdings nur, wenn das aktuelle
				// Nachbardreieck nicht dem Dreieck entspricht, das die letzte
				// Rekursion gestartet hat
				if (currentTriangle != null && currentTriangle == lastStart)
					continue;
				else
					horizonLines.addAll(checkNeighboursForHorizon(
							currentTriangle, currentStart, startPoint));
			}

		}
		return horizonLines;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet eine Kante, die sich die uebergebenen Dreiecke teilen
	 * 
	 * @param first
	 *            Erstes Eingabedreieck
	 * @param second
	 *            Zweites Eingabedreieck
	 * @return Strahl, der die geteilte Kante beschreibt
	 */
	private Ray getSharedEdge(QuickHullTriangle first, QuickHullTriangle second) {

		// bestimme zwei Vertices, die in beiden Dreiecken vorkommen, zwischen
		// diesen verlaeuft die geteilte Kante
		List<Vertex3d> sharedVertices = new ArrayList<Vertex3d>(2);
		List<Vertex3d> firstTriangleVerts = first.getPoly().getVertices();
		List<Vertex3d> secondTriangleVerts = second.getPoly().getVertices();

		Vertex3d currentFirst = null;
		for (int i = 0; i < firstTriangleVerts.size(); i++) {
			currentFirst = firstTriangleVerts.get(i);
			if (secondTriangleVerts.contains(currentFirst))
				sharedVertices.add(currentFirst);
		}

		// assert sharedVertices.size() == 2: "FEHLER: Es wurden "
		// +sharedVertices.size() + " gemeinsame Vertices gefunden!";
		if (sharedVertices.size() == 2)
			return new Ray(sharedVertices.get(0), sharedVertices.get(1));
		else
			return null;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet einen initialen Koerper, von dem ausgehend der
	 * Algorithmus startet. Zunaechst such man Extrempunkte in allen 3
	 * Koordinatenachsen. Diese verwendet man dann zur Konstruktion des
	 * Startkoepers
	 */
	private void init() {

		// Array nimmt die Vertices mit maximalen Koordinaten auf
		Vertex3d[] minMaxSet = new Vertex3d[6];
		Vertex3d currentVert = null;

		Float min_x = Float.MAX_VALUE, min_y = Float.MAX_VALUE, min_z = Float.MAX_VALUE;
		Float max_x = -Float.MAX_VALUE, max_y = -Float.MAX_VALUE, max_z = -Float.MAX_VALUE;

		for (int i = 0; i < mVertices.size(); i++) {
			currentVert = mVertices.get(i);

			if (currentVert.getX() < min_x) {
				minMaxSet[0] = currentVert;
				min_x = currentVert.getX();
			}

			if (currentVert.getX() > max_x) {
				minMaxSet[1] = currentVert;
				max_x = currentVert.getX();
			}

			if (currentVert.getY() < min_y) {
				minMaxSet[2] = currentVert;
				min_y = currentVert.getY();
			}

			if (currentVert.getY() > max_y) {
				minMaxSet[3] = currentVert;
				max_y = currentVert.getY();
			}

			if (currentVert.getZ() < min_z) {
				minMaxSet[4] = currentVert;
				min_z = currentVert.getZ();
			}

			if (currentVert.getZ() > max_z) {
				minMaxSet[5] = currentVert;
				max_z = currentVert.getZ();
			}
		}

		// enfterne die verwendeten Vertices aus dem Vertex-Container, da diese
		// nicht mehr weiter getestet werden muessen
		for (int i = 0; i < minMaxSet.length; i++) {
			LOGGER.trace("Vertex " + minMaxSet[i]
					+ " aus dem VertexBuffer entfernt!");
			mVertexBuffer.remove(minMaxSet[i]);
		}

		Ray[] rays = new Ray[3];

		String message = "minX: " + min_x + " " + minMaxSet[0] + " max_x: "
				+ max_x + " " + minMaxSet[1] + " minY: " + min_y + " "
				+ minMaxSet[2] + " maxY: " + max_y + " " + minMaxSet[3]
				+ " minZ: " + min_z + " " + minMaxSet[4] + " maxZ: " + max_z
				+ " " + minMaxSet[5];
		LOGGER.error(message);

		// berechne Kanten zwischen den min-max-Vertices jeder Koordinatenachse
		rays[0] = new Ray(minMaxSet[0], minMaxSet[1]);
		rays[1] = new Ray(minMaxSet[2], minMaxSet[3]);
		rays[2] = new Ray(minMaxSet[4], minMaxSet[5]);

		// bestimme den Strahl mit maximaler Laenge
		float longest = -Float.MAX_VALUE;
		Ray currentLongest = null;

		for (int i = 0; i < 3; i++) {
			if (rays[i].getLength() > longest) {
				longest = rays[i].getLength();
				currentLongest = rays[i];
			}
		}

		// bestimme nun den Punkt innerhalb des min-max-Sets, der den groessten
		// Abstand zur berechenten Kante besitzt
		longest = -Float.MAX_VALUE;
		float currentDistance = 0;
		Vertex3d maxVert = null;

		MyVectormath mathHelper = MyVectormath.getInstance();

		for (int i = 0; i < minMaxSet.length; i++) {
			currentVert = minMaxSet[i];
			currentDistance = mathHelper.calculatePointEdgeDistance(
					currentVert.getPositionPtr(), currentLongest);
			if (currentDistance > longest) {
				longest = currentDistance;
				maxVert = currentVert;
			}
		}

		// das erste Dreieck des Koerpers wird durch die Kante und den Punkt
		// beschrieben, erzeuge eine Triangle-Instanz
		List<Vertex3d> points = new ArrayList<Vertex3d>(3);
		points.add(maxVert);
		points.add(currentLongest.getStartVertex());
		points.add(currentLongest.getEndVertex());

		LOGGER.error("MAX: " + maxVert + " Longest Start: " + points.get(1)
				+ " Longest End: " + points.get(2));

		QuickHullTriangle firstTriangle = new QuickHullTriangle(points);
		mTriangles.add(firstTriangle);
		Plane trianglePlane = firstTriangle.getPoly().getPlane();

		// bestimme den Punkt aus dem initialen Set, der den groessten Abstand
		// zur berechneten Ebene besitzt
		longest = -Float.MAX_VALUE;
		currentDistance = -1;
		for (int i = 0; i < minMaxSet.length; i++) {
			currentDistance = mathHelper.calculatePointPlaneDistance(
					minMaxSet[i].getPositionPtr(), trianglePlane);
			if (currentDistance > longest) {
				currentVert = minMaxSet[i];
				longest = currentDistance;
			}
		}

		// mit dem so bestimmten Punkt und den 3 kanten des ersten Dreiecks
		// bildet man weitere Dreiecke und bekommt darueber den initialen
		// Startkoerper
		List<Ray> polyRays = firstTriangle.getPoly().getRays();
		QuickHullTriangle currentPoly = null;

		for (int i = 0; i < polyRays.size(); i++) {
			points = new ArrayList<Vertex3d>();
			points.add(currentVert);
			points.add(new Vertex3d(polyRays.get(i).getStart()));
			points.add(new Vertex3d(polyRays.get(i).getEnd()));
			currentPoly = new QuickHullTriangle(points);
			mTriangles.add(currentPoly);
		}

		// aktualisiere die Nachbarschaftsbeziehungen zwischen den Dreiecken
		// aufgrund der bidirektionalen Aktualisierung kann man die folgenden
		// geschachtelten Schleifen verwenden
		for (int i = 0; i < mTriangles.size(); i++) {
			for (int k = i + 1; k < mTriangles.size(); k++) {
				mTriangles.get(i).addNeighbour(mTriangles.get(k));
			}
		}

		normalizeNormals();

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode liefert alle Vertices, die zu einem beliebigen
	 * Berechnungszeitpunkt noch im VertexBuffer vorhanden sind
	 * 
	 * @return Liste mit Vertices
	 */
	public List<Vertex3d> getCurrentVertices() {
		return mVertexBuffer;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * 
	 * @author Patrick Gunia Klasse dient der Verwaltung der speziellen
	 *         Anforderungen des QuickHull-Algorithmus bsw. Speicherung der
	 *         Nachbarn fuer jedes Dreieck
	 * 
	 */
	private class QuickHullTriangle {

		/** Beschreibung des Dreiecks durch eine Polygonstruktur */
		private MyPolygon mTriangle = null;

		/** Speichert Pointer auf alle 3 adjazenten Dreiecke fuer jedes Dreieck */
		private QuickHullTriangle[] mNeighbours = new QuickHullTriangle[3];

		/** Flag fuer Rekursionsabbruch bei Horizontbestimmung */
		private boolean mVisited = false;

		/** Mittelpunkt des Dreiecks */
		private MyVector3f mCenter = null;

		/** ID, dient dem Debugging */
		private String mID = null;

		// ------------------------------------------------------------------------------------------

		/**
		 * 
		 * @param vertices
		 *            Vertices, die das aktuelle Dreieck beschreiben
		 */
		public QuickHullTriangle(List<Vertex3d> vertices) {
			mTriangle = new MyPolygon(vertices);
			for (int i = 0; i < mNeighbours.length; i++)
				mNeighbours[i] = null;
			mCenter = MyVectormath.getInstance().calculatePolygonCenter(
					vertices);
			mID = "Tri_" + Quickhull3d.this.getTriangleIndex();
		}

		// ------------------------------------------------------------------------------------------

		public MyPolygon getPoly() {
			return mTriangle;
		}

		// ------------------------------------------------------------------------------------------

		public QuickHullTriangle[] getNeighbours() {
			return mNeighbours;
		}

		// ------------------------------------------------------------------------------------------

		/**
		 * @return the mVisited
		 */
		public boolean isVisited() {
			return mVisited;
		}

		// ------------------------------------------------------------------------------------------

		/**
		 * @param mVisited
		 *            the mVisited to set
		 */
		public void setVisited(boolean mVisited) {
			this.mVisited = mVisited;
		}

		// ------------------------------------------------------------------------------------------
		/**
		 * @return the mCenter
		 */
		public MyVector3f getCenter() {
			return mCenter;
		}

		// ------------------------------------------------------------------------------------------
		public String getID() {
			return mID;
		}

		// ------------------------------------------------------------------------------------------
		/**
		 * Methode fuegt ein Nachbardreieck zum aktuellen Dreieck hinzu und baut
		 * direkt die bidirektionale Beziehung auf
		 * 
		 * @param neighbour
		 */
		public void addNeighbour(QuickHullTriangle neighbour) {
			boolean added = false;

			// Nachbarn an die erste freie Stelle setzen,
			for (int i = 0; i < mNeighbours.length; i++) {
				if (mNeighbours[i] == null) {
					added = true;
					mNeighbours[i] = neighbour;

					// den Nachbarn auch auf der anderen Seite setzen
					neighbour.addNeighbour(this);
					break;
				} else if (mNeighbours[i] == neighbour)
					return;
			}
			assert added : "FEHLER: Nachbardreieck konnte nicht geadded werden!";
		}

		// ------------------------------------------------------------------------------------------
		/**
		 * Methode setzt die Nachbarn des Dreiecks auf null
		 */
		public void resetNeighbourhood() {
			for (int i = 0; i < mNeighbours.length; i++)
				mNeighbours[i] = null;
		}

		// ------------------------------------------------------------------------------------------

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "QuickHullTriangle [mID=" + mID + " " + mTriangle + "]";
		}

		// ------------------------------------------------------------------------------------------
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * 
	 * @author Patrick Gunia Klasse dient der effizienten hashbasierten
	 *         Nachbarschaftsberechnung, indem Strahlen nur durch Start- und
	 *         Endvertex beschrieben werden. Equals testet nur, ob eine Struktur
	 *         die Vertices enthaelt unabhaengig von der Reihenfolge. Dadurch
	 *         spielt die Abfolge der Vertices beim Konstruktoraufruf keine
	 *         Rolle fuer den Vergleich.
	 */
	private class QuickHullNeighbourHelper {

		/** Vertices innerhalb der aktuellen Neighbourstruktur */
		private List<Vertex3d> mVertices = new ArrayList<Vertex3d>(2);

		/**
		 * Nachbardreiecke fuer die Kante, die durch die beiden Vertices
		 * beschrieben wird
		 */
		private Set<QuickHullTriangle> mNeighbours = new HashSet<QuickHullTriangle>(
				2);

		public QuickHullNeighbourHelper(Vertex3d first, Vertex3d second) {

			mVertices.add(first);
			mVertices.add(second);
		}

		// ------------------------------------------------------------------------------------------
		public void addNeighbour(QuickHullTriangle neighbour) {
			mNeighbours.add(neighbour);
		}

		// ------------------------------------------------------------------------------------------
		public List<QuickHullTriangle> getNeighboursAsList() {

			List<QuickHullTriangle> result = new ArrayList<QuickHullTriangle>(
					mNeighbours.size());
			result.addAll(mNeighbours);
			return result;

		}

		// ------------------------------------------------------------------------------------------
		public int getNeighbourCount() {
			return mNeighbours.size();
		}

		// ------------------------------------------------------------------------------------------
		public Ray getEdgeAsRay() {
			return new Ray(mVertices.get(0), mVertices.get(1));
		}

		// ------------------------------------------------------------------------------------------

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			QuickHullNeighbourHelper other = (QuickHullNeighbourHelper) obj;

			List<Vertex3d> otherVerts = other.getVertices();
			/*
			 * System.out.println("OTHERS: "); System.out.println("0: " +
			 * otherVerts.get(0) + " 1: " + otherVerts.get(1));
			 * System.out.println("THESE: "); System.out.println("0: " +
			 * mVertices.get(0) + " 1: " + mVertices.get(1));
			 */

			for (int i = 0; i < mVertices.size(); i++) {
				if (!otherVerts.contains(mVertices.get(i))) {
					// System.out.println("FALSE");
					return false;
				}
			}
			// System.out.println("TRUE");
			return true;
		}

		// ------------------------------------------------------------------------------------------

		/**
		 * @return the mVertices
		 */
		public List<Vertex3d> getVertices() {
			return mVertices;
		}

		// ------------------------------------------------------------------------------------------

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "QuickHullNeighbourHelper [mVertices=" + mVertices + "]";
		}

		// ------------------------------------------------------------------------------------------

		private Quickhull3d getOuterType() {
			return Quickhull3d.this;
		}

	}

	// ------------------------------------------------------------------------------------------

}
