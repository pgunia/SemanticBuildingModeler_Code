package semantic.building.modeler.math;

/**
 * @author Patrick Gunia
 * 
 *         Klasse zur Beschreibung einer Ebene in Normalenform also: n * (x - a)
 *         = 0 mit: n := Normalenvektor der Ebene a := Stuetzvektor der Ebene
 *         WICHTIG: alle Getter geben Deep-Copies der Objekte zurueck, keine
 *         Referenzen!
 */
public class Plane implements Cloneable {

	/** Stuetzvektor der Ebene */
	private transient MyVector3f mStuetzvektor = null;

	/** 1. aufspannender Vektor der Ebene */
	private transient MyVector3f mRichtungsvektor1 = null;

	/** 2. aufspannender Vektor der Ebene */
	private transient MyVector3f mRichtungsvektor2 = null;

	/** Normalenvektor der Ebene */
	private transient MyVector3f mNormal = null;

	/** Koeffizienten der Parameterform der Ebene */
	private transient Double[] mKoeffizienten = null;

	// ------------------------------------------------------------------------------------------
	/**
	 * Enum fuer vereinfachte Erstellung von Koordinatenebenen
	 */
	public enum CoordinatePlane {
		XY, XZ, YZ;
	}

	// ------------------------------------------------------------------------------------------
	/** soll nur bei Clone-Operationen verwendet werden */
	private Plane() {

	}

	// ------------------------------------------------------------------------------------------

	/**
	 * @param mNormal
	 * @param mStuetzvektor
	 */
	public Plane(final MyVector3f mNormal, final MyVector3f mStuetzvektor) {
		super();
		this.mStuetzvektor = mStuetzvektor;
		this.mNormal = mNormal;
		this.mNormal.normalize();

		calculateParameterfreieDarstellung();
		calculateParameterdarstellung();
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Konstruktor fuer Ebenen, die innerhalb der Koordinatenebenen liegen,
	 * vereinfacht die Erstellung solcher Strukturen. Stuetzvektor ist dabei
	 * immer der Koordinatenursprung, nur der Normalenvektor wird variiert.
	 * 
	 * @param plane
	 *            Enum gibt an, um welche Koordinatenebene es sich handelt
	 */
	public Plane(final CoordinatePlane plane) {

		mStuetzvektor = new MyVector3f(0.0f, 0.0f, 0.0f);
		switch (plane) {
		case XY:
			mNormal = new MyVector3f(0.0f, 0.0f, 1.0f);
			break;
		case XZ:
			mNormal = new MyVector3f(0.0f, 1.0f, 0.0f);
			break;
		case YZ:
			mNormal = new MyVector3f(1.0f, 0.0f, 0.0f);
			break;
		default:
			assert false : "FEHLER: Fuer den uebergebenen Ausrichtungstyp "
					+ plane + " ist keine Ebenenstruktur definiert";
			break;
		}
		calculateParameterfreieDarstellung();
		calculateParameterdarstellung();
	}

	// ------------------------------------------------------------------------------------------

	/**
	 * Berechne eine Ebene basierend auf zwei aufspannenden Vektoren und einem
	 * Stuetzvektor
	 * 
	 * @param stuetzvektor
	 * @param richtungsvektor1
	 * @param richtungsvektor2
	 */
	public Plane(final MyVector3f stuetzvektor,
			final MyVector3f richtungsvektor1, final MyVector3f richtungsvektor2) {

		final MyVector3f ebenennormale = new MyVector3f();
		mRichtungsvektor1 = richtungsvektor1;
		mRichtungsvektor2 = richtungsvektor2;
		ebenennormale.cross(richtungsvektor1, richtungsvektor2);
		mNormal = ebenennormale;
		mNormal.normalize();
		mStuetzvektor = stuetzvektor;

		calculateParameterfreieDarstellung();

	}

	// ------------------------------------------------------------------------------------------

	public MyVector3f getStuetzvektor() {
		assert mStuetzvektor != null : "Stuetzvektor ist null, Kopie kann nicht erstellt werden";
		return mStuetzvektor.clone();
	}

	// ------------------------------------------------------------------------------------------

	public MyVector3f getStuetzvektorPtr() {
		return mStuetzvektor;
	}

	// ------------------------------------------------------------------------------------------

	public void setStuetzvektor(final MyVector3f mStuetzvektor) {
		mStuetzvektor.normalizeRange();
		this.mStuetzvektor = mStuetzvektor;
	}

	// ------------------------------------------------------------------------------------------

	public MyVector3f getNormal() {
		assert mNormal != null : "Normalenvektor ist null, Kopie kann nicht erzeugt werden";
		return mNormal.clone();
	}

	// ------------------------------------------------------------------------------------------

	public MyVector3f getNormalPtr() {
		return mNormal;
	}

	// ------------------------------------------------------------------------------------------
	public void setNormal(final MyVector3f mNormal) {
		mNormal.normalizeRange();
		this.mNormal = mNormal;
	}

	// ------------------------------------------------------------------------------------------

	public MyVector3f getRichtungsvektor1Ptr() {
		return mRichtungsvektor1;
	}

	// ------------------------------------------------------------------------------------------

	public MyVector3f getRichtungsvektor1() {
		assert mRichtungsvektor1 != null : "Richtungsvektor1 ist null, Kopie kann nicht erzeugt werden";
		return mRichtungsvektor1.clone();
	}

	// ------------------------------------------------------------------------------------------

	public void setRichtungsvektor1(final MyVector3f mRichtungsvektor1) {
		this.mRichtungsvektor1 = mRichtungsvektor1;
	}

	// ------------------------------------------------------------------------------------------

	public MyVector3f getRichtungsvektor2Ptr() {
		return mRichtungsvektor2;
	}

	// ------------------------------------------------------------------------------------------

	public MyVector3f getRichtungsvektor2() {
		assert mRichtungsvektor2 != null : "Richtungsvektor2 ist null, Kopie kann nicht erzeugt werden";
		return mRichtungsvektor2.clone();
	}

	// ------------------------------------------------------------------------------------------

	public void setRichtungsvektor2(final MyVector3f mRichtungsvektor2) {
		this.mRichtungsvektor2 = mRichtungsvektor2;
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mNormal == null) ? 0 : mNormal.hashCode());
		result = prime * result
				+ ((mStuetzvektor == null) ? 0 : mStuetzvektor.hashCode());
		return result;
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public String toString() {

		final String linebreak = System.getProperty("line.separator");
		final StringBuffer strBuf = new StringBuffer(100);

		strBuf.append("Normalenform: " + mNormal + "*(x-" + mStuetzvektor + ")"
				+ linebreak);
		strBuf.append("Parameterfreie Form: " + mKoeffizienten[0] + "x1 + "
				+ mKoeffizienten[1] + "x2 + " + mKoeffizienten[2] + "x3 + "
				+ mKoeffizienten[3] + " = 0");
		return strBuf.toString();
	}

