package semantic.building.modeler.prototype.graphics.complex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import processing.core.PApplet;
import semantic.building.modeler.configurationservice.model.enums.FloorPosition;
import semantic.building.modeler.configurationservice.model.enums.Side;
import semantic.building.modeler.math.Axis;
import semantic.building.modeler.math.MyPolygon;
import semantic.building.modeler.math.MyVector2f;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.MyVectormath;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.prototype.enums.QuadType;
import semantic.building.modeler.prototype.graphics.primitives.AbstractQuad;
import semantic.building.modeler.prototype.graphics.primitives.PolygonalQuad;
import semantic.building.modeler.prototype.roof.configuration.FixedRoofWeightConfiguration;
import semantic.building.modeler.prototype.service.EdgeManager;
import semantic.building.modeler.prototype.service.PrototypeHelper;
import semantic.building.modeler.prototype.service.Texture;
import semantic.building.modeler.prototype.service.TextureManagement;
import semantic.building.modeler.prototype.service.TextureManagement.TextureCategory;
import semantic.building.modeler.weightedstraightskeleton.algorithm.SkeletonRoofDescriptor;

/**
 * 
 * @author Patrick Gunia Klasse dient der Verwaltung von Gebaeuden. Diese
 *         bestehen aus einer beliebigen Anzahl einzelner Stockwerke, die selber
 *         wiederum komplexe Objekte sind. Aus diesem Grund ist BuildingComplex
 *         als Subklasse von CompositeComplex realisiert.
 */

public class BuildingComplex extends CompositeComplex {

	/**
	 * Map speichert fuer PolyQuads, die fuer die durchgaengige Texturierung
	 * berechnet wurden, die zugehoerigen Quellquads, um Texturkoordinaten
	 * uebertragen zu koennen
	 */
	private Map<AbstractQuad, List<AbstractQuad>> mPolyToSrcQuadMap = null;

	// ------------------------------------------------------------------------------------------

	public BuildingComplex(PApplet parent) {
		super(parent);

	}

