package semantic.building.modeler.configurationservice.model;

import org.jdom2.Element;
import org.jdom2.Namespace;

public class RoofConfiguration extends AbstractConfigurationObject {

	/** Konfigurationsobjekt fuer Standard-Daecher */
	private transient RoofDescriptor mRoofDescriptor = null;

	/** Art des verwendeten RoofDescriptors */
	private transient String mRoofDescriptorType = null;

	// -------------------------------------------------------------------------------------
	/** Leerer Default-Konstruktor */
	public RoofConfiguration() {

	}

	// -------------------------------------------------------------------------------------

	@Override
	public void construct(Element configRoot) {

		LOGGER.info(configRoot.getName());

		// externe Ressource laden?
		Element externalRoot = getExternalRootElement(configRoot, "ro");
		if (externalRoot != null) {
			configRoot = externalRoot;
		}

		// handelt es sich um ein Standard- oder ein Mansardendach?
		Element roofDescriptor = configRoot.getChild("roofDescriptor",
				getNamespace());

		if (roofDescriptor == null) {
			roofDescriptor = configRoot.getChild("roofDescriptorMansardRoof",
					getNamespace());
			assert roofDescriptor != null : "FEHLER: Ungueltige Dachkonfigurationsinstanz!";
			mRoofDescriptor = new RoofDescriptorMansard();
			mRoofDescriptor.construct(roofDescriptor);
			mRoofDescriptorType = "MansardRoof";
		} else {
			mRoofDescriptor = new RoofDescriptor();
			mRoofDescriptor.construct(roofDescriptor);
			mRoofDescriptorType = "StandardRoof";
		}

		LOGGER.debug(this);

	}

	// -------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return mProcessingMetadata.getNamespaceByPrefix("ro");
	}

	// -------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "Roof";
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mRoofDescriptorType
	 */
	public String getRoofDescriptorType() {
		return mRoofDescriptorType;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mRoofDescriptor
	 */
	public RoofDescriptor getRoofDescriptor() {
		return mRoofDescriptor;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return
	 * @see semantic.building.modeler.configurationservice.model.RoofDescriptor#getEqualWeightProbability()
	 */
	public Float getEqualWeightProbability() {
		return mRoofDescriptor.getEqualWeightProbability();
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return
	 * @see semantic.building.modeler.configurationservice.model.RoofDescriptor#getRoofScaling()
	 */
	public Float getRoofScaling() {
		return mRoofDescriptor.getRoofScaling();
	}

	// -------------------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "RoofConfiguration [mRoofDescriptor=" + mRoofDescriptor
				+ ", mRoofDescriptorType=" + mRoofDescriptorType + "]";
	}

	// -------------------------------------------------------------------------------------

}
