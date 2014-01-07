package semantic.building.modeler.prototype.exporter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import semantic.building.modeler.math.MyVector2f;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.prototype.graphics.complex.AbstractComplex;
import semantic.building.modeler.prototype.graphics.primitives.AbstractQuad;
import semantic.building.modeler.prototype.graphics.primitives.Triangle;

/**
 * 
 * @author Patrick Gunia Klasse realisiert den Export komplexer Objekte in das
 *         obj-3d-Format WICHTIG: Beim 3ds-Import Flag FLIP Y/Z-Axis aktivieren
 * 
 */

public class ObjExport extends AbstractModelExport {

	/** Liste mit Texturkoordinaten des komplexen Objekts */
	private List<MyVector2f> mRescaledTextureCoordinatesBuffer = null;

	/**
	 * Liste mit skalierten Texturkoordinaten, die das Hochrechnen des Bildes
	 * innerhalb des Systems durch ein Tiling ersetzen, das in Modellingtools
	 * unterstuetzt wird
	 */
	private Map<MyVector2f, MyVector2f> mRescaledTextureCoordinatesMap = null;

	/** Liste mit den Normalenvektoren aller verwendeten Faces */
	private List<MyVector3f> mNormalBuffer = null;

	/**
	 * Index des letzten geschriebenen Vertex, wenn mehrere komplexe Objekte
	 * exportiert werden, speichert diese Variable den Index des letzten Vertex
	 * des vorher exportierten Objekts
	 */
	private Integer mLastVertexIndex = 0;

	/**
	 * Analog zum Vertex-Index, speichert den Index der letzten geschriebenen
	 * Texturkoordinaten
	 */
	private Integer mLastTextureIndex = 0;

	/**
	 * Analog: letzter Index eines vorher geschriebenen Normalenvektors
	 */
	private Integer mLastNormalIndex = 0;

	/**
	 * Wenn fuer 3ds exportiert wird, muessen andere
	 * Koordinatenachsenausrichtungen etc. verwendet werden
	 */
	private boolean mExportTo3dsMax = true;

	/**
	 * Fester Skalierungsfaktor fuer Texturkoordinaten => alle Koordinaten
	 * werden uniform mit diesem Faktor skaliert
	 */
	private Integer mTextureScaling = 4;

