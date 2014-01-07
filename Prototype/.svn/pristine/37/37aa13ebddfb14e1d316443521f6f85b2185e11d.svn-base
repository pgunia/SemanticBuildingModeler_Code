package semantic.building.modeler.prototype.graphics.primitives;

/**
 * 
 * @author Patrick Gunia Instanzen dieser Klasse halten Informationen ueber
 *         Ausdehnung und Struktur von grafischen Primitiven fest. Hierbei gilt,
 *         dass nicht alle Informationen auch fuer alle Primitive benoetigt
 *         werden. Informationen dieser Art werden fuer die
 *         Texturkoordiantenrechnung eingesetzt.
 * 
 */

public class PrimitiveMetaInformation {

	/** Breite der unteren Kante des Elements */
	private float mWidthLowerEdge;

	/** Breite der oberen Kante, sofern definiert (nur Trapez) */
	private float mWidthUpperEdge;

	/** Hoehe des Elements */
	private float mHeight;

	// Nur fuer Trapez relevant
	/**
	 * Abweichung der oberen Vertices von der Grundkante, Versatz nach links
	 * oder rechts bzgl. der unteren beiden Vertices, die Delta-Werte sind genau
	 * dann 0, wenn es sich um ein Rechteck handelt (alle Winkel 90°)
	 */
	/** Linkes oberes Vertex */
	private float mDeltaX2;

	/** Rechtes oberes Vertex */
	private float mDeltaX3;

	// FLAGS (ebenfalls nur fuer Trapez relevant)
	/** Handelt es sich bei dem Element um ein Parallelogramm? */
	private boolean mIsParallelogramm;

	/**
	 * Handelt es sich um ein Trapez, dessen obere Kante laenger ist, als seine
	 * untere?
	 */
	private boolean mExceptionCase;

	/** Handelt es sich um ein Element, das zum Startvertex geneigt ist? */
	private boolean mSlopedToStart;

	/**
	 * Skalierungsfaktor fuer gleichmaessige Texturausdehnung, ist
	 * MIN(HoeheTextur / HoeheElement, BreiteTextur / BreiteElement)
	 */
	private float mScaleFactor;

	// ------------------------------------------------------------------------------------------

	/**
	 * Leerer Default-Konstruktor
	 */
	public PrimitiveMetaInformation() {
		super();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mWidthLowerEdge
	 */
	public float getWidthLowerEdge() {
		return mWidthLowerEdge;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mWidthLowerEdge
	 *            the mWidthLowerEdge to set
	 */
	public void setWidthLowerEdge(float mWidthLowerEdge) {
		this.mWidthLowerEdge = mWidthLowerEdge;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mWidthUpperEdge
	 */
	public float getWidthUpperEdge() {
		return mWidthUpperEdge;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mWidthUpperEdge
	 *            the mWidthUpperEdge to set
	 */
	public void setWidthUpperEdge(float mWidthUpperEdge) {
		this.mWidthUpperEdge = mWidthUpperEdge;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mHeight
	 */
	public float getHeight() {
		return mHeight;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mHeight
	 *            the mHeight to set
	 */
	public void setHeight(float mHeight) {
		this.mHeight = mHeight;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mDeltaX2
	 */
	public float getDeltaX2() {
		return mDeltaX2;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mDeltaX2
	 *            the mDeltaX2 to set
	 */
	public void setDeltaX2(float mDeltaX2) {
		this.mDeltaX2 = mDeltaX2;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mDeltaX3
	 */
	public float getDeltaX3() {
		return mDeltaX3;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mDeltaX3
	 *            the mDeltaX3 to set
	 */
	public void setDeltaX3(float mDeltaX3) {
		this.mDeltaX3 = mDeltaX3;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mIsParallelogramm
	 */
	public boolean isIsParallelogramm() {
		return mIsParallelogramm;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mIsParallelogramm
	 *            the mIsParallelogramm to set
	 */
	public void setIsParallelogramm(boolean mIsParallelogramm) {
		this.mIsParallelogramm = mIsParallelogramm;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mExceptionCase
	 */
	public boolean isExceptionCase() {
		return mExceptionCase;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mExceptionCase
	 *            the mExceptionCase to set
	 */
	public void setExceptionCase(boolean mExceptionCase) {
		this.mExceptionCase = mExceptionCase;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mSlopedToStart
	 */
	public boolean isSlopedToStart() {
		return mSlopedToStart;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mSlopedToStart
	 *            the mSlopedToStart to set
	 */
	public void setSlopedToStart(boolean mSlopedToStart) {
		this.mSlopedToStart = mSlopedToStart;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mScaleFactor
	 */
	public float getScaleFactor() {
		return mScaleFactor;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mScaleFactor
	 *            the mScaleFactor to set
	 */
	public void setScaleFactor(float mScaleFactor) {
		this.mScaleFactor = mScaleFactor;
	}

	// ------------------------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "PrimitiveMetaInformation [mWidthLowerEdge=" + mWidthLowerEdge
				+ ", mWidthUpperEdge=" + mWidthUpperEdge + ", mHeight="
				+ mHeight + ", mDeltaX2=" + mDeltaX2 + ", mDeltaX3=" + mDeltaX3
				+ ", mIsParallelogramm=" + mIsParallelogramm
				+ ", mExceptionCase=" + mExceptionCase + ", mSlopedToStart="
				+ mSlopedToStart + ", mScaleFactor=" + mScaleFactor + "]";
	}

	// ------------------------------------------------------------------------------------------

}
