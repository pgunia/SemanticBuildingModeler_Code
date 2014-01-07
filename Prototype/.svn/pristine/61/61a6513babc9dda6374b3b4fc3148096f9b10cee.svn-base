package semantic.building.modeler.prototype.graphics.primitives;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import semantic.building.modeler.configurationservice.model.enums.Side;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.MyVectormath;
import semantic.building.modeler.math.Plane;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.prototype.exception.PrototypeException;
import semantic.building.modeler.prototype.graphics.interfaces.iElement;
import semantic.building.modeler.prototype.graphics.interfaces.iGraphicPrimitive;
import semantic.building.modeler.prototype.service.EdgeManager;

/**
 * @author Patrick Gunia Abstrakte Basisklasse der Graphik-Primitiven Quad,
 *         Triangle, TriangleFan und Line
 */
public abstract class AbstractPrimitive implements iElement, iGraphicPrimitive {

	/** Logging-Instanz */
	protected static Logger LOGGER = Logger.getLogger(AbstractPrimitive.class);

	/**
	 * Sofern das Objekt Kind einer weiteren Primitive-Instanz ist, wird diese
	 * hier verspeichert
	 */
	protected iGraphicPrimitive mParent = null;

	/** Indices des primitiven Objekts */
	protected Integer[] mIndices = null;

	/** Ebene des primitiven Objekts */
	protected Plane mPlane = null;

	/** Mittelpunkt */
	protected MyVector3f mCenter = null;

	/** ID fuer Objektverwaltung */
	protected String msID = null;

	/** Systemspezifischer Linebreak fuer Outputstatements */
	protected String mLineBreak = null;

	// ------------------------------------------------------------------------------------------
	public AbstractPrimitive() {
		mLineBreak = System.getProperty("line.separator");
	}

	// ------------------------------------------------------------------------------------------

