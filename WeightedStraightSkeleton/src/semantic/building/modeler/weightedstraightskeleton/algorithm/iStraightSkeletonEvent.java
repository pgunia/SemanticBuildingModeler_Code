package semantic.building.modeler.weightedstraightskeleton.algorithm;

import semantic.building.modeler.math.MyVector3f;


/**
 * @author Patrick Gunia
 * 
 */
public interface iStraightSkeletonEvent {

	/**
	 * Liefert den Abstand des Events von der jeweiligen Kante
	 * 
	 * @return Abstand
	 */
	public Float getDistance();

	/**
	 * Gibt den Typ des aufgetretenen Events zurueck, also Edge, Split oder
	 * Vertex
	 * 
	 * @return Typ
	 */
	public String getType();

	/**
	 * Liefert den Schnittpunkt (je nach Event-Art unterschiedlich berechnet)
	 * 
	 * @return Schnittpunkt
	 */
	public MyVector3f getSchnittpunkt();

	/**
	 * Gibt eine Referenz auf den Schnittpunkt zurueck
	 * 
	 * @return Referenz auf Schnittpunkt
	 */
	public MyVector3f getSchnittpunktPtr();

	/**
	 * liefert das ausloesende Vertex
	 * 
	 * @return Ausloesendes Vertex
	 */
	public SkeletonVertex getVertex();

	/**
	 * Liefert die Distanz vom Schnittpunkt zum Vertex
	 * 
	 * @return Distanz vom Schnittpunkt zum Vertex
	 */
	public Float getShrinkDistance();
}
