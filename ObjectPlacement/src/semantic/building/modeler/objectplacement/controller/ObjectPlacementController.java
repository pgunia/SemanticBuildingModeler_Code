package semantic.building.modeler.objectplacement.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import semantic.building.modeler.configurationservice.model.ObjectPlacementFootprintConfiguration;
import semantic.building.modeler.math.MyPolygon;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.MyVectormath;
import semantic.building.modeler.math.Ray;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.objectplacement.algorithm.ContentType;
import semantic.building.modeler.objectplacement.algorithm.Quadtree;
import semantic.building.modeler.objectplacement.algorithm.QuadtreeNode;
import semantic.building.modeler.objectplacement.model.AbstractComponent;
import semantic.building.modeler.objectplacement.model.ComponentDescriptor;
import semantic.building.modeler.objectplacement.model.FreeComponent;
import semantic.building.modeler.objectplacement.model.RectComponent;

/**
 * 
 * @author Patrick Gunia Klasse implementiert die Positionierungslogik
 * 
 */

public class ObjectPlacementController {

	/** Logger */
	protected static Logger logger = Logger
			.getLogger(ObjectPlacementController.class);

	/** Quadtree zur Unterteilung der Eingabeflaeche */
	private Quadtree mTree = null;

	/** Zufallsgenerator */
	private Random mRandom = null;

	/** Haupttrakt des zu erstellenden Gebaeudes */
	private AbstractComponent mMainComponent = null;

	/** Enthaelt alle waeherend der Berechnung erzeugten Komponenten */
	private List<AbstractComponent> mComponents = null;

	/**
	 * Datenstruktur zur Speicherung von Komponenten pro Strahl, dadurch kann
	 * schnell ermittelt werden, wie viele und welche Komponenten bereits auf
	 * einem bestimmten Strahl positioniert wurden
	 */
	private Map<Ray, List<AbstractComponent>> mComponentsOnRay = null;

	/**
	 * Verwaltet die Anzahl der Positionierungsversuche pro Strahl, soll
	 * Endlosschleifen bei der Positionierung verhindern
	 */
	private Map<Ray, Integer> mRetriesPerRay = null;

	/** Instanz der Mathebibliothek */
	private MyVectormath mMathHelper = MyVectormath.getInstance();

	/**
	 * Skalierungsfaktor des minimalen Boundingrechtecks bei beliebigen
	 * Grundrissstrukturen. Wird verwendet, um das initial berechnete
	 * Boundingrechteck zu skalieren und dadurch einen initialen Grundbereich zu
	 * definieren, der den Eingabegrundriss vollstaendig enthaelt
	 */
	private float mMinAreaRectScale = 1.5f;

	/**
	 * Maximale Laenge eines Kreissegments bei zylindrischen Komponenten => wird
	 * fuer die Berechnung der Segmentanzahl verwendet
	 */
	private float mMaxKreissegmentLength = 14.0f;

	/** Instanz der Xml-basierten Footprintkonfiguration */
	private ObjectPlacementFootprintConfiguration mPlacementConfigXml = null;

	// ------------------------------------------------------------------------------------------