	// ------------------------------------------------------------------------------------------

	public Double[] getKoeffizienten() {
		return mKoeffizienten;
	}

	// ------------------------------------------------------------------------------------------

	public void setKoeffizienten(final Double[] mKoeffizienten) {
		this.mKoeffizienten = mKoeffizienten.clone();
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Plane other = (Plane) obj;
		if (mNormal == null) {
			if (other.mNormal != null)
				return false;
		} else if (!mNormal.equalsComponentByComponent(other.mNormal))
			return false;
		if (mStuetzvektor == null) {
			if (other.mStuetzvektor != null)
				return false;
		} else if (!mStuetzvektor
				.equalsComponentByComponent(other.mStuetzvektor))
			return false;
		return true;
	}

	// ------------------------------------------------------------------------------------------
	@Override
	public Plane clone() {
		Plane result = null;
		try {
			result = (Plane) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		result.setNormal(getNormal());
		result.setStuetzvektor(getStuetzvektor());

		// berechne die alternativen Darstellungsformen der Ebene
		result.calculateParameterfreieDarstellung();
		result.calculateParameterdarstellung();

		return result;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * berechnet eine parameterfreie Darstellung der Ebene aus der Normalform
	 * Koeffizientendarstellung der Form: A1x1 + A2x2 + A3x3 + A4 = 0 Es handelt
	 * sich bei dieser Berechnung um ein einfaches Ausmultiplizieren der
	 * Normalenform.
	 * http://www.rither.de/a/mathematik/lineare-algebra-und-analytische
	 * -geometrie/ebenen-vektoriell/umrechnen-zwischen-ebenengleichungen/
	 */
	private void calculateParameterfreieDarstellung() {

		assert mNormal != null : "Normalenvektor ist nicht gesetzt";

		mKoeffizienten = new Double[4];

		mKoeffizienten[0] = (double) mNormal.x;
		mKoeffizienten[1] = (double) mNormal.y;
		mKoeffizienten[2] = (double) mNormal.z;

		mKoeffizienten[3] = (double) (mNormal.dot(mStuetzvektor) * (-1));

		// System.out.println("calculateParameterfreieDarstellung: Normal: " +
		// mNormal + " Stuetzvektor: " + mStuetzvektor + " 3.Koeffizient: " +
		// mKoeffizienten[3]);

		// validiere die berechnete parameterfreie Darstellung
		final double testValue = Math.pow(mKoeffizienten[0], 2.0f)
				+ Math.pow(mKoeffizienten[1], 2.0f)
				+ Math.pow(mKoeffizienten[2], 2.0f);
		assert testValue > 0.0f : "Die berechnete parameterfreie Darstellung ist ungueltig, die Summe der Quadrate der Koeffizienten betraegt "
				+ testValue + " Normalenvektor: " + mNormal;

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode berechnet aus der Koordinatendarstellung eine
	 * Parameterdarstellung der Ebene
	 */
	private void calculateParameterdarstellung() {

		MyVector3f point1 = null, point2 = null;

		// implemnetiere unterschiedliche Umrechnungen, abhaengig davon, welche
		// Komponenten 0 sind
		if (mKoeffizienten[2] != 0.0f) {
			// rate 2 Punkte auf der Ebene
			// setze x- und y-Werte und berechne jeweils z
			double x = 1.0f;
			double y = 1.0f;

			double z = (mKoeffizienten[0] * x + mKoeffizienten[1] * y + mKoeffizienten[3]);
			// auf die andere Seite holen
			z *= -1.0f;
			z /= mKoeffizienten[2];
			point1 = new MyVector3f(x, y, z);

			// zweiten Punkt raten
			x = -1.0f;
			y = 1.0f;
			z = (mKoeffizienten[0] * x + mKoeffizienten[1] * y + mKoeffizienten[3]);

			z *= -1.0f;
			z /= mKoeffizienten[2];
			point2 = new MyVector3f(x, y, z);
		} else if (mKoeffizienten[0] != 0.0f) {

			double y = 1.0f;
			double z = 1.0f;

			double x = (mKoeffizienten[1] * y + mKoeffizienten[2] * z + mKoeffizienten[3]);

			// auf die andere Seite holen
			x *= -1.0f;
			x /= mKoeffizienten[0];
			point1 = new MyVector3f(x, y, z);

			y = -1.0f;
			z = 1.0f;

			x = (mKoeffizienten[1] * y + mKoeffizienten[2] * z + mKoeffizienten[3]);

			// auf die andere Seite holen
			x *= -1.0f;
			x /= mKoeffizienten[0];
			point2 = new MyVector3f(x, y, z);
		} else if (mKoeffizienten[1] != 0.0f) {
			double x = 1.0f;
			double z = 1.0f;

			double y = (mKoeffizienten[0] * x + mKoeffizienten[2] * z + mKoeffizienten[3]);

			// auf die andere Seite holen
			y *= -1.0f;
			y /= mKoeffizienten[1];
			point1 = new MyVector3f(x, y, z);

			x = -1.0f;
			z = 1.0f;

			y = (float) (mKoeffizienten[0] * x + mKoeffizienten[2] * z + mKoeffizienten[3]);

			// auf die andere Seite holen
			y *= -1.0f;
			y /= mKoeffizienten[1];
			point2 = new MyVector3f(x, y, z);
		}

		assert point1 != null && point2 != null : "Es konnten keine gueltigen Punkte fuer die Parameterform der Ebene errechnet werden";
		assert mStuetzvektor != null : "Es ist kein Stuetzvektor gesetzt";

		// berechne nun die Richtungsvektoren aus den geratetenen Puntken und
		// dem Stuetzvektor
		mRichtungsvektor1 = new MyVector3f();
		mRichtungsvektor1.sub(point1, mStuetzvektor);

		mRichtungsvektor2 = new MyVector3f();
		mRichtungsvektor2.sub(point2, mStuetzvektor);

	}

	// ------------------------------------------------------------------------------------------

}