	@Override
	/**
	 * Methode liefert den vollstaendigen Vertex-Buffer des komplexen Parentobjekts.
	 * @return Liste mit saemtlichen Vertices aller Quad-Strukturen des komplexen Objekts.
	 */
	public List<Vertex3d> getVertices() {

		if (mParent == null) {
			new PrototypeException("AbstractElement: kein Parent gesetzt");
			return null;
		}

		return mParent.getVertices();
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public void setParent(iGraphicPrimitive parent) {
		mParent = parent;

	}

	// ------------------------------------------------------------------------------------------
	public Integer[] getIndices() {
		return mIndices;
	}

	// ------------------------------------------------------------------------------------------

	public void setIndices(Integer[] mIndices) {
		this.mIndices = mIndices;
	}

	// ------------------------------------------------------------------------------------------
	@Override
	public MyVector3f getNormal() {
		assert mPlane != null : "Normalenvektor nicht gesetzt";
		return mPlane.getNormal();

	}

	// ------------------------------------------------------------------------------------------
	public MyVector3f getNormalPtr() {
		assert mPlane != null : "Normalenvektor nicht gesetzt";
		return mPlane.getNormalPtr();
	}

	// ------------------------------------------------------------------------------------------
	public void setCenter(MyVector3f center) {
		if (center != null)
			mCenter = center;

	}

	// ------------------------------------------------------------------------------------------
	@Override
	public MyVector3f getCenter() {

		assert mCenter != null : "Kein Mittelpunkt gesetzt";
		return mCenter.clone();
	}

	// ------------------------------------------------------------------------------------------
	public MyVector3f getCenterPtr() {
		return mCenter;
	}

	// ------------------------------------------------------------------------------------------
	public void setPlane(Plane plane) {
		mPlane = plane;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mPlane
	 */
	public Plane getPlane() {
		return mPlane;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode berechnet den Normalenvektor des primitiven Grafikelements
	 */
	@Override
	public void calculatePlane() {
		List<Vertex3d> vertices = getVertices();

		if (mIndices == null) {
			new PrototypeException(
					"AbstractPrimitive.calculateNormal wurde ohne gesetzte Indices aufgerufen");
			return;
		}

		if (vertices.size() == 0) {
			new PrototypeException(
					"AbstractPrimitive.calculateNormal wurde ein leeres Vertex-Array uebergeben");
			return;
		}

		// erstelle eine Liste mit allen Vertices des aktuellen Quads
		List<Vertex3d> localVerts = new ArrayList<Vertex3d>(mIndices.length);
		for (int i = 0; i < mIndices.length; i++) {
			localVerts.add(vertices.get(mIndices[i]));
		}

		mPlane = MyVectormath.getInstance()
				.calculatePlaneByVertices(localVerts);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode gibt die Koordinaten der Vertices aus, aus denen das Element
	 * besteht
	 */
	public void printVertices() {
		List<Vertex3d> vertices = getVertices();
		System.out
				.println("-------------------------------------------------------------------------");
		for (int i = 0; i < mIndices.length; i++) {
			System.out.println("Index " + mIndices[i] + " ; Vertex: "
					+ vertices.get(mIndices[i]));
		}
		System.out
				.println("-------------------------------------------------------------------------");

	}

	// ------------------------------------------------------------------------------------------
	@Override
	public String getType() {
		return "default";
	}

	// ------------------------------------------------------------------------------------------
	@Override
	public String getID() {
		return msID;
	}

	// ------------------------------------------------------------------------------------------
	@Override
	public void setID(String id) {
		msID = id;
	}

	// ------------------------------------------------------------------------------------------
	/** leere Default-Implementation */
	@Override
	public void generateID(String baseID, String concat) {

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Default-Implementation, wenn eine Klasse diese Methode nicht
	 * ueberschreibt, besitzt sie auch keine Kinder
	 */
	@Override
	public boolean hasChildren() {
		return false;
	}

	// ------------------------------------------------------------------------------------------
	/** leere Default-Implementation */
	@Override
	public Side getDirection() {

		// bsw. fuer Lines, deren Ausrichtung im 3d undefiniert ist
		return Side.UNDEFINED;
	}

	// ------------------------------------------------------------------------------------------
	@Override
	public EdgeManager getEdgeManager() {
		// frage alle Elternelemente bis hinauf zum komplexen Objekt
		return mParent.getEdgeManager();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * erzeugt einen Index aus den Quellindices bei dem der erste Index immer
	 * kleiner ist als der zweite
	 */
	protected String createOrderedIndex(Integer index1, Integer index2) {
		String result = "";

		if (index1 > index2) {
			result = String.valueOf(index2) + "_" + String.valueOf(index1);

		} else {
			result = String.valueOf(index1) + "_" + String.valueOf(index2);
		}

		// result = String.valueOf(index1) + String.valueOf(index2);
		return result;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mParent
	 */
	public iGraphicPrimitive getParent() {
		return mParent;
	}

	// ------------------------------------------------------------------------------------------
	@Override
	/**
	 * Methode wird vor der endgueltigen Zerstoerung durch den GC gecallt
	 */
	protected void finalize() throws Throwable {
		LOGGER.debug("Zerstoere Primitive vom Typ " + getType() + " mit ID: "
				+ getID());
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode ist ein Wrapper fuer die finalize-Methode, um keinen direkten
	 * Zugriff auf diese zu ermoeglichen. Dies erlaubt die Verarbeitung der
	 * Operationen, die alle Daten aus den Objekten entfernen, bevor dies durch
	 * den GC automatisch erfolgt. Bei manchen Operationen ist eine solche
	 * Verarbeitung zu einem bestimmten Zeitpunkt notwendig und nicht erst,
	 * sobald der GC die Berechnungen durchfuehrt
	 */
	public void destroy() {
		LOGGER.debug("Manueller Aufruf der Finalize-Berechnungen");
		try {
			finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	// ------------------------------------------------------------------------------------------

}
