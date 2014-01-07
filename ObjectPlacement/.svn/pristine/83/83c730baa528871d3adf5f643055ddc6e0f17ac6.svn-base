package semantic.building.modeler.objectplacement.model;

import java.lang.reflect.InvocationTargetException;

import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.Plane;

/**
 * 
 * @author Patrick Gunia
 * 
 *         Klasse dient der Speicherung von relevanten Parametern fuer die
 *         Erzeugung von Komponenten. Welche Parameter hierbei von konkreten
 *         Komponenten-Klassen verwendet werden, haengt von deren Struktur ab.
 *         Ziel dieser Klasse ist die Moeglichkeit, Komponenten zufallsbasiert
 *         auszuwaehlen und ueber einen einheitlichen Konstruktor zu erzeugen,
 *         ohne dabei die spezifischen Besonderheiten der einzelnen Klassen
 *         beruecksichtigen zu muessen. Die jeweiligen Klassen verwenden nach
 *         ihrer Erzeugung ausschliesslich die Komponenten, die sie benoetigen.
 * 
 */

public class ComponentDescriptor {

	/** Mittelpunkt der zu positionierenden Subkomponente */
	private MyVector3f mCenter = null;

	/** Hoehe der Komponente */
	private Float mHeight = null;

	/** Breite der Komponente */
	private Float mWidth = null;;

	/** Ausrichtung der Breitenachse => RECT */
	private MyVector3f mWidthAxis = null;

	/** Ausrichtung der Hoehenachse => RECT */
	private MyVector3f mHeightAxis = null;

	/** Ebene, innerhalb derer alle Komponenten platziert werden => CYLINDER */
	private Plane mGroundPlane = null;

	/** Anzahl der Komponenten, aus denen das Objekt besteht => CYLINDER */
	private Integer mNumberOfSegments = 24;

	/** Handelt es sich bei der beschriebenen Komponente um eine Subkomponente */
	private Boolean isSubcomponent = true;

	/**
	 * Klassenname (exakt!) der Komponente, die durch diesen Descriptor erzeugt
	 * wird. Standard ist RectComponent
	 */
	private String mComponentClassName = "RectComponent";

