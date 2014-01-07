package semantic.building.modeler.modelsynthesis.algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import semantic.building.modeler.configurationservice.model.ExampleBasedFootprintConfiguration;
import semantic.building.modeler.configurationservice.model.enums.RuleType;
import semantic.building.modeler.modelsynthesis.model.ComponentState;
import semantic.building.modeler.modelsynthesis.model.Face;
import semantic.building.modeler.modelsynthesis.model.FacePosition;
import semantic.building.modeler.modelsynthesis.model.ModelSynthesisBaseGeometry;
import semantic.building.modeler.modelsynthesis.model.RayWrapper;
import semantic.building.modeler.modelsynthesis.model.RuleApplication;
import semantic.building.modeler.modelsynthesis.model.State;
import semantic.building.modeler.modelsynthesis.model.VertexWrapper;

/**
 * Klasse fuehrt die eigentlichen Berechnungen durch, die fuer die Erzeugung der
 * neuen Strukturen basierend auf dem Synthesis-Algorithmus erforderlich sind.
 * 
 * @author Patrick Gunia
 * 
 */
public class SynthesisProcessing {

	/** Logging-Instanz */
	protected final static Logger LOGGER = Logger
			.getLogger(SynthesisProcessing.class);

	/**
	 * Map-Struktur speichert fuer saemtliche Eingabekomponenten alle moeglichen
	 * Status, die diesen zugewiesen werden koennen. Diese Liste wird waehrend
	 * der Bearbeitung fortlaufend modifiziert, da Zuweisungen direkte
	 * Auswirkungen auf die Stati benachbarter Komponenten besitzen.
	 */
	private transient Map<ModelSynthesisBaseGeometry, List<ComponentState>> mCatalog = null;

	/**
	 * Regeltyp-basierter Katalog Fuer jede Art von Regel existiert eine Liste
	 * mit RuleApplication-Instanzen, die solche Komponenten umsetzen
	 */
	private transient Map<RuleType, List<RuleApplication>> mInverseCatalog = null;

	/**
	 * Mapstruktur. die fuer jede Regel die globale Anzahl ihrer Anwendungen
	 * speichert.
	 */
	private transient Map<Integer, Integer> mRuleApplicationCount = new TreeMap<Integer, Integer>();

	/**
	 * Map enthaelt fuer jedes vorkommend Face saemtliche zu diesem adjazenten
	 * Komponenten
	 */
	private transient Map<Face, Set<ModelSynthesisBaseGeometry>> mFaceConnectivities = null;

	/** Liste mit saemtlichen direkt oder indirekt beruehrten Komponenten */
	private transient List<ModelSynthesisBaseGeometry> mTouchedComponents = null;

	/** Zufallsgenerator */
	private transient Random mRand = new Random();

	/** Soll versucht werden, moeglichst geschlossene Strukturen zu erzeugen? */
	private Boolean mUseConsistencyConstraint = null;

	/**
	 * Flag gibt an, ob bei der Algorithmenberechnung nur Ray-Komponenten
	 * verwendet werden sollen
	 */
	private Boolean mUseOnlyRayComponents = null;

	/**
	 * Flag gibt an, ob bei der Algorithmenberechnung nur Vertex-Komponenten
	 * verwendet werden sollen
	 */
	private Boolean mUseOnlyVertexComponents = null;

	/** Soll das Wachstum von einem Startpunkt aus gleichmaessig erfolgen? */
	private Boolean mUseRandomGrowth = null;

	/**
	 * Flag gibt an ob es sich um den ersten Durchlauf innerhalb der Berechnung
	 * handelt
	 */
	private boolean firstStep = true;

	/** Datenstruktur speichert die Anzahl der Anwendungen bestimmter Regeltypen */
	private transient Map<RuleType, Integer> mRuleSynthesisCounter = new HashMap<RuleType, Integer>();

	/** Konfigurationsdatei fuer aehnlichkeitsbasierte Grundrisserzeugung */
	private transient ExampleBasedFootprintConfiguration mSynthesisConfiguration = null;

