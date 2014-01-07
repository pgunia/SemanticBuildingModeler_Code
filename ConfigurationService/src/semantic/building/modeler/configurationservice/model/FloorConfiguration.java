package semantic.building.modeler.configurationservice.model;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.helper.XMLParsingHelper;
import semantic.building.modeler.configurationservice.model.enums.FloorPosition;
import semantic.building.modeler.configurationservice.model.enums.ReuseFloorEnum;

/**
 * Konfigurationsklasse fuer ein einzelnes Stockwerk
 * 
 * @author Patrick Gunia
 * 
 */

public class FloorConfiguration extends AbstractConfigurationObject {

	/** Position des Stockwerks innerhalb des Gebaeudes */
	private transient FloorPosition mPosition = null;

	/** Range innerhalb derer die Stockwerkshoehe bestimmt wird */
	private transient RangeConfigurationObject mHeight = null;

	/**
	 * Konfigurationsobjekt, das das jeweilige Verfahren fuer die Erstellung
	 * eines Grundrisses steuert
	 */
	private transient AbstractConfigurationObject mFootprint = null;

	/**
	 * Falls fuer die Berechnung eine vorherige Stockwerksdefinition verwendet
	 * werden soll, gibt der Wert der Enum an, welches Stockwerk dafuer
	 * verwendet werden soll
	 */
	private transient ReuseFloorEnum mReuseFloorSource = null;

	// -------------------------------------------------------------------------------------

	@Override
	public void construct(final Element configRoot) {
		assert configRoot.getName().equals("floor") : "FEHLER: Ungueltiges Konfigurationsobjekt! Tagname: "
				+ configRoot.getName();
		mPosition = FloorPosition.valueOf(XMLParsingHelper.getInstance()
				.getString(configRoot, "floorposition", getNamespace()));

		// Stockwerke koennen vorherige Definitionen wiederverwenden
		// wenn dies der Fall ist, werden keine weiteren Parameter ausgelesen
		String reuseSrc = XMLParsingHelper.getInstance().getString(configRoot,
				"reuseFloorDefinition",
				mProcessingMetadata.getNamespaceByPrefix("fl"));
		if (reuseSrc != null) {
			mReuseFloorSource = ReuseFloorEnum.valueOf(reuseSrc);
			mHeight = new RangeConfigurationObject(configRoot.getChild(
					"floorHeight", getNamespace()));
		}
		// ansonsten wird die gesamte Konfiguration geladen
		else {
			mHeight = new RangeConfigurationObject(configRoot.getChild(
					"floorHeight", getNamespace()));

			// greife auf das src-Attribut des Footprints zu, um zu entscheiden,
			// um was fuer eine Art von Grundrissbeschreibung es sich handelt
			Namespace footprintNamespace = mProcessingMetadata
					.getNamespaceByPrefix("fp");
			Element footprint = configRoot.getChild("footprint",
					mProcessingMetadata.getNamespaceByPrefix("fp"));
			Attribute footprintSrc = footprint.getAttribute("src");
			String footprintSrcStr = footprintSrc.getValue();

			// wird eine externe Ressource eingebunden => wenn ja, mache deren
			// Wurzelelement zum aktuellen Wurzelelement
			Element externalRoot = getExternalRootElement(footprint, "fp");
			if (externalRoot != null) {
				footprint = externalRoot;
				footprintSrc = footprint.getAttribute("src");
				footprintSrcStr = footprintSrc.getValue();
			}

			LOGGER.debug("Footprint-Source: " + footprintSrcStr);

			if (footprintSrcStr.equals("objectplacement")) {
				mFootprint = new ObjectPlacementFootprintConfiguration();
				mFootprint.construct(footprint.getChild("objectplacement",
						this.mProcessingMetadata.getNamespaceByPrefix("op")));
			} else if (footprintSrcStr.equals("class")) {
				mFootprint = new ClassBasedFootprintConfiguration();
				mFootprint.construct(footprint.getChild("classBased",
						this.mProcessingMetadata.getNamespaceByPrefix("cb")));
			} else if (footprintSrcStr.equals("exampleBased")) {
				mFootprint = new ExampleBasedFootprintConfiguration();
				mFootprint.construct(footprint.getChild("exampleBased",
						this.mProcessingMetadata.getNamespaceByPrefix("eb")));
			} else if (footprintSrcStr.equals("polygonFootprint")) {
				mFootprint = new PolygonFootprintConfiguration();
				mFootprint.construct(footprint.getChild("polygonBased",
						this.mProcessingMetadata.getNamespaceByPrefix("pb")));
			} else {
				LOGGER.error("Invalid Footprint-Source: " + footprintSrc);
			}
		}

	}

	// -------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return mProcessingMetadata.getNamespaceByPrefix("fl");
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mPosition
	 */
	public FloorPosition getPosition() {
		return mPosition;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mHeight
	 */
	public RangeConfigurationObject getHeight() {
		return mHeight;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mFootprint
	 */
	public AbstractConfigurationObject getFootprint() {
		return mFootprint;
	}

	// -------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "Floor";
	}

	// -------------------------------------------------------------------------------------
	/**
	 * @return the mReuseFloorSource
	 */
	public ReuseFloorEnum getReuseFloorSource() {
		return mReuseFloorSource;
	}
	// -------------------------------------------------------------------------------------

}
