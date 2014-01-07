package semantic.building.modeler.objectplacement.algorithm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import semantic.building.modeler.math.MyPolygon;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.MyVectormath;
import semantic.building.modeler.math.Vertex3d;

/**
 * 
 * @author Patrick Gunia Klasse zur Beschreibung eines Knotens innerhalb eines
 *         Quadtrees
 * 
 * 
 */

public class QuadtreeNode {

	/** Logger */
	protected static Logger logger = Logger
			.getLogger("objectplacement.placementalgorithm.quadtreenode");

	/** Mittelpunkt des rechteckigen Node-Bereichs */
	private MyVector3f mCenter = null;

	/** Laenge des rechteckigen Bereiches */
	private Float mHeight = null;

	/** Breite des rechteckigen Bereichs */
	private Float mWidth = null;

	/** Flag wird gesetzt, wenn der aktuelle Knoten ein Blattknoten ist */
	private boolean isLeaf = true;

	/** Ebene innerhalb des Baumes */
	private Integer mLevel = null;

	/** Elternknoten innerhalb des Baumes */
	private QuadtreeNode mParent = null;

	/**
	 * Kindknoten des QuadtreeNodes, werden im Uhrzeigersinn definiert, beginnen
	 * links oben, rechts oben, rechts unten, links unten
	 */
	private List<QuadtreeNode> mChildren = null;

	/**
	 * Alle nachfolgenden Kanten entsprechen in ihrer Ausrichtung der Eingabe
	 * der Eckpunkte im Uhrzeigersinn
	 */
	/** Richtung der oberen Kante */
	private MyVector3f mUpperEdgeDirection = null;

	/** Richtung der unteren Kante */
	private MyVector3f mLowerEdgeDirection = null;

	/** Richtung der linken Kante */
	private MyVector3f mLeftEdgeDirection = null;

	/** Richtung der rechten Kante */
	private MyVector3f mRightEdgeDirection = null;

	/**
	 * Enum beschreibt den Inhalt eines Quadtrees in Bezug auf die
	 * positionierten Komponenten
	 */
	private ContentType mContentType = ContentType.EMPTY;

	/** Beschreibung des Nodes als Polygon */
	private MyPolygon mPolygon = null;

	/** Position des Nodes im Quadtree in Bezug auf seine Geschwister */
	private NodePosition mNodePosition = null;

	// ------------------------------------------------------------------------------------------

