import org.apache.log4j.xml.DOMConfigurator;

import processing.core.PApplet;

public class ContinuousModelSynthesisMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DOMConfigurator.configureAndWatch("ressource/Logging/log4j.xml");
		PApplet.main(new String[] { ContinuousModelSynthesisApplet.class
				.getName() });

	}
}
