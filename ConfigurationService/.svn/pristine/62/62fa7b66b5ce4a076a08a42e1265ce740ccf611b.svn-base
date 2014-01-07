package semantic.building.modeler.configurationservice.model;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.helper.XMLParsingHelper;
import semantic.building.modeler.configurationservice.model.component.BuildingComponentsConfiguration;
import semantic.building.modeler.configurationservice.model.enums.BuildingComponentType;
import semantic.building.modeler.configurationservice.model.enums.FloorPosition;

/**
 * Klasse beschreibt Konfigurationen fuer die Konstruktion von
 * Jugendstil-Gebaeuden.
 * 
 * @author Patrick Gunia
 * 
 */

public class BuildingJugendstilConfiguration extends
		AbstractConfigurationObject implements IBuildingConfiguration {

	/** Konfigurationsobjekt mit Gebaeudedimensionen */
	private transient BuildingDimensionsConfiguration mDimensions = null;

	/** Dachkonfigurationen */
	private transient RoofConfiguration mRoof = null;

	/**
	 * Fuer jede Stockwerksposition kann ein RangeConfigurationObject definiert
	 * werden, dass den Hoehenbereich angibt
	 */
	private transient Map<FloorPosition, RangeConfigurationObject> mFloorHeights = null;

	/** Wahrscheinlichkeit, den Grundriss des Erdgeschosses zu modifizieren */
	private transient Float mModifyGroundFootprintProbability = null;

	/** Wahrscheinlichkeit, den Grundriss der Zwischengeschosse zu modifizieren */
	private transient Float mModifyIntermediateFootprintProbability = null;

	/** Wahrscheinlichkeit, den TOP-Grundriss zu modifizieren */
	private transient Float mModifyTopFootprintProbability = null;

	/**
	 * Wahrscheinlichkeit, den Grundriss des Erdgeschosses im Dachgeschoss
	 * erneut zu verwenden
	 */
	private transient Float mUseGroundFloorFootprintForTopFloorProbability = null;

	/** Objectplacementkonfiguration */
	private transient ObjectPlacementFootprintConfiguration mObjectplacement = null;

	/** Gebaeudekomponenten */
	private transient BuildingComponentsConfiguration mComponents = null;

	// -------------------------------------------------------------------------------------

	@Override
	public void construct(Element configRoot) {
		assert configRoot.getName().equals("buildingJugendstil") : "FEHLER: Ungueltiges Konfigurationsobjekt! Tagname: "
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
						mComponents = (BuildingComponentsConfiguration) instance;
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
				if (childName.equals("jugendstilProbabilities")) {
					processProbabilities(child);
				} else if (childName.equals("floorHeights")) {
					processFloorHeights(child);
				} else if (childName.equals("objectplacementConfiguration")) {
					processObjectplacementConfig(child);
				}
			}
		}
	}

	// -------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return mProcessingMetadata.getNamespaceByPrefix("buj");
	}

	// -------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "BuildingJugendstil";
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode liest die Wahrscheinlichkeiten aus der Konfigurationsdatei, die
	 * bei der Konstruktion verwendet werden
	 * 
	 * @param probabilities
	 *            XML-Element, das die verschiedenen Wahrscheinlichkeiten als
	 *            Kinder enthaelt
	 */
	private void processProbabilities(final Element probabilities) {
		XMLParsingHelper helper = XMLParsingHelper.getInstance();
		mModifyGroundFootprintProbability = helper.getFloat(probabilities,
				"modifyGroundFootprintProbability", getNamespace());
		mModifyIntermediateFootprintProbability = helper.getFloat(
				probabilities, "modifyIntermediateFootprintProbability",
				getNamespace());
		mModifyTopFootprintProbability = helper.getFloat(probabilities,
				"modifyTopFootprintProbability", getNamespace());
		mUseGroundFloorFootprintForTopFloorProbability = helper.getFloat(
				probabilities, "useGroundFloorFootprintForTopFloorProbability",
				getNamespace());
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode verarbeitet die stockwerkstypspezifischen Hoehenangaben
	 * 
	 * @param floorHeights
	 *            Element, das als Kind die jeweiligen
	 *            Stockwerkshoehenfestlegungen enthaelt
	 */
	private void processFloorHeights(final Element floorHeights) {

		mFloorHeights = new EnumMap<FloorPosition, RangeConfigurationObject>(
				FloorPosition.class);
		XMLParsingHelper helper = XMLParsingHelper.getInstance();
		List<Element> children = floorHeights.getChildren();

		// es muss immer genau drei Festlegungen geben, fuer jeden Stockwerkstyp
		// eine
		assert children.size() == 3 : "FEHLER: Ungueltige Anzahl an Stockwerkshoehendefinitionen!";

		for (Element child : children) {

			FloorPosition pos = FloorPosition.valueOf(helper.getString(child,
					"position", getNamespace()));

			RangeConfigurationObject heightRange = new RangeConfigurationObject(
					child.getChild("height", getNamespace()));

			if (pos != null && heightRange != null) {
				assert !mFloorHeights.containsKey(pos) : "FEHLER: Mehrfache Festlegung fuer Position: "
						+ pos;
				mFloorHeights.put(pos, heightRange);
			}
		}
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mDimensions
	 */
	public BuildingDimensionsConfiguration getDimensions() {
		return mDimensions;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mRoof
	 */
	public RoofConfiguration getRoof() {
		return mRoof;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mFloorHeights
	 */
	public Map<FloorPosition, RangeConfigurationObject> getFloorHeights() {
		return mFloorHeights;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mModifyGroundFootprintProbability
	 */
	public Float getModifyGroundFootprintProbability() {
		return mModifyGroundFootprintProbability;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mModifyIntermediateFootprintProbability
	 */
	public Float getModifyIntermediateFootprintProbability() {
		return mModifyIntermediateFootprintProbability;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mModifyTopFootprintProbability
	 */
	public Float getModifyTopFootprintProbability() {
		return mModifyTopFootprintProbability;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mUseGroundFloorFootprintForTopFloorProbability
	 */
	public Float getUseGroundFloorFootprintForTopFloorProbability() {
		return mUseGroundFloorFootprintForTopFloorProbability;
	}

	// -------------------------------------------------------------------------------------

	public Map<String, Class> getPotentialSubclasses() {
		Map<String, Class> result = new HashMap<String, Class>();
		result.put("dimensions", BuildingDimensionsConfiguration.class);
		result.put("roof", RoofConfiguration.class);
		result.put("buildingComponents", BuildingComponentsConfiguration.class);
		return result;
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode verarbeitet die Placementkonfiguration, die entweder direkt in
	 * der Konfigurationsdatei festgelegt ist oder ueber eine externe Ressource
	 * geladen werden muss
	 * 
	 * @param placementRoot
	 *            Wurzelelement, dass die Objectplacement-Konfiguration enthaelt
	 */
	private void processObjectplacementConfig(final Element placementRoot) {

		XMLConfigurationMetadata meta = XMLConfigurationMetadata.getInstance();
		mObjectplacement = new ObjectPlacementFootprintConfiguration();

		// entweder es handelt sich um ein <extern>-Tag, in dem die URI der
		// Ressource angeben ist oder die Definition erfolg direkt innerhalb der
		// Konfigruationsdatei
		Element extern = placementRoot.getChild("extern",
				meta.getNamespaceByPrefix("ct"));
		if (extern != null) {
			String uri = extern.getValue();
			Document placement = loadExternalRessource(uri,
					meta.getSchemaLocationByNamespacePrefix("op"));
			mObjectplacement.construct(placement.getRootElement());

		} else {
			Element objectPlacement = placementRoot.getChild("objectplacement",
					XMLConfigurationMetadata.getInstance()
							.getNamespaceByPrefix("op"));
			mObjectplacement.construct(objectPlacement);
		}
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mObjectplacement
	 */
	public ObjectPlacementFootprintConfiguration getObjectplacement() {
		return mObjectplacement;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @param type
	 * @return
	 * @see semantic.building.modeler.configurationservice.model.component.BuildingComponentsConfiguration#getComponentConfigurationByType(semantic.building.modeler.configurationservice.model.enums.BuildingComponentType)
	 */
	public AbstractConfigurationObject getComponentConfigurationByType(
			BuildingComponentType type) {
		return mComponents.getComponentConfigurationByType(type);
	}

	// -------------------------------------------------------------------------------------

	public BuildingComponentsConfiguration getBuildingComponents() {
		return mComponents;
	}

	// -------------------------------------------------------------------------------------

}
