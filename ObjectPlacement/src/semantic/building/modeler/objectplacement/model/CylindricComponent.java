package semantic.building.modeler.objectplacement.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import semantic.building.modeler.math.MyPolygon;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.MyVectormath;
import semantic.building.modeler.math.Plane;
import semantic.building.modeler.math.Plane.CoordinatePlane;
import semantic.building.modeler.math.Vertex3d;

public class CylindricComponent extends AbstractComponent {

	/** Radius des Zylinders */
	private Float mRadius = null;

	/** Anzahl an Segmenten, aus denen der Zylinder gebildet wird */
	private Integer mNumberOfSegments = null;

	// ------------------------------------------------------------------------------------------

	public CylindricComponent(List<Vertex3d> corners) {
		super(corners);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Konstruktor mit Descriptor-Uebergabe
	 * 
	 * @param descriptor
	 *            Instanz der SubComponentDescriptor-Klasse, die fuer die
	 *            Speicherung von Parametern zur Erzeugung von Subkomponenten
	 *            verwendet wird. Methode erzeugt einen Zylinder in der
	 *            Grundebene, die mit dem Descriptor an den Konstruktor
	 *            uebergeben wird
	 */
	public CylindricComponent(ComponentDescriptor descriptor) {
		super(descriptor);

		Float height = descriptor.getHeight();
		Float width = descriptor.getWidth();

		// verwende den kleineren der beiden Werte als Radius des Zylinders
		if (width < height)
			mRadius = width;
		else
			mRadius = height;

		// halbiere den Radius, da bei rechteckigen Komponenten ebenfalls nur
		// die halbe berechnete Ausdehnung verwendet wird
		mRadius /= 2.0f;

		Random rand = new Random();

		// lies die Anzahl verwendeter Segmente aus der Descriptor-Instanz
		mNumberOfSegments = descriptor.getNumberOfSegments();

		Float sliceAngle = 360.0f / mNumberOfSegments;
		Float x = null, z = null;
		Vertex3d temp = null;

		List<Vertex3d> vertices = new ArrayList<Vertex3d>(mNumberOfSegments);

		// berechne den Kreis zunaechst in der xz-Ebene
		for (int i = 0; i < mNumberOfSegments; i++) {
			x = (float) (mRadius * Math.cos(Math.toRadians(i * sliceAngle)));
			z = (float) (mRadius * Math.sin(Math.toRadians(i * sliceAngle)));

			temp = new Vertex3d(x, 0.0f, z);
			vertices.add(temp);
		}

		// Erzeuge eine Ebenenrepraesentation fuer die Eingabeebene und fuer die
		// xz-Ebene
		Plane xzPlane = new Plane(CoordinatePlane.XZ);

		// hole die Grundebene
		Plane groundPlane = descriptor.getGroundPlane();

		MyVectormath mathHelper = MyVectormath.getInstance();
		Iterator<Vertex3d> vertIter = null;
		Vertex3d currentVert = null;

		// projiziere die Punkte aus der xz-Ebene in die Zielebene
		mathHelper.calculatePlaneToPlaneProjectionForPoints(xzPlane,
				groundPlane, vertices);

		// verschiebe die Punkte nun noch bzgl. ihres Mittelpunktes
		vertIter = vertices.iterator();
		MyVector3f center = descriptor.getCenter();
		while (vertIter.hasNext()) {
			currentVert = vertIter.next();
			currentVert.getPositionPtr().add(center);
		}

		mPolygon = new MyPolygon(vertices);

	}

	// ------------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "cylinder";
	}
	// ------------------------------------------------------------------------------------------

}
