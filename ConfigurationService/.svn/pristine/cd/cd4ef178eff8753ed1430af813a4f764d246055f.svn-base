package semantic.building.modeler.configurationservice.model.component;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;

import semantic.building.modeler.configurationservice.helper.XMLParsingHelper;
import semantic.building.modeler.configurationservice.model.AbstractConfigurationObject;
import semantic.building.modeler.configurationservice.model.enums.FloorPosition;
import semantic.building.modeler.configurationservice.model.enums.Side;

/**
 * 
 * Abstrackte Basisklasse fuer saemtliche Gebaeudekomponenten, die aus
 * 3D-Modells erzeugt werden
 * 
 * @author Patrick Gunia
 * 
 */

public abstract class Abstract3DModelComponent extends
		AbstractConfigurationObject {

	/** Gibt an, woher das geladene Model stammt */
	protected ComponentModelSource mComponentModel = null;

	// -------------------------------------------------------------------------------------
	/**
	 * Methode laedt die ComponentModelSource-Komponente aus der
	 * Konfigurationsdatei
	 * 
	 * @param configRoot
	 *            Wurzelelement der Konfiguration, unter diesem befindet sich
	 *            die gesuchte ComponentModel-Komponente
	 */
	protected void loadComponentSource(final Element configRoot) {

		Element modelComponentSource = configRoot.getChild(
				"componentModelSource",
				this.mProcessingMetadata.getNamespaceByPrefix("co"));
		mComponentModel = new ComponentModelSource();
		mComponentModel.construct(modelComponentSource);

	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mComponentModel
	 */
	public ComponentModelSource getComponentModel() {
		return mComponentModel;
	}

	/**
	 * Methode verarbeitete Elemente des Typs 'targetWalls' und gibt als
	 * Ergebnis der Berechnungen eine Liste mit Side-Enum-Instanzen zurueck
	 * 
	 * @param root
	 *            XML-Element, das das targetWalls-Element als Kind besitzt
	 * @return Liste mit den extrahierten Side-Instanzen
	 */
	protected List<Side> processTargetWallsElement(final Element root) {

		final XMLParsingHelper helper = XMLParsingHelper.getInstance();
		final Element targetWall = root.getChild("targetWalls",
				mProcessingMetadata.getNamespaceByPrefix("co"));
		final List<Element> directions = targetWall.getChildren("direction",
				mProcessingMetadata.getNamespaceByPrefix("co"));

		String curSideString = null;
		final List<Side> targetWalls = new ArrayList<Side>(directions.size());
		for (Element child : directions) {
			curSideString = helper.getString(child);
			if (curSideString != null) {
				targetWalls.add(Side.valueOf(curSideString));
			}
		}
		return targetWalls;
	}

	/**
	 * Methode verarbeitete Elemente des Typs 'targetFloors' und gibt als
	 * Ergebnis der Berechnungen eine Liste mit FloorPosition-Enum-Instanzen
	 * zurueck
	 * 
	 * @param root
	 *            XML-Element, das das targetFloors-Element als Kind besitzt
	 * @return Liste mit den extrahierten FloorPosition-Instanzen
	 */
	protected List<FloorPosition> processTargetFloorsElement(final Element root) {
		final XMLParsingHelper helper = XMLParsingHelper.getInstance();
		final Element targetFloorsElement = root.getChild("targetFloors",
				mProcessingMetadata.getNamespaceByPrefix("co"));
		assert targetFloorsElement != null : "FEHLER: Gesuchtes Element ist nich vorhanden.";

		final List<Element> floors = targetFloorsElement
				.getChildren("floorPosition",
						mProcessingMetadata.getNamespaceByPrefix("co"));
		final List<FloorPosition> targetFloors = new ArrayList<FloorPosition>(
				floors.size());
		String targetFloorString = null;
		for (Element floor : floors) {
			targetFloorString = helper.getString(floor);
			if (targetFloorString != null) {
				targetFloors.add(FloorPosition.valueOf(targetFloorString));
			}
		}
		return targetFloors;
	}
	// ------------------------------------------------------------------------------------------

	// -------------------------------------------------------------------------------------

}
