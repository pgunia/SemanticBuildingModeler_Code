package semantic.building.modeler.configurationservice.model.component;

import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.helper.XMLParsingHelper;

/**
 * Instanzen dieser Klasse enthalten saemtliche Parameter, die zur Konstruktion
 * von Fensterbaenken erforderlich sind
 * 
 * @author Patrick Gunia
 * 
 */

public class WindowLedgeComponentConfiguration extends Abstract3DModelComponent {

	/** Verhaeltnis von Fensterbank- zu Zielquadhoehe */
	private transient Float mLedgeToTargetQuadRatio = null;

	// ------------------------------------------------------------------------------------------
	@Override
	public void construct(Element configRoot) {

		// lade das Fensterbankprofil
		loadComponentSource(configRoot);
		mLedgeToTargetQuadRatio = XMLParsingHelper.getInstance().getFloat(
				configRoot, "ledgeToTargetQuadRatio", getNamespace());
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return this.mProcessingMetadata.getNamespaceByPrefix("co");
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "WindowLedgeComponent";
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mLedgeToTargetQuadRatio
	 */
	public Float getLedgeToTargetQuadRatio() {
		return mLedgeToTargetQuadRatio;
	}

	// ------------------------------------------------------------------------------------------

}
