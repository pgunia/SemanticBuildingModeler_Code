package semantic.building.modeler.objectplacement.algorithm;

/**
 * 
 * @author Patrick Gunia
 * 
 *         Enum beschreibt bei Quadtree-Nodes deren Inhalt bzgl. der
 *         AbstractComponent-Teile, die sie enthalten koennen Die Reihenfolge
 *         der Konstanten ist zentral, da das Comparable-Interface im Code
 *         genutzt wird. Ihre Bedeutung steigt mit ihrer Position => CORNER hat
 *         die hoechste Prioritaet
 * 
 */

public enum ContentType {
	EMPTY, OUTSIDE, INSIDE, EDGE, CORNER, FULL;
}
