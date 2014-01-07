package semantic.building.modeler.prototype.graphics.primitives;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import semantic.building.modeler.configurationservice.model.enums.ModelCategory;
import semantic.building.modeler.configurationservice.model.enums.Side;
import semantic.building.modeler.math.Axis;
import semantic.building.modeler.math.MyPolygon;
import semantic.building.modeler.math.MyVector2f;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.MyVectormath;
import semantic.building.modeler.math.Ray;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.prototype.enums.subdivisionType;
import semantic.building.modeler.prototype.exception.PrototypeException;
import semantic.building.modeler.prototype.graphics.complex.AbstractComplex;
import semantic.building.modeler.prototype.graphics.interfaces.iGraphicPrimitive;
import semantic.building.modeler.prototype.service.EdgeManager;
import semantic.building.modeler.prototype.service.PrototypeHelper;
import semantic.building.modeler.prototype.service.Texture;
import semantic.building.modeler.tesselation.service.TesselationService;

/**
 * 
 * @author Patrick Gunia Abstrakte Basisklasse fuer alle Arten von Quads
 *         (verallgemeinert auf Faces), also fuer Quad, TriangleFan etc.
 *         Implementiert Methoden, die nur fuer Quads, aber nicht fuer andere
 *         Primitives relevant sind
 * 
 */

public abstract class AbstractQuad extends AbstractPrimitive {

	/** Dreiecksliste aller Dreiecke, aus denen das Face besteht */
	protected List<Triangle> mTriangles;

	/** Faces sind immer Kinder eines AbstractComplex-Objektes */
	protected AbstractComplex mComplexParent;

	/**
	 * Faces koennen weitere faces als Kinder haben Kindfaces sind dabei immer
	 * durch die Ausmasse ihres Eltern-Faces beschraenkt
	 */
	protected AbstractQuad[] mqChildQuads;
	/**
	 * HashMap mit Texturkoordinaten, wird ueber den Index des jeweiligen Vertex
	 * indiziert
	 */
	protected Map<Integer, MyVector2f> mTextureCoords = null;

	/** wenn bestimmbar, dann Seite setzen */
	protected Side meDirection;

	/** Liste mit Quads, die Loecher im Ausgangsquad beschreiben */
	protected List<AbstractQuad> mHoles = new ArrayList<AbstractQuad>();

	/**
	 * Repraesentation der Quadoutline als Polygon, wird erst bei Bedarf
	 * errechnet
	 */
	protected MyPolygon mQuadOutlinePoly = null;

	/** Zeigt an, ob es sich um ein Quad handelt, das eine Innenwand bildet */
	protected boolean mIndoor = false;

	/**
	 * Bei Loechern zeigt diese Membervariable den Typ des komplexen Objekts an,
	 * das innerhalb des Loches positioniert wurde
	 */
	protected ModelCategory mContentType = null;