	// ------------------------------------------------------------------------------------------
	/** Leerer Default-Konstruktor */
	public ComponentDescriptor() {

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mComponentType
	 */
	public String getComponentClassName() {
		return mComponentClassName;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mComponentType
	 *            the mComponentType to set
	 */
	public void setComponentType(String mComponentType) {
		this.mComponentClassName = mComponentType;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * @return the mCenter
	 */
	public MyVector3f getCenter() {
		assert mCenter != null : "FEHLER: Kein Center-Vektor gesetzt";
		return mCenter.clone();
	}

	// ------------------------------------------------------------------------------------------
	public MyVector3f getCenterPtr() {
		return mCenter;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mCenter
	 *            the mCenter to set
	 */
	public void setCenter(MyVector3f mCenter) {
		this.mCenter = mCenter;
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
	 * @param mHeight
	 *            the mHeight to set
	 */
	public void setHeight(Float mHeight) {
		this.mHeight = mHeight;
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
	 * @param mWidth
	 *            the mWidth to set
	 */
	public void setWidth(Float mWidth) {
		this.mWidth = mWidth;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mWidthAxis
	 */
	public MyVector3f getWidthAxis() {
		assert mWidthAxis != null : "FEHLER: Keine Breitenachse gesetzt";
		return mWidthAxis.clone();
	}

	// ------------------------------------------------------------------------------------------

	public MyVector3f getWidthAxisPtr() {
		return mWidthAxis;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mWidthAxis
	 *            the mWidthAxis to set
	 */
	public void setWidthAxis(MyVector3f mWidthAxis) {
		this.mWidthAxis = mWidthAxis;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mHeightAxis
	 */
	public MyVector3f getHeightAxis() {
		assert mHeightAxis != null : "FEHLER: Keine Hoehenachse gesetzt";
		return mHeightAxis.clone();
	}

	// ------------------------------------------------------------------------------------------

	public MyVector3f getHeightAxisPtr() {
		return mHeightAxis;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mHeightAxis
	 *            the mHeightAxis to set
	 */
	public void setHeightAxis(MyVector3f mHeightAxis) {
		this.mHeightAxis = mHeightAxis;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mGroundPlane
	 */
	public Plane getGroundPlane() {
		return mGroundPlane;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mGroundPlane
	 *            the mGroundPlane to set
	 */
	public void setGroundPlane(Plane mGroundPlane) {
		this.mGroundPlane = mGroundPlane;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mNumberOfComponents
	 */
	public Integer getNumberOfSegments() {
		return mNumberOfSegments;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mNumberOfComponents
	 *            the mNumberOfComponents to set
	 */
	public void setNumberOfSegments(Integer mNumberOfComponents) {
		this.mNumberOfSegments = mNumberOfComponents;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the isSubcomponent
	 */
	public boolean isSubcomponent() {
		return isSubcomponent;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param isSubcomponent
	 *            the isSubcomponent to set
	 */
	public void setSubcomponent(boolean isSubcomponent) {
		this.isSubcomponent = isSubcomponent;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Fabrikmethoden-Implementation Methode erstellt abhaengig vom gesetzten
	 * Typ des zu erzeugenden Objekts Instanzen der Subkomponentenklasse. Dier
	 * Erzeugung erfolgt mittels Reflection ueber den gesetzten Klassennamen.
	 * 
	 */
	public AbstractComponent createSubComponent() {

		String classPathPrefix = "semantic.city.builder.objectplacement.model";
		String fullClassName = classPathPrefix + "." + mComponentClassName;

		try {
			// hole ueber den Klassennamen eine Class-Instanz
			Class clazz = Class.forName(fullClassName);

			// hole den AbstractComponent-Konstruktor, der auf Uebergabe einer
			// SubcomponentDescription basiert und fuehre ihn aus
			AbstractComponent result = (AbstractComponent) clazz
					.getConstructor(ComponentDescriptor.class)
					.newInstance(this);
			result.setSubComponent(this.isSubcomponent);

			return result;

		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

		return null;
	}

	// ------------------------------------------------------------------------------------------
	@Override
	/**
	 * Copy-Konstruktor fuer die SubcomponentDescriptor-Klasse, erzeugt
	 * Deep-Copys von Instanzen dieser Klasse
	 */
	public ComponentDescriptor clone() {

		ComponentDescriptor copy = new ComponentDescriptor();
		if (mCenter != null)
			copy.setCenter(mCenter.clone());
		if (mGroundPlane != null)
			copy.setGroundPlane(mGroundPlane.clone());
		if (mHeight != null)
			copy.setHeight(mHeight.floatValue());
		if (mWidth != null)
			copy.setWidth(mWidth.floatValue());
		if (mWidthAxis != null)
			copy.setWidthAxis(mWidthAxis.clone());
		if (mHeightAxis != null)
			copy.setHeightAxis(mHeightAxis.clone());
		if (mNumberOfSegments != null)
			copy.setNumberOfSegments(mNumberOfSegments.intValue());
		if (mComponentClassName != null)
			copy.setComponentType(getComponentClassName());
		if (isSubcomponent != null)
			copy.setSubcomponent(isSubcomponent);

		return copy;

	}

	// ------------------------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String lineBreak = System.getProperty("line.separator");
		return "SubComponentDescriptor: Center: " + mCenter + ", Height: "
				+ mHeight + ", Width: " + mWidth + ", Breitenachse: "
				+ mWidthAxis + ", Hoehenachse: " + mHeightAxis
				+ ", Grundebene: " + mGroundPlane + ", Anzahl der Segmente: "
				+ mNumberOfSegments + ", Klassenname: " + mComponentClassName;
	}
	// ------------------------------------------------------------------------------------------

}
