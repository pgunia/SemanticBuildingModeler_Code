import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import processing.core.PApplet;
import processing.core.PConstants;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.Plane;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.tesselation.service.TesselationService;

/**
 * 
 * @author Patrick Gunia Klasse stellt das fuer die Tesselation benoetigte
 *         Applet zur Verfuegung
 * 
 */

public class TesselationApplet extends PApplet {

	/** Input-Vertex-List */
	private List<Vertex3d> mVertices = null;

	/** Result-Vertex-List */
	private List<Vertex3d> mResultVerts = null;

	/** Rotationswinkel fuer Kamerabewegung */
	private float dragRotateX, dragRotateY, dragDelta, dragLastPosX,
			dragLastPosY, dragTreshold;

	// ------------------------------------------------------------------------------------------

	@Override
	public void setup() {

		dragRotateX = 0.0f;
		dragRotateY = 0.0f;
		dragLastPosX = 0.0f;
		dragLastPosY = 0.0f;
		dragTreshold = 0.05f;
		dragDelta = 0.05f;

		// only works with OPENGL
		size(1000, 1000, OPENGL);
		// smooth();

		TesselationService tessController = TesselationService.getInstance();
		tessController.init(this);

		List<List<Vertex3d>> contours = new ArrayList<List<Vertex3d>>();
		mVertices = new ArrayList<Vertex3d>();

		mVertices.add(new Vertex3d(392.0f, 500.0f, 120.0f));
		mVertices.add(new Vertex3d(392.0f, 500.0f, -120.0f));
		mVertices.add(new Vertex3d(392.0f, 300.0f, -120.0f));
		mVertices.add(new Vertex3d(392.0f, 300.0f, 120.0f));

		contours.add(mVertices);

		mVertices = new ArrayList<Vertex3d>();

		mVertices.add(new Vertex3d(608.0f, 500.0f, -120.0f));
		mVertices.add(new Vertex3d(608.0f, 500.0f, 120.0f));
		mVertices.add(new Vertex3d(608.0f, 300.0f, -120.0f));
		mVertices.add(new Vertex3d(608.0f, 300.0f, 120.0f));
		contours.add(mVertices);

		/*
		 * mVertices.add(new Vertex3d(100.0f, 100.0f, 0.0f)); mVertices.add(new
		 * Vertex3d(300.0f, 100.0f, 0.0f)); mVertices.add(new Vertex3d(300.0f,
		 * 300.0f, 0.0f)); mVertices.add(new Vertex3d(100.0f, 300.0f, 0.0f));
		 * mVertices.add(new Vertex3d(100.0f, 100.0f, 0.0f));
		 * 
		 * mVertices.add(new Vertex3d(150.0f, 150.0f, 0.0f)); mVertices.add(new
		 * Vertex3d(150.0f, 250.0f, 0.0f)); mVertices.add(new Vertex3d(250.0f,
		 * 250.0f, 0.0f)); mVertices.add(new Vertex3d(250.0f, 150.0f, 0.0f));
		 * mVertices.add(new Vertex3d(150.0f, 150.0f, 0.0f));
		 */
		/*
		 * mVertices.add(new Vertex3d(580.4328f, 450.0f, 77.039f));
		 * mVertices.add(new Vertex3d(545.1087f, 397.9083f, 91.6708f));
		 * mVertices.add(new Vertex3d(545.1087f, 397.9082f, 91.6708f));
		 * mVertices.add(new Vertex3d(533.3339f, 380.5443f, 96.548f));
		 * mVertices.add(new Vertex3d(525.0006f, 368.255f, 100.0003f));
		 * mVertices.add(new Vertex3d(524.9998f, 368.2557f, 100.0008f));
		 * mVertices.add(new Vertex3d(512.7821f, 363.5731f, 87.7828f));
		 * mVertices.add(new Vertex3d(512.782f, 363.5731f, 87.7828f));
		 * mVertices.add(new Vertex3d(501.6016f, 355.2716f, 81.8067f));
		 * mVertices.add(new Vertex3d(468.1934f, 355.2716f, 31.8066f));
		 * mVertices.add(new Vertex3d(439.0896f, 333.6617f, 16.25f));
		 * mVertices.add(new Vertex3d(408.6886f, 311.0886f, 0.0f));
		 * mVertices.add(new Vertex3d(501.6015f, 355.2717f, 81.8068f));
		 * mVertices.add(new Vertex3d(479.8305f, 339.1065f, 70.1697f));
		 * mVertices.add(new Vertex3d(417.3745f, 333.6617f, -16.25f));
		 * mVertices.add(new Vertex3d(446.4783f, 355.2716f, -0.6934f));
		 * mVertices.add(new Vertex3d(457.6586f, 363.5731f, 5.2827f));
		 * mVertices.add(new Vertex3d(463.9651f, 368.2557f, 8.6536f));
		 * mVertices.add(new Vertex3d(480.5155f, 380.5443f, 17.5f));
		 * mVertices.add(new Vertex3d(480.5155f, 380.5444f, 17.5f));
		 * mVertices.add(new Vertex3d(503.9013f, 397.9082f, 30.0f));
		 * mVertices.add(new Vertex3d(574.059f, 450.0f, 67.5f));
		 */
		/*
		 * mVertices.clear(); mVertices.add(new Vertex3d(200f, 400f, 0f));
		 * mVertices.add(new Vertex3d(200f, 170f, 0f)); mVertices.add(new
		 * Vertex3d(600f, 170f, 0f)); mVertices.add(new Vertex3d(600f, 250f,
		 * 0f)); mVertices.add(new Vertex3d(650f, 250f, 0f)); mVertices.add(new
		 * Vertex3d(650f, 350f, 0f)); mVertices.add(new Vertex3d(600f, 350f,
		 * 0f)); mVertices.add(new Vertex3d(600f, 400f, 0f));
		 */
		/*
		 * mVertices.clear(); mVertices.add(new
		 * Vertex3d(250.0f,450.0f,-100.0f)); mVertices.add(new
		 * Vertex3d(550.0f,450.0f,-100.0f)); mVertices.add(new
		 * Vertex3d(550.0f,450.0f,30.0f)); mVertices.add(new
		 * Vertex3d(650.0f,450.0f,30.0f)); mVertices.add(new
		 * Vertex3d(650.0f,450.0f,230.0f)); mVertices.add(new
		 * Vertex3d(450.0f,450.0f,230.0f)); mVertices.add(new
		 * Vertex3d(450.0f,450.0f,100.0f)); mVertices.add(new
		 * Vertex3d(250.0f,450.0f,100.0f));
		 */
		// Zylinder
		/*
		 * mVertices.clear(); mVertices.add(new Vertex3d(469.56723f, 450.0f,
		 * 77.038994f)); mVertices.add(new Vertex3d(482.5736f, 450.0f,
		 * 57.573593f)); mVertices.add(new Vertex3d(502.039f, 450.0f,
		 * 44.567226f)); mVertices.add(new Vertex3d(525.0f, 450.0f, 40.0f));
		 * mVertices.add(new Vertex3d(547.961f, 450.0f, 44.567226f));
		 * mVertices.add(new Vertex3d(567.4264f, 450.0f, 57.573593f));
		 * mVertices.add(new Vertex3d(580.4328f, 450.0f, 77.038994f));
		 * mVertices.add(new Vertex3d(585.0f, 450.0f, 100.0f));
		 * mVertices.add(new Vertex3d(580.4328f, 450.0f, 122.961006f));
		 * mVertices.add(new Vertex3d(567.4264f, 450.0f, 142.4264f));
		 * mVertices.add(new Vertex3d(547.961f, 450.0f, 155.43277f));
		 * mVertices.add(new Vertex3d(525.0f, 450.0f, 160.0f));
		 * mVertices.add(new Vertex3d(502.039f, 450.0f, 155.43277f));
		 * mVertices.add(new Vertex3d(482.5736f, 450.0f, 142.4264f));
		 * mVertices.add(new Vertex3d(469.56723f, 450.0f, 122.961006f));
		 * mVertices.add(new Vertex3d(465.0f, 450.0f, 100.0f));
		 */
		/*
		 * mVertices.clear(); mVertices.add(new Vertex3d(-100, 100, 0));
		 * mVertices.add(new Vertex3d(-100, 30, 0)); mVertices.add(new
		 * Vertex3d(-10, 30, 0)); mVertices.add(new Vertex3d(-10, 0, 0));
		 * mVertices.add(new Vertex3d(-40, 0, 0)); mVertices.add(new
		 * Vertex3d(-40, -70, 0)); mVertices.add(new Vertex3d(-10, -70, 0));
		 * mVertices.add(new Vertex3d(-10, -100, 0)); mVertices.add(new
		 * Vertex3d(60, -100, 0)); mVertices.add(new Vertex3d(60, 30, 0));
		 * mVertices.add(new Vertex3d(180, 30, 0)); mVertices.add(new
		 * Vertex3d(180, 100, 0)); mVertices.add(new Vertex3d(60, 100, 0));
		 * mVertices.add(new Vertex3d(60, 220, 0)); mVertices.add(new
		 * Vertex3d(-10, 220, 0)); mVertices.add(new Vertex3d(-10, 100, 0));
		 * MyVector3f translation = new MyVector3f(200, 200,0); for(int i = 0; i
		 * < mVertices.size(); i++)
		 * mVertices.get(i).getPositionPtr().add(translation);
		 */
		mResultVerts = tessController.tesselate(contours);

		// validateResult();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode erzeugt testweise Ebenen fuer die berechneten Dreiecke, um bsw.
	 * zu testen, ob Dreiecke erzeugt wurden, deren Flaecheninhalt = 0 ist
	 */
	private void validateResult() {

		Iterator<Vertex3d> vertIter = mResultVerts.iterator();
		Vertex3d currentVertex = null;

		List<Vertex3d> vertList = new ArrayList<Vertex3d>(3);

		while (vertIter.hasNext()) {

			currentVertex = vertIter.next();
			vertList.add(currentVertex);
			if (vertList.size() == 3) {
				computePlaneForTriangle(vertList);
				vertList = new ArrayList<Vertex3d>(3);
			}

		}

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Berechnet eine Ebene fuer die uebergebenen Punkte
	 */
	private void computePlaneForTriangle(List<Vertex3d> vertices) {

		Vertex3d vert0 = vertices.get(0);
		Vertex3d vert1 = vertices.get(1);
		Vertex3d vert2 = vertices.get(2);

		System.out
				.println("------------------------------------------------------------------------");
		System.out.println(vert0);
		System.out.println(vert1);
		System.out.println(vert2);
		System.out
				.println("------------------------------------------------------------------------");

		MyVector3f vert1To0 = new MyVector3f();
		vert1To0.sub(vert0.getPosition(), vert1.getPosition());

		MyVector3f vert1To2 = new MyVector3f();
		vert1To2.sub(vert2.getPosition(), vert0.getPosition());

		// normale
		MyVector3f normal = new MyVector3f();
		normal.cross(vert1To0, vert1To2);

		Plane test = new Plane(normal, vert1.getPosition());

	}

	// ------------------------------------------------------------------------------------------

	@Override
	public void draw() {

		rotateX(-dragRotateY);
		rotateY(dragRotateX);

		background(0);
		Iterator<Vertex3d> vertIter = mResultVerts.iterator();
		Vertex3d currentVertex = null;
		stroke(255);
		strokeWeight(1);
		noFill();

		beginShape(PConstants.TRIANGLES);
		while (vertIter.hasNext()) {
			currentVertex = vertIter.next();
			vertex(currentVertex.getX(), currentVertex.getY(),
					currentVertex.getZ());
		}
		endShape();
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

}