	/**
	 * Standardkonstruktor
	 * 
	 * @param corners
	 *            Eckpunkte, die den Bereich beschreiben, innerhalb dessen
	 *            Komponenten durch das Verfahren positioniert werden sollen
	 * @param footprintConf
	 *            Konfigurationsinstanz der Placement-Konfigurationsklasse
	 */
	public ObjectPlacementController(List<Vertex3d> corners,
			final ObjectPlacementFootprintConfiguration footprintConf) {

		init();

		mTree = new Quadtree(corners);

		// mPlacementConfig = new ObjectPlacementConfigurator();
		mPlacementConfigXml = footprintConf;

		// unterteile
		mTree.createSubdivision();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Konstruktor mit Uebergabe eines Grundrisses und eines spezifischen
	 * Konfigurationsobjekts
	 * 
	 * @param corners
	 *            Eingabegrundriss, der durch das Berechnungsverfahren
	 *            modifiziert wird
	 * @param placementConfiguration
	 *            Instanz einer Placementkonfiguration, durch die die Erzeugung
	 *            der Grundrisse angepasst und gesteuert werden kann
	 * @param isFootprint
	 *            Flag gibt an, ob es sich bei der uebergebenen Vertexliste um
	 *            eine Beschreibung des Footprints oder des Eingabebereichs
	 *            handelt => TRUE := Vertexliste beschreibt Footprint, FALSE
	 *            sonst
	 */
	public ObjectPlacementController(final List<Vertex3d> corners,
			final ObjectPlacementFootprintConfiguration footprintConf,
			boolean isFootprint) {

		init();
		mPlacementConfigXml = footprintConf;
		MyVectormath mathHelper = MyVectormath.getInstance();

		// die Eingabevertices beschreiben ein Polygon, berechne zunaechst ein
		// minimales Boundingrechteck, das den Eingabebereich fuer den
		// Algorithmus definiert
		// das Eingabepolygon dient in diesem Fall als Hauptkomponente

		if (isFootprint) {
			// bverechne ein minimales Bounding-Rechteck fuer die
			// Eingabevertices
			MyPolygon minAreaRect = mathHelper.getMinAreaRect(corners);
			mMainComponent = new FreeComponent(corners);
			mComponents.add(mMainComponent);

			ComponentDescriptor componentDescriptor = new ComponentDescriptor();
			MyPolygon componentPoly = mMainComponent.getPolygon();
			componentDescriptor.setCenter(componentPoly.getCenter());
			componentDescriptor.setComponentType("FreeComponent");
			componentDescriptor.setGroundPlane(componentPoly.getPlane());

			List<Float> dimensions = mathHelper
					.getDimensionsByAxis(componentPoly);
			componentDescriptor.setWidth(dimensions.get(0));
			componentDescriptor.setHeight(dimensions.get(1));
			mMainComponent.setComponentDescriptor(componentDescriptor);

			// skaliere das Rechteck hoch, so dass ein gueltiger Eingabebereich
			// entsteht
			logger.trace("MinAreaRect: " + minAreaRect);
			minAreaRect.scale(mMinAreaRectScale);

			// mPlacementConfig = placementConfiguration;
			mTree = new Quadtree(minAreaRect.getVertices());
		}

		// sonst den Quadtree ueber die Vertexliste erzeugen
		else {
			mTree = new Quadtree(corners);
		}

		// unterteile
		mTree.createSubdivision();

		// wenn es sich um einen zu modifizierenden Footprint handelt,
		if (isFootprint) {
			mTree.setMarksForComponent(mMainComponent, mComponents);
		}

		// erzeuge einen Placement-Configurator unter Verwendung der
		// uerbegebenen Placementparameter
		/*
		 * mPlacementConfig = new ObjectPlacementConfigurator(configFilename);
		 * logger.debug(mPlacementConfig);
		 */
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Initialisierungsroutine
	 */
	private void init() {
		mRandom = new Random();
		mComponents = new ArrayList<AbstractComponent>();
		mMathHelper = MyVectormath.getInstance();
		mComponentsOnRay = new HashMap<Ray, List<AbstractComponent>>();
		mRetriesPerRay = new HashMap<Ray, Integer>();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode umfasst die vollstaendige Positionierungslogik und soll von
	 * aussen gecallt werden
	 */
	public List<AbstractComponent> computeComponents() {
		logger.trace("Computing MainComponent...");
		positionMainComponent();
		logger.trace("Computing MainComponent...done");
		logger.trace("Computing SubComponents...");
		positionSubComponents();
		logger.trace("Computing SubComponents...done");

		for (int i = 1; i < mPlacementConfigXml.getNumberOfIterations(); i++) {
			positionSubSubComponents();
		}

		return mComponents;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode wird verwendet, um einen bereits existierenden Grundriss
	 * nachtraeglich durch das Positionieren von Subkomponenten zu modifizieren.
	 * In diesem Fall enthaelt die Klasse bereits eine gesetzte Hauptkomponente.
	 * An dieser werden nachtrraeglich weitere Komponenten positioniert
	 */
	public void modifyExsitingFootprint() {
		logger.trace("Modifying existing Footprint...");
		logger.trace("Computing SubComponents...");
		positionSubComponents();
		logger.trace("Computing SubComponents...done");

		for (int i = 1; i < mPlacementConfigXml.getNumberOfIterations(); i++) {
			logger.trace("Positioning SubSubComponents...");
			positionSubSubComponents();
			logger.trace("Positioning SubSubComponents...done");
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet weitere Komponentenebenen. Hierbei wird das
	 * Hauptverfahren unveraendert eingesetzt. Jede in einer vorherigen
	 * Iteration positionierte Subkomponente wird dabei als Hauptkomponente
	 * aufgefasst, mit der das Standardverfahren gestartet wird. Dadurch lassen
	 * sich die Berechnungsschritte unveraendert fuer die Berechnung einsetzen.
	 */
	private void positionSubSubComponents() {
		logger.trace("Computing SubSubComponents...");

		// durchlaufe nun alle bereits positionierten Subkomponenten und
		// positioniere weitere Subkomponenten auf diesen
		// sammele hierfuer zunaechst alle Komponenten ein
		List<AbstractComponent> subcomponents = new ArrayList<AbstractComponent>();
		Set<Ray> keys = mComponentsOnRay.keySet();
		Iterator<Ray> keyIter = keys.iterator();
		Ray currentKey = null;

		while (keyIter.hasNext()) {
			currentKey = keyIter.next();
			subcomponents.addAll(mComponentsOnRay.get(currentKey));
		}

		// Map-Struktur loeschen => diese wird in der nachfolgenden Berechnung
		// mit den neuen Komponenten befuellt
		// Struktur speichert fuer jeden innerhalb des Durchlaufs vorkommenden
		// Strahl saemtliche auf diesem positionierten Komponenten
		mComponentsOnRay.clear();

		AbstractComponent currentSubcomponent = null;
		for (int i = 0; i < subcomponents.size(); i++) {
			currentSubcomponent = subcomponents.get(i);
			// auf zylindrischen Subkomponenten werden keine weiteren
			// Komponenten positioniert
			if (currentSubcomponent.getType().equals("cylinder"))
				continue;
			else
				mMainComponent = currentSubcomponent;
			positionSubComponents();
		}
		logger.trace("Computing SubSubComponents...done");
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * @return the mTree
	 */
	public Quadtree getTree() {
		return mTree;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode setzt alle verwendeten Datenstrukturen zurueck, wird aufgerufen,
	 * sobald eine neue Hauptkomponente erzeugt wird
	 */
	private void reset() {

		// loesche alle vorhandenen Subkomponenten
		mComponents.clear();

		// sowie die strahlbasierten Subkomponenten
		mComponentsOnRay.clear();

		// ebenfalls die Retry-Verwaltung
		mRetriesPerRay.clear();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode positioniert den Haupttrakt innerhalb des vorgegebenen Bereichs.
	 * Der Haupttrakt besteht zunaechst aus einem Rechteck, dessen Laenge und
	 * Breite innerhalb eines zufaellig generierten Bereichs bzgl. der Ausmasse
	 * des Eingabebereichs schwanken
	 */
	public void positionMainComponent() {

		// Datenstrukturen zuruecksetzen
		reset();

		final ComponentDescriptor mainComponentDescriptor = new ComponentDescriptor();
		Float width = mTree.getRoot().getWidth();
		Float height = mTree.getRoot().getHeight();

		// berechne den Range ueber die Konfigurationswerte der Property-Datei
		// => die geladenen Werte beschreiben Anteile an der Gesamtausdehnung
		// des
		// Positionierungsbereichs
		Float range = mPlacementConfigXml.getRatioScalingMaincomponent()
				.getUpperBorder()
				- mPlacementConfigXml.getRatioScalingMaincomponent()
						.getLowerBorder();

		// normalisiere auf den vorkonfigurierten Wertebereich
		Float widthProportion = mRandom.nextFloat();
		widthProportion *= range;
		widthProportion += mPlacementConfigXml.getRatioScalingMaincomponent()
				.getLowerBorder();

		Float heightProportion = mRandom.nextFloat();
		heightProportion *= range;
		heightProportion += mPlacementConfigXml.getRatioScalingMaincomponent()
				.getLowerBorder();

		// hole Breiten- und Hoehenachse des Eingabebereichs
		MyVector3f upperEdge = mTree.getRoot().getUpperEdgeDirection();
		MyVector3f rightEdge = mTree.getRoot().getRightEdgeDirection();

		// berechne die Ausdehnungen des Zielobjekts
		Float absoluteWidth = width * widthProportion;
		Float absoluteHeight = height * heightProportion;

		mainComponentDescriptor.setHeight(absoluteHeight);
		mainComponentDescriptor.setWidth(absoluteWidth);

		// berechne die Ausgangsvektoren fuer die neuen Punkte
		upperEdge.normalize();

		// nimm die rechte Kante, da diese in die richtige Richtung zeigt!
		// (Uhrzeigersinn)
		rightEdge.normalize();

		mainComponentDescriptor.setHeightAxis(rightEdge);
		mainComponentDescriptor.setWidthAxis(upperEdge);

		// Grundebene der Komponente setzen => entspricht der Ebene des
		// Eingabebereichs
		mainComponentDescriptor.setGroundPlane(mTree.getRoot().getPolygon()
				.getPlane());

		// Mittelpunkt der Grundebene verwenden => an diesem Punkt soll die
		// Hauptkomponente positioniert werden
		mainComponentDescriptor.setCenter(mTree.getRoot().getCenter());

		mMainComponent = new RectComponent(mainComponentDescriptor);
		mComponents.add(mMainComponent);

		logger.info("Positionierte Komponente: " + mMainComponent);

		// markiere alle verwendeten Nodes im Quadtree
		mTree.setMarksForComponent(mMainComponent, mComponents);

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode liefert alle erzeugten Komponenten an den Aufrufer zurueck
	 * 
	 * @return Saemtliche im Laufe der Berechnung erzeugten Komponenten. Dabei
	 *         ist die Hauptkomponente immer die erste Komponente innerhalb der
	 *         Rueckgabeliste
	 */
	public List<AbstractComponent> getComponents() {

		return mComponents;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Positioniere Subkomponenten auf allen Strahlen der Hauptkomponente.
	 */
	private void positionSubComponents() {

		// nur dann Objekte auf Eckpunkten positionieren, falls es sich um die
		// Hauptkomponente handelt und das Losglueck entscheidet
		if (mMathHelper.decide(mPlacementConfigXml
				.getProbPositionSubcomponentsOnCorners())
				&& !mMainComponent.isSubComponent())
			positionSubComponentsOnCorners();

		// hole schrittweise die Nachbarn der Strahlen-Knoten im Quadtree
		MyPolygon mainComponentPoly = mMainComponent.getPolygon();
		List<Ray> rays = mainComponentPoly.getRays();
		Ray currentRay = null;
		Iterator<Ray> rayIter = rays.iterator();

		Boolean useSymmetry = mMathHelper.decide(mPlacementConfigXml
				.getProbSymmetry());
		Boolean symmetryPossible = false;

		int rayIndex = 0;
		while (rayIter.hasNext()) {
			currentRay = rayIter.next();
			rayIndex++;

			// sollen fuer die aktuelle Kante Subkomponenten berechnet werden?
			if (mMathHelper.decide(mPlacementConfigXml
					.getProbPositionComponentsOnEdge())) {

				// teste, ob die Hauptkomponente ein Rechteck ist, wenn nicht,
				// kann nicht symmetrisch positioniert werden
				if (mMathHelper.isRectangle(mMainComponent.getPolygon())) {

					// teste die Vorbedingungen, die fuer eine symmetrische
					// Positionierung der Komponenten erfuellt sein muessen
					if (rayIndex > 2) {
						List<AbstractComponent> components = mComponentsOnRay
								.get(rays.get(rayIndex - 3));
						if (components != null && components.size() > 0)
							symmetryPossible = true;
						else
							symmetryPossible = false;
					}
				}

				if (useSymmetry && symmetryPossible) {
					positionSubComponentsOnEdgesBySymmetry(currentRay);
				} else {
					positionSubComponentsOnEdges(currentRay);
				}
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode positioniert Subkomponenten auf den Eckpunkten der
	 * Hauptkomponente
	 */
	private void positionSubComponentsOnCorners() {

		logger.info("Positionierung von Subkomponenten auf Eckpunkten...");
		int numberOfVerts = mMainComponent.getPolygon().getVertices().size();

		final List<Vertex3d> cornerVerts = new ArrayList<Vertex3d>(
				numberOfVerts);
		cornerVerts.addAll(mMainComponent.getPolygon().getVertices());

		Ray currentRay = null;
		float horizontalDistance = Float.MAX_VALUE;
		float verticalDistance = Float.MAX_VALUE;
		float minNeighbourDistance = Float.MAX_VALUE;

		// berechne Abstaende zu allen 4 Kanten des Aussenbereiches => dieser
		// ist nach Konstruktion immer rechteckig

		// berechne die relevanten Distanzen fuer alle Eckpunkte des Polygons,
		// aktualisiere dabei die minimalen Distanzparameter
		for (Vertex3d currentVert : cornerVerts) {
			final List<Float> distances = calculateDistances(cornerVerts,
					currentVert);
			if (distances.get(0) < horizontalDistance)
				horizontalDistance = distances.get(0);
			if (distances.get(1) < verticalDistance)
				verticalDistance = distances.get(1);
			if (distances.get(2) < minNeighbourDistance)
				minNeighbourDistance = distances.get(2);
		}

		// wenn der minimale Nachbarabstand kleiner als die Randabstaende ist,
		// wird die Ausdehnung durch diesen Abstand modifiziert
		if (minNeighbourDistance < horizontalDistance
				&& minNeighbourDistance < verticalDistance) {
			horizontalDistance = minNeighbourDistance;
			verticalDistance = minNeighbourDistance;
		}

		// hole die Descriptor-Instanz der Hauptkomponente
		final ComponentDescriptor mainComponentDescriptor = mMainComponent
				.getComponentDescriptor();
		final Float mainComponentHeight = mainComponentDescriptor.getHeight();
		final Float mainComponentWidth = mainComponentDescriptor.getWidth();
		final Float maxRatioCornerComponentToMain = mPlacementConfigXml
				.getMaxCornerMainComponentDimensionsRatio();

		// liegen die Dimensionen der Subkomponente im akzeptablen Bereich bzgl.
		// der Ausdehnung der Hauptkomponente?
		// wenn nein, setze sie auf die Obergrenze
		if (verticalDistance / mainComponentHeight > maxRatioCornerComponentToMain) {
			verticalDistance = mainComponentHeight
					* maxRatioCornerComponentToMain;
		}

		if (horizontalDistance / mainComponentWidth > maxRatioCornerComponentToMain) {
			horizontalDistance = mainComponentWidth
					* maxRatioCornerComponentToMain;
		}

		ComponentDescriptor descriptor = null;
		final List<Ray> rays = mMainComponent.getPolygon().getRays();

		Ray neighbourRay = null;
		final List<ComponentDescriptor> componentDescriptors = new ArrayList<ComponentDescriptor>(
				numberOfVerts);

		// erzeuge einen Basisdeskriptor, der fuer die Erzeugung aller
		// Komponenten verwendet wird
		// die einzelnen Komponenten unterscheiden sich ausschliesslich in ihrer
		// Position
		final ComponentDescriptor basicDescriptor = new ComponentDescriptor();

		// berechne die Ausdehnungen der Komponente basierend auf den vorab
		// berechneten Abstaenden von den Aussenkanten des Eingabebereichs
		// sollten die Ausdehnungen unterhalb der Minimumausdehnungen liegen,
		// breche ab und positioniere keine Komponenten
		if (!computeSubComponentDimensions(verticalDistance,
				horizontalDistance, basicDescriptor, mainComponentWidth))
			return;

		// die Komponenten benoetigen einheitliche Achsen, nehme die ersten
		// beiden Strahlen der Hauptkomponente
		currentRay = rays.get(0);
		neighbourRay = rays.get(1);

		basicDescriptor.setWidthAxis(currentRay.getDirection());
		basicDescriptor.setHeightAxis(neighbourRay.getDirection());
		basicDescriptor.setSubcomponent(true);

		basicDescriptor.setGroundPlane(mMainComponent.getPolygon().getPlane());

		// verwende ausschliesslich Zylinder als Eckelemente
		basicDescriptor.setComponentType("CylindricComponent");
		basicDescriptor.setNumberOfSegments(getNumberOfCylindricSegments(
				basicDescriptor.getWidth(), basicDescriptor.getHeight()));

		// erzeuge fuer jeden Eckpunkt eine Kopie des Basic-Descriptos und
		// aendere nur die Position
		for (Vertex3d currentVert : cornerVerts) {

			if (!mMathHelper.decide(mPlacementConfigXml
					.getProbPositionSubcomponentsOnCorners())) {
				continue;
			}
			descriptor = basicDescriptor.clone();
			descriptor.setCenter(currentVert.getPosition());
			componentDescriptors.add(descriptor);
		}

		AbstractComponent component = null;

		for (ComponentDescriptor currentDescriptor : componentDescriptors) {
			component = currentDescriptor.createSubComponent();
			component.setSubComponent(true);
			mComponents.add(component);
			mTree.setMarksForComponent(component, mComponents);
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechent saemtliche Abstaende, die fuer die Bestimmung der
	 * Komponentengroesse erforderlich sind
	 * 
	 * @param polyVerts
	 *            Vertices, die das Polygon der Komponente beschreiben, die
	 *            aktuell verarbeitet wird
	 * @param currentVertex
	 *            Eckvertex des Polygons, fuer das die Abstaende berechnet
	 *            werden
	 * @return Liste mit Abstaenden: 1. Abstand zur horizontalen Kante des
	 *         Aussenbereichs, 2. Abstand zur vertikalen Kante des
	 *         Aussenbereichs, 3. minimaler Abstand zu Nachbarn
	 */
	private List<Float> calculateDistances(final List<Vertex3d> polyVerts,
			final Vertex3d currentVertex) {

		final List<Float> result = new ArrayList<Float>(3);
		Ray currentRay = null;
		final List<Ray> rays = mTree.getRoot().getPolygon().getRays();
		float currentDistance = 0.0f;
		float horizontalDistance = Float.MAX_VALUE, verticalDistance = Float.MAX_VALUE;

		// gerade Indices sind horizontale Kanten, ungerade vertikale
		for (int i = 0; i < rays.size(); i++) {
			currentRay = rays.get(i);
			currentDistance = mMathHelper.calculatePointEdgeDistance(
					currentVertex.getPositionPtr(), currentRay);

			// Abstand zur horizontalen Kante aktualisieren
			if (i % 2 == 0) {
				if (currentDistance < horizontalDistance)
					horizontalDistance = currentDistance;
			}
			// Abstand zur vertikalen Kante aktualisieren
			else {
				if (currentDistance < verticalDistance)
					verticalDistance = currentDistance;
			}
		}

		result.add(horizontalDistance);
		result.add(verticalDistance);

		// Bestimmung des minimalen Abstands zu einem der Nachbarknoten
		int index = polyVerts.indexOf(currentVertex);
		int previousIndex = -1;

		if (index == 0)
			previousIndex = polyVerts.size() - 1;
		else
			previousIndex = index - 1;

		final int nextIndex = (index + 1) % polyVerts.size();

		final Vertex3d prevVert = polyVerts.get(previousIndex);
		final Vertex3d nextVert = polyVerts.get(nextIndex);

		// bestimme Abstaende
		final float distanceNext = mMathHelper.calculatePointPointDistance(
				currentVertex.getPositionPtr(), nextVert.getPositionPtr());
		final float distancePrev = mMathHelper.calculatePointPointDistance(
				currentVertex.getPositionPtr(), prevVert.getPositionPtr());

		if (distanceNext < distancePrev)
			result.add(distanceNext);
		else
			result.add(distancePrev);
		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Ziel dieser Funktion ist die Verkapselung der Dimensionsberechnungen fuer
	 * Subkomponenten. Eingabe sind die berechnete Breite sowie die maximale
	 * Hoehe. Die Methode wertet die Placementkonfiguration aus und modifiziert
	 * darauf basierend die Masszahlen. Nach Abschluss werden die berechneten
	 * Werte zurueck in den Komponentendeskriptor geschrieben.
	 * 
	 * @param width
	 *            Zielbreite der Subkomponente
	 * @param maxHeight
	 *            Obergrenze fuer die Hoehe der Subkomponente
	 * @param descriptor
	 *            SubComponentDescriptor-Instanz, die fuer die Beschreibung der
	 *            zu erzeugenden Subkomponente verwendet wird
	 * @param targetWidth
	 *            Parameter beschreibt die Breite des Strahls, an der die
	 *            Komponente positioniert wird
	 * @return Sofern die berechneten Dimensionen unterhalb eines Grenzwerts
	 *         liegen, wird die Komponente nicht erzeugt und die Methode gibt
	 *         False zurueck, True sonst
	 */
	private boolean computeSubComponentDimensions(final Float width,
			final Float maxHeight, final ComponentDescriptor descriptor,
			final float targetWidth) {

		logger.debug("Eingabebreite: " + width + " Eingabehoehe: " + maxHeight);

		// modifiziere vertikale und horizontale Distanz zufallsbasiert
		Float randomScale = mRandom.nextFloat();
		final Float range = mPlacementConfigXml.getRatioScalingSubcomponents()
				.getUpperBorder()
				- mPlacementConfigXml.getRatioScalingSubcomponents()
						.getLowerBorder();

		// skaliere beide Komponenten innerhalb eines zufaelligen Bereichs
		// zwischen 0.5f...0.95f
		randomScale *= range;
		randomScale += mPlacementConfigXml.getRatioScalingSubcomponents()
				.getLowerBorder();

		final Float scaledWidth = width * randomScale;
		final Float scaledHeight = maxHeight * randomScale;

		// Berechne die Komponentenhoehe basierend auf dem Zielverhaeltnis von
		// Hoehe-zu-Breite ueber die vorab berechente Breite
		final Float heightWidth = mPlacementConfigXml
				.getHeightWidthRatioComponents().getRandValueWithinRange();
		logger.info("Height-Width-Ratio: " + heightWidth);
		Float newHeight = scaledWidth * heightWidth;

		// sollte der Hoehenwert ausserhalb des gueltigen Bereichs liegen,
		// verwende die skalierte Hoehe (dadurch wird das Zielverhaeltnis
		// verworfen)
		if (newHeight > maxHeight) {
			newHeight = scaledHeight;

		}

		assert scaledWidth >= newHeight : "FEHLER: Die berechnete Hoehe des Elements: "
				+ newHeight
				+ " ist groesser als die berechnete Breite: "
				+ scaledWidth;

		// teste, ob die berechneten Ausdehnungen oberhalb eines Minimums liegen
		// dies ist primaer fuer Corner-Subkomponenten relevant,
		// Edge-Komponenten werden a-priori mit einer Mindestgroesse erzeugt
		// verwende die Breitendimension als Testkriterium, da die
		// Hoehenkomponente aus dieser berechnet wird
		final QuadtreeNode leaf = mTree.getRoot().getLeafs().get(0);
		Float leafWidth = leaf.getWidth();
		Float leafHeight = leaf.getHeight();

		// Mindesthoehe muss gegeben sein, um Ueberlappungen der Haupt- mit den
		// Subkomponenten gewaehrleisten zu koennen. Da die Komponenten in den
		// Nodes
		// positioniert werden, die bereits eine Kante enthalten, entspricht die
		// Minimalhoehe der
		// Hoehe eines Blattes
		if (newHeight < leafHeight) {
			// System.out.println("Komponentenhoehe unterschritten! Hoehe: " +
			// newHeight + " Zielhoehe: " + leafHeight + " maxHeight: " +
			// maxHeight + " Eingabebreite: " + width);
			return false;
		}

		// liegt das Vehaeltnis zwischen der nun verwendeten Hoehe und Breite
		// innerhalb des vorgegeben Toleranzbereichs fuer Subkomponenten?
		float heightWidthRatio = newHeight / scaledWidth;
		if (heightWidthRatio < mPlacementConfigXml
				.getHeightWidthRatioComponents().getLowerBorder()
				|| heightWidthRatio > mPlacementConfigXml
						.getHeightWidthRatioComponents().getUpperBorder()) {
			logger.trace("Breite / Hoehe-Verhaeltnis: " + heightWidthRatio);
			return false;
		}

		// teste, ob das Verhaeltnis zwischen Breite der Subkomponente und der
		// Laenge der Zielkante (der Kante, auf der die Subkomponente
		// positioniert wird) innerhalb der vorgegebenen Parameter liegt
		float subCompWidthToRayLength = scaledWidth / targetWidth;

		if (subCompWidthToRayLength > mPlacementConfigXml
				.getMaxSubToMainComponentWidth()) {
			logger.trace("Breitenverhaeltnis oberhalb des Grenzwerts: "
					+ subCompWidthToRayLength);
			return false;
		}

		// Berechne das Verhaeltnis zwischen Subkomponentenbreite und
		// Mainkomponenten-Hoehe =>
		// verhindere Subkomponenten, die der Hauptkomponentenausdehnung zu
		// stark aehneln => berechne dafuer das Verhaeltnis zwischen Haupt- und
		// Subkomponentenbreite
		Float mainToSubcomponentWidthRatio = null;
		mainToSubcomponentWidthRatio = targetWidth / scaledWidth;
		if (Math.abs(mainToSubcomponentWidthRatio - 1.0f) < mPlacementConfigXml
				.getMinMainToSubcomponentDeviance())
			return false;

		// liegt die Breite der Komponente oberhalb des festgelegten
		// Mindestwertes?
		float minNumberOfBlocks = mPlacementConfigXml
				.getMinSubcomponentSizeInBlocks();
		descriptor.setWidth(scaledWidth);
		descriptor.setHeight(newHeight);
		if (leafWidth * minNumberOfBlocks > scaledWidth) {
			logger.trace("Komponentenbreite unterschritten! Breite: "
					+ scaledWidth + " Zielbreite: " + leafWidth
					* minNumberOfBlocks);
			return false;
		} else
			return true;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode positioniert Subkomponenten auf dem uebergebenen Strahl.
	 * 
	 * @param ray
	 *            Strahl, an dessen freien Knoten Subkomponenten positioniert
	 *            werden sollen
	 */
	private void positionSubComponentsOnEdges(final Ray ray) {

		logger.info("Positionierung von Subkomponenten auf Kanten...");

		// berechne die maximale Hoehe einer Subkomponente, die auf dem Strahl
		// positioniert wird
		float heightSubcomponent = computeMinHeightByIntersection(ray);

		// setze Komponenten auf den Strahl
		while (positionSubComponent(heightSubcomponent, ray))
			;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet die minimale Hoehe einer zu positionierenden
	 * Subkomponente anhand von Schnittpunkttests mit den Kanten des
	 * Randbereichs. Hierfuer bestimmt man einen Strahl, der orthogonal zur
	 * Zielkante verlaeuft und berechnet mit diesem Schnitte mit allen Kanten
	 * des Wurzelbereichs des Quadtrees
	 * 
	 * @param ray
	 *            Strahl, an dem eine Komponente positioniert werden soll
	 * @return Maximale Hoehe, die eine Komponente haben darf, damit sie den
	 *         Eingabebereich nicht ��berschneidet
	 */
	private float computeMinHeightByIntersection(final Ray ray) {

		// berechne einen Vektor, der senkrecht zum uebergebenen Strahl
		// verl��uft
		final MyVector3f orthogonalVector = mMathHelper
				.calculateOrthogonalVectorWithSamePlane(ray.getDirection(),
						mTree.getRoot().getPolygon().getNormal());

		Ray testRay = null;
		float currentDistance = 0.0f, minDistance = Float.MAX_VALUE;

		// durchlaufe Anfang und Ende des Strahls und berechne Schnitt mit
		// jeweils allen Randstrahlen und beiden Richtungen des senkrechten
		// Strahls
		final MyVector3f rayStart = ray.getStart();
		final MyVector3f rayEnd = ray.getEnd();

		// jeweils mit Strahlanfang und -ende Entfernungen berechnen
		testRay = new Ray(rayStart, orthogonalVector);
		currentDistance = getMinDistanceForRay(testRay);
		if (currentDistance < minDistance)
			minDistance = currentDistance;

		testRay = new Ray(rayEnd, orthogonalVector);
		currentDistance = getMinDistanceForRay(testRay);
		if (currentDistance < minDistance)
			minDistance = currentDistance;

		// jetzt den senkrechten Vektor drehen und erneut Abstaende bestimmen
		orthogonalVector.scale(-1.0f);
		testRay = new Ray(rayStart, orthogonalVector);
		currentDistance = getMinDistanceForRay(testRay);
		if (currentDistance < minDistance)
			minDistance = currentDistance;

		testRay = new Ray(rayEnd, orthogonalVector);
		currentDistance = getMinDistanceForRay(testRay);
		if (currentDistance < minDistance)
			minDistance = currentDistance;

		// minimalen Abstand zurueckgeben, dieser beschreibt die maximal
		// moegliche Hoehe
		return minDistance;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet Schnittpunkte des uebergebenen Strahs mit allen Kanten
	 * des Wurzelknotenbereichs und liefert den Schnittpunkt mit minimalem
	 * Abstand zurueck
	 * 
	 * @param testRay
	 *            Strahl, fuer den Schnittpunkte mit den Aussenbereichskanten
	 *            berechnet werden
	 * @return Minimale Distanz der gefundenen Schnittpunkte vom Startpunkt des
	 *         Strahls
	 */
	private float getMinDistanceForRay(Ray testRay) {

		float minDistance = Float.MAX_VALUE, currentDistance = 0.0f;
		List<Ray> areaRays = mTree.getRoot().getPolygon().getRays();

		Ray currentRay = null;
		MyVector3f intersection = null;

		for (int i = 0; i < areaRays.size(); i++) {
			currentRay = areaRays.get(i);

			intersection = mMathHelper.calculateRay2RayIntersectionMatrixStyle(
					testRay, currentRay);
			if (intersection == null)
				continue;

			currentDistance = mMathHelper.calculatePointPointDistance(
					testRay.getDirectionPtr(), intersection);
			if (currentDistance < minDistance)
				minDistance = currentDistance;

		}

		return minDistance;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode positioniert Subkomponenten auf der Kante, die parallel zum
	 * uebergebenen Strahl verlaueft. Hierfuer verwendet sie die vorab
	 * berechneten Descriptorinstanzen, die den Komponenten auf dem
	 * Uebergabestrahl zugewiesen sind und erzeugt Nachbauten der aus diesen
	 * Plaenen erstellten Komponenten
	 * 
	 * @param targetRay
	 *            Strahl, auf dem Komponenten positioniert werden sollen
	 */
	private void positionSubComponentsOnEdgesBySymmetry(Ray targetRay) {

		logger.info("Positionierung symmetrischer Subkomponenten...");
		assert mMainComponent != null : "FEHLER: Es wurde noch keine Hauptkomponente positioniert";

		// bestimme die Ausrichtung des Strahls basierend auf dem Index in der
		// Rayliste der Hauptkomponente
		// gerader Index: horizontal, ungerader Index: vertikal
		List<Ray> mainComponentRays = mMainComponent.getPolygon().getRays();
		int rayIndex = mainComponentRays.indexOf(targetRay);

		// diese Methode wird erst dann aufgerufen, wenn bereits 2 Strahlen
		// verarbeitet wurden
		// der parallele Zielstrahl ist aufgrund der RECT-Componente als Basis
		// immer 2 Indices vor dem Uebergabestrahl
		rayIndex -= 2;
		Ray sourceRay = mainComponentRays.get(rayIndex);

		// ebenfalls aufgrund der Definition im Uhrzeigersinn so machbar =>
		// Index des Verbindungstrahls zwischen
		// dem Quell- und dem Zielstrahl ist immer um 1 groesser
		int nextIndex = rayIndex + 1;

		// berechne den Abstand zwischen den beiden Strahlen ueber die Laenge
		// der Zwischenkanten
		// und den doppelten Abstand der positionierten Komponenten von der
		// Aussenkante des Uebergabestrahls
		Ray connectingRay = mainComponentRays.get(nextIndex);

		List<AbstractComponent> rayComponents = mComponentsOnRay.get(sourceRay);
		assert rayComponents != null && rayComponents.size() > 0 : "FEHLER: Auf dem parallelen Strahl wurden noch keine Komponenten positioniert";

		ComponentDescriptor currentDescriptor = null, newDescriptor = null;

		Iterator<AbstractComponent> componentIter = rayComponents.iterator();
		AbstractComponent newComponent = null, currentComponent = null;

		List<AbstractComponent> componentsOnNewRay = mComponentsOnRay
				.get(targetRay);
		assert componentsOnNewRay == null : "FEHLER: Auf dem Zielstrahl wurden bereits Komponenten positioniert";

		componentsOnNewRay = new ArrayList<AbstractComponent>(
				mPlacementConfigXml.getMaxNumberOfSubcomponentsPerRay());

		float translationDistance;
		MyVector3f translateVector = null;

		// erzeuge Kopien der Descriptoren der positionierten Komponenten auf
		// dem Quellstrahl
		while (componentIter.hasNext()) {
			currentComponent = componentIter.next();

			// berechne die Verschiebungsdistanz fuer die aktuelle Komponente
			translationDistance = computeTranslationDistance(currentComponent,
					sourceRay, connectingRay);
			translateVector = connectingRay.getDirection();
			translateVector.normalize();
			translateVector.scale(translationDistance);

			currentDescriptor = currentComponent.getComponentDescriptor();

			assert currentDescriptor != null : "FEHLER: Fuer die uebergebene Subkomponente ist kein Descriptor definiert";
			newDescriptor = currentDescriptor.clone();

			// berechne die neue Position der kopierten Komponenten ueber den
			// vorab berechneten Translate-Vektor
			newDescriptor.getCenterPtr().add(translateVector);

			// drehe abschliessend die Komponentenachsen
			newDescriptor.getHeightAxisPtr().scale(-1.0f);
			newDescriptor.getWidthAxisPtr().scale(-1.0f);

			newComponent = newDescriptor.createSubComponent();
			componentsOnNewRay.add(newComponent);
			mComponents.add(newComponent);

			mTree.setMarksForComponent(newComponent, mComponents);

		}
		mComponentsOnRay.put(targetRay, componentsOnNewRay);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet die Translationsdistanz fuer die Quellkomponente. Diese
	 * wird benoetigt, um bei einer symmetrischen Positionierung eine korrekte
	 * Verschiebung auf die Zielkante zu erreichen.
	 * 
	 * @param sourceComponent
	 *            Quellkomponente, von der eine verschobene Kopie auf der
	 *            Zielkante erzeugt wird
	 * @param sourceRay
	 *            Quellkante, auf der die Quellkomponente positioniert wurde
	 * @param translationRay
	 *            Strahl, der die Verbindungskante zwischen Quell- und Zielkante
	 *            beschreibt
	 * @return Verschiebungsentfernung in Richtung des Verschiebungsstrahls
	 */
	private float computeTranslationDistance(AbstractComponent sourceComponent,
			Ray sourceRay, Ray translationRay) {

		MyPolygon mainComponentPoly = mMainComponent.getPolygon();
		MyVector3f sourceComponentCenter = sourceComponent
				.getComponentDescriptor().getCenter();

		// teste zunaechst, ob der Mittelpunkt der Quellkomponente innerhalb der
		// Mainkomponente liegt
		boolean isSubComponentCenterWithinMainComponent = mMathHelper
				.isPointInPolygon(mainComponentPoly, sourceComponentCenter);

		// berechne den Abstand des Source-Centers zur Quellkante
		float distanceToRay = mMathHelper.calculatePointEdgeDistance(
				sourceComponentCenter, sourceRay);

		float result;

		// liegt der Centerpunkt innerhalb der Mainkomponente, muss der Abstand
		// zur Kante abgezogen, sonst addiert werden
		if (isSubComponentCenterWithinMainComponent)
			result = translationRay.getDirectionPtr().length() - 2
					* distanceToRay;
		else
			result = translationRay.getDirectionPtr().length() + 2
					* distanceToRay;

		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erzeugt eine Subkomponente auf dem uebergebenen Strahl. Die Hoehe
	 * dieser Komponente wird dabei definiert durch den uebergebenen Hoehenwert,
	 * die Breite wird ueber Allocation-Informationen bestimmt und durch die
	 * uerbegebene Breite nach oben begrenzt. Nach Abschluss der Berechnungen
	 * wird geprueft, ob noch weitere Komponenten erzeugt und positioniert
	 * werden sollen.
	 * 
	 * @param heightSubcomponent
	 *            Hoehe der neu zu erzeugenden Subkomponente
	 * @param ray
	 *            Strahl, auf dem die Subkomponenten positioniert werden sollen
	 * @return True, sofern nach Abschluss der Berechnung noch weitere
	 *         Komponenten gesetzt werden sollen, False sonst
	 */
	private boolean positionSubComponent(final Float heightSubcomponent,
			final Ray ray) {

		Integer numberOfRetries = mRetriesPerRay.get(ray);
		if (numberOfRetries != null)
			numberOfRetries++;
		else {
			numberOfRetries = 1;
		}
		mRetriesPerRay.put(ray, numberOfRetries);

		// lade die Knoten, die den Uebergabestrahl enthalten
		final List<QuadtreeNode> rayNodes = mTree.getNodesForRay(ray);

		logger.trace("Ray: " + ray);
		logger.trace("Anzahhl RayNodes: " + rayNodes.size());

		// Belegungsliste der Nachbarbloecke erstellen
		final List<ListAllocation> listAllocations = getAllocationForNodes(rayNodes);
		if (listAllocations.size() == 0) {
			logger.trace("Keine freien Noeds fuer Positionierung vorhanden");
			return false;
		}

		// hole die erste Allocation-Komponente, sofern die Anzahl der
		// neighbourNodes mit der Anzahl der freien Bloecke uebereinstimmt,
		// handelt es sich um die erste Subkomponente, die auf der Kante
		// positioniert wird
		Boolean firstSubcomponent = null;
		ListAllocation currentAllocationRegion = listAllocations
				.get(listAllocations.size() - 1);

		if (currentAllocationRegion.getNumberOfFreeBlocks() == rayNodes.size())
			firstSubcomponent = true;
		else
			firstSubcomponent = false;

		// bestimme den Punkt, an dem die Subkomponente positioniert wird
		final MyVector3f position = getPositionForSubcomponent(rayNodes,
				currentAllocationRegion);

		Float width = null;

		// wenn bereits Komponenten positioniert wurden, verwende die
		// Allocationstabellen fuer die Dimensionsberechnung
		if (!firstSubcomponent) {
			width = getMaxWidthWithinAllocationRegion(position,
					currentAllocationRegion, rayNodes);
		} else {
			width = ray.getLength();
		}

		// zufallsbasiert skalieren, verwende einen zufallsbasierten float und
		// einen zufallsbasierten int-Wert
		float scale = mRandom.nextFloat();
		int intScale = mRandom.nextInt(3) + 1;
		scale *= intScale;
		width *= scale;

		// bestimme den Nachbarstrahl des aktuell verarbeiteten Strahls in der
		// positionierten Hauptkomponente
		Ray neighbourRay = null;
		List<Ray> mainComponentRays = mMainComponent.getPolygon().getRays();

		int indexRay = mainComponentRays.indexOf(ray);
		indexRay = (indexRay + 1) % mainComponentRays.size();

		neighbourRay = mainComponentRays.get(indexRay);
		ComponentDescriptor descriptor = new ComponentDescriptor();

		// teste, ob die Strahlen einen rechten Winkel bilden
		if (!mMathHelper.isAngleMultipleOf90(ray.getDirection(),
				neighbourRay.getDirection())) {

			// wenn nicht, berechne einen Vektor, der senkrecht zum
			// Ausgangsstrahl steht und dadurch die zweite Achse der Komponente
			// bildet
			MyVector3f secondAxis = mMathHelper
					.calculateOrthogonalVectorWithSamePlane(ray.getDirection(),
							mMainComponent.getPolygon().getNormal());
			neighbourRay.setDirection(secondAxis);

		}

		// berechne die Ausdehnungen der Subkomponente
		// wenn die brechneten Ausdehnungen ein Minimum unterschreiten, wird
		// keine Komponente gesetzt
		if (computeSubComponentDimensions(width, heightSubcomponent,
				descriptor, ray.getLength())) {

			descriptor.setCenter(position);
			descriptor.setWidthAxis(ray.getDirection());
			descriptor.setHeightAxis(neighbourRay.getDirection());
			descriptor.setSubcomponent(true);

			// Zylinder werden nur dann positioniert, falls es sich um die erste
			// gesetzte Komponente handelt
			if (mMathHelper.decide(mPlacementConfigXml
					.getProbPositionCylinders()) && firstSubcomponent) {

				// berechne die Segmentanzahl als Funktion des Radius
				descriptor.setComponentType("CylindricComponent");
				descriptor.setNumberOfSegments(getNumberOfCylindricSegments(
						descriptor.getWidth(), descriptor.getHeight()));
			}

			descriptor.setGroundPlane(mTree.getRoot().getPolygon().getPlane()
					.clone());

			AbstractComponent subComponent = descriptor.createSubComponent();

			// wenn es sich um eine Zylinderkomponente handelt, sorge dafuer,
			// dass die Vertices symmetrisch zu einer Kante sind, die senkrecht
			// zur Zielkante
			// verlaeuft => dadurch vermeidet man Zylinder, die ungleichmaessig
			// vom Rest der Geometrie geschnitten werden
			if (descriptor.getComponentClassName() == "CylindricComponent") {

				final MyPolygon poly = subComponent.getPolygon();
				final MyVector3f center = descriptor.getCenter();

				final MyVector3f currentAxis = new MyVector3f();

				// Achse vom ersten Vertex durch den Kreismittelpunkt
				currentAxis
						.sub(poly.getVertices().get(0).getPosition(), center);
				double angleToTargetRay = mMathHelper.calculateAngleRadians(
						currentAxis, ray.getDirection());

				// die Achsen sollen senkrecht aufeinander stehen
				double halfPi = Math.PI / 2.0d;
				double diffAngle = halfPi - angleToTargetRay;

				// wenn der Unterschied groesser als 1 Grad ist, rotiere
				// saemtliche Punkte um den Mittelpunkt des Kreises
				if (!mMathHelper.isWithinTolerance(diffAngle, 0.0d,
						Math.toRadians(1.0d))) {

					MyVector3f rotationAxisDir = descriptor.getGroundPlane()
							.getNormal();
					Ray rotationAxis = new Ray(center, rotationAxisDir);
					mMathHelper.rotatePolygonAroundArbitraryAxis(rotationAxis,
							poly, -diffAngle);
					logger.debug("Rotation um " + -diffAngle
							+ " Grad durchgefuehrt!");
				}

			}

			// teste, ob sich das Polygon der neu erstellten Komponente mit dem
			// Aussenbereich ueberschneidet
			// wenn ja, dann wird die Komponente verworfen
			if (!mMathHelper.intersects(subComponent.getPolygon(), mTree
					.getRoot().getPolygon())) {
				mComponents.add(subComponent);

				logger.trace("Neue Subkomponente erzeugt!");

				// fuege die neu erzeugte Komponente zur Components-On-Ray-Liste
				// hinzu
				List<AbstractComponent> rayComponents = mComponentsOnRay
						.get(ray);
				if (rayComponents == null)
					rayComponents = new ArrayList<AbstractComponent>(
							mPlacementConfigXml
									.getMaxNumberOfSubcomponentsPerRay());

				rayComponents.add(subComponent);
				mComponentsOnRay.put(ray, rayComponents);
				mTree.setMarksForComponent(subComponent, mComponents);
			}
		}
		return addMoreSubComponents(ray);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet die Anzahl von Segmenten, die fuer eine zylindrische
	 * Komponente gesetzt werden. Dadurch skaliert die Segmentanzahl als
	 * Funktion des Radius. Der Radius ergibt sich dabei aus der Haelfte der
	 * kleineren Komponentenausdehnungen.
	 * 
	 * @param width
	 *            Breite der Komponente
	 * @param height
	 *            Hoehe der Subkomponente
	 * @return Anzahl der Kreissegmente, die erzeugt werden muessen, damit die
	 *         maximale Sehnenlaenge nicht ueberschritten wird
	 */
	private int getNumberOfCylindricSegments(Float width, Float height) {

		float radius;
		if (width < height)
			radius = width;
		else
			radius = height;
		radius /= 2.0f;

		// double targetKreissegmentLength = radius * 0.3f;

		// Ziellaenge des Kreisbogens / Radius des Kreises => Winkel der
		// einzelnen Segmente
		double angle = mMaxKreissegmentLength / radius;
		double fullCircleAngle = Math.PI * 2.0d;
		double numberOfSegments = fullCircleAngle / angle;

		int minNumberOfSegments = 10;

		int resultNumber = (int) Math.round(numberOfSegments);
		if (resultNumber < minNumberOfSegments)
			resultNumber = minNumberOfSegments;

		logger.debug("Radius: " + radius + " #Segments:" + resultNumber);
		return resultNumber;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet den Punkt, an dem Subkomponenten positioniert werden.
	 * Hierfuer verwendet man die Informationen der ListAllocation-Instanz und
	 * sucht den Mittelpunkt des freien Bereichs.
	 * 
	 * @param nodes
	 *            Liste mit allen QuadtreeNode innerhalb derer eine Position
	 *            bestimmt werden soll
	 * @param region
	 *            ListAllocation-Instanz mit Informationen ueber Regionen freier
	 *            Bloecke
	 * @return Position innerhalb der Allocation-Region, an der die
	 *         Subkomponente positioniert wird
	 */
	private MyVector3f getPositionForSubcomponent(List<QuadtreeNode> nodes,
			ListAllocation region) {

		// ist die Anzahl freier Bloecke in der Region gerade, muss man den
		// Mittelpunkt zwischen zwei Bloecken berechnen
		Integer numberOfFreeBlocks = region.getNumberOfFreeBlocks();

		// sollen die Komponenten zentriert innerhalb der AllocationRegion
		// positioniert werden?
		if (mMathHelper.decide(mPlacementConfigXml.getProbPositionOnCenter())) {
			if (numberOfFreeBlocks % 2 == 0) {

				Integer secondIndex = numberOfFreeBlocks / 2
						+ region.getStartIndex();
				Integer startIndex = secondIndex - 1;

				QuadtreeNode firstNode = nodes.get(startIndex);
				QuadtreeNode secondNode = nodes.get(secondIndex);

				MyVector3f center2Center = new MyVector3f();
				center2Center
						.sub(secondNode.getCenter(), firstNode.getCenter());

				// Mitte zwischen den Center-Punkten
				center2Center.scale(0.5f);
				MyVector3f result = new MyVector3f();
				result.add(firstNode.getCenter(), center2Center);
				return result;

			}
			// sonst gebe den Mittelpunkt des mittleren Blocks zurueck
			else {
				Integer index = numberOfFreeBlocks / 2 + region.getStartIndex();
				return nodes.get(index).getCenter();
			}
		}
		// wenn nicht, dann waehle einen Block zufaellig aus der Region aus
		else {
			Integer index = mRandom.nextInt(numberOfFreeBlocks);
			// Offset hinzuaddieren
			index += region.getStartIndex();
			return nodes.get(index).getCenter();
		}

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode prueft Abbruchkriterien fuer die weitere Positionierung von
	 * Subkomponenten auf dem uebergebenen Strahl
	 * 
	 * @param ray
	 *            Strahl, fuer den geprueft wird, ob weitere Komponenten gesetzt
	 *            werden sollen
	 * @return True, sofern weitere Komponenten gesetzt werden sollen, False
	 *         sonst
	 */
	private boolean addMoreSubComponents(Ray ray) {

		// lese die Anzahl bereits positionierter Komponenten aus der
		// Verwaltungsstruktur
		if (mComponentsOnRay.containsKey(ray)) {
			Integer numberOfPositionedSubComponents = mComponentsOnRay.get(ray)
					.size();

			// Maximalanzahl erreicht?
			if (numberOfPositionedSubComponents >= mPlacementConfigXml
					.getMaxNumberOfSubcomponentsPerRay())
				return false;
		}

		// Maximalanzahl erneuter Versuche fuer den Uebergabestrahl
		// ueberschritten?
		Integer numberOfRetries = mRetriesPerRay.get(ray);
		if (numberOfRetries >= mPlacementConfigXml
				.getMaxNumberOfRetriesPerRay()) {
			logger.info("ABBRUCH: Maximalanzahl an Retries erreicht!");
			return false;
		}

		// berechne eine aktualsiierte Allocation-Struktur
		// hole die Nachbarn des Strahls
		/*
		 * List<QuadtreeNode> neighbourNodes = mTree
		 * .getNeighbourNodesParallelToRay(ray);
		 */
		List<QuadtreeNode> neighbourNodes = mTree.getNodesForRay(ray);
		assert neighbourNodes.size() > 0 : "FEHLER: Es konnten keine Quadtreeknoten ermittelt werden, die parallel zum uebergebenen Strahl verlaufen";

		List<ListAllocation> allocationInformation = getAllocationForNodes(neighbourNodes);

		// wenn die Rueckgabeliste leer ist, so wurde kein freier Block mehr auf
		// der Kante gefunden, breche ab
		if (allocationInformation.size() == 0) {
			logger.info("ABBRUCH: Keine freien Knoten gefunden!");
			return false;
		}

		// minimale Anzahl freier Bloecke unterschritten?
		if (allocationInformation.get(allocationInformation.size() - 1)
				.getNumberOfFreeBlocks() < mPlacementConfigXml
				.getMinNumberOfFreeBlocks()) {
			logger.info("ABBRUCH: Minimalanzahl freier Bloecke unterschritten");
			return false;
		}

		// alles in Ordnung, weitermachen
		return true;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erzeugt eine Liste von List-Allocation-Instanzen, die aufsteigend
	 * nach der Anzahl der freien Bloecke sortiert zurueckgeben werden
	 * 
	 * @param nodes
	 *            Liste mit aufeinanderfolgenden QuadtreeNodes
	 * @param Sortierte
	 *            Liste von ListAllocation-Instanzen, die die aktuelle Belegung
	 *            der uebergebenen Nodes beschreiben
	 */
	private List<ListAllocation> getAllocationForNodes(List<QuadtreeNode> nodes) {

		// durchlaufe die Liste sequentiell, sobald ein Block gefunden wird, der
		// nicht frei ist (ContentType.FULL), erzeuge eine
		// List-Allocation-Instanz
		Iterator<QuadtreeNode> nodeIter = nodes.iterator();
		QuadtreeNode currentNode = null;

		List<ListAllocation> result = new ArrayList<ListAllocation>(
				nodes.size());
		ListAllocation currentAllocation = null;
		Integer currentCount = null;

		while (nodeIter.hasNext()) {

			currentNode = nodeIter.next();

			// wenn der aktuelle Knoten noch nicht voll ist
			if (!currentNode.getContentType().equals(ContentType.FULL)) {

				// wird aktuell keine Allocation aktualisiert, erzeuge eine neue
				if (currentAllocation == null) {
					currentAllocation = new ListAllocation(
							nodes.indexOf(currentNode));

					// zaehle den Startknoten immer mit
					currentCount = 1;
				}

				// es wird eine Allocation verwaltet, inkrementiere nur den
				// Counter
				else
					currentCount++;
			}
			// Knoten entdeckt, der nicht leer ist
			else {
				// wird eine Allocation verwaltet, schliesse diese ab
				if (currentAllocation != null) {
					currentAllocation.setNumberOfFreeBlocks(currentCount);
					result.add(currentAllocation);
					currentAllocation = null;
					currentCount = null;
				}
			}
		}

		// wenn die Iteration ueber alle Knoten abgeschlossen ist, fuege etwaige
		// offene Allokationen zur Liste hinzu
		if (currentAllocation != null) {
			currentAllocation.setNumberOfFreeBlocks(currentCount);
			result.add(currentAllocation);
		}

		// sortiere die Liste aufsteigend aufgrund der Anzahl freier Bloecke
		sortAllocationList(result);

		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * @author Patrick Gunia Klasse zur Speicherung von Informationen ueber
	 *         belegte Bloecke innerhalb einer List von Nodes
	 */
	private class ListAllocation {

		private Integer mStartIndex = null;

		private Integer mNumberOfFreeBlocks = null;

		// ------------------------------------------------------------------------------------------

		/**
		 * @param mStartIndex
		 * @param mNumberOfFreeBlocks
		 */
		public ListAllocation(Integer mStartIndex, Integer mNumberOfFreeBlocks) {
			super();
			this.mStartIndex = mStartIndex;
			this.mNumberOfFreeBlocks = mNumberOfFreeBlocks;
		}

		// ------------------------------------------------------------------------------------------
		/**
		 * @param mStartIndex
		 */
		public ListAllocation(Integer mStartIndex) {
			super();
			this.mStartIndex = mStartIndex;
		}

		// ------------------------------------------------------------------------------------------

		/**
		 * @return the mStartIndex
		 */
		public Integer getStartIndex() {
			return mStartIndex;
		}

		// ------------------------------------------------------------------------------------------

		/**
		 * @param mStartIndex
		 *            the mStartIndex to set
		 */
		public void setStartIndex(Integer mStartIndex) {
			this.mStartIndex = mStartIndex;
		}

		// ------------------------------------------------------------------------------------------

		/**
		 * @return the mNumberOfFreeBlocks
		 */
		public Integer getNumberOfFreeBlocks() {
			return mNumberOfFreeBlocks;
		}

		// ------------------------------------------------------------------------------------------

		/**
		 * @param mNumberOfFreeBlocks
		 *            the mNumberOfFreeBlocks to set
		 */
		public void setNumberOfFreeBlocks(Integer mNumberOfFreeBlocks) {
			this.mNumberOfFreeBlocks = mNumberOfFreeBlocks;
		}

		// ------------------------------------------------------------------------------------------

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "ListAllocation [mStartIndex=" + mStartIndex
					+ ", mNumberOfFreeBlocks=" + mNumberOfFreeBlocks + "]";
		}

		// ------------------------------------------------------------------------------------------

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode sortiert die uebergebene Liste mit Nodes aufsteigend aufgrund der
	 * Anzahl freier, aufeinanderfolgender Bloecke
	 * 
	 * @param allocationNodes
	 *            Liste mit Allocation-Instanzen
	 */
	private void sortAllocationList(List<ListAllocation> allocationNodes) {
		Collections.sort(allocationNodes, new Comparator<ListAllocation>() {
			@Override
			public int compare(ListAllocation o1, ListAllocation o2) {
				return o1.getNumberOfFreeBlocks().compareTo(
						o2.getNumberOfFreeBlocks());
			}
		});
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet die maximale Breite, die eine Subkomponente haben darf,
	 * die innerhalb der uebergebenen Allocation-Region an der uebergebenen
	 * Position positioniert werden soll. Die Berechnung bestimmt die Abstaende
	 * der Uebergabeposition vom Anfangs- und Endknoten der AllocationRegion und
	 * gibt den jeweils kleineren Abstand zurueck
	 * 
	 * @param position
	 *            Position, auf der die Subkomponente zentriert werden soll
	 * @param region
	 *            ListAllocation-Instanz, die die uebergebene Position enthaelt
	 * @param nodes
	 *            QuadtreeNodes, von der ein Teil durch die
	 *            ListAllocation-Struktur beschrieben wird
	 * @return Maximale Breite, die eine Komponente innerhalb der Region haben
	 *         darf, sofern sie an der Zielposition positioniert werden soll
	 */
	private Float getMaxWidthWithinAllocationRegion(final MyVector3f position,
			final ListAllocation region, final List<QuadtreeNode> nodes) {

		// bestimme die Abstaende der gewaehlten Position von den Mittelpunkten
		// des ersten und letzten Nodes
		QuadtreeNode regionBeginNode = nodes.get(region.getStartIndex());
		QuadtreeNode regionEndNode = nodes.get(region.getStartIndex()
				+ region.getNumberOfFreeBlocks() - 1);

		assert regionBeginNode != null && regionEndNode != null : "FEHLER: RegionBeginNode: "
				+ regionBeginNode + " RegionEndNode: " + regionEndNode;

		float distanceBegin = mMathHelper.calculatePointPointDistance(position,
				regionBeginNode.getCenter());
		float distanceEnd = mMathHelper.calculatePointPointDistance(position,
				regionEndNode.getCenter());

		if (distanceBegin < distanceEnd)
			return distanceBegin;
		else
			return distanceEnd;

	}
	// ------------------------------------------------------------------------------------------

}
