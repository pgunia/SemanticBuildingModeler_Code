package semantic.building.modeler.prototype.algorithm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import semantic.building.modeler.configurationservice.model.enums.Side;
import semantic.building.modeler.math.MyPolygon;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.MyVectormath;
import semantic.building.modeler.math.Plane;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.prototype.graphics.complex.AABB;
import semantic.building.modeler.prototype.graphics.complex.BoundingBox;
import semantic.building.modeler.prototype.graphics.complex.OBB;
import semantic.building.modeler.prototype.graphics.primitives.AbstractQuad;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.EigenvalueDecomposition;

/**
 * 
 * Klasse berechent fuer eine Menge von Vertices entweder eine AABB oder eine
 * OBB, fuer die OBB stehen mehrere Verfahren zur Verfuegung, u.a. eine
 * kovarianzbasierte Methode nach Gottschalk S. Gottschalk (2000),
 * "Collision Queries using Oriented Bounding Boxes"
 * 
 * @author Patrick Gunia
 * 
 */

public class BoundingBoxCreator {

	/** Logging-Instanz */
	protected static Logger LOGGER = Logger.getLogger(BoundingBoxCreator.class);

	/** Eingabepunktwolke */
	private List<Vertex3d> mVertices = null;

	/** Dreiecke, die als Ergebnis der Berechnung der konvexen Huelle entstehen */
	private List<MyPolygon> mConvexHull = null;

	/** Oberflaecheninhalt der konvexen Huelle des Polyeders */
	private Float mAreaPolyeder = null;

	/** Schwerpunkt des Polyeders */
	private MyVector3f mCentroidPolyeder = null;

	/** Kovarianzmatrix */
	private double[][] mCovarianceMatrix = null;

	/** Eigenvektoren der Kovarianzmatrix */
	private List<MyVector3f> mOBBAxis = null;

	/** Mittelpunkt der berechneten Boundingbox */
	private MyVector3f mOBBCenter = null;

	/** Ergebnis der Eigenwert- und Eigenvektorenberechnung */
	private EigenvalueDecomposition mEigenvalueDecomp = null;

	// ------------------------------------------------------------------------------------------
	/**
	 * Standardkonstruktur fuer BB-Factory
	 */
	public BoundingBoxCreator() {
		init();
	}

	// ------------------------------------------------------------------------------------------
	private void init() {
		mVertices = new ArrayList<Vertex3d>();
		mConvexHull = null;
		mAreaPolyeder = null;
		mCentroidPolyeder = null;
		mCovarianceMatrix = null;
		mOBBAxis = new ArrayList<MyVector3f>(3);
		mOBBCenter = null;
		// mEigenvalueDecomp = null;
	}

	// ------------------------------------------------------------------------------------------
	public void reset() {
		init();
	}

	// ------------------------------------------------------------------------------------------

	public List<MyPolygon> getConvexHull() {
		return mConvexHull;
	}

	// ------------------------------------------------------------------------------------------

	public List<MyVector3f> getEigenvektoren() {
		return mOBBAxis;
	}

