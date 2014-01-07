package semantic.building.modeler.configurationservice.model;

import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.helper.XMLParsingHelper;

/**
 * Instanzen dieser Klasse repraesentieren Parameter, die das gesamte Gebaeude
 * und seine Ausdehnungen betreffen
 * 
 * @author Patrick Gunia
 * 
 */

public class BuildingDimensionsConfiguration extends
		AbstractConfigurationObject {

	/** Gebauedebreite */
	private transient RangeConfigurationObject mWidth = null;

	/** Gebaeudehoehe */
	private transient RangeConfigurationObject mHeight = null;

	/** Gebaeudelaenge */
	private transient RangeConfigurationObject mLength = null;

	/** Wanddicke */
	private transient Float mWallThickness = null;

	// -------------------------------------------------------------------------------------

	@Override
	public void construct(final Element configRoot) {

		assert configRoot.getName().equals("dimensions") : "FEHLER: Ungueltiges Konfigurationsobjekt! Tagname: "
				+ configRoot.getName();

		Element width = configRoot.getChild("width", getNamespace());
		mWidth = new RangeConfigurationObject(width);

		Element height = configRoot.getChild("height", getNamespace());
		mHeight = new RangeConfigurationObject(height);

		Element length = configRoot.getChild("length", getNamespace());
		mLength = new RangeConfigurationObject(length);

		mWallThickness = XMLParsingHelper.getInstance().getFloat(configRoot,
				"wallThickness", getNamespace());
		LOGGER.debug(this);
	}

	// -------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return mProcessingMetadata.getNamespaceByPrefix("bu");
	}

	// -------------------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "BuildingDimension: BuildingDimensions: Width: " + mWidth
				+ " Height: " + mHeight + " Length: " + mLength
				+ " Wall-Thickness: " + mWallThickness;
	}

	// -------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "BuildingDimensions";
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mWidth
	 */
	public RangeConfigurationObject getWidth() {
		return mWidth;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mHeight
	 */
	public RangeConfigurationObject getHeight() {
		return mHeight;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mLength
	 */
	public RangeConfigurationObject getLength() {
		return mLength;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mWallThickness
	 */
	public Float getWallThickness() {
		return mWallThickness;
	}
	// -------------------------------------------------------------------------------------
}