	// ------------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "building";
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet einen gebaeude-globalen Vertexbuffer fuer das gesamte
	 * Gebaeude und speichert diesen Buffer anschliessend als Referenz in allen
	 * Subkomponenten. Diese muessen anschliessend all ihre Subkomponenten
	 * aktualisieren, bsw. Update der Triangles, inkl. EdgeManager etc. Die
	 * Methode aktualisiert immer nur die letzte hinzugefuegte Komponente, da
	 * deren Vertices keine Auswirkung auf die bereits vorhandenen Strukturen
	 * haben (werden immer hinten an den Buffer angefuegt)
	 */
	private void computeVertexBufferForBuilding(
			final AbstractComplex newComponent) {

		final List<Vertex3d> vertexBuffer = new ArrayList<Vertex3d>(
				mVertices.size() + newComponent.getVertices().size());

		// alle alten Vertices adden
		vertexBuffer.addAll(mVertices);
		mVertices.clear();
		mVertices = null;

		final List<Vertex3d> newComponentVertices = newComponent.getVertices();

		// fuege nun alle Vertices der neuen Komponente zum Buffer hinzu,
		// vermeide Duplikate
		for (Vertex3d currentVertex : newComponentVertices) {
			if (!vertexBuffer.contains(currentVertex)) {
				vertexBuffer.add(currentVertex);
			}
		}

		newComponent.updateIndicesForNewVertexBuffer(vertexBuffer);

		// aktualisiere die Vertexbuffer in allen Subkomponenten
		for (AbstractComplex curComplex : mComponents) {
			curComplex.setVertices(vertexBuffer);
		}
		mVertices = vertexBuffer;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet fuer saemtliche Eingabequadsd eines Gebauedes eine
	 * Menge polygonaler Quads, die fortlaufende Flaechen des Gebaeudes
	 * abbilden. Zu diesem Zweck werden zunaechst alle Quads aufgrund ihrer
	 * Ausrichtung in "Eimer" geworfen, anschliessend wird jeder Eimer aufgrund
	 * der y-Koordinaten der Quad-Mittelpunkte sortiert, bevor dann fuer jeweils
	 * aufeinanderfolgende Quads ein polygonales Quad bestimmt wird.
	 */
	public void computePolyQuadsForContiguousBaseQuads() {

		mPolyToSrcQuadMap = new HashMap<AbstractQuad, List<AbstractQuad>>();

		final List<AbstractQuad> allQuads = new ArrayList<AbstractQuad>();	
		for(FloorComplex current : getFloors()) {
			allQuads.addAll(current.getOutdoorQuads());
		}
					
		// erzeuge Eimerchen basierend auf den Quadausrichtungen
		final Map<MyVector3f, List<AbstractQuad>> bucketMap = computeQuadBuckets(allQuads);
		
		// sortiere die Eimerchen basierend auf den y-Koordinaten der
		// Quad-Mittelpunkte
		for(MyVector3f currentAlignment : bucketMap.keySet()) {
	
			final List<AbstractQuad> currentBucket  = bucketMap.get(currentAlignment);

			// sortiere die Quads im Bucket basierend auf ihren y-Koordinaten
			sortBucketByYCoordinate(currentBucket);

			// erstelle PolyQuads fuer jeden Bucket
			mOutdoorQuads.addAll(computePolyQuadsForBuckets(currentBucket));
		}

		// fuehre die Updateberechnungen fuer alle berechneten PolyQuads durch
		for(AbstractQuad current : mOutdoorQuads) {
			current.update();
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet eine Map, die Listen von Quads basierend auf ihren
	 * Ausrichtungen erstellt. Alle Quads mit einer bestimmten
	 * Normalenausrichtung werden so in eine Liste gepackt und zur HashMap
	 * hinzugefuegt.
	 * 
	 * @param quads
	 *            Ausgangsquads, fuer die eine Zuordnungsmap erstellt werden
	 *            soll
	 * @return Map mit Listen, die aufgrund der Ausrichtung ihrer Quads erstellt
	 *         werden
	 */
	private Map<MyVector3f, List<AbstractQuad>> computeQuadBuckets(
			final List<AbstractQuad> quads) {

		
		final Map<MyVector3f, List<AbstractQuad>> resultMap = new HashMap<MyVector3f, List<AbstractQuad>>();
		
		for(AbstractQuad currentQuad : quads) {

			// Berechnung basiert darauf, dass alle Quads genau 4 Ecken besitzen
			// Quads, die nicht dieser Vorgabe entsprechen, werden direkt zum
			// Quad-Buffer geadded
			if (!(currentQuad instanceof semantic.building.modeler.prototype.graphics.primitives.Quad)) {
				mOutdoorQuads.add(currentQuad);
				continue;
			}

			final MyVector3f currentNormal = currentQuad.getNormal();

			if (resultMap.containsKey(currentNormal)) {
				resultMap.get(currentNormal).add(currentQuad);
			} else {
				List<AbstractQuad> quadList = new ArrayList<AbstractQuad>();
				quadList.add(currentQuad);
				resultMap.put(currentNormal, quadList);
			}
		}

		return resultMap;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode sortiert alle Quads innerhalb des uebergebenen Buckets basierend
	 * auf dem y-Wert ihres Mittelpunktes. Ergebnis ist eine aufsteigend
	 * sortierte Liste, bei der aufgrund der y-Achsenausrichtung bei Processing
	 * Quads mit "hoeherem" Mittelpunkt am Anfang stehen (= hoeherer Mittelpunkt
	 * <=> kleinere y-Koordinate)
	 * 
	 * @param bucket
	 *            Liste mit Quads mit gleicher Ausrichtung innerhalb eines
	 *            Stockwerks
	 * @return Sortierte Liste
	 */
	private List<AbstractQuad> sortBucketByYCoordinate(final List<AbstractQuad> bucket) {
		// sortiere anhand der y-koordinate
		Collections.sort(bucket, new Comparator<AbstractQuad>() {
			public int compare(AbstractQuad o1, AbstractQuad o2) {

				final Float o1Float = o1.getCenterPtr().y;
				final Float o2Float = o2.getCenterPtr().y;
				return o1Float.compareTo(o2Float);
			}
		});
		return bucket;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet PolyQuads fuer adjazente Quads innerhalb eines Eimers
	 * 
	 * @param bucket
	 *            Eimer mit einer Menge von Quads, fuer die PolyQuads berechnet
	 *            werden sollen
	 * @return Liste mit PolyQuads fuer die Eingabequads
	 */
	private List<AbstractQuad> computePolyQuadsForBuckets(
			final List<AbstractQuad> bucket) {

		final List<AbstractQuad> result = new ArrayList<AbstractQuad>(bucket.size());
		AbstractQuad currentQuad = null;

		final Set<Integer> indices = new HashSet<Integer>(bucket.size() * 4);
		List<AbstractQuad> connectedQuads = new ArrayList<AbstractQuad>(
				bucket.size());

		int numberOfIndicesBeforeInsertion = -1, numberOfIndicesAfterInsertion = -1;
		Integer[] currentIndices = null;

		for (int i = 0; i < bucket.size(); i++) {
			currentQuad = bucket.get(i);
			numberOfIndicesBeforeInsertion = indices.size();
			currentIndices = currentQuad.getIndices();

			// kopiere die Indices ins Set
			for (int k = 0; k < currentIndices.length; k++)
				indices.add(currentIndices[k]);
			numberOfIndicesAfterInsertion = indices.size();

			// falls sich die Anzahl der Indices um mehr als 2 geaendert hat, so
			// ist das aktuelle Quad nicht adjazent zu den bereits gespeicherten
			// die Logik geht dabei davon aus, dass alle entahltenen Quads genau
			// 4 Indices haben, sind sie adjanzent, so teilen sie sich 2
			// Vertices
			if ((numberOfIndicesAfterInsertion - numberOfIndicesBeforeInsertion) > 2
					&& numberOfIndicesBeforeInsertion > 0) {
				// erzeuge ein Polygonales Quad
				result.add(computePolyQuadForConnectedQuads(connectedQuads));
				indices.clear();
				connectedQuads = new ArrayList<AbstractQuad>(bucket.size());
				numberOfIndicesAfterInsertion = 0;
				numberOfIndicesBeforeInsertion = 0;
				connectedQuads.add(currentQuad);
			} else
				connectedQuads.add(currentQuad);
		}

		// erzeuge ein PolyQuad nach Abschluss der Iteration fuer alle noch
		// nicht verarbeiteten Quads
		result.add(computePolyQuadForConnectedQuads(connectedQuads));
		return result;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet ein PolyQuad fuer eine uebergebene Liste von
	 * verbundenen Quads, die sich gemeinsame Vertices teilen
	 * 
	 * @param connectedQuads
	 * @return
	 */
	private AbstractQuad computePolyQuadForConnectedQuads(
			final List<AbstractQuad> connectedQuads) {

		final List<Integer> leftSideIndexBuffer = new ArrayList<Integer>();
		final List<Integer> rightSideIndexBuffer = new ArrayList<Integer>();

		final List<AbstractQuad> holeBuffer = new ArrayList<AbstractQuad>();

		Integer[] quadIndices = null;
		AbstractQuad currentQuad = null;

		// durchlaufe die Quadas von unten nach oben
		for (int i = connectedQuads.size() - 1; i >= 0; i--) {

			currentQuad = connectedQuads.get(i);
			quadIndices = currentQuad.getIndices();

			// linke Kante => Indices 1 und 2
			addIndexToBuffer(quadIndices[1], leftSideIndexBuffer);
			addIndexToBuffer(quadIndices[2], leftSideIndexBuffer);

			// rechte Kante => Indices 3 und 0
			addIndexToBuffer(quadIndices[0], rightSideIndexBuffer);
			addIndexToBuffer(quadIndices[3], rightSideIndexBuffer);

			// wenn das aktuelle Quad Loecher enthaelt, fuege sie zum
			// Hole-Buffer hinzu
			if (currentQuad.hasHoles())
				holeBuffer.addAll(currentQuad.getHoles());
		}

		// erzeuge nun ein polygonales Quad basierend auf den Indices und den
		// Loechern
		final Integer[] indices = new Integer[rightSideIndexBuffer.size()
				+ leftSideIndexBuffer.size()];

		// linke Seite adden:
		for (int i = 0; i < leftSideIndexBuffer.size(); i++)
			indices[i] = leftSideIndexBuffer.get(i);

		// rechte Seite von oben nach unten adden
		int lastIndex = leftSideIndexBuffer.size();
		for (int i = rightSideIndexBuffer.size() - 1; i >= 0; i--) {
			indices[lastIndex] = rightSideIndexBuffer.get(i);
			lastIndex++;
		}

		// erzeuge ein PolygonalesQuad und adde es zum Quad-Buffer des Buildings
		final PolygonalQuad sideQuad = new PolygonalQuad();
		sideQuad.setComplexParent(this);
		sideQuad.setIndices(indices);
		sideQuad.setDirection(currentQuad.getDirection());
		sideQuad.setHoles(holeBuffer);

		// speichere das Polyquad und die Liste der Quellquads innerhalb der
		// Klsasenmap
		mPolyToSrcQuadMap.put(sideQuad, connectedQuads);

		return sideQuad;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode sortiert die uebergebene Liste bzgl. der zu den Indices
	 * gehoerenden Vertices anhand ihrer Werte in allen Koordinatenachsen
	 * 
	 * @param indices
	 *            Liste mit Integer-Werten, die Indices auf den Vertex-Buffer
	 *            des komplexen Objekts beschreiben
	 * @return Neu angeordnete Liste mit Indices basierend auf der Sortierung
	 */
	private List<Integer> sortListByCoordinates(List<Integer> indices,
			MyVector3f normal) {

		MyVectormath mathHelper = MyVectormath.getInstance();
		Iterator<Integer> indexIter = indices.iterator();

		Integer currentIndex = null;
		Vertex3d currentVertex = null;
		Map<Vertex3d, Integer> vertexToIndexMap = new HashMap<Vertex3d, Integer>(
				indices.size());
		List<Vertex3d> vertexBuffer = new ArrayList<Vertex3d>(indices.size());

		while (indexIter.hasNext()) {
			currentIndex = indexIter.next();
			currentVertex = getVertices().get(currentIndex).clone();
			// runde auf ganze Zahlen
			mathHelper.roundVector3f(currentVertex.getPositionPtr(), 1.0f);

			// Map wird verwendet, um nach der Sortierung die Zuordnung von
			// Vertices zu Indices wieder herstellen zu koennen
			vertexToIndexMap.put(currentVertex, currentIndex);
			vertexBuffer.add(currentVertex);
		}

		Axis ignorableAxis = mathHelper.getIgnorableAxis(normal, false);
		PrototypeHelper helper = PrototypeHelper.getInstance();

		// wenn es sich um die Z-Achse oder um die X-Achse handelt (FRONT oder
		// BACK bzw. LEFT oder RIGHT), sortiere nach der y-Koordinate
		// haengt mit dem Aufbau der Buffer zusammen => da immer eine
		// vollstaendige Kante des Quads durch die Indices beschrieben wird,
		// sollten sich die Indices bei Buildings auch nur in einer Koordinate
		// unterscheiden, was bei LEFT / RIGHT / FRONT / BACK-Ausrichtung
		// die y-Koordinate ist
		if (ignorableAxis.equals(Axis.Z) || ignorableAxis.equals(Axis.X))
			helper.sortVerticesByYCoordinate(vertexBuffer);
		else {
			// sortiere anhand der x-koordinate => spielt zunaechst allerdings
			// eine untergeordnete Rolle, da
			// Top- und Bottom keine polygonalen Quads bekommen
			helper.sortVerticesByXCoordinate(vertexBuffer);
		}

		// baue ueber die sortierte Liste den ueberarbeiteten Indexbuffer auf
		List<Integer> sortedIndices = new ArrayList<Integer>(indices.size());

		Iterator<Vertex3d> vertIter = vertexBuffer.iterator();
		while (vertIter.hasNext()) {
			currentVertex = vertIter.next();
			currentIndex = vertexToIndexMap.get(currentVertex);
			assert currentIndex != null : "FEHLER: Fuer das Vertex "
					+ currentVertex + " existiert kein Mapping!";
			sortedIndices.add(currentIndex);
		}

		return sortedIndices;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Utitility-Funktion fuegt den uebergebenen Index zum uebergebenen Buffer
	 * hinzu, sofern er dort nicht bereits vorkommt
	 * 
	 * @param index
	 *            Hinzuzufuegender Index
	 * @param buffer
	 *            Liste mit Indices, zu der der Index hinzugefuegt wird
	 */
	private void addIndexToBuffer(Integer index, List<Integer> buffer) {
		if (buffer.indexOf(index) == -1)
			buffer.add(index);
	}

	// ------------------------------------------------------------------------------------------
	@Override
	/**
	 * Methode fuegt ein komplexes Objekt zum Gebaeude hinzu und aktualisiert daraufhin den gebaeude-globalen Vertexbuffer
	 */
	public void addComplex(final AbstractComplex complex) {

		super.addComplex(complex);
		computeVertexBufferForBuilding(complex);

		// falls bereits eine Textur geadded wurde, berechne die Koorinaten neu
		// TEXTURKOORDINATEN WERDEN ERST GANZ AM ENDE BERECHNET
		/*
		 * if (mHasTextureApplied) { computeTextureCoordinates(); }
		 */

		// wenn es sich um ein Stockwerk handelt, teste, ob auch ein inneres
		// Stockwerk vorkommt
		if (complex instanceof FloorComplex) {
			FloorComplex floor = (FloorComplex) complex;

			// wenn es sich um ein Stockwerk handelt, kann dieses komplexe
			// Objekte als Komponenten enthalten, fuege auch diese
			// zum Gebeude hinzu => darin sind auch evtl. vorhandene
			// Innenbereiche vorhanden
			List<AbstractComplex> components = floor.getComponents();
			for (int i = 0; i < components.size(); i++) {
				addComplex(components.get(i));
			}
			floor.clearComponentBuffer();
		}

		else if (complex instanceof IndoorFloorComplex) {
			IndoorFloorComplex indoor = (IndoorFloorComplex) complex;
			// wenn es sich um ein Indoorcomplex handelt, kann dieses komplexe
			// Objekte als Komponenten enthalten, fuege auch diese
			// zum Gebeude hinzu
			List<AbstractComplex> components = indoor.getComponents();
			for (int i = 0; i < components.size(); i++) {
				addComplex(components.get(i));
			}
			indoor.clearComponentBuffer();
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erstellt ein Zwischengeschoss mit der uebergebenen Hoehe und dem
	 * uebergebenen Grundriss und added dieses zum aktuellen Gebauede
	 * 
	 * @param footprint
	 *            Grundriss des Zwischengeschosses
	 * @param height
	 *            Hoehe des Zwischengeschosses
	 * @param position
	 *            Position des Zwischengeschoss innerhalb des Gebaeudes
	 * @return Erzeugtes Zwischenstockwerk
	 */
	public AbstractComplex addIntermediateFloor(final MyPolygon footprint,
			final Float height, final MyVector3f position) {
		boolean isTop = false;
		IntermediateFloor intermediate = new IntermediateFloor(getParent(),
				footprint, height, getNormalToDirectionMap(),
				FloorPosition.INTERMEDIATE, getFloors().size(), isTop);
		intermediate.create();

		intermediate.translate(position);
		intermediate.update();

		addComplex(intermediate);
		return intermediate;
	}

	// ------------------------------------------------------------------------------------------

	@Override
	/**
	 * Buildings verhalten sich im Management ihrer Texturen vergleichbar zu "normalen" Complex-Objekten, sie fordern eine Kategorie an und
	 * verwalten im Gegensatz zu Composites ihre eigene Texturmap. Anschliessend reichen sie die geladene Textur an all ihre Subobjekte weiter.
	 * Dadurch ist garantiert, dass alle Objekte gleicher Art (bsw. Floors) auch die gleiche Textur assigned bekommen
	 * @param category Texturkategorie, aus der eine Textur angefordert wird
	 */
	public void setTextureByCategory(TextureCategory category) {

		LOGGER.trace("Demanding Texture for Category: " + category
				+ " for Complex: " + getID());

		// Texturflag setzen (wie bei Composite)
		mHasTextureApplied = true;

		// teste, ob fuer die uebergebene Kategorie bereits eine Textur
		// existiert
		Texture texture = mTextures.get(category.toString());

		// sofern bereits eine Textur existiert, gebe diese in allen Subobjekten
		// frei
		if (texture != null) {
			releaseTextureByTextureObject(texture);
		}

		// lade eine neue Textur fuer die uebergebene Kategorie
		texture = TextureManagement.getInstance().getTextureForCategory(
				category);

		// setze die gewaehlte Textur in allen Komponentenobjekten (diese
		// entscheiden dann, ob die Textur fuer sie geeignet ist)
		setTexture(category.toString(), texture);

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode berechnet Texturkoordinaten fuer vollstaendige Gebaeude basierend
	 * auf den Vertices der einzelnen Stockwerke. Zu diesem Zweck ermittelt man
	 * aus den Stockwerken zunaechst polygonale Quads, fuer die man die
	 * Koordinatenberechnung ausfuehrt. Anschliessend weisst man die berechneten
	 * Koordinaten wiederum den urspruenglichen Quads zu. Das Verfahren geht
	 * davon aus, dass die einzelnen Stockwerke aus einem Quellobjekt erzeugt
	 * wurden und somit die Quad-Indices in allen Floor-Complex-Instanzen gleich
	 * sind.
	 * 
	 */

	public Float computeTextureCoordinates() {

		LOGGER.trace("Berechne Texturkoordinaten fuer Gebaeude " + getID());
		Float scaleFactor = null;

		// berechne Texturkoordinaten => wenn noch keine Quads fuer die
		// Seitenflaechen des Gebaeudes extrahiert wurden,
		// verwende die Composite-Implementation, die die Calls an die
		// Subkomponenten weiterleitet
		if (mOutdoorQuads.size() == 0) {
			LOGGER.debug("Verwende Standardmethode zur Berechnung der Texturkoordinaten fuer Composite-Objekte");
			return super.computeTextureCoordinates();
		}

		// sonst berechne die Texturkoordinaten, wie bei einem
		// "Nicht-Composite"-Objekt ueber die extrahierten Seitenflaechen
		else {

			LOGGER.debug("Verwende spezielle Berechnungsmethode fuer Building-Instanzen");
			scaleFactor = super.computeTextureCoordinatesStandardWay();

			// dieser Ansatz berechnet nur die Koordinaten fuer das Gebaeude
			// selber => dabei werden aber bsw. Daecher oder importierte
			// Komponenten nicht neu berechnet => wurde ein Dach hinzugefuegt,
			// so wird der Vertex-Buffer aktualisiert, allerdings werden die
			// Dachtexturkoordinaten nicht neu berechnet => dadurch kommt es zu
			// Fehlern => aus diesem Grund muss man fuer
			// solche Objekte die Texturberechnung noch einmal gesondert
			// aktivieren
			for (AbstractComplex currentComplex : mComponents) {
				if (currentComplex instanceof semantic.building.modeler.prototype.graphics.complex.RoofComplex
						|| currentComplex instanceof semantic.building.modeler.prototype.graphics.complex.ImportedComplex) {
					currentComplex.computeTextureCoordinates();
				}
			}

			// scaleTextureCoordinatesStandardWay(scaleFactor);
			updateQuadTextureCoordinates();
		}

		// skaliere die gesetzten Koordinaten => kann bei Roofs zu Problemen
		// fuehren => erstmal abwarten
		scaleTextureCoordinates(scaleFactor);

		return scaleFactor;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode liefert alle Quads des uebergebenen Quadtyps. Sofern sich die
	 * Buffer fuer diesen Typ seit der letzten anfrage geaendet haben, werden
	 * diese aktualisiert
	 * 
	 * @param type
	 *            Typ der Quads, die geholt werden sollen
	 * @return Liste mit allen Quads aller Subkomponenten, die diesem Typ
	 *         entsprechen
	 */
	protected List<AbstractQuad> getQuads(QuadType type) {

		// bei Buildings werden Outdoorquads direkt zurueckgegeben, da diese aus
		// den Floor-Quads bestimmt werden
		if (type == QuadType.OUTDOOR)
			return mOutdoorQuads;

		// bei Buildings besteht "ALL" aus den Outdoor-Quads des Buildings und
		// den Indoor-Quads der Floors
		if (type == QuadType.ALL) {
			getQuads(QuadType.INDOOR);
			mAllQuads = new ArrayList<AbstractQuad>(mIndoorQuads.size()
					+ mOutdoorQuads.size());
			mAllQuads.addAll(mIndoorQuads);
			mAllQuads.addAll(mOutdoorQuads);
			return mAllQuads;
		}

		// liefert die Indoor-Quads der Floors
		if (type == QuadType.INDOOR) {
			List<FloorComplex> floors = getFloors();
			mIndoorQuads = new ArrayList<AbstractQuad>(mOutdoorQuads.size()
					* floors.size());
			for (int i = 0; i < floors.size(); i++) {
				mIndoorQuads.addAll(floors.get(i).getIndoorQuads());
			}
			return mIndoorQuads;
		}
		return null;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode uebertraegt die Texturindices, die fuer die gesamten
	 * Seitenflaechen ueber die polygonalen Quads berechnet wurden zurueck auf
	 * die zugehoerigen Quads in den komplexen Stockwerkinstanzen. Dadurch soll
	 * ein fluessiger Texturuebergang erreicht werden. Die Methode basiert auf
	 * der Annahme, dass die Indices der Quads sowohl im Building als auch in
	 * den Stockwerksinstanzen identisch sind.
	 */
	private void updateQuadTextureCoordinates() {
		LOGGER.info("Berechne Texturkoordinaten fuer: " + getID()
				+ " ANZAHL OUTDOOR-QUADS: " + mOutdoorQuads.size());
		AbstractComplex currentComplex = null;

		// Ansatz basiert auf der Annahme, dass die Quadindizierung beim Quell-
		// und den Zielquads identisch sind, also das polygonale Quad
		// des Buildings mit Index i Indices fuer alle Quads der Floors mit
		// Index i enthaelt
		for (AbstractQuad currentQuad : mOutdoorQuads) {

			// ueberspringe alle Boden und Decken-Quads, da es sich hierbei um
			// Referenzen auf die Originale handelt => somit sind diese bereits
			// mit Texturkoordinaten versehen
			if (currentQuad.getDirection().equals(Side.BOTTOM)
					|| currentQuad.getDirection().equals(Side.TOP)) {
				continue;
			}

			for (int j = 0; j < mComponents.size(); j++) {
				currentComplex = mComponents.get(j);

				// setze das Flag in den komplexen Subobjekten, um anzuzeigen,
				// dass die Koordinaten noch nicht skaliert wurden
				currentComplex.setIsScaledTextureCoord(false);

				// und das Flag, das anzeigt, dass ueberhaupt schon Koordinaten
				// vorhanden sind
				currentComplex.setHasTextureCoordinates(true);

				if (!(currentComplex instanceof semantic.building.modeler.prototype.graphics.complex.FloorComplex)) {
					continue;
				}

				// sonst nehme das i-te-Quad der aktuellen Komponente und
				// uebertrage die Koordinaten
				else {

					final List<AbstractQuad> connectedQuads = mPolyToSrcQuadMap
							.get(currentQuad);
					assert connectedQuads != null && connectedQuads.size() > 0 : "FEHLER: Fuer das aktuelle PolyQuad wurde keine Liste mit Source-Quads gefunden";

					// uebertrage die Texturkoordinaten vom Polyquad auf die
					// Komponentenquads
					for (int k = 0; k < connectedQuads.size(); k++) {
						updateTextureCoordinatesForSingleQuad(currentQuad,
								connectedQuads.get(k));
					}
				}
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode uebertraegt die Texturkoordinaten vom Quell- auf das Zielquad.
	 * Setzt voraus, dass dest Teil des Source-Quads ist, also saemtliche
	 * Indices von dest ebenfalls in source vorkommen. Darum muss vorher
	 * zwingend eine Texturberechnung auf
	 * 
	 * @param source
	 *            Polygonales Quad, von dem aus die Texturkoordinaten
	 *            uebertragen werden
	 * @param dest
	 *            Zielquad, auf das die Koordinaten uebertragen werden (anhand
	 *            der Vertexindices)
	 */
	private void updateTextureCoordinatesForSingleQuad(AbstractQuad source,
			AbstractQuad dest) {

		// assert source.getDirection().equals(dest.getDirection()):
		// "FEHLER: Quell- und Zielquad unterscheiden sich in ihren Ausrichtungen! Source: "
		// + source.getDirection() + " Dest: " + dest.getDirection();

		Map<Integer, MyVector2f> sourceCoords = source.getTextureCoords();
		List<Vertex3d> verts = source.getVertices();

		/*
		 * logger.info("SOURCE INDOOR: " + source.isIndoor() + " DEST INDOOR: "
		 * + dest.isIndoor() + " SOURCE-TEXTURE-COORDS: " +
		 * sourceCoords.size());
		 * 
		 * 
		 * System.out.println("SOURCE INDICES: "); Set<Integer> keys =
		 * sourceCoords.keySet(); logger.info("ANZAHL KEYS: " + keys.size());
		 * Iterator<Integer> keyIter = keys.iterator(); Integer currentKey =
		 * null; while(keyIter.hasNext()) { currentKey = keyIter.next();
		 * System.out.println("KEY: " + currentKey + " VALUE: " +
		 * sourceCoords.get(currentKey)); }
		 */
		// hole alle Indices, auch diejenigen der Triangles und Loecher
		List<Integer> destIndices = new ArrayList<Integer>(dest.getAllIndices());

		/*
		 * System.out.println("DEST-TYPE: " +dest.getType() + " RICHTUNG: " +
		 * dest.getDirection() + " SOURCE-DIRECTION: " + source.getDirection());
		 * for(int i = 0; i < destIndices.size(); i++) System.out.println(i +
		 * ": KEY: " + destIndices.get(i));
		 */

		Map<Integer, MyVector2f> newCoords = new HashMap<Integer, MyVector2f>(
				destIndices.size());
		MyVector2f currentCoords = null;

		// verwende die Vertexindices des Zielquads, um auf die Vektoren
		// zuzugreifen => das funktioniert wiederum NUR dann,
		// wenn vorher ein gemeinsamer Vertexbuffer fuer alle Komponenten im
		// Building erzeugt wurde => das sollte aber immer der Fall sein!
		for (int i = 0; i < destIndices.size(); i++) {
			currentCoords = sourceCoords.get(destIndices.get(i));

			// System.out.println("VERTEX-INDEX: " + destIndices.get(i) +
			// " NEUE INDICES: " + currentCoords + " ALTE INDICES: " +
			// dest.getTextureCoords().get(destIndices.get(i)) + " VERTEX: " +
			// getVertices().get(destIndices.get(i)));
			assert currentCoords != null : "FEHLER: Fuer Index "
					+ destIndices.get(i)
					+ " existieren keine Texturkoordinaten im Source-Quad: "
					+ source.getDirection() + " Normal: " + source.getNormal()
					+ " Vertex: " + verts.get(destIndices.get(i))
					+ " Dest-Direction: " + dest.getDirection()
					+ " Dest-Normal: " + dest.getNormal();
			newCoords.put(destIndices.get(i), currentCoords.clone());
		}

		dest.setTextureCoords(newCoords);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Utility-Methode: Liefert alle Floors eines Gebauedes
	 * 
	 * @return Liste mit allen Floor-Objekten des aktuellen Gebaeudes
	 */
	public List<FloorComplex> getFloors() {

		List<FloorComplex> floors = new ArrayList<FloorComplex>(
				mComponents.size());
		AbstractComplex currentComplex = null;
		for (int i = 0; i < mComponents.size(); i++) {
			currentComplex = mComponents.get(i);
			if (currentComplex instanceof semantic.building.modeler.prototype.graphics.complex.FloorComplex)
				floors.add((FloorComplex) currentComplex);

		}
		return floors;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode liefert eine Liste mit Floors, die die uebergebene Position
	 * besitzen
	 * 
	 * @param floorPosition
	 *            Position der gesuchten Stockwerke innerhalb des Gebaeudes
	 * @return Liste mit allen Stockwerken, die die gesuchte Position besitzen
	 */
	public List<FloorComplex> getFloorsByPosition(
			final FloorPosition floorPosition) {

		final List<FloorComplex> allFloors = getFloors();
		if (floorPosition.equals(FloorPosition.ALL)) {
			return allFloors;
		} else {
			final List<FloorComplex> result = new ArrayList<FloorComplex>(
					allFloors.size());
			FloorComplex currentFloor = null;

			for (int i = 0; i < allFloors.size(); i++) {
				currentFloor = allFloors.get(i);
				if (currentFloor.getFloorPosition().equals(floorPosition)) {
					result.add(currentFloor);
				}
			}

			return result;
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode durchlaeuft alle TOP-Floors des Gebauedes und fuegt fuer jeden
	 * solchen Floor die Outline des Top-Quads zur Footprint-Liste hinzu.
	 * 
	 * @param useConvexHull
	 *            Wird in dieser Implementation nicht verwendet
	 * @return Footprints als Liste von Vertex3d-Strukturen
	 */
	public List<List<Vertex3d>> computeFootprints(Boolean useConvexHull) {
		List<FloorComplex> topFloors = null;

		// wenn mehr als ein Stockwerk vorhanden ist, dann gibt es auch ein
		// Top-Stockwerk
		if (getFloors().size() > 1)
			topFloors = getFloorsByPosition(FloorPosition.TOP);
		// sonst verwende das Ground-Stockwerk
		else
			topFloors = getFloorsByPosition(FloorPosition.GROUND);

		LOGGER.debug("Anzahl verwendbarer Stockwerke: " + topFloors.size());

		List<List<Vertex3d>> footprints = new ArrayList<List<Vertex3d>>(
				topFloors.size());

		for (int i = 0; i < topFloors.size(); i++) {
			List<AbstractQuad> topQuads = topFloors.get(i)
					.getAllOutsideQuadsWithDirection(Side.TOP);
			assert topQuads.size() == 1 : "FEHLER: Das aktuelle Stockwerk "
					+ topFloors.get(i).getID() + " enthaelt " + topQuads.size()
					+ " Quads mit Ausrichtung TOP";

			for (int j = 0; j < topQuads.size(); j++)
				footprints.add(topQuads.get(j).getQuadVertices());
		}
		LOGGER.debug("Insgesamt wurden " + footprints.size()
				+ " Footprints extrahiert");

		return footprints;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode liefert die Anzahl der Vertices innerhalb des Gebauedes => muss
	 * bei Buildings ueberschrieben werden, da diese im Gegensatz zu
	 * Standard-Composites einen geteilten Vertexbuffer pflegen, in den alle
	 * Vertices eingefuegt werden.
	 * 
	 * @return Anzahl der Vertices des Objekts
	 */
	public int getVertexCount() {
		// logger.info("Komponente: " + getID() + " VertexCount: " +
		// mVertices.size());
		return mVertices.size();
	}

	// ------------------------------------------------------------------------------------------
	@Override
	/**
	 * Methode berechnet Gewichte fuer die Kanten, die durch den uebergebenen Vertex-Vector beschrieben werden. Bei Building-Objekten muss man 
	 * im Gegensatz zu Composites nicht alle Komponenten durchlaufen und ihre EdgeManager einsammeln, sondern man beschraenkt sich auf den Top-Floor 
	 * @param footprint Liste mit Vertex3d-Instanzen, die im Uhrzeigersinn definiert sind und die Kanten des Grundrisses festlegen
	 * @return Liste mit Float-Gewichten
	 */
	protected List<Float> computeWeights(final List<Vertex3d> footprint,
			final FixedRoofWeightConfiguration weightManager) {
		List<Float> weights = new ArrayList<Float>();
		List<FloorComplex> floors = getFloors();

		assert floors.size() > 0 : "FEHLER: Es wurden " + floors.size()
				+ " Floors gefunden!";

		// der oberste Floor ist immer der zuletzt hinzugefuegte
		FloorComplex topFloor = floors.get(floors.size() - 1);
		LOGGER.debug("TOPFLOOR AUSRICHTUNG: " + topFloor.getFloorPosition());
		List<EdgeManager> topFloorEdgeManager = topFloor.getEdgeManager();

		assert topFloorEdgeManager.size() == 1 : "FEHLER: Es wurden "
				+ topFloorEdgeManager.size()
				+ " EdgeManager fuer den aktuellen Top-Floor gefunden";
		EdgeManager edgeManager = topFloorEdgeManager.get(0);
		weightManager.setCurrentEdgeManager(edgeManager);

		Vertex3d currentVert = null, currentNeighbour = null;
		String index = null;
		Float weight = null;

		for (int i = 0; i < footprint.size(); i++) {
			currentVert = footprint.get(i);

			if ((i + 1) < footprint.size())
				currentNeighbour = footprint.get(i + 1);
			else
				currentNeighbour = footprint.get(0);

			index = edgeManager.getEdgeIndexByVertices(currentVert,
					currentNeighbour);
			assert index != null : "FEHLER: Fuer die Vertices "
					+ currentVert.getPositionPtr() + " und "
					+ currentNeighbour.getPositionPtr()
					+ " existiert keine Kante im EdgeManager des Top-Floors "
					+ edgeManager.toString();

			weight = weightManager.getWeightByEdgeIndex(index);
			weights.add(weight);
		}
		return weights;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erzeugt ein Dach fuer das aktuelle Gebauede basierend auf den
	 * Vorgaben des erzeugten Weight-Managers
	 * 
	 * @param weightManager
	 *            Konfiguration fuer Gewichtszuweisung zu Haupt- und
	 *            Seitenflaechen des Dachs
	 * @param roofScaling
	 *            Skalierungsfaktor fuer den uebergebenen Grundriss, ermoeglicht
	 *            die nachtraegliche Reskalierung des Dachgrundrisses, um bsw.
	 *            ueberragende Daecher zu bauen
	 */
	public void computeRoofOLD(FixedRoofWeightConfiguration weightManager,
			Float roofScaling) {
		// hole den Footprint des TOP-Quads des obersten Floors
		List<FloorComplex> topFloors = getFloorsByPosition(FloorPosition.TOP);
		assert topFloors.size() == 1 : "FEHLER: Es wurden " + topFloors.size()
				+ " Stockwerke mit Position 'TOP' gefunden.";

		// hole das Top-Quad des Top-Floors
		AbstractQuad top = topFloors.get(0).getQuadByDirection(Side.TOP);
		assert top != null : "FEHLER: Es wurde kein Quad mit Ausrichtung 'TOP' gefunden.";

		List<Vertex3d> quadVerts = top.getQuadVertices();

		List<Float> edgeWeights = computeWeights(quadVerts, weightManager);
		for (int i = 0; i < edgeWeights.size(); i++)
			LOGGER.debug("Edge Weight: " + i + ": " + edgeWeights.get(i));

		// wenn der Dachgrundriss anders skaliert werden soll, skaliere die
		// Vertices ueber Polygonstrukturen
		if (roofScaling != 1.0f) {

			// erstelle Kopien, um die eigentlichen Grundrisse nicht zu
			// veraendern
			List<Vertex3d> footprintClones = new ArrayList<Vertex3d>(
					quadVerts.size());
			for (int i = 0; i < quadVerts.size(); i++)
				footprintClones.add(quadVerts.get(i).clone());
			MyPolygon scaledFootprint = new MyPolygon(footprintClones);
			scaledFootprint.scale(roofScaling);
			quadVerts = new ArrayList<Vertex3d>(footprintClones.size());
			quadVerts.addAll(scaledFootprint.getVertices());
		}

		Texture texture = null;
		Map<String, Texture> textures = getTextureMap();

		// sofern bereits eine Textur fuer das Dach geladen wurde, dekrementiere
		// die Anzahl der Verweise
		if (textures.containsKey("Roof")) {
			texture = textures.get("Roof");
			texture.removeReference();
			textures.remove("Roof");
		}

		// lade eine Dachtextur und uebergebe sie an die Dachberechnung
		// Wandtexturen werden nicht nach unten durchgereicht, da sie auf der
		// Ebene des Composite-Objekts verwaltet werden
		texture = TextureManagement.getInstance().getTextureForCategory(
				TextureCategory.Roof);
		textures.put("Roof", texture);

		String lineSeparator = System.getProperty("line.separator");
		String message = "";
		Vertex3d currentVertex = null;

		SkeletonRoofDescriptor roofConfig = new SkeletonRoofDescriptor();
		roofConfig.setVertices(quadVerts);
		roofConfig.setTexture(texture);
		roofConfig.setEdgeWeights(edgeWeights);

		boolean producedRoof = produceRoof(roofConfig);

		if (!producedRoof) {

			// Dump der Quad-Verts
			for (int i = 0; i < quadVerts.size(); i++) {
				currentVertex = quadVerts.get(i);
				message += "mVertices.add(new Vertex3d(" + currentVertex.getX()
						+ "f, " + currentVertex.getY() + "f, "
						+ currentVertex.getZ() + "f));" + lineSeparator;
			}
			LOGGER.error(message);

			// Dump der Gewichte
			message = "";
			for (int i = 0; i < edgeWeights.size(); i++) {
				message += "weights.add(" + edgeWeights.get(i) + "f);"
						+ lineSeparator;
			}
			LOGGER.error(message);

		}

		assert producedRoof : "FEHLER: Es konnte kein Dach fuer das Gebauede "
				+ getID() + " erzeugt werden!";

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erzeugt ein Dach fuer das aktuelle Gebauede basierend auf den
	 * Vorgaben des erzeugten Weight-Managers
	 * 
	 * @param roofConfiguration
	 *            Dachkonfiguration
	 */
	public void computeRoof(final FixedRoofWeightConfiguration weightManager) {

		// hole den Footprint des TOP-Quads des obersten Floors
		final List<FloorComplex> topFloors = getFloorsByPosition(FloorPosition.TOP);
		assert topFloors.size() == 1 : "FEHLER: Es wurden " + topFloors.size()
				+ " Stockwerke mit Position 'TOP' gefunden.";

		// hole das Top-Quad des Top-Floors
		final AbstractQuad top = topFloors.get(0).getQuadByDirection(Side.TOP);
		assert top != null : "FEHLER: Es wurde kein Quad mit Ausrichtung 'TOP' gefunden.";
		List<Vertex3d> quadVerts = top.getQuadVertices();

		final List<Float> edgeWeights = computeWeights(quadVerts, weightManager);

		final String lineSeparator = System.getProperty("line.separator");
		final StringBuffer message = new StringBuffer(lineSeparator);

		for (int i = 0; i < edgeWeights.size(); i++) {
			message.append("Edge Weight: " + i + ": " + edgeWeights.get(i)
					+ lineSeparator);
		}
		LOGGER.debug(message);

		float roofScaling = weightManager.getRoofScaling();

		// wenn der Dachgrundriss anders skaliert werden soll, skaliere die
		// Vertices ueber Polygonstrukturen
		if (roofScaling != 1.0f) {

			// erstelle Kopien, um die eigentlichen Grundrisse nicht zu
			// veraendern
			final List<Vertex3d> footprintClones = new ArrayList<Vertex3d>(
					quadVerts.size());
			for (int i = 0; i < quadVerts.size(); i++)
				footprintClones.add(quadVerts.get(i).clone());
			final MyPolygon scaledFootprint = new MyPolygon(footprintClones);
			scaledFootprint.scale(roofScaling);
			quadVerts = new ArrayList<Vertex3d>(footprintClones.size());
			quadVerts.addAll(scaledFootprint.getVertices());
		}

		Texture texture = null;
		Map<String, Texture> textures = getTextureMap();

		// sofern bereits eine Textur fuer das Dach geladen wurde, dekrementiere
		// die Anzahl der Verweise
		if (textures.containsKey("Roof")) {
			texture = textures.get("Roof");
			texture.removeReference();
			textures.remove("Roof");
		}

		// lade eine Dachtextur und uebergebe sie an die Dachberechnung
		// Wandtexturen werden nicht nach unten durchgereicht, da sie auf der
		// Ebene des Composite-Objekts verwaltet werden
		texture = TextureManagement.getInstance().getTextureForCategory(
				TextureCategory.Roof);
		textures.put("Roof", texture);

		final SkeletonRoofDescriptor roofConfig = new SkeletonRoofDescriptor();
		roofConfig.setVertices(quadVerts);
		roofConfig.setTexture(texture);
		roofConfig.setEdgeWeights(edgeWeights);
		roofConfig.setMainWeight(weightManager.getStandardWeight());
		roofConfig.setSideWeight(weightManager.getSideWeight());

		// wenn es sich um ein Mansardendach handelt, speichere di zusaetzlichen
		// Parameter in der Konfiguration
		if (weightManager.getRoofDescriptorType().equals("MansardRoof")) {
			roofConfig.setSecondMainWeight(weightManager
					.getSecondStandardWeight());
			roofConfig.setSecondSideWeight(weightManager.getSecondSideWeight());
			roofConfig.setSlopeChangeHeight(weightManager
					.getSlopeChangeHeight());
		}

		boolean producedRoof = produceRoof(roofConfig);
		StringBuffer strBuf = new StringBuffer();

		if (!producedRoof) {
			// Dump der Quad-Verts
			for (Vertex3d currentVertex : quadVerts) {
				strBuf.append("mVertices.add(new Vertex3d("
						+ currentVertex.getX() + "f, " + currentVertex.getY()
						+ "f, " + currentVertex.getZ() + "f));" + lineSeparator);
			}
			LOGGER.error(strBuf);

			// Dump der Gewichte
			strBuf = new StringBuffer();
			for (int i = 0; i < edgeWeights.size(); i++) {
				strBuf.append("weights.add(" + edgeWeights.get(i) + "f);"
						+ lineSeparator);
			}
			LOGGER.error(strBuf);

			assert false : "FEHLER: Es konnte kein Dach fuer das Gebauede "
					+ getID() + " erzeugt werden!";
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode liefert die Quads aller enthaltenen Stockwerke (im Gegensatz zu
	 * getQuads() werden hier aber nicht die Quads importierter Objekte etc.
	 * geliefert!
	 * 
	 * @return Liste mit Quads der Seitenwaende aller Stockwerke
	 */
	public List<AbstractQuad> getFloorOutdoorQuads() {

		List<AbstractQuad> quads = new ArrayList<AbstractQuad>();
		List<FloorComplex> floors = getFloors();

		for (int i = 0; i < floors.size(); i++)
			quads.addAll(floors.get(i).getOutdoorQuads());
		return quads;
	}

	// ------------------------------------------------------------------------------------------

	@Override
	/**
	 * Methode erzeugt ein leeres Composite-Objekt, das dann waehrend eines Clonings mit Kopien der Elemente befuellt wird
	 */
	protected AbstractComplex cloneConcreteComponent() {
		return new BuildingComplex(mParent);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode liefert das Stockwerk mit dem uebergebenen Index im
	 * Stockwerk-Stack
	 * 
	 * @param index
	 *            Index des Stockwerks innerhalb des Stacks
	 * @return Stockwerk, das sich an der gesuchten Position befindet
	 */
	public FloorComplex getFloorByPositionIndex(int index) {
		List<FloorComplex> floors = getFloors();
		assert index >= 0 && index < floors.size() : "FEHLER: Ungueltiger Index: "
				+ index + "!";
		return floors.get(index);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode liefert das komplexe Objekt, das das Dach repraesentiert
	 * 
	 * @return AbstractComplex, das das Dach darstellt, null, wenn noch kein
	 *         Dach berechnet wurde
	 */
	public RoofComplex getRoof() {

		// durchlaufe alle Komponenten bis eine Komponente gefunden wird, die
		// vom Typ "Dach" ist
		for (AbstractComplex curComp : mComponents) {
			if (curComp instanceof RoofComplex) {
				return (RoofComplex) curComp;
			}
		}
		return null;
	}
	// ------------------------------------------------------------------------------------------

}
