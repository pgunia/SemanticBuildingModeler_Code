package semantic.building.modeler.prototype.graphics.primitives;

import java.util.List;

import processing.core.PImage;
import semantic.building.modeler.math.MyVector2f;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.MyVectormath;
import semantic.building.modeler.math.Ray;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.prototype.enums.subdivisionType;

/**
 * @author Patrick Gunia
 * 
 *         Ein Quad ist eine Struktur, die durch 4 Vertexindices beschrieben
 *         wird sie wird weiter unterteilt in jeweils 2 Dreiecke Quads koennen
 *         Kind-Quads haben, die eine Unterteilung des Ausgangsquads beschreiben
 *         die Indices werden im Uhrzeigersinn definiert und starten immer links
 *         unten
 * 
 */

public class Quad extends AbstractQuad {

	/**
	 * Speichert Metainformationen ueber das Quad, die fuer das Texturing
	 * relevant sind
	 */
	protected PrimitiveMetaInformation mMetaInformation = null;

	// ------------------------------------------------------------------------------------------
	public Quad() {
		init();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * saemtliche Initialisierungen eines Faces durchfuehren => Codeduplikation
	 * vermeiden
	 */
	@Override
	protected void init() {
		super.init();
		mIndices = new Integer[4];
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet 2 Dreiecke aus den Indices
	 * 
	 * @deprecated ersetzt durch GLU-Tesselation
	 */
	@Deprecated
	public void createTrianglesByIndices() {

		// erzeuge das erste Dreieck
		Integer[] indices = new Integer[3];
		indices[0] = mIndices[0];
		indices[1] = mIndices[1];
		indices[2] = mIndices[2];

		Triangle tempTriangle = new Triangle(indices, this);
		mTriangles.add(tempTriangle);

		// erzeuge das 2. Dreieck
		indices = new Integer[3];
		indices[0] = mIndices[2];
		indices[1] = mIndices[3];
		indices[2] = mIndices[0];

		tempTriangle = new Triangle(indices, this);
		mTriangles.add(tempTriangle);

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * unterteilt das Face in horizontaler oder vertikaler Richtung im
	 * Verhaeltnis des subdivisionFactors Ergebnis ist die Erzeugung zweier
	 * neuer Faces als Kindelemente des Ausgangsfaces
	 */
	@Override
	public void subdivideQuad(subdivisionType type, float subdivisionFactor) {

		// die Unterteilung soll rekursiv moeglich sein
		// wenn ein Quad bereits Kinder besitzt, dann unterteile die Kinder
		if (!hasChildren()) {

			Line[] lineBuffer = new Line[2];

			// abhaengig vom Unterteilungstyp unterscheiden sich die Indices,
			// die
			// fuer die Kindelemente benoetigt werden
			if (type == subdivisionType.HORIZONTAL) {

				// hole die vertikalen Lines (sind immer die ersten beiden Lines
				// der Basisdreiecke)
				lineBuffer[0] = mTriangles.get(0).getEdges()[0];
				lineBuffer[1] = mTriangles.get(1).getEdges()[0];
			} else {
				// hole die horizontalen Lines (sind immer die zweiten beiden
				// Lines
				// der Basisdreiecke)
				lineBuffer[0] = mTriangles.get(0).getEdges()[1];
				lineBuffer[1] = mTriangles.get(1).getEdges()[1];
			}

			calculateSubdivision(lineBuffer, subdivisionFactor, type);

			// rufe fuer die neu erzeugten Kind-Quads die Update-Methode auf, um
			// Normalen und Mittelpunkte zur errechnen
			mqChildQuads[0].update();
			mqChildQuads[1].update();

			// es wurden neue Quads erzeugt
			// generiere IDs
			mqChildQuads[0].generateID(getID(), "0");
			mqChildQuads[1].generateID(getID(), "1");

		} else {
			mqChildQuads[0].subdivideQuad(type, subdivisionFactor);
			mqChildQuads[1].subdivideQuad(type, subdivisionFactor);
		}

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * berechnet die neuen Faces, zunaechst werden die neuen Vertices bestimmt,
	 * anschliessend die neuen Indices
	 */
	private void calculateSubdivision(Line[] lineBuffer,
			float subdivisionFactor, subdivisionType type) {

		// hole einen Pointer auf das Vertexarray des Parent
		List<Vertex3d> vertexPointer = getVertices();

		// hole die Vertexpositions als Vektoren, verwende Kopien, um
		// Seiteneffekte zu vermeiden
		MyVector3f vecLine1Vert1 = vertexPointer.get(
				lineBuffer[0].getIndices()[0]).getPosition();
		MyVector3f vecLine1Vert2 = vertexPointer.get(
				lineBuffer[0].getIndices()[1]).getPosition();
		MyVector3f vecLine2Vert1 = vertexPointer.get(
				lineBuffer[1].getIndices()[0]).getPosition();
		MyVector3f vecLine2Vert2 = vertexPointer.get(
				lineBuffer[1].getIndices()[1]).getPosition();

		// Berechnung des neuen Vertices:
		// teile die uebergebenen Lines anhand des uebergebenen Faktors
		// sorge dafuer, dass die Vektoren beide in die gleiche Richtung zeigen
		MyVector3f line1 = new MyVector3f();
		line1.sub(vecLine1Vert1, vecLine1Vert2);

		MyVector3f line2 = new MyVector3f();
		line2.sub(vecLine2Vert2, vecLine2Vert1);

		// multipliziere die Line-Vektoren mit dem Unterteilungsfaktor
		line1.scale(subdivisionFactor);
		line2.scale(subdivisionFactor);

		// addiese die so skalierten Vektoren auf die Startvertices, um einen
		// Punkt auf der Geraden zu erhalten

		MyVector3f newVert1Pos = new MyVector3f();
		newVert1Pos.add(line1, vecLine1Vert2);

		MyVector3f newVert2Pos = new MyVector3f();
		newVert2Pos.add(line2, vecLine2Vert1);

		// bestimme die Indices der neuen Vertices waehrend des Hinzufuegens zum
		// Vertex-Buffer
		int nextIndex1 = vertexPointer.size();

		Vertex3d newVert1 = new Vertex3d(newVert1Pos);
		vertexPointer.add(newVert1);

		int nextIndex2 = vertexPointer.size();
		Vertex3d newVert2 = new Vertex3d(newVert2Pos);
		vertexPointer.add(newVert2);

		// unterteile bereits jetzt die Lines, die uebergeben wurden anhand der
		// neuen Indices
		// die so erzeugten Lines werden spaeter bei der Quad-Erstellung ueber
		// den EdgeManager geholt
		lineBuffer[0].subdivideLine(nextIndex1);
		lineBuffer[1].subdivideLine(nextIndex2);

		// hole abhaengig vom Subdivision-Type die Indices aus dem Indexarray
		Integer[] indexBuffer = new Integer[4];

		if (type == subdivisionType.HORIZONTAL) {

			// horizontal: behalte fuer das erste Quad Indices 1 und 2 (Zaehlung
			// beginnt bei 0)
			// fuer das zweite Quad 3 und 0

			// erzeuge die Indices fuer das erste neue Quad
			indexBuffer[0] = mIndices[0];
			indexBuffer[1] = nextIndex1;
			indexBuffer[2] = nextIndex2;
			indexBuffer[3] = mIndices[3];

			// erzeuge ein neues Quad mit den Index-Werten
			AbstractQuad tempQuad = new Quad();
			tempQuad.setIndices(indexBuffer);

			// die Richtung bleibt auch bei Unterteilung erhalten
			tempQuad.setDirection(getDirection());
			tempQuad.setParent(this);
			tempQuad.setComplexParent(getComplexParent());
			// tempQuad.createTrianglesByIndices();
			tempQuad.tesselate();
			addChildQuad(tempQuad);

			// erzeuge das zweite Kindquad
			indexBuffer = new Integer[4];
			indexBuffer[0] = nextIndex1;
			indexBuffer[1] = mIndices[1];
			indexBuffer[2] = mIndices[2];
			indexBuffer[3] = nextIndex2;

			// erzeuge ein neues Quad mit den Index-Werten
			tempQuad = new Quad();
			tempQuad.setIndices(indexBuffer);

			// die Richtung bleibt auch bei Unterteilung erhalten
			tempQuad.setDirection(getDirection());
			tempQuad.setParent(this);
			tempQuad.setComplexParent(getComplexParent());
			tempQuad.tesselate();
			// tempQuad.createTrianglesByIndices();
			addChildQuad(tempQuad);

		} else {

			// vertikal: behalte fuer das erste Quad Indices 0 und 1 (Zaehlung
			// beginnt bei 0)
			// fuer das zweite Quad 2 und 3
			indexBuffer[0] = mIndices[0];
			indexBuffer[1] = mIndices[1];
			indexBuffer[2] = nextIndex1;
			indexBuffer[3] = nextIndex2;

			// erzeuge ein neues Quad mit den Index-Werten
			AbstractQuad tempQuad = new Quad();
			tempQuad.setIndices(indexBuffer);

			// die Richtung bleibt auch bei Unterteilung erhalten
			tempQuad.setDirection(getDirection());
			tempQuad.setParent(this);
			tempQuad.setComplexParent(getComplexParent());
			// tempQuad.createTrianglesByIndices();
			tempQuad.tesselate();
			addChildQuad(tempQuad);

			// erzeuge das zweite Kindquad
			indexBuffer = new Integer[4];
			indexBuffer[0] = nextIndex2;
			indexBuffer[1] = nextIndex1;
			indexBuffer[2] = mIndices[2];
			indexBuffer[3] = mIndices[3];

			// erzeuge ein neues Quad mit den Index-Werten
			tempQuad = new Quad();
			tempQuad.setIndices(indexBuffer);

			// die Richtung bleibt auch bei Unterteilung erhalten
			tempQuad.setDirection(getDirection());
			tempQuad.setParent(this);
			tempQuad.setComplexParent(getComplexParent());
			// tempQuad.createTrianglesByIndices();
			tempQuad.tesselate();
			addChildQuad(tempQuad);
		}

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet den Mittelpunkt des Quads => arithmetisches Mittel der
	 * 4 Eckpunkte => typspezifisch
	 */
	@Override
	public void calculateCenter() {

		List<Vertex3d> vertices = getVertices();

		// hole die Vertices ueber die Indices
		Vertex3d vert1 = vertices.get(mIndices[0]);
		Vertex3d vert2 = vertices.get(mIndices[1]);
		Vertex3d vert3 = vertices.get(mIndices[2]);
		Vertex3d vert4 = null;
		try {
			vert4 = vertices.get(mIndices[3]);

		} catch (Exception e) {
			LOGGER.error("FEHLER: " + mIndices.length);
		}

		// berechne das arithmetische Mittel fuer alle Komponenten
		float x = (vert1.getX() + vert2.getX() + vert3.getX() + vert4.getX()) / 4;
		float y = (vert1.getY() + vert2.getY() + vert3.getY() + vert4.getY()) / 4;
		float z = (vert1.getZ() + vert2.getZ() + vert3.getZ() + vert4.getZ()) / 4;

		MyVector3f center = new MyVector3f(x, y, z);
		setCenter(center);

	}

	// ------------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "quad";
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet die Texturkoordinaten der Quad-Vertices in
	 * Abhaengigkeit von den Texturausdehnungen. Hierbei gilt immer, dass
	 * Wall-Quads Vierecke sind, deren Indices eine festgelegte Reihenfolge
	 * besitzen
	 * 
	 * @param textureWidth
	 *            Breite der Textur in px
	 * @param textureHeight
	 *            Hoehe der Textur in px
	 * @return Verhaeltnis von Textur- zu Elementausdehnung, Rueckgabe ist der
	 *         kleinere der beiden Verhaeltniswerte (Hoehe / Breite)
	 */
	public float computeWallTexturing(int textureWidth, int textureHeight) {

		computeMetainformation();
		createTextureCoords(textureWidth, textureHeight);

		return mMetaInformation.getScaleFactor();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode befuellt die HashMap mit Texturkoordinaten fuer die
	 * Vertexindices. Vertices liegen immer im Uhrzeigersinn vor, beginnend mit
	 * start, end, endChild, endStart. Berechnet werden die Koordinaten
	 * basierend auf den berechneten Ausdehnungen des Trapez unter
	 * Beruecksichtigung der uebergebenen Hoehen- und Breitenangaben der Textur.
	 * 
	 * @param width
	 *            Breite der Textur in Pixeln
	 * @param height
	 *            Hoehe der Textur in Pixeln
	 */
	protected void createTextureCoords(float width, float height) {
		mTextureCoords.clear();

		float widthScaleFactor, heightScaleFactor, widthProportion, heightProportion, scaleFactor = 1;
		float offset = 0.0f;

		assert mMetaInformation != null : "FEHLER: Es wurde keine PrimitiveMetaInformation-Instanz angelegt!";

		/*
		 * System.out.println("Texturausdehnungen: Breite: " + width +
		 * " Hoehe: " + height + " Offset: " + offset);
		 * System.out.println("Faceelement: Breite: " + mWidthLowerEdge +
		 * " Hoehe: " + mHeight);
		 */
		// Verhaeltnis von Texturhoehe zu Elementhoehe => wie oft passt das
		// Element in der Hoehendimension in die Textur
		heightProportion = height / mMetaInformation.getHeight();

		// Verhaeltnis von Elementhoehe zur Texturhoehe
		heightScaleFactor = mMetaInformation.getHeight() / height;

		// offset = 0;

		// Trapezfall
		if (!mMetaInformation.isIsParallelogramm()) {
			// System.out.println("Trapezfall");
			// offset = 0;
			// wie oft kann die Textur in der Breite bzgl. der unteren Kante
			// gemappt werden
			widthScaleFactor = mMetaInformation.getWidthLowerEdge() / width;

			// wie oft passt das Element in der Breite in die Textur
			// widthProportion = width / (mWidthLowerEdge + offset * width);

			scaleFactor = -1;
			// mExceptionCase = false;
			// handelt es sich um ein Trapez, bei dem die obere Kante kuerzer
			// ist, als die untere (Regelfall)?
			if (!mMetaInformation.isExceptionCase()) {
				// System.out.println("Standardfall");
				// untere Kante
				// rechtes Vertex
				mTextureCoords.put(mIndices[0], new MyVector2f(widthScaleFactor
						+ offset, heightScaleFactor));

				// linkes Vertex
				mTextureCoords.put(mIndices[1], new MyVector2f(offset,
						heightScaleFactor));

				// obere Kante
				// linkes Vertex
				mTextureCoords
						.put(mIndices[2],
								new MyVector2f(
										(mMetaInformation.getDeltaX2() / width)
												+ offset, 0.0f));

				// rechtes Vertex
				mTextureCoords.put(mIndices[3], new MyVector2f(
						(widthScaleFactor - mMetaInformation.getDeltaX3()
								/ width)
								+ offset, 0.0f));

				// der Offset bestimmt sich immer aus der u-Koordinate des
				// Vertex, an dem das darueberliegende Face ansetzt
				// bei Trapezen ist dies immer Vertex mit Index 2
				// mTextureOffsetULeft = mDeltaX2 / width + offset;
				// mTextureOffsetURight = mWidthLowerEdge / width + offset;
			}
			// oder ist der Ausnahmefall aufgetreten?
			else {
				// untere Kante
				// rechtes Vertex
				mTextureCoords
						.put(mIndices[0],
								new MyVector2f((mMetaInformation
										.getWidthLowerEdge() + mMetaInformation
										.getDeltaX2())
										/ width + offset, heightScaleFactor));

				// linkes Vertex
				mTextureCoords.put(mIndices[1],
						new MyVector2f(mMetaInformation.getDeltaX2() / width
								+ offset, heightScaleFactor));

				// obere Kante
				// linkes Vertex
				mTextureCoords.put(mIndices[2], new MyVector2f(offset, 0.0f));
				// rechtes Vertex

				mTextureCoords.put(mIndices[3],
						new MyVector2f(mMetaInformation.getWidthUpperEdge()
								/ width + offset, 0.0f));

				// tritt der Sonderfall auf, so aendert sich der Offset nicht,
				// da der Ansatzpunkt des naechsten
				// Elements bzgl. des UV-Raumes unveraendert bleibt
				// mTextureOffsetULeft = offset;
				// mTextureOffsetURight = mWidthUpperEdge / width + offset;
			}

			// beim Trapezfall ist die Verhaeltnisbestimmung einfacher als beim
			// Parallelogramm, man nimmt die laengere Kante
			// beruecksichtige bei der Berechnung den Offset => wenn man das
			// nicht tut, kann es dazu kommen, dass die Skalierung des
			// Texturraum sprengt
			// offset * texturbreite ist die Uebersetzung der Offset-Ausdehnung
			// in absolute Werte, die auf die Breite des Elements addiert werden
			// muessen
			if (mMetaInformation.getWidthUpperEdge() > mMetaInformation
					.getWidthLowerEdge()) {
				widthProportion = width
						/ (mMetaInformation.getWidthUpperEdge() + offset
								* width);
			} else {
				widthProportion = width
						/ (mMetaInformation.getWidthLowerEdge() + offset
								* width);
			}

		}
		// Parallelogramm-Fall (deckt auch entartete Parallelogramme ab, bei
		// denen obere und untere Kante unterschiedlich lang sind)
		else {
			// System.out.println("Parallelogrammfall");
			// berechne die Verhaeltnisse fuer den Parallelogrammfall anders,
			// bei der Breite muss das "Ueberragen" beruecksichtigt
			// werden, sonst kann es vorkommen, dass die Texturkoordinaten den
			// Raum sprengen

			// Parallelogramm ist zum Vertex mit Index 0 geneigt
			if (mMetaInformation.isSlopedToStart()) {
				// System.out.println("Neigung zu Vertex 0");
				mTextureCoords.put(mIndices[0],
						new MyVector2f(mMetaInformation.getWidthLowerEdge()
								/ width + offset, heightScaleFactor));
				mTextureCoords.put(mIndices[1], new MyVector2f(offset,
						heightScaleFactor));
				mTextureCoords
						.put(mIndices[2],
								new MyVector2f(
										(mMetaInformation.getDeltaX2() / width)
												+ offset, 0.0f));
				mTextureCoords
						.put(mIndices[3],
								new MyVector2f((mMetaInformation
										.getWidthUpperEdge() + mMetaInformation
										.getDeltaX2())
										/ width + offset, 0.0f));

				// mTextureOffsetULeft = mDeltaX2 / width + offset;
				// mTextureOffsetURight = (mWidthUpperEdge + mDeltaX2) / width +
				// offset;

			}
			// Parallelogramm ist zum Vertex mit Index 1 geneigt
			else {
				// System.out.println("Neigung zu Vertex 1");

				mTextureCoords
						.put(mIndices[0],
								new MyVector2f((mMetaInformation
										.getWidthLowerEdge() + mMetaInformation
										.getDeltaX2())
										/ width + offset, heightScaleFactor));
				mTextureCoords.put(mIndices[1], new MyVector2f(
						(mMetaInformation.getDeltaX2() / width) + offset,
						heightScaleFactor));
				mTextureCoords.put(mIndices[2], new MyVector2f(offset, 0.0f));
				mTextureCoords.put(mIndices[3],
						new MyVector2f(mMetaInformation.getWidthUpperEdge()
								/ width + offset, 0.0f));

				// in diesem Sonderfall ist der Eingabeoffset gleich dem
				// Ausgabeoffset, da das Vertex im Texturraum keine zusaetzliche
				// Verschiebung in der u-Dimension aufweist
				// mTextureOffsetULeft = offset;
				// mTextureOffsetURight = (mWidthLowerEdge + mDeltaX2) / width +
				// offset;

			}

			// bestimme den Breitenproportionsfaktor
			// aufgrund der Struktur des Parallelogramms (auch im entarteten
			// Fall) gilt, dass die Lange je einer Kante mit einem der beiden
			// Deltawerte
			// gleich der Lange der anderen mit dem jeweils anderen Delta-Wert
			// ist, man muss nur die Kombination bestimmen
			float testLaenge = mMetaInformation.getWidthLowerEdge()
					+ mMetaInformation.getDeltaX2();
			float testLaenge2 = mMetaInformation.getWidthUpperEdge()
					+ mMetaInformation.getDeltaX3();
			if (testLaenge == testLaenge2) {
				widthProportion = width / (testLaenge + offset * width);
			} else {
				testLaenge = mMetaInformation.getWidthLowerEdge()
						+ mMetaInformation.getDeltaX3();
				widthProportion = width / (testLaenge + offset * width);
			}

		}

		// verwende den kleineren der beiden Faktoren, damit die Koordinaten
		// nicht aus dem Texturraum herauslaufen
		if (widthProportion >= heightProportion) {
			scaleFactor = heightProportion;
			// System.out.println("Verwende HeightProportion: " +
			// heightProportion);
		} else {
			scaleFactor = widthProportion;
			// System.out.println("Verwende WidthProportion: " +
			// widthProportion);
		}

		mMetaInformation.setScaleFactor(scaleFactor);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet alle relevanten Parameter fuer das Texturmapping, dazu
	 * gehoeren Ausdehnungen und Ausrichtung des Elements
	 */
	protected void computeMetainformation() {
		// Breite bezieht sich auf die untere Kante des Trapez

		List<Vertex3d> vertices = mComplexParent.getVertices();

		Vertex3d vert0 = vertices.get(mIndices[0]);
		Vertex3d vert1 = vertices.get(mIndices[1]);
		Vertex3d vert2 = vertices.get(mIndices[2]);
		Vertex3d vert3 = vertices.get(mIndices[3]);

		MyVectormath mathHelper = MyVectormath.getInstance();

		mMetaInformation = new PrimitiveMetaInformation();
		/*
		 * System.out .println(
		 * "Starte Berechnung der Texturkoordinaten fuer Quad-Face mit: untenStart: "
		 * + vert0.getPositionPtr() + " untenEnd: " + vert1.getPositionPtr() +
		 * " obenEnd: " + vert2.getPositionPtr() + " obenStart: " +
		 * vert3.getPositionPtr());
		 */
		MyVector3f direction = new MyVector3f();
		direction.sub(vert1.getPosition(), vert0.getPosition());
		mMetaInformation.setWidthLowerEdge(direction.length());
		Ray lowerEdge = new Ray(vert0.getPosition(), direction);

		// berechne die Delta-Werte
		// bestimme Schnittpunkte einer zur Grundlinie senkrechten Kante durch
		// die oberen Vertices mit der Grundlinie
		// bestimme die Richtung des Strahls durch Rotation der oberen Kante um
		// 90°
		MyVector3f upperEdge = new MyVector3f();

		// Bestimme Richtung und Laenge der oberen Kante
		upperEdge.sub(vert2.getPosition(), vert3.getPosition());
		mMetaInformation.setWidthUpperEdge(upperEdge.length());

		MyVector3f rayDirection = mathHelper.calculateRotatedVector(
				mPlane.getNormal(), upperEdge, 90.0f);

		rayDirection.normalize();

		Ray startRay = new Ray(vert3.getPosition(), rayDirection);
		Ray endRay = new Ray(vert2.getPosition(), rayDirection);

		// berechne Schnittpunkte
		MyVector3f schnittpunktX3 = mathHelper
				.calculateRay2RayIntersectionApproximation(startRay, lowerEdge);
		MyVector3f schnittpunktX2 = mathHelper
				.calculateRay2RayIntersectionApproximation(endRay, lowerEdge);

		assert schnittpunktX3 != null && schnittpunktX2 != null : "Es konnten keine Schnittpunkte mit der Grundkante ermittelt werden";

		// teste, ob sich beide Schnittpunkte auf dem Liniensegment der unteren
		// Kante befinden
		boolean endPointOnSegment = mathHelper.isPointOnLineSegment(
				schnittpunktX2, lowerEdge);
		boolean startPointOnSegment = mathHelper.isPointOnLineSegment(
				schnittpunktX3, lowerEdge);

		// Hoehe des Trapez bestimmen => Abstand zwischen den Endpunkten der
		// senkrechten Kante
		mMetaInformation.setHeight(mathHelper.calculatePointPointDistance(
				startRay.getStart(), schnittpunktX3));

		// berechne die Delta-Werte
		mMetaInformation.setDeltaX3(mathHelper.calculatePointPointDistance(
				schnittpunktX3, vert0.getPosition()));
		mMetaInformation.setDeltaX2(mathHelper.calculatePointPointDistance(
				schnittpunktX2, vert1.getPosition()));

		// berechne die Abstaende der Schnittpunkte von den Vertices
		float distX2ToVert0 = mathHelper.calculatePointPointDistance(
				schnittpunktX2, vert0.getPosition());
		float distX3ToVert0 = mathHelper.calculatePointPointDistance(
				schnittpunktX3, vert0.getPosition());
		float distX2ToVert1 = mathHelper.calculatePointPointDistance(
				schnittpunktX2, vert1.getPosition());
		float distX3ToVert1 = mathHelper.calculatePointPointDistance(
				schnittpunktX3, vert1.getPosition());

		// setze nun die Flags abhaengig von der Struktur des Quads
		// koennte wahrscheinlich kuerzer gemacht werden, ist so aber
		// offensichtlicher
		// wenn beide Schnittpunkte auf der unteren Kante liegen, hat man ein
		// "regulaeres" Trapez
		if (endPointOnSegment && startPointOnSegment) {
			// System.out.println("Regulaeres Trapez");
			mMetaInformation.setIsParallelogramm(false);
			mMetaInformation.setExceptionCase(false);
		}

		// wenn beide Schnittpunkte nicht auf dem unteren Segment liegen, kann
		// es sowohl ein gedrehtes Trapez
		// als auch ein extrem verzerrtes Parallelogramm sein
		else if (!endPointOnSegment && !startPointOnSegment) {

			// es handelt sich um ein verzerrtes Parallelogramm, wenn beide
			// Schnittpunkte bzgl. der Vertices auf der gleichen Seite
			// liegen, also bsw. beide Schnittpunkte rechts von Vert0 oder links
			// von Vert1
			if ((distX2ToVert0 < distX2ToVert1)
					&& (distX3ToVert0 < distX3ToVert1)) {
				// verzerrtes Parallelogramm mit Neigung zu Vert0
				mMetaInformation.setIsParallelogramm(true);
				mMetaInformation.setExceptionCase(false);
				mMetaInformation.setSlopedToStart(true);
			} else if ((distX2ToVert0 > distX2ToVert1)
					&& (distX3ToVert0 > distX3ToVert1)) {
				// verzerrtes Parallelogramm mit Neigung zu Vert 1
				mMetaInformation.setIsParallelogramm(true);
				mMetaInformation.setExceptionCase(false);
				mMetaInformation.setSlopedToStart(false);
			}
			// es handelt sich um ein gedrehtes Trapez
			else {
				// System.out.println("Gedrehtes Trapez");
				mMetaInformation.setIsParallelogramm(false);
				mMetaInformation.setExceptionCase(true);
			}
		} else if (endPointOnSegment) {
			// regulaeres Parallelogramm mit Neigung zu Vert 0
			mMetaInformation.setIsParallelogramm(true);
			mMetaInformation.setExceptionCase(false);
			mMetaInformation.setSlopedToStart(true);
		} else if (startPointOnSegment) {
			// regulaeres Parallelogramm mit Neigung zu Vert 1
			mMetaInformation.setIsParallelogramm(true);
			mMetaInformation.setExceptionCase(false);
			mMetaInformation.setSlopedToStart(false);
		} else
			assert false : "Nicht beruecksichtiger Sonderfall bzgl. der Elementausrichtung";
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode fuegt der HashMap des komplexen Parent-Objektes ein
	 * Key-Value-Pair bestehend aus dem uebergebenen Schluessel und der
	 * uebergebenen Textur hinzu.
	 * 
	 * @param id
	 *            Key, unter dem die Textur in der HashMap abgelegt wird
	 * @param texture
	 *            Textur, die dem Schluessel zugeordnet wird
	 */
	private void setTextureForID(String id, PImage texture) {

	}

	// ------------------------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String message = "Quad: Direction: " + getDirection() + mLineBreak;

		List<Vertex3d> vertices = mComplexParent.getVertices();

		for (int i = 0; i < mIndices.length; i++) {
			message += "Vertex " + (mIndices[i] + 1) + ": "
					+ vertices.get(mIndices[i]) + " Texturkoordinaten: "
					+ getTextureCoordsByIndex(mIndices[i]) + mLineBreak;
		}
		/*
		 * message += "Ebene: " + mPlane + mLineBreak; message += "Center: " +
		 * mCenter + mLineBreak; message += "Direction: " + meDirection +
		 * mLineBreak;
		 */

		return message;
	}

	// ------------------------------------------------------------------------------------------

}
