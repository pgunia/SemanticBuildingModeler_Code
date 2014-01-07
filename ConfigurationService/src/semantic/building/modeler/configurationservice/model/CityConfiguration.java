package semantic.building.modeler.configurationservice.model;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;
import org.jdom2.Namespace;

/**
 * Klasse verwaltet saemtliche Informationen, die zur Konstruktion einer Stadt
 * basierend auf einer XML-Konfiguration benoetigt werden
 * 
 * @author Patrick Gunia
 * 
 */

public class CityConfiguration extends AbstractConfigurationObject {

	/** Liste enthaelt saemtliche eingeleseenen Gebauedekonfigurationen */
	private List<BuildingDescriptor> mBuildingDescriptors = null;

	// -------------------------------------------------------------------------------------

	@Override
	public void construct(Element configRoot) {
		assert configRoot.getName().equals("city") : "FEHLER: Ungueltiges Konfigurationsobjekt! Tagname: "
				+ configRoot.getName();

		final List<Element> buildingDescriptors = configRoot.getChildren(
				"buildingDescriptor", getNamespace());
		mBuildingDescriptors = new ArrayList<BuildingDescriptor>(
				buildingDescriptors.size());

		BuildingDescriptor curDescriptor = null;

		// durchlaufe alle Descritpor-Instanzen aus dem XML-Dokument und erzeuge
		// fuer jede Instanz ein Domain-Objekt
		for (Element descriptor : buildingDescriptors) {
			curDescriptor = new BuildingDescriptor();
			curDescriptor.construct(descriptor);
			mBuildingDescriptors.add(curDescriptor);
		}
	}

	// -------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return mProcessingMetadata.getNamespaceByPrefix("ci");
	}

	// -------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "City";
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mBuildingDescriptors
	 */
	public List<BuildingDescriptor> getBuildingDescriptors() {
		return mBuildingDescriptors;
	}

	// -------------------------------------------------------------------------------------
}
