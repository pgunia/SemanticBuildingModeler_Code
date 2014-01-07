package semantic.building.modeler.configurationservice.model;

import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.helper.XMLParsingHelper;

public class RoofDescriptorMansard extends RoofDescriptor {

	/**
	 * Range innerhalb dessen sich die zweite Dachsteigung an den Hauptseiten
	 * bewegt
	 */
	private transient RangeConfigurationObject mSecondSlopeMain = new RangeConfigurationObject(
			0.7f, 1.4f);

	/**
	 * Range innerhalb dessen sich die zweite Dachsteigung an den Gebaeudeseiten
	 * bewegt
	 */
	private transient RangeConfigurationObject mSecondSlopeSide = new RangeConfigurationObject(
			0.6f, 1.4f);

	/** Hoehe, ab der die Dachneigung geaendert wird */
	private transient Integer mSlopeChangeHeight = null;

	// -------------------------------------------------------------------------------------

	/**
	 * 
	 */
	public RoofDescriptorMansard() {
		super();
	}

	// -------------------------------------------------------------------------------------

	@Override
	public void construct(final Element roofDescriptor) {
		assert "roofDescriptorMansardRoof".equals(roofDescriptor.getName()) : "FEHLER: Ungueltiges Konfigurationsobjekt! Tagname: "
				+ roofDescriptor.getName();

		Element mainSlopeElement = roofDescriptor.getChild(
				"roofFirstSlopeMain", getNamespace());
		mMainSlope = new RangeConfigurationObject(mainSlopeElement,
				getNamespace(), mMainSlope);

		mainSlopeElement = roofDescriptor.getChild("roofSecondSlopeMain",
				getNamespace());
		mSecondSlopeMain = new RangeConfigurationObject(mainSlopeElement,
				getNamespace(), mSecondSlopeMain);

		Element sideSlopeElement = roofDescriptor.getChild(
				"roofFirstSlopeSide", getNamespace());
		mSideSlope = new RangeConfigurationObject(sideSlopeElement,
				getNamespace(), mSideSlope);

		sideSlopeElement = roofDescriptor.getChild("roofSecondSlopeSide",
				getNamespace());
		mSecondSlopeSide = new RangeConfigurationObject(sideSlopeElement,
				getNamespace(), mSecondSlopeSide);

		final XMLParsingHelper helper = XMLParsingHelper.getInstance();
		mSlopeChangeHeight = helper.getInteger(roofDescriptor,
				"slopeChangeHeight", getNamespace());

		mEqualWeightProbability = XMLParsingHelper.getInstance().getFloat(
				roofDescriptor, "equalWeightForRoofSides", getNamespace(),
				mEqualWeightProbability);
		mRoofScaling = XMLParsingHelper.getInstance().getFloat(roofDescriptor,
				"roofScaling", getNamespace(), mRoofScaling);
		LOGGER.debug("Dach-Konfiguration: " + this);

	}

	// -------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return mProcessingMetadata.getNamespaceByPrefix("ro");
	}

	// -------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "RoofDescriptorMansard";
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mSecondSlopeMain
	 */
	public RangeConfigurationObject getSecondSlopeMain() {
		return mSecondSlopeMain;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mSecondSlopeSide
	 */
	public RangeConfigurationObject getSecondSlopeSide() {
		return mSecondSlopeSide;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mEqualWeightProbability
	 */
	public Float getEqualWeightProbability() {
		return mEqualWeightProbability;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mRoofScaling
	 */
	public Float getRoofScaling() {
		return mRoofScaling;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mSlopeChangeHeight
	 */
	public Integer getSlopeChangeHeight() {
		return mSlopeChangeHeight;
	}

	// -------------------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "RoofDescriptorMansard [mSecondSlopeMain=" + mSecondSlopeMain
				+ ", mSecondSlopeSide=" + mSecondSlopeSide
				+ ", mSlopeChangeHeight=" + mSlopeChangeHeight
				+ ", mMainSlope=" + mMainSlope + ", mSideSlope=" + mSideSlope
				+ ", mEqualWeightProbability=" + mEqualWeightProbability
				+ ", mRoofScaling=" + mRoofScaling + ", mProcessingMetadata="
				+ mProcessingMetadata + "]";
	}

	// -------------------------------------------------------------------------------------

}