	@Override
	protected void exportComplex(AbstractComplex complex) {

		LOGGER.debug("Exportiere Objekt: " + complex.getID());

		mRescaledTextureCoordinatesBuffer = new ArrayList<MyVector2f>();
		mRescaledTextureCoordinatesMap = new HashMap<MyVector2f, MyVector2f>();
		mNormalBuffer = new ArrayList<MyVector3f>();

		// schreibe zunaechst alle Vertices in die Outputdatei
		List<Vertex3d> vertices = complex.getVertices();

		writeMessage("# " + vertices.size() + " Vertices");
		Iterator<Vertex3d> vertIter = vertices.iterator();
		while (vertIter.hasNext()) {
			writeVertex(vertIter.next());
		}

		// durchlaufe alle Quads und adde die Texturekoordinaten zum
		// Koordinatenbuffer
		List<AbstractQuad> quads = complex.getAllQuads();

		LOGGER.debug("Exportierte " + quads.size() + " Quads");

		Iterator<AbstractQuad> quadIter = quads.iterator();
		AbstractQuad currentQuad = null;
		Iterator<MyVector2f> coordinateIter = null;
		MyVector2f currentCoord = null, currentCoordRescaled = null;

		MyVector3f currentNormal = null;
		/*
		 * Texture currentTexture = null; Map<String, Texture> complexTextures =
		 * complex.getTextureMap();
		 */
		while (quadIter.hasNext()) {
			currentQuad = quadIter.next();

			// Quadnormale zum Buffer hinzufuegen
			currentNormal = currentQuad.getNormal();
			if (mNormalBuffer.indexOf(currentNormal) == -1)
				mNormalBuffer.add(currentNormal);

			// TEXTURABHAENGIGE KOORDINATENSKALIERUNG => wird zunaechst nicht
			// weiter verwendet, zu fehleranfaellig, ausserdem erreicht man mit
			// hoeheren Skalierungsfaktoren bessere Ergebnisse
			/*
			 * currentTexture = complexTextures.get(currentQuad.getID()); //
			 * aendere den Scalingfactor nur, wenn fuer das Quad eine Textur
			 * gesetzt ist (muss nicht zwingend der Fall sein, da bsw.
			 * Wall-Texturen BOTTOM und TOP nicht zugewiesen if(currentTexture
			 * != null) { // currentTextureScaling =
			 * currentTexture.getTextureScaleFactor();
			 * 
			 * } logger.debug("TextureScaling: " + currentTextureScaling +
			 * " fuer Ausrichtung: " + currentQuad.getDirection());
			 */

			// ueber alle Texturkoordinaten des aktuellen Quads iterieren
			coordinateIter = currentQuad.getTextureCoords().values().iterator();

			while (coordinateIter.hasNext()) {
				currentCoord = coordinateIter.next();
				currentCoordRescaled = currentCoord.clone();

				// skaliere die Texturen zurueck, drehe fuer 3ds zunaechst den
				// Koordinatenraum
				if (mExportTo3dsMax) {
					currentCoordRescaled.x = 1.0f - currentCoordRescaled.x;
					currentCoordRescaled.y = 1.0f - currentCoordRescaled.y;
				}

				currentCoordRescaled.scale(mTextureScaling);

				// fuege die skalierten Koordinaten zum Buffer hinzu
				if (mRescaledTextureCoordinatesBuffer
						.indexOf(currentCoordRescaled) == -1) {
					mRescaledTextureCoordinatesBuffer.add(currentCoordRescaled);
				}

				// erzeuge ein Key-Value-Pair aus skalierter und unskalierter
				// Komponente
				if (!mRescaledTextureCoordinatesMap.containsKey(currentCoord)) {
					mRescaledTextureCoordinatesMap.put(currentCoord,
							currentCoordRescaled);
				} else {
					MyVector2f oldValue = mRescaledTextureCoordinatesMap
							.get(currentCoord);
					assert oldValue.x == currentCoordRescaled.x
							&& oldValue.y == currentCoordRescaled.y : "FEHLER: Fuer Key "
							+ currentCoord
							+ " existieren unterschiedliche Values: Old: "
							+ oldValue + " new:  " + currentCoordRescaled;
				}
			}
		}

		// assert mRescaledTextureCoordinatesBuffer.size() ==
		// mRescaledTextureCoordinatesMap.size():
		// "FEHLER: Unterschiedliche Anzahl von Koordinatenvektoren in den verwendeten Buffern, dies fuerht zu falschen Faceexports. Buffer: "
		// + mRescaledTextureCoordinatesBuffer.size() + " Map: " +
		// mRescaledTextureCoordinatesMap.size();
		if (mRescaledTextureCoordinatesBuffer.size() != mRescaledTextureCoordinatesMap
				.size())
			LOGGER.error("FEHLER: Unterschiedliche Anzahl von Objekten in Buffer und Map: Buffer: "
					+ mRescaledTextureCoordinatesBuffer.size()
					+ " Map: "
					+ mRescaledTextureCoordinatesMap.size());

		// schreibe die Texturkoordinaten in die Zieldatei
		writeTextureCoordinates();

		// schreibe Normalenvektoren in Zieldatei
		writeNormals();

		writeGroup(complex);

		// schreibe die Face-Outputs
		quadIter = quads.iterator();
		List<Triangle> triangles = null;
		Iterator<Triangle> triangleIter = null;
		Integer numberOfFaces = 0;
		Triangle currentTriangle = null;
		while (quadIter.hasNext()) {
			currentQuad = quadIter.next();
			triangles = currentQuad.getTriangles();
			// System.out.println("CURRENTQUAD-TYPE: " + currentQuad.getType());
			// if(currentQuad.getType().equals("trianglequad"))
			// System.out.println("ANZAHL DREIECKE: " + triangles.size());
			numberOfFaces += triangles.size();
			triangleIter = triangles.iterator();
			while (triangleIter.hasNext()) {
				currentTriangle = triangleIter.next();
				LOGGER.debug(currentTriangle);
				writeTriangle(currentTriangle, currentQuad);
			}
		}

		writeMessage("# " + numberOfFaces + " Faces");

		// aktualisiere vor einem eventuell weiteren Export die Indices
		// (Zaehlung beginnt immer bei 1)
		mLastVertexIndex = vertices.size() + mLastVertexIndex;
		mLastTextureIndex = mRescaledTextureCoordinatesBuffer.size()
				+ mLastTextureIndex;
		mLastNormalIndex = mNormalBuffer.size() + mLastNormalIndex;

		LOGGER.debug("LastVertexIndex: " + mLastVertexIndex
				+ " LastTextureIndex: " + mLastTextureIndex
				+ " LastNormalIndex: " + mLastNormalIndex);
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode exportiert ein einzelnes Dreieck beschrieben durch eine Liste von
	 * Vertices
	 * 
	 * @param triangle
	 *            Dreieck, dessen Indices etc. in die Zieldatei geschrieben
	 *            werden
	 * @param quad
	 *            Elternquad des zu schreibenden Dreiecks
	 */
	private void writeTriangle(Triangle triangle, AbstractQuad quad) {

		Integer[] indices = triangle.getIndices();
		Integer[] textureIndices = new Integer[3];
		Integer normalIndex = null;
		MyVector2f currentCoords = null, currentRescaledCoords = null;
		Integer currentIndex = null;

		// ermittle den Index des Normalenvektors im Normalenbuffer => dies muss
		// fuer jedes Dreieck nur einmal getan werden,
		// da alle Vertices eines Dreiecks die gleichen Normalenvektoren
		// zugewiesen bekommen
		normalIndex = mNormalBuffer.indexOf(quad.getNormal());

		// ermittle die Indices der Texturkoordinaten ueber den
		// Koordinatenbuffer
		for (int i = 0; i < indices.length; i++) {
			currentIndex = indices[i];

			// ermittle den Index der Texturkoordinaten im Buffer
			currentCoords = quad.getTextureCoordsByIndex(currentIndex);
			currentRescaledCoords = mRescaledTextureCoordinatesMap
					.get(currentCoords);
			assert currentRescaledCoords != null : "FEHLER: Es existiert keine Zurodnung des Schluessels "
					+ currentCoords + " zu einem skalierten Vektor";
			textureIndices[i] = mRescaledTextureCoordinatesBuffer
					.indexOf(currentRescaledCoords);
		}

		// erzeuge die Faces
		String line = "f ";
		// inkrementiere immer um 1, da die obj-File-Zaehlung bei 1 beginnt
		for (int i = 0; i < indices.length; i++) {
			line += (indices[i] + 1 + mLastVertexIndex) + "/"
					+ (textureIndices[i] + 1 + mLastTextureIndex) + "/"
					+ (normalIndex + 1 + mLastNormalIndex) + " ";
		}

		mWriter.println(line);

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Schreibt Group-Statement basierend auf der erzeugten ObjektID
	 * 
	 * @param complex
	 *            Komplexes Objekt, das im Output zu einer Group zusammengefasst
	 *            werden soll
	 */
	private void writeGroup(AbstractComplex complex) {
		String output = "g " + complex.getID();
		mWriter.println(output);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Schreibt die Vertex-Koordinaten in den Output;
	 * 
	 * @param vertex
	 *            Vertex, dessen Koordinaten exportiert werden
	 */
	private void writeVertex(Vertex3d vertex) {
		String output;

		// 3ds verwendet die z-Achse als Hoehenachse, weiterhin muss die interne
		// y-Koordinate noch gedreht werden, da das Koordinatensystem eine
		// andere Ausrichtung aufweist
		if (mExportTo3dsMax)
			output = "v " + vertex.getX() + " " + vertex.getZ() + " "
					+ vertex.getY() * -1;
		else
			output = "v " + vertex.getX() + " " + vertex.getY() + " "
					+ vertex.getZ();
		mWriter.println(output);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Export die skalierten Texturkoordinaten aller verwendeten Dreiecke
	 */
	private void writeTextureCoordinates() {

		writeMessage("#" + mRescaledTextureCoordinatesMap.size()
				+ " texture coords");

		Iterator<MyVector2f> coordIter = mRescaledTextureCoordinatesBuffer
				.iterator();

		String line = null;
		MyVector2f currentCoord = null;
		while (coordIter.hasNext()) {
			currentCoord = coordIter.next();
			line = "vt " + currentCoord.x + " " + currentCoord.y + " 0.0";
			mWriter.println(line);
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode schreibt alle Normalenvektoren in das Output-File, die in einer
	 * ersten Berechnungsphase aus den Face-Quads extrahiert wurden
	 */
	private void writeNormals() {
		writeMessage("#" + mNormalBuffer.size() + " vertex-normals");
		Iterator<MyVector3f> normalIter = mNormalBuffer.iterator();
		MyVector3f currentNormal = null;

		String line = null;
		while (normalIter.hasNext()) {
			currentNormal = normalIter.next();

			if (mExportTo3dsMax)
				line = "vn " + currentNormal.x + " " + currentNormal.z + " "
						+ currentNormal.y * -1.0f;
			else
				line = "vn " + currentNormal.x + " " + currentNormal.y + " "
						+ currentNormal.z;

			// line = "vn " + currentNormal.x + " " + currentNormal.y + " " +
			// currentNormal.z;
			mWriter.println(line);
		}
	}

	// ------------------------------------------------------------------------------------------

	@Override
	protected String getFileExtension() {
		return "obj";
	}

	// ------------------------------------------------------------------------------------------

}
