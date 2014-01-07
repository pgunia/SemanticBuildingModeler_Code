package semantic.building.modeler.prototype.building.footprint.walldescriptor;

import semantic.building.modeler.math.MyPolygon;
import semantic.building.modeler.prototype.enums.FootprintConnectionType;
import semantic.building.modeler.prototype.service.PositionConfig;

/**
 * Klasse modelliert Tuerverbindungen zwischen Kanten zweier Polygone. Hierfuer
 * werden sowohl die Polygone als auch die betroffenen Kanten in jedem Polygon
 * gespeichert
 * 
 * @author Patrick Gunia
 * 
 */

public class DoorDescriptor extends OpenWallConnectionDescriptor {

	/**
	 * Position-Config, enthaelt alle relevanten Parameter zur Positionierung
	 * der Tuer
	 */
	private PositionConfig mPosConf = null;

	/**
	 * Standardkonstruktor
	 * 
	 * @param poly1
	 *            1. Grundriss
	 * @param poly2
	 *            2. Grundriss
	 * @param edgeIndex1
	 *            Index der betroffenen Kante auf dem ersten Grundriss
	 * @param edgeIndex2
	 *            Index der betroffenen Kante auf dem zweiten Grundriss
	 * @param doorType
	 *            Typ des zu verwendenden Tuermodels
	 */

	public DoorDescriptor(MyPolygon poly1, MyPolygon poly2, int edgeIndex1,
			int edgeIndex2, PositionConfig posConf) {
		super(poly1, poly2, edgeIndex1, edgeIndex2);
		mConnectionType = FootprintConnectionType.DOOR;
		mPosConf = posConf;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mPosConf
	 */
	public PositionConfig getPosConf() {
		return mPosConf;
	}

	// ------------------------------------------------------------------------------------------

}
