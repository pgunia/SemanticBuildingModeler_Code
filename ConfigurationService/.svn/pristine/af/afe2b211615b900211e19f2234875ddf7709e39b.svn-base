package semantic.building.modeler.configurationservice.model;

import org.apache.log4j.Logger;

import semantic.building.modeler.configurationservice.model.enums.ReuseFloorEnum;

/**
 * Konfigurationsobjekt enthaelt saemtliche erforderlichen Daten fuer die
 * Grundrisserzeugung mittels der unterschiedlichen Verfahren
 * 
 * @author Patrick Gunia
 * 
 */

public class FootprintDescriptor {

	/** Logger */
	protected static Logger logger = Logger
			.getLogger(FootprintDescriptor.class);

	/** Instanz einer der verschiedenen Footprint-Konfigurationsklassen */
	private transient AbstractConfigurationObject mConf = null;

	/** Gebaeudebreite */
	private transient Float mBuildingWidth = null;

	/** Gebaeudehoehe */
	private transient Float mBuildingLength = null;

	/** Wiederverwendung vorheriger Grundrisse */
	private transient ReuseFloorEnum mReuseFloor = null;

	// -------------------------------------------------------------------------------------

	/**
	 * @param mConf
	 *            Footprint-Konfigurationsobjekt
	 * @param mBuildingWith
	 *            Gebaeudebreite
	 * @param mBuildingLength
	 *            Gebaeudelaenge
	 * @param reuse
	 *            Welches Stockwerk soll wiederverwendet werden
	 */
	public FootprintDescriptor(final AbstractConfigurationObject mConf,
			final Float mBuildingWith, final Float mBuildingLength,
			final ReuseFloorEnum reuse) {
		super();
		this.mConf = mConf;
		this.mBuildingWidth = mBuildingWith;
		this.mBuildingLength = mBuildingLength;
		this.mReuseFloor = reuse;
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Leerer Default-Konstruktor
	 */
	public FootprintDescriptor() {

	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mConf
	 */
	public AbstractConfigurationObject getConf() {
		return mConf;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mBuildingWith
	 */
	public Float getBuildingWidth() {
		return mBuildingWidth;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mBuildingLength
	 */
	public Float getBuildingLength() {
		return mBuildingLength;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return
	 * @see semantic.building.modeler.configurationservice.model.AbstractConfigurationObject#getType()
	 */
	public String getType() {
		// wenn eine Konfiguration gesetzt ist, gebe deren Typ zurueck
		if (mConf != null) {
			return mConf.getType();
		}
		// sonst muss ein Reuse-Value gesetzt sein
		else if (mConf == null && mReuseFloor != null) {
			return "ReuseFootprint";
		}
		logger.error("Ungueltiger Footprint-Descriptor! Typ kann nicht ermittelt werden.");
		return null;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mReuseFloor
	 */
	public ReuseFloorEnum getReuseFloor() {
		return mReuseFloor;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @param mConf
	 *            the mConf to set
	 */
	public void setConf(final AbstractConfigurationObject mConf) {
		this.mConf = mConf;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @param mBuildingWith
	 *            the mBuildingWith to set
	 */
	public void setBuildingWidth(final Float mBuildingWith) {
		this.mBuildingWidth = mBuildingWith;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @param mBuildingLength
	 *            the mBuildingLength to set
	 */
	public void setBuildingLength(final Float mBuildingLength) {
		this.mBuildingLength = mBuildingLength;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @param mReuseFloor
	 *            the mReuseFloor to set
	 */
	public void setReuseFloor(final ReuseFloorEnum mReuseFloor) {
		this.mReuseFloor = mReuseFloor;
	}

	// -------------------------------------------------------------------------------------

}
