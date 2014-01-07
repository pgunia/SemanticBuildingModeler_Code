package semantic.building.modeler.configurationservice.model.component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.model.AbstractConfigurationObject;
import semantic.building.modeler.configurationservice.model.enums.BuildingComponentType;

/**
 * Wrapper-Klasse fuer saemtliche Gebaeudekomponenten innerhalb einer
 * Gebaeudedefinition
 * 
 * @author Patrick Gunia
 * 
 */

public class BuildingComponentsConfiguration extends
		AbstractConfigurationObject {

	/**
	 * Map enthaelt saemtliche Gebauedekomponentenkonfigurationen, die aus den
	 * jeweiligen XML-Dateien geladen wurden
	 */
	private transient Map<BuildingComponentType, AbstractConfigurationObject> mComponents = null;

	@Override
	public void construct(Element configRoot) {

		assert configRoot.getName().equals("buildingComponents")
				|| configRoot.getName().equals(
						"buildingComponentsDoppelantentempel") : "FEHLER: Ungueltiges Konfigurationsobjekt! Tagname: "
				+ configRoot.getName();

		List<Element> concreteComponents = null;

		// wenn es sich um Standard-BuildingComponents handelt, sind die
		// Komponentendefinition durch das <comp>-Tag gewrappt
		if (configRoot.getName().equals("buildingComponents")) {

			// hole saemtliche gespeicherten Komponentenobjekte
			final List<Element> components = configRoot.getChildren(
					"component",
					this.mProcessingMetadata.getNamespaceByPrefix("co"));

			// und fuege die Kindelemente zu einer Liste hinzu, die
			// anschliessend einheitlich verarbeitet werden kann
			concreteComponents = new ArrayList<Element>(components.size());
			for (Element curElement : components) {
				concreteComponents.addAll(curElement.getChildren());
			}
			// sonst stehen die Komponentendefinitionen direkt unterhalb des
			// Wurzelknotens
		} else {
			concreteComponents = configRoot.getChildren();
		}

		mComponents = new EnumMap<BuildingComponentType, AbstractConfigurationObject>(
				BuildingComponentType.class);

		String elementName = null, elementNameUC = null;
		Class curClass = null;
		AbstractConfigurationObject curObject = null;

		final Map<String, Class> potentialSubclasses = getPotentialSubclasses();
		for (Element curElement : concreteComponents) {
			elementName = curElement.getName();
			if (potentialSubclasses.containsKey(elementName)) {
				curClass = potentialSubclasses.get(elementName);
				try {
					curObject = (AbstractConfigurationObject) curClass
							.newInstance();
					curObject.construct(curElement);
					elementNameUC = elementName.toUpperCase();
					LOGGER.info("Added Component: " + elementNameUC);
					mComponents.put(
							BuildingComponentType.valueOf(elementNameUC),
							curObject);

				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// -------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return this.mProcessingMetadata.getNamespaceByPrefix("bu");
	}

	// -------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "BuildingComponents";
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode liefert die Zuordnung zwischen XML-Tag-Namen und
	 * Konfigurationsklassen, die fuer die Verarbeitung dieser Tags zustaendig
	 * sind
	 * 
	 * @return Map-Struktur mit saetmlichen Klassennamen und Klassenobjekten
	 */
	private Map<String, Class> getPotentialSubclasses() {

		Map<String, Class> classes = new HashMap<String, Class>(6);
		classes.put("door", DoorComponentConfiguration.class);
		classes.put("window", WindowComponentConfiguration.class);
		classes.put("fascia", FasciaComponentConfiguration.class);
		classes.put("moulding", MouldingComponentConfiguration.class);
		classes.put("windowLedge", WindowLedgeComponentConfiguration.class);
		classes.put("pillar", PillarComponentConfiguration.class);
		classes.put("roundstairs", RoundStairsComponentConfiguration.class);
		classes.put("edgeaddition", EdgeAdditionComponentConfiguration.class);
		return classes;

	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methoder liefert das Konfigurationsobjekt, das dem Uebergabetyp
	 * entspricht, null sonst
	 * 
	 * @param type
	 *            Typ des angeforderten Konfigurationsobjekts
	 * @return Konfigurationsobjekt fuer den angefordernten Typ, null sonst
	 */
	public AbstractConfigurationObject getComponentConfigurationByType(
			final BuildingComponentType type) {

		if (!mComponents.containsKey(type)) {
			LOGGER.warn("Fuer den Komponententyp '" + type
					+ "' wurde keine Konfiguration geladen!");
			return null;
		} else {
			return mComponents.get(type);
		}

	}

	// -------------------------------------------------------------------------------------

}
