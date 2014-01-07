package semantic.building.modeler.configurationservice.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.helper.XMLParsingHelper;
import semantic.building.modeler.configurationservice.model.component.BuildingComponentsConfiguration;
import semantic.building.modeler.configurationservice.model.component.EdgeAdditionComponentConfiguration;
import semantic.building.modeler.configurationservice.model.component.FasciaComponentConfiguration;
import semantic.building.modeler.configurationservice.model.component.PillarComponentConfiguration;
import semantic.building.modeler.configurationservice.model.component.RoundStairsComponentConfiguration;
import semantic.building.modeler.configurationservice.model.enums.BuildingComponentType;

/**
 * Klasse beschreibt Konfigurationen fuer die Konstruktion von
 * Doppelantentempeln
 * 
 * @author Patrick Gunia
 * 
 */

public class BuildingDoppelantentempelConfiguration extends
		AbstractConfigurationObject implements IBuildingConfiguration {

	/** Konfigurationsobjekt mit Gebaeudedimensionen */
	private transient BuildingDimensionsConfiguration mDimensions = null;

	/** Dachkonfigurationen */
	private transient RoofConfiguration mRoof = null;

	/** Gebaeudekomponenten */
	private transient BuildingComponentsConfiguration mBuildingComponents = null;

	/** Hoehe des Architravs */
	private transient Float mArchitraveHeight = null;

	/** Hoehe der Metope */
	private transient Float mMetopeHeight = null;

	/** Hoehe des Gleison */
	private transient Float mGeisonHeight = null;

	// -------------------------------------------------------------------------------------

	public Map<String, Class> getPotentialSubclasses() {
		Map<String, Class> result = new HashMap<String, Class>();
		result.put("dimensions", BuildingDimensionsConfiguration.class);
		result.put("roof", RoofConfiguration.class);
		result.put("buildingComponentsDoppelantentempel",
				BuildingComponentsConfiguration.class);
		return result;
	}

	// -------------------------------------------------------------------------------------

	public RoofConfiguration getRoof() {
		return mRoof;
	}

	// -------------------------------------------------------------------------------------

	public BuildingDimensionsConfiguration getDimensions() {
		return mDimensions;
	}

	// -------------------------------------------------------------------------------------

	@Override
	public void construct(Element configRoot) {
		assert configRoot.getName().equals("buildingDoppelantentempel") : "FEHLER: Ungueltiges Konfigurationsobjekt! Tagname: "
				+ configRoot.getName();

		List<Element> children = configRoot.getChildren();
		for (Element child : children) {

			String childName = child.getName();
			// das aktuell verarbeitete Element wird durch eine vollstaendige
			// Subklasse beschrieben
			if (getPotentialSubclasses().get(childName) != null) {

				Class confClass = getPotentialSubclasses().get(childName);
				AbstractConfigurationObject instance = null;
				try {
					instance = (AbstractConfigurationObject) confClass
							.newInstance();
					instance.construct(child);
					if (instance.getType().equals("Roof")) {
						mRoof = (RoofConfiguration) instance;
					} else if (instance.getType().equals("BuildingDimensions")) {
						mDimensions = (BuildingDimensionsConfiguration) instance;
					} else if (instance.getType().equals("BuildingComponents")) {
						mBuildingComponents = (BuildingComponentsConfiguration) instance;
					}
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}

			}

			// es handelt sich um Elemente, fuer die keine eigenen Subklassen
			// existieren
			else {
				if (childName.equals("doppelantentempelParameters")) {
					processParameters(child);
				}
			}
		}

	}

	// -------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return mProcessingMetadata.getNamespaceByPrefix("budt");
	}

	// -------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "BuildingDoppelantentempel";
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode verarbeitet die Doppelantentempel-spezifischen Parameter
	 * 
	 * @param child
	 *            Wurzelelement der zusaetzlichen Parameter
	 */
	private void processParameters(final Element child) {

		XMLParsingHelper helper = XMLParsingHelper.getInstance();
		mArchitraveHeight = helper.getFloat(child, "architraveHeight",
				getNamespace());
		mMetopeHeight = helper.getFloat(child, "metopeHeight", getNamespace());
		mGeisonHeight = helper.getFloat(child, "geisonHeight", getNamespace());
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mArchitraveHeight
	 */
	public Float getArchitraveHeight() {
		return mArchitraveHeight;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mMetopeHeight
	 */
	public Float getMetopeHeight() {
		return mMetopeHeight;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mGleisonHeight
	 */
	public Float getGeisonHeight() {
		return mGeisonHeight;
	}

	// -------------------------------------------------------------------------------------
	/**
	 * @return the mFascia
	 */
	public FasciaComponentConfiguration getFascia() {
		return (FasciaComponentConfiguration) mBuildingComponents
				.getComponentConfigurationByType(BuildingComponentType.FASCIA);
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mPillar
	 */
	public PillarComponentConfiguration getPillar() {
		return (PillarComponentConfiguration) mBuildingComponents
				.getComponentConfigurationByType(BuildingComponentType.PILLAR);
	}

	// -------------------------------------------------------------------------------------
	/**
	 * 
	 * @return
	 */
	public RoundStairsComponentConfiguration getRoundStairs() {
		return (RoundStairsComponentConfiguration) mBuildingComponents
				.getComponentConfigurationByType(BuildingComponentType.ROUNDSTAIRS);
	}

	// -------------------------------------------------------------------------------------
	/**
	 * 
	 * @return
	 */
	public EdgeAdditionComponentConfiguration getEdgeAdditions() {
		return (EdgeAdditionComponentConfiguration) mBuildingComponents
				.getComponentConfigurationByType(BuildingComponentType.EDGEADDITION);
	}

	// -------------------------------------------------------------------------------------

	public BuildingComponentsConfiguration getBuildingComponents() {
		return mBuildingComponents;
	}
	// -------------------------------------------------------------------------------------

}
