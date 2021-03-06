package semantic.building.modeler.prototype.building.component;

import java.util.HashMap;
import java.util.Map;

import processing.core.PApplet;
import semantic.building.modeler.configurationservice.model.AbstractConfigurationObject;
import semantic.building.modeler.configurationservice.model.component.RoundStairsComponentConfiguration;
import semantic.building.modeler.configurationservice.model.enums.Side;
import semantic.building.modeler.math.MyPolygon;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.prototype.graphics.complex.FreeComplex;

/**
 * Klasse implementiert Treppen, die ausgehend von einem Grundriss ein Gebaeude
 * vollstaendig umgeben (bsw. fuer griechische Tempel)
 * 
 * @author Patrick Gunia
 * 
 */

public class RoundStairs extends AbstractBuildingComponent {

	/** Grundriss der obersten Stufe */
	private MyPolygon mBaseFootprint = null;

	// ------------------------------------------------------------------------------------------
	/**
	 * Standardkonstuktor
	 * 
	 * @param applet
	 *            Instanz des Device-Kontexts, damit sich die Treppe selber
	 *            zeichnen kann
	 * @param baseFootprint
	 *            Grundriss der obersten Treppenstufe
	 */
	public RoundStairs(final PApplet applet, final MyPolygon baseFootprint,
			final AbstractConfigurationObject conf) {
		super(applet, conf);
		mBaseFootprint = baseFootprint;
		mConf = conf;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode erzeugt eine Treppenkomponente basierend auf der angegebenen
	 * Konfigurationsdatei. Dabei geht das System derart vor, dass der
	 * Ausgangs-Footprint zunaechst derart skaliert wird, dass seine Ausdehnung
	 * den Dimensionen auf der untersten Ebene entspricht. Fuer jede Ebene wird
	 * dann das Polygon skaliert und als Footprint fuer eine Cube-Instanz
	 * genutzt. Nach Erzeugung dieser Instanz werden die Vertices und Faces
	 * extrahiert und zur Komponente hinzugefuegt (nachdem die Indices auf die
	 * Vewrtices angepasst wurden.
	 */
	@Override
	public void createComponent() {

		assert mConf != null : "FEHLER: Es wurde keine Konfigurationsdatei geladen!";

		RoundStairsComponentConfiguration conf = (RoundStairsComponentConfiguration) mConf;
		float stepHeight = conf.getStepHeight();
		float stepWidth = conf.getStepWidth();
		int numberOfSteps = conf.getNumberOfSteps();

		float absoluteScaling = 1;
		float squareStairWidth = (float) Math.sqrt(stepWidth);

		MyVector3f diagonale = new MyVector3f();

		// Diagonale berechnen, wegen Rechteck sind alle Diagonalen gleich lang
		diagonale.sub(mBaseFootprint.getVertices().get(0).getPositionPtr(),
				mBaseFootprint.getCenter());
		float diagonalLength = diagonale.length();

		// absoluter Skalierungsfaktor von oberster auf unterste Ebene :=
		// (Diagonale_oberste + #Stufen * Wurzel_Stufenbreite) / Diagonale
		absoluteScaling = (diagonalLength + numberOfSteps * squareStairWidth)
				/ diagonalLength;
		mBaseFootprint.scale(absoluteScaling);

		// MyVector3f translationPerLevel = mBaseFootprint.getNormalPtr();
		MyVector3f absoluteTranslation = mBaseFootprint.getNormal();
		MyVector3f translationPerStep = mBaseFootprint.getNormal();

		translationPerStep.normalize();
		translationPerStep.scale(stepHeight);

		float totalHeight = numberOfSteps * stepHeight;
		absoluteTranslation.normalize();
		absoluteTranslation.scale(-totalHeight);
		LOGGER.debug("Translation: " + absoluteTranslation);

		// verschiebe den skalierten Footprint auf die unterste Ebene
		mBaseFootprint.translate(absoluteTranslation);
		MyPolygon currentFootprint = mBaseFootprint;

		// kein Alignment erforderlich, verwende eine leere Map
		Map<MyVector3f, Side> vectorToMap = new HashMap<MyVector3f, Side>(0);
		FreeComplex stairLevel = null;

		float scalingPerLevel;
		boolean isTop = false;

		for (int i = 0; i < numberOfSteps; i++) {

			// durch die
			stairLevel = new FreeComplex(mComponent.getParent(),
					currentFootprint, stepHeight, vectorToMap, isTop);
			stairLevel.create();

			// Abmelden, sonst versucht das System, die einzelnen Komponenten zu
			// zeichnen
			stairLevel.unregister();

			// Quads und Vertices extrhieren
			extractDataFromComponent(stairLevel);

			// berechne den neuen Scalingfaktor fuer die naechste Ebene
			diagonale.sub(currentFootprint.getVertices().get(0)
					.getPositionPtr(), currentFootprint.getCenter());
			diagonalLength = diagonale.length();

			scalingPerLevel = diagonalLength
					/ (diagonalLength + squareStairWidth);
			currentFootprint = new MyPolygon(currentFootprint.getVertices());

			// verschiebe und skaliere den Footprint fuer die naechste Ebene
			currentFootprint.translate(translationPerStep);
			currentFootprint.scale(scalingPerLevel);

		}

		// Komponente berechnen und fertigstellen
		mComponent.create();

	}

	// ------------------------------------------------------------------------------------------

	@Override
	protected String getType() {
		return "RoundStairs";
	}

	// ------------------------------------------------------------------------------------------

}
