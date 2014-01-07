package semantic.building.modeler.prototype.service;

import java.util.ArrayList;
import java.util.List;

import semantic.building.modeler.configurationservice.model.enums.FloorPosition;
import semantic.building.modeler.configurationservice.model.enums.ModelCategory;
import semantic.building.modeler.configurationservice.model.enums.Side;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.prototype.enums.HorizontalAlignment;
import semantic.building.modeler.prototype.graphics.complex.AbstractComplex;
import semantic.building.modeler.prototype.graphics.complex.BuildingComplex;

/**
 * 
 * @author Patrick Gunia Klasse buendelt saemtliche erforderlichen Parameter zur
 *         Positionierung von Objekten in einerm Gebaeude
 * 
 */

public class PositionConfig {

	/** Gebaeude, innerhalb dessen die Objekte positioniert werden sollen */
	private BuildingComplex building = null;

	/**
	 * Stockwerksbeschreibung, in welchen Stockwerken sollen die Objekte
	 * positioniert werden
	 */
	private List<FloorPosition> floorPositions = null;

	/**
	 * Index des Stockwerks innerhalb der Stockwerkshierarchie, falls nicht
	 * ueber die Position gearbeitet werden soll
	 */
	private int mFloorIndex = -1;

	/** Auf welchen Seiten des Stockwerks sollen die Objekte positioniert werden */
	private List<Side> sides = null;

	/** Aus welcher Kategorie stammt das zu positionierende Modell */
	private ModelCategory modelCategory = null;

	/** Soll das Objekt auf den Eckpunkten der Seite positioniert werden */
	private Boolean onCorners = null;

	/** Soll das Objekt auf den Kanten der Seite positioniert werden */
	private Boolean onEdges = null;

	/** Untere Grenze fuer das Verhaeltnis der Objekt- zur Quadhoehe */
	private Float lowerBorderObjectToQuadRatio = null;

	/** Obere Grenze fuer das Verhaeltnis der Objekt- zur Quadhoehe */
	private Float upperBorderObjectToQuadRatio = null;

	/**
	 * Mindestabstand des zu positionierenden Objekts zur vertikalen Kante des
	 * Positionierungsbereichs
	 */
	private Float lowerBorderObjectToEdgeRatio = null;

	/**
	 * Relative Distanz zwischen den zu positionierenden Objekten in Bezug auf
	 * die Quadbreite
	 */
	private Float distance = null;

	/**
	 * Vektor gibt an, welche Seite beim jeweiligen Objekt die Front-Achse
	 * beschreibt
	 */
	private MyVector3f mObjectFront = new MyVector3f(0.0f, 0.0f, 1.0f);

	/**
	 * Wert beschreibt die relative Position des Objekts in Bezug auf die
	 * Hoehenachse des Quads betrachtet von der unteren Quad-Kante, Beispiel:
	 * Wert 0.3 <=> Mittelpunkt des Objekts wird auf 0.3 * HoeheQuad gemessen
	 * von der unteren Kante aus positoniert
	 */
	private Float mRelativeFloorHeightPosition = null;

	/**
	 * Ausrichtung des zu positionierenden Objekts in Bezug auf die horizontale
	 * Achse des Zielquads
	 */
	private HorizontalAlignment mHorizontalAlign = null;

	/** Komponente, die positioniert werden soll */
	private AbstractComplex mComponent = null;

