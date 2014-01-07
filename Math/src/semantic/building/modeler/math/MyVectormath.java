package semantic.building.modeler.math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;

/**
 * 
 * @author Patrick Gunia Mathepaket mit einer Reihe von Methoden fuer
 *         Berechnungen im Bereich der linearen Algebra und Vektormathematik.
 * 
 */
public class MyVectormath {

	/** Logging-Instanz */
	protected final static Logger LOGGER = Logger
			.getLogger("math.MyVectormath");

	/**
	 * Anzahl der Nachkommastellen, die exakt gerundet werden => 100.0 => auf 2
	 * Nachkommastellen genau etc.
	 */
	private static final float ROUNDACCURACY = 10000.0f;

	/** Toleranzdelta bzgl. Floating-Point-Rundungsfehlern */
	private static final float TOLERANCE = 0.04f;

	/** kleine Zahl */
	final static float SMALL_NUM = 0.00000001f;

	/** Debugging-Flag */
	private static final boolean DEBUGMATH = true;

	/** Singleton-Instanz */
	private static MyVectormath mInstance = null;

	/** Versteckter Singleton-Konstruktor */
	protected MyVectormath() {
	};

	/** Instanz der Random-Bib */
	protected transient Random mRandom = new Random();

	public enum Drehsinn {
		POSITIV, NEGATIV;
	}

	// ------------------------------------------------------------------------------------------

