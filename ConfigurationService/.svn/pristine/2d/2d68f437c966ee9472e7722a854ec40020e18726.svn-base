package semantic.building.modeler.configurationservice.controller;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaderJDOMFactory;
import org.jdom2.input.sax.XMLReaderSchemaFactory;

import semantic.building.modeler.configurationservice.helper.XMLParsingHelper;
import semantic.building.modeler.configurationservice.model.AbstractConfigurationObject;
import semantic.building.modeler.configurationservice.model.BuildingConfiguration;
import semantic.building.modeler.configurationservice.model.CityConfiguration;
import semantic.building.modeler.configurationservice.model.ClassBasedFootprintConfiguration;
import semantic.building.modeler.configurationservice.model.ExampleBasedFootprintConfiguration;
import semantic.building.modeler.configurationservice.model.FloorConfiguration;
import semantic.building.modeler.configurationservice.model.FloorsConfiguration;
import semantic.building.modeler.configurationservice.model.ObjectPlacementFootprintConfiguration;
import semantic.building.modeler.configurationservice.model.PolygonFootprintConfiguration;
import semantic.building.modeler.configurationservice.model.RoofConfiguration;
import semantic.building.modeler.configurationservice.model.SystemConfiguration;
import semantic.building.modeler.configurationservice.model.XMLConfigurationMetadata;

/**
 * Controller fuer die Validierung und das Parsing der eingelesenen
 * Konfigurationen. Singleton-Implementations
 * 
 * @author Patrick Gunia
 * 
 */
public class ConfigurationController {

	/** Logging-Instanz */
	protected final static Logger LOGGER = Logger
			.getLogger(ConfigurationController.class);

	/** Instanz der XML-Metadaten-Klasse */
	private XMLConfigurationMetadata mProcessingMetadata = XMLConfigurationMetadata
			.getInstance();

	/** JDOM-Document-Instanz, die das geladene XML-Dokument repraesentiert */
	private transient Document mDocument = null;

	/**
	 * Map zum Zwischenspeichern von Konfigurationsobjekten
	 */
	private transient Map<String, AbstractConfigurationObject> mConfigInstances = new HashMap<String, AbstractConfigurationObject>();

	// -------------------------------------------------------------------------------------

