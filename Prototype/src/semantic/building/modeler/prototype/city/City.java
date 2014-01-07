package semantic.building.modeler.prototype.city;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import semantic.building.modeler.configurationservice.controller.ConfigurationController;
import semantic.building.modeler.configurationservice.model.AbstractConfigurationObject;
import semantic.building.modeler.configurationservice.model.BuildingConfiguration;
import semantic.building.modeler.configurationservice.model.BuildingDescriptor;
import semantic.building.modeler.configurationservice.model.BuildingDoppelantentempelConfiguration;
import semantic.building.modeler.configurationservice.model.BuildingJugendstilConfiguration;
import semantic.building.modeler.configurationservice.model.CityConfiguration;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.prototype.building.AbstractBuilding;
import semantic.building.modeler.prototype.building.ArbitraryBuilding;
import semantic.building.modeler.prototype.building.JugendstilBuilding;
import semantic.building.modeler.prototype.building.temple.Doppelantentempel;
import semantic.building.modeler.prototype.graphics.complex.CompositeComplex;
import semantic.building.modeler.prototype.service.PrototypeHelper;

/**
 * Instanz dieser Klasse ist zustaendig fuer die Erzeugung einer Stadt basierend
 * aauf den eingegebenen XML-Konfigurationen
 * 
 * @author Patrick Gunia
 * 
 */

public class City {

	/** XML-Konfiguration, die die Struktur der Stadt beschreibt */
	private transient CityConfiguration mCityConfig = null;

	/** Logging-Instanz */
	private static Logger LOGGER = Logger.getLogger(City.class);

	/** Liste mit vorab berechneten Positionen der Gebaeude */
	private List<MyVector3f> mBuildingPositions = null;

	/** Basisvektor, der auf alle berechneten Positionen aufaddiert wird */
	private static MyVector3f mBaseVector = new MyVector3f(500.0f, 200.0f, 0.0f);

