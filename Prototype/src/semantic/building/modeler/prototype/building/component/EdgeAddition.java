package semantic.building.modeler.prototype.building.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import processing.core.PApplet;
import semantic.building.modeler.configurationservice.model.component.EdgeAdditionComponentConfiguration;
import semantic.building.modeler.configurationservice.model.enums.Side;
import semantic.building.modeler.math.MyPolygon;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.MyVectormath;
import semantic.building.modeler.math.MyVectormath.Drehsinn;
import semantic.building.modeler.math.Ray;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.prototype.graphics.complex.AbstractComplex;
import semantic.building.modeler.prototype.graphics.complex.FreeComplex;
import semantic.building.modeler.prototype.graphics.primitives.AbstractQuad;

/**
 * Klasse modelliert profilbasierte Komponenten, die auf allen Kanten des
 * Uebergabequads durch Profilextrusion appliziert werden
 * 
 * @author Patrick Gunia
 * 
 */

public class EdgeAddition extends AbstractBuildingComponent {

	/** Quad, an dessen Kanten die Additions appliziert werden */
	private transient AbstractQuad mTargetQuad = null;

	/** Profile, das als Basis fuer die Additions fungiert */
	private transient AbstractComplex mProfile = null;

	// ------------------------------------------------------------------------------------------
	/**
	 * Konstruktor mit Uebergabe saemtlicher fuer die Konstruktion
	 * erforderlicher Parameter
	 * 
	 * @param parent
	 *            Drawing-Context
	 * @param conf
	 *            Konfigurationsobjekt
	 * @param targetQuad
	 *            Quad, auf dem die Applikationen vorgenommen werden
	 * @param profile
	 *            Profil, das als Basis fuer die Extrusion verwendet wird
	 */
	public EdgeAddition(final PApplet parent,
			final EdgeAdditionComponentConfiguration conf,
			final AbstractQuad targetQuad, final AbstractComplex profile) {
		super(parent, conf);
		mProfile = profile;
		mTargetQuad = targetQuad;
	}

	// ------------------------------------------------------------------------------------------

