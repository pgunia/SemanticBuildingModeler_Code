package semantic.building.modeler.weightedstraightskeleton.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;

import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.Plane;
import semantic.building.modeler.math.Ray;
import semantic.building.modeler.weightedstraightskeleton.exception.SquareCaseException;
import semantic.building.modeler.weightedstraightskeleton.math.MySkeletonVectormath;

/**
 * @author Patrick Gunia
 * 
 *         ein SkeletonJob besteht aus einem Polygon, das verarbeitet wird und
 *         aus einer Reihe von Steuervariablen, die nur fuer diesen Job gelten
 */
public class SkeletonJob implements Runnable {

	/** Logger */
	protected static Logger LOGGER = Logger.getLogger(SkeletonJob.class);

	/**
	 * Algorithmuscontroller, von dem aus die Hauptabfolge gesteuert und
	 * verarbeitet wird
	 */
	private StraightSkeleton mAlgorithm = null;

	/** enthaelt des Polygon, das durch den Job verarbeitet werden soll */
	private SkeletonPolygon mPolygon;

	/**
	 * Flag speichert, ob es sich bei der aktuellen Iteration um den ersten
	 * Durchlauf handelt
	 */
	private boolean mCompletedFirstIteration = false;

	/**
	 * Wenn ein Reflex-Vertex gefunden wurde, muss auf Split-Events getestet
	 * werden
	 */
	private boolean mFoundReflexVertex = false;

	/**
	 * Wenn mehr als ein Split-Event gefunden wurde, sind Vertex-Events moeglich
	 */
	private boolean mVertexEventPossible = false;

	/** Flag haelt fest, ob ein Split-Event aufgetreten ist */
	private boolean mSplitEventOccured = false;

	/** Flag haelt fest, ob ein Vertex-Event aufgetreten ist */
	private boolean mVertexEventOccured = false;

	/** Struktur zur Erzeugung und Verwaltung virtueller Kanten */
	private VirtualEdgeManager mVirtualEdgeManager = null;

	/**
	 * Berechnungstiefe => entspricht in der Praxis der Anzahl der Kinder der
	 * Wurzelelemente
	 */
	private int mLevel;

	/** Instanz der Mathebibliothek fuer alle relevanten Berechnungen */
	private MySkeletonVectormath mMathHelper = null;

	/** speichert alle in diesem Durchlauf aufgetretenen Events */
	private List<iStraightSkeletonEvent> mEvents = null;

	/** nimmt die neu berechneten Vertices eines Durchlaufs auf */
	private List<SkeletonVertex> mChildBuffer = null;

	/**
	 * Systemspezifisches Sonderzeichen fuer Line-Breaks fuer formatierte
	 * Ausgabe
	 */
	private static String mLineBreak = System.getProperty("line.separator");

	/**
	 * Differenzwert unterhalb dessen Events bzgl. ihrer Distanz als
	 * "gleichzeitig" betrachtet werden
	 */
	private final float mAccetableDistanceDelta = 0.1f;

	/**
	 * werden bei der ersten Iteration an den Startjob uebergeben, anschliessend
	 * von Eltern zu Kindern weitergereicht
	 */
	private List<Float> mEdgeWeights = null;

	/** Richtung, in die das Dach erzeugt wird */
	private MyVector3f mBuildDirection = null;

	/** Flag zeigt an, ob ein Slope-Event berechnet wurde */
	private Boolean mProcessedSlopeEvent = false;

	// ------------------------------------------------------------------------------------------