	/** Singleton-Getter */
	public static MyVectormath getInstance() {
		if (mInstance == null)
			mInstance = new MyVectormath();
		return mInstance;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * berechnet die Winkelhalbierende der beiden uebergebenen Rays Die
	 * Winkelhalbierende zweier Vektoren a und b mit gemeinsamem Stuetzpunkt C
	 * berechnet sich durch die Addition der Einheitsvektoren von a und b
	 * http://www.mathe-online.at/mathint/vect1/i.html#30
	 * 
	 * @param ray1
	 * @param ray2
	 * @return
	 */
	public MyVector3f calculateWinkelhalbierende(final Ray ray1, final Ray ray2) {

		// hole den Vektor a. also den Vektor vom gemeinsamen Stuetzpunkt auf
		// Punkt A
		final MyVector3f a = ray1.getDirection();
		a.normalize();

		// hole fuer die Winkelberechnung den zweiten Richtungsvektor von Punkt
		// C auf Punkt B
		MyVector3f b = ray2.getDirection();
		b.normalize();

		final MyVector3f result = new MyVector3f();
		result.add(a, b);
		result.normalize();

		return result;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Algorithmus zur Berechnung eines Geradenschnittpunktes basierend auf der
	 * Methode nach Ronald Goldblum Verwendet Kreuzprodukte, wird hier
	 * beschrieben: http://mathforum.org/library/drmath/view/62814.html
	 * http://mathforum.org/library/drmath/view/63719.html
	 * 
	 * @param ray1
	 *            Eingabestrahl 1
	 * @param ray2
	 *            Eingabestrahl 2
	 * @return Schnittpunkt, sofern ein solcher existiert, NULL sonst
	 * @deprecated
	 */
	@Deprecated
	public MyVector3f calculateRay2RayIntersection3DVectorStyle(final Ray ray1,
			final Ray ray2) {

		final MyVector3f result = new MyVector3f();
		final MyVector3f p1 = ray1.getStart();
		MyVector3f p2 = ray2.getStart();

		MyVector3f v1 = ray1.getDirection();
		MyVector3f v2 = ray2.getDirection();

		MyVector3f crossV1V2 = new MyVector3f();
		crossV1V2.cross(v1, v2);

		MyVector3f p2Minusp1 = new MyVector3f();
		p2Minusp1.sub(p2, p1);

		MyVector3f rightSide = new MyVector3f();
		rightSide.cross(p2Minusp1, v2);

		// es liegt genau dann ein Schnittpunkt vor, wenn crossV1V2 parallel zu
		// rightSide ist
		// und crossV1V2 nicht dem 0-Vektor entspricht
		if (!isParallel(rightSide, crossV1V2)) {
			LOGGER.debug("Vektoren sind nicht parallel, die Geraden sind windschief");
			return null;
		}

		if (crossV1V2.length() == 0) {
			LOGGER.debug("Linke Seite entspricht dem Null-Vektor, die Geraden sind echt parallel oder windschief");

			return null;
		}

		float a;
		// da zu diesem Zeitpunkt feststeht, dass die linke Seite nicht der
		// Null-Vektor ist, muss eine
		// der Komponenten der rechten Seite != 0 sein
		if (crossV1V2.x != 0 && crossV1V2.x != -0) {
			a = rightSide.x / crossV1V2.x;
		} else if (crossV1V2.y != 0 && crossV1V2.y != -0) {
			a = rightSide.y / crossV1V2.y;
		} else {
			a = rightSide.z / crossV1V2.z;
		}

		v1.scale(a);
		result.add(p1, v1);

		// runde den berechneten Schnittpunkt
		// result = roundVector3f(result);
		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Berechnet den Schnittpunkt zweier Geraden im 3-dimensionalen Raum durch
	 * Loesen des zugehorigen Gleichungssystems ueber die JAMA-Matrix-Solver.
	 * Setzt die beiden Punkt-Richtungsformen der Strahlen gleich und berechnet
	 * guelgite Parameter. Sofern ein Schnittpunkt existiert, wird dieser
	 * zurueckgegeben, sonst NULL.
	 * 
	 * @param ray1
	 *            Eingabestrahl1
	 * @param ray2
	 *            Eingabestrahl2
	 * @return Schnittpunkt, sofern ein solcher berechnet werden konnte, NULL
	 *         sonst
	 * 
	 */
	public MyVector3f calculateRay2RayIntersectionMatrixStyle(final Ray ray1,
			final Ray ray2) {

		// System.out.println("Ray1: " + ray1);
		// System.out.println("Ray2: " + ray2);
		assert ray1 != null && ray2 != null : "Die eingegebenen Strahlen sind null";

		// fuehre direkt einen Test auf Parallelitaet durch, um den
		// Rechenaufwand zu minimieren
		if (isParallel(ray1.getDirection(), ray2.getDirection()))
			return null;

		double[][] koeffizienten = new double[3][2];

		MyVector3f stuetzvektorRay1 = ray1.getStart();
		MyVector3f stuetzvektorRay2 = ray2.getStart();

		MyVector3f directionRay1 = ray1.getDirection();
		MyVector3f directionRay2 = ray2.getDirection();

		// auf die andere Seite holen
		directionRay2.scale(-1.0f);

		// x-Komponente
		koeffizienten[0][0] = directionRay1.x;
		koeffizienten[0][1] = directionRay2.x;

		// y-Komponente
		koeffizienten[1][0] = directionRay1.y;
		koeffizienten[1][1] = directionRay2.y;

		// z-Komponente
		koeffizienten[2][0] = directionRay1.z;
		koeffizienten[2][1] = directionRay2.z;

		// Matrix lhs = new Matrix(koeffizienten);
		DoubleMatrix2D lhs = new DenseDoubleMatrix2D(koeffizienten);
		// rechte Seite berechnen
		stuetzvektorRay1.scale(-1.0f);

		MyVector3f rhsVector = new MyVector3f();
		rhsVector.add(stuetzvektorRay1, stuetzvektorRay2);

		double[] rhsKoeffizienten = new double[3];
		rhsKoeffizienten[0] = rhsVector.x;
		rhsKoeffizienten[1] = rhsVector.y;
		rhsKoeffizienten[2] = rhsVector.z;

		/*
		 * Matrix rhs = new Matrix(rhsKoeffizienten, 3); Matrix result = null;
		 */

		DoubleMatrix2D rhs = new DenseDoubleMatrix2D(3, 1);
		rhs.setQuick(0, 0, rhsKoeffizienten[0]);
		rhs.setQuick(0, 1, rhsKoeffizienten[1]);
		rhs.setQuick(0, 2, rhsKoeffizienten[2]);
		DoubleMatrix2D result = null;
		Algebra algebra = new Algebra();

		try {
			// result = lhs.solve(rhs);
			result = algebra.solve(lhs, rhs);
		}
		// wenn diese Exception ausgeloest wurde, dann ist das Gleichungssystem
		// nicht loesbar
		catch (RuntimeException e) {
			return null;
		}

		double parameter = result.get(0, 0);

		// setze den berechneten Parameter in die Geradengleichung ein
		stuetzvektorRay1 = ray1.getStart();
		directionRay1 = ray1.getDirection();

		directionRay1.scale((float) parameter);

		MyVector3f schnittpunkt = new MyVector3f();
		schnittpunkt.add(stuetzvektorRay1, directionRay1);

		// fuehre eine Proberechnung durch => berechne den Schnittpunkt ueber
		// den 2. Parameter
		MyVector3f schnittpunkt2 = new MyVector3f();
		directionRay2 = ray2.getDirection();
		stuetzvektorRay2 = ray2.getStart();

		double parameter2 = result.get(1, 0);
		directionRay2.scale((float) parameter2);
		schnittpunkt2.add(stuetzvektorRay2, directionRay2);

		assert isPointOnRay(schnittpunkt, ray1)
				&& isPointOnRay(schnittpunkt, ray2) : "FEHLER: Berechneter Schnittpunkt "
				+ schnittpunkt
				+ " liegt nicht auf den Eingabestrahlen, Probeschnittpunkt: "
				+ schnittpunkt2
				+ " ! Winkel: "
				+ calculateAngle(ray1.getDirectionPtr(), ray2.getDirectionPtr());
		// if(!isPointOnRay(schnittpunkt, ray1) && isPointOnRay(schnittpunkt,
		// ray2)) System.out.println("FEHLER: Berechneter Schnittpunkt "+
		// schnittpunkt +
		// " liegt nicht auf den Eingabestrahlen, Probeschnittpunkt: " +
		// schnittpunkt2 + " ! Winkel: " +
		// calculateAngle(ray1.getDirectionPtr(), ray2.getDirectionPtr()));

		// nur wenn die beiden Schnittpunkte einen Abstand von weniger als 0.1
		// besitzen, werden sie als gleich betrachtet
		/*
		 * if (isWithinTolerance(schnittpunkt, schnittpunkt2) &&
		 * isPointOnRay(schnittpunkt, ray1) && isPointOnRay(schnittpunkt, ray2))
		 * return schnittpunkt;
		 */
		if (isWithinTolerance(schnittpunkt, schnittpunkt2))
			return schnittpunkt;

		// sonst gebe null zurueck, das Gleichungssystem ist nicht eindeutig
		// loesbar aufgrund der Ueberbestimmtheit
		else
			return null;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet den Schnittpunkt der Uebergabestrahlen ueber die
	 * Bestimmung der Punkte auf den beiden Geraden, die den geringsten
	 * senkrechten Abstand voneinander haben. Dies sollte zu einer hoeheren
	 * Robustheit in der Berechnung fuehren.
	 * 
	 * @param ray1
	 *            Eingabestrahl1
	 * @param ray2
	 *            Eingabestrahl2
	 * @return Punkt, der den minimalen Abstand zwischen den beiden Strahlen
	 *         minimiert, wobei der Abstand unterhalb eines Grenzwertes liegen
	 *         muss
	 */
	public MyVector3f calculateRay2RayIntersectionApproximation(final Ray ray1,
			final Ray ray2) {
		float maxDistance = 0.5f;
		return calculateRay2RayIntersectionApproximation(ray1, ray2,
				maxDistance);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet den Schnittpunkt der Uebergabestrahlen ueber die
	 * Bestimmung der Punkte auf den beiden Geraden, die den geringsten
	 * senkrechten Abstand voneinander haben. Dies sollte zu einer hoeheren
	 * Robustheit in der Berechnung fuehren.
	 * 
	 * @param ray1
	 *            Eingabestrahl1
	 * @param ray2
	 *            Eingabestrahl2
	 * @param acceptableDistance
	 *            Distanz zwischen den naechstgelegenen Punkt unterhalb derer
	 *            noch ein Schnittpunkt akzeptiert wird
	 * @return Punkt, der den minimalen Abstand zwischen den beiden Strahlen
	 *         minimiert, wobei der Abstand unterhalb eines Grenzwertes liegen
	 *         muss
	 */
	public MyVector3f calculateRay2RayIntersectionApproximation(Ray ray1,
			Ray ray2, final float acceptableDistance) {

		// Punkte mit minimalem Abstand berechnen
		List<MyVector3f> minDistancePoints = calculateMinDistancePointsOnRays(
				ray1, ray2);

		// wenn keine Punkte bestimmt wurden, so sind die Strahlen parallel,
		// gebe in diesem Fall keinen Schnittpunkt zurueck
		if (minDistancePoints.isEmpty())
			return null;

		MyVector3f vec = new MyVector3f();
		vec.sub(minDistancePoints.get(0), minDistancePoints.get(1));

		// wenn der Abstand zu gross wird, null zurueck
		float distance = vec.length();
		if (distance > acceptableDistance) {
			// System.out.println("calculateRay2RayIntersectionApproximation: Abstand: "
			// + distance);
			return null;
		}

		// halber Vektor beschreibt genau den Mittelpunkt
		vec.scale(0.5f);
		MyVector3f intersection = new MyVector3f();
		intersection.add(minDistancePoints.get(1), vec);
		return intersection;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * berechnet die Distanz des Punktes point von der Geraden edge Formel:
	 * d(point, edge) = |(a-b) x r| / |r| a := Ortsvektor von Point b :=
	 * Stuetzvektor der Geraden r := Richtungsvektor der Geraden
	 * http://www.tutorials.de/java/340809-abstand-punkt-zu-gerade.html
	 * http://www.frustfrei-lernen.de/mathematik/abstand-punkt-gerade.html
	 * 
	 * @param myVector3f
	 *            Eingabepunkt
	 * @param edge
	 *            Kante, zu der der senkrechte Abstand des Punktes berechnet
	 *            wird
	 * 
	 * @return Abstand zwischen Punkt und Kante als Gleitpunktzahl
	 */
	public float calculatePointEdgeDistance(final MyVector3f myVector3f,
			final Ray edge) {

		final MyVector3f startEdge = edge.getStart();
		final MyVector3f directionEdge = edge.getDirection();

		// (a-b)
		final MyVector3f a_b = new MyVector3f();
		a_b.sub(myVector3f, startEdge);

		// (a-b) x r
		final MyVector3f cross = new MyVector3f();
		cross.cross(a_b, directionEdge);

		// Laenge von cross
		float lengthCross = cross.length();
		float lengthDirection = directionEdge.length();

		float result = lengthCross / lengthDirection;
		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Aufwendigere Methode, zu Proberechnungen gedacht. Berechnet den Abstand
	 * zwischen Punkt und Gerade wie in der Schule: Lege zunaechst eine
	 * Hilfsebene durch Gerade und Punkt, berechne den Durchstosspunkt der
	 * Gerade durch diese Ebene und dann die Entfernung von diesem
	 * Durchstosspunkt zum Testpunkt.
	 * 
	 * @param myVector3f
	 *            Punkt, dessen Abstand zur Gerade ermittelt werden soll
	 * @param edge
	 *            Gerade, zu der der Abstand berechnet werden soll
	 * @return Abstand zwischen Gerade und Ebene
	 */
	public float calculatePointEdgeDistancePlaneStyle(MyVector3f myVector3f,
			final Ray edge) {

		MyVector3f directionEdge = edge.getDirection();
		directionEdge.normalize();

		Plane plane = new Plane(directionEdge, myVector3f);

		MyVector3f intersection = calculateRayPlaneIntersection(edge, plane);

		assert intersection != null : "FEHLER: Kein Schnittpunkt zwischen Ebene und Gerade gefunden!";

		return calculatePointPointDistance(myVector3f, intersection);

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet die Orthogonalprojektion des uebergebenen Punktes auf
	 * den uebergebenen Strahl
	 * 
	 * @param point
	 *            Punkt, der auf den Strahl projeziert werden soll
	 * @param ray
	 *            Zielstrahl der Projektion
	 * @return Projezierter Punkt
	 */
	public MyVector3f calculatePointEdgeProjection(final MyVector3f point,
			final Ray ray) {

		// bestimme zunaechst die Ebene, in der Punkt und Strahl liegen
		MyVector3f stuetzvector1 = new MyVector3f();
		stuetzvector1.sub(ray.getStartPtr(), point);
		stuetzvector1.normalize();

		MyVector3f rayDirection = ray.getDirectionPtr().clone();
		rayDirection.normalize();

		LOGGER.error("Stuetze1: " + stuetzvector1 + " Stuetze2: "
				+ rayDirection);

		// Ebene wird aufgespannt durch den Richtungsvektor der Geraden und den
		// Vektor vom Punkt auf den Stuetzvektor der Geraden
		Plane plane = new Plane(point, stuetzvector1, rayDirection);

		// berechne einen zur Gerade senkrechten Vektor innerhalb der
		// berechneten Ebene
		MyVector3f orthogonal = calculateOrthogonalVectorWithSamePlane(
				ray.getDirectionPtr(), plane.getNormalPtr());
		Ray orthogonalRay = new Ray(point, orthogonal);

		// berechne den Schnittpunkt der beiden Strahlen
		return calculateRay2RayIntersectionApproximation(ray, orthogonalRay);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet den minimalen senkrechten Abstand der beiden
	 * Uerbegabestrahlen
	 * 
	 * @param ray1
	 *            Eingabestrahl1
	 * @param ray2
	 *            Eingabestrahl2
	 * @return Minimaler senkrechter Abstand der Eingabestrahlen voneinander
	 */
	public Float calculateRayToRayDistance(Ray ray1, Ray ray2) {

		List<MyVector3f> minDistancePoints = calculateMinDistancePointsOnRays(
				ray1, ray2);

		// wenn keine Punkte zurueckgegeben wurden, so sind die Strahlen
		// parallel
		if (minDistancePoints.isEmpty())
			return Float.MAX_VALUE;

		MyVector3f vec = new MyVector3f();
		vec.sub(minDistancePoints.get(0), minDistancePoints.get(1));
		return vec.length();

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet die zwei Punkte auf den Uebergabestrahlen, die den
	 * kleinsten senkrechten Abstand voneinander haben. Code stammt aus
	 * folgendem Artikel:
	 * http://softsurfer.com/Archive/algorithm_0106/algorithm_0106
	 * .htm#dist3D_Segment_to_Segment
	 * 
	 * @param ray1
	 *            Eingabestrahl 1
	 * @param ray2
	 *            Eingabestrahl 2
	 * @return 2 Punkte auf den Strahlen, die den minimalen senkrechten Abstand
	 *         besitzen
	 */
	private List<MyVector3f> calculateMinDistancePointsOnRays(Ray ray1, Ray ray2) {

		List<MyVector3f> resultPoints = new ArrayList<MyVector3f>(2);

		MyVector3f u = ray1.getDirection();
		u.normalize();

		MyVector3f v = ray2.getDirection();
		v.normalize();

		MyVector3f w = new MyVector3f();
		w.sub(ray1.getStart(), ray2.getStart());

		float a = u.dot(u);
		float b = u.dot(v);
		float c = v.dot(v);
		float d = u.dot(w);
		float e = v.dot(w);
		float D = a * c - b * b;
		float sc, tc;

		// compute the line parameters of the two closest points
		if (D < SMALL_NUM) { // the lines are almost parallel

			// wenn die Strahlen parallel sind, gebe keine Naeherungsloesung
			// zurueck
			return resultPoints;
		} else {
			sc = (b * e - c * d) / D;
			tc = (a * e - b * d) / D;
		}

		u.scale(sc);
		MyVector3f point1 = new MyVector3f();
		point1.add(ray1.getStart(), u);
		resultPoints.add(point1);

		v.scale(tc);
		MyVector3f point2 = new MyVector3f();
		point2.add(ray2.getStart(), v);
		resultPoints.add(point2);

		return resultPoints;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode zur Berechnung des Abstands zweier Punkte im 3-dimensionalen
	 * Raum. Verfahren berechnet den Vebindungsvektor und verwendet dessen
	 * Laenge
	 * 
	 * @param point1
	 *            Eingabepunkt 1
	 * @param point2
	 *            Eingabepunkt 2
	 * 
	 * @return Abstand der beiden Eingabepunkte als Gleitpunktzahl
	 */
	public float calculatePointPointDistance(final MyVector3f point1,
			final MyVector3f point2) {

		MyVector3f difference = new MyVector3f();
		difference.sub(point1, point2);

		float result = difference.length();

		return result;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Berechnet den senkrechten Abstand des Punktes von der Ebene
	 * 
	 * @param point
	 *            Eingabepunkt
	 * @param plane
	 *            Eingabeebene
	 * 
	 * @return Senkrechter Abstand des Punktes von der Ebene
	 */
	public float calculatePointPlaneDistance(final MyVector3f point,
			final Plane plane) {

		// teste zunaechst, ob der Punkt in der Ebene liegt
		if (isPointOnPlane(point, plane))
			return 0.0f;

		// erzeuge einen Strahl vom Punkt auf die Ebene
		Ray ray = new Ray(point, plane.getNormal());
		// MyVector3f intersection = calculateRayPlaneIntersection(ray, plane);

		MyVector3f intersection = calculateRayPlaneIntersection(ray, plane);

		// es muss einen Schnittpunkt geben
		if (intersection != null) {
			MyVector3f difference = new MyVector3f();
			difference.sub(intersection, point);

			return difference.length();
		} else
			return 0.0f;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Berechnung des Schnittpuntkes von Strahl und Ebene
	 * http://theorie.informatik
	 * .uni-ulm.de/Lehre/WS9798/Computergrafik/Engels/node6.html
	 * http://softsurfer.com/Archive/algorithm_0104/algorithm_0104B.htm
	 * 
	 * @param ray
	 *            Strahl, der auf Schnitte mit der Ebene getestet wird
	 * @param plane
	 *            Testebene
	 * @return Schnittpunkt, falls der Strahl die Ebene trifft, null sonst
	 */
	public MyVector3f calculateRayPlaneIntersection(Ray ray, Plane plane) {

		MyVector3f stuetzvektorGerade = ray.getStart();
		MyVector3f richtungsvektorGerade = ray.getDirection();
		richtungsvektorGerade.normalize();

		// teste auf Parallelitaet des Strahls mit der Geraden
		double angle = calculateAngleRadians(richtungsvektorGerade,
				plane.getNormalPtr());
		double halfPi = Math.PI / 2.0d;

		// wenn Normale und Gerade senkrecht aufeinander stehen, verlaueft der
		// Strahl parallel zur Ebene
		if (isWithinTolerance(angle, halfPi, TOLERANCE))
			return null;

		Double[] koeffizienten = plane.getKoeffizienten();

		double zaehler = (koeffizienten[0] * stuetzvektorGerade.x
				+ koeffizienten[1] * stuetzvektorGerade.y + koeffizienten[2]
				* stuetzvektorGerade.z + koeffizienten[3]);
		zaehler *= -1.0f;

		double nenner = (koeffizienten[0] * richtungsvektorGerade.x
				+ koeffizienten[1] * richtungsvektorGerade.y + koeffizienten[2]
				* richtungsvektorGerade.z);

		// wenn der Nenner 0 ist, so wird die Ebene nicht getroffen
		if (nenner == 0.0f) {
			return null;
		}

		// sonst berechne t
		double result = (zaehler / nenner);

		// wenn der Bruch < 0 ist, trifft der Strahl die Ebene nicht, drehe in
		// diesem Fall die Strahlrichtung um und berechne den Schnittpunkt
		// erneut!
		if (result < 0.0d) {
			ray.getDirectionPtr().scale(-1.0f);
			return calculateRayPlaneIntersection(ray, plane);
		} else {
			MyVector3f resultVector = new MyVector3f();

			richtungsvektorGerade.scale((float) result);
			resultVector.add(stuetzvektorGerade, richtungsvektorGerade);

			assert isPointOnPlane(resultVector, plane) : "FEHLER: Berechneter Durchstosspunkt "
					+ resultVector
					+ " liegt nicht auf der Zielebene "
					+ plane
					+ "!";
			// assert isPointOnRay(resultVector, ray):
			// "FEHLER: Der berechnete Durchstosspunkt " + resultVector +
			// " befindet sich nicht auf dem Eingabestrahl " + ray + "!";
			return resultVector;
		}

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode berechnet einen orthogonalen Vektor zum uebergebenen
	 * Source-Vektor basierend auf einer 90°-Drehung um die Vertex-Normale
	 * 
	 * @param source
	 *            zu drehender Eingabevektor
	 * @param normal
	 *            Normalenvektor am Fusspunkt des Eingabevektors
	 * @return Der um den Normalenvektor gedrehte Eingabevektor
	 * 
	 */
	public MyVector3f calculateOrthogonalVectorWithSamePlane(
			final MyVector3f source, final MyVector3f normal) {

		MyVector3f orthogonal = new MyVector3f();
		orthogonal.cross(source, normal);
		orthogonal.normalize();

		return orthogonal;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Diese Funktion rotiert den uebergebenen Vektor um die uebergebene Achse
	 * um den uebergebenen Winkel. Ansatz basiert auf der Aufstellung einer
	 * Rotationsmatrix basierend auf der Rotationsachse. Diese muss normalisiert
	 * werden, damit es sich um einen Einheitsvektor handelt. Definition
	 * Drehmatrix um beliebige Achse:
	 * http://de.wikipedia.org/wiki/Drehmatrix#Drehmatrizen_des_Raumes_R.C2.B3
	 * Variablennamen orientieren sich am Wikipedia-Artikel
	 * 
	 * @param axis
	 *            Rotationsachse
	 * @param vector
	 *            Vektor, der um die Rotationsachse rotiert werden soll
	 * @param angle
	 *            Rotationswinkel in Grad
	 * @return Rotierter Vektor
	 * 
	 */

	public MyVector3f calculateRotatedVector(final MyVector3f axis,
			final MyVector3f vector, float angle) {

		float localAngle = angle;
		assert axis != null : "Drehachse ist NULL";
		assert vector != null : "Eingabevektor ist NULL";

		MyVector3f rotationAxis = axis.clone();
		rotationAxis.normalize();

		boolean negativeAngle = false;

		// bei negativen Winkeln nutzt man die Eigenschaften von Sinus und
		// Kosinus aus, um die Sonderfaelle nutzen zu koennen
		if (localAngle < 0.0f) {
			negativeAngle = true;
			localAngle *= -1.0f;
		}

		double cosAngle, sinAngle;

		// fuer zentrale Werte die Winkel exakt angeben
		if (localAngle == 90.0f) {
			cosAngle = 0.0d;
			sinAngle = 1.0d;
		} else if (localAngle == 180.0f) {
			cosAngle = -1.0d;
			sinAngle = 0.0d;
		} else if (localAngle == 270.0f) {
			cosAngle = 0.0d;
			sinAngle = -1.0d;
		} else if (localAngle == 360.0f) {
			sinAngle = 0.0d;
			cosAngle = 1.0d;
		}
		// wenn kein Vielfaches von pi verwendet wird, bemuehe die
		// trigonometrischen Funktionen
		else {
			// Eingabe in trigonometrische Funktionen sind Winkel im bogenmaß
			cosAngle = Math.cos(Math.toRadians(localAngle));
			sinAngle = Math.sin(Math.toRadians(localAngle));
		}

		// zurueckrechnen => s. Wikipedia
		if (negativeAngle)
			sinAngle *= -1.0f;

		return rotateVector(rotationAxis, vector, cosAngle, sinAngle);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode fuerht eine Rotation des uebergebenen Vertex um die uebergebene
	 * Achse um den uebergebenen Winkel durch. Dabei wird die Rotation derart
	 * durchgefuehrt, dass zunaechst der Vektor in Richtung des Startpunktes der
	 * Ursprungsgerade verschoben wird, dann erfolgt die Rotation, gefolgt von
	 * einer Translation zurueck an den Ausgangspunkt.
	 * 
	 * @param rotationAxis
	 *            Rotationsachse, beschrieben durch einen Strahl
	 * @param vector
	 *            Vektor, der um die Achse rotiert werden soll
	 * @param angleRad
	 *            Rotationswinkel in Rad
	 * @return Rotierter Vector
	 */
	public MyVector3f calculateRotatedVectorArbitraryRotationRad(
			final Ray rotationAxis, final MyVector3f vector,
			final double angleRad) {

		MyVector3f localVector = vector.clone();
		LOGGER.trace("LOCAL BEFORE: " + localVector + " Rotationsachse: "
				+ rotationAxis.getDirectionPtr());

		// verschiebe die Achse in den Ursprung (damit auch den Punkt)
		MyVector3f originTranslation = rotationAxis.getStart();
		originTranslation.scale(-1.0f);

		// Punkt verschieben
		localVector.add(originTranslation);

		// Vektor rotieren
		localVector = calculateRotatedVectorRadians(
				rotationAxis.getDirectionPtr(), localVector, angleRad);
		LOGGER.trace("LOCAL: " + localVector);

		// wieder an den Ausgangspunkt verschieben
		originTranslation = rotationAxis.getStartPtr();
		localVector.add(originTranslation);
		return localVector;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Diese Funktion rotiert den uebergebenen Vektor um die uebergebene Achse
	 * um den uebergebenen Winkel. Ansatz basiert auf der Aufstellung einer
	 * Rotationsmatrix basierend auf der Rotationsachse. Diese muss normalisiert
	 * werden, damit es sich um einen Einheitsvektor handelt. Definition
	 * Drehmatrix um beliebige Achse:
	 * http://de.wikipedia.org/wiki/Drehmatrix#Drehmatrizen_des_Raumes_R.C2.B3
	 * Variablennamen orientieren sich am Wikipedia-Artikel
	 * 
	 * @param axis
	 *            Rotationsachse
	 * @param vector
	 *            Vektor, der um die Rotationsachse rotiert werden soll
	 * @param angle
	 *            Rotationswinkel in Radians
	 * @return Rotierter Vektor
	 * 
	 */

	public MyVector3f calculateRotatedVectorRadians(final MyVector3f axis,
			final MyVector3f vector, double angle) {

		double localAngle = angle;
		assert axis != null : "Drehachse ist NULL";
		assert vector != null : "Eingabevektor ist NULL";

		final MyVector3f rotationAxis = axis.clone();
		rotationAxis.normalize();

		double cosAngle = 0.0d, sinAngle = 0.0d;
		double pi = Math.PI;
		Double tempResult;
		double roundAccuracy = 10000.0d;
		boolean foundExceptionalCase = false;
		boolean negativeAngle = false;

		// Winkel < 0.0d werden wie positive Winkel verarbeitet, anschliessend
		// verwendet man die Eigenschaften von Sinus und Kosinus
		if (localAngle < 0.0d) {
			negativeAngle = true;
			localAngle *= -1.0d;
		}

		// fuer zentrale Werte die Winkel exakt angeben
		tempResult = round(localAngle - pi / 2.0, roundAccuracy);
		if (tempResult == 0.0d) {
			cosAngle = 0.0d;
			sinAngle = 1.0d;
			foundExceptionalCase = true;
		}

		tempResult = round(localAngle - pi, roundAccuracy);
		if (tempResult == 0.0d) {
			cosAngle = -1.0d;
			sinAngle = 0.0d;
			foundExceptionalCase = true;
		}

		tempResult = round(localAngle - pi * 1.5d, roundAccuracy);
		if (tempResult == pi * 1.5d) {
			cosAngle = 0.0d;
			sinAngle = -1.0d;
			foundExceptionalCase = true;
		}

		tempResult = round(localAngle - pi * 2.0d, roundAccuracy);
		if (tempResult == pi * 2.0d) {
			sinAngle = 0.0d;
			cosAngle = 1.0d;
			foundExceptionalCase = true;
		}

		// wenn kein Vielfaches von pi verwendet wird, bemuehe die
		// trigonometrischen Funktionen
		if (!foundExceptionalCase) {

			// Eingabe in trigonometrische Funktionen sind Winkel im bogenmaß
			cosAngle = Math.cos(localAngle);
			sinAngle = Math.sin(localAngle);
		}

		// wenn der Einhabewinkel < 0 war, gilt: sin (-alpha) = -sin (alpha) und
		// cos (-alpha) = cos (alpha)
		if (negativeAngle)
			sinAngle *= -1.0d;
		return rotateVector(rotationAxis, vector, cosAngle, sinAngle);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode fuehrt die Matrixoperationen fuer die Rotationen des uebergebenen
	 * Vektors um die uebergebene Rotationsachse durch
	 * 
	 * @param rotationAxis
	 *            Rotationsachse
	 * @param vector
	 *            Zu rotierender Vektor
	 * @param cosAngle
	 *            Kosinus-Winkel
	 * @param sinAngle
	 *            Sinus-Winkel
	 * @return Rotierter Vektor
	 */
	private MyVector3f rotateVector(MyVector3f rotationAxis, MyVector3f vector,
			double cosAngle, double sinAngle) {
		double v1 = rotationAxis.x;
		double v2 = rotationAxis.y;
		double v3 = rotationAxis.z;

		// erstelle eine 3*3-Matrix als eindimensionales Array
		// Zaehlung erfolgt zeileweise, also m00, m01, m02, m10 etc.
		double[] matrixNumbers = new double[9];
		matrixNumbers[0] = (cosAngle + Math.pow(v1, 2) * (1 - cosAngle));
		matrixNumbers[1] = (v1 * v2 * (1 - cosAngle) - v3 * sinAngle);
		matrixNumbers[2] = (v1 * v3 * (1 - cosAngle) + v2 * sinAngle);
		matrixNumbers[3] = (v2 * v1 * (1 - cosAngle) + v3 * sinAngle);
		matrixNumbers[4] = (cosAngle + Math.pow(v2, 2) * (1 - cosAngle));
		matrixNumbers[5] = (v2 * v3 * (1 - cosAngle) - v1 * sinAngle);
		matrixNumbers[6] = (v3 * v1 * (1 - cosAngle) - v2 * sinAngle);
		matrixNumbers[7] = (v3 * v2 * (1 - cosAngle) + v1 * sinAngle);
		matrixNumbers[8] = (cosAngle + Math.pow(v3, 2) * (1 - cosAngle));

		// initialisiere die Matrix
		MyMatrix3d matrix = new MyMatrix3d(matrixNumbers);

		// wandle den Eingabevektor in ein Tuple um, um es mit der Matrix zu
		// transformieren
		MyVector3d vectorJava3d = new MyVector3d(vector.x, vector.y, vector.z);

		// transformiere den Eingabevektor mittels der Roationsmatrix
		// matrix.transform(vectorJava3d);S
		matrix.transform(vectorJava3d);

		// schreibe die Werte zurueck in eine MyVector3f-Struktur
		// result = new MyVector3f(vectorJava3d.x, vectorJava3d.y,
		// (vectorJava3d.z* (-1)));
		MyVector3f result = new MyVector3f(vectorJava3d.x, vectorJava3d.y,
				vectorJava3d.z);

		// result = roundVector3f(result, 10000.0f);
		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode rotiert das uebergebene Polygon um die uebergebene Achse und den
	 * uebergebenen Winkel. Dabei werden zunaechst alle Polygonvertices derart
	 * verschobem, dass der Startpunkt der Rotationsachse im Ursprung ligt,
	 * anschliessend erfolgt die inverse Translation
	 * 
	 * @param axis
	 *            Beliebige Rotationsachse
	 * @param poly
	 *            Polygon, das um diese Achse rotiert werden soll
	 * @param angleRad
	 *            Rotationswinkel
	 */
	public void rotatePolygonAroundArbitraryAxis(final Ray axis,
			final MyPolygon poly, final double angleRad) {

		// verschiebe zunaechst das Polygon derart, dass der Startpunkt der
		// Achse den Ursprung bildet
		final MyVector3f originTranslation = axis.getStart();
		originTranslation.scale(-1.0f);
		poly.translate(originTranslation);

		// alle Vertices um die Achse rotieren
		for (Vertex3d currentVertex : poly.getVertices()) {

			// Punkte auf der Drehachse muessen nicht rotiert werden
			if (isPointOnRay(currentVertex.getPositionPtr(), axis)) {
				continue;
			}
			currentVertex
					.setPosition(calculateRotatedVectorRadians(
							axis.getDirection(), currentVertex.getPosition(),
							angleRad));
		}

		// inverse Translation durchfuehren
		originTranslation.scale(-1.0f);
		poly.translate(originTranslation);

		// aktualisiere das Polygon
		poly.update();

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet den Winkel zwischen zwei Vektoren, Ergebnis enthaelt
	 * Fehler, da die Umwandlung von Radians in Degrees nicht exakt ist
	 * 
	 * @param vec1
	 *            Eingabevektor1
	 * @param vec2
	 *            Eingabevekto2
	 * @return Winkel zwischen den Vektoren in Grad
	 */
	public float calculateAngle(final MyVector3f vec1, final MyVector3f vec2) {

		vec1.normalize();
		vec2.normalize();
		return (float) Math.toDegrees(Math.acos(vec1.dot(vec2)));
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet den Winkel zwischen zwei Vektoren, Ergebnis enthaelt
	 * Fehler, da die Umwandlung von Radians in Degrees nicht exakt ist
	 * 
	 * @param vec1
	 *            Eingabevektor1
	 * @param vec2
	 *            Eingabevekto2
	 * @return Winkel zwischen den Vektoren in Radians
	 */
	public double calculateAngleRadians(final MyVector3f vec1,
			final MyVector3f vec2) {

		final MyVector3f locVec1 = vec1.clone();
		final MyVector3f locVec2 = vec2.clone();
		locVec1.normalize();
		locVec2.normalize();

		float punkt = locVec1.dot(locVec2);

		// Wertebereichscheck, kann bei bestimmten Kombinationen out-of-range
		// laufen
		if (punkt > 1.0f)
			punkt = 1.0f;
		else if (punkt < -1.0f)
			punkt = -1.0f;
		return Math.acos(punkt);

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode berechnet die Schnittegerade zweier Ebenen, sofern eine solche
	 * existiert. http://geomalgorithms.com/a05-_intersect-1.html
	 * 
	 * @param plane1
	 *            Erste Eingabeebene
	 * @param plane2
	 *            Zweite Eingabeebene
	 * @return Schnittgerade, sofern diese existiert, null sonst
	 */
	public Ray calculatePlane2PlaneIntersection(final Plane plane1,
			final Plane plane2) {

		// Richtung der Schnittgeraden bestimmen
		MyVector3f direction = new MyVector3f();
		direction.cross(plane1.getNormalPtr(), plane2.getNormalPtr());
		float ax = (direction.x >= 0 ? direction.x : -direction.x);
		float ay = (direction.y >= 0 ? direction.y : -direction.y);
		float az = (direction.z >= 0 ? direction.z : -direction.z);

		// wenn die Ebenen parallel sind, existiert keine Schnittgerade
		if ((ax + ay + az) < SMALL_NUM) {
			return null;
		}

		// bestimme die Komponente mit dem groessten Absoultwert
		Axis maxAxis = getIgnorableAxis(direction, false);

		// bestimme einen Punkt auf der Schnittgeraden
		// setze die Komponente mit dem maximalen Wert auf 0 und loese fuer die
		// anderen beiden Komponenten
		MyVector3f intersectionPoint = new MyVector3f();
		float d1, d2; // the constants in the 2 plane equations
		d1 = plane1.getNormalPtr().dot(plane1.getStuetzvektorPtr());
		d2 = plane2.getNormalPtr().dot(plane2.getStuetzvektorPtr());

		LOGGER.info("ACHSE: " + maxAxis + " Normal: " + direction);

		switch (maxAxis) { // select max coordinate
		case X: // intersect with x=0
			intersectionPoint.x = 0.0f;
			intersectionPoint.y = (d2 * plane1.getNormalPtr().z - d1
					* plane2.getNormalPtr().z)
					/ direction.x;
			intersectionPoint.z = (d1 * plane2.getNormalPtr().y - d2
					* plane1.getNormalPtr().y)
					/ direction.x;
			break;
		case Y: // intersect with y=0
			intersectionPoint.x = (d1 * plane2.getNormalPtr().z - d2
					* plane1.getNormalPtr().z)
					/ direction.y;
			intersectionPoint.y = 0.0f;
			intersectionPoint.z = (d2 * plane1.getNormalPtr().x - d1
					* plane2.getNormalPtr().x)
					/ direction.y;
			break;
		case Z: // intersect with z=0
			intersectionPoint.x = (d2 * plane1.getNormalPtr().y - d1
					* plane2.getNormalPtr().y)
					/ direction.z;
			intersectionPoint.y = (d1 * plane2.getNormalPtr().x - d2
					* plane1.getNormalPtr().x)
					/ direction.z;
			intersectionPoint.z = 0.0f;
			break;
		default:
			LOGGER.error("Ungueltige Achse: " + maxAxis);
			return null;
		}
		return new Ray(intersectionPoint, direction);

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode testet Sonderfaelle bei Winkeln zwischen Vektoren ueber die
	 * direkte Auswertung des Radian-Masses, dadurch koennen bei solchen
	 * Sonderfaellen Rundungsfehler vermieden werden
	 * 
	 * @param vec1
	 *            Eingabevektor1
	 * @param vec2
	 *            Eingabevektor2
	 * @return True, falls der Winkel zwischen den Vektoren 0 oder ein
	 *         Vielfaches von PI/2 = 90° ist, false sonst
	 */
	public boolean isAngleMultipleOf90(MyVector3f vec1, MyVector3f vec2) {
		double halfPi = Math.PI / 2.0d;

		vec1.normalize();
		vec2.normalize();

		double winkel = Math.acos(vec1.dot(vec2));

		// ist der Winkel 0?
		if (isWithinTolerance(winkel, 0.0d, 1000.0d))
			return true;

		// teste, ob der Winkel ein Vielfaches von PI / 2 ist
		double timesPi = winkel % halfPi;

		// teste, ob der winkel ein Vielfaches von PI ist
		if (isWithinTolerance(timesPi, 0.0d, 100.0d))
			return true;
		return false;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erechnet die Richtung der Schnittgeraden zweier Ebenen gibt null
	 * zurueck, wenn die Ebenen parallel oder identisch sind, sonst den
	 * Richtungsvektor
	 * 
	 * @param plane1
	 *            Eingabeebene1
	 * @param plane2
	 *            Eingabeebene2
	 * @return Vektor, der den Schnitt zwischen den beiden Ebenen beschreibt,
	 *         null sonst
	 */
	public MyVector3f calculatePlane2PlaneIntersectionDirection(
			final Plane plane1, final Plane plane2) {

		MyVector3f normalPlane1 = plane1.getNormal();
		MyVector3f normalPlane2 = plane2.getNormal();

		normalPlane1.normalize();
		normalPlane2.normalize();

		// teste zunaechst, ob die Ebenen parallel sind => wenn ja, breche ab
		if (isParallel(normalPlane1, normalPlane2)) {
			LOGGER.info("NormalPlane1: " + normalPlane1 + " NormalPlane2: "
					+ normalPlane2 + " Winkel: "
					+ calculateAngle(normalPlane1, normalPlane2));
			LOGGER.info("Ebenen sind parallel");
			return null;
		}

		// sonst bestimme einen Normalenvektor fuer die Schnittgerade
		MyVector3f rayNormal = new MyVector3f();
		rayNormal.cross(normalPlane1, normalPlane2);
		rayNormal.normalize();

		return rayNormal;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet den Parameter k in der Punkt-Richtungsdarstellung der
	 * Geraden Geht davon aus, dass der Punkt auf der Geraden liegt
	 * 
	 * @param point
	 *            Punkt auf dem Strahl
	 * @param ray
	 *            Strahl
	 * 
	 * @return Parameter in der Punkt-Richtungsdarstellung der Geraden, der bei
	 *         Einsetzen den Punkt ergibt
	 */
	public double calculateParameterOnRayForPoint(final MyVector3f point,
			final Ray ray) {

		// assert isPointOnRay(point, ray) :
		// "Punkt liegt nicht auf dem Strahl, Parameter kann nicht berechnet werden";

		double[] koeffizienten = new double[3];

		MyVector3f richtungsvektor = ray.getDirection();
		MyVector3f stuetzvektor = ray.getStart();

		// hole den Stuetzvektor auf die andere Seite
		stuetzvektor.scale(-1.0f);

		koeffizienten[0] = richtungsvektor.x;
		koeffizienten[1] = richtungsvektor.y;
		koeffizienten[2] = richtungsvektor.z;

		MyVector3f vectorRHS = new MyVector3f();
		vectorRHS.add(point, stuetzvektor);

		double[] rhsKoeffizienten = new double[3];
		rhsKoeffizienten[0] = vectorRHS.x;
		rhsKoeffizienten[1] = vectorRHS.y;
		rhsKoeffizienten[2] = vectorRHS.z;

		DoubleMatrix2D lhs = new DenseDoubleMatrix2D(3, 1);
		lhs.setQuick(0, 0, koeffizienten[0]);
		lhs.setQuick(1, 0, koeffizienten[1]);
		lhs.setQuick(2, 0, koeffizienten[2]);

		DoubleMatrix2D rhs = new DenseDoubleMatrix2D(3, 1);
		rhs.setQuick(0, 0, rhsKoeffizienten[0]);
		rhs.setQuick(1, 0, rhsKoeffizienten[1]);
		rhs.setQuick(2, 0, rhsKoeffizienten[2]);

		DoubleMatrix2D result = null;
		Algebra algebra = new Algebra();

		/*
		 * Matrix lhs = new Matrix(koeffizienten, 3); Matrix rhs = new
		 * Matrix(rhsKoeffizienten, 3);
		 * 
		 * Matrix result = null;
		 */
		/*
		 * try { result = lhs.solve(rhs); } catch (RuntimeException e) { return
		 * Float.NaN; }
		 */
		try {
			result = algebra.solve(lhs, rhs);
		} catch (RuntimeException e) {
			return Float.NaN;
		}

		double x1 = result.get(0, 0);

		// Probe:
		/*
		 * stuetzvektor = ray.getStart(); MyVector3f testpunkt = new
		 * MyVector3f(); richtungsvektor.scale((float) x1);
		 * testpunkt.add(stuetzvektor, richtungsvektor);
		 */

		return x1;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Berechnet den Schnittpunkt 3er Ebenen ueber den JAMA-Matrix-Solver
	 * 
	 * @param plane1
	 *            Eingabeebene 1
	 * @param plane2
	 *            Eingabeebene 2
	 * @param plane3
	 *            Eingabeebene 3
	 * @return Schnittpunkt der 3 Ebenen
	 */
	public MyVector3f calculatePlanePlanePlaneIntersection(Plane plane1,
			Plane plane2, Plane plane3) {

		// verwende die parameterfreien Darstellungen den Ebene, um eine Matrix
		// aufzustellen
		final Double[] koeffizienten1 = plane1.getKoeffizienten();
		final Double[] koeffizienten2 = plane2.getKoeffizienten();
		final Double[] koeffizienten3 = plane3.getKoeffizienten();

		// Matrix ueber die Koeffizienten befuellen
		final double[][] matrixInput = new double[3][3];

		// 1. Ebene
		matrixInput[0][0] = koeffizienten1[0];
		matrixInput[0][1] = koeffizienten1[1];
		matrixInput[0][2] = koeffizienten1[2];

		// 2.Ebene
		matrixInput[1][0] = koeffizienten2[0];
		matrixInput[1][1] = koeffizienten2[1];
		matrixInput[1][2] = koeffizienten2[2];

		// 3.Ebene
		matrixInput[2][0] = koeffizienten3[0];
		matrixInput[2][1] = koeffizienten3[1];
		matrixInput[2][2] = koeffizienten3[2];

		// rechte Seite des Gleichungssystems
		// multipliziere mit -1, um die Werte auf die andere Seite zu holen
		final double[] rhsVals = new double[3];
		rhsVals[0] = koeffizienten1[3] * -1;
		rhsVals[1] = koeffizienten2[3] * -1;
		rhsVals[2] = koeffizienten3[3] * -1;

		final DoubleMatrix2D lhs = new DenseDoubleMatrix2D(matrixInput);
		final DoubleMatrix2D rhs = new DenseDoubleMatrix2D(3, 1);
		rhs.setQuick(0, 0, rhsVals[0]);
		rhs.setQuick(1, 0, rhsVals[1]);
		rhs.setQuick(2, 0, rhsVals[2]);
		final Algebra algebra = new Algebra();
		DoubleMatrix2D result = null;

		/*
		 * // erzeuge JAMA-Matrizen Matrix koeffizienten = new
		 * Matrix(matrixInput); Matrix rhs = new Matrix(rhsVals, 3);
		 * 
		 * Matrix result = null;
		 * 
		 * try { result = koeffizienten.solve(rhs); }
		 */
		try {
			result = algebra.solve(lhs, rhs);
		}
		// wenn diese Exception ausgeloest wurde, dann ist das Gleichungssystem
		// nicht loesbar
		catch (RuntimeException e) {
			return null;
		}

		final MyVector3f schnittpunkt = new MyVector3f();
		schnittpunkt.x = (float) result.get(0, 0);
		schnittpunkt.y = (float) result.get(1, 0);
		schnittpunkt.z = (float) result.get(2, 0);

		return schnittpunkt;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Berechnet fuer das uebergebene Trapez dessen Hoehe. Geht davon aus, das
	 * jeweils 2 Punkte auf der gleichen Hoehe liegen. Sonst ist die Hoehe nicht
	 * eindeutig definiert und es handelt sich um ein Polygon und nicht um ein
	 * Trapez. Die Hoehe wird bestimmt als senkrechter Abstand eines Eckpunkts
	 * auf der oberen Kante zur unteren Kante des Trapez
	 * 
	 * Methode kann ins Haupt-Mathe-Paket verschoben werden, da keine
	 * algorithmenspezifischen Strukturen verwendet werden
	 * 
	 * @param quad
	 *            Vector mit 4 Punkten im 3-dimensionalen Raum. Auf der gleichen
	 *            Hoehe befinden sich die Punkte mit Indices 0 und 1 (obere
	 *            Kante), sowie mit Indices 2 und 3 (untere Kante)
	 * @return Abstand von der oberen Kante zur unteren Kante des Trapez
	 * 
	 */
	public float calculateQuadHeight(List<Vertex3d> quad) {

		assert quad.size() == 4 : "Die Anzahl der Vertices betraegt "
				+ quad.size() + ", es handelt sich nicht um ein Quad-Element.";
		Vertex3d untenStart = quad.get(0);
		Vertex3d untenEnd = quad.get(1);
		Vertex3d obenEnd = quad.get(2);

		// Strahl fuer die untere Kante erzeugen
		MyVector3f direction = new MyVector3f();
		direction.sub(untenEnd.getPosition(), untenStart.getPosition());
		Ray ray = new Ray(untenStart.getPosition(), direction);

		return calculatePointEdgeDistance(obenEnd.getPosition(), ray);

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet den Winkel zwischen einer Ebene und einem uebergebenen
	 * Vektor
	 * 
	 * @param plane
	 *            Ebene, zu der der Winkel bestimmt wird
	 * @param direction
	 *            Richtungsvektor, dessen Winkel zur Ebene berechnet wird
	 * @return Winkel
	 */

	public Float calculatePlaneDirectionAngle(Plane plane, MyVector3f direction) {

		// berechne den Winkel zwischen Normalenvektor und Richtungsvektor
		MyVector3f planeNormal = plane.getNormal();

		Float angle = calculateAngle(planeNormal, direction);

		// da der Normalenvektor senkrecht auf der Ebene steht, muss man seine
		// 90° verrechnen
		Float result = angle - 90.0f;
		if (result < 0)
			result = 90.0f - angle;
		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet fuer vier uebergebene Punkte deren Mittelpunkt ueber
	 * die Schnittpunktbestimmung der Diagonalen. Hierbei geht die Methode davon
	 * aus, dass die Punkte einen konvexen Linienzug bilden und im Uhrzeigersinn
	 * definiert sind (erster Punkt links oben)
	 * 
	 * @param points
	 *            Eingabepunkte, deren Mittelpunkt bestimmt werden soll
	 * @return Vektor, der den Mittelpunkt der 4 Eingabepunkte beschreibt
	 */
	public MyVector3f calculateQuadCenter(List<Vertex3d> points) {

		assert points.size() == 4 : "FEHLER: Es wurden " + points.size()
				+ " Punkte eingegeben, es sind nur 4 Punkte erlaubt";

		// bestimme die beiden Diagonalen
		MyVector3f upperLeftCorner = points.get(0).getPosition();
		MyVector3f upperRightCorner = points.get(1).getPosition();
		MyVector3f lowerRightCorner = points.get(2).getPosition();
		MyVector3f lowerLeftCorner = points.get(3).getPosition();

		MyVector3f upperLeftToLowerRight = new MyVector3f();
		upperLeftToLowerRight.sub(lowerRightCorner, upperLeftCorner);

		MyVector3f upperRightToLowerLeft = new MyVector3f();
		upperRightToLowerLeft.sub(lowerLeftCorner, upperRightCorner);

		Ray leftToRight = new Ray(upperLeftCorner, upperLeftToLowerRight);
		Ray rightToLeft = new Ray(upperRightCorner, upperRightToLowerLeft);

		// berechne den Schnittpunkt der Diagonalen
		MyVector3f intersection = calculateRay2RayIntersectionApproximation(
				leftToRight, rightToLeft);

		assert intersection != null : "FEHLER: Es konnte kein Schnittpunkt der Diagonalen errechnet werden";
		return intersection;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet eine Projektion der uebergebenen Punkte, die bei
	 * Eingabe innerhalb der Base-Plane liegen in die Zielebene. Fuer diese
	 * Punkte wird eine Projektion derart durchgefuehrt, dass die uebergebenen
	 * Punkte aus der Quell- in die Zielebene ueberfuehrt werden. Sind die
	 * beiden Ebenen parallel, werden die Vertices einfach ueber einen
	 * Translationsvektorn in die Zielebene verschoben, ansonsten berechnet man
	 * eine Schnittgerade der beiden Ebenen, die als Rotationsachse dient.
	 * Anschliessend rotiert man alle uebergebenen Punkte um diese Achse, so
	 * dass die Abstaende erhalten bleiben.
	 * 
	 * @param basePlane
	 *            Eingabeebene, in der sich die uebergebenen Punkte befinden
	 * @param targetPlane
	 *            Zielebene, in der sich die Punkte nach der Rotation befinden
	 *            sollen
	 * @param points
	 *            Punkte, die rotiert werden sollen
	 */
	public void calculatePlaneToPlaneProjectionForPoints(Plane basePlane,
			Plane targetPlane, List<Vertex3d> points) {

		// teste zunaechst, ob die Eingabepunkte tatsaechlich auf der Quellebene
		// liegen
		Iterator<Vertex3d> pointIter = points.iterator();
		Vertex3d currentPoint = null;

		while (pointIter.hasNext()) {
			currentPoint = pointIter.next();
			assert isPointOnPlane(currentPoint.getPosition(), basePlane) : "FEHLER: Punkt "
					+ currentPoint
					+ " liegt nicht auf der Eingabeebene: "
					+ basePlane;
		}

		// fuehre nun die Berechnungen durch
		// teste zunaechst, ob die Ebenen parallel sind, wenn ja, verschiebe
		// alle Punkte in Richtung des Normalenvektors
		if (isParallel(targetPlane.getNormal(), basePlane.getNormal())) {

			// berechne einen Vektor zwischen den beiden Ebenen, ueber den die
			// Kreispunkte verschoben werden
			// Schnittpunkt eines Strahls durch Stuetzpunkt mit Normalenvektor
			// durch andere Ebene
			MyVector3f stuetzvektorXZ = basePlane.getStuetzvektor();
			MyVector3f normalXZ = basePlane.getNormal();

			Ray rayXZ = new Ray(stuetzvektorXZ, normalXZ);

			// durchstosspunkt durch zweite Ebene
			MyVector3f intersection = calculateRayPlaneIntersection(rayXZ,
					targetPlane);
			assert intersection != null : "FEHLER: Fuer die parallelen Ebenen konnte kein Schnittpunkt bestimmt werden";

			MyVector3f verschiebungsVektor = new MyVector3f();
			verschiebungsVektor.sub(intersection, stuetzvektorXZ);

			// verschiebe alle Punkte um den berechneten Verschiebungsvektor
			pointIter = points.iterator();

			while (pointIter.hasNext()) {
				currentPoint = pointIter.next();
				currentPoint.getPositionPtr().add(verschiebungsVektor);
			}
		}
		// Ebenen sind nicht parallel, rotiere alle Kreispunkte um die
		// Schnittgerade der beiden Ebenen
		else {

			MyVector3f intersectionDirection = calculatePlane2PlaneIntersectionDirection(
					targetPlane, basePlane);
			assert intersectionDirection != null : "FEHLER: Es konnte keine Schnittgerade zwischen den beiden Ebenen berechnet werden";

			// berechne den Winkel zwischend en Normalenvektoren
			double angle = calculateAngleRadians(targetPlane.getNormal(),
					basePlane.getNormal());

			// rotiere nun jeden Kreispunkt um die Achse mit dem berechneten
			// Winkel
			pointIter = points.iterator();
			while (pointIter.hasNext()) {
				currentPoint = pointIter.next();
				MyVector3f rotatedDirection = calculateRotatedVectorRadians(
						intersectionDirection, currentPoint.getPosition(),
						angle);
				currentPoint.setPosition(rotatedDirection);
			}

		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet eine Projektion der uebergebenen Punkte, die bei
	 * Eingabe innerhalb der Base-Plane liegen in die Zielebene. Dabei geht man
	 * so vor, dass die neue Position durch Schnittpunktbestimmungen ausgehend
	 * von der Quell- in die Zielebene erfolgen. Fuer jeden Punkt in der
	 * Quellebene berechnet man einen Strahl mit dem Normalenvektor der
	 * QUELLebene und berechnet den Durchstosspunkt in der Zielebene. Dadurch
	 * kommt es zu einer Verzerrung von Laengenverhaeltnisse etc., falls die
	 * Ebenen nicht parallel sind.
	 * 
	 * @param basePlane
	 *            Eingabeebene, in der sich die uebergebenen Punkte befinden
	 * @param targetPlane
	 *            Zielebene, in der sich die Punkte nach der Rotation befinden
	 *            sollen
	 * @param points
	 *            Punkte, die rotiert werden sollen
	 */
	public void calculatePlaneToPlaneProjectionForPointsNoRotation(
			Plane basePlane, Plane targetPlane, List<Vertex3d> points) {

		MyVector3f intersection = null;
		Ray intersectionRay = null;
		Vertex3d currentVertex = null;
		double angleRad = calculateAngleRadians(basePlane.getNormalPtr(),
				targetPlane.getNormalPtr());
		double half_pi = Math.PI / 2.0d;

		// Projektion funktioniert nicht, wenn die Ebenen senkrecht zueinander
		// stehen
		if (angleRad == half_pi) {
			LOGGER.error("Die Eingabeebenen stehen senkrecht aufeinander!");
			return;
		}

		// alle Punkte projizieren und neue Positionen setzen
		for (int i = 0; i < points.size(); i++) {
			currentVertex = points.get(i);
			intersectionRay = new Ray(currentVertex.getPositionPtr(),
					basePlane.getNormalPtr());
			intersection = this.calculateRayPlaneIntersection(intersectionRay,
					targetPlane);
			assert intersection != null : "FEHLER: Es konnte kein Schnittpunkt bestimmt werden!";

			currentVertex.setPosition(intersection);
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet den Abstand zweier Ebenen. Dieser ist nur definiert,
	 * wenn die Ebenen parallel sind
	 * 
	 * @param plane1
	 *            Eingabeebene 1
	 * @param plane2
	 *            Eingabeebene 2
	 * @return Distanz der Ebenene, sofern diese parallel und nicht identisch
	 *         sind, null sonst
	 */
	public Float calculatePlane2PlaneDistance(Plane plane1, Plane plane2) {

		MyVector3f normal1 = plane1.getNormal();
		MyVector3f normal2 = plane2.getNormal();

		// wenn die Ebenen nicht parallel sind, breche ab
		if (!isParallel(normal1, normal2))
			return null;

		// sonst berechne einen Durchstosspunkt eines Strahls in Richtung der
		// Normale von Ebene1 mit Ebene2
		Ray ray = new Ray(plane1.getStuetzvektor(), normal1);
		MyVector3f intersection = calculateRayPlaneIntersection(ray, plane2);
		assert intersection != null : "FEHLER: Es konnte kein Durchstosspunkt des Strahls mit der Ebene gefunden werden";

		// bestimme den Abstand des Durchstosspunktes vom Startpunkt des Strahls
		return calculatePointPointDistance(intersection,
				plane1.getStuetzvektor());

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet den Flaecheninhalt eines beliebigen uebergebenen
	 * Dreiecks durch die Bestimmung des Kreuzprodukts der aufspannenden
	 * Vektoren. Fuer diesen Algorithmus muss einer der Eckpunkte auf (0,0,0)
	 * liegen, darum werden alle Punkte derart verschoben, dass diese
	 * Voraussetzung erfuellt ist, anschliessend bestimmt man das Kreuzprodukt
	 * der beiden Vektoren, die nicht im Koordinatenursprung liegen. Der
	 * Flaecheninhalt entspricht dann der Haelfte des Betrags des Vektors des
	 * Kreuzprodukts. http://de.wikipedia.org/wiki/Dreiecksfläche
	 * 
	 * @param vertices
	 *            Liste mit den Eckpunkten des Dreiecks
	 * @return Flaecheninhalt des Dreiecks
	 */
	private Float calculateTriangleArea(List<Vertex3d> vertices) {

		assert vertices.size() == 3 : "FEHLER: Es wurden " + vertices.size()
				+ " Vertices uebergeben";

		List<Vertex3d> vertexBuffer = new ArrayList<Vertex3d>(vertices.size());

		// verschiebe alle Punkte um den Positionsbetrag des ersten Vertices
		MyVector3f translationVektor = vertices.get(0).getPosition();
		Iterator<Vertex3d> vertIter = vertices.iterator();
		Vertex3d currentVertex = null;

		while (vertIter.hasNext()) {
			currentVertex = vertIter.next().clone();
			currentVertex.getPositionPtr().sub(translationVektor);
			vertexBuffer.add(currentVertex);
		}

		// verwende die beiden Vektoren, die nicht im Urpsrung liegen
		MyVector3f cross = new MyVector3f();
		cross.cross(vertexBuffer.get(1).getPositionPtr(), vertexBuffer.get(2)
				.getPositionPtr());

		// der Betrag entspricht dem Flaecheninhalt des Parallelogramms, darum
		// teile durch 2
		Float result = cross.length();
		result /= 2.0f;

		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet einen Normalenvektor fuer ein gegebenes Face basierend
	 * auf dem Algorithmus von Newell (dadurch werden Probleme bei Kreuzprodukt
	 * durch annaehernd parallele Kanten vermieden
	 * http://books.google.de/books?id
	 * =VBogNNs10zEC&pg=PA200&lpg=PA200&dq=normalenvektor
	 * +polygon+berechnen&source
	 * =bl&ots=THU20RUV9t&sig=d3dlRC4L8HNuSmFX_VqpB-Ky4bk
	 * &hl=de&ei=OzpvTsPNKaHk4QSMko26CQ
	 * &sa=X&oi=book_result&ct=result&resnum=2&ved
	 * =0CCEQ6AEwAQ#v=onepage&q=normalenvektor%20polygon%20berechnen&f=false
	 * 
	 * @param verts
	 *            Liste mit Eckpunkten des Polygons
	 * @return Normalisierter Normalenvektor
	 */
	public MyVector3f calculateNormalNewell(List<Vertex3d> verts) {

		assert verts.size() > 2 : "FEHLER: Der Vertexbuffer enthaelt nur "
				+ verts.size() + " Vertices!";
		float nx = 0, ny = 0, nz = 0;
		Vertex3d currentVert = null, nextVert = null;

		// Normale nach Newell ueber alle Punkte berechnen
		for (int i = 0; i < verts.size(); i++) {
			currentVert = verts.get(i);
			if (i < verts.size() - 1)
				nextVert = verts.get(i + 1);
			else
				nextVert = verts.get(0);
			nx += (currentVert.getY() - nextVert.getY())
					* (currentVert.getZ() + nextVert.getZ());
			ny += (currentVert.getZ() - nextVert.getZ())
					* (currentVert.getX() + nextVert.getX());
			nz += (currentVert.getX() - nextVert.getX())
					* (currentVert.getY() + nextVert.getY());

		}

		MyVector3f result = new MyVector3f(nx, ny, nz);
		result.normalize();

		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet eine Ebenennormale basierend auf dem Kreuzprodukt
	 * 
	 * @param verts
	 *            Liste mit Vertices, die in der Ebene liegen
	 * @return Vektor, der die Normale der Ebene beschreibt
	 */
	public MyVector3f calculateNormalByCrossProduct(List<Vertex3d> verts) {
		assert verts.size() > 2 : "FEHLER: Der uebergebene Vertexbuffer enthaelt nur "
				+ verts.size() + " Vertices!";
		MyVector3f result = new MyVector3f();
		MyVector3f a_b = new MyVector3f();
		a_b.sub(verts.get(1).getPositionPtr(), verts.get(0).getPositionPtr());
		MyVector3f b_c = new MyVector3f();
		b_c.sub(verts.get(2).getPositionPtr(), verts.get(1).getPositionPtr());
		result.cross(a_b, b_c);
		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet eine Ebene, in der die uebergebenen Vertices liegen
	 * 
	 * @param vertices
	 *            Liste mit Vertices, fuer die eine Ebene berechnet werden soll
	 * @return Ebene, die alle uebergebenen Vertices enthaelt
	 */
	public Plane calculatePlaneByVertices(List<Vertex3d> vertices) {

		// erzeuge eine Ebene, die das primitive Objekt enthaelt
		try {

			MyVector3f normal = calculateNormalNewell(vertices);

			// wenn per Newell keine gueltige Normale bestimmt werden konnte,
			// verwende das Kreuzprodukt
			if (normal.isInvalid()) {
				normal = calculateNormalByCrossProduct(vertices);
			}

			// runden auf 100.000stel Stelle => dadurch werden Floating-Point
			// Ungenauigkeiten entfernt => bsw. Abweichung auf 15.
			// Nachkommastelle
			normal = roundVector3f(normal, 100000.0f);

			Plane result = null;
			result = new Plane(normal, vertices.get(0).getPosition());
			return result;
		} catch (AssertionError e) {
			e.printStackTrace();
			LOGGER.error("MyVectormath.calculatePlaneByVertices: ");
			for (Vertex3d currentVertex : vertices) {
				LOGGER.error("mVertices.add(new Vertex3d("
						+ currentVertex.getX() + "f, " + currentVertex.getY()
						+ "f, " + currentVertex.getZ() + "f));");
			}
		}
		return null;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet einen naeherungsweisen Mittelpunkt fuer das Polygon,
	 * das durch die uebergebenen Eckpunkte berechnet wird. Das Verfahren
	 * basiert auf einer Repraesentation des Linienzuges in der XY-Ebene,
	 * anschliessend kann ein 2d-Verfahren fuer die Flaechen- und
	 * Mittelpunktberechnung eingesetzt werden.
	 * http://de.wikipedia.org/wiki/Schwerpunkt#cite_note-0
	 * 
	 * @param vertices
	 *            Eckpunkte des Polygonzuges, dessen Mittelpunkt berechnet
	 *            werden soll
	 * @return Mittelpunkt des beschriebenen Polygons
	 */
	public MyVector3f calculatePolygonCenter(List<Vertex3d> vertices) {

		// wenn es sich um ein Dreieck handelt, verwende die Dreiecksmethode
		if (vertices.size() == 3)
			return calculateTriangleCenter(vertices);

		MyPolygon polygon = new MyPolygon(vertices);
		Vertex3d startPoint = vertices.get(0);
		Float x = 0.0f, y = 0.0f;

		// sonst projiziere saemtliche Eckpunkte in die XY-Ebene
		List<Vertex3d> projectedPoints = calculateXYPlaneProjectionForPoints(
				vertices, polygon.getNormal());
		Float area = calculatePolygonArea2D(projectedPoints);
		Vertex3d current = null, next = null;

		for (int i = 0; i < projectedPoints.size(); i++) {

			current = projectedPoints.get(i);
			next = projectedPoints.get((i + 1) % projectedPoints.size());

			x += (current.getX() + next.getX())
					* (current.getX() * next.getY() - next.getX()
							* current.getY());
			y += (current.getY() + next.getY())
					* (current.getX() * next.getY() - next.getX()
							* current.getY());
		}

		Float scale = 1 / (6 * area);

		x *= scale;
		y *= scale;

		// Mittelpunkt in der projizierten Ebene muss nun zurueck auf Quellebene
		MyVector3f result = null;

		// bestimme die vorab ignorierte Achse, setze die Koordinate eines
		// Eingabepunktes auf diese Komponente und
		// die berechneten Werte auf die verbleibenden Werte
		Axis ignorableAxis = getIgnorableAxis(polygon.getNormal(), false);
		switch (ignorableAxis) {

		case X:
			result = new MyVector3f(startPoint.getX(), y, x);
			break;

		case Y:
			result = new MyVector3f(x, startPoint.getY(), y);
			break;

		case Z:
			result = new MyVector3f(x, y, startPoint.getZ());
			break;

		}

		// nimm nun den geschaetzten Punkt (wegen der Startkoordinate) und
		// berechne die Projektion auf die Zielebene
		Vertex3d resultVert = new Vertex3d(result);
		projectPointOntoPlane(polygon.getPlane(), resultVert);

		return resultVert.getPosition();

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet die Flaeche des Polygons, das durch die uebergebenen
	 * Vertices beschrieben wird. Hierbei geht die Methode davon aus, dass es
	 * sich um ein Polygon in der xy-Ebene handelt, da das verwendete Verfahren
	 * 2-dimensional ist. http://de.wikipedia.org/wiki/Schwerpunkt#cite_note-0
	 * 
	 * @param vertices
	 *            Liste von Vertices, die den Kantenzug bestimmen, dessen
	 *            Flaeche berechnet werden soll
	 * @return Flaecheninhalt des Polygons
	 */
	public Float calculatePolygonArea2D(List<Vertex3d> vertices) {

		Vertex3d current = null, next = null;
		Float area = 0.0f;

		for (int i = 0; i < vertices.size(); i++) {
			current = vertices.get(i);
			if (i == vertices.size() - 1)
				next = vertices.get(0);
			else
				next = vertices.get(i + 1);

			area += current.getX() * next.getY() - next.getX() * current.getY();

		}

		area *= 0.5f;
		return area;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet eine Repraesentation der uebergebenen Vertices in der
	 * xy-Ebene, indem eine Komponente der Koordinaten weggelassen wird, die
	 * restlichen Komponenten werden dann auf x und y gesetzt
	 * 
	 * @param vertices
	 *            Liste mit Vertex3d-Strukturen, deren xy-Repraesentation
	 *            berechnet werden soll
	 * @param normal
	 *            Normalenvektor der Ebene, in der die uebergebenen Punkte
	 *            liegen
	 * @return Liste mit transformierten Vertices
	 */
	public List<Vertex3d> calculateXYPlaneProjectionForPoints(
			List<Vertex3d> vertices, MyVector3f normal) {

		Axis ignorableAxis = getIgnorableAxis(normal, false);
		Iterator<Vertex3d> vertIter = vertices.iterator();

		List<Vertex3d> projectedVertexBuffer = new ArrayList<Vertex3d>(
				vertices.size());

		Vertex3d currentVertex = null;

		// projiziere alle Punkte auf die xy-Ebene (ignoriere die Komponente,
		// die vorab als "irrelevant" bestimmt wurde und setze die anderen
		// Komponenten auf x und y)
		while (vertIter.hasNext()) {
			currentVertex = vertIter.next();
			projectedVertexBuffer.add(createXYPlaneProjectionForPoint(
					currentVertex, ignorableAxis));
		}

		return projectedVertexBuffer;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode berechnet den Mittelpunkt des uebergebenen Dreiecks (beschrieben
	 * durch seine Eckpunkte) als arithmetisches Mittel der Ecken und gibt
	 * dieses zurueck.
	 * 
	 * @param corners
	 *            Eckpunkte des Dreiecks, dessen Mittelpunkt berechnet werden
	 *            soll
	 * @return Dreiecksmittelpunkt
	 */
	public MyVector3f calculateTriangleCenter(List<Vertex3d> corners) {

		Vertex3d vert1 = corners.get(0);
		Vertex3d vert2 = corners.get(1);
		Vertex3d vert3 = corners.get(2);

		// der Mittelpunkt eines Dreiecks ist das arithmethische Mittel seiner
		// Eckpunkte
		float x = (vert1.getX() + vert2.getX() + vert3.getX()) / 3;
		float y = (vert1.getY() + vert2.getY() + vert3.getY()) / 3;
		float z = (vert1.getZ() + vert2.getZ() + vert3.getZ()) / 3;

		return new MyVector3f(x, y, z);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Utilitymethode zur Ausgabe einer Matrix
	 */
	private void print(final double[][] matrix) {

		final String lineBreak = System.getProperty("line.separator");
		String message = "----------------------------------------------------"
				+ lineBreak;

		// Zeilen verarbeiten
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[i].length; j++) {
				message += " " + matrix[i][j];
			}
			message += lineBreak;
			;
		}
		message += "----------------------------------------------------";
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * berechnet den Winkel zwischen zwei Vektoren durch Reduktion der Rechnung
	 * auf 2d-Raeume => geht alle Ebenen durch und bestimmt dort die
	 * Skalarprodukt => gibt dann die Winkel als Vektor zurueck in Bezug auf
	 * alle 3-Ebenen
	 * 
	 * @param input
	 * @param input2
	 * @return
	 */
	public double getFullAngleRad(final MyVector3f input1,
			final MyVector3f input2) {
		MyVector2f rot1 = null, rot2 = null;

		final MyVector3f locInput1 = input1.clone();
		final MyVector3f locInput2 = input2.clone();

		locInput1.normalize();
		locInput2.normalize();

		rot1 = new MyVector2f(locInput1.y, locInput1.z);
		rot2 = new MyVector2f(locInput2.y, locInput2.z);

		double rotDiffx;
		if ((rot1.x == 0.0f && rot1.y == 0.0f)
				|| (rot2.x == 0.0f && rot2.y == 0.0f)) {
			rotDiffx = 0.0d;
		} else {
			rotDiffx = getAngle2dRad(rot1, rot2);
		}

		rot1 = new MyVector2f(locInput1.x, locInput1.z);
		rot2 = new MyVector2f(locInput2.x, locInput2.z);

		double rotDiffy;

		if ((rot1.x == 0.0f && rot1.y == 0.0f)
				|| (rot2.x == 0.0f && rot2.y == 0.0f)) {
			rotDiffy = 0.0d;
		} else {
			rotDiffy = getAngle2dRad(rot1, rot2);
		}

		rot1 = new MyVector2f(locInput1.x, locInput1.y);
		rot2 = new MyVector2f(locInput2.x, locInput2.y);

		double rotDiffz;

		if ((rot1.x == 0.0f && rot1.y == 0.0f)
				|| (rot2.x == 0.0f && rot2.y == 0.0f)) {
			rotDiffz = 0.0d;
		} else {
			rotDiffz = getAngle2dRad(rot1, rot2);
		}

		MyVector3f cross = new MyVector3f();
		cross.cross(locInput1, locInput2);

		// setze die "irrelevanten" Drehwinkel auf 0 => das sind die
		// Komponenten, die im Kreuzprodukt 0 sind
		// um diese wird in der Darstellung nicht rotiert
		if (cross.x == -0.0f || cross.x == 0.0f) {
			cross.x = 0.0f;
			rotDiffx = 0.0d;
		}
		if (cross.y == -0.0f || cross.y == 0.0f) {
			cross.y = 0.0f;
			rotDiffy = 0.0d;
		}
		if (cross.z == -0.0f || cross.z == 0.0f) {
			cross.z = 0.0f;
			rotDiffz = 0.0d;
		}

		double doublePi = Math.PI * 2.0d;
		if (cross.x > 0.0f)
			rotDiffx = doublePi - rotDiffx;
		if (cross.y < 0.0f)
			rotDiffy = doublePi - rotDiffy;
		if (cross.z > 0.0f)
			rotDiffz = doublePi - rotDiffz;

		// jetzt gebe nur die Komponente zurueck, die ungleich 0 ist
		if (rotDiffx != 0.0f)
			return rotDiffx;
		else if (rotDiffy != 0.0f)
			return rotDiffy;
		else if (rotDiffz != 0.0f)
			return rotDiffz;
		else
			return 0.0f;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * 2-dimesionales Skalarprodukt => berechnet das Skalarprodukt der
	 * uebergebenen Vektoren im 2d-Raum
	 * http://www.spieleprogrammierer.de/index.php?page=Thread&threadID=3678
	 * 
	 * @param rot1
	 * @param rot2
	 * @return
	 */
	public double getAngle2dRad(final MyVector2f rot1, final MyVector2f rot2) {

		double grenzwert = 0.0000005f;
		double delta;

		float temp = rot1.x * rot2.x + rot1.y * rot2.y;

		if (temp == 0.0f)
			return Math.PI / 2.0d;

		double x1_Quadrat = (double) Math.pow(rot1.x, 2);
		double x2_Quadrat = (double) Math.pow(rot2.x, 2);
		double y1_Quadrat = (double) Math.pow(rot1.y, 2);
		double y2_Quadrat = (double) Math.pow(rot2.y, 2);

		double sqrt1 = Math.sqrt(x1_Quadrat + y1_Quadrat);
		double sqrt2 = Math.sqrt(x2_Quadrat + y2_Quadrat);

		double bruch = temp / (sqrt1 * sqrt2);

		if (bruch > 1.0d) {
			delta = bruch - 1.0d;
			if (delta < grenzwert)
				bruch = 1.0d;
		}

		if (bruch < -1.0d) {
			delta = bruch + 1.0d;
			delta *= -1;
			if (delta < grenzwert)
				bruch = -1.0d;
		}

		return Math.acos(bruch);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Diese Methode setzt den Faktor in die Geradengleichung des Strahls ein
	 * und gibt den so beschriebenen Punkt zurueck
	 * 
	 * @param ray
	 *            Eingabestrahl
	 * @param factor
	 *            Parameter der Geradengleichung
	 * @return Ortsvektor des Punktes auf dem Strahl fuer den uebergebenen
	 *         Parameter
	 */
	public MyVector3f getPointOnRay(final Ray ray, float factor) {
		MyVector3f result = new MyVector3f();
		MyVector3f startRay = ray.getStartPtr();
		MyVector3f directionRay = ray.getDirectionPtr();

		// Punkt ist startRay + faktor * directionRay
		result.scaleAdd(factor, directionRay, startRay);
		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Berechnet einen Punkt auf dem uebergebenen Strahl mit der geforderten
	 * Zielhoehe
	 * 
	 * @param ray
	 *            Eingabestrahl
	 * @param height
	 *            Zielhoehe
	 * @return Punkt auf dem Eingabestrahl mit der geforderten Zielhoehe
	 */
	public MyVector3f getPointOnRayByHeight(final Ray ray, final float height) {

		MyVector3f stuetzvektor = ray.getStart();
		MyVector3f direction = ray.getDirection();

		// loese die Gleichung
		float parameter = height - stuetzvektor.z;
		parameter /= direction.z;

		// berechne den punkt
		MyVector3f result = ray.getDirection();
		result.scale(parameter);
		result.add(stuetzvektor);

		return result;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Funktion zum Runden von Vektoren basierend auf einer vorgegebenen
	 * Genauigkeit
	 * 
	 * @param input
	 *            Zu rundender Vektor
	 * @return Gerundeter Vektor
	 */
	public MyVector3f roundVector3f(final MyVector3f input) {

		MyVector3f result = null;

		float x = round(input.x);
		float y = round(input.y);
		float z = round(input.z);

		result = new MyVector3f(x, y, z);
		return result;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Rundungsmethode fuer 2-dimensionale Vektoren. Rundet die Koordinaten des
	 * Vektors mit der Standardgenauigkeit
	 * 
	 * @param input
	 *            Vektor, der auf die vorgegebene Genauigkeit gerundet werden
	 *            soll
	 */
	public void roundVector2f(MyVector2f input) {

		float x = round(input.x);
		float y = round(input.y);
		input.x = x;
		input.y = y;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * rundet einen Float-Wert auf die festgelegte Anzahl an Nachkommastellen
	 * 
	 * @param input
	 *            Zu rundender Wert
	 * @return Wert, der auf die festgelegte Anzahl an Nachkommastellen gerundet
	 *         ist
	 */
	public float round(final float input) {
		float result = input;

		result *= ROUNDACCURACY;
		result = Math.round(result);
		result /= ROUNDACCURACY;

		return result;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Rundet einen uebergebenen Wert mit der uebergebenen Genauigkeit und gibt
	 * den gerundeten Wert zurueck
	 * 
	 * @param input
	 *            Zu rundendender Wert
	 * @param accuracy
	 *            Rundungsgenauigkeit
	 * @return gerundeter Wert
	 * 
	 * 
	 */
	public float round(final float input, final float accuracy) {

		float result = input;
		result *= accuracy;
		result = Math.round(result);
		result /= accuracy;
		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Rundet einen uebergebenen Wert mit der uebergebenen Genauigkeit und gibt
	 * den gerundeten Wert zurueck
	 * 
	 * @param input
	 *            Zu rundendender Wert als double
	 * @param accuracy
	 *            Rundungsgenauigkeit
	 * @return gerundeter Wert
	 * 
	 * 
	 */
	public double round(final double input, final double accuracy) {

		double result = input;
		result *= accuracy;
		result = Math.round(result);
		result /= accuracy;
		return result;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Funktion zum Runden von Vektoren mit uebergebener Genauigkeit
	 * 
	 * @param input
	 *            Zu rundender Vektor
	 * @param accuracy
	 *            Rundungsgenauigkeit
	 * @return gerundeter Vektor
	 */
	public MyVector3f roundVector3f(final MyVector3f input, final float accuracy) {

		MyVector3f result = null;

		float x = round(input.x, accuracy);
		float y = round(input.y, accuracy);
		float z = round(input.z, accuracy);

		result = new MyVector3f(x, y, z);
		return result;

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Testet, ob der uebergebene Punkt auf der Gerade liegt, dies ist dann der
	 * Fall, wenn der Abstand zwischen Punkt und Gerade innerhalb eines
	 * vordefinierten Toleranzbereichs liegt
	 * 
	 * @param point
	 *            Eingabepunkt
	 * @param ray
	 *            Eingabestrahl
	 * 
	 * @return True, falls der Punkt auf dem Eingabestrahl liegt, False sonst
	 */
	public boolean isPointOnRay(final MyVector3f point, final Ray ray) {
		// maximale tolerierbare Distanz, so dass der Punkt noch als auf dem
		// Strahl liegend betrachtet wird
		float acceptableDelta = 0.01f;
		float distance = calculatePointEdgeDistance(point, ray);

		distance = Math.abs(distance);
		LOGGER.debug("isPointOnRay Point: " + point + " Ray: " + ray
				+ " Distance: " + distance);
		return isWithinTolerance(distance, 0.0f, acceptableDelta);
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Testet, ob sich der Punkt auf dem Liniensegment befindet, das durch die
	 * PunktRichtungsform beschrieben wird. Dies ist der Fall, falls ein
	 * Parameter k im Bereich von 0 bis 1 gefunden werden kann, so dass das
	 * Einsetzen dieses Parameters den angeforderten Punkt ergibt
	 * 
	 * @param point
	 *            Eingabepunkt
	 * @param ray
	 *            Eingabestrahl
	 * 
	 * @return True, falls ein Parameter gefunden werden konnte, der die
	 *         Bedingungen erfuellt, False sonst
	 */
	public boolean isPointOnLineSegment(final MyVector3f point, final Ray ray) {

		double parameter = calculateParameterOnRayForPoint(point, ray);
		parameter = round(parameter, 10000.0f);

		if (parameter < 0.0f || parameter > 1.0f || parameter == Double.NaN)
			return false;
		return true;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode aehnelt isPointOnLineSegment, testet aber nur, ob ein Punkt in
	 * Richtung des Strahls liegt, sofern dies der Fall ist, muss der berechnete
	 * Parameter > 0 sein
	 * 
	 * @param point
	 *            Punkt, fuer den getestet wird, ob er auf dem Strahl liegt
	 * @param ray
	 *            Strahl, fuer den der Test gerechnet wird
	 * @return True, sofern der Punkt in Richtung des Strahls liegt, False sonst
	 */
	public boolean isPointOnRayDirection(MyVector3f point, Ray ray) {
		double parameter = calculateParameterOnRayForPoint(point, ray);

		if (parameter < 0.0f)
			return false;
		return true;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Testet durch Einsetzen des Punktes in die Koordinatendarstellung der
	 * Ebene, ob der Punkt in der Ebene liegt
	 * 
	 * @param point
	 *            Eingabepunkt
	 * @param plane
	 *            Eingabeebene
	 * 
	 * @return True, falls der Punkt in der Ebene liegt, False sonst
	 */
	public boolean isPointOnPlane(final MyVector3f point, final Plane plane) {

		Double[] koeffizienten = plane.getKoeffizienten();

		double result = point.x * koeffizienten[0] + point.y * koeffizienten[1]
				+ point.z * koeffizienten[2] + koeffizienten[3];

		// System.out.println("isPointOnPlane: Abweichung: " + result);

		return !(Math.abs(result) > TOLERANCE);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Wrapper-Methode fuer isPointInPolygon(), gestattet Uebergabe eines
	 * Vectors anstelle einer Vertex3d-Instanz.
	 * 
	 * @param polygon
	 *            Polygon, fuer das geetestet wird, ob der uebergebene Punkt in
	 *            diesem enthalten ist
	 * @param point
	 *            Punkt, fuer den getestet wird, ob er im Polygon enthalten ist
	 * @return True, sofern der Punkt innerhalb des Polygons liegt, False sonst
	 */
	public Boolean isPointInPolygon(MyPolygon polygon, MyVector3f point) {
		return isPointInPolygon(polygon, new Vertex3d(point));
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode dient der Berechnung einer Koordinatenkomponente, die bei einer
	 * Projektion in eine Koordinatenebene ignoriert werden kann. Hat man Punkte
	 * in der xz-Ebene, so ist die y-Komponente der Ebenennormale 1, somit der
	 * groesste Wert => moechte man nun Punkte aus einer 3d- in eine
	 * 2d-Repraesentation projizieren, so ignoriert man am besten die Achse mit
	 * den groessten Werten im Normalenvektor, dies fuehrt mathematisch zum
	 * kleinsten Fehler. s. John M. Snyder & Alan H. Barr,
	 * "Ray Tracing Complex Models Containing Surface Tessellations", Computer
	 * Graphics 21(4), 119-126 (1987) [also in the Proceedings of SIGGRAPH 1987]
	 * 
	 * @param normal
	 *            Normalenvektor der Ebene, fuer die die zu ignorierende
	 *            Komponente bestimmt werden soll
	 * @param exact
	 *            Flag gibt an, ob die exakte Achse zurückgegeben werden soll
	 *            (also auch unterschieden bzgl. negativer oder positiver
	 *            Achse), oder nicht
	 * @return Achse, die ignoriert werden soll
	 */
	public Axis getIgnorableAxis(final MyVector3f normal, final Boolean exact) {

		Float currentBiggestComponent = -1.0f;

		Axis result = Axis.UNKNOWN;

		if (Math.abs(normal.x) > currentBiggestComponent) {
			currentBiggestComponent = Math.abs(normal.x);
			if (exact && normal.x < 0) {
				result = Axis.NEGATIVE_X;
			} else {
				result = Axis.X;
			}
		}
		if (Math.abs(normal.y) > currentBiggestComponent) {
			currentBiggestComponent = Math.abs(normal.y);
			if (exact && normal.y < 0) {
				result = Axis.NEGATIVE_Y;
			} else {
				result = Axis.Y;
			}
		}
		if (Math.abs(normal.z) > currentBiggestComponent) {
			currentBiggestComponent = Math.abs(normal.z);
			if (exact && normal.z < 0) {
				result = Axis.NEGATIVE_Z;
			} else {
				result = Axis.Z;
			}
		}
		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * http://www.softsurfer.com/Archive/algorithm_0103/algorithm_0103.htm PDF:
	 * PointInPolygonWindingNumber.pdf Winding-Number-Algorithmus fuer
	 * Punkt-in-Polygon-Tests Kernidee ist die Berechnung der Anzahl der
	 * "Umrundungen" des Punktes durch den Kantenzug des Polygons. Diese Anzahl
	 * gibt an, ob sich der Punkt innerhalb oder ausserhalb des Polygons
	 * befindet. Die hier verwendete Implementation ist eine 2D-Loesung und
	 * erfordert die Projektion der 3d-Repraesentation von Punkt und Polygon in
	 * die xy-Ebene. Um zu entscheiden, welche Komponente bei der Projektion
	 * ignoriert wird, berechnet man den Ebenennormalenvektor und waehlt die
	 * groesste Komponente. Anschliessend kopiert man die verbleibenden Werte in
	 * die x- und y-Komponente neuer Vektoren. Das Verfahren selber basiert auf
	 * der Berechnung einer horizontalen Linie vom Testpunkt ausgehend. Immer,
	 * wenn der Polygonzug diese Linie schneidet, muss unterschieden werden, ob
	 * die Kante von oben nach unten verlaeuft (isUpward()) und ob sich der
	 * Testpunkt rechts oder links von der Polygonkante befindet. Darauf
	 * basierend veraendert man die Winding-Number.
	 * 
	 * @param polygon
	 *            Polygon fuer das getestet werden soll, ob es den uebergebenen
	 *            Punkt enthaelt
	 * @param testPoint
	 *            Testpunkt
	 * @return True, falls sich der Punkt innerhalb des Polygons befindet, False
	 *         sonst
	 * 
	 */
	public boolean isPointInPolygon(final MyPolygon polygon,
			final Vertex3d testPoint) {

		// bestimme zunaechst die Projektionsebene => waehle dafuer die groesste
		// Komponente im Normalenvektor der Ebene
		MyVector3f polygonNormal = polygon.getNormal();
		Axis axisToIgnore = getIgnorableAxis(polygonNormal, false);

		// erzeuge nun fuer jeden Punkt des Polygons eine Repraesentation in der
		// xy-Ebene, bei der die Komponente im
		// Quellpunkt ignoriert wird, die vorab ermittelt wurde
		List<Vertex3d> xyRepresentations = new ArrayList<Vertex3d>(polygon
				.getVertices().size());

		List<Vertex3d> polygonPoints = polygon.getVertices();
		Vertex3d currentVertex = null;

		for (int i = 0; i < polygonPoints.size(); i++) {
			currentVertex = polygonPoints.get(i);
			xyRepresentations.add(createXYPlaneProjectionForPoint(
					currentVertex, axisToIgnore));
		}

		// auch den Testpunkt in die Zielebene projizieren
		Vertex3d newTestPoint = createXYPlaneProjectionForPoint(testPoint,
				axisToIgnore);

		// erzeuge einen horizontalen Strahl ausgehend vom Testpunkt
		Ray horizontalRay = new Ray(newTestPoint.getPosition(), new MyVector3f(
				1.0f, 0.0f, 0.0f));
		List<Ray> rayRepresentations = new ArrayList<Ray>(
				xyRepresentations.size());

		Vertex3d currentNeighbour = null;

		// erzeuge Strahlenrepraesentationen fuer alle projizierten Vertices
		for (int i = 0; i < xyRepresentations.size(); i++) {
			currentVertex = xyRepresentations.get(i);
			if (i + 1 < xyRepresentations.size())
				currentNeighbour = xyRepresentations.get(i + 1);
			else
				currentNeighbour = xyRepresentations.get(0);

			Ray newRay = new Ray(currentVertex, currentNeighbour);
			rayRepresentations.add(newRay);
		}

		Float windingNumber = 0.0f;
		Iterator<Ray> rayIter = rayRepresentations.iterator();
		Ray currentRay = null;
		MyVector3f intersection = null;
		double parameter = 0;
		while (rayIter.hasNext()) {
			currentRay = rayIter.next();

			// berechne Schnittpunkt zwischen aktuellem Strahl und der
			// horizontalen Kante
			try {

				intersection = calculateRay2RayIntersectionApproximation(
						currentRay, horizontalRay);
				/*
				 * intersection =
				 * calculateRay2RayIntersectionMatrixStyle(currentRay,
				 * horizontalRay);
				 */
				// System.out.println("IntersectionApprox: " +
				// intersectionApprox + " IntersectionMatrix: " + intersection +
				// " Ray1: " + currentRay + " Ray2: " + horizontalRay);
			} catch (AssertionError e) {
				intersection = null;
			}

			if (intersection != null) {
				if (isPointOnLineSegment(intersection, currentRay)) {

					// wenn es sich um eine Kante handelt, die von unten nach
					// oben verlaeuft
					if (isUpwardEdge(currentRay)) {

						// teste, ob der Punkt sich links von dieser befindet
						float testValue = isLeft(currentRay, newTestPoint);

						if (testValue > 0.0f) {

							// Teste den Fall, bei dem der Schnittpunkt Start-
							// oder Endpunkt einer Polygonkante ist
							parameter = calculateParameterOnRayForPoint(
									intersection, currentRay);

							// wenn der Parameter 0.0 oder 1.0 ist, liegt der
							// Startpunkt auf der horizontalen Kante, erhoehe um
							// 0.5f
							if (isWithinTolerance((float) parameter, 0.0f,
									0.005f)
									|| isWithinTolerance((float) parameter,
											1.0f, 0.005f))
								windingNumber += 0.5f;
							else
								windingNumber++;
						}
					} else {
						// sonst handelt es sich um eine Kante, die von oben
						// nach unten verlaeuft
						float testValue = isLeft(currentRay, newTestPoint);
						// teste, ob sich der Punkt rechts von der Linie
						// befindet
						if (testValue < 0.0f) {
							// Teste den Fall, bei dem der Schnittpunkt Start-
							// oder Endpunkt einer Polygonkante ist
							parameter = calculateParameterOnRayForPoint(
									intersection, currentRay);

							// wenn der Parameter 0.0 oder 1.0 ist, liegt der
							// Startpunkt auf der horizontalen Kante, erhoehe um
							// 0.5f
							if (isWithinTolerance((float) parameter, 0.0f,
									0.005f)
									|| isWithinTolerance((float) parameter,
											1.0f, 0.005f))
								windingNumber -= 0.5f;
							else
								windingNumber--;
						}
					}
				} else
					continue;
			} else
				continue;
		}
		return !(windingNumber == 0);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Diese Methode dient der Bestimmung von Reflex-Vertices => hierbei handelt
	 * es sich um Vertices in konkaven Polygonen, deren adjazente Kanten einen
	 * stumpfen Winkel bilden Die Bestimmung, ob ein Vertex ein Relfex-Vertex
	 * ist, besteht in der Berechnung des Winkels zwischen seinen adjazenten
	 * Kanten
	 * 
	 * @param ray1
	 * @param ray2
	 * @return
	 */
	public boolean isReflexVertex(final Ray ray1, final Ray ray2) {

		MyVector3f kante1 = ray1.getDirection();
		MyVector3f kante2 = ray2.getDirection();

		MyVector3f cross = new MyVector3f();
		cross.cross(kante1, kante2);

		kante1.normalize();
		kante2.normalize();

		// verwende das Punktprodukt mit den normalisierten Vektoren
		double winkel = getFullAngleRad(kante1, kante2);

		// es handelt sich um ein Reflex-Vertex wenn der Winkel > 180°, aber
		// kleiner 360° ist
		double doublePi = Math.PI * 2.0d;
		if (winkel > Math.PI && winkel < doublePi)
			return true;

		return false;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet, ob zwei Vektoren parallel sind. Dies ist genau dann
	 * der Fall wenn das Punktprodukt gleich dem Produkt der Betraege ist. Da es
	 * zu Floating-Point-Rundungsproblemen kommen kann, wird die Gleichheit
	 * innerhalb eines Toleranzbereichs interpretiert
	 * 
	 * @param vector1
	 *            Eingabevektor 1
	 * @param vector2
	 *            Eingabevektor 2
	 * @return True, falls die Vektoren innerhalb eines Toleranzbereichs
	 *         parallel sind, False sonst
	 */
	public boolean isParallel(final MyVector3f vector1, MyVector3f vector2) {

		float SMALL_NUM = 0.00000001f;

		MyVector3f u = vector1.clone();
		u.normalize();

		MyVector3f v = vector2.clone();
		v.normalize();

		float a = u.dot(u);
		float b = u.dot(v);
		float c = v.dot(v);
		float D = a * c - b * b;

		// compute the line parameters of the two closest points
		if (D < SMALL_NUM) { // the lines are almost parallel
			// wenn die Strahlen parallel sind, gebe keine Naeherungsloesung
			// zurueck
			return true;
		}
		return false;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode prueft, ob die beiden uebergebenen Vektoren antiparallel sind.
	 * Verwendet die s. Eigenschaften des Skalarprodukts:
	 * http://de.wikipedia.org/wiki/Skalarprodukt
	 * 
	 * @param vec1
	 *            Erster Eingabevektor
	 * @param vec2
	 *            Zweiter Eingabevektor
	 * @return True, falls die beiden Vektoren antiparallel sind, False sonst
	 */
	public boolean isAntiparallel(final MyVector3f vec1, final MyVector3f vec2) {

		float dotResult = vec1.dot(vec2);
		float vec1Length = vec1.length();
		float vec2Length = vec2.length();

		// zwei Vektoren sind antiparallel, wenn gilt: (vecA * vecB) = -length_A
		// * length_B
		// somit sind zwei Vektoren antiparallel, wenn die Summe der Ausdruecke
		// = 0 ist
		float addition = Math.abs(dotResult + (vec1Length * vec2Length));
		return addition < 0.05f;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet, ob zwei Vektoren parallel sind. Dies ist genau dann
	 * der Fall wenn das Punktprodukt gleich dem Produkt der Betraege ist.Im
	 * Gegensatz zur "normalen" isParallel-Methode wird hier keine Toleranz
	 * beruecksichtigt, Vektoren sind nur dann parallel, wenn exakt 0.0f als
	 * Ergebnis der Subtraktion herauskommt
	 * 
	 * @param vector1
	 *            Eingabevektor 1
	 * @param vector2
	 *            Eingabevektor 2
	 * @return True, falls die Vektoren parallel sind, False sonst
	 */
	public boolean isParallelNoTolerance(final MyVector3f vector1,
			MyVector3f vector2) {

		float punktprodukt = Math.abs(vector1.dot(vector2));
		float length1 = vector1.length();
		float length2 = vector2.length();

		float lengthProdukt = length1 * length2;

		// nur, wenn exakt 0.0f herauskommt, werden die Kanten als parallel
		// betrachtet
		return (Math.abs(punktprodukt - lengthProdukt) == 0.0f);

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode bestimmt, ob zwei Vektoren senkrecht zueinander sind. Dies ist
	 * der Fall, wenn ihr Punktprodukt = 0.0f ist
	 * 
	 * @param vector1
	 *            Eingabevektor 1
	 * @param vector2
	 *            Eingabevektor 2
	 * 
	 * @return True, wenn die Vektoren senkrecht zueinander stehen, False sonst
	 */
	public boolean isOrthogonal(final MyVector3f vector1,
			final MyVector3f vector2) {

		float dotProduct = Math.abs(vector1.dot(vector2));

		if (isWithinTolerance(dotProduct, 0.0f))
			return true;
		return false;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * bestimmt, ob der uebergebene Wert nicht mehr als tolerance vom Zielwert
	 * abweicht
	 * 
	 * @param value
	 *            Testwert
	 * @param targetValue
	 *            Zielwert, dem der Testwert moeglichst aehnlich sein soll
	 * @return True, falls die Abweichung zwischen Test- und Zielwert kleines
	 *         als die globale Klassentoleranz ist, False sonst
	 */
	public boolean isWithinTolerance(float value, float targetValue) {

		float delta = Math.abs(value - targetValue);
		return !(delta > TOLERANCE);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * bestimmt, ob der uebergebene Wert nicht mehr als tolerance vom Zielwert
	 * abweicht
	 * 
	 * @param value
	 *            Testwert
	 * @param targetValue
	 *            Zielwert, dem der Testwert moeglichst aehnlich sein soll
	 * @return True, falls die Abweichung zwischen Test- und Zielwert kleines
	 *         als die globale Klassentoleranz ist, False sonst
	 */
	public boolean isWithinTolerance(double value, double targetValue) {

		double delta = Math.abs(value - targetValue);
		if (delta > TOLERANCE)
			return false;
		else
			return true;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Bestimmt, ob der uebergebene Wert nicht mehr als die uebergebene
	 * tolerance vom Zielwert abweicht
	 * 
	 * @param value
	 *            Testwert
	 * @param targetValue
	 *            Zielwert, dem der Testwert moeglichst aehnlich sein soll
	 * @param tolerance
	 *            Tolerierte Abweichung des Testwerts vom Zielwert
	 * @return True, falls die Abweichung zwischen Test- und Zielwert kleines
	 *         als tolerance ist, False sonst
	 */
	public boolean isWithinTolerance(float value, float targetValue,
			float tolerance) {

		float delta = Math.abs(value - targetValue);
		if (delta > tolerance)
			return false;
		else
			return true;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Bestimmt, ob der uebergebene Wert nicht mehr als die uebergebene
	 * tolerance vom Zielwert abweicht
	 * 
	 * @param value
	 *            Testwert
	 * @param targetValue
	 *            Zielwert, dem der Testwert moeglichst aehnlich sein soll
	 * @param tolerance
	 *            Tolerierte Abweichung des Testwerts vom Zielwert
	 * @return True, falls die Abweichung zwischen Test- und Zielwert kleines
	 *         als tolerance ist, False sonst
	 */
	public boolean isWithinTolerance(double value, double targetValue,
			double tolerance) {

		double delta = Math.abs(value - targetValue);
		if (delta > tolerance)
			return false;
		else
			return true;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode bestimmt, ob die uebergebenen Vektoren "gleich" sind, also einen
	 * Abstand besitzen, der unterhalb eines Toleranzwertes liegt.
	 * 
	 * @param vec1
	 *            Eingabevektor 1
	 * @param vec2
	 *            Eingabevektor 2
	 * @return True, wenn der Abstand der beiden Vektoren unterhalb des
	 *         Toleranzwertes liegt, False sonst
	 */
	public boolean isWithinTolerance(MyVector3f vec1, MyVector3f vec2) {
		float tolerance = 0.3f;
		MyVector3f sub = new MyVector3f();
		sub.sub(vec1, vec2);
		float distance = sub.length();

		// System.out.println("isWithinToleranceVector: " + distance);

		return isWithinTolerance(distance, 0.0f, tolerance);

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Diese Funktion prueft, ob sich Punkt1 und Punkt2 auf der gleichen Seite
	 * in Bezug auf den uebergebenen Strahl befinden fuer Punkt 2 muss bekannt
	 * sein, wo sich dieser befindet => wenn nun das Kreuzprodukt des
	 * Richtungsvektors des Strahls mit dem vektor vom Startpunkt des Strahls
	 * mit Punkt1 die gleiche gleiche Richtung besitzt wie mit Punkt2, so liegen
	 * die Punkte auf der gleichen Seite Algorithmus:
	 * http://www.blackpawn.com/texts/pointinpoly/default.html
	 * 
	 * @param ray
	 * @param point1
	 * @param point2
	 * @return True, wenn sich die beiden Punkte auf der selben Seite des
	 *         Strahls befinden, false sonst
	 */
	public boolean isSameSideOfRay(final Ray ray, final MyVector3f point1,
			final MyVector3f point2) {

		MyVector3f cross1 = new MyVector3f();
		MyVector3f cross2 = new MyVector3f();

		MyVector3f rayStart = ray.getStartPtr();
		MyVector3f rayDirection = ray.getDirectionPtr();

		MyVector3f point1Suba = new MyVector3f();
		point1Suba.sub(point1, rayStart);

		MyVector3f point2Suba = new MyVector3f();
		point2Suba.sub(point2, rayStart);

		cross1.cross(rayDirection, point1Suba);
		cross2.cross(rayDirection, point2Suba);
		float dotCross = cross1.dot(cross2);

		if (dotCross >= 0)
			return true;

		else
			return false;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erzeugt eine Projektion des uebergebenen Punktes auf eine
	 * Zielebene , indem die Komponente des Eingabepuntkes ignoriert wird, die
	 * durch den uebergebenen Index angegeben ist (0 := x, 1 := y, 2 := z) Die
	 * verbleibenden Komponenten werden in die x- und y-Koordinaten eines neuen
	 * Punktes kopiert, dessen z-Koordinate immer 0 betraegt.
	 * 
	 * @param point
	 *            Punkt, dessen Projektion in eine Zielebene bestimmt werden
	 *            soll
	 * @param componentToIgnore
	 *            Komponente des Eingabevektors, die bei der Projektion
	 *            wegfallen soll
	 * @return Projektion des Eingabevektors
	 */
	public Vertex3d createXYPlaneProjectionForPoint(Vertex3d point,
			Axis componentToIgnore) {
		Vertex3d newVertex = new Vertex3d();
		newVertex.setZ(0.0f);

		// x-Komponente ignorieren
		if (componentToIgnore.equals(Axis.X)) {
			newVertex.setX(point.getZ());
			newVertex.setY(point.getY());
		}
		// y-Komponente ignorieren
		else if (componentToIgnore.equals(Axis.Y)) {
			newVertex.setX(point.getX());
			newVertex.setY(point.getZ());
		}
		// z-Komponente ignorieren
		else if (componentToIgnore.equals(Axis.Z)) {
			newVertex.setX(point.getX());
			newVertex.setY(point.getY());
		}
		return newVertex;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode fuehrt eine Fluchtpunktprojektion vom uebergebenen Fluchtpunkt
	 * auf die uebergebene Ebene durch. Dabei wird der Durchstosspunkt des
	 * Strahls vom Fluchtpunkt zu jedem Punkt in der Ebene bestimmt. Diese Liste
	 * mit Durchstosspunkten wird an den Aufrufer zurueckgereicht.
	 * 
	 * @param targetPlane
	 *            Projektionsebene
	 * @param vanishingPoint
	 *            Fluchtpunkt
	 * @param points
	 *            Liste mit Punkten, die vom Fluchtpunkt auf die Zielebene
	 *            projiziert werden sollen
	 * @return Projektion der uebergebenen Punkte in der Zielebene
	 */
	public List<Vertex3d> projectPointsOntoPlaneFromVanishingPoint(
			Plane targetPlane, Vertex3d vanishingPoint, List<Vertex3d> points) {

		List<Vertex3d> result = new ArrayList<Vertex3d>(points.size());
		MyVector3f pointOnPlane = null;
		Ray projectionRay = null;

		for (int i = 0; i < points.size(); i++) {
			projectionRay = new Ray(vanishingPoint, points.get(i));
			LOGGER.info("WINKEL: "
					+ calculateAngle(projectionRay.getDirectionPtr(),
							targetPlane.getNormalPtr()));

			// bei der Projektion kann der Fall auftreten, dass der Strahl
			// parallel zur Zielebene verlaueft, in diesem Fall muss eine andere
			// Zielebene verwendet werden
			try {
				pointOnPlane = calculateRayPlaneIntersection(projectionRay,
						targetPlane);
				assert pointOnPlane != null : "FEHLER: Es konnte kein Durchstosspunkt durch die Ebene berechnet werden!";
			} catch (AssertionError e) {
				return null;
			}
			result.add(new Vertex3d(pointOnPlane));
		}

		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet im 2D-Raum, ob sich der uebergebene Punkt links oder
	 * rechts von oder auf dem uebergebenen Strahl befindet. Die Berchnung
	 * basiert darauf, die Beziehungen der Punkte untereinander bzgl. ihrer x-
	 * und y-Koordinaten zu analysieren. Zentral fuer die Berechnung sind nur
	 * die Vorzeichen, die absoluten Betraege spielen nur eine untergeordnete
	 * Rolle. Das Verfahren laesst sich am einfachsten anhand konkreter
	 * Beispiele nachvollziehen.
	 * 
	 * @param ray
	 *            , Strahl, fuer den die Orientierung des uebergebenen Punktes
	 *            geprueft wird
	 * @param point
	 *            , Punkt, dessen Orientierung berechnet wird
	 * @return > 0 := point ist links vom uebergebenen Strahl, < 0 := point ist
	 *         rechts vom uebergebenen Strahl, = 0 := point liegt auf dem
	 *         uebergebenen Strahl
	 */
	private float isLeft(final Ray ray, final Vertex3d point) {

		MyVector3f p0 = ray.getStart();
		MyVector3f p1 = ray.getEnd();
		MyVector3f p2 = point.getPosition();

		return isLeft(p0, p1, p2);

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode berechnet im 2D-Raum, ob sich der uebergebene Punkt links oder
	 * rechts von oder auf dem uebergebenen Strahl befindet. Die Berchnung
	 * basiert darauf, die Beziehungen der Punkte untereinander bzgl. ihrer x-
	 * und y-Koordinaten zu analysieren. Zentral fuer die Berechnung sind nur
	 * die Vorzeichen, die absoluten Betraege spielen nur eine untergeordnete
	 * Rolle. Das Verfahren laesst sich am einfachsten anhand konkreter
	 * Beispiele nachvollziehen.
	 * 
	 * @param ray
	 *            , Strahl, fuer den die Orientierung des uebergebenen Punktes
	 *            geprueft wird
	 * @param point
	 *            , Punkt, dessen Orientierung berechnet wird
	 * @return > 0 := point ist links vom uebergebenen Strahl, < 0 := point ist
	 *         rechts vom uebergebenen Strahl, = 0 := point liegt auf dem
	 *         uebergebenen Strahl
	 */
	public Boolean isLeft(final Ray ray, final MyVector3f point) {

		MyVector3f p0 = ray.getStart();
		MyVector3f p1 = ray.getEnd();
		float result = isLeft(p0, p1, point);
		return result > 0;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet im 2D-Raum, ob sich der uebergebene Punkt links oder
	 * rechts von oder auf dem uebergebenen Strahl befindet. Die Berchnung
	 * basiert darauf, die Beziehungen der Punkte untereinander bzgl. ihrer x-
	 * und y-Koordinaten zu analysieren. Zentral fuer die Berechnung sind nur
	 * die Vorzeichen, die absoluten Betraege spielen nur eine untergeordnete
	 * Rolle. Das Verfahren laesst sich am einfachsten anhand konkreter
	 * Beispiele nachvollziehen. Spart trigonometrische Berechnungen.
	 * 
	 * @param p0
	 *            Startpunkt der Teststrahlen
	 * @param p1
	 *            Erster Testpunkt
	 * @param p2
	 *            Zweiter Testpunkt
	 * 
	 * @return > 0 := point ist links vom uebergebenen Strahl, < 0 := point ist
	 *         rechts vom uebergebenen Strahl, = 0 := point liegt auf dem
	 *         uebergebenen Strahl
	 */

	public Float isLeft(MyVector3f p0, MyVector3f p1, MyVector3f p2) {
		return ((p1.x - p0.x) * (p2.y - p0.y) - (p2.x - p0.x) * (p1.y - p0.y));
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Wrappermethode fuer isLeft-Test
	 * 
	 * @param rayStart
	 *            Startpunkt des Teststrahls
	 * @param rayEnd
	 *            Endpunkt des Teststrahls
	 * @param testpoint
	 *            Testpunkt
	 * @return True, wenn der Testpunkt links vom Teststrahl liegt, False sonst
	 */
	public Boolean isLeft(final Vertex3d rayStart, final Vertex3d rayEnd,
			final Vertex3d testpoint) {
		float result = isLeft(rayStart.getPositionPtr(),
				rayEnd.getPositionPtr(), testpoint.getPositionPtr());
		return result > 0;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Einfache Methode fuer xy-Ebenen: testet, ob der Startpunkt eine hoehere
	 * y-Koordinate besitzt, als der Endpunkt => in diesem Fall verlaeuft die
	 * Kante in positiver y-Richtung
	 * 
	 * @param ray
	 *            Strahl, fuer den getestet wird, ob er in der xy-Ebene von oben
	 *            nach unten oder in umgekehrter Richtung verlaeuft
	 * @return True, falls der Strahl von unten nach oben verlaeuft, False sonst
	 */
	private boolean isUpwardEdge(Ray ray) {
		MyVector3f start = ray.getStartPtr();
		MyVector3f end = ray.getEndPtr();

		if (start.y > end.y)
			return true;
		else
			return false;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Implementiert einen toleranten Positionsvergleich bei Vertices, um
	 * Floating-Point-Ungenauigkeiten auszugleichen. Sofern sich die beiden
	 * Vektoren in jeder Komponente um weniger als die festgelegte Toleranz
	 * unterscheiden, werden sie als gleich betrachtet.
	 * 
	 * @param vector1
	 *            Eingabevektor 1
	 * @param vector2
	 *            Eingabevektor 2
	 * @param radius
	 *            Radius der Testkugel
	 * @return True, falls die Abweichungen in den Komponenten unter der
	 *         Schwelle liegen, False sonst
	 */
	public boolean comparePositionsWithTolerance(MyVector3f vector1,
			MyVector3f vector2, Float radius) {

		// simuliere einen Punkt-in-Kugel-Test => wenn der Abstand zwischen den
		// Punkten unter
		// einem Grenzwert liegt, befindet sich der Punkt in der Kugel
		MyVector3f differenceVector = new MyVector3f();
		differenceVector.sub(vector1, vector2);

		float distance = differenceVector.length();
		distance = round(distance, 10000.0f);

		if (distance > radius)
			return false;
		else
			return true;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode projiziert den uebergebenen Punkt auf die uebergebene Ebene,
	 * indem ein Strahl vom Punkt in Richtung der Ebenennormale berechnet wird.
	 * Anschliessend berechnet man den Schnittpunkt zwischen Strahl und Ebene
	 * und weist dem Punkt die Schnittpunktkoordinaten zu, sofern ein solcher
	 * gefunden wird.
	 * 
	 * @param plane
	 *            Ebene, in die projiziert wird
	 * @param point
	 *            Punkt, der projiziert wird
	 */
	public void projectPointOntoPlane(Plane plane, Vertex3d point) {

		assert plane != null && point != null : "FEHLER: Punkt oder Ebene sind null: Punkt: "
				+ point + " Ebene: " + plane;

		// berechne einen Strahl durch den Punkt in Richtung der Ebenennormale
		Ray ray = new Ray(point.getPosition(), plane.getNormal());
		MyVector3f intersection = null;

		intersection = calculateRayPlaneIntersection(ray, plane);

		assert isPointOnPlane(intersection, plane);

		if (intersection == null)
			LOGGER.info("Kein Schnittpunkt gefunden!");
		else {
			// verwende eine Rundung des Vektors, um leichte Abweichungen
			// abzufangen, die bei der Texturierung zu Loechern fuehren koennen
			intersection = roundVector3f(intersection);
			point.setPosition(intersection);
		}

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode liefert einen Zufallswert zwischen den uebergebenen Grenzwerten
	 * 
	 * @param lowerBorder
	 *            Untere Grenze fuer berechneten Wert
	 * @param upperBorder
	 *            Obere Grenze fuer berechneten Wert
	 * @return Zufallszahl innerhalb des Bereichs
	 */
	public float getRandomValueWithinRange(float lowerBorder, float upperBorder) {

		assert upperBorder >= lowerBorder : "FEHLER: Obergrenze ist kleiner als Untergrenze: Obergrenze: "
				+ upperBorder + " Untergrenze: " + lowerBorder;

		float range = upperBorder - lowerBorder;
		float rand = mRandom.nextFloat();

		// berechnete Zufallszahl auf Wertebereich abbilden
		rand *= range;

		// Untergrenze aufaddieren
		rand += lowerBorder;
		return rand;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet eine Zufallszahl, ist diese Zufallszahl kleiner als die
	 * uebergebene Wahrscheinlichkeit, so gibt die Methode true zurueck, sonst
	 * false
	 * 
	 * @param probability
	 *            Basiswahrscheinlichkeit
	 * @return True, falls die berechnete Zufallszahl unterhalb der
	 *         Basiswahrscheinlichkeit liegt, False sonst
	 */
	public boolean decide(float probability) {

		assert probability >= 0.0f && probability <= 1.0f : "FEHLER: Die uebergebene Basiswahrscheinlichkeit '"
				+ probability + "' liegt ausserhalb des zulaessigen Bereichs!";
		float rand = mRandom.nextFloat();
		if (rand <= probability) {
			return true;
		} else {
			return false;
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode testet, ob sich die uebergebenen Polygone ueberschneiden. Zu
	 * diesem Zweck berechnet das Verfahren Schnittpunkte zwischen allen Paaren
	 * von Strahlen in beiden Polygonen. Sobald ein Schnittpunkt gefunden wird,
	 * der auf beiden Strahlen liegt, ueberschneiden sich die Polygone, sonst
	 * nicht.
	 * 
	 * @param poly0
	 *            Erstes Eingabepolygon
	 * @param poly1
	 *            Zweites Eingabepolygon
	 * @return True, falls ein Schnittpunkt zwischen einem Strahlenpaar gefunden
	 *         wird, der auf beiden Strahlsegmenten liegt
	 */

	public Boolean intersects(MyPolygon poly0, MyPolygon poly1) {

		Ray currentRay0 = null, currentRay1 = null;
		List<Ray> poly0Rays = poly0.getRays();
		List<Ray> poly1Rays = poly1.getRays();

		MyVector3f intersection = null;
		double param0, param1;

		for (int i = 0; i < poly0Rays.size(); i++) {
			currentRay0 = poly0Rays.get(i);
			for (int j = 0; j < poly1Rays.size(); j++) {
				currentRay1 = poly1Rays.get(j);
				intersection = calculateRay2RayIntersectionApproximation(
						currentRay0, currentRay1);

				// wenn ein Schnitt vorliegt, teste, ob der Schnittpunkt auf
				// beiden Liniensegmenten liegt
				if (intersection != null) {

					// wenn der berechnete Schnittpunkt auf beiden Segmenten
					// liegt, gebe true zurueck, sonst rechne weiter
					param0 = calculateParameterOnRayForPoint(intersection,
							currentRay0);
					if (param0 >= 0 && param0 <= 1) {
						param1 = calculateParameterOnRayForPoint(intersection,
								currentRay1);

						if (param1 >= 0 && param1 <= 1)
							return true;
					}
				}
			}
		}
		// keine Ueberschneidung gefunden
		return false;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet den Drehsinn des uebergebenen Polygons durch folgendes
	 * Vorgehen: http://www.matheboard.de/archive/419771/thread.html (letzter
	 * Beitrag)
	 * http://softsurfer.com/Archive/algorithm_0101/algorithm_0101.htm#3D
	 * Polygons
	 * 
	 * @param vertices
	 *            Vertexliste, die das Polygon beschreibt
	 * @return Positiver oder negativer Drehsinn des Polygons
	 */
	public Drehsinn computeDrehsinnForPolygon(final List<Vertex3d> vertices) {

		assert vertices.size() >= 3 : "FEHLER: Es wurden nur "
				+ vertices.size() + " Vertices uebergeben!";
		float polygonArea = computeSignedPolygonArea3d(vertices);

		// Drehsinn < 0: bei Draufsicht auf das Polygon sind die Vertices im
		// Uhrzeigersinn definiert
		if (polygonArea <= 0)
			return Drehsinn.NEGATIV;

		// Drehsinn > 0: bei Draufsicht sind die Vertices entgegen dem
		// Uhrzeigersinn definiert
		else
			return Drehsinn.POSITIV;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet den Flaecheninhalt eines 3d-Polygons nach folgender
	 * Methode:
	 * http://softsurfer.com/Archive/algorithm_0101/algorithm_0101.htm#3D
	 * Polygons
	 * 
	 * @param vertices
	 *            Vertices, die die Eckpunkte des Polygons beschreiben
	 * @return Vorzeichenbehafteter Flaecheninhalt
	 * @deprecated FEHLERHAFTE ERGEBNISSE!!!!
	 */
	private float computeSignedPolygonArea3dOLD(List<Vertex3d> vertices) {

		Vertex3d currentVertex = null, nextVertex = null;
		MyVector3f currentCross = null;
		MyVector3f result = new MyVector3f();

		// berechne die Normale der Ebene, die das Eingabepolygon enthaelt
		final MyVector3f normal = calculateNormalNewell(vertices);

		for (int i = 0; i < vertices.size() - 1; i++) {

			currentVertex = vertices.get(i);
			nextVertex = vertices.get(i + 1);

			currentCross = new MyVector3f();
			currentCross.cross(currentVertex.getPositionPtr(),
					nextVertex.getPositionPtr());

			if (result == null) {
				result = currentCross;
			} else {
				result.add(currentCross);
			}
		}

		// Flaecheninhalt bestimmen mit dem Referenzvektor der
		// XY-Projektionsebene
		return 0.5f * result.dot(normal);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet die Flaeche eines planaren Polygons im 3d-Raum.
	 * Algorithmus: http://geomalgorithms.com/a01-_area.html#3D Polygons
	 * 
	 * @param polyVerts
	 *            Eckpunkte des Polygons, dessen Flaeche berechnet werden soll
	 * @return Flaecheninhalt des Polygons
	 */

	public float computeSignedPolygonArea3d(final List<Vertex3d> polyVerts) {

		Collections.reverse(polyVerts);

		float area = 0;

		// abs value of normal and its coords
		float an, ax, ay, az;

		final int countPolyVerts = polyVerts.size();
		final MyVector3f normal = calculateNormalNewell(polyVerts);

		// kopiere die Vertices in eine neue Liste fuer die gilt, dass Vert[0] =
		// Vert[n] fuer n-1 Vertices
		final List<Vertex3d> vertices = new ArrayList<Vertex3d>(
				polyVerts.size() + 1);
		vertices.addAll(polyVerts);
		vertices.add(polyVerts.get(0));

		// Bestimme die Achse, die fuer die Projektion ignoriert werden soll
		final Axis ingorableAxis = getIgnorableAxis(normal, false);

		// select largest abs coordinate to ignore for projection
		ax = Math.abs(normal.x);
		ay = Math.abs(normal.y);
		az = Math.abs(normal.z);

		int i, j, k;

		// Flaecheninhalt der 2D-Projektion berechnen, abhaengig von der
		// ignorierten Achse
		for (i = 1, j = 2, k = 0; i < countPolyVerts; i++, j++, k++) {
			switch (ingorableAxis) {
			case X:
				area += (vertices.get(i).getY() * (vertices.get(j).getZ() - vertices
						.get(k).getZ()));
				continue;
			case Y:
				area += (vertices.get(i).getX() * (vertices.get(j).getZ() - vertices
						.get(k).getZ()));
				continue;
			case Z:
				area += (vertices.get(i).getX() * (vertices.get(j).getY() - vertices
						.get(k).getY()));
				continue;
			default:
				LOGGER.error("FEHLER: Falsche Achse: " + ingorableAxis);
				break;
			}
		}

		// Uerbtrag zwischen Anfangs- und Endvertex
		switch (ingorableAxis) {
		case X:
			area += (vertices.get(countPolyVerts).getY() * (vertices.get(1)
					.getZ() - vertices.get(countPolyVerts - 1).getZ()));
			break;
		case Y:
			area += (vertices.get(countPolyVerts).getX() * (vertices.get(1)
					.getZ() - vertices.get(countPolyVerts - 1).getZ()));
			break;
		case Z:
			area += (vertices.get(countPolyVerts).getX() * (vertices.get(1)
					.getY() - vertices.get(countPolyVerts - 1).getY()));
			break;
		default:
			LOGGER.error("FEHLER: Falsche Achse: " + ingorableAxis);
			break;
		}

		// scale to get area before projection
		an = normal.length();
		switch (ingorableAxis) {
		case X:
			area *= (an / (2 * ax));
			break;
		case Y:
			area *= (an / (2 * ay));
			break;
		case Z:
			area *= (an / (2 * az));
			break;
		default:
			LOGGER.error("FEHLER: Falsche Achse: " + ingorableAxis);
			break;
		}
		return area;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Wrapper zur Flaechenberechnung bei Polygonen
	 * 
	 * @param vertices
	 *            Liste mit Eckpunkten des Polygons
	 * @return Flaecheninhalt des Polygons
	 */
	public float computePolygonArea(List<Vertex3d> vertices) {

		// Dreieck
		if (vertices.size() == 3)
			return calculateTriangleArea(vertices);

		// beliebiges Polygon
		else {
			return Math.abs(computeSignedPolygonArea3d(vertices));
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode testet, ob der uebergebene Punkt auf einer der Kanten des
	 * uebergebenen Polygons liegt
	 * 
	 * @param poly
	 *            Testpolygon
	 * @param testPoint
	 *            Testpunkt
	 * @return True, falls der Punkt auf einer Kante liegt, False sonst
	 */
	public boolean isPointOnPolyEdge(MyPolygon poly, MyVector3f testPoint) {

		// teste zunaechst, ob der uebergebene Punkt auf einem der
		// Eingabestrahlen liegt => wenn dies der Fall ist, wird der Punkt als
		// im Polygon liegend betrachtet
		List<Ray> polyRays = poly.getRays();
		Ray currentRay = null;
		for (int i = 0; i < polyRays.size(); i++) {
			currentRay = polyRays.get(i);
			if (!isPointOnRay(testPoint, currentRay))
				continue;
			if (isPointOnLineSegment(testPoint, polyRays.get(i))) {
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet den Oberflaecheninhalt des uebergebenen Polyeders als
	 * Summe der Flaecheninhalte der Eingabedreiecke
	 * 
	 * @param triangles
	 *            Liste mit Dreiecken, die die Oberflaeche des Polyeders
	 *            beschreiben
	 * @return Oberflaecheninhalt
	 */
	public Float computeAreaForPolyeder(List<MyPolygon> triangles) {

		Float area = 0.0f;
		for (int i = 0; i < triangles.size(); i++) {
			area += calculateTriangleArea(triangles.get(i).getVertices());
		}
		return area;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet den Schwerpunkt des Polyeder als gewichtetes Mittel der
	 * Polyederdreiecksmittelpunkte. Die Gewichtung erfolgt dabei ueber die
	 * Flaeche der Dreiecke.
	 * 
	 * @param triangles
	 * @return Schwerpunkt des uebergebenen Polyeders
	 */
	public MyVector3f computeCentroidForPolyeder(List<MyPolygon> triangles) {
		MyPolygon currentPoly = null;
		MyVector3f triCenter = null, weightedCenter = null, result = new MyVector3f(
				0.0f, 0.0f, 0.0f);
		float triArea = 0;

		for (int i = 0; i < triangles.size(); i++) {
			currentPoly = triangles.get(i);
			triCenter = currentPoly.getCenter();
			triArea = computePolygonArea(currentPoly.getVertices());
			weightedCenter = new MyVector3f();
			// Mittelpunkt ueber Flaecheninhalt gewichten
			weightedCenter.scale(triArea, triCenter);
			result.add(weightedCenter);
		}

		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet das minimale Boundingrechteck, dass die uebergebenen
	 * Vertices enthaelt. Dabei geht die Methode davon aus, dass die
	 * Eingabevertices alle in einer Ebene liegen Algorithmus: Rotating-Calipers
	 * 
	 * @param vertices
	 *            Liste mit Vertices, fuer die das Rechteck bestimmt werden soll
	 * @return Polygon, das das minimale Boundingrechteck beschreibt
	 */
	public MyPolygon getMinAreaRect(final List<Vertex3d> vertices) {

		// bestimme die beiden Achsen, die das Boundingrecht beschreiben
		List<MyVector3f> rectAxis = getMinAreaRectAxis(vertices);
		assert rectAxis.size() == 2 : "FEHLER: Es wurden " + rectAxis.size()
				+ " aufspannende Achsen berechnet!";

		// 3. Achse steht senkrecht auf den berechneten Achsen und stellt die
		// Normale der Ebene dar, die das minimale Rechteck enthaelt
		MyVector3f currentMinAxis3 = new MyVector3f();
		currentMinAxis3.cross(rectAxis.get(0), rectAxis.get(1));
		currentMinAxis3.normalize();
		rectAxis.add(currentMinAxis3);

		float l1 = Float.MAX_VALUE, u1 = -Float.MAX_VALUE, l2 = Float.MAX_VALUE, u2 = -Float.MAX_VALUE, v3 = 0.0f;
		float currentValue;

		MyVector3f currentPos = null, currentAxis = null;

		// Projektion aller Punkte auf die berechneten Achsen, Bestimmung der
		// MinMax-Werte pro Achse
		for (int i = 0; i < vertices.size(); i++) {
			currentPos = vertices.get(i).getPosition();

			for (int k = 0; k < rectAxis.size(); k++) {
				currentAxis = rectAxis.get(k);
				currentValue = currentPos.dot(currentAxis);

				// Dimensionen bzgl. der ermittelten Achsen berechnen
				if (k == 0) {
					if (currentValue < l1)
						l1 = currentValue;
					else if (currentValue > u1)
						u1 = currentValue;
				} else if (k == 1) {
					if (currentValue < l2)
						l2 = currentValue;
					else if (currentValue > u2)
						u2 = currentValue;
				}

			}
			// auf die 3. Achse projizieren, das sollte immer der gleiche Wert
			// sein, da diese die Normale der Ebene ist, die das minimale
			// Rechteck aufspannt
			currentAxis = rectAxis.get(2);
			v3 = currentPos.dot(currentAxis);
		}

		// einmal den Verschiebungsvektor bzgl. der 3. Achse einberechnen
		MyVector3f constantTranslation = rectAxis.get(2);
		constantTranslation.scale(v3);

		// Berechnung des Boundingrechtecks aus den Achsen
		List<Vertex3d> rectVerts = new ArrayList<Vertex3d>(4);
		MyVector3f axis1 = rectAxis.get(0);
		MyVector3f axis2 = rectAxis.get(1);
		MyVector3f tempAxis = null;

		// 1. Punkt: Min1, Min2
		MyVector3f currentVertPos = axis1.clone();
		currentVertPos.scale(l1);
		tempAxis = axis2.clone();
		tempAxis.scale(l2);
		currentVertPos.add(tempAxis);
		Vertex3d currentVert = new Vertex3d(currentVertPos);
		rectVerts.add(currentVert);

		// 2. Punkt: Min1, Max2
		currentVertPos = axis1.clone();
		currentVertPos.scale(l1);
		tempAxis = axis2.clone();
		tempAxis.scale(u2);
		currentVertPos.add(tempAxis);
		currentVert = new Vertex3d(currentVertPos);
		rectVerts.add(currentVert);

		// 3. Punkt: Max1, Max2
		currentVertPos = axis1.clone();
		currentVertPos.scale(u1);
		tempAxis = axis2.clone();
		tempAxis.scale(u2);
		currentVertPos.add(tempAxis);
		currentVert = new Vertex3d(currentVertPos);
		rectVerts.add(currentVert);

		// 4. Punkt: Max1, Min2
		currentVertPos = axis1.clone();
		currentVertPos.scale(u1);
		tempAxis = axis2.clone();
		tempAxis.scale(l2);
		currentVertPos.add(tempAxis);
		currentVert = new Vertex3d(currentVertPos);
		rectVerts.add(currentVert);

		// Polygon um den konstanten Vektor bzgl. der 3. Achse verschieben
		MyPolygon result = new MyPolygon(rectVerts);
		result.translate(constantTranslation);

		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet die Achsen eines minimalen Rechtecks, das saemtliche
	 * uebergebenen Vertices enthaelt. Dabei geht die Berechnung davon aus, dass
	 * die Eingabevertices den Kantenzug eines konvexen Polygons beschreiben.
	 * Ist dies nicht der Fall, muss zunaechst die konvexe Huelle der Vertices
	 * berechnet werden
	 * 
	 * @param vertices
	 *            Liste mit Eingabevertices, die den Kantenzug eines konvexen
	 *            Polygons beschreiben
	 * @return Achsen, die das minimale Boundingrechteck aufspannen
	 */
	public List<MyVector3f> getMinAreaRectAxis(final List<Vertex3d> vertices) {

		final Plane vertexPlane = calculatePlaneByVertices(vertices);

		// durchlaufe alle Kanten des konvexen Polygons
		// fuer jede Kante berechnet man eine senkrechte Kante
		// und projiziert alle Vertices auf diese beiden Achsen, um die
		// Dimension des Rechtecks zu bestimmen
		// man verwendet die Achsen, die den kleinsten Flaecheninhalt ergeben
		MyVector3f currentEdge = null, perpendicularEdge = null;
		float currentArea = 0, minArea = Float.MAX_VALUE;
		MyVector3f currentMinAxis1 = null, currentMinAxis2 = null;
		Vertex3d current = null, next = null;

		for (int i = 0; i < vertices.size(); i++) {

			current = vertices.get(i);
			next = vertices.get((i + 1) % vertices.size());

			// Kante berechnen
			currentEdge = new MyVector3f();
			currentEdge.sub(next.getPositionPtr(), current.getPositionPtr());
			currentEdge.normalize();

			// berechne senkrechten Vektor
			perpendicularEdge = calculateOrthogonalVectorWithSamePlane(
					currentEdge, vertexPlane.getNormalPtr());
			perpendicularEdge.normalize();

			currentArea = computeRectAreaForAxis(currentEdge,
					perpendicularEdge, vertices);
			if (currentArea < minArea) {
				minArea = currentArea;
				currentMinAxis1 = currentEdge;
				currentMinAxis2 = perpendicularEdge;
			}
		}

		List<MyVector3f> result = new ArrayList<MyVector3f>(2);
		result.add(currentMinAxis1);
		result.add(currentMinAxis2);

		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet die Groesse des Rechtecks mit den uebergebenen Achsen
	 * derart, dass es alle uebergebenen Vertices enthaelt
	 * 
	 * @param first
	 *            1. Achse
	 * @param second
	 *            2. Achse
	 * @return Flaecheninhalt des Rechtecks
	 */
	private float computeRectAreaForAxis(MyVector3f first, MyVector3f second,
			List<Vertex3d> vertices) {

		float min_u = Float.MAX_VALUE, max_u = -Float.MAX_VALUE;
		float min_v = Float.MAX_VALUE, max_v = -Float.MAX_VALUE;

		Vertex3d currentVert = null;
		float currentProjectionValue = -1;

		for (int i = 0; i < vertices.size(); i++) {
			currentVert = vertices.get(i);

			// Vertex auf 1.Achse projizieren
			currentProjectionValue = first.dot(currentVert.getPositionPtr());
			if (currentProjectionValue > max_u)
				max_u = currentProjectionValue;
			if (currentProjectionValue < min_u)
				min_u = currentProjectionValue;

			// Vertex auf 2.Achse projizieren
			currentProjectionValue = second.dot(currentVert.getPositionPtr());
			if (currentProjectionValue > max_v)
				max_v = currentProjectionValue;
			if (currentProjectionValue < min_v)
				min_v = currentProjectionValue;
		}

		// Laenge * Breite
		float area = Math.abs((max_u - min_u) * (max_v - min_v));
		// System.out.println("AREA: " + area + " MaxU: " + max_u + " MinU: " +
		// min_u + " MaxV: " + max_v +" MinV: " + min_v);
		return area;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet den Punkt, der zwischen den beiden Eingabepunkten liegt
	 * 
	 * @param point1
	 *            Eingabepunkt1
	 * @param point2
	 *            Eingabepunkt2
	 * @return Punkt, der auf einer Verbindungsstrecke zwischen den beiden
	 *         Punkten in der Mitte liegt
	 */
	public MyVector3f getCenterBetweenPoints(MyVector3f point1,
			MyVector3f point2) {

		MyVector3f result = new MyVector3f();
		result.sub(point2, point1);
		result.scale(0.5f);
		point1.add(result);
		return point1;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet mittels Skalarprodukt, ob der uebergebene Punkt hinter
	 * der uebergebenen Ebene liegt. Hierfuer berechnet man den Winkel zwischen
	 * dem Vektor vom Punkt auf die Ebene und der Ebenennormalen. Ist der Winkel
	 * > 90°, so liegt der Punkt von der Ebene aus betrachtet dahinter. Gleiches
	 * Verfahren wie Sichtbarkeitstests fuer Polygone.
	 * 
	 * @param point
	 *            Punkt, fuer den die Orientierung zur Ebene gesucht wird
	 * @param plane
	 *            Ebene
	 */
	public boolean isPointBehindPlane(MyVector3f point, Plane plane) {

		MyVector3f pointToPlane = new MyVector3f();
		pointToPlane.sub(point, plane.getStuetzvektorPtr());

		double angle = calculateAngleRadians(pointToPlane, plane.getNormalPtr());
		double halfPi = Math.PI / 2.0d;
		if (angle > halfPi)
			return true;
		else
			return false;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode liefert die Ausdehnungen des beschriebenen Polygons als
	 * AABB-Struktur. Die Methode nimmt für jede Koordinatenachse die Differenz
	 * zwischen minimalem und maximalem Wert. Dann wird anhand der Ebene, in der
	 * die Punkte liegen, bestimmt, welcher Wert als Hoehe und welcher als
	 * Laenge interpretiert wird. Die Methode bietet also eine Moeglichkeit,
	 * unabhaengig von der Anordnung im Raum immer eine interpretierbare Laenge
	 * und Breite zu bekommen.
	 * 
	 * @param poly
	 *            Polygon, fuer das die Ausdehnungen in den Koordinatenebenen
	 *            berechnet werden
	 * @return Liste mit Ausdehnungen fuer die jeweiligen Dimensionen, 0 :=
	 *         Laenge, 1 := Hoehe, 2 := Tiefe (bei xy-Ebene waere x := Laenge, y
	 *         := Hoehe, z := Tiefe)
	 */
	public List<Float> getDimensionsByAxis(MyPolygon poly) {

		float min_x = Float.MAX_VALUE, min_y = Float.MAX_VALUE, min_z = Float.MAX_VALUE;
		float max_x = -Float.MAX_VALUE, max_y = -Float.MAX_VALUE, max_z = -Float.MAX_VALUE;

		List<Vertex3d> verts = poly.getVertices();
		MyVector3f currentVertexPos = null;

		// bestimme die Dimensionen fuer die Koordinatenachsen
		for (int i = 0; i < verts.size(); i++) {
			currentVertexPos = verts.get(i).getPositionPtr();
			if (currentVertexPos.x < min_x)
				min_x = currentVertexPos.x;
			else if (currentVertexPos.x > max_x)
				max_x = currentVertexPos.x;
			else if (currentVertexPos.y < min_y)
				min_y = currentVertexPos.y;
			else if (currentVertexPos.y > max_y)
				max_y = currentVertexPos.y;
			else if (currentVertexPos.z < min_z)
				min_z = currentVertexPos.z;
			else if (currentVertexPos.z > max_z)
				max_z = currentVertexPos.z;
		}

		float diff_x = max_x - min_x;
		float diff_y = max_y - min_y;
		float diff_z = max_z - max_z;

		List<Float> result = new ArrayList<Float>(3);

		// bestimme die Zuordnungen von Laenge / Breite / Hoehe anhand der
		// Ebenenausrichtung
		Axis ignorableAxis = getIgnorableAxis(poly.getNormalPtr(), false);
		switch (ignorableAxis) {
		// zy-Ebene
		case X:
			// Laenge
			result.add(diff_z);
			// Hoehe
			result.add(diff_y);
			// Tiefe
			result.add(diff_x);
			break;
		// xz-Ebene
		case Y:
			// Laenge
			result.add(diff_x);
			// Hoehe
			result.add(diff_z);
			// Tiefe
			result.add(diff_y);
			break;
		// xy-Ebene
		case Z:
			// Laenge
			result.add(diff_x);
			// Hoehe
			result.add(diff_y);
			// Tiefe
			result.add(diff_z);
			break;

		}
		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode prueft, ob das eingegebene Polyon ein Rechteck ist, indem die
	 * Winkel zwischen allen Strahlen bestimmt werden. Es handelt sich nur dann
	 * um ein Rechteck, wenn alle Winkel 90° betragen
	 * 
	 * @param poly
	 *            Eingabepolygon
	 * @return True, falls das Eingabepolygon ein Rechteck beschreibt, False
	 *         sonst
	 */
	public boolean isRectangle(MyPolygon poly) {

		// wenn nicht genau 4 Strahlen vorkommen, handelt es sich nicht um ein
		// Rechteck
		List<Ray> polyRays = poly.getRays();
		if (polyRays.size() != 4)
			return false;

		Ray currentRay = null, nextRay = null;
		for (int i = 0; i < polyRays.size(); i++) {
			currentRay = polyRays.get(i);
			nextRay = polyRays.get((i + 1) % polyRays.size());

			// sobald der Winkel zwischen einem Strahlenpaar kein Vielfaches von
			// 90 ist, breche ab
			if (!isAngleMultipleOf90(currentRay.getDirection(),
					nextRay.getDirection()))
				return false;

		}

		return true;
	}
	// ------------------------------------------------------------------------------------------

}