	// ------------------------------------------------------------------------------------------
	/**
	 * Leerer Default-Konstruktor
	 */
	public AbstractQuad() {
		init();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode tesseliert ein beliebig geformtes Quad und erzeugt die
	 * Dreiecksrepraesentation, die auf der untersten Ebene fuer das Zeichnen
	 * des Quads benoetigt wird
	 */
	public void tesselate() {

		// loesche evtl. vorhandene Dreiecke
		for (int i = 0; i < mTriangles.size(); i++)
			mTriangles.get(i).destroy();
		mTriangles.clear();

		// berechne die Ebene des Quads => diese wird fuer die korrekte
		// Anordnung der Triangle-Indices benoetigt
		// ist eigentlich doppelt gemacht, da die finalize-Berechnung das
		// ebenfalls durchfuehrt, allerdings muss hier
		// bereits die Normale bekannt sein
		calculatePlane();

		// Anzahl der Polygonabschnitte: 1 fuer das Quad + beliebige Anzahl an
		// Loechern
		Integer numberOfContours = 1 + mHoles.size();

		// erzeuge den Polygonzug des Quadobjekts als Abfolge von Vertices
		List<List<Vertex3d>> polygonContours = new ArrayList<List<Vertex3d>>();

		List<Vertex3d> polygonContour = null;
		new ArrayList<Vertex3d>(this.mIndices.length);
		List<AbstractQuad> tesselationQuads = new ArrayList<AbstractQuad>(
				numberOfContours);

		// fuege zuerst das tatsaechliche Quad ein
		tesselationQuads.add(this);

		// und anschliessend alle Loecher
		if (hasHoles()) {
			for (int i = 0; i < mHoles.size(); i++)
				tesselationQuads.add(mHoles.get(i));
		}

		final List<Vertex3d> vertices = getVertices();
		assert vertices != null : "FEHLER: Keine Vertices gesetzt";

		final String lineSeparator = System.getProperty("line.separator");
		final StringBuilder vertexStrings = new StringBuilder(lineSeparator);

		// arbeite nun die Liste mit den Quads ab, das erste Quad ist immer der
		// Umriss, die weiteren sind Loecher
		for (AbstractQuad currentQuad : tesselationQuads) {
			polygonContour = currentQuad.getQuadVertices();

			for (Vertex3d currentVertex : polygonContour) {
				vertexStrings
						.append("mVertices.add(new Vertex3d("
								+ currentVertex.getX() + "f, "
								+ currentVertex.getY() + "f, "
								+ currentVertex.getZ() + "f));" + lineSeparator);

			}
			polygonContours.add(polygonContour);
		}
		LOGGER.trace(vertexStrings);

		// berechne die Tesselation
		List<Vertex3d> tesselatedVertics = null;
		tesselatedVertics = TesselationService.getInstance().tesselate(
				polygonContours);

		Vertex3d currentVertex = null, currentCorner = null;
		Triangle tempTriangle = null;
		int index, count = 0;
		Integer[] indices = new Integer[3];
		List<Vertex3d> corners = new ArrayList<Vertex3d>(3);

		final PrototypeHelper helper = PrototypeHelper.getInstance();

		// nun erzeuge Dreieecke basierend auf dem Tesselationsergebnis
		Iterator<Vertex3d> vertIter = tesselatedVertics.iterator();
		while (vertIter.hasNext()) {
			currentVertex = vertIter.next();
			corners.add(currentVertex);

			// wenn 3 Indices zusammen sind, erzeuge ein Dreieck und adde es zur
			// Dreiecksliste
			if (count == 2) {

				// teste zunaechst, ob die Eckpunkte ein Dreieck bilden, dessen
				// Flaecheninhalt > 0 ist
				// wenn ja, erzeuge ein Dreieck, sonst verwerfe das aktuell
				// berechnete sowie die zugehoerigen Vertices
				if (helper.isValidTriangle(corners)) {

					// erzeuge Indices basierend auf den Corners
					Iterator<Vertex3d> cornerIter = corners.iterator();
					int innerCounter = 0;

					while (cornerIter.hasNext()) {
						currentCorner = cornerIter.next();

						index = vertices.indexOf(currentCorner);
						// Vertex kommt noch nicht im Buffer vor => hinzufuegen
						if (index == -1) {
							vertices.add(currentCorner);
							index = vertices.indexOf(currentCorner);
						}
						indices[innerCounter] = index;
						innerCounter++;
					}
					// for(int i = 0; i < indices.length; i++)
					// logger.error("INDEX: " + i + " Wert: " + indices[i]);
					// Ordne die Indices im Uhrzeigersinn

					indices = arrangeTriangleIndices(indices);
					tempTriangle = new Triangle(indices, this);
					mTriangles.add(tempTriangle);

				} else {
					LOGGER.trace("SKIPPED TRIANGLE");

				}

				corners = new ArrayList<Vertex3d>(3);
				indices = new Integer[3];
				count = 0;
			} else
				count++;
		}
	}

	// ------------------------------------------------------------------------------------------

	public AbstractComplex getComplexParent() {
		return mComplexParent;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Speichert das komplexe Parent-Objekt im Quad und in allen seinen Kindern
	 */
	public void setComplexParent(AbstractComplex mComplexParent) {
		this.mComplexParent = mComplexParent;
		// falls Kinder gesetzt sind, uebergebe auch diesen den
		// AbstractPrimitive
		if (hasChildren()) {
			mqChildQuads[0].setComplexParent(mComplexParent);
			mqChildQuads[1].setComplexParent(mComplexParent);
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * saemtliche Initialisierungen eines Faces durchfuehren => Codeduplikation
	 * vermeiden
	 */
	protected void init() {
		mTriangles = new ArrayList<Triangle>();
		mqChildQuads = new AbstractQuad[2];
		mqChildQuads[0] = null;
		mqChildQuads[1] = null;
		mTextureCoords = new HashMap<Integer, MyVector2f>();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * ueberschreibe die Methode der Basisklasse, da der Parent einess Faces ein
	 * komplexes Objekt und kein primitives Grundelement ist
	 */
	@Override
	public List<Vertex3d> getVertices() {

		if (mComplexParent == null) {
			new PrototypeException(
					"Quad.getVertices: Primitive-Parent ist nicht gesetzt");
			return null;
		}

		// liefere die Vertices des Parents zurueck
		return mComplexParent.getVertices();

	}

	// ------------------------------------------------------------------------------------------

	@Override
	public EdgeManager getEdgeManager() {
		return mComplexParent.getEdgeManager().get(0);
	}

	// ------------------------------------------------------------------------------------------

	/** wenn Child-Quads gesetzt sind, dann gebe true zurueck, sonst false */
	@Override
	public boolean hasChildren() {
		for (int i = 0; i < mqChildQuads.length; i++) {
			if (mqChildQuads[i] != null) {
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public void update() {

		if (mComplexParent == null) {
			new PrototypeException(
					"Quad.update: Ohne Primitive-Parent aufgerufen");
			return;
		}

		// Ebene berechnen
		calculatePlane();

		// Mittelpunkt berechnen
		calculateCenter();

		// rufe rekursiv auch die Update-Methoden der Quad-Kinder auf, sofern
		// solche existieren
		if (hasChildren()) {
			mqChildQuads[0].update();
			mqChildQuads[1].update();
		}

		// rufe jetzt die Update-Methode der Kind-Dreiecke auf
		Iterator<Triangle> triangleIter = mTriangles.iterator();
		while (triangleIter.hasNext()) {
			triangleIter.next().update();
		}
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Fuegt dem Quad ein Child-Quad hinzu
	 */
	protected void addChildQuad(AbstractQuad child) {

		boolean addedChild = false;

		for (int i = 0; i < mqChildQuads.length; i++) {
			if (mqChildQuads[i] == null) {
				mqChildQuads[i] = child;
				addedChild = true;
				break;
			}
		}

		if (!addedChild)
			new PrototypeException(
					"Quad.addChild: Es wurden mehr als zwei Kinder zum Quad hinzugefuegt");

	}

	// ------------------------------------------------------------------------------------------

	public void setDirection(Side meDirection) {
		this.meDirection = meDirection;
	}

	// ------------------------------------------------------------------------------------------

	public List<Triangle> getTriangles() {
		return mTriangles;
	}

	// ------------------------------------------------------------------------------------------

	public void setTriangles(ArrayList<Triangle> mTriangles) {
		this.mTriangles = mTriangles;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * liefert die Dreiecke auf der untersten Ebene eines Quads
	 */
	public List<Triangle> getChildTriangles() {

		List<Triangle> result = new ArrayList<Triangle>();

		// wenn das aktuelle Quad keine Kinder hat, gebe dessen Triangles
		// zurueck
		if (!hasChildren()) {
			return getTriangles();
		} else {
			for (int i = 0; i < this.mqChildQuads.length; i++) {

				// sonst rufe die Methode rekursiv fuer die Kindelemente auf
				result.addAll(mqChildQuads[i].getChildTriangles());
			}
		}
		return result;
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public Integer[] getIndices() {
		return mIndices;
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public void setIndices(Integer[] mIndices) {
		this.mIndices = mIndices;
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public Side getDirection() {
		return meDirection;
	}

	// ------------------------------------------------------------------------------------------

	private void addTriangle(Triangle addable) {
		if (mTriangles == null)
			mTriangles = new ArrayList<Triangle>();
		mTriangles.add(addable);
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public void updateParent(iGraphicPrimitive parent) {

		if (parent != null)
			setParent(parent);

		// durchlaufe alle Dreiecke und setze das aktuelle Quad als Parent
		Iterator<Triangle> triangleIter = this.mTriangles.iterator();
		while (triangleIter.hasNext()) {
			triangleIter.next().updateParent(this);
		}

		// nun durchlaufe alle Child-Quads und rufe deren Update-Parent-Methode
		// auf
		if (hasChildren()) {
			mqChildQuads[0].updateParent(this);
			mqChildQuads[1].updateParent(this);
		}

	}

	// ------------------------------------------------------------------------------------------

	@Override
	public void generateID(String baseID, String concat) {
		String id = "";
		if (baseID == null) {
			new PrototypeException(
					"AbstractQuad.generateID: Aufruf ohne BaseID, Quads muessen Parent-Elemente besitzen");
			return;
		}
		// pruefe, ob das Parent des aktuellen Objekts den gleichen Typ besitzt
		// (dann enthaelt die ID als Typ "quad")
		if (baseID.startsWith(getType() + "_")) {
			// der Parent besitzt den gleichen Typ, konkateniere die uebergebene
			// Nummer an die baseID
			if (concat == null) {
				new PrototypeException(
						"AbstractQuad.generateID: Aufruf eines Child-Objekts ohne Konkatenierungsnummer");
				return;
			} else {
				id += baseID + "#" + concat;
			}
		} else {
			// sonst handelt es sich um ein Parent-Objekt mit anderem Typ =>
			// ersetze den Typ durch den Typ des aktuellen Objekts
			// extrahiere die generierte ID-Nummer aus der baseID und baue eine
			// neue ID (umgeben von _xxx_)
			int startID = baseID.indexOf("_");
			if (startID == -1) {
				assert false : "FEHLER";
				new PrototypeException(
						"AbstractQuad.generateID: Aufruf mit ungueltiger BaseID: "
								+ baseID);
				return;
			}

			int endID = baseID.lastIndexOf("_");
			if (endID == -1) {
				new PrototypeException(
						"AbstractQuad.generateID: Aufruf mit ungueltiger BaseID: "
								+ baseID);
				return;
			}

			String idNumber = baseID.substring(startID, endID);

			// baue die neue ID um die extrahierte ID-Nummer und fuege die
			// concat Nummer an, sofern sie uebergeben wurde
			id = getType() + "_" + idNumber + "_";
			if (concat != null)
				id += concat;

		}
		// id wurde fertig generiert, wenn Kinder vorliegen, rufe deren
		// Generierungsmethoden auf
		setID(id);

		if (hasChildren()) {
			mqChildQuads[0].generateID(id, "0");
			mqChildQuads[1].generateID(id, "1");
		}
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public void updateReferences() {

		Iterator<Triangle> triangleIter = mTriangles.iterator();
		while (triangleIter.hasNext()) {
			triangleIter.next().updateReferences();
		}

		if (hasChildren()) {
			mqChildQuads[0].updateReferences();
			mqChildQuads[1].updateReferences();
		}

	}

	// ------------------------------------------------------------------------------------------

	@Override
	public Set<Integer> getAllIndices() {

		Set<Integer> result = new HashSet<Integer>();

		// wenn Triangles durch Tesselation erzeugt wurden, verwende ihre
		// Indices
		if (mTriangles.size() > 0) {
			Iterator<Triangle> triangleIter = mTriangles.iterator();
			while (triangleIter.hasNext()) {
				result.addAll(triangleIter.next().getAllIndices());
			}

			// wenn Kinder da sind, dann rufe die Methode rekursiv fuer die
			// Kinder auf
			if (hasChildren()) {
				result.addAll(mqChildQuads[0].getAllIndices());
				result.addAll(mqChildQuads[1].getAllIndices());
			}
		}
		// sonst verwende die Indices, die im Quad selber gespeichert sind
		else {
			for (int i = 0; i < mIndices.length; i++) {
				result.add(mIndices[i]);
			}
		}

		// wenn das aktuelle Quad Loecher enthaelt, fuege auch deren Indices zum
		// Set hinzu
		if (hasHoles()) {
			AbstractQuad currentHole = null;
			Integer[] currentIndices = null;

			for (int j = 0; j < mHoles.size(); j++) {
				currentHole = mHoles.get(j);
				currentIndices = currentHole.getIndices();

				for (int i = 0; i < currentIndices.length; i++)
					result.add(currentIndices[i]);
			}
		}

		/*
		 * System.out.println("ANZAHL INDICES: " + result.size() +
		 * " AUSRICHTUNG: " + getDirection()); Iterator<Integer> indIter =
		 * result.iterator(); int i = 0; while(indIter.hasNext()) {
		 * System.out.println("INDEX " + i + ": " + indIter.next()); i++; }
		 */
		return result;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode dient dem Zugriff auf die berechneten Texturkoordinaten ueber den
	 * Index des jeweiligen Vertex innerhalb des lokalen Faces
	 * 
	 * @param index
	 *            Index des Vertex, fuer das die Texturkoordinaten geholt werden
	 *            sollen
	 * @return Zweidimensionaler Vektor mit den u- und v-Koordinaten fuer das
	 *         Texturing des Vertex
	 */
	public MyVector2f getTextureCoordsByIndex(Integer index) {
		// assert index >= 0 && mTextureCoords.size() > index:
		// "Index out of Range, fuer das Vertex existieren keine Texturkoordinaten";
		MyVector2f result = mTextureCoords.get(index);
		assert result != null : "Es existieren keine Texturkoordinaten an Index "
				+ index
				+ " fuer Facetyp: "
				+ getType()
				+ " mit Ausrichtung: "
				+ getDirection();
		// if(result == null) return new MyVector2f(0.0f, 0.0f);
		return result.clone();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mTextureCoords
	 *            the mTextureCoords to set
	 */
	public void setTextureCoords(Map<Integer, MyVector2f> mTextureCoords) {
		this.mTextureCoords = mTextureCoords;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * @return Texturkoordinaten
	 */
	public Map<Integer, MyVector2f> getTextureCoords() {
		return this.mTextureCoords;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode kapselt die Kategorienhierarchie vor der restlichen Klasse
	 * 
	 * @param category
	 *            Kategorie, fuer die das aktuelle Objekt auf Zugehoerigkeit
	 *            getestet wird
	 * @return True, falls das aktuelle Objekt zu dieser Kategorie gehoert,
	 *         False sonst
	 */
	protected boolean isQuadInCategory(String category) {

		// wenn die Wand texturiert werden soll, nehme alle Quads ausser BOTTOM
		// und TOP
		if (category.equalsIgnoreCase("wall")
				|| category.equalsIgnoreCase("test")) {

			if (!getDirection().equals(Side.TOP)
					&& !getDirection().equals(Side.BOTTOM))
				return true;
			else
				return false;

		}
		// wenn es sich um eine Ground-Textur handelt, wird die Ausrichtung
		// immer auf UNKNOWN gesetzt
		else if (category.equalsIgnoreCase("ground")) {
			if (getDirection().equals(Side.UNKNOWN))
				return true;
			else
				return false;
		}
		// bei Dachflaechen ist eine exakte Orientierung nicht anzugeben
		else if (category.equalsIgnoreCase("roof")) {
			if (getDirection().equals(Side.ROOF)
					|| getDirection().equals(Side.ROOF_SIDE))
				return true;
			else
				return true;
		}

		assert false : "FEHLER: Fuer die uebergebene Kategorie: " + category
				+ " existiert keine Zuordnung";
		return false;

	}

	// ------------------------------------------------------------------------------------------

	public void subdivideQuad(subdivisionType type, float subdivisionFactor) {
		// TODO Auto-generated method stub

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erstellt eine Kopie des aktuellen Quads und gibt diese zurueck
	 * 
	 * @param complexParent
	 *            Parent-Complex des aktuellen Quads
	 * @return Deep-Copy des aktuellen Quads
	 */
	public AbstractQuad clone(AbstractComplex complexParent) {

		AbstractQuad result = null;
		try {
			// ermittle die Runtime-Klasse mittels Reflection => dadurch wird
			// bei Clones immer der richtige Quad-Typ zurueckgegeben
			Class clazz = this.getClass();
			result = (AbstractQuad) clazz.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		result.init();

		// kopiere alle Indices
		int numberOfIndices = mIndices.length;
		Integer[] indices = new Integer[numberOfIndices];
		for (int i = 0; i < numberOfIndices; i++)
			indices[i] = new Integer(mIndices[i]);

		result.setIndices(indices);
		result.setDirection(getDirection());
		result.setComplexParent(complexParent);

		if (mHoles.size() > 0) {
			for (int i = 0; i < mHoles.size(); i++)
				result.addHole(mHoles.get(i).clone(complexParent));
		}

		// erzeuge basierend auf den Indices neue Dreiecke im Result-Quad
		// diese greifen direkt auf die kopierten Kanten im Edge-Manager zu
		result.tesselate();

		// wenn Kinder vorhanden sind, dann kopiere die Kinder in das neue Quad
		if (hasChildren()) {
			AbstractQuad childQuad = mqChildQuads[0].clone(complexParent);
			childQuad.setParent(this);
			result.addChildQuad(childQuad);

			childQuad = mqChildQuads[1].clone(complexParent);
			childQuad.setParent(this);
			result.addChildQuad(childQuad);
		}

		// gebe die Kopie zurueck
		return result;

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode setzt Texturen fuer eine bestimmte Kategorie von Quads. Die
	 * Entscheidung, ob ein Quad dabei zu einer bestimmten Kategorie gehoert,
	 * wird dem Quad selber ueberlassen, dieses fuegt sich dann selbststaendig
	 * zur Parent-HashMap hinzu.
	 * 
	 * @param category
	 *            Kategorie von Quads, denen die uebergebene Textur zugeordnet
	 *            werden soll
	 * @param texture
	 *            Textur, die zugeordnet werden soll
	 */
	public void setTextureForCategory(String category, Texture texture) {

		// pruefe, ob das Quad zur Anfragekategorie gehoert, wenn ja, adde es
		// zur Textur-HashMap
		if (isQuadInCategory(category)) {
			mComplexParent.addTextureToMap(getID(), texture);
			texture.addReference();
		}

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode durchlaeuft alle Texturkoordinaten, die fuer das aktuelle Quad
	 * berechnet wurden und skaliert sie mittels des uebergebenen
	 * Scaling-Faktors
	 * 
	 * @param minScaleFaktor
	 *            Faktor, mit dem die Texturkoordinaten skaliert werden
	 */
	public void scaleTextureCoordinates(float minScaleFaktor) {
		MyVectormath mathHelper = MyVectormath.getInstance();
		MyVector2f current = null;
		Iterator<MyVector2f> coordIter = mTextureCoords.values().iterator();
		while (coordIter.hasNext()) {
			current = coordIter.next();
			current.scale(minScaleFaktor);
			mathHelper.roundVector2f(current);
		}

		// validiere, dass alle berechneten und skalierten Koordinaten innerhalb
		// des gueltigen Texturraums von 0-1 liegen

		// validateTexturspaceRanges();

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode wird nach der Berechnung der Texturkoordinaten aufgerufen und
	 * testet, ob die Texturkoordinaten alle innerhalb des gueltigen Ranges von
	 * 0.0f <= x <= 1.0f liegen. Wenn dies nicht der Fall ist, wird ein
	 * AssertionError ausgeloest.
	 */
	protected void validateTexturspaceRanges() {
		Collection<MyVector2f> textureCoords = mTextureCoords.values();
		Iterator<MyVector2f> coordIter = textureCoords.iterator();
		MyVector2f currentCoords = null;
		while (coordIter.hasNext()) {
			currentCoords = coordIter.next();
			if (!(currentCoords.x >= 0.0f && currentCoords.x <= 1.0f)) {
				assert false : "Texturkoordinaten liegen ausserhalb des gueltigen Bereichs: "
						+ currentCoords + " fuer " + this;
			}
			if (!(currentCoords.y >= 0.0f && currentCoords.y <= 1.0f)) {
				assert false : "Texturkoordinaten liegen ausserhalb des gueltigen Bereichs: "
						+ currentCoords + " fuer " + this;
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode durchlaeuft die uebergebenen Indices und ordnet diese im
	 * Uhrzeigersinn an. Dies ist bsw. fuer die Normalenberechnung zentral, da
	 * die Reihenfolge die Ausrichtung definiert. Der Ansatz projiziert alle
	 * Vertices eines Dreiecks in die xy-Ebene, und berechnet anschliessend die
	 * Winkel zwischen einer Geraden vom Dreiecksmittelpunkt zu den Eckpunkten
	 * in Bezug auf eine Referenzgerade. Anschliessend sortiert man die Punkte
	 * aufsteigend bzgl. ihrer Winkel. Abschliessend muss noch geprueft werden,
	 * in welcher Ausrichtung die Quellebene stand, da dies u.U. eine Aenderung
	 * des Drehsinns erfordert. Dadurch ist es moeglich, den Uhrzeigersinn zu
	 * erzwingen (wobei das Startvertex zunaechst irrelevant ist)
	 * 
	 * @param indices
	 *            Anzuordnende Indices
	 * @return Geordnete Indices
	 */
	private Integer[] arrangeTriangleIndices(Integer[] indices) {

		List<Vertex3d> vertices = mComplexParent.getVertices();
		List<Vertex3d> triangleVertices = new ArrayList<Vertex3d>(3);
		List<Vertex3d> projectedVertices = new ArrayList<Vertex3d>(3);

		MyVectormath mathHelper = MyVectormath.getInstance();

		// verwende die Quadnormale, nicht die Triangle-Normale
		MyVector3f normal = getNormal();

		LOGGER.debug("Ausgangsindices: 0: " + indices[0] + " 1: " + indices[1]
				+ " 2: " + indices[2]);

		for (int i = 0; i < indices.length; i++)
			triangleVertices.add(vertices.get(indices[i]));

		LOGGER.debug("Ausrichtung: " + getDirection());
		LOGGER.debug("Normale: " + normal);

		projectedVertices = mathHelper.calculateXYPlaneProjectionForPoints(
				triangleVertices, normal);

		Axis ignorableAxis = mathHelper.getIgnorableAxis(normal, true);

		MyVector3f projectedTriangleCenter = mathHelper
				.calculateTriangleCenter(projectedVertices);
		Vertex3d center = new Vertex3d(projectedTriangleCenter);

		LOGGER.debug("Vertices: Center: " + center + " Vert0: "
				+ projectedVertices.get(0) + " Vert1: "
				+ projectedVertices.get(1) + " Vert2: "
				+ projectedVertices.get(2));

		// definiere einen Referenzstrahl
		Ray reference = new Ray(center.getPosition(), new MyVector3f(1.0f,
				0.0f, 0.0f));

		// berechne nun Winkel aller Punkte zu diesem Referenzstrahl
		Ray centerToStart = new Ray(center, projectedVertices.get(0));
		Ray centerToSecond = new Ray(center, projectedVertices.get(1));
		Ray centerToThird = new Ray(center, projectedVertices.get(2));

		List<Double> angles = new ArrayList<Double>(3);

		angles.add(mathHelper.getFullAngleRad(reference.getDirection(),
				centerToStart.getDirection()));
		angles.add(mathHelper.getFullAngleRad(reference.getDirection(),
				centerToSecond.getDirection()));
		angles.add(mathHelper.getFullAngleRad(reference.getDirection(),
				centerToThird.getDirection()));

		LOGGER.debug("ANGLES: 0: " + angles.get(0) + " 1: " + angles.get(1)
				+ " 2: " + angles.get(2));

		// sortiere die Winkel aufsteigend
		List<Double> sortedAngles = sortAngles(angles);
		Integer[] newIndices = new Integer[3];

		for (int i = 0; i < sortedAngles.size(); i++) {
			// greife ueber die Liste mit den unsortierten Winkeln auf die
			// Ausgangsindices zu und ordne diese basierend auf der Sortierung
			// neu an
			newIndices[i] = indices[angles.indexOf(sortedAngles.get(i))];
		}

		// wenn eine negative Achse oder eine positive z-Komponente gefunden
		// wurde, aendere die Drehrichtung, abhaengig von der Ausrichtung des
		// Parent-Quads
		if (ignorableAxis.equals(Axis.NEGATIVE_X)
				|| ignorableAxis.equals(Axis.NEGATIVE_Y)
				|| ignorableAxis.equals(Axis.Z)) {
			int temp = newIndices[1];
			newIndices[1] = newIndices[2];
			newIndices[2] = temp;

		}

		return newIndices;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode sortiert eine Liste mit Winkeln aufsteigend
	 * 
	 * @param angles
	 *            Liste mit zu sortierenden Winkeln
	 * @return Sortierte Kopie der Eingabeliste
	 */
	private List<Double> sortAngles(List<Double> angles) {
		List<Double> copiedAngles = new ArrayList<Double>(3);
		copiedAngles.addAll(angles);

		PrototypeHelper.getInstance().sortDoubles(copiedAngles);
		return copiedAngles;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode fuegt der AbstractQuad-Instanz ein Loch hinzu
	 * 
	 * @param hole
	 *            Loch, beschrieben durch ein Quad
	 */
	public void addHole(AbstractQuad hole) {
		if (mHoles.indexOf(hole) == -1)
			mHoles.add(hole);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode liefert saemtliche Loecher, die sich innerhalb des aktuellen
	 * Quads befinden
	 * 
	 * @return Liste mit allen Loechern im Quad
	 */
	public List<AbstractQuad> getHoles() {
		return mHoles;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Setter fuer Holes-Listen
	 * 
	 * @param holes
	 *            Liste mit AbstractQuad-Instanzen, die Loecher innerhalb eines
	 *            Quads beschreiben
	 */
	public void setHoles(List<AbstractQuad> holes) {
		mHoles = holes;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode prueft, ob das aktuelle Quad Loecher aufweist, die durch
	 * Beschneidungsberechnungen entstanden sind
	 * 
	 * @return True, falls bereits Quads gespeichert wurden, die die Loecher
	 *         beschreiben, False sonst
	 */
	public Boolean hasHoles() {
		if (mHoles.size() > 0)
			return true;
		else
			return false;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode liefert alle Vertices der Quad-Outline (dabei werden die
	 * Dreiecksindices nicht beruecksichtigt!)
	 * 
	 * @return Liste mit Vertices, die ueber das Index-Array des Quads
	 *         referenziert werden
	 */
	public List<Vertex3d> getQuadVertices() {

		List<Vertex3d> vertices = getVertices();
		List<Vertex3d> quadVertices = new ArrayList<Vertex3d>(mIndices.length);
		for (int i = 0; i < mIndices.length; i++) {
			// logger.error("INDEX: " + mIndices[i] + " VERT: " +
			// vertices.get(mIndices[i]));
			quadVertices.add(vertices.get(mIndices[i]));
		}
		return quadVertices;

	}

	// ------------------------------------------------------------------------------------------
	@Override
	/**
	 * Methode wird vor der endgueltigen Zerstoerung durch den GC gecallt, loescht alle Members
	 */
	protected void finalize() throws Throwable {
		LOGGER.trace("Zerstoere Quad mit ID " + getID());

		for (int i = 0; i < mTriangles.size(); i++)
			mTriangles.get(i).destroy();
		mTriangles.clear();

		for (int i = 0; i < mHoles.size(); i++)
			mHoles.get(i).destroy();
		mHoles.clear();
		mComplexParent = null;
		for (int i = 0; i < mqChildQuads.length; i++) {
			if (mqChildQuads[i] == null)
				continue;
			mqChildQuads[i].destroy();
			mqChildQuads[i] = null;
		}
		mTextureCoords.clear();
		meDirection = null;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode errechnet eine Polygon-Repraesentation fuer die Outline des
	 * aktuellen Quads
	 * 
	 * @return
	 */
	public MyPolygon getPolygon() {
		if (mQuadOutlinePoly == null)
			mQuadOutlinePoly = new MyPolygon(getQuadVertices());
		return mQuadOutlinePoly;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode verschiebt das Quad um den uebergebenen Richtungsvektor
	 * 
	 * @param translation
	 *            Verschiebungsvektor
	 */
	public void translate(MyVector3f translation) {

		List<Vertex3d> quadVerts = getQuadVertices();
		for (int i = 0; i < quadVerts.size(); i++)
			quadVerts.get(i).getPositionPtr().add(translation);

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode verschiebt das Quad um den uebergebenen Richtungsvektor, erzeugt
	 * dabei aber neue Vertices und aktualisiert die Indices etc.
	 * 
	 * @param translation
	 *            Verschiebungsvektor
	 */
	public void translateNewVerts(MyVector3f translation) {

		List<Vertex3d> quadVerts = getQuadVertices();
		List<Vertex3d> clonedVerts = new ArrayList<Vertex3d>(quadVerts.size());
		for (int i = 0; i < quadVerts.size(); i++) {
			clonedVerts.add(quadVerts.get(i).clone());
			clonedVerts.get(i).getPositionPtr().add(translation);
		}

		// Vertices zum Parentbuffer adden
		List<Vertex3d> verts = getComplexParent().getVertices();
		verts.addAll(clonedVerts);

		// Indices des Quads auf die neuen Vertices umstellen
		int index = -1;
		Integer[] indices = new Integer[clonedVerts.size()];
		for (int i = 0; i < clonedVerts.size(); i++) {
			index = verts.indexOf(clonedVerts.get(i));
			assert index != -1 : "FEHLER: Vertex "
					+ clonedVerts.get(i)
					+ " befindet sich nicht im Vertex-Buffer des Parentobjekts!";
			indices[i] = index;
		}

		setIndices(indices);

		// durch die neuen Vertices sind die Indices der Dreiecke ungueltig,
		// berechne darum eine neue Tesselation
		tesselate();
		update();

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * @return the mIndoor
	 */
	public boolean isIndoor() {
		return mIndoor;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mIndoor
	 *            the mIndoor to set
	 */
	public void setIndoor(boolean mIndoor) {
		this.mIndoor = mIndoor;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode flippt die Indices des Quads
	 */
	public void flipIndices() {

		Integer[] currentIndices = getIndices();
		Integer[] flippedIndices = new Integer[currentIndices.length];

		int count = 0;
		for (int i = (currentIndices.length - 1); i >= 0; i--) {
			flippedIndices[count] = currentIndices[i];
			mIndices = flippedIndices;
			count++;
		}

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet einen Strahl, der eine horizontale Kante des
	 * uebergebenen Quads beschreibt. Ueber diesen Strahl kann die Breite des
	 * Quads ermittelt werden.
	 * 
	 * @param quad
	 *            Quad-Instanz, fuer die eine horizontale Kante berechnet werden
	 *            soll
	 * @return Strahl, der die horizontale Kante des Quads beschreibt
	 */
	public Ray getHorizontalRayForQuad() {

		MyVectormath mathHelper = MyVectormath.getInstance();

		// fuer Decken oder Bodenflaechen kann die horizontale Breite nicht
		// sinnvoll ermittelt werden
		if (isTopOrBottom())
			return null;

		List<Vertex3d> quadVerts = getQuadVertices();

		// sortiere die Vertices basierend auf ihren y-Koordinaten (horizontale
		// Kante besteht aus Punkten mit gleicher Hoehe
		PrototypeHelper.getInstance().sortVerticesByYCoordinate(quadVerts);

		// verwende die ersten beiden Vertices in der sortierten Liste
		Vertex3d start = quadVerts.get(0);
		Vertex3d end = quadVerts.get(1);

		// toleranzbasierter Vergleich
		if (!mathHelper.isWithinTolerance(start.getY(), end.getY(), 0.5f)) {
			LOGGER.error("Quadverts mit unterschiedlicher Hoehe entdeckt: Start: "
					+ start + " End: " + end + " Quad: " + getID());
			return null;
		}

		return new Ray(start, end);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet einen vertikalen Strahl, der eine vertikale Kante
	 * innerhalb des uebergebenen Quads beschreibt. Diese Berechnung kann fuer
	 * die Bestimmung der Quad-Hoehe eingesetzt werden.
	 */
	public Ray getVerticalRayForQuad() {

		// fuer Decken oder Bodenflaechen kann die horizontale Breite nicht
		// sinnvoll ermittelt werden
		if (isTopOrBottom())
			return null;

		return getPolygon().getVerticalRay();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Utility-Methode,testet, ob das aktuelle Quad ein Decken- oder Bodenquad
	 * ist
	 * 
	 * 
	 * @return True, falls es ein Decken- oder Bodenquad ist, False sonst
	 */
	public boolean isTopOrBottom() {
		if (getDirection().equals(Side.TOP)
				|| getDirection().equals(Side.BOTTOM))
			return true;
		else
			return false;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mContentType
	 */
	public ModelCategory getContentType() {
		return mContentType;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mContentType
	 *            the mContentType to set
	 */
	public void setContentType(ModelCategory mContentType) {
		this.mContentType = mContentType;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode liefert alle Loecher innerhalb eines Quads, deren Content-Type
	 * der Uebergabekategorie entspricht. Wenn das exactMatch-Flag gesetzt ist,
	 * wird ein Loch nur bei exakter Uebereinstimmung geliefert, sonst wird per
	 * Stringmatching verglichen
	 * 
	 * @param category
	 *            KModellkategorie, fuer die Loecher gesucht werden sollen
	 * @param exactMatch
	 *            Flag gibt an, ob exakt oder per Stringmatching verglichen
	 *            werden soll
	 * @return Liste mit Loechern, an deren Stelle komplexe Objekte aus der
	 *         Uebergabekategorie zu finden sind
	 */
	public List<AbstractQuad> getHolesByContent(final ModelCategory category,
			final boolean exactMatch) {

		List<AbstractQuad> resultHoles = new ArrayList<AbstractQuad>(
				mHoles.size());
		if (mHoles.size() == 0)
			return resultHoles;

		AbstractQuad currentHole = null;
		String targetCategory = category.toString();

		for (int i = 0; i < mHoles.size(); i++) {
			currentHole = mHoles.get(i);
			assert currentHole.getContentType() != null : "FEHLER: Fuer das Loch wurde kein ContentType deklariert!";

			if (exactMatch) {
				if (currentHole.getContentType().equals(category))
					resultHoles.add(currentHole);
			}
			// beim Nicht-Exakten-Matching werden Loecher zum Ergebnis geadded,
			// sobald die Kategorien in irgendeiner Beziehung zueinander stehen
			else {
				String currentCategoryString = currentHole.getContentType()
						.toString();
				if (targetCategory == currentCategoryString
						|| currentCategoryString.contains(targetCategory)
						|| targetCategory.contains(currentCategoryString))
					resultHoles.add(currentHole);
			}
		}

		return resultHoles;
	}
	// ------------------------------------------------------------------------------------------

}
