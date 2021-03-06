package semantic.building.modeler.prototype.controller;

import java.io.File;
import java.util.Set;

import javax.media.opengl.GL;

import org.apache.log4j.Logger;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PFont;
import processing.opengl.PGraphicsOpenGL;
import semantic.building.modeler.configurationservice.controller.ConfigurationController;
import semantic.building.modeler.configurationservice.model.SystemConfiguration;
import semantic.building.modeler.prototype.city.City;
import semantic.building.modeler.prototype.exporter.ExportFormat;
import semantic.building.modeler.prototype.graphics.complex.CompositeComplex;
import semantic.building.modeler.prototype.graphics.interfaces.iGraphicComplex;
import semantic.building.modeler.prototype.service.IdentifierService;
import semantic.building.modeler.prototype.service.ObjectManagementService;
import semantic.building.modeler.prototype.service.ObjectPositioningService;
import semantic.building.modeler.prototype.service.TextureManagement;
import semantic.building.modeler.prototype.service.TextureManagement.TextureCategory;
import semantic.building.modeler.tesselation.service.TesselationService;

public class PrototypeController {

	/**
	 * Instanz der Appletklasse, wird als Renderkontext verwendet und an alle
	 * komplexen Objekte weitergereicht
	 */
	private PApplet mParentApplet = null;

	/** Logging-Instanz */
	private static Logger LOGGER = Logger.getLogger(PrototypeController.class);

	/** Root-Objekt der Objekthierarchie, enthaelt alle erzeugten Objekte */
	private CompositeComplex mRoot = null;

	/** Flag fuer das Zeichnen von Texturen auf die Objekte */
	private Boolean mDrawTextures = false;

	/** Flag legt fest, ob die Framerate angezeigt werden soll, oder nicht */
	private Boolean mDrawFramerate = true;

	/** Benoetigte Zeit zum Zeichnen eines Frames */
	private long mCurrentTimePerFrame = 0;

	/** Schriftart zum Zeichnen der Framerate etc. */
	private PFont mFont = null;

	/**
	 * Instanz der System-Konfigurationsklasse mit saemtlichen relevanten
	 * Systemparametern
	 */
	private transient SystemConfiguration mConfig = null;

	/** Pfad zur Systemkonfiguration */
	private static String mConfigFilePath = "ressource/Config/SystemConfiguration.xml";

	/** Flag zur Verwendung direkter OpenGL-Draw-Calls */
	private Boolean mUseGL = true;
	
	private static String pathSeparator = System.getProperty("file.separator");