	// ------------------------------------------------------------------------------------------
	/** Standardkonstruktor */
	public PositionConfig() {
		floorPositions = new ArrayList<FloorPosition>();
		sides = new ArrayList<Side>();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the building
	 */
	public BuildingComplex getBuilding() {
		return building;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param building
	 *            the building to set
	 */
	public void setBuilding(BuildingComplex building) {
		this.building = building;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the floorPosition
	 */
	public List<FloorPosition> getFloorPositions() {
		return floorPositions;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param floorPosition
	 *            the floorPosition to set
	 */
	public void addFloorPosition(FloorPosition floorPosition) {
		if (!floorPositions.contains(floorPosition))
			floorPositions.add(floorPosition);
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the side
	 */
	public List<Side> getSides() {
		return sides;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param side
	 *            the side to set
	 */
	public void addSide(Side side) {
		if (!sides.contains(side))
			sides.add(side);
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the modelCategory
	 */
	public ModelCategory getModelCategory() {
		return modelCategory;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param modelCategory
	 *            the modelCategory to set
	 */
	public void setModelCategory(ModelCategory modelCategory) {
		this.modelCategory = modelCategory;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the onCorners
	 */
	public Boolean getOnCorners() {
		return onCorners;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param onCorners
	 *            the onCorners to set
	 */
	public void setOnCorners(Boolean onCorners) {
		this.onCorners = onCorners;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the onEdges
	 */
	public Boolean getOnEdges() {
		return onEdges;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param onEdges
	 *            the onEdges to set
	 */
	public void setOnEdges(Boolean onEdges) {
		this.onEdges = onEdges;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the lowerBorderObjectToQuadRatio
	 */
	public Float getLowerBorderObjectToQuadRatio() {
		return lowerBorderObjectToQuadRatio;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param lowerBorderObjectToQuadRatio
	 *            the lowerBorderObjectToQuadRatio to set
	 */
	public void setLowerBorderObjectToQuadRatio(
			Float lowerBorderObjectToQuadRatio) {
		this.lowerBorderObjectToQuadRatio = lowerBorderObjectToQuadRatio;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the upperBorderObjectToQuadRatio
	 */
	public Float getUpperBorderObjectToQuadRatio() {
		return upperBorderObjectToQuadRatio;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param upperBorderObjectToQuadRatio
	 *            the upperBorderObjectToQuadRatio to set
	 */
	public void setUpperBorderObjectToQuadRatio(
			Float upperBorderObjectToQuadRatio) {
		this.upperBorderObjectToQuadRatio = upperBorderObjectToQuadRatio;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the distance
	 */
	public Float getDistance() {
		return distance;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param distance
	 *            the distance to set
	 */
	public void setDistance(Float distance) {
		this.distance = distance;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mObjectFront
	 */
	public MyVector3f getObjectFront() {
		return mObjectFront;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mObjectFront
	 *            the mObjectFront to set
	 */
	public void setObjectFront(MyVector3f mObjectFront) {
		this.mObjectFront = mObjectFront;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mRelativeFloorHeightPosition
	 */
	public Float getRelativeFloorHeightPosition() {
		return mRelativeFloorHeightPosition;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mRelativeFloorHeightPosition
	 *            the mRelativeFloorHeightPosition to set
	 */
	public void setRelativeFloorHeightPosition(
			Float mRelativeFloorHeightPosition) {
		this.mRelativeFloorHeightPosition = mRelativeFloorHeightPosition;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mAlign
	 */
	public HorizontalAlignment getHorizontalAlign() {
		return mHorizontalAlign;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mAlign
	 *            the mAlign to set
	 */
	public void setVertAlign(HorizontalAlignment mAlign) {
		this.mHorizontalAlign = mAlign;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the lowerBorderObjectToEdgeRatio
	 */
	public Float getLowerBorderObjectToEdgeRatio() {
		return lowerBorderObjectToEdgeRatio;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param lowerBorderObjectToEdgeRatio
	 *            the lowerBorderObjectToEdgeRatio to set
	 */
	public void setLowerBorderObjectToEdgeRatio(
			Float lowerBorderObjectToEdgeRatio) {
		this.lowerBorderObjectToEdgeRatio = lowerBorderObjectToEdgeRatio;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mFloorIndex
	 */
	public int getFloorIndex() {
		return mFloorIndex;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mFloorIndex
	 *            the mFloorIndex to set
	 */
	public void setFloorIndex(int mFloorIndex) {
		this.mFloorIndex = mFloorIndex;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mComponent
	 */
	public AbstractComplex getComponent() {
		return mComponent;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mComponent
	 *            the mComponent to set
	 */
	public void setComponent(AbstractComplex mComponent) {
		this.mComponent = mComponent;
	}

	// ------------------------------------------------------------------------------------------

}
