package semantic.building.modeler.configurationservice.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jdom2.Namespace;

/**
 * Klasse kapselt die fuer die XML-Verarbeitung erforderlichen Informationen und
 * nicht ueber Domain-Klassen verteilt werden muessen
 * 
 * @author Patrick Gunia
 * 
 */

public class XMLConfigurationMetadata {

	/** Map mit allen vom System verwalteten Namespaces */
	private Map<String, Namespace> mNamespaces = null;

	/** Map, die die Namespace-Prefixes den Schema-Locations zuordnet */
	private Map<String, URL> mSchemaLocations = null;

	/** Singleton-Getter */
	private static XMLConfigurationMetadata mInstance = null;

	/** Verwendetes Namespace-Prefix */
	protected final static String mNamespacePrefix = "smcb_";

	// -------------------------------------------------------------------------------------
	/**
	 * Singleton-Getter
	 * 
	 * @return Only instance of XMLConfigurationMetadata-Object
	 */
	public static XMLConfigurationMetadata getInstance() {
		if (mInstance == null)
			mInstance = new XMLConfigurationMetadata();
		return mInstance;
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Leerer Default-Konstruktor
	 */
	private XMLConfigurationMetadata() {
		init();
	}

	// -------------------------------------------------------------------------------------

	/**
	 * Methode liefert ein Namespace-Objekt, das zu dem uebergebenen Prefix
	 * gehoert
	 * 
	 * @param prefix
	 *            Prefix des Namespaces
	 * @return Namespace-Instanz
	 */
	public Namespace getNamespaceByPrefix(final String prefix) {
		if (mNamespaces == null)
			init();
		final String completePrefix = mNamespacePrefix + prefix;
		assert mNamespaces.containsKey(completePrefix) : "FEHLER: Fuer das uebergebene Prefix '"
				+ completePrefix + "' existiert kein Namespace-Mapping.";

		return mNamespaces.get(completePrefix);
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode liefert die Schema-Location-URLs anhand des Namespace-Prefix
	 * 
	 * @param prefix
	 *            Namespaceprefix, ueber das auf die Schema-Locations
	 *            zugegriffen wird
	 * @return URL-Objekt, das die Schema-Location beschreibt
	 */
	public URL getSchemaLocationByNamespacePrefix(final String prefix) {
		if (mSchemaLocations == null)
			init();
		final String completePrefix = mNamespacePrefix + prefix;
		assert mSchemaLocations.containsKey(completePrefix) : "FEHLER: Fuer das uebergebene Prefix '"
				+ completePrefix + "' existiert kein Schema-Location-Eintrag!";
		return mSchemaLocations.get(completePrefix);
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode initialisiert die Namespace-Strukturen
	 */
	private void init() {

		int numberOfElements = 14;
		mNamespaces = new HashMap<String, Namespace>(numberOfElements);
		mNamespaces
				.put(mNamespacePrefix + "sy",
						Namespace
								.getNamespace(mNamespacePrefix + "sy",
										"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/System"));
		mNamespaces
				.put(mNamespacePrefix + "ct",
						Namespace
								.getNamespace(mNamespacePrefix + "ct",
										"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/CommonTypes"));
		mNamespaces
				.put(mNamespacePrefix + "fl",
						Namespace
								.getNamespace(mNamespacePrefix + "fl",
										"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/Floor"));
		mNamespaces
				.put(mNamespacePrefix + "bu",
						Namespace
								.getNamespace(mNamespacePrefix + "bu",
										"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/Building"));
		mNamespaces
				.put(mNamespacePrefix + "fp",
						Namespace
								.getNamespace(mNamespacePrefix + "fp",
										"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/Footprint"));
		mNamespaces
				.put(mNamespacePrefix + "ro",
						Namespace
								.getNamespace(mNamespacePrefix + "ro",
										"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/Roof"));
		mNamespaces
				.put(mNamespacePrefix + "co",
						Namespace
								.getNamespace(mNamespacePrefix + "fl",
										"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/Component"));
		mNamespaces
				.put(mNamespacePrefix + "ci",
						Namespace
								.getNamespace(mNamespacePrefix + "ci",
										"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/City"));
		mNamespaces
				.put(mNamespacePrefix + "op",
						Namespace
								.getNamespace(
										mNamespacePrefix + "op",
										"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/Objectplacement"));
		mNamespaces
				.put(mNamespacePrefix + "eb",
						Namespace
								.getNamespace(
										mNamespacePrefix + "eb",
										"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/ExampleBasedSynthesis"));
		mNamespaces
				.put(mNamespacePrefix + "cb",
						Namespace
								.getNamespace(
										mNamespacePrefix + "cb",
										"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/ClassBasedFootprint"));
		mNamespaces
				.put(mNamespacePrefix + "pb",
						Namespace
								.getNamespace(
										mNamespacePrefix + "pb",
										"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/PolygonFootprint"));
		mNamespaces
				.put(mNamespacePrefix + "buj",
						Namespace
								.getNamespace(
										mNamespacePrefix + "buj",
										"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/BuildingJugendstil"));
		mNamespaces
				.put(mNamespacePrefix + "budt",
						Namespace
								.getNamespace(
										mNamespacePrefix + "budt",
										"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/BuildingDoppelantentempel"));
		mSchemaLocations = new HashMap<String, URL>(numberOfElements);
		try {
			mSchemaLocations
					.put(mNamespacePrefix + "sy",
							new URL(
									"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/System/SystemSchema.xsd"));
			mSchemaLocations
					.put(mNamespacePrefix + "ct",
							new URL(
									"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/CommonTypes/CommonTypes.xsd"));
			mSchemaLocations
					.put(mNamespacePrefix + "fl",
							new URL(
									"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/Floor/FloorSchema.xsd"));
			mSchemaLocations
					.put(mNamespacePrefix + "bu",
							new URL(
									"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/Building/BuildingSchema.xsd"));
			mSchemaLocations
					.put(mNamespacePrefix + "fp",
							new URL(
									"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/Footprint/FootprintSchema.xsd"));
			mSchemaLocations
					.put(mNamespacePrefix + "ro",
							new URL(
									"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/Roof/RoofSchema.xsd"));
			mSchemaLocations
					.put(mNamespacePrefix + "co",
							new URL(
									"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/Component/ComponentSchema.xsd"));
			mSchemaLocations
					.put(mNamespacePrefix + "ci",
							new URL(
									"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/City/CitySchema.xsd"));
			mSchemaLocations
					.put(mNamespacePrefix + "op",
							new URL(
									"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/Objectplacement/ObjectplacementSchema.xsd"));
			mSchemaLocations
					.put(mNamespacePrefix + "eb",
							new URL(
									"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/ExampleBasedSynthesis/ExampleBasedSynthesisSchema.xsd"));
			mSchemaLocations
					.put(mNamespacePrefix + "cb",
							new URL(
									"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/ClassBasedFootprint/ClassBasedFootprintSchema.xsd"));
			mSchemaLocations
					.put(mNamespacePrefix + "pb",
							new URL(
									"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/PolygonFootprint/PolygonFootprintSchema.xsd"));
			mSchemaLocations
					.put(mNamespacePrefix + "buj",
							new URL(
									"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/BuildingJugendstil/BuildingJugendstilSchema.xsd"));
			mSchemaLocations
					.put(mNamespacePrefix + "budt",
							new URL(
									"https://raw.github.com/pgunia/SemanticCityBuilder/master/Schema/BuildingDoppelantentempel/BuildingDoppelantentempelSchema.xsd"));

		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

	}
	// -------------------------------------------------------------------------------------

}
