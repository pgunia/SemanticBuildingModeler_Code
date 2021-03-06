package semantic.building.modeler.objectplacement.model;

import java.util.List;

import semantic.building.modeler.math.MyPolygon;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.Vertex3d;

/**
 * 
 * @author Patrick Gunia Klasse beschreibt Komponenten, die durch das System
 *         zufaellig auf dem Eingabebereich positioniert werden koennen.
 * 
 */

public abstract class AbstractComponent {

	/** Repraesentation des Umrisses als Polygon */
	protected MyPolygon mPolygon = null;

	/** Mittelpunkt der zu positionierenden Komponente */
	protected MyVector3f mCenter = null;

	/** Gibt den Typ der Komponente als String zurueck */
	public abstract String getType();

	/**
	 * Flag zeigt an, ob es sich bei der verarbeiteten Komponente um eine
	 * Subkomponente handelt
	 */
	protected boolean isSubComponent = false;

	/**
	 * Instanz der SubComponentDescriptor-Klasse, die verwendet wird, um
	 * Subkomponenten typunabhaengig zu beschreiben
	 */
	protected ComponentDescriptor mComponentDescriptor = null;

	// ------------------------------------------------------------------------------------------
	/**
	 * Konstruktor mit Uebergabe der Vertices, die die Komponente beschreiben,
	 * definiert im Uhrzeigersinn.
	 * 
	 * @param corners
	 *            Liste von Vertex-Strukturen, definiert im Uhrzeigersinn
	 */
	public AbstractComponent(List<Vertex3d> corners) {
		mPolygon = new MyPolygon(corners);
	}

	// ------------------------------------------------------------------------------------------
	/** Leerer Default-Konstruktor */
	public AbstractComponent() {

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Konstruktor mit Uebergabe einer Subcomponent-Descriptor-Instanz
	 * 
	 * @param descriptor
	 */
	public AbstractComponent(ComponentDescriptor descriptor) {
		mComponentDescriptor = descriptor;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mPoints
	 */
	public List<Vertex3d> getVertices() {
		return mPolygon.getVertices();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mPolygon
	 */
	public MyPolygon getPolygon() {
		return mPolygon;
	}

	// ------------------------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Hauptkomponente: Polygon: " + mPolygon.toString();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * @return the isSubComponent
	 */
	public boolean isSubComponent() {
		return isSubComponent;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param isSubComponent
	 *            the isSubComponent to set
	 */
	public void setSubComponent(boolean isSubComponent) {
		this.isSubComponent = isSubComponent;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * @return the mComponentDescriptor
	 */
	public ComponentDescriptor getComponentDescriptor() {
		return mComponentDescriptor;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mComponentDescriptor
	 *            the mComponentDescriptor to set
	 */
	public void setComponentDescriptor(ComponentDescriptor mComponentDescriptor) {
		this.mComponentDescriptor = mComponentDescriptor;
	}

	// ------------------------------------------------------------------------------------------

}
