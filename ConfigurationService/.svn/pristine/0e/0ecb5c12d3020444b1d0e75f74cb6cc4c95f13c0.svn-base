package semantic.building.modeler.configurationservice.model.component;

import java.io.File;

import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.configurationservice.model.AbstractConfigurationObject;
import semantic.building.modeler.configurationservice.model.enums.ModelCategory;

/**
 * 
 * Klasse beschreibt Elemente aus Komponentenkonfigurationen, die die Quelle
 * einer 3D-Komponente definieren. Diese kann entweder durch eine ModelCategory
 * definiert sein oder direkt eine Quelldatei laden und bereitstellen.
 * 
 * @author Patrick Gunia
 * 
 */

public class ComponentModelSource extends AbstractConfigurationObject {

	/** Komponente wird aus einer Datei geladen */
	private transient File mComponentSource = null;

	/**
	 * Komponente wird aus einem Pool von Komponenten geladen, der durch eine
	 * Kategorienbezeichnung spezifiziert wird
	 */
	private transient ModelCategory mModelCategory = null;

	/** Stammt das Model aus einer Datei oder aus einer Kategorie */
	private transient Boolean mIsCategory = null;

	// -------------------------------------------------------------------------------------

	@Override
	public void construct(final Element configRoot) {

		assert configRoot.getName().equals("componentModelSource") : "FEHLER: Ungueltiges Konfigurationsobjekt! Tagname: "
				+ configRoot.getName();

		// entweder es wurde eine Modellkategorie oder die URI eines Models
		// angegeben
		Element modelCategory = configRoot.getChild("modelCategory",
				getNamespace());
		if (modelCategory != null) {
			mModelCategory = ModelCategory.valueOf(modelCategory.getValue());
			mIsCategory = true;
		} else {
			mComponentSource = loadFile(configRoot);
			mIsCategory = false;
		}
	}

	// -------------------------------------------------------------------------------------

	@Override
	public Namespace getNamespace() {
		return this.mProcessingMetadata.getNamespaceByPrefix("co");
	}

	// -------------------------------------------------------------------------------------

	@Override
	public String getType() {
		return "ComponentModelSource";
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mComponentSource
	 */
	public File getComponentSource() {
		return mComponentSource;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mModelCategory
	 */
	public ModelCategory getModelCategory() {
		return mModelCategory;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @return the mIsCategory
	 */
	public Boolean isCategory() {
		return mIsCategory;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * @param mModelCategory
	 */
	public ComponentModelSource(ModelCategory mModelCategory) {
		super();
		this.mModelCategory = mModelCategory;
		this.mIsCategory = true;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * 
	 */
	public ComponentModelSource() {
		super();
	}

	// -------------------------------------------------------------------------------------

}
