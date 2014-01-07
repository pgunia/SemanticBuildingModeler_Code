package semantic.building.modeler.configurationservice.model.component;

import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.helper.XMLParsingHelper;
import semantic.building.modeler.configurationservice.model.enums.FloorPosition;

/**
 * Klasse dient der Konfigurationen von Saeulen, die an Gebaeuden angebracht
 * werden
 * 
 * @author Patrick Gunia
 * 
 */

public class PillarComponentConfiguration extends Abstract3DModelComponent {

	/**
	 * Minimale Distanz zwischen zwei Saeulen als Anteil an der Gesamtlaenge der
	 * Kante, an der die Saeulen positioniert werden
	 */
	private transient Float mPillarDistanceRatio = null;

	/** In welchem Stockwerk sollen die Saeulen positioniert werden */
	private transient FloorPosition mFloorPosition = null;

	/** Bei rechteckigen Grundrissen: Anzahl an Saeulen auf der Laengsseite */
	private transient Integer mNumberOfPillarsLongside = null;

	/** Bei rechteckigen Grundrissen: Anzahl an Saeulen auf der Querseite */
	private transient Integer mNumberOfPillarsBroadside = null;

	/**
	 * Flag zeigt an, ob die Anzahl der zu positionierenden Saeulen absolut
	 * angegeben ist
	 */
	private transient boolean mIsAbsolutePillarCount = false;

	@Override
	public void construct(Element configRoot) {

		assert configRoot.getName().equals("pillar") : "FEHLER: Ungueltiges Konfigurationsobjekt! Tagname: "
				+ configRoot.getName();

		// verarbeite die Komponentenkonfiguration
		loadComponentSource(configRoot);

		// entweder der Saeulenabstand wird angegeben als relativer Absatnd in
		// Bezug auf die Kantenlaenge
		mPillarDistanceRatio = XMLParsingHelper.getInstance().getFloat(
				configRoot, "pillarDistanceRatio", getNamespace());

		// oder in Form von Absolutwerten bei rechteckigen Grundrissen
		if (mPillarDistanceRatio == null) {
			mNumberOfPillarsLongside = XMLParsingHelper.getInstance()
					.getInteger(configRoot, "numberOfPillarsLongside",
							getNamespace());
			mNumberOfPillarsBroadside = XMLParsingHelper.getInstance()
					.getInteger(configRoot, "numberOfPillarsBroadside",
							getNamespace());
			mIsAbsolutePillarCount = true;
		}

		mFloorPosition = FloorPosition.valueOf(XMLParsingHelper.getInstance()
				.getString(configRoot, "pillarFloorPosition", getNamespace()));
	}

	// -------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return this.mProcessingMetadata.getNamespaceByPrefix("co");
	}

	// -------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "PillarComponent";
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mPillarDistanceRatio
	 */
	public Float getPillarDistanceRatio() {
		return mPillarDistanceRatio;
	}

	// -------------------------------------------------------------------------------------
	/**
	 * @return the mFloorPosition
	 */
	public FloorPosition getFloorPosition() {
		return mFloorPosition;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mNumberOfPillarsLongside
	 */
	public Integer getNumberOfPillarsLongside() {
		return mNumberOfPillarsLongside;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mNumberOfPillarsBroadside
	 */
	public Integer getNumberOfPillarsBroadside() {
		return mNumberOfPillarsBroadside;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mIsAbsolutePillarCount
	 */
	public boolean isAbsolutePillarCount() {
		return mIsAbsolutePillarCount;
	}

	// -------------------------------------------------------------------------------------

}
