package semantic.building.modeler.prototype.controller;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import processing.core.PApplet;
import processing.core.PConstants;
import semantic.building.modeler.math.MyPolygon;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.prototype.algorithm.FootprintCreator;
import semantic.building.modeler.prototype.service.ObjectManagementService;
import semantic.building.modeler.prototype.service.TextureManagement;

/**
 * 
 * @author Patrick Gunia Viewklasse, erzeugt den Renderkontext, fuehrt
 *         Messageverarbeitung und -weiterleitung durch. Alle weiteren Aufgaben
 *         werden an den Controller delegiert.
 * 
 */
public class PrototypeApplet extends PApplet {

	/** Logging-Instanz */
	protected static Logger LOGGER = Logger.getLogger(PrototypeApplet.class);

	/** Controller-Instanz fuer die Erzeugung der Gebaeudegeometrien */
	private PrototypeController mController = null;

	private float dragRotateX, dragRotateY, dragDelta, dragLastPosX,
			dragLastPosY, dragTreshold;

	/**
	 * DEBUG: Liste mit einer Menge von Grundrissen, die fuer das Debugging des
	 * Footprintalgorithmus eingesetzt werden koennen
	 */
	private List<MyPolygon> footprints = null;

	// ------------------------------------------------------------------------------------------

	public PrototypeApplet() {

		dragRotateX = 0.0f;
		dragRotateY = 0.0f;
		dragLastPosX = 0.0f;
		dragLastPosY = 0.0f;
		dragTreshold = 0.05f;
		dragDelta = 0.05f;
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public void setup() {

		mController = new PrototypeController(this);
		mController.createGeometry();

		// footprints = debugFootprintCreator();

		MyVector3f blickrichtung = new MyVector3f(0.0f, -1.0f, 0.0f);
		MyVector3f cameraPosition = new MyVector3f(200.0f, 300.0f, 0.0f);
		MyVector3f cameraUp = new MyVector3f(1.0f, 0.0f, 0.0f);
		// camera(blickrichtung.x, blickrichtung.y, blickrichtung.z,
		// cameraPosition.x, cameraPosition.y, cameraPosition.z, cameraUp.x,
		// cameraUp.y, cameraUp.z);
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public void draw() {
		// background(76,76,253);
		background(155, 155, 155);
		mController.draw(dragRotateX, dragRotateY);

		if (footprints != null) {

			List<Vertex3d> footprintVerts = null;
			Vertex3d currentVertex = null;
			for (int i = 0; i < footprints.size(); i++) {
				footprintVerts = footprints.get(i).getVertices();

				beginShape();
				for (int j = 0; j < footprintVerts.size(); j++) {
					currentVertex = footprintVerts.get(j);

					vertex(currentVertex.getX(), currentVertex.getY(),
							currentVertex.getZ());
				}
				endShape(PConstants.CLOSE);
			}
			noFill();
		}
	}

	// ------------------------------------------------------------------------------------------
	@Override
	public void keyPressed() {

		switch (key) {
		case 'o':
			ObjectManagementService.getInstance().printCurrentState();
			break;
		case 'r':
			mController.computeRoof();
			break;
		case 't':
			mController.computeWallTexturing();
			break;
		case 'x':
			TextureManagement.getInstance().printReferenceStatus();
			break;
		case 'n':
			mController.createGeometry();
			break;
		case 'e':
			mController.exportModel();
			break;
		case 'p':
			mController.printSceneTree();
			break;
		case 's':
			if (mController.getDrawTextures())
				mController.setDrawTextures(false);
			else
				mController.setDrawTextures(true);
			break;
		case 'f':
			if (mController.getDrawFramerate())
				mController.setDrawFramerate(false);
			else
				mController.setDrawFramerate(true);
			break;
		}

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * benutze das Mouse-Dragging fuer die Rotation der Geometrie
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
	 * Debugging-Methode fuer den Footprint-Creator. Erzeugt eine Instanz des
	 * Footprint-Creators und laesst ihn den Grundriss berechnen.
	 */
	private List<MyPolygon> debugFootprintCreator() {
		List<Vertex3d> vertices = new ArrayList<Vertex3d>();
		List<MyPolygon> polygons = new ArrayList<MyPolygon>();

		// 0
		vertices = new ArrayList<Vertex3d>();
		vertices.add(new Vertex3d(425.0f, 500.0f, 0.0f));
		vertices.add(new Vertex3d(425.0f, 500.0f, -125.0f));
		vertices.add(new Vertex3d(575.0f, 500.0f, -125.0f));
		vertices.add(new Vertex3d(575.0f, 500.0f, 0.0f));

		// 1
		polygons.add(new MyPolygon(vertices));
		vertices = new ArrayList<Vertex3d>();
		vertices.add(new Vertex3d(425.0f, 500.0f, 0.0f));
		vertices.add(new Vertex3d(425.0f, 500.0f, 75.0f));
		vertices.add(new Vertex3d(575.0f, 500.0f, 75.0f));
		vertices.add(new Vertex3d(575.0f, 500.0f, 0.0f));

		// 2
		polygons.add(new MyPolygon(vertices));
		vertices = new ArrayList<Vertex3d>();
		vertices.add(new Vertex3d(575.0f, 500.0f, 0.0f));
		vertices.add(new Vertex3d(575.0f, 500.0f, 75.0f));
		vertices.add(new Vertex3d(700.0f, 500.0f, 75.0f));
		vertices.add(new Vertex3d(700.0f, 500.0f, 0.0f));

		// 3
		polygons.add(new MyPolygon(vertices));
		vertices = new ArrayList<Vertex3d>();
		vertices.add(new Vertex3d(575.0f, 500.0f, 0.0f));
		vertices.add(new Vertex3d(575.0f, 500.0f, -125.0f));
		vertices.add(new Vertex3d(700.0f, 500.0f, -125.0f));
		vertices.add(new Vertex3d(700.0f, 500.0f, 0.0f));

		// 4
		polygons.add(new MyPolygon(vertices));
		vertices = new ArrayList<Vertex3d>();
		vertices.add(new Vertex3d(700.0f, 500.0f, 0.0f));
		vertices.add(new Vertex3d(700.0f, 500.0f, 75.0f));
		vertices.add(new Vertex3d(825.0f, 500.0f, 75.0f));
		vertices.add(new Vertex3d(825.0f, 500.0f, 0.0f));

		// 5
		polygons.add(new MyPolygon(vertices));
		vertices = new ArrayList<Vertex3d>();
		vertices.add(new Vertex3d(700.0f, 500.0f, 0.0f));
		vertices.add(new Vertex3d(700.0f, 500.0f, -125.0f));
		vertices.add(new Vertex3d(825.0f, 500.0f, -125.0f));
		vertices.add(new Vertex3d(825.0f, 500.0f, 0.0f));

		// 6
		polygons.add(new MyPolygon(vertices));
		vertices = new ArrayList<Vertex3d>();
		vertices.add(new Vertex3d(425.0f, 500.0f, 125.0f));
		vertices.add(new Vertex3d(425.0f, 500.0f, 75.0f));
		vertices.add(new Vertex3d(575.0f, 500.0f, 75.0f));
		vertices.add(new Vertex3d(575.0f, 500.0f, 125.0f));

		// 7
		polygons.add(new MyPolygon(vertices));
		vertices = new ArrayList<Vertex3d>();
		vertices.add(new Vertex3d(425.0f, 500.0f, 125.0f));
		vertices.add(new Vertex3d(425.0f, 500.0f, 200.0f));
		vertices.add(new Vertex3d(575.0f, 500.0f, 200.0f));
		vertices.add(new Vertex3d(575.0f, 500.0f, 125.0f));

		// 8
		polygons.add(new MyPolygon(vertices));
		vertices = new ArrayList<Vertex3d>();
		vertices.add(new Vertex3d(575.0f, 500.0f, 125.0f));
		vertices.add(new Vertex3d(575.0f, 500.0f, 75.0f));
		vertices.add(new Vertex3d(700.0f, 500.0f, 75.0f));
		vertices.add(new Vertex3d(700.0f, 500.0f, 125.0f));

		// 9
		polygons.add(new MyPolygon(vertices));
		vertices = new ArrayList<Vertex3d>();
		vertices.add(new Vertex3d(575.0f, 500.0f, 125.0f));
		vertices.add(new Vertex3d(575.0f, 500.0f, 200.0f));
		vertices.add(new Vertex3d(700.0f, 500.0f, 200.0f));
		vertices.add(new Vertex3d(700.0f, 500.0f, 125.0f));

		// 10
		polygons.add(new MyPolygon(vertices));
		vertices = new ArrayList<Vertex3d>();
		vertices.add(new Vertex3d(700.0f, 500.0f, 200.0f));
		vertices.add(new Vertex3d(575.0f, 500.0f, 200.0f));
		vertices.add(new Vertex3d(575.0f, 500.0f, 325.0f));
		vertices.add(new Vertex3d(700.0f, 500.0f, 325.0f));

		// 11
		polygons.add(new MyPolygon(vertices));
		vertices = new ArrayList<Vertex3d>();
		vertices.add(new Vertex3d(575.0f, 500.0f, 200.0f));
		vertices.add(new Vertex3d(425.0f, 500.0f, 200.0f));
		vertices.add(new Vertex3d(425.0f, 500.0f, 325.0f));
		vertices.add(new Vertex3d(575.0f, 500.0f, 325.0f));

		polygons.add(new MyPolygon(vertices));

		FootprintCreator creator = new FootprintCreator();
		LOGGER.info("Adde " + polygons.size() + " Polygone zum Creator!");

		for (int i = 0; i < polygons.size(); i++)
			creator.addComponent(polygons.get(i));
		List<List<Vertex3d>> footprints = creator.process(false);
		LOGGER.info("#Vertexlisten: " + footprints.size());

		List<MyPolygon> footprintPolys = new ArrayList<MyPolygon>(
				footprints.size());
		for (int i = 0; i < footprints.size(); i++) {
			List<Vertex3d> curList = footprints.get(i);
			MyPolygon poly = new MyPolygon(curList);
			footprintPolys.add(poly);
		}

		LOGGER.info("Insgesamt wurden " + footprintPolys.size()
				+ " Grundrisse berechnet.");

		return footprintPolys;
	}

}
