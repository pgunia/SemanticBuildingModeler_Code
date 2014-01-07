package semantic.building.modeler.configurationservice.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.helper.XMLParsingHelper;

/**
 * Klasse dient der Konfiguration des Objectplacement-Verfahrens
 * 
 * @author Patrick Gunia
 * 
 */

public class ObjectPlacementFootprintConfiguration extends
		AbstractConfigurationObject {

	/**
	 * Konfigurationsinstanz gibt an, aus welcher Quelle die Polygone stammen,
	 * die als Basis fuer die Modifikation eingesetzt wird
	 */
	private transient PolygonSourceConfiguration mPolySourceConfiguration = null;

	/**
	 * Verhaeltnisvariablen => legen die Bereiche fest, innerhalb derer sich die
	 * Ausdehnungen von Sub- und Hauptkomponten bewegen
	 */
	/** Reskalierungsfaktor fuer positionierte Subkomponenten */
	private transient RangeConfigurationObject mRatioScalingSubcomponents = new RangeConfigurationObject(
			0.6f, 0.9f);

	/** Reskalierungsfaktor fuer positionierte Hauptkomponente */
	private transient RangeConfigurationObject mRatioScalingMaincomponent = new RangeConfigurationObject(
			0.4f, 0.7f);

	/** Verhaeltnis von Subkomponentenhoehe zu Hauptkomponentenbreite */
	private transient RangeConfigurationObject mHeightWidthRatioComponents = new RangeConfigurationObject(
			0.4f, 0.8f);

	/**
	 * Verhaeltnis zwischen der Ausdehnung der Subkomponenten- zur
	 * Hauptkomponentenbreite => dadurch sollen riesige Subkomponenten vermieden
	 * werden
	 */
	private transient Float mMaxSubToMainComponentWidth = 100.0f;

	/**
	 * Minimales Verhaeltnis von Subkomponenten- zu Hauptkomponentenbreite, so
	 * dass die Subkomponente positioniert wird
	 */
	private transient Float mMinMainToSubcomponentDeviance = 0.07f;

	/**
	 * Verhaeltnis zwischen Subkomponentenhoehe und -breite fuer Subkomponenten,
	 * die auf Ecken positioniert wurden im Vergleich mit den Ausdehnungen der
	 * Hauptkomponente
	 */
	private transient Float mMaxCornerMainComponentDimensionsRatio = 0.4f;

	/** Absolte Angaben bsw. Abstande, Groessen etc. */
	/** Minimale Anzahl freier Bloecke zwischen benachbarten Subkomponenten */
	private transient Integer mMinNumberOfFreeBlocks = 3;

	/**
	 * Maximale Anzahl an Subkomponenten, die pro Strahl der Hauptkonmponente
	 * positioniert werden
	 */
	private transient Integer mMaxNumberOfSubcomponentsPerRay = 3;

	/** Minimale Subkomponentengroesse in Bloecken */
	private transient Integer mMinSubcomponentSizeInBlocks = 4;

	/** Liste mit den Klassennamen der erlaubten Subkomponenten */
	private transient List<String> mSubcomponentClassnames = new ArrayList<String>(
			Arrays.asList(new String[] { "RectComponent", "CylindricComponent" }));

	/**
	 * Wie viele Versuche hat der Algorithmus pro Strahl der Hauptkomponente,
	 * Subkomponenten zu positionieren
	 */
	private transient Integer mMaxNumberOfRetriesPerRay = 10;

	/**
	 * Anzahl der Iterationen des Verfahrens. Bei Werten groesser 1 werden
	 * rekursiv Subkomponenten auf vorab positionierte Subkomponenten
	 * positioniert
	 */
	private transient Integer mNumberOfIterations = 1;

	/**
	 * Wahrscheinlichkeitsparameter, die den randomisierten Erzeugungsprozess
	 * steuern
	 */
	/**
	 * Wahrscheinlichkeit, dass die Positionierung von Subkomponenten
	 * symmetrisch erfolgt
	 */
	private transient Float mProbSymmetry = 0.0f;

	/** Wahrscheinlichkeit, Subkomponenten auf Eckpunkten zu positionieren */
	private transient Float mProbPositionSubcomponentsOnCorners = 0.1f;

	/**
	 * Wahrscheinlichkeit, dass das System zylindrische Subkomponenten platziert
	 */
	private transient Float mProbPositionCylinders = 0.1f;

	/** Wahrscheinlichkeit, dass Komponenten auf Kanten positioniert werden */
	private transient Float mProbPositionComponentsOnEdge = 0.7f;

	/**
	 * Wahrscheinlichkeit, dass Komponenten auf der Mitte eines Strahls
	 * positioniert werden
	 */
	private transient Float mProbPositionOnCenter = 1.0f;

	/**
	 * Wahrscheinlichkeit, dass eine Convex-Hull-Implementation eingesetzt wird,
	 * um aus den Footprint-Subkomponenten ein Grundriss-Polygon zu errechnen
	 */
	private transient Float mProbConvexHull = 0.0f;

	// -------------------------------------------------------------------------------------

	@Override
	public void construct(Element configRoot) {

		Namespace namespace = getNamespace();
		XMLParsingHelper parsing = XMLParsingHelper.getInstance();

		assert configRoot.getName().equals("objectplacement") : "FEHLER: Ungueltiges Konfigurationsobjekt! Tagname: "
				+ configRoot.getName();

		Element polySourceElement = configRoot.getChild("polygonSource",
				mProcessingMetadata.getNamespaceByPrefix("ct"));
		assert polySourceElement != null : "FEHLER: Es ist keine Polygon-Quelle definiert!";

		mPolySourceConfiguration = new PolygonSourceConfiguration();
		mPolySourceConfiguration.construct(polySourceElement);

		mRatioScalingSubcomponents = new RangeConfigurationObject(
				configRoot.getChild("placementScalingSubcomponents", namespace),
				mRatioScalingSubcomponents);
		mRatioScalingMaincomponent = new RangeConfigurationObject(
				configRoot
						.getChild("placementScalingMaincomponents", namespace),
				mRatioScalingMaincomponent);
		mHeightWidthRatioComponents = new RangeConfigurationObject(
				configRoot.getChild("heightWidthRatioComponents", namespace),
				mHeightWidthRatioComponents);

		mMinNumberOfFreeBlocks = parsing.getInteger(configRoot,
				"minNumberOfFreeBlocks", namespace, mMinNumberOfFreeBlocks);
		mMaxNumberOfSubcomponentsPerRay = parsing.getInteger(configRoot,
				"maxNumberOfSubcomponentsPerRay", namespace,
				mMaxNumberOfSubcomponentsPerRay);
		mMinSubcomponentSizeInBlocks = parsing.getInteger(configRoot,
				"minSubcomponentSizeInBlocks", namespace,
				mMinSubcomponentSizeInBlocks);
		mMaxCornerMainComponentDimensionsRatio = parsing.getFloat(configRoot,
				"maxCornerMainComponentDimensionsRatio", namespace,
				mMaxCornerMainComponentDimensionsRatio);
		mMaxNumberOfRetriesPerRay = parsing.getInteger(configRoot,
				"maxNumberOfRetriesPerRay", namespace,
				mMaxNumberOfRetriesPerRay);
		mNumberOfIterations = parsing.getInteger(configRoot,
				"numberOfIterations", namespace, mNumberOfIterations);

		mMinMainToSubcomponentDeviance = parsing.getFloat(configRoot,
				"minMainToSubcomponentDeviance", namespace,
				mMinMainToSubcomponentDeviance);

		processSubcomponentTypes(configRoot.getChild(
				"maxCornerMainComponentDimensionsRatio", namespace));

		Element probabilityRoot = configRoot.getChild("placementProbability",
				namespace);
		mProbSymmetry = parsing.getFloat(probabilityRoot, "symmetry",
				namespace, mProbSymmetry);
		mProbPositionSubcomponentsOnCorners = parsing.getFloat(probabilityRoot,
				"positionSubComponentsOnCorners", namespace,
				mProbPositionSubcomponentsOnCorners);
		mProbPositionCylinders = parsing.getFloat(probabilityRoot,
				"positionCylinders", namespace, mProbPositionCylinders);
		mProbPositionComponentsOnEdge = parsing.getFloat(probabilityRoot,
				"positionComponentsOnEdge", namespace,
				mProbPositionComponentsOnEdge);
		mProbPositionOnCenter = parsing.getFloat(probabilityRoot,
				"positionOnCenter", namespace, mProbPositionOnCenter);
		mProbConvexHull = parsing.getFloat(probabilityRoot, "convexHull",
				namespace, mProbConvexHull);

		LOGGER.debug("Objectplacement-Config: " + this);
	}

	// -------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return mProcessingMetadata.getNamespaceByPrefix("op");
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode liest die Klassennamen der erlaubten Subkomponenten auf
	 * 
	 * @param allowedSubcomponents
	 *            XML-Element, unter dem die Klassennamen zu finden sind
	 */
	private void processSubcomponentTypes(final Element allowedSubcomponents) {

		// wenn keine Klassen angegeben sind, verwende den Default-Wert, sonst
		// parse das XML-Element
		if (allowedSubcomponents == null) {
			return;
		} else {
			List<Element> types = allowedSubcomponents.getChildren("type",
					getNamespace());
			mSubcomponentClassnames = new ArrayList<String>(types.size());

			Element curType = null;
			for (int i = 0; i < types.size(); i++) {
				curType = types.get(i);
				mSubcomponentClassnames.add(curType.getValue());
			}
		}
	}

	// -------------------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "RatioScalingSubcomponents:" + mRatioScalingSubcomponents
				+ ", RatioScalingMaincomponent:" + mRatioScalingMaincomponent
				+ ", HeightWidthRatioComponents:" + mHeightWidthRatioComponents
				+ ", MinNumberOfFreeBlocks:" + mMinNumberOfFreeBlocks
				+ ", MaxNumberOfSubcomponentsPerRay:"
				+ mMaxNumberOfSubcomponentsPerRay
				+ ", MinSubcomponentSizeInBlocks:"
				+ mMinSubcomponentSizeInBlocks + ", SubcomponentClassnames:"
				+ mSubcomponentClassnames
				+ ", MaxCornerMainComponentDimensionsRatio:"
				+ mMaxCornerMainComponentDimensionsRatio
				+ ", MaxNumberOfRetriesPerRay:" + mMaxNumberOfRetriesPerRay
				+ ", MinMainToSubcomponentDeviance:"
				+ mMinMainToSubcomponentDeviance + ", ProbSymmetry:"
				+ mProbSymmetry + ", ProbPositionSubcomponentsOnCorners:"
				+ mProbPositionSubcomponentsOnCorners
				+ ", ProbPositionCylinders:" + mProbPositionCylinders
				+ ", ProbPositionComponentsOnEdge:"
				+ mProbPositionComponentsOnEdge + ", ProbPositionOnCenter:"
				+ mProbPositionOnCenter + ", ProbConvexHull:" + mProbConvexHull;
	}

	// -------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "ObjectplacementFootprint";
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mRatioScalingSubcomponents
	 */
	public RangeConfigurationObject getRatioScalingSubcomponents() {
		return mRatioScalingSubcomponents;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mRatioScalingMaincomponent
	 */
	public RangeConfigurationObject getRatioScalingMaincomponent() {
		return mRatioScalingMaincomponent;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mHeightWidthRatioComponents
	 */
	public RangeConfigurationObject getHeightWidthRatioComponents() {
		return mHeightWidthRatioComponents;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mMinNumberOfFreeBlocks
	 */
	public Integer getMinNumberOfFreeBlocks() {
		return mMinNumberOfFreeBlocks;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mMaxNumberOfSubcomponentsPerRay
	 */
	public Integer getMaxNumberOfSubcomponentsPerRay() {
		return mMaxNumberOfSubcomponentsPerRay;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mMinSubcomponentSizeInBlocks
	 */
	public Integer getMinSubcomponentSizeInBlocks() {
		return mMinSubcomponentSizeInBlocks;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mSubcomponentClassnames
	 */
	public List<String> getSubcomponentClassnames() {
		return mSubcomponentClassnames;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mMaxCornerMainComponentDimensionsRatio
	 */
	public Float getMaxCornerMainComponentDimensionsRatio() {
		return mMaxCornerMainComponentDimensionsRatio;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mMaxNumberOfRetriesPerRay
	 */
	public Integer getMaxNumberOfRetriesPerRay() {
		return mMaxNumberOfRetriesPerRay;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mMinMainToSubcomponentDeviance
	 */
	public Float getMinMainToSubcomponentDeviance() {
		return mMinMainToSubcomponentDeviance;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mProbSymmetry
	 */
	public Float getProbSymmetry() {
		return mProbSymmetry;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mProbPositionSubcomponentsOnCorners
	 */
	public Float getProbPositionSubcomponentsOnCorners() {
		return mProbPositionSubcomponentsOnCorners;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mProbPositionCylinders
	 */
	public Float getProbPositionCylinders() {
		return mProbPositionCylinders;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mProbPositionComponentsOnEdge
	 */
	public Float getProbPositionComponentsOnEdge() {
		return mProbPositionComponentsOnEdge;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mProbPositionOnCenter
	 */
	public Float getProbPositionOnCenter() {
		return mProbPositionOnCenter;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mProbConvexHull
	 */
	public Float getProbConvexHull() {
		return mProbConvexHull;
	}

	// -------------------------------------------------------------------------------------
	public Float getMaxSubToMainComponentWidth() {
		return mMaxSubToMainComponentWidth;
	}

	// -------------------------------------------------------------------------------------
	/**
	 * @return the mNumberOfIterations
	 */
	public Integer getNumberOfIterations() {
		return mNumberOfIterations;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mPolySourceConfiguration
	 */
	public PolygonSourceConfiguration getPolySourceConfiguration() {
		return mPolySourceConfiguration;
	}

	// -------------------------------------------------------------------------------------

}
