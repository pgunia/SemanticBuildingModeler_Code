package semantic.building.modeler.prototype.exception;

import org.apache.log4j.Logger;

public class PrototypeException extends Exception {

	private static Logger LOGGER = Logger.getLogger(PrototypeException.class);

	private final static String defaultErrorMessage = "An unexpected Error occured";

	// ------------------------------------------------------------
	// leerer Default-Konstruktor
	PrototypeException() {

		// Basisklasse
		super(defaultErrorMessage);

		// Logger
		LOGGER.error(this.getMessage());
	}

	// ------------------------------------------------------------
	// Konstruktor mit Message-Uebergabe
	public PrototypeException(String message) {

		// calle die Basisklasse
		super(message);

		// schreibe die aufgetretene Exception in das Logfile
		LOGGER.error(this.getMessage());
	}

	// ------------------------------------------------------------

	// Konstruktor mit Message-Uebergabe
	public PrototypeException(String message, Throwable cause) {

		// calle die Basisklasse
		super(message, cause);

		// schreibe die aufgetretene Exception in das Logfile
		LOGGER.error(this.getMessage() + " => " + this.getCause());
	}
	// ------------------------------------------------------------

}
