package semantic.building.modeler.prototype.building;

import java.util.Map;

import semantic.building.modeler.configurationservice.model.enums.FloorPosition;

/**
 * Instanzen dieser Klasse enthalten alle Parameter, die Ausdehnungen des
 * Gebaeudes und der Stockwerke enthalten
 * 
 * @author Patrick Gunia
 * 
 */

public class BuildingDimensions {

	/** Gebaeudelaenge */
	private Float mLength = null;

	/** Gebaeudebreite */
	private Float mWidth = null;

	/** Gebaeudehoehe */
	private Float mHeight = null;

	/** Hoehen der einzelnen Stockwerke sortiert nach ihrer Position */
	private Map<FloorPosition, Float> mFloorHeights = null;

	/** Anzahl der Stockwerke */
	private Integer mNumberOfFloors = null;

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mLength
	 * @param mWidth
	 * @param mHeight
	 * @param floorHeights
	 * @param mNumberOfFloors
	 */
	public BuildingDimensions(Float mLength, Float mWidth, Float mHeight,
			Map<FloorPosition, Float> floorHeights, Integer mNumberOfFloors) {
		super();
		this.mLength = mLength;
		this.mWidth = mWidth;
		this.mHeight = mHeight;
		this.mFloorHeights = floorHeights;
		this.mNumberOfFloors = mNumberOfFloors;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mLength
	 */
	public Float getLength() {
		return mLength;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mWidth
	 */
	public Float getWidth() {
		return mWidth;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mHeight
	 */
	public Float getHeight() {
		return mHeight;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the floorHeights
	 */
	public Map<FloorPosition, Float> getFloorHeights() {
		return mFloorHeights;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mNumberOfFloors
	 */
	public Integer getNumberOfFloors() {
		return mNumberOfFloors;
	}
	// ------------------------------------------------------------------------------------------

}
