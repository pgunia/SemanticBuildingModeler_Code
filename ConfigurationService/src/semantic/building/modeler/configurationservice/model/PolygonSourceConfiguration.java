package semantic.building.modeler.configurationservice.model;

import java.io.File;

import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.helper.XMLParsingHelper;
import semantic.building.modeler.configurationservice.model.enums.ReuseFloorEnum;
import semantic.building.modeler.math.MyPolygon;

/**
 * Klasse enthaelt Konfigurationsparameter fuer die Defintion von Polygonen.
 * Diese koennen aus verschiedenen Quellen stammen. Die erforderlichen Parameter
 * werden dabei in Instanzen dieser Klasse gespeichert und verarbeitet
 * 
 * @author Patrick Gunia
 * 
 */

public class PolygonSourceConfiguration extends AbstractConfigurationObject {

	/**
	 * Wenn der Beispielgrundriss durch ein Polygon beschrieben wird, wird
	 * dieses in dieser Variablen gespeichert
	 */
	private transient MyPolygon mExamplePoly = null;

	/**
	 * Sofern der Eingabegrundriss durch eine Klasse definiert wurde, wird deren
	 * Name in dieser Variablen gespeichert
	 */
	private transient String mExampleClassName = null;

	/**
	 * Hierin wird nach Abschluess der Verarbeitungen das Beispielpolygon
	 * gespeichert, dadurch ist eine einheitliche Verarbeitung in der
	 * Syntheseberechnung moeglich
	 */
	private transient MyPolygon mSrcPolygon = null;

	/** Typ gibt an, aus welcher Quelle der verwendete Beispielgrundriss stammt */
	private transient String mSrcType = null;

	/** Quelldatei aus der das Grundrisspolygon geladen wird */
	private transient File mFile = null;

	/** Format der Quelldatei */
	private transient String mFileFormat = null;

	/**
	 * Wenn der Eingabegrundriss zufaellig aus einer Kategorie geladen werden
	 * soll, wird der Kategoriebezeichenr hier gespeichert
	 */
	private transient String mFootprintCategory = null;

	/**
	 * Falls ein vorheriger Grundriss wiederverwendet werden soll, wird hier
	 * gespeichert, welcher Grundriss das ist
	 */
	private transient ReuseFloorEnum mReuse = null;

	// -------------------------------------------------------------------------------------

	@Override
	public void construct(Element configRoot) {
		assert configRoot.getName().equals("polygonSource") : "FEHLER: Ungueltiges Konfigurationsobjekt! Tagname: "
				+ configRoot.getName();

		// hole die Kinder => das das Dokument validiert wurde, kann es nur
		// genau ein Kindelement geben
		Element child = configRoot.getChildren().get(0);
		String childName = child.getName();

		// Beispielgrundriss stammt aus einer Java-Klasse
		if (childName.equals("exampleClass")) {

			// speichere nur den Klassennamen und setze das Flag => die
			// Instanziierung wird in den Zielklassen vorgenommen
			mExampleClassName = child.getValue();
			mSrcType = "Class";
		}
		// Beispielgrundriss liegt als Polygon definiert in einer XML-Datei vor
		else if (childName.equals("polygon")) {

			// verarbeite die Polygondefinition
			mExamplePoly = XMLParsingHelper.getInstance().getPolygon(child,
					mProcessingMetadata.getNamespaceByPrefix("ct"));
			mSrcType = "XMLPolygon";

		}
		// Beispielgrundriss wird aus einer 3D-Datei geladen => lade die Datei
		else if (childName.equals("file")) {

			// hole die Datei => <file> ist das Elternelement der benoetigten
			// Kinder in der Konfigurationsdatei
			mFile = loadFile(child);
			mSrcType = "File";

			// Dateiformat auslesen
			mFileFormat = XMLParsingHelper.getInstance().getString(
					child,
					"fileFormat",
					XMLConfigurationMetadata.getInstance()
							.getNamespaceByPrefix("ct"));
		}
		// Polygon wird aus einer Modelkategorie geladen
		else if (childName.equals("modelCategory")) {
			mFootprintCategory = child.getValue();
			mSrcType = "FootprintCategory";
		} else if (childName.equals("reuseFootprint")) {
			mReuse = ReuseFloorEnum.valueOf(child.getValue());
			mSrcType = "ReuseFloor";
		}

		LOGGER.debug("PolygonSourceConfiguration: " + this);

	}

	// -------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return XMLConfigurationMetadata.getInstance()
				.getNamespaceByPrefix("ct");
	}

	// -------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "PolygonSource";
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mExamplePoly
	 */
	public MyPolygon getExamplePoly() {
		return mExamplePoly;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mExamplePoly
	 */
	public void setExamplePoly(final MyPolygon examplePoly) {
		mExamplePoly = examplePoly;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mExampleClassName
	 */
	public String getExampleClassName() {
		return mExampleClassName;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mSrcPolygon
	 */
	public MyPolygon getSrcPolygon() {
		return mSrcPolygon;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mSrcType
	 */
	public String getSrcType() {
		return mSrcType;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mFile
	 */
	public File getFile() {
		return mFile;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mFileFormat
	 */
	public String getFileFormat() {
		return mFileFormat;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mFootprintCategory
	 */
	public String getFootprintCategory() {
		return mFootprintCategory;
	}

	// -------------------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ExamplePoly:" + mExamplePoly + ", ExampleClassName:"
				+ mExampleClassName + ", mSrcType:" + mSrcType
				+ ", mFileFormat:" + mFileFormat + ", mFootprintCategory:"
				+ mFootprintCategory;
	}

	// -------------------------------------------------------------------------------------
	/**
	 * @return the mReuse
	 */
	public ReuseFloorEnum getReuse() {
		return mReuse;
	}
	// -------------------------------------------------------------------------------------

}
