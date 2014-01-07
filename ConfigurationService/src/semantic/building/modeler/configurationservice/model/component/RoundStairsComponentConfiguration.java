package semantic.building.modeler.configurationservice.model.component;

import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.helper.XMLParsingHelper;
import semantic.building.modeler.configurationservice.model.AbstractConfigurationObject;

/**
 * 
 * Klasse beschreibt Konfigurationen fuer die Konstruktionen von Treppen, die in
 * ihrer Struktu vergleichbar zu Pyramiden mit rechteckigem Grundriss wachsen
 * 
 * @author Patrick Gunia
 * 
 */

public class RoundStairsComponentConfiguration extends
		AbstractConfigurationObject {

	/** Anzahl der Treppenstufen */
	private transient Integer mNumberOfSteps = null;

	/**
	 * Breite des rechteckigen Grundrisses, aus dem die Treppenstufen erzeugt
	 * werden (optional)
	 */
	private transient Float mStairWidth = null;

	/**
	 * Laenge des rechteckigen Grundrisses, aus dem die Treppenstufen erzeugt
	 * werden (optional)
	 */
	private transient Float mStairLength = null;

	/** Hoehe einer einzelnen Treppenstufe */
	private transient Float mStepHeight = null;

	/** Breite einer einzelnen Treppenstufe */
	private transient Float mStepWidth = null;

	// -------------------------------------------------------------------------------------

	@Override
	public void construct(Element configRoot) {

		XMLParsingHelper helper = XMLParsingHelper.getInstance();
		mStairWidth = helper.getFloat(configRoot, "stairWidth", getNamespace());
		mStairLength = helper.getFloat(configRoot, "stairLength",
				getNamespace());
		mStepHeight = helper.getFloat(configRoot, "stepHeight", getNamespace());
		mStepWidth = helper.getFloat(configRoot, "stepWidth", getNamespace());
		mNumberOfSteps = helper.getInteger(configRoot, "numberOfSteps",
				getNamespace());
	}

	// -------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return mProcessingMetadata.getNamespaceByPrefix("co");
	}

	// -------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "RoundStairsComponent";
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mNumberOfSteps
	 */
	public Integer getNumberOfSteps() {
		return mNumberOfSteps;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mStairWidth
	 */
	public Float getStairWidth() {
		return mStairWidth;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mStairLength
	 */
	public Float getStairLength() {
		return mStairLength;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mStepHeight
	 */
	public Float getStepHeight() {
		return mStepHeight;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mStepWidth
	 */
	public Float getStepWidth() {
		return mStepWidth;
	}

	// -------------------------------------------------------------------------------------

}
