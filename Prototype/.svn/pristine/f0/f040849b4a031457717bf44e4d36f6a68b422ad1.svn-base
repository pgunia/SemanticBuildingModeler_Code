package semantic.building.modeler.prototype.graphics.complex;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;

import processing.core.PApplet;
import semantic.building.modeler.configurationservice.model.enums.Side;
import semantic.building.modeler.math.Axis;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.prototype.algorithm.FootprintCreator;
import semantic.building.modeler.prototype.enums.QuadType;
import semantic.building.modeler.prototype.enums.subdivisionType;
import semantic.building.modeler.prototype.graphics.interfaces.iGraphicComplex;
import semantic.building.modeler.prototype.graphics.primitives.AbstractQuad;
import semantic.building.modeler.prototype.roof.configuration.FixedRoofWeightConfiguration;
import semantic.building.modeler.prototype.roof.configuration.RandomRoofWeightConfiguration;
import semantic.building.modeler.prototype.service.EdgeManager;
import semantic.building.modeler.prototype.service.IdentifierService;
import semantic.building.modeler.prototype.service.Texture;
import semantic.building.modeler.prototype.service.TextureManagement;
import semantic.building.modeler.prototype.service.TextureManagement.TextureCategory;
import semantic.building.modeler.weightedstraightskeleton.algorithm.SkeletonRoofDescriptor;
import semantic.building.modeler.weightedstraightskeleton.controller.StraightSkeletonController;

/**
 * 
 * @author Patrick Gunia Klasse repraesentiert eine Menge von komplexen
 *         Objekten, die innerhalb des Composite-Objekts zusammengefasst werden.
 *         Konzeptuell handelt es sich um eine Implementation des
 *         Composite-Patterns. Ein Composite-Objekt kann eine beliebige Anzahl
 *         komplexer Objekte oder weitere Composite-Objekte enthalten.
 *         Saemtliche Methodenaufrufe werden einfach an die Teilobjekte
 *         weitergeleitet.
 */

public class CompositeComplex extends AbstractComplex {

	/**
	 * Liste mit allen komplexen Komponenten, die dem Composite-Objekt
	 * hinzugefuegt wurden
	 */
	protected List<AbstractComplex> mComponents = null;

	/**
	 * Flag gibt an, ob das komplexe Objekt bereits irgend eine Art von Textur
	 * zugewiesen bekommen hat, unabhaengig von einer konkreten Kategorie o.ae.
	 * => sobald eine Textur zugewiesen wurde, berechnet man die Koordinaten des
	 * Composites neu, falls eine neue Komponente geadded wurde, sonst nicht
	 */
	protected Boolean mHasTextureApplied = false;

	public CompositeComplex(PApplet parent) {
		super(parent);
		mComponents = new ArrayList<AbstractComplex>();
	}

	// ------------------------------------------------------------------------------------------
	@Override
	public String getType() {
		return "composite";
	}

	// ------------------------------------------------------------------------------------------

