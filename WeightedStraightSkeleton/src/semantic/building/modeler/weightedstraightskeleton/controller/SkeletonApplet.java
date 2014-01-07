package semantic.building.modeler.weightedstraightskeleton.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PFont;
import semantic.building.modeler.configurationservice.controller.ConfigurationController;
import semantic.building.modeler.configurationservice.model.SystemConfiguration;
import semantic.building.modeler.math.MyVector2f;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.Ray;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.prototype.service.Texture;
import semantic.building.modeler.prototype.service.TextureManagement;
import semantic.building.modeler.prototype.service.TextureManagement.TextureCategory;
import semantic.building.modeler.weightedstraightskeleton.algorithm.SkeletonEdge;
import semantic.building.modeler.weightedstraightskeleton.algorithm.SkeletonPolygon;
import semantic.building.modeler.weightedstraightskeleton.algorithm.SkeletonRoofDescriptor;
import semantic.building.modeler.weightedstraightskeleton.algorithm.SkeletonVertex;
import semantic.building.modeler.weightedstraightskeleton.algorithm.iStraightSkeletonEvent;
import semantic.building.modeler.weightedstraightskeleton.exception.AccuracyException;
import semantic.building.modeler.weightedstraightskeleton.exception.SquareCaseException;
import semantic.building.modeler.weightedstraightskeleton.result.AbstractResultElement;
import semantic.building.modeler.weightedstraightskeleton.result.ResultFace;
import semantic.building.modeler.weightedstraightskeleton.result.SkeletonResultComplex;

/**
 * @author Patrick Gunia PApplet-Ableitung, initialisiert Processing-relevante
 *         Strukturen und ruft den eigentlichen SS-Algorithmus auf. Drawing und
 *         Message-Verarbeitung.
 * 
 */
public class SkeletonApplet extends PApplet {

	/** Logger */
	protected static Logger LOGGER = Logger.getLogger(SkeletonApplet.class);

	/** Mapstruktur mit saemtlichen vorhandenen Konfigurationsobjekten */
	Map<Integer, SkeletonRoofDescriptor> mConfigs = null;

	/**
	 * Listenstruktur mit saemtlichen waehrend der Berechnung erzeugten
	 * Polygonen
	 */
	List<List<SkeletonPolygon>> mPolygons = null;

	/**
	 * Liste speichert alle waehrend der Verarbeitung aufgetretenen Events,
	 * DEBUG
	 */
	private List<iStraightSkeletonEvent> mEvents;

	/**
	 * Vector speichert saemtliche (auch verworfene) Schnittpunkte der
	 * Plane-Plane-Plane-Intersection-Tests, DEBUG
	 */
	private List<MyVector3f> mSchnittpunkte = null;

	/**
	 * Neue Struktur zur Verwaltung der Ergebnisse. Enthaelt fuer jede Kante des
	 * Eingabepolygons eine Face-Struktur bestehend aus einer beliebigen Anzahl
	 * von Elementen
	 */
	private SkeletonResultComplex mResultComplex = null;

	/** Bildinstanz, die die geladene Textur vorhaelt */
	private Texture mRoofTexture = null;

	/** Liste nimmt alle waehrend der Berechnung ermittelten Virtual-Edges auf */
	private List<Ray> mVirtualEdges = null;

	/** zeichnet nur den grundriss, fuehrt keine Berechnungen durch */
	boolean drawInput = true;

	/** Steuervariablen fuer das Mouse-Dragging */
	private float dragLastPosX = 0.0f;

	private float dragTreshold = 0.05f;

	private float dragRotateX = 0.0f;

	private float dragDelta = 0.05f;

	private float dragLastPosY = 0.0f;

	private float dragRotateY = 0.0f;

	/** Zeichne die rotierten Winkelhalbierenden an den Vertices */
	private boolean debugRotatedBisectors = false;

	/** Zeichne die Winkelhalbierenden */
	private boolean debugBisectors = false;

	/** Zeichne die Events an ihre Auftretenskoordinaten */
	private boolean debugEvents = false;

	/** Zeichne die Normalenvektoren der Vertices */
	private boolean debugNormals = false;

	/** Zeichne die Eltern-Kind-Beziehungen zwischen zwei Iterationslevels */
	private boolean debugDrawConnections = false;

	/** Zeichne die Nachbarn der Vertices ein */
	private boolean debugDrawNeighbours = false;

	/** Zeichne die alte Result-Struktur */
	private boolean debugDrawResult = false;

	/** Zeichne die Normalenvektoren der Kanten */
	private boolean debugDrawEdgeNormals = false;

	/** Zeichne die Normalenvektoren der schraegen Ebenen an den Polygonkanten */
	private boolean debugEdgePlaneNormal = false;

	/** Zeichne die rotierten Normalenvektoren der Kanten */
	private boolean debugEdgeRotatedNormal = false;

	/** Zeichne die schraegen Ebenen an die Kanten */
	private boolean debugEdgePlanes = false;

	/** Zeichne alle berechneten Schnittpunkte */
	private boolean debugSchnittpunkte = false;

	/** Zeichne die berechneten Ergebnisstrukturen */
	private boolean drawFinalResultStructure = true;

	/** Zeichne die berechneten virtuellen Kanten */
	private boolean drawVirtualEdges = false;

	/** Zeichne die Texturen */
	private boolean drawTextures = false;

	/** Schriftart zum Zeichnen der Framerate etc. */
	private transient final PFont mFont = createFont("sans-serif", 15);;

	/** Pfad zur Konfigurationsdatei */
	private static String mConfigPath = "ressource/Config/SystemConfiguration.xml";

	// ------------------------------------------------------------------------------------------

	@Override
	public void setup() {

		size(1000, 700, OPENGL);

		// verwende normalisierte Texturkoordianten im Texturraum
		textureMode(PConstants.NORMAL);
		drawInput = false;
		generateGeometry();

		// intialisiere den Texturmanager ausserhalb der Verarbeitung
		ConfigurationController config = new ConfigurationController();
		File confFile = new File(mConfigPath);
		SystemConfiguration sysConf = config
				.processSystemConfiguration(confFile);

		TextureManagement textureManager = TextureManagement.getInstance();
		textureManager.initializeTextureManagement(this, sysConf);
		mRoofTexture = textureManager
				.getTextureForCategory(TextureCategory.Roof);

		if (!drawInput) {
			// testAll();
			drawSpecific(4);
		}
	}

