package semantic.building.modeler.weightedstraightskeleton.result;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * 
 * @author Patrick Gunia Ein ResultFace stellt die vollstaendige Struktur des
 *         Schrumpfungsalgorithmus ausgehend von einer Kante des Eingabepolygons
 *         dar. Fuer jede solche Kante wird waehrend der Ergebnisberechnung ein
 *         solches ResultFace bestehend aus einer beliebigen Anzahl von
 *         ResultElements gespeichert
 * 
 */

public class ResultFace {

	/** Eingabepolygonkante, fuer die das Face erstellt wurde */
	private ResultEdge mBaseEdge = null;

	/** Vektor mit allen ResultElementen des Faces */
	private List<AbstractResultElement> mElements = null;

	/** RGB-Werte, in denen das Ergebniselement gezeichnet wird */
	private float drawColorModR;
	private float drawColorModG;
	private float drawColorModB;

	// ------------------------------------------------------------------------------------------

	public ResultFace() {
		super();
		mElements = new ArrayList<AbstractResultElement>();
		getRandomNumber();
	}

	// ------------------------------------------------------------------------------------------

	public void addElementToFace(AbstractResultElement element) {

		if (mElements.indexOf(element) == -1) {
			mElements.add(element);
		}

	}

	// ------------------------------------------------------------------------------------------

	public ResultEdge getBaseEdge() {
		return mBaseEdge;
	}

	// ------------------------------------------------------------------------------------------

	public void setBaseEdge(ResultEdge mBaseEdge) {
		this.mBaseEdge = mBaseEdge;
	}

	// ------------------------------------------------------------------------------------------

	public List<AbstractResultElement> getElements() {
		return mElements;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Erzeugt Zufallszahlen fuer die einzelnen Komponenten der RGB-Farbe, die
	 * fuer das Zeichnen der Faces verwendet werden
	 */
	private void getRandomNumber() {
		Random generator = new Random();
		float resultNumber = generator.nextFloat();
		resultNumber *= 255.0f;
		drawColorModR = resultNumber;

		resultNumber = generator.nextFloat();
		resultNumber *= 255.0f;
		drawColorModG = resultNumber;

		resultNumber = generator.nextFloat();
		resultNumber *= 255.0f;
		drawColorModB = resultNumber;

	}

	// ------------------------------------------------------------------------------------------

	public float getDrawColorModR() {
		return drawColorModR;
	}

	// ------------------------------------------------------------------------------------------

	public float getDrawColorModG() {
		return drawColorModG;
	}

	// ------------------------------------------------------------------------------------------
	public float getDrawColorModB() {
		return drawColorModB;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode durchlaeuft saemtliche Elemente dieses Faces und fuehrt fuer
	 * jedes Element die Texturberechnungen durch. Die uebergebenen
	 * Texturausdehnungen sind Basis der Koordinatenberechnungen im Texturraum
	 * 
	 * @param textureWidth
	 *            Texturbreite
	 * @param textureHeight
	 *            Texturhoehe
	 * 
	 */
	public void finalizeFace(float textureWidth, float textureHeight) {

		AbstractResultElement currentElement = null;

		// wenn ein Face Split-Element-Results enthaelt, so werden diese in dem
		// Buffer abgelegt
		// um dadurch die Reihenfolge der Berechnung umzukehren
		LinkedList<AbstractResultElement> elementBuffer = new LinkedList<AbstractResultElement>();

		Iterator<AbstractResultElement> elementIter = mElements.iterator();
		while (elementIter.hasNext()) {
			currentElement = elementIter.next();

			// wenn ein linker Nachbar vorliegt, fuege das Element hinten an den
			// Buffer an
			if (currentElement.hasLeftNeighbour())
				elementBuffer.addLast(currentElement);
			else
				currentElement.finalizeElement(textureWidth, textureHeight);
		}

		// an diesem Punkt enthaelt der Buffer alle Result-Elemente, die auf
		// Split- oder Vertex-Event
		// Kanten vorgekommen sind, in umgekehrter Reihenfolge ihrer Erzeugung.
		// Rufe nun ebenfalls
		// die finalize()-Methoden auf, so dass die notwendigen
		// Koordinatenoffsets im Texturraum
		// berechnet werden
		if (elementBuffer.size() > 0) {
			elementIter = elementBuffer.iterator();
			while (elementIter.hasNext()) {
				elementIter.next().finalizeElement(textureWidth, textureHeight);
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode skaliert die Texturkoordinaten basierend auf dem ueber alle
	 * Elemente berechneten Skalierungsfaktor und validiert anschliessend die
	 * Gueltigkeit der so bestimmten Koordinaten (liegen diese Koordinaten im
	 * Texturraum)
	 */
	public void scaleAndValidateTexture() {

		Iterator<AbstractResultElement> elementIter = mElements.iterator();
		while (elementIter.hasNext()) {
			elementIter.next().scaleAndValidateTexture();
		}

	}
	// ------------------------------------------------------------------------------------------

}
