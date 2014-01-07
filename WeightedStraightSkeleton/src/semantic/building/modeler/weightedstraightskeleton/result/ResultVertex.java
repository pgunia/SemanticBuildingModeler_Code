package semantic.building.modeler.weightedstraightskeleton.result;

import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.Vertex3d;


/**
 * 
 * @author Patrick Gunia Klasse beschreibt Ergebnisstrukturen in der final zu
 *         errechnenden ResultComplex-Struktur. Dient dabei primaer dem
 *         Ueberschreiben der Vergleichsoperatoren, um im Gegensatz zur
 *         Basisklasse Positionen mit Toleranz vergleichen zu koennen
 * 
 */
public class ResultVertex extends Vertex3d {

	// ------------------------------------------------------------------------------------------

	/**
	 * Leerer Default-Konstruktors
	 */
	public ResultVertex() {
		super();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Konstruktor mit Positionsuebergabe
	 * 
	 * @param position
	 *            Posiion des Vertex
	 */
	public ResultVertex(MyVector3f position) {
		super(position);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode implementiert einen Vergleich mit "Toleranz" bezgl. der
	 * Vertexpositionen. Dies unterscheidet diese Klasse von der Basisklasse
	 * Vertex3d => Implementation dient dabei dem Ueberschreiben der
	 * equals()-Methode
	 */
	@Override
	public boolean equals(Object obj) {

		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResultVertex other = (ResultVertex) obj;
		if (!comparePositionsWithTolerance(mPosition, other.mPosition))
			return false;
		return true;
	}

	// ------------------------------------------------------------------------------------------

}
