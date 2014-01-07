package semantic.building.modeler.configurationservice.model.component;

import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.helper.XMLParsingHelper;
import semantic.building.modeler.configurationservice.model.RangeConfigurationObject;

/**
 * 
 * Konfigurationsklasse beschreibt Tuermodelle
 * 
 * @author Patrick Gunia
 * 
 */

public class DoorComponentConfiguration extends Abstract3DModelComponent {

	/**
	 * Verhaeltnis in Bezug auf die Quadlaenge, in der die Tuer positioniert
	 * wird. Beschreibt den minimalen Abstand, den die Tuer von einer Kante
	 * haben soll
	 */
	private transient Float mDistanceToCornerRatio = null;

	/** Verhaeltnis der Tuer- zu den Quaddimensionen */
	private transient RangeConfigurationObject mDoorToQuadRatio = null;

	// ------------------------------------------------------------------------------------------

	@Override
	public void construct(final Element configRoot) {

		XMLParsingHelper helper = XMLParsingHelper.getInstance();
		mDistanceToCornerRatio = helper.getFloat(configRoot,
				"distanceToCornerRatio", getNamespace());
		mDoorToQuadRatio = new RangeConfigurationObject(configRoot.getChild(
				"doorToTargetQuadRatio", getNamespace()));

		// verarbeite die Komponentenkonfiguration
		loadComponentSource(configRoot);

	}

	// ------------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return this.mProcessingMetadata.getNamespaceByPrefix("co");
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "DoorComponent";
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mDistanceToCornerRatio
	 */
	public Float getDistanceToCornerRatio() {
		return mDistanceToCornerRatio;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mDoorToQuadRatio
	 */
	public RangeConfigurationObject getDoorToQuadRatio() {
		return mDoorToQuadRatio;
	}

	// ------------------------------------------------------------------------------------------

}
