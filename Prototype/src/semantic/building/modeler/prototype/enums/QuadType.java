package semantic.building.modeler.prototype.enums;

/**
 * Aufzaehlung dient der Unterteilung einer Menge von Quads in INDOOR- und
 * OUTDOOR-Quads, also solche, die Innenwaende beschreiben und solche, die
 * Aussenwaende beschreiben. Ueber diese Aufzaehlungstypen erfolgt der Zugriff
 * auf eine Teilmenge der Quads, die innerhalb eines komplexen Objekts verwaltet
 * werden
 * 
 * @author Patrick Gunia
 * 
 */

public enum QuadType {
	INDOOR, OUTDOOR, ALL
}
