package semantic.building.modeler.weightedstraightskeleton.result;

import semantic.building.modeler.math.MyVector2f;


/**
 * 
 * @author Patrick Gunia Eine ResultEdge beschreibt eine Kante im Ergebnis des
 *         Algorithmus. Die Result-Edge-Kanten entsprechen den Kanten des
 *         Eingabepolygons.
 * 
 */

public class ResultEdge extends AbstractResultElement {

	/** Kantengewicht */
	private transient Float mWeight = null;

	// ------------------------------------------------------------------------------------------

	@Override
	public int getNumberOfPoints() {
		return 2;
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public String getType() {

		return "ResultEdge";
	}

	// ------------------------------------------------------------------------------------------

	@Override
	protected void doComputations() {
		// TODO Auto-generated method stub

	}

	// ------------------------------------------------------------------------------------------

	@Override
	protected void createTextureCoords(float width, float height) {
		mTextureCoords.put(0, new MyVector2f(0.0f, 0.0f));
		mTextureCoords.put(1, new MyVector2f(1.0f, 0.0f));

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mWeight
	 */
	public Float getWeight() {
		return mWeight;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mWeight
	 *            the mWeight to set
	 */
	public void setWeight(Float mWeight) {
		this.mWeight = mWeight;
	}

	// ------------------------------------------------------------------------------------------

}
