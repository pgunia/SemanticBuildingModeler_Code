package semantic.building.modeler.configurationservice.model;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaderJDOMFactory;
import org.jdom2.input.sax.XMLReaderSchemaFactory;
import org.xml.sax.SAXException;

import semantic.building.modeler.configurationservice.helper.XMLParsingHelper;

public abstract class AbstractConfigurationObject {

	/** Logging-Instanz */
	protected final static Logger LOGGER = Logger
			.getLogger(AbstractConfigurationObject.class);

	/** Instanz der Konfigurationsklasse mit Namespaces etc. */
	protected XMLConfigurationMetadata mProcessingMetadata = XMLConfigurationMetadata
			.getInstance();

	/**
	 * Methode erzeugt eine Instanz des jeweiligen Konfigurationsobjektes und
	 * liest saemtliche Konfigurationsparameter aus der XML-Struktur
	 */
	public abstract void construct(final Element configRoot);

	/** Liefert den Namespace des Konfigurationsobjekts */
	public abstract Namespace getNamespace();

	/** Art der Konfigurationsdatei */
	public abstract String getType();

	// -------------------------------------------------------------------------------------
	/**
	 * Leerer Default-Konstruktor
	 */
	public AbstractConfigurationObject() {
	}

	// -------------------------------------------------------------------------------------

	/**
	 * Methode dient dem Laden externer Ressourcen. Dies koennen sowohl URIs in
	 * Form von Webressourcen als auch lokale Dateien sein
	 * 
	 * @param ressourceLocator
	 *            Besteht entweder aus einer URI zu einer Webressource oder dem
	 *            Pfad zu einer Datei im lokalen Dateisystem
	 * @param schemaLocation
	 *            URI des XML-Schemas ueber das die zu ladende Ressource
	 *            validiert werden soll
	 */
	protected Document loadExternalRessource(final String ressourceLocator,
			final URL schemaLocationUri) {

		LOGGER.debug("Loading external Ressource '" + ressourceLocator + "'...");

		// Webressourcen beginnen mit http / https, Dateipfade mit dem prefix
		// file://
		boolean isURI = (ressourceLocator.startsWith("http://") || ressourceLocator
				.startsWith("https://"));
		Document loadedRessource = null;

		// Schema fuer Validierung laden
		SAXBuilder sb = null;
		try {

			SchemaFactory schemafac = SchemaFactory
					.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = schemafac.newSchema(schemaLocationUri);
			XMLReaderJDOMFactory factory = new XMLReaderSchemaFactory(schema);
			sb = new SAXBuilder(factory);
		} catch (SAXException e) {
			e.printStackTrace();
		}

		// wenn es sich um eine URI handelt, lade die Ressource ueber das Web
		if (isURI) {
			URL ressourceURL = null;
			try {
				ressourceURL = new URL(ressourceLocator);
				loadedRessource = sb.build(ressourceURL);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (JDOMException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		} else {
			// entferne das file-Prefix vom Pdad
			final String path = ressourceLocator.replace("file://", "");

			// und lade die Ressource
			File localRessource = new File(path);
			assert localRessource.exists() : "FEHLER: Die referenzierte Ressource '"
					+ path + "' existiert nicht.";
			try {
				loadedRessource = sb.build(localRessource);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		LOGGER.debug("Loading external Ressource...done");
		return loadedRessource;

	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode laedt den Root-Knoten einer externen Ressource, die innerhalb
	 * eines Konfigurationsobjekts verwendet werden soll
	 * 
	 * @param curRoot
	 *            Root-Knoten, der u.U. ein extern-Attribut enthaelt, das den
	 *            Speicherort der externen Ressource spezifiziert
	 * @param schemaLocationPrefix
	 *            Prefix des Schema-Dokuments, mit dem die externe Ressource
	 *            validiert werden soll
	 * @return Rootknoten der geladenen Ressource, falls eine solche vorhanden
	 *         ist, null sonst
	 */
	protected Element getExternalRootElement(final Element curRoot,
			final String schemaLocationPrefix) {

		Element externalElement = curRoot.getChild("extern",
				mProcessingMetadata.getNamespaceByPrefix("ct"));
		if (externalElement != null) {
			String ressourceLocator = externalElement.getValue();
			Document externalDoc = loadExternalRessource(
					ressourceLocator,
					mProcessingMetadata
							.getSchemaLocationByNamespacePrefix(schemaLocationPrefix));
			Element externalRoot = externalDoc.getRootElement();

			// das Tag der externen und lokalen Ressource muessen identisch sein
			assert externalRoot.getName().equals(curRoot.getName()) : "FEHLER: Externe Ressource ist ungueltig: Root: "
					+ externalRoot.getName()
					+ " Erwartet: "
					+ curRoot.getName();
			return externalRoot;
		} else {
			return null;
		}
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode dient dem Laden von Dateien (bsw. 3D-Modelle) von lokalen oder
	 * externen Ressourcen
	 * 
	 * @param fileDescriptor
	 *            ModelFile-Rootknoten
	 * @return Geladene Datei
	 */
	protected File loadFile(final Element fileDescriptor) {

		// Metainformationen auslesen
		String modelUri = XMLParsingHelper.getInstance().getString(
				fileDescriptor, "extern",
				mProcessingMetadata.getNamespaceByPrefix("ct"));
		File result = null;
		// lade die Datei, teste, ob es sich um eine lokale oder entfernte
		// Ressource handelt
		if (modelUri.startsWith("file://")) {
			// ueber das normale Dateisystem laden
			String noPrefix = modelUri.replace("file://", "");
			result = new File(noPrefix);
			assert result.exists() : "FEHLER: Datei '" + noPrefix
					+ "' existiert nicht.";
		} else {
			// sonst muss die Datei von einer externen Quelle geladen und auf
			// dem lokalen Dateisystem gespeichert werden
			int filenamePos = modelUri.lastIndexOf("/");
			String filename = modelUri.substring(filenamePos + 1);
			String newFilename = "Models/" + filename;

			// lade die Datei und speichere sie lokal fuer die weitere
			// Verarbeitung
			result = XMLParsingHelper.getInstance().loadFromURL(modelUri,
					newFilename);
		}
		return result;
	}
	// -------------------------------------------------------------------------------------

}
