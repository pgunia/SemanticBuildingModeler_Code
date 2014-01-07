import java.io.File;

import org.apache.log4j.xml.DOMConfigurator;

import semantic.building.modeler.configurationservice.controller.ConfigurationController;

public class ConfigurationServiceMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DOMConfigurator.configureAndWatch("ressource/Logging/log4j.xml");
		File xmlConfiguration = null;

		xmlConfiguration = new File("ressource/Config/SystemConfiguration.xml");

		assert xmlConfiguration.exists() : "FEHLER: Konfigurationsdatei wurde nicht gefunden!";
		final ConfigurationController confController = new ConfigurationController();

		// confController.processCityConfiguration(xmlConfiguration);
		confController.processSystemConfiguration(xmlConfiguration);
	}

	// -------------------------------------------------------------------------------------

}
