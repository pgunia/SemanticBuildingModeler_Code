package semantic.building.modeler.modelsynthesis.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import semantic.building.modeler.configurationservice.model.ExampleBasedFootprintConfiguration;
import semantic.building.modeler.configurationservice.model.enums.RuleType;
import semantic.building.modeler.math.Axis;
import semantic.building.modeler.math.MyPolygon;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.MyVectormath;
import semantic.building.modeler.math.Ray;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.modelsynthesis.algorithm.FaceComputation;
import semantic.building.modeler.modelsynthesis.algorithm.IDGenerator;
import semantic.building.modeler.modelsynthesis.algorithm.RuleComputation;
import semantic.building.modeler.modelsynthesis.algorithm.SynthesisProcessing;
import semantic.building.modeler.modelsynthesis.model.ComponentState;
import semantic.building.modeler.modelsynthesis.model.Face;
import semantic.building.modeler.modelsynthesis.model.FacePosition;
import semantic.building.modeler.modelsynthesis.model.ModelSynthesisBaseGeometry;
import semantic.building.modeler.modelsynthesis.model.RayWrapper;
import semantic.building.modeler.modelsynthesis.model.State;
import semantic.building.modeler.modelsynthesis.model.VertexWrapper;
import semantic.building.modeler.prototype.algorithm.FootprintCreator;

/**
 * Controllerklasse fuer die Model-Synthese
 * 
 * @author Patrick Gunia
 * 
 */

public class ModelSynthesisController {

	/** Logging-Instanz */
	protected final static Logger LOGGER = Logger
			.getLogger(ModelSynthesisController.class);

	/** Instanz der Mathebibliothek */
	private final MyVectormath mMathHelper = MyVectormath.getInstance();

	/** Eingabepolygon */
	private transient MyPolygon mInputPolygon = null;

	/** Liste mit gewrappten Inputstrahlen */
	private transient List<RayWrapper> mInputRays = null;

	/**
	 * Faktor, um den die Ausgangsstrahlen des Eingabepolygons skaliert werden,
	 * um darauf basierend die parallelen Strahlen zur erzeugen
	 */
	private final static float RAY_SCALE_FACTOR = 10000.0f;

	/**
	 * Liste mit allen Vertices, die als Schnittpunkte der parallelen Strahlen
	 * erzeugt wurden
	 */
	private transient List<VertexWrapper> mVertices = null;

	/**
	 * Liste mit allen Strahlsegmenten, die zwischen den Strahlschnittpunkten
	 * erzeugt wurden
	 */
	private transient List<RayWrapper> mRays = null;

	/**
	 * Liste mit allen parallelen Strahlen, die fuer das Eingabepolygon erzeugt
	 * wurden => kann man sich evtl. sparen, wenn man sowieso die
	 * Strahlensegmente verwaltet
	 */
	private transient List<RayWrapper> mParallelRays = null;

	/** Faceelemente, die basierend auf den parallelen Strahlen errechnet wurden */
	private transient List<Face> mFaces = null;

	/**
	 * Map speichert fuer jedes Label eine Menge gueltiger States, die direkt
	 * aus dem Eingabepolygon abgeleitet wurden
	 */
	private transient Map<String, List<ComponentState>> mLabelToStatesMap = null;

	/** Fuer Step-By-Step-Processing => fuer Release rausnehmen */
	private SynthesisProcessing processing = null;

	/**
	 * Map speichert die Zuordnung der Kantensteigung zu Labels => gleiche
	 * Steigung <=> gleiches Label
	 */
	private transient Map<Double, String> mSlopeToLabel = null;

	/** MApstruktur, die Labels zu ihren Referenzstrahlen zuordnet */
	private transient Map<String, ModelSynthesisBaseGeometry> mLabelToComponent = null;

	/**
	 * Mapstruktur speichert fuer jedes Strahlenlabel den Strahl, fuer den
	 * dieses erzeugt wurde
	 */
	private transient Map<String, Ray> mLabelToRay = null;

	/**
	 * Liste mit saemtlichen Grundrisspolygonen, die durch das Verfahren
	 * errechnet wurden
	 */
	private transient List<MyPolygon> mResultFootprints = null;

	/** Ergebnispolygon */
	private transient MyPolygon mResultFootprint = null;

	/** Konfigurationsdatei fuer aehnlichkeitsbasierte Grundrisserzeugung */
	private transient ExampleBasedFootprintConfiguration mSynthesisConfiguration = null;

	/**
	 * Basisvektor anhand dessen die Strahlen verschoben werden => dient bsw.
	 * dazu, beim separaten Debugging das Koordinatensystem zentriert
	 * darzustellen
	 */
	// MyVector3f baseVector = new MyVector3f(600.0f, 500.0f, 0.0f);
	private static MyVector3f mBaseVector = new MyVector3f(0.0f, 0.0f, 0.0f);

	// ------------------------------------------------------------------------------------------

	/**
	 * Klasse wird fuer die Berechnung der Strahlensegmente benoetigt, um die
	 * Schnittpunkte basierend auf ihren Abstaenden zu den Strahlenstartpunkten
	 * sortieren zu koennen
	 * 
	 * @author Patrick Gunia
	 * 
	 */
	private class IntersectionDistanceHelper {

		/** Schnittpunkt */
		private transient IntersectionWrapper mIntersection = null;

		/** Abstand zum Startpunkt des Basistrahls */
		private transient Float mDistance = null;

		// ------------------------------------------------------------------------------------------

		/**
		 * @param mIntersection
		 */
		public IntersectionDistanceHelper(
				final IntersectionWrapper mIntersection, RayWrapper ray) {
			super();
			this.mIntersection = mIntersection;
			this.mDistance = mMathHelper.calculatePointPointDistance(ray
					.getRay().getStartPtr(), this.mIntersection
					.getmIntersection());
		}

		// ------------------------------------------------------------------------------------------

		/**
		 * @return the mIntersection
		 */
		public MyVector3f getIntersection() {
			return mIntersection.getmIntersection();
		}

		// ------------------------------------------------------------------------------------------

		/**
		 * @return the mDistance
		 */
		public Float getDistance() {
			return mDistance;
		}