	/**
	 * Konstruktor mit Uebergabe der Eckpunkte des aktuellen Rechtecks
	 * 
	 * @param corners
	 *            Eckpuntke des Rechtecks im Uhrzeigersinn, beginnend links oben
	 */
	public QuadtreeNode(List<Vertex3d> corners, Integer level,
			NodePosition position) {
		super();

		mPolygon = new MyPolygon(corners);
		mNodePosition = position;

		mLevel = level;
		mChildren = new ArrayList<QuadtreeNode>(4);
		computeDimensions();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * @return the mCenter
	 */
	public MyVector3f getCenter() {
		return mCenter;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mHeight
	 */
	public Float getHeight() {
		return mHeight;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mWidth
	 */
	public Float getWidth() {
		return mWidth;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the isLeaf
	 */
	public boolean isLeaf() {
		return isLeaf;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param isLeaf
	 *            the isLeaf to set
	 */
	public void setLeaf(boolean isLeaf) {
		this.isLeaf = isLeaf;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mChildren
	 */
	public List<QuadtreeNode> getChildren() {
		return mChildren;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mLevel
	 */
	public Integer getLevel() {
		return mLevel;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * @return the mParent
	 */
	public QuadtreeNode getParent() {
		return mParent;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mParent
	 *            the mParent to set
	 */
	public void setParent(QuadtreeNode mParent) {
		this.mParent = mParent;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * @return True, sofern ein Elternknoten gesetzt ist, False sonst
	 */
	public boolean hasParent() {
		if (mParent != null)
			return true;
		else
			return false;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Durchlaeuft den Baum rekursiv, bis ein Blattknoten gefunden wird, dieser
	 * wird nach oben durchgereicht, bis alle Knoten eingesammelt sind. Hierbei
	 * wird nur bis zum Aufrufer eingesammelt, nicht zwingend bis zur Wurzel
	 * 
	 * @return Liste mit allen Blattknoten innerhalb des aktuellen Teilbaumes
	 */
	public List<QuadtreeNode> getLeafs() {

		List<QuadtreeNode> result = new ArrayList<QuadtreeNode>();

		// wenn es sich um ein Blatt handelt, fuege es zur Result-Struktur hinzu
		// und
		// gebe diese zurueck
		if (isLeaf) {
			result.add(this);
			return result;
		}
		// sonst rufe rekursiv die Methoden der Kinder auf und fuege deren
		// Rueckgabe hinzu
		else {
			Iterator<QuadtreeNode> childIter = mChildren.iterator();
			QuadtreeNode currentChild = null;

			while (childIter.hasNext()) {
				currentChild = childIter.next();
				result.addAll(currentChild.getLeafs());
			}
		}
		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Erzeugt die naechste Unterteilungsebene fuer den aktuellen
	 * Quadtree-Knoten, berechnet dafuer die Eckpunkte aller 4 Kinder
	 */
	public void createNextLevel() {

		assert isLeaf() : "FEHLER: Startknoten der Unterteilung ist kein Blatt";

		List<Vertex3d> corners = new ArrayList<Vertex3d>(4);
		QuadtreeNode childNode = null;

		List<Vertex3d> mCorners = mPolygon.getVertices();

		// starte links oben
		corners.add(mCorners.get(0));

		// berechne den Punkt auf der Mitte der oberen Kante
		MyVector3f upperEdgeDirection = mUpperEdgeDirection.clone();
		upperEdgeDirection.scale(0.5f);
		MyVector3f upperEdgeCenter = new MyVector3f();
		upperEdgeCenter.add(mCorners.get(0).getPositionPtr(),
				upperEdgeDirection);

		corners.add(new Vertex3d(upperEdgeCenter));
		corners.add(new Vertex3d(mCenter));

		MyVector3f leftEdgeDirection = mLeftEdgeDirection.clone();
		leftEdgeDirection.scale(0.5f);
		MyVector3f leftEdgeCenter = new MyVector3f();
		leftEdgeCenter.add(mCorners.get(3).getPositionPtr(), leftEdgeDirection);
		corners.add(new Vertex3d(leftEdgeCenter));

		childNode = new QuadtreeNode(corners, mLevel + 1,
				NodePosition.UPPERLEFT);
		mChildren.add(childNode);

		// rechts oben
		corners = new ArrayList<Vertex3d>(4);
		corners.add(new Vertex3d(upperEdgeCenter));
		corners.add(mCorners.get(1));

		MyVector3f rightEdgeDirection = mRightEdgeDirection.clone();
		rightEdgeDirection.scale(0.5f);
		MyVector3f rightEdgeCenter = new MyVector3f();
		rightEdgeCenter.add(mCorners.get(1).getPositionPtr(),
				rightEdgeDirection);
		corners.add(new Vertex3d(rightEdgeCenter));
		corners.add(new Vertex3d(mCenter));
		childNode = new QuadtreeNode(corners, mLevel + 1,
				NodePosition.UPPERRIGHT);
		mChildren.add(childNode);

		// rechts unten
		corners = new ArrayList<Vertex3d>(4);
		corners.add(new Vertex3d(mCenter));
		corners.add(new Vertex3d(rightEdgeCenter));
		corners.add(mCorners.get(2));

		MyVector3f lowerEdgeDirection = mLowerEdgeDirection.clone();
		lowerEdgeDirection.scale(0.5f);
		MyVector3f lowerEdgeCenter = new MyVector3f();
		lowerEdgeCenter.add(mCorners.get(2).getPositionPtr(),
				lowerEdgeDirection);
		corners.add(new Vertex3d(lowerEdgeCenter));
		childNode = new QuadtreeNode(corners, mLevel + 1,
				NodePosition.LOWERRIGHT);
		mChildren.add(childNode);

		// links unten
		corners = new ArrayList<Vertex3d>(4);
		corners.add(new Vertex3d(leftEdgeCenter));
		corners.add(new Vertex3d(mCenter));
		corners.add(new Vertex3d(lowerEdgeCenter));
		corners.add(mCorners.get(3));

		childNode = new QuadtreeNode(corners, mLevel + 1,
				NodePosition.LOWERLEFT);
		mChildren.add(childNode);

		// Knoten ist nun nicht mehr laenger Wurzel
		setLeaf(false);

		// setze bei allen Kindern den aktuellen Knoten als Elternknoten
		for (int i = 0; i < mChildren.size(); i++)
			mChildren.get(i).setParent(this);

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet aufgrund der gesetzten Eckpunkte Laenge, Breite und
	 * Mittelpunkt des Rechtecks
	 */
	private void computeDimensions() {

		assert mPolygon != null : "FEHLER: Fuer den aktuellen Quadtree-Knoten ist kein Polygon definiert";

		List<Vertex3d> mCorners = mPolygon.getVertices();

		// berechne Laenge und Hoehe basierend auf den Eckpunkten
		MyVector3f upperLeftCorner = mCorners.get(0).getPosition();
		MyVector3f upperRightCorner = mCorners.get(1).getPosition();
		MyVector3f lowerRightCorner = mCorners.get(2).getPosition();
		MyVector3f lowerLeftCorner = mCorners.get(3).getPosition();

		mUpperEdgeDirection = new MyVector3f();
		mUpperEdgeDirection.sub(upperRightCorner, upperLeftCorner);
		mWidth = mUpperEdgeDirection.length();

		mRightEdgeDirection = new MyVector3f();
		mRightEdgeDirection.sub(lowerRightCorner, upperRightCorner);
		mHeight = mRightEdgeDirection.length();

		mLeftEdgeDirection = new MyVector3f();
		mLeftEdgeDirection.sub(upperLeftCorner, lowerLeftCorner);

		mLowerEdgeDirection = new MyVector3f();
		mLowerEdgeDirection.sub(lowerLeftCorner, lowerRightCorner);

		// berechne den Mittelpunkt als Schnitt zweier Geraden
		mCenter = MyVectormath.getInstance().calculateQuadCenter(mCorners);

		assert mCenter != null : "FEHLER: Es konnte kein Mittelpunkt ermittelt werden";

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mUpperEdgeDirection
	 */
	public MyVector3f getUpperEdgeDirection() {
		return mUpperEdgeDirection.clone();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mLowerEdgeDirection
	 */
	public MyVector3f getLowerEdgeDirection() {
		return mLowerEdgeDirection.clone();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * @return the mLeftEdgeDirection
	 */
	public MyVector3f getLeftEdgeDirection() {
		return mLeftEdgeDirection.clone();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mRightEdgeDirection
	 */
	public MyVector3f getRightEdgeDirection() {
		return mRightEdgeDirection.clone();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mPolygon
	 */
	public MyPolygon getPolygon() {
		return mPolygon;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mContentType
	 */
	public ContentType getContentType() {
		return mContentType;
	}

	// ------------------------------------------------------------------------------------------
	public void setContentType(ContentType mContentType) {
		this.mContentType = mContentType;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode setzt den Content-Type des Knotens, dabei wird die in der
	 * Enum-Deklaration definiert Ordnungsrelation genutzt, um eine
	 * Prioritaetenliste zu implementieren => der uebergebene Wert ueberschreibt
	 * nur dann den gesetzten, wenn der neue Wert eine hoehere Prioritaet
	 * besitzt
	 * 
	 * @param mContentType
	 *            the mContentType to set
	 */
	public void replaceContentType(ContentType contentType) {

		int compareResult = mContentType.compareTo(contentType);

		// wenn das Vergleichsergebnis < 0 ist, hat der uebergebene Wert eine
		// hoehere Prioritaet
		if (compareResult < 0)
			mContentType = contentType;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Setzt den Content-Type des aktuellen Knotens und den seiner Kinder
	 * zurueck auf EMPTY
	 */
	public void resetNode() {
		setContentType(ContentType.EMPTY);

		if (!isLeaf) {
			Iterator<QuadtreeNode> childIter = mChildren.iterator();
			while (childIter.hasNext())
				childIter.next().resetNode();
		}

	}

	// ------------------------------------------------------------------------------------------

}
