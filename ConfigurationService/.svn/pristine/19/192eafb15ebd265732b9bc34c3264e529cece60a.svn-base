package semantic.building.modeler.configurationservice.model;

import java.util.HashSet;
import java.util.Set;

import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.helper.XMLParsingHelper;

/**
 * Konfigurationsklasse dient der Verarbeitung von Konfigurationsparametern, die
 * fuer das Funktionieren der Software benoetigt werden
 * 
 * @author Patrick Gunia
 * 
 */

public class SystemConfiguration extends AbstractConfigurationObject {

	/** Breite des Anwendungsfensters */
	private transient Integer mWindowWidth = null;

	/** Hoehe des Anwendungsfensters */
	private transient Integer mWindowHeight = null;

	/** Pfad zum Verzeichnis, das die Stadtkonfiguration enthaelt */
	private transient String mCityConfigurationFolder = null;

	/** Konfigurationsdatei, die die Stadtkonfiguration enthaelt */
	private transient String mCityConfigurationFile = null;

	/** Liste mit saemtlichen unterstuetzen Textur-Kategorien */
	private transient Set<String> mSupportedTextureCategories = null;

	/** Texturverzeichnispfad */
	private transient String mTextureFolder = null;

	/** Maximale Texturaufloesung in Pixeln */
	private transient Integer mMaxTextureSize = null;

	/** Maximaler Texturreskalierungsfaktor */
	private transient Integer mMaxTextureScaleFactor = null;

	/** Liste mit saemtlichen unterstuetzen Modelkategorien */
	private transient Set<String> mSuppotedModelCategories = null;

	/** Lokaler Verzeichnispfad */
	private transient String mModelPath = null;

	/** Pfad, in den die Modell-Datei geschrieben wird */
	private transient String mExportModelFolder = null;

	/** Dateiname der Exportdatei */
	private transient String mExportFileName = null;

	// -------------------------------------------------------------------------------------

	@Override
	public void construct(Element configRoot) {

		assert configRoot.getName().equals("system") : "FEHLER: Ungueltiges Konfigurationsobjekt! Tagname: "
				+ configRoot.getName();

		final XMLParsingHelper helper = XMLParsingHelper.getInstance();

		final Element applicationWindow = configRoot.getChild(
				"applicationWindow", getNamespace());
		mWindowWidth = helper.getInteger(applicationWindow, "width",
				getNamespace());
		mWindowHeight = helper.getInteger(applicationWindow, "height",
				getNamespace());

		final Element cityConfiguration = configRoot.getChild(
				"cityConfiguration", getNamespace());
		mCityConfigurationFolder = helper.getString(cityConfiguration,
				"configFolder", getNamespace());
		mCityConfigurationFile = helper.getString(cityConfiguration,
				"configFile", getNamespace());

		final Element textures = configRoot
				.getChild("textures", getNamespace());
		final String supportedTextureCategories = helper.getString(textures,
				"supportedTextureCategories", getNamespace());
		String[] splittedCategories = supportedTextureCategories.split(",");

		mSupportedTextureCategories = new HashSet<String>(
				splittedCategories.length);
		for (String category : splittedCategories) {
			mSupportedTextureCategories.add(category);
		}
		mMaxTextureSize = helper.getInteger(textures, "maxTextureSize",
				getNamespace());
		mMaxTextureScaleFactor = helper.getInteger(textures,
				"maxTextureScaleFactor", getNamespace());
		mTextureFolder = helper.getString(textures, "texturePath",
				getNamespace());

		final Element importModels = configRoot.getChild("importModels",
				getNamespace());
		final String supportedModelCategories = helper.getString(importModels,
				"supportedModelCategories", getNamespace());
		splittedCategories = supportedModelCategories.split(",");
		mSuppotedModelCategories = new HashSet<String>(
				splittedCategories.length);
		for (String category : splittedCategories) {
			mSuppotedModelCategories.add(category);
		}
		mModelPath = helper
				.getString(importModels, "modelPath", getNamespace());

		final Element exportModels = configRoot.getChild("exportModels",
				getNamespace());
		mExportModelFolder = helper.getString(exportModels, "exportFolder",
				getNamespace());
		mExportFileName = helper.getString(exportModels, "exportFile",
				getNamespace());
	}

	// -------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return this.mProcessingMetadata.getNamespaceByPrefix("sy");
	}

	// -------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "System";
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mWindowWidth
	 */
	public Integer getWindowWidth() {
		return mWindowWidth;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mWindowHeight
	 */
	public Integer getWindowHeight() {
		return mWindowHeight;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mCityConfigurationFolder
	 */
	public String getCityConfigurationFolder() {
		return mCityConfigurationFolder;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mCityConfigurationFile
	 */
	public String getCityConfigurationFile() {
		return mCityConfigurationFile;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mSupportedTextureCategories
	 */
	public Set<String> getSupportedTextureCategories() {
		return mSupportedTextureCategories;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mTextureFolder
	 */
	public String getTextureFolder() {
		return mTextureFolder;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mMaxTextureSize
	 */
	public Integer getMaxTextureSize() {
		return mMaxTextureSize;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mMaxTextureScaleFactor
	 */
	public Integer getMaxTextureScaleFactor() {
		return mMaxTextureScaleFactor;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mSuppotedModelCategories
	 */
	public Set<String> getSuppotedModelCategories() {
		return mSuppotedModelCategories;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mModelPath
	 */
	public String getModelPath() {
		return mModelPath;
	}

	// -------------------------------------------------------------------------------------

	public String getExportModelFolder() {
		return mExportModelFolder;
	}

	// -------------------------------------------------------------------------------------

	public String getExportFileName() {
		return mExportFileName;
	}

	// -------------------------------------------------------------------------------------

}