		// ------------------------------------------------------------------------------------------
		/**
		 * Methode liefert das Label des Schnittpunktes
		 * 
		 * @return Label
		 */
		public String getLabel() {
			return mIntersection.getmIntersectionLabel();
		}
		// ------------------------------------------------------------------------------------------

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Klasse modelliert Schnittpunkte zwischen Strahlen und speichert neben den
	 * Koordinaten auch das Label des Schnittpunktes
	 * 
	 * @author Patrick Gunia
	 * 
	 */
	private class IntersectionWrapper implements
			Comparable<IntersectionWrapper> {

		/** Eindeutiger Bezeichner der IntersectionWrapper-Instanz */
		private transient Integer mID = IDGenerator.getInstance().getID();

		/** Schnittpunktkoordinaten */
		private transient MyVector3f mIntersection = null;

		/**
		 * Label des Schnittpunktes => ergibt sich aus den Labels der
		 * beteiligten Strahlen
		 */
		private transient String mIntersectionLabel = null;

		// ------------------------------------------------------------------------------------------

		/**
		 * @param mIntersection
		 *            Schnittpunkt
		 * @param mIntersectionLabel
		 *            Label des Schnittpunkts
		 */
		public IntersectionWrapper(MyVector3f mIntersection,
				String mIntersectionLabel) {
			super();
			this.mIntersection = mIntersection;
			this.mIntersectionLabel = mIntersectionLabel;
		}

		// ------------------------------------------------------------------------------------------

		/**
		 * @return the mIntersection
		 */
		public MyVector3f getmIntersection() {
			return mIntersection;
		}

		// ------------------------------------------------------------------------------------------

		/**
		 * @return the mIntersectionLabel
		 */
		public String getmIntersectionLabel() {
			return mIntersectionLabel;
		}

		// ------------------------------------------------------------------------------------------

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((mIntersection == null) ? 0 : mIntersection.hashCode());
			return result;
		}

		// ------------------------------------------------------------------------------------------

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			IntersectionWrapper other = (IntersectionWrapper) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (mIntersection == null) {
				if (other.mIntersection != null)
					return false;
			} else if (!mIntersection.equals(other.mIntersection))
				return false;
			return true;
		}

		private ModelSynthesisController getOuterType() {
			return ModelSynthesisController.this;
		}

		// ------------------------------------------------------------------------------------------
		public int compareTo(IntersectionWrapper arg0) {
			return mID.compareTo(arg0.getID());
		}

		// ------------------------------------------------------------------------------------------

