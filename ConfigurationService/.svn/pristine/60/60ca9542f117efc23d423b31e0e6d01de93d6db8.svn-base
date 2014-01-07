package semantic.building.modeler.configurationservice.model;

import java.util.Random;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.Namespace;

/**
 * Einfache Datenstruktur zur Repraesentation von Ranges innerhalb einer
 * Konfiguration
 * 
 * @author Patrick Gunia
 * 
 */

public class RangeConfigurationObject {

	/** Logging-Instanz */
	protected final static Logger LOGGER = Logger
			.getLogger(RangeConfigurationObject.class);

	/** Untere Grenze innerhalb des Bereichs */
	private transient Float mLowerBorder = null;

	/** Obere Grenze innerhalb des Bereichs */
	private transient Float mUpperBorder = null;

	/**
	 * Zufallsgenerator fuer die zufallsbasierte Auswahl eines Wertes aus dem
	 * Range-Intervall
	 */
	private static Random mRand = new Random();

	/** Namespace-URI der Common-Types */
	private static Namespace mNamespace = XMLConfigurationMetadata
			.getInstance().getNamespaceByPrefix("ct");

	// ----------------------------------------------------------------------------------------

	/**
	 * Konstruktor mit Uebergabe beider Grenzen
	 * 
	 * @param lower
	 *            Untere Grenze
	 * @param upper
	 *            Obere Grenze
	 */
	public RangeConfigurationObject(final Float lower, final Float upper) {
		mLowerBorder = lower;
		mUpperBorder = upper;
		assert mUpperBorder >= mLowerBorder : "FEHLER: Ungueltiges Intervall, untere Grenze '"
				+ mLowerBorder + "' ist groesser als obere Grenze " + "'!";
	}

	// ----------------------------------------------------------------------------------------
	/**
	 * Konstruktor mit Uebergabe eines XML-Elements
	 * 
	 * @param rangeElement
	 *            Knoten in der XML-Konfigurationsdatei
	 */
	public RangeConfigurationObject(final Element rangeElement) {

		if (rangeElement == null) {
			LOGGER.warn("Das uebergebene Element existiert nicht!");
		} else {
			processChildren(rangeElement, mNamespace);
		}
	}

	// ----------------------------------------------------------------------------------------
	/**
	 * Konstruktor mit Uebergabe eines XML-Elements
	 * 
	 * @param rangeElement
	 *            Knoten in der XML-Konfigurationsdatei
	 * @param defaultValue
	 *            Standardwert, falls in der Konfiguration keine Werte vorkommen
	 */
	public RangeConfigurationObject(final Element rangeElement,
			RangeConfigurationObject defaultValue) {

		if (rangeElement == null) {
			LOGGER.warn("Das uebergebene Element existiert nicht!");
			mLowerBorder = defaultValue.getLowerBorder();
			mUpperBorder = defaultValue.getUpperBorder();
			defaultValue = null;
		} else {
			processChildren(rangeElement, mNamespace);
		}

	}

	// ----------------------------------------------------------------------------------------
	/**
	 * Konstruktor mit Uebergabe eines XML-Elements und eines Namespaces
	 * 
	 * @param rangeElement
	 *            Knoten in der XML-Konfigurationsdatei
	 * @param namespace
	 *            Namensraum, falls dieser vom Standardnamesraum des
	 *            Range-Elements abweicht
	 */
	public RangeConfigurationObject(final Element rangeElement,
			final Namespace namespace) {

		if (rangeElement == null) {
			LOGGER.warn("Das uebergebene Element existiert nicht!");
		} else {
			processChildren(rangeElement, namespace);
		}

	}

	// ----------------------------------------------------------------------------------------

	/**
	 * Konstruktor mit Uebergabe eines XML-Elements und eines Namespaces
	 * 
	 * @param rangeElement
	 *            Knoten in der XML-Konfigurationsdatei
	 * @param namespace
	 *            Namensraum, falls dieser vom Standardnamesraum des
	 *            Range-Elements abweicht
	 * @param defaultValue
	 *            Standardwerd, der verwendet wird, sofern kein
	 *            Konfigurationselement existiert
	 */
	public RangeConfigurationObject(final Element rangeElement,
			final Namespace namespace, RangeConfigurationObject defaultValue) {
		if (rangeElement == null) {
			LOGGER.warn("Das uebergebene Element existiert nicht!");
			mLowerBorder = defaultValue.getLowerBorder();
			mUpperBorder = defaultValue.getUpperBorder();
			defaultValue = null;
		} else {
			processChildren(rangeElement, namespace);
		}

	}

	// ----------------------------------------------------------------------------------------
	/**
	 * Methode liest die Grenrwerte aus dem uebergebenen Elemtn
	 * 
	 * @param rangeElement
	 *            Element, das die Grenzwerte enthaelt
	 * @param namespace
	 *            Namespace der Subelemente
	 */
	private void processChildren(final Element rangeElement,
			final Namespace namespace) {
		Element element = rangeElement.getChild("lowerBorder", namespace);
		assert element != null : "FEHLER: Es existiert kein Element fuer die untere Grenze!";
		mLowerBorder = Float.valueOf(element.getValue());

		element = rangeElement.getChild("upperBorder", namespace);
		if (element != null) {
			mUpperBorder = Float.valueOf(element.getValue());
		} else {
			// wenn keine Obergrenze gesetzt ist, verwende die Untergrenze fuer
			// beide Werte
			mUpperBorder = mLowerBorder;
		}

		if (mUpperBorder != null) {
			assert mUpperBorder >= mLowerBorder : "FEHLER: Ungueltiges Intervall, untere Grenze '"
					+ mLowerBorder + "' ist groesser als obere Grenze " + "'!";
		}
	}

	// ----------------------------------------------------------------------------------------

	/**
	 * Konstruktor mit Uebergabe nur der unteren Grenze
	 * 
	 * @param lower
	 *            Untere Grenze innerhalb des uebergebenen Range-Bereichs
	 */
	public RangeConfigurationObject(final Float lower) {
		mLowerBorder = lower;
	}

	// ----------------------------------------------------------------------------------------
	/**
	 * Untere Grenze
	 * 
	 * @return Untere Grenze innerhalb des Intervalls
	 */
	public Float getLowerBorder() {
		return mLowerBorder;
	}

	// ----------------------------------------------------------------------------------------
	/**
	 * Obere Grenze
	 * 
	 * @return Obere Grenze innerhalb des Intervalls
	 */
	public Float getUpperBorder() {
		return mUpperBorder;
	}

	// ----------------------------------------------------------------------------------------
	/**
	 * Methode liefert zufallsbasiert einen Wert aus dem repraesentierten
	 * Intervall
	 * 
	 * @return Wert innerhalb des Intervalls, falls eine obere und untere Grenze
	 *         angegeben sind, sonst wird die untere Grenze zurueckgegeben
	 */
	public Float getRandValueWithinRange() {

		if (mUpperBorder != null) {
			final Float difference = mUpperBorder - mLowerBorder;
			final Float rand = mRand.nextFloat();

			final Float value = difference * rand;
			return mLowerBorder + value;
		} else {
			return mLowerBorder;
		}
	}

	// ----------------------------------------------------------------------------------------
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Range: lower Border: " + mLowerBorder + " upper Border: "
				+ mUpperBorder;
	}
	// ----------------------------------------------------------------------------------------

}