	// ------------------------------------------------------------------------------------------
	private void drawInput(Integer config) {

		List<Vertex3d> vertices = mConfigs.get(config).getVertices();
		assert vertices != null : "FEHLER: Fuer den uebergebenen Key '"
				+ config + "' existiert keine Konfiguration";
		Iterator<Vertex3d> vertIter = vertices.iterator();
		Vertex3d currentVertex = null;

		background(255);
		stroke(0);
		strokeWeight(3);
		// fill(255, 100, 100);
		translate(200, 200, 0);
		rotateX(-dragRotateY);
		rotateY(dragRotateX);

		beginShape();

		while (vertIter.hasNext()) {
			currentVertex = vertIter.next();
			vertex(currentVertex.getX(), currentVertex.getY(),
					currentVertex.getZ());
		}
		endShape(PConstants.CLOSE);

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode dient der Reaktion auf Mauseingaben. Die Methode wird automatisch
	 * von der Parent-PApplet-Klasse aufgerufen, sobald Mausbewegungen
	 * festgestellt werden
	 */
	public void mouseDraggedCurrent() {
		if (dragLastPosX > mouseX + dragTreshold) {
			dragRotateX += dragDelta;
		} else if (dragLastPosX < mouseX - dragTreshold) {
			dragRotateX -= dragDelta;
		}

		// normalisiere auf Bereich 0-360°
		if (dragRotateX > 720.0f || dragRotateX < -360.0f)
			dragRotateX = 0.0f;

		if (dragLastPosY > mouseY + dragTreshold) {
			dragRotateY += dragDelta;
		} else if (dragLastPosX < mouseY - dragTreshold) {
			dragRotateY -= dragDelta;
		}

		if (dragRotateY > 720.0f || dragRotateY < -360.0f)
			dragRotateY = 0.0f;

		// speichere Positionen des aktuellen Frames
		dragLastPosX = mouseX;
		dragLastPosY = mouseY;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode berechnet alle Konfigurationen, zeichnet diese aber nicht,
	 * sondern faengt nur Exceptions ab
	 */
	private void testAll() {

		// deaktiviere das automatische Zeichnen, hier geht es nur um das
		// Abfangen von Exceptions
		this.noLoop();

		// hole alle Schluessel
		Set<Integer> keys = mConfigs.keySet();
		Integer current = null;
		List<Integer> errorConfigs = new ArrayList<Integer>();

		Texture texture = TextureManagement.getInstance()
				.getTextureForCategory(TextureCategory.Roof);
		int count = 0;
		SkeletonRoofDescriptor currentConfig = null;

		Map<Integer, Float> accuracyDeviations = new HashMap<Integer, Float>();

		String output = "";
		String lineBreak = System.getProperty("line.separator");

		Iterator<Integer> keyIter = keys.iterator();

		while (keyIter.hasNext()) {
			current = keyIter.next();
			try {
				LOGGER.info("NEXT...");
				currentConfig = mConfigs.get(current);
				assert currentConfig.getVertices().size() > 0 : "FEHLER: Die ArrayList enthaelt keine Vertices";
				currentConfig.setTexture(texture);
				count++;
				output = lineBreak
						+ "------------------------------------------------------------------------------------"
						+ lineBreak;
				output += "Berechne Konfiguration " + count
						+ " mit Schluessel:" + current + " mit "
						+ mConfigs.get(current).getVertices().size()
						+ " Vetices" + lineBreak;
				LOGGER.info("Config:Berechne Konfiguration " + count
						+ " mit Schluessel:" + current);
				StraightSkeletonController skeletonController = new StraightSkeletonController(
						currentConfig);

				accuracyDeviations.put(current,
						skeletonController.getMaxAccuracyDeviation());

				output += "------------------------------------------------------------------------------------"
						+ lineBreak;
				output += "Berechnung abgeschlossen" + lineBreak;
				output += "------------------------------------------------------------------------------------"
						+ lineBreak;
				LOGGER.info(output);

			} catch (AccuracyException e) {
				LOGGER.error("ACCURACY-EXCEPTION BEI CONFIG " + current + "!");
				e.printStackTrace();
				errorConfigs.add(current);
			} catch (SquareCaseException e) {
				LOGGER.error("SQUARE-CASE-EXCEPTION BEI CONFIG " + current
						+ "!");
				e.printStackTrace();
				errorConfigs.add(current);
			}

			catch (AssertionError e) {
				LOGGER.error("ASSERTION-ERROR BEI CONFIG " + current + "!");
				e.printStackTrace();
				errorConfigs.add(current);
			} catch (Exception e) {
				LOGGER.error("EXCEPTION BEI CONFIG " + current + "!");
				e.printStackTrace();
				errorConfigs.add(current);
			}
		}

		LOGGER.info("Insgesamt wurden " + count
				+ " Konfigurationen berechnet, von diesen waren "
				+ errorConfigs.size() + " fehlerhaft");
		if (errorConfigs.size() > 0) {
			output = "Fehlerhafte Konfigurationen: " + lineBreak;
			for (int i = 0; i < errorConfigs.size(); i++) {
				output += "Konfiguration: " + errorConfigs.get(i) + lineBreak;
			}
		}
		LOGGER.info(output);

		output = "Abweichungen: " + lineBreak;
		Set<Integer> keysAccuracy = accuracyDeviations.keySet();
		Iterator<Integer> keyIterAccuracy = keysAccuracy.iterator();
		Integer currentKey = null;
		while (keyIterAccuracy.hasNext()) {
			currentKey = keyIterAccuracy.next();
			output += "Konfiguration " + currentKey + " Abweichung: "
					+ accuracyDeviations.get(currentKey) + lineBreak;
		}
		LOGGER.info(output);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet und zeichnet genau eine Konfiguration
	 * 
	 * @param key
	 *            Schluessel der Konfiguration, die gezeichnet werden soll
	 */
	private void drawSpecific(Integer key) {

		// intialisiere den Texturmanager ausserhalb der Verarbeitung
		TextureManagement textureManager = TextureManagement.getInstance();
		textureManager.setParent(this);
		Texture texture = textureManager
				.getTextureForCategory(TextureCategory.Roof);

		SkeletonRoofDescriptor currentConfig = null;
		try {
			currentConfig = mConfigs.get(key);
			currentConfig.setTexture(texture);
			StraightSkeletonController skeletonController = new StraightSkeletonController(
					currentConfig);
			Float accuracyDeviation = skeletonController
					.getMaxAccuracyDeviation();

			mEvents = skeletonController.getEvents();
			mSchnittpunkte = skeletonController.getSchnittpunktBuffer();
			mResultComplex = skeletonController.getResultComplex();
			mVirtualEdges = skeletonController.getVirtualEdges();
			mPolygons = skeletonController.getAllPolygons();

			LOGGER.info("Abweichung: " + accuracyDeviation);
		} catch (AccuracyException e) {
			LOGGER.error("ACCURACY EXCEPTION");
			e.printStackTrace();

		} catch (SquareCaseException e) {
			LOGGER.error("SQUARECASE EXCEPTION");
			e.printStackTrace();
		} catch (AssertionError e) {
			LOGGER.error("ASSERTION ERROR");
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode zeichnet in Abhaengigkeit von den gesetzten Draw-Flags. Wird von
	 * der Applet-Methode automatisch in jedem Frame gecallt.
	 */
	@Override
	public void draw() {

		background(255);
		stroke(0);
		fill(100, 100, 100, 10);
		translate(200, 200, 0);
		rotateX(-dragRotateY);
		rotateY(dragRotateX);

		if (!drawInput) {
			if (debugEvents) {
				drawEventCoordinates();
			}

			if (debugSchnittpunkte) {
				drawSchnittpunkte();
			}

			if (drawFinalResultStructure) {
				drawFinalResultStructures();
			}

			if (drawVirtualEdges) {
				drawVirtualEdges();
			}
			List<SkeletonPolygon> polygonVector = null;
			SkeletonPolygon currentPolygon = null;
			if (mPolygons != null) {
				for (int i = 0; i < mPolygons.size(); i++) {

					// hole den Polygon-Buffer fuer das aktuell verarbeitete
					// Level
					polygonVector = mPolygons.get(i);

					// zeichne alle Polygone innerhalb des Buffers
					Iterator<SkeletonPolygon> polygonIter = polygonVector
							.iterator();
					while (polygonIter.hasNext()) {
						currentPolygon = polygonIter.next();
						drawPolygon(currentPolygon);
					}
				}
			}
		} else {
			drawInput(-17);
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode dient der Reaktion auf Mauseingaben. Die Methode wird automatisch
	 * von der Parent-PApplet-Klasse aufgerufen, sobald Mausbewegungen
	 * festgestellt werden
	 */
	@Override
	public void mouseDragged() {
		if (dragLastPosX > mouseX + dragTreshold) {
			dragRotateX += dragDelta;
		} else if (dragLastPosX < mouseX - dragTreshold) {
			dragRotateX -= dragDelta;
		}

		// normalisiere auf Bereich 0-360°
		if (dragRotateX > 720.0f || dragRotateX < -360.0f)
			dragRotateX = 0.0f;

		if (dragLastPosY > mouseY + dragTreshold) {
			dragRotateY += dragDelta;
		} else if (dragLastPosX < mouseY - dragTreshold) {
			dragRotateY -= dragDelta;
		}

		if (dragRotateY > 720.0f || dragRotateY < -360.0f)
			dragRotateY = 0.0f;

		// speichere Positionen des aktuellen Frames
		dragLastPosX = mouseX;
		dragLastPosY = mouseY;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Zeichenroutine zum Zeichnen eines Polygons beschrieben durch eine Menge
	 * von Vertices. Diese Methode wertet einen Grossteil der Zeichenflags aus
	 * und realisiert neben dem Zeichnen des eigentlichen Polygons eine Vielzahl
	 * von graphischen Debugginghilfen
	 * 
	 * @param polygon
	 *            Polygon, das durch die Methode gezeichnet werden soll
	 */
	private void drawPolygon(final SkeletonPolygon polygon) {

		SkeletonVertex currentVertex = null, neighbour0 = null, childCurrentVertex = null;
		MyVector3f start = null, direction = null, normal = null, position = null, position2 = null;
		Ray bisector = null, rotatedWinkelhalbierende = null;
		Iterator<SkeletonVertex> vertIter = null;

		// zeichne das Polygon basierend auf den Kanten zwischen den Nachbarn
		vertIter = polygon.getVertices().iterator();
		beginShape(PConstants.LINES);
		while (vertIter.hasNext()) {

			currentVertex = vertIter.next();
			neighbour0 = currentVertex.getNeighbourByIndex(0);
			position = currentVertex.getPositionPtr();
			position2 = neighbour0.getPositionPtr();

			vertex(position.x, position.y, position.z);
			vertex(position2.x, position2.y, position2.z);

		}
		endShape();

		// zeichne Winkelhalbierende
		if (debugBisectors) {

			vertIter = polygon.getVertices().iterator();

			while (vertIter.hasNext()) {
				currentVertex = vertIter.next();
				beginShape(PConstants.LINE);
				pushStyle();
				stroke(255, 0, 0);
				bisector = currentVertex.getWinkelhalbierende();
				// wenn die Vertex-Anzahl < 3 ist, gibt es keine Halbierenden
				// mehr
				if (bisector == null)
					continue;
				start = bisector.getStartPtr();
				direction = bisector.getDirection();
				direction.scale(20);
				vertex(start.x, start.y, start.z);
				vertex(start.x + direction.x, start.y + direction.y, start.z
						+ direction.z);
				popStyle();
				endShape();
			}

		}

		// zeichne Normalen
		if (debugNormals) {

			vertIter = polygon.getVertices().iterator();
			while (vertIter.hasNext()) {
				currentVertex = vertIter.next();
				normal = currentVertex.getNormal();

				if (normal != null) {
					beginShape(PConstants.LINE);
					normal.scale(30);
					vertex(currentVertex.getX(), currentVertex.getY(),
							currentVertex.getZ());
					vertex(currentVertex.getX() + normal.x,
							currentVertex.getY() + normal.y,
							currentVertex.getZ() + normal.z);
					endShape();
				}

			}

		}

		if (debugDrawConnections) {
			SkeletonVertex childVertex = null;
			vertIter = polygon.getVertices().iterator();
			while (vertIter.hasNext()) {
				currentVertex = vertIter.next();
				if (currentVertex.hasChild()) {
					childVertex = currentVertex.getChild();
				} else
					continue;
				beginShape(PConstants.LINE);
				pushStyle();
				stroke(0, 255, 0);
				vertex(currentVertex.getX(), currentVertex.getY(),
						currentVertex.getZ());
				vertex(childVertex.getX(), childVertex.getY(),
						childVertex.getZ());
				popStyle();
				endShape();
			}
		}

		// zeichne die rotierten Winkelhalbierenden
		if (debugRotatedBisectors) {

			vertIter = polygon.getVertices().iterator();
			while (vertIter.hasNext()) {

				currentVertex = vertIter.next();
				position = currentVertex.getPosition();

				rotatedWinkelhalbierende = currentVertex
						.getRotatedWinkelhalbierende();
				if (rotatedWinkelhalbierende == null)
					continue;
				direction = rotatedWinkelhalbierende.getDirection();
				direction.scale(200);
				pushStyle();
				stroke(255, 0, 0);
				beginShape(PConstants.LINE);

				vertex(position.x, position.y, position.z);
				vertex(position.x + direction.x, position.y + direction.y,
						position.z + direction.z);
				endShape();
				popStyle();

				// und die orthogonalen Vektoren an den Vertices

				direction = currentVertex.getOrtho();
				direction.scale(10);
				pushStyle();
				stroke(0, 255, 0);
				beginShape(PConstants.LINE);
				vertex(position.x - direction.x, position.y - direction.y,
						position.z - direction.z);
				vertex(position.x + direction.x, position.y + direction.y,
						position.z + direction.z);
				endShape();
				popStyle();

			}
		}

		// zeichne die Nachbarn => immer die Kind-Nachbar-Beziehungen
		if (debugDrawNeighbours) {
			vertIter = polygon.getVertices().iterator();
			while (vertIter.hasNext()) {
				currentVertex = vertIter.next();
				childCurrentVertex = currentVertex.getChild();
				position = childCurrentVertex.getPositionPtr();
				if (!childCurrentVertex.hasNeighbourWithIndex(0))
					continue;
				neighbour0 = childCurrentVertex.getNeighbourByIndex(0);
				position2 = neighbour0.getPositionPtr();
				beginShape(PConstants.LINE);
				pushStyle();
				stroke(255, 0, 0);
				vertex(position.x, position.y, position.z);
				vertex(position2.x, position2.y, position2.z);
				popStyle();
				endShape();
			}
		}

		if (debugDrawEdgeNormals) {
			vertIter = polygon.getVertices().iterator();
			MyVector3f edgeNormal = null, edgeStart = null, edgeDirection = null, edgeCenter = null;
			float edgeLength = -1.0f;
			SkeletonEdge currentEdge = null;
			while (vertIter.hasNext()) {
				currentVertex = vertIter.next();
				currentEdge = currentVertex.getNeighbourEdgeByIndex(0);
				edgeNormal = currentEdge.getNormal();

				edgeNormal.scale(30.0f);
				edgeLength = currentEdge.getEdgeLength();
				edgeDirection = currentEdge.getDirection();
				edgeStart = currentEdge.getStart();

				edgeDirection.normalize();

				// berechne die Mitte der Kante
				edgeLength /= 2.0f;
				edgeCenter = new MyVector3f();
				edgeCenter.scale(edgeLength, edgeDirection);
				edgeCenter.add(edgeStart);

				// zeichne die Normale
				beginShape();
				pushStyle();
				stroke(0.0f, 0.0f, 255.0f);
				vertex(edgeCenter.x, edgeCenter.y, edgeCenter.z);
				vertex(edgeCenter.x + edgeNormal.x,
						edgeCenter.y + edgeNormal.y, edgeNormal.z
								+ edgeCenter.z);
				popStyle();
				endShape();
			}
		}

		if (debugEdgePlaneNormal) {
			vertIter = polygon.getVertices().iterator();
			MyVector3f edgeNormal = null, edgeStart = null, edgeDirection = null, edgeCenter = null;
			float edgeLength = -1.0f;
			SkeletonEdge currentEdge = null;
			while (vertIter.hasNext()) {
				currentVertex = vertIter.next();
				currentEdge = currentVertex.getNeighbourEdgeByIndex(0);

				edgeNormal = currentEdge.getRotatedNormal();

				edgeNormal.scale(50.0f);
				edgeLength = currentEdge.getEdgeLength();
				edgeDirection = currentEdge.getDirection();
				edgeDirection.normalize();
				edgeStart = currentEdge.getStart();

				// berechne die Mitte der Kante
				edgeLength /= 2.0f;
				edgeCenter = new MyVector3f();
				edgeCenter.scale(edgeLength, edgeDirection);
				edgeCenter.add(edgeStart);

				// zeichne die Normale
				beginShape();
				pushStyle();
				stroke(0.0f, 0.0f, 0.0f);
				vertex(edgeCenter.x, edgeCenter.y, edgeCenter.z);
				vertex(edgeCenter.x + edgeNormal.x,
						edgeCenter.y + edgeNormal.y, edgeNormal.z
								+ edgeCenter.z);
				popStyle();
				endShape();
			}
		}
		if (debugEdgeRotatedNormal) {
			vertIter = polygon.getVertices().iterator();
			MyVector3f edgeNormal = null, edgeStart = null, edgeDirection = null, edgeCenter = null;
			float edgeLength = -1.0f;
			SkeletonEdge currentEdge = null;
			while (vertIter.hasNext()) {
				currentVertex = vertIter.next();
				currentEdge = currentVertex.getNeighbourEdgeByIndex(0);
				// edgeNormal = currentEdge.getNormal();
				// edgeNormal = currentEdge.getRotatedNormal();
				edgeNormal = currentEdge.getPlane().getNormal();
				edgeNormal.scale(50.0f);
				edgeLength = currentEdge.getEdgeLength();
				edgeDirection = currentEdge.getDirection();
				edgeDirection.normalize();
				edgeStart = currentEdge.getStart();

				// berechne die Mitte der Kante
				edgeLength /= 2.0f;
				edgeCenter = new MyVector3f();
				edgeCenter.scale(edgeLength, edgeDirection);
				edgeCenter.add(edgeStart);

				// zeichne die Normale
				beginShape();
				pushStyle();
				stroke(255.0f, 0.0f, 0.0f);
				vertex(edgeCenter.x, edgeCenter.y, edgeCenter.z);
				vertex(edgeCenter.x + edgeNormal.x,
						edgeCenter.y + edgeNormal.y, edgeNormal.z
								+ edgeCenter.z);
				popStyle();
				endShape();
			}
		}

		if (debugEdgePlanes) {
			List<MyVector3f> pointsOnPlane = null;
			MyVector3f currentPoint = null;
			SkeletonEdge currentEdge = null;
			vertIter = polygon.getVertices().iterator();
			Iterator<MyVector3f> pointIter = null;
			while (vertIter.hasNext()) {
				currentVertex = vertIter.next();
				currentEdge = currentVertex.getNeighbourEdgeByIndex(0);
				pointsOnPlane = currentEdge.getPointsOnPlane();
				pointIter = pointsOnPlane.iterator();
				beginShape(PConstants.QUAD);
				pushStyle();
				fill(0.0f, 140.0f, 0.0f, 100.0f);
				while (pointIter.hasNext()) {
					currentPoint = pointIter.next();
					vertex(currentPoint.x, currentPoint.y, currentPoint.z);
				}
				popStyle();
				endShape();

			}
		}
	}

	// ------------------------------------------------------------------------------------------
	/** Methode zeichnet Events inklusive Koordinaten und Ort des Auftretens */
	private void drawEventCoordinates() {
		textFont(mFont, 20);
		int number = 0;
		MyVector3f position = null;
		iStraightSkeletonEvent currentEvent = null;
		Iterator<iStraightSkeletonEvent> eventIter = mEvents.iterator();
		String text = "";

		while (eventIter.hasNext()) {
			currentEvent = eventIter.next();
			// if(!currentEvent.getType().equals("SplitEvent")) continue;
			position = currentEvent.getVertex().getPositionPtr();
			pushStyle();
			// text = String.valueOf(number) + ":" +
			// currentEvent.getType().substring(0, 1) + " " +
			// position.toString();
			text = "  " + String.valueOf(number);

			textFont(mFont, 10);
			fill(255, 0, 0);
			text(text, position.x, position.y, position.z);
			popStyle();
			number++;

		}

		// zeichne Punkte an die Vertex-Koordinaten, die
		eventIter = mEvents.iterator();

		pushStyle();
		while (eventIter.hasNext()) {
			currentEvent = eventIter.next();
			// if(!currentEvent.getType().equals("SplitEvent")) continue;
			position = currentEvent.getVertex().getPositionPtr();

			if (currentEvent.getType().equals("SplitEvent"))
				position = currentEvent.getSchnittpunkt();

			fill(255, 0, 0, 100);
			stroke(255, 0, 0, 100);
			pushMatrix();
			translate(position.x, position.y, position.z);
			sphere(3);
			popMatrix();

		}
		popStyle();

	}

	// ------------------------------------------------------------------------------------------
	/** Methode zeichnet saemtliche im Schnittpunktbuffer befindlichen Vertices */
	private void drawSchnittpunkte() {

		Iterator<MyVector3f> schnittpunktIter = mSchnittpunkte.iterator();
		MyVector3f currentSchnittpunkt = null;
		pushStyle();
		fill(0, 0, 255, 100);
		stroke(0, 0, 255, 100);
		while (schnittpunktIter.hasNext()) {

			currentSchnittpunkt = schnittpunktIter.next();
			pushMatrix();
			translate(currentSchnittpunkt.x, currentSchnittpunkt.y,
					currentSchnittpunkt.z);
			sphere(2);
			popMatrix();
		}
		popStyle();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode durchlaeuft alle Faces des Results und zeichnet jedes Face als
	 * Folge von Punkten
	 */
	private void drawFinalResultStructures() {
		ResultFace currentFace = null;

		// sofern mittels testall viele Konfigurationen gerechnet wurden, werden
		// keine
		// konkreten Ergebniselemente aus den Berechnungen gezogen, in diesem
		// Fall existiert kein
		// Ergebnisobjekt
		if (mResultComplex != null) {
			List<ResultFace> faces = mResultComplex.getFaces();

			Iterator<ResultFace> faceIter = faces.iterator();
			while (faceIter.hasNext()) {
				currentFace = faceIter.next();
				drawFace(currentFace);
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode zeichnet ein Face der neuen ResultComplex-Strukturen. Ein Face
	 * besteht dabei aus einer beliebigen Anzahl von Elementen, die jeweils
	 * einzeln gezeichnet werden.
	 * 
	 * @param face
	 *            Das zu zeichnende Face aus der vollstaendigen Result-Struktur
	 */
	private void drawFace(ResultFace face) {

		AbstractResultElement currentElement = null;
		List<AbstractResultElement> elements = face.getElements();
		Iterator<AbstractResultElement> elementIter = elements.iterator();
		List<Vertex3d> currentPoints = null;
		Vertex3d currentPoint = null;
		MyVector2f textureCoords = null;

		MyVector3f newPos = new MyVector3f(519.7f, 412.3f, 30.0f);
		MyVector3f wrongPos = new MyVector3f(519.6f, 412.3f, 30.0f);

		while (elementIter.hasNext()) {
			currentElement = elementIter.next();
			currentPoints = currentElement.getPoints();
			// System.out.println(currentElement);
			beginShape();

			if (drawTextures)
				texture(mRoofTexture.getTexture());
			pushStyle();
			fill(face.getDrawColorModR(), face.getDrawColorModG(),
					face.getDrawColorModB());
			// stroke(face.getDrawColorModR(), face.getDrawColorModG(),
			// face.getDrawColorModB());
			for (int i = 0; i < currentPoints.size(); i++) {
				currentPoint = currentPoints.get(i);
				if (currentPoint.getPosition().equals(wrongPos)) {
					// System.out.println("CHANGED");
					// currentPoint.setPosition(newPos);
				}

				if (drawTextures) {
					textureCoords = currentElement.getTextureCoordsByIndex(i);
					// System.out.println("Koordinaten: " + currentPoint);
					// System.out.println("Texturkoordinaten: " +
					// textureCoords);

					vertex(currentPoint.getX(), currentPoint.getY(),
							currentPoint.getZ(), textureCoords.x,
							textureCoords.y);
				}

				else
					vertex(currentPoint.getX(), currentPoint.getY(),
							currentPoint.getZ());
			}
			popStyle();
			endShape(PConstants.CLOSE);

		}

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode zeichnet saemtliche waehrend der Berechnung aufgetretenen
	 * virtuellen Kanten
	 */
	private void drawVirtualEdges() {

		Iterator<Ray> edgeIter = mVirtualEdges.iterator();
		MyVector3f start = null, direction = null, end = null;
		Ray currentRay = null;

		while (edgeIter.hasNext()) {
			currentRay = edgeIter.next();
			start = currentRay.getStartPtr();
			direction = currentRay.getDirectionPtr();
			end = new MyVector3f();
			end.add(start, direction);
			beginShape();
			pushStyle();
			stroke(255, 0, 0);
			strokeWeight(2.0f);
			vertex(start.x, start.y, start.z);
			vertex(end.x, end.y, end.z);
			popStyle();
			endShape();
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Verarbeitung von Nutzereingaben waehrend der Laufzeit. Wird von der
	 * PApplt-Parent-Instanz im Falle von Nutzereingaben aufgerufen
	 */
	@Override
	public void keyPressed() {

		if (!drawInput) {
			switch (key) {
			case 'b':
				if (debugBisectors)
					debugBisectors = false;
				else
					debugBisectors = true;
				break;
			case 'e':
				if (debugEvents)
					debugEvents = false;
				else
					debugEvents = true;
				break;
			case 'n':
				if (debugNormals)
					debugNormals = false;
				else
					debugNormals = true;
				break;
			case 'c':
				if (debugDrawConnections)
					debugDrawConnections = false;
				else
					debugDrawConnections = true;
				break;
			case 'q':
				if (debugDrawNeighbours)
					debugDrawNeighbours = false;
				else
					debugDrawNeighbours = true;
				break;
			case 'r':
				if (debugDrawResult)
					debugDrawResult = false;
				else
					debugDrawResult = true;
				break;
			case 'p':
				if (debugEdgePlanes)
					debugEdgePlanes = false;
				else
					debugEdgePlanes = true;
				break;
			case 's':
				if (debugSchnittpunkte)
					debugSchnittpunkte = false;
				else
					debugSchnittpunkte = true;
				break;
			case 'f':
				if (drawFinalResultStructure)
					drawFinalResultStructure = false;
				else
					drawFinalResultStructure = true;
				break;
			case 't':
				if (drawTextures)
					drawTextures = false;
				else
					drawTextures = true;
				break;
			case 'o':
				if (debugRotatedBisectors)
					debugRotatedBisectors = false;
				else
					debugRotatedBisectors = true;
				break;
			case 'i':
				if (drawVirtualEdges)
					drawVirtualEdges = false;
				else
					drawVirtualEdges = true;
				break;
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode enthaelt saemtliche Beispielgrundrisse inklusive Gewichten etc.
	 */
	private void generateGeometry() {

		SkeletonRoofDescriptor roofDescriptor = null;
		List<Vertex3d> vertices = new ArrayList<Vertex3d>();
		List<Float> weights = new ArrayList<Float>();
		mConfigs = new HashMap<Integer, SkeletonRoofDescriptor>();

		vertices.add(new Vertex3d(425.0f, 299.6699f, -75.0f));
		vertices.add(new Vertex3d(575.0f, 299.6699f, -75.0f));
		vertices.add(new Vertex3d(575.0f, 299.6699f, -74.46404f));
		vertices.add(new Vertex3d(589.67413f, 299.6699f, -74.46404f));
		vertices.add(new Vertex3d(589.67413f, 299.6699f, -31.00471f));
		vertices.add(new Vertex3d(575.0f, 299.6699f, -31.004707f));
		vertices.add(new Vertex3d(575.0f, 299.6699f, -23.780111f));
		vertices.add(new Vertex3d(587.87384f, 299.6699f, -23.780111f));
		vertices.add(new Vertex3d(587.87384f, 299.6699f, 23.780111f));
		vertices.add(new Vertex3d(575.0f, 299.6699f, 23.780113f));
		vertices.add(new Vertex3d(575.0f, 299.6699f, 37.402164f));
		vertices.add(new Vertex3d(586.0618f, 299.6699f, 37.402164f));
		vertices.add(new Vertex3d(586.0618f, 299.6699f, 68.06659f));
		vertices.add(new Vertex3d(575.0f, 299.6699f, 68.06659f));
		vertices.add(new Vertex3d(575.0f, 299.6699f, 75.0f));
		vertices.add(new Vertex3d(527.73175f, 299.6699f, 75.0f));
		vertices.add(new Vertex3d(527.73175f, 299.6699f, 95.84302f));
		vertices.add(new Vertex3d(472.26828f, 299.6699f, 95.84302f));
		vertices.add(new Vertex3d(472.26828f, 299.6699f, 75.0f));
		vertices.add(new Vertex3d(463.26788f, 299.6699f, 75.0f));
		vertices.add(new Vertex3d(463.26788f, 299.6699f, 83.50408f));
		vertices.add(new Vertex3d(431.26337f, 299.6699f, 83.504074f));
		vertices.add(new Vertex3d(431.26337f, 299.6699f, 75.0f));
		vertices.add(new Vertex3d(425.0f, 299.6699f, 75.0f));

		weights.add(1.0f);
		weights.add(0.7f);
		weights.add(1.0f);
		weights.add(0.7f);
		weights.add(1.0f);
		weights.add(0.7f);
		weights.add(1.0f);
		weights.add(0.7f);
		weights.add(1.0f);
		weights.add(0.7f);
		weights.add(1.0f);
		weights.add(0.7f);
		weights.add(1.0f);
		weights.add(0.7f);
		weights.add(1.0f);
		weights.add(0.7f);
		weights.add(1.0f);
		weights.add(0.7f);
		weights.add(1.0f);
		weights.add(0.7f);
		weights.add(1.0f);
		weights.add(0.7f);
		weights.add(1.0f);

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		// roofDescriptor.setEdgeWeights(weights);
		mConfigs.put(-21, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();
		weights = new ArrayList<Float>();

		// Konfiguration 20 funktioniert mit Gewichten nur dann, wenn man die
		// Toleranz bei der Geradenschnittberechnung auf uber 2.4 stellt
		// das soll allerdings zunaechst vermieden werden, da sonst die
		// Berechnungen insgesamt zu ungenau werden
		vertices.add(new Vertex3d(475.10522f, 242.46588f, 85.05064f));
		vertices.add(new Vertex3d(433.10254f, 242.46588f, 82.65611f));
		vertices.add(new Vertex3d(425.0f, 242.46588f, 75.0f));
		vertices.add(new Vertex3d(409.9425f, 242.46588f, 28.984722f));
		vertices.add(new Vertex3d(409.9425f, 242.46588f, -28.984722f));
		vertices.add(new Vertex3d(412.49673f, 242.46588f, -79.158936f));
		vertices.add(new Vertex3d(459.2034f, 242.46588f, -98.64528f));
		vertices.add(new Vertex3d(540.79663f, 242.46588f, -98.64528f));
		vertices.add(new Vertex3d(574.62335f, 242.46588f, -85.41763f));
		vertices.add(new Vertex3d(591.6676f, 242.46588f, -54.037224f));
		vertices.add(new Vertex3d(591.6676f, 242.46588f, -9.244028f));
		vertices.add(new Vertex3d(589.5062f, 242.46588f, 73.03097f));
		vertices.add(new Vertex3d(503.80103f, 242.46588f, 85.05064f));

		weights.add(0.7f);
		weights.add(0.7f);
		weights.add(1.0f);
		weights.add(1.0f);
		weights.add(1.0f);
		weights.add(0.7f);
		weights.add(0.7f);
		weights.add(0.7f);
		weights.add(1.0f);
		weights.add(1.0f);
		weights.add(1.0f);
		weights.add(0.7f);
		weights.add(0.7f);

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		roofDescriptor.setEdgeWeights(weights);
		mConfigs.put(-20, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();
		weights = new ArrayList<Float>();

		vertices.add(new Vertex3d(358.8043f, 300.0f, 0.0f));
		vertices.add(new Vertex3d(362.2633f, 300.0f, -12.909289f));
		vertices.add(new Vertex3d(371.71356f, 300.0f, -22.359545f));
		vertices.add(new Vertex3d(382.7915f, 300.0f, -25.327866f));
		vertices.add(new Vertex3d(382.7915f, 300.0f, -95.33562f));
		vertices.add(new Vertex3d(617.2085f, 300.0f, -95.33562f));
		vertices.add(new Vertex3d(617.2085f, 300.0f, 95.33562f));
		vertices.add(new Vertex3d(382.7915f, 300.0f, 95.33562f));
		vertices.add(new Vertex3d(382.7915f, 300.0f, 25.327864f));
		vertices.add(new Vertex3d(371.71356f, 300.0f, 22.359545f));
		vertices.add(new Vertex3d(362.2633f, 300.0f, 12.909289f));

		weights.add(0.7f);
		weights.add(0.7f);
		weights.add(0.7f);
		weights.add(1.0f);
		weights.add(0.7f);
		weights.add(1.0f);
		weights.add(0.7f);
		weights.add(1.0f);
		weights.add(0.7f);
		weights.add(0.7f);
		weights.add(0.7f);

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		roofDescriptor.setEdgeWeights(weights);
		mConfigs.put(-19, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();
		weights = new ArrayList<Float>();

		vertices.add(new Vertex3d(381.79364f, 179.15369f, -7.028322f));
		vertices.add(new Vertex3d(387.51102f, 179.15369f, -19.869707f));
		vertices.add(new Vertex3d(397.95712f, 179.15369f, -29.275436f));
		vertices.add(new Vertex3d(411.3258f, 179.15369f, -33.61918f));
		vertices.add(new Vertex3d(413.50787f, 179.15369f, -33.38983f));
		vertices.add(new Vertex3d(413.50787f, 179.15369f, -85.88752f));
		vertices.add(new Vertex3d(478.51614f, 179.15369f, -85.88752f));
		vertices.add(new Vertex3d(478.51614f, 179.15369f, -101.90865f));
		vertices.add(new Vertex3d(521.4838f, 179.15369f, -101.90866f));
		vertices.add(new Vertex3d(521.4838f, 179.15369f, -85.88752f));
		vertices.add(new Vertex3d(586.4921f, 179.15369f, -85.88752f));
		vertices.add(new Vertex3d(586.4921f, 179.15369f, -15.291444f));
		vertices.add(new Vertex3d(593.0061f, 179.15369f, -15.291444f));
		vertices.add(new Vertex3d(593.0061f, 179.15369f, 15.291444f));
		vertices.add(new Vertex3d(586.4921f, 179.15369f, 15.2914505f));
		vertices.add(new Vertex3d(586.4921f, 179.15369f, 85.88752f));
		vertices.add(new Vertex3d(533.23004f, 179.15369f, 85.88752f));
		vertices.add(new Vertex3d(533.23004f, 179.15369f, 112.72526f));
		vertices.add(new Vertex3d(466.76996f, 179.15369f, 112.725266f));
		vertices.add(new Vertex3d(466.76996f, 179.15369f, 85.88752f));
		vertices.add(new Vertex3d(413.50787f, 179.15369f, 85.88752f));
		vertices.add(new Vertex3d(413.50787f, 179.15369f, 33.38983f));
		vertices.add(new Vertex3d(411.3258f, 179.15369f, 33.61918f));
		vertices.add(new Vertex3d(397.95712f, 179.15369f, 29.275436f));
		vertices.add(new Vertex3d(387.51102f, 179.15369f, 19.869707f));
		vertices.add(new Vertex3d(381.79364f, 179.15369f, 7.028322f));

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		// roofDescriptor.setEdgeWeights(weights);
		mConfigs.put(-18, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();
		weights = new ArrayList<Float>();

		vertices.add(new Vertex3d(27.506968f, -41.000015f, -28.124985f));
		vertices.add(new Vertex3d(89.235085f, -41.000015f, -28.124983f));
		vertices.add(new Vertex3d(89.235085f, -41.000015f, -89.42215f));
		vertices.add(new Vertex3d(310.76498f, -41.000015f, -89.42215f));
		vertices.add(new Vertex3d(310.76498f, -41.000015f, -37.5563f));
		vertices.add(new Vertex3d(373.92783f, -41.000015f, -37.5563f));
		vertices.add(new Vertex3d(373.92783f, -41.000015f, 37.55633f));
		vertices.add(new Vertex3d(310.76498f, -41.000015f, 37.556335f));
		vertices.add(new Vertex3d(310.76498f, -41.000015f, 89.42215f));
		vertices.add(new Vertex3d(89.235085f, -41.000015f, 89.42215f));
		vertices.add(new Vertex3d(89.235085f, -41.000015f, 28.12502f));
		vertices.add(new Vertex3d(27.506968f, -41.000015f, 28.12502f));

		weights.add(0.95450234f);
		weights.add(0.95450234f);
		weights.add(0.95450234f);
		weights.add(1.2265422f);
		weights.add(0.95450234f);
		weights.add(1.2265422f);
		weights.add(0.95450234f);
		weights.add(1.2265422f);
		weights.add(0.95450234f);
		weights.add(0.95450234f);
		weights.add(0.95450234f);
		weights.add(1.2265422f);

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		// roofDescriptor.setEdgeWeights(weights);
		mConfigs.put(-17, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();
		weights = new Vector<Float>();

		// -16
		vertices.add(new Vertex3d(100.0f, 450.0f, -20.0f));
		vertices.add(new Vertex3d(200.0f, 450.0f, -20.0f));
		vertices.add(new Vertex3d(200.0f, 450.0f, -140.0f));
		vertices.add(new Vertex3d(300.0f, 450.0f, -140.0f));
		vertices.add(new Vertex3d(300.0f, 450.0f, -100.0f));
		vertices.add(new Vertex3d(550.0f, 450.0f, -100.0f));
		vertices.add(new Vertex3d(550.0f, 450.0f, -7.5f));
		vertices.add(new Vertex3d(640.0f, 450.0f, -7.5f));
		vertices.add(new Vertex3d(640.0f, 450.0f, 67.5f));
		vertices.add(new Vertex3d(550.0f, 450.0f, 67.5f));
		vertices.add(new Vertex3d(550.0f, 450.0f, 84.45416f));
		vertices.add(new Vertex3d(552.7164f, 450.0f, 88.5195f));
		vertices.add(new Vertex3d(555.0f, 450.0f, 100.0f));
		vertices.add(new Vertex3d(552.7164f, 450.0f, 111.4805f));
		vertices.add(new Vertex3d(546.2132f, 450.0f, 121.2132f));
		vertices.add(new Vertex3d(536.4805f, 450.0f, 127.716385f));
		vertices.add(new Vertex3d(525.0f, 450.0f, 130.0f));
		vertices.add(new Vertex3d(513.5195f, 450.0f, 127.716385f));
		vertices.add(new Vertex3d(503.7868f, 450.0f, 121.2132f));
		vertices.add(new Vertex3d(500.0f, 450.0f, 115.54587f));
		vertices.add(new Vertex3d(500.0f, 450.0f, 150.0f));
		vertices.add(new Vertex3d(400.0f, 450.0f, 150.0f));
		vertices.add(new Vertex3d(400.0f, 450.0f, 100.0f));
		vertices.add(new Vertex3d(300.0f, 450.0f, 100.0f));
		vertices.add(new Vertex3d(300.0f, 450.0f, 160.0f));
		vertices.add(new Vertex3d(200.0f, 450.0f, 160.0f));
		vertices.add(new Vertex3d(200.0f, 450.0f, 80.0f));
		vertices.add(new Vertex3d(100.0f, 450.0f, 80.0f));

		/*
		 * weights.add(0.526475f); weights.add(0.9837615f);
		 * weights.add(0.526475f); weights.add(0.9837615f);
		 * weights.add(0.9837615f); weights.add(0.526475f);
		 * weights.add(0.9837615f); weights.add(0.526475f);
		 * weights.add(0.9837615f); weights.add(0.526475f);
		 * weights.add(0.9837615f); weights.add(0.526475f);
		 * weights.add(0.526475f); weights.add(0.526475f);
		 * weights.add(0.526475f); weights.add(0.526475f);
		 * weights.add(0.526475f); weights.add(0.526475f);
		 * weights.add(0.526475f); weights.add(0.526475f);
		 * weights.add(0.9837615f); weights.add(0.526475f);
		 * weights.add(0.9837615f); weights.add(0.526475f);
		 * weights.add(0.526475f); weights.add(0.9837615f);
		 * weights.add(0.9837615f); weights.add(0.526475f);
		 * weights.add(0.9837615f); weights.add(0.526475f);
		 * weights.add(0.9837615f);
		 */

		weights.add(0.6267342f);
		weights.add(0.8504509f);
		weights.add(0.6267342f);
		weights.add(0.8504509f);
		weights.add(0.8504509f);
		weights.add(0.6267342f);
		weights.add(0.8504509f);
		weights.add(0.6267342f);
		weights.add(0.8504509f);
		weights.add(0.6267342f);
		weights.add(0.8504509f);
		weights.add(0.6267342f);
		weights.add(0.6267342f);
		weights.add(0.6267342f);
		weights.add(0.6267342f);
		weights.add(0.6267342f);
		weights.add(0.6267342f);
		weights.add(0.6267342f);
		weights.add(0.6267342f);
		weights.add(0.6267342f);
		weights.add(0.8504509f);
		weights.add(0.6267342f);
		weights.add(0.8504509f);
		weights.add(0.6267342f);
		weights.add(0.6267342f);
		weights.add(0.8504509f);
		weights.add(0.8504509f);
		weights.add(0.6267342f);
		weights.add(0.8504509f);
		weights.add(0.6267342f);
		weights.add(0.8504509f);

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		roofDescriptor.setEdgeWeights(weights);
		mConfigs.put(-16, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();
		weights = new Vector<Float>();

		// -15
		vertices.add(new Vertex3d(550.0f, 450.0f, -100.0f));
		vertices.add(new Vertex3d(550.0f, 450.0f, -7.5f));
		vertices.add(new Vertex3d(519.6765f, 412.23846f, 30.0f));
		vertices.add(new Vertex3d(519.7f, 412.3f, -62.5f));
		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		mConfigs.put(-15, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();
		weights = new Vector<Float>();

		// -14
		// LOCH IN DER TEXTURIERUNG
		vertices.add(new Vertex3d(100.0f, 450.0f, -20.0f));
		vertices.add(new Vertex3d(200.0f, 450.0f, -20.0f));
		vertices.add(new Vertex3d(200.0f, 450.0f, -140.0f));
		vertices.add(new Vertex3d(300.0f, 450.0f, -140.0f));
		vertices.add(new Vertex3d(300.0f, 450.0f, -100.0f));
		vertices.add(new Vertex3d(550.0f, 450.0f, -100.0f));
		vertices.add(new Vertex3d(550.0f, 450.0f, -7.5f));
		vertices.add(new Vertex3d(640.0f, 450.0f, -7.5f));
		vertices.add(new Vertex3d(640.0f, 450.0f, 67.5f));
		vertices.add(new Vertex3d(550.0f, 450.0f, 67.5f));
		vertices.add(new Vertex3d(550.0f, 450.0f, 100.0f));
		vertices.add(new Vertex3d(500.0f, 450.0f, 100.0f));
		vertices.add(new Vertex3d(500.0f, 450.0f, 150.0f));
		vertices.add(new Vertex3d(400.0f, 450.0f, 150.0f));
		vertices.add(new Vertex3d(400.0f, 450.0f, 100.0f));
		vertices.add(new Vertex3d(300.0f, 450.0f, 100.0f));
		vertices.add(new Vertex3d(300.0f, 450.0f, 160.0f));
		vertices.add(new Vertex3d(200.0f, 450.0f, 160.0f));
		vertices.add(new Vertex3d(200.0f, 450.0f, 80.0f));
		vertices.add(new Vertex3d(100.0f, 450.0f, 80.0f));

		weights.add(0.77900183f);
		weights.add(0.7092913f);
		weights.add(0.77900183f);
		weights.add(0.7092913f);
		weights.add(0.7092913f);
		weights.add(0.77900183f);
		weights.add(0.7092913f);
		weights.add(0.77900183f);
		weights.add(0.7092913f);
		weights.add(0.77900183f);
		weights.add(0.7092913f);
		weights.add(0.77900183f);
		weights.add(0.7092913f);
		weights.add(0.77900183f);
		weights.add(0.7092913f);
		weights.add(0.77900183f);
		weights.add(0.7092913f);
		weights.add(0.7092913f);
		weights.add(0.77900183f);
		weights.add(0.7092913f);
		weights.add(0.77900183f);
		weights.add(0.7092913f);

		/*
		 * weights.add(0.47845185f); weights.add(0.9170791f);
		 * weights.add(0.47845185f); weights.add(0.9170791f);
		 * weights.add(0.9170791f); weights.add(0.47845185f);
		 * weights.add(0.9170791f); weights.add(0.47845185f);
		 * weights.add(0.9170791f); weights.add(0.47845185f);
		 * weights.add(0.9170791f); weights.add(0.47845185f);
		 * weights.add(0.9170791f); weights.add(0.47845185f);
		 * weights.add(0.9170791f); weights.add(0.47845185f);
		 * weights.add(0.9170791f); weights.add(0.9170791f);
		 * weights.add(0.47845185f); weights.add(0.9170791f);
		 * weights.add(0.47845185f); weights.add(0.9170791f);
		 */

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		roofDescriptor.setEdgeWeights(weights);
		mConfigs.put(-14, roofDescriptor);

		// -13
		vertices = new ArrayList<Vertex3d>();
		vertices.add(new Vertex3d(200.0f, 450.0f, -140.0f));
		vertices.add(new Vertex3d(300.0f, 450.0f, -140.0f));
		vertices.add(new Vertex3d(300.0f, 450.0f, -100.0f));
		vertices.add(new Vertex3d(550.0f, 450.0f, -100.0f));
		vertices.add(new Vertex3d(550.0f, 450.0f, 100.0f));
		vertices.add(new Vertex3d(300.0f, 450.0f, 100.0f));
		vertices.add(new Vertex3d(300.0f, 450.0f, 160.0f));
		vertices.add(new Vertex3d(200.0f, 450.0f, 160.0f));
		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		weights = new Vector<Float>();

		weights.add(1.0f);
		weights.add(0.75f);
		weights.add(1.0f);
		weights.add(0.75f);
		weights.add(1.0f);
		weights.add(0.75f);
		weights.add(1.0f);
		weights.add(0.75f);
		roofDescriptor.setEdgeWeights(weights);

		mConfigs.put(-13, roofDescriptor);

		// -12
		// Accuracy-Exception: FEHLER
		vertices = new ArrayList<Vertex3d>();
		vertices.add(new Vertex3d(100.0f, 450.0f, -20.0f));
		vertices.add(new Vertex3d(200.0f, 450.0f, -20.0f));
		vertices.add(new Vertex3d(200.0f, 450.0f, -140.0f));
		vertices.add(new Vertex3d(300.0f, 450.0f, -140.0f));
		vertices.add(new Vertex3d(300.0f, 450.0f, -100.0f));
		vertices.add(new Vertex3d(550.0f, 450.0f, -100.0f));
		vertices.add(new Vertex3d(550.0f, 450.0f, -7.5f));
		vertices.add(new Vertex3d(640.0f, 450.0f, -7.5f));
		vertices.add(new Vertex3d(640.0f, 450.0f, 67.5f));
		vertices.add(new Vertex3d(580.85486f, 450.0f, 67.5f));
		vertices.add(new Vertex3d(584.6716f, 450.0f, 73.21216f));
		vertices.add(new Vertex3d(590.0f, 450.0f, 100.0f));
		vertices.add(new Vertex3d(584.6716f, 450.0f, 126.78784f));
		vertices.add(new Vertex3d(569.4975f, 450.0f, 149.49747f));
		vertices.add(new Vertex3d(546.78784f, 450.0f, 164.67157f));
		vertices.add(new Vertex3d(520.0f, 450.0f, 170.0f));
		vertices.add(new Vertex3d(493.21216f, 450.0f, 164.67157f));
		vertices.add(new Vertex3d(471.25464f, 450.0f, 150.0f));
		vertices.add(new Vertex3d(400.0f, 450.0f, 150.0f));
		vertices.add(new Vertex3d(400.0f, 450.0f, 100.0f));
		vertices.add(new Vertex3d(300.0f, 450.0f, 100.0f));
		vertices.add(new Vertex3d(300.0f, 450.0f, 160.0f));
		vertices.add(new Vertex3d(200.0f, 450.0f, 160.0f));
		vertices.add(new Vertex3d(200.0f, 450.0f, 80.0f));
		vertices.add(new Vertex3d(100.0f, 450.0f, 80.0f));

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		mConfigs.put(-12, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();

		// -11
		vertices.add(new Vertex3d(250.0f, 450.0f, -100.0f));
		vertices.add(new Vertex3d(550.0f, 450.0f, -100.0f));
		vertices.add(new Vertex3d(550.0f, 450.0f, 100.0f));
		vertices.add(new Vertex3d(541.2132f, 450.0f, 121.2132f));
		vertices.add(new Vertex3d(520.0f, 450.0f, 130.0f));
		vertices.add(new Vertex3d(498.7868f, 450.0f, 121.2132f));
		vertices.add(new Vertex3d(490.0f, 450.0f, 100.0f));
		vertices.add(new Vertex3d(250.0f, 450.0f, 100.0f));

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		mConfigs.put(-11, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();

		// -10
		vertices.add(new Vertex3d(250.0f, 450.0f, -100.0f));
		vertices.add(new Vertex3d(550.0f, 450.0f, -100.0f));
		vertices.add(new Vertex3d(550.0f, 450.0f, 30.0f));
		vertices.add(new Vertex3d(650.0f, 450.0f, 30.0f));
		vertices.add(new Vertex3d(650.0f, 450.0f, 230.0f));
		vertices.add(new Vertex3d(450.0f, 450.0f, 230.0f));
		vertices.add(new Vertex3d(450.0f, 450.0f, 100.0f));
		vertices.add(new Vertex3d(250.0f, 450.0f, 100.0f));

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		mConfigs.put(-10, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();

		// -9
		// extreme Konfiguration, bei der ein Vertex 2 Split-Events im gleichen
		// Punkt ausloest => Square-Case-Exception: FEHLER
		vertices.add(new Vertex3d(250.0f, 450.0f, -100.0f));
		vertices.add(new Vertex3d(550.0f, 450.0f, -100.0f));
		vertices.add(new Vertex3d(550.0f, 450.0f, 80.0f));
		vertices.add(new Vertex3d(650.0f, 450.0f, 80.0f));
		vertices.add(new Vertex3d(650.0f, 450.0f, 180.0f));
		vertices.add(new Vertex3d(450.0f, 450.0f, 180.0f));
		vertices.add(new Vertex3d(450.0f, 450.0f, 100.0f));
		vertices.add(new Vertex3d(250.0f, 450.0f, 100.0f));

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		mConfigs.put(-9, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();

		// -8

		vertices.add(new Vertex3d(250.0f, 450.0f, -100.0f));
		vertices.add(new Vertex3d(550.0f, 450.0f, -100.0f));
		vertices.add(new Vertex3d(550.0f, 450.0f, 74.01924f));
		vertices.add(new Vertex3d(565.0f, 450.0f, 74.01924f));
		vertices.add(new Vertex3d(580.0f, 450.0f, 100.0f));
		vertices.add(new Vertex3d(565.0f, 450.0f, 125.98076f));
		vertices.add(new Vertex3d(535.0f, 450.0f, 125.98076f));
		vertices.add(new Vertex3d(520.0f, 450.0f, 100.0f));
		vertices.add(new Vertex3d(250.0f, 450.0f, 100.0f));

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		mConfigs.put(-8, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();

		// -7
		vertices.add(new Vertex3d(100.0f, 450.0f, -20.0f));
		vertices.add(new Vertex3d(200.0f, 450.0f, -20.0f));
		vertices.add(new Vertex3d(200.0f, 450.0f, -140.0f));
		vertices.add(new Vertex3d(300.0f, 450.0f, -140.0f));
		vertices.add(new Vertex3d(300.0f, 450.0f, -100.0f));
		vertices.add(new Vertex3d(550.0f, 450.0f, -100.0f));
		vertices.add(new Vertex3d(550.0f, 450.0f, -7.5f));
		vertices.add(new Vertex3d(640.0f, 450.0f, -7.5f));
		vertices.add(new Vertex3d(640.0f, 450.0f, 67.5f));
		vertices.add(new Vertex3d(550.0f, 450.0f, 67.5f));
		vertices.add(new Vertex3d(550.0f, 450.0f, 100.0f));
		vertices.add(new Vertex3d(500.0f, 450.0f, 100.0f));
		vertices.add(new Vertex3d(500.0f, 450.0f, 150.0f));
		vertices.add(new Vertex3d(400.0f, 450.0f, 150.0f));
		vertices.add(new Vertex3d(400.0f, 450.0f, 100.0f));
		vertices.add(new Vertex3d(300.0f, 450.0f, 100.0f));
		vertices.add(new Vertex3d(300.0f, 450.0f, 160.0f));
		vertices.add(new Vertex3d(200.0f, 450.0f, 160.0f));
		vertices.add(new Vertex3d(200.0f, 450.0f, 80.0f));
		vertices.add(new Vertex3d(100.0f, 450.0f, 80.0f));

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		mConfigs.put(-7, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();

		// -6

		vertices.add(new Vertex3d(100.0f, 450.0f, -20.0f));
		vertices.add(new Vertex3d(200.0f, 450.0f, -20.0f));
		vertices.add(new Vertex3d(200.0f, 450.0f, -140.0f));
		vertices.add(new Vertex3d(300.0f, 450.0f, -140.0f));
		vertices.add(new Vertex3d(300.0f, 450.0f, -100.0f));
		vertices.add(new Vertex3d(550.0f, 450.0f, -100.0f));
		vertices.add(new Vertex3d(550.0f, 450.0f, -7.5f));
		vertices.add(new Vertex3d(640.0f, 450.0f, -7.5f));
		vertices.add(new Vertex3d(640.0f, 450.0f, 67.5f));
		vertices.add(new Vertex3d(550.0f, 450.0f, 67.5f));
		vertices.add(new Vertex3d(550.0f, 450.0f, 100.0f));
		vertices.add(new Vertex3d(300.0f, 450.0f, 100.0f));
		vertices.add(new Vertex3d(300.0f, 450.0f, 160.0f));
		vertices.add(new Vertex3d(200.0f, 450.0f, 160.0f));
		vertices.add(new Vertex3d(200.0f, 450.0f, 80.0f));
		vertices.add(new Vertex3d(100.0f, 450.0f, 80.0f));

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		mConfigs.put(-6, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();

		// -5

		vertices.add(new Vertex3d(-100, 200, 0));
		vertices.add(new Vertex3d(-100, -100, 0));
		vertices.add(new Vertex3d(-50, -100, 0));
		vertices.add(new Vertex3d(-30, -70, 0));
		vertices.add(new Vertex3d(-10, -100, 0));
		vertices.add(new Vertex3d(200, -100, 0));
		vertices.add(new Vertex3d(200, -50, 0));
		vertices.add(new Vertex3d(160, -30, 0));
		vertices.add(new Vertex3d(200, -10, 0));

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		mConfigs.put(-5, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();

		// -4

		vertices.add(new Vertex3d(-100, 160, 0));
		vertices.add(new Vertex3d(-140, 30, 0));
		vertices.add(new Vertex3d(-10, 30, 0));
		vertices.add(new Vertex3d(-10, -50, 0));
		vertices.add(new Vertex3d(-40, -50, 0));
		vertices.add(new Vertex3d(-40, -150, 0));
		vertices.add(new Vertex3d(120, -130, 0));
		vertices.add(new Vertex3d(120, 0, 0));
		vertices.add(new Vertex3d(60, 0, 0));
		vertices.add(new Vertex3d(60, 70, 0));
		vertices.add(new Vertex3d(180, 70, 0));
		vertices.add(new Vertex3d(180, 90, 0));
		vertices.add(new Vertex3d(230, 90, 0));
		vertices.add(new Vertex3d(230, 140, 0));
		vertices.add(new Vertex3d(180, 140, 0));
		vertices.add(new Vertex3d(180, 160, 0));
		vertices.add(new Vertex3d(90, 160, 0));
		vertices.add(new Vertex3d(60, 220, 0));
		vertices.add(new Vertex3d(-10, 220, 0));
		vertices.add(new Vertex3d(-10, 160, 0));

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		mConfigs.put(-4, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();

		// -3

		vertices.add(new Vertex3d(-100, 160, 0));
		vertices.add(new Vertex3d(-100, 30, 0));
		vertices.add(new Vertex3d(-10, 30, 0));
		vertices.add(new Vertex3d(-10, -50, 0));
		vertices.add(new Vertex3d(-40, -50, 0));
		vertices.add(new Vertex3d(-40, -120, 0));
		vertices.add(new Vertex3d(120, -120, 0));
		vertices.add(new Vertex3d(120, 0, 0));
		vertices.add(new Vertex3d(60, 0, 0));
		vertices.add(new Vertex3d(60, 70, 0));
		vertices.add(new Vertex3d(180, 70, 0));
		vertices.add(new Vertex3d(180, 90, 0));
		vertices.add(new Vertex3d(230, 90, 0));
		vertices.add(new Vertex3d(230, 140, 0));
		vertices.add(new Vertex3d(180, 140, 0));
		vertices.add(new Vertex3d(180, 160, 0));
		vertices.add(new Vertex3d(60, 160, 0));
		vertices.add(new Vertex3d(60, 220, 0));
		vertices.add(new Vertex3d(-10, 220, 0));
		vertices.add(new Vertex3d(-10, 160, 0));

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		mConfigs.put(-3, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();

		// -2

		vertices.add(new Vertex3d(-100, 160, 0));
		vertices.add(new Vertex3d(-100, 30, 0));
		vertices.add(new Vertex3d(-10, 30, 0));
		vertices.add(new Vertex3d(-10, -50, 0));
		vertices.add(new Vertex3d(-40, -50, 0));
		vertices.add(new Vertex3d(-40, -120, 0));
		vertices.add(new Vertex3d(120, -120, 0));
		vertices.add(new Vertex3d(120, -20, 0));
		vertices.add(new Vertex3d(60, -20, 0));
		vertices.add(new Vertex3d(60, 70, 0));
		vertices.add(new Vertex3d(180, 70, 0));
		vertices.add(new Vertex3d(180, 160, 0));
		vertices.add(new Vertex3d(60, 160, 0));
		vertices.add(new Vertex3d(60, 220, 0));
		vertices.add(new Vertex3d(-10, 220, 0));
		vertices.add(new Vertex3d(-10, 160, 0));

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		mConfigs.put(-2, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();

		// -1 komplexes degeneriertes Polygon

		vertices.add(new Vertex3d(-100, 100, 0));
		vertices.add(new Vertex3d(-100, 30, 0));
		vertices.add(new Vertex3d(-10, 30, 0));
		vertices.add(new Vertex3d(-10, -50, 0));
		vertices.add(new Vertex3d(-40, -50, 0));
		vertices.add(new Vertex3d(-40, -120, 0));
		vertices.add(new Vertex3d(120, -120, 0));
		vertices.add(new Vertex3d(120, -20, 0));
		vertices.add(new Vertex3d(60, -20, 0));
		vertices.add(new Vertex3d(60, 70, 0));
		vertices.add(new Vertex3d(180, 70, 0));
		vertices.add(new Vertex3d(180, 160, 0));
		vertices.add(new Vertex3d(60, 160, 0));
		vertices.add(new Vertex3d(60, 220, 0));
		vertices.add(new Vertex3d(-10, 220, 0));
		vertices.add(new Vertex3d(-10, 100, 0));

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		mConfigs.put(-1, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();

		// 0 komplexes degeneriertes Polygon

		vertices.add(new Vertex3d(-100, 100, 0));
		vertices.add(new Vertex3d(-100, 30, 0));
		vertices.add(new Vertex3d(-10, 30, 0));
		vertices.add(new Vertex3d(-10, 0, 0));
		vertices.add(new Vertex3d(-40, 0, 0));
		vertices.add(new Vertex3d(-40, -70, 0));
		vertices.add(new Vertex3d(-10, -70, 0));
		vertices.add(new Vertex3d(-10, -100, 0));
		vertices.add(new Vertex3d(60, -100, 0));
		vertices.add(new Vertex3d(60, 30, 0));
		vertices.add(new Vertex3d(180, 30, 0));
		vertices.add(new Vertex3d(180, 100, 0));
		vertices.add(new Vertex3d(60, 100, 0));
		vertices.add(new Vertex3d(60, 220, 0));
		vertices.add(new Vertex3d(-10, 220, 0));
		vertices.add(new Vertex3d(-10, 100, 0));

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		mConfigs.put(0, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();

		// 1 komplexes, degeneriertes Polygon

		vertices.add(new Vertex3d(-100, 0, 0));
		vertices.add(new Vertex3d(-100, -70, 0));
		vertices.add(new Vertex3d(-30, -70, 0));
		vertices.add(new Vertex3d(-30, -130, 0));
		vertices.add(new Vertex3d(60, -130, 0));
		vertices.add(new Vertex3d(60, -50, 0));
		vertices.add(new Vertex3d(180, -50, 0));
		vertices.add(new Vertex3d(180, 30, 0));
		vertices.add(new Vertex3d(60, 30, 0));
		vertices.add(new Vertex3d(60, 120, 0));
		vertices.add(new Vertex3d(-70, 120, 0));
		vertices.add(new Vertex3d(-70, 0, 0));

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		mConfigs.put(1, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();

		// 2 Vertex-Event-Konfiguration

		vertices.add(new Vertex3d(-100, 80, 0));
		vertices.add(new Vertex3d(-100, -100, 0));
		vertices.add(new Vertex3d(100, -100, 0));
		vertices.add(new Vertex3d(100, 80, 0));
		vertices.add(new Vertex3d(60, 40, 0));
		vertices.add(new Vertex3d(85, 80, 0));
		vertices.add(new Vertex3d(-85, 80, 0));
		vertices.add(new Vertex3d(-60, 40, 0));

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		mConfigs.put(2, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();

		// 3 Konfiguration fuer komplexe Grundrisse

		vertices.add(new Vertex3d(-100, 100, 0));
		vertices.add(new Vertex3d(-100, -100, 0));
		vertices.add(new Vertex3d(300, -100, 0));
		vertices.add(new Vertex3d(300, -50, 0));
		vertices.add(new Vertex3d(200, -50, 0));
		vertices.add(new Vertex3d(200, 50, 0));
		vertices.add(new Vertex3d(300, 50, 0));
		vertices.add(new Vertex3d(300, 100, 0));

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		mConfigs.put(3, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();

		// 4 vereinfachte Konfiguration fuer degeneriertes Polygon

		vertices.add(new Vertex3d(-100, 200, 0));
		vertices.add(new Vertex3d(-100, -30, 0));
		vertices.add(new Vertex3d(300, -30, 0));
		vertices.add(new Vertex3d(300, 50, 0));
		vertices.add(new Vertex3d(350, 50, 0));
		vertices.add(new Vertex3d(350, 150, 0));
		vertices.add(new Vertex3d(300, 150, 0));
		vertices.add(new Vertex3d(300, 200, 0));

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		mConfigs.put(4, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();

		// 5 degeneriertes Polygon mit Ausstuelpung auf Grundkante
		vertices.add(new Vertex3d(-100, 100, 0));
		vertices.add(new Vertex3d(-100, -100, 0));
		vertices.add(new Vertex3d(300, -100, 0));
		vertices.add(new Vertex3d(300, 50, 0));
		vertices.add(new Vertex3d(350, 50, 0));
		vertices.add(new Vertex3d(350, 100, 0));

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		mConfigs.put(5, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();

		// 6 konkaves Polygon ohne Split-Events
		vertices.add(new Vertex3d(-100, 100, 0));
		vertices.add(new Vertex3d(0, -23, 0));
		vertices.add(new Vertex3d(-100, -100, 0));
		vertices.add(new Vertex3d(300, -100, 0));
		vertices.add(new Vertex3d(80, 75, 0));
		vertices.add(new Vertex3d(300, 100, 0));

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		mConfigs.put(6, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();

		// 7 Split-Event-Konfiguration
		vertices.add(new Vertex3d(-100, 100, 0));
		vertices.add(new Vertex3d(-100, -100, 0));
		vertices.add(new Vertex3d(300, -100, 0));
		vertices.add(new Vertex3d(80, 0, 0));
		vertices.add(new Vertex3d(300, 100, 0));

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		mConfigs.put(7, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();

		// 8 konvexes Polygon, geschlossener Lieninzug
		vertices.add(new Vertex3d(-100, 120, 0));
		vertices.add(new Vertex3d(-100, -120, 0));
		vertices.add(new Vertex3d(300, -100, 0));
		vertices.add(new Vertex3d(300, 100, 0));

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		mConfigs.put(8, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();
		vertices.add(new Vertex3d(-28.0f, 200.0f, -28.0f));
		vertices.add(new Vertex3d(28.0f, 200.0f, -28.0f));
		vertices.add(new Vertex3d(28.0f, 200.0f, 28.0f));
		vertices.add(new Vertex3d(-28.0f, 200.0f, 28.0f));

		// Collections.reverse(vertices);

		weights = new ArrayList<Float>();
		weights.add(1.1867789f);
		weights.add(0.8906255f);
		weights.add(1.1867789f);
		weights.add(0.8906255f);

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		roofDescriptor.setEdgeWeights(weights);
		mConfigs.put(9, roofDescriptor);

		vertices = new ArrayList<Vertex3d>();
		vertices.add(new Vertex3d(503.39612f, -81.0f, 59.09699f));
		vertices.add(new Vertex3d(422.53253f, -81.0f, 42.58618f));
		vertices.add(new Vertex3d(422.53253f, -81.0f, -42.58618f));
		vertices.add(new Vertex3d(500.0f, -81.0f, -59.768692f));
		vertices.add(new Vertex3d(605.3762f, -81.0f, -50.58596f));
		vertices.add(new Vertex3d(605.3762f, -81.0f, 10.661415f));
		vertices.add(new Vertex3d(577.46747f, -81.0f, 42.58618f));
		vertices.add(new Vertex3d(540.1793f, -81.0f, 59.09699f));

		weights = new ArrayList<Float>();
		weights.add(1.3f);
		weights.add(1.57f);
		weights.add(1.3f);
		weights.add(1.3f);
		weights.add(1.57f);
		weights.add(1.57f);
		weights.add(1.3f);
		weights.add(1.3f);

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		roofDescriptor.setEdgeWeights(weights);
		roofDescriptor.setMainWeight(1.3f);
		roofDescriptor.setSecondMainWeight(0.85f);
		roofDescriptor.setSideWeight(1.57f);
		roofDescriptor.setSecondSideWeight(1.57f);
		roofDescriptor.setSlopeChangeHeight(40);
		mConfigs.put(10, roofDescriptor);

		vertices.add(new Vertex3d(430.79285f, -116.165344f, -44.98513f));
		vertices.add(new Vertex3d(478.4613f, -116.165344f, -44.98513f));
		vertices.add(new Vertex3d(478.4613f, -116.165344f, -55.92962f));
		vertices.add(new Vertex3d(521.5387f, -116.165344f, -55.92962f));
		vertices.add(new Vertex3d(521.5387f, -116.165344f, -44.98513f));
		vertices.add(new Vertex3d(569.20715f, -116.165344f, -44.98513f));
		vertices.add(new Vertex3d(569.20715f, -116.165344f, 44.98513f));
		vertices.add(new Vertex3d(430.79285f, -116.165344f, 44.98513f));

		roofDescriptor = new SkeletonRoofDescriptor();
		roofDescriptor.setVertices(vertices);
		roofDescriptor.setEdgeWeights(weights);
		roofDescriptor.setMainWeight(1.3f);
		roofDescriptor.setSecondMainWeight(0.85f);
		roofDescriptor.setSideWeight(1.57f);
		roofDescriptor.setSecondSideWeight(1.57f);
		roofDescriptor.setSlopeChangeHeight(40);
		mConfigs.put(11, roofDescriptor);

	}
	// ------------------------------------------------------------------------------------------

}
