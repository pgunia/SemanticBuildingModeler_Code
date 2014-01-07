package semantic.building.modeler.prototype.graphics.interfaces;

import java.util.List;
import java.util.Set;

import semantic.building.modeler.configurationservice.model.enums.Side;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.prototype.service.EdgeManager;

/**
 * 
 * @author Patrick Gunia Interface fuer alle primitiven Grafikkomponenten. Dazu
 *         gehoeren Linien, Dreieckes und Quads als Abstraktion von Faces.
 * 
 */

public interface iGraphicPrimitive extends Cloneable {

	/** Methode liefert saemtliche Vertices des komplexen Parentobjekts */
	public List<Vertex3d> getVertices();

	/**
	 * Methode speichert das Elternelement eines Elements, bsw. Face bei
	 * Triangle, Triangle bei Line etc.
	 */
	public void setParent(iGraphicPrimitive parent);

	/**
	 * Methode dient dazu, in einer Hierarchie aus graphischen Primitiven
	 * nachtraeglich die Parents zu setzen oder zu veraendern
	 * 
	 * @param parent
	 *            Elternelement des primitiven Grafikobjekts
	 */
	//
	public void updateParent(iGraphicPrimitive parent);

	/** Berechnung einer Ebene fuer primitive Objekte, die diese enthaelt */
	public void calculatePlane();

	/** Berechnung des Mittelpunktes */
	public void calculateCenter();

	/**
	 * Funktion, die nach einer Modifikation der Geometrie aufgerufen wird jede
	 * implementierende Klasse kann hier evtl. erforderliche Berechnungen
	 * unterbringen bsw. Neuberechnung von Normalenvektoren etc.
	 */
	public void update();

	/** Getter fuer Normalenvektor */
	public MyVector3f getNormal();

	/** Getter fuer Mittelpunktsvektor */
	public MyVector3f getCenter();

	/**
	 * Methode gibt die Ausrichtung des jeweiligen Elements zurueck, sollte das
	 * Element selber diese nicht verwalten, so wird die Ausrichtung des
	 * Elternelements zurueckgegeben
	 * 
	 * @return Ausrichtung des Elements selber oder die seines Elternelements
	 */
	public Side getDirection();

	/**
	 * Zugriffsmethode auf den EdgeManager auf der hoechsten Ebene eine
	 * Grafikhierarchie
	 */
	public EdgeManager getEdgeManager();

	/**
	 * Methode gibt an, ob ein grafisches Element weitere Kindelemente gleichen
	 * Typs besitzt
	 */
	public boolean hasChildren();

	/**
	 * Methode dient zum Update der Referenzen auf Lines im Line-Manager bsw.
	 * nach einem Clone eines komplexen Objekts
	 */
	public void updateReferences();

	/**
	 * Methode liefert fuer ein geometrisches Primitiv alle Indices bis auf
	 * Line-Ebene
	 */
	public Set<Integer> getAllIndices();

}