	public EdgeAddition(PApplet applet) {
		super(applet);
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public void createComponent() {

		EdgeAdditionComponentConfiguration conf = (EdgeAdditionComponentConfiguration) mConf;
		assert mProfile != null && mTargetQuad != null : "FEHLER: Profil oder Zielquad sind nicht gesetzt!";

		// erzeuge aus den geladenen Vertices des Profils ein Footprint-Polygon
		List<Vertex3d> profVerts = null;

		// erzeuge Kopien der Vertices des geladenen Profils, auf diesen wird
		// spaeter gearbeitet
		final List<Vertex3d> srcVerts = mProfile.getVertices();
		profVerts = new ArrayList<Vertex3d>(srcVerts.size());

		for (Vertex3d curVert : srcVerts) {
			profVerts.add(curVert.clone());
		}

		final MyVectormath mathHelper = MyVectormath.getInstance();
		final MyPolygon profilePoly = new MyPolygon(profVerts);
		profVerts = null;

		// erzeuge ein skaliertes Profil
		scaleProfile(conf.getEdgeAdditionHeightRatio(), profilePoly, mProfile
				.getBB().getHeight(), mTargetQuad.getPolygon().getHeight());

		// Versuch ueber die Schnittgerade der beiden Ebenen => berechne die
		// Schnittgerade und verwende diese als Rotationsachse
		final Ray intersectionRay = mathHelper
				.calculatePlane2PlaneIntersection(profilePoly.getPlane(),
						mTargetQuad.getPlane());
		if (intersectionRay == null) {
			LOGGER.error("FEHLER: Ebenen sind parallel, Ausrichtungsberechnung abgebrochen!");
			return;
		}

		// Winkel berechnen
		double angleProfilePlane2QuadPlane = mathHelper.calculateAngleRadians(
				profilePoly.getNormalPtr(), mTargetQuad.getNormalPtr());

		LOGGER.info("Winkel zwischen Profil und Wandpolygon: "
				+ angleProfilePlane2QuadPlane);

		final double targetAngleRad = Math.PI / 2.0d;
		double rotationAngleRad = targetAngleRad - angleProfilePlane2QuadPlane;

		// Polygonprofil in die Zielausrichtung rotieren
		if (!mathHelper.isWithinTolerance(rotationAngleRad, 0.0d, 0.05d)) {
			mathHelper.rotatePolygonAroundArbitraryAxis(intersectionRay,
					profilePoly, -rotationAngleRad);
			angleProfilePlane2QuadPlane = mathHelper.calculateAngleRadians(
					profilePoly.getNormalPtr(), mTargetQuad.getNormalPtr());
			LOGGER.trace("Winkel zwischen Profil und Wandpolygon nach Rotation: "
					+ angleProfilePlane2QuadPlane);
		}

		// HIER MUESSTE DAS PROFIL JETZT KORREKT AUSGERICHTET SEIN
		// nun muessen die Ausrichtungen fuer die einzelnen Kanten des Quads
		// durchgefuehrt werden
		final List<Ray> quadRays = mTargetQuad.getPolygon().getRays();

		// Rotationsachse laeuft immer durch den Mittelpunkt des Profils in
		// Richtung der Wandnormalen
		final Ray targeRayRotationAxis = new Ray(profilePoly.getCenter(),
				mTargetQuad.getNormalPtr());
		final MyVector3f alignedProfileCenter = profilePoly.getCenter();

		MyPolygon clonedProfile = null;
		double rotationAngleTargetRay;

		LOGGER.trace("Quad-Ausrichtung: " + mTargetQuad.getNormalPtr()
				+ " Rotationsachse: " + targeRayRotationAxis);
		LOGGER.trace("Drehsinn: "
				+ mathHelper.computeDrehsinnForPolygon(mTargetQuad
						.getQuadVertices()));

		Drehsinn drehsinnTargetPoly = mathHelper
				.computeDrehsinnForPolygon(mTargetQuad.getQuadVertices());
		for (Ray curRay : quadRays) {

			clonedProfile = profilePoly.clone();

			// 1. Variante: Profil und Kante sind parallel
			if (mathHelper.isParallel(clonedProfile.getNormalPtr(),
					curRay.getDirectionPtr())) {

				// wenn die beiden Richtungsvektoren antiparallel sind, rotiere
				// das Profil um 180°
				if (mathHelper.isAntiparallel(clonedProfile.getNormalPtr(),
						curRay.getDirectionPtr())) {
					mathHelper.rotatePolygonAroundArbitraryAxis(
							targeRayRotationAxis, clonedProfile, Math.PI);
					LOGGER.trace("Profil und Kante sind antiparallel");
				} else {
					LOGGER.trace("Profil und Kante sind parallel");
				}
			} else {
				rotationAngleTargetRay = mathHelper.getFullAngleRad(
						clonedProfile.getNormalPtr(), curRay.getDirectionPtr());

				// wenn der Drehsinn des Zielpolygons POSITIV ist, muss die
				// Rotation in die andere Richtung durchgefuehrt werden
				if (Drehsinn.POSITIV.equals(drehsinnTargetPoly)) {
					rotationAngleTargetRay *= -1.0f;
				}
				LOGGER.trace("Winkel vor Rotation: " + rotationAngleTargetRay);
				mathHelper.rotatePolygonAroundArbitraryAxis(
						targeRayRotationAxis, clonedProfile,
						-rotationAngleTargetRay);
			}

			rotationAngleTargetRay = mathHelper.getFullAngleRad(
					clonedProfile.getNormalPtr(), curRay.getDirectionPtr());
			LOGGER.trace("Kante: " + curRay.getDirectionPtr() + " Profil: "
					+ clonedProfile.getNormalPtr() + " Winkel nach Rotation: "
					+ rotationAngleTargetRay);

			// verschiebe das Profil jetzt an den Startpunkt des Strahls
			final MyVector3f translation = new MyVector3f();
			translation.sub(curRay.getStartPtr(), alignedProfileCenter);
			clonedProfile.translate(translation);

			// erzeuge die Addition durch einfache Extrusion
			final FreeComplex edgeAddition = new FreeComplex(
					mComponent.getParent(), clonedProfile, curRay.getLength(),
					new HashMap<MyVector3f, Side>(0), false);
			edgeAddition.create();
			edgeAddition.unregister();

			// extrahiere die Geometrie und fuege sie zur Komponente hinzu
			extractDataFromComponent(edgeAddition);

		}

		mComponent.finalizeCreation();
	}

	// ------------------------------------------------------------------------------------------

	@Override
	protected String getType() {
		return "EdgeApplication";
	}

	// ------------------------------------------------------------------------------------------

}
