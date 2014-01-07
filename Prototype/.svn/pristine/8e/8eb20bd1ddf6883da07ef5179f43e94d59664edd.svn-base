package semantic.building.modeler.prototype.roof.configuration;

import java.util.Iterator;
import java.util.List;

import semantic.building.modeler.configurationservice.model.RoofConfiguration;
import semantic.building.modeler.configurationservice.model.RoofDescriptor;
import semantic.building.modeler.configurationservice.model.RoofDescriptorMansard;
import semantic.building.modeler.configurationservice.model.enums.Side;
import semantic.building.modeler.prototype.graphics.primitives.AbstractPrimitive;
import semantic.building.modeler.prototype.service.EdgeManager;

/**
 * Klasse verwaltet Dachgewichte fuer Daecher mit fester Neigungszuweisung. Bei
 * solchen Daechern werden die Haupt- und Seitenneigung fest aus einer
 * Konfigurationsdatei geladen und nach dem Laden nicht mehr veraendert.
 * 
 * @author Patrick Gunia
 * 
 */

public class FixedRoofWeightConfiguration {

	/** Aktuell verwendeter Edge-Manager */
	protected EdgeManager mCurrentEdgeManager = null;

	/** Neigung der Hauptseiten */
	protected Float mStandardWeight = null;

	/** Neigung an den Nebenseiten */
	protected Float mSideWeight = null;

	/** Dachneigung der Hauptseiten nach dem Knick bei Mansardendaechern */
	protected Float mSecondStandardWeight = null;

	/** Dachneigung der Nebenseiten nach dem Knick */
	protected Float mSecondSideWeight = null;

	/** Dachkonfiguration */
	protected RoofConfiguration mRoofConf = null;

	/** Hoehe, auf der bei Mansardendaechern die Dachneigung geaendert wird */
	protected Integer mSlopeChangeHeight = null;

	// ------------------------------------------------------------------------------------------
	public FixedRoofWeightConfiguration() {

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Konstruktor mit Uebergabe des Konfigurationsdateinamens
	 * 
	 * @param mRoofConfig
	 *            Dachkonfigurationsobjekt
	 */
	public FixedRoofWeightConfiguration(final RoofConfiguration roofConfig) {

		mRoofConf = roofConfig;

		final RoofDescriptor roofDescriptor = roofConfig.getRoofDescriptor();
		mStandardWeight = roofDescriptor.getMainSlope()
				.getRandValueWithinRange();
		mSideWeight = roofDescriptor.getSideSlope().getRandValueWithinRange();

		if (roofConfig.getRoofDescriptorType().equals("MansardRoof")) {
			RoofDescriptorMansard mansardConf = (RoofDescriptorMansard) roofDescriptor;
			mSecondStandardWeight = mansardConf.getSecondSlopeMain()
					.getRandValueWithinRange();
			mSecondSideWeight = mansardConf.getSecondSlopeSide()
					.getRandValueWithinRange();
			mSlopeChangeHeight = mansardConf.getSlopeChangeHeight();
		}
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode berechnet aus den uebergebenen Indices einen Anfrageindex und
	 * greift anschliessend auf den Edge-Manager zu, um die Gewichte der
	 * zugehoerigen Kante zu bestimmen
	 */
	public Float getWeightByEdgeIndices(final Integer index1,
			final Integer index2) {
		assert mCurrentEdgeManager != null : "FEHLER: Es ist kein EdgeManager gesetzt!";
		String index = createOrderedIndex(index1, index2);
		return getWeightByEdgeIndex(index);
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode ermittelt ueber den EdgeManager alle Eltern einer Kante und
	 * testet ihre Ausrichtung. Basierend auf diesen Berechnungen wird dann ein
	 * Kantengewicht zurueckgegeben
	 */
	public Float getWeightByEdgeIndex(final String index) {
		assert mCurrentEdgeManager != null : "FEHLER: Es ist kein EdgeManager gesetzt!";

		final List<AbstractPrimitive> parents = mCurrentEdgeManager
				.getParentsForEdgeByIndex(index);
		AbstractPrimitive currentPrimitive = null;

		// durchlaufe nun alle Eltern und teste, ob es ein Quad mit Ausrichtung
		// LEFT oder RIGHT gibt
		Iterator<AbstractPrimitive> parentIter = parents.iterator();
		while (parentIter.hasNext()) {

			// hole das Parent-Objekt des Primitive, da Quads und nicht
			// Triangles gesucht werden
			currentPrimitive = (AbstractPrimitive) parentIter.next()
					.getParent();
			if (currentPrimitive instanceof semantic.building.modeler.prototype.graphics.primitives.AbstractQuad
					&& (currentPrimitive.getDirection().equals(Side.LEFT) || currentPrimitive
							.getDirection().equals(Side.RIGHT))) {
				return mSideWeight;
			}

		}
		return mStandardWeight;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the currentEdgeManager
	 */
	public EdgeManager getCurrentEdgeManager() {
		return mCurrentEdgeManager;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param currentEdgeManager
	 *            the currentEdgeManager to set
	 */
	public void setCurrentEdgeManager(final EdgeManager currentEdgeManager) {
		this.mCurrentEdgeManager = currentEdgeManager;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * erzeugt einen Index aus den Quellindices bei dem der erste Index immer
	 * kleiner ist als der zweite
	 */
	protected String createOrderedIndex(final Integer index1,
			final Integer index2) {
		String result = "";

		if (index1 > index2) {
			result = String.valueOf(index2) + String.valueOf(index1);

		} else {
			result = String.valueOf(index1) + String.valueOf(index2);
		}
		return result;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return
	 * @see configurationservice.model.RoofConfiguration#getmEqualWeightProbability()
	 */
	public Float getEqualWeightProbability() {
		return mRoofConf.getEqualWeightProbability();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return
	 * @see configurationservice.model.RoofConfiguration#getmRoofScaling()
	 */
	public Float getRoofScaling() {
		return mRoofConf.getRoofScaling();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mStandardWeight
	 */
	public Float getStandardWeight() {
		return mStandardWeight;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mSideWeight
	 */
	public Float getSideWeight() {
		return mSideWeight;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mRoofConf
	 */
	public RoofConfiguration getRoofConf() {
		return mRoofConf;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mSecondStandardWeight
	 */
	public Float getSecondStandardWeight() {
		return mSecondStandardWeight;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mSecondSideWeight
	 */
	public Float getSecondSideWeight() {
		return mSecondSideWeight;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return
	 * @see configurationservice.model.RoofConfiguration#getRoofDescriptorType()
	 */
	public String getRoofDescriptorType() {
		return mRoofConf.getRoofDescriptorType();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mSlopeChangeHeight
	 */
	public Integer getSlopeChangeHeight() {
		return mSlopeChangeHeight;
	}

	// ------------------------------------------------------------------------------------------

}
