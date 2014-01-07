package semantic.building.modeler.weightedstraightskeleton.exception;

/**
 * 
 * @author Patrick Gunia Exceptions dieser Art werden geworfen, falls das
 *         "Quadrat-Problem" festgestellt wurde, bei dem ein Reflex-Vertex
 *         mehrere Split-Events im gleichen Punkt ausloest.
 * 
 */

public class SquareCaseException extends Exception {

	private final static String defaultMessage = "Es wurde eine irregulaere Split-Event-Konfiguration entdeckt. Die Verarbeitung wird abgebrochen!";

	// ------------------------------------------------------------
	/** Default-Konstruktor mit Standard-Message */
	public SquareCaseException() {
		super(defaultMessage);
	}

	// ------------------------------------------------------------

	/** Konstruktor mit Message-Uebergabe */
	public SquareCaseException(String message) {

		// calle die Basisklasse
		super(message);
	}

	// ------------------------------------------------------------

	/** Konstruktor mit Message-Uebergabe */
	public SquareCaseException(String message, Throwable cause) {

		// calle die Basisklasse
		super(message, cause);

	}
	// ------------------------------------------------------------

}