	/** Konstruktor */
	public ConfigurationController() {

	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode verarbeitet Konfigurationen, die das <building>-Tag als
	 * Root-Element verwenden
	 * 
	 * @param xmlConfiguration
	 *            XML-Datei mit den erforderlichen Konfigurationsparametern fuer
	 *            die Konstruktion von Gebaueden
	 * @return Building-Konfigurationsobjekt
	 */
	public SystemConfiguration processSystemConfiguration(
			final File xmlConfiguration) {

		LOGGER.debug("Processing Systemconfiguration...");

		// validieren und einlesen
		boolean isValid = isValid(xmlConfiguration,
				mProcessingMetadata.getSchemaLocationByNamespacePrefix("sy"));
		assert isValid : "FEHLER: Die uebergebene Konfigurationsdatei konnte nicht korrekt gelesen werden oder ist ungueltig.";

		// Verarbeite die Konfigurationsdatei
		SystemConfiguration systemConf = new SystemConfiguration();
		systemConf.construct(mDocument.getRootElement());

		mConfigInstances.put("system", systemConf);
		LOGGER.debug("Processing Systemconfiguration...done!");
		return systemConf;
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode verarbeitet Konfigurationen, die das <building>-Tag als
	 * Root-Element verwenden
	 * 
	 * @param xmlConfiguration
	 *            XML-Datei mit den erforderlichen Konfigurationsparametern fuer
	 *            die Konstruktion von Gebaueden
	 * @return Building-Konfigurationsobjekt
	 */
	public CityConfiguration processCityConfiguration(
			final File xmlConfiguration) {

		LOGGER.debug("Processing Cityconfiguration...");

		// validieren und einlesen
		boolean isValid = isValid(xmlConfiguration,
				mProcessingMetadata.getSchemaLocationByNamespacePrefix("ci"));
		assert isValid : "FEHLER: Die uebergebene Konfigurationsdatei konnte nicht korrekt gelesen werden oder ist ungueltig. Pfad: "
				+ xmlConfiguration.getAbsolutePath();

		// Verarbeite die Konfigurationsdatei
		CityConfiguration cityConf = new CityConfiguration();
		cityConf.construct(mDocument.getRootElement());
		LOGGER.debug("Processing Cityconfiguration...done!");

		mConfigInstances.put("city", cityConf);
		return cityConf;
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode verarbeitet Konfigurationen, die das <building>-Tag als
	 * Root-Element verwenden
	 * 
	 * @param xmlConfiguration
	 *            XML-Datei mit den erforderlichen Konfigurationsparametern fuer
	 *            die Konstruktion von Gebaueden
	 * @return Building-Konfigurationsobjekt
	 */
	public BuildingConfiguration processBuildingConfiguration(
			final File xmlConfiguration) {

		// validieren und einlesen
		boolean isValid = isValid(xmlConfiguration,
				mProcessingMetadata.getSchemaLocationByNamespacePrefix("bu"));
		assert isValid : "FEHLER: Die uebergebene Konfigurationsdatei konnte nicht korrekt gelesen werden oder ist ungueltig.";

		// Verarbeite die Konfigurationsdatei
		BuildingConfiguration buildingConf = new BuildingConfiguration();
		buildingConf.construct(mDocument.getRootElement());
		return buildingConf;
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode verarbeitet Konfigurationen, die das <floors>-Tag als
	 * Root-Element verwenden
	 * 
	 * @param xmlConfiguration
	 *            XML-Datei mit den erforderlichen Konfigurationsparametern fuer
	 *            die Konstruktion von Gebaueden
	 * @return Floors-Konfigurationsobjekt
	 * 
	 */
	public FloorsConfiguration processFloorsConfiguration(
			final File xmlConfiguration) {

		// validieren und einlesen
		boolean isValid = isValid(xmlConfiguration,
				mProcessingMetadata.getSchemaLocationByNamespacePrefix("fl"));
		assert isValid : "FEHLER: Die uebergebene Konfigurationsdatei konnte nicht korrekt gelesen werden oder ist ungueltig.";

		// Verarbeite die Konfigurationsdatei
		FloorsConfiguration floorsConf = new FloorsConfiguration();
		floorsConf.construct(mDocument.getRootElement());
		return floorsConf;
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode verarbeitet Konfigurationen, die das <building>-Tag als
	 * Root-Element verwenden
	 * 
	 * @param xmlConfiguration
	 *            XML-Datei mit den erforderlichen Konfigurationsparametern fuer
	 *            die Konstruktion von Gebaueden
	 * @return Floor-Konfigurationsobjekt
	 */
	public FloorConfiguration processFloorConfiguration(
			final File xmlConfiguration) {

		// validieren und einlesen
		boolean isValid = isValid(xmlConfiguration,
				mProcessingMetadata.getSchemaLocationByNamespacePrefix("fl"));
		assert isValid : "FEHLER: Die uebergebene Konfigurationsdatei konnte nicht korrekt gelesen werden oder ist ungueltig.";

		// Verarbeite die Konfigurationsdatei
		FloorConfiguration floorConf = new FloorConfiguration();
		floorConf.construct(mDocument.getRootElement());
		return floorConf;
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode verarbeitet Konfigurationen, die das <objectplacement>-Tag als
	 * Root-Element verwenden
	 * 
	 * @param xmlConfiguration
	 *            XML-Datei mit den erforderlichen Konfigurationsparametern fuer
	 *            die Konstruktion von Gebaueden
	 * @return Objectplacement-Konfigurationsobjekt
	 */
	public ObjectPlacementFootprintConfiguration processsObjectPlacementConfiguration(
			final File xmlConfiguration) {

		// validieren und einlesen
		boolean isValid = isValid(xmlConfiguration,
				mProcessingMetadata.getSchemaLocationByNamespacePrefix("op"));
		assert isValid : "FEHLER: Die uebergebene Konfigurationsdatei konnte nicht korrekt gelesen werden oder ist ungueltig.";

		// Verarbeite die Konfigurationsdatei
		ObjectPlacementFootprintConfiguration placementConf = new ObjectPlacementFootprintConfiguration();
		placementConf.construct(mDocument.getRootElement());
		return placementConf;
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode verarbeitet Konfigurationen, die das <exampleBased>-Tag als
	 * Root-Element verwenden
	 * 
	 * @param xmlConfiguration
	 *            XML-Datei mit den erforderlichen Konfigurationsparametern fuer
	 *            die Konstruktion von Gebaueden
	 * @return ExampleBased-Konfigurationsobjekt
	 */
	public ExampleBasedFootprintConfiguration processsExampleBasedFootprintConfiguration(
			final File xmlConfiguration) {

		// validieren und einlesen
		boolean isValid = isValid(xmlConfiguration,
				mProcessingMetadata.getSchemaLocationByNamespacePrefix("eb"));
		assert isValid : "FEHLER: Die uebergebene Konfigurationsdatei konnte nicht korrekt gelesen werden oder ist ungueltig.";

		// Verarbeite die Konfigurationsdatei
		ExampleBasedFootprintConfiguration exampleBasedConf = new ExampleBasedFootprintConfiguration();
		exampleBasedConf.construct(mDocument.getRootElement());
		return exampleBasedConf;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * Methode verarbeitet Konfigurationen, die das <polygonBased>-Tag als
	 * Root-Element verwenden
	 * 
	 * @param xmlConfiguration
	 *            XML-Datei mit den erforderlichen Konfigurationsparametern fuer
	 *            die Konstruktion von Gebaueden
	 * @return PolygonFootprint-Konfigurationsobjekt
	 */
	public PolygonFootprintConfiguration processsPolygonFootprintConfiguration(
			final File xmlConfiguration) {

		// validieren und einlesen
		boolean isValid = isValid(xmlConfiguration,
				mProcessingMetadata.getSchemaLocationByNamespacePrefix("pb"));
		assert isValid : "FEHLER: Die uebergebene Konfigurationsdatei konnte nicht korrekt gelesen werden oder ist ungueltig.";

		// Verarbeite die Konfigurationsdatei
		PolygonFootprintConfiguration polyBasedConf = new PolygonFootprintConfiguration();
		polyBasedConf.construct(mDocument.getRootElement());
		return polyBasedConf;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * Methode verarbeitet Konfigurationen, die das <exampleBased>-Tag als
	 * Root-Element verwenden
	 * 
	 * @param xmlConfiguration
	 *            XML-Datei mit den erforderlichen Konfigurationsparametern fuer
	 *            die Konstruktion von Gebaueden
	 * @return ExampleBased-Konfigurationsobjekt
	 */
	public ClassBasedFootprintConfiguration processsClassBasedFootprintConfiguration(
			final File xmlConfiguration) {

		// validieren und einlesen
		boolean isValid = isValid(xmlConfiguration,
				mProcessingMetadata.getSchemaLocationByNamespacePrefix("cb"));
		assert isValid : "FEHLER: Die uebergebene Konfigurationsdatei konnte nicht korrekt gelesen werden oder ist ungueltig.";

		// Verarbeite die Konfigurationsdatei
		ClassBasedFootprintConfiguration classBasedConf = new ClassBasedFootprintConfiguration();
		classBasedConf.construct(mDocument.getRootElement());
		return classBasedConf;
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode verarbeitet Konfigurationen, die das <roof>-Tag als Root-Element
	 * verwenden
	 * 
	 * @param xmlConfiguration
	 *            XML-Datei mit den erforderlichen Konfigurationsparametern fuer
	 *            die Konstruktion von Gebaueden
	 * @return Roof-Konfigurationsobjekt
	 */
	public RoofConfiguration processsRoofConfiguration(
			final File xmlConfiguration) {

		// validieren und einlesen
		boolean isValid = isValid(xmlConfiguration,
				mProcessingMetadata.getSchemaLocationByNamespacePrefix("ro"));
		assert isValid : "FEHLER: Die uebergebene Konfigurationsdatei konnte nicht korrekt gelesen werden oder ist ungueltig.";

		// Verarbeite die Konfigurationsdatei
		RoofConfiguration roofConf = new RoofConfiguration();
		roofConf.construct(mDocument.getRootElement());
		return roofConf;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * Methode liest die uebergebene XML-Konfiguration mittels Sax-Builder ein.
	 * Dabei wird die Datei gegen das Schema validiert. Sofern es sich nicht um
	 * eine valide Konfigurationsdatei handelt, kommt es zu Exceptions
	 * 
	 * @param xmlConfiguration
	 *            XML-Datei, die eine Prototype-Konfiguration enthaelts
	 * @return True, falls die Konfigurationsdatei valide ist, False sonst
	 */
	private boolean isValid(final File xmlConfiguration,
			final URL schemaLocation) {

		loadFromURL(schemaLocation.toString(), "tempFile.xsd");

		LOGGER.debug(schemaLocation);
		mDocument = null;
		try {
			// URL schemaLocation = new URL(mSchemaLocation);
			final SchemaFactory schemafac = SchemaFactory
					.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			final Schema schema = schemafac.newSchema(schemaLocation);
			final XMLReaderJDOMFactory factory = new XMLReaderSchemaFactory(
					schema);
			final SAXBuilder sb = new SAXBuilder(factory);
			mDocument = sb.build(xmlConfiguration);

		} catch (Exception e) {
			LOGGER.error("Laden / Validieren des XML-Dokuments fehlgeschlagen.");
			printException(e);
			return false;
		}
		return true;
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode gibt Stacktrace und Root-Cause der uebergebenen Exception in den
	 * Logger-Stream aus
	 * 
	 * @param e
	 *            Exception, ueber die Informationen auusgegeben werden sollen
	 */
	private void printException(final Exception e) {
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		e.printStackTrace(printWriter);
		final StringBuilder builder = new StringBuilder(e.getLocalizedMessage());
		builder.append(result.toString());
		LOGGER.error(builder.toString());
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mConfigInstances
	 */
	public Map<String, AbstractConfigurationObject> getConfigInstances() {
		return mConfigInstances;
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode liefert das Konfigurationsobjekt, das dem angefragten Typ
	 * entspricht, sofern ein solches vorhanden ist
	 * 
	 * @param type
	 *            Bezeichner des Konfigurationsobjekts
	 * @return Konfigurationsinstanz, falls eine Instanz des gesuchten Typs
	 *         vorhanden ist, null sonst
	 */
	public AbstractConfigurationObject getConfigByType(final String type) {
		return mConfigInstances.get(type);
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Delegate-Methode fuer den Zugriff auf die Funktionalitaet zum Laden
	 * entfernter Ressourcen
	 * 
	 * @param url
	 *            URL der entfernten Ressource
	 * @param newFilepath
	 *            Dateipfad relativ zum ressource/-Ordner des Projektes
	 * @return File-Objekt, das die geladene Datei repraesentiert
	 */
	public File loadFromURL(final String url, final String newFilepath) {
		return XMLParsingHelper.getInstance().loadFromURL(url, newFilepath);
	}
	// -------------------------------------------------------------------------------------

}
