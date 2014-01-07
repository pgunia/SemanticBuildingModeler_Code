package semantic.building.modeler.configurationservice.model;

import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.helper.XMLParsingHelper;

public class RoofDescriptor extends AbstractConfigurationObject {

	/** Range innerhalb dessen sich die Dachsteigung an den Hauptseiten bewegt */
	protected transient RangeConfigurationObject mMainSlope = new RangeConfigurationObject(
			0.7f, 1.4f);

	/**
	 * Range innerhalb dessen sich die Dachsteigung an den Gebaeudeseiten bewegt
	 */
	protected transient RangeConfigurationObject mSideSlope = new RangeConfigurationObject(
			0.6f, 1.4f);

	/**
	 * Wahrscheinlichkeit, dass fuer die Haupt- und Nebenseite die gleiche
	 * Steigung verwendet wird
	 */
	protected transient Float mEqualWeightProbability = 0.3f;

	/**
	 * Skalierungsfaktor, um den der Dachgrundriss skaliert wird, bsw. um ein
	 * ueberhaengendes Dach zu realisieren
	 */
	protected transient Float mRoofScaling = 1.0f;

	// -------------------------------------------------------------------------------------

	@Override
	public void construct(final Element roofDescriptor) {
		assert roofDescriptor.getName().equals("roofDescriptor") : "FEHLER: Ungueltiges Konfigurationsobjekt! Tagname: "
				+ roofDescriptor.getName();

		Element mainSlopeElement = roofDescriptor.getChild("roofSlopeMain",
				getNamespace());
		mMainSlope = new RangeConfigurationObject(mainSlopeElement,
				getNamespace(), mMainSlope);

		Element sideSlopeElement = roofDescriptor.getChild("roofSlopeSide",
				getNamespace());
		mSideSlope = new RangeConfigurationObject(sideSlopeElement,
				getNamespace(), mSideSlope);

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
		return "RoofDescriptor";
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mMainSlope
	 */
	public RangeConfigurationObject getMainSlope() {
		return mMainSlope;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mSideSlope
	 */
	public RangeConfigurationObject getSideSlope() {
		return mSideSlope;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "RoofDescriptor [mMainSlope=" + mMainSlope + ", mSideSlope="
				+ mSideSlope + ", mEqualWeightProbability="
				+ mEqualWeightProbability + ", mRoofScaling=" + mRoofScaling
				+ "]";
	}

	// -------------------------------------------------------------------------------------

}