	// ------------------------------------------------------------------------------------------
	/**
	 * Default-Konstruktor
	 */
	public City() {

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode laedt die Konfigurationsdatei, die die Stadtkonstruktion
	 * beschreibt
	 * 
	 * @param path
	 *            Pfad zur Konfigurationsdatei
	 */
	public void loadConfiguration(final String path) {

		final ConfigurationController config = new ConfigurationController();
		final File file = new File(path);

		try {
			if (!file.exists() && file.getCanonicalFile() == null) {
				LOGGER.error("Konfiguration konnte nicht geladen werden: '"
						+ path + "'!");
				return;
			} else {
				mCityConfig = config.processCityConfiguration(file);
				LOGGER.info("Konfiguration erfolgreich geladen.");
			}
		} catch (IOException e) {
			LOGGER.error("Konfiguration konnte nicht geladen werden: '" + path
					+ "'!");
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erzeugt basierend auf der uebergebenen Stadtkonfiguration eine
	 * Menge von Gebaeuden
	 * 
	 * @param hierarchyRoot
	 *            Wurzel des Geometriebaumes
	 */
	public void createCity(final CompositeComplex hierarchyRoot) {
		if (mCityConfig == null) {
			LOGGER.error("Es wurde keine Konfiguration geladen!");
			return;
		} else {

			int numberOfBuildings = computeNumberOfBuildings();
			computeBuildingPositions(numberOfBuildings);

			final List<BuildingDescriptor> descriptors = mCityConfig
					.getBuildingDescriptors();

			AbstractConfigurationObject currentBuildingConf = null;
			AbstractBuilding currentBuilding = null;
			int numberOfBuildingsForCurrentType = -1;

			int totalNumberOfBuildingsToCreate = 0;
			int errors = 0;

			for (BuildingDescriptor curDescriptor : descriptors) {
				currentBuildingConf = curDescriptor.getBuilding();
				numberOfBuildingsForCurrentType = curDescriptor
						.getInstanceCount();
				final String currentType = currentBuildingConf.getType();
				LOGGER.debug("Erzeuge " + numberOfBuildingsForCurrentType
						+ " Gebaeude des Typs: " + currentType);

				totalNumberOfBuildingsToCreate += numberOfBuildingsForCurrentType;

				for (int i = 0; i < numberOfBuildingsForCurrentType; i++) {

					try {
						final MyVector3f nextPosition = mBuildingPositions
								.remove(0);
						if (currentBuildingConf.getType().equals("Building")) {
							currentBuilding = new ArbitraryBuilding(
									hierarchyRoot.getParent(), nextPosition,
									(BuildingConfiguration) currentBuildingConf);
						} else if (currentBuildingConf.getType().equals(
								"BuildingJugendstil")) {
							currentBuilding = new JugendstilBuilding(
									hierarchyRoot.getParent(),
									nextPosition,
									(BuildingJugendstilConfiguration) currentBuildingConf);
						} else if (currentBuildingConf.getType().equals(
								"BuildingDoppelantentempel")) {
							currentBuilding = new Doppelantentempel(
									hierarchyRoot.getParent(),
									nextPosition,
									(BuildingDoppelantentempelConfiguration) currentBuildingConf);
						} else {
							LOGGER.warn("Ungueltiger Gebaeudetyp: "
									+ currentType);
							continue;
						}
						currentBuilding.create();
						hierarchyRoot.addComplex(currentBuilding.getBuilding());
					} catch (Exception ex) {
						LOGGER.error("FEHLER: Bei der Gebäudeerzeugung ist ein Fehler aufgetretem!");
						LOGGER.error(PrototypeHelper.getInstance()
								.getStackTrace(ex));
						errors++;
					} catch (AssertionError ex) {
						LOGGER.error("FEHLER: Bei der Gebäudeerzeugung ist ein Fehler aufgetretem!");
						LOGGER.error(PrototypeHelper.getInstance()
								.getStackTrace(ex));
						errors++;
					}
				}
			}

			LOGGER.info("Von " + totalNumberOfBuildingsToCreate
					+ " zu erzeugenden Gebäuden wurden "
					+ (totalNumberOfBuildingsToCreate - errors)
					+ " korrekt erstellt. Insgesamt traten " + errors
					+ " Fehler auf!");

		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode bestimmt, wieviele Gebaeude gebaut werden sollen
	 * 
	 * @return Anzahl der insgesamt zu errichtenden Gebaeude
	 */
	private int computeNumberOfBuildings() {

		int numberOfBuildings = 0;
		List<BuildingDescriptor> descriptors = mCityConfig
				.getBuildingDescriptors();
		for (BuildingDescriptor cur : descriptors) {
			numberOfBuildings += cur.getInstanceCount();
		}
		return numberOfBuildings;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet die Positionen der zu erzeugenden Gebaeude. Dabei wird
	 * versucht, die Gebaeude in einem quadratischen Raster anzuordnen
	 * 
	 * @param numberOfBuildings
	 *            Gesamtanzahl der zu erzeugenden Gebaeude
	 */
	private void computeBuildingPositions(int numberOfBuildings) {

		// Aenderung pro Koordinatenkomponente
		int delta = 450;
		float y = 0.0f;

		// Wurzel ziehen, dadurch bekommt man die Anzahl der Positionen pro
		// Koordinate
		double squareRoot = Math.sqrt(numberOfBuildings);

		// aufrunden
		int positionsPerDirection = (int) Math.ceil(squareRoot);
		MyVector3f curPos = null;
		mBuildingPositions = new ArrayList<MyVector3f>(positionsPerDirection
				* positionsPerDirection);

		for (int i = 0; i < positionsPerDirection; i++) {
			for (int j = 0; j < positionsPerDirection; j++) {
				curPos = new MyVector3f(i * delta, y, j * delta);
				curPos.add(mBaseVector);
				mBuildingPositions.add(curPos);
			}
		}
	}

	// ------------------------------------------------------------------------------------------

}
