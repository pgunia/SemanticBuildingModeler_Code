package semantic.building.modeler.prototype.graphics.interfaces;

import javax.media.opengl.GL;

import semantic.building.modeler.configurationservice.model.enums.Side;
import semantic.building.modeler.math.Axis;
import semantic.building.modeler.prototype.enums.subdivisionType;
import semantic.building.modeler.prototype.exporter.ExportFormat;
import semantic.building.modeler.prototype.service.Texture;
import semantic.building.modeler.prototype.service.TextureManagement.TextureCategory;

/**
 * 
 * @author Patrick Gunia Interface fuer alle komplexen Grafikelemente. Definiert
 *         die Kernmethoden, die fuer die Modifikation einer Instanz verwendet
 *         werden
 * 
 */

public interface iGraphicComplex extends Cloneable {

	/** erzeuge das Primitive */
	public void create();

	/** zeichne das Primitive */
	public void draw(final Boolean drawTextures);

	/** zeichne das Primitive ueber direkt OpenGL-Calls */
	public void drawGL(final Boolean drawTextures, GL gl);

	/**
	 * Extrudiere das jeweilige Element entlang einer vorgegebenen Achse um den
	 * jeweiligen Amount Dieser Amount ist ein relativer Wert, ueber den die
	 * Vertex-Koordinaten skaliert werden
	 */
	public void extrude(final Side whichFace, final Axis extrudeAxis,
			final float extrudeAmount);

	/**
	 * Unterteilt das / die ausgewaehlte(n) Face(s) in x-, y- oder z-Richtung
	 * Richtung anhand des subdivideFactor-Verhaeltnisses
	 */
	public void subdivideQuad(final Side whichFace, final subdivisionType type,
			final float subdivisionFactor);

	/**
	 * Unterteilt das Primitive und nicht seine Faces entsprechend des
	 * jeweiligen Unterteilungstyps
	 */
	public iGraphicComplex subdivide(final subdivisionType type,
			final float subdivisionFactor);

	/**
	 * Methode berechnet fuer das komplexe Objekt ein Dach mit Hilfe des
	 * Straight Skeleton Algorithmus
	 */
	public void computeRoof();

	/**
	 * Methode fordert vom Texturmanagement eine Textur aus der uebergebenen
	 * Kategorie an und speichert diese in den Zielquads
	 * 
	 * @param category
	 *            Konstante aus der TextureCategory-Enumg
	 */
	public void setTextureByCategory(final TextureCategory category);

	/**
	 * Methode wird dazu verwendet, um einem komplexen Objekt bzgl. einer
	 * bestimmten Kategorie (bsw. Roof oder Wall) eine Textur zuzuweisen.
	 * 
	 * @param category
	 *            Kategorie, fuer die eine Textur festgelegt wird
	 * @param texture
	 *            Die zu verwendende Textur
	 */
	public void setTexture(final String category, final Texture texture);

	/**
	 * Methode exportiert das aktuelle Modell im uebergebenen Dateiformat in die
	 * uebergebene Zieldatei
	 * 
	 * @param filename
	 *            Dateiname der Exportdatei
	 * @param format
	 *            Exportformat
	 */
	public void exportModelToFile(final String path, final String filename,
			final ExportFormat format);

	/**
	 * Methode, die alle Klassen implementieren muessen, die sich bei der
	 * Objektverwaltung anmelden wollen
	 */
	public void register();

	/**
	 * Methode, die alle Klassen implementieren muessen, die sich ueber
	 * register() bei der Objektverwaltung anmelden, dient dem Abmelden bsw. bei
	 * der Zertörung der jeweiligen Objekte
	 */
	public void unregister();

}
