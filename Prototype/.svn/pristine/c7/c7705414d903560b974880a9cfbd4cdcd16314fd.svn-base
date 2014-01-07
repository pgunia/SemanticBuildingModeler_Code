package semantic.building.modeler.prototype.roof.configuration;

import java.util.Random;

import semantic.building.modeler.configurationservice.model.RoofConfiguration;

/**
 * 
 * @author Patrick Gunia Klasse dient der Vergabe von Kantengewichten bei der
 *         Dachstrukturerzeugung. Ziel ist die Auslagerung der Gewichtungslogik
 *         aus den eigentlichen Berechnungsstrukturen.
 */

public class RandomRoofWeightConfiguration extends FixedRoofWeightConfiguration {

	/**
	 * Zufallsgenerator, wird verwendet, um die Kantengewichte zufallsgesteuert
	 * zu setzen
	 */
	private Random mRandom = null;

	/** Untere Grenze Gewichtzuweisung fuer Seitenkanten */
	private Float mWeightSideLowerBorder;

	/** Obere Grenze Gewichtzuweisung fuer Seitenkanten */
	private Float mWeightSideUpperBorder;

	/** Untere Grenze Gewichtzuweisung fuer Standardkanten */
	private Float mWeightStandardLowerBorder;

	/** Obere Grenze Gewichtzuweisung fuer Standardkanten */
	private Float mWeightStandardUpperBorder;

	/**
	 * Wahrscheinlichkeit, alle Kanten eines Grunrisses mit dem gleichen Gewicht
	 * auszuzeichnen
	 */
	private Float mEqualWeightProbability;

	// ------------------------------------------------------------------------------------------

	/**
	 * Konstruktor mit Uebergabe des Konfigurationsdateinamens
	 * 
	 * @param weightConfigFilename
	 *            Dateiname der zu verwendenden Gewichtskonfiguration
	 */
	public RandomRoofWeightConfiguration(final RoofConfiguration roofConfig) {
		super(roofConfig);
		initializeEdgeWeights();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Leerer Default-Konstruktor
	 */
	public RandomRoofWeightConfiguration() {
		super();
		// initializeEdgeWeights();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Zufallsbasierte Initialisierung der Kantengewichte, Unterscheidung nach
	 * "Standardkanten" und "Seitenkanten"
	 */
	public void initializeEdgeWeights() {

		boolean useIdenticalWeights = false;

		if (mRandom == null)
			mRandom = new Random();

		// auch Gleichgewichtungen sollen vorkommen
		Float randFloat = mRandom.nextFloat();

		// Wahrscheinichkeit, identische Gewichte zu verwenden
		if (randFloat < mEqualWeightProbability)
			useIdenticalWeights = true;

		// Range bestimmen
		Float range = mWeightSideUpperBorder - mWeightSideLowerBorder;
		mSideWeight = mRandom.nextFloat();

		// auf Range abbilden
		mSideWeight *= range;
		mSideWeight += mWeightSideLowerBorder;

		// gleiche Logik fuer Standardgewichte
		range = mWeightStandardUpperBorder - mWeightStandardLowerBorder;
		mStandardWeight = mRandom.nextFloat();
		mStandardWeight *= range;
		mStandardWeight += mWeightStandardLowerBorder;

		// wenn gleiche Gewichtungen fuer alle Kanten verwendet werden sollen,
		// verwende das Standardgewicht
		if (useIdenticalWeights)
			mSideWeight = mStandardWeight;

	}
	// ------------------------------------------------------------------------------------------

}
