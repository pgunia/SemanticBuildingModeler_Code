package semantic.building.modeler.configurationservice.model.component;

import java.util.List;

import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.helper.XMLParsingHelper;
import semantic.building.modeler.configurationservice.model.enums.FloorPosition;

/**
 * Klasse beschreibt Konfigurationen zur Konstruktion von Fascia-Elementen
 * 
 * @author Patrick Gunia
 * 
 */

public class FasciaComponentConfiguration extends Abstract3DModelComponent {

	/**
	 * Verhaeltnis zwischen der Hoehe des geladenen Profils und den Quads, an
	 * denen es appliziert wird
	 */
	private transient Float mMouldingHeightRatio = null;

	/**
	 * Liste mit saemtlichen FloorPosition-Enums, an denen Fascias appliziert
	 * werden sollen
	 */
	private transient List<FloorPosition> mFloorPositions = null;

	@Override
	public void construct(final Element configRoot) {

		// verarbeite die Komponentenkonfiguration
		loadComponentSource(configRoot);
		mMouldingHeightRatio = XMLParsingHelper.getInstance().getFloat(
				configRoot, "mouldingHeightRatio", getNamespace());
		mFloorPositions = processTargetFloorsElement(configRoot);
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return mProcessingMetadata.getNamespaceByPrefix("co");
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "FasciaComponent";
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
	 * @return the mFloorPositions
	 */
	public List<FloorPosition> getFloorPositions() {
		return mFloorPositions;
	}

	// ------------------------------------------------------------------------------------------

}
