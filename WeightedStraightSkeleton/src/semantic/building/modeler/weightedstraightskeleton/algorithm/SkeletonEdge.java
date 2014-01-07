package semantic.building.modeler.weightedstraightskeleton.algorithm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.Plane;
import semantic.building.modeler.math.Ray;
import semantic.building.modeler.weightedstraightskeleton.math.MySkeletonVectormath;

/**
 * @author Patrick Gunia Klasse zur Verwaltung einer Edge im Algorithmus eine
 *         Edge ist ein Ray mit beschraenkter Laenge und weiteren
 *         Zusatzkomponenten
 */
public class SkeletonEdge extends Ray {

	/** Startvertex der gerichteten Kante */
	private SkeletonVertex mStartVertex = null;

	/** Endvertex der gerichteten Kante */
	private SkeletonVertex mEndVertex = null;

	/**
	 * Dreieck gebildet durch die Kante und die Winkelhalbierenden an den
	 * Eckpunkten
	 */
	private SkeletonTriangle mTriangle = null;

	/** semi-infinte Ebene, mit einer gewichtsabhaengigen Steigung */
	private Plane mPlane = null;

	/** Standard-Kantengewicht */
	private Float mWeight = 0.7071067f; // 45°
	// private Float mWeight = 0.5f; //30°
	// private Float mWeight = 0.9f; // 64.158066°

	/** Steigung der Ebene an der jeweiligen Kante in Grad */
	private Double mAngle = null;

	/**
	 * "Normalenvektor" der Kante, Vektor senkrecht zur Kante innerhalb der
	 * Grundebene
	 */
	private MyVector3f mNormal = null;

	/**
	 * Rotierte Variante des Normalenvektors, liegt innerhalb der Steigungsebene
	 * der Kante
	 */
	private MyVector3f mRotatedNormal = null;

	/** Instanz der Mathebibliothek */
	private MySkeletonVectormath mMathHelper = null;

	/** Laenge der Kante */
	private float mEdgeLength = -1.0f;