	// ------------------------------------------------------------------------------------------
	/**
	 * Default Konstruktor Initialisiert Service-Klassen
	 */
	public PrototypeController(final PApplet parentApplet) {

		mParentApplet = parentApplet;

		final ConfigurationController sysConf = new ConfigurationController();
		final File configFile = new File(mConfigFilePath);
		mConfig = sysConf.processSystemConfiguration(configFile);

		parentApplet.size(mConfig.getWindowWidth(), mConfig.getWindowHeight(),
				PConstants.OPENGL);

		if (mUseGL) {
			PGraphicsOpenGL pgl = (PGraphicsOpenGL) parentApplet.g;
			GL gl = pgl.beginGL();
			gl.glFrontFace(GL.GL_CW);
			// gl.glEnable(GL.GL_CULL_FACE);
			gl.glCullFace(GL.GL_BACK);
			gl.glPolygonMode(GL.GL_FRONT, GL.GL_FILL);
			pgl.endGL();
		}

		// Texturmanagement initialisieren
		TextureManagement.getInstance().initializeTextureManagement(
				parentApplet, mConfig);
		if (mUseGL) {
			TextureManagement.getInstance().setUseGL(mUseGL);
		}

		// Objectpositioning-Service initialisieren
		ObjectPositioningService.getInstance().init(parentApplet, mConfig);

		// TesselationService initialisieren
		TesselationService.getInstance().init(parentApplet);

		// Schrift laden
		mFont = mParentApplet.loadFont("ressource/Font/CourierNewPSMT-24.vlw");
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Zuruecksetzen der gesamten Verarbeitungslogik
	 */
	private void reset() {

		mRoot = null;
		TextureManagement.getInstance().reset();
		ObjectManagementService.getInstance().reset();
		IdentifierService.getInstance().reset();
		mDrawTextures = false;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode leitet die drawCalls an alle Subobjekte weiter, die innerhalb der
	 * Managementservices verspeichert sind
	 */
	public void draw(final Float dragRotateX, final Float dragRotateY) {
		long startFrame = System.currentTimeMillis();

		final ObjectManagementService objects = ObjectManagementService
				.getInstance();
		final Set<String> renderables = objects.getRenderables();

		// schnelle Iteration ueber Array und Runterzaehlen
		final String[] renderableIDs = (String[]) renderables
				.toArray(new String[renderables.size()]);
		int numberOfRenderables = renderables.size();

		// verwende direkte OpenGL-Draw-Calls ueber den GL-Device-Context
		if (mUseGL) {
			final PGraphicsOpenGL pgl = (PGraphicsOpenGL) mParentApplet.g;
			final GL gl = pgl.beginGL(); // always use the GL object returned by
									// beginGL

			gl.glRotatef(dragRotateY * 10, 1.0f, 0.0f, 0.0f);
			gl.glRotatef(-dragRotateX * 10, 0.0f, 1.0f, 0.0f);

			// durchlaufe alle zeichenbaren Objekte und rufe ihre Draw-Routinen
			// auf
			for (int i = numberOfRenderables - 1; i >= 0; i--) {
				final iGraphicComplex tempObject = (iGraphicComplex) objects
						.getObject(renderableIDs[i]);
				tempObject.drawGL(mDrawTextures, gl);
			}
			pgl.endGL();
		}
		// verwende die Processing-GL-Wrapper
		else {
			mParentApplet.pushMatrix();

			// rotiere basierend auf dem Mousedragging
			mParentApplet.rotateX(dragRotateY);
			mParentApplet.rotateY(-dragRotateX);

			// durchlaufe alle zeichenbaren Objekte und rufe ihre Draw-Routinen
			// auf
			for (int i = numberOfRenderables - 1; i >= 0; i--) {
				final iGraphicComplex tempObject = (iGraphicComplex) objects
						.getObject(renderableIDs[i]);
				tempObject.draw(mDrawTextures);
			}

			mParentApplet.popMatrix();
		}

		long endFrame = System.currentTimeMillis();
		mCurrentTimePerFrame = endFrame - startFrame;

		if (mCurrentTimePerFrame == 0)
			mCurrentTimePerFrame = 1;

		if (mDrawFramerate)
			drawFrameInformation();

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode dient der Geometrieerzeugung
	 */
	public void createGeometry() {

		mParentApplet.noLoop();

		// alles zuruecksetzen
		reset();
		mRoot = new CompositeComplex(mParentApplet);
		mRoot.create();


		// wenn es sich um einen Windows-Pfad handelt, escapen!
		if (pathSeparator.equals("\\")) {
			pathSeparator = "\\";
		}

		final String configPath = mConfig.getCityConfigurationFolder()
				+ pathSeparator + mConfig.getCityConfigurationFile();
		final City city = new City();
		city.loadConfiguration(configPath);
		city.createCity(mRoot);

		mParentApplet.loop();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode ruft fuer das globale Composite-Objekt die Berechnungsmethode
	 * fuer Texturkoordinaten auf
	 */
	public void computeWallTexturing() {
		if (mRoot != null) {
			mParentApplet.noLoop();
			mRoot.setTextureByCategory(TextureCategory.Wall);
			mParentApplet.loop();
		}
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode ruft die Dachberechnungsroutine fuer das Composite-Objekt auf
	 */
	public void computeRoof() {
		if (mRoot != null) {
			// unterbreche die Loop fuer die Dauer der Roof-Berechnung, um
			// Fehler beim Texturkoordinatenzugriff zu vermeiden
			// sollte die FrameLoop schneller als die Berechnung sein, kommt es
			// sonst zu Fehlern
			mParentApplet.noLoop();
			mRoot.computeRoof();
			mParentApplet.loop();
		}
	}

	// ------------------------------------------------------------------------------------------
	public void exportModel() {
		if (mRoot != null) {
			mRoot.exportModelToFile(mConfig.getExportModelFolder(),
					mConfig.getExportFileName(), ExportFormat.OBJ);
		}
	}

	// ------------------------------------------------------------------------------------------
	public void printSceneTree() {
		if (mRoot != null)
			mRoot.printComplex("");
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * @return the mDrawTextures
	 */
	public Boolean getDrawTextures() {
		return mDrawTextures;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mDrawTextures
	 *            the mDrawTextures to set
	 */
	public void setDrawTextures(Boolean mDrawTextures) {
		this.mDrawTextures = mDrawTextures;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * @return the mDrawFramerate
	 */
	public Boolean getDrawFramerate() {
		return mDrawFramerate;
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mDrawFramerate
	 *            the mDrawFramerate to set
	 */
	public void setDrawFramerate(Boolean mDrawFramerate) {
		this.mDrawFramerate = mDrawFramerate;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode zeichnet die aktuelle Framerate und Vertex-Anzahl
	 */
	private void drawFrameInformation() {

		long fps = 1000 / mCurrentTimePerFrame;

		// Position berechnen
		int windowWidth = mConfig.getWindowWidth() - 100;
		windowWidth -= 100;

		mParentApplet.textFont(mFont);
		mParentApplet.fill(255);
		mParentApplet.text(fps + " fps", windowWidth, 30);
		mParentApplet.text(mRoot.getVertexCount() + " Verts", windowWidth, 50);

	}
	// ------------------------------------------------------------------------------------------

}