	// ------------------------------------------------------------------------------------------
	/**
	 * Konstruktor mit Uebergabe der Grundkomponenten, Vertices und Rays
	 * 
	 * @param vertices
	 *            Eckpunkte des Rasters
	 * @param rays
	 *            Strahlensegmente innerhalb des Rasters
	 */
	public SynthesisProcessing(final List<VertexWrapper> vertices,
			final List<RayWrapper> rays,
			final Map<String, List<ComponentState>> rules,
			final ExampleBasedFootprintConfiguration conf) {

		List<ModelSynthesisBaseGeometry> components = new ArrayList<ModelSynthesisBaseGeometry>(
				vertices.size() + rays.size());
		components.addAll(vertices);
		components.addAll(rays);
		mSynthesisConfiguration = conf;

		mUseConsistencyConstraint = mSynthesisConfiguration
				.getUseConsistencyConstraint();
		mUseOnlyRayComponents = mSynthesisConfiguration
				.getUseOnlyRayComponents();
		mUseOnlyVertexComponents = mSynthesisConfiguration
				.getUseOnlyVertexComponents();
		mUseRandomGrowth = mSynthesisConfiguration.getUseRandomGrowth();

		// berechne den komponentenbasierten Regelkatalog
		computeCatalog(rules, components);

		// printCatalogState();

		// berechne den regeltypbasierten Regelkatalog
		computeInverseCatalog();

		// erzeuge eine Liste, die fuer jedes Face alle zu diesem Face
		// adjazenten Komponenten enthaelt
		computeFaceConnectivity(rays);

		// erzeuge einen Zaehlkatalog, der fuer jede Regel die Haeufigkeit ihrer
		// Anwendungen enthaelt
		computeRuleApplicationCount();

		assert mCatalog != null && mInverseCatalog != null
				&& mFaceConnectivities != null && mRuleApplicationCount != null : "FEHLER: Es wurden nicht alle benoetigten Datenstrukturen erezeugt!";

		mTouchedComponents = new ArrayList<ModelSynthesisBaseGeometry>();

		// printVertexStates(vertices);

		components.clear();
		components = null;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet fuer jedes vorkommende Face saemtliche zu diesem
	 * adjazenten Komponenten. Zu diesem Zweck werden saemtliche Strahlen
	 * durchlaufen und fuer jeden Strahl der Strahl selber und sein Start- und
	 * Endvertex zur Liste hinzugefuegt
	 * 
	 * @param rays
	 *            Liste mit allen Strahlen
	 */
	private void computeFaceConnectivity(final List<RayWrapper> rays) {

		mFaceConnectivities = new TreeMap<Face, Set<ModelSynthesisBaseGeometry>>(
				new ComponentComparator());

		Face curFace = null;
		RayWrapper curRay = null;
		Set<ModelSynthesisBaseGeometry> adjacentComponents = null;

		for (int i = 0; i < rays.size(); i++) {
			curRay = rays.get(i);

			// zunaechst oberes Face verarbeiten
			curFace = curRay.getFace(FacePosition.UPPER);
			if (curFace != null) {
				adjacentComponents = mFaceConnectivities.get(curFace);
				if (adjacentComponents == null)
					adjacentComponents = new HashSet<ModelSynthesisBaseGeometry>();

				// Strahl und Vertices adden => Set sorgt dafuer, dass keine
				// doppelten Komponenten geadded werden
				adjacentComponents.add(curRay);
				adjacentComponents.add(curRay.getStartVert());
				adjacentComponents.add(curRay.getEndVert());
				mFaceConnectivities.put(curFace, adjacentComponents);
			}

			// das gleiche fuer das untere Face durchfuehren
			curFace = curRay.getFace(FacePosition.LOWER);
			if (curFace != null) {
				adjacentComponents = mFaceConnectivities.get(curFace);
				if (adjacentComponents == null)
					adjacentComponents = new HashSet<ModelSynthesisBaseGeometry>();

				// Strahl und Vertices adden => Set sorgt dafuer, dass keine
				// doppelten Komponenten geadded werden
				adjacentComponents.add(curRay);
				adjacentComponents.add(curRay.getStartVert());
				adjacentComponents.add(curRay.getEndVert());
				mFaceConnectivities.put(curFace, adjacentComponents);
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode realisiert eine Step-by-Step-Umsetzung des Verfahrens, die fuer
	 * Debugging-Zwecke eingesetzt werden soll
	 * 
	 * @param nextIndex
	 *            Index des naechsten zu verwendenden Elements
	 */
	public void nextStep(int nextIndex) {

		LOGGER.info("Next Step: " + nextIndex);
		ModelSynthesisBaseGeometry curComp = null;
		ComponentState curState = null;
		Random rand = new Random();

		if (firstStep) {

			LOGGER.info("First Step!");

			// waehle zunaechst eine Regel, die eine Ecke erzeugt aus dem
			// inversen Katalog
			List<RuleApplication> possibleStartRules = mInverseCatalog
					.get(mSynthesisConfiguration.getFirstRuleType());

			LOGGER.info("#Potentieller Startregeln fuer Komponententyp: "
					+ mSynthesisConfiguration.getFirstRuleType() + ": "
					+ possibleStartRules.size());
			RuleApplication rule = null;

			// waehle jetzt die erste passende Regel (kann auch noch
			// zufallsbasiert erfolgen)
			if (mSynthesisConfiguration.getRandomComponentChoice()) {
				rule = possibleStartRules.get(rand.nextInt(possibleStartRules
						.size()));
				curComp = rule.getComponents().get(
						rand.nextInt(rule.getComponents().size()));
			} else {
				rule = possibleStartRules.get(0);
				curComp = rule.getComponents().get(5);
			}
			curState = rule.getRule();
			firstStep = false;
		} else {
			// waehle eine Komponente basierend auf dem Index
			curComp = chooseComponent(nextIndex);
			if (curComp == null) {
				// wenn keine Komponente gefunden werden konnte, terminiere das
				// Verfahren!
				return;
			}

			// waehle den zuzuweisenden Status
			curState = chooseState(curComp);

			// wenn kein Status gefunden werden konnte, versuche es mit einem
			// anderen Index
			if (curState == null) {
				nextIndex++;
				nextStep(nextIndex);
			}
		}

		// wenn kein State gefunden wurde, terminiere die Berechnung
		if (curState == null)
			return;

		// teste, ob der ausgewaehlte Status gueltig ist
		if (isStateAssignable(curComp, curState)) {

			// Status zuweisen
			LOGGER.info("Assigne State " + curState + " zu Komponente: "
					+ curComp.getID() + " mit Label: " + curComp.getLabel());

			assignStateToComponent(curComp, curState);

			// festgelegte Komponente wird aus dem Katalog entfernt
			LOGGER.info("Entferne Komponente " + curComp.getID()
					+ " aus dem Katalog!");

			// Komponente ist gesetzt => aus Liste entfernen
			removeAssignedState(curComp, curState);
		} else {

			LOGGER.info("State " + curState + " konnte Komponente "
					+ curComp.getID() + " nicht zugewiesen werden.");
			// Zuweisung nicht moeglich, somit auch zu keinem spaeteren
			// Zeitpunkt => wird aus der Liste moeglicher Assignments entfernt
			removeUnassignableState(curComp, curState);
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Wurde eine Regel erfolgreich auf eine Komponente angewendet, entfernt
	 * diese Methode Regel und Komponente aus allen Verwaltungslisten
	 * 
	 * @param component
	 *            Komponente, auf die die Regel angewendet wurde
	 * @param state
	 *            Regel, die auf die Komponente angewendet wurde
	 */
	private void removeAssignedState(
			final ModelSynthesisBaseGeometry component,
			final ComponentState state) {

		// Komponente aus dem inversen Katalog loeschen
		removeComponentFromInverseCatalog(component);

		// Eintrag fuer die Komponente im Hauptkatalog entfernen
		mCatalog.remove(component);

		// aus der Liste der beruehrten Objekte entfernen
		mTouchedComponents.remove(component);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Wenn ein State fuer eine Komponente nicht zuweisbar ist, muss dieser
	 * ebenfalls aus den Verwaltungsstrukturen geloescht werden
	 * 
	 * @param component
	 *            Komponente, der der Status zugewiesen werden sollte
	 * @param state
	 *            Status, der zugewiesen werden sollte
	 */
	private void removeUnassignableState(
			final ModelSynthesisBaseGeometry component,
			final ComponentState state) {

		// entferne den State-Eintrag im Hauptkatalog
		removeStateFromComponent(component, state);

		// entferne den Eintrag der Komponente im inversen Katalog
		removeComponentFromRuleInInverseCatalog(component, state);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode entfernt den uebergebenen Status aus der Liste der uebergebenen
	 * Komponente im Hauptkatalog
	 * 
	 * @param component
	 *            Komponente, fuer die der Status entfernt wird
	 * @param state
	 *            Status, der entfernt wird
	 */
	private void removeStateFromComponent(
			final ModelSynthesisBaseGeometry component,
			final ComponentState state) {

		List<ComponentState> states = mCatalog.get(component);
		assert states != null : "FEHLER: Fuer Komponente " + component
				+ " existiert kein Eintrag im Hauptkatalog!";

		Iterator<ComponentState> stateIter = states.iterator();
		ComponentState curState = null;

		while (stateIter.hasNext()) {
			curState = stateIter.next();
			if (curState.getID() == state.getID()) {
				LOGGER.debug("Entferne State " + state.getID()
						+ " aus dem Katalog fuer Komponente "
						+ component.getID());
				stateIter.remove();
				break;
			}
		}

		// wenn fuer die Komponente keine weiteren Zuweisungen mehr existieren,
		// loesche den Eintrag im Katalog
		if (states.isEmpty())
			mCatalog.remove(component);

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode entfernt die uebergebene Komponente aus ALLEN Regeln im inversen
	 * Katalog
	 * 
	 * @param component
	 *            Komponente, die vollstaendig entfernt werden soll
	 */
	private void removeComponentFromInverseCatalog(
			final ModelSynthesisBaseGeometry component) {

		// hole alle Regeln, die der Komponente zugewiesen werden koennen ueber
		// den Hauptkatalog
		List<ComponentState> states = mCatalog.get(component);

		if (states != null) {
			ComponentState curState = null;

			for (int i = 0; i < states.size(); i++) {
				curState = states.get(i);
				removeComponentFromRuleInInverseCatalog(component, curState);
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode entfernt den uebergebenen Komponenteneintrag aus der Liste der
	 * RuleApplication-Instanz, die das uebergebene State-Objekt enthaelt
	 * 
	 * @param component
	 *            Komponente, die aus der Liste der Regel entfernt wird
	 * @param state
	 *            Regel
	 */
	private void removeComponentFromRuleInInverseCatalog(
			final ModelSynthesisBaseGeometry component,
			final ComponentState state) {

		// finde zunaechst die RuleApplication-Instanz, die der uebergebenen
		// Regel entspricht
		Iterator<RuleApplication> ruleIter = mInverseCatalog.get(
				state.getRuleType()).iterator();
		RuleApplication curRule = null;

		// durchlaufe alle Regeln
		while (ruleIter.hasNext()) {
			curRule = ruleIter.next();

			// wenn die richtige Regel gefunden wurde, loesche die Komponente
			// aus der Liste
			if (curRule.getRuleID() == state.getID()) {
				LOGGER.trace("Entferne Komponente: " + component.getID()
						+ " aus der Regelliste von Regel "
						+ curRule.getRuleID());
				curRule.getComponents().remove(component);

				// wenn die Liste leer ist, loesche die Regel direkt hinterher
				if (curRule.getComponents().isEmpty())
					ruleIter.remove();
				break;
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode entfernt die uebergebene Komponente aus saemtlichen Listen.
	 * 
	 * @param component
	 *            Komponente, die aus den Listen entfernt werden soll
	 */
	private void removeComponentFromLists(
			final ModelSynthesisBaseGeometry component) {
		LOGGER.trace("Komponente " + component.getID()
				+ " wird aus dem Katalog entfernt!");

		// entferne saemtliche Verweise auf die aktuelle Komponente aus dem
		// inversen Katalog
		removeComponentFromInverseCatalog(component);

		// entferne die Komponente aus dem Hauptkatalog und der Liste der
		// beruehrten Komponenten
		mCatalog.remove(component);
		mTouchedComponents.remove(component);

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode gibt den aktuellen Zustand des Katalogs aus
	 */
	private void printCatalogState() {
		Iterator<ModelSynthesisBaseGeometry> compIter = mCatalog.keySet()
				.iterator();
		ModelSynthesisBaseGeometry curComp = null;
		while (compIter.hasNext()) {
			curComp = compIter.next();
			LOGGER.info("Comp: " + curComp.getID() + " #Assignments: "
					+ mCatalog.get(curComp).size() + " Type: "
					+ curComp.getClass().getSimpleName());

			List<ComponentState> states = mCatalog.get(curComp);
			String possibleStates = "";
			for (int i = 0; i < states.size(); i++)
				possibleStates += " " + states.get(i).getID();
			LOGGER.info("......................." + possibleStates);
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet den initialen Katalog basierend auf den Regeln, die
	 * vorab berechnet wurden. Der Katalog enthaelt fuer jede Komponente
	 * saemtliche moeglichen Assignments, die basierend auf dem Eingabepolygon
	 * ermittelt wurden.
	 * 
	 * @param rules
	 *            Regeln fuer die initialen Komponenten
	 * @param components
	 *            Alle Komponenten des Eingaberasters
	 */
	private void computeCatalog(final Map<String, List<ComponentState>> rules,
			final List<ModelSynthesisBaseGeometry> components) {

		mCatalog = new TreeMap<ModelSynthesisBaseGeometry, List<ComponentState>>(
				new ComponentComparator());

		// durchlaufe alle Komponenten, fuege fuer jede Komponente saemtliche
		// gueltigen Assignments zur Map hinzu
		ModelSynthesisBaseGeometry curComp = null;
		List<ComponentState> curStates = null;
		for (int i = 0; i < components.size(); i++) {
			curComp = components.get(i);

			assert curComp.getLabel() != null : "FEHLER: Komponente "
					+ curComp.getID() + ", Typ: " + curComp.getClass()
					+ " besitzt kein Label!";
			// indiiziere die Regelmap mit dem Komponentenlabel
			curStates = rules.get(curComp.getLabel());
			if (curStates == null) {
				LOGGER.trace("Skipped Label: " + curComp.getLabel());
				continue;
			}

			// kopiere die States in eine neue Liste (Seiteneffekte beim
			// Entfernen ungueltiger Stati vermeiden)
			List<ComponentState> clonedList = new ArrayList<ComponentState>(
					curStates.size());
			clonedList.addAll(curStates);
			mCatalog.put(curComp, clonedList);
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet basierend auf dem vorab errechneten Hauptkatalog den
	 * inversen Katalog, der ueber die Art der Komponenten indiziert wird, die
	 * durch eine Regel erzeugt werden
	 */
	private void computeInverseCatalog() {

		assert mCatalog != null : "FEHLER: Es wurde noch kein Katalog berechnet!";

		// Buffer speichert fuer jede Regelinstanz alle Komponenten, die diese
		// anwenden koennen
		Map<ComponentState, List<ModelSynthesisBaseGeometry>> tempBuffer = new HashMap<ComponentState, List<ModelSynthesisBaseGeometry>>();
		Iterator<ModelSynthesisBaseGeometry> componentIter = mCatalog.keySet()
				.iterator();
		ModelSynthesisBaseGeometry curComp = null;
		List<ComponentState> curRules = null;
		ComponentState curRule = null;
		List<ModelSynthesisBaseGeometry> componentList = null;

		while (componentIter.hasNext()) {
			curComp = componentIter.next();
			curRules = mCatalog.get(curComp);

			// fuer jedes Regel- / Komponenten-Paar wird ein Eintrag im Buffer
			// angelegt
			for (int i = 0; i < curRules.size(); i++) {
				curRule = curRules.get(i);

				// wurde bereits ein Eintrag fuer die Regel angelegt?
				componentList = tempBuffer.get(curRule);
				if (componentList == null) {
					componentList = new ArrayList<ModelSynthesisBaseGeometry>();
					tempBuffer.put(curRule, componentList);
				}
				componentList.add(curComp);
			}
		}

		// verwende jetzt den temporaeren Buffer als Ausgangspunkt fuer die
		// Erzeugung des inversen Katalogs
		Iterator<ComponentState> ruleIter = tempBuffer.keySet().iterator();
		RuleApplication applicationInst = null;

		mInverseCatalog = new HashMap<RuleType, List<RuleApplication>>();
		List<RuleApplication> ruleApplicationList = null;
		while (ruleIter.hasNext()) {
			curRule = ruleIter.next();
			// erzeuge fuer jede Regel eine RuleApplication-Instanz
			applicationInst = new RuleApplication(curRule,
					tempBuffer.get(curRule));

			// wurde fuer den aktuellen Regeltyp bereits eine Liste im Katalog
			// erstellt
			ruleApplicationList = mInverseCatalog.get(applicationInst
					.getRuleType());
			if (ruleApplicationList == null) {
				ruleApplicationList = new ArrayList<RuleApplication>();
				mInverseCatalog.put(applicationInst.getRuleType(),
						ruleApplicationList);
			}
			ruleApplicationList.add(applicationInst);
		}

		// Testausgabe
		/*
		 * Iterator<RuleType> ruleTypeIter =
		 * mInverseCatalog.keySet().iterator(); RuleType curRuleType = null;
		 * while(ruleTypeIter.hasNext()) { curRuleType = ruleTypeIter.next();
		 * LOGGER.info(curRuleType + ": " +
		 * mInverseCatalog.get(curRuleType).size() + " Regeln"); }
		 */
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode implementiert den grundsaetzlichen Algorithmus.
	 */
	public void compute() {

		int maxNumberOfIterations = mCatalog.size() * 10;
		int count = 0;
		ModelSynthesisBaseGeometry curComp = null;
		ComponentState curState = null;

		// printFaceStates();
		LOGGER.info("#Elemente: " + mCatalog.size());

		while (!mCatalog.isEmpty() && count < maxNumberOfIterations) {

			count++;
			if ((count % 100) == 0)
				LOGGER.info("Iteration: " + count + " max. Iteration: "
						+ maxNumberOfIterations + " Katalog: "
						+ mCatalog.size());

			// Sonderverarbeitung fuer erstes positioniertes Element
			if (firstStep) {

				// waehle zunaechst eine Regel, die eine Ecke erzeugt aus dem
				// inversen Katalog
				List<RuleApplication> possibleStartRules = mInverseCatalog
						.get(mSynthesisConfiguration.getFirstRuleType());

				// waehle jetzt die erste passende Regel (kann auch noch
				// zufallsbasiert erfolgen)
				RuleApplication rule = possibleStartRules.get(0);

				// wenn zufallsbasiert gewaehlt werden soll, waehle zufaellig
				// aus der Menge
				if (mSynthesisConfiguration.getRandomComponentChoice())
					curComp = rule.getComponents().get(
							mRand.nextInt(rule.getComponents().size()));
				else
					curComp = rule.getComponents().get(20);

				curState = rule.getRule();
				firstStep = false;
			} else {
				curComp = chooseComponent(0);
				if (curComp == null)
					continue;

				curState = chooseState(curComp);
				if (curState == null)
					continue;
			}

			// Status zuweisen, falls es sich um eine gueltige Zuweisung handelt
			if (isStateAssignable(curComp, curState)) {
				assignStateToComponent(curComp, curState);
				removeAssignedState(curComp, curState);
			}

			// sonst entferne die Statuszuweisung => wenn diese zu diesem
			// Zeitpunkt nicht gueltig ist, dann auch zu keinem spaeteren (da
			// sich die bereits vorhandenen Assignments nicht mehr aendern)
			else {

				// Zuweisung nicht moeglich, somit auch zu keinem spaeteren
				// Zeitpunkt => wird aus der Liste moeglicher Assignments
				// entfernt
				removeUnassignableState(curComp, curState);
			}
		}
		LOGGER.info("Needed " + count + " Trys to compute new Model!");
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode ermittelt die naechste Komponente, fuer die eine Statuszuweisung
	 * vorgenommen werden soll und gibt diese zurueck.
	 * 
	 * @param nextIndex
	 *            Wird ein schrittweises Vorgehen eingesetzt, wird der
	 *            Uebergabeindex verwendet, um die Komponenten zu iterieren
	 * @return Komponente, der im darauffolgenden Schritt ein Status zugewiesen
	 *         wird
	 */
	private ModelSynthesisBaseGeometry chooseComponent(final int nextIndex) {

		// bei der Komponentenauswahl sollte fuer geschlossene Strukturen auf
		// die zufallsbasierte Auswahl verzichtet werden
		boolean useRand = mSynthesisConfiguration.getRandomComponentChoice();
		ModelSynthesisBaseGeometry curComp = null;

		// versuche zuerst, Komponenten aus der Liste der bereits angefassten
		// Komponenten zu waehlen
		if (mTouchedComponents.size() > 0) {
			ModelSynthesisBaseGeometry[] curTouchedComponents = (ModelSynthesisBaseGeometry[]) mTouchedComponents
					.toArray(new ModelSynthesisBaseGeometry[mTouchedComponents
							.size()]);

			// wenn eine Komponente in direkter Nachbarschaft gefunden wird,
			// verwende diese
			if (nextIndex < mTouchedComponents.size()) {
				curComp = curTouchedComponents[nextIndex];
				return curComp;
			}
		}

		// sonst waehle Komponenten aus dem Katalog aus
		// hole die Keys als Array => dadurch kann man ueber Indices auf sie
		// zugreifen
		final ModelSynthesisBaseGeometry[] curKeys = (ModelSynthesisBaseGeometry[]) mCatalog
				.keySet().toArray(
						new ModelSynthesisBaseGeometry[mCatalog.size()]);

		// wenn das Verfahren deterministisch arbeiten soll, nehme immer das
		// jeweils naechste Element
		if (!useRand) {
			if (nextIndex >= curKeys.length) {
				LOGGER.error("Es konnte kein Assignment gefunden werden!");
				return null;
			}
			curComp = curKeys[nextIndex];
		}
		// sonst waehle zufallsbasiert
		else {
			curComp = curKeys[mRand.nextInt(curKeys.length)];
		}

		// wenn nur ein bestimmter Komponententyp verwendet werden darf (Kante
		// oder Vertex), teste, ob die Auswahl mit dieser Konfiguration
		// kompatibel ist
		if (mUseOnlyRayComponents && !(curComp instanceof RayWrapper))
			return null;
		else if (mUseOnlyVertexComponents
				&& !(curComp instanceof VertexWrapper))
			return null;

		LOGGER.warn("Gewaehlte Komponente stammt nicht aus der Liste bereits angefasster Komponenten!");
		return curComp;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode dient der Auswahl eines Status, der der uebergebenen Komponente
	 * zugewiesen wird
	 * 
	 * @param component
	 * @return
	 */
	private ComponentState chooseState(
			final ModelSynthesisBaseGeometry component) {

		boolean useRand = mSynthesisConfiguration.getRandomStateChoice();
		List<ComponentState> curAssignments = mCatalog.get(component);
		if (curAssignments == null) {
			LOGGER.error("Keine Zuweisungen fuer Komponente "
					+ component.getID() + " Typ: "
					+ component.getClass().getSimpleName());
			// mTouchedComponents.remove(component);
			removeComponentFromLists(component);
			return null;
		}

		// soll versucht werden, geschlossene Strukturen zu entwickeln?
		if (mUseConsistencyConstraint) {
			if (component instanceof RayWrapper)
				return chooseConsistentStateForRay((RayWrapper) component,
						useRand, curAssignments);
			else
				return chooseConsistentStateForVertex(
						(VertexWrapper) component, useRand, curAssignments);
		} else
			return makeSimpleChoice(curAssignments, useRand);
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode versucht, das "Consistency Constraint" umzusetzen => es werden
	 * solche Statuszuweisungen bevorzugt, die benachbarten Faces den gleichen
	 * Status geben
	 * 
	 * @param ray
	 *            Komponente, fuer die eine Zuweisung gesucht wird
	 * @param useRand
	 *            Soll bei mehreren Moeglichkeiten zufaellig ausgewaehlt werden?
	 * @return Status, der der Uebergabekomponente zugewiesen werden soll
	 */
	private ComponentState chooseConsistentStateForRay(final RayWrapper ray,
			final boolean useRand,
			final List<ComponentState> avaiableAssignments) {

		// 1. Schritt: Wenn nur eine Zuweisung moeglich ist, gebe diese zurueck!
		if (avaiableAssignments.size() == 1)
			return avaiableAssignments.get(0);

		Set<Face> adjacentFaces = ray.getAdjacentFaces();

		// pruefe, ob bereits eines der adjazenten Faces gesetzt wurde
		Iterator<Face> faceIter = adjacentFaces.iterator();
		Face curFace = null;
		State state = null;
		while (faceIter.hasNext()) {
			curFace = faceIter.next();

			// wenn eines der beiden Faces bereits eine Zuweisung erhalten hat,
			// speichere diesen und breche ab
			if (curFace.getState() != State.UNDEFINED) {
				state = curFace.getState();
				break;
			}
		}

		// es ist noch keine Zuweisung vorgenommen worden => waehle je nach
		// Konfiguration zufaellig oder deterministisch
		if (state == null) {
			return makeSimpleChoice(avaiableAssignments, useRand);
		}
		// sonst versuche eine "passende" Zuweisung zu finden
		else {

			// hole die Position des Faces in Bezug auf den Uebergabestrahl
			FacePosition pos = ray.getFacePosition(curFace);
			FacePosition targetPos = null;
			if (pos == FacePosition.LOWER)
				targetPos = FacePosition.UPPER;
			else
				targetPos = FacePosition.LOWER;

			List<ComponentState> possibleAssignments = new ArrayList<ComponentState>(
					avaiableAssignments.size());

			// durchlaufe alle moeglichen Assignments und sammle alle moeglichen
			// Zuweisungen
			for (int i = 0; i < avaiableAssignments.size(); i++) {
				ComponentState curState = avaiableAssignments.get(i);

				// wenn die Statuszuweisung des jeweiligen Status mit dem
				// Ausgangsface uebereinstimmt, ist der State ein potentieller
				// Kandidat
				if (curState.getAssignmentByIndex(targetPos) == state)
					possibleAssignments.add(avaiableAssignments.get(i));
			}

			LOGGER.info("Fuer die Statuszuweisung wurden "
					+ possibleAssignments.size()
					+ " moegliche Zuweisungen gefunden!");

			// es gibt keinen konsitenten Status => waehle per Standardverfahren
			if (possibleAssignments.size() == 0)
				return makeSimpleChoice(avaiableAssignments, useRand);

			// nur eine Komponente enthalten => verwende diese
			else if (possibleAssignments.size() == 1)
				return possibleAssignments.get(0);

			// sonst waehle per Standardverfahren aus allen potentiellen
			// Kandidaten
			else
				return makeSimpleChoice(possibleAssignments, useRand);
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode versucht bei den Statezuweisungen so vorzugehen, dass moeglichst
	 * konsistente Verlaeufe entstehen. Dabei werden solche Zuweisungen
	 * verwendet, die moeglichst viele konsistente Uebergaenge erzielen
	 * 
	 * @param vertex
	 *            Vertex, fuer das eine moegliche Zuweisung gesucht wird
	 * @param useRand
	 *            Flag gibt an, ob innerhalb der vorhandenen Komponenten
	 *            zufaellig oder deterministisch ausgewaehlt werden soll
	 * @return State, der dem uebergebenen Vertex zugewiesen werden soll
	 */
	private ComponentState chooseConsistentStateForVertex(
			final VertexWrapper vertex, final boolean useRand,
			final List<ComponentState> avaiableAssignments) {

		// 1. Schritt: Wenn nur eine Zuweisung moeglich ist, gebe diese zurueck!
		if (avaiableAssignments.size() == 1)
			return avaiableAssignments.get(0);

		// durchlaufe nun alle moeglichen Assignments und berechne, welches die
		// Konsistenzforderung am besten erfuellt
		Map<Integer, List<ComponentState>> orderedAssignments = new TreeMap<Integer, List<ComponentState>>();
		ComponentState curAssignment = null;
		List<RayWrapper> adjacentRays = vertex.getRays();
		int votes;
		RayWrapper curRay = null;

		for (int i = 0; i < avaiableAssignments.size(); i++) {

			curAssignment = avaiableAssignments.get(i);
			votes = 0;

			// durchlaufe alle adjazenten Strahlen des Testvertex
			for (int j = 0; j < adjacentRays.size(); j++) {
				curRay = adjacentRays.get(j);
				int numberOfFaces = curRay.getAdjacentFaces().size();

				// wenn es keine zwei adjazenten Faces gibt, gehe zum naechsten
				// Strahl
				if (numberOfFaces < 2)
					continue;

				Face[] faces = curRay.getAdjacentFaces().toArray(
						new Face[numberOfFaces]);

				// wenn entweder beide Faces gesetzt oder beide ungesetzt sind,
				// soll dieser Strahl nicht zur Entscheidung beitragen
				if ((faces[0].getState() != State.UNDEFINED && faces[1]
						.getState() != State.UNDEFINED)
						|| (faces[0].getState() == State.UNDEFINED && faces[1]
								.getState() == State.UNDEFINED))
					continue;

				State faceState = faces[0].getState();

				// State des ersten Faces ist gesetzt
				// teste, ob die Statuszuweisung fuer das zweite Face zu einem
				// konsistenten Zustand fuehrt
				// wenn ja, vote fuer dieses Assignment
				if (faceState != State.UNDEFINED) {
					Face secondFace = faces[1];
					FacePosition curPos = vertex.getFacePosition(secondFace);
					if (curAssignment.getAssignmentByIndex(curPos) == faceState)
						votes++;
					continue;
				}

				// nun das gleiche fuer das andere Face durchfuehren
				faceState = faces[1].getState();
				if (faceState != State.UNDEFINED) {
					Face secondFace = faces[0];

					FacePosition curPos = vertex.getFacePosition(secondFace);
					if (curPos == null) {
						LOGGER.error("Vertex " + vertex
								+ " ist nicht adjazent zu Face " + secondFace);
						continue;
					}
					if (curAssignment.getAssignmentByIndex(curPos) == faceState)
						votes++;
					continue;
				}
			}

			// ///////////////////////////////////////////////////////////////////////////
			// VOTE-BOOSTER
			// bevorzuge solche Stati, die mehr Elemente auf INTERIOR setzen
			// multipliziere dafuer die Anzahl der Votes mit der Anzahl der
			// INTERIOR-Zuweisungen im Assignment

			int numberOfInteriorAssignments = curAssignment
					.getNumberOfInteriorStates();
			if (numberOfInteriorAssignments == 0)
				numberOfInteriorAssignments = 1;

			// HOEHERGEWICHTUNG VON INSIDE ERSTMAL RAUS
			votes *= numberOfInteriorAssignments;

			// aufgrund des Regeltyps boosten
			switch (curAssignment.getRuleType()) {
			case CORNER:
				votes *= mSynthesisConfiguration.getCornerVoteBoost();
				break;
			case REFLEX_CORNER:
				votes *= mSynthesisConfiguration.getReflexCornerVoteBoost();
				break;
			case INSIDE:
				votes *= mSynthesisConfiguration.getInsideVoteBoost();
				break;
			case OUTSIDE:
				votes *= mSynthesisConfiguration.getOutsideVoteBoost();
				break;
			case EDGE:
				votes *= mSynthesisConfiguration.getEdgeVoteBoost();
				break;
			default:
				votes *= 1;
				break;
			}

			LOGGER.info("#Votes: " + votes + " State: " + curAssignment);

			// teste, ob bereits ein State mit der erreichten Stimmanzahl
			// erreicht wurde
			List<ComponentState> states = orderedAssignments.get(votes);
			if (states == null)
				states = new ArrayList<ComponentState>();
			states.add(curAssignment);

			// alle Strahlen wurden durchlaufen, speichere die Votes und die
			// Statusliste
			orderedAssignments.put(votes, states);
		}

		// hole nach Abschluss der Berechnungen die Liste mit Zuweisungen, die
		// die meisten Stimmen erhalten haben
		// Iterator<Integer> voteIter = orderedAssignments.values()

		Integer[] voteKeys = orderedAssignments.keySet().toArray(
				new Integer[orderedAssignments.size()]);

		// TreeMap sortiert die Werte aufsteigend => hole das letzte Element aus
		// der Liste
		int maxVote = voteKeys[orderedAssignments.size() - 1];
		List<ComponentState> bestAssignments = orderedAssignments.get(maxVote);

		LOGGER.debug("Max Votes: " + maxVote + " #Elemente: "
				+ bestAssignments.size());

		// wenn es nur eine Zuweisung gibt, waehle diese
		if (bestAssignments.size() == 1)
			return bestAssignments.get(0);

		// sonst waehle zufaellig oder deterministisch unter den Listenelementen
		// aus
		else
			return makeSimpleChoice(bestAssignments, useRand);

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode waehlt je nach Einstellung entweder zufallsbasiert oder
	 * deterministisch aus der Uebergabeliste
	 * 
	 * @param avaiableAssignments
	 *            Liste mit saemtlichen moeglichen Zuweisungen fuer die
	 *            uebergebene Komponente
	 * @return State, der der Komponente zugewiesen wird
	 */
	private ComponentState makeSimpleChoice(
			final List<ComponentState> avaiableAssignments,
			final boolean useRand) {
		if (!useRand) {
			// immer das letzte Assignment holen
			return avaiableAssignments.get(avaiableAssignments.size() - 1);
		} else {
			// sonst zufallsbasiert auswaehlen
			LOGGER.trace("Using Rand for State-Choice, Number of Possibilities: "
					+ avaiableAssignments.size());
			return avaiableAssignments.get(mRand.nextInt(avaiableAssignments
					.size()));
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode testet, ob es moeglich ist, der uebergebenen Komponente den
	 * uebergebenen Status zuzuweisen, indem geprueft wird, ob die
	 * Statuszuweisungen mit den Statuszuweisungen der adjazenten Komponenten
	 * kompatibel sind.
	 * 
	 * @param curComp
	 *            Komponente, fuer die eine Zuweisung vorgenommen wird
	 * @param curState
	 *            Status, der zugewiesen wird
	 * @return True, falls die Zuweisung moeglich ist, false sonst
	 */
	private boolean isStateAssignable(final ModelSynthesisBaseGeometry curComp,
			final ComponentState curState) {

		// hole alle adjazenten Faces der aktuellen Komponente
		Set<Face> adjacentFaces = curComp.getAdjacentFaces();

		Iterator<Face> faceIter = adjacentFaces.iterator();
		Face curFace = null;
		FacePosition facePosition = null;
		State stateToAssign = null;

		while (faceIter.hasNext()) {
			curFace = faceIter.next();

			// ermittle den Index des Faces in Bezug auf die aktuelle Komponente
			facePosition = curComp.getFacePosition(curFace);
			if (facePosition == null) {
				LOGGER.error("Das verarbeitete Face ist nicht adjazent zur verarbeiteten Komponente.");
				return false;
			}
			assert facePosition != null : "FEHLER: Ungueltiges Face!";

			// und hole den Status, der diesem Face zugewiesen werden soll
			stateToAssign = curState.getAssignmentByIndex(facePosition);

			// LOGGER.info("Teste Zuweisung: " + stateToAssign + " zu Face " +
			// curFace.getID() + " auf Position " + facePosition);

			// sobald ein Face gefunden wurde, fuer das die Zuweisung
			// inkompatibel ist, breche ab
			if (!isStateAssignableToFace(curFace, stateToAssign)) {
				LOGGER.trace("Status " + stateToAssign + " konnte Face "
						+ curFace.getID() + " nicht zugewiesen werden!");
				return false;
			}
		}

		// die Zuweisung ist fuer alle adjazenten Faces gueltig
		return true;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode prueft, ob der uebergebene Status dem uebergebenen Face
	 * zugewiesen werden kann. Dafuer wird geprueft, ob die Zuweisung kompatibel
	 * zu den Zuweisungen der adjazenten Komponenten ist
	 * 
	 * @param face
	 *            Face, dem ein Status zugewiesen werden soll
	 * @param state
	 *            Status
	 * @return True, falls die Zuweisung moeglich ist, False sonst
	 */
	private boolean isStateAssignableToFace(final Face face, final State state) {

		assert face != null : "FEHLER: Uebergebenes Face ist null!";

		Set<ModelSynthesisBaseGeometry> adjacentComponents = mFaceConnectivities
				.get(face);
		assert adjacentComponents != null : "FEHLER: Fuer das Face "
				+ face.getID() + " existieren keine adjazenten Komponenten!";

		// sonst durchlaufe alle Komponenten und pruefe, ob die Zuweisung
		// funktioniert
		ModelSynthesisBaseGeometry curComp = null;
		Iterator<ModelSynthesisBaseGeometry> adjIter = adjacentComponents
				.iterator();

		// LOGGER.info("Teste adjazente Komnponenten fuer Face " +
		// face.getID());
		while (adjIter.hasNext()) {
			curComp = adjIter.next();

			// LOGGER.info("Komponente: " + curComp.getClass().getSimpleName() +
			// ": " + curComp.getID());

			// sobald eine inkompatible Komponente gefunden wird, breche ab
			if (!isStateAssignableToComponent(curComp, face, state)) {
				LOGGER.trace("Status " + state + " konnte Face " + face.getID()
						+ " fuer Komponente " + curComp.getID()
						+ " nicht zugewiesen werden!");
				return false;
			}
		}

		// alle Komponenten besitzen kompatible Zuweisungen
		return true;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode prueft, ob es in Bezug auf die Komponentenzuweisungen der
	 * uebergebenen Komponente moeglich ist, dem uebergebenen Face den
	 * uebergebenen Status zuzuweisen
	 * 
	 * @param comp
	 *            Komponente, fuer die die Zuweisung geprueft werden soll
	 * @param face
	 *            Face, das assigned werden soll
	 * @param state
	 *            Status, der zugewiesen werden soll
	 * @return True, falls die Zuweisung moeglich ist, False sonst
	 */
	private boolean isStateAssignableToComponent(
			final ModelSynthesisBaseGeometry comp, final Face face,
			final State state) {

		// hole die fuer die Komponente moeglichen Zuweisungen
		List<ComponentState> validStates = mCatalog.get(comp);

		// wenn sich die angefragte Komponente nicht mehr im Katalog befindet,
		// wurde sie in einem vorherigen Durchlauf bereits gesetzt
		// durch das Durchreichen muesste es sich somit um eine gueltige
		// Zuweisung handeln
		if (validStates == null) {
			LOGGER.trace("Komponente "
					+ comp.getID()
					+ " wurde aus dem Katalog entfernt. Zuweisung wird als gueltig aufgefasst!");
			return true;
		}

		// hole den Index des Faces in Bezug auf die Komponente
		FacePosition facePosition = comp.getFacePosition(face);
		if (facePosition == null) {
			LOGGER.error("Es konnte keine Position fuer das Face "
					+ face.getID() + " in Komponente " + comp.getID()
					+ " ermittelt werden!");
			return false;
		}

		assert facePosition != null : "FEHLER: Es konnte keine Position fuer das Face "
				+ face.getID()
				+ " in Komponente "
				+ comp.getID()
				+ " ermittelt werden!";
		LOGGER.trace("Teste Zuweisungen fuer Komponente: " + comp.getID());

		// durchlaufe alle moeglichen Zuweisungen und pruefe, ob eine gueltige
		// dabei ist
		ComponentState curState = null;
		for (int i = 0; i < validStates.size(); i++) {

			// sobald eine gueltige Zuweisung gefunden wurde, breche ab
			curState = validStates.get(i);

			LOGGER.trace("CurState: " + curState);

			if (curState.getAssignmentByIndex(facePosition) == state)
				return true;
		}
		LOGGER.trace("Fuer Komponente: " + comp.getID()
				+ " konnte keine kompatible Zuordnung gefunden werden!");
		return false;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode aktualisiert den Katalog fuer alle Komponenten, die adjazent zum
	 * jeweiligen Face sind
	 * 
	 * @param face
	 *            Face, dem ein Status zugewiesen wurde
	 */
	private void updateNeighbourStates(final Face face) {

		Set<ModelSynthesisBaseGeometry> adjacentComponents = mFaceConnectivities
				.get(face);
		assert adjacentComponents != null : "FEHLER: Fuer Face " + face.getID()
				+ " existieren keine adjazenten Komponenten!";

		// fuege saemtliche adjazenten Komponenten des aktuell verarbeiteten
		// Faces zur Liste hinzun
		Iterator<ModelSynthesisBaseGeometry> compIter = adjacentComponents
				.iterator();
		ModelSynthesisBaseGeometry curComp = null;

		// wenn ein gleichmaessiges Wachstum angestrebt wird, adde die
		// Komponenten zur Liste der angefassten Komponenten
		if (!mUseRandomGrowth) {
			while (compIter.hasNext()) {
				curComp = compIter.next();

				// Kanten werden zur Liste geadded, wenn sie keine Randkanten
				// sind
				// und nicht nur Vertex-Komponenten verwendet werden sollen
				if (curComp instanceof RayWrapper
						&& curComp.getAdjacentFaces().size() == 2) {
					if (!mUseOnlyVertexComponents
							&& !mTouchedComponents.contains(curComp))
						mTouchedComponents.add(curComp);
				}

				// analog fuer Vertexkomponenten
				if (curComp instanceof VertexWrapper
						&& curComp.getAdjacentFaces().size() == 4) {
					if (!mUseOnlyRayComponents
							&& !mTouchedComponents.contains(curComp))
						mTouchedComponents.add(curComp);
				}
			}
		}

		Iterator<ModelSynthesisBaseGeometry> adjacentCompIter = adjacentComponents
				.iterator();

		// durchlaufe alle adjazenten Komponenten und aktualisiere deren Status
		while (adjacentCompIter.hasNext()) {
			curComp = adjacentCompIter.next();
			LOGGER.trace("Aktualisiere Komponente: " + curComp.getID()
					+ " fuer Face: " + face.getID());
			updateNeighbourStatesForComponent(curComp, face);
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode aktualisiert den Katalog fuer die aktuelle Komponente und
	 * entfernt saemtliche Statuszuweisungen, die mit der vorherigen Zuweisung
	 * inkompatibel sind
	 * 
	 * @param component
	 *            Komponente, fuer die das Update durchgefuehrt wird
	 * @param face
	 *            Face, dem ein Status zugewiesen wurde
	 */
	private void updateNeighbourStatesForComponent(
			final ModelSynthesisBaseGeometry component, final Face face) {

		LOGGER.trace("Aktualisiere Neighbour-States fuer Komponente "
				+ component.getID());

		// teste zunaechst, ob saemtliche adjazenten Faces der Komponente
		// bereits gesetzt sind, in diesem Fall kann die Komponente aus allen
		// Katalogen entfernt werden
		Iterator<Face> adjacentFaceIter = component.getAdjacentFaces()
				.iterator();
		Face curFace = null;
		boolean incomplete = false;
		while (adjacentFaceIter.hasNext()) {
			curFace = adjacentFaceIter.next();
			if (curFace.getState() == State.UNDEFINED) {
				incomplete = true;
				break;
			}
		}

		// wenn kein Face gefunden wurde, dem noch kein Face zugewiesen wurde,
		// so ist die Komponente vollstaendig gesetzt und kann aus den Listen
		// entfernt werden
		if (!incomplete) {
			removeComponentFromLists(component);
			return;
		}

		// alle gueltigen Zustaende der Komponente holen
		List<ComponentState> validCompStates = mCatalog.get(component);

		// es kann vorkommen, dass eine Nachbarkomponente nicht mehr im Katalog
		// auftaucht, also alle moeglichen Assignments fuer diese bereits
		// entfernt wurden
		// dieser Fall ist korrekt
		if (validCompStates == null)
			return;

		LOGGER.trace("Fuer Komponenten " + component.getID() + " existieren "
				+ validCompStates.size() + " Zuweisungen.");
		FacePosition facePos = component.getFacePosition(face);

		// entferne alle Zuweisungen aus der Zuweisungsliste, die nicht mit der
		// Facefestlegung kompatibel sind
		Iterator<ComponentState> stateIter = validCompStates.iterator();
		ComponentState curState = null;
		while (stateIter.hasNext()) {
			curState = stateIter.next();

			// wenn das Statusassignment mit der Regel inkompatibel ist,
			// entferne die Regel aus dem Katalog
			if (curState.getAssignmentByIndex(facePos) != face.getState()) {
				LOGGER.trace("Entferne Zuweisung " + curState.getID()
						+ " fuer Komponente " + component.getID()
						+ " aus dem Katalog!");

				// Loesche den State im Katalog
				stateIter.remove();

				// entferne auch den Eintrag im inversen Katalog
				removeComponentFromRuleInInverseCatalog(component, curState);
			}
		}

		// wenn es keine gueltigen Zuweisungen mehr fuer den Strahl gibt,
		// entferne ihn aus dem Katalog und aus der Liste der beruehrten
		// Komponenten
		if (validCompStates.isEmpty()) {
			removeComponentFromLists(component);
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode fuehrt die Statuszuweisung durch
	 * 
	 * @param comp
	 *            Komponente, der ein Status zugewiesen werden soll
	 * @param state
	 *            Zuzuweisender Status
	 */
	private void assignStateToComponent(final ModelSynthesisBaseGeometry comp,
			final ComponentState state) {

		LOGGER.debug("Assigne State " + state.getID() + " zu Komponente "
				+ comp.getID());
		for (FacePosition curPos : FacePosition.values()) {

			State curState = state.getAssignmentByIndex(curPos);
			if (curState != null) {

				Face curFace = comp.getFace(curPos);
				if (curFace != null) {

					if (curFace.getState() != State.UNDEFINED) {
						assert curFace.getState() == curState : "FEHLER: Inkompatible Statuszuweisung! Position: "
								+ curPos
								+ ", Alter Status: "
								+ curFace.getState()
								+ ", Neuer Status: "
								+ curState;
					} else {
						LOGGER.debug("Setze Face " + curFace.getID()
								+ " auf State " + curState);
						curFace.setState(curState);
						updateNeighbourStates(curFace);
					}
				}
			}
		}

		// wenn es sich bei der verarbeiteten Komponente um ein Vertex handelt,
		// koennen saemtliche adjazenten Strahlen aus den Listen entfernt werden
		// durch die Statezuweisungen sind deren adjazente Faces gesetzt
		if (comp instanceof VertexWrapper) {
			List<RayWrapper> rays = ((VertexWrapper) comp).getRays();
			for (int i = 0; i < rays.size(); i++)
				removeComponentFromLists(rays.get(i));
		} else {

			// wenn es sich um einen Strahl handelt, kann fuer die beiden
			// adjazenten Vertices getestet werden
			// ob saemtliche Faces bereits festgelegt wurden. Wenn dies der Fall
			// ist, koennen die Vertices ebenfalls geloescht werden
			Set<ModelSynthesisBaseGeometry> adjacentComponents = comp
					.getAdjacentComponents();
			Iterator<ModelSynthesisBaseGeometry> compIter = adjacentComponents
					.iterator();
			ModelSynthesisBaseGeometry curComp = null;
			Face curFace = null;
			while (compIter.hasNext()) {
				boolean complete = true;
				curComp = compIter.next();
				Set<Face> adjacentFaces = curComp.getAdjacentFaces();
				Iterator<Face> faceIter = adjacentFaces.iterator();

				// durchlaufe alle Faces des aktuellen Vertex
				// wenn alle Faces gesetzt sind, kann das Vertex aus dem Katalog
				// entfernt werden
				while (faceIter.hasNext()) {
					curFace = faceIter.next();
					if (curFace.getState() == State.UNDEFINED) {
						complete = false;
						break;
					}
				}
				// wenn alle Faces gesetzt sind, kann die Komponente aus dem
				// Katalog entfernt werden
				if (complete) {
					LOGGER.debug("Entferne Komponente: "
							+ curComp
							+ " aus dem Katalog, da saemtliche adjazenten Faces gesetzt wurden!");
					removeComponentFromLists(curComp);
				}
			}
		}

		// verringere den Counter fuer die Anwendung der aktuell verarbeiteten
		// Regel => Verarbeitung ist beschraenkt auf VertexWrapper-Instanzen
		decrementApplicationCount(state);

		Integer numberOfApplications = mRuleSynthesisCounter.get(state
				.getRuleType());
		if (numberOfApplications == null)
			numberOfApplications = 1;
		else
			numberOfApplications++;
		mRuleSynthesisCounter.put(state.getRuleType(), numberOfApplications);

		// Zaehler erstmal rausnehmen
		// if(comp instanceof VertexWrapper)
		// decrementApplicationCount(state.getID());

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Methode berechnet den Rule-Application-Katalog, der fuer die Zaehlung der
	 * Regelanwendungen verwendet wird. Die Haeufigkeitszuweisung basiert dabei
	 * auf den Regeltypen.
	 */
	private void computeRuleApplicationCount() {

		// hard gecodete Haeufigkeiten der Regelanwendung
		Map<RuleType, Integer> ruleApplications = new HashMap<RuleType, Integer>();
		ruleApplications.put(RuleType.CORNER,
				mSynthesisConfiguration.getCornerCount());
		ruleApplications.put(RuleType.REFLEX_CORNER,
				mSynthesisConfiguration.getReflexCornerCount());
		ruleApplications.put(RuleType.EDGE,
				mSynthesisConfiguration.getEdgeCount());
		ruleApplications.put(RuleType.OUTSIDE,
				mSynthesisConfiguration.getOutsideCount());
		ruleApplications.put(RuleType.INSIDE,
				mSynthesisConfiguration.getInsideCount());

		assert mInverseCatalog != null : "FEHLER: Der inverse Katalog wurde noch nicht berechnet!";

		// verwende jetzt den inversen Katalog, um den Regelcounter zu berechnen
		List<RuleApplication> ruleApplicationList = null;
		Iterator<List<RuleApplication>> ruleListIter = mInverseCatalog.values()
				.iterator();

		RuleApplication curRule = null;
		// durchlaufe alle Listen und lege fuer jede Regel einen Wert in der Map
		// an
		while (ruleListIter.hasNext()) {
			ruleApplicationList = ruleListIter.next();
			for (int i = 0; i < ruleApplicationList.size(); i++) {
				curRule = ruleApplicationList.get(i);
				mRuleApplicationCount.put(curRule.getRuleID(),
						ruleApplications.get(curRule.getRuleType()));
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode dekerementiert den Anwendungscounter fuer die Regel mit der
	 * Uebergabe-ID. Sobald der Counter einer solchen Regel = 0 ist, wird sie
	 * vollstaendig aus dem Katalog entfernt und kann somit nich mehr angewendet
	 * werden
	 * 
	 * @param rule
	 *            Regel, fuer die der Counter verringert wird
	 */
	private void decrementApplicationCount(final ComponentState rule) {

		Integer count = mRuleApplicationCount.get(rule.getID());
		final Integer ruleID = rule.getID();
		assert count != null : "Fuer Regel " + rule.getID()
				+ " existiert kein Application-Counter!";
		assert count != 0 : "Regel wurde angewendet, obwohl ihr Counter bereits auf 0 stand!";

		// dekrementiere
		--count;

		if (count == 0) {

			LOGGER.info("Entferne Regel "
					+ ruleID
					+ " aus dem Katalog, da die maximale Anzahl an Anwendungen ueberschritten wurde.");

			// hole ueber den inversen Katalog eine Liste mit allen Komponenten,
			// auf die die Regel anwendbar ist
			List<RuleApplication> ruleApplications = mInverseCatalog.get(rule
					.getRuleType());

			assert ruleApplications != null : "FEHLER: Fuer den Regeltyp "
					+ rule.getRuleType()
					+ " existiert kein Eintrag im inversen Katalog!";

			// bestimme die RuleApplication-Instanz, die diese Regel enthaelt
			Iterator<RuleApplication> ruleIter = ruleApplications.iterator();
			RuleApplication curRule = null;
			while (ruleIter.hasNext()) {
				curRule = ruleIter.next();
				if (curRule.getRuleID() == rule.getID()) {

					ModelSynthesisBaseGeometry curComp = null;

					// durchlaufe alle Komponenten, auf die diese Regel
					// anwendbar war
					Iterator<ModelSynthesisBaseGeometry> compIter = curRule
							.getComponents().iterator();

					// durchlaufe alle Komponenten und entferne die Regel aus
					// dem Hauptkatalog
					while (compIter.hasNext()) {
						curComp = compIter.next();

						List<ComponentState> assignableStates = mCatalog
								.get(curComp);

						assert assignableStates != null : "FEHLER: Fuer Komponente "
								+ curComp
								+ " existieren keine moeglichen Zuweisungen mehr im Katalog!";
						Iterator<ComponentState> stateIter = assignableStates
								.iterator();

						ComponentState curState = null;

						// durchlaufe alle Regeln, die fuer die Komponente
						// anwendbar sind und loesche die Uebergaberegel
						while (stateIter.hasNext()) {
							curState = stateIter.next();
							if (curRule.getRuleID() == curState.getID()) {

								LOGGER.debug("Entferne Regel "
										+ curRule.getRuleID()
										+ " aus der Liste der moeglichen Regeln fuer Komponente "
										+ curComp.getID());
								// State aus der Liste loeschen
								stateIter.remove();
								break;
							}
						}

						// wenn nach dem Entfernen der Regel keine weiteren
						// Regeln mehr fuer die Komponente im Katalog vorhanden
						// sind
						// loesche die Komponente aus dem Katalog
						if (assignableStates.isEmpty()) {
							LOGGER.debug("Entferne Komponente "
									+ curComp.getID() + " aus dem Katalog.");
							mCatalog.remove(curComp);
						}

						// Komponente aus der Regelliste loeschen
						compIter.remove();
					}

					// RuleApplication-Instanz loeschen => die Regel kann nicht
					// mehr
					// amgewemdet werden
					LOGGER.info("Loesche Regel " + curRule.getRuleID()
							+ " aus dem inversen Katalog.");
					ruleIter.remove();

					// Schleife verlassen
					break;
				}
			}

		} else {
			mRuleApplicationCount.put(rule.getID(), count);
		}
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Comparator-Klasse fuer die Verwendung einer TreeMap fuer die Verwaltung
	 * des Katelogs. Alle Komponenten werden anhand ihrer Komponenten sortiert.
	 */
	private class ComponentComparator implements
			Comparator<ModelSynthesisBaseGeometry> {

		public int compare(ModelSynthesisBaseGeometry arg0,
				ModelSynthesisBaseGeometry arg1) {
			return arg0.getID().compareTo(arg1.getID());
		}
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @return the mRuleSynthesisCounter
	 */
	public Map<RuleType, Integer> getRuleSynthesisCounter() {
		return mRuleSynthesisCounter;
	}

	// ------------------------------------------------------------------------------------------

}
