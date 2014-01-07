package semantic.building.modeler.configurationservice.model.component;

import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.helper.XMLParsingHelper;
import semantic.building.modeler.configurationservice.model.RangeConfigurationObject;

public class WindowComponentConfiguration extends Abstract3DModelComponent {

	/**
	 * Anteil an der Zielquadhoehe, um den das Fenster in Richtung der unteren
	 * Kante verschoben wird
	 */
	private transient Float mTranslationRatio = null;

	/**
	 * Anteil an Zielquadbreite, der als Mindestabstand zwischen positionierten
	 * Fenstern verwendet wird
	 */
	private transient Float mMinDistanceBetweenWindowsRatio = null;

	/** Verhaeltnis zwischen Fenster- und Quadausdehnungen */
	private transient RangeConfigurationObject mWindowToTargetQuadRatio = null;

	// ------------------------------------------------------------------------------------------

	@Override
	public void construct(Element configRoot) {

		// verarbeite die Komponentenkonfiguration
		loadComponentSource(configRoot);

		XMLParsingHelper helper = XMLParsingHelper.getInstance();
		mTranslationRatio = helper.getFloat(configRoot,
				"translationRatioForWindows", getNamespace());
		mMinDistanceBetweenWindowsRatio = helper.getFloat(configRoot,
				"minDistanceBetweenWindowsRatio", getNamespace());
		mWindowToTargetQuadRatio = new RangeConfigurationObject(
				configRoot.getChild("windowToTargetQuadRatio", getNamespace()));
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return this.mProcessingMetadata.getNamespaceByPrefix("co");
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "WindowComponent";
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mTranslationRatio
	 */
	public Float getTranslationRatio() {
		return mTranslationRatio;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mMinDistanceBetweenWindowsRatio
	 */
	public Float getMinDistanceBetweenWindowsRatio() {
		return mMinDistanceBetweenWindowsRatio;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mWindowToTargetQuadRatio
	 */
	public RangeConfigurationObject getWindowToTargetQuadRatio() {
		return mWindowToTargetQuadRatio;
	}

	// ------------------------------------------------------------------------------------------

}
