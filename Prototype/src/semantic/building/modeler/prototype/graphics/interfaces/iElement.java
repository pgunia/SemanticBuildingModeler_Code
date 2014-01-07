package semantic.building.modeler.prototype.graphics.interfaces;

/**
 * Interface fuer das abstrakte Konzept eines Elements Jedes Element besitzt
 * einen Typ und eine ID
 */

public interface iElement {

	/** gibt den Typ des aktuellen Objekts hart kodiert an den Aufrufer zurueck */
	public String getType();

	/** gibt die ID des jeweiligen Elements als String zurueck */
	public String getID();

	/** Setter fuer die ID des jeweiligen Elements */
	public void setID(String id);

	/**
	 * ID - Erzeugungsmethode fuer jede Art von Element IDs kodieren hierbei den
	 * Typ und die Ebene fuer einen bestimmten Typ nach dem Schema:
	 * Typ_ID_KindNummerEbene1#KindNummerEbene2... baseID: ID des Parent-Objekts
	 * concat: Nummerierung innerhalb des Objekts die ID selber, also der
	 * Zahlenteil, stimmt bei allen Elementen ueberein, die zu einem grafischen
	 * Element gehoeren
	 */
	public void generateID(String baseID, String concat);

}
