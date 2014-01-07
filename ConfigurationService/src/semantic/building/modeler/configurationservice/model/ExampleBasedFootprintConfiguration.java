package semantic.building.modeler.configurationservice.model;

import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.helper.XMLParsingHelper;
import semantic.building.modeler.configurationservice.model.enums.RuleType;
import semantic.building.modeler.math.MyPolygon;

/**
 * Konfigurationsklasse fuer beispielbasierte Grundrisserzeugung
 * 
 * @author Patrick Gunia
 * 
 */

public class ExampleBasedFootprintConfiguration extends
		AbstractConfigurationObject {

	/**
	 * Konfigurationsinstanz gibt an, aus welcher Quelle die Polygone stammen,
	 * die fuer die Grundrisssynthese eingesetzt werden
	 */
	private transient PolygonSourceConfiguration mPolySourceConfiguration = null;

	/** Bezeichner fuer den Typ der ersten angewendeten Regel */
	private transient RuleType mFirstRuleType = RuleType.INSIDE;

	/** Obergrenze fuer die Anwendung von Regeln des Typs "CORNER" */
	private transient Integer mCornerCount = -1;

	/** Obergrenze fuer die Anwendung von Regeln des Typs "REFLEX_CORNER" */
	private transient Integer mReflexCornerCount = -1;

	/** Obergrenze fuer die Anwendung von Regeln des Typs "EDGE" */
	private transient Integer mEdgeCount = -1;

	/** Obergrenze fuer die Anwendung von Regeln des Typs "INDOOR" */
	private transient Integer mInsideCount = -1;

	/** Obergrenze fuer die Anwendung von Regeln des Typs "OUTDOOR" */
	private transient Integer mOutsideCount = -1;

	/** Flag fuer zufallsbasierte Komponentenauswahl */
	private transient Boolean mRandomComponentChoice = false;

	/** Flag fuer zufallsbasierte Stateauswahl */
	private transient Boolean mRandomStateChoice = false;

	/** Flag fuer die Verwendung des ConsistencyConstraints */
	private transient Boolean mUseConsistencyConstraint = true;

	/** Flag fuer die Beschraenkung auf Strahlenkomponenten */
	private transient Boolean mUseOnlyRayComponents = false;

	/** Flag fuer die Beschraenkung auf Strahlenkomponenten */
	private transient Boolean mUseOnlyVertexComponents = false;

	/**
	 * Erfolgt die Komponentenauswahl auf dem Gesamtkatalog oder nur aus einer
	 * Teilmenge bereits angefasster Komponenten
	 */
	private transient Boolean mUseRandomGrowth = false;

	/** Boost-Faktor fuer CORNER-Regeln */
	private transient Integer mCornerVoteBoost = 1;

	/** Boost-Faktor fuer REFLEX_CORNER-Regeln */
	private transient Integer mReflexCornerVoteBoost = 1;

	/** Boost-Faktor fuer Edge-Regeln */
	private transient Integer mEdgeVoteBoost = 1;

	/** Boost-Faktor fuer Outside-Regeln */
	private transient Integer mOutsideVoteBoost = 1;

	/** Boost-Faktor fuer Inside-Regeln */
	private transient Integer mInsideVoteBoost = 1;

	/** Anzahl paralleler Strahlen fuer Syntheseberechnung */
	private transient Integer mNumberOfParallelRays = 5;

	/** Abstand paralleler Strahlen in der Syntheseberechnung */
	private transient Float mParallelRayDistance = 73.71f;

	/** Abstand paralleler Strahlen in der Regelberechnung */
	private transient Float mParallelRayDistanceRuleComputation = 17.7f;

	// -------------------------------------------------------------------------------------

	@Override
	public void construct(Element configRoot) {

		assert configRoot.getName().equals("exampleBased") : "FEHLER: Ungueltiges Konfigurationsobjekt! Tagname: "
				+ configRoot.getName();

		Namespace namespace = getNamespace();
		XMLParsingHelper parsing = XMLParsingHelper.getInstance();

		// Hole zunaechst den Beispielgrundriss
		Element exampleSrc = configRoot.getChild(
				"polygonSource",
				XMLConfigurationMetadata.getInstance().getNamespaceByPrefix(
						"ct"));
		assert exampleSrc != null : "FEHLER: Keine Quellangabe fuer das Beispielpolygon!";

		mPolySourceConfiguration = new PolygonSourceConfiguration();
		mPolySourceConfiguration.construct(exampleSrc);

		// verbleibenden Parameter sind unabhaengig von der Art des
		// Eingabegrundrisses
		mFirstRuleType = RuleType.valueOf(parsing.getString(configRoot,
				"firstComponentType", namespace, mFirstRuleType.toString()));
		mCornerCount = parsing.getInteger(configRoot, "cornerCount", namespace,
				mCornerCount);
		mReflexCornerCount = parsing.getInteger(configRoot,
				"reflexCornerCount", namespace, mReflexCornerCount);
		mEdgeCount = parsing.getInteger(configRoot, "edgeCount", namespace,
				mEdgeCount);
		mInsideCount = parsing.getInteger(configRoot, "insideCount", namespace,
				mInsideCount);
		mOutsideCount = parsing.getInteger(configRoot, "outsideCount",
				namespace, mOutsideCount);
		mRandomComponentChoice = parsing.getBoolean(configRoot,
				"randomComponentChoice", namespace, mRandomComponentChoice);
		mRandomStateChoice = parsing.getBoolean(configRoot,
				"randomStateChoice", namespace, mRandomStateChoice);
		mUseConsistencyConstraint = parsing.getBoolean(configRoot,
				"consistencyConstraint", namespace, mUseConsistencyConstraint);
		mUseOnlyRayComponents = parsing.getBoolean(configRoot,
				"onlyRayComponents", namespace, mUseOnlyRayComponents);
		mUseOnlyVertexComponents = parsing.getBoolean(configRoot,
				"onlyVertexComponents", namespace, mUseOnlyVertexComponents);
		mCornerVoteBoost = parsing.getInteger(configRoot, "cornerVoteBoost",
				namespace, mCornerVoteBoost);
		mReflexCornerVoteBoost = parsing.getInteger(configRoot,
				"reflexCornerVoteBoost", namespace, mReflexCornerVoteBoost);
		mEdgeVoteBoost = parsing.getInteger(configRoot, "edgeVoteBoost",
				namespace, mEdgeVoteBoost);
		mOutsideVoteBoost = parsing.getInteger(configRoot, "outsideVoteBoost",
				namespace, mOutsideVoteBoost);
		mInsideVoteBoost = parsing.getInteger(configRoot, "insideVoteBoost",
				namespace, mInsideVoteBoost);
		mNumberOfParallelRays = parsing.getInteger(configRoot,
				"numberOfParallelRays", namespace, mNumberOfParallelRays);
		mParallelRayDistance = parsing.getFloat(configRoot,
				"parallelRayDistance", namespace, mParallelRayDistance);
		mParallelRayDistanceRuleComputation = parsing.getFloat(configRoot,
				"parallelRayDistanceRuleComputation", namespace,
				mParallelRayDistanceRuleComputation);
		mUseRandomGrowth = parsing.getBoolean(configRoot, "randomGrowth",
				namespace, mUseRandomGrowth);

		LOGGER.debug("Example-Based-Footprint: " + this);
	}

	// -------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return mProcessingMetadata.getNamespaceByPrefix("eb");
	}

	// -------------------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return " FirstRuleType:" + mFirstRuleType + ", CornerCount:"
				+ mCornerCount + ", ReflexCornerCount:" + mReflexCornerCount
				+ ", EdgeCount:" + mEdgeCount + ", IndoorCount:" + mInsideCount
				+ ", OutdoorCount:" + mOutsideCount
				+ ", RandomComponentChoice:" + mRandomComponentChoice
				+ ", RandomStateChoice:" + mRandomStateChoice
				+ ", UseConsistencyConstraint:" + mUseConsistencyConstraint
				+ ", UseOnlyRayComponents:" + mUseOnlyRayComponents
				+ ", UseOnlyVertexComponents:" + mUseOnlyVertexComponents
				+ ", UseRandomGrowth:" + mUseRandomGrowth
				+ ", CornerVoteBoost:" + mCornerVoteBoost
				+ ", ReflexCornerVoteBoost:" + mReflexCornerVoteBoost
				+ ", EdgeVoteBoost:" + mEdgeVoteBoost + ", OutsideVoteBoost:"
				+ mOutsideVoteBoost + ", InsideVoteBoost:" + mInsideVoteBoost
				+ ", NumberOfParallelRays:" + mNumberOfParallelRays
				+ ", ParallelRayDistance:" + mParallelRayDistance
				+ ", ParallelRayDistanceRuleComputation:"
				+ mParallelRayDistanceRuleComputation;
	}

	// -------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "ExampleBasedFootprint";
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mPolySourceConfiguration
	 */
	public PolygonSourceConfiguration getPolySourceConfiguration() {
		return mPolySourceConfiguration;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mFirstRuleType
	 */
	public RuleType getFirstRuleType() {
		return mFirstRuleType;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mCornerCount
	 */
	public Integer getCornerCount() {
		return mCornerCount;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mReflexCornerCount
	 */
	public Integer getReflexCornerCount() {
		return mReflexCornerCount;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mEdgeCount
	 */
	public Integer getEdgeCount() {
		return mEdgeCount;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mIndoorCount
	 */
	public Integer getInsideCount() {
		return mInsideCount;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mOutdoorCount
	 */
	public Integer getOutsideCount() {
		return mOutsideCount;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mRandomComponentChoice
	 */
	public Boolean getRandomComponentChoice() {
		return mRandomComponentChoice;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mRandomStateChoice
	 */
	public Boolean getRandomStateChoice() {
		return mRandomStateChoice;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mUseConsistencyConstraint
	 */
	public Boolean getUseConsistencyConstraint() {
		return mUseConsistencyConstraint;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mUseOnlyRayComponents
	 */
	public Boolean getUseOnlyRayComponents() {
		return mUseOnlyRayComponents;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mUseOnlyVertexComponents
	 */
	public Boolean getUseOnlyVertexComponents() {
		return mUseOnlyVertexComponents;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mUseRandomGrowth
	 */
	public Boolean getUseRandomGrowth() {
		return mUseRandomGrowth;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mCornerVoteBoost
	 */
	public Integer getCornerVoteBoost() {
		return mCornerVoteBoost;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mReflexCornerVoteBoost
	 */
	public Integer getReflexCornerVoteBoost() {
		return mReflexCornerVoteBoost;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mEdgeVoteBoost
	 */
	public Integer getEdgeVoteBoost() {
		return mEdgeVoteBoost;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mOutsideVoteBoost
	 */
	public Integer getOutsideVoteBoost() {
		return mOutsideVoteBoost;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mInsideVoteBoost
	 */
	public Integer getInsideVoteBoost() {
		return mInsideVoteBoost;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mNumberOfParallelRays
	 */
	public Integer getNumberOfParallelRays() {
		return mNumberOfParallelRays;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mParallelRayDistance
	 */
	public Float getParallelRayDistance() {
		return mParallelRayDistance;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mParallelRayDistanceRuleComputation
	 */
	public Float getParallelRayDistanceRuleComputation() {
		return mParallelRayDistanceRuleComputation;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @param examplePoly
	 * @see semantic.building.modeler.configurationservice.model.PolygonSourceConfiguration#setExamplePoly(math.MyPolygon)
	 */
	public void setExamplePoly(MyPolygon examplePoly) {
		mPolySourceConfiguration.setExamplePoly(examplePoly);
	}

	// -------------------------------------------------------------------------------------

}
