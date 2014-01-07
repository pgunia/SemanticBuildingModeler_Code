package semantic.building.modeler.configurationservice.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.model.component.BuildingComponentsConfiguration;
import semantic.building.modeler.configurationservice.model.enums.BuildingComponentType;
import semantic.building.modeler.configurationservice.model.enums.FloorPosition;

/**
 * 
 * Klasse dient der Verarbeitung und Verwaltung von
 * Gebaeudekonfigurationsdateien
 * 
 * @author Patrick Gunia
 * 
 */

public class BuildingConfiguration extends AbstractConfigurationObject
		implements IBuildingConfiguration {

	/** Konfigurationsobjekt mit Gebaeudedimensionen */
	private transient BuildingDimensionsConfiguration mDimensions = null;

	/** Liste mit saemtlichen vorhandenen Stockwerkkonfigurationen */
	private transient Map<FloorPosition, FloorConfiguration> mFloors = null;

	/** Dachkonfigurationen */
	private transient RoofConfiguration mRoof = null;

	/** Gebaeudekomponenten */
	private transient BuildingComponentsConfiguration mComponents = null;

	// -------------------------------------------------------------------------------------

	@Override
	public void construct(final Element configRoot) {

		assert configRoot.getName().equals("building") : "FEHLER: Ungueltiges Konfigurationsobjekt! Tagname: "
				+ configRoot.getName();

		// Mapstruktur mit saemtlichen unterhalb des building-Tags vorhandenen
		// Elementen und den Konfigurationsklassen, die fuer deren Verarbeitung
		// zustaendig sind
		final Map<String, Class> potentialSubclasses = getPotentialSubclasses();

		// durchlaufe alle Kindelemente des Wurzelelements und baue fuer diese
		// die jeweiligen Instanzen der Konfigurationsklassen auf
		final List<Element> children = configRoot.getChildren();
		final Iterator<Element> childIter = children.iterator();
		Element curElement = null;
		while (childIter.hasNext()) {
			curElement = childIter.next();
			final Class curConfClass = potentialSubclasses.get(curElement
					.getName());

			if (curConfClass != null) {
				AbstractConfigurationObject confClass;
				try {
					confClass = (AbstractConfigurationObject) curConfClass
							.newInstance();
					confClass.construct(curElement);
					if (confClass.getType().equals("BuildingDimensions")) {
						mDimensions = (BuildingDimensionsConfiguration) confClass;
					} else if (confClass.getType().equals("Roof")) {
						mRoof = (RoofConfiguration) confClass;
					} else if (confClass.getType().equals("Floors")) {
						FloorsConfiguration floors = (FloorsConfiguration) confClass;
						mFloors = floors.getFloors();
					} else if (confClass.getType().equals("BuildingComponents")) {
						mComponents = (BuildingComponentsConfiguration) confClass;
					} else {
						LOGGER.error("Unbekanntes Kindelement: "
								+ confClass.getType());
					}
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		LOGGER.debug("Gebaeudekonfiguration wurde erfolgreich eingelesen.");

	}

	// -------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return mProcessingMetadata.getNamespaceByPrefix("bu");
	}

	// -------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "Building";
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mFloors
	 */
	public Map<FloorPosition, FloorConfiguration> getFloorsConfig() {
		return mFloors;
	}

	// -------------------------------------------------------------------------------------

	public Map<String, Class> getPotentialSubclasses() {
		Map<String, Class> result = new HashMap<String, Class>(3);
		result.put("dimensions", BuildingDimensionsConfiguration.class);
		result.put("roof", RoofConfiguration.class);
		result.put("floors", FloorsConfiguration.class);
		result.put("buildingComponents", BuildingComponentsConfiguration.class);
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

	/**
	 * @param type
	 * @return
	 * @see semantic.building.modeler.configurationservice.model.component.BuildingComponentsConfiguration#getComponentConfigurationByType(semantic.building.modeler.configurationservice.model.enums.BuildingComponentType)
	 */
	public AbstractConfigurationObject getComponentConfigurationByType(
			final BuildingComponentType type) {
		return mComponents.getComponentConfigurationByType(type);
	}

	// -------------------------------------------------------------------------------------

	public BuildingComponentsConfiguration getBuildingComponents() {
		return mComponents;
	}

	// -------------------------------------------------------------------------------------

}
