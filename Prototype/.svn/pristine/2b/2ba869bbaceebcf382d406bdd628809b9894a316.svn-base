package semantic.building.modeler.prototype.building.footprint.walldescriptor;

import semantic.building.modeler.math.MyPolygon;
import semantic.building.modeler.prototype.enums.FootprintConnectionType;

/**
 * Instanzen dieser Klasse beschreiben offene Waende in Grundrissen, die nach
 * "aussen" fuehren
 * 
 * @author Patrick Gunia
 * 
 */

public class OpenWallOutsideDescriptor extends WallDescriptor {

	/** Grundrisspolygon, fuer das der Deskriptor definiert wird */
	protected MyPolygon mFootprint = null;

	/** Index der betroffenen Kante */
	protected int mEdgeIndex = -1;

	// ------------------------------------------------------------------------------------------

	public OpenWallOutsideDescriptor(MyPolygon poly, int edgeIndex) {
		mConnectionType = FootprintConnectionType.WALL_OUTSIDE;
		mFootprint = poly;
		mEdgeIndex = edgeIndex;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mFootprint
	 */
	public MyPolygon getFootprint() {
		return mFootprint;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mEdgeIndex
	 */
	public int getEdgeIndex() {
		return mEdgeIndex;
	}

	// ------------------------------------------------------------------------------------------

}