		/**
		 * @return the mID
		 */
		public Integer getID() {
			return mID;
		}
		// ------------------------------------------------------------------------------------------
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erzeugt fuer jede Kantensteigung innerhalb des Eingabepolygons
	 * ein Label und speichert diese in einer Map, die ueber die Steigung
	 * indiziert wird
	 */
	private void computeInputLabels() {

		assert mInputPolygon != null : "FEHLER: Es wurde noch kein Eingabepolygon gespeichert!";

		List<Ray> polyRays = mInputPolygon.getRays();
		Ray curRay = null;
		Double curSlope = null;
		mSlopeToLabel = new TreeMap<Double, String>();
		mLabelToRay = new TreeMap<String, Ray>();

		for (int i = 0; i < polyRays.size(); i++) {
			curRay = polyRays.get(i);
			curSlope = curRay.getXSlope();

			if (mSlopeToLabel.containsKey(curSlope))
				continue;
			else {
				String label = "r" + IDGenerator.getInstance().getLabelID();
				mSlopeToLabel.put(curSlope, label);

				mLabelToRay.put(label, curRay.clone());
				LOGGER.info("Label: " + label + " Strahl: " + curRay
						+ " Slope: " + curSlope);
			}
		}

		LOGGER.info("Es wurden " + mSlopeToLabel.size()
				+ " unterschiedliche Labels berechnet!");
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Comparator-Klasse fuer die Verwendung einer TreeMap fuer die Erzeugung
	 * der Strahlensegmente
	 */
	private class RayComparator implements Comparator<RayWrapper> {

		public int compare(RayWrapper o1, RayWrapper o2) {
			return o1.getID().compareTo(o2.getID());
		}
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Konstruktor mit Uebergabe eines Eingabepolygons fuer das der Algorithmus
	 * berechnet wird
	 * 
	 * @param inputPoly
	 *            Eingabepolygon
	 * @param conf
	 *            Konfigurationsobjekt mit Steuerparametern fuer die
	 *            Modellsynthese
	 */
	public ModelSynthesisController(final MyPolygon inputPoly,
			final ExampleBasedFootprintConfiguration conf) {
		mInputPolygon = inputPoly;
		mSynthesisConfiguration = conf;

		LOGGER.info("INPUT POLY CENTER: " + inputPoly.getCenter());

		// berechne steigungsabhaengige Strahlenlabels
		computeInputLabels();

		// berechne die moeglichen Statuszuweisungen fuer alle Kanten und
		// Vertices basierend auf dem Eingabepolygon
		computeRules();

		// printEdgeStatistics();
		initAlgorithmStructures();

		processing = new SynthesisProcessing(mVertices, mRays,
				mLabelToStatesMap, conf);
		// processing.nextStep(0);

		processing.compute();

		// berechne die Grundrisse basierend auf den berechneten Faces
		createFootprints();
		// testFacePoly();

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode initialisiert die Datenstrukturen, die vom Algorithmus benoetigt
	 * werden
	 */
	private void initDatastructures() {
		mVertices = new ArrayList<VertexWrapper>();
		mRays = new ArrayList<RayWrapper>();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Naechster Schritt in der Synthese
	 */
	public void nextStep() {
		if (processing != null)
			processing.nextStep(0);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode fuehrt die Berechnung der States durch, die spaeter fuer das
	 * Assignment benoetigt werden
	 */
	private void computeRules() {

		// initialisiere die Datenstrukturen
		initDatastructures();

		// berechne die gewrappten Ausgangsstrukturen fuer die Regelberechnung
		initWrappedStructures();

		boolean initialComputation = true;
		mParallelRays = computeParallelLines(initialComputation);

		LOGGER.info("#parallele Strahlen: " + mParallelRays.size());
		computeRayIntersections(initialComputation);

		printVertices();

		// FaceComputation faceComputation = new FaceComputation(mVertices,
		// mInputPolygon.getVertices().size());

		FaceComputation faceComputation = new FaceComputation(mVertices,
				mSlopeToLabel.size());
		mFaces = faceComputation.computeFaces();

		// Face-Positionen fuer Vertices berechnen
		addFacesToVertices();

		// Face-Positionen fuer Strahlen berechnen
		orientFacesForRays(mRays);

		LOGGER.info("Es wurden " + mFaces.size() + " Faces berechnet!");

		RuleComputation ruleComputation = new RuleComputation(mRays,
				mInputPolygon, mVertices, mLabelToComponent.keySet());
		// hole die berechneten Regeln
		mLabelToStatesMap = ruleComputation.computeRules();

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode ordnet die Faces innerhalb des Strahls ihren jeweiligen
	 * Positionen zu, also UPPER und LOWER. Dabei gilt die Festlegung: In
	 * Strahlenrichtung linkes Face wird auf Position "UPPER" gesetzt, in
	 * Strahlenrichtung rechtes Face auf Position "LOWER"
	 * 
	 * @param rays
	 *            Liste mit Strahlen, fuer die diese Zuordnung berechnet werden
	 *            soll
	 */
	private void orientFacesForRays(final List<RayWrapper> rays) {
		// durchlaufe nun alle Strahlen und orientiere die Faces
		for (int i = 0; i < rays.size(); i++) {
			try {
				orientFacesForRay(rays.get(i), mLabelToComponent);
			} catch (AssertionError e) {
				LOGGER.error(e);
				continue;
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode durchlaeuft saemtliche Vertices und speichert fuer jedes Vertex
	 * die Position seiner adjazenten Faces
	 */
	private void addFacesToVertices() {

		for (int i = 0; i < mVertices.size(); i++) {
			try {
				mVertices.get(i).computeFacePositions(mLabelToComponent);
			} catch (AssertionError e) {
				LOGGER.error(e);
				continue;
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode fasst saemtliche erforderlichen Schritt fuer die Berechnungen der
	 * initialen Strukturen fuer die Modellsynthese zusammen
	 */
	private void initAlgorithmStructures() {

		// initialisiere die Datenstrukturen
		initDatastructures();

		boolean initialComputation = false;

		// jetzt Menge paralleler Strahlen in allen Richtungen berechnen
		mParallelRays = computeParallelLines(initialComputation);

		// Schnittpunkte berechnen
		computeRayIntersections(initialComputation);

		// Faces berechnen und ausrichten
		FaceComputation faceComputation = new FaceComputation(mVertices,
				mInputPolygon.getVertices().size());

		// FaceComputation faceComputation = new FaceComputation(mVertices,
		// mSlopeToLabel.size());
		mFaces = faceComputation.computeFaces();
		LOGGER.info("#FACE: " + mFaces.size());

		try {
			// Face-Positionen fuer Vertices berechnen
			addFacesToVertices();

			// Face-Positionen fuer Strahlen berechnen
			orientFacesForRays(mRays);
		} catch (AssertionError e) {
			LOGGER.error(e);
			LOGGER.error("Fehler bei der Zuordnung von Faces zu Grundkomponenten...Berechnung wird erneut angestossen.");

			// wenn bei der Berechnung ein Fehler aufgetreteten ist, dann liegt
			// dieser wahrscheinlich in der Faceberechnung und -zurodnung
			// versuche es in diesem Fall erneut
			initAlgorithmStructures();
		}

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Bezeichner fuer Eingabestrahlen und Vertices erzeugen
	 */
	private void initWrappedStructures() {

		List<Vertex3d> inputPolyVerts = mInputPolygon.getVertices();
		List<Ray> inputPolyRays = mInputPolygon.getRays();

		List<VertexWrapper> inputVertices = new ArrayList<VertexWrapper>(
				inputPolyVerts.size());
		Ray curRay = null, prevRay = null;
		mLabelToComponent = new HashMap<String, ModelSynthesisBaseGeometry>(
				mSlopeToLabel.size());

		for (int i = 0; i < inputPolyVerts.size(); i++) {

			// das Vertexlabel ergibt sich aus den Labels der adjazenten
			// Strahlen
			curRay = inputPolyRays.get(i);
			int prevIndex = i - 1;
			if (prevIndex < 0)
				prevIndex = inputPolyVerts.size() - 1;
			prevRay = inputPolyRays.get(prevIndex);

			// Hole die Labels der Strahlen
			String curRayLabel = mSlopeToLabel.get(curRay.getXSlope());
			assert curRayLabel != null : "FEHLER: Fuer Strahl " + curRay
					+ " existiert noch kein Label!";

			String prevLabel = mSlopeToLabel.get(prevRay.getXSlope());
			assert prevLabel != null : "FEHLER: Fuer Strahl " + prevRay
					+ " existiert noch kein Label!";

			String label = getNormalisedIntersectionLabel(curRayLabel,
					prevLabel);
			VertexWrapper inputVert = new VertexWrapper(State.UNDEFINED, label,
					inputPolyVerts.get(i));
			inputVertices.add(inputVert);

			// in Map ablegen
			mLabelToComponent.put(label, inputVert);
		}
		/*
		 * String msg = "IDs der Eingabevertices: "; for(int i = 0; i <
		 * mInputVertices.size(); i++) msg += mInputVertices.get(i).getID() +
		 * " "; LOGGER.info(msg);
		 */
		// gewrappte Strahlen erstellen
		mInputRays = new ArrayList<RayWrapper>(inputPolyRays.size());
		for (int i = 0; i < inputPolyRays.size(); i++) {
			RayWrapper inputRay = new RayWrapper(State.UNDEFINED,
					mSlopeToLabel.get(inputPolyRays.get(i).getXSlope()),
					inputVertices.get(i), inputVertices.get((i + 1)
							% inputPolyVerts.size()));
			mInputRays.add(inputRay);

			// HIER NOCHMAL SCHAUEN
			mLabelToComponent.put(inputRay.getLabel(), inputRay);
		}

		// for(int i = 0; i < mInputRays.size(); i++)
		// LOGGER.info(mInputRays.get(i).getLabel() + ": " +
		// mInputRays.get(i).getRay());

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet die Schnittpunkte der parallelen Strahlen, die fuer das
	 * Ausgangspolygon ermittelt wurden und erzeugt dabei sowohl Vertices als
	 * auch Kantensegmente zwischen diesen
	 * 
	 * @param initialComputation
	 *            Handelt es sich um den ersten Durchlauf zur Bestimmung der
	 *            Regeln?
	 */
	private void computeRayIntersections(final boolean initialComputation) {

		RayWrapper currentWrRay = null, currentWrTestRay = null;
		Ray currentStartRay = null, currentTestRay = null;
		MyVector3f intersection = null;

		// fuer jeden parallelen Strahl wird eine Liste aller Schnittpunkte
		// dieses Strahls verwaltet, diese wird in einer TreeMap gespeichert
		// dadurch wird eine feste Reihenfolge der Strahlen garantiert, so dass
		// das Ergebnis deterministisch ist
		Map<RayWrapper, Set<IntersectionWrapper>> rayIntersections = new TreeMap<RayWrapper, Set<IntersectionWrapper>>(
				new RayComparator());
		Set<IntersectionWrapper> currentStartIntersectionList = null, currentTestIntersectionList = null;

		int numberOfIntersections = 0;

		// berechne die Schnittpunkte, speichere diese in einer Liste fuer jeden
		// vorhandenen Strahl
		for (int i = 0; i < mParallelRays.size(); i++) {
			currentWrRay = mParallelRays.get(i);

			LOGGER.trace("Strahl: " + currentWrRay.getID() + ": "
					+ currentWrRay.getRay());

			currentStartRay = currentWrRay.getRay();
			for (int j = i + 1; j < mParallelRays.size(); j++) {

				currentWrTestRay = mParallelRays.get(j);
				currentTestRay = currentWrTestRay.getRay();

				// berechne das Label des Vertex am Schnittpunkt
				String label = getNormalisedIntersectionLabel(
						currentWrRay.getLabel(), currentWrTestRay.getLabel());
				intersection = mMathHelper
						.calculateRay2RayIntersectionApproximation(
								currentStartRay, currentTestRay);
				if (intersection == null) {
					continue;
				}

				// befindet sich der Schnittpunkt auf beiden Strahlensegmenten
				// => sonst abbrechen?
				if (mMathHelper.isPointOnLineSegment(intersection,
						currentStartRay)
						&& mMathHelper.isPointOnLineSegment(intersection,
								currentTestRay)) {

					intersection = mMathHelper.roundVector3f(intersection,
							1000.0f);
					numberOfIntersections++;

					// fuege den Schnittpunkt zu den Listen beider beteiligten
					// Strahlen hinzu
					currentStartIntersectionList = rayIntersections
							.get(currentWrRay);
					if (currentStartIntersectionList == null)
						currentStartIntersectionList = new TreeSet<IntersectionWrapper>();
					currentStartIntersectionList.add(new IntersectionWrapper(
							intersection, label));
					rayIntersections.put(currentWrRay,
							currentStartIntersectionList);

					currentTestIntersectionList = rayIntersections
							.get(currentWrTestRay);
					if (currentTestIntersectionList == null)
						currentTestIntersectionList = new TreeSet<IntersectionWrapper>();
					currentTestIntersectionList.add(new IntersectionWrapper(
							intersection, label));
					rayIntersections.put(currentWrTestRay,
							currentTestIntersectionList);
				}
			}
		}

		LOGGER.info("Es wurden " + numberOfIntersections
				+ " Schnittpunkte berechnet.");

		// erzeuge basierend auf den Schnittpunktlisten Strahlensegmente auf den
		// Strahlen
		Iterator<RayWrapper> rayIter = rayIntersections.keySet().iterator();
		while (rayIter.hasNext()) {
			currentWrRay = rayIter.next();
			computeRaySegments(currentWrRay,
					rayIntersections.get(currentWrRay), initialComputation);
		}
		LOGGER.info("Es wurden " + mVertices.size() + " Vertices und "
				+ mRays.size() + " Strahlensegmente erzeugt!");
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode erzeugt ein standardisiertes Label aus den beiden
	 * Uebergabelabels. Dabei wird die Konkatenierung derart vorgenommen, dass
	 * das kleinnere Label (kleinere Zahl) immer vorne kommt
	 * 
	 * @param label1
	 *            Label des ersten Strahls
	 * @param label2
	 *            Label des zweiten Strahls
	 * @return Normalisierte Konkatenierung der beiden Labels
	 */
	private String getNormalisedIntersectionLabel(final String label1,
			final String label2) {

		assert label1 != null && label2 != null : "FEHLER: Es wurden keine gueltigen Labels uebergeben!";
		if (label1.compareTo(label2) >= 0)
			return label2 + "_" + label1;
		else
			return label1 + "_" + label2;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode sortiert zunaechst die Schnittpunkte aufgrund ihrer Entfernung
	 * zum Startpunkt. Anschliessend wird fuer je zwei aufeinanderfolgende
	 * Schnittpunkte ein Strahlsegmente berechnet
	 * 
	 * @param ray
	 *            Strahl, auf dem die Schnittpunkte liegen
	 * @param intersections
	 *            Schnittpunkte des Uebergabestrahls mit allen anderen Strahlen
	 * @param initialComputation
	 *            Flag gibt an, ob es sich um den Regelberechnungsdurchlauf
	 *            handelt
	 */
	private void computeRaySegments(final RayWrapper ray,
			final Set<IntersectionWrapper> intersections,
			final boolean initialComputation) {

		assert ray != null : "FEHLER: Uebergebener Strahl ist null!";
		assert intersections != null & !intersections.isEmpty() : "FEHLER: Keine Intersections uebergeben";

		// erzeuge eine Liste von Helper-Instanzen, um die Comparator-Funktionen
		// nutzen zu koennen
		List<IntersectionDistanceHelper> intersectionHelpers = new ArrayList<IntersectionDistanceHelper>(
				intersections.size());
		LOGGER.trace("Strahl " + ray.getID() + " :" + ray.getRay()
				+ " #Schnittpunkte: " + intersections.size());

		Iterator<IntersectionWrapper> intersectionIter = intersections
				.iterator();
		IntersectionWrapper curWr = null;
		while (intersectionIter.hasNext()) {
			curWr = intersectionIter.next();
			intersectionHelpers.add(new IntersectionDistanceHelper(curWr, ray));
			LOGGER.trace(curWr.getmIntersection());
		}

		// sortiere die Schnittpunkte
		sortIntersectionsByDistanceToRayStart(intersectionHelpers);
		// fuege Start und Endpunkt dazu => diese beschreiben ebenfalls
		// Strahlensegmente
		// die zugehoerigen Vertices befinden sich bereits im Vertex-Array =>
		// diese wurden bei der Erstellung der parallelen Strahlen gesetzt
		// darum MUESSEN die zugehoerigen Vertices auch wiederverwendet werden
		// => somit ist die Angabe eines Lables nicht erforderlich
		// sollte es zu Fehlern kommen, kann dies ein Indiz sein
		intersectionHelpers.add(0, new IntersectionDistanceHelper(
				new IntersectionWrapper(ray.getRay().getStart(), "TEST"), ray));
		intersectionHelpers.add(intersectionHelpers.size(),
				new IntersectionDistanceHelper(new IntersectionWrapper(ray
						.getRay().getEnd(), "TESTEND"), ray));

		// erzeuge VertesWrapper- und Ray-Wrapper-Instanzen
		MyVector3f currentIntersection = null, nextIntersection = null;
		RayWrapper currentRayWrapper = null;
		VertexWrapper startVert = null, endVert = null;

		LOGGER.trace("#Anzahl Punkte auf dem Strahl: "
				+ intersectionHelpers.size() + " Verarbeite Strahl: "
				+ ray.getID() + ": " + ray.getRay());
		for (int i = 0; i < intersectionHelpers.size() - 1; i++) {

			// wenn Vertices auf dem Weg blind sind, skippe sie
			// if(intersectionHelpers.get(i).isBlind() ||
			// intersectionHelpers.get(i + 1).isBlind()) continue;
			currentIntersection = intersectionHelpers.get(i).getIntersection();
			nextIntersection = intersectionHelpers.get(i + 1).getIntersection();

			// erzeuge Vertices aus den Schnittpunkten
			startVert = new VertexWrapper(State.UNDEFINED, intersectionHelpers
					.get(i).getLabel(), new Vertex3d(currentIntersection));

			int index = mVertices.indexOf(startVert);
			if (index != -1) {
				startVert = mVertices.get(index);
			} else
				mVertices.add(startVert);

			endVert = new VertexWrapper(State.UNDEFINED, intersectionHelpers
					.get(i + 1).getLabel(), new Vertex3d(nextIntersection));
			index = mVertices.indexOf(endVert);
			if (index != -1) {
				endVert = mVertices.get(index);

			} else
				mVertices.add(endVert);

			// erzeuge ein Strahlensegment basierend auf den erzeugten Vertices
			currentRayWrapper = new RayWrapper(State.UNDEFINED, ray.getLabel(),
					startVert, endVert);

			// teste, ob das Strahlensegment bereits vorkommt
			if (!mRays.contains(currentRayWrapper))
				mRays.add(currentRayWrapper);
			if (currentRayWrapper.getRay().getLength() == 0.0f) {
				LOGGER.trace(i + ": Erzeuge Strahl: "
						+ currentRayWrapper.getID() + " fuer Verts: "
						+ startVert.getID() + " End: " + endVert.getID()
						+ " Label: " + intersectionHelpers.get(i).getLabel());
			}

			// aktualisiere die Vertices
			startVert.addRayStart(currentRayWrapper);
			endVert.addRayEnd(currentRayWrapper);
		}

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode sortiert die uebergebenen DistanceHelper-Instanzen basierend auf
	 * ihrer Entfernung zum Startpunkt des Strahls, auf dem sie liegen
	 * 
	 * @param intersections
	 *            Liste mit gewrappten Schnittpunkten
	 */
	private void sortIntersectionsByDistanceToRayStart(
			final List<IntersectionDistanceHelper> intersections) {

		Collections.sort(intersections,
				new Comparator<IntersectionDistanceHelper>() {

					public int compare(IntersectionDistanceHelper arg0,
							IntersectionDistanceHelper arg1) {
						return arg0.getDistance().compareTo(arg1.getDistance());
					}
				});
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode erzeugt die Ausgangsstruktur fuer die Modelsynthese, indem fuer
	 * saemtliche Kanten innerhalb des Eingabepolygons eine Menge paralleler
	 * Strahlen berechnet wird
	 * 
	 * @param initialComputation
	 *            Flag gibt an, ob es sich um die initiale Berechnung handelt,
	 *            die fuer die Berechnung der Regeln benutzt wird
	 */
	private List<RayWrapper> computeParallelLines(boolean initialComputation) {

		List<RayWrapper> resultRays = null;
		if (initialComputation)
			resultRays = new ArrayList<RayWrapper>();
		else
			resultRays = new ArrayList<RayWrapper>();

		// wenn es sich um die Regelberechnung handelt, verwende die
		// Eingabestrahlen und berechne basierend auf diesen eine Menge
		// paralleler Strahlen
		if (initialComputation) {
			RayWrapper curRay = null;
			for (int i = 0; i < mInputRays.size(); i++) {
				curRay = mInputRays.get(i);
				resultRays.addAll(createParallelRaysForInputRayInitial(curRay));
			}
		}
		// sonst erzeuge fuer jedes Ray-Label eine Menge paralleler Strahlen
		else {
			Iterator<String> labelIter = mLabelToRay.keySet().iterator();
			String curLabel = null;
			Ray jitteredRay = null, curRay = null;
			Random rand = new Random();

			// Basisvektor wird auf mit den zufallsbasierten Modifikationen
			// kombiniert => dadurch erscheint das Koordinatensystem mittig

			// erzeuge ein gleichmaeissiges Raster basierend auf dem Abstand der
			// parallelen Strahlen zueinander
			// dabei wird der Raum zwischen den parallelen Strahlen mit den
			// Stratstrahlen fuer die Parallel-Ray-Berechnung gefuellt
			int numberOfDifferentLabels = mLabelToRay.keySet().size();

			// verschiebe zufallsbasiert => dadurch kann bei wiederholter
			// Berechnung die Wahrscheinlichkeit erhoeht werden, dass die
			// Berechnung durchlaeuft
			MyVector3f stuetzTranslation = new MyVector3f(
					(mSynthesisConfiguration.getParallelRayDistance() / numberOfDifferentLabels)
							+ rand.nextFloat() * 3,
					0.0f,
					(mSynthesisConfiguration.getParallelRayDistance() / numberOfDifferentLabels)
							+ rand.nextFloat() * 5);

			int curLabelIndex = 0;
			while (labelIter.hasNext()) {
				curLabel = labelIter.next();
				curRay = mLabelToRay.get(curLabel);

				// der Stuetzvektor entsteht durch eine Verschiebung des
				// Ausgangsvektors um den aktuellen Index
				// dadurch kollidieren Strahlen nicht und das Basisraster
				// zeichnet sich durch eine hohe Gleichmaessigkeit aus
				MyVector3f stuetzvektor = stuetzTranslation.clone();
				stuetzvektor.scale(curLabelIndex);
				stuetzvektor.add(mBaseVector);
				jitteredRay = new Ray(stuetzvektor, curRay.getDirection());

				// resultRays.addAll(createParallelRaysForInputRay(mLabelToRay.get(curLabel),
				// curLabel, NUMBER_OF_PARALLEL_RAYS,
				// SynthesisConfiguration.getInstance().getParallellRaysDistance()));
				resultRays.addAll(createParallelRaysForInputRay(jitteredRay,
						curLabel));
				++curLabelIndex;
			}
		}
		return resultRays;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode erzeugt fur den uebergebenen Strahl eine Menge paralleler
	 * Strahlen mit festem Abstand zueinander. Diese Methode wird fuer die
	 * Berechnung paralleler Strahlen im Kontext der Regelerzeugung eingesetzt.
	 * 
	 * @param inputRay
	 *            RayWrapper-Instanz, fuer die parallele Strahlen erzeugt werden
	 * @return Liste mit allen parallelen Strahlen, die innerhalb der Methode
	 *         erzeugt wurden
	 */
	private List<RayWrapper> createParallelRaysForInputRayInitial(
			final RayWrapper inputRay) {

		final int numberOfRaysForInitialComputation = 3;
		final int numberOfRaysPerDirection = numberOfRaysForInitialComputation / 2;

		// skaliere den Eingabestrahl => durch die Normalisierung der Strahlen
		// besitzen saemtliche Strahlen die gleiche Laenge
		final Ray scaledRay = inputRay.getRay().clone();

		// Strahlen werden nicht normalisiert, um Floating-Point-Ungenauigkeiten
		// zu reduzieren, darum werden sie hier auf eine einheitliche Laenge
		// gebracht
		// verwende ausserdem ganzzahlige Vielfache, um eine geringere
		// Ungenauigkeit zu erreichen
		float scaleFactor = (float) Math.floor(RAY_SCALE_FACTOR
				/ scaledRay.getLength());
		scaledRay.scale(scaleFactor);

		final MyVector3f orthogonalVector = mMathHelper
				.calculateOrthogonalVectorWithSamePlane(inputRay.getRay()
						.getDirection(), mInputPolygon.getNormal());
		final float rayDistance = mSynthesisConfiguration
				.getParallelRayDistanceRuleComputation();

		final String label = inputRay.getLabel();

		MyVector3f newPosition = null, translation = null;
		VertexWrapper rayStart = null, rayEnd = null;
		List<RayWrapper> resultRays = new ArrayList<RayWrapper>(
				numberOfRaysForInitialComputation + 1);

		// erzeuge verschobene Strahlversionen, speichere jeweils den
		// Elternstrahl in den erzeugten Wrapper-Instanzen
		// beginne mit Index 0, um auch den Ursprungsstrahl anzulegen
		for (int i = 0; i < numberOfRaysPerDirection + 1; i++) {
			newPosition = scaledRay.getStart();
			translation = orthogonalVector.clone();
			translation.scale(i * rayDistance);
			newPosition.add(translation);

			// erzeuge gewrappte Vertices fuer die Strahlenendpunkte
			rayStart = new VertexWrapper(State.UNDEFINED, label + "_Start",
					new Vertex3d(newPosition));
			LOGGER.trace("Adde Start: " + rayStart.getID() + " Label: "
					+ rayStart.getLabel() + " Pos: "
					+ rayStart.getVertex().getPositionPtr());
			mVertices.add(rayStart);

			MyVector3f endPosition = new MyVector3f();
			endPosition.add(newPosition, scaledRay.getDirection());
			rayEnd = new VertexWrapper(State.UNDEFINED, label + "_End",
					new Vertex3d(endPosition));
			LOGGER.trace("Adde End: " + rayEnd.getID() + " Label: "
					+ rayEnd.getLabel() + " Pos: "
					+ rayEnd.getVertex().getPositionPtr());

			mVertices.add(rayEnd);
			resultRays.add(new RayWrapper(State.UNDEFINED, label, rayStart,
					rayEnd, inputRay));
		}

		// das gleiche noch einmal in der anderen Richtung
		orthogonalVector.scale(-1.0f);

		// erzeuge verschobene Strahlversionen
		for (int i = 1; i < numberOfRaysPerDirection + 1; i++) {
			newPosition = scaledRay.getStart();
			translation = orthogonalVector.clone();
			translation.scale(i * rayDistance);
			newPosition.add(translation);

			// erzeuge gewrappte Vertices fuer die Strahlenendpunkte
			rayStart = new VertexWrapper(State.UNDEFINED, label + "_Start",
					new Vertex3d(newPosition));
			LOGGER.trace("Adde Start: " + rayStart.getID() + " Label: "
					+ rayStart.getLabel() + " Pos: "
					+ rayStart.getVertex().getPositionPtr());
			mVertices.add(rayStart);

			MyVector3f endPosition = new MyVector3f();
			endPosition.add(newPosition, scaledRay.getDirection());
			rayEnd = new VertexWrapper(State.UNDEFINED, label + "_End",
					new Vertex3d(endPosition));
			LOGGER.trace("Adde End: " + rayEnd.getID() + " Label: "
					+ rayEnd.getLabel() + " Pos: "
					+ rayEnd.getVertex().getPositionPtr());

			mVertices.add(rayEnd);
			LOGGER.trace("START: " + newPosition + " END: " + endPosition);
			resultRays.add(new RayWrapper(State.UNDEFINED, label, rayStart,
					rayEnd, inputRay));
		}
		return resultRays;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode erzeugt fur den uebergebenen Strahl eine Menge paralleler
	 * Strahlen mit festem Abstand zueinander. Diese Methode wird innerhalb der
	 * Hauptberechnung eingesetzt
	 * 
	 * @param inputRay
	 *            Eingabestrahl
	 * @param label
	 *            Label des Eingabestrahls
	 * @return Liste mit allen parallelen Strahlen, die innerhalb der Methode
	 *         erzeugt wurden
	 */
	private List<RayWrapper> createParallelRaysForInputRay(final Ray inputRay,
			final String label) {

		// skaliere den Eingabestrahl => durch die Normalisierung der Strahlen
		// besitzen saemtliche Strahlen die gleiche Laenge
		final Ray scaledRay = inputRay.clone();
		// Strahlen werden nicht normalisiert, um Floating-Point-Ungenauigkeiten
		// zu reduzieren, darum werden sie hier auf eine einheitliche Laenge
		// gebracht
		// verwende ausserdem ganzzahlige Vielfache, um eine geringere
		// Ungenauigkeit zu erreichen
		float scaleFactor = (float) Math.floor(RAY_SCALE_FACTOR
				/ scaledRay.getLength());
		scaledRay.scale(scaleFactor);

		final int numberOfRaysPerDirection = mSynthesisConfiguration
				.getNumberOfParallelRays() / 2;
		final MyVector3f orthogonalVector = mMathHelper
				.calculateOrthogonalVectorWithSamePlane(
						inputRay.getDirection(), mInputPolygon.getNormal());
		final float rayDistance = mSynthesisConfiguration
				.getParallelRayDistance();

		MyVector3f newPosition = null, translation = null;
		VertexWrapper rayStart = null, rayEnd = null;

		List<RayWrapper> resultRays = new ArrayList<RayWrapper>(
				mSynthesisConfiguration.getNumberOfParallelRays());

		// erzeuge verschobene Strahlversionen, speichere jeweils den
		// Elternstrahl in den erzeugten Wrapper-Instanzen
		// beginne mit Index 0, um auch den Ursprungsstrahl anzulegen
		for (int i = 0; i < numberOfRaysPerDirection + 1; i++) {
			newPosition = scaledRay.getStart();
			translation = orthogonalVector.clone();
			translation.scale(i * rayDistance);
			newPosition.add(translation);

			// erzeuge gewrappte Vertices fuer die Strahlenendpunkte
			rayStart = new VertexWrapper(State.UNDEFINED, label + "_Start",
					new Vertex3d(newPosition));
			LOGGER.trace("Adde Start: " + rayStart.getID() + " Label: "
					+ rayStart.getLabel() + " Pos: "
					+ rayStart.getVertex().getPositionPtr());
			mVertices.add(rayStart);

			MyVector3f endPosition = new MyVector3f();
			endPosition.add(newPosition, scaledRay.getDirection());
			rayEnd = new VertexWrapper(State.UNDEFINED, label + "_End",
					new Vertex3d(endPosition));
			LOGGER.trace("Adde End: " + rayEnd.getID() + " Label: "
					+ rayEnd.getLabel() + " Pos: "
					+ rayEnd.getVertex().getPositionPtr());

			mVertices.add(rayEnd);
			resultRays.add(new RayWrapper(State.UNDEFINED, label, rayStart,
					rayEnd));
		}

		// das gleiche noch einmal in der anderen Richtung
		orthogonalVector.scale(-1.0f);

		// erzeuge verschobene Strahlversionen
		for (int i = 1; i < numberOfRaysPerDirection + 1; i++) {
			newPosition = scaledRay.getStart();
			translation = orthogonalVector.clone();
			translation.scale(i * rayDistance);
			newPosition.add(translation);

			// erzeuge gewrappte Vertices fuer die Strahlenendpunkte
			rayStart = new VertexWrapper(State.UNDEFINED, label + "_Start",
					new Vertex3d(newPosition));
			LOGGER.trace("Adde Start: " + rayStart.getID() + " Label: "
					+ rayStart.getLabel() + " Pos: "
					+ rayStart.getVertex().getPositionPtr());
			mVertices.add(rayStart);

			MyVector3f endPosition = new MyVector3f();
			endPosition.add(newPosition, scaledRay.getDirection());
			rayEnd = new VertexWrapper(State.UNDEFINED, label + "_End",
					new Vertex3d(endPosition));
			LOGGER.trace("Adde End: " + rayEnd.getID() + " Label: "
					+ rayEnd.getLabel() + " Pos: "
					+ rayEnd.getVertex().getPositionPtr());

			mVertices.add(rayEnd);
			LOGGER.trace("START: " + newPosition + " END: " + endPosition);
			resultRays.add(new RayWrapper(State.UNDEFINED, label, rayStart,
					rayEnd));
		}
		return resultRays;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mParallelRays
	 */
	public List<RayWrapper> getParallelRays() {
		return mParallelRays;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mVertices
	 */
	public List<VertexWrapper> getVertices() {
		return mVertices;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mFaces
	 */
	public List<Face> getFaces() {
		return mFaces;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mRays
	 */
	public List<RayWrapper> getRays() {
		return mRays;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode speichert die Position der Faces in der Face-Map-Struktur
	 * 
	 * @param cur
	 *            Strahl, der aktuell orientiert wird
	 * @param labelToComponent
	 *            Mapstruktur, die den Komponentenlabels ihre Referenzstrahlen
	 *            zuordnet
	 */
	private void orientFacesForRay(final RayWrapper cur,
			final Map<String, ModelSynthesisBaseGeometry> labelToComponent) {

		// wenn keine adjazenten Faces vorhanden sind, kann man sich die
		// Berechnung sparen
		int numberOfAdjacentFaces = cur.getNumberOfAdjacentFaces();
		if (numberOfAdjacentFaces == 0)
			return;

		// erstelle Projektionen fuer den Start- und den Endpunkt des Strahls
		// sowie fuer die Mittelpunkte der adjazenten Faces
		// Bezeichner richten sich nach der initialen Zuweisung
		Vertex3d projectedCurFaceCenter = null;

		// hole den Referenzstrahl, um eine einheitliche Orientierung zu
		// garantieren
		RayWrapper refRay = (RayWrapper) labelToComponent.get(cur.getLabel());

		// erzeuge einen neuen Strahl mit der Richtung des Referenzstrahls und
		// dem Start des eigentlichen Strahls
		Ray testRay = new Ray(cur.getStartVert().getVertex().getPosition(),
				refRay.getRay().getDirection());

		Vertex3d projectedRayStart = mMathHelper
				.createXYPlaneProjectionForPoint(testRay.getStartVertex(),
						Axis.Y);
		Vertex3d projectedRayEnd = mMathHelper.createXYPlaneProjectionForPoint(
				testRay.getEndVertex(), Axis.Y);

		Face[] faces = cur.getAdjacentFaces().toArray(new Face[2]);
		Face curFace = null;
		for (int i = 0; i < faces.length; i++) {
			curFace = faces[i];
			if (curFace != null) {
				Vertex3d curFaceCenter = new Vertex3d(curFace.getFacePoly()
						.getCenter());
				projectedCurFaceCenter = mMathHelper
						.createXYPlaneProjectionForPoint(curFaceCenter, Axis.Y);

				// es muss nur dann etwas getan werden, wenn das linke Face
				// nicht links liegt! Tausche in diesem Fall
				if (mMathHelper.isLeft(projectedRayStart, projectedRayEnd,
						projectedCurFaceCenter)) {

					// es existiert bereits eine Zuordnung auf der Position =>
					// dies deutet auf einen Fehler in der Faceberechnung hin
					// verwende das kleinere der Faces
					if (cur.getFace(FacePosition.UPPER) != null) {

						Face oldFace = cur.getFace(FacePosition.UPPER);
						if (mMathHelper.calculatePolygonArea2D(oldFace
								.getFacePoly().getVertices()) < mMathHelper
								.calculatePolygonArea2D(curFace.getFacePoly()
										.getVertices())) {
							cur.setFace(FacePosition.UPPER, curFace);
							LOGGER.error("Vorhandenes Face wird durch Polygon mit kleinerem Flaecheninhalt ersetzt!");
						}
					}
					// wenn noch keine Zuordnung existiert, ist alles in Ordnung
					// => Face zuweisen
					else {
						cur.setFace(FacePosition.UPPER, curFace);
					}

				} else {

					// existiert bereits eine Zuordnung auf der LOWER-Position?
					if (cur.getFace(FacePosition.LOWER) != null) {
						Face oldFace = cur.getFace(FacePosition.LOWER);
						if (mMathHelper.calculatePolygonArea2D(oldFace
								.getFacePoly().getVertices()) < mMathHelper
								.calculatePolygonArea2D(curFace.getFacePoly()
										.getVertices())) {
							cur.setFace(FacePosition.LOWER, curFace);
							LOGGER.error("Vorhandenes Face wird durch Polygon mit kleinerem Flaecheninhalt ersetzt!");
						}
					}
					// wenn nicht, dann einfach setzen
					else {
						cur.setFace(FacePosition.LOWER, curFace);
					}
				}
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * @return the mInputRays
	 */
	public List<RayWrapper> getInputRays() {
		return mInputRays;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erzeugt basierend auf allen Faces, die als INDOOR gekennzeichnet
	 * wurden, ein Polygon, das den jeweiligen Footprint beschreibt
	 */
	private void createFootprints() {

		// erzeuge eine Instanz der Creatorklasse
		final FootprintCreator creator = new FootprintCreator();

		// durchlaufe alle Faces und adde alle Polygone zum Creator, die als
		// INSIDE gekennzeichnet sind
		Face curFace = null;
		int numberOfInteriors = 0;
		MyPolygon curPoly = null;
		MyVector3f negativeYAxis = new MyVector3f(0.0f, -1.0f, 0.0f);

		for (int i = 0; i < mFaces.size(); i++) {
			curFace = mFaces.get(i);
			if (curFace.getState() == State.INTERIOR) {
				curPoly = curFace.getFacePoly();

				// wenn der Normalenvektor des Polygons nicht in Richtung der
				// positiven y-Achse zeigt, drehe die Vertexorder
				if (!curPoly.getNormal().equalsWithinTolerance(negativeYAxis)) {
					curPoly.switchVertexOrder();
					LOGGER.trace("Poly-Normal nach Vertexorder-Switch: "
							+ curPoly.getNormalPtr());
				}

				numberOfInteriors++;
				creator.addComponent(curPoly);
			}
		}

		LOGGER.info("Insgesamt wurden " + numberOfInteriors
				+ " INTERIOR-Faces zum Footprint-Creator hinzugefuegt.");

		boolean useConvexHull = false;
		final List<List<Vertex3d>> resultFootprints = creator
				.process(useConvexHull);

		// erzeuge fuer jede Vertexliste ein Polygon
		mResultFootprints = new ArrayList<MyPolygon>(resultFootprints.size());
		for (int i = 0; i < resultFootprints.size(); i++) {
			List<Vertex3d> curList = resultFootprints.get(i);
			mResultFootprints.add(new MyPolygon(curList));
		}

		LOGGER.info("Insgesamt wurden " + mResultFootprints.size()
				+ " Grundriss(e) durch die Modellsynthese erstellt!");
		assert mResultFootprints.size() > 0 : "FEHLER: Es konnten keine Ergebnisgrundrisse extrahiert werden!";

		if (mResultFootprints.size() > 1) {
			chooseResultFootprint();
		} else {
			mResultFootprint = mResultFootprints.get(0);
		}
		fitFootprint();

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode skaliert den als Ergebnis berechneten Grundriss und richtet ihn
	 * anschliessend anhand des Quellgrundrisses aus
	 */
	private void fitFootprint() {

		// skaliere zunaechst den Ergebnisgrundriss, so dass die Flaecheninhalte
		// aehnlich sind
		final MyVectormath mathHelper = MyVectormath.getInstance();

		LOGGER.trace("INPUT-POLY: " + mInputPolygon);

		final float areaInput = mathHelper.computePolygonArea(mInputPolygon
				.getVertices());
		final float areaResult = mathHelper.computePolygonArea(mResultFootprint
				.getVertices());

		float ratio = areaInput / areaResult;
		ratio = (float) Math.sqrt(ratio);

		LOGGER.trace("Scaling Result-Footprint: Input: " + areaInput
				+ " Result: " + areaResult + " Ratio: " + ratio);
		mResultFootprint.scale(ratio);

		float testAreaResult = mathHelper.computePolygonArea(mResultFootprint
				.getVertices());
		LOGGER.trace("Neuer Flaecheninhalt: " + testAreaResult);

		// richte den Ergebnisgrundriss derart aus, dass die Mittelpunkte
		// identisch sind
		final MyVector3f inputCenter = mInputPolygon.getCenter();
		final MyVector3f resultCenter = mResultFootprint.getCenter();

		final MyVector3f diff = new MyVector3f();
		diff.sub(inputCenter, resultCenter);
		LOGGER.trace("Input-Center: " + inputCenter + " Result-Center: "
				+ resultCenter + " Diff: " + diff);

		mResultFootprint.translate(diff);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode waehlt im Fall mehrerer vorkommender Ergebnisgrundrisse
	 * denjenigen Umriss mit dem groessten Flaecheninhalt aus
	 */
	private void chooseResultFootprint() {

		final MyVectormath mathHelper = MyVectormath.getInstance();
		float maxArea = -Float.MAX_VALUE;
		float curArea;

		for (MyPolygon currentPoly : mResultFootprints) {

			curArea = mathHelper.computePolygonArea(currentPoly.getVertices());
			if (curArea > maxArea) {
				maxArea = curArea;
				mResultFootprint = currentPoly;
			}
		}
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mResultFootprints
	 */
	public List<MyPolygon> getResultFootprints() {
		return mResultFootprints;
	}

	// ------------------------------------------------------------------------------------------

	private void testFacePoly() {
		Face curFace = null;
		for (int i = 0; i < mFaces.size(); i++) {
			curFace = mFaces.get(i);
			if (curFace.getState() == State.INTERIOR)
				LOGGER.info(curFace.getFacePoly());
		}
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mResultFootprint
	 */
	public MyPolygon getResultFootprint() {
		return mResultFootprint;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode durchlaeuft alle Vertices und gibt ihre Nachbarschaft aus
	 */
	private void printVertices() {
		String dots = "...............";
		VertexWrapper curVert = null;
		List<VertexWrapper> neighbours = null;
		for (int i = 0; i < mVertices.size(); i++) {
			curVert = mVertices.get(i);
			LOGGER.info(curVert);
			neighbours = curVert.getNeighbours();
			for (int j = 0; j < neighbours.size(); j++)
				LOGGER.info(dots + neighbours.get(j));
		}

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * @return the mLabelToComponent
	 */
	public Set<String> getLabels() {
		return mLabelToComponent.keySet();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * @return
	 * @see semantic.building.modeler.modelsynthesis.algorithm.SynthesisProcessing#getRuleSynthesisCounter()
	 */
	public Map<RuleType, Integer> getRuleSynthesisCounter() {
		return processing.getRuleSynthesisCounter();
	}
	// ------------------------------------------------------------------------------------------

}
