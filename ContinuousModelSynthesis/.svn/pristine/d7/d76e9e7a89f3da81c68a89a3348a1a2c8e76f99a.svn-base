package semantic.building.modeler.modelsynthesis.model;

import java.util.List;

import semantic.building.modeler.configurationservice.model.enums.RuleType;

/**
 * 
 * @author Patrick Gunia Instanzen dieser Klasse realisieren eine zum
 *         eigentlichen Katalog inverse Struktur, bei der ueber die Regel selber
 *         auf die Komponenten zugegriffen werden kann. Diese Instanzen werden
 *         in einer regeltypbasierten Map-Struktur verwaltet, die es
 *         ermoeglicht, Regeln gezielt aufgrund ihres Typs auszuwaehlen.
 * 
 */

public class RuleApplication {

	/** Regel, die verwaltet wird */
	private transient ComponentState mRule = null;

	/** Liste aller Komponenten, in denen die Regel noch angewendet werden kann */
	private transient List<ModelSynthesisBaseGeometry> mComponents = null;

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mRule
	 * @param mApplicationCount
	 */
	public RuleApplication(ComponentState mRule,
			List<ModelSynthesisBaseGeometry> components) {
		super();
		this.mComponents = components;
		this.mRule = mRule;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * @return
	 * @see semantic.building.modeler.modelsynthesis.model.ComponentState#getID()
	 */
	public Integer getRuleID() {
		return mRule.getID();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return
	 * @see semantic.building.modeler.modelsynthesis.model.ComponentState#getRuleType()
	 */
	public RuleType getRuleType() {
		return mRule.getRuleType();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Fuegt eine Komponente zur Liste der Komponenten hinzu, auf die diese
	 * Regel angewendet werden kann
	 * 
	 * @param comp
	 *            Komponente, auf die diese Regel angewendet werden kann
	 */
	public void addComponent(final ModelSynthesisBaseGeometry comp) {
		if (!mComponents.contains(comp))
			mComponents.add(comp);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode entfernt die Uebergabekomponente aus der Liste der Komponenten,
	 * auf die die Regel anwendbar ist
	 * 
	 * @param comp
	 *            Zu entfernende Komponente
	 */
	public void removeComponent(final ModelSynthesisBaseGeometry comp) {
		mComponents.remove(comp);
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mComponents
	 */
	public List<ModelSynthesisBaseGeometry> getComponents() {
		return mComponents;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mRule
	 */
	public ComponentState getRule() {
		return mRule;
	}

	// ------------------------------------------------------------------------------------------

}
