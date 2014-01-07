package semantic.building.modeler.weightedstraightskeleton.result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import semantic.building.modeler.math.MyVector2f;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.weightedstraightskeleton.math.MySkeletonVectormath;

/**
 * 
 * @author Patrick Gunia Abstrakte Basisklasse fuer die Elemente, aus denen sich
 *         ein Result-Objekt zusammensetzt Hierbei soll explizit darauf geachtet
 *         werden, die Strukturen frei von algorithmen-spezifischen Elementen zu
 *         halten.
 * 
 */
public abstract class AbstractResultElement {

	/**
	 * ein ResultElement speichert Zeiger auf seinen direkten Nachbarn innhalb
	 * des gleichen Faces
	 */
	protected AbstractResultElement mUpperNeighbour = null;
	protected AbstractResultElement mLowerNeighbour = null;
	protected AbstractResultElement mRightNeighbour = null;
	protected AbstractResultElement mLeftNeighbour = null;

	/** Hoehe des Elements (sofern definiert) */
	protected float mHeight;

	/** Breite der unteren Kante (sofern definiert) */
	protected float mWidthLowerEdge;

	/** Breite der oberen Kante (sofern definiert) */
	protected float mWidthUpperEdge;

	/**
	 * speichert die Eckpunkte des jeweiligen Elements. Je nach Typ variiert die
	 * Anzahl.
	 */
	protected List<Vertex3d> mPoints = null;

	/**
	 * speichert den minimalen Skalierungsfaktor bzgl. der Texturierung ueber
	 * alle Faces, damit anschliessend alle Texturkoordinaten uniform skaliert
	 * werden koennen
	 */
	private static float mMinScaleFactor = Float.MAX_VALUE;

	/** Logger */
	protected static Logger LOGGER = Logger
			.getLogger(AbstractResultElement.class);

	/**
	 * offset bzgl. der u-Koordinate (bzgl. des Ursprungs des
	 * Koordinatensystems), dient dem "Buendig-machen" zwischen Nachbar-Faces
	 */
	protected float mTextureOffsetULeft = 0.0f;

	/**
	 * offset bzgl. der u-Koordinate (bzgl. des Max-Wertes der u-Koordinate im
	 * Texturraum, also der rechten Seite)
	 */
	protected float mTextureOffsetURight = 0.0f;

	/**
	 * @return Anzahl an Punkten innerhalb des jeweiligen Elements, abhaengig
	 *         vom Elementtyp
	 */
	public abstract int getNumberOfPoints();

	/**
	 * @return Elementtyp
	 */
	public abstract String getType();

	/**
	 * Fuehrt saemtliche Berechnungen durch, die fuer die Bestimmung der
	 * Texturkoordinaten erforderlich sind. Dazu gehoert die Bestimmung der
	 * Ausdehnungen (Hoehe, Breite) sowie die an den Ausdehnungen der Textur und
	 * dem Element orientierte Berechnung der Texturkoordinaten.
	 * 
	 * @param width
	 *            Texturbreite
	 * @param height
	 *            Texturhoehe
	 */
	protected abstract void createTextureCoords(float width, float height);

	/**
	 * Methode fuehrt Element-spezifische Berechnungen durch, bsw. die Hoehe des
	 * jeweiligen Elements. Da die Berechnungen vom Elementtyp abhaengen, muss
	 * jede abgeleitete Klassen eigene Berechnungen implementieren.
	 */
	protected abstract void doComputations();

	/** Instanz der Mathebibliothek */
	protected final static MySkeletonVectormath mMathHelper = MySkeletonVectormath
			.getInstance();

	/**
	 * speichert indizierte Textturkoordinaten der Face-Vertices, muessen von
	 * den abgeleiteten Klassen befuellt werden. wird verwendet, um die
	 * Texturkoordinaten unabhaengig von der raeumlichen Position der Vertices
	 * zu machen, so kann ein Vertex in unterschiedlichen Faces unterschiedliche
	 * Texturkoordinaten besitzen
	 */
	protected Map<Integer, MyVector2f> mTextureCoords = null;

	/** Normalenvektor des Faces */
	protected MyVector3f mNormal = null;

	// ------------------------------------------------------------------------------------------