	/**
	 * Konstruktor fuer einen Skeleton-Job. Ein Job besteht in genau einer
	 * Iteration des Algorithmus auf dem uebergebenen Polygon. Nach Abschluss
	 * wird mit dem Ergebnis der Berechnung einer (oder mehrere) neue Jobs
	 * erzeugt, die unabhaengig von einander berechnet werden
	 * 
	 * @param mPolygon
	 *            Eingabepolygon, auf dem die Berechnungen des Algorithmus
	 *            durchgefuehrt werden.
	 * @param level
	 *            Aktuelle Iterationsebene. Bei jeder neuen Iteration wird die
	 *            Ebene inkrementiert.
	 * @param firstIteration
	 *            Flag, das die erste Iteration kennzeichnet
	 * @param algorithm
	 * @param extrusionNormal
	 */
	public SkeletonJob(SkeletonPolygon mPolygon, int level,
			boolean firstIteration, StraightSkeleton algorithm,
			MyVector3f extrusionNormal) {
		super();
		this.mAlgorithm = algorithm;
		this.mPolygon = mPolygon;
		this.mMathHelper = MySkeletonVectormath.getInstance();
		this.mLevel = level;
		mEvents = new Vector<iStraightSkeletonEvent>();
		mChildBuffer = new Vector<SkeletonVertex>();
		mCompletedFirstIteration = firstIteration;
		mVirtualEdgeManager = new VirtualEdgeManager(this.mMathHelper);
		mBuildDirection = extrusionNormal;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode fuehrt zu Beginn jeder Iteration saemtliche Berechnungen durch,
	 * die waehrend der Iteration benoetigt werden, dazu gehoeren
	 * Winkelhalbierende, Slope-Planex, Nachbarschaften etc.
	 */
	private void doVertexComputations() {

		// globale Steuervariable zuruecksetzen
		mFoundReflexVertex = false;
		final List<SkeletonVertex> currentVertices = mPolygon.getVertices();

		SkeletonVertex a = null, b = null, c = null;
		MyVector3f ba = null, bc = null, normal = null, winkelhalbierendeVec = null;
		Ray rWinkelhalbierende = null;

		int centerIndex = -1;

		// durchlaufe alle Vertices der Reihe nach und berechne fuer je 3
		// Vertices die Winkelhalbierenden
		for (int i = 0; i < currentVertices.size(); i++) {

			// beim ersten Durchlauf berechne Vertices und Nachbarschaften
			if (!mCompletedFirstIteration) {
				// b ist immer das Vertex, an dem der Winkel bestimmt wird
				a = currentVertices.get(i);

				// Ueberhang-Index-Verschiebung
				if (i + 2 < currentVertices.size()) {
					centerIndex = i + 1;
					b = currentVertices.get(centerIndex);
					c = currentVertices.get(i + 2);
				} else if (i + 1 < currentVertices.size()) {
					centerIndex = i + 1;
					b = currentVertices.get(centerIndex);
					c = currentVertices.get(0);
				} else {
					centerIndex = 0;
					b = currentVertices.get(centerIndex);
					c = currentVertices.get(1);
				}
				// wenn die erste Iteration beendet ist, bediene dich bei den
				// berechneten Nachbarn
			} else {
				b = currentVertices.get(i);
				a = b.getNeighbourByIndex(1);
				c = b.getNeighbourByIndex(0);
			}
			// berechne darauf die verschiedenen Vektoren
			// ba ist der Vektor von Vertex b auf Vertex a
			LOGGER.trace(b);
			ba = new MyVector3f();
			bc = new MyVector3f();

			assert b != null : "Fehler: Vertex b ist NULL";
			assert a != null : "Fehler: Vertex a ist NULL";
			assert c != null : "Fehler: Vertex c ist NULL";

			ba.sub(a.getPosition(), b.getPosition());
			bc.sub(c.getPosition(), b.getPosition());

			final SkeletonEdge eBA = new SkeletonEdge(b, a);
			final SkeletonEdge eBC = new SkeletonEdge(b, c);

			// berechne aus ba und bc die Vertexnormale
			normal = new MyVector3f();
			normal.cross(bc, ba);

			normal.normalize();
			// mMathHelper.roundVector3f(normal);

			b.setNormal(normal);

			winkelhalbierendeVec = mMathHelper.calculateWinkelhalbierende(eBA,
					eBC);

			rWinkelhalbierende = new Ray(b.getPosition(), winkelhalbierendeVec);

			// speichere die Winkelhalbierende im aktuell verarbeiteten Vertex
			b.setWinkelhalbierende(rWinkelhalbierende);

			// speichere, ob es sich um ein Reflex-Vertex handelt
			if (mMathHelper.isReflexVertex(eBA, eBC)) {

				// globale Steuervariable setzen => entscheidend, ob nach Split-
				// und Vertex-Events gesucht wird
				mFoundReflexVertex = true;

				b.setIsReflexVertex(true);

				// wenn es sich um ein Reflex-Vertex handelt, muss die
				// Winkelhalbierende aktualisiert werden
				mMathHelper.calculateWinkelhalbierendeForReflexVertex(b);

				// Update der lokalen Variablen;
				rWinkelhalbierende = b.getWinkelhalbierende();
				winkelhalbierendeVec = rWinkelhalbierende.getDirection();
				// winkelhalbierendeVec =
				// mMathHelper.roundVector3f(winkelhalbierendeVec);

				// wenn es sich um ein Reflex-Vertex handelt, dann zeigt der
				// Normalenvektor in die "falsche" Richtung
				normal = b.getNormal();
				normal.scale(-1.0f);
				b.setNormal(normal);

			} else {
				b.setIsReflexVertex(false);
			}
			// Nachbarvertices speichern, sofern es sich um den ersten Durchlauf
			// handelt
			// sonst sind die Nachbarn bereits gesetzt
			// Nachbar 0 ist naechstes Vertex im Uhrzeigersinn
			// Nachbar 1 ist naechstes Vertex entgegen dem Uhrzeigersinn
			if (!mCompletedFirstIteration) {
				b.setNeighbourOnIndex(0, c);
				b.setNeighbourOnIndex(1, a);
			}

			// speichere das Gewicht der ersten Kante
			if (!mCompletedFirstIteration) {
				assert mEdgeWeights != null : "FEHLER: Es wurden keine Kantengewichte gesetzt!";

				// Gewichte den Kanten zuweisen
				Integer previousIndex = null;
				if (centerIndex == 0)
					previousIndex = currentVertices.size() - 1;
				else
					previousIndex = centerIndex - 1;

				// Kante mit Index 0
				eBC.setWeight(mEdgeWeights.get(centerIndex));

				// Kante mit Index 1
				eBA.setWeight(mEdgeWeights.get(previousIndex));

				// adde die Richtungen und Gewichtungen zum Edge-Weightservice
				EdgeWeightService weightService = mAlgorithm
						.getEdgeWeightService();
				weightService.addWeight(eBC.getDirection(),
						mEdgeWeights.get(centerIndex));
				weightService.addWeight(eBA.getDirection(),
						mEdgeWeights.get(previousIndex));

			}

			assert eBC != null && eBA != null : "FEHLER: Es konnten nicht beide Nachbarkanten bestimmt werden! eBA: "
					+ eBA + " eBC: " + eBC;

			LOGGER.trace("CURRENT VERTEX: " + b + " Edge eBC: " + eBC
					+ " Edge eBA: " + eBA);

			// adjazente Kanten speichern
			b.addNeighbourEdge(eBC);
			b.addNeighbourEdge(eBA);

			// berechne den orhtogonalen Vektor an das Vertex
			MyVector3f orthogonale = mMathHelper
					.calculateOrthogonalVectorWithSamePlane(
							winkelhalbierendeVec, b.getNormal());
			// orthogonale = mMathHelper.roundVector3f(orthogonale);
			b.setOrtho(orthogonale);
		}

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode fuehrt abschliessende Berechnungen fuer die Vertex-Strukturen
	 * durch
	 */
	private void finalizeInitialization() {

		// Iteriere ueber alle Vertices und berechne die Slope-Planes der Kanten
		final List<SkeletonVertex> verts = mPolygon.getVertices();

		for (SkeletonVertex currentVertex : verts) {
			currentVertex.computeSlopePlanes(mAlgorithm);
		}

		// berechne die rotierten Winkelhalbierenden an den Vertices basierend
		// auf Schnittgeradenbestimmungen zwischen adjazenten Planes
		computeRotatedWinkelhalbierende();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode durchlaueft alle Vertices und berechnet deren rotierte
	 * Winkelhalbierende auf Basis der Schnittgerade der adjazenten Ebenen an
	 * diesem Vertex.
	 */
	private void computeRotatedWinkelhalbierende() {

		SkeletonVertex currentVertex = null, currentVertexNeighbour1 = null;
		Plane plane0 = null, plane1 = null;
		Ray rotatedWinkelhalbierende = null;
		MyVector3f rayDirection = null;

		// printJobResult();

		LOGGER.trace(mPolygon.getVertices().size());

		Iterator<SkeletonVertex> vertIter = mPolygon.getVertices().iterator();
		while (vertIter.hasNext()) {
			currentVertex = vertIter.next();
			currentVertexNeighbour1 = currentVertex.getNeighbourByIndex(1);

			LOGGER.trace("CURRENTNEIGHBOUR1: " + currentVertexNeighbour1
					+ " CURRENTNEIGHBOUREDGE: "
					+ currentVertexNeighbour1.getNeighbourEdgeByIndex(0));

			plane1 = currentVertexNeighbour1.getNeighbourEdgeByIndex(0)
					.getPlane();
			plane0 = currentVertex.getNeighbourEdgeByIndex(0).getPlane();
			rayDirection = mMathHelper
					.calculatePlane2PlaneIntersectionDirection(plane1, plane0);

			// wenn es sich um ein Reflex-Vertex handelt, so zeigt die
			// Halbierende in die falsche Richtung
			if (currentVertex.isReflexVertex()) {
				rayDirection.scale(-1.0f);
			}
			rotatedWinkelhalbierende = new Ray(currentVertex.getPosition(),
					rayDirection);
			assert rotatedWinkelhalbierende != null : "Es konnte keine rotierte Winkelhalbierende fuer Vertex: "
					+ currentVertex + " berechnet werden";
			currentVertex.setRotatedWinkelhalbierende(rotatedWinkelhalbierende);
		}
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Hauptverarbeitungsroutine eines SkeletonJobs. Fuehrt alle Schritte von
	 * der Verarbeitung des Eingabepolygons ueber die Event-Berechnung bis zum
	 * Shrinking durch. Testet, ob der Algorithmus terminiert, added sonst einen
	 * weiteren Job zur globalen Job-Queue
	 */
	@Override
	public void run() {

		LOGGER.debug("Berechne Level " + mLevel);

		if (!mCompletedFirstIteration) {
			doVertexComputations();
			finalizeInitialization();
		}

		LOGGER.debug(printInputPoly());
		LOGGER.debug(printNeighbourStructure());

		// Slope-Events werden nur einmal berechnet, sobald die Aenderung
		// realisiert wurde, werden keine weiteren Aenderungen mehr
		// durchgefuehrt
		if (!mAlgorithm.getProcessedSlopeEvent()) {
			computeChangeSlopeEvents();
		}

		computeIntersectionEvents();

		preprocessEventBuffer();

		adjustEvents();

		// die Berechnung wird nur durchgefuehrt, wenn das Polygon ein
		// Reflex-Vertex enthaelt
		if (mFoundReflexVertex) {
			detectSplitEvents();
		}

		// die Berechnung wird nur durchgefuehrt, falls mindestens 2
		// gleichzeitige Split-Events gefunden wurden
		if (mVertexEventPossible) {
			int numberOfEvents = mEvents.size();
			detectVertexEvents();

			// wenn sich die Anzahl der Events im Buffer veraendert hat, so
			// wurde ein Vertex-Event mit
			// u.U. anderer Distanz hinzugefuegt, verarbeite darum den
			// Event-Buffer erneut
			if (mEvents.size() != numberOfEvents)
				preprocessEventBuffer();
		}

		// teste auf Square-Case-Sonderfaelle
		if (mSplitEventOccured) {
			assert !findIrregularSplitEvents() : "SquareCaseException";
		}

		// speichere alle zu berechnenden Events im globalen Event-Buffer
		mAlgorithm.addToEventBuffer(mEvents);

		printEventBuffer("Umgesetzte Events: ");

		// fuehre den Schrumpfungsprozess durch
		if (mEvents.size() > 0) {

			processEvents();

			shrink();

			if (mSplitEventOccured) {
				computeVirtualEdges();
			}

			updateNeighbours();

			updateNeighboursForIntersectionEvents();

			if (mSplitEventOccured)
				updateNeighboursForSplitEvents();

			if (mVertexEventOccured)
				updateNeighboursForVertexEventsNewSchool();

			// fuehrt die reflexiven Nachbarschaftsberechnungen fuer alle
			// Vertices aus, die ueber Zwillinge verfuegen
			if (mSplitEventOccured || mVertexEventOccured) {
				updateNeighboursForTwinVertices();

				// stellt die Verbindung zwischen Vertex-Event-Parents und
				// Zwillingen her
				connectVertexEventParents();
			}

			// teste Abbruchkriterium
			if (!hasReachedEnd()) {

				LOGGER.trace(printChildren());

				mCompletedFirstIteration = true;

				// raeume auf und vervollstaendige virtuelle Kanten
				cleanUp();

				// validiere die Nachbarschaftsbeziehungen
				validatePolygonStructure();

				// da durch die Berechnung der Polygonvalidierung neue Vertices
				// zu den virtuellen Kanten hinzugekommen sein koennen
				// berechne die Abstaende neu und sortiere den Kantenbuffer
				mVirtualEdgeManager.finalizeVirtualEdgeStructures();

				// wenn kein Abbruch, dann bereite die naechste Iteration vor
				// teste hier die Abbruchbedingungen erneut, da durch die
				// Polygonvalidierung Vertices aus dem Childbuffer geloescht
				// werden koennen

				if (!hasReachedEnd()) {
					validateBidirectionalNeighbourStructures();
					initializeNextIteration();
				}
			}
		} else {
			LOGGER.info("Abbruch der Berechnung: es wurden keine Events mehr gefunden");
		}

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode fuegt einen ChangeSlope-Event zur Eventliste hinzu, falls ein
	 * Mansardendach konstruiert werden soll
	 * 
	 * @return True, falls ein SlopeEvent geadded wurde, False sonst
	 */
	private Boolean computeChangeSlopeEvents() {

		// wenn eine Hoehe angegeben wurde, fuege einen ChangeSlopeEvent hinzu
		final Integer changeSlopeHeight = mAlgorithm.getConf()
				.getSlopeChangeHeight();
		if (changeSlopeHeight != null) {
			mEvents.add(new ChangeSlopeEvent(mPolygon.getVertices().get(0),
					null, changeSlopeHeight));
			return true;
		} else {
			return false;
		}

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * @return
	 */
	public int getLevel() {
		return mLevel;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return
	 */
	public SkeletonPolygon getPolygon() {

		return mPolygon;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode liefert alle innerhalb dieses Jobs berechneten virtuellen Kanten
	 * als einfache Strahlen zurueck, damit diese gezeichnet werden koennen
	 * 
	 * @return
	 */
	public List<VirtualEdge> getVirtualEdges() {

		return mVirtualEdgeManager.getVirtualEdges();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * durchlaeuft alle Vertices und berechnet Schnittpunkte von je 2 adjazenten
	 * Ebenen mit allen anderen Ebenen
	 */
	private void computeIntersectionEvents() {

		SkeletonVertex currentVertex = null, currentNeighbourVertex = null, testVertex = null;
		Plane currentPlane = null, neighbourPlane = null, testPlane = null;
		Iterator<SkeletonVertex> innerVertIter = null;
		MyVector3f schnittpunkt = null;
		iStraightSkeletonEvent event = null;

		Iterator<SkeletonVertex> vertIter = mPolygon.getVertices().iterator();
		while (vertIter.hasNext()) {

			currentVertex = vertIter.next();
			currentNeighbourVertex = currentVertex.getNeighbourByIndex(0);
			currentPlane = currentVertex.getNeighbourEdgeByIndex(0).getPlane();
			neighbourPlane = currentNeighbourVertex.getNeighbourEdgeByIndex(0)
					.getPlane();

			innerVertIter = mPolygon.getVertices().iterator();
			while (innerVertIter.hasNext()) {
				testVertex = innerVertIter.next();

				// ueberspringe das aktuelle und das Neighbourvertex
				if (testVertex.equals(currentVertex)
						|| testVertex.equals(currentNeighbourVertex))
					continue;

				testPlane = testVertex.getNeighbourEdgeByIndex(0).getPlane();
				schnittpunkt = mMathHelper
						.calculatePlanePlanePlaneIntersection(currentPlane,
								neighbourPlane, testPlane);

				// assert mMathHelper.isPointOnRay(schnittpunkt,
				// currentNeighbourVertex.getRotatedWinkelhalbierende()) :
				// "Der berechnete Schnittpunkt liegt nicht auf der Winkelhalbierenden!";
				if (schnittpunkt != null) {

					mAlgorithm.addSingleSchnittpunkt(schnittpunkt);
					// System.out.println("Teste Schnittpunkt " + schnittpunkt +
					// " mit Testvertex: " + testVertex);
					// handelt es sich um einen gueltigen Schnittpunkt?

					if (!isValidIntersection(schnittpunkt, testVertex)) {
						// System.out.println("abgelehnt");
						continue;
					}

					// runde den Schnittpunkt
					schnittpunkt = mMathHelper.roundVector3f(schnittpunkt);
					schnittpunkt.normalizeRange();

					float distance = mMathHelper.calculatePointPlaneDistance(
							schnittpunkt, mPolygon.getPlane());
					distance = mMathHelper.round(distance);
					// System.out.println(distance);

					event = new IntersectionEvent(currentNeighbourVertex,
							schnittpunkt, distance, testVertex);

					mEvents.add(event);
				}
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Sortierroutine => sortiert den Event-Vektor basierend auf den
	 * Distanzwerten
	 */

	private void sortEventsByDistance() {
		Collections.sort(mEvents, new Comparator<iStraightSkeletonEvent>() {
			@Override
			public int compare(iStraightSkeletonEvent o1,
					iStraightSkeletonEvent o2) {
				return o1.getDistance().compareTo(o2.getDistance());
			}
		});
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode testet alle berechneten Events auf Gueltigkeit und entfernt
	 * anschliessend alle ungueltigen Events und alle Events, deren Distanz
	 * groesser als die minimale Distanz aller berechneten Event ist
	 */
	private void preprocessEventBuffer() {

		// nachdem alle Height-Events berechnet wurden, sortiere den
		// Event-Vektor
		sortEventsByDistance();

		// extrahiere alle gleichzeitig auftretenden Events
		float testDistance = Float.MAX_VALUE;

		// durchlaufe den eventBuffer und kopiere alle Elemente mit der gleichen
		// Distanz in den lokalen buffer
		IntersectionEvent currentIntersectionEvent = null;
		iStraightSkeletonEvent curEvent = null;

		final Iterator<iStraightSkeletonEvent> eventIter = mEvents.iterator();
		while (eventIter.hasNext()) {

			curEvent = eventIter.next();
			if (curEvent instanceof IntersectionEvent) {
				currentIntersectionEvent = (IntersectionEvent) curEvent;
			}

			if (currentIntersectionEvent != null
					&& !isValidEvent(currentIntersectionEvent)) {
				eventIter.remove();
				continue;
			}

			// loesche alle Events aus dem Buffer, deren Distanz groesser als
			// die Testdistanz ist
			if (curEvent.getDistance() > testDistance) {
				if (!mMathHelper.isWithinTolerance(curEvent.getDistance(),
						testDistance, mAccetableDistanceDelta)) {
					eventIter.remove();
				}
			}

			// aktualisiere Testdistanz nach erstem Durchlauf
			else {
				testDistance = curEvent.getDistance();
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet fuer Events, deren Schnittpunkte nahe beieinander
	 * liegen, einen gemittelten Schnittpunkt, der dann bei allen Events
	 * innerhalb des Ranges gesetzt wird
	 */
	private void adjustEvents() {

		final List<iStraightSkeletonEvent> workingBuffer = new ArrayList<iStraightSkeletonEvent>(
				mEvents);
		final List<iStraightSkeletonEvent> adjustBuffer = new ArrayList<iStraightSkeletonEvent>(
				mEvents.size());
		MyVector3f currentCenter = null;
		IntersectionEvent currentEvent = null;

		while (workingBuffer.size() > 0) {
			currentCenter = computeAdjustBuffer(workingBuffer, adjustBuffer);
			for (int i = 0; i < adjustBuffer.size(); i++) {
				currentEvent = (IntersectionEvent) adjustBuffer.get(i);
				if (currentEvent.getSchnittpunktPtr()
						.equalsComponentByComponent(currentCenter)) {
					continue;
				}
				String message = "Schnittpunkt "
						+ currentEvent.getSchnittpunktPtr()
						+ " wurde modifiziert zu: " + currentCenter;
				addToMessageBuffer(message);
				LOGGER.debug(message);

				currentEvent.setSchnittpunkt(currentCenter);
			}
			adjustBuffer.clear();
		}

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode bestimmt gemittelte Schnittpunkte fuer Events, deren
	 * Schnittpunkte sehr dicht beieinander liegen. Dadurch sollen gemeinsame
	 * Split-Events etc. besser erkannt werden. Durch die Mittelung soll ein
	 * geringerer Fehler durch die Iterationen hinweg entstehen.
	 * 
	 * @param workingBuffer
	 *            Buffer mit allen noch nicht bearbeiteten Events
	 * @param adjustBuffer
	 *            Buffer, in dem alle Events abgelegt werden, fuer die ein
	 *            gemeinsamer Schnittpunkt gesetzt wird
	 * @return Gemittelter Schnittpunkt aus allen Events, die sich innerhalb des
	 *         festgelegten Toleranzbereichs befinden
	 */
	private MyVector3f computeAdjustBuffer(
			final List<iStraightSkeletonEvent> workingBuffer,
			final List<iStraightSkeletonEvent> adjustBuffer) {

		Float acceptableDistance = 0.25f, currentDistance = 0.0f;
		IntersectionEvent currentIntersectionEvent = null;
		iStraightSkeletonEvent currentEvent = null;
		MyVector3f currentCenter = null;

		final Iterator<iStraightSkeletonEvent> eventIter = workingBuffer
				.iterator();
		// durchlaufe alle Events
		while (eventIter.hasNext()) {
			currentEvent = eventIter.next();

			// beruecksichtige nur IntersectionEvents
			if (currentEvent instanceof IntersectionEvent) {
				currentIntersectionEvent = (IntersectionEvent) currentEvent;
			} else {
				eventIter.remove();
				continue;
			}

			// noch kein Mittelpunkt gesetzt, Mittelpunkt ist Schnittpunkt des
			// ersten Events
			if (currentCenter == null) {
				currentCenter = currentIntersectionEvent.getSchnittpunkt();
				eventIter.remove();
				adjustBuffer.add(currentIntersectionEvent);
			} else {
				// sonst Abstand bestimmen, wenn Abstand innerhalb
				// Toleranzbereich, Mittelpunkt aktualisieren, Event in
				// Adjust-Buffer, aus Working-Buffer entfernen
				currentDistance = mMathHelper.calculatePointPointDistance(
						currentIntersectionEvent.getSchnittpunkt(),
						currentCenter);
				if (currentDistance < acceptableDistance) {
					currentCenter.add(currentIntersectionEvent
							.getSchnittpunkt());
					currentCenter.scale(0.5f);
					adjustBuffer.add(currentIntersectionEvent);
					eventIter.remove();
				}
			}
		}
		// Mittelpunkt zurueck
		return currentCenter;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Die Methode versucht ueber einen heuristischen Ansatz, Split-Events aus
	 * der Menge der berechneten Height-Events zu bestimmen. Hierbei gelten
	 * verschiedene Regeln fuer das Auftreten von Split-Events. Split-Events
	 * koennen nur in Reflex-Vertices auftreten, somit betrachtet man nur solche
	 * Events, die durch Reflex-Vertices ausgeloest wurden. Weiterhin gilt, dass
	 * Height-Events, bei denen die beteiligten Vertices nicht direkte Nachbarn
	 * sind, ebenfalls Split-Events sind (Edge-Events sind nur moeglich, wenn
	 * die am Schnitt-Test beteiligten Ebenen alle zu adjazenten Kanten
	 * gehoeren). Das letzte Kriterium ist fuzzy, unter Beruecksichtigung der
	 * vorherigen Bedigungen aber WAHRSCHEINLICH hinreichend, ein Split-Event
	 * liegt bei den verbleibenden Events genau dann vor, wenn der Schnittpunkt
	 * nur einmal vorkommt (basiert auf der Annahme, dass Edge-Events immer von
	 * zwei benachbarten Vertices im gleichen Schnittpunkt ausgeloest werden).
	 * Kommen dagegen zwei Height-Events mit gleichem Schnittpunkt vor, hat man
	 * es mit einem Edge-Event zu tun.
	 * 
	 * @throws SquareCaseException
	 */
	private void detectSplitEvents() {

		IntersectionEvent currentIntersectionEvent = null;
		iStraightSkeletonEvent currentEvent = null;

		List<iStraightSkeletonEvent> reflexVertexEventBuffer = new ArrayList<iStraightSkeletonEvent>();

		// durchlaufe alle Vertices und kopiere alle Reflex-Vertices in einen
		// zweiten Buffer
		Iterator<iStraightSkeletonEvent> eventIter = mEvents.iterator();
		while (eventIter.hasNext()) {
			currentEvent = eventIter.next();
			if (currentEvent instanceof IntersectionEvent) {
				currentIntersectionEvent = (IntersectionEvent) currentEvent;
			} else {
				continue;
			}

			if (currentIntersectionEvent.getVertex().isReflexVertex()) {
				// verschiebe das Vertex in den ZweitBuffer
				reflexVertexEventBuffer.add(currentIntersectionEvent);
				eventIter.remove();
			}
		}

		// falls kein Reflex-Vertex kopiert wurde, gibt es auch keine
		// SplitEvents
		if (reflexVertexEventBuffer.size() == 0)
			return;

		// nimmt Split-Events auf, falls solche gefunden werden
		List<SplitEvent> splitEventBuffer = new ArrayList<SplitEvent>();

		IntersectionEvent currentReflexEvent = null;

		// teste nun die Nachbarschaftsbeziehungen => wenn das Second-Vertex im
		// Event kein direkter Nachbar der am Event beteiligten Vertices ist,
		// handelt es sich um einen Split-Event
		Iterator<iStraightSkeletonEvent> reflexEventIter = reflexVertexEventBuffer
				.iterator();

		while (reflexEventIter.hasNext()) {
			currentReflexEvent = (IntersectionEvent) reflexEventIter.next();

			// wenn alle beteiligten Vertices des Events direkte Nachbarn sind,
			// ist ein Edge-Event moeglich
			if (checkDirectNeighbours(currentReflexEvent))
				continue;

			// sonst handelt es sich um einen Split-Event
			else {
				// erzeuge einen Split-Event
				SplitEvent split = new SplitEvent(
						currentReflexEvent.getVertex(),
						currentReflexEvent.getSchnittpunkt(),
						currentReflexEvent.getDistance(),
						currentReflexEvent.getSecondVertex());
				splitEventBuffer.add(split);

				// loesche das Quell-Event aus dem reflexEventBuffer
				reflexEventIter.remove();
				if (mSplitEventOccured) {
					mVertexEventPossible = true;
				} else
					mSplitEventOccured = true;
			}
		}

		// teste nun alle verbleibenden Events daraufhin, ob es sich um Edge
		// oder um Split-Events handelt
		// das Kriterium ist dann, ob der Schnittpunkt der Events mehrfach
		// vorkommt. Wenn dies der Fall ist,
		// handelt es sich mit hoher Wahrscheinlichkeit um einen Edge-Event.

		// ANNAHME: Unter der Voruassetzung, dass im ersten Schritt bereits alle
		// Split-Events gefunden werden, die durch
		// die direkten Nachbarn identifiziert werden koennen, liegen nun nur
		// noch Events im Buffer, bei denen saemtliche
		// Vertices beteiligte Nachbarn sind. Die Wahrscheinlichkeit, dass 3
		// Events dieser Art den gleichen Schnittpunkt
		// teilen, ist dabei sehr gering, gehe vereinfachend davon aus, dass
		// maximal zwei Events im Buffer den gleichen
		// Schnittpunkt besitzen

		MyVector3f currentSchnittpunkt = null;
		boolean foundSchnittpunkt = false;
		IntersectionEvent currentInlineEvent = null;

		// System.out.println("Reflex-Event-Buffer vor zweiter Phase: ");
		// System.out.println(reflexVertexEventBuffer);

		Iterator<iStraightSkeletonEvent> mainEventIter = null;

		reflexEventIter = reflexVertexEventBuffer.iterator();
		while (reflexEventIter.hasNext()) {
			currentIntersectionEvent = (IntersectionEvent) reflexEventIter
					.next();
			currentSchnittpunkt = currentIntersectionEvent.getSchnittpunkt();
			foundSchnittpunkt = false;

			// durchlaufe alle Events im Reflex-Vertex-Buffer, pruefe, ob ein
			// weiterer Event diesen Schnittpunkt besitzt
			for (int i = 0; i < reflexVertexEventBuffer.size(); i++) {
				currentInlineEvent = (IntersectionEvent) reflexVertexEventBuffer
						.get(i);
				if (currentInlineEvent.equals(currentIntersectionEvent))
					continue;

				// wenn die Schnittpunkte uebereinstimmen, setze das Flag
				if (currentInlineEvent.getSchnittpunkt().equalsWithinTolerance(
						currentSchnittpunkt)) {
					// System.out.println(currentSchnittpunkt);
					foundSchnittpunkt = true;
				}

			}
			// hier muessen auch alle Events beruecksichtigt werden, die nicht
			// im Reflex-Vertex-Buffer sind
			// Edge-Events koennen durch Reflex-Vertices auch mit
			// Nicht-Reflex-Vertices entstehen
			if (!foundSchnittpunkt) {
				mainEventIter = mEvents.iterator();
				while (mainEventIter.hasNext()) {
					currentInlineEvent = (IntersectionEvent) mainEventIter
							.next();

					// wenn die Schnittpunkte uebereinstimmen, setze das Flag
					if (currentInlineEvent.getSchnittpunkt()
							.equalsWithinTolerance(currentSchnittpunkt)) {
						// System.out.println(currentSchnittpunkt);
						foundSchnittpunkt = true;
					}
				}

			}

			// wenn kein Event mit gleichem Schnittpunkt gefunden wurde, handelt
			// es sich hoechst wahrscheinlich um einen Split-Event
			// erzeuge eine Instanz
			if (!foundSchnittpunkt) {
				// erzeuge einen Split-Event
				SplitEvent split = new SplitEvent(
						currentIntersectionEvent.getVertex(),
						currentIntersectionEvent.getSchnittpunkt(),
						currentIntersectionEvent.getDistance(),
						currentIntersectionEvent.getSecondVertex());
				// System.out.println("Split-Event 2.Phase hinzugefuegt: " +
				// split);

				splitEventBuffer.add(split);

				// loesche das Quell-Event aus dem reflexEventBuffer
				reflexEventIter.remove();
				if (mSplitEventOccured) {
					mVertexEventPossible = true;
				} else
					mSplitEventOccured = true;
			}

		}

		// fuege die Split-Events zum regulaeren Buffer hinzu
		mEvents.addAll(splitEventBuffer);

		// und die Reflex-Vertex-Events, die keine Split-Events waren
		mEvents.addAll(reflexVertexEventBuffer);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode sucht nach dem seltenen Sonderfall, bei dem ein Vertex 2
	 * Split-Events im selben Punkt ausloest. Dies ist genau dann der Fall, wenn
	 * das Reflex-Vertex auf einen Eckpunkt des Polygons trifft und somit dessen
	 * adjazente Kanten splittet. In diesem Fall wird eine Exception ausgeloest
	 * und die Verarbeitung wird abgebrochen
	 * 
	 * @return True, falls irregulaere Events gefunden wurden, False sonst
	 */
	private boolean findIrregularSplitEvents() {

		Vector<SplitEvent> splitEventBuffer = new Vector<SplitEvent>();
		Iterator<iStraightSkeletonEvent> eventIterSSE = mEvents.iterator();
		iStraightSkeletonEvent current = null;
		while (eventIterSSE.hasNext()) {
			current = eventIterSSE.next();
			if (current.getType().equals("SplitEvent"))
				splitEventBuffer.add((SplitEvent) current);
		}

		if (splitEventBuffer.size() > 1) {
			Iterator<SplitEvent> eventIter = splitEventBuffer.iterator();
			SplitEvent currentEvent = null;
			while (eventIter.hasNext()) {
				currentEvent = eventIter.next();
				if (isIrregularSplitEvent(currentEvent, splitEventBuffer)) {
					LOGGER.warn("Ungueltiger Split-Event: " + currentEvent);
					return true;
				}
			}

		}
		return false;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode durchlaeuft fuer einen uebergebenen Split-Event alle anderen
	 * Split-Events und sucht nach Events mit gleichem Vertex und gleichem
	 * Schnittpunkt
	 * 
	 * @param current
	 *            Split-Event, fuer den nach irregulaeren Events gesucht wird
	 * @param splitEvent
	 *            Vector mit allen berechneten Split-Events
	 * @return True, falls ein Split-Event gefunden wurde, bei dem ausloesendes
	 *         Vertex und Schnittpunkt mit dem Eingabeevent uebereinstimmen,
	 *         False sonst
	 */
	private boolean isIrregularSplitEvent(SplitEvent current,
			Vector<SplitEvent> splitEvents) {

		SkeletonVertex eventVertex = current.getVertex();
		MyVector3f intersection = current.getSchnittpunkt();
		SplitEvent currentEvent = null;

		Iterator<SplitEvent> eventIter = splitEvents.iterator();
		while (eventIter.hasNext()) {
			currentEvent = eventIter.next();
			if (currentEvent.equals(current))
				continue;
			if (intersection.equals(currentEvent.getSchnittpunkt())
					&& eventVertex.equals(currentEvent.getVertex()))
				return true;
		}
		return false;

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode testet Events daraufhin, ob es sich um Edge-Events handeln kann.
	 * Edge-Events treten nur zwischen 3 adjazenten Planes auf. Wenn also das
	 * gespeicherte Nachbarvertex im Event nicht einem der direkten Nachbarn der
	 * beteiligten Vertices entspricht so kann es sich nicht um einen Edge-Event
	 * handeln, stattdessen muss ein Split-Event vorliegen.
	 * 
	 * @return True Es handelt sich um einen potentiellen Edge-Event, da die
	 *         Vertices alle direkte Nachbarn sind, False sonst
	 * 
	 */
	private boolean checkDirectNeighbours(IntersectionEvent currentEvent) {

		// hole saemtliche am Event potentiell beteiligte Vertices
		SkeletonVertex eventVertex = currentEvent.getVertex();

		// nur, wenn das Nachbarvertex einem der ermittelten Vertices
		// entspricht, kann es sich um einen Edge-Event handeln,
		// da Edge-Events immer nur dann auftreten, wenn direkte Nachbarn
		// beteiligt sind
		SkeletonVertex eventNeighbour = currentEvent.getSecondVertex();

		// direkte Nachbarn des EventVertex
		SkeletonVertex eventVertexNeighbour0 = eventVertex
				.getNeighbourByIndex(0);
		SkeletonVertex eventVertexNeighbour1 = eventVertex
				.getNeighbourByIndex(1);

		// Vorgaenger des Nachbarn mit Index 1 => nur hier muss der direkte
		// Vorgaenger geholt werden, da
		// beim Nachbar mit Index 0 dieser bereits der Ausgangspunkt der
		// naechsten Kante ist
		SkeletonVertex neighbour1OfNeighbour1 = eventVertexNeighbour1
				.getNeighbourByIndex(1);

		if (eventNeighbour.equals(eventVertexNeighbour0)
				|| eventNeighbour.equals(neighbour1OfNeighbour1)) {
			return true;
		}

		return false;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode dient der Erkennung von Vertex-Events basierend auf der Analyse
	 * aufgetretener Split-Events. Dabei wird genau dann ein Vertex-Event
	 * erzeugt, wenn zwei Split-Events im gleichen Schnittpunkt auftreten. In
	 * diesem Fall werden die gefundenen Split-Events aus dem Event-Buffer
	 * geloescht und durch den neuen Vertex-Event ersetzt. Zwei Split-Events
	 * werden genau dann zusammengefasst, wenn ihre Schnittpunkte gleich sind,
	 * beide Events noch nicht zu anderen Vertex-Events hinzugefuegt wurden und
	 * es sich nicht um das gleiche Event-Vertex handelt.
	 */
	private void detectVertexEvents() {

		iStraightSkeletonEvent currentEvent = null;

		// verschiebe alle Split-Events in einen zusaetzlichen Buffer
		List<SplitEvent> splitEventBuffer = new ArrayList<SplitEvent>();

		Iterator<iStraightSkeletonEvent> eventIter = mEvents.iterator();
		while (eventIter.hasNext()) {
			currentEvent = eventIter.next();
			if (currentEvent.getType() == "SplitEvent") {
				splitEventBuffer.add((SplitEvent) currentEvent);
				eventIter.remove();
			}
		}

		// damit es ein Vertex-Event geben kann, muessen mindestens 2
		// Split-Events aufgetreten sein
		if (splitEventBuffer.size() < 2)
			return;

		List<VertexEvent> vertexEventBuffer = new ArrayList<VertexEvent>();

		SplitEvent currentSplit = null, testSplit = null;
		MyVector3f schnittpunkt = null;

		VertexEvent resultEvent = null;
		SkeletonVertex currentVertex = null;

		// durchlaufe alle Split-Events im Buffer
		Iterator<SplitEvent> splitEventIter = splitEventBuffer.iterator();
		while (splitEventIter.hasNext()) {
			currentSplit = splitEventIter.next();
			schnittpunkt = currentSplit.getSchnittpunkt();

			// wenn der Event bereits verarbeitet wurde, fahre fort
			if (currentSplit.isToDelete())
				continue;
			currentVertex = currentSplit.getVertex();

			// durchlaufe alle anderen Events
			for (int i = 0; i < splitEventBuffer.size(); i++) {
				testSplit = splitEventBuffer.get(i);

				// teste auf Verarbeitung
				// gleicher Event
				if (testSplit.equals(currentSplit))
					continue;

				// gleiches Event-Vertex
				if (testSplit.getVertex().equals(currentVertex))
					continue;

				// wurde bereits verarbeitet
				if (testSplit.isToDelete())
					continue;

				// Uebereinstimmung der Schnittpunkte
				if (!testSplit.getSchnittpunkt().equalsWithinTolerance(
						currentSplit.getSchnittpunkt()))
					continue;

				// hier hat man 2 Split-Events mit gleichem Schnittpunkt, aber
				// unterschiedlichen Event-Vertices, die beide noch nicht
				// verarbeitet wurden
				// erzeuge einen Vertex-Event

				resultEvent = new VertexEvent(currentSplit.getVertex(),
						schnittpunkt, currentSplit.getDistance(),
						currentSplit.getSecondVertex());

				// fuege sowohl ein zweites Event-Vertex als auch ein zweites
				// Testplanevertex hinzu
				resultEvent.addEventVertex(testSplit.getVertex());
				resultEvent.addTestPlaneVertex(testSplit.getSecondVertex());
				vertexEventBuffer.add(resultEvent);

				// markiere beide als verarbeitet
				currentSplit.setToDelete(true);
				testSplit.setToDelete(true);

				// globales Flag fuer Weiterverarbeitung setzen
				mVertexEventOccured = true;

				break;
			}

		}

		// loesche alle als loeschbar markierten Split-Events
		splitEventIter = splitEventBuffer.iterator();
		while (splitEventIter.hasNext()) {
			currentSplit = splitEventIter.next();
			if (currentSplit.isToDelete())
				splitEventIter.remove();
		}

		// verschiebe die verbliebenen Split-Events und die erzeugten
		// Vertex-Events in den globalen Event-Buffer
		mEvents.addAll(splitEventBuffer);
		mEvents.addAll(vertexEventBuffer);

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode prueft fuer den uebergebenen Schnittpunkt und das uebergebene
	 * Testbvertex, ob der Schnittpunkt sich innerhalb des Bereiches befindet,
	 * den die Kante mit Index 0 ausgehend vom Testvertex waehrend des
	 * Schrumpfungsprozesses einnimmt. Hierfür berechnet man Referenzpunkte, die
	 * definitiv ausserhalb dieses Bereiches liegen und prueft, ob sie sich
	 * bzgl. der Winkelhalbierenden an den Endvertices der Kante auf der
	 * gleichen Seite wie der Schnittpunkt befinden, oder nicht.
	 * 
	 * @param schnittpunkt
	 *            Schnittpunkt der Plane-Intersection-Tests, der auf Gueltigkeit
	 *            geprueft werden soll
	 * @param vertex
	 *            Testvertex, von dem ausgehend die 3. Ebene fuer die
	 *            Intersection-Tests definiert ist
	 * @return True, wenn sich der Schnittpunkt innerhalb des Bereichs befindet,
	 *         den die Kante 0 vom Testvertex waehrend des Schrumpfvorgangs
	 *         einnimmt, False sonst
	 */

	private boolean isValidIntersection(final MyVector3f schnittpunkt,
			final SkeletonVertex vertex) {

		// hole alle relevanten Datenstrukturen
		final SkeletonEdge edge = vertex.getNeighbourEdgeByIndex(0);
		final SkeletonVertex neighbourVertex = vertex.getNeighbourByIndex(0);
		final Ray winkelhalbierendeVertex = vertex
				.getRotatedWinkelhalbierende();
		final Ray winkelhalbierendeNeighbour = neighbourVertex
				.getRotatedWinkelhalbierende();

		// System.out.println("Teste Gueltigkeit von Schnittpunkt: " +
		// schnittpunkt + " Testvertex: " + vertex + " Ednvertex: " +
		// neighbourVertex);

		// teste zunaechst, ob sich der Punkt ueber der Flaeche befindet, also
		// einen hoeheren z-Wert besitzt
		// da alle Flaechen planar sind, kann man die Normale des uebergebenen
		// Vertex verwenden
		MyVector3f vertexNormal = vertex.getNormal();

		// berechnen einen Referenzpunkt in Richtung der Vertex-Normalen
		MyVector3f referencePoint = new MyVector3f();
		referencePoint.add(vertex.getPosition(), vertexNormal);

		// der Referenzpunkt und der Schnittpunkt muessen auf der gleichen Seite
		// liegen
		if (!mMathHelper.isPointOnRay(schnittpunkt, edge)) {
			if (!mMathHelper
					.isSameSideOfRay(edge, referencePoint, schnittpunkt)) {
				LOGGER.trace("Abgelehnt: Schnittpunkt befindet sich unterhalb der Kante");
				return false;

			}

		}

		// berechne einen Referenzpunkt am Startvertex der Kante
		// verwende die umgekehrte Richtung der Kante und addiere diese auf das
		// Startvertex
		MyVector3f edgeDirection = edge.getDirection();
		// edgeDirection.normalize();
		// invertieren
		edgeDirection.scale(-1.0f);

		// Referenzpunkt berechnen
		referencePoint = new MyVector3f();
		referencePoint.add(vertex.getPosition(), edgeDirection);

		// Schnittpunkt und Referenzpunkt muessen sich auf unterschiedlichen
		// Seiten befinden
		if (!mMathHelper.isPointOnRay(schnittpunkt, winkelhalbierendeVertex)) {
			if (mMathHelper.isSameSideOfRay(winkelhalbierendeVertex,
					referencePoint, schnittpunkt)) {
				LOGGER.trace("Abgelehnt: Punkt befindet sich auf der gleichen Seite wie der Referenzpunkt "
						+ referencePoint
						+ " bzgl. der Start-Winkelhalbierenden");
				return false;

			}

		}

		// teste den gleichen Referenzpunkt auch gegen den Strahl des Endvertex
		// hier muessen Schnittpunkt und Referenzpunkt auf der gleichen Seite
		// liegen
		if (!mMathHelper.isPointOnRay(schnittpunkt, winkelhalbierendeNeighbour)) {
			if (!mMathHelper.isSameSideOfRay(winkelhalbierendeNeighbour,
					referencePoint, schnittpunkt)) {
				LOGGER.trace("Abgelehnt: Punkt befindet sich nicht auf der gleichen Seite wie der Referenzpunkt "
						+ referencePoint + " bzgl. der End-Winkelhalbierenden");
				return false;

			}
		}

		// berechne nun einen neuen Referenzpunkt fuer das Endvertex der Kante
		// addiere die Richtung der Kante auf das Endvertex
		referencePoint = new MyVector3f();
		edgeDirection = edge.getDirection();
		// edgeDirection.normalize();
		referencePoint.add(edgeDirection, neighbourVertex.getPosition());

		// Referenz- und Schnittpunkt duerfen bzgl. der Winkelhalbierenden des
		// Endvertex nicht auf der gleichen Seite liegen
		if (!mMathHelper.isPointOnRay(schnittpunkt, winkelhalbierendeNeighbour)) {
			if (mMathHelper.isSameSideOfRay(winkelhalbierendeNeighbour,
					referencePoint, schnittpunkt)) {
				LOGGER.trace("Abgelehnt:  Punkt befindet sich auf der gleichen Seite wie der Referenzpunkt "
						+ referencePoint + " bzgl. der End-Winkelhalbierenden");
				return false;

			}
		}

		// teste den gleichen Referenzpunkt auch noch mit der Winkelhalbierenden
		// am Startvertex
		// hier muessen beide Punkte auf der gleichen Seite liegen
		if (!mMathHelper.isPointOnRay(schnittpunkt, winkelhalbierendeVertex)) {
			if (!mMathHelper.isSameSideOfRay(winkelhalbierendeVertex,
					referencePoint, schnittpunkt)) {
				LOGGER.trace("Abgelehnt:  Punkt befindet sich nicht auf der gleichen Seite wie der Referenzpunkt "
						+ referencePoint
						+ " bzgl. der Start-Winkelhalbierenden");
				return false;

			}
		}

		// der Schnittpunkt hat alle Tests bestanden, es handelt sich um einen
		// gueltigen Punkt
		return true;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode testet, ob der berechnete Event gueltig ist => ein Event ist
	 * gueltig, wenn die beteiligten Vertices alle direkte Nachbarn sind
	 * (EdgeEvent) oder das ausloesende Vertex ein Reflex-Vertex ist
	 * (SplitEvent)
	 * 
	 * @param event
	 *            Event, dessen Gueltigkeit getestet wird
	 * @return True, falls es sich um einen gueltigen Event handelt, False sonst
	 */
	private boolean isValidEvent(IntersectionEvent event) {

		SkeletonVertex eventVertex = null, neighbourEventVertex = null, secondEventVertex = null;

		// teste auf EdgeEvent
		eventVertex = event.getVertex();

		// wenn das EventVertex ein Reflex-Vertex ist, dann sind alle Tests
		// gueltig
		if (eventVertex.isReflexVertex())
			return true;

		secondEventVertex = event.getSecondVertex();
		neighbourEventVertex = eventVertex.getNeighbourByIndex(0);

		// wenn das NeighbourVertex dem zweiten Vertex im Event entspricht, dann
		// handelt es sich ebenfalls um ein gueltiges Event
		if (neighbourEventVertex.equals(secondEventVertex)) {
			return true;
		}

		// teste auch, ob der Event durch direkte Nachbarschaft auf der 1.
		// Indexposition ausgeloest wird
		neighbourEventVertex = eventVertex.getNeighbourByIndex(1)
				.getNeighbourByIndex(1);
		if (neighbourEventVertex.equals(secondEventVertex)) {
			return true;
		}

		// wenn keiner der vorherigen Faelle eingetreten ist, so ist das Event
		// ungueltig
		return false;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * verarbeitet alle auftretenden Events, erzeugt Vertex-Kinder und
	 * aktualisiert die Graphbeziehungen
	 */
	private void processEvents() {

		iStraightSkeletonEvent currentEvent = null;
		SkeletonVertex childVertex = null, currentVertex = null;
		MyVector3f schnittpunkt = null;
		int index = -1;
		Iterator<iStraightSkeletonEvent> eventIter = mEvents.iterator();
		String message;

		while (eventIter.hasNext()) {
			currentEvent = eventIter.next();

			if (currentEvent.getType().equals("IntersectionEvent")) {
				currentVertex = currentEvent.getVertex();
				schnittpunkt = currentEvent.getSchnittpunkt();
				childVertex = new SkeletonVertex(schnittpunkt);

				// teste, ob das Vertex bereits im Child-Buffer liegt
				index = mChildBuffer.indexOf(childVertex);
				if (index != -1) {
					childVertex = mChildBuffer.get(index);
				} else
					mChildBuffer.add(childVertex);

				currentVertex.setChild(childVertex);
				currentVertex.setProcessed(true);
				childVertex.setParent(currentVertex);
				currentVertex.setSkeletonNode(true);
				childVertex.setSkeletonNode(true);

				message = "Erzeuge Kind fuer IntersectionEvent Vertex: "
						+ currentVertex;
				message += " Child: " + childVertex;
				LOGGER.debug(message);

			}
			// Verarbeitungslogik fuer Split-Events
			else if (currentEvent.getType().equals("SplitEvent")) {
				createChildrenForSplitEvent((SplitEvent) currentEvent);
			} else if (currentEvent.getType().equals("VertexEvent")) {
				createChildrenForVertexEvent((VertexEvent) currentEvent);
			} else if (currentEvent.getType().equals("ChangeSlopeEvent")) {
				this.mProcessedSlopeEvent = true;
				mAlgorithm.setProcessedSlopeEvent(true);
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet die Positionen der Vertexkinder auf der naechsten
	 * Iterationsebene. Dafuer berechnet man eine Ebene, die parallel zur
	 * aktuellen Ebene auf der Zielhoehe liegt. Anschliessend berechnet man fuer
	 * jedes Vertex den Schnittpunkt seiner rotierten Winkelhalbierenden mit
	 * dieser Ebene. Dieser Ansatz ist deutlich robuster und effizienter, als
	 * das wiederholte Berechnen von Geradenschnittpunkten, das dem
	 * urspruenglichen Shrinking-Ansatz zugrunde lag.
	 */
	private void shrink() {

		float targetHeight = mEvents.get(0).getDistance();
		Plane initialPlane = mPolygon.getPlane();

		// verwende bei allen Extrusionen immer die gleiche Richtung (berechnet
		// aus dem initialen Eingabepolygon)
		final MyVector3f planeNormal = mBuildDirection.clone();
		planeNormal.scale(targetHeight);

		final MyVector3f stuetzvektorTarget = initialPlane.getStuetzvektor();
		stuetzvektorTarget.add(planeNormal);

		// Ebene auf Zielhoehe erzeugen
		final Plane targetPlane = new Plane(mBuildDirection.clone(),
				stuetzvektorTarget);

		List<SkeletonVertex> verts = mPolygon.getVertices();
		SkeletonVertex current = null, childCurrentVertex = null;
		Ray rotatedWinkelhalbierende = null;
		MyVector3f intersection = null;
		boolean isProcessedVertex = false;

		SkeletonEdge currentEdge = null;
		Plane currentPlane = null;

		for (int i = 0; i < verts.size(); i++) {
			current = verts.get(i);

			// bereits verarbeitete Vertices besitzen bereits Kinder =>
			// verschiebe aber auch deren
			// Punkte auf die Zielhoehe mittels gleicher Technik, um eine
			// einheitliche Grundebene zu garantieren
			if (current.isProcessed())
				isProcessedVertex = true;
			rotatedWinkelhalbierende = current.getRotatedWinkelhalbierende();

			// Schnittpunkt der rotierten Winkelhalbierenden mit Ebene auf
			// Zielhoehe
			intersection = mMathHelper.calculateRayPlaneIntersection(
					rotatedWinkelhalbierende, targetPlane);
			assert intersection != null : "FEHLER: Kein Schnittpunkt gefunden!";
			intersection = mMathHelper.roundVector3f(intersection);

			currentEdge = current.getNeighbourEdgeByIndex(0);
			assert currentEdge != null : "FEHLER: Keine Nachbarkante auf Index 0 fuer Vertex: "
					+ current;
			currentPlane = currentEdge.getPlane();
			if (!mMathHelper.isPointOnPlane(intersection, currentPlane))
				LOGGER.warn("FEHLER: Der berechnete Schnittpunkt "
						+ intersection + " liegt nicht auf der Ebene "
						+ currentPlane + " der Einagbekante!");

			currentEdge = current.getNeighbourByIndex(1)
					.getNeighbourEdgeByIndex(0);
			assert currentEdge != null : "FEHLER: Keine Nachbarkante auf Index 1 fuer Vertex: "
					+ current;
			currentPlane = currentEdge.getPlane();
			if (!mMathHelper.isPointOnPlane(intersection, currentPlane))
				LOGGER.warn("FEHLER: Der berechnete Schnittpunkt "
						+ intersection + " liegt nicht auf der Ebene "
						+ currentPlane + " der Einagbekante!");

			LOGGER.debug("Shrinke: Source: " + current + " Dest: "
					+ intersection);

			// wenn das Vertex bereits verarbeitet wurde, aktualisiere nur die
			// Koordinaten
			// der Kinder und etwaiger Zwillinge
			if (isProcessedVertex) {
				SkeletonVertex vertChild = current.getChild();
				vertChild.setPosition(intersection.clone());
				if (vertChild.hasTwinVertex())
					vertChild.getTwinVertex().setPosition(intersection.clone());
				isProcessedVertex = false;
			} else {
				// teste, ob sich das Vertex bereits im Childbuffer befindet
				childCurrentVertex = new SkeletonVertex(intersection);
				int index = mChildBuffer.indexOf(childCurrentVertex);
				if (index != -1) {
					childCurrentVertex = mChildBuffer.get(index);
				} else
					mChildBuffer.add(childCurrentVertex);
				current.setChild(childCurrentVertex);
				childCurrentVertex.setParent(current);
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erzeugt Kinder fuer SplitEvents, je ein "Original"-Kind und ein
	 * Zwilling
	 * 
	 * @param event
	 *            Split-Event fuer das die Kinder erzeugt werden
	 */
	private void createChildrenForSplitEvent(SplitEvent event) {

		SkeletonVertex child = null, twin = null, currentVertex = null;

		currentVertex = event.getVertex();

		// erzeuge zwei Kinder
		child = new SkeletonVertex(event.getSchnittpunkt());
		twin = new SkeletonVertex(event.getSchnittpunkt());

		// fuege Child und Twin zum Buffer hinzu, nur in seltenen
		// Ausnahmefaellen ("Quadrat-Problem"), bei denen
		// ein Vertex mehrere Split-Events im gleichen Punkt ausloest, werden
		// Vertices doppelt angelegt
		mChildBuffer.add(child);
		mChildBuffer.add(twin);

		child.setParent(currentVertex);
		twin.setParent(currentVertex);

		// speichere den Kind-Pointer nur auf das "Original", nicht auf den
		// Zwilling
		currentVertex.setChild(child);

		// speichere die Verwandtschaftsverhaeltnisse
		child.setTwinVertex(twin);
		twin.setTwinVertex(child);

		currentVertex.setProcessed(true);
		currentVertex.setSkeletonNode(true);
		child.setSkeletonNode(true);
		twin.setSkeletonNode(true);
		twin.setIsTwinVertex(true);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erzeugt Kinder fuer VertexEvents, jedes Parent-Vertex bekommt ein
	 * eigenes Kind
	 * 
	 * @param event
	 *            VertexEvent, fuer das die Kinder angelegt werden
	 * 
	 */
	private void createChildrenForVertexEvent(VertexEvent event) {

		// hole saemtliche Elternknoten
		List<SkeletonVertex> parents = event.getEventVertices();

		assert parents.size() == 2 : "Das Vertex-Event speichert "
				+ parents.size()
				+ " Parents, dieser Fall wird nicht verarbeitet";

		SkeletonVertex firstParent = parents.get(0);
		SkeletonVertex secondParent = parents.get(1);

		// wenn beide Eltern bereits Kinder haben, handelt es sich um den
		// Sonderfall, bei dem mehrere Vertex-Events in den gleichen Knoten
		// auftreten
		// in diesem Fall wird der akteulle Event nicht verarbeitet

		if (firstParent.hasChild() && secondParent.hasChild()) {
			// setze das Process-Flag, damit keine Nachbarschaftsupdates fuer
			// das Event durchgefuehrt werden
			LOGGER.info("Beide Event-Vertices besitzen bereits Kinder, Verarbeitung abgebrochen...");
			event.setToProcess(false);
			return;
		}

		MyVector3f schnittpunkt = event.getSchnittpunkt();

		SkeletonVertex child = new SkeletonVertex(schnittpunkt);
		SkeletonVertex twin = new SkeletonVertex(schnittpunkt);

		// aufgrund der verschiedenen Sonderfaelle bei Vertex-Events (mehrere
		// Vertex-Events im gleichen Punkt etc.) wird hier nicht geprueft,
		// ob bereits ein Vertex im Buffer vorhanden ist, sondern Original und
		// Zwilling werden beide hinzugefuegt
		mChildBuffer.add(child);
		mChildBuffer.add(twin);

		// setze alle Flags und erzeuge die Kanten zwischen den Vertices im
		// Graphen
		child.setTwinVertex(twin);
		twin.setTwinVertex(child);
		twin.setIsTwinVertex(true);
		child.setSkeletonNode(true);
		twin.setSkeletonNode(true);

		firstParent.setProcessed(true);
		firstParent.setSkeletonNode(true);
		secondParent.setProcessed(true);
		secondParent.setSkeletonNode(true);

		firstParent.setChild(child);
		child.setParent(firstParent);

		// versuche, bei beiden das gleiche Vertex als Kind zu setzen => dadurch
		// koennen Twin-Vertices niemals in der Nachbarschaftsberechnung
		// auftauchen
		secondParent.setChild(child);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * updated die Neighbours aller Vertices, die keinen Event ausgeloest haben
	 * gibt jedem Kind eines Vertices die Kinder seiner Nachbarn als neue
	 * Nachbarn
	 */

	private void updateNeighbours() {

		Iterator<SkeletonVertex> vertIter = mPolygon.getVertices().iterator();
		SkeletonVertex currentVertex = null, childCurrentVertex = null, neighbourVertex0 = null, neighbourVertex1 = null;
		SkeletonVertex childNeighbourVertex0 = null, childNeighbourVertex1 = null;

		while (vertIter.hasNext()) {
			currentVertex = vertIter.next();

			// skippe Vertices, die Events ausgeloest haben
			if (currentVertex.isProcessed())
				continue;
			/*
			 * System.out.println("Aktualisiere die Nachbarn fuer Vertex: " +
			 * currentVertex);
			 */
			childCurrentVertex = currentVertex.getChild();

			neighbourVertex0 = currentVertex.getNeighbourByIndex(0);
			neighbourVertex1 = currentVertex.getNeighbourByIndex(1);
			childNeighbourVertex0 = neighbourVertex0.getChild();
			childNeighbourVertex1 = neighbourVertex1.getChild();

			assert childNeighbourVertex0 != null
					&& childNeighbourVertex1 != null
					&& childCurrentVertex != null : "Vertex ohne Kind gefunden: "
					+ childNeighbourVertex0
					+ " "
					+ childNeighbourVertex1
					+ " "
					+ childCurrentVertex;

			childCurrentVertex.setNeighbourOnIndex(0, childNeighbourVertex0);
			childCurrentVertex.setNeighbourOnIndex(1, childNeighbourVertex1);

			String message = "Kind-Vertex :" + childCurrentVertex;
			message += " Nachbar0: " + childNeighbourVertex0;
			message += " Nachbar1: " + childNeighbourVertex1;
			LOGGER.debug(message);
		}

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode aktualisiert die Nachbarn fuer Edge-Events Jedes Event-Vertex
	 * versucht, beide Nachbarn zu aktualisieren, sollten diese Nachbarn bereits
	 * gesetzt sein, so aktualisiert das Vertex nur dann den Nachbarn, falls der
	 * gesetzte Nachbar er selber sein sollte
	 * 
	 */
	private void updateNeighboursForIntersectionEvents() {

		IntersectionEvent currentIntersectionEvent = null;
		iStraightSkeletonEvent currentEvent = null;
		SkeletonVertex currentVertex = null, neighbourCurrent = null, childCurrent = null, childNeighbourCurrent = null;
		boolean updateNeighbour0 = false, updateNeighbour1 = false;

		Iterator<iStraightSkeletonEvent> eventIter = mEvents.iterator();
		while (eventIter.hasNext()) {

			currentEvent = eventIter.next();
			if (currentEvent instanceof IntersectionEvent) {
				currentIntersectionEvent = (IntersectionEvent) currentEvent;
			} else {
				continue;
			}

			// zuruecksetzen
			updateNeighbour0 = false;
			updateNeighbour1 = false;

			// Split-Events werden gesondert verarbeitet
			if (currentIntersectionEvent.getType() != "IntersectionEvent")
				continue;

			currentVertex = currentIntersectionEvent.getVertex();
			childCurrent = currentVertex.getChild();

			assert childCurrent != null : "Event-Vertex ohne Kind gefunden";

			// teste, ob die Nachbarn gesetzt sind, falls nicht, oder falls der
			// gesetzte Nachbar dem eigenen Kind
			// entspricht: aktualisiere die Nachbarn

			if (childCurrent.hasNeighbourWithIndex(0)) {
				childNeighbourCurrent = childCurrent.getNeighbourByIndex(0);
				if (childNeighbourCurrent.equals(childCurrent))
					updateNeighbour0 = true;
			} else
				updateNeighbour0 = true;

			if (updateNeighbour0) {
				neighbourCurrent = currentVertex.getNeighbourByIndex(0);
				childNeighbourCurrent = neighbourCurrent.getChild();
				assert childNeighbourCurrent != null : "Neighbour-Vertex des Event-Vertex besitzt kein Kind";

				childCurrent.setNeighbourOnIndex(0, childNeighbourCurrent);
			}

			// gleiches fuer Nachbar auf Index 1
			if (childCurrent.hasNeighbourWithIndex(1)) {
				childNeighbourCurrent = childCurrent.getNeighbourByIndex(1);
				if (childNeighbourCurrent.equals(childCurrent))
					updateNeighbour1 = true;
			} else
				updateNeighbour1 = true;

			if (updateNeighbour1) {
				neighbourCurrent = currentVertex.getNeighbourByIndex(1);
				childNeighbourCurrent = neighbourCurrent.getChild();
				// System.out.println("Update Nachbar1 von Vertex: " +
				// childCurrent + " mit Kind: " + childNeighbourCurrent +
				// " als Kind von: " + neighbourCurrent);
				assert childNeighbourCurrent != null : "Neighbour-Vertex des Event-Vertex besitzt kein Kind";
				childCurrent.setNeighbourOnIndex(1, childNeighbourCurrent);
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode fuehrt Nachbarschaftsupdates bei Split-Events durch und bestimmt
	 * sowohl fuer das Original- als auch fuer das Twin-Vertex die gueltigen
	 * Nachbarn. Hierbei greift die Methode auf den Virtual-Edge-Ansatz zurueck,
	 * um die gueltigen Nachbarn zu finden. Kern dieses Ansatzes ist die
	 * Sortierung aller Vertices auf einer Kante bzgl. ihrer Distanz zum
	 * Startvertex. Aufgrund der Nachbarschaftsbeziehungen der Eltern kann man
	 * fuer die Vertices angeben, in welcher Richtung sich die jeweiligen
	 * Nachbarn befinden. Da es bei komplexen Polygonen aber dazu kommen kann,
	 * dass mehrere Split- oder Vertex-Events auf der gleichen Kante
	 * stattfinden, muss der Virtual-Edge-Ansatz verwendet werden, um die
	 * korrekten Vertices zu bestimmen.
	 */
	private void updateNeighboursForSplitEvents() {

		iStraightSkeletonEvent currentEvent = null;
		SplitEvent currentSplitEvent = null;
		Iterator<iStraightSkeletonEvent> eventIter = mEvents.iterator();
		SkeletonVertex eventVertex = null, startVertex = null, endVertex = null, eventChild = null, startChild = null, endChild = null;
		SkeletonVertex neighbourStartDirection = null, neighbourEndDirection = null, neighbour0 = null, neighbour1 = null;
		SkeletonVertex neighbour0Child = null, neighbour1Child = null;

		while (eventIter.hasNext()) {
			currentEvent = eventIter.next();
			if (currentEvent.getType() != "SplitEvent")
				continue;
			currentSplitEvent = (SplitEvent) currentEvent;
			eventVertex = currentSplitEvent.getVertex();
			eventChild = eventVertex.getChild();

			// Startvertex der zerteilten Kante
			startVertex = currentSplitEvent.getSecondVertex();
			startChild = startVertex.getChild();

			String message = "Berechne Nachbarschaftsupdates fuer Split-Event mit Event-Vertex: "
					+ eventVertex + " und Second-Vertex: " + startVertex;

			LOGGER.debug(message);

			// Endvertex der zerteilten Kante
			endVertex = startVertex.getNeighbourByIndex(0);
			endChild = endVertex.getChild();

			assert eventChild != null && startChild != null && endChild != null : "Es sind nicht alle relevanten Vertices fuer die Nachbarschaftsbestimmung definiert";

			// hole die Nachbarvertices in Richtung des Start- und des Endvertex
			// der Virtual-Edge
			neighbourStartDirection = mVirtualEdgeManager
					.getNeighbourForVertex(eventChild, startChild);

			neighbourEndDirection = mVirtualEdgeManager.getNeighbourForVertex(
					eventChild, endChild);

			assert neighbourStartDirection != null
					&& neighbourEndDirection != null : "Es konnten keine gueltigen Nachbarn mittels Vritual Edge bestimmt werden";

			// das originale Vertex bekommt das Nachbarvertex auf der virtuellen
			// Kante in Richtung des Startknotens der Kante
			if (neighbourStartDirection != null) {
				eventChild.setNeighbourOnIndex(1, neighbourStartDirection);

			}

			// Nachbar 0 ist Kind des Nachbarn 0 des Event-Vertex
			neighbour0 = eventVertex.getNeighbourByIndex(0);
			neighbour0Child = neighbour0.getChild();

			// ebenfalls ueber virtuelle Kanten anfragen, wenn keine Kante
			// existiert, kommt das gleiche Vertex wieder zurueck
			neighbour0Child = mVirtualEdgeManager.getNeighbourForVertex(
					eventChild, neighbour0Child);

			assert neighbour0Child != null : "Es konnte kein gueltiger Nachbar bestimmt werden";

			if (neighbour0Child != null) {
				eventChild.setNeighbourOnIndex(0, neighbour0Child);
			}

			// Nachbarschaftsupdates des Zwillings
			// Nachbar 0 ist direkter Nachbar auf der Kante in Richtung
			// End-Vertex
			if (neighbourEndDirection != null) {
				eventChild.setNeighbourOnIndex(2, neighbourEndDirection);
			}

			// Nachbar 1 ist Kind des Nachbarn 1 des Event-Vertex
			neighbour1 = eventVertex.getNeighbourByIndex(1);
			neighbour1Child = neighbour1.getChild();

			// System.out.println("Suche Nachbar 1 fuer Zwillingsvertex:");
			neighbour1Child = mVirtualEdgeManager.getNeighbourForVertex(
					eventChild, neighbour1Child);

			assert neighbour1Child != null : "Es konnte kein gueltiger Nachbar bestimmt werden";
			if (neighbour1Child != null) {
				eventChild.setNeighbourOnIndex(3, neighbour1Child);
			}

		}

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode fuehrt die Nachbarschaftsupdates fuer VertexEvent-Vertices durch,
	 * vereinfachend geht man davon aus, dass keine zwei Vertex-Events auf der
	 * selben Kante entstehen koennen
	 */
	private void updateNeighboursForVertexEventsNewSchool() {

		Iterator<iStraightSkeletonEvent> eventIter = mEvents.iterator();
		List<SkeletonVertex> parents = null;

		SkeletonVertex child = null, twin = null;
		SkeletonVertex neighbour0Parent0 = null, neighbour1Parent0 = null, neighbour0Parent1 = null, neighbour1Parent1 = null;
		SkeletonVertex childNeighbour0Parent0Child = null, childNeighbour1Parent0Child = null, childNeighbour0Parent1Child = null, childNeighbour1Parent1Child = null;

		iStraightSkeletonEvent currentEvent = null;
		VertexEvent currentVertexEvent = null;
		while (eventIter.hasNext()) {
			currentEvent = eventIter.next();
			if (!currentEvent.getType().equals("VertexEvent"))
				continue;

			currentVertexEvent = (VertexEvent) currentEvent;
			// wenn der Event als "Nicht verarbeiten" gekennzeichnet wurde,
			// skippe ihn

			if (!currentVertexEvent.isToProcess())
				continue;

			parents = currentVertexEvent.getEventVertices();
			assert parents.size() == 2 : "Vertex Event mit " + parents.size()
					+ " Eltern-Knoten gefunden.";

			// hole Elternvertices
			SkeletonVertex parent0 = parents.get(0);
			SkeletonVertex parent1 = parents.get(1);

			// hole das gemeinsame Kind
			child = parent0.getChild();
			assert child == parent1.getChild() : "Die Eventvertices Parent0: "
					+ parent0 + " und Parent1: " + parent1
					+ " besitzen nicht das gleiche Kind Child: " + child
					+ " Child1: " + parent1.getChild();

			// hole nun die Nachbarn aller beteiligten Objekte
			neighbour0Parent0 = parent0.getNeighbourByIndex(0);
			neighbour1Parent0 = parent0.getNeighbourByIndex(1);
			neighbour0Parent1 = parent1.getNeighbourByIndex(0);
			neighbour1Parent1 = parent1.getNeighbourByIndex(1);

			assert neighbour0Parent0 != null && neighbour1Parent0 != null
					&& neighbour0Parent1 != null && neighbour1Parent1 != null : "Die fuer das Update erforderlichen Nachbarn sind nicht gesetzt";

			// hole die Kinder der Nachbarn
			childNeighbour0Parent0Child = neighbour0Parent0.getChild();
			childNeighbour1Parent0Child = neighbour1Parent0.getChild();
			childNeighbour0Parent1Child = neighbour0Parent1.getChild();
			childNeighbour1Parent1Child = neighbour1Parent1.getChild();

			assert childNeighbour0Parent0Child != null
					&& childNeighbour1Parent0Child != null
					&& childNeighbour0Parent1Child != null
					&& childNeighbour1Parent1Child != null : "Die Kinder der Nachbarn sind nicht gesetzt";

			// hole ueber den Virtual-Edge Ansatz potentiell nicht gespeicherte
			// Nachbarn auf der gleichen Kante
			childNeighbour0Parent0Child = mVirtualEdgeManager
					.getNeighbourForVertex(child, childNeighbour0Parent0Child);
			childNeighbour1Parent0Child = mVirtualEdgeManager
					.getNeighbourForVertex(child, childNeighbour1Parent0Child);

			// arbeite ausschliesslich mit dem Child-Vertex, den Zwilling gibt
			// es auf den virtuellen Kanten nicht mehr
			childNeighbour0Parent1Child = mVirtualEdgeManager
					.getNeighbourForVertex(child, childNeighbour0Parent1Child);
			childNeighbour1Parent1Child = mVirtualEdgeManager
					.getNeighbourForVertex(child, childNeighbour1Parent1Child);

			// an diesem Punkt sind saemtliche Objekte vorhanden, fuehre die
			// Updates durch
			child.setNeighbourOnIndex(0, childNeighbour0Parent0Child);
			child.setNeighbourOnIndex(1, childNeighbour1Parent1Child);

			child.setNeighbourOnIndex(2, childNeighbour0Parent1Child);
			child.setNeighbourOnIndex(3, childNeighbour1Parent0Child);

			// System.out.println("Neighbour-Updates fuer Vertex-Event");
			String message = "Child: " + child + " Nachbar0: "
					+ childNeighbour0Parent0Child + " Nachbar1: "
					+ childNeighbour1Parent1Child;
			// System.out.println(message);
			message = "Child: " + twin + " Nachbar2: "
					+ childNeighbour0Parent1Child + " Nachbar3: "
					+ childNeighbour1Parent0Child;
			LOGGER.debug(message);

		}

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * bereitet den naechsten Iterationsschritt vor, sofern ein Split-Event
	 * waehrend der Berechnung aufgetreten ist, wird das Eingabepolygon in
	 * mehrere Polygone aufgespalten, fuer jedes dieser Polygone wird dann ein
	 * neuer Job zur globalen Job-Queue hinzugefuegt
	 */
	private void initializeNextIteration() {

		final List<SkeletonJob> nextJobs = new ArrayList<SkeletonJob>();

		if (mSplitEventOccured) {
			nextJobs.addAll(splitPolygon());
		} else {
			// erzeuge ein neues Polygon aus den Vertices im ChildBuffer
			SkeletonPolygon polygon = new SkeletonPolygon(mChildBuffer);
			SkeletonJob job = new SkeletonJob(polygon, mLevel + 1, true,
					mAlgorithm, mBuildDirection);
			nextJobs.add(job);
		}

		// berechne die Graphstrukturen fuer alle Jobs vor
		for (SkeletonJob curJob : nextJobs) {
			curJob.doVertexComputations();
		}

		SkeletonEdge currentEdge = null;
		float edgeWeight;

		final EdgeWeightService edgeWeightService = mAlgorithm
				.getEdgeWeightService();

		// wenn ein ChangeSlopeEvent verarbeitet wurde, aktualisiere die
		// Kantengewichte im EdgeManager
		if (this.mProcessedSlopeEvent) {
			edgeWeightService.changeWeights(mAlgorithm.getConf());
			LOGGER.info("Changing SlopeChangeHeight!");
		}

		// durchlaufe alle Kinder, weise Gewichte basierend auf den
		// Ausrichtungen der Kanten zu
		for (SkeletonVertex currentChild : mChildBuffer) {

			currentEdge = currentChild.getNeighbourEdgeByIndex(0);

			assert currentEdge != null : "FEHLER: Keine Kante auf Index 0 definiert!";

			edgeWeight = edgeWeightService
					.getWeight(currentEdge.getDirection());
			currentEdge.setWeight(edgeWeight);

			currentEdge = currentChild.getNeighbourEdgeByIndex(1);

			assert currentEdge != null : "FEHLER: Keine Kante auf Index 1 definiert!";

			edgeWeight = edgeWeightService
					.getWeight(currentEdge.getDirection());
			currentEdge.setWeight(edgeWeight);
		}

		// abschliessende Berechnungen durchfuehren
		for (SkeletonJob curJob : nextJobs) {
			curJob.finalizeInitialization();
			mAlgorithm.addJobToQueue(curJob);
		}
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode fuehrt Aufraeumarbeiten durch, added saemtliche Twin-Vertices zu
	 * den Virtual-Edges, damit diese in der Result-Berechnung verwendet werden
	 * koennen usw.
	 */

	private void cleanUp() {

		// rufe fuer alle Vertices die Clear-Methode auf, um den
		// Neighbour-Buffer zu leeren
		Iterator<SkeletonVertex> vertIter = mChildBuffer.iterator();
		SkeletonVertex currentVertex = null;
		while (vertIter.hasNext()) {

			currentVertex = vertIter.next();

			// wird nur bei wenigen Vertices notwendig sein, aber so ists
			// einfacher
			currentVertex.clearNeighbourBuffer();

			// wenn es sich um ein Twin-Vertex handelt, adde es zu den
			// virtuellen Kanten dieser Iteration
			// wird fuer die Result-Berechnung benoetigt
			if (currentVertex.isTwinVertex()) {
				mVirtualEdgeManager.addTwinVertexToVirtualEdge(currentVertex);
			}
		}
		// sortiere die Kanten neu, etc.
		mVirtualEdgeManager.finalizeVirtualEdgeStructures();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * testet das Abbruchkriterium des Algorithmus breche ab, wenn keine Events
	 * mehr festgestellt werden => dann schrumpft das Polygon nicht mehr oder
	 * die die Anzahl an Vertices <= 2 ist => in diesem Fall ist der
	 * Flaecheninhalt 0
	 */
	private boolean hasReachedEnd() {

		if (!(mLevel < mAlgorithm.getMaxNumberOfLevels())) {
			LOGGER.info("Abburch der Verarbeitung: Die maximale Anzahl von "
					+ mLevel + " Kindebenen wurde erreicht");
			return true;
		}

		if (!(mEvents.size() > 0)) {
			LOGGER.info("Abburch der Verarbeitung: Es wurden keine neuen Events berechnet");
			return true;
		}

		if (!(mChildBuffer.size() > 2)) {
			LOGGER.info("Abburch der Verarbeitung: Die Anzahl der Kindvertices ist kleiner / gleich 2");
			return true;
		}

		return false;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode versucht einen vereinfachten Ansatz fuer das Splitting der
	 * Polygone umzusetzen. Anstatt bei Event-Vertices zu starten, verwendet man
	 * einfach nur die Vertices im Child-Buffer und durchlaeuft diesen
	 * sequentiell
	 */
	private List<SkeletonJob> splitPolygon() {

		List<SkeletonVertex> polygon = null;
		List<Integer> polygonVertexCounter = new ArrayList<Integer>();
		List<SkeletonJob> newJobs = new ArrayList<SkeletonJob>();

		Iterator<SkeletonVertex> vertIter = mChildBuffer.iterator();
		SkeletonVertex currentVertex = null;

		int numberOfItemsInBuffer = mChildBuffer.size();

		while (vertIter.hasNext()) {
			currentVertex = vertIter.next();

			// wenn das aktuelle Vertex noch nicht zu einem Polygon hinzugefuegt
			// wurde, versuche ein solches zu berechnen
			if (!currentVertex.isAddedToNewPolygon()) {
				polygon = getPolygonForStartVertex(currentVertex);

				// erzeuge einen neuen Job fuer das berechnete Polygon
				if (polygon.size() > 1) {
					SkeletonPolygon firstPolygon = new SkeletonPolygon(polygon);
					SkeletonJob firstJob = new SkeletonJob(firstPolygon,
							mLevel + 1, true, mAlgorithm, mBuildDirection);
					newJobs.add(firstJob);

					// System.out.println(firstPolygon);
					polygonVertexCounter.add(polygon.size());
				}
			}
		}

		LOGGER.info("-----------------------------------------------------------------------");
		LOGGER.info("Polygonsplitting-Statistiken: ");
		LOGGER.info("Gesamtzahl der Vertices im Childbuffer: "
				+ numberOfItemsInBuffer);

		for (int i = 0; i < polygonVertexCounter.size(); i++) {
			LOGGER.info("Polygon " + i + ": " + polygonVertexCounter.get(i)
					+ " Vertices");
			numberOfItemsInBuffer -= polygonVertexCounter.get(i);
		}

		assert numberOfItemsInBuffer == 0 : "Es wurden "
				+ numberOfItemsInBuffer
				+ " Vertices nicht zu neuen Jobs hinzugefuegt";
		LOGGER.info("-----------------------------------------------------------------------");
		return newJobs;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode durchlaeuft die Nachbarschaftsbeziehungen ausgehend von dem
	 * uebergebenen Startvertex und fuegt alle Nachbarn mit Index 0 sequentiell
	 * zum Polygon hinzu (sofern sie nocht nicht zu einem anderen Polygon
	 * geadded wurden)
	 * 
	 * @param start
	 *            Vertex, von dem ausgehend die Graphenstruktur durchlaufen wird
	 * @return Polygon, das alle Vertices enthaelt, die ueber Nachbarschaften
	 *         mit Index 0 vom Startvertex ausgehend erreicht werden koennen
	 */
	private List<SkeletonVertex> getPolygonForStartVertex(SkeletonVertex start) {

		List<SkeletonVertex> polygon = new ArrayList<SkeletonVertex>();
		SkeletonVertex currentNeighbour = null;

		assert !start.isAddedToNewPolygon() : "Das uebergebene Startvertex wurde bereits zu einem anderen Polygon hinzugefuegt";
		currentNeighbour = start.getNeighbourByIndex(0);

		assert !currentNeighbour.isAddedToNewPolygon() : "Der Nachbar des Startvertex wurde bereits zu einem anderen Polygon hinzugefuegt";

		// fuege die beiden Vertices zum Ergebnisvektor hinzu
		polygon.add(start);
		polygon.add(currentNeighbour);

		start.setAddedToNewPolygon(true);
		currentNeighbour.setAddedToNewPolygon(true);

		// wenn das Startvertex erreicht wird, breche ab
		while (!currentNeighbour.equals(start)) {
			currentNeighbour = currentNeighbour.getNeighbourByIndex(0);

			// wenn ein Vertex erreicht wird, das bereits einem anderen Polygon
			// hinzugefuegt wurde, breche ab
			if (currentNeighbour.isAddedToNewPolygon())
				break;
			else {
				polygon.add(currentNeighbour);
				currentNeighbour.setAddedToNewPolygon(true);
			}
		}

		return polygon;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode prueft, ob es sich um ein degeneriertes Polygon handelt und
	 * untersucht die Nachbarschaftsbeziehungen der Vertices im Child-Buffer.
	 * Ein Polygon ist degeneriert, falls die Nachbarn eines Vertices auf einer
	 * Kante liegen oder ein Vertex zweimal den gleichen Nachbarn besitzt. In
	 * diesem Fall wird das betreffende Vertex aus dem Child-Buffer entfernt und
	 * die Nachbarschaftsbeziehungen der betroffenen anderen Vertices werden
	 * aktualisiert NOTE: das entfernen kann bei Split-Events etc. zu Problemen
	 * fuehren, wenn das Kind des Split- oder Vertex-Events entfernt wird
	 */
	private void validatePolygonStructure() {

		Iterator<SkeletonVertex> vertIter = mChildBuffer.iterator();

		SkeletonVertex currentVertex = null;
		String message;

		while (vertIter.hasNext()) {
			currentVertex = vertIter.next();

			if (areNeighboursTheSame(currentVertex)) {

				// aufgrund der bidirektionalen Verbindung zwischen Vertices
				// kann es nur dann gleiche Nachbarn geben, wenn auch der
				// Nachbar gleiche Nachbarn besitzt
				// sonst hätte einer der Nachbarn unterschiedlich sein muessen
				// updateNeighboursForSameNeighbour(currentVertex);

				message = "Vertex: "
						+ currentVertex
						+ " wurde aufgrund gleicher Nachbarn aus dem Child-Buffer geloescht";
				LOGGER.debug(message);
				// addToMessageBuffer(message);
				// markiere das Vertex als geloescht
				currentVertex.setDeleted(true);
				vertIter.remove();
				// printJobResult();

			} else if (areNeighboursOnSameRay(currentVertex)) {

				// Update die Nachbarn
				updateNeighboursForInvalidVertexOnSameRayNewSchool(currentVertex);
				message = "Vertex: "
						+ currentVertex
						+ " wurde aufgrund von linearen Nachbarn aus dem Child-Buffer geloescht";
				LOGGER.debug(message);
				// addToMessageBuffer(message);
				// markiere das Vertex als geloescht
				currentVertex.setDeleted(true);

				// und loesche das Vertex aus dem Buffer
				vertIter.remove();
				// printJobResult();
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode validiert die Nachbarschaft des uebergebenen Vertex. Sofern es
	 * sich um gueltige Nachbarn handelt, gibt sie true zurueck, sonst false.
	 * Eine Nachbarschaft ist gueltig, wenn die Nachbarn nicht auf einer Geraden
	 * liegen. Macht keine Probleme bei Split- oder Vertex-Events, weil die
	 * Nachbarn dabei definitiv nicht auf einer Kante liegen. Somit ist es in
	 * der regulaeren Verarbeitung immer gueltig, Vertices zu entfernen, die die
	 * Testbedingung erfuellen.
	 */
	private boolean areNeighboursOnSameRay(SkeletonVertex vertex) {

		SkeletonVertex neighbour0 = null, neighbour1 = null;
		neighbour0 = vertex.getNeighbourByIndex(0);
		neighbour1 = vertex.getNeighbourByIndex(1);

		MyVector3f direction = new MyVector3f();

		// berechne einen Strahl zwischen den Nachbarn und pruefe, ob sich das
		// Vertex auf diesem Strahl befindet
		direction.sub(neighbour0.getPosition(), neighbour1.getPosition());

		Ray ray = new Ray(neighbour1.getPosition(), direction);

		// teste, ob der zweite Nachbar ebenfalls auf dem Strahl liegt
		if (mMathHelper.isPointOnRay(vertex.getPosition(), ray))
			return true;
		else
			return false;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode prueft nur, ob beide Nachbarn des Vertex gleich sind
	 * 
	 * @param vertex
	 *            Vertex, fuer das die Ueberpruefung durchgefuehrt wird
	 * @return True, falls die Nachbarn uebereinstimmen, false sonst
	 */
	private boolean areNeighboursTheSame(SkeletonVertex vertex) {

		SkeletonVertex neighbour0 = null, neighbour1 = null;
		neighbour0 = vertex.getNeighbourByIndex(0);
		neighbour1 = vertex.getNeighbourByIndex(1);

		if (neighbour0.equals(neighbour1))
			return true;
		else
			return false;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode aktualisiert die Nachbarn eines Loesch-Vertex, um eine gueltige
	 * Polygonstruktur zu garantieren.
	 * 
	 * @param invalidVertex
	 *            Eingabevertex, dessen Nachbarn aktualisiert werden sollen
	 * 
	 */
	private void updateNeighboursForInvalidVertexOnSameRayNewSchool(
			SkeletonVertex invalidVertex) {

		SkeletonVertex newNeighbourForNeighbour0 = null, newNeighbourForNeighbour1 = null;

		SkeletonVertex neighbour0 = invalidVertex.getNeighbourByIndex(0);
		SkeletonVertex neighbour1 = invalidVertex.getNeighbourByIndex(1);

		String message = "Berechne Nachbarschaftsupdate fuer geloeschtes Vertex: "
				+ invalidVertex
				+ " Nachbar0: "
				+ neighbour0
				+ " Nachbar1: "
				+ neighbour1;

		LOGGER.debug(message);

		// aktualisiere zunaechst Nachbar 0
		// wenn Nachbar 0 geloescht wurde, skippe das Update
		if (!neighbour0.isDeleted()) {

			// aktualisiere nur, wenn der Nachbar auch tatsaechlich eine
			// Referenz auf den geloeschten Knoten gespeichert hat
			if (neighbour0.getIndexForNeighbour(invalidVertex) != -1) {
				// bestimme ueber die Virtual-Edges den naechsten gueltigen
				// Nachbarn auf der Kante
				newNeighbourForNeighbour0 = mVirtualEdgeManager
						.getNeighbourForDeletedVertex(neighbour0, invalidVertex);
				neighbour0.setNeighbourOnIndex(1, newNeighbourForNeighbour0);

				// reflexives Update
				newNeighbourForNeighbour0.setNeighbourOnIndex(0, neighbour0);

				LOGGER.debug("Nachbar0 " + neighbour0
						+ " bekommt neuen Nachbar auf Index1: "
						+ newNeighbourForNeighbour0);

			}
		}

		// das Ganze ebenfalls fuer Nachbar 1
		if (!neighbour1.isDeleted()) {
			if (neighbour1.getIndexForNeighbour(invalidVertex) != -1) {
				newNeighbourForNeighbour1 = mVirtualEdgeManager
						.getNeighbourForDeletedVertex(neighbour1, invalidVertex);
				neighbour1.setNeighbourOnIndex(0, newNeighbourForNeighbour1);

				// reflexives Update
				newNeighbourForNeighbour1.setNeighbourOnIndex(1, neighbour1);

				LOGGER.debug("Nachbar1 " + neighbour1
						+ " bekommt neuen Nachbar auf Index0: "
						+ newNeighbourForNeighbour1);
			}
		}

		// erzeuge eine virtuelle Kante fuer das geloeschte Vertex => wird fuer
		// die spaetere Result-Struktur-Berechnung benoetigt
		VirtualEdge newEdge = mVirtualEdgeManager.addVertsToVirtualEdge(
				neighbour0, neighbour1);
		newEdge.addVertexToEdge(invalidVertex);

		// keine Zwillinge mehr zur Kante adden
		if (invalidVertex.hasTwinVertex())
			newEdge.addVertexToEdge(invalidVertex.getTwinVertex());

		// Edge-Vertices sortieren
		newEdge.sortEdgeVerticesByDistance();
		LOGGER.debug(mVirtualEdgeManager);

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Wrapper Methode zum Hinzufuegen von Nachrichten zum globalen
	 * Nachrichten-Buffer
	 */
	private void addToMessageBuffer(String message) {
		mAlgorithm.addToMessageBuffer(message);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erzeugt fuer alle Split- und Vertex-Events virtuelle Edges
	 */
	private void computeVirtualEdges() {
		Iterator<iStraightSkeletonEvent> eventIter = mEvents.iterator();
		iStraightSkeletonEvent currentEvent = null;
		SplitEvent currentSplitEvent = null;
		VertexEvent currentVertexEvent = null;

		SkeletonVertex eventVertex = null, eventNeighbourVertex = null;

		List<SkeletonVertex> testPlaneVertices = null;

		while (eventIter.hasNext()) {
			currentEvent = eventIter.next();
			if (currentEvent.getType().equals("IntersectionEvent"))
				continue;

			else if (currentEvent.getType().equals("SplitEvent")) {
				currentSplitEvent = (SplitEvent) currentEvent;
				eventVertex = currentSplitEvent.getVertex();
				eventNeighbourVertex = currentSplitEvent.getSecondVertex();

				createVirtualEdgeForNeighbourVertex(eventVertex,
						eventNeighbourVertex);
			}

			// fuer jeden Nachbarn erstellt man eine Virtual-Edge-Struktur
			else if (currentEvent.getType().equals("VertexEvent")) {
				currentVertexEvent = (VertexEvent) currentEvent;

				// hole nur das erste Event-Vertex, ueber dieses kommt man an
				// alle weiteren Vertices heran
				eventVertex = currentVertexEvent.getEventVertices().get(0);

				testPlaneVertices = currentVertexEvent.getTestplaneVertices();

				// erzeuge fuer jedes Vertex im TestPlaneVertices-Vektor eine
				// virtuelle Kante (bzw. fuege die Vertices zur virtuellen Kante
				// hinzu)
				for (int i = 0; i < testPlaneVertices.size(); i++) {
					createVirtualEdgeForNeighbourVertex(eventVertex,
							testPlaneVertices.get(i));
				}

			}
		}

		// alle Edges sind erstellt, rufe die abschliessenden
		// Berechnungsstrukturen auf
		mVirtualEdgeManager.finalizeVirtualEdgeStructures();
		LOGGER.debug(mVirtualEdgeManager);

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erstellt fuer das uebergebene neighbourVertex eine VirtualEdge.
	 * Anschliessend fuegt sie die Kinder des Event-Vertex zu dieser virtuellen
	 * Kante hinzu.
	 * 
	 * @param eventVertex
	 *            Vertex, das den Event ausgeloest hat, die Kinder dieses Vertex
	 *            werden zur virtuellen Kante hinzugefuegt
	 * @param neighbour
	 *            Startvertex der Kante, die erstellt wird.
	 */
	private void createVirtualEdgeForNeighbourVertex(
			SkeletonVertex eventVertex, SkeletonVertex neighbour) {

		String message = "Erstelle eine virtuelle Kante fuer Vertex: "
				+ eventVertex + " mit dem Startvertex der zerteilten Kante: "
				+ neighbour;

		LOGGER.debug(message);

		// Nachbar mit Index 0 des im Event gespeicherten Nachbarn
		SkeletonVertex eventNeighbourNeighbour = neighbour
				.getNeighbourByIndex(0);

		// hole die Kinder der Nachbarn
		SkeletonVertex eventNeighbourChild = neighbour.getChild();
		SkeletonVertex eventNeighbourNeighbourChild = eventNeighbourNeighbour
				.getChild();

		LOGGER.debug("Kinder: " + eventNeighbourChild + " Kind2: "
				+ eventNeighbourNeighbourChild);

		assert eventNeighbourChild != null
				&& eventNeighbourNeighbourChild != null : "Die zerteilte Kante ist nicht definiert";

		// KEINE ZWILLINGE ZUR KANTE ADDEN!!!
		// dieser Fall kann auftreten, wenn das Vertex Nachbar eines
		// Vertex-Events ist, in diesem Fall ist einer der Event-Knoten Vater
		// eines Zwillings
		if (eventNeighbourChild.isTwinVertex()) {
			eventNeighbourChild = eventNeighbourChild.getTwinVertex();
		}

		if (eventNeighbourNeighbourChild.isTwinVertex()) {
			eventNeighbourNeighbourChild = eventNeighbourNeighbourChild
					.getTwinVertex();
		}

		// erzeuge eine virtuelle Kante fuer die zerteilte Kante
		VirtualEdge virtualEdge = mVirtualEdgeManager.addVertsToVirtualEdge(
				eventNeighbourChild, eventNeighbourNeighbourChild);

		SkeletonVertex eventChild = eventVertex.getChild();
		assert virtualEdge != null : "Es konnte keine virtuelle Kante erzeugt werden";
		assert eventChild != null : "Das Event-Vertex besitzt kein Kind";
		assert eventChild.hasTwinVertex() : "Das Kind des Eventvertex besitzt keinen Zwilling";

		LOGGER.debug("Adde Vertex: " + eventChild);

		// fuege dieser virtuellen Kante nun das Event-Child hinzu
		virtualEdge.addVertexToEdge(eventChild);

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode durchlaeuft am Abschluss der Verarbeitung (aber vor dem
	 * Polygon-Splitting) alle Kinder im Child-Buffer und prueft, ob die
	 * gespeicherten Beziehungen bidirektional sind.
	 */
	private void validateBidirectionalNeighbourStructures() {

		Iterator<SkeletonVertex> vertIter = mChildBuffer.iterator();
		SkeletonVertex currentVertex = null, currentNeighbour = null;

		while (vertIter.hasNext()) {
			currentVertex = vertIter.next();

			// teste Nachbar 0
			currentNeighbour = currentVertex.getNeighbourByIndex(0);
			if (!currentNeighbour.getNeighbourByIndex(1).equals(currentVertex)) {
				findValidNeighbourForVertex(currentVertex, 0);
			}

			// teste Nachbar 1
			currentNeighbour = currentVertex.getNeighbourByIndex(1);
			if (!currentNeighbour.getNeighbourByIndex(0).equals(currentVertex)) {
				findValidNeighbourForVertex(currentVertex, 1);
			}
		}

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode versucht, fuer das uebergebene Vertex einen passenden Nachbarn
	 * fuer den uebergebenen Neighbourslot zu finden. Die Methode wird
	 * aufgerufen, wenn festgestellt wurde, dass eine Beziehung zwischen
	 * Vertices nicht bidrektional ist. In diesem Fall versucht die Methode, ein
	 * Vertex im Child-Buffer zu finden, das eine Referenz auf das uebergebene
	 * Vertex an der korrekten Nachbarposition besitzt. Wenn dies der Fall ist,
	 * wird das uebergebene Vertex dementsprechend aktualisiert
	 */
	private void findValidNeighbourForVertex(SkeletonVertex vertex,
			Integer neighbourIndex) {

		LOGGER.debug("Suche gueltigen Nachbarn fuer Vertex: " + vertex
				+ " auf Position: " + neighbourIndex);

		Iterator<SkeletonVertex> vertIter = mChildBuffer.iterator();
		SkeletonVertex currentVertex = null, currentNeighbour = null;

		// bestimme, an welcher Position im Nachbararray die Referenz stehen
		// muesste
		Integer indexToLookFor = -1;
		if (neighbourIndex == 0)
			indexToLookFor = 1;
		else
			indexToLookFor = 0;

		while (vertIter.hasNext()) {
			currentVertex = vertIter.next();
			currentNeighbour = currentVertex
					.getNeighbourByIndex(indexToLookFor);

			// wenn eine Referenz gefunden wurde, aktualisiere die
			// Nachbarschaftsbeziehung
			if (currentNeighbour.equals(vertex)) {
				vertex.setNeighbourOnIndex(neighbourIndex, currentVertex);
				String message = "FEHLER: Es wurde eine fehlende bidirektionale Beziehung fuer Vertex "
						+ vertex
						+ " repariert, der neue Nachbar auf Slot "
						+ neighbourIndex + " ist Vertex " + currentVertex;
				LOGGER.debug(message);
				addToMessageBuffer(message);
				return;
			}
		}
		assert false : "Es konnte kein Vertex gefunden werden, dass eine Referenz auf Vertex "
				+ vertex + " auf Index " + indexToLookFor + " speichert";
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode durchlaeuft alle Vertices im Child-Buffer und sucht Vertices mit
	 * Zwilling. Wird ein solches Vertex gefunden, ubertraegt man die Nachabrn
	 * mit Index 3 und 4 auf die Zwillinge. Anschliessend durchlaeuft man
	 * saemtliche Vertices mit Zwilling erneut (die aber nicht selber Zwilling
	 * sind) und aktualisiert jeden einzelnen Nachbarslot. Slots > 1 werden
	 * direkt im Zwilling aktualisiert, <= 1 im Original. Hierbei setzt man die
	 * bidirektionalen Updates um.
	 */
	private void updateNeighboursForTwinVertices() {
		Iterator<SkeletonVertex> vertIter = mChildBuffer.iterator();
		SkeletonVertex currentVertex = null, twin = null;
		SkeletonVertex neighbour0 = null, neighbour1 = null;

		while (vertIter.hasNext()) {
			currentVertex = vertIter.next();
			if (currentVertex.hasTwinVertex() && !currentVertex.isTwinVertex()) {
				twin = currentVertex.getTwinVertex();
			} else
				continue;

			// kopiere den Buffer des aktuellen Vertex, damit durch die
			// reflexiven Updates nicht die Ausgangsnachbarn verloren gehen
			currentVertex.copyNeighbourBuffer();

			// hole die Nachbarn mit Indices 3 und 4
			neighbour0 = currentVertex.getNeighbourByIndex(2);
			neighbour1 = currentVertex.getNeighbourByIndex(3);

			assert neighbour0 != null && neighbour1 != null : "Fehler: das verarbeitete Originalvertex besitzt keine 4 Nachbarn: Neighbour2: "
					+ neighbour0 + " Neighbour3: " + neighbour1;

			// und setze sie auf die Nachbarslots
			twin.setNeighbourOnIndex(0, neighbour0);
			twin.setNeighbourOnIndex(1, neighbour1);

			// kopiere auch den Nachbarbuffer des Zwillings
			twin.copyNeighbourBuffer();

			String message = "Setze Nachbarn fuer Twin-Vertex: " + twin
					+ " Nachbar0: " + neighbour0 + " Nachbar1: " + neighbour1;
			LOGGER.debug(message);
		}

		// fuehre nun die reflexiven Updates durch
		vertIter = mChildBuffer.iterator();
		while (vertIter.hasNext()) {
			currentVertex = vertIter.next();

			// update nun alle Slots der Original-Vertices, wenn der Slot > 1
			// ist, wird das Twin-Vertex aktualisiert
			if (currentVertex.hasTwinVertex() && !currentVertex.isTwinVertex()) {
				for (int i = 0; i < currentVertex.getNeighbours().length; i++) {
					updateNeighbour(currentVertex, i);
				}
			} else
				continue;
		}

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode aktualisiert die Nachbarschaftsbeziehung zwischen dem
	 * Ausgangsvertex und dem Nachbarn auf dem uebergebenen Slot. Hierbei wird
	 * auch der Sonderfall abgedeckt, bei dem der Nachbar selber ein Twin-Vertex
	 * ist. Sofern dies der Fall ist, muss beim Update entschieden werden, ob
	 * auf den Zwilling oder das Original gezeigt wird. Das Verfahren geht davon
	 * aus, dass zwischen den Originalen eine bidirektionale Verbindung besteht.
	 * Darum testet man im Zielvertex, an welchem Slot das Ausgangsvertex in den
	 * Nachbarn vorkommt. Ist der Slot > 1, verwendet man das Zwillingsvertex,
	 * sonst das Original.
	 * 
	 * @param origin
	 *            Vertex, fuer das ein Update der Nachbarn im uebergebenen Slot
	 *            durchgefuehrt werden soll
	 * @param slot
	 *            Nachbarslot im Neighboursarray, der aktualisiert werden soll
	 */
	private void updateNeighbour(SkeletonVertex origin, int slot) {

		int newSlot = -1, neighbourSlot = -1, indexForTwinVertexNeighbour = -1;
		SkeletonVertex validNeighbour = null;

		String errorCase = "Aktualisiere Nachbarn fuer Origin: " + origin
				+ " auf Slot: " + slot + mLineBreak;

		// verwende den Buffer, um nicht ueber Aktualisierungen zu stolpern
		SkeletonVertex currentNeighbour = origin
				.getNeighbourByIndexFromBuffer(slot);

		assert currentNeighbour != null : "Fehler: Das uebergebene Vertex "
				+ origin + " besitzt auf dem uebergebenen Slot: " + slot
				+ " keinen gueltigen Nachbarn";

		// wenn der Slot > 1 ist, muss das Twin-Vertex verwendet werden
		if (slot > 1) {
			validNeighbour = origin.getTwinVertex();
			if (slot == 2) {
				// slot des TwinVertex
				newSlot = 0;
				// slot des Nachbarn beim reflexiven Update
				neighbourSlot = 1;
			} else if (slot == 3) {
				newSlot = 1;
				neighbourSlot = 0;
			}
		} else {
			// Slot ist <= 1, verwende das Original und aktualisiere seinen
			// Nachbarn
			validNeighbour = origin;
			newSlot = slot;
			if (slot == 0)
				neighbourSlot = 1;
			else
				neighbourSlot = 0;
		}

		// fange den Sonderfall ab, ob das aktuelle Nachbarvertex selber einen
		// Zwilling besitzt
		// aufgrund des Zugriffs ueber den Neighbour-Buffer (der nur Originale
		// vorhaelt) kann currentNeighbour selber kein Zwilling sein
		if (!currentNeighbour.hasTwinVertex()) {
			validNeighbour.setNeighbourOnIndex(newSlot, currentNeighbour);
			currentNeighbour.setNeighbourOnIndex(neighbourSlot, validNeighbour);
		} else {
			// wenn der Nachbar ein TwinVertex besitzt, muss man prüfen, ob das
			// Original-Vertex einen Nachbarn besitzt,
			// der dem Origin-Vertex entspricht. Diesen muss man dann switchen

			indexForTwinVertexNeighbour = currentNeighbour
					.getIndexForNeighbourInBuffer(origin);

			errorCase += "Verwende Vertex: " + validNeighbour + " fuer Update."
					+ mLineBreak;
			errorCase += "Ziel-Nachbar: " + currentNeighbour + mLineBreak;
			for (int i = 0; i < currentNeighbour.getNeighbourBuffer().length; i++) {
				errorCase += " Nachbar " + i + ": "
						+ currentNeighbour.getNeighbourBuffer()[i] + mLineBreak;
			}

			// besteht eine bidirektionale Verbindung zwischen den Vertices?
			assert indexForTwinVertexNeighbour != -1 : "Der Zwilling des Zielvertex besitzt keinen Zeiger auf das Origin-Vertex "
					+ mLineBreak + errorCase;

			// wenn der Index > 1 ist, verwende den Zwilling als Ziel
			if (indexForTwinVertexNeighbour > 1) {
				currentNeighbour = currentNeighbour.getTwinVertex();
			}

			validNeighbour.setNeighbourOnIndex(newSlot, currentNeighbour);
			currentNeighbour.setNeighbourOnIndex(neighbourSlot, validNeighbour);
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode stellt nach Abschluss aller Berechnungen die Verbindung vom 2.
	 * Elternknoten eines Vertex-Events derart um, dass diesem der Zwilling als
	 * Kind zugewiesen wird und nicht das Original-Kind. Diese urspruengliche
	 * Zuordnung ist in den ersten Phasen der Berechnung fuer die korrekte
	 * Bestimmung der Nachbarschaften notwendig.
	 */
	private void connectVertexEventParents() {

		Iterator<iStraightSkeletonEvent> eventIter = mEvents.iterator();
		iStraightSkeletonEvent current = null;
		VertexEvent currentVertexEvent = null;
		SkeletonVertex parent = null, child = null, twin = null;

		while (eventIter.hasNext()) {
			current = eventIter.next();
			if (!current.getType().equals("VertexEvent"))
				continue;
			currentVertexEvent = (VertexEvent) current;

			// ueberspringe Events, die als "nicht zu verarbeiten" markiert sind
			if (!currentVertexEvent.isToProcess())
				continue;

			// hole das 2. Event-Vertex, diesem wird der Zwilling zugewiesen
			parent = currentVertexEvent.getEventVertices().get(1);
			child = parent.getChild();

			// stelle sicher, dass bei Vertices das selbe Kind besitzen
			assert child == currentVertexEvent.getEventVertices().get(0)
					.getChild() : "Die Event-Vertices besitzen unterschiedliche Kinder";

			twin = child.getTwinVertex();
			assert twin != null : "Das Kindvertex besitzt keinen Zwilling";

			// weise nun dem 2. Parent das Twin-Vertex als Kind zu
			parent.setChild(twin);
			twin.setParent(parent);
		}
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mEdgeWeights
	 */
	public List<Float> getEdgeWeights() {
		return mEdgeWeights;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mEdgeWeights
	 *            the mEdgeWeights to set
	 */
	public void setEdgeWeights(List<Float> mEdgeWeights) {
		this.mEdgeWeights = mEdgeWeights;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode gibt das Eingabepolygon aus
	 * 
	 * @return String mit den Koordinaten des Einagbepolygons des aktuellen Jobs
	 */
	private String printInputPoly() {

		String lineSeparator = System.getProperty("line.separator");
		String message = "printInputPoly: " + lineSeparator;

		SkeletonVertex currentVert = null;
		List<SkeletonVertex> polyVerts = mPolygon.getVertices();
		for (int i = 0; i < polyVerts.size(); i++) {
			currentVert = polyVerts.get(i);
			message += "mVertices.add(new Vertex3d(" + currentVert.getX()
					+ "f, " + currentVert.getY() + "f, " + currentVert.getZ()
					+ "f));" + lineSeparator;

		}
		return message;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode gibt alle berechneten Kinder des Jobs aus
	 */
	private String printChildren() {

		String lineBreak = System.getProperty("line.separator");
		String message = "printChildren: " + lineBreak;
		for (int i = 0; i < mChildBuffer.size(); i++) {
			message += "mVertices.add(new Vertex3d("
					+ mChildBuffer.get(i).getX() + "f, "
					+ mChildBuffer.get(i).getY() + "f, "
					+ mChildBuffer.get(i).getZ() + "f));" + lineBreak;
		}

		return message;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Utility-Methode: durchlaeuft den gesamten Child-Buffer und gibt die
	 * Nachbarn saemtlicher Vertices aus
	 */
	private void printJobResult() {

		SkeletonVertex currentVertex = null, neighbour0 = null, neighbour1 = null;
		Iterator<SkeletonVertex> vertIter = mChildBuffer.iterator();
		String output = "-----------------------------------------------------------------------"
				+ mLineBreak;
		output += "Job-Result fuer Level " + mLevel + mLineBreak;
		output += "Vertices im Child-Buffer: " + mChildBuffer.size()
				+ mLineBreak;
		while (vertIter.hasNext()) {
			currentVertex = vertIter.next();
			output += currentVertex + mLineBreak;
			if (currentVertex.hasNeighbourWithIndex(0)) {
				neighbour0 = currentVertex.getNeighbourByIndex(0);
				output += "............Nachbar0: " + neighbour0 + mLineBreak;
			} else
				output += "............Nachbar0: NULL" + mLineBreak;

			if (currentVertex.hasNeighbourWithIndex(1)) {
				neighbour1 = currentVertex.getNeighbourByIndex(1);
				output += "............Nachbar1: " + neighbour1 + mLineBreak;

			} else
				output += "............Nachbar1: NULL" + mLineBreak;
		}
		output += "-----------------------------------------------------------------------"
				+ mLineBreak;
		LOGGER.info(output);

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Utility-Methode: Ausgabe aller aufgetretenen Events im Event-Buffer
	 */
	private void printEventBuffer(String prefix) {

		iStraightSkeletonEvent currentEvent = null;
		int counter = 1;
		String output = "";
		if (prefix.length() > 0)
			output += prefix + mLineBreak;
		output += "Insgesamt befinden sich " + mEvents.size()
				+ " Events im Buffer" + mLineBreak;

		Iterator<iStraightSkeletonEvent> eventIter = mEvents.iterator();
		while (eventIter.hasNext()) {
			currentEvent = eventIter.next();
			output += counter + ". " + currentEvent.toString() + mLineBreak;
			counter++;
		}
		LOGGER.info(output);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode durchlaeuft alle Vertices des Eingabepolygons und gibt deren
	 * Nachbarschaftsstruktur aus
	 * 
	 * @return String mit allen Vertices inklusive ihrer Nachbarn
	 */
	private String printNeighbourStructure() {

		List<SkeletonVertex> polyVerts = mPolygon.getVertices();
		SkeletonVertex currentVert = null;
		String lineSeparator = System.getProperty("line.separator");
		String message = lineSeparator;

		for (int i = 0; i < polyVerts.size(); i++) {
			currentVert = polyVerts.get(i);
			message += currentVert.printNeighbours() + lineSeparator;
		}

		return message;
	}
	// ------------------------------------------------------------------------------------------

}