	/** Liste mit Punkten innerhalb der Ebene, DEBUG */
	private List<MyVector3f> pointsOnPlane = null;

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mStartVertex
	 * @param mEndVertex
	 * @param mWeight
	 */
	public SkeletonEdge(SkeletonVertex mStartVertex, SkeletonVertex mEndVertex,
			Float mWeight) {
		super();
		this.mStartVertex = mStartVertex;
		this.mEndVertex = mEndVertex;
		this.mWeight = mWeight;
		this.mMathHelper = MySkeletonVectormath.getInstance();
		this.pointsOnPlane = new Vector<MyVector3f>();
		init();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mStartVertex
	 * @param mEndVertex
	 */
	public SkeletonEdge(SkeletonVertex mStartVertex, SkeletonVertex mEndVertex) {
		super();
		this.mStartVertex = mStartVertex;
		this.mEndVertex = mEndVertex;
		this.mMathHelper = MySkeletonVectormath.getInstance();
		this.pointsOnPlane = new ArrayList<MyVector3f>();
		init();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return
	 */
	@Override
	public SkeletonVertex getStartVertex() {
		return mStartVertex;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mStartVertex
	 */
	public void setStartVertex(SkeletonVertex mStartVertex) {
		this.mStartVertex = mStartVertex;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return
	 */
	@Override
	public SkeletonVertex getEndVertex() {
		return mEndVertex;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mEndVertex
	 */
	public void setEndVertex(SkeletonVertex mEndVertex) {
		this.mEndVertex = mEndVertex;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return
	 */
	public SkeletonTriangle getTriangle() {
		return mTriangle;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mTriangle
	 */
	public void setTriangle(SkeletonTriangle mTriangle) {
		this.mTriangle = mTriangle;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return
	 */
	public Float getWeight() {
		return mWeight;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mWeight
	 */
	public void setWeight(Float mWeight) {
		this.mWeight = mWeight;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return
	 */

	public Plane getPlane() {
		return mPlane;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mPlane
	 */
	public void setPlane(Plane mPlane) {
		this.mPlane = mPlane;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return
	 */
	public Double getAngle() {
		return mAngle;
	}

	// ------------------------------------------------------------------------------------------
	public void setAngle(Double angle) {
		mAngle = angle;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return
	 */
	public MyVector3f getNormal() {
		assert mNormal != null : "Normalenvektor ist null, Kopie kann nicht erzeugt werden";
		return mNormal.clone();
	}

	// ------------------------------------------------------------------------------------------
	public void setNormal(MyVector3f normal) {
		mNormal = normal;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return
	 */
	public MyVector3f getNormalPtr() {
		return mNormal;

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return
	 */
	public float getEdgeLength() {
		return mEdgeLength;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return
	 */
	public MyVector3f getRotatedNormal() {
		assert mRotatedNormal != null : "Rotierter Normalenvektor ist null, Kopie kann nicht erzeugt werden";
		return mRotatedNormal.clone();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return
	 */
	public MyVector3f getRotatedNormalPtr() {
		return mRotatedNormal;

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mRotatedNormal
	 */
	public void setRotatedNormal(MyVector3f mRotatedNormal) {
		this.mRotatedNormal = mRotatedNormal;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return
	 */
	public List<MyVector3f> getPointsOnPlane() {
		return pointsOnPlane;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param pointsOnPlane
	 */
	public void setPointsOnPlane(Vector<MyVector3f> pointsOnPlane) {
		this.pointsOnPlane = pointsOnPlane;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Initialisierungsroutine, berechnet Laenge und Richtung der Kante
	 */
	private void init() {

		assert mStartVertex != null : "Kein Startvertex gesetzt";
		assert mEndVertex != null : "Kein Endvertex gesetzt";

		// berechne einen Strahl basierend auf Start- und Endvertex
		MyVector3f direction = new MyVector3f();
		direction.sub(mEndVertex.getPosition(), mStartVertex.getPosition());

		mEdgeLength = direction.length();

		MyVector3f start = mStartVertex.getPosition();

		setDirection(direction);
		setStart(start);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Berechnet eine Ebene mit einer gewichtsabhaengigen Steigung an die Kante
	 * 
	 * @param algorithm
	 *            Wird benoetigt fuer den Zugriff auf den Slope-Plane-Manager
	 */
	public void calculateSlopePlane(final StraightSkeleton algorithm) {

		// verwende zufallsbasierte Gewichtungen an den Kanten
		// getRandomNumber();

		assert mDirection != null : "Kante besitzt keinen Richtungsvektor";

		// planares Polygon, verwende eine Vertex-Normale
		MyVector3f vertexNormal = mStartVertex.getNormalPtr();

		assert vertexNormal != null : "Vertex besitzt keinen Normalenvektor";

		// berechne einen Normalenvektor fuer die Kante in der gleichen Ebene
		final MyVector3f orthogonalVector = mMathHelper
				.calculateOrthogonalVectorWithSamePlane(getDirectionPtr(),
						vertexNormal);

		// orthogonalVector = mMathHelper.roundVector3f(orthogonalVector);
		mNormal = orthogonalVector;
		mNormal.normalize();

		// rotiere die Senkrechte um die Kante
		final MyVector3f rotatedOrthogonalVector = mMathHelper
				.calculateRotatedVectorRadians(getDirection(),
						orthogonalVector, mWeight);

		// normalisieren
		rotatedOrthogonalVector.normalize();
		mRotatedNormal = rotatedOrthogonalVector;

		assert rotatedOrthogonalVector != null : "Rotierter orthogonaler Vektor ist NULL";

		// berechne nun die Ebene
		// bestimme die Ebenen-Normale:
		final MyVector3f planeNormal = new MyVector3f();

		planeNormal.cross(getDirection(), rotatedOrthogonalVector);
		// planeNormal.scale(-1.0f);
		planeNormal.normalizeRange();

		assert planeNormal != null : "Ebenennormale ist NULL";

		// erzeuge eine neue Ebene basierend auf der Normalen und der
		// Vertexposition als Stuetzvektor
		mPlane = new Plane(planeNormal, mStartVertex.getPosition());

		assert mPlane != null : "FEHLER: Es konnte keine Ebene berechnet werden! Normal: "
				+ planeNormal;

		calculatePointsOnPlane();
	}

	// ------------------------------------------------------------------------------------------
	/** Methode berechnet Punkte auf der Ebene, DEBUG */
	private void calculatePointsOnPlane() {

		// Punkte starten an den Endvertices und folgen dann dem rotierten
		// orthogonalen Vektor
		pointsOnPlane.add(mStartVertex.getPosition());
		MyVector3f direction = this.getRotatedNormal();

		direction.scale(-100.0f);

		MyVector3f secondPoint = new MyVector3f();
		secondPoint.add(mStartVertex.getPosition(), direction);
		pointsOnPlane.add(secondPoint);

		MyVector3f thirdPoint = new MyVector3f();
		thirdPoint.add(mEndVertex.getPosition(), direction);
		pointsOnPlane.add(thirdPoint);

		pointsOnPlane.add(mEndVertex.getPosition());

		// iteriere ueber die Punktemenge und teste fuer jeden Puntk, dass
		// dieser auf der Ebene liegt
		Iterator<MyVector3f> pointIter = pointsOnPlane.iterator();
		MyVector3f currentPoint = null;

		while (pointIter.hasNext()) {
			currentPoint = pointIter.next();
			assert mMathHelper.isPointOnPlane(currentPoint, mPlane) : "Punkt "
					+ currentPoint + " liegt nicht auf der Ebene";
		}

		;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode generiert zufallsbasierte Gewichte einzelner Kanten
	 */
	private void getRandomNumber() {
		Random generator = new Random();
		float resultNumber = generator.nextFloat();

		// minimaler Winkel
		float base = 0.5f;
		resultNumber /= 2;
		if (resultNumber > 0.4f)
			resultNumber = 0.4f;
		resultNumber = base + resultNumber;
		mWeight = resultNumber;

	}
	// ------------------------------------------------------------------------------------------

}
