package semantic.building.modeler.prototype.importer;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import processing.core.PApplet;
import processing.core.PVector;
import saito.objloader.Face;
import saito.objloader.OBJModel;
import saito.objloader.Segment;
import semantic.building.modeler.configurationservice.model.enums.Side;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.prototype.graphics.primitives.AbstractQuad;
import semantic.building.modeler.prototype.graphics.primitives.PolygonalQuad;
import semantic.building.modeler.prototype.graphics.primitives.Quad;
import semantic.building.modeler.prototype.graphics.primitives.TriangleQuad;
import semantic.building.modeler.prototype.service.PrototypeHelper;

/**
 * 
 * @author Patrick Gunia Klasse implementiert das Laden von OBJ-3d-Modellen und
 *         greift dabei auf die OBJLoader-Library
 *         http://code.google.com/p/saitoobjloader/ zurueck.
 * 
 */

public class ObjImport extends AbstractModelImport {

	/** Instanz des aus der Datei geladenen Modells */
	private OBJModel mModel = null;

	/** Liste mit allen Vertices des geladenen Modells */
	private List<Vertex3d> mVertices = null;

	/**
	 * Liste mit AbstractQuad-Instanzen, die fuer die Faces des geladenen Models
	 * erstellt werden
	 */
	private List<AbstractQuad> mQuadFaces = null;

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode durchlaeuft alle Vertices des geladenen Modells und erstellt
	 * Vertex3d-Instanzen fuer jeden geladenen Positionsvektor
	 * 
	 */
	private void extractVertices() {
		assert mModel != null : "FEHLER: Es wurde kein Model geladen";

		// hole den Vertex-Buffer aus dem Model
		List<PVector> vertexVectors = new ArrayList<PVector>(
				mModel.getVertexCount());
		for (int i = 0; i < mModel.getVertexCount(); i++)
			vertexVectors.add(mModel.getVertex(i));

		// durchlaufe nun alle geladenen Vektoren und erstelle
		// Vertex3d-Instanzen
		mVertices = new ArrayList<Vertex3d>(mModel.getVertexCount());
		Iterator<PVector> positionIter = vertexVectors.iterator();

		PVector currentVector = null;
		Vertex3d currentVertex = null;

		while (positionIter.hasNext()) {
			currentVector = positionIter.next();
			currentVertex = new Vertex3d(currentVector.x, currentVector.y,
					currentVector.z);
			mVertices.add(currentVertex);
		}
		vertexVectors.clear();
		vertexVectors = null;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode durchlaeuft alle Faces des geladenen Modells und erstellt in
	 * Abhaengigkeit der Vertexanzahl unterschiedliche Quads, die dann innerhalb
	 * einer Liste gesammelt werden.
	 */
	private void extractFaces() {

		// hole das Segment
		Segment segment = mModel.getSegment(0);
		Face[] faces = segment.getFaces();
		Face currentFace = null;

		mQuadFaces = new ArrayList<AbstractQuad>(faces.length);

		// durchlaufe die geladenen Faces und erstelle fuer jedes Face ein
		// AbstractQuad in Abhaengigkeit von der Anzahl von Face-Vertices
		for (int i = 0; i < faces.length; i++) {
			currentFace = faces[i];
			AbstractQuad faceQuad = createQuadForFace(currentFace);
			if (faceQuad != null)
				mQuadFaces.add(faceQuad);
		}

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erstellt eine AbstractQuad-Instanz fuer das uebergebene Face. Die
	 * Art des erstellten Quads haengt dabei ab von der Anzahl der Anzahl der
	 * Indices des uebergebenen Faces
	 * 
	 * @param face
	 *            Face, fuer das ein Quad erstellt werden soll
	 * @return AbstractQuad-Instanz
	 */
	private AbstractQuad createQuadForFace(Face face) {

		int numberOfIndices = face.getVertIndexCount();
		// hier kann man sich die Normalen holen, dann spart man sich bei
		// importierten Objekten die Normalenberechnung
		List<PVector> faceNormals = face.normals;

		// berechne eine gemittelte Normale fuer das aktuelle Face basierend auf
		// den importierten Normalenvektoren
		PVector averageNormal = new PVector(0.0f, 0.0f, 0.0f);
		for (int i = 0; i < faceNormals.size(); i++) {
			averageNormal.add(faceNormals.get(i));
		}
		// averageNormal.div(faceNormals.size());
		averageNormal.normalize();

		MyVector3f averageNormalMV = new MyVector3f();
		averageNormalMV.x = averageNormal.x;
		averageNormalMV.y = averageNormal.y;
		averageNormalMV.z = averageNormal.z;

		// Testcase
		List<Integer> indexBuffer = new ArrayList<Integer>(numberOfIndices);

		int[] indices = face.getVertexIndices();
		Integer lastIndex = null;

		for (int i = 0; i < indices.length; i++) {

			// teilweise existieren Models, die mehrmals den gleichen Index
			// hintereinander besitzen, sortiere solche Indices aus
			if (lastIndex != null && indices[i] == lastIndex)
				continue;
			lastIndex = indices[i];
			indexBuffer.add(indices[i]);
			lastIndex = indices[i];
		}

		// durch das Aussortieren koennen Indexfolgen aus weniger als 3 Vertices
		// entstehen, erzeuge in diesem Fall kein Quad
		if (indexBuffer.size() < 3) {
			String message = "Ungueltiges Face geladen, Quellindices: ";
			for (int i = 0; i < indices.length; i++)
				message += indices[i] + "; ";
			LOGGER.trace(message);
			return null;
		}

		List<Vertex3d> quadVerts = new ArrayList<Vertex3d>(indexBuffer.size());
		Integer[] objIndices = new Integer[indexBuffer.size()];
		for (int i = 0; i < indexBuffer.size(); i++) {
			objIndices[i] = indexBuffer.get(i);
			quadVerts.add(mVertices.get(objIndices[i]));
		}

		// pruefe Normalenausrichtung, normalisiere bei Bedarf
		PrototypeHelper helper = PrototypeHelper.getInstance();
		boolean changedOrder = helper.normalizeNormal(quadVerts,
				averageNormalMV);

		// falls die Abfolge geaendert wurde, modifiziere die Indices
		if (changedOrder) {
			for (int i = 0; i < quadVerts.size(); i++) {
				objIndices[i] = mVertices.indexOf(quadVerts.get(i));
			}
		}

		AbstractQuad resultQuad = null;
		if (numberOfIndices == 3) {
			resultQuad = new TriangleQuad();
		} else if (numberOfIndices == 4)
			resultQuad = new Quad();
		else
			resultQuad = new PolygonalQuad();
		resultQuad.setIndices(objIndices);
		resultQuad.setDirection(Side.UNKNOWN);

		return resultQuad;

	}

	// ------------------------------------------------------------------------------------------

	@Override
	public List<Vertex3d> getVertices() {
		return mVertices;
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public List<AbstractQuad> getFaces() {
		return mQuadFaces;
	}

	// ------------------------------------------------------------------------------------------
	public OBJModel getModel() {
		return mModel;
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public void loadModel(File srcFile, PApplet parent) {
		load(srcFile, parent);
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public void loadModel(final String absolutePath, final PApplet parent) {

		File modelFile = new File(absolutePath);
		if (!modelFile.exists())
			throw new RuntimeException("Datei '" + absolutePath
					+ "' konnte nicht geladen werden.");
		load(modelFile, parent);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode fuehrt die erforderlichen Berechnung zum Laden und Verarbeiten
	 * des uebergebenen Modells durch
	 * 
	 * @param src
	 *            Quelldateo
	 * @param parent
	 *            Parent-Applet, wird vom Processing-Loader benoetigt
	 */
	private void load(final File modelFile, final PApplet parent) {

		assert modelFile.exists() : "DATEI NICHT GEFUNDEN!";
		assert parent != null : "PARENT NULL";

		// lade das Modell
		mModel = new OBJModel(parent, modelFile.getAbsolutePath());
		assert mModel.getSegmentCount() == 1 : "FEHLER: Der OBJ-Loader unterstuetzt aktuell nur Modells mit einem einzigen Segment. Das geladene Model enthaelt "
				+ mModel.getSegmentCount() + " Segmente.";

		extractVertices();
		extractFaces();

		LOGGER.info("Model " + modelFile.getName() + ": Vertex-Count: "
				+ mVertices.size() + " Face-Count: " + mQuadFaces.size());

	}
	// ------------------------------------------------------------------------------------------

}
