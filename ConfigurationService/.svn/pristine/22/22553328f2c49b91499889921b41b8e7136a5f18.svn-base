package semantic.building.modeler.configurationservice.model.component;

import java.util.List;

import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.helper.XMLParsingHelper;
import semantic.building.modeler.configurationservice.model.enums.FloorPosition;
import semantic.building.modeler.configurationservice.model.enums.ModelCategory;
import semantic.building.modeler.configurationservice.model.enums.Side;
import semantic.building.modeler.configurationservice.model.enums.VerticalAlignment;

/**
 * Klasse beschreibt Konfigurationen, die zur Erzeugung von Gesimsen verwendet
 * werden
 * 
 * @author Patrick Gunia
 * 
 */

public class MouldingComponentConfiguration extends Abstract3DModelComponent {

	/**
	 * Verhaeltnis zwischen der Hoehe des geladenen Profils und den Quads, an
	 * denen es appliziert wird
	 */
	private transient Float mMouldingHeightRatio = null;

	/** Vertikale Ausrichtung des Gesimses */
	private transient VerticalAlignment mVerticalAlignment = null;

	/**
	 * Liste mit den Richtungsangaben aller Quads, an denen das Gesimse
	 * appliziert werden soll
	 */
	private transient List<Side> mQuaddirections = null;

	/** An welchen Stockerken sollen die Gesimse angebracht werden */
	private transient List<FloorPosition> mFloorPositions = null;

	// ------------------------------------------------------------------------------------------

	@Override
	public void construct(final Element configRoot) {

		LOGGER.info("Root: " + configRoot);

		final XMLParsingHelper helper = XMLParsingHelper.getInstance();

		// lade die Quelldatei
		loadComponentSource(configRoot);
		mMouldingHeightRatio = helper.getFloat(configRoot,
				"mouldingHeightRatio", getNamespace());
		mVerticalAlignment = VerticalAlignment.valueOf(helper.getString(
				configRoot, "verticalAlignment", getNamespace()));

		mQuaddirections = processTargetWallsElement(configRoot);
		mFloorPositions = processTargetFloorsElement(configRoot);
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mMouldingHeightRatio
	 * @param mVerticalAlignment
	 */
	public MouldingComponentConfiguration(final Float mMouldingHeightRatio,
			final VerticalAlignment mVerticalAlignment) {
		super();
		this.mMouldingHeightRatio = mMouldingHeightRatio;
		this.mVerticalAlignment = mVerticalAlignment;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Leerer Default-Konstruktor
	 */
	public MouldingComponentConfiguration() {
		super();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mMouldingHeightRatio
	 * @param mVerticalAlignment
	 * @param mQuaddirections
	 */
	public MouldingComponentConfiguration(final Float mMouldingHeightRatio,
			final VerticalAlignment mVerticalAlignment,
			final List<Side> mQuaddirections, final ModelCategory modelCategory) {
		super();
		this.mMouldingHeightRatio = mMouldingHeightRatio;
		this.mVerticalAlignment = mVerticalAlignment;
		this.mQuaddirections = mQuaddirections;
		this.mComponentModel = new ComponentModelSource(modelCategory);
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return this.mProcessingMetadata.getNamespaceByPrefix("co");
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "MouldingComponent";
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mMouldingHeightRatio
	 */
	public Float getMouldingHeightRatio() {
		return mMouldingHeightRatio;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mVerticalAlignment
	 */
	public VerticalAlignment getVerticalAlignment() {
		return mVerticalAlignment;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mQuaddirections
	 */
	public List<Side> getQuaddirections() {
		return mQuaddirections;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mFloorPositions
	 */
	public List<FloorPosition> getFloorPositions() {
		return mFloorPositions;
	}

	// ------------------------------------------------------------------------------------------

}
