package semantic.building.modeler.configurationservice.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.model.enums.FloorPosition;

/**
 * Container-Konfigurationsklasse fuer saemtliche Floor-Unterkonfigurationen
 * 
 * @author Patrick
 * 
 */

public class FloorsConfiguration extends AbstractConfigurationObject {

	/**
	 * Mapstruktur mit saemtlichen geladenen Floor-Konfigurationen indiziert
	 * ueber die jeweilige Floor-Position
	 */
	private transient Map<FloorPosition, FloorConfiguration> mFloors = null;

	// -------------------------------------------------------------------------------------
	@Override
	public void construct(Element configRoot) {

		// durchlaufe die Liste saemtlicher Floor-Unterelemente und erzeuge fuer
		// jedes Element ein Floor-Konfigurationsobjekt
		List<Element> floors = configRoot.getChildren();

		// verwende eine EnumMap, sortiert basierend auf den Stockwerkpositionen
		mFloors = new EnumMap<FloorPosition, FloorConfiguration>(
				FloorPosition.class);
		Element curFloorElement = null;

		for (Element floor : floors) {

			// teste, ob die verarbeitete Floor-Definition aus einer externen
			// Datei geladen wird
			Element externalRoot = getExternalRootElement(floor, "fl");
			if (externalRoot != null) {
				floor = externalRoot;
			}

			final FloorConfiguration curFloor = new FloorConfiguration();
			curFloor.construct(floor);

			assert !mFloors.containsKey(curFloor.getPosition()) : "FEHLER: Es existiert bereits eine Konfiguration fuer die Position "
					+ curFloor.getPosition();
			mFloors.put(curFloor.getPosition(), curFloor);
			LOGGER.debug("Adding Floor-Definition: " + curFloor.getPosition());
		}

	}

	// -------------------------------------------------------------------------------------
	@Override
	public Namespace getNamespace() {
		return mProcessingMetadata.getNamespaceByPrefix("bu");
	}

	// -------------------------------------------------------------------------------------
	@Override
	public String getType() {
		return "Floors";
	}

	// -------------------------------------------------------------------------------------
	/**
	 * @return the mFloors
	 */
	public Map<FloorPosition, FloorConfiguration> getFloors() {
		return mFloors;
	}

	// -------------------------------------------------------------------------------------

}
