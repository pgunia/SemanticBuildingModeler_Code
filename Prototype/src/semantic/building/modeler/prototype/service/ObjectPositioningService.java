package semantic.building.modeler.prototype.service;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import processing.core.PApplet;
import semantic.building.modeler.configurationservice.model.SystemConfiguration;
import semantic.building.modeler.configurationservice.model.component.ComponentModelSource;
import semantic.building.modeler.configurationservice.model.component.DoorComponentConfiguration;
import semantic.building.modeler.configurationservice.model.component.EdgeAdditionComponentConfiguration;
import semantic.building.modeler.configurationservice.model.component.FasciaComponentConfiguration;
import semantic.building.modeler.configurationservice.model.component.MouldingComponentConfiguration;
import semantic.building.modeler.configurationservice.model.component.PillarComponentConfiguration;
import semantic.building.modeler.configurationservice.model.component.RoundStairsComponentConfiguration;
import semantic.building.modeler.configurationservice.model.component.WindowComponentConfiguration;
import semantic.building.modeler.configurationservice.model.component.WindowLedgeComponentConfiguration;
import semantic.building.modeler.configurationservice.model.enums.FloorPosition;
import semantic.building.modeler.configurationservice.model.enums.ModelCategory;
import semantic.building.modeler.configurationservice.model.enums.Side;
import semantic.building.modeler.configurationservice.model.enums.VerticalAlignment;
import semantic.building.modeler.math.MyPolygon;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.MyVectormath;
import semantic.building.modeler.math.Plane;
import semantic.building.modeler.math.Ray;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.prototype.algorithm.BoundingBoxCreator;
import semantic.building.modeler.prototype.building.component.AbstractBuildingComponent;
import semantic.building.modeler.prototype.building.component.EdgeAddition;
import semantic.building.modeler.prototype.building.component.Fascia;
import semantic.building.modeler.prototype.building.component.Moulding;
import semantic.building.modeler.prototype.building.component.RoundStairs;
import semantic.building.modeler.prototype.enums.HorizontalAlignment;
import semantic.building.modeler.prototype.graphics.complex.AABB;
import semantic.building.modeler.prototype.graphics.complex.AbstractComplex;
import semantic.building.modeler.prototype.graphics.complex.BoundingBox;
import semantic.building.modeler.prototype.graphics.complex.BuildingComplex;
import semantic.building.modeler.prototype.graphics.complex.FloorComplex;
import semantic.building.modeler.prototype.graphics.complex.ImportedComplex;
import semantic.building.modeler.prototype.graphics.complex.IndoorFloorComplex;
import semantic.building.modeler.prototype.graphics.complex.OBB;
import semantic.building.modeler.prototype.graphics.primitives.AbstractQuad;
import semantic.building.modeler.prototype.graphics.primitives.PolygonalQuad;
import semantic.building.modeler.prototype.importer.AbstractModelImport;
import semantic.building.modeler.prototype.importer.ObjImport;

/**
 * 
 * @author Patrick Gunia Klasse dient als Service fuer die Positionierung von
 *         Objekten in Building-Instanzen und vereint die dafuer erforderliche
 *         Logik
 */

public class ObjectPositioningService {

	/** Drawing-Context */
	private transient PApplet mParentApplet = null;

	/** Singleton-Instanz */
	private static ObjectPositioningService mInstance = null;

	/** Logging-Instanz */
	private static Logger LOGGER = Logger
			.getLogger(ObjectPositioningService.class);

	/** Instanz der Mathebibliothek fuer Vektorberechnungen */
	private MyVectormath mMathHelper = MyVectormath.getInstance();

	/**
	 * Map speichert fuer jede in der Konfiguration gelistete Kategorie die
	 * Anzahl von Models, die fuer diese Kategorie zur Verfuegung stehen
	 */
	private Map<String, Integer> mModelCategoryCount = null;

	/** Map speichert Key-Dateiname-Paare */
	private Map<String, String> mModelFilenameConnection = null;

	/** Vorderseite der importierten Objekte */
	private MyVector3f mObjectNormal = new MyVector3f(0.0f, 0.0f, 1.0f);

	/** Flag gibt an, ob der Service bereits initialisiert wurde */
	private transient boolean mInitialized = false;

	/**
	 * Mapstruktur enthaelt eine Standardzuordnung von Normalenvektoren zu den
	 * Richtungs-Enums
	 */
	private transient Map<MyVector3f, Side> mNormalToDirectionMap = null;

	/** Zufallsgenerator */
	final Random mRandom = new Random();

