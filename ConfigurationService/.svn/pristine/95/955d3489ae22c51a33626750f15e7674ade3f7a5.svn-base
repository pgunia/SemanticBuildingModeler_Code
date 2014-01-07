package semantic.building.modeler.configurationservice.model.component;

import java.util.List;

import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.helper.XMLParsingHelper;
import semantic.building.modeler.configurationservice.model.enums.FloorPosition;
import semantic.building.modeler.configurationservice.model.enums.Side;

/**
 * Klasse modelliert Komponenten, die durch Profilextraktion an Kanten
 * angebracht werden,
 * 
 * @author Patrick Gunia
 * 
 */

public class EdgeAdditionComponentConfiguration extends
		Abstract3DModelComponent {

	/** Groesse der Komponente ausgedrueckt als Anteil an der Quadhoehe */
	private transient Float mEdgeAdditionHeightRatio = null;

	/**
	 * Liste enthaelt Enum-Instanzen, die die Ziel Wandelemente beschreiben, an
	 * deren Kanten die Komponente appliziert werden soll
	 */
	private transient List<Side> mTargetWalls = null;

	/**
	 * Liste mit FloorPosition-Instanzen, die die Stockwerke beschreiben, an
	 * denen deren Wandelementen die Komponenten appliziert werden sollen
	 */
	private transient List<FloorPosition> mTargetFloors = null;

	// ------------------------------------------------------------------------------------------

	@Override
	public void construct(Element configRoot) {
		final XMLParsingHelper helper = XMLParsingHelper.getInstance();

		// lade die Quelldatei des Profils
		loadComponentSource(configRoot);
		mEdgeAdditionHeightRatio = helper.getFloat(configRoot,
				"edgeAdditionHeightRatio", getNamespace());
		mTargetWalls = processTargetWallsElement(configRoot);
		mTargetFloors = processTargetFloorsElement(configRoot);
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return this.mProcessingMetadata.getNamespaceByPrefix("co");
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "EdgeAddition";
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mEdgeAdditionHeightRatio
	 */
	public Float getEdgeAdditionHeightRatio() {
		return mEdgeAdditionHeightRatio;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mTargetWalls
	 */
	public List<Side> getTargetWalls() {
		return mTargetWalls;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mTargetFloors
	 */
	public List<FloorPosition> getTargetFloors() {
		return mTargetFloors;
	}
	// ------------------------------------------------------------------------------------------

}
