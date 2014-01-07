package semantic.building.modeler.configurationservice.model;

import java.util.Map;

import semantic.building.modeler.configurationservice.model.component.BuildingComponentsConfiguration;

/**
 * Interface fuer saemtliche Konfigurationsobjekte, die Gebaeude beschreiben
 * 
 * @author Patrick Gunia
 * 
 */

public interface IBuildingConfiguration {

	/**
	 * Methode liefert eine Map, die den XML-Element-Namen Konfigurationsklassen
	 * zuordnet, die fuer deren Verarbeitung zustaendig sind
	 */
	public Map<String, Class> getPotentialSubclasses();

	/** Dachkonfiguration */
	public RoofConfiguration getRoof();

	/** Gebaeudeausdehneungen */
	public BuildingDimensionsConfiguration getDimensions();

	/** Applizierte Gebauedekomponenten */
	public BuildingComponentsConfiguration getBuildingComponents();
}
