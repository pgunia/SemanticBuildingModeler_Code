package semantic.building.modeler.prototype.graphics.complex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.media.opengl.GL;

import org.apache.log4j.Logger;

import processing.core.PApplet;
import processing.core.PConstants;
import semantic.building.modeler.configurationservice.model.enums.FloorPosition;
import semantic.building.modeler.configurationservice.model.enums.Side;
import semantic.building.modeler.math.Axis;
import semantic.building.modeler.math.MyPolygon;
import semantic.building.modeler.math.MyVector2f;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.MyVectormath;
import semantic.building.modeler.math.Plane;
import semantic.building.modeler.math.Ray;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.prototype.enums.QuadType;
import semantic.building.modeler.prototype.enums.subdivisionType;
import semantic.building.modeler.prototype.exception.PrototypeException;
import semantic.building.modeler.prototype.exporter.AbstractModelExport;
import semantic.building.modeler.prototype.exporter.ExportFormat;
import semantic.building.modeler.prototype.exporter.ObjExport;
import semantic.building.modeler.prototype.graphics.interfaces.iElement;
import semantic.building.modeler.prototype.graphics.interfaces.iGraphicComplex;
import semantic.building.modeler.prototype.graphics.interfaces.iGraphicPrimitive;
import semantic.building.modeler.prototype.graphics.primitives.AbstractQuad;
import semantic.building.modeler.prototype.graphics.primitives.PolygonalQuad;
import semantic.building.modeler.prototype.graphics.primitives.Quad;
import semantic.building.modeler.prototype.graphics.primitives.Triangle;
import semantic.building.modeler.prototype.roof.configuration.FixedRoofWeightConfiguration;
import semantic.building.modeler.prototype.roof.configuration.RandomRoofWeightConfiguration;
import semantic.building.modeler.prototype.service.EdgeManager;
import semantic.building.modeler.prototype.service.IdentifierService;
import semantic.building.modeler.prototype.service.ObjectManagementService;
import semantic.building.modeler.prototype.service.PrototypeHelper;
import semantic.building.modeler.prototype.service.Texture;
import semantic.building.modeler.prototype.service.TextureManagement;
import semantic.building.modeler.prototype.service.TextureManagement.TextureCategory;
import semantic.building.modeler.weightedstraightskeleton.algorithm.SkeletonRoofDescriptor;
import semantic.building.modeler.weightedstraightskeleton.controller.StraightSkeletonController;
import semantic.building.modeler.weightedstraightskeleton.exception.AccuracyException;
import semantic.building.modeler.weightedstraightskeleton.exception.SquareCaseException;

/**
 * 
 * @author Patrick Gunia Abstrakte Basisklasse aller komplexen Objekte innerhalb
 *         des Systems
 * 
 */