	public AbstractResultElement() {
		super();
		mPoints = new ArrayList<Vertex3d>();

		// maximale 4 Elemente
		mTextureCoords = new HashMap<Integer, MyVector2f>();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode soll aufgerufen werden, sobald alle Punkte des Elements gesetzt
	 * sind. In diesem Fall werden anschliessend die Berechnungen fuer die
	 * Ausdehnungen durchgefuehrt und die Texturkoordinaten der Vertices
	 * bestimmt.
	 * 
	 * @param width
	 *            Breite der verwendeten Textur in Pixeln
	 * @param height
	 *            Hoehe der verwendeten Textur in Pixeln
	 */
	public void finalizeElement(float width, float height) {
		assert mPoints.size() == getNumberOfPoints() : "Die notwendige Anzahl an Punkten "
				+ getNumberOfPoints()
				+ " wurde fuer das Element noch nicht gesetzt. Vorhandene Anzahl: "
				+ mPoints.size();
		doComputations();
		createTextureCoords(width, height);
		// validateTexturspaceRanges();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode durchlaeuft saemtliche Vertices des Elements und skaliert diese
	 * mit Hilfe des globalen Skalierungsfaktors, anschliessend werden die so
	 * berechnten Koordinaten validiert
	 */
	public void scaleAndValidateTexture() {

		MyVector2f currentCoords = null;

		Iterator<MyVector2f> coordIter = mTextureCoords.values().iterator();

		// durchlaufe alle berechneten Koordinaten und skaliere sie mit dem
		// globalen Skalierungsfaktor
		while (coordIter.hasNext()) {
			currentCoords = coordIter.next();
			currentCoords.scale(getMinScaleFactor());

		}
		// validiere nun, ob die skalierten Texturkoordinaten innerhalb des
		// Texturraums liegen
		validateTexturspaceRanges();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Fuegt Punkte zum Vektor des jeweiligen Elements hinzu.
	 * 
	 * @param point
	 *            Vertex, das zum Vektor hinzugefuegt werden soll
	 */
	public void addPoint(Vertex3d point) {

		assert mPoints.size() < getNumberOfPoints() : "Die maximale Anzahl an Punkte dieses "
				+ getType() + " wurde ueberschritten";

		if (mPoints.indexOf(point) == -1) {
			mPoints.add(point);
		}
	}

	// ------------------------------------------------------------------------------------------
	public AbstractResultElement getUpperNeighbour() {
		return mUpperNeighbour;
	}

	// ------------------------------------------------------------------------------------------

	public void setUpperNeighbour(AbstractResultElement mUpperNeighbour) {
		this.mUpperNeighbour = mUpperNeighbour;
	}

	// ------------------------------------------------------------------------------------------

	public AbstractResultElement getLowerNeighbour() {
		return mLowerNeighbour;
	}

	// ------------------------------------------------------------------------------------------

	public void setLowerNeighbour(AbstractResultElement mLowerNeighbour) {
		this.mLowerNeighbour = mLowerNeighbour;
	}

	// ------------------------------------------------------------------------------------------

	public AbstractResultElement getRightNeighbour() {
		return mRightNeighbour;
	}

	// ------------------------------------------------------------------------------------------

	public void setRightNeighbour(AbstractResultElement mRightNeighbour) {
		this.mRightNeighbour = mRightNeighbour;
	}

	// ------------------------------------------------------------------------------------------

	public AbstractResultElement getLeftNeighbour() {
		return mLeftNeighbour;
	}

	// ------------------------------------------------------------------------------------------

	public void setLeftNeighbour(AbstractResultElement mLeftNeighbour) {
		this.mLeftNeighbour = mLeftNeighbour;
	}

	// ------------------------------------------------------------------------------------------

	public List<Vertex3d> getPoints() {
		return mPoints;
	}

	// ------------------------------------------------------------------------------------------
	public boolean hasUpperNeighbour() {
		if (mUpperNeighbour == null)
			return false;
		else
			return true;
	}

	// ------------------------------------------------------------------------------------------
	public boolean hasLeftNeighbour() {
		if (mLeftNeighbour == null)
			return false;
		else
			return true;
	}

	// ------------------------------------------------------------------------------------------

	public boolean hasLowerNeighbour() {
		if (mLowerNeighbour == null)
			return false;
		else
			return true;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Liefert die Texturkoordinaten als zweidimensionalen Vektor fuer das
	 * Vertex mit uebergebenem Index.
	 * 
	 * @param index
	 *            Index des Anfragevertex im Punktebuffer des Faces
	 * @return Texturkoordinaten als zweidimensionaler Vektor
	 */
	public MyVector2f getTextureCoordsByIndex(int index) {

		assert index >= 0 && mTextureCoords.size() > index : "Index out of Range, fuer das Vertex existieren keine Texturkoordinaten";
		MyVector2f result = mTextureCoords.get(index);
		assert result != null : "Es existieren keine Texturkoordinaten an Index "
				+ index + " fuer Facetyp: " + getType();
		return result.clone();

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode dient dem Update des minimalen Skalierungsfaktors. Dieser wird
	 * verwendet, um die Texturkoordinaten aller Result-Elemente uniform zu
	 * skalieren. Berechnet wird der Skalierungsfaktor derart, dass bei allen
	 * Elementen das Verhaeltnis der Texturausdehnungen (Breite und Hoehe) zu
	 * den Elementausdehnungen (Breite und Hoehe) berechnet wird. Anschliessend
	 * wird der kleinere der beiden Werte an diese Methode uebergeben und der
	 * globale Skalierungsfaktor wird angepasst, sofern der neue Wert kleiner
	 * als der vorherige ist. Dies fuehrt dazu, dass alle Elemente innerhalb des
	 * gueltigen Texturraums uniform skaliert werden und somit gleiche
	 * Ausdehnungen innerhalb dieses Raumes besitzen.
	 * 
	 * @param newFactor
	 *            Skalierungsfaktor eines einzelnen Elements.
	 */
	protected void updateMinScaleFactor(float newFactor) {
		if (mMinScaleFactor > newFactor) {
			// System.out.println("Update Min-Scale-Factor: alter Wert: " +
			// mMinScaleFactor + " neuer Wert: " + newFactor);
			mMinScaleFactor = newFactor;
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Liefert den aktuell kleinsten Skalierungsfaktor ueber alle Elemente.
	 * 
	 * @return Rueckgabe ist der kleinste Skalierungsfaktor, der waehrend der
	 *         gesamten Berechnung aufgetreten ist.
	 */
	public float getMinScaleFactor() {
		return mMinScaleFactor;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Liefert den Offset bzgl. der u-Texturkoordinate des darunterliegenden
	 * Nachbarn
	 * 
	 * @return Offset des darunterliegenden Nachbarn
	 */
	public float getTextureOffset() {
		return mTextureOffsetULeft;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Liefert die maximale u-Koordinate des Elements im Texturraum. Wird
	 * verwendet fuer die Bestimmung des korrekten Offsets bei Nachbarelementen
	 * auf der gleiche Face-Ebene (bsw. bei Split- oder Vertex-Event-Elementen)
	 * 
	 * @return Maximal vorkommende u-Koordinate des Elements
	 */
	public float getTextureOffsetMaxU() {
		return mTextureOffsetURight;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode wird nach der Berechnung der Texturkoordinaten aufgerufen und
	 * testet, ob die Texturkoordinaten alle innerhalb des gueltigen Ranges von
	 * 0.0f <= x <= 1.0f liegen. Wenn dies nicht der Fall ist, wird ein
	 * AssertionError ausgeloest.
	 */
	protected void validateTexturspaceRanges() {
		Collection<MyVector2f> textureCoords = mTextureCoords.values();
		Iterator<MyVector2f> coordIter = textureCoords.iterator();
		MyVector2f currentCoords = null;
		while (coordIter.hasNext()) {
			currentCoords = coordIter.next();
			// tolerante Texturraumvalidieriung, um Floatingpoint-Rundungsfehler
			// auszugleichen
			if (!(currentCoords.x >= 0.0f && currentCoords.x <= 1.0001f)) {
				assert false : "Texturkoordinaten liegen ausserhalb des gueltigen Bereichs: "
						+ currentCoords + " fuer " + this;
			}
			if (!(currentCoords.y >= 0.0f && currentCoords.y <= 1.0001f)) {
				assert false : "Texturkoordinaten liegen ausserhalb des gueltigen Bereichs: "
						+ currentCoords + " fuer " + this;
			}
		}
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public String toString() {
		String message = getType() + ": Vertices: ";
		String lineSeparator = System.getProperty("line.separator");
		for (int i = 0; i < mPoints.size(); i++) {
			message += i + ": " + mPoints.get(i).getPositionPtr()
					+ lineSeparator;
			// message += " Textur: " + mTextureCoords.get(i);
		}
		return message;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode validiert, dass alle Punkte des ResultElements innerhalb einer
	 * Ebene liegen. Fehler koennen nur bei Quads auftreten, darum ueberschreibt
	 * nur die ResultQuad-Klasse diese Methode mit tatsaechlichen Berechnungen
	 * 
	 * @return True, falls alle Punkte des Elements auf der Ebene liegen, false
	 *         sonst
	 * 
	 */
	public boolean validatePlane() {
		return true;
	}
	// ------------------------------------------------------------------------------------------

}
