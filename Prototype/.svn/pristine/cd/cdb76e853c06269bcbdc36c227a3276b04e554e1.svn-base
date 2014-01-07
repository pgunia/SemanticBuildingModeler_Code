package semantic.building.modeler.prototype.building;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

import processing.core.PApplet;
import semantic.building.modeler.configurationservice.model.BuildingDimensionsConfiguration;
import semantic.building.modeler.configurationservice.model.BuildingJugendstilConfiguration;
import semantic.building.modeler.configurationservice.model.component.DoorComponentConfiguration;
import semantic.building.modeler.configurationservice.model.component.FasciaComponentConfiguration;
import semantic.building.modeler.configurationservice.model.component.WindowComponentConfiguration;
import semantic.building.modeler.configurationservice.model.component.WindowLedgeComponentConfiguration;
import semantic.building.modeler.configurationservice.model.enums.BuildingComponentType;
import semantic.building.modeler.configurationservice.model.enums.FloorPosition;
import semantic.building.modeler.configurationservice.model.enums.ReuseFloorEnum;
import semantic.building.modeler.math.MyPolygon;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.MyVectormath;
import semantic.building.modeler.math.Plane;
import semantic.building.modeler.math.Plane.CoordinatePlane;
import semantic.building.modeler.prototype.building.footprint.RectFootprint;
import semantic.building.modeler.prototype.service.ObjectPositioningService;

/**
 * Klasse steuert die Erzeugung von Jugenstil-Gebaeuden
 * 
 * @author Patrick Gunia
 * 
 */

public class JugendstilBuilding extends AbstractBuilding {

