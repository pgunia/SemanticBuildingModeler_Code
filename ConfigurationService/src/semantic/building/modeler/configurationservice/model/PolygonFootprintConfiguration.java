package semantic.building.modeler.configurationservice.model;

import java.io.File;

import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.helper.XMLParsingHelper;

/**
 * Klasse modelliert Grundrisse, die aus 3d-Modelldateien als Polygonzug geladen
 * und direkt weiterverarbeitet werden
 * 
 * @author Patrick Gunia
 * 
 */

public class PolygonFootprintConfiguration extends AbstractConfigurationObject {

	/** Modelldatei */
	private File mModelFile = null;

	/** Dateityp der geladenen Datei bsw. OBJ */
	private transient String mModelFormat = null;

	// -------------------------------------------------------------------------------------

	@Override
	public void construct(Element configRoot) {

		assert configRoot.getName().equals("polygonBased") : "FEHLER: Ungueltiges Konfigurationsobjekt! Tagname: "
				+ configRoot.getName();

		// Metainformationen auslesen
		Element modelSrc = configRoot.getChild("ModelSource", getNamespace());

		// Dateiformat auslesen
		mModelFormat = XMLParsingHelper.getInstance().getString(modelSrc,
				"fileFormat", mProcessingMetadata.getNamespaceByPrefix("ct"));

		// Datei laden
		mModelFile = loadFile(modelSrc);

	}

	// -------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return mProcessingMetadata.getNamespaceByPrefix("pb");
	}

	// -------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "PolygonFootprint";
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mModelFile
	 */
	public File getModelFile() {
		return mModelFile;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mModelFormat
	 */
	public String getModelFormat() {
		return mModelFormat;
	}
	// -------------------------------------------------------------------------------------

}
