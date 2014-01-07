package semantic.building.modeler.prototype.building.footprint.walldescriptor;

import semantic.building.modeler.prototype.enums.FootprintConnectionType;

/**
 * Abstrakte Basisklasse fuer Wanddeskriptoren basierend auf poylgonalen
 * Grundrissen => Instanzen definieren die Art, wie Waende erzeugt bzw. mit
 * Tueren etc. ausgestattet werden, abgeleitete Klassen enthalten dann die
 * jeweilgs notwendige Logik zur Modellierungen solcher Wandstrukturen
 * 
 * @author Patrick
 * 
 */

abstract public class WallDescriptor {

	/** Art der Verbindung zwischen den Grundrisselementen */
	protected FootprintConnectionType mConnectionType = null;

	// ------------------------------------------------------------------------------------------
	/**
	 * @return the mConnectionType
	 */
	public FootprintConnectionType getConnectionType() {
		return mConnectionType;
	}

	// ------------------------------------------------------------------------------------------
	public void setFootprintConnectionType(FootprintConnectionType type) {
		mConnectionType = type;
	}
	// ------------------------------------------------------------------------------------------

}
