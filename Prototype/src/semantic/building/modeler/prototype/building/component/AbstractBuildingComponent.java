package semantic.building.modeler.prototype.building.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import processing.core.PApplet;
import semantic.building.modeler.configurationservice.model.AbstractConfigurationObject;
import semantic.building.modeler.math.MyPolygon;
import semantic.building.modeler.math.Ray;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.prototype.graphics.complex.AbstractComplex;
import semantic.building.modeler.prototype.graphics.complex.ImportedComplex;
import semantic.building.modeler.prototype.graphics.primitives.AbstractQuad;
import semantic.building.modeler.prototype.service.PrototypeHelper;

/**
 * Klasse ist abstrakte Basisklasse fuer alle Arten von Komponenten, die zu
 * einem Gebaeude hinzugefuegt werden koennen, unabhaengig von der Art ihrer
 * Erzeugung
 * 
 * @author Patrick Gunia
 * 
 */
public abstract class AbstractBuildingComponent {

	/** Logger */
	protected static Logger LOGGER = Logger
			.getLogger(AbstractBuildingComponent.class);

	/** Komponente, die durch Berechnung oder Laden erstellt wurde */
	protected ImportedComplex mComponent = null;

	/** Konfigurationsdatei fuer Komponentenerzeugung */
	protected AbstractConfigurationObject mConf = null;

	/**
	 * Methode zur Erzeugung der Komponenten, muss von Subklassen ueberschrieben
	 * werden
	 */
	public abstract void createComponent();

	/** Typbezeichnung der Komponente */
	protected abstract String getType();

	// ------------------------------------------------------------------------------------------
	/**
	 * Konstruktor mit Uebergabe des Drawing-Context
	 * 
	 * @param applet
	 *            Drawing-Context, wird der erzeugten Grafikkomponente
	 *            uebergeben, damit sie sich auf diese Struktur zeichnen kann
	 */
	public AbstractBuildingComponent(PApplet applet) {
		mComponent = new ImportedComplex(applet);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Konstruktor mit Uebergabe eines Konfigurationsobjekts
	 * 
	 * @param applet
	 *            Drawing-Context
	 * @param conf
	 *            Konfigurationsobjekt
	 */
	public AbstractBuildingComponent(final PApplet applet,
			final AbstractConfigurationObject conf) {
		mComponent = new ImportedComplex(applet);
		mConf = conf;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mComponent
	 */
	public ImportedComplex getComponent() {
		return mComponent;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mComponent
	 *            the mComponent to set
	 */
	public void setComponent(ImportedComplex mComponent) {
		this.mComponent = mComponent;
	}

	// ------------------------------------------------------------------------------------------
	public AbstractConfigurationObject getConf() {
		return mConf;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode extrahiert Vertices und Quads aus der uebergebenen Box und fuegt
	 * sie zum Vertex- und Quadbuffer der hier berechneten Komponente hinzu.
	 * Dabei werden doppelte Vertices vermieden.
	 * 
	 * @param stairBox
	 *            Kubusfoermiges Objekt, aus dem die Vertices und Faces geladen
	 *            werden
	 */
	protected void extractDataFromComponent(final AbstractComplex component) {

		final List<Vertex3d> vertices = component.getVertices();
		final List<AbstractQuad> quads = component.getOutdoorQuads();

		final List<Vertex3d> componentVerts = mComponent.getVertices();
		final List<AbstractQuad> componentQuads = mComponent.getOutdoorQuads();

		LOGGER.debug("#Verts vor Extraction: " + componentVerts.size());
		LOGGER.debug("#Quads Komponente: " + quads.size());

		for (Vertex3d currentVert : vertices) {
			if (componentVerts.contains(currentVert)) {
				continue;
			} else {
				componentVerts.add(currentVert);
			}
		}

		// Quad-Indices updaten
		Integer[] indices = null;
		int index;

		Vertex3d currentVert = null;
		for (AbstractQuad currentQuad : quads) {
			indices = currentQuad.getIndices();
			for (int k = 0; k < indices.length; k++) {
				currentVert = vertices.get(indices[k]);
				index = componentVerts.indexOf(currentVert);
				assert index != -1 : "FEHLER: Vertex " + currentVert
						+ " befindet sich nicht im Vertexbuffer!";
				indices[k] = index;
			}
			currentQuad.setIndices(indices);

			// Parent umlenken, das urspruengliche Objekt wird nach dem Transfer
			// der Daten nicht mehr verwendet
			currentQuad.setComplexParent(mComponent);
		}
		componentQuads.addAll(quads);
		LOGGER.debug("#Verts nach Extraction: " + componentVerts.size());

		// Eingabeobjekt bei Objektverwaltung abmelden, kann geloescht werden
		component.unregister();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode ermittelt die beiden Vertices innerhalb des Polygons mit der
	 * groessten x-Komponente. Diese beiden Vertices definieren die Kante, mit
	 * der das Gesimse an der Wand positioniert wird. Diese Berechnung setzt
	 * voraus, dass das Profil nach den oben definierten Vrogaben erstellt wurde
	 * 
	 * @param poly
	 *            Polygon, dass das Profil des Gesimses definiert
	 * @return Strahl, der die gesuchte Kante beschreibt und der immer von oben
	 *         nach unten verlaeuft (in Richtung der positiven y-Achse)
	 */
	protected Ray detectFittingEdge(final MyPolygon poly) {

		final List<Vertex3d> profilePoints = new ArrayList<Vertex3d>(
				poly.getVertices());
		PrototypeHelper.getInstance().sortVerticesByXCoordinate(profilePoints);

		// die letzten beiden Vertices sind diejenigen mit der groessten
		// x-Koordinate
		final Vertex3d first = profilePoints.get(profilePoints.size() - 2);
		final Vertex3d second = profilePoints.get(profilePoints.size() - 1);

		assert first.getX() == second.getX() : "FEHLER: Die Vertices bilden keine gemeinsame Anschlusskante! Vert1: "
				+ first.getX() + " Vert2: " + second.getX() + "!";

		// Strahl soll immer von oben nach unten zeigen (also in Richtung der
		// positiven y-Achse)
		if (first.getY() < second.getY()) {
			return new Ray(first, second);
		} else {
			return new Ray(second, first);
		}
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode skaliert das uebergebene Profil derart, dass es die korrekten
	 * Ausdehnungen in Bezug auf das uebergebene Quad besitzt
	 * 
	 * @param heightRatio
	 *            Hoehe des zu skalierenden Profils als Anteil an der
	 *            Gesamthoehe
	 * @param profile
	 *            Profil, das skaliert wird
	 * @param profileHeight
	 *            = Hoehe des Profils (y-Achse)
	 * @param targetPolygonHeight
	 *            Hoehe des Polygons, an dem das Profil angebracht werden soll
	 * @return Berechneter Skalierungsfaktor
	 */
	protected float scaleProfile(final float heightRatio,
			final MyPolygon profile, final float profileHeight,
			final float targetPolygonHeight) {

		float absoluteProfileHeight = targetPolygonHeight * heightRatio;
		float scale = absoluteProfileHeight / profileHeight;
		profile.scale(scale);
		return scale;
	}

	// ------------------------------------------------------------------------------------------

}
