package semantic.building.modeler.configurationservice.model;

import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.helper.XMLParsingHelper;

/**
 * Konfigurationsklasse zur Konfiguration von klassenbasierten Footprints
 * 
 * @author Patrick Gunia
 * 
 */

public class ClassBasedFootprintConfiguration extends
		AbstractConfigurationObject {

	/**
	 * Bezeichner der Java-Klasse, die fuer die Grundrisserzeugung zustaendig
	 * ist
	 */
	private transient String mClassName = null;

	// -------------------------------------------------------------------------------------

	@Override
	public void construct(Element configRoot) {
		assert configRoot.getName().equals("classBased") : "FEHLER: Ungueltiges Konfigurationsobjekt! Tagname: "
				+ configRoot.getName();
		mClassName = XMLParsingHelper.getInstance().getString(configRoot,
				"className", getNamespace());
		LOGGER.debug("ClassBasedFootprint: " + this);
	}

	// -------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return mProcessingMetadata.getNamespaceByPrefix("cb");
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mClassName
	 */
	public String getClassName() {
		return mClassName;
	}

	// -------------------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return " mClassName:" + mClassName;
	}

	// -------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "ClassBasedFootprint";
	}
	// -------------------------------------------------------------------------------------

}