	public JugendstilBuilding(final PApplet applet, final MyVector3f position,
			final BuildingJugendstilConfiguration buildingConf) {
		super(applet, position, buildingConf);
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "Jugendstil";
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public void create() {

		final BuildingJugendstilConfiguration conf = (BuildingJugendstilConfiguration) mConf;
		final MyVectormath mathHelper = MyVectormath.getInstance();

		// Gebaeudeausdehnungen berechnen
		computeBuildingDimensions();

		boolean modifyGroundFootprint = mathHelper.decide(conf
				.getModifyGroundFootprintProbability());
		LOGGER.info("Modify Ground Footprint: " + modifyGroundFootprint);

		boolean modifyIntermediateFootprint = mathHelper.decide(conf
				.getModifyIntermediateFootprintProbability());
		LOGGER.info("Modify Intermediate Footprint: "
				+ modifyIntermediateFootprint);

		boolean modifyTopFootprint = mathHelper.decide(conf
				.getModifyTopFootprintProbability());
		LOGGER.info("Modify Top Footprint: " + modifyTopFootprint);

		boolean useGroundFloorFootprintForTopFloor = mathHelper.decide(conf
				.getUseGroundFloorFootprintForTopFloorProbability());
		LOGGER.info("Use Ground Footprint for Top Floor: "
				+ useGroundFloorFootprintForTopFloor);

		// erzeuge einen rechteckigen Basisgrundriss in den Ausdehnungen, die in
		// der Konfiguration festgelegt sind
		mOuterFootprint = new RectFootprint(new Plane(CoordinatePlane.XZ),
				mDimensions.getLength(), mDimensions.getWidth());
		MyPolygon baseFootprint = mOuterFootprint.getFootprints().get(0);
		float currentHeight = 0.0f;

		// Erdgeschoss
		MyPolygon groundFootprint = null;
		if (modifyGroundFootprint) {
			groundFootprint = modifyFootprint(baseFootprint,
					conf.getObjectplacement());
		} else {
			groundFootprint = baseFootprint;
			baseFootprint = null;
		}

		mReuseFloorMap.put(ReuseFloorEnum.GROUND, groundFootprint);
		mReuseFloorMap.put(ReuseFloorEnum.PREVIOUS, groundFootprint);

		// erzeuge das Erdgeschoos
		currentHeight = createFloor(groundFootprint, currentHeight, mDimensions
				.getFloorHeights().get(FloorPosition.GROUND),
				FloorPosition.GROUND);

		// Zwischengeschosse erzeugen
		MyPolygon intermediateFootprint = null;
		if (modifyIntermediateFootprint) {
			intermediateFootprint = modifyFootprint(groundFootprint,
					conf.getObjectplacement());
		} else {
			intermediateFootprint = groundFootprint;
			groundFootprint = null;
		}
		mReuseFloorMap.put(ReuseFloorEnum.PREVIOUS, intermediateFootprint);

		// erzeuge nun eine Anzahl von Intermediate-Footprint-Instanzen
		// die Anzahl ergibt sich aus der Gesamstockwerksanzahl
		int numberOfIntermediates = mDimensions.getNumberOfFloors() - 2;

		for (int i = 0; i < numberOfIntermediates; i++) {
			currentHeight = createFloor(
					intermediateFootprint,
					currentHeight,
					mDimensions.getFloorHeights().get(
							FloorPosition.INTERMEDIATE),
					FloorPosition.INTERMEDIATE);

		}

		// Top-Floor
		// Soll der Erdgeschoss-Grundriss wiederverwendet werden?
		MyPolygon topFloorFootprint = null;
		if (useGroundFloorFootprintForTopFloor) {
			topFloorFootprint = mReuseFloorMap.get(ReuseFloorEnum.GROUND);
		} else {
			if (modifyTopFootprint) {
				topFloorFootprint = modifyFootprint(intermediateFootprint,
						conf.getObjectplacement());
			} else {
				topFloorFootprint = intermediateFootprint;
				intermediateFootprint = null;
			}
		}

		createFloor(topFloorFootprint, currentHeight, mDimensions
				.getFloorHeights().get(FloorPosition.TOP), FloorPosition.TOP);

		final ObjectPositioningService posService = ObjectPositioningService
				.getInstance();

		posService.addDoor(mBuilding, (DoorComponentConfiguration) conf
				.getComponentConfigurationByType(BuildingComponentType.DOOR));
		posService.addWindows(mBuilding, (WindowComponentConfiguration) conf
				.getComponentConfigurationByType(BuildingComponentType.WINDOW));
		posService
				.addWindowLedges(
						mBuilding,
						(WindowLedgeComponentConfiguration) conf
								.getComponentConfigurationByType(BuildingComponentType.WINDOWLEDGE));

		// Fascia
		if (conf.getComponentConfigurationByType(BuildingComponentType.FASCIA) != null) {
			posService
					.addFascia(
							mBuilding,
							(FasciaComponentConfiguration) conf
									.getComponentConfigurationByType(BuildingComponentType.FASCIA));
		}

		finalizeBuilding();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet basierend auf den Konfigurationsobjekten saemtliche
	 * Gebaeudeausdehnungen, Stockwerkshoehen etc.
	 */
	protected void computeBuildingDimensions() {

		BuildingJugendstilConfiguration conf = (BuildingJugendstilConfiguration) mConf;

		Iterator<FloorPosition> posIter = conf.getFloorHeights().keySet()
				.iterator();
		FloorPosition curPos = null;
		Float curFloorHeight = null;

		Map<FloorPosition, Float> floorHeights = new EnumMap<FloorPosition, Float>(
				FloorPosition.class);
		while (posIter.hasNext()) {
			curPos = posIter.next();
			curFloorHeight = conf.getFloorHeights().get(curPos)
					.getRandValueWithinRange();
			floorHeights.put(curPos, curFloorHeight);
		}

		final BuildingDimensionsConfiguration dimensions = conf.getDimensions();
		assert dimensions != null : "FEHLER: Es wurden keine Gebaeudedimensionen gefunden!";
		final Float buildingHeight = dimensions.getHeight()
				.getRandValueWithinRange();

		mDimensions = new BuildingDimensions(dimensions.getLength()
				.getRandValueWithinRange(), dimensions.getWidth()
				.getRandValueWithinRange(), buildingHeight, floorHeights,
				getNumberOfFloors(buildingHeight, floorHeights));

	}
	// ------------------------------------------------------------------------------------------

}