	@Override
	/** Leere Create-Methode, ein Composite-Objekt wird durch das Hinzufuegen komplexer Objekte erzeugt */
	public void create() {

		// erzeuge eine ID => TODO: fuer mehrere Composite-Ebenen muss hier die
		// Moeglichkeit bestehen, eine ID durchzureichen
		generateID(null, null);

		// registriere das Composite-Objekt beim Management-Service
		register();

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erzeugt fuer das Composite-Objekt eine ID mit Hilfe des
	 * Identifier-Service
	 */
	@Override
	public void generateID(final String baseID, final String concat) {

		String id = "";

		// beginne die ID mit dem Typ des aktuellen Elements
		id = getType();

		// wenn keine baseID uebergeben wurde, fordere eine vom ID-Generator
		if (baseID == null) {
			id += "_" + IdentifierService.getInstance().generate() + "_";
		} else {
			id += "_" + baseID + "_";
		}

		setID(id);
	}

	// ------------------------------------------------------------------------------------------

	@Override
	/**
	 * Methode durchlaeuft alle Komponenten des Composite-Objekts und leitet den Draw-Call an diese weiter
	 * @param drawTextures Flag gibt an, ob das Model mit Texturen gezeichnet werden soll
	 */
	public void draw(final Boolean drawTextures) {

		// fuehre die Mouse-Drag-Rotationen auf der Ebene des Composite-Objekts
		// durch und leite sie nicht weiter an die Kinder
		mParent.pushMatrix();

		// schnelle Iteration => Runterzaehlen ohne Iterator
		int numberOfComponents = mComponents.size();
		AbstractComplex[] components = (AbstractComplex[]) mComponents
				.toArray(new AbstractComplex[numberOfComponents]);
		for (int i = numberOfComponents - 1; i >= 0; i--)
			components[i].draw(drawTextures);
		mParent.popMatrix();

	}

	// ------------------------------------------------------------------------------------------
	@Override
	/**
	 * Methode durchlaeuft alle Komponenten des Composite-Objekts und leitet den Draw-Call an diese weiter
	 * @param drawTextures Flag gibt an, ob das Model mit Texturen gezeichnet werden soll
	 * @param gl GL-Kontext, auf den gezeichnet werden soll
	 */
	public void drawGL(final Boolean drawTextures, final GL gl) {

		// fuehre die Mouse-Drag-Rotationen auf der Ebene des Composite-Objekts
		// durch und leite sie nicht weiter an die Kinder
		// mParent.pushMatrix();

		// schnelle Iteration => Runterzaehlen ohne Iterator
		int numberOfComponents = mComponents.size();
		final AbstractComplex[] components = (AbstractComplex[]) mComponents
				.toArray(new AbstractComplex[numberOfComponents]);
		for (int i = numberOfComponents - 1; i >= 0; i--)
			components[i].drawGL(drawTextures, gl);
		// mParent.popMatrix();

	}

	// ------------------------------------------------------------------------------------------

	@Override
	/**
	 * Methode leitet die Extrude-Calls an saemtliche Unterobjekte weiter
	 */
	public void extrude(Side whichFace, Axis extrudeAxis, float extrudeAmount) {
		Iterator<AbstractComplex> componentIter = mComponents.iterator();
		while (componentIter.hasNext()) {
			componentIter.next().extrude(whichFace, extrudeAxis, extrudeAmount);
		}
	}

	// ------------------------------------------------------------------------------------------

	@Override
	/**
	 * Weiterleitung der Subdivision-Aufrufe an die Komponenten
	 */
	public void subdivideQuad(Side whichFace, subdivisionType type,
			float subdivisionFactor) {
		Iterator<AbstractComplex> componentIter = mComponents.iterator();
		while (componentIter.hasNext()) {
			componentIter.next().subdivideQuad(whichFace, type,
					subdivisionFactor);
		}

	}

	// ------------------------------------------------------------------------------------------

	@Override
	/**
	 * Unterteilungsmehtode fuer das gesamte komplexe Objekt. Leitet die Aufrufe ebenfalls wieder an die Subobjekte weiter.
	 * Bei jeder Unterteilung entsteht ein neues Objekt, das automatisch bei der Objektverwaltung registriert wird und automatisch
	 * zum Composite-Objekt geadded wird
	 */
	public iGraphicComplex subdivide(subdivisionType type,
			float subdivisionFactor) {
		Iterator<AbstractComplex> componentIter = mComponents.iterator();
		while (componentIter.hasNext()) {
			addComplex((AbstractComplex) componentIter.next().subdivide(type,
					subdivisionFactor));
		}
		return null;
	}

	// ------------------------------------------------------------------------------------------

	@Override
	/**
	 * Der Dachberechnungsalgorithmus ist fuer Composite-Objekte aufwendiger. Er erfordert zunaechst die Ermittlung
	 * des Grundrisses aller beteiligten komplexen Objekte
	 */
	public void computeRoof() {

		List<AbstractComplex> components = getComponents();
		AbstractComplex currentComponent = null;

		for (int i = 0; i < components.size(); i++) {
			currentComponent = components.get(i);

			// bei Composite-Komponenten Call an die Komponenten weiterleiten
			if (currentComponent instanceof semantic.building.modeler.prototype.graphics.complex.CompositeComplex) {
				currentComponent.computeRoof();
			}
		}

		// berechne gemergede Grundrisse, verwende bei Composites immer den
		// Standardalgorithmus
		// erst bei MergedComplex-Objekten kann der Grundriss selber ueber
		// Graham-Scan errechnet werden
		List<List<Vertex3d>> mergedFootprints = computeFootprints(false);

		// wurde kein gemergter Grundriss berechnet, breche ab
		if (mergedFootprints.size() == 0)
			return;

		List<Vertex3d> currentRoofFootprint = null;

		// erstelle fuer jeden germergten Footprint einen Weight-Vector
		List<List<Float>> footprintWeights = new ArrayList<List<Float>>();
		Iterator<List<Vertex3d>> footprintIter = mergedFootprints.iterator();

		RandomRoofWeightConfiguration weightManager = new RandomRoofWeightConfiguration();
		weightManager.initializeEdgeWeights();

		while (footprintIter.hasNext()) {
			List<Vertex3d> currentVertices = footprintIter.next();
			footprintWeights
					.add(computeWeights(currentVertices, weightManager));
		}

		String weightDefinitions = "";
		String lineSeparator = System.getProperty("line.separator");
		weightDefinitions = lineSeparator;
		for (int j = 0; j < footprintWeights.size(); j++) {
			List<Float> currentWeights = footprintWeights.get(j);
			LOGGER.info("Footprint " + j + ": ");
			for (int i = 0; i < currentWeights.size(); i++) {
				weightDefinitions += "weights.add(" + currentWeights.get(i)
						+ "f);" + lineSeparator;
			}
			LOGGER.info(weightDefinitions);
		}

		Texture texture = null;

		// sofern bereits eine Textur fuer das Dach geladen wurde, dekrementiere
		// die Anzahl der Verweise
		if (mTextures.containsKey("Roof")) {
			texture = mTextures.get("Roof");
			texture.removeReference();
			mTextures.remove("Roof");
		}

		// lade eine Dachtextur und uebergebe sie an die Dachberechnung
		// Wandtexturen werden nicht nach unten durchgereicht, da sie auf der
		// Ebene des Composite-Objekts verwaltet werden
		texture = TextureManagement.getInstance().getTextureForCategory(
				TextureCategory.Roof);
		mTextures.put("Roof", texture);

		SkeletonRoofDescriptor roofConfig = null;

		// durchlaufe nun alle berechneten gemergten Grundrisse und erzeuge fuer
		// jeden Grundriss ein Dach
		footprintIter = mergedFootprints.iterator();

		List<SkeletonRoofDescriptor> roofConfigs = new ArrayList<SkeletonRoofDescriptor>();
		for (int i = 0; i < mergedFootprints.size(); i++) {
			currentRoofFootprint = new ArrayList<Vertex3d>();
			currentRoofFootprint.addAll(mergedFootprints.get(i));

			String message = "";
			Vertex3d currentVertex = null;
			for (int k = 0; k < currentRoofFootprint.size(); k++) {
				currentVertex = currentRoofFootprint.get(k);
				message += "mVertices.add(new Vertex3d(" + currentVertex.getX()
						+ "f, " + currentVertex.getY() + "f, "
						+ currentVertex.getZ() + "f));" + lineSeparator;
			}
			LOGGER.info(message);

			// erzeuge fuer jeden Footprint eine Dachkonfiguration
			roofConfig = new SkeletonRoofDescriptor();
			roofConfig.setVertices(currentRoofFootprint);
			roofConfig.setTexture(texture);
			roofConfig.setEdgeWeights(footprintWeights.get(i));
			roofConfigs.add(roofConfig);
		}

		// erzeuge nun fuer jede Konfiguration ein Dach
		Iterator<SkeletonRoofDescriptor> configIter = roofConfigs.iterator();
		SkeletonRoofDescriptor currentConfig = null;

		int count = 0;
		int maxNumberOfRetries = 50;

		List<Float> recomputedWeights = null;
		boolean computationSuccessful = true;

		while (configIter.hasNext()) {
			count = 0;
			currentConfig = configIter.next();
			count++;

			while (!produceRoof(currentConfig)) {
				weightManager.initializeEdgeWeights();
				recomputedWeights = computeWeights(currentConfig.getVertices(),
						weightManager);
				weightDefinitions = "";
				for (int i = 0; i < recomputedWeights.size(); i++) {
					weightDefinitions += "weights.add("
							+ recomputedWeights.get(i) + "f);" + lineSeparator;
				}
				LOGGER.info(weightDefinitions);
				LOGGER.info("Recomputing Roof...");
				currentConfig.setEdgeWeights(recomputedWeights);
				count++;

				// Obergrenze, wenn nach der festegelegten Anzahl von Retries
				// kein Dach berechnet wurde, dann wirds auch nichts mehr
				if (count >= maxNumberOfRetries) {
					computationSuccessful = false;
					String vertexDefinitions = null;

					// Footprint fuer Debugging-Zwecke ausgeben
					List<Vertex3d> vertices = currentConfig.getVertices();
					LOGGER.info("################# Roof Footprint #################");
					vertexDefinitions = lineSeparator;
					for (int j = 0; j < vertices.size(); j++) {

						MyVector3f pos = vertices.get(j).getPosition();
						vertexDefinitions += "mVertices.add(new Vertex3d("
								+ pos.x + "f, " + pos.y + "f, " + pos.z
								+ "f));" + lineSeparator;
					}

					LOGGER.error("Dachberechnung wurde aufgrund zu vieler erfolgloser Versuche abgebrochen! Anzahl Vertices: "
							+ vertices.size()
							+ " Anzahl weights: "
							+ recomputedWeights.size()
							+ " Fehlerhafter Footprint: ");
					LOGGER.error(vertexDefinitions);
					break;
				}
			}
		}

		float averageNumberOfRetries = count / mergedFootprints.size();
		LOGGER.info("Es wurden durchschnittlich " + averageNumberOfRetries
				+ " Versuche benoetigt, um ein Dach zu berechnen");
		if (!computationSuccessful)
			LOGGER.error("Bei mindestens einer Konfiguration musste die Berechnung aufgrund zu vieler Fehlversuche abgebrochen werden");

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet ein Dach fuer die uebergebene Konfiguration.
	 * 
	 * @param config
	 *            RoofDescriptor-Instanz
	 */
	protected boolean produceRoof(final SkeletonRoofDescriptor config) {

		try {
			final StraightSkeletonController roof = new StraightSkeletonController(
					config);

			// mRoofControllers.add(roof);

			final RoofComplex roofComplex = new RoofComplex(mParent, roof,
					config);
			roofComplex.create();

			// Texturkoordinatenberechnung ist hier unnoetigt, da eine
			// Neuberechnung beim Hinzufuegen zum Complex-Objekt erfolgt
			// roofComplex.computeTextureCoordinates();
			roofComplex.setTexture("roof", config.getTexture());

			// adde das Dach als neues komplexes Objekt
			addComplex(roofComplex);

			// mRoofs.add(roofComplex);

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} catch (AssertionError e) {
			e.printStackTrace();
			return false;
		}
		return true;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet Gewichte fuer die Kanten, die durch den uebergebenen
	 * Vertex-Vector beschrieben werden. Hierfuer greift sie auf den
	 * WeightManager-Service und die Edge-Manager der jeweiligen komplexen
	 * Komponenten zurueck.
	 * 
	 * @param footprint
	 *            Vector mit Vertex3d-Instanzen, die im Uhrzeigersinn definiert
	 *            sind und die Kanten des Grundrisses festlegen
	 * @return Vector mit Float-Gewichten
	 */
	protected List<Float> computeWeights(List<Vertex3d> footprint,
			FixedRoofWeightConfiguration weightManager) {
		List<Float> weights = new ArrayList<Float>();

		// sammele alle EdgeManager ueber alle Hierarchieebenen hinweg ein
		List<EdgeManager> allEdgeManager = new ArrayList<EdgeManager>();
		Iterator<AbstractComplex> componentIter = mComponents.iterator();
		AbstractComplex currentComponent = null;

		while (componentIter.hasNext()) {
			currentComponent = componentIter.next();
			allEdgeManager.addAll(currentComponent.getEdgeManager());
		}

		Vertex3d currentVert = null, currentNeighbour = null;

		Iterator<EdgeManager> edgeManagerIter = null;
		EdgeManager currentEdgeManager = null;

		for (int i = 0; i < footprint.size(); i++) {
			currentVert = footprint.get(i);

			if ((i + 1) < footprint.size())
				currentNeighbour = footprint.get(i + 1);
			else
				currentNeighbour = footprint.get(0);

			// durchlaufe alle geladenen EdgeManager und suche denjenigen, der
			// die aktuelle Kante verwaltet
			edgeManagerIter = allEdgeManager.iterator();

			while (edgeManagerIter.hasNext()) {
				currentEdgeManager = edgeManagerIter.next();
				String index = currentEdgeManager.getEdgeIndexByVertices(
						currentVert, currentNeighbour);
				if (index == null)
					continue;
				else {
					weightManager.setCurrentEdgeManager(currentEdgeManager);
					Float weight = weightManager.getWeightByEdgeIndex(index);
					weights.add(weight);
				}
			}
		}
		return weights;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode zum Adden von beliebigen komplexen Objekten zur Composite-Instanz
	 */
	public void addComplex(final AbstractComplex component) {
		if (mComponents.indexOf(component) == -1) {

			// fuege das komplexe Objekt zum Composite-Objekt hinzu
			if (component instanceof semantic.building.modeler.prototype.graphics.complex.AbstractComplex) {
				LOGGER.debug("Added Component to Composite-Complex: " + getID()
						+ " Komponente: " + component.getID());
				mComponents.add(component);

				// melde es bei der Objektverwaltung ab, die Kontrolle liegt
				// dann beim Composite-Objekt
				component.unregister();

				// es handelt sich bei dem hinzugefuegten Komplexen Objekt nicht
				// mehr laenger um das Root-Objekt einer Hierarchie
				component.setIsRoot(false);

				// Parent aktualisieren
				component.setComplexParent(this);
			}

			// durch die neue Komponente kann sich das Texture-Scaling
			// veraendert haben, berechne die Koordinaten neu, allerdings nur,
			// falls bereits eine
			// Textur gesetzt wurde, sonst wird die Koordinatenberechnung erst
			// spaeter durchgefuehrt
			if (mHasTextureApplied) {
				computeTextureCoordinates();
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode entfernt die uebergebene Komponente aus der Composite-Vertwaltung
	 * 
	 * @param complex
	 *            Komplexe Komponente, die aus der Verwaltung herausgenommen
	 *            werden soll
	 */
	public void removeComplex(AbstractComplex complex) {

		if (mComponents.indexOf(complex) == -1)
			LOGGER.error("FEHLER: Das komplexe Objekt " + complex.getID()
					+ " ist nicht Komponente des Composite-Objekts " + getID());
		else {
			mComponents.remove(complex);

			// melde das entfernte Objekt wieder bei der Objektverwaltung an und
			// setze es auf Root, da es nun wieder unabhaengig vom Composite ist
			complex.register();
			complex.setIsRoot(true);
			complex.setComplexParent(null);
		}
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mComponents
	 */
	public List<AbstractComplex> getComponents() {
		return mComponents;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mComponents
	 *            the mComponents to set
	 */
	public void setComponents(List<AbstractComplex> mComponents) {
		this.mComponents = mComponents;
	}

	// ------------------------------------------------------------------------------------------
	@Override
	public List<EdgeManager> getEdgeManager() {
		List<EdgeManager> edgeManager = new ArrayList<EdgeManager>();
		Iterator<AbstractComplex> componentIter = mComponents.iterator();

		AbstractComplex currentComponent = null;
		while (componentIter.hasNext()) {
			currentComponent = componentIter.next();
			edgeManager.addAll(currentComponent.getEdgeManager());
		}

		return edgeManager;
	}

	// ------------------------------------------------------------------------------------------
	@Override
	/**
	 * Methode durchlaeuft alle Komponentenobjekte und ruft deren Texturkoordinatenberechnungsroutinen auf. Bei 
	 * jedem Aufruf sammelt sie die Skalierungsfaktoren und waehlt den kleinsten vorkommenden Faktor aus. Wurde 
	 * die Wurzel der Hierarchie erreicht, so werden alle Texturkoordinaten mit dem ermittelten Faktor skaliert.
	 * @return Minimal aufgetretener Skalierungsfaktor ueber alle Quads des komplexen Objekts
	 */
	public Float computeTextureCoordinates() {
		LOGGER.trace("Computing Texturcoordinates for: " + getID());

		Float currentScaleFactor = null, minScaleFactor = Float.MAX_VALUE;
		for(AbstractComplex current : mComponents) {

			// Sprites nicht beruecksichtigen, das kann deren
			// Texturierung verhauen
			if (current instanceof semantic.building.modeler.prototype.graphics.complex.Sprite)
				continue;

			// aktualisiere die Skalierungsfaktoren ueber alle
			// Komponentenobjekte
			currentScaleFactor = current.computeTextureCoordinates();
			if (currentScaleFactor < minScaleFactor) {
				minScaleFactor = currentScaleFactor;
			}
		}

		mScaledTextureCoords = false;

		// skaliere alle Subkomponenten, deren Texturkoordinaten noch nicht
		// skaliert wurden
		for(AbstractComplex current : mComponents) {
	
			// skippe, wenn es sich im bestimmte Arten handelt
			if (current instanceof semantic.building.modeler.prototype.graphics.complex.Sprite)
				continue;
			current.scaleTextureCoordinates(minScaleFactor);
		}

		mScaledTextureCoords = true;

		// gebe den minimalen Scalingfaktor zurueck
		return minScaleFactor;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Hack, um den fehlenden Scope-Operator bei polymorphen Calls zu ersetzen
	 * => BuildingComplex-Instanzen benoetigen sowohl den Standard-Weg, wie er
	 * von AbstractComplex implementiert wird, als auch den Composite-Weg ueber
	 * mehrere Component-Instanzen hinweg
	 */
	protected Float computeTextureCoordinatesStandardWay() {
		return super.computeTextureCoordinates();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Hack, um den fehlenden Scope-Operator bei polymorphen Calls zu ersetzen
	 * => BuildingComplex-Instanzen benoetigen sowohl den Standard-Weg, wie er
	 * von AbstractComplex implementiert wird, als auch den Composite-Weg ueber
	 * mehrere Component-Instanzen hinweg
	 */
	protected void scaleTextureCoordinatesStandardWay(float scalingFactor) {
		super.scaleTextureCoordinates(scalingFactor);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode gibt Texturen fuer komplexe Objekte und alle ihre
	 * Komponentenobjekte frei
	 * 
	 * @param category
	 *            Kategorie der Textur, ueber diese erfolgt der Zugriff auf die
	 *            HashMap
	 */
	@Override
	protected void releaseTextureByCategory(String category) {

		Texture texture = mTextures.get(category);
		if (texture != null) {
			texture.removeReference();
			mTextures.remove(category);
		}

		// durchlaufe nun alle Kindobjekte
		Iterator<AbstractComplex> componentIter = mComponents.iterator();
		AbstractComplex current = null;

		while (componentIter.hasNext()) {
			current = componentIter.next();
			current.releaseTextureByCategory(category);
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode speichert die uebergebene Textur in der HashMap fuer die
	 * uebergebene Kategorie
	 */
	@Override
	public void setTexture(String category, Texture texture) {

		computeTextureCoordinates();

		// speichere die Kategorie-Texturzuordnung im aktuellen HashMap-Objekt
		addTextureToMap(category, texture);
			
		for(AbstractComplex current : mComponents) {
			if(current instanceof FloorComplex || current instanceof RoofComplex) {
				current.setTexture(category, texture);
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode durchlaeuft alle gespeicherten Texturinstanzen und gibt die
	 * Verweise frei, sofern ein Zugriff auf die Textur erfolgt
	 * 
	 * @param texture
	 *            Texturobjekt, dessen Referenzen freigegeben werden sollen
	 */
	@Override
	protected void releaseTextureByTextureObject(Texture texture) {

		// loesche saemtliche Referenzen aus dem Composite-Objekt
		super.releaseTextureByTextureObject(texture);

		// und entferne alle Verweise
		for (int i = 0; i < mComponents.size(); i++)
			mComponents.get(i).releaseTextureByTextureObject(texture);

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

		LOGGER.error("ANGEFORDERTER TYP: " + type);
		int numberOfQuadsInCategory = 0;

		AbstractComplex currentComponent = null;
		for (int i = 0; i < mComponents.size(); i++) {
			currentComponent = mComponents.get(i);
			numberOfQuadsInCategory += currentComponent.getQuads(type).size();
		}

		boolean updateNecessary = false;
		// sind die Composite-Buffer fuer den aktuellen Typ aktuell?
		switch (type) {
		case INDOOR:
			if (mIndoorQuads == null
					|| mIndoorQuads.size() != numberOfQuadsInCategory)
				updateNecessary = true;
			else
				return mIndoorQuads;
			break;
		case OUTDOOR:
			if (mOutdoorQuads == null
					|| mOutdoorQuads.size() != numberOfQuadsInCategory)
				updateNecessary = true;
			else
				return mOutdoorQuads;
			break;
		case ALL:
			if (mAllQuads == null
					|| mAllQuads.size() != numberOfQuadsInCategory)
				updateNecessary = true;
			else
				return mAllQuads;
			break;
		default:
			LOGGER.error("Unbekannter Quad-Typ: " + type);
			return null;
		}

		// wenn nein, dann aktualisieren
		if (updateNecessary) {
			List<AbstractQuad> buffer = new ArrayList<AbstractQuad>(
					numberOfQuadsInCategory);
			for (int i = 0; i < mComponents.size(); i++) {
				currentComponent = mComponents.get(i);
				buffer.addAll(currentComponent.getQuads(type));
			}

			// jetzt noch entscheiden, welchem Buffer assigned wird
			switch (type) {
			case INDOOR:
				mIndoorQuads = buffer;
				return mIndoorQuads;
			case OUTDOOR:
				mOutdoorQuads = buffer;
				return mOutdoorQuads;
			case ALL:
				mAllQuads = buffer;
				return mAllQuads;
			default:
				return null;
			}
		}
		return null;
	}

	// ------------------------------------------------------------------------------------------
	@Override
	/**
	 * Methode durchlaueft alle Quads der Komponenten des Composite-Objekts und sucht nach einer Komponente mit der gewuenschten Ausrichtung. Sobald eine solche Komponente gefunden wird,
	 * bricht die Methode ab und gibt diese zurueck.
	 * @param direction Gesuchte Ausrichtung
	 * @return Quad mit der gesuchten Ausrichtung, sofern ein solches gefunden wurde, null sonst
	 */
	public AbstractQuad getQuadByDirection(Side direction) {

		List<AbstractQuad> quads = getOutdoorQuads();
		AbstractQuad currentQuad = null;
		for (int i = 0; i < quads.size(); i++) {
			currentQuad = quads.get(i);
			if (currentQuad.getDirection().equals(direction))
				return currentQuad;
		}

		assert false : "FEHLER: Ein Quad mit der Zielausrichtung " + direction
				+ " wurde nicht gefunden";
		return null;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Bisher noch nicht vorgesehen
	 */
	@Override
	public void rotate(MyVector3f axis, MyVector3f anchorPoint, double angle) {
		assert false : "FEHLER: Methode fuer Composite-Objekte noch nicht implementiert";

	}

	// ------------------------------------------------------------------------------------------
	@Override
	public void alignDirectionsByNormals(Map<MyVector3f, Side> map,
			List<AbstractQuad> quads) {
		assert false : "FEHLER: Methode fuer Composite-Objekte noch nicht implementiert";
	}

	// ------------------------------------------------------------------------------------------
	@Override
	/**
	 * Methode leitet den Aufruf fuer die Texturkoordinatenskalierung an die Subkomponenten weiter
	 * @param minScaleFaktor Skalierungsfaktor ueber den die Texturkoordinaten skaliert werden
	 */
	public void scaleTextureCoordinates(float minScaleFaktor) {
		LOGGER.trace("Scaling Texture Coordinates for Composite " + getID()
				+ " Factor: " + minScaleFaktor);
		Iterator<AbstractComplex> componentIter = getComponents().iterator();
		while (componentIter.hasNext()) {
			componentIter.next().scaleTextureCoordinates(minScaleFaktor);
		}

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode liefert die Outline des TOP-Quads des komplexen Objekts. Bei
	 * Composite-Objekten koennen Komponenten auf unterschiedlichen Hoehen
	 * vorliegen, in diesem Fall werden mehrere Grundrisse zurueckgeliefert.
	 * 
	 * @return Liste mit den Outlines des komplexen Objekts basierend auf dem
	 *         Polygonzug, der das TOP-Quad beschreibt
	 */
	public List<List<Vertex3d>> getTopQuadFootprints() {

		List<AbstractQuad> topQuads = getAllOutsideQuadsWithDirection(Side.TOP);
		List<List<Vertex3d>> outlines = new ArrayList<List<Vertex3d>>(
				topQuads.size());

		for (int i = 0; i < topQuads.size(); i++)
			outlines.add(topQuads.get(i).getQuadVertices());
		return outlines;

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode berechnet ueber den FootprintMerger-Service eine Menge von
	 * Grundrissen, die jeweils als Polygonzug durch eine Liste von
	 * Vertex3d-Strukturen beschrieben werden
	 * 
	 * @param useConvexHull
	 *            Flag gibt an, ob fuer die Footprint-Berechnung der
	 *            Graham-Scan-Algorithmus verwendet werden soll
	 * @return Footprints als Liste von Vertex3d-Strukturen
	 */
	public List<List<Vertex3d>> computeFootprints(final Boolean useConvexHull) {
		Iterator<AbstractComplex> componentIter = mComponents.iterator();

		List<AbstractComplex> relevantComponents = new ArrayList<AbstractComplex>();

		AbstractComplex currentComponent = null;

		while (componentIter.hasNext()) {
			currentComponent = componentIter.next();

			// Ueberspringe Sprites, ImportedComplex und
			// CompositeComplex-Objekte
			if (currentComponent instanceof semantic.building.modeler.prototype.graphics.complex.Sprite
					|| currentComponent instanceof semantic.building.modeler.prototype.graphics.complex.ImportedComplex
					|| currentComponent instanceof semantic.building.modeler.prototype.graphics.complex.CompositeComplex)
				continue;

			// alle anderen Typen werden durch Extrusion erzeugt und verfuegen
			// dadurch automatisch ueber einen Footprint in Form des TOP-Quads
			else
				relevantComponents.add(currentComponent);
		}

		// es wurde mehr als eine relevante Komponente gefunden => berechne
		// einen gemergten Grundriss
		if (relevantComponents.size() > 1) {
			FootprintCreator footprintAlgo = new FootprintCreator();
			for (int i = 0; i < relevantComponents.size(); i++) {
				footprintAlgo.addComponent(relevantComponents.get(i));
			}
			return footprintAlgo.process(useConvexHull);
		}
		// nur eine Komponente gefunden => gebe deren Footprint ohne weitere
		// Berechnungen zurueck
		else if (relevantComponents.size() == 1)
			return relevantComponents.get(0).getTopQuadFootprints();

		// keine Komponente gefunden: gib ein leeres Array zurueck!
		else {
			LOGGER.debug("Composite " + getID() + " Root:" + isRoot()
					+ " enthaelt keine relevanten Komponenten");
			return new ArrayList<List<Vertex3d>>();
		}

	}

	// ------------------------------------------------------------------------------------------
	@Override
	/**
	 * Ueberschriebene Methode reicht die Texturanforderung an die Komponentenobjekte weiter. Dadurch soll erreicht werden,
	 * dass unterschiedliche Komponenten eines Composite-Objekts unterschiedlich texturiert werden. Dies spielt dann eine 
	 * wichtige Rolle, wenn Composite-Objekte als Root-Objekt einer Hierarchier verwendet werden. Durch die Verwendung von
	 * MergedComplex-Objekten, spielen sie primaer fuer diesen Zweck eine wichtige Rolle
	 * @param category Texturkategorie, aus der eine Textur angefordert wird
	 */
	public void setTextureByCategory(TextureCategory category) {

		LOGGER.trace("Demanding Texture for Category: " + category
				+ " for Complex: " + getID());

		// Textur-Flag setzen
		mHasTextureApplied = true;
		final Iterator<AbstractComplex> componentIter = mComponents.iterator();

		while (componentIter.hasNext()) {
			componentIter.next().setTextureByCategory(category);
		}

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode gibt alle erzeugten Komponenten mit ihren zugehoerigen Massen aus
	 * 
	 * @return String mit allen erzeugten Komponenten
	 */
	public String printComponents() {

		Iterator<AbstractComplex> componentIter = mComponents.iterator();

		// iteriere ueber alle hinzugefuegten Komponenten und erzeuge deren
		// Create Statements

		/*
		 * AbstractComplex test = new Cube(mParentApplet, 300.0f,100.0f,200.0f);
		 * test.create(); test.setPosition(new MyVector3f(400, 500, 0));
		 */

		String lineSeparator = System.getProperty("line.separator");
		String message = lineSeparator;

		AbstractComplex currentComplex = null;
		Integer index = 0;
		while (componentIter.hasNext()) {
			currentComplex = componentIter.next();

			if (currentComplex instanceof semantic.building.modeler.prototype.graphics.complex.Cube) {
				message += lineSeparator + "AbstractComplex temp" + index
						+ "= new Cube(mParentApplet, ";
				Cube cube = (Cube) currentComplex;
				MyVector3f position = cube.getPosition();
				message += cube.getWidth() + "f, ";
				message += cube.getHeight() + "f, ";
				message += cube.getDepth() + "f);" + lineSeparator;
				message += "temp" + index + ".create();" + lineSeparator;
				message += "temp" + index + ".translate(new MyVector3f("
						+ position.x + "f, " + position.y + "f, " + position.z
						+ "f));" + lineSeparator;
				message += "comp.addComplex(temp" + index + ");";
				index++;
			}

			// public Cylinder(PApplet parent, int numberOfSegments, Float
			// height, Float radius) {

			else if (currentComplex instanceof semantic.building.modeler.prototype.graphics.complex.Cylinder) {
				Cylinder cylinder = (Cylinder) currentComplex;
				MyVector3f position = cylinder.getPosition();
				message += lineSeparator + "AbstractComplex temp" + index
						+ "= new Cylinder(mParentApplet, ";
				message += cylinder.getNumberOfSegments() + ", ";
				message += cylinder.getHeight() + "f, ";
				message += cylinder.getRadius() + "f);" + lineSeparator;
				message += "temp" + index + ".create();" + lineSeparator;
				message += "temp" + index + ".translate(new MyVector3f("
						+ position.x + "f, " + position.y + "f, " + position.z
						+ "f));" + lineSeparator;
				message += "comp.addComplex(temp" + index + ");";
				index++;

			}
		}
		return message;

	}

	// ------------------------------------------------------------------------------------------
	@Override
	/**
	 * Methode reicht alle konkreten (Nicht-Composite) komplexen Objekte an den Aufrufer zurueck
	 * @return Liste mit allen Objekten, die Komponenten des Composite-Objekts sind
	 */
	public List<AbstractComplex> getConcreteComponents() {

		Iterator<AbstractComplex> componentIter = getComponents().iterator();
		AbstractComplex currentComplex = null;
		List<AbstractComplex> resultComponents = new ArrayList<AbstractComplex>();

		while (componentIter.hasNext()) {
			currentComplex = componentIter.next();
			resultComponents.addAll(currentComplex.getConcreteComponents());
		}

		return resultComponents;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode gibt Basisinformationen ueber das aktuelle Objekt aus, ID,
	 * Position in der Hierarchie usw.
	 * 
	 * @param prefix
	 *            Prefix, das vor die Ausgabe gehangen wird
	 */
	public void printComplex(String prefix) {

		// Basisklassenmethode aufrufen
		super.printComplex(prefix);

		// fuer alle Komponenten die Methode rekursiv weiteraufrufen
		Iterator<AbstractComplex> componentIter = mComponents.iterator();

		String newPrefix = prefix + "......";

		while (componentIter.hasNext()) {
			componentIter.next().printComplex(newPrefix);
		}
	}

	// ------------------------------------------------------------------------------------------
	@Override
	/**
	 * Methode leitet die Calls zum Erstellen von Stockwerken fuer das aktuelle komplexe Objekte an die Kindobjekte weiter.
	 * @param numberOfLevels Anzahl an Stockwerken, die fuer das Objekt generiert werden soll
	 */
	public AbstractComplex createLevels(int numberOfLevels) {

		Iterator<AbstractComplex> componentIter = mComponents.iterator();
		AbstractComplex currentComponent = null;

		List<AbstractComplex> newComponents = new ArrayList<AbstractComplex>(
				mComponents.size());

		while (componentIter.hasNext()) {
			currentComponent = componentIter.next();

			if (currentComponent instanceof semantic.building.modeler.prototype.graphics.complex.RoofComplex
					|| currentComponent instanceof semantic.building.modeler.prototype.graphics.complex.Sprite)
				continue;

			// erzeuge fuer jede Komponente eine Stockwerkunterteilung
			newComponents.add(currentComponent.createLevels(numberOfLevels));

			// und entferne die Quellkomponente aus dem Composite-Objekt
			componentIter.remove();
			currentComponent = null;
		}

		// fuege nun alle neu erszeugten Komponenten zum Composite-Objekt hinzu
		for (int i = 0; i < newComponents.size(); i++)
			addComplex(newComponents.get(i));
		return this;
	}

	// ------------------------------------------------------------------------------------------
	@Override
	/**
	 * Clone-Methode fuer Composites, erstellt ein neues Composite-Objekt und fuegt Kopien aller Komponenten zu diesem
	 * neuen Objekt hinzu
	 * @return Kopie des Composite-Objekts
	 */
	public AbstractComplex clone() {

		CompositeComplex newComposite = (CompositeComplex) cloneConcreteComponent();
		newComposite.create();

		for (int i = 0; i < mComponents.size(); i++) {
			newComposite.addComplex(mComponents.get(i).clone());
		}
		return newComposite;

	}

	// ------------------------------------------------------------------------------------------

	@Override
	/**
	 * Methode erzeugt ein leeres Composite-Objekt, das dann waehrend eines Clonings mit Kopien der Elemente befuellt wird
	 */
	protected AbstractComplex cloneConcreteComponent() {
		return new CompositeComplex(mParent);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode leitet den Tesselate-Call an die Komponenten des
	 * Composite-Objekts weiter
	 */
	public void tesselate() {
		for (int i = 0; i < mComponents.size(); i++)
			mComponents.get(i).tesselate();
	}

	// ------------------------------------------------------------------------------------------
	@Override
	/**
	 * Methode liefert die Summe der Vertices aller Subkomponenten
	 */
	public int getVertexCount() {

		int sum = 0;
		List<AbstractComplex> components = getComponents();
		for (int i = 0; i < components.size(); i++)
			sum += components.get(i).getVertexCount();
		return sum;
	}
	// ------------------------------------------------------------------------------------------

}
