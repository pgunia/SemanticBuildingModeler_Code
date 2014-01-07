package semantic.building.modeler.configurationservice.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.Namespace;

import semantic.building.modeler.math.MyPolygon;
import semantic.building.modeler.math.Vertex3d;

/**
 * Klasse stellt eine Meng evon Helper-Funktionen zur Verarbeitung von
 * XML-Elementen zur Verfuegung. Dadurch sollen wiederkehrende Aufgaben zentral
 * zusammengefasst werden.
 * 
 * @author Patrick Gunia
 * 
 */

public class XMLParsingHelper {

	/** Logging-Instanz */
	protected final static Logger LOGGER = Logger
			.getLogger(XMLParsingHelper.class);

	/** Singleton-Instanz */
	private static XMLParsingHelper mInstance = null;

	// -------------------------------------------------------------------------------------

	/** Leerer Default-Konstruktor */
	private XMLParsingHelper() {

	}

	// -------------------------------------------------------------------------------------

	/** Singleton-Getter */
	public static XMLParsingHelper getInstance() {
		if (mInstance == null)
			mInstance = new XMLParsingHelper();
		return mInstance;
	}

	// -------------------------------------------------------------------------------------

	/**
	 * Methode fuer Float-Zugriff
	 * 
	 * @param parent
	 *            Elternelement innerhalb der XML-Hierarchie
	 * @param childName
	 *            Bezeichner des Tags, dessen Wert gelesen werden soll
	 * @param ns
	 *            Namespace, innerhalb dessen sich das Kindelement befindet
	 * @return Float-Wert, der innerhalb des Tags gespeichert ist
	 */
	public Float getFloat(final Element parent, final String childName,
			final Namespace ns) {

		final Element child = parent.getChild(childName, ns);
		if (child == null) {
			LOGGER.warn("FEHLER: Es existiert kein Kindelement mit Namen "
					+ childName + " im Namespace " + ns.getURI());
			return null;
		}
		return Float.valueOf(child.getValue());
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode fuer Float-Zugriff
	 * 
	 * @param parent
	 *            Elternelement innerhalb der XML-Hierarchie
	 * @param childName
	 *            Bezeichner des Tags, dessen Wert gelesen werden soll
	 * @param ns
	 *            Namespace, innerhalb dessen sich das Kindelement befindet
	 * @param defaultValue
	 *            Standardwert fuer die jeweilige Variable
	 * @return Float-Wert, der innerhalb des Tags gespeichert ist
	 */
	public Float getFloat(final Element parent, final String childName,
			final Namespace ns, final Float defaultValue) {

		final Element child = parent.getChild(childName, ns);
		if (child == null) {
			LOGGER.warn("FEHLER: Es existiert kein Kindelement mit Namen "
					+ childName + " im Namespace " + ns.getURI());
			return defaultValue;
		}
		return Float.valueOf(child.getValue());
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode fuer String-Zugriff
	 * 
	 * @param parent
	 *            Elternelement innerhalb der XML-Hierarchie
	 * @param childName
	 *            Bezeichner des Tags, dessen Wert gelesen werden soll
	 * @param ns
	 *            Namespace, innerhalb dessen sich das Kindelement befindet
	 * @return String-Wert, der innerhalb des Tags gespeichert ist
	 */
	public String getString(final Element parent, final String childName,
			final Namespace ns) {
		final Element child = parent.getChild(childName, ns);
		if (child == null) {
			LOGGER.warn("FEHLER: Es existiert kein Kindelement mit Namen "
					+ childName + " im Namespace " + ns.getURI());
			return null;
		}
		return child.getValue();
	}

	// -------------------------------------------------------------------------------------

	/**
	 * Methode fuer String-Zugriff
	 * 
	 * @param parent
	 *            Elternelement innerhalb der XML-Hierarchie
	 * @param childName
	 *            Bezeichner des Tags, dessen Wert gelesen werden soll
	 * @param ns
	 *            Namespace, innerhalb dessen sich das Kindelement befindet
	 * @param defaultValue
	 *            Standardwert fuer die jeweilige Variable
	 * @return String-Wert, der innerhalb des Tags gespeichert ist
	 */
	public String getString(final Element parent, final String childName,
			final Namespace ns, String defaultValue) {
		final Element child = parent.getChild(childName, ns);
		if (child == null) {
			LOGGER.warn("FEHLER: Es existiert kein Kindelement mit Namen "
					+ childName + " im Namespace " + ns.getURI());
			return defaultValue;
		}
		return child.getValue();
	}

	// -------------------------------------------------------------------------------------

	/**
	 * Methode fuer Integer-Zugriff
	 * 
	 * @param parent
	 *            Elternelement innerhalb der XML-Hierarchie
	 * @param childName
	 *            Bezeichner des Tags, dessen Wert gelesen werden soll
	 * @param ns
	 *            Namespace, innerhalb dessen sich das Kindelement befindet
	 * @return Integer-Wert, der innerhalb des Tags gespeichert ist
	 */
	public Integer getInteger(final Element parent, final String childName,
			final Namespace ns) {
		final Element child = parent.getChild(childName, ns);
		if (child == null) {
			LOGGER.warn("FEHLER: Es existiert kein Kindelement mit Namen "
					+ childName + " im Namespace " + ns.getURI());
			return null;
		}
		return Integer.valueOf(child.getValue());
	}

	// -------------------------------------------------------------------------------------

	/**
	 * Methode fuer Integer-Zugriff
	 * 
	 * @param parent
	 *            Elternelement innerhalb der XML-Hierarchie
	 * @param childName
	 *            Bezeichner des Tags, dessen Wert gelesen werden soll
	 * @param ns
	 *            Namespace, innerhalb dessen sich das Kindelement befindet
	 * @param defaultValue
	 *            Standardwert fuer die jeweilige Variable
	 * @return Integer-Wert, der innerhalb des Tags gespeichert ist
	 */
	public Integer getInteger(final Element parent, final String childName,
			final Namespace ns, Integer defaultValue) {
		final Element child = parent.getChild(childName, ns);
		if (child == null) {
			LOGGER.warn("FEHLER: Es existiert kein Kindelement mit Namen "
					+ childName + " im Namespace " + ns.getURI());
			return defaultValue;
		}
		return Integer.valueOf(child.getValue());
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode liest Polygon-Daten aus einer XML-Konfiguration und liefert eine
	 * Polygon-Instanz an den Aufrufer zurueck
	 * 
	 * @param parent
	 *            Parent-Element innerhalb der XML-Konfiguration
	 * @return Polygon basierend auf den extrahierten Vertex-Koordinaten
	 */
	public MyPolygon getPolygon(final Element parent, final Namespace ns) {

		assert parent.getName().equals("polygon") : "FEHLER: Ungueltiges Konfigurationsobjekt! Tagname: "
				+ parent.getName();
		List<Element> vertices = parent.getChildren("vertex", ns);
		List<Vertex3d> vertexRepresentations = new ArrayList<Vertex3d>(
				vertices.size());

		Element curVert = null;
		float x, y, z;
		for (int i = 0; i < vertices.size(); i++) {
			curVert = vertices.get(i);
			x = getFloat(curVert, "x", ns);
			y = getFloat(curVert, "y", ns);
			z = getFloat(curVert, "z", ns);
			vertexRepresentations.add(new Vertex3d(x, y, z));
		}

		return new MyPolygon(vertexRepresentations);

	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode fuer Boolean-Zugriff
	 * 
	 * @param parent
	 *            Elternelement innerhalb der XML-Hierarchie
	 * @param childName
	 *            Bezeichner des Tags, dessen Wert gelesen werden soll
	 * @param ns
	 *            Namespace, innerhalb dessen sich das Kindelement befindet
	 * @return Boolean-Wert, der innerhalb des Tags gespeichert ist
	 */
	public Boolean getBoolean(final Element parent, final String childName,
			final Namespace ns) {
		final Element child = parent.getChild(childName, ns);
		if (child == null) {
			LOGGER.warn("FEHLER: Es existiert kein Kindelement mit Namen "
					+ childName + " im Namespace " + ns.getURI());
			return null;
		}
		return Boolean.valueOf(child.getValue());
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode fuer Boolean-Zugriff
	 * 
	 * @param parent
	 *            Elternelement innerhalb der XML-Hierarchie
	 * @param childName
	 *            Bezeichner des Tags, dessen Wert gelesen werden soll
	 * @param ns
	 *            Namespace, innerhalb dessen sich das Kindelement befindet
	 * @param defaultValue
	 *            Standardwert fuer die jeweilige Variable
	 * @return Boolean-Wert, der innerhalb des Tags gespeichert ist
	 */
	public Boolean getBoolean(final Element parent, final String childName,
			final Namespace ns, final Boolean defaultValue) {
		final Element child = parent.getChild(childName, ns);
		if (child == null) {
			LOGGER.warn("FEHLER: Es existiert kein Kindelement mit Namen "
					+ childName + " im Namespace " + ns.getURI());
			return defaultValue;
		}
		return Boolean.valueOf(child.getValue());
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode laedt die Konfigurationsdatei von einer URL und verwendet dabei
	 * die Java-NIO-Bibliotheken
	 * 
	 * @param url
	 *            URL des Objektes, das geladen werden soll
	 * @param newFilePath
	 *            Pfad relativ zum ressource/-Folder des Projektes an dem die
	 *            Datei nach dem Laden gespeichert wird
	 * @return Dateiobjekt, das basierend auf dem URL-Inputstream erzeugt wurde
	 */
	public File loadFromURL(final String url, final String newFilepath) {

		File temp = new File("ressource/" + newFilepath);

		// wenn die Datei bereits existiert, loesche sie und erstelle sie
		// anschliessend neu
		if (temp.exists()) {
			temp.delete();
			try {
				temp.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		ReadableByteChannel readChannel = null;
		WritableByteChannel writeChannel = null;
		InputStream in = null;
		try {

			// erzeuge einen Read-Channel, der Daten ueber den URL-Inputstream
			// liest
			URL src = new URL(url);
			in = src.openStream();
			readChannel = Channels.newChannel(in);
			ByteBuffer buffer = ByteBuffer.allocate(2048);

			// erzeuge einen Write-Channel in die Zieldatei
			writeChannel = new FileOutputStream(temp).getChannel();

			// lies die Bytes direkt in den Buffer
			while (readChannel.read(buffer) != -1 && writeChannel.isOpen()) {

				// Bufferzeiger wieder auf Anfang
				buffer.flip();

				// Bufferinhalt in den Write-Channel rausschreiben
				writeChannel.write(buffer);

				// sollte der Buffer nicht vollstaendig geschrieben worden sein,
				// verschiebe die verbleibenden Bytes an den Anfang
				buffer.compact();
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (writeChannel.isOpen())
					writeChannel.close();
				if (readChannel.isOpen())
					readChannel.close();
				in.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return temp;
	}

	// -------------------------------------------------------------------------------------
	/**
	 * Methode holt den String-Wert, der im uebergebenen Element gespeichert ist
	 * und gibt diesen an den Aufrufer zurueck
	 * 
	 * @param element
	 *            Element, aus dem der Stringwert ausgelesen werden soll
	 * @return Ausgelesener String-Wert
	 */
	public String getString(final Element element) {
		if (element == null) {
			LOGGER.warn("Uebergebenes Element ist null, Extraktion des gespeicherten Wertes ist nicht moeglich!");
			return null;
		} else {
			return element.getValue();
		}

	}
	// -------------------------------------------------------------------------------------

}