public abstract class AbstractComplex implements iElement, iGraphicComplex,
		Cloneable {

	/** Logger */
	protected static Logger LOGGER = Logger.getLogger(AbstractComplex.class);

	/**
	 * Muss von jeder Subklasse ueberschrieben werden, erstellt eine
	 * Clone-Instanz des jeweiligen Objekts, Schablonenmethode, die von der
	 * Haupt-Clone-Methode aufgerufen wird
	 */
	abstract protected AbstractComplex cloneConcreteComponent();

	/**
	 * Instanz der Processing-PApplet-Klasse, dient der Umsetzung direkter
	 * -Calls
	 */
	protected PApplet mParent = null;

	/** Vector mit allen Vertices, aus denen das Objekt besteht */
	protected List<Vertex3d> mVertices = null;

	/** ID des komplexen Objekts */
	protected String msID = "";

	/** Positionsvektor des komplexen Objekts */
	protected MyVector3f mPosition = null;

	/**
	 * Kantenverwaltungsinstanz fuer saemtliche Kanten, aus denen das komplexe
	 * Objekt besteht
	 */
	protected EdgeManager mEdges = null;

	/** Liste mit allen Aussen-Quads, aus denen das komplexe Objekt besteht */
	protected List<AbstractQuad> mOutdoorQuads = null;

	/** Liste mit allen Innen-Quads aus denen das komplexe Objekt besteht */
	protected List<AbstractQuad> mIndoorQuads = null;

	/**
	 * Liste enthaelt alle Quads des komplexen Objekts, wird aus
	 * Performancegruenden beim Zeichnen gepflegt
	 */
	protected List<AbstractQuad> mAllQuads = null;

	/** HashMap speichert Textur-zu-ID- bzw. Textur-zu-Kategorie-Zuordnungen */
	protected Map<String, Texture> mTextures = null;

	/**
	 * Flag speichert, ob das Objekt Wurzel in einer potentiellen
	 * Composite-Hierarchie ist, oder nicht
	 */
	protected boolean mIsRoot = true;

	/** Hoehe des komplexen Objekts */
	protected Float mHeight = null;

	/** Dach des komplexen Objekts */
	protected RoofComplex mRoof = null;

	/**
	 * Flag gibt an, ob bereits Texturkoordinaten fuer das Objekt berechnet
	 * wurden
	 */
	protected boolean mHasTextureCoordinates = false;

	/**
	 * Flag gibt an, ob die Texturkoordinaten fuer das komplexe Objekt und all
	 * seine Subobjekte bereits skaliert wurden
	 */
	protected boolean mScaledTextureCoords = false;

	/** Grundflaeche des Koerpers, beschrieben durch ein Polygon */
	protected MyPolygon mFootprint = null;

	/**
	 * Map speichert feste Zuordnungen von Normalenvektoren zu bestimmten
	 * Ausrichtungen, die durch die Side angegeben sind
	 */
	protected Map<MyVector3f, Side> mNormalToDirectionMap = null;

	/** Achsenausgerichtete Boundingbox */
	protected BoundingBox mBB = null;

	/**
	 * Objektausgerichtete BoundingBox, wird nur in bestimmten Faellen verwendet
	 */
	protected OBB mOBB = null;

	/** Flag gibt an, ob die OBBs gezeichnet werden sollen */
	protected Boolean mDrawOBB = false;

	/**
	 * Komplexes Parent-Objekt in der Objekthierarchie, kann Component oder
	 * Composite sein
	 */
	protected AbstractComplex mComplexParent = null;

	// ------------------------------------------------------------------------------------------
	/**
	 * 
	 * @author Patrick Gunia Enum dient der Zustandsuebwerwachung bei der
	 *         Vertexerzeugung
	 * 
	 */
	protected enum CreationState {
		UPPER_START, UPPER_END, LOWER_START, LOWER_END;
	}

	// ------------------------------------------------------------------------------------------

	public AbstractComplex() {
		init();
	}

	// ------------------------------------------------------------------------------------------
	public AbstractComplex(PApplet parent) {
		this.mParent = parent;
		init();
	}

	// ------------------------------------------------------------------------------------------

	public AbstractComplex(PApplet parent, Float height) {
		this.mParent = parent;
		this.mHeight = height;
		init();
	}

	// ------------------------------------------------------------------------------------------
	private void init() {
		this.mPosition = new MyVector3f(0, 0, 0);
		this.mEdges = new EdgeManager(this);
		mTextures = new HashMap<String, Texture>();
		mOutdoorQuads = new ArrayList<AbstractQuad>();
		mIndoorQuads = new ArrayList<AbstractQuad>();
		mVertices = new ArrayList<Vertex3d>();

		// baue die Normal-To-Direction-Map auf
		mNormalToDirectionMap = new HashMap<MyVector3f, Side>(6);

		mNormalToDirectionMap.put(new MyVector3f(0.0f, -1.0f, 0.0f), Side.TOP);
		mNormalToDirectionMap
				.put(new MyVector3f(0.0f, 1.0f, 0.0f), Side.BOTTOM);
		mNormalToDirectionMap.put(new MyVector3f(-1.0f, 0.0f, 0.0f), Side.LEFT);
		mNormalToDirectionMap.put(new MyVector3f(1.0f, 0.0f, 0.0f), Side.RIGHT);
		mNormalToDirectionMap.put(new MyVector3f(0.0f, 0.0f, 1.0f), Side.FRONT);
		mNormalToDirectionMap.put(new MyVector3f(0.0f, 0.0f, -1.0f), Side.BACK);
	}

	// ------------------------------------------------------------------------------------------
	@Override
	public AbstractComplex clone() {

		// erstelle eine geklonte Instanz der jeweiligen Subklasse => ruft deren
		// Konstruktor auf
		AbstractComplex newObject = cloneConcreteComponent();

		// klone den Edge-Manager des komplexen Objekts
		newObject.setEdgeManager(mEdges.clone());
		newObject.getEdgeManager().get(0).setParent(newObject);

		// kopiere alle Vertices in das neue Objekt
		for (int i = 0; i < mVertices.size(); i++) {
			Vertex3d tempVert = mVertices.get(i);
			newObject.addVertex(tempVert.clone());
		}

		// kopiere alle Quads in das neue Objekt
		// aktualisiere direkt Complex- und Standard-Parent-Beziehungen
		for (int i = 0; i < mOutdoorQuads.size(); i++) {
			AbstractQuad tempQuad = mOutdoorQuads.get(i);
			newObject.addOutdoorQuad(tempQuad.clone(newObject));
		}

		if (mBB != null) {
			BoundingBox obb = (BoundingBox) mBB.clone();
			newObject.setBB(obb);
		}

		// Quad-Updates, Anmeldung etc.
		newObject.finalizeCreation();

		LOGGER.debug(newObject.getType() + "-Kopie abgeschlossen");
		return newObject;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * @return Vertexbuffer des komplexen Objekts
	 */
	public List<Vertex3d> getVertices() {
		return mVertices;
	}

	// ------------------------------------------------------------------------------------------

	public void setVertices(final List<Vertex3d> mVertices) {
		this.mVertices = mVertices;
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public String getID() {

		if (msID.isEmpty())
			return getType();
		else
			return msID;
	}

	// ------------------------------------------------------------------------------------------

	@Override
	/**
	 * Methode speichert die uebergebene ID im Objekt
	 * @param msID ID des aktuellen Objekts
	 */
	public void setID(String msID) {
		this.msID = msID;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * @return Positionsvektor des komplexen Objekts
	 */
	public MyVector3f getPositionPtr() {
		return mPosition;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * @return Kopie des Positionsvektors des Objekts
	 */
	public MyVector3f getPosition() {
		assert mPosition != null : "Kein Positionsvektor gesetzt";
		return mPosition.clone();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode liefert die Hoehe des abstrakten Objekts
	 * 
	 * @return Objekthoehe
	 */
	public Float getHeight() {
		return mHeight;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * @return PApplet-Instanz, ueber die das komplexe Objekt gezeichnet wird
	 */
	public PApplet getParent() {
		return mParent;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * @return Mapstruktur mit Zuordnungen von IDs zu Texturen
	 */
	public Map<String, Texture> getTextureMap() {
		return mTextures;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mScaledTextureCoords
	 */
	public boolean isScaledTextureCoord() {
		return mScaledTextureCoords;
	}

	// ------------------------------------------------------------------------------------------
	public void setIsScaledTextureCoord(Boolean isScaled) {
		mScaledTextureCoords = isScaled;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode speichert den uebergebenen Positionsvektor als Position des
	 * komplexen Objekts.
	 * 
	 * @param initialPosition
	 *            Positionsvektor, der als Position des aktuellen Objekts
	 *            gesetzt wird
	 */
	public void setInitialPosition(MyVector3f initialPosition) {
		this.mPosition = initialPosition;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode verschiebt alle Vertices relativ zu ihrer Ausgangsposition an die
	 * uebergebene Position => es handelt sich also um eine
	 * Translationsberechnung in Bezug auf den Start und nicht in Bezug auf
	 * absolute Koordinaten (sprich: die uebergebene Position ist nur dann die
	 * tatsaechliche Endposition, wenn sich das Objekt vorher im Ursprung
	 * befand)
	 * 
	 * @param mPosition
	 *            Translationsvektor, ueber den die Vertices verschoben werden
	 */
	public void translate(MyVector3f mPosition) {
		translateAllVerticesToNewPosition(mPosition);

		MyVector3f oldPosition = this.mPosition.clone();
		this.mPosition = new MyVector3f();
		this.mPosition.add(oldPosition, mPosition);

		// wenn eine OBB berechnet wurde, verschiebe diese ebenfalls
		if (mBB != null)
			mBB.translate(mPosition);

		// aktualisiere alle Subelemente des komplexen Basisobjekts
		this.update();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode verschiebt alle Vertices zurueck an ihre Ausgangsposition im
	 * Ursprung des Weltkoordinatensystems
	 */
	public void resetPositionToOrigin() {
		MyVector3f originVector = new MyVector3f(0.0f, 0.0f, 0.0f);
		translateAllVerticesToNewPosition(originVector);
		this.update();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Wenn fuer ein Objekt eine neue Position gesetzt wird, verschiebt man
	 * saemtliche Vertices zunaechst anhand des vorher gespeicherten
	 * Positionsvektors in ihre Ursprungsposition und fuehrt danach eine
	 * Translation in die neue Position durch. Dadurch ist eine Translation
	 * immer relativ zu den Ursprungskoordinaten und nicht zur vorherigen
	 * Position.
	 * 
	 * @param newPosition
	 *            Zielposition, an die das Objekt verschoben werden soll
	 */
	protected void translateAllVerticesToNewPosition(MyVector3f newPosition) {

		// System.out.println("Verschiebe Vertices fuer Cube " + getID() +
		// " von Position: " + mPosition + " nach: " + newPosition);
		for (int i = 0; i < mVertices.size(); i++)
			mVertices.get(i).getPositionPtr().add(newPosition);
	}

	// ------------------------------------------------------------------------------------------

	protected void addVertex(Vertex3d vertex) {
		if (mVertices == null)
			mVertices = new Vector<Vertex3d>();
		mVertices.add(vertex);

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode liefert alle Edge-Manager des komplexen Objekts. Bei
	 * Nicht-Composite-Objekten liegt nur ein Edge-Manager pro Objekt vor, bei
	 * Composite-Objekten werden dagegen die Edge-Manager aller Kindobjekte
	 * durch die vollstaendige Objekthierarchie zurueckgereicht.
	 * 
	 * @return Liste mit allen EdgeManagern des komplexen Objekts
	 */
	public List<EdgeManager> getEdgeManager() {
		if (mEdges == null) {
			new PrototypeException(
					"AbstractComplex.getEdgeManager: Kein Edge-Manager gesetzt");
		}
		// bei nicht Composite-Instanzen gibt es immer nur einen EdgeManager
		final List<EdgeManager> result = new ArrayList<EdgeManager>(1);
		result.add(mEdges);
		return result;
	}

	// ------------------------------------------------------------------------------------------

	public void setEdgeManager(EdgeManager manager) {
		mEdges = manager;
	}

	// ------------------------------------------------------------------------------------------
	public List<AbstractQuad> getOutdoorQuads() {
		return getQuads(QuadType.OUTDOOR);
	}

	// ------------------------------------------------------------------------------------------

	public void setOutdoorQuads(List<AbstractQuad> mQuads) {
		this.mOutdoorQuads = mQuads;
	}

	// ------------------------------------------------------------------------------------------
	public void addOutdoorQuad(AbstractQuad quad) {
		addQuad(quad, QuadType.OUTDOOR);

	}

	// ------------------------------------------------------------------------------------------

	public List<AbstractQuad> getIndoorQuads() {
		return getQuads(QuadType.INDOOR);
	}

	// ------------------------------------------------------------------------------------------

	public void setIndoorQuads(List<AbstractQuad> mQuads) {
		this.mIndoorQuads = mQuads;
	}

	// ------------------------------------------------------------------------------------------
	public void addIndoorQuad(AbstractQuad quad) {
		addQuad(quad, QuadType.INDOOR);
	}

	// ------------------------------------------------------------------------------------------
	public List<AbstractQuad> getAllQuads() {
		return getQuads(QuadType.ALL);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Fuegt ein Quad zur Liste mit dem uebergebenen Typ hinzu
	 * 
	 * @param quad
	 *            Hinzuzufuegendes Quad
	 * @param type
	 *            Typ des Quads
	 */
	protected void addQuad(AbstractQuad quad, QuadType type) {

		switch (type) {
		case INDOOR:
			if (mIndoorQuads == null)
				mIndoorQuads = new ArrayList<AbstractQuad>();
			mIndoorQuads.add(quad);
			break;
		case OUTDOOR:
			if (mOutdoorQuads == null)
				mOutdoorQuads = new ArrayList<AbstractQuad>();
			mOutdoorQuads.add(quad);
			break;
		default:
			break;
		}
		// quad.generateID(getID(), String.valueOf(mOutdoorQuads.size() +
		// mIndoorQuads.size()));
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Liefert alle Quads, die dem Uebergabetyp entsprechen
	 * 
	 * @param type
	 *            Typ der zu liefernden Quads
	 * @return Liste mit Quads, die dem Uebergabetyp entsprechen
	 */
	protected List<AbstractQuad> getQuads(QuadType type) {
		switch (type) {
		case OUTDOOR:
			return mOutdoorQuads;
		case INDOOR:
			return mIndoorQuads;
		case ALL:
			if (mAllQuads == null)
				mAllQuads = new ArrayList<AbstractQuad>(mIndoorQuads.size()
						+ mOutdoorQuads.size());
			if (mAllQuads.size() != mOutdoorQuads.size() + mIndoorQuads.size()) {
				mAllQuads.clear();
				mAllQuads.addAll(mIndoorQuads);
				mAllQuads.addAll(mOutdoorQuads);
			}
			return mAllQuads;
		default:
			LOGGER.error("Unbekannter Quad-Typ: " + type);
			return null;
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode wird von komplexen Objekten aufgerufen, nachdem deren Geometrie
	 * vollstaendig festgelegt wurde und alle Primitiven Subobjekte erzeugt
	 * wurden. Sie initiiert Berechnungen auf der Ebene der primitiven Objekte,
	 * erzeugt eine ID fuer alle Bestandteile und registriert alle Komponenten
	 * bei der Objektverwaltung
	 */
	protected void finalizeCreation() {

		// berechne Mittelpunkte, Normalen etc. fuer alle berechneten faces
		update();

		// Texturkoordinaten berechnen
		// computeTextureCoordinates();

		// erzeuge IDs fuer das Objekt und alle Kindelemente
		generateID(null, null);

		// registriere das erzeugte Objekt bei der Objektverwaltung
		register();

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode zeichnet die Normalenvektoren eines Grafikelements (bsw. Quad
	 * oder Triangle)
	 */
	public void drawNormals(iGraphicPrimitive element) {
		MyVector3f center = element.getCenter();
		MyVector3f normal = element.getNormal();
		normal.scale(20);

		mParent.pushStyle();
		mParent.beginShape(PConstants.LINE);

		mParent.strokeWeight(2);

		// zeichne nun die Normalenvektoren des Faces
		mParent.stroke(255, 0, 0);
		mParent.vertex(center.x, center.y, center.z);
		mParent.vertex(center.x + normal.x, center.y + normal.y, center.z
				+ normal.z);

		mParent.endShape();
		mParent.popStyle();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode zeichnet ein lokales Koordinatensystem fuer das jeweilige Objekt
	 * wenn mehrere Objekte in der Szene vorkommen, koennen auch mehrere
	 * Koordinatensysteme gezeichnet werden
	 */

	protected void drawCoordinateSystem() {

		float length = 1000000;

		mParent.beginShape(PConstants.LINES);

		// x- Achse in schwarz
		mParent.stroke(0);
		mParent.vertex(-length, 0, 0);
		mParent.vertex(length, 0, 0);

		// y-Achse in rot
		mParent.stroke(255, 0, 0);
		mParent.vertex(0, -length, 0);
		mParent.vertex(0, length, 0);

		// z-Achse in gruen
		mParent.stroke(0, 255, 0);
		mParent.vertex(0, 0, length);
		mParent.vertex(0, 0, -length);

		mParent.endShape();
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public String toString() {
		String message = getType() + ": ID: " + getID() + " Position: "
				+ getPosition();
		return message;
	}

	// -----------------------------------------------------------------------------------------
	/**
	 * Methode durchlaeuft alle Quad-Elemente des komplexen Objekts und ruft
	 * deren Update-Methoden auf. Dies fuehrt zu einer Kette von Updates, da die
	 * Quads selber wiederum weitere Updates initiieren etc.
	 */
	public void update() {

		// Update der BB
		if (mBB != null) {
			mBB.update();
		}

		// Update der Quads
		for (int i = 0; i < mOutdoorQuads.size(); i++) {
			mOutdoorQuads.get(i).update();
		}

		// Footprint aktualisieren, Ebene, Rays etc.
		if (mFootprint != null) {
			mFootprint.update();
		}
	}

	// -----------------------------------------------------------------------------------------
	/**
	 * leere Default-Implementation => saemtliche Objekte, die texturiert werden sollen,
	 * muessen diese Methode ueberschreiben
	 */
	@Override
	public void setTexture(String category, Texture texture) {

		
	}

	// -----------------------------------------------------------------------------------------
	/**
	 * Methode fuegt die uebergebene Textur mir dem uebergebenen Schluessel zur
	 * HashMap hinzu
	 * 
	 * @param key
	 *            Schluessel, unter dem die Textur in der HashMap abgelegt wird,
	 *            kann sowohl eine ID als auch eine Kategorie sein
	 * @param texture
	 *            Texturobjekt, das dem Element mit dem jeweiligen Schluessel
	 *            zugeordnet wird
	 */
	public void addTextureToMap(String key, Texture texture) {

		// wurde einem Objekt bereits eine Textur zugewiesen, ueberspringe es
		if (mTextures.get(key) != null)
			return;
		else
			mTextures.put(key, texture);

	}

	// -----------------------------------------------------------------------------------------
	/**
	 * Methode reicht den ueber alle Faces berechneten Scalingfaktor durch die
	 * Hierarchie zurueck bis auf die Ebene der Quads. Deren Texturkoordinaten
	 * werden anschliessend mit dem Scaling-Faktor skaliert, um eine ueber alle
	 * Faces einheitliche Texturgroesse zu erreichen.
	 */
	public void scaleTextureCoordinates(float minScaleFaktor) {

		// wenn die Komponente bereits skaliert wurde, breche ab
		if (isScaledTextureCoord())
			return;
		Iterator<AbstractQuad> quadIter = mOutdoorQuads.iterator();
		LOGGER.trace("Scaling Texturecoords for: " + getID() + " Factor: "
				+ minScaleFaktor);
		while (quadIter.hasNext()) {
			quadIter.next().scaleTextureCoordinates(minScaleFaktor);
		}

		// Flag setzen
		mScaledTextureCoords = true;
	}

	// -----------------------------------------------------------------------------------------

	/**
	 * Cube ist ein Abstract-Primitive und somit Root der ID-Vergabekette hier
	 * besteht die Moeglichkeit, auch eine manuelle ID-Festlegung fuer den
	 * statischen Teil durchzufuehren, ansonsten wird eine ID generiert
	 */
	@Override
	public void generateID(String baseID, String concat) {

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

		// rufe nun die ID-Generierungsmethoden der Kinder auf
		int quadCounter = 0;
		Iterator<AbstractQuad> quadIter = mOutdoorQuads.iterator();
		while (quadIter.hasNext()) {
			// rufe die Methode mit der jeweiligen Base auf und uebergebe den
			// Counter bzgl. der Nummer des Kindes
			quadIter.next().generateID(id, String.valueOf(quadCounter));
			quadCounter++;
		}
	}

	// -----------------------------------------------------------------------------------------

	/**
	 * Fuege das Objekt zur Objektverwaltung hinzu
	 */
	@Override
	public void register() {
		ObjectManagementService.getInstance().addObjectToManagement(this);

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Entferne das Objekt aus der Objektverwaltung
	 */
	@Override
	public void unregister() {
		ObjectManagementService.getInstance().removeObjectFromManagement(
				getID());
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode zeichnet das komplexe Objekt unter Verwendung des direkten
	 * OpenGL-Renderkontext. Dadurch erhoeht sich die Renderperformance deutlich
	 * 
	 * @param drawTextures
	 *            Flag gibt an, ob das Objekt texturiert werden soll
	 * @param gl
	 *            OpenGL-Device-Kontext
	 */
	public void drawGL(Boolean drawTextures, GL gl) {
		Texture texture = null;

		Vertex3d tempVertex;
		Integer[] triangleIndices;

		if (drawTextures)
			gl.glEnable(GL.GL_TEXTURE_2D);
		else
			gl.glDisable(GL.GL_TEXTURE_2D);
		// Textur ersetzt das aktuelle Material
		gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_REPLACE);

		// sorge fuer eine aktuelle Quadliste
		getAllQuads();
		final AbstractQuad[] quads = (AbstractQuad[]) mAllQuads
				.toArray(new AbstractQuad[mAllQuads.size()]);

		Triangle[] triangles = null;
		Triangle tempTriangle = null;

		
		// HIER IS DER TEXTURFEHLER! NICHT ALLE MIT TEXTUR ZEICHNEN
		int numberOfQuadsToDraw = mAllQuads.size();
		int numberOfTrianglesToDraw = 0;

		List<Triangle> childTriangles = null;
		float normalizedStandardColor = 100 / 255;

		// schelle Iteration => Runterzaehlen, Arrays, kein Iterator
		for (int i = numberOfQuadsToDraw - 1; i >= 0; i--) {
			final AbstractQuad tempQuad = quads[i];

			// pruefe, ob fuer das aktuelle Quad eine Texturzuordnung vorliegt
			if (drawTextures) {
				texture = mTextures.get(tempQuad.getID());
			}
			
			MyVector2f textureCoords = null;
			childTriangles = tempQuad.getChildTriangles();

			triangles = (Triangle[]) childTriangles
					.toArray(new Triangle[childTriangles.size()]);
			numberOfTrianglesToDraw = triangles.length;

			for (int j = numberOfTrianglesToDraw - 1; j >= 0; j--) {
				tempTriangle = triangles[j];
				triangleIndices = tempTriangle.getIndices();

				// zeichne die Dreicke in unterschiedlichen Farben
				float triangleDrawColor = tempTriangle.getDrawColor();
				float normalizedColor = triangleDrawColor / 255;

				// wenn eine Textur verwendet werden soll, aktiviere diese
				// Texturobjekt ueber OGL laden
				if (texture != null && mHasTextureCoordinates) {
					gl.glBindTexture(GL.GL_TEXTURE_2D, texture.getGLTextureID());
				}
				gl.glBegin(GL.GL_TRIANGLES);
				gl.glColor3f(normalizedColor, normalizedStandardColor,
						normalizedColor);

				for (int k = 0; k < triangleIndices.length; k++) {
					tempVertex = mVertices.get(triangleIndices[k]);
					if (texture != null && mHasTextureCoordinates) {
						textureCoords = tempQuad
								.getTextureCoordsByIndex(triangleIndices[k]);
						gl.glTexCoord2f(textureCoords.x, textureCoords.y);
						gl.glVertex3f(tempVertex.getX(), tempVertex.getY(),
								tempVertex.getZ());
					} else {
						gl.glVertex3f(tempVertex.getX(), tempVertex.getY(),
								tempVertex.getZ());
					}
				}
				gl.glEnd();
			}
		}

		// wenn eine OBB berechnet wurde, zeichne diese
		if (mBB != null && mDrawOBB)
			mBB.drawGL(gl);
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public void draw(Boolean drawTextures) {
		Vertex3d tempVertex, tempVertex2;
		Integer[] triangleIndices;

		getAllQuads();
		AbstractQuad[] quads = (AbstractQuad[]) mAllQuads
				.toArray(new AbstractQuad[mAllQuads.size()]);

		// zeichne das Koordinatensystem
		/*
		 * mParent.pushMatrix(); mParent.translate(mPosition.x, mPosition.y,
		 * mPosition.z); drawCoordinateSystem(); mParent.popMatrix();
		 */

		String text = "";

		mParent.noStroke();
		boolean textureN = false;
		Texture texture = null;

		Triangle[] triangles = null;
		Triangle tempTriangle = null;

		int numberOfQuadsToDraw = mAllQuads.size();
		int numberOfTrianglesToDraw = 0;

		List<Triangle> childTriangles = null;

		// schelle Iteration => Runterzaehlen, Arrays, kein Iterator
		for (int i = numberOfQuadsToDraw - 1; i >= 0; i--) {
			AbstractQuad tempQuad = quads[i];

			// pruefe, ob fuer das aktuelle Quad eine Texturzuordnung vorliegt
			if (drawTextures)
				texture = mTextures.get(tempQuad.getID());

			MyVector2f textureCoords = null;
			childTriangles = tempQuad.getChildTriangles();

			triangles = (Triangle[]) childTriangles
					.toArray(new Triangle[childTriangles.size()]);
			numberOfTrianglesToDraw = triangles.length;

			/*
			 * if(drawTextures) { System.out.println("QUAD-Ausrichtung: " +
			 * tempQuad.getDirection()); for(int i = 0; i <
			 * tempQuad.getIndices().length; i++) { System.out.println("INDEX: "
			 * + tempQuad.getIndices()[i] + " COORDS: " +
			 * tempQuad.getTextureCoordsByIndex(tempQuad.getIndices()[i])); } }
			 */

			// Normalenvektoren des Quads zeichnen
			drawNormals(tempQuad);

			for (int j = numberOfTrianglesToDraw - 1; j >= 0; j--) {
				tempTriangle = triangles[j];
				triangleIndices = tempTriangle.getIndices();
				// zeichne die Dreicke in unterschiedlichen Farben
				float triangleDrawColor = tempTriangle.getDrawColor();

				// zeichne keine Outlines
				// mParent.noStroke();
				// mParent.strokeWeight(1);
				// mParent.stroke(0, 0, 0);

				mParent.fill(triangleDrawColor, 100, triangleDrawColor, 255);
				;
				mParent.beginShape(PConstants.TRIANGLES);
				// wenn eine Textur verwendet werden soll, aktiviere diese
				if (texture != null && mHasTextureCoordinates) {
					mParent.texture(texture.getTexture());
					mParent.textureMode(PConstants.NORMALIZED);
				}

				for (int k = 0; k < triangleIndices.length; k++) {
					tempVertex = mVertices.get(triangleIndices[k]);
					if (texture != null && mHasTextureCoordinates) {
						textureN = true;
						textureCoords = tempQuad
								.getTextureCoordsByIndex(triangleIndices[k]);
						// assert textureCoords.x >= 0.0f && textureCoords.x <=
						// 1.0f && textureCoords.y >= 0.0f && textureCoords.y <=
						// 1.0f: "FEHLER: Texturekoordinaten OoR";

						// System.out.println("INDEX: " + triangleIndices[i] +
						// " VERTEX: " + tempVertex + " COORDS: " +
						// textureCoords);
						mParent.vertex(tempVertex.getX(), tempVertex.getY(),
								tempVertex.getZ(), textureCoords.x,
								textureCoords.y);
						// mParent.noLoop();
					} else {
						mParent.vertex(tempVertex.getX(), tempVertex.getY(),
								tempVertex.getZ());
					}
				}
				mParent.endShape();
				// Dreiecksnormalen zeichnen
				// drawNormals(tempTriangle);
			}
		}

		// wenn eine OBB berechnet wurde, zeichne diese
		if (mBB != null && mDrawOBB)
			mBB.draw(mParent);

		// if(textureN) mParent.noLoop();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet Daecher fuer Cube- und Cylinder-Instanzen. Hierzu
	 * waehlt sie das Quad mit Ausrichtung "TOP" aus und extrahiert dessen
	 * Vertices. Diese dienen anschliessend als Eingabe in den
	 * Straight-Skeleton-Algorithmus
	 */

	@Override
	public void computeRoof() {

		// suche das Quad mit Ausrichtung "Top"
		AbstractQuad top = null, currentQuad = null;
		Iterator<AbstractQuad> quadIter = mOutdoorQuads.iterator();
		while (quadIter.hasNext()) {
			currentQuad = quadIter.next();
			if (currentQuad.getDirection().equals(Side.TOP)) {
				top = currentQuad;
				break;
			}
		}

		assert top != null : "Es konnte kein Quad mit der Ausrichtung 'TOP' gefunden werden";

		// extrahiere die Koordinaten der Quad-Vertices
		List<Vertex3d> quadVerts = new ArrayList<Vertex3d>();
		Integer[] indices = top.getIndices();
		Vertex3d current = null;
		int startIndex = 0;

		List<Float> edgeWeights = new ArrayList<Float>();
		Integer firstIndex = null, secondIndex = null;

		final FixedRoofWeightConfiguration weightManager = new RandomRoofWeightConfiguration();
		weightManager.setCurrentEdgeManager(mEdges);

		for (int i = startIndex; i < indices.length; i++) {

			// greife ueber die Kantenindices auf den Edge-Manager zu
			// hole alle Referenzen von anderen Quads auf diesen Kante
			current = top.getVertices().get(indices[i]);
			firstIndex = indices[i];
			if (i + 1 < indices.length)
				secondIndex = indices[i + 1];
			else
				secondIndex = indices[0];

			edgeWeights.add(weightManager.getWeightByEdgeIndices(firstIndex,
					secondIndex));
			quadVerts.add(current);
		}

		Texture texture = null;
		if (mTextures.containsKey("Roof")) {
			texture = mTextures.get("Roof");
		} else
			texture = TextureManagement.getInstance().getTextureForCategory(
					TextureCategory.Roof);

		SkeletonRoofDescriptor roofConfig = new SkeletonRoofDescriptor();
		roofConfig.setVertices(quadVerts);
		roofConfig.setTexture(texture);
		roofConfig.setEdgeWeights(edgeWeights);
		roofConfig.setMainWeight(weightManager.getStandardWeight());
		roofConfig.setSideWeight(weightManager.getSideWeight());

		if (quadVerts.size() > 0) {
			try {
				StraightSkeletonController roofController = new StraightSkeletonController(
						roofConfig);
				// erzeuge ein Roof-Complex aus den Ergebnissen der Berechnung
				mRoof = new RoofComplex(mParent, roofController, roofConfig);
				mRoof.create();
				// mRoof.computeTextureCoordinates();
				mRoof.setTexture("Roof", texture);

			} catch (AccuracyException e) {
				e.printStackTrace();
			} catch (SquareCaseException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			} catch (AssertionError e) {
				e.printStackTrace();
			}
		}
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode gibt Texturen fuer komplexe Objekte frei, sobald sie nicht mehr
	 * benoetigt werden
	 * 
	 * @param category
	 *            Kategorie der Textur
	 */
	protected void releaseTextureByCategory(String category) {

		Texture texture = mTextures.get(category);
		if (texture != null) {
			texture.removeReference();
			mTextures.remove(category);
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet Texturkoordinaten fuer alle Quads des Objekts Idee ist
	 * die Projektion der Quads in die XY-Ebene, anschliessend verschiebt man
	 * alle Vertices ueber Translationen derart, dass einzelne Vertices auf den
	 * Achsen des xy-Koordinatensystems liegen
	 * 
	 * @return Der errechnete Skalierungsfaktor fuer alle Quads des komplexen
	 *         Objekts
	 */
	public Float computeTextureCoordinates() {
		LOGGER.debug("Computing Texturcoordinates for: " + getID());
		getAllQuads();

		Iterator<AbstractQuad> quadIter = mAllQuads.iterator();
		AbstractQuad currentQuad = null;

		// alle Quads durchlaufen, unskalierte Koordinaten ermitteln
		while (quadIter.hasNext()) {
			currentQuad = quadIter.next();
			computeUnscaledTextureCoordinatesForQuad(currentQuad);
		}

		// Flag setzen
		mScaledTextureCoords = false;

		Float maxValue = -Float.MAX_VALUE;

		// jetzt alle errechneten Texturkoordinaten durchlaufen, und den
		// maximalen Wert ermitteln (unabhaengig, ob in x- oder y-Achse
		// mit diesem Wert werden dann alle Koordinaten skaliert
		Collection<MyVector2f> coords = null;
		Iterator<MyVector2f> coordIter = null;
		MyVector2f currentCoord = null;

		quadIter = mAllQuads.iterator();
		while (quadIter.hasNext()) {
			currentQuad = quadIter.next();
			coords = currentQuad.getTextureCoords().values();

			coordIter = coords.iterator();
			while (coordIter.hasNext()) {
				currentCoord = coordIter.next();
				if (currentCoord.x > maxValue)
					maxValue = currentCoord.x;
				if (currentCoord.y > maxValue)
					maxValue = currentCoord.y;
			}
		}

		// Skaliere nun alle Koordinaten mit dem ermittelten Skalierungsfaktor
		Float scaleFactor = 1 / maxValue;

		// nur, wenn das Objekt Root einer Hierarchie ist, direkt skalieren,
		// sonst erfolgt der Call von aussen
		if (isRoot()) {
			scaleTextureCoordinates(scaleFactor);
		}

		// Flag setzen
		mHasTextureCoordinates = true;

		return scaleFactor;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erzeugt eine 2D-Projektion in die XY-Koordinatenebene. Dann
	 * verschiebt man die Punkte derart, dass die Extrempunkte bzgl. x- und
	 * y-Achse auf den Koordinatenachsen liegen. Die Verschiebungsberechnungen
	 * sind dabei zunaechst unabhaengig von der Grundebene, erst bei der
	 * Zuweisung der Koordinaten als 2d-Vektoren spielt die Eingabeebene wieder
	 * eine Rolle, da abhaengig von der Ausrichtung die u- und v-Achsen anders
	 * verwendet werden muessen. Die anfaengliche Reduktion auf die XY-Ebene
	 * dient dabei nur einer einheitlichen Verarbeitung
	 * 
	 * @param quad
	 *            Quad, fuer dessen Vertices zunaechst unskalierte Koordinaten
	 *            >= 0 errechnet werden
	 */
	protected void computeUnscaledTextureCoordinatesForQuad(AbstractQuad quad) {

		LOGGER.trace("Berechne Texturkoordinaten fuer Quad mit Ausrichtung: "
				+ quad.getDirection() + " Normal: " + quad.getNormal());

		// hole alle Indices des Quads, beruecksichtige dabei auch die
		// Triangle-Indices
		Set<Integer> indices = quad.getAllIndices();

		List<Vertex3d> quadVerts = new ArrayList<Vertex3d>(indices.size());
		Vertex3d currentVertex = null;

		Plane quadPlane = quad.getPlane();
		assert quadPlane != null : "FEHLER: Keine Ebene fuer das Quad berechnet";
		MyVectormath mathHelper = MyVectormath.getInstance();
		Axis ignorableAxis = mathHelper.getIgnorableAxis(quadPlane.getNormal(),
				false);

		Integer currentIndex = null;

		Iterator<Integer> indexIter = indices.iterator();
		while (indexIter.hasNext()) {
			currentIndex = indexIter.next();

			// arbeite auf Kopien der Vertices, da diese verschoben werden
			currentVertex = mVertices.get(currentIndex).clone();
			LOGGER.trace("INDEX: " + currentIndex + " VERTEX: " + currentVertex);

			// projiziere die Vertices in die xy-Ebene durch Ignorieren der
			// ermittelten Achse (z-Komponente ist in den Ergebnisvertices immer
			// 0)
			currentVertex = mathHelper.createXYPlaneProjectionForPoint(
					currentVertex, ignorableAxis);
			quadVerts.add(currentVertex);
		}

		// ermittle den kleinsten x- und y-Wert
		Float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
		MyVector3f currentPosition = null;

		Iterator<Vertex3d> vertIter = quadVerts.iterator();
		while (vertIter.hasNext()) {
			currentPosition = vertIter.next().getPositionPtr();
			if (currentPosition.x < minX)
				minX = currentPosition.x;
			if (currentPosition.y < minY)
				minY = currentPosition.y;
		}

		// berechne einen Translationsvektor, der auf alle Vertices addiert wird
		// drehe die Vorzeichen der Minimalwerte um, damit eine Addition mit den
		// Vertices fuer die Quellverts an mindestens einer Komponente 0 ergibt
		minX *= -1.0f;
		minY *= -1.0f;

		MyVector3f translation = new MyVector3f(minX, minY, 0.0f);

		// verschiebe jetzt alle Vertices um den Translationsvektor
		vertIter = quadVerts.iterator();
		while (vertIter.hasNext()) {
			currentVertex = vertIter.next();
			currentVertex.getPositionPtr().add(translation);
		}

		Map<Integer, MyVector2f> textureMap = new HashMap<Integer, MyVector2f>(
				indices.size());
		MyVector3f unscaledCoords3f = null;
		MyVector2f unscaledCoords2f = null;

		currentIndex = null;

		// erstelle die TextureMap fuer das aktuelle Quad mit den modifizierten
		// unskalierten Koordinaten
		// die Abfolge der Vertices wurde durch die Berechnungen nicht
		// veraendert, darum sind auch die Indices gueltig
		Object[] indexArray = indices.toArray();
		for (int i = 0; i < indices.size(); i++) {
			unscaledCoords3f = quadVerts.get(i).getPosition();
			currentIndex = (Integer) indexArray[i];
			unscaledCoords2f = new MyVector2f(unscaledCoords3f.x,
					unscaledCoords3f.y);
			textureMap.put(currentIndex, unscaledCoords2f);
		}
		quad.setTextureCoords(textureMap);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode durchlaeuft alle gespeicherten Texturinstanzen und gibt die
	 * Verweise frei, sofern ein Zugriff auf die Textur erfolgt
	 * 
	 * @param texture
	 *            Texturobjekt, dessen Referenzen freigegeben werden sollen
	 */
	protected void releaseTextureByTextureObject(Texture texture) {

		Set<String> keySet = mTextures.keySet();
		String current = null;
		Iterator<String> keyIter = keySet.iterator();
		while (keyIter.hasNext()) {
			current = keyIter.next();

			if (mTextures.get(current).equals(texture)) {
				// dekrementiere den Reference-Counter im Texturobjekt
				texture.removeReference();
				keyIter.remove();
			}
		}

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mIsRoot
	 */
	public boolean isRoot() {
		return mIsRoot;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mIsRoot
	 *            the mIsRoot to set
	 */
	public void setIsRoot(boolean mIsRoot) {
		this.mIsRoot = mIsRoot;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet Vektoren vom uebergebenen Ankerpunkt auf alle Vertices
	 * des komplexen Objekts und rotiert anschliessend diese Vektoren um die
	 * uebergebene Rotationsachse.
	 * 
	 * @param axis
	 *            Rotatiosachse
	 * @param anchorPoint
	 *            Punkt, von dem ausgehend die zu rotierenden Vektoren berechnet
	 *            werden
	 * @param angleRad
	 *            Rotationswinkel in Radians
	 */
	public void rotate(MyVector3f axis, MyVector3f anchorPoint, double angleRad) {

		Iterator<Vertex3d> vertIter = mVertices.iterator();
		Vertex3d currentVertex = null;
		MyVector3f rotationVector = null, resultVector = null, resultCoordinates = null;
		MyVectormath mathHelper = MyVectormath.getInstance();

		while (vertIter.hasNext()) {
			currentVertex = vertIter.next();
			rotationVector = new MyVector3f();

			// vektor vom Ankerpunkt auf das Vertex berechnen
			rotationVector.sub(currentVertex.getPosition(), anchorPoint);
			resultVector = mathHelper.calculateRotatedVectorRadians(axis,
					rotationVector, angleRad);

			// rechne die Koordinaten des Zielpunktes aus
			resultCoordinates = new MyVector3f();
			resultCoordinates.add(anchorPoint, resultVector);
			currentVertex.setPosition(resultCoordinates);
		}

		// fuehre alle Updateberechnungen fuer saemtliche Subelemente durch
		update();

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode liefert alle Quads, die die uebergebene Ausrichtung besitzen
	 * 
	 * @param direction
	 *            Ausrichtung, die die gesuchten Quads besitzen sollen
	 * @return Liste mit allen Quads, deren Ausrichtung der Uebergabeausrichtung
	 *         entspricht
	 */
	public List<AbstractQuad> getAllOutsideQuadsWithDirection(
			final Side direction) {

		if (direction.equals(Side.ALL)) {
			return mOutdoorQuads;
		} else {
			final List<AbstractQuad> result = new ArrayList<AbstractQuad>();
			for (AbstractQuad currentQuad : mOutdoorQuads) {
				if (currentQuad.getDirection().equals(direction)) {
					result.add(currentQuad);
				}
			}
			return result;
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode durchlaeuft alle Quads innerhalb des komplexen Objekts und sucht
	 * nach einem Quad mit der angeforderten Aufloesung. Sofern ein solches
	 * gefunden wurde, wird es an den Aufrufer zurueckgegeben.
	 * 
	 * @param direction
	 *            Gesuchte Ausrichtung
	 * @return Das erste gefundene Quad mit der gesuchten Ausrichtung, null
	 *         sonst
	 */
	public AbstractQuad getQuadByDirection(Side direction) {

		List<AbstractQuad> quads = getAllOutsideQuadsWithDirection(direction);
		if (quads.size() > 0)
			return quads.get(0);
		else
			return null;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode modifiziert die Ausrichtungen der Quads basierend auf einer
	 * Uebergabemap, die Zuordnungen von Vektoren zu Seitenausrichtungen
	 * enthaelt. Anhand dieser Map modifiziert man die Seitenausrichtungen aller
	 * Quads innerhalb des komplexen Objekts. Dadurch kann man Subkomponenten
	 * genau wie ihre Hauptkomponenten ausrichten
	 * 
	 * @param map
	 *            Map, die Zuordnung von Normalenvektoren zu Ausrichtungen
	 *            enthaelt
	 * @param quads
	 *            Quadliste, fuer die die Zuordnung berechnet werden soll
	 */
	public void alignDirectionsByNormals(Map<MyVector3f, Side> map,
			List<AbstractQuad> quads) {

		LOGGER.trace("Aligning Directions for " + getID());

		Set<MyVector3f> keys = map.keySet();
		// Iterator<AbstractQuad> quadIter = mOutdoorQuads.iterator();
		Iterator<AbstractQuad> quadIter = quads.iterator();
		AbstractQuad currentQuad = null;

		MyVector3f currentNormal = null;
		Side currentSide = null, newSide = null;

		Iterator<MyVector3f> keyIter = keys.iterator();
		MyVector3f currentKey = null;

		// maximal tolerierte Abweichung in Grad unterhalb derer zwei Seiten die
		// gleiche Ausrichtung zugewiesen bekommen
		// verwende 45°, da durch die Extrusionsberechnungen Strukturen
		// entstehen, bei denen die Quads alle 90°-Winkel zueinander haben
		// dadurch trifft man immer noch die richtige Normale, wenn der Winke <
		// 45° ist => so kann man auch Objekte alignen, die auf einer schraegen
		// Ebene stehen
		Float maxDeviationTolerance = 45.0f;

		/*
		 * while(keyIter.hasNext()) { currentKey = keyIter.next();
		 * System.out.println("Key: " + currentKey + " Value: " +
		 * map.get(currentKey)); }
		 */
		MyVectormath mathHelper = MyVectormath.getInstance();

		MyVector3f minimalDeviation = null, keyNormal = null;

		while (quadIter.hasNext()) {
			currentQuad = quadIter.next();

			// skippe ZUNAECHST alle Quads mit Ausrichtung = ID, UNDEFINED, SIDE
			currentSide = currentQuad.getDirection();
			if (currentSide.equals(Side.UNDEFINED)
					|| currentSide.equals(Side.ID)
					|| currentSide.equals(Side.SIDE))
				continue;

			currentNormal = currentQuad.getNormal();

			// probiere zunaechst, ob der Vektor bereits so in der Map vorkommt
			if (map.containsKey(currentNormal)) {
				newSide = map.get(currentNormal);
				assert newSide != null : "FEHLER: Neu zu setzende Seite ist NULL";
				currentQuad.setDirection(newSide);
			}
			// sonst muss man den Vektor waehlen, dessen Winkel am geringsten
			// vom Eingabewinkel abweicht
			else {

				Float minAngle = Float.MAX_VALUE, angle = null;
				keyIter = keys.iterator();
				while (keyIter.hasNext()) {
					// arbeite mit Clones, um ungewollte Seiteneffekte zu
					// vermeiden, die zu Zugriffsfehlern auf die Map fuehren
					keyNormal = keyIter.next().clone();
					angle = mathHelper.calculateAngle(currentNormal,
							keyNormal.clone());
					// waehle den Winkel mit der geringsten Abweichung
					if (angle < minAngle) {
						minAngle = angle;
						minimalDeviation = keyNormal;
					}
				}

				// wenn die minimale Abweichung unterhalb des Grenzwerts liegt,
				// verwende die gefunde Ausrichtung fuer das neue Quad
				if (minAngle < maxDeviationTolerance) {
					// verwende die Ausrichtung, die am wenigsten von der
					// Quellausrichtung abweicht
					assert minimalDeviation != null : "FEHLER: Es konnte kein Normalenvektor fuer den Keyzugriff bestimmt werden";
					newSide = map.get(minimalDeviation);
					assert newSide != null : "FEHLER: Neu zu setzende Seite ist NULL, Normalenvektor: "
							+ minimalDeviation;
					currentQuad.setDirection(newSide);
				}
				// sonst verwende "SIDE" als Standard (geht davon aus, das TOP
				// und BOTTOM gefunden werden und Abweichungen nur in den Seiten
				// auftreten)
				// ueberschreibe nur, wenn bisher noch keine Seite zugewiesen
				// wurde
				else {
					if (currentQuad.getDirection().equals(Side.UNKNOWN))
						currentQuad.setDirection(Side.SIDE);
				}

			}

			LOGGER.trace("Quad-Normal: " + currentQuad.getNormal()
					+ " Zugewiesene Seite: " + currentQuad.getDirection());
		}
		/*
		 * Iterator<AbstractQuad> quadIter2 = mQuads.iterator(); AbstractQuad
		 * current2 = null; boolean foundTop = false; while(quadIter2.hasNext())
		 * { current2 = quadIter2.next();
		 * if(current2.getDirection().equals(Side.TOP)) foundTop = true;
		 * 
		 * } assert foundTop:
		 * "FEHLER: Es existiert kein Quad mit Ausrichtung TOP fuer Complex: " +
		 * this;
		 */

		updateDirectionForCylindricalComponents();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode dient der Aktualisierung der Seitenausrichtungen bei Grundrissen,
	 * die zylindrische Komponenten enthalten. Bei solchen Grundrissen sollen
	 * die Seiten, die den Zylinder formen, immer mit SIDE festgelegt sein.
	 * Durch das automatisierte Alignment kann es sonst passieren, dass Quads
	 * mit RIGHT oder LEFT ausgerichtet werden und dann bei der Dachberechnung
	 * Steigungen zugewiesen bekommen, die von den anderen Seiten abweichen.
	 * Dies fuehrt zu unregelmaessigen Strukturen, die vermieden werden sollen.
	 * Das Verfahren basiert auf der Bestimmung der Schnittpunkte aller
	 * Winkelhalbierenden. Bei zylinderfoermigen Grundrisskomponenten schneiden
	 * sich diese Winkelhalbierenden in genau einem Punkt, naemlich dem
	 * Mittelpunkt des Kreises, aus dem der Zylinder erstellt wurde. Findet man
	 * zwei aufeinanderfolgende identische Schnittpunkte, so weiss man, dass die
	 * beteiligten Seitenflaechen Teile eines Zylinders sein muessen. In diese
	 * Fall wird ihre Ausrichtung angepasst.
	 */
	protected void updateDirectionForCylindricalComponents() {

		// wenn es sich um ein geladenes 3d-Objekt handelt, gibt es keinen
		// Footprint und somit auch keine Extrusion
		if (this instanceof ImportedComplex)
			return;

		MyVectormath mathHelper = MyVectormath.getInstance();
		List<Ray> footprintRays = mFootprint.getRays();
		List<AbstractQuad> outdoorQuads = getOutdoorQuads();

		List<Ray> winkelhalbierenden = new ArrayList<Ray>(footprintRays.size());

		Ray currentRay = null, nextRay = null;
		MyVector3f winkelhalbierendeDir = null;
		Ray winkelhalbierende = null;

		// Winkelhalbierende berechnen
		for (int i = 0; i < footprintRays.size(); i++) {

			currentRay = footprintRays.get(i);
			nextRay = footprintRays.get((i + 1) % footprintRays.size()).clone();
			nextRay.getDirectionPtr().scale(-1.0f);
			winkelhalbierendeDir = mathHelper.calculateWinkelhalbierende(
					currentRay, nextRay);
			winkelhalbierende = new Ray(nextRay.getStart(),
					winkelhalbierendeDir);
			winkelhalbierenden.add(winkelhalbierende);
		}

		List<MyVector3f> intersections = new ArrayList<MyVector3f>(
				footprintRays.size());
		MyVector3f intersection = null;

		// Schnittpunkte aufeinanderfolgender Halbierenden bestimmen
		for (int i = 0; i < winkelhalbierenden.size(); i++) {
			currentRay = winkelhalbierenden.get(i);
			nextRay = winkelhalbierenden.get((i + 1)
					% winkelhalbierenden.size());
			intersection = mathHelper
					.calculateRay2RayIntersectionApproximation(currentRay,
							nextRay);
			intersections.add(intersection);
		}

		// jetzt immer paarweise aufeinander folgende Schnittpunkte vergleichen
		// wenn 2 aufeinanderfolgende Schnittpunkte innerhalb eines
		// Toleranzbereichs identisch sind, setzt man alle beiteligten Quads auf
		// SIDE
		MyVector3f currentIntersection = null, nextIntersection = null, previousIntersection = null, nextNextIntersection = null;
		AbstractQuad currentQuad = null, nextQuad = null;
		int index = -1;
		for (int i = 0; i < intersections.size(); i++) {
			currentIntersection = intersections.get(i);
			nextIntersection = intersections
					.get((i + 1) % intersections.size());

			LOGGER.trace("CurrentIntersection: " + currentIntersection);
			if (currentIntersection == null || nextIntersection == null)
				continue;

			index = i - 1;
			if (index < 0)
				index = intersections.size() - 1;
			previousIntersection = intersections.get(index);

			nextNextIntersection = intersections.get((i + 2)
					% intersections.size());

			// Schnittpunkte sind innerhalb eines Toleranzbereichs identisch
			if (mathHelper.isWithinTolerance(currentIntersection,
					nextIntersection)) {

				currentQuad = outdoorQuads.get((i + 1) % intersections.size());
				nextQuad = outdoorQuads.get((i + 2) % intersections.size());
				// nextNextQuad = outdoorQuads.get((i + 2) %
				// intersections.size());

				LOGGER.trace("Setze Current: " + currentQuad.getDirection()
						+ " Next: " + nextQuad.getDirection() + " auf SIDE");
				currentQuad.setDirection(Side.SIDE);
				nextQuad.setDirection(Side.SIDE);

				// teste nun auch noch die Vorgaenger bzw. Nachfolgerschnitte =>
				// bei diesen findet nur dann eine Aktualisierung statt, wenn
				// der Schnittpunkt nicht uebereinstimmt
				// sonst wird / wurde an anderer Stelle das Update durchgefuehrt
				if (previousIntersection != null) {
					if (!mathHelper.isWithinTolerance(currentIntersection,
							previousIntersection)) {
						AbstractQuad previous = outdoorQuads.get(i);
						LOGGER.trace("Setze Previous: "
								+ previous.getDirection() + " auf SIDE");
						previous.setDirection(Side.SIDE);
					}
				}

				if (nextNextIntersection != null) {
					if (!mathHelper.isWithinTolerance(currentIntersection,
							nextNextIntersection)) {
						AbstractQuad next = outdoorQuads.get((i + 3)
								% intersections.size());
						LOGGER.trace("Setze Next: " + next.getDirection()
								+ " auf SIDE");
						next.setDirection(Side.SIDE);
					}
				}
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	@Override
	/**
	 * Methode wird vor der endgueltigen Zerstoerung durch den GC gecallt
	 */
	protected void finalize() throws Throwable {
		LOGGER.debug("Zerstoere Complex vom Typ " + getType() + " mit ID: "
				+ getID());
	}

	// ------------------------------------------------------------------------------------------
	@Override
	public void setTextureByCategory(TextureCategory category) {
		LOGGER.debug("Setting Texture with category " + category.toString()
				+ " for " + getID());
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
		setTexture(category.toString(), texture);

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode liefert alle Komponenten des komplexen Objekts, bei
	 * Nicht-Composite-Objekten handelt es sich immer um eine einzelne
	 * Komponente, Composite-Objekte ueberschreiben die Methode und liefern nur
	 * "konkrete" Objekte an den Aufrufer zurueck
	 * 
	 * @return Liste mit allen abstrakten Objekten, die in dem aktuellen Objekt
	 *         enthalten sind (inkl. Objekt selber)
	 */
	public List<AbstractComplex> getConcreteComponents() {

		final List<AbstractComplex> components = new ArrayList<AbstractComplex>();
		components.add(this);
		return components;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode exportiert das Modell im Zielformat in die Zieldatei
	 */
	public void exportModelToFile(final String path, final String filename,
			final ExportFormat format) {

		AbstractModelExport exporter = null;

		switch (format) {
		case OBJ:
			exporter = new ObjExport();
			exporter.export(this, path, filename);
			break;
		default:
			assert false : "FEHLER: Unbekanntes Exportformat: " + format
					+ ", kein geeigneter Exporter vorhanden!";
			break;
		}

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
		String message = prefix + getID() + " ROOT: " + isRoot();
		LOGGER.info(message);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erstellt Stockwerke fuer komplexe Objekte durch Verschieben des
	 * Grundrisses entlang der Grundrissnormalen. Erzeugung basiert auf den
	 * Ansaetzen zur Erzeugung von MergedComplex-Objekten
	 * 
	 * @param numberOfLevels
	 *            Anzahl an Stockwerken, die fuer das Objekt generiert werden
	 *            soll
	 */
	public AbstractComplex createLevels(int numberOfLevels) {

		AbstractQuad bottom = null;
		List<Vertex3d> bottomVertices = null;
		FloorComplex currentFloor = null;
		Integer[] bottomIndices = null;
		FloorPosition currentFloorPosition = null;
		float interemdiateLevelHeight = 5.0f;

		LOGGER.debug("Erzeuge " + numberOfLevels + " Levels fuer Complex: "
				+ getID());

		// berechne die Hoehe je Level => unterteile zunaechst gleichmaessig, in
		// spaeteren Ausbaustufen kann das Erdgeschoss mehr Raum bekommen
		float heightPerLevel = mHeight / numberOfLevels;
		bottom = getQuadByDirection(Side.BOTTOM);

		assert bottom != null : "FEHLER: Es ist kein Bottom-Quad definiert, Berechnung wird abgebrochen. Complex: "
				+ getID();

		// erzeuge eine Directions-To-Side-Map-Struktur, die den
		// Normalenvektoren die unterschiedlichen Seiten der komplexen Objekte
		// zuordnet
		Map<MyVector3f, Side> directionToSideMap = createDirectionToSideMap();

		// erstelle eine MyPolygon-Struktur, die den Grundriss des Objekts
		// beschreibt
		bottomVertices = new ArrayList<Vertex3d>(bottom.getIndices().length);
		bottomIndices = bottom.getIndices();

		LOGGER.debug("BOTTOM-Normal: " + bottom.getNormal());

		// erzeuge den Grundriss fuer das Erdgeschoss
		// for(int i = 0; i < bottomIndices.length; i++)
		// bottomVertices.add(mVertices.get(bottomIndices[i]).clone());
		// drehe die Vertices, damit die Normale der Ausrichtung eines Top-Quads
		// entspricht => es wird immer in Richtung der negativen y-Achse
		// verschoben
		for (int i = bottomIndices.length - 1; i >= 0; i--)
			bottomVertices.add(mVertices.get(bottomIndices[i]).clone());

		// erzeuge den groundFloor auf der Zielhoehe
		LOGGER.debug("Erzeuge Ground-Floor");
		LOGGER.debug("#Vertices: " + bottomVertices.size());
		boolean isTop = false;

		// HIER TESTEN; OB DAS GANZE PASST
		FloorComplex groundFloor = new FloorComplex(mParent, bottomVertices,
				heightPerLevel, directionToSideMap, FloorPosition.GROUND, 0,
				isTop);
		groundFloor.create();

		List<AbstractComplex> levels = new ArrayList<AbstractComplex>(
				numberOfLevels);
		levels.add(groundFloor);

		float currentHeight = heightPerLevel;
		MyPolygon footprint = new MyPolygon(bottomVertices);
		LOGGER.error("FOOTPRINT GROUND: " + footprint);

		// von nun an werden die weiteren Stockwerke basierend auf dem TOP-Quad
		// des jeweils vorhergehenden Objekts erstellt
		if (numberOfLevels > 1)
			LOGGER.debug("Erzeuge weitere Stockwerke");
		for (int i = 0; i < numberOfLevels - 1; i++) {

			// erzeuge ein Zwischengeschoss, um z-Fighting zwischen TOP- und
			// BOTTOM-Floors zu verhindern
			IntermediateFloor intermediate = new IntermediateFloor(getParent(),
					footprint, interemdiateLevelHeight, directionToSideMap,
					FloorPosition.INTERMEDIATE, levels.size(), isTop);
			intermediate.create();

			MyVector3f translation = footprint.getNormal().clone();
			translation.scale(currentHeight);
			intermediate.translate(translation);
			intermediate.update();
			levels.add(intermediate);
			currentHeight += interemdiateLevelHeight;
			LOGGER.error("INTERMEDIATE: " + intermediate.getFootprint());

			// regulaeren Floor hinterherschieben
			// wenn das letzte Stockwerk berechnet wird, setze seine Position
			// auf GROUND
			if (i == numberOfLevels - 2)
				currentFloorPosition = FloorPosition.TOP;
			else
				currentFloorPosition = FloorPosition.INTERMEDIATE;

			currentFloor = new FloorComplex(mParent, footprint, heightPerLevel,
					directionToSideMap, currentFloorPosition, levels.size(),
					isTop);
			currentFloor.create();

			translation = footprint.getNormal().clone();
			translation.scale(currentHeight);
			currentFloor.translate(translation);
			currentFloor.update();
			levels.add(currentFloor);
			currentHeight += heightPerLevel;
			LOGGER.error("FLOOR: " + currentFloor.getFootprint());
		}

		// adde nun alle erzeugten komplexen Objekte zu einem gemeinsamen
		// Composite-Objekt, das als Ergebnis der Berechnung zurueckgereicht
		// wird
		CompositeComplex result = new CompositeComplex(mParent);
		result.create();

		LOGGER.debug("Erzeuge Composite: " + result.getID()
				+ " fuer Stockwerke.");

		Iterator<AbstractComplex> floorIter = levels.iterator();
		while (floorIter.hasNext()) {
			result.addComplex(floorIter.next());
		}

		// melde das Quell-Objekt bei der Objektverwaltung ab
		unregister();

		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erzeugt eine Map-Struktur, die den Normalenvektoren der Quads
	 * ihre Ausrichtung zuordnet. Diese Map wird verwendet, um bsw. beim Merging
	 * den Quads des zusammengefassten Objekts einheitliche Richtungen zuweisen
	 * zu koennen.
	 * 
	 * @return Map mit Zuordnungen von Ausrichtungen zu Side-Enum-Werten
	 */
	public Map<MyVector3f, Side> createDirectionToSideMap() {

		List<AbstractQuad> outdoorQuads = getOutdoorQuads();
		Map<MyVector3f, Side> normalToDirectionMap = new HashMap<MyVector3f, Side>(
				outdoorQuads.size());
		AbstractQuad currentQuad = null;
		MyVector3f currentNormal = null;
		Side currentSide = null;

		for (int i = 0; i < outdoorQuads.size(); i++) {
			currentQuad = outdoorQuads.get(i);
			currentNormal = currentQuad.getNormal();
			currentSide = currentQuad.getDirection();
			normalToDirectionMap.put(currentNormal, currentSide);
		}

		return normalToDirectionMap;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode aktualisiert die Indices aller Subkomponenten, so dass diese auf
	 * den neuen VertexBuffer umgestellt werden
	 * 
	 * @param vertexBuffer
	 *            Neuer Vertexbuffer, der als Basis fuer die Umstellung
	 *            verwendet werden soll
	 */
	public void updateIndicesForNewVertexBuffer(List<Vertex3d> vertexBuffer) {

		LOGGER.debug("Aktualisiere Vertex- und Indexbuffer!");

		List<Vertex3d> oldBuffer = getVertices();
		Iterator<AbstractQuad> quadIter = mOutdoorQuads.iterator();

		AbstractQuad currentQuad = null;
		List<AbstractQuad> holes = null;
		while (quadIter.hasNext()) {

			currentQuad = quadIter.next();
			updateQuadIndices(vertexBuffer, oldBuffer, currentQuad);

			// Loecher verarbeiten
			if (currentQuad.hasHoles()) {
				holes = currentQuad.getHoles();
				for (int i = 0; i < holes.size(); i++) {
					currentQuad = holes.get(i);
					updateQuadIndices(vertexBuffer, oldBuffer, currentQuad);

				}
			}
		}

		// Edge-Manager loeschen
		mEdges.clear();
		mEdges = null;

		// erzeuge einen neuen EdgeManager, um Altlasten zu entsorgen
		mEdges = new EdgeManager(this);

		// Referenz auf neuen VertexBuffer speichern
		mVertices = vertexBuffer;

		// alle Quads erneut tesselieren
		for (int i = 0; i < mOutdoorQuads.size(); i++) {
			mOutdoorQuads.get(i).tesselate();
		}

	}

	// ------------------------------------------------------------------------------------------
	private void updateQuadIndices(List<Vertex3d> vertexBuffer,
			List<Vertex3d> oldBuffer, AbstractQuad quad) {

		Integer[] oldIndices = quad.getIndices();
		Integer[] newIndices = new Integer[oldIndices.length];

		Vertex3d currentVertex = null;
		int newIndex;

		// bestimme die neuen Indices des aktuellen Quads in Bezug auf den neuen
		// VertexBuffer
		for (int i = 0; i < oldIndices.length; i++) {
			currentVertex = oldBuffer.get(oldIndices[i]);
			newIndex = vertexBuffer.indexOf(currentVertex);
			assert newIndex != -1 : "FEHLER: Vertex " + currentVertex
					+ " befindet sich nicht im neuen Vertexbuffer!";
			newIndices[i] = newIndex;

		}

		quad.setIndices(newIndices);

		// loesche die Dreiecke des Quads, diesen werden bei nachfolgenden
		// Tesselationen neu erstellt
		List<Triangle> triangles = quad.getTriangles();
		for (int i = 0; i < triangles.size(); i++)
			triangles.get(i).destroy();
		triangles.clear();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mHasTextureCoordinates
	 */
	public boolean hasTextureCoordinates() {
		return mHasTextureCoordinates;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mHasTextureCoordinates
	 *            the mHasTextureCoordinates to set
	 */
	public void setHasTextureCoordinates(boolean mHasTextureCoordinates) {
		this.mHasTextureCoordinates = mHasTextureCoordinates;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode tesseliert alle Quads des komplexen Objekts
	 */
	public void tesselate() {
		for (int i = 0; i < mOutdoorQuads.size(); i++)
			mOutdoorQuads.get(i).tesselate();

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode extrudiert den gespeicherten Footprint in Richtung seines
	 * Normalenvektors. Die Methode geht dabei in der Berechnung davon aus, dass
	 * der gespeicherte Grundriss die Bodenflaeche des Objekts bildet, der
	 * Normalenvektor der Ebene definiert die Extrusionsrichtung, zeigt somit in
	 * Richtung der negativen y-Achse (vom Drehsinn her ist die Eingabe also ein
	 * Quad mit TOP-Ausrichtung)
	 */
	public void extrudeFootprint() {
		assert mFootprint != null : "FEHLER: Es wurde kein polygonaler Grundriss gesetzt!";
		assert mHeight != null : "FEHLER: Es ist keine Hoehe angegeben";

		// erzeuge die Vertices fuer das germergte Objekt
		// der uebergebene Grundriss beschreibt die Bodenflaeche, die
		// Ausrichtung entspricht aber einem Deckenquad, verschiebe die Vertices
		// in Richtung der Normale bis auf Zielhoehe
		MyVector3f translationDirection = mFootprint.getNormal();

		// Zielhoehe in die Normale einrechnen
		translationDirection.scale(mHeight);
		LOGGER.debug("Translationsvektor: " + translationDirection);

		// sorge dafuer, dass Vertices, die ein Quad bilden, immer aufeinander
		// folgen und die richtige Abfolge besitzen
		List<Vertex3d> vertices = mFootprint.getVertices();
		// fuege alle Vertices des Footprints zum Buffer hinzu
		mVertices.addAll(vertices);

		Vertex3d currentVertex = null;

		CreationState currentState = CreationState.LOWER_START;
		int index;

		// #Indices = #Quads * 4 = #Kanten * 4 = #Vertices * 4
		Integer[] indices = new Integer[vertices.size() * 4];
		int lastIndex = 0;

		boolean lastIteration = false;

		// durchlaufe die Schleife einmal haeufiger, um den Ueberlauf auf das
		// Startvertex zu beruecksichtigen
		for (int i = 0; (i < vertices.size() + 1) && !lastIteration; i++) {

			// wenn der Index out of range laeuft, muss das Startvertex
			// verwendet werden
			if (i == vertices.size()) {
				lastIteration = true;
				i = 0;
			}

			// arbeite immer auf geklonten Vertices
			currentVertex = vertices.get(i);

			// beginne mit der unteren Kante, damit die Abfolge zu den anderen
			// komplexen Objekten einheitlich ist
			switch (currentState) {
			case LOWER_START:

				// Verschiebung
				index = mVertices.indexOf(currentVertex);
				if (index != -1) {
					currentVertex = mVertices.get(index);
				} else {
					mVertices.add(currentVertex);
					index = mVertices.indexOf(currentVertex);
				}
				indices[lastIndex] = index;
				lastIndex++;
				currentState = CreationState.LOWER_END;
				break;

			case LOWER_END:

				// Ende der unteren Kante
				// Verschiebung
				// currentVertex.getPositionPtr().add(translationDirection);

				index = mVertices.indexOf(currentVertex);
				if (index != -1) {
					currentVertex = mVertices.get(index);
					// System.out.println("Vertex aus Buffer fuer UPPER_END1: "
					// + currentVertex);
				} else {
					mVertices.add(currentVertex);
					index = mVertices.indexOf(currentVertex);
				}

				// untere Kante => Endvertex
				indices[lastIndex] = index;
				lastIndex++;

				// wechsle auf die obere Kante
				currentVertex = currentVertex.clone();

				// verschiebe die vorhandenen Vertices um den Translationsvektor
				currentVertex.getPositionPtr().add(translationDirection);

				index = mVertices.indexOf(currentVertex);
				if (index != -1) {
					currentVertex = mVertices.get(index);
					// System.out.println("Vertex aus Buffer fuer UPPER_END2: "
					// + currentVertex);
				} else {
					mVertices.add(currentVertex);
					index = mVertices.indexOf(currentVertex);

				}
				indices[lastIndex] = index;
				lastIndex++;

				// schliesse die Kante ab, gehe dafuer zurueck zum Startvertex
				// der unteren Kante (lastIndex - 3)
				currentVertex = mVertices.get(indices[lastIndex - 3]).clone();
				currentVertex.getPositionPtr().add(translationDirection);
				index = mVertices.indexOf(currentVertex);
				if (index != -1) {
					currentVertex = mVertices.get(index);
					// System.out.println("Vertex aus Buffer fuer UPPER_END3: "
					// + currentVertex);

				} else {
					mVertices.add(currentVertex);
					index = mVertices.indexOf(currentVertex);
				}
				indices[lastIndex] = index;
				lastIndex++;
				currentState = CreationState.LOWER_START;

				// das letzte Endvertex wird Start des naechsten Quads
				i--;
				break;
			}
		}

		// an diesem Punkt existieren alle Vertices und alle Indices fuer die
		// Quaderzeugung der Seitenflaechen
		// durchlaufe die Indices erneut und erstelle fuer je 4
		// aufeinanderfolgende Indices ein Quad
		Integer[] quadIndices = new Integer[4];
		int count = 0;
		AbstractQuad tempQuad = null;
		for (int i = 0; i < indices.length; i++) {
			quadIndices[count] = indices[i];
			if (count == 3) {
				tempQuad = new Quad();
				tempQuad.setComplexParent(this);
				tempQuad.setIndices(quadIndices);
				tempQuad.setDirection(Side.UNKNOWN);
				tempQuad.tesselate();

				mOutdoorQuads.add(tempQuad);
				count = 0;
				quadIndices = new Integer[4];
				continue;
			} else {
				++count;
			}
		}

		// erzeuge nun jeweils Quads fuer Decke und Boden
		Integer[] topIndices = new Integer[mFootprint.getVertices().size()];
		Integer[] bottomIndices = new Integer[mFootprint.getVertices().size()];

		Iterator<Vertex3d> vertIter = mFootprint.getVertices().iterator();
		Vertex3d currentVert = null;

		// durchlaufe alle Vertices, ermittle die Indices von Top- und
		// Bottom-Verts
		// und adde sie in die zugehoerigen Integer-Arrays
		Integer indexBottom = null, indexTop = null;
		count = 0;
		int revertedIndex = mFootprint.getVertices().size() - 1;

		// der durch das eingegebene Polygon beschriebene Grundriss ist immer
		// BOTTOM
		vertices = mFootprint.getVertices();
		// logger.error("FOOTPRINT0: " + mFootprint);

		for (int i = 0; i < vertices.size(); i++) {

			// Bodenflaeche wird mit gedrehten Indices erzeugt, um eine korrekte
			// Normale zu erhalten (Normale des Eingabepolygons zeigt immer in
			// Richtung der negativen y-Achse)
			currentVert = vertices.get(i);
			indexBottom = mVertices.indexOf(currentVert);
			assert indexBottom != null : "FEHLER: Vertex " + currentVert
					+ " befindet sich nicht im Vertexbuffer des Quads";
			bottomIndices[revertedIndex] = indexBottom;

			// Deckenflaeche wird mit regulaeren Indices erstellt
			currentVert = currentVert.clone();
			currentVert.getPositionPtr().add(translationDirection);
			indexTop = mVertices.indexOf(currentVert);
			assert indexTop != null : "FEHLER: Vertex " + currentVert
					+ " befindet sich nicht im Vertexbuffer des Quads";
			topIndices[count] = indexTop;
			--revertedIndex;
			++count;
		}
		// logger.error("FOOTPRINT1: " + mFootprint);

		// Quads fuer Decke und Boden erzeugen
		AbstractQuad bottomQuad = new PolygonalQuad();
		bottomQuad.setComplexParent(this);
		bottomQuad.setDirection(Side.BOTTOM);
		bottomQuad.setIndices(bottomIndices);
		bottomQuad.tesselate();
		mOutdoorQuads.add(bottomQuad);
		/*
		 * bottomQuad.update(); logger.debug("BOTTOM: Normal: " +
		 * bottomQuad.getNormal());
		 */
		/*
		 * bottomQuad.update(); MyVectormath mathHelper =
		 * MyVectormath.getInstance();
		 * if(mathHelper.calculateAngle(bottomQuad.getNormal(),
		 * mFootprint.getNormal()) < 0.1f) { logger.error("AUSGANGSNORMALE: " +
		 * bottomQuad.getNormal()); logger.error(
		 * "Drehen der Bottom-Quad-Indices fuehrte nicht zu korrekter Ausrichtung der Normalen!"
		 * ); Integer[] revertedIndices = new Integer[bottomIndices.length];
		 * for(int i = bottomIndices.length - 1, j = 0; i >= 0; i--, j++)
		 * revertedIndices[j] = bottomIndices[i];
		 * bottomQuad.setIndices(revertedIndices); bottomQuad.tesselate();
		 * bottomQuad.update(); logger.error("NORMALE NACH DREHEN: " +
		 * bottomQuad.getNormal()); }
		 */

		AbstractQuad topQuad = new PolygonalQuad();
		topQuad.setComplexParent(this);

		topQuad.setDirection(Side.TOP);
		topQuad.setIndices(topIndices);
		topQuad.tesselate();
		mOutdoorQuads.add(topQuad);
		/*
		 * topQuad.update(); logger.error("TOP: Normal: " +
		 * topQuad.getNormal());
		 */

		// Quadausrichtungen an Basisobjektausrichtungen anpassen
		alignDirectionsByNormals(mNormalToDirectionMap, mOutdoorQuads);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erzegut eine standardisierte Abfolge von Vertices, indem ein
	 * Startpunkt innerhalb der uebergebenen Vertexliste ermittelt wird, der auf
	 * der konvexen Huelle der Vertexwolke liegt (indem ein Startpunkt bestimmt
	 * wird). Dies dient bsw. der Standardisierung der Polygonberechnungen, da
	 * dadurch eine feste Normalenbestimmung erleichtert werden sollte. =>
	 * funktioniert leider nicht immer...
	 * 
	 * @param vertices
	 *            Liste mit Vertices, die neu angeordnet werden sollen, indem
	 *            ein neues "erstes Vertex" ermittelt wird, die Grundstruktur
	 *            bleibt dabei gleich.
	 */
	protected List<Vertex3d> optimizeVertexOrder(List<Vertex3d> vertices) {

		PrototypeHelper helper = PrototypeHelper.getInstance();
		MyVector3f planeNormal = MyVectormath.getInstance()
				.calculateNormalNewell(vertices);
		// NORMALE MIT IN DIE BERECHNUNG REINNEHMEN

		// projiziere alle Vertices in die XY-Ebene (wird fuer die
		// Startpunktermittlung benoetigt)
		Map<Vertex3d, Vertex3d> projectedToOriginal = helper
				.projectVerticesToXYPlane(vertices, planeNormal);

		Set<Vertex3d> keys = projectedToOriginal.keySet();

		List<Vertex3d> listKeys = new ArrayList<Vertex3d>(keys.size());
		listKeys.addAll(keys);

		Vertex3d startProjected = helper.findStartPoint(listKeys);
		Vertex3d startOriginal = projectedToOriginal.get(startProjected);
		assert startOriginal != null : "FEHLER: Fuer das projizierte Vertex "
				+ startProjected + " existiert keine Zurodnung!";

		keys.clear();
		keys = null;

		projectedToOriginal.clear();
		projectedToOriginal = null;

		Integer index = vertices.indexOf(startOriginal);

		List<Vertex3d> reorderedVertices = new ArrayList<Vertex3d>(
				vertices.size());

		while (reorderedVertices.size() != vertices.size()) {
			if (index == vertices.size())
				index = 0;
			reorderedVertices.add(vertices.get(index));
			index++;
		}
		return reorderedVertices;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode versucht heuristisch, die Vertices derart anzuordnen, dass immer
	 * eine Ebene konstruiert wird, deren Normalenvektor in Richtung der
	 * negativen y-Achse (TOP-Quad-Ausrichtung) zeigen. Weiterhin wird geprueft,
	 * ob die Umkehrung der Reihenfolge zu einer gedrehten Normale fuehrt (dies
	 * ist fuer die Bestimmung der Bodenflaeche entscheidend). Ist dies nicht
	 * der Fall, so wird eine neue Vertexorder erzeugt, die dann getestet wird.
	 * 
	 * @param vertices
	 *            Vertexliste, die in ihrer Reihenfolge bei Bedarf angepasst
	 *            wird
	 * @return Liste mit Vertices, deren Reihenfolge den Ausrichtungskriterien
	 *         genuegt
	 */
	protected List<Vertex3d> optimizeVertexOrderNewSchool(
			List<Vertex3d> vertices) {

		int numberOfRetries = 0, maxNumberOfRetries = vertices.size();

		List<Vertex3d> rearrangedOrder = new ArrayList<Vertex3d>(
				vertices.size());
		List<Vertex3d> workingBuffer = new ArrayList<Vertex3d>(vertices.size());
		rearrangedOrder.addAll(vertices);

		// teste zunaechst, ob die Order so bereits funktioniert
		while (!testVertexOrder(rearrangedOrder)) {

			assert numberOfRetries <= maxNumberOfRetries : "FEHLER: Es konnte keine Anordung der Vertices gefunden werden, die den Anforderungen entspricht!";

			// kopiere alle Vertices um, verschiebe das Startvertex um jeweils
			// eine Position
			for (int i = 1; i < rearrangedOrder.size(); i++) {
				workingBuffer.add(rearrangedOrder.get(i));
				if (i == rearrangedOrder.size() - 1)
					workingBuffer.add(rearrangedOrder.get(0));
			}
			rearrangedOrder.clear();
			rearrangedOrder.addAll(workingBuffer);
			workingBuffer.clear();
			numberOfRetries++;

		}
		LOGGER.info("Benoetigte Retries: " + numberOfRetries);
		return rearrangedOrder;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode testet, ob die uebergebenen Vertices in einer Reihenfolge
	 * vorliegen, die zu einer Ebene fuehrt, deren Normale in Richtung der
	 * negativen y-Achse zeigt und bei der eine Umkehrung der Reihenfolge zu
	 * einer Normale in Richtung der positiven y-Achse fuehrt.
	 * 
	 * @param vertices
	 *            Vertexliste, die getestet werden soll
	 * @return True, falls die Anordnung die Anforderungen erfuellt, False sonst
	 */
	private boolean testVertexOrder(List<Vertex3d> vertices) {

		MyVectormath mathHelper = MyVectormath.getInstance();

		MyVector3f negativeYAxis = new MyVector3f(0, -1, 0);
		MyVector3f positiveYAxis = new MyVector3f(0, 1, 0);
		double doublePi = Math.PI * 2.0d;

		// erzeuge eine Ebene fuer die aktuellen Vertices
		Plane topPlane = mathHelper.calculatePlaneByVertices(vertices);
		double angle = mathHelper.getFullAngleRad(negativeYAxis,
				topPlane.getNormal());
		// Winkel muss ein Vielfaches von 360° sein
		angle %= doublePi;

		// Winkel zwischen den Vektoren muss 0 sein
		if (!mathHelper.isWithinTolerance(angle, 0.01f))
			return false;

		// jetzt Order umkehren und noch mal testen
		List<Vertex3d> revertedVertices = new ArrayList<Vertex3d>(
				vertices.size());
		for (int j = vertices.size() - 1; j >= 0; j--)
			revertedVertices.add(vertices.get(j));

		Plane bottomPlane = mathHelper
				.calculatePlaneByVertices(revertedVertices);
		angle = mathHelper.getFullAngleRad(positiveYAxis,
				bottomPlane.getNormal());

		// Winkel muss ein Vielfaches von 360° sein
		angle %= doublePi;

		// Winkel zwischen den Vektoren muss 0 sein
		if (mathHelper.isWithinTolerance(angle, 0.01f))
			return true;
		else
			return false;
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
	 * Methode liefert die Anzahl der Vertices innerhalb des komplexen Objekts
	 * 
	 * @return Anzahl der Vertices des Objekts
	 */
	public int getVertexCount() {
		// logger.info("Komponente: " + getID() + " VertexCount: " +
		// mVertices.size());
		return mVertices.size();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode validiert saemtliche Triangle-Strukutren daraufhin, ob es sich um
	 * "korrekte" Triangles handelt, also solche, bei denen der Flaecheninhalt >
	 * 0 ist. Durch Skalierungsoperationen kann hier der Fall auftreten, dass
	 * Dreiecke entarten, wodurch Linien statt Dreiecken entstehen, solche
	 * Dreiecke werden bei dieser Berechnung entfernt.
	 */
	public void validateStructure() {

		AbstractQuad currentQuad = null;
		List<Triangle> triangles = null;

		Iterator<Triangle> triangleIter = null;
		Iterator<AbstractQuad> quadIter = null;

		Triangle currentTriangle = null;
		Integer[] triangleIndices = null;
		List<Vertex3d> triangleVerts = null;
		Vertex3d currentVertex = null;
		boolean isValid = true;
		PrototypeHelper helper = PrototypeHelper.getInstance();

		quadIter = mOutdoorQuads.iterator();
		while (quadIter.hasNext()) {
			currentQuad = quadIter.next();
			triangles = currentQuad.getTriangles();
			triangleIter = triangles.iterator();
			while (triangleIter.hasNext()) {
				isValid = true;
				currentTriangle = triangleIter.next();
				triangleIndices = currentTriangle.getIndices();
				triangleVerts = new ArrayList<Vertex3d>(3);

				for (int k = 0; k < triangleIndices.length; k++) {
					currentVertex = mVertices.get(triangleIndices[k]);
					if (triangleVerts.contains(currentVertex)) {
						isValid = false;
						break;
					} else
						triangleVerts.add(currentVertex);

				}

				if (isValid) {
					isValid = helper.isValidTriangle(triangleVerts);
				}

				// wenn das Dreieck nicht gueltig ist, entferne es
				if (!isValid) {
					triangleIter.remove();
				}
			}

			// falls kein Dreieck mehr im Quad vorhanden ist, entferne auch das
			// Quad aus der Liste
			if (triangles.size() == 0)
				quadIter.remove();
		}
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mOBB
	 */
	public BoundingBox getBB() {
		return mBB;
	}

	// ------------------------------------------------------------------------------------------
	public void setBB(BoundingBox obb) {
		mBB = obb;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode skaliert das aktuelle Objekt uniform um den uebergebenen
	 * Scalingfactor. Hier koennte noch eine zufallsbasierte nicht-uniforme
	 * Skalierung eingebaut werden
	 * 
	 * @param scaleFactor
	 *            Skalierungsfaktor
	 */
	public AbstractComplex scaleComplex(Float scaleFactor) {

		LOGGER.debug("Scaling-Factor: " + scaleFactor);

		// skaliere alle Vertices um den uebergebenen Skalierungsfaktor
		AbstractComplex copy = this.clone();
		List<Vertex3d> vertices = copy.getVertices();
		for (int i = 0; i < vertices.size(); i++) {
			vertices.get(i).getPositionPtr().scale(scaleFactor);
		}

		// OBB skalieren, falls vorhanden
		if (copy.getBB() != null) {
			copy.getBB().scale(scaleFactor);
		}

		// falls das Objekt herunterskaliert wurde, berechne eine neue
		// Triangulation, um ungueltige Dreiecke / Quads zu vermeiden
		if (scaleFactor < 1.0f) {

			// Struktur validieren, alle Dreiecke mit Flaecheninhalt 0 oder
			// gleichen Indices entfernen
			copy.validateStructure();

			// alle Quads updaten
			copy.update();
		}
		return copy;
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public void extrude(Side whichFace, Axis extrudeAxis, float extrudeAmount) {

	}

	// ------------------------------------------------------------------------------------------

	@Override
	public void subdivideQuad(Side whichFace, subdivisionType type,
			float subdivisionFactor) {

	}

	// ------------------------------------------------------------------------------------------

	@Override
	public iGraphicComplex subdivide(subdivisionType type,
			float subdivisionFactor) {

		return null;
	}

	// ------------------------------------------------------------------------------------------
	public Map<MyVector3f, Side> getNormalToDirectionMap() {
		return mNormalToDirectionMap;
	}

	// ------------------------------------------------------------------------------------------
	public MyPolygon getFootprint() {
		return mFootprint;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode ermittelt das Quad, dass die uebergebenen Vertices enthaelt und
	 * gibt dieses zurueck. Da sich immer 2 Quads eine Kante teilen und die
	 * Kante aus dem Grundriss stammt, ist das richtige Quad immer das
	 * Nicht-Bottom-Quad => Methode dient dem Insetting der Wand
	 * 
	 * @param edgeVerts
	 *            Vertices, die die Eckpunkte der Kante beschreiben
	 * @return Quad, das die uebergebene Kante enthaelt und nicht Bottom-Quad
	 *         ist
	 */
	public AbstractQuad getQuadByVertices(List<Vertex3d> edgeVerts) {

		// ermittle die Indices der Vertices im Vertexbuffer des komplexen
		// Objekts
		assert edgeVerts.size() == 2 : "FEHLER: Die uebergebene Vertexliste enthaelt "
				+ edgeVerts.size() + " Vertices!";
		int index0 = mVertices.indexOf(edgeVerts.get(0));
		int index1 = mVertices.indexOf(edgeVerts.get(1));
		assert index0 != -1 && index1 != -1 : "FEHLER: Die uebergebenen Vertices kommen nicht im komplexen Objekt vor!";
		LOGGER.trace("Index1: " + index0 + " Vert1: " + edgeVerts.get(0)
				+ " Index2: " + index1 + "  Vert2: " + edgeVerts.get(1));

		AbstractQuad currentQuad = null;
		Set<Integer> currentIndices = null;
		for (int i = 0; i < mOutdoorQuads.size(); i++) {
			currentQuad = mOutdoorQuads.get(i);
			// Bottom-Quad direkt skippen WARUM?
			if (currentQuad.getDirection().equals(Side.BOTTOM))
				continue;
			currentIndices = currentQuad.getAllIndices();

			Iterator<Integer> indexIter = currentIndices.iterator();
			LOGGER.trace("QUAD: " + currentQuad.getID() + " "
					+ currentQuad.getDirection());
			while (indexIter.hasNext()) {
				Integer current = indexIter.next();
				LOGGER.trace("Index: " + current + " Ind1: " + index0
						+ " Ind2: " + index1);
				// if(current == index0 || current == index1)
				// logger.error("FOUND");

			}

			// wenn das aktuelle Quad beide Indices enthaelt, gebe es zurueck
			if (currentIndices.contains(index0)
					&& currentIndices.contains(index1)) {
				return currentQuad;
			}
		}

		return null;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mComplexParent
	 */
	public AbstractComplex getComplexParent() {
		return mComplexParent;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mComplexParent
	 *            the mComplexParent to set
	 */
	public void setComplexParent(AbstractComplex mComplexParent) {
		this.mComplexParent = mComplexParent;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mOBB
	 */
	public OBB getOBB() {
		return mOBB;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mOBB
	 *            the mOBB to set
	 */
	public void setOBB(OBB mOBB) {
		this.mOBB = mOBB;
	}

}