	// ------------------------------------------------------------------------------------------
	public MyVector3f getOBBCenter() {
		return mOBBCenter;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode berechnet eine OBB fuer den uebergebenen Polyeder. Zunaechst wird
	 * die konvexe Huelle fuer den Eingabepolyeder mittels QuickHull bestimmt,
	 * anschliessend berechnet das Verfahren mittels Covarianzmatrixanalyse und
	 * Eigenvektorzerlegung die OBB und gibt diese als Liste von Polygonen an
	 * den Aufrufer zurueck
	 * 
	 * @return
	 */
	public BoundingBox computeOBBUsingConvexHull(List<Vertex3d> vertices) {
		reset();
		mVertices = vertices;
		MyVectormath mathHelper = MyVectormath.getInstance();

		LOGGER.debug("Berechnung der konvexen Huelle...");
		Quickhull3d quickHull = new Quickhull3d(mVertices);
		mConvexHull = quickHull.computeConvexHull();
		LOGGER.debug("Berechnung der konvexen Huelle...abgeschlossen");

		mAreaPolyeder = mathHelper.computeAreaForPolyeder(mConvexHull);

		mCentroidPolyeder = mathHelper.computeCentroidForPolyeder(mConvexHull);

		// Centroid muss noch ueber den Gesamtflaecheninhalt gewichtet werden
		mCentroidPolyeder.scale(1 / mAreaPolyeder);

		// berechne die Kovarianzmatrix
		computeCovarianceMatrixByTriangles();

		// berechne die OBB basierend auf der Kovarianzmatrix
		List<Float> extremwerte = computeOBBByCovarianceMatrix();
		return new OBB(extremwerte, mOBBAxis);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet die Eigenvektoren der berechneten Kovarianzmatrix
	 * aufgrund derer die OBB-Achsenausdehnungen bestimmt werden
	 * 
	 * @return Liste mit den Min-Max-Werten je Achse
	 */
	private List<Float> computeOBBByCovarianceMatrix() {

		// Eigenvektoren bestimmen
		DoubleMatrix2D covarianceMatrix = new DenseDoubleMatrix2D(
				mCovarianceMatrix);
		// Matrix covarianceMatrix = new Matrix(mCovarianceMatrix);

		// mEigenvalueDecomp = covarianceMatrix.eig();
		mEigenvalueDecomp = new EigenvalueDecomposition(covarianceMatrix);
		DoubleMatrix2D eigenvektorMatrix = mEigenvalueDecomp.getV();

		mOBBAxis = new ArrayList<MyVector3f>(3);
		float[] vektorComponents = null;

		for (int i = 0; i < eigenvektorMatrix.rows(); i++) {
			vektorComponents = new float[3];
			for (int k = 0; k < eigenvektorMatrix.columns(); k++) {
				vektorComponents[k] = (float) eigenvektorMatrix.get(i, k);
			}
			mOBBAxis.add(new MyVector3f(vektorComponents[0],
					vektorComponents[1], vektorComponents[2]));
			LOGGER.debug("Eigenvektor: " + mOBBAxis.get(i));
		}

		List<Float> extremwerte = projectVertsToAxes(mOBBAxis, mVertices);
		return extremwerte;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet die OBB ueber einen Brute-Force-Ansatz basierend auf
	 * den Faces der konvexen Huelle. Man durchlaeuft alle Faces der konvexen
	 * Huelle. Fuer jedes Face berechnet man die Projektion aller Punkte der
	 * konvexen Huelle auf die Faceebene, anschliessend berechnet man das
	 * minimale Bounding-Rechteck dieser Projektion. Die Achsen dieses Rechtecks
	 * definieren mit der Face-Normalen die Achsen der OBB.
	 * 
	 * @return Liste mit Polygonen, die die OBB fuer das Objekt beschreiben
	 */
	public BoundingBox computeOBBBruteForce(List<Vertex3d> vertices) {
		reset();
		mVertices = vertices;

		LOGGER.debug("Berechnung der konvexen Huelle...");
		Quickhull3d quickHull = new Quickhull3d(mVertices);
		mConvexHull = quickHull.computeConvexHull();
		LOGGER.debug("Berechnung der konvexen Huelle...abgeschlossen");

		List<Vertex3d> chVerts = getVertsFromPolyhedron(mConvexHull);

		MyPolygon currentPoly = null;
		List<MyVector3f> currentOBBAxis = null, currentBestOBBAxis = null;
		float currentVolume = 0, minVolume = Float.MAX_VALUE;
		Set<MyVector3f> usedVectors = new HashSet<MyVector3f>();

		// Durchlaufe alle Faces der konvexen Huelle und verwende diese, um OBBs
		// zu berechnen
		for (int i = 0; i < mConvexHull.size(); i++) {
			currentPoly = mConvexHull.get(i);

			// wenn Faces die gleiche Ausrichtung haben, dann erzeugen sie auch
			// die gleiche OBB, skippe solche Neuberechnungen
			if (usedVectors.contains(currentPoly.getNormalPtr()))
				continue;

			usedVectors.add(currentPoly.getNormalPtr());
			currentOBBAxis = computeOBBByPoly(currentPoly, chVerts);
			currentVolume = computeOBBVolume(currentOBBAxis, chVerts);
			if (currentVolume < minVolume) {
				minVolume = currentVolume;
				currentBestOBBAxis = currentOBBAxis;
			}
		}

		assert currentBestOBBAxis.size() == 3 : "FEHLER: Es wurden "
				+ currentBestOBBAxis.size() + " Achsen ermittelt.";

		mOBBAxis = new ArrayList<MyVector3f>(currentBestOBBAxis);
		// mEigenvektoren.addAll(currentBestOBBAxis);
		List<Float> extremwerte = projectVertsToAxes(currentBestOBBAxis,
				chVerts);
		return new OBB(extremwerte, currentBestOBBAxis);

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet eine OBB basierend auf einer Menge von Faces. Im
	 * Gegensatz zum 2. Brute-Force-Ansatz wird dabei keine konvexe Huelle
	 * berechnet. Diese Methode eignet sich darum fuer Modelle mit relativ
	 * wenigen Vertices, bei denen die Convex-Hull-Berechnung zu einer
	 * groesseren Komplexitaet fuehren wuerde.
	 * 
	 * @param faces
	 *            Seitenflaechen des komplexen Objekts
	 * @param vertices
	 *            Vertices des komplexen Objekts
	 * @return Objektorientierte Bounding-Box
	 */
	public BoundingBox computeOBBBruteForceNoCH(List<MyPolygon> faces,
			List<Vertex3d> vertices) {
		MyPolygon currentPoly = null;
		List<MyVector3f> currentOBBAxis = null, currentBestOBBAxis = null;
		float currentVolume = 0, minVolume = Float.MAX_VALUE;
		Set<MyVector3f> usedVectors = new HashSet<MyVector3f>();

		// Durchlaufe alle Faces des Objekts und verwende diese, um OBBs zu
		// berechnen
		for (int i = 0; i < faces.size(); i++) {
			currentPoly = faces.get(i);

			// wenn Faces die gleiche Ausrichtung haben, dann erzeugen sie auch
			// die gleiche OBB, skippe solche Neuberechnungen
			if (usedVectors.contains(currentPoly.getNormalPtr()))
				continue;

			usedVectors.add(currentPoly.getNormalPtr());
			currentOBBAxis = computeOBBByPoly(currentPoly, vertices);
			currentVolume = computeOBBVolume(currentOBBAxis, vertices);
			if (currentVolume < minVolume) {
				minVolume = currentVolume;
				currentBestOBBAxis = currentOBBAxis;
			}
		}

		assert currentBestOBBAxis.size() == 3 : "FEHLER: Es wurden "
				+ currentBestOBBAxis.size() + " Achsen ermittelt.";

		mOBBAxis = new ArrayList<MyVector3f>(currentBestOBBAxis);

		// mEigenvektoren.addAll(currentBestOBBAxis);
		List<Float> extremwerte = projectVertsToAxes(currentBestOBBAxis,
				vertices);
		return new OBB(extremwerte, currentBestOBBAxis);
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode berechnet eine OBB basierend auf dem Uebergabepolygon, indem
	 * saemtliche Uebergabepunkte auf die Polygonebene projiziert werden. Dann
	 * berechnet man in dieser Ebene das minimale Boundingrechteck, welches mit
	 * der Polygonnormale die Achsen der OBB definiert.
	 * 
	 * @param poly
	 *            Polygon auf dem basierend die OBB bestimmt wird
	 * @param vertices
	 *            Punkte, die auf die Dreiecksebene projiziert werden
	 * @return Liste mit den Achsen der OBB
	 */
	private List<MyVector3f> computeOBBByPoly(MyPolygon poly,
			List<Vertex3d> vertices) {

		Plane projectionPlane = new Plane(poly.getNormalPtr(), poly
				.getVertices().get(0).getPositionPtr());
		List<Vertex3d> projectedPoints = new ArrayList<Vertex3d>(
				vertices.size());
		MyVectormath mathHelper = MyVectormath.getInstance();
		Vertex3d currentVertex = null;

		for (int i = 0; i < vertices.size(); i++) {
			currentVertex = vertices.get(i).clone();
			mathHelper.projectPointOntoPlane(projectionPlane, currentVertex);
			projectedPoints.add(currentVertex);
		}

		List<MyVector3f> result = new ArrayList<MyVector3f>(3);
		result.add(poly.getNormal());

		// Achsen des minimalen Boundingrechtsecks berechnen
		result.addAll(mathHelper.getMinAreaRectAxis(projectedPoints));
		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode extrahiert alle Vertices aus einer Menge von Uebergabepolygonen
	 * 
	 * @param polys
	 *            Polygone, aus denen die Vertices extrahiert werden
	 * @return Liste mit Vertices, wobei keien Duplikate in der Liste vorhanden
	 *         sind
	 */
	private List<Vertex3d> getVertsFromPolyhedron(List<MyPolygon> polys) {
		Set<Vertex3d> vertSet = new HashSet<Vertex3d>();
		for (int i = 0; i < polys.size(); i++)
			vertSet.addAll(polys.get(i).getVertices());
		List<Vertex3d> result = new ArrayList<Vertex3d>(vertSet);
		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methoed berechnet eine PCA-basierte OBB durch Berechnung der
	 * Kovarianzmatrix auf Vertexebene
	 * 
	 * @return OBB-Instanz fuer die uebergebene Punktwolke
	 */
	public BoundingBox computeOBBUsingVertices(List<Vertex3d> vertices) {
		reset();
		mVertices = vertices;
		computeCovarianceMatrixByVertices();
		List<Float> extremwerte = computeOBBByCovarianceMatrix();
		return new OBB(extremwerte, mOBBAxis);
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode projiziert alle Eingabevertices auf die Eigenvektoren, die die
	 * Achsen der OBB beschreiben. Dabei werden die Maximalwerte in allen 3
	 * Achsen gesucht.
	 * 
	 * @param axis
	 *            Liste mit den Achsen der aktuellen OBB
	 * @return Liste mit den Extremwerten fuer alle 3 Achsen der OBB
	 */
	private List<Float> projectVertsToAxes(List<MyVector3f> axis,
			List<Vertex3d> vertices) {

		float u1 = -Float.MAX_VALUE, u2 = -Float.MAX_VALUE, u3 = -Float.MAX_VALUE;
		float l1 = Float.MAX_VALUE, l2 = Float.MAX_VALUE, l3 = Float.MAX_VALUE;
		float currentValue;
		Vertex3d currentVertex = null;

		MyVector3f currentAxis = null;

		// projiziere alle Punkte auf die 3 Eigenvektoren und bestimme darueber
		// die Ausdehnung der Boundingbox
		for (int i = 0; i < vertices.size(); i++) {
			currentVertex = vertices.get(i);
			for (int k = 0; k < axis.size(); k++) {
				currentAxis = axis.get(k);
				currentValue = currentAxis.dot(currentVertex.getPositionPtr());

				// MinMax-Werte aktualisieren
				if (k == 0) {
					if (currentValue > u1)
						u1 = currentValue;
					if (currentValue < l1)
						l1 = currentValue;
				} else if (k == 1) {
					if (currentValue > u2)
						u2 = currentValue;
					if (currentValue < l2)
						l2 = currentValue;
				} else if (k == 2) {
					if (currentValue > u3)
						u3 = currentValue;
					if (currentValue < l3)
						l3 = currentValue;
				}
			}
		}

		List<Float> result = new ArrayList<Float>(6);
		result.add(u1);
		result.add(l1);
		result.add(u2);
		result.add(l2);
		result.add(u3);
		result.add(l3);
		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet die Kovarianzmatrix aufgrund der Formeln nach
	 * Gottschalk fuer Dreiecke.
	 */
	private void computeCovarianceMatrixByTriangles() {
		MyPolygon current = null;
		MyVector3f currentTriangleCenter = null;
		Vertex3d p = null, q = null, r = null;

		initCovarianceMatrix();

		// die Formeln entsprechen der Beschreibung in der Dissertation
		for (int i = 0; i < mConvexHull.size(); i++) {
			current = mConvexHull.get(i);
			currentTriangleCenter = current.getCenter();
			p = current.getVertices().get(0);
			q = current.getVertices().get(1);
			r = current.getVertices().get(2);
			// j = 0, i = 0
			mCovarianceMatrix[0][0] += 1 / 24 * (9 * currentTriangleCenter.x
					* currentTriangleCenter.x + p.getX() * p.getX() + q.getX()
					* q.getX() + r.getX() * r.getX());
			// j = 0, i = 1
			mCovarianceMatrix[0][1] += 1 / 24 * (9 * currentTriangleCenter.x
					* currentTriangleCenter.y + p.getX() * p.getY() + q.getX()
					* q.getY() + r.getX() * r.getY());
			// j = 0, i = 2
			mCovarianceMatrix[0][2] += 1 / 24 * (9 * currentTriangleCenter.x
					* currentTriangleCenter.z + p.getX() * p.getZ() + q.getX()
					* q.getZ() + r.getX() * r.getZ());
			// j = 1, i = 1
			mCovarianceMatrix[1][1] += 1 / 24 * (9 * currentTriangleCenter.y
					* currentTriangleCenter.y + p.getY() * p.getY() + q.getY()
					* q.getY() + r.getY() * r.getY());
			// j = 1, i = 2
			mCovarianceMatrix[1][2] += 1 / 24 * (9 * currentTriangleCenter.y
					* currentTriangleCenter.z + p.getY() * p.getZ() + q.getY()
					* q.getZ() + r.getY() * r.getZ());
			// j = 2, i = 2
			mCovarianceMatrix[2][2] += 1 / 24 * (9 * currentTriangleCenter.z
					* currentTriangleCenter.z + p.getZ() * p.getZ() + q.getZ()
					* q.getZ() + r.getZ() * r.getZ());
		}

		mCovarianceMatrix[0][0] = mCovarianceMatrix[0][0] / mAreaPolyeder
				- mCentroidPolyeder.x * mCentroidPolyeder.x;
		mCovarianceMatrix[0][1] = mCovarianceMatrix[0][1] / mAreaPolyeder
				- mCentroidPolyeder.x * mCentroidPolyeder.y;
		mCovarianceMatrix[0][2] = mCovarianceMatrix[0][2] / mAreaPolyeder
				- mCentroidPolyeder.x * mCentroidPolyeder.z;
		mCovarianceMatrix[1][1] = mCovarianceMatrix[1][1] / mAreaPolyeder
				- mCentroidPolyeder.y * mCentroidPolyeder.y;
		mCovarianceMatrix[1][2] = mCovarianceMatrix[1][2] / mAreaPolyeder
				- mCentroidPolyeder.y * mCentroidPolyeder.z;
		mCovarianceMatrix[2][2] = mCovarianceMatrix[2][2] / mAreaPolyeder
				- mCentroidPolyeder.z * mCentroidPolyeder.z;

		// symmetische Matrix bauen
		mCovarianceMatrix[1][0] = mCovarianceMatrix[0][1];
		mCovarianceMatrix[2][0] = mCovarianceMatrix[0][2];
		mCovarianceMatrix[2][1] = mCovarianceMatrix[1][2];

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet die Kovarianzmatrix aufgrund der Formeln nach
	 * Gottschalk fuer Vertexwolken
	 */
	private void computeCovarianceMatrixByVertices() {

		MyVector3f vertexCenter = null;
		initCovarianceMatrix();
		Vertex3d currentVertex = null;
		int numVertices = mVertices.size();

		// Schwerpunkt der Punkte berechnen
		for (int i = 0; i < numVertices; i++) {
			currentVertex = mVertices.get(i);
			if (vertexCenter == null)
				vertexCenter = currentVertex.getPosition();
			else
				vertexCenter.add(currentVertex.getPositionPtr());
		}

		vertexCenter.scale(1 / numVertices);

		// Kovarianzmatrix aufbauen
		for (int i = 0; i < numVertices; i++) {
			currentVertex = mVertices.get(i);
			// j = 0, i = 0
			mCovarianceMatrix[0][0] += ((currentVertex.getX() - vertexCenter.x) * (currentVertex
					.getX() - vertexCenter.x));
			// j = 0, i = 1
			mCovarianceMatrix[0][1] += ((currentVertex.getX() - vertexCenter.x) * (currentVertex
					.getY() - vertexCenter.y));
			// j = 0; i = 2
			mCovarianceMatrix[0][2] += ((currentVertex.getX() - vertexCenter.x) * (currentVertex
					.getZ() - vertexCenter.z));
			// j = 1; i = 1
			mCovarianceMatrix[1][1] += ((currentVertex.getY() - vertexCenter.y) * (currentVertex
					.getY() - vertexCenter.y));
			// j = 1; i = 2
			mCovarianceMatrix[1][2] += ((currentVertex.getY() - vertexCenter.y) * (currentVertex
					.getZ() - vertexCenter.z));
			// j = 2; i = 2
			mCovarianceMatrix[2][2] += ((currentVertex.getZ() - vertexCenter.z) * (currentVertex
					.getZ() - vertexCenter.z));
		}

		mCovarianceMatrix[0][0] = mCovarianceMatrix[0][0] / numVertices;
		mCovarianceMatrix[0][1] = mCovarianceMatrix[0][1] / numVertices;
		mCovarianceMatrix[0][2] = mCovarianceMatrix[0][2] / numVertices;
		mCovarianceMatrix[1][1] = mCovarianceMatrix[1][1] / numVertices;
		mCovarianceMatrix[1][2] = mCovarianceMatrix[1][2] / numVertices;
		mCovarianceMatrix[2][2] = mCovarianceMatrix[2][2] / numVertices;

		// symmetische Matrix bauen
		mCovarianceMatrix[1][0] = mCovarianceMatrix[0][1];
		mCovarianceMatrix[2][0] = mCovarianceMatrix[0][2];
		mCovarianceMatrix[2][1] = mCovarianceMatrix[1][2];
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode initialisiert die Kovarianzmatrix
	 */
	private void initCovarianceMatrix() {
		// Kovarianzmatrix initialisieren
		mCovarianceMatrix = new double[3][3];
		mCovarianceMatrix[0][0] = 0.0d;
		mCovarianceMatrix[0][1] = 0.0d;
		mCovarianceMatrix[0][2] = 0.0d;
		mCovarianceMatrix[1][0] = 0.0d;
		mCovarianceMatrix[2][0] = 0.0d;
		mCovarianceMatrix[1][1] = 0.0d;
		mCovarianceMatrix[1][2] = 0.0d;
		mCovarianceMatrix[2][1] = 0.0d;
		mCovarianceMatrix[2][2] = 0.0d;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet das Volumen der berechneten OBB durch Projektion aller
	 * Vertices auf die OBB-Achsen und Bestimmung der Ausdehnungen in den 3
	 * Dimensionen
	 * 
	 * @param axis
	 *            Achsen der OBB, fuer die das Volumen berechnet werden soll
	 * @return Volumen des berechneten OBB
	 */
	private float computeOBBVolume(List<MyVector3f> axis,
			List<Vertex3d> vertices) {

		List<Float> axisValues = projectVertsToAxes(axis, vertices);
		return Math.abs((axisValues.get(0) - axisValues.get(1))
				* (axisValues.get(3) - axisValues.get(2))
				* (axisValues.get(5) - axisValues.get(4)));

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode berechnet eine achsenausgerichtete Bounding-Box durch
	 * Min-Max-Berechnungen ueber alle Vertices des komplexen Objekts
	 * 
	 * @param complex
	 *            Punktwolke, fuer die eine Bounding-Box berechnet werden soll
	 */

	public AABB computeAABB(List<Vertex3d> vertices) {
		reset();
		MyVector3f currentPosition = null;

		MyVector3f mMin = new MyVector3f(), mMax = new MyVector3f();
		mMin.x = Float.MAX_VALUE;
		mMin.y = Float.MAX_VALUE;
		mMin.z = Float.MAX_VALUE;
		mMax.x = -Float.MAX_VALUE;
		mMax.y = -Float.MAX_VALUE;
		mMax.z = -Float.MAX_VALUE;

		for (int i = 0; i < vertices.size(); i++) {
			currentPosition = vertices.get(i).getPositionPtr();

			// aktualisiere die Min-Max-Werte basierend auf den Koordinaten des
			// aktuell verarbeiteten Vertex
			if (currentPosition.x < mMin.x) {
				mMin.x = currentPosition.x;
			}
			if (currentPosition.x > mMax.x) {
				mMax.x = currentPosition.x;
			}
			if (currentPosition.y < mMin.y) {
				mMin.y = currentPosition.y;
			}
			if (currentPosition.y > mMax.y) {
				mMax.y = currentPosition.y;
			}
			if (currentPosition.z < mMin.z) {
				mMin.z = currentPosition.z;
			}
			if (currentPosition.z > mMax.z) {
				mMax.z = currentPosition.z;
			}

		}

		float length = 0, height = 0, width = 0;

		// Ausdehnungen berechnen
		length = (float) Math.sqrt((mMax.x - mMin.x) * (mMax.x - mMin.x));
		height = (float) Math.sqrt((mMax.y - mMin.y) * (mMax.y - mMin.y));
		width = (float) Math.sqrt((mMax.z - mMin.z) * (mMax.z - mMin.z));

		// handelt es sich um einen 2D-Shape?
		boolean is2D = is2D(vertices);

		MyVector3f center = new MyVector3f(0.0f, 0.0f, 0.0f);

		// Center berechnen
		center.add(mMin);
		center.add(mMax);
		center.scale(0.5f);

		AABB result = new AABB();
		result.setHeight(height);
		result.setWidth(width);
		result.setLength(length);
		result.setCenter(center);

		// berechne die Faces der BB abhaengig davon, ob es sich um einen
		// Koerper, oder einen Shape handelt
		if (!is2D)
			result.computePolygons();
		else
			result.computeRect(projectVertsToAxes(result.getAxes(), vertices));
		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode testet, ob die uebergebenen Vertices einen 2D-Shape bilden
	 * 
	 * @param vertices
	 *            Liste mit Vertices
	 * @return True, falls alle Uebergabepunkte in einer gemeinsamen Ebene
	 *         liegen, False sonst
	 */
	private boolean is2D(List<Vertex3d> vertices) {

		// teste, ob es sich bei der uebergebenen Punktemenge um einen Shape
		// handelt
		MyVector3f vert0 = vertices.get(0).getPositionPtr();
		MyVector3f vert1 = vertices.get(1).getPositionPtr();
		MyVector3f vert2 = vertices.get(2).getPositionPtr();

		MyVector3f vert1_vert0 = new MyVector3f();
		vert1_vert0.sub(vert0, vert1);
		MyVector3f vert1_vert2 = new MyVector3f();
		vert1_vert2.sub(vert2, vert1);
		MyVector3f normal = new MyVector3f();
		normal.cross(vert1_vert2, vert1_vert0);
		assert !normal.isInvalid() : "FEHLER: Ungueltiger Normalenvektor: "
				+ normal;
		Plane polyPlane = null;
		polyPlane = new Plane(normal, vert0);

		// teste, ob die restlichen Punkte auf der Ebene liegen, die durch die
		// ersten 3 Punkte aufgespannt wird
		MyVector3f currentPos = null;
		MyVectormath mathHelper = MyVectormath.getInstance();
		for (int i = 2; i < vertices.size(); i++) {
			currentPos = vertices.get(i).getPositionPtr();
			if (!mathHelper.isPointOnPlane(currentPos, polyPlane)) {
				return false;
			}
		}
		return true;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet eine AABB, die das uebergebene Quad vollstaendig
	 * enthaelt
	 * 
	 * @param quad
	 *            Quadstruktur, fuer die eine AABB erstellt werden soll
	 * @return AABB, die saemtliche Quad-Vertices enthaelt
	 */
	public AABB computeAABB(AbstractQuad quad) {
		reset();
		List<Vertex3d> quadVertices = quad.getQuadVertices();
		AABB aabb = computeAABB(quadVertices);
		float length = aabb.getLength(), width = aabb.getWidth(), height = aabb
				.getHeight();

		Side quadDirection = quad.getDirection();
		float temp;

		// veraendere jetzt die Ausdehnungen abhaengig von der Ausrichtung
		// fuer alle nicht abgedeckten Ausrichtungen ist eine Zuordnung nicht
		// sinnvoll moeglich, somit werden die Ausdehnungen wie urspruenglich
		// interpretiert
		if (quadDirection.equals(Side.LEFT) || quadDirection.equals(Side.RIGHT)) {

			// z-Achse beschreibt Breite, x-Achse Tiefe (wobei Quads keine Tiefe
			// haben)
			temp = length;
			aabb.setLength(width);
			aabb.setWidth(temp);

		} else if (quadDirection.equals(Side.TOP)
				|| quadDirection.equals(Side.BOTTOM)) {

			// z-Achse beschreibt Hoehe
			temp = height;
			aabb.setHeight(width);
			aabb.setHeight(width);
		}
		return aabb;
	}
	// ------------------------------------------------------------------------------------------

}
