package semantic.building.modeler.prototype.algorithm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import semantic.building.modeler.math.Ray;

//------------------------------------------------------------------------------------------
/**
 * @author Patrick Gunia Klasse dient der Verwaltung gemergter Grundrisse. Ein
 *         Bucket ist dabei ein Eimer, in den alle Grundrisse geworfen werden,
 *         die sich ueberschneiden. In der 2. Phase des Algorithmus wird fuer
 *         jeden Bucket ein gemergter Grundriss berechnet, der in weiteren
 *         Schritten als Basis fuer die weiteren Berechnungen fungiert.
 */
public class FootprintBucket {

	/** Vector mit allen Grundrissen, die sich uberschneiden */
	private List<Footprint> mFootprints = null;

	// ------------------------------------------------------------------------------------------

	public FootprintBucket() {
		mFootprints = new ArrayList<Footprint>();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode fuegt dem Bucket einen weiteren Footprint hinzu, sofern dieser
	 * noch nicht enthalten ist.
	 */
	public void addFootprint(Footprint newFootprint) {
		if (mFootprints.indexOf(newFootprint) == -1) {
			mFootprints.add(newFootprint);
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode fuegt dem Bucket alle Grundrisse aus dem Vektor hinzu
	 * 
	 * @param footprints
	 *            Hinzuzufuegende Grundriss-Instanzen
	 */
	private void addAll(List<Footprint> footprints) {
		Footprint currentFootprint = null;
		Iterator<Footprint> footprintIter = footprints.iterator();
		while (footprintIter.hasNext()) {
			currentFootprint = footprintIter.next();
			addFootprint(currentFootprint);
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode testet, ob sich der uebgergebene Footprint bereits im
	 * Grundriss-Vektor befindet
	 * 
	 * @param footprint
	 *            Grundriss, fuer den geprueft wird, ob er bereits hinzugefuegt
	 *            wurde
	 * @return True, falls der Grundriss bereits enthalten ist, False sonst
	 */
	public boolean isFootprintInBucket(Footprint footprint) {
		if (mFootprints.contains(footprint))
			return true;
		else
			return false;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode merged zwei Buckets, indem sie die Footprints des uebergebenen
	 * Buckets zu den eigenen Footprints hinzufuegt
	 * 
	 * @param bucket
	 *            Bucket, dessen Footprints zum aktuellen Bucket hinzugefuegt
	 *            werden sollen
	 */
	public void mergeBuckets(FootprintBucket bucket) {
		addAll(bucket.getFootprints());
	}

	// ------------------------------------------------------------------------------------------
	public List<Footprint> getFootprints() {
		return mFootprints;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Loescht alle Footprints innerhalb des Eimers
	 */
	public void clear() {
		mFootprints.clear();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode prueft, ob noch Grundrisse im Eimer sind.
	 * 
	 * @return True, wenn keine Footprints mehr vorhanden sind, False sonst
	 */
	public boolean isEmpty() {
		return mFootprints.isEmpty();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode testet, ob es Elemente im Eingabebucket gibt, die auch im
	 * aktuellen Bucket vorkommen
	 * 
	 * @param other
	 *            Eingabebucket
	 * @return True, falls ein Footprint gefunden wurde, der in beiden Buckets
	 *         vorkommt, false sonst
	 */
	public boolean isMergeNecessary(FootprintBucket other) {

		Iterator<Footprint> bucketIter = mFootprints.iterator();
		Footprint current = null;

		// durchlaufe alle Grundrisse im aktuellen Bucket und teste, ob
		// diese im uebergebenen Bucket vorkommen
		while (bucketIter.hasNext()) {
			current = bucketIter.next();
			if (other.mFootprints.contains(current))
				return true;
		}
		return false;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode liefert einen Vektor mit allen Strahlen die sich als
	 * Polygonkanten innerhalb des Eimers befinden
	 * 
	 * @return Vector mit allen Polygonkanten des Eimers
	 */
	public List<Ray> getAllRays() {
		List<Ray> allRays = new Vector<Ray>();
		Iterator<Footprint> footprintIter = mFootprints.iterator();
		while (footprintIter.hasNext()) {
			allRays.addAll(footprintIter.next().getRays());
		}
		return allRays;
	}

	// ------------------------------------------------------------------------------------------

}