	// ------------------------------------------------------------------------------------------
	/**
	 * Singleton-Getter
	 * 
	 * @return Singleton-Instanz
	 */
	public static ObjectPositioningService getInstance() {
		if (mInstance == null) {
			mInstance = new ObjectPositioningService();
		}
		return mInstance;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Filterklasse fuer Modeldateien, akzeptiert werden nur Dateien mit
	 * ".obj"-Endung
	 */
	private class ObjFileFilter implements FilenameFilter {

		@Override
		public boolean accept(File arg0, String arg1) {
			return arg1.toLowerCase().endsWith(".obj");
		}
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Initialisierungsroutine, laedt die Dateinamen aller vorhandenen Models
	 * und speichert sie in einer Mapstruktur
	 */
	public void init(final PApplet applet, final SystemConfiguration sysConf) {
		mParentApplet = applet;

		// baue die Normal-To-Direction-Map auf
		mNormalToDirectionMap = new HashMap<MyVector3f, Side>(6);

		mNormalToDirectionMap.put(new MyVector3f(0.0f, -1.0f, 0.0f), Side.TOP);
		mNormalToDirectionMap
				.put(new MyVector3f(0.0f, 1.0f, 0.0f), Side.BOTTOM);
		mNormalToDirectionMap.put(new MyVector3f(-1.0f, 0.0f, 0.0f), Side.LEFT);
		mNormalToDirectionMap.put(new MyVector3f(1.0f, 0.0f, 0.0f), Side.RIGHT);
		mNormalToDirectionMap.put(new MyVector3f(0.0f, 0.0f, 1.0f), Side.FRONT);
		mNormalToDirectionMap.put(new MyVector3f(0.0f, 0.0f, -1.0f), Side.BACK);

		final String modelFolderPath = sysConf.getModelPath();
		final List<String> splittedCategories = new ArrayList<String>(
				sysConf.getSuppotedModelCategories());

		// sortiere die uebergebene Stringliste anhand der Laenge der einzelnen
		// Elemente => dadurch soll bei Modelkategorien mit gleichem Anfang die
		// "laengere" Kategorie bevorzugt werden
		PrototypeHelper.getInstance().sortStringsByLength(splittedCategories);

		final File modelFolder = new File(modelFolderPath);
		if (!modelFolder.exists() || !modelFolder.isDirectory()) {
			LOGGER.error("FEHLER: Ungueltiger Pfad zum Model-Verzeichnis! Pfad: "
					+ modelFolderPath);
		}

		// lies die Dateinamen aller Dateien im Model-Verzeichnis
		final File[] modelFiles = modelFolder.listFiles(new ObjFileFilter());
		final Map<String, List<String>> categoryToFilenames = new HashMap<String, List<String>>(
				splittedCategories.size());

		String currentFilename;
		List<String> categoryFilenames = null;

		// fuege die vollstaendigen Pfade zur Map hinzu, je Kategorie wird eine
		// Liste mit allen Dateinamen erzeugt
		for (File currentFile : modelFiles) {
			currentFilename = currentFile.getAbsolutePath();
			for (String currentCategory : splittedCategories) {
				if (currentFilename.contains(currentCategory)) {
					categoryFilenames = categoryToFilenames
							.get(currentCategory);
					if (categoryFilenames == null) {
						categoryFilenames = new ArrayList<String>();
					}
					categoryFilenames.add(currentFilename);
					categoryToFilenames.put(currentCategory, categoryFilenames);
					break;
				}
			}
		}

		// erzeuge nun IDs fuer die Modells bestehend aus Kategorie und
		// fortlaufender Nummer
		// ausserdem eine Map, die die Anzahl von Modellen pro Kategorie
		// speichert
		mModelCategoryCount = new HashMap<String, Integer>(
				splittedCategories.size());
		mModelFilenameConnection = new HashMap<String, String>(
				modelFiles.length);

		String id = null;
		for (String currentKey : categoryToFilenames.keySet()) {
			categoryFilenames = categoryToFilenames.get(currentKey);

			mModelCategoryCount.put(currentKey, categoryFilenames.size());

			// erzeuge nun Kategorie + Nummer-Paare
			for (int i = 0; i < categoryFilenames.size(); i++) {
				id = currentKey + i;
				LOGGER.debug("ID: " + id + " Pfad: " + categoryFilenames.get(i));
				mModelFilenameConnection.put(id, categoryFilenames.get(i));
			}
		}

		// Flag setzen
		mInitialized = true;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode waehlt zufallsbasiert ein Model aus einer Liste vorhandenener
	 * Models aus, laedt dieses und liefert es zurueck. Die uebergebene
	 * Kategorie definiert dabei, um was fuer eine Art von Model es sich handeln
	 * soll (bsw. Fenster)
	 * 
	 * @param category
	 *            Model-Kategorie, aus der das geladene Model stammen soll
	 * @return Geladenes Model
	 */
	public AbstractComplex getModelByCategory(final ModelCategory category) {

		assert mInitialized : "FEHLER: Der Objectpositioningservice wurde nicht korrekt initialisiert!";

		Integer numberOfModelsInCategory = mModelCategoryCount.get(category
				.toString());
		assert numberOfModelsInCategory != null && numberOfModelsInCategory > 0 : "Es existieren keine Models fuer die angefragte Kategorie: "
				+ category;

		// waehle zufallsbasiert eine Textur aus der Kategorie

		int randomID = mRandom.nextInt(numberOfModelsInCategory);

		// pruefe, ob diese Textur bereits geladen wurde => Zugriff auf HashMap
		// ist Kategorie + Index
		String key = category.toString() + randomID;

		String path = mModelFilenameConnection.get(key);
		LOGGER.debug("Lade Model: " + path);

		// lade das Model von der Uebergabelocation
		final AbstractComplex importedModel = createObjectFrom3dModel(path);

		assert importedModel != null : "FEHLER: Model '" + path
				+ "' konnte nicht geladen werden!";
		return importedModel;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode laedt ein 3D-Objekt aus einer Datei und gibt dieses als
	 * ImportedComplex an den Aufrufer zurueck. Ist konzeptuell eine
	 * Delegate-Methode, die den Aufruf an den ObjectCreator weiterleitet
	 * 
	 * @param sourceFile
	 *            3D-Datei aus der das Objekt geladen werden soll
	 * @return ImportedComplex-Instanz, die die Geometrie des geladenen Objekts
	 *         enthaelt
	 */
	public AbstractComplex getModelFromFile(final File sourceFile) {
		assert mInitialized : "FEHLER: Der Objectpositioningservice wurde nicht korrekt initialisiert!";
		return createObjectFrom3dModel(sourceFile.getAbsolutePath());
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode fuegt Fenster zum uebergebenen Gebauede hinzu, die zunaechst als
	 * 3d-Modelle geladen und anschliessend innerhalb der Stockwerke
	 * positioniert werden.
	 * 
	 * @param building
	 *            Gebauede, innerhalb dessen Fenster positioniert werden sollen
	 * @param windowConf
	 *            Konfigurationsobjekt fuer Fensterkomponenten
	 * @param model
	 *            Sollte bereits vorab ein Model geladen worden sein, kann
	 *            dieses ueber diesen Parameter weiterverwendet werden
	 */
	public void addWindows(final BuildingComplex building,
			final WindowComponentConfiguration windowConf) {

		assert mInitialized : "FEHLER: Der Objectpositioningservice wurde nicht korrekt initialisiert!";

		ModelCategory windowModel = null;
		if (windowConf.getComponentModel().isCategory()) {
			windowModel = windowConf.getComponentModel().getModelCategory();
		} else {
			windowModel = ModelCategory.Window;
		}

		final AbstractComplex window = getModelFromComponentSource(windowConf
				.getComponentModel());

		final List<FloorComplex> floors = building.getFloors();
		final List<AbstractComplex> positionedObjects = new ArrayList<AbstractComplex>();

		// berechne die Positionen aller zu setzenden Fenster
		for(FloorComplex currentFloor : floors) {
			// ueberspringe Zwischengeschosse
			if (currentFloor.getType().equals("intermediatefloor"))
				continue;

			positionedObjects.addAll(computePositionsForWindows(currentFloor,
					Side.BACK, window, windowModel, windowConf));

			positionedObjects.addAll(computePositionsForWindows(currentFloor,
					Side.RIGHT, window, windowModel, windowConf));

			positionedObjects.addAll(computePositionsForWindows(currentFloor,
					Side.LEFT, window, windowModel, windowConf));

			positionedObjects.addAll(computePositionsForWindows(currentFloor,
					Side.FRONT, window, windowModel, windowConf));

			positionedObjects.addAll(computePositionsForWindows(currentFloor,
					Side.SIDE, window, windowModel, windowConf));
		}

		LOGGER.debug("Insgesamt werden " + positionedObjects.size()
				+ " Fenster zum Gebaeude hinzugefuegt");

		// fuege nun alle berechneten Objekte zum eigentlichen Gebaeude hinzu
		for(AbstractComplex current : positionedObjects) {
			building.addComplex(current);
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode positioniert Objekte basierend auf den Einstellungen des
	 * uebergebenen Konfigurationsobjekts
	 * 
	 * @param positionConfig
	 *            Konfigurationsobjekt, das alle benoetigten Informationen fuer
	 *            die Positionierung enthaelt
	 */
	public void computePositionsForObjects(final PositionConfig positionConfig) {
		assert mInitialized : "FEHLER: Der Objectpositioningservice wurde nicht korrekt initialisiert!";

		final AbstractComplex model = getModelByCategory(positionConfig
				.getModelCategory());
		model.unregister();

		BuildingComplex building = positionConfig.getBuilding();
		List<FloorComplex> floors = new ArrayList<FloorComplex>();

		List<FloorPosition> floorPositions = positionConfig.getFloorPositions();

		// wenn ueber
		if (floorPositions.size() > 0) {
			for (int i = 0; i < floorPositions.size(); i++)
				floors.addAll(building.getFloorsByPosition(floorPositions
						.get(i)));

		} else {
			int floorIndex = positionConfig.getFloorIndex();
			floors.add(building.getFloorByPositionIndex(floorIndex));
		}

		List<AbstractComplex> result = new ArrayList<AbstractComplex>();

		LOGGER.debug("Anzahl gefundener Stockwerke: " + floors.size());

		for (int i = 0; i < floors.size(); i++) {
			result.addAll(computePositionsForObjectsInFloor(positionConfig,
					floors.get(i), model));
		}

		// alle positionierten Elemente zum Gebaeude hinzufuegen
		for (int i = 0; i < result.size(); i++)
			positionConfig.getBuilding().addComplex(result.get(i));
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode positioniert Kopien des uebergebenen Objekts innerhalb des
	 * uebergebenen Floors und verwendet dafuer die Parameter, die innerhalb der
	 * Konfigurationsdatei gegeben sind
	 * 
	 * @param config
	 *            Instanz der Konfigurationsdatei, die alle relevanten Parameter
	 *            enthaelt
	 * @param floor
	 *            Stockwerk innerhalb dessen die Objekte positioniert werden
	 *            sollen
	 * @param baseObject
	 *            Zu positionierendes Objekt
	 * @return Liste mit den positionierten Objekten
	 */
	private List<AbstractComplex> computePositionsForObjectsInFloor(
			PositionConfig config, FloorComplex floor,
			AbstractComplex baseObject) {
		assert mInitialized : "FEHLER: Der Objectpositioningservice wurde nicht korrekt initialisiert!";
		List<AbstractComplex> result = new ArrayList<AbstractComplex>();

		List<Side> quadSides = config.getSides();
		List<AbstractQuad> quads = new ArrayList<AbstractQuad>();
		for (int i = 0; i < quadSides.size(); i++)
			quads.addAll(floor.getAllOutsideQuadsWithDirection(quadSides.get(i)));

		// Objekt fuer den aktuellen Floor skalieren
		AbstractComplex scaledComplex = scaleObjectByHeight(floor.getHeight(),
				baseObject, config.getLowerBorderObjectToQuadRatio(),
				config.getUpperBorderObjectToQuadRatio());
		scaledComplex.unregister();
		LOGGER.debug("Anzahl gefundener Quads: " + quads.size());

		// Footprint beschreibt Bodenflaeche aber mit TOP-Quad-Ausrichtung
		MyPolygon floorPoly = floor.getFootprint();
		MyVector3f heightTranslation = floorPoly.getNormal();

		// verschiebe die Objekte in die Mitte des Quads => kann fuer andere
		// Zwecke noch angepasst werden
		heightTranslation.scale(floor.getHeight()
				* config.getRelativeFloorHeightPosition());

		List<AbstractComplex> tempBuffer = new ArrayList<AbstractComplex>();
		AbstractComplex currentComplex = null;
		AbstractQuad currentQuad = null;

		for (int i = 0; i < quads.size(); i++) {
			currentQuad = quads.get(i);
			tempBuffer.addAll(computePositionsForObjectsInQuad(config,
					scaledComplex, currentQuad, floor));

			// alle verschieben bzgl. Hoehendimension
			for (int j = 0; j < tempBuffer.size(); j++)
				tempBuffer.get(j).translate(heightTranslation);

			// Ausschneiden
			AbstractQuad[] holes = (AbstractQuad[]) currentQuad.getHoles()
					.toArray(new AbstractQuad[currentQuad.getHoles().size()]);
			for (int j = 0; j < tempBuffer.size(); j++) {
				currentComplex = tempBuffer.get(j);

				// nur adden, wenn das Clipping erfolgreich war
				if (clipQuadByImported(currentComplex, currentQuad, holes,
						config.getModelCategory()))
					result.add(currentComplex);
			}

			tempBuffer.clear();
		}
		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode fuehrt die Positionierungsberechnungen auf Quadebene durch
	 * 
	 * @param config
	 *            Konfiguration fuer die Positionierung
	 * @param object
	 *            Zu positionierendes 3d-Objekt
	 * @param quad
	 *            Quad, auf dem das Objekt positioniert wird
	 * @param floor
	 *            Stockwerk, zu dem das Quad gehoert
	 * @return Liste mit den skalierten und positionierten Objekten
	 */
	private List<AbstractComplex> computePositionsForObjectsInQuad(
			PositionConfig config, AbstractComplex object, AbstractQuad quad,
			FloorComplex floor) {
		assert mInitialized : "FEHLER: Der Objectpositioningservice wurde nicht korrekt initialisiert!";
		List<AbstractComplex> result = new ArrayList<AbstractComplex>();

		// bestimme die Kante, die sich das Quad mit dem Grundriss des Floors
		// teilt
		MyPolygon floorFootprint = floor.getFootprint();
		MyPolygon quadPoly = quad.getPolygon();
		Ray sharedEdge = getSharedEdge(quadPoly, floorFootprint);
		assert sharedEdge != null : "FEHLER: Es konnte keine gemeinsame Kante zwischen den Quads ermittelt werden!";

		// richte das Objekt an der Kante, bzw. dem Face aus
		AbstractComplex alignedObject = rotateObject(object, quad.getNormal(),
				config.getObjectFront());
		alignedObject.unregister();

		AbstractComplex positionObject = null;
		BoundingBox alignedObjectBB = alignedObject.getBB();

		// wenn Objekte auf den Ecken des Quads positioniert werden sollen, tue
		// dies
		if (config.getOnCorners()) {

			Vertex3d start = sharedEdge.getStartVertex();
			Vertex3d end = sharedEdge.getEndVertex();

			// Verschiebung des Objekts entlang der gemeinsamen Kante
			MyVector3f startTranslation = sharedEdge.getDirection();
			startTranslation.normalize();
			float scale = alignedObjectBB.getLength() / 2.0f;
			startTranslation.scale(scale);

			MyVector3f position = new MyVector3f();
			position.add(start.getPosition(), startTranslation);

			// positioniert
			positionObject = alignedObject.clone();
			positionObject.translate(position);
			result.add(positionObject);

			MyVector3f endTranslation = sharedEdge.getDirection();
			endTranslation.scale(-1.0f);
			endTranslation.normalize();
			endTranslation.scale(alignedObjectBB.getLength() / 2.0f);

			// das gleiche fuer das Endvertex
			position = new MyVector3f();
			position.add(end.getPosition(), endTranslation);

			// positioniert
			positionObject = alignedObject.clone();
			positionObject.translate(position);
			result.add(positionObject);

			// aendere die gemeinsame Kante, damit einheitlich weitergerechnet
			// werden kann => start und end sind dann die Grenzen der
			// positionierten Objekte
			start.getPositionPtr().add(startTranslation);
			end.getPositionPtr().add(endTranslation);
			sharedEdge = new Ray(start, end);
		}

		// wenn Objekte auf der Kante positioniert werden sollen, berechne die
		// Positionen
		if (config.getOnEdges()) {

			// berechne die Anzahl an Positionierungen entlang der Kante
			float objectLength = alignedObjectBB.getLength();

			// Distanz zwischen zu positionierenden Objekten
			float relativeDistance = config.getDistance();
			float absoluteDistance = relativeDistance * sharedEdge.getLength();

			float distancePerPositioning = objectLength + absoluteDistance;

			// Bereich, in dem positioniert wird, entspricht der Kantenlaenge -
			// dem doppelten Abstand zu den Objekten
			float totalPositioningLength = sharedEdge.getLength() - 2
					* absoluteDistance;

			// abgerundete Anzahl an Positionierungen
			int numberOfPositionings = (int) Math.floor(totalPositioningLength
					/ distancePerPositioning);

			// darueber nun die tatsaechliche Verschiebungsdistanz bestimmen
			float finalDistance = totalPositioningLength / numberOfPositionings;

			MyVector3f translation = sharedEdge.getDirection();
			translation.normalize();
			translation.scale(finalDistance);

			// vom Startvertex einmal die vollstaendige Verschiebung aufaddieren
			MyVector3f startVertTranslation = sharedEdge.getDirection();
			startVertTranslation.normalize();
			startVertTranslation.scale(finalDistance);

			MyVector3f currentPos = sharedEdge.getStart();
			currentPos.add(startVertTranslation);

			for (int i = 0; i < numberOfPositionings; i++) {

				// Kopie erstellen und an die Zielposition verschieben
				positionObject = alignedObject.clone();
				positionObject.translate(currentPos);

				result.add(positionObject);
				currentPos.add(translation);
			}
		}

		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode bestimmt eine Kante, die in beiden Polygonen vorkommt
	 * 
	 * @param poly1
	 *            Eingabepolygon
	 * @param poly2
	 *            Eingabepolygon
	 * @return Gemeinsame Kante, falls eine solche vorhanden ist, null sonst
	 */
	public Ray getSharedEdge(MyPolygon poly1, MyPolygon poly2) {
		assert mInitialized : "FEHLER: Der Objectpositioningservice wurde nicht korrekt initialisiert!";
		List<Vertex3d> firstPolyVerts = poly1.getVertices();
		List<Vertex3d> secondPolyVerts = poly2.getVertices();
		Vertex3d currentVertFirst = null, nextVertFirst = null, firstVertSecond = null, nextVertSecond = null, previousVertSecond = null;
		;

		// suche nach 2 aufeinanderfolgenden Vertices, die in beiden Polygonen
		// vorkommen
		for (int i = 0; i < firstPolyVerts.size(); i++) {
			currentVertFirst = firstPolyVerts.get(i);
			if (i < firstPolyVerts.size() - 1)
				nextVertFirst = firstPolyVerts.get(i + 1);
			else
				nextVertFirst = firstPolyVerts.get(0);

			for (int k = 0; k < secondPolyVerts.size(); k++) {
				firstVertSecond = secondPolyVerts.get(k);
				if (!currentVertFirst.equals(firstVertSecond))
					continue;
				else {
					if (k < secondPolyVerts.size() - 1)
						nextVertSecond = secondPolyVerts.get(k + 1);
					else
						nextVertSecond = secondPolyVerts.get(0);
					// die Abfolge der Vertices ist nicht festgelegt, darum
					// koennen die Vertices auch andersherum vorliegen
					if (!nextVertSecond.equals(nextVertFirst)) {
						// teste nun zu guter Letzt noch den Vorgaenger des
						// aktuellen Vertex
						if (k > 0)
							previousVertSecond = secondPolyVerts.get(k - 1);
						else
							previousVertSecond = secondPolyVerts
									.get(secondPolyVerts.size() - 1);
						if (previousVertSecond.equals(nextVertFirst))
							return new Ray(currentVertFirst, nextVertFirst);
						else
							continue;
					} else
						return new Ray(currentVertFirst, nextVertFirst);
				}
			}
		}
		// keine gemeinsame Kante gefunden
		return null;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode positioniert Saeulen innerhalb des uebergebenen Stockwerks
	 * 
	 * @param building
	 *            Gebaeude, innerhalb dessen der Floor sich befindet
	 * @param pillarConf
	 *            Konfigurationsobjekt fuer die Saeulenpositionierung
	 */
	public void addPillars(final BuildingComplex building,
			final PillarComponentConfiguration pillarConf) {

		assert mInitialized : "FEHLER: Der Objectpositioningservice wurde nicht korrekt initialisiert!";
		final List<FloorComplex> floors = new ArrayList<FloorComplex>();

		final FloorPosition pos = pillarConf.getFloorPosition();
		if (pos.equals(FloorPosition.ALL)) {
			floors.addAll(building.getFloors());
		} else
			floors.addAll(building.getFloorsByPosition(pos));

		FloorComplex currentFloor = null;
		AbstractQuad bottom = null;
		final List<AbstractComplex> pillars = new ArrayList<AbstractComplex>();

		// Saeule laden
		final AbstractComplex pillar = getModelFromComponentSource(pillarConf
				.getComponentModel());

		for (int i = 0; i < floors.size(); i++) {
			currentFloor = floors.get(i);
			bottom = currentFloor.getQuadByDirection(Side.BOTTOM);
			if (bottom != null) {
				if (pillarConf.isAbsolutePillarCount()) {
					pillars.addAll(computePositionsForPillars(bottom, pillar,
							pillarConf.getNumberOfPillarsLongside(),
							pillarConf.getNumberOfPillarsBroadside()));
				} else {
					pillars.addAll(computePositionsForPillars(bottom, pillar,
							pillarConf.getPillarDistanceRatio()));
				}
			}
		}

		for (int i = 0; i < pillars.size(); i++)
			building.addComplex(pillars.get(i));
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode positioniert Saeulen innerhalb eines Stockwerks, Ausgangspunkt
	 * ist zunaechst das Grundrisspolygon, die Saeule wird dabei immer
	 * orientiert an den Vertices gesetzt
	 * 
	 * @param bottomQuad
	 *            Bodenflaeche des Stockwerks, deren Vertices Ausgangspunkt der
	 *            Positionierung sind
	 * @param pillarType
	 *            Kategorie der zu verwendenen Saeule
	 * @param pillarDistance
	 *            Relative Distanz zwischen Saeulen in Bezug auf die
	 *            Gesamtlaenge der Kante
	 * @return Liste mit verschobenen Modellinstanzen der geladenen Saeulen
	 */
	private List<AbstractComplex> computePositionsForPillars(
			final AbstractQuad bottomQuad, final AbstractComplex pillar,
			float pillarDistance) {

		assert mInitialized : "FEHLER: Der Objectpositioningservice wurde nicht korrekt initialisiert!";
		final List<AbstractComplex> resultPillars = new ArrayList<AbstractComplex>(
				bottomQuad.getQuadVertices().size());

		// skaliere die Saeulen auf Zielhoehe
		final AbstractComplex scaledPillar = scaleObjectByHeight(bottomQuad
				.getComplexParent().getHeight(), pillar, 1.0f, 1.0f);

		// berechne den Verschiebungsvektor inklusive Hoehenverschiebung <=>
		// umgedrehte, skalierte Quad-Normale
		final MyVector3f heightTranslation = bottomQuad.getNormal();
		heightTranslation.scale(-1.0f
				* (bottomQuad.getComplexParent().getHeight() / 2.0f));

		// AbstractComplex scaledPillar = scaleComplex(pillar, 0.5f);
		AbstractComplex pillarClone = null, pillarTrans = null;

		// als initiale Positionen die Vertexkoordinaten verwenden
		final List<Vertex3d> quadVerts = bottomQuad.getQuadVertices();
		MyVector3f completeTranslation = null;

		Vertex3d currentVert = null, endVert = null;
		MyPolygon quadPoly = bottomQuad.getPolygon();
		List<Ray> quadRays = quadPoly.getRays();
		Ray currentRay = null, previousRay = null, edge = null;
		MyVector3f lastPos = null, nextPos = null;
		boolean inside = true;

		// setze an jedes Vertex des bottomQuads eine Saeule
		for (int i = 0; i < quadVerts.size(); i++) {

			currentVert = quadVerts.get(i);

			// erstelle einen Clone der skalierten Saeule und verschiebe ihn an
			// die Position eines Vertex
			// pillarClone = scaledPillar.clone();
			pillarClone = scaledPillar.clone();

			// verschiebt die Saeule mit ihrem Mittelpunkt auf die
			// Vertexkoordinate => muss noch hochgeschoben werden
			completeTranslation = new MyVector3f();
			completeTranslation.add(currentVert.getPositionPtr(),
					heightTranslation);

			// richte die Saeule an den beiden Kanten aus, die sich im aktuellen
			// Vertex treffen
			if (i == 0) {
				previousRay = quadRays.get(quadVerts.size() - 1);
			} else {
				previousRay = quadRays.get(i - 1);
			}

			currentRay = quadRays.get(i);
			pillarClone.translate(completeTranslation);

			// an der ersten Kante ausrichten
			alignObjectWithEdge(quadPoly, previousRay, pillarClone, inside);

			// an der zweiten Kante ausrichten
			alignObjectWithEdge(quadPoly, currentRay, pillarClone, inside);

			resultPillars.add(pillarClone);

			// setze nun Kopien der Saeule entlang der Kante zum naechsten
			// Vertex
			// VORLAEUFIGE LOESUNG, MAN MUSS PRUEFEN, OB DAS AUCH BEI NICHT
			// RECHTECKIGEN GRUNDRISSEN SO FUNKTIONIERT
			endVert = quadVerts.get((i + 1) % quadVerts.size());

			// verschiebe die Vertpositionen derart, dass die Kante genau
			// zwischen den bereits positionierten Saeulen verlaeuft
			final MyVector3f start = currentVert.getPosition();
			final MyVector3f end = endVert.getPosition();

			MyVector3f edgeDirection = new MyVector3f();
			edgeDirection.sub(end, start);

			// berechne die Positionierungspunkte fuer die Saeulen unter der
			// Praemisse, dass die erste Saeule noch nicht gesetzt ist
			// => man verwendet das Startvertex als Ausgangspunkt und berechnet
			// von diesem ausgehend die die Abstaende und Positionen

			Vertex3d startVert = new Vertex3d(start);
			edge = new Ray(startVert, endVert);

			float pillarLength = pillarClone.getBB().getLength();
			float edgeLength = edge.getLength();

			// Laenge, auf der positioniert werden kann, ist um eine
			// Saeulenbreite kleiner, da am Ende der Kante bereits eine Saeule
			// steht
			float totalPositioningLength = edgeLength - pillarLength;

			// absoluten Saeulenabstand berechnen
			float absolutePillarDistance = totalPositioningLength
					* pillarDistance;

			// benoetigter Abstand pro Saeule ist Abstand + Saeulenbreite
			float neededDistancePerPillar = absolutePillarDistance
					+ pillarLength;

			// Anzahl an Positionierungen bestimmen und dann den ganzzahligen
			// Wert verwenden und damit die Laenge pro Pos berechnen
			int numberOfPositionings = (int) Math.floor(totalPositioningLength
					/ neededDistancePerPillar);
			float lengthPerPos = totalPositioningLength / numberOfPositionings;

			MyVector3f translation = edge.getDirection();
			translation.normalize();
			translation.scale(lengthPerPos);

			MyVector3f pillarTranslation = null;
			lastPos = currentVert.getPositionPtr();

			for (int k = 0; k < numberOfPositionings; k++) {

				// letzte Saeule nciht setzenm, die wird ueber Vertex gesetzt
				if (k == numberOfPositionings - 1)
					break;

				nextPos = new MyVector3f();
				nextPos.add(lastPos, translation);

				pillarTranslation = new MyVector3f();
				pillarTranslation.sub(nextPos, start);

				pillarTrans = pillarClone.clone();
				pillarTrans.translate(pillarTranslation);
				resultPillars.add(pillarTrans);
				lastPos = nextPos;
			}
		}
		return resultPillars;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode positioniert Saeulen innerhalb eines rechteckigen Stockwerks
	 * unter Angabe einer absoluten Anzahl an Saelen fuer die jeweiligen Kanten
	 * 
	 * @param bottomQuad
	 *            Bodenflaeche des Stockwerks, deren Vertices Ausgangspunkt der
	 *            Positionierung sind
	 * @param pillar
	 *            Modell der zu positionierenden Saeule
	 * @param numberOfPillarsLongside
	 *            Anzahl der Saeulen, die an der laengeren Kante des Rechtecks
	 *            positioniert werden
	 * @param numberOfPillarsBroadside
	 *            Anzahl der Saeulen, die an der kuerzeren Kante des Rechtecks
	 *            positioniert werden
	 * @return Liste mit verschobenen Modellinstanzen der geladenen Saeulen
	 */
	private List<AbstractComplex> computePositionsForPillars(
			final AbstractQuad bottomQuad, final AbstractComplex pillar,
			final Integer numberOfPillarsLongside,
			final Integer numberOfPillarsBroadside) {

		final int numberOfVerts = bottomQuad.getQuadVertices().size();
		assert mInitialized : "FEHLER: Der Objectpositioningservice wurde nicht korrekt initialisiert!";
		assert numberOfVerts == 4 : "FEHLER: Kein viereckiger Grundriss! Anzahl Vertices: "
				+ numberOfVerts;

		// skaliere die Saeulen auf Zielhoehe
		final AbstractComplex scaledPillar = scaleObjectByHeight(bottomQuad
				.getComplexParent().getHeight(), pillar, 1.0f, 1.0f);

		final MyPolygon bottomQuadPoly = bottomQuad.getPolygon();
		assert bottomQuadPoly.isRectangle() : "FEHLER: Das Eingabequad ist nicht rechteckig!";

		final List<Ray> quadRays = bottomQuadPoly.getRays();

		final float insetDistance = computeHalfPillarFootprintLength(
				bottomQuadPoly, scaledPillar);

		LOGGER.info("INSET DISTACNE: " + insetDistance);

		final Set<MyVector3f> positions = computePositionsOnRectFootprint(
				bottomQuadPoly, numberOfPillarsLongside,
				numberOfPillarsBroadside, insetDistance);

		final List<AbstractComplex> resultPillars = new ArrayList<AbstractComplex>(
				numberOfVerts);

		// berechne den Verschiebungsvektor inklusive Hoehenverschiebung <=>
		// umgedrehte, skalierte Quad-Normale
		final MyVector3f heightTranslation = bottomQuad.getNormal();
		heightTranslation.scale(-1.0f
				* (bottomQuad.getComplexParent().getHeight() / 2.0f));

		final boolean inside = true;

		// richte die Saeule an den beiden Startkanten aus
		// an der ersten Kante ausrichten
		alignObjectWithEdge(bottomQuadPoly, quadRays.get(0), scaledPillar,
				inside);

		// an der zweiten Kante ausrichten
		alignObjectWithEdge(bottomQuadPoly, quadRays.get(1), scaledPillar,
				inside);

		AbstractComplex pillarClone = null;

		MyVector3f completeTranslation = null;
		for (MyVector3f curPos : positions) {

			completeTranslation = new MyVector3f();
			completeTranslation.add(curPos, heightTranslation);

			pillarClone = scaledPillar.clone();
			pillarClone.translate(completeTranslation);
			resultPillars.add(pillarClone);

		}
		return resultPillars;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode berechnet die Projektion des uebergebenen 3D-Objekts auf die
	 * Ebene des uebergebenen Polygons. Anschliessend wird fuer die projizierten
	 * Punkte ein minimales Bounding-Rechteck bestimmt. Basierend darauf
	 * bestimmt man dann den Abstand eines Eckpunkts des Bounding-Rechtecks zu
	 * dessen Mittelpunkt.
	 * 
	 * @param bottomQuadPoly
	 *            Polygon, auf dessen Ebene die Projektion erfolgt
	 * @param pillar
	 *            Saeule, deren Punkte auf die Ebene projiziert werden
	 * @return Abstand eines Eckpunkts des Bounding-Rechtecks zu dessen
	 *         Mittelpunkt
	 */
	private float computeHalfPillarFootprintLength(
			final MyPolygon bottomQuadPoly, final AbstractComplex pillar) {

		// projiziere alle Punkte der Saeule auf die Ebene
		final List<Vertex3d> projectedPoints = new ArrayList<Vertex3d>(
				pillar.getVertexCount());
		final MyVectormath mathHelper = MyVectormath.getInstance();

		Vertex3d currentProjected = null;
		for (Vertex3d current : pillar.getVertices()) {
			currentProjected = current.clone();
			mathHelper.projectPointOntoPlane(bottomQuadPoly.getPlane(),
					currentProjected);
			projectedPoints.add(currentProjected);
		}

		// minimales Boundingrechteck bestimmen
		final MyPolygon minAreaRect = mathHelper
				.getMinAreaRect(projectedPoints);

		// Abstand eines Eckpunkts zum Mittelpunkt berechnen => das ist der
		// gesuchte Wert
		final MyVector3f boundingCenter = minAreaRect.getCenter();
		final MyVector3f vertToCenter = new MyVector3f();
		vertToCenter.sub(boundingCenter, minAreaRect.getVertices().get(0)
				.getPositionPtr());

		return vertToCenter.length();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet eine Menge von Positionen innerhalb des uebergebenen,
	 * rechteckigen Polygons. Dafuer wird dieses zunaechst insetted.
	 * Anschließend werden die Positionen basierend auf den Uebergabeparametern
	 * bestimmt.
	 * 
	 * @param poly
	 *            Polygon, innerhalb dessen die Positionen berechnet werden
	 * @param numberOfPositionsLongside
	 *            Anzahl an Positionen auf der laengeren Rechteckseite
	 * @param numberOfPositionsBroadside
	 *            Anzahl an Positionen auf der kuerzeren Rechteckseite
	 * @param insetDistance
	 *            Insetdistance, mittels derer das Polygon auf seinen
	 *            Mittelpunkt verschoben wird
	 * @return
	 */

	private Set<MyVector3f> computePositionsOnRectFootprint(
			final MyPolygon poly, final int numberOfPositionsLongside,
			final int numberOfPositionsBroadside, final float insetDistance) {

		final Set<MyVector3f> positions = new HashSet<MyVector3f>(2
				* numberOfPositionsLongside + 2 * numberOfPositionsBroadside
				- 2);

		// erstelle eine Kopie des Polygons und insette diese
		final MyPolygon insettedPoly = poly.clone();
		insettedPoly.inset(insetDistance);

		final List<Ray> quadRays = insettedPoly.getRays();

		// zuerst die Eckpunkte setzen => jeweils die Startpunkte jedes Strahls
		for (Ray current : quadRays) {
			positions.add(current.getStartPtr());
		}

		Ray longsideRay = null, broadsideRay = null;
		if (quadRays.get(0).getLength() > quadRays.get(1).getLength()) {
			longsideRay = quadRays.get(0);
			broadsideRay = quadRays.get(1);
		} else {
			longsideRay = quadRays.get(1);
			broadsideRay = quadRays.get(0);
		}

		final float pillarDistanceLongside = longsideRay.getLength()
				/ (numberOfPositionsLongside - 1);
		final float pillarDistanceBroadside = broadsideRay.getLength()
				/ (numberOfPositionsBroadside - 1);

		final MyVector3f translationLongside = longsideRay.getDirection();
		translationLongside.normalize();
		translationLongside.scale(pillarDistanceLongside);

		final MyVector3f translationBroadside = broadsideRay.getDirection();
		translationBroadside.normalize();
		translationBroadside.scale(pillarDistanceBroadside);

		MyVector3f position = null, localTranslation = null;
		final MyVector3f longsideStart = longsideRay.getStartPtr();

		// berechne Verschiebung auf parallele Kante
		MyVector3f edgeTranslation = null;
		if (longsideRay.getEndPtr().equals(broadsideRay.getStartPtr())) {
			edgeTranslation = broadsideRay.getDirectionPtr();
		} else {
			edgeTranslation = broadsideRay.getDirection();
			edgeTranslation.scale(-1.0f);
		}

		for (int i = 1; i < numberOfPositionsLongside - 1; i++) {
			position = longsideStart.clone();
			localTranslation = translationLongside.clone();
			localTranslation.scale(i);
			position.add(localTranslation);
			positions.add(position);

			// direkt Verschiebung auf andere Kante berechnen und Positionen
			// adden
			MyVector3f parallelPosition = position.clone();
			parallelPosition.add(edgeTranslation);
			positions.add(parallelPosition);

		}

		// gleiche Berechnungslogik fuer die breite Seite
		if (broadsideRay.getEndPtr().equals(longsideRay.getStartPtr())) {
			edgeTranslation = longsideRay.getDirectionPtr();
		} else {
			edgeTranslation = longsideRay.getDirection();
			edgeTranslation.scale(-1.0f);
		}

		final MyVector3f broadsideStart = broadsideRay.getStartPtr();
		for (int i = 1; i < numberOfPositionsBroadside - 1; i++) {
			position = broadsideStart.clone();
			localTranslation = translationBroadside.clone();
			localTranslation.scale(i);
			position.add(localTranslation);
			positions.add(position);

			// direkt Verschiebung auf andere Kante berechnen und Positionen
			// adden
			MyVector3f parallelPosition = position.clone();
			parallelPosition.add(edgeTranslation);
			positions.add(parallelPosition);
		}
		return positions;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode richtet das uebergebene Objekt an der Kante derart aus, dass eine
	 * der BoundingBox-Seiten parallel zur Uebergabekante verlaueft. Wenn das
	 * Inside-Flag gesetzt ist, wird das Objekt derart verschoben, dass es sich
	 * vollstaendig innerhalb des uebergebenen Polygons befindet, sonst wird es
	 * nach aussen geschoben
	 * 
	 * @param poly
	 *            Polygon, zu dem die Kante gehoert, an der das Objekt
	 *            positioniert werden soll
	 * @param ray
	 *            Referenzkante
	 * @param object
	 *            Objekt, das ausgerichtet werden soll
	 * @param inside
	 *            Flag, das angibt, ob das Objekt vollstaendig innerhalb des
	 *            Polygons liegen soll
	 */
	private void alignObjectWithEdge(final MyPolygon poly, final Ray ray,
			final AbstractComplex object, boolean inside) {

		LOGGER.debug("Aligning with Edge: " + ray);

		// Ausrichtung stimmt bzgl. aller Achsen, verschiebe das Objekt nun an
		// seine Zielposition
		BoundingBox bb = object.getBB();
		if (bb.isSymmetric())
			alignObjectAxesForSymmetricalObjects(ray, object);

		// projiziere alle BB-Punkte auf die Polygonebene, um zu testen, ob sich
		// die BB innerhalb des Polygons befindet
		final List<Vertex3d> projectedBBPoints = new ArrayList<Vertex3d>(9);
		final List<Vertex3d> obbPoints = bb.getVertices();

		// Mittelpunkt mitprojizieren
		projectedBBPoints.add(new Vertex3d(bb.getCenter()));

		// zunaechst Kopien der Vertices erstellen, damit die Ausgangsvertices
		// unveraendert bleiben
		for (int i = 0; i < obbPoints.size(); i++) {
			projectedBBPoints.add(obbPoints.get(i).clone());
		}

		for (int i = 0; i < projectedBBPoints.size(); i++) {
			mMathHelper.projectPointOntoPlane(poly.getPlane(),
					projectedBBPoints.get(i));
		}

		// teste, ob sich die projizierten Punkte vollstaendig inner- oder
		// ausserhalb des Polygons befinden
		List<Vertex3d> wrongPositionedPoints = new ArrayList<Vertex3d>(9);
		List<Vertex3d> correctPositionedPoints = new ArrayList<Vertex3d>(9);
		Vertex3d currentVertex = null;

		for (int i = 0; i < projectedBBPoints.size(); i++) {
			currentVertex = projectedBBPoints.get(i);

			// Punkt ist nicht innerhalb des Polygons
			if (!mMathHelper
					.isPointInPolygon(poly, currentVertex.getPosition())) {

				// Objekt soll aber innerhalb liegen, adde ihn zur Liste
				// falscher Punkte
				if (inside) {
					wrongPositionedPoints.add(currentVertex);
				}

				// sonst zur Liste korrekter Punkte
				else {
					correctPositionedPoints.add(currentVertex);
				}

			} else {
				// Punkt ist innerhalb des Polygons, soll aber ausserhalb liegen
				if (!inside) {
					wrongPositionedPoints.add(currentVertex);
				}

				// sonst zur Liste korrekter Punkte
				else {
					correctPositionedPoints.add(currentVertex);
				}
			}
		}

		LOGGER.debug("Anzahl falsch positionierter Vertices: "
				+ wrongPositionedPoints.size());

		// alle Punkte liegen richtig, breche ab
		if (correctPositionedPoints.size() == 9)
			return;
		assert wrongPositionedPoints.size() < 9 : "FEHLER: Alle gesetzten Punkt sind falsch! Anzahl: "
				+ wrongPositionedPoints.size();

		// berechne einen Vektor aus Polygonnormaler und Kantenrichtung
		MyVector3f cross = new MyVector3f();

		// dieser Vektor gibt die Richtung an, in die man die jeweiligen
		// Komponenten verschieben muss (wobei noch berechnet werden muss, ob
		// der Vektor in das Innere oder Aeussere des Polygons zeigt
		cross.cross(poly.getNormalPtr(), ray.getDirectionPtr());
		cross.normalize();

		// sonst berechne eine Ebene, die senkrecht zur Verschiebungsrichtung
		// steht
		Plane plane = new Plane(cross, ray.getStart());

		// teste, ob die richtigen Vertices vor oder hinter der Ebene liegen
		// nimm das erste Vertex der Menge korrekter Vertices
		MyVector3f correctToPlane = new MyVector3f();
		correctToPlane.sub(correctPositionedPoints.get(0).getPosition(),
				plane.getStuetzvektor());
		float angle = mMathHelper.calculateAngle(correctToPlane,
				plane.getNormal());

		// der Winkel ist groesser als 90°, damit liegen die richtigen Vertices
		// hinter der Ebene, damit ist die Verschiebungsrichtung falsch
		// aktualisiere die Ebene mit gedrehter Normalen
		if (angle > 90.0f) {
			LOGGER.debug("Neuberechnung Ebene, Winkel: " + angle + " Punkt: "
					+ correctPositionedPoints.get(0).getPositionPtr());
			cross.scale(-1.0f);
			plane = new Plane(cross, ray.getStart());
		}

		// berechne nun den Punkt HINTER der Ebene, der die groesste Entfernung
		// von der Kante hat
		float currentDistance = 0, maxDistance = -Float.MAX_VALUE;
		Vertex3d maxDistancePoint = null;
		MyVector3f pointToPlane = null;

		for (int i = 0; i < wrongPositionedPoints.size(); i++) {
			currentVertex = wrongPositionedPoints.get(i);
			pointToPlane = new MyVector3f();
			pointToPlane.sub(currentVertex.getPosition(),
					plane.getStuetzvektor());
			angle = mMathHelper.calculateAngle(pointToPlane, plane.getNormal());
			if (angle < 90.0f)
				continue;

			// Abstand bestimmen
			currentDistance = mMathHelper.calculatePointEdgeDistance(
					currentVertex.getPosition(), ray);
			if (currentDistance > maxDistance) {
				maxDistancePoint = currentVertex;
				maxDistance = currentDistance;
			}
		}

		// wenn kein Maximumvertex gefunden wurde, dann liegen alle "falschen"
		// Punkte auf der richtigen Seite der Ebene, eine verschiebung ist nich
		// notwendig
		if (maxDistancePoint == null)
			return;

		// Vektor hat jetzt die richtige Richtung, auf Zielentfernung skalieren
		cross.scale(maxDistance);
		LOGGER.debug("Verschiebungsvektor: " + cross);

		// verschiebe nun alle Punkte sowohl des Objekts als auch der OBB
		object.translate(cross);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode fuehrt die Rotationen des auszurichtenden Objekts aus, so dass
	 * alle Seiten der OBB Winkel mit der uebergebenen Kante bilden, die
	 * Vielfache von 90° sind. Dies funktioniert nur fuer symmetrische Objekte
	 * (bsw. Saeulen), da bei diesen kein "Vorne" definiert werden kann. Bei
	 * nicht symmetrischen Objekten muss die Ausrichtung basierend auf einer
	 * "Vorne"-Festlegung durchgefuehrt werden
	 * 
	 * @param ray
	 *            Strahl, an dem ausgerichtet wird
	 * @param object
	 *            Auszurichtendes 3d-Objekt
	 */
	private void alignObjectAxesForSymmetricalObjects(Ray ray,
			AbstractComplex object) {
		// hole die OBB des Objekts
		BoundingBox obb = object.getBB();
		assert obb != null : "FEHLER: Fuer Objekt " + object.getID()
				+ " wurde keine OBB berechnet!";

		List<MyPolygon> obbPolys = obb.getFaces();

		// teste, ob alle Seiten der OBB Winkel mit der Kante bilden, die
		// Vielfache von 90° sind
		// in diesem Fall stimmt die Grundausrichtung
		MyPolygon currentPoly = null;
		List<MyPolygon> wrongAlignedPolys = new ArrayList<MyPolygon>(6);

		// erster Durchlauf while
		wrongAlignedPolys.addAll(obbPolys);

		while (wrongAlignedPolys.size() > 0) {
			wrongAlignedPolys.clear();
			// durchlaufe alle Seitenflaechen der OBB und teste, ob diese alle
			// eine Ausrichtung zur Kante besitzen, die ein Vielfaches von 90°
			// ist
			for (int i = 0; i < obbPolys.size(); i++) {
				currentPoly = obbPolys.get(i);
				if (!mMathHelper.isAngleMultipleOf90(
						currentPoly.getNormalPtr(), ray.getDirection()))
					;
			}

			// korrigiere die erste falsch ausgerichtete Seite, teste dann, ob
			// die anderen Seiten korrekt sind, da durch die Ausrichtung einer
			// Achse u.U. das Objekt in einer korrekten Ausrichtung steht
			if (wrongAlignedPolys.size() > 0) {
				correctAlignment(ray, wrongAlignedPolys.get(0), object);
			}
		}
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode richtet das uebergebene Objekt derart aus, dass die uebergebene
	 * Seite des OBB-Polyeders parallel zum uebergebenen Strahl verlaeuft
	 * 
	 * @param ray
	 *            Strahl, an dem das uebergebene Objekt ausgerichtet wird
	 * @param poly
	 *            Polygon der Objekt-OBB, in Bezug auf die das Objekt falsch
	 *            ausgerichtet ist
	 * @param object
	 *            Objekt, das ausgerichtet werden soll
	 */
	private void correctAlignment(Ray ray, MyPolygon poly,
			AbstractComplex object) {

		LOGGER.debug("Korrigiere Polygonausrichtung fuer Objekt "
				+ object.getID() + " an Kante: " + ray);

		// berechne testweise erneut den Winkel, da die Ausrichtung durch
		// vorherige Berechnung inzwischen korrekt sein kann
		float angle = mMathHelper.calculateAngle(ray.getDirectionPtr(),
				poly.getNormalPtr());
		if (angle % 90.0f == 0)
			return;

		// sonst muss eine Rotationsachse berechnet werden => diese entspricht
		// dem Normalenvektor der Ebene, die durch die Vektoren aufgespannt wird
		MyVector3f rotationAxis = new MyVector3f();
		rotationAxis.cross(ray.getDirectionPtr(), poly.getNormalPtr());
		rotationAxis.normalize();

		// Berechnung des Rotationswinkels => Ziel ist immer eine Rotation
		// derart, dass das naechste Vielfache von 90° getroffen wird
		int fullPart = (int) (angle / 90.0f);
		float rest = angle - fullPart * 90;
		float targetAngle = 0.0f;

		// Rotationsrichtung haengt vom Rest ab => Ziel sind immer Vielfache von
		// 90
		if (rest > 45.0f)
			targetAngle = 90 - rest;
		else
			targetAngle = rest - 90;

		// nun rotiere alle Punkte der OBB und des Objekts um die Achse und den
		// Winkel
		// die Achse verlaeuft dabei immer durch das Center der OBB, berechne
		// darum Vektoren vom Center auf jedes Vertex, diese Vektoren werden
		// dann rotiert
		BoundingBox bb = object.getBB();

		// einfach alle Vertices auf einmal verarbeiten, die Berechnungen sind
		// identisch
		List<Vertex3d> verts = bb.getVertices();
		verts.addAll(object.getVertices());
		MyVector3f obbCenter = bb.getCenter();

		MyVector3f centerToVert = null, newPosition = null;
		Vertex3d currentVert = null;
		for (int i = 0; i < verts.size(); i++) {
			currentVert = verts.get(i);
			centerToVert = new MyVector3f();
			centerToVert.sub(currentVert.getPositionPtr(), obbCenter);
			mMathHelper.calculateRotatedVector(rotationAxis, centerToVert,
					targetAngle);
			newPosition = new MyVector3f();
			newPosition.add(obbCenter, centerToVert);
			currentVert.setPosition(newPosition);
		}

		// aktualisiere Objekt und OBB anhand der transformierten Vertices
		object.update();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode positioniert eine Tuer im uebergebenen Gebaeude
	 * 
	 * @param building
	 *            Gebauede, innerhalb dessen eine Tuer positioniert werden soll
	 */
	public void addDoor(final BuildingComplex building,
			final DoorComponentConfiguration doorConf) {

		// Tuer wird sinnvollerweise im untersten Stockwerk positioniert
		final List<FloorComplex> groundFloors = building
				.getFloorsByPosition(FloorPosition.GROUND);

		// kein Ground-Floor => Abbruch
		if (groundFloors.size() == 0)
			return;

		// sollten mehrere GroundFloors gefunden worden sein, verwende nur einen
		final FloorComplex groundFloor = groundFloors.get(0);
		final AbstractQuad doorQuad = computeDoorQuad(groundFloor);

		assert doorQuad != null : "FEHLER: Es konnte kein Quad fuer die Tuerpositionierung gefunden werden!";
		final PositionConfig posConf = new PositionConfig();

		// relevante Parameter im Position-Objekt speichern
		posConf.setLowerBorderObjectToQuadRatio(doorConf.getDoorToQuadRatio()
				.getLowerBorder());
		posConf.setUpperBorderObjectToQuadRatio(doorConf.getDoorToQuadRatio()
				.getUpperBorder());
		posConf.setLowerBorderObjectToEdgeRatio(doorConf
				.getDistanceToCornerRatio());

		if (doorConf.getComponentModel().isCategory()) {
			posConf.setModelCategory(doorConf.getComponentModel()
					.getModelCategory());
		} else {
			posConf.setModelCategory(ModelCategory.Door);
		}

		AbstractComplex door = getModelFromComponentSource(doorConf
				.getComponentModel());
		posConf.setComponent(door);

		door = computePositionForDoor(doorQuad, posConf);

		if (door != null) {
			building.addComplex(door);
		} else {
			LOGGER.error("FEHLER: Es konnte keine Tuer positioniert werden!");
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode fuehrt die Tuerpositionierung fuer komplexe Objekte durch, die
	 * andere Objekte enthalten koennen, dies sind alle alle Stockwerkobjekte
	 * sowie Indoor-Objekte
	 * 
	 * @param complex
	 *            Komplexes Objekt, fuer das eine Tuer positioniert werden soll
	 * @param targetQuad
	 *            Target-Quad innerhalb des komplexen Objekts
	 * @param posConf
	 *            Konfigurationsparameter fuer die Positionsberechnungen
	 */
	public void addDoor(final AbstractComplex complex,
			final AbstractQuad targetQuad, PositionConfig posConf) {
		assert mInitialized : "FEHLER: Der Objectpositioningservice wurde nicht korrekt initialisiert!";
		final AbstractComplex door = computePositionForDoor(targetQuad, posConf);

		if (complex instanceof FloorComplex) {
			FloorComplex floor = (FloorComplex) complex;
			floor.addComponent(door);
		} else if (complex instanceof IndoorFloorComplex) {
			IndoorFloorComplex indoor = (IndoorFloorComplex) complex;
			indoor.addComponent(door);
		} else
			assert false : "FEHLER: Dem uebergebenen Objekt " + complex.getID()
					+ " kann keine komplexe Komponente uebergeben werden!";

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode bestimmt das Front-Quad innerhalb des Stockwerks, in dem die Tuer
	 * positioniert werden soll. Prinzipiell wird die Tuer im Front-Quad
	 * positioniert, gibt es mehrere Quads mit dieser Ausrichtung, wird das
	 * Groesste verwendet
	 * 
	 * @param floor
	 *            Stockwerk, in dem die Tuer positioniert wird
	 * @return Quad innerhalb des Stockwerks, in dem die Tuer positioniert wird
	 */
	private AbstractQuad computeDoorQuad(FloorComplex floor) {

		AbstractQuad currentQuad = null, maxQuad = null;
		List<AbstractQuad> frontQuads = floor
				.getAllOutsideQuadsWithDirection(Side.FRONT);

		// FALLBACKS, falls keine FRONT existiert
		// teste Rueckseite
		if (frontQuads.size() == 0)
			frontQuads = floor.getAllOutsideQuadsWithDirection(Side.BACK);
		// sonst nimm eine Seitenflaeche
		if (frontQuads.size() == 0)
			frontQuads = floor.getAllOutsideQuadsWithDirection(Side.SIDE);

		Float maxWidth = -Float.MAX_VALUE, currentWidth = -Float.MAX_VALUE;
		Ray horizontalRay = null;

		// bestimme nun das Quad mit der groessten Breite und gebe dieses
		// zurueck
		for (int i = 0; i < frontQuads.size(); i++) {
			currentQuad = frontQuads.get(i);
			horizontalRay = currentQuad.getHorizontalRayForQuad();
			currentWidth = horizontalRay.getDirectionPtr().length();
			if (currentWidth > maxWidth) {
				maxWidth = currentWidth;
				maxQuad = currentQuad;
			}
		}
		return maxQuad;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet die horizontale Position eines Objekts, beschrieben
	 * durch seine Bounding Box, in Bezug auf ein Quad, dessen horizontale Kante
	 * durch den uebergebenen Strahl beschrieben wird. Dabei wird die Position
	 * innerhalb der Konfigurationsparameter zufaellig schwankend bestimmt
	 * 
	 * @param horizontalQuadRay
	 *            Horizontale Kante des Quads
	 * @param bb
	 *            Bounding Box des Objekts
	 * 
	 * @return Position des Objekts auf der horizontalen Kante
	 */
	private MyVector3f computeHorizontalPositionForObjectRand(
			final Ray horizontalQuadRay, final BoundingBox bb,
			final PositionConfig posConf) {
		float quadWidth = horizontalQuadRay.getDirectionPtr().length();

		// berechne die Breite des Bereichs, innerhalb dessen positioniert wird
		final Float distanceToCornerRatio = posConf
				.getLowerBorderObjectToEdgeRatio();

		final Float doorLength = bb.getLength();
		final Float halfDistanceToCorner = bb.getWidth()
				* distanceToCornerRatio;

		final Float positionAreaLength = quadWidth - 2 * halfDistanceToCorner
				- doorLength;

		// waehle nun zufaellig einen Wert zwischen 0 und der gesamten
		// Positionierungslaenge
		final Float randFloat = mRandom.nextFloat();

		// Breitenposition ergibt sich aus dem Mindestabstand zum Rand, der
		// halben Tuerbreite (wegen Start von einer Kante sowie dem
		// zufallsbasierten Positionierungswert
		final Float widthPositionScale = halfDistanceToCorner + doorLength / 2
				+ positionAreaLength * randFloat;

		final MyVector3f horizontalDirection = horizontalQuadRay.getDirection();
		horizontalDirection.normalize();
		horizontalDirection.scale(widthPositionScale);

		// Position auf der oberen Kante bestimmen
		final MyVector3f position = new MyVector3f();
		position.add(horizontalQuadRay.getStart(), horizontalDirection);

		return position;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet die horizontale Position eines zu positionierenden
	 * Objekts basierend auf der geforderten Ausrichtung, den Objektausdehnungen
	 * beschrieben durch das uebergebene Bounding Box-Objekt und den
	 * horizontalen Strahl, der eine Achse des Zielquads beschreibt.
	 * 
	 * @param horizontalRay
	 *            Horizontale Kante innerhalb des Quads, in dem das Objekt
	 *            positioniert werden soll
	 * @param bb
	 *            Bounding Box des Objekts
	 * @param posConf
	 *            Konfigurationsobjekt mit saemtlichen relevanten
	 *            Positionierungsparametern
	 * @return Position des Objekts in Bezug auf horizontale Achse und
	 *         Zielausrichtung
	 */
	private MyVector3f computeHorizontalPositionForObjectFixedAlignment(
			Ray horizontalRay, BoundingBox bb, PositionConfig posConf) {

		// bestimme die Anzahl moeglicher Segmente, die durch die Alignment-Enum
		// definiert sein koennen
		HorizontalAlignment align = posConf.getHorizontalAlign();
		Class alignClazz = align.getClass();
		int numberOfSegments = alignClazz.getEnumConstants().length;

		float quadWidth = horizontalRay.getDirectionPtr().length();
		float positionAreaWidth = quadWidth / numberOfSegments;

		// bestimme die Mitte innerhalb des Zielsegments
		int ordinalPosition = align.ordinal();
		float positionScale = ordinalPosition * positionAreaWidth
				+ positionAreaWidth * 0.5f;

		MyVector3f translation = horizontalRay.getDirection();
		translation.normalize();
		translation.scale(positionScale);

		// finale Position errechnen
		MyVector3f position = new MyVector3f();
		position.add(horizontalRay.getStartPtr(), translation);
		return position;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet eine Position fuer die uebergeben Tuer innerhalb des
	 * uebergebenen Quads
	 * 
	 * @param quad
	 *            Quad, innerhalb dessen die Tuer positioniert werden soll
	 * @return Geladenes, ausgerichtetes und positioniertes 3d-Modell einer Tuer
	 */
	private AbstractComplex computePositionForDoor(final AbstractQuad quad,
			final PositionConfig posConf) {

		// hole das vorab geladene Tuermodell
		final AbstractComplex door = posConf.getComponent();
		assert door != null : "FEHLER: Es wurde kein Tuermodell geladen!";

		// bestimme fuer evtl. erforderliche Skalierungen die Hoehe des Quads
		final Ray verticalQuadRay = quad.getVerticalRayForQuad();
		final Float quadHeight = verticalQuadRay.getDirectionPtr().length();

		final Ray horizontalRay = quad.getHorizontalRayForQuad();
		final Float quadWidth = horizontalRay.getDirectionPtr().length();

		// skaliere das Objekt in Abhaengigkeit von der Quadhoehe und den
		// Konfigurationsparametern in Bezug auf das Hoehenverhaeltnis
		AbstractComplex scaledDoor = scaleObjectByHeight(quadHeight, door,
				posConf.getLowerBorderObjectToQuadRatio(),
				posConf.getUpperBorderObjectToQuadRatio());

		// rotiere das Objekt, so dass die Objektnormale mit der Normalen des
		// Zielquads zusammenfaellt
		scaledDoor = rotateObject(scaledDoor, quad.getNormal(), mObjectNormal);

		// Bounding Box des Objekts, Dimensionen sind derart definiert, wie sie
		// beim Laden des Objekts bestimmt wurden => Rotation aendert nichts an
		// den zurueckgereichten Ausdehnungen
		final BoundingBox doorBB = scaledDoor.getBB();

		// teste zur Sicherheit auch das Breitenverhaeltnis
		Integer numberOfPositionings = (int) (quadWidth / doorBB.getWidth());
		if (numberOfPositionings == 0) {
			LOGGER.error("FEHLER: Tuer zu breit fuer Positionierung! Tuerbreite: "
					+ doorBB.getWidth() + " Quadbreite: " + quadWidth);
			return null;
		}

		MyVector3f position = null;
		if (posConf.getHorizontalAlign() == null)
			position = computeHorizontalPositionForObjectRand(horizontalRay,
					doorBB, posConf);
		else
			position = computeHorizontalPositionForObjectFixedAlignment(
					horizontalRay, doorBB, posConf);

		// vertikale Verschiebung => Ausgangsposition liegt auf der oberen Kante
		// addiere ein kleines Delta auf die Verschiebung, dadurch fallen die
		// Grundkante des Hauses und der Tuer nicht exakt aufeinander
		// dies vermeidet Fehler bei der Texturkoordinatenberechnung, die durch
		// die Tesselation entstehen koennen
		float delta = 1.0f;
		Float heightScale = verticalQuadRay.getDirectionPtr().length()
				- doorBB.getHeight() / 2 - delta;
		MyVector3f verticalTranslation = verticalQuadRay.getDirection();
		verticalTranslation.normalize();
		verticalTranslation.scale(heightScale);
		Float doorWidth = doorBB.getWidth();

		// verschiebe nun die Tuer noch senkrecht zum Quad, um die Tiefe
		// auszugleichen, halbe Objekttiefe verwenden
		MyVector3f quadNormal = quad.getNormal();
		quadNormal.scale(-doorWidth / 2);

		verticalTranslation.add(quadNormal);

		// vertikale Verschiebung auf horizontale Position addieren
		position.add(verticalTranslation);

		// die skalierte Tuer an ihre Endposition verschieben
		scaledDoor.translate(position);

		List<AbstractQuad> holes = quad.getHoles();
		AbstractQuad[] occupiedQuads = new AbstractQuad[holes.size()];
		for (int i = 0; i < holes.size(); i++)
			occupiedQuads[i] = holes.get(i);

		// fuege die Tuer als Loch in das Quad ein, sofern das Clipping
		// erfolgreich war
		if (clipQuadByImported(scaledDoor, quad, occupiedQuads,
				posConf.getModelCategory())) {
			return scaledDoor;
		} else {
			return null;
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet eine Menge von Punkten, an denen das geladenen
	 * Fensterobjekt positioniert werden soll. Hierfuer greift man auf die
	 * Ausdehnungen zurueck, die durch die uebergebene BoundingBox errechnet
	 * wurden
	 * 
	 * @param floor
	 *            Stockwerk, auf dem das uebergebene Objekt positioniert werden
	 *            soll
	 * @param side
	 *            Ausrichtung des Quads, in dem das Objekt positioniert werden
	 *            soll
	 * @param object
	 *            Objekt, das positioniert werden soll
	 * @param windowConfig
	 *            Fenster-Konfigurationsobjekt
	 * @return Liste mit positionierten und ausgerichteten Instanzen des
	 *         uebergebenen Fenstermodells
	 */
	private List<AbstractComplex> computePositionsForWindows(
			final FloorComplex floor, final Side side,
			final AbstractComplex object, final ModelCategory modelCategory,
			final WindowComponentConfiguration windowConfig) {

		List<AbstractComplex> windows = new ArrayList<AbstractComplex>();
		List<MyVector3f> positions = null;

		// an Decken oder Boeden werden keine Fenster angebracht
		if (isTopOrBottom(side))
			return windows;

		final List<AbstractQuad> quads = floor
				.getAllOutsideQuadsWithDirection(side);

		if (quads.size() == 0) {
			LOGGER.info("Das Stockwerk enthaelt kein Quad mit Ausrichtung: "
					+ side);
			return windows;
		}

		// rotiere das Objekt an die Zielposition => geht davon aus, dass die
		// initiale Ausrichtung "FRONT" entspricht
		// auf dem rotierten Objekt wird anschliessend weiter gerechnet
		// erstelle eine Kopie des Ausgangsobjekts
		AbstractComplex scaledObject = null, alignedObject = null;
		AABB objectBB = null;

		Ray verticalQuadRay = null, horizontalQuadRay = null;
		Float quadHeight = null, quadWidth = null;

		LOGGER.trace("Anzahl Quads mit Ausrichtung: " + side + ": "
				+ quads.size());

		// Caching-Ansatz, um die Anzahl der zu erstellenden Kopien
		// einzuschraenken
		// verwendet gerundete Hoehenwerte als Keys fuer den Zugriff auf die
		// Objekte
		final Map<Float, AbstractComplex> scaledObjects = new HashMap<Float, AbstractComplex>();

		// Caching-Ansatz fuer rotierte Objekte, gleiche Logik
		final Map<Double, AbstractComplex> rotatedObjects = new HashMap<Double, AbstractComplex>();

		// durchlaufe alle Quads mit der gesuchten Ausrichtung und positioniere
		// Fenster auf diesen
		for(AbstractQuad quad : quads) {

			// bestimme einen Strahl, der eine vertikale Kante des Quads
			// beschreibt
			verticalQuadRay = quad.getVerticalRayForQuad();
			quadHeight = verticalQuadRay.getDirectionPtr().length();

			// runde den Wert fuer den Zugriff auf die Map
			final Float roundedHeight = mMathHelper.round(quadHeight);

			// wenn das Objekt mit der Zielhoehe bereits vorhanden ist, verwende
			// es wieder
			if (scaledObjects.containsKey(roundedHeight)) {
				scaledObject = scaledObjects.get(roundedHeight);
			} else {
				// sonst skaliere es abhaengig von der Zielhoehe
				scaledObject = scaleObjectByHeight(quadHeight, object,
						windowConfig.getWindowToTargetQuadRatio()
								.getLowerBorder(), windowConfig
								.getWindowToTargetQuadRatio().getUpperBorder());
				scaledObjects.put(roundedHeight, scaledObject);
			}

			final BoundingBoxCreator bbCreator = new BoundingBoxCreator();
			objectBB = bbCreator.computeAABB(scaledObject.getVertices());

			// hole die Breite des skalierten Objekts
			final Float objectLength = objectBB.getLength();
			final Float objectWidth = objectBB.getWidth();

			horizontalQuadRay = quad.getHorizontalRayForQuad();
			quadWidth = horizontalQuadRay.getDirectionPtr().length();

			// die absoulte Breite fuer ein Positionierungssegment besteht in
			// der Breite des Fensters sowie einem Mindestabstand, der sich als
			// Anteil der Quadbreite ergibt
			float absoluteWindowSize = objectLength
					+ 2
					* (windowConfig.getMinDistanceBetweenWindowsRatio() * quadWidth);

			// wie oft kann das Objekt positioniert werden => verwende ints fuer
			// Abrundung
			Integer numberOfPositionings = (int) Math.floor(quadWidth
					/ absoluteWindowSize);

			LOGGER.debug("Auf Seite: " + side + " werden "
					+ numberOfPositionings + " Fenster positioniert");

			// wenn nichts positioniert wird, gehe zur naechsten Iteration
			if (numberOfPositionings == 0)
				continue;

			// berechne einen Schluessel fuer die Rotated-Objects-Map als
			// Produkt von gerundeter Hoehe und gerundetem Rotationswinkel
			double angle = mMathHelper.getFullAngleRad(mObjectNormal,
					quad.getNormal());

			// auf Deziamlstellen runden
			angle = mMathHelper.round(angle, 1.0f);
			Double key = roundedHeight * angle;

			if (rotatedObjects.containsKey(key)) {
				alignedObject = rotatedObjects.get(key);
			} else {
				// rotiere das skalierte Objekt, so dass es sich in der
				// Zielausrichtung befindet
				alignedObject = rotateObject(scaledObject, quad.getNormal(),
						mObjectNormal);
				rotatedObjects.put(key, alignedObject);
			}

			positions = new ArrayList<MyVector3f>(numberOfPositionings);

			LOGGER.trace("Mittelpunkt des geladenen Objekts: "
					+ objectBB.getCenter());
			LOGGER.trace("Breite des Zielquads: " + quadWidth
					+ " Breite des Objekts: " + objectLength);

			// unteteile die horizontale Strecke in so viele gleich grosse
			// Teile, wie Positionen errechnet wurden
			final MyVector3f currentStart = horizontalQuadRay.getStart();
			final MyVector3f direction = horizontalQuadRay.getDirection();

			final Float length = direction.length();
			final MyVector3f segmentDirection = new MyVector3f();
			segmentDirection.normalize(direction);

			Float segmentLength = length / numberOfPositionings;
			LOGGER.trace("Laenge der gesamten Kante: " + length
					+ " Laenge eines einzelnen Segments: " + segmentLength);

			// halbe Segmentlaenge => dadurch kommt man leichter auf die Mitte
			segmentLength /= 2.0f;

			// Laenge und Richtung eines einzelnen Segments
			segmentDirection.scale(segmentLength);

			MyVector3f position = null;
			for (int i = 0; i < numberOfPositionings; i++) {
				position = new MyVector3f();

				// halbe Segmentlaenge aufaddieren => Mitte des Streckensegments
				position.add(currentStart, segmentDirection);

				positions.add(position);

				// naechste Iteration
				currentStart.add(position, segmentDirection);
			}

			// nun verschiebt man die berechneten Punkte noch in Richtung der 2.
			// Quad-Kante
			// da die Punkte aufsteigend sortiert wurden und Processing mit
			// einer gedrehten y-Achse arbeitet
			// ist die verwendete horizontale Kante die obere Kante des Quads =>
			// verschiebe jeden der Puntke um
			// 40% in Richtung der unteren Kante
			final MyVector3f verticalDirection = verticalQuadRay.getDirection();
			Float verticalLength = verticalDirection.length();

			// berechne einen Anteil an der Gesamthoehe des Quads => um diesen
			// werden die Punkte dann nach unten verschoben
			verticalLength *= windowConfig.getTranslationRatio();

			final MyVector3f verticalTranslation = new MyVector3f();

			verticalTranslation.normalize(verticalDirection);
			verticalTranslation.scale(verticalLength);
			LOGGER.trace("Vertikale Verschiebung: " + verticalTranslation);

			// verschiebe nun das Objekt noch senkrecht zum Quad, um die Tiefe
			// auszugleichen, verwende die halbe Objekttiefe
			final MyVector3f quadNormal = quad.getNormal();
			quadNormal.scale(-objectWidth / 2.0f);

			verticalTranslation.add(quadNormal);

			// addiere nun auf alle errechneten Positionen die vertikale
			// Verschiebung
			for (int i = 0; i < positions.size(); i++) {
				positions.get(i).add(verticalTranslation);
			}

			AbstractComplex windowCopy = null;
			final List<AbstractQuad> holes = quad.getHoles();

			AbstractQuad[] occupiedQuads = new AbstractQuad[holes.size()];
			for (int i = 0; i < holes.size(); i++)
				occupiedQuads[i] = holes.get(i);

			// erzeuge verschobene Kopien des Fensters fuer jede neue Position
			// und fuege sie zur Ergebnisliste hinzu
			for (int i = 0; i < positions.size(); i++) {
				windowCopy = alignedObject.clone();
				windowCopy.translate(positions.get(i));
				// beschneide das Quad gegen das importierte Objekt, fuege es
				// nur hinzu, falls keine Ueberschneidung mit bereits
				// vorab importierten
				if (clipQuadByImported(windowCopy, quad, occupiedQuads,
						modelCategory))
					windows.add(windowCopy);
			}
		}
		return windows;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet einen Skalierungsfaktor, um den das uebergebene Objekt
	 * skaliert wird. Dieser Faktor ist abhaengig vom Verhaeltnis zwischen
	 * Objekt- und Quadhoehe.
	 * 
	 * @param quadHeight
	 *            Hoehe des Zielquads
	 * @param object
	 *            Objekt, das skaliert werden soll
	 * @param lowerRatio
	 *            Kleinstes akzeptiertes Verhaeltnis zwischen Quad- und
	 *            Objekthoehe
	 * @param upperRatio
	 *            Groesstes akzeptiertes Verharltnis zwischen Quad- und
	 *            Objekthoehe
	 * @return Kopie des Eingabeobjekts, evtl. skaliert
	 */
	private AbstractComplex scaleObjectByHeight(final Float quadHeight,
			final AbstractComplex object, final Float lowerRatio,
			Float upperRatio) {

		BoundingBox objectBB = object.getBB();

		// wenn es sich um eine OBB handelt, berechne eine AABB, um die Masse
		// korrekt berechnen zu koennen
		if (objectBB instanceof OBB) {
			BoundingBoxCreator bbCreator = new BoundingBoxCreator();
			objectBB = bbCreator.computeAABB(object.getVertices());
		}

		float objectHeight = objectBB.getHeight();

		float scaleFactor, targetRatio, targetHeight;
		AbstractComplex scaledObject = null;

		// Verhaeltnis Objekt zu Quad
		float objectToQuadRatio = objectHeight / quadHeight;

		if (objectToQuadRatio > upperRatio || objectToQuadRatio < lowerRatio) {

			// skaliere immer auf die obere Grenze => zufallsbasierte Scalings
			// fuehren zu unterschiedlich grossen Objetken im gleichen Gebaeude
			targetRatio = upperRatio;

			// Zielhoehe im Quad bestimmen
			targetHeight = quadHeight * targetRatio;

			// Anteil der Zielhoehe an Objekthoehe bestimmen
			scaleFactor = targetHeight / objectHeight;

			// Objekt mit dem Zielverhaeltnis skalieren
			scaledObject = object.scaleComplex(scaleFactor);

			// wenn skaliert wurde, gebe die skalierte Variante zurueck
			return scaledObject;
		}

		// sonst gebe eine Kopie des Originals zurueck, damit immer mit einer
		// Kopie gearbeitet wird
		return object.clone();

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erstellt eine rotierte Kopie des uebergebenen Objekts. Der
	 * Rotationswinkel errechnet sich aus der Abweichung der Standardnormalen
	 * fuer importierte Objekte (z-Achse) und der Normalen des Zielquads. Sofern
	 * der Winkel ausserhalb des Toleranzbereichs liegt, wird eine rotierte
	 * Kopie erstellt und an den Aufrufer zurueckgegeben.
	 * 
	 * @param object
	 *            Objekt, das rotiert werden soll
	 * @param normalDest
	 *            Normalenvektor der Flaeche, auf der das Objekt positioniert
	 *            werden soll
	 * @return Kopie des Eingabeobjekts, bei Bedarf rotiert
	 */
	private AbstractComplex rotateObject(final AbstractComplex object,
			final MyVector3f normalDest, final MyVector3f objectNormal) {

		// berechne den vollen Winkel (0-360) zwischen Quelle und Ziel
		double angleRad = mMathHelper.getFullAngleRad(objectNormal, normalDest);

		// Ausnahme, falls der Winkel 180° betraegt, in diesem Fall liefert die
		// Berechnungsmethode 0° zurueck (warum auch immer)
		if (angleRad == 0.0d)
			angleRad = mMathHelper.calculateAngleRadians(objectNormal,
					normalDest);

		// wenn die Winkelabweichung innerhalb des Toleranzbereichs liegt, gebe
		// nur eine Kopie des Objekts zurueck
		if (mMathHelper.isWithinTolerance(angleRad, 0.0d, 0.1d))
			return object.clone();

		// verwende y-Achse fuer Rotation => Kreuzprodukt scheitert, wenn der
		// WInkel 180° ist
		final MyVector3f rotationAxis = new MyVector3f(0.0f, 1.0f, 0.0f);

		// Mittelpunkt des Objekts entspricht seiner Position
		final MyVector3f objectCenter = object.getPosition();

		// erstelle eine Kopie, die dann rotiert wird
		final AbstractComplex copy = object.clone();

		// rotiere das Objekt um den berechneten Winkel
		copy.rotate(rotationAxis, objectCenter, angleRad);
		return copy;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode schneidet die Form des importierten Objekts aus dem uebergebenen
	 * Quad aus und speichert die entstehende Form als Lochquad im Ausgangsquad.
	 * Das Verfahren projiziert zunaechst saemtliche Punkte des importierten
	 * Objekts auf das Zielquad. Anschliessend berechnet es ueber GrahamScan die
	 * konvexe Huelle, die dann aus dem Objekt ausgeschnitten wird.
	 * 
	 * @param imported
	 *            Importiertes 3d-Objekt
	 * @param quad
	 *            Quad-Struktur, auf der das 3d-Objekt positioniert werden soll
	 * @param modelCategory
	 *            Modellkategorie, aus der das eingefuegte Objekt stammt
	 * @return True, falls das importierte Objekt auf dem Quad ohne
	 *         Ueberschneidung positioniert werden konnte, False sonst
	 */
	private Boolean clipQuadByImported(final AbstractComplex imported,
			final AbstractQuad quad, final AbstractQuad[] occupiedQuads,
			final ModelCategory modelCategory) {

		List<Vertex3d> outline = null;
		PrototypeHelper prototypeHelper = PrototypeHelper.getInstance();
		Plane quadPlane = quad.getPlane();

		Vertex3d currentProjected = null;
		List<Vertex3d> importedVertices = imported.getVertices();
		Set<Vertex3d> projectedPointsSet = new HashSet<Vertex3d>();

		// projiziere die Vertices des 3d-Objekts auf die Ebene des Quads
		for (int i = 0; i < importedVertices.size(); i++) {
			currentProjected = importedVertices.get(i).clone();
			mMathHelper.projectPointOntoPlane(quadPlane, currentProjected);

			if (!projectedPointsSet.contains(currentProjected)) {
				// System.out.println("mVertices.add(new Vertex3d(" +
				// currentProjected.getX() + "f, " + currentProjected.getY() +
				// "f, " + currentProjected.getZ() + "f));");
				// logger.error("ADDED: " + currentProjected);
				projectedPointsSet.add(currentProjected);
			}
		}

		List<Vertex3d> projectedPointsList = new ArrayList<Vertex3d>(
				projectedPointsSet.size());
		projectedPointsList.addAll(projectedPointsSet);

		// berechne die konvexe Huelle fuer die auf das Quad projizierten
		// Vertices
		outline = prototypeHelper.computeConvexHullForUnprojectedVertices(
				projectedPointsList, quadPlane.getNormal());

		// wenn das Objekt nicht positioniert werden konnte, da es sich mit
		// anderen Objekten ueberschneidet, breche ab
		if (!validatePosition(occupiedQuads, outline))
			return false;

		// cleanUpPolygon(outline);

		List<Vertex3d> parentVerts = quad.getVertices();
		// fuege die Outline-Vertices zum Vertex-Buffer des Parents hinzu
		// Index des ersten eingefuegten Vertex
		Integer startIndex = parentVerts.size();
		parentVerts.addAll(outline);

		Integer[] indices = new Integer[outline.size()];
		for (int i = 0; i < outline.size(); i++)
			indices[i] = startIndex + i;

		LOGGER.debug("Startindex: " + startIndex
				+ " Berechnete Outline: Anzhal Indices: " + outline.size());

		Vertex3d currentVertex = null;
		String message = "";
		String lineSeparator = System.getProperty("line.separator");
		for (int i = 0; i < outline.size(); i++) {
			currentVertex = parentVerts.get(indices[i]);
			message += "mVertices.add(new Vertex3d(" + currentVertex.getX()
					+ "f, " + currentVertex.getY() + "f, "
					+ currentVertex.getZ() + "f));" + lineSeparator;
		}
		LOGGER.debug(message);

		// erzeuge ein Polygonales Quad mit den Indices, die sich auf die neuen
		// Vertices beziehen
		PolygonalQuad hole = new PolygonalQuad();
		hole.setComplexParent(quad.getComplexParent());
		hole.setDirection(quad.getDirection());
		hole.setIndices(indices);
		hole.setContentType(modelCategory);
		hole.update();

		// sorge dafuer, dass die Normalen von Loch und Quad immer in
		// unterschiedliche Richtungen zeigen
		double angleRad = mMathHelper.calculateAngleRadians(hole.getNormal(),
				quad.getNormal());
		if (angleRad == 0.0d) {
			hole.flipIndices();
			hole.update();
			angleRad = mMathHelper.calculateAngleRadians(hole.getNormal(),
					quad.getNormal());
			assert angleRad == Math.PI : "FEHLER: Loch und Quellquad besitzen den gleichen Normalenvektor! Winkel: "
					+ angleRad;
		}

		quad.addHole(hole);
		return true;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode testet saemtliche Vertices der uebergebenen Outline dahingehend,
	 * ob sie innerhalb eines der uebergebenen Quads liegen.
	 * 
	 * @param occupiedQuads
	 *            Quad-Liste, fuer die geprueft wird, ob eines der Vertices der
	 *            Outline innerhalb eines der Quads liegt
	 * @param newOutline
	 *            Outline des neuen Objekts, fuer die geprueft wird, ob eine
	 *            Ueberschneidung mit bereits existierenden Objekten vorliegt
	 * @return True, falls keine Ueberschneidung vorliegt, das Objekt also
	 *         positioniert werden kann, False sonst
	 */
	private boolean validatePosition(final AbstractQuad[] occupiedQuads,
			final List<Vertex3d> newOutline) {
		MyPolygon outlinePoly = null;

		// berechne eine Polygoninstanz fuer die berechnete Outline
		try {
			outlinePoly = new MyPolygon(newOutline);
		} catch (AssertionError e) {
			e.printStackTrace();
			return false;
		}

		// durchlaufe alle Quads und teste jedes vorhandene Loch-Quad gegen alle
		// Vertices der Outline
		for (int i = 0; i < occupiedQuads.length; i++) {

			// wenn sich das Eingabepolygon mit einem der Lochpolygone
			// ueberschneidet, gebe false zurueck
			if (mMathHelper.intersects(outlinePoly,
					occupiedQuads[i].getPolygon()))
				return false;
		}

		return true;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Utility-Methode, testet, ob die uebergebene Seite Decke oder Boden
	 * beschreibt
	 * 
	 * @param side
	 *            Seite, die getestet wird
	 * 
	 * @return True, falls es ein Decken- oder Bodenquad ist, False sonst
	 */
	private boolean isTopOrBottom(Side side) {
		if (side.equals(Side.TOP) || side.equals(Side.BOTTOM))
			return true;
		else
			return false;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet Fensterbaenke fuer saemtliche Fenster, die in das
	 * Gebaeude eingefuegt wurden
	 * 
	 * @param building
	 *            Gebaeude, an dem Fensterbaenke angebracht werden sollen
	 * @param ledgeConf
	 *            Konfigurationsinstanz zur Erzeugung von Fensterbaenken
	 */
	public void addWindowLedges(final BuildingComplex building,
			final WindowLedgeComponentConfiguration ledgeConf) {

		assert mInitialized : "FEHLER: Der Objectpositioningservice wurde nicht korrekt initialisiert!";
		final List<FloorComplex> floors = building.getFloors();
		List<AbstractQuad> wallQuads = null;
		List<AbstractQuad> holes = null;
		boolean exactMatch = false;

		final AbstractComplex ledgeProfile = getModelFromComponentSource(ledgeConf
				.getComponentModel());

		ImportedComplex currentLedge = null;
		AbstractBuildingComponent currentLedgeComponent = null;
		MyPolygon minAreaRect = null;
		List<Vertex3d> vertices = null;
		double angleRad;

		MouldingComponentConfiguration mouldingConf = null;

		for (FloorComplex currentFloor : floors) {
			wallQuads = currentFloor.getOutdoorQuads();
			for (AbstractQuad currentQuad : wallQuads) {
				holes = currentQuad.getHolesByContent(ModelCategory.Window,
						exactMatch);

				for (AbstractQuad currentHole : holes) {
					minAreaRect = mMathHelper.getMinAreaRect(currentHole
							.getQuadVertices());

					// die Normale des MinArea-Polygons muss die gleiche
					// Ausrichtung haben, wie das Wandquad
					// bei Bedarf muessen darum die Vertices geflippt werden
					angleRad = mMathHelper.calculateAngleRadians(
							currentQuad.getNormal(), minAreaRect.getNormal());
					if (angleRad != 0.0d) {
						vertices = minAreaRect.getVertices();
						List<Vertex3d> tempVertBuffer = new ArrayList<Vertex3d>(
								minAreaRect.getVertices().size());

						for (int l = vertices.size() - 1; l >= 0; l--) {
							tempVertBuffer.add(vertices.get(l));
						}
						vertices.clear();
						vertices.addAll(tempVertBuffer);
						minAreaRect = new MyPolygon(vertices);
					}

					// erstelle eine Gesimsekonfiguration, die anschliessend
					// fuer die Erstellung der Fensterbaenke eingesetzt wird
					mouldingConf = new MouldingComponentConfiguration(
							ledgeConf.getLedgeToTargetQuadRatio(),
							VerticalAlignment.BOTTOM);

					currentLedgeComponent = new Moulding(building.getParent(),
							mouldingConf, ledgeProfile, minAreaRect);
					currentLedgeComponent.createComponent();
					currentLedge = currentLedgeComponent.getComponent();
					building.addComplex(currentLedge);
				}
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode laedt einen Grundriss aus einer Modeldatei und erzeugt aus diesem
	 * ein Polygon, das an den Aufrufer zurueckgereicht wird.
	 * 
	 * @param footprint
	 *            Kategorie des zu ladenden Footprints
	 * @return Geladenes Footprintpolygon
	 */
	public MyPolygon loadFootprint(final ModelCategory footprint) {
		assert mInitialized : "FEHLER: Der Objectpositioningservice wurde nicht korrekt initialisiert!";

		AbstractComplex footprintModel = getModelByCategory(footprint);
		footprintModel.unregister();

		MyPolygon footprintPoly = new MyPolygon(footprintModel.getVertices());
		return footprintPoly;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode laedt einen Grundriss aus einer Modeldatei und erzeugt aus diesem
	 * ein Polygon, das an den Aufrufer zurueckgereicht wird.
	 * 
	 * @param footprint
	 *            Kategorie des zu ladenden Footprints
	 * @return Geladenes Footprintpolygon
	 */
	public MyPolygon loadFootprint(final File footprint) {
		assert mInitialized : "FEHLER: Der Objectpositioningservice wurde nicht korrekt initialisiert!";

		// lade das Model von der Uebergabelocation
		final AbstractComplex footprintModel = createObjectFrom3dModel(footprint
				.getAbsolutePath());

		assert footprintModel != null : "FEHLER: Model '"
				+ footprint.getAbsolutePath()
				+ "' konnte nicht geladen werden!";

		// aus der Objektverwaltung entfernen
		footprintModel.unregister();
		MyPolygon footprintPoly = new MyPolygon(footprintModel.getVertices());
		return footprintPoly;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode laedt das Komponentenmodell basierend auf der uebergebenen
	 * Konfiguration und gibt es an den Aufrufer zurueck
	 * 
	 * @param modelSource
	 *            Component-Model-Source-Konfiguration, die angibt, woher das zu
	 *            ladende Modell stammt
	 */
	public AbstractComplex getModelFromComponentSource(
			final ComponentModelSource modelSource) {

		assert mInitialized : "FEHLER: Der Objectpositioningservice wurde nicht korrekt initialisiert!";

		// Wenn das Model ueber eine Kategorieangabe spezifiziert wurde, lade es
		// ueber den Kategoriemechanismus
		AbstractComplex loadedModel = null;
		if (modelSource.isCategory()) {
			loadedModel = getModelByCategory(modelSource.getModelCategory());
		} else {
			loadedModel = getModelFromFile(modelSource.getComponentSource());
		}

		// melde das Objekt bei der Objektverwaltung ab => Komponenten werden
		// ueber ihre Eltern gerendert
		loadedModel.unregister();
		return loadedModel;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erzeugt Treppe basierend auf der uebergebenen Konfiguration und
	 * dem uebergebenen Grundriss
	 * 
	 * @param building
	 *            Gebaeude, an dem die Treppe angebracht werden soll
	 * @param footprint
	 *            Polygon, das den Grundriss der Treppenkonstruktion beschreibt
	 * @param stairsConf
	 *            Treppenkonfigurationsobjekt
	 * @param position
	 *            Positionsangabe, um die die erzeugte Treppe verschoben wird
	 */
	public void addRoundStairs(final BuildingComplex building,
			final MyPolygon footprint,
			final RoundStairsComponentConfiguration stairsConf,
			final MyVector3f position) {
		assert mInitialized : "FEHLER: Der Objectpositioningservice wurde nicht korrekt initialisiert!";
		final AbstractBuildingComponent stairs = new RoundStairs(
				building.getParent(), footprint, stairsConf);
		stairs.createComponent();
		AbstractComplex stairsComplex = stairs.getComponent();
		stairsComplex.translate(position);
		building.addComplex(stairsComplex);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erzeugt eines Fascia an dem uerbegebenen Gebauede an dem
	 * uebergebenen Stockwerk. Die Konstruktion basiert auf der uerbegebenen
	 * Konfiguration.
	 * 
	 * @param building
	 *            Gebauede, an dem die Fascia angebracht wird
	 * @param fasciaConf
	 *            Fascia-Konfigurationsobjekt
	 * @param targetFloor
	 *            Stockwerk, an dem die Fascia appliziert wird
	 */
	public void addFascia(final BuildingComplex building,
			final FasciaComponentConfiguration fasciaConf,
			final AbstractComplex targetFloor) {
		assert mInitialized : "FEHLER: Der Objectpositioningservice wurde nicht korrekt initialisiert!";
		final Fascia fascia = new Fascia(building.getParent(), fasciaConf,
				targetFloor);
		fascia.createComponent();
		building.addComplex(fascia.getComponent());
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erzeugt eines Fascia an dem uerbegebenen Gebauede. An welchen
	 * Stockwerken dabei eine Fascia appliziert wird, wird anhand der
	 * Fascia-Konfiguration entschieden
	 * 
	 * @param building
	 *            Gebauede, an dem die Fascia angebracht wird
	 * @param fasciaConf
	 *            Fascia-Konfigurationsobjekt
	 */
	public void addFascia(final BuildingComplex building,
			final FasciaComponentConfiguration fasciaConf) {
		assert mInitialized : "FEHLER: Der Objectpositioningservice wurde nicht korrekt initialisiert!";
		final List<FloorPosition> targetFloorPositions = fasciaConf
				.getFloorPositions();
		final List<FloorComplex> floors = new ArrayList<FloorComplex>();
		for (FloorPosition cur : targetFloorPositions) {
			floors.addAll(building.getFloorsByPosition(cur));
		}

		for (FloorComplex curFloor : floors) {
			Fascia fascia = new Fascia(building.getParent(), fasciaConf,
					curFloor);
			fascia.createComponent();
			building.addComplex(fascia.getComponent());
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erzeugt Gesimse, die basierend auf der uebergebenen Konfiguration
	 * an dem uebergebenen Gebauede angebracht werden
	 * 
	 * @param building
	 *            Gebaeude, an dem die Gesimse angebracht werden
	 * @param mouldingConf
	 *            Gesimsekonfiguration
	 * @param targetQuads
	 *            Quads,
	 */
	public void addMoulding(final BuildingComplex building,
			final MouldingComponentConfiguration mouldingConf) {
		assert mInitialized : "FEHLER: Der Objectpositioningservice wurde nicht korrekt initialisiert!";
		// sammle zunaechst saemtliche Stockwerke ein, an denen das Gesimse
		// appliziert werden soll
		final List<FloorComplex> targetFloors = new ArrayList<FloorComplex>();
		final List<FloorPosition> floorPositions = mouldingConf
				.getFloorPositions();
		for (FloorPosition floorPos : floorPositions) {
			targetFloors.addAll(building.getFloorsByPosition(floorPos));
		}

		// sammle nun saemtliche Quads ein, an denen Gesimse appliziert werden
		// sollen
		final List<AbstractQuad> targetQuads = new ArrayList<AbstractQuad>();
		final List<Side> targetQuadDirections = mouldingConf
				.getQuaddirections();

		for (Side curSide : targetQuadDirections) {
			for (FloorComplex curFloor : targetFloors) {
				targetQuads.addAll(curFloor
						.getAllOutsideQuadsWithDirection(curSide));
			}
		}

		// Lade das ausgewaehlte Profil aus der 3D-Quelldatei
		final AbstractComplex mouldingProfile = ObjectPositioningService
				.getInstance().getModelFromComponentSource(
						mouldingConf.getComponentModel());
		AbstractBuildingComponent curMoulding = null;

		LOGGER.info("Adde " + targetQuads.size() + " Moulding-Instanzen.");

		// erzeuge fuer jedes Zielquad ein Gesimse
		for (AbstractQuad curQuad : targetQuads) {

			// Skippe TOP- und BOTTOM-Quads => an diesen werden keine Gesimse
			// angebracht
			if (curQuad.getDirection().equals(Side.TOP)
					|| curQuad.getDirection().equals(Side.BOTTOM)) {
				continue;
			}
			curMoulding = new Moulding(building.getParent(), mouldingConf,
					mouldingProfile, curQuad.getPolygon());
			curMoulding.createComponent();
			building.addComplex(curMoulding.getComponent());
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erzeugt Gesimse, die basierend auf der uebergebenen Konfiguration
	 * an dem uebergebenen Gebauede angebracht werden
	 * 
	 * @param building
	 *            Gebaeude, an dem die Gesimse angebracht werden
	 * @param target
	 *            Zielkomponente des Gebaeudes
	 * @param mouldingConf
	 *            Gesimsekonfiguration
	 * @param targetQuads
	 *            Quads,
	 */
	public void addMoulding(final BuildingComplex building,
			final AbstractComplex target,
			final MouldingComponentConfiguration mouldingConf) {
		assert mInitialized : "FEHLER: Der Objectpositioningservice wurde nicht korrekt initialisiert!";

		// sammle nun saemtliche Quads ein, an denen Gesimse appliziert werden
		// sollen
		final List<AbstractQuad> targetQuads = new ArrayList<AbstractQuad>();
		final List<Side> targetQuadDirections = mouldingConf
				.getQuaddirections();

		for (Side curSide : targetQuadDirections) {
			targetQuads.addAll(target.getAllOutsideQuadsWithDirection(curSide));
		}

		// Lade das ausgewaehlte Profil aus der 3D-Quelldatei
		final AbstractComplex mouldingProfile = getModelFromComponentSource(mouldingConf
				.getComponentModel());
		AbstractBuildingComponent curMoulding = null;

		LOGGER.info("Adde " + targetQuads.size() + " Moulding-Instanzen.");

		// erzeuge fuer jedes Zielquad ein Gesimse
		for (AbstractQuad curQuad : targetQuads) {

			// Skippe TOP- und BOTTOM-Quads => an diesen werden keine Gesimse
			// angebracht
			if (curQuad.getDirection().equals(Side.TOP)
					|| curQuad.getDirection().equals(Side.BOTTOM)) {
				continue;
			}

			LOGGER.info("Moulding-Quad-Normal: " + curQuad.getNormalPtr());

			curMoulding = new Moulding(building.getParent(), mouldingConf,
					mouldingProfile, curQuad.getPolygon());
			curMoulding.createComponent();
			building.addComplex(curMoulding.getComponent());

		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode fuegt innerhalb des uebergebenen Gebaeudes Kanten-Verzierungen
	 * hinzu, die auf der Basis von Profilextrusionen erzeugt werden
	 * 
	 * @param building
	 *            Gebaeude, innerhalb dessen die Kantenverzierungen angebracht
	 *            werden sollen
	 * @param conf
	 *            Konfiguration fuer die Kantenkomponenten
	 */
	public void addEdgeAdditions(final BuildingComplex building,
			final EdgeAdditionComponentConfiguration conf) {

		final List<FloorPosition> floors = conf.getTargetFloors();
		final List<AbstractComplex> targetFloors = new ArrayList<AbstractComplex>();
		for (FloorPosition pos : floors) {

			// Sonderfallverarbeitung fuer Daecher => diese werden nicht wie
			// Stockwerke behandelt
			if (pos.equals(FloorPosition.ROOF)) {
				targetFloors.add(building.getRoof());
			} else {
				targetFloors.addAll(building.getFloorsByPosition(pos));
			}
		}

		final List<Side> targetQuadDirections = conf.getTargetWalls();
		final List<AbstractQuad> quads = new ArrayList<AbstractQuad>();

		for (AbstractComplex floor : targetFloors) {
			for (Side side : targetQuadDirections) {
				quads.addAll(floor.getAllOutsideQuadsWithDirection(side));
			}
		}

		// lade das zu verwendende Profil
		final AbstractComplex profile = getModelFromComponentSource(conf
				.getComponentModel());

		// erstelle fuer jedes ermittelte Quad eine EdgeAddition-Komponente
		EdgeAddition edgeAddition = null;
		for (AbstractQuad quad : quads) {
			edgeAddition = new EdgeAddition(building.getParent(), conf, quad,
					profile);
			edgeAddition.createComponent();
			building.addComplex(edgeAddition.getComponent());

		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erzeugt AbstractComplex-Instanzen aus geladenen 3d-Modells, die
	 * vorab aus Modelling-Programmen exportiert wurden. Anhand der Extension
	 * des Dateinamens wird entschieden, welcher Loader eingesetzt wird, um die
	 * Datei zu laden.
	 * 
	 * @param absolutePath
	 *            Vollstaendiger Pfad des zu ladenden Modells
	 * @return ImportedComplex-Instanz, die aus der Modell-Datei erstellt wurde
	 */
	public AbstractComplex createObjectFrom3dModel(final String absolutePath) {

		LOGGER.info("MODEL-PATH: " + absolutePath);
		AbstractModelImport loader = null;

		// ermittle zunaechst den zu verwendenden Loader anhand der
		// File-Extension
		int index = absolutePath.lastIndexOf(".");
		assert index != -1 : "FEHLER: Keine Extension gefunden, das Model '"
				+ absolutePath + "' kann nicht geladen werden.";

		String extension = absolutePath.substring(index + 1);
		if (extension.equalsIgnoreCase("obj")) {
			loader = new ObjImport();
		} else {
			assert false : "FEHLER: Unbekanntes Model-Format: " + extension
					+ ", Verarbeitung wird abgebrochen.";
		}

		// beginne die Verarbeitung
		loader.loadModel(absolutePath, mParentApplet);

		final ImportedComplex imported = new ImportedComplex(mParentApplet,
				loader.getVertices(), loader.getFaces());
		imported.create();

		// versuche nun das Modell in den Ursprung des Koordinatensystems zu
		// verschieben, verwende den ungefaehren Mittelpunkt, der durch
		// die Bounding-Box berechnet wird
		BoundingBox importedBB = imported.getBB();
		MyVector3f center = importedBB.getCenter().clone();

		// setze die Startposition des importierten Objekts auf den Mittelpunkt
		// der berechneten Bounding-Box
		imported.setInitialPosition(center.clone());
		center.scale(-1.0f);

		// verschiebe das Objekt, so dass das Zentrum im Ursprung liegt
		imported.translate(center);

		// versuche, den Quads aufgrund der Standardausrichtungen der Seiten
		// korrekte Richtungen zuzuweisen
		// kann bei nicht achsenausgerichteten Objekten zu Problemen fuehren...
		imported.alignDirectionsByNormals(mNormalToDirectionMap,
				imported.getOutdoorQuads());

		LOGGER.debug("New Position after Translation: "
				+ imported.getPositionPtr());
		return imported;

	}

	// ------------------------------------------------------------------------------------------

}
