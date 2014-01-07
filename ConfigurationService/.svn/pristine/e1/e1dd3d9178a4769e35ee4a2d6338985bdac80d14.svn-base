package semantic.building.modeler.configurationservice.model;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.helper.XMLParsingHelper;
import semantic.building.modeler.configurationservice.model.enums.BuildingType;

/**
 * 
 * Instanzen dieser Klasse modellieren Bauplaene fuer die Errichtung von
 * Gebaeuden
 * 
 * @author Patrick Gunia
 * 
 */

public class BuildingDescriptor extends AbstractConfigurationObject {

	/** Konfigurationsobjekt fuer das eigentliche Gebaeude */
	private transient AbstractConfigurationObject mBuilding = null;

	/** Wie viele Gebaeude des angegebenen Typs sollen gebaut werden? */
	private transient Integer mInstanceCount = null;

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mBuilding
	 */
	public AbstractConfigurationObject getBuilding() {
		return mBuilding;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mInstanceCount
	 */
	public Integer getInstanceCount() {
		return mInstanceCount;
	}

	// -------------------------------------------------------------------------------------

	@Override
	public void construct(Element configRoot) {

		assert configRoot.getName().equals("buildingDescriptor") : "FEHLER: Ungueltiges Konfigurationsobjekt! Tagname: "
				+ configRoot.getName();

		final XMLParsingHelper helper = XMLParsingHelper.getInstance();
		mInstanceCount = helper.getInteger(configRoot, "count", getNamespace());

		LOGGER.debug("Anzahl Gebaeude: " + mInstanceCount);

		Element buildingRoot = null;

		// pruefe zunaechst, ob es sich um eine extern definierte Ressource
		// handelt
		Element external = configRoot.getChild("extern",
				this.mProcessingMetadata.getNamespaceByPrefix("ct"));
		if (external != null) {

			// ermittle anhand des Gebaeudetyps das Schema, das zur Validierung
			// der externen Ressource verwendet wird
			final BuildingType curType = BuildingType.valueOf(helper.getString(
					configRoot, "buildingType",
					this.mProcessingMetadata.getNamespaceByPrefix("ci")));
			final String externalDocumenLocation = external.getValue();

			LOGGER.debug("Lade externes Dokument: " + externalDocumenLocation);
			final Document externalDocument = loadExternalRessource(
					externalDocumenLocation,
					getNamespaceURIByBuildingType(curType));
			buildingRoot = externalDocument.getRootElement();

		} else {

			final List<Element> descriptorChildren = configRoot.getChildren();
			assert descriptorChildren.size() == 2 : "FEHLER: Es existieren "
					+ descriptorChildren.size() + " Gebaeudedefinitionen!";

			// erstes Kind ist immer die Anzahl der zu berechnenenden Instanzen
			// des jeweiligen Typs
			// zweites Kind die Gebaeudekomponente
			buildingRoot = descriptorChildren.get(1);
		}

		LOGGER.debug("Verarbeite Dokument mit Wurzel: " + buildingRoot);

		final Map<String, Class> potentialSubclasses = getPotentialSubclasses();

		// nachdem der Wurzelknoten der Konfiguration geladen wurde, erzeuge
		// eine Konfigurationsinstanz
		if (!potentialSubclasses.containsKey(buildingRoot.getName())) {
			LOGGER.error("FEHLER: Ungueltiger Gebaeudetyp: "
					+ buildingRoot.getName());
		} else {

			final Class curClass = potentialSubclasses.get(buildingRoot
					.getName());
			try {
				mBuilding = (AbstractConfigurationObject) curClass
						.newInstance();
				mBuilding.construct(buildingRoot);
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	// -------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return this.mProcessingMetadata.getNamespaceByPrefix("ci");
	}

	// -------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "BuildingDescriptor";
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode liefert anhand des uebergebenen Gebaeudetyps die URL, an der das
	 * Schema fuer die Validierung liegt
	 * 
	 * @param buildingType
	 *            Gebaeudetyp, fuer das das Validierungsschema gesucht wird
	 * @return URL, die die Schema-Location beschreibt
	 */
	protected URL getNamespaceURIByBuildingType(final BuildingType buildingType) {
		switch (buildingType) {
		case ArbitraryBuilding:
			return this.mProcessingMetadata
					.getSchemaLocationByNamespacePrefix("bu");
		case JugendstilBuilding:
			return this.mProcessingMetadata
					.getSchemaLocationByNamespacePrefix("buj");
		case Doppelantentempel:
			return this.mProcessingMetadata
					.getSchemaLocationByNamespacePrefix("budt");
		default:
			LOGGER.warn("Undefinierter Gebaeutetyp: " + buildingType);
			return null;
		}
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode liefert saemtliche potentiellen Subobjekte fuer
	 * Konfigurationsobjekte des Typs "buildingDescriptor"
	 * 
	 * @return Mapping zwischen dem Elementnamen und dem zugehoerigen
	 *         Klassenobjekt
	 */
	protected Map<String, Class> getPotentialSubclasses() {
		Map<String, Class> result = new HashMap<String, Class>();
		result.put("building", BuildingConfiguration.class);
		result.put("buildingJugendstil", BuildingJugendstilConfiguration.class);
		result.put("buildingDoppelantentempel",
				BuildingDoppelantentempelConfiguration.class);
		return result;
	}
	// -------------------------------------------------------------------------------------

}
