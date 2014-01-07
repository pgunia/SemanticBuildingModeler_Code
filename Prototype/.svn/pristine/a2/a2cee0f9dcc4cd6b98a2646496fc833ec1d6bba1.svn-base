package semantic.building.modeler.prototype.building;

import java.util.ArrayList;
import java.util.List;

import processing.core.PApplet;
import semantic.building.modeler.configurationservice.model.enums.FloorPosition;
import semantic.building.modeler.math.MyPolygon;
import semantic.building.modeler.math.MyVector3f;
import semantic.building.modeler.math.Vertex3d;
import semantic.building.modeler.prototype.graphics.complex.FloorComplex;
import semantic.building.modeler.prototype.service.ObjectPositioningService;
import semantic.building.modeler.prototype.service.TextureManagement.TextureCategory;

public class TestBuilding extends AbstractBuilding {

	public TestBuilding(PApplet applet, MyVector3f position) {
		super(applet, position, null);
	}

	@Override
	public String getType() {
		return "Test";
	}

	@Override
	public void create() {

		float buildingLength = 150.0f, buildingWidth = 200.0f, floorHeight = 100.0f, currentHeight = 0.0f;
		boolean isTop = false;

		List<Vertex3d> mVertices = new ArrayList<Vertex3d>();
		mVertices.add(new Vertex3d(-125.644875f, 0.0f, -13.185925f));
		mVertices.add(new Vertex3d(-117.021164f, 0.0f, -13.185921f));
		mVertices.add(new Vertex3d(-117.021164f, 0.0f, -80.66721f));
		mVertices.add(new Vertex3d(-28.233147f, 0.0f, -80.66721f));
		mVertices.add(new Vertex3d(-28.233147f, 0.0f, -86.36565f));
		mVertices.add(new Vertex3d(-21.765268f, 0.0f, -98.68915f));
		mVertices.add(new Vertex3d(-10.31123f, 0.0f, -106.5953f));
		mVertices.add(new Vertex3d(3.5049987f, 0.0f, -108.27289f));
		mVertices.add(new Vertex3d(16.518261f, 0.0f, -103.33761f));
		mVertices.add(new Vertex3d(25.747404f, 0.0f, -92.92006f));
		mVertices.add(new Vertex3d(28.767456f, 0.0f, -80.66721f));
		mVertices.add(new Vertex3d(117.021164f, 0.0f, -80.66721f));
		mVertices.add(new Vertex3d(117.021164f, 0.0f, -21.688837f));
		mVertices.add(new Vertex3d(126.56401f, 0.0f, -21.688837f));
		mVertices.add(new Vertex3d(126.56402f, 0.0f, 21.688837f));
		mVertices.add(new Vertex3d(117.021164f, 0.0f, 21.688843f));
		mVertices.add(new Vertex3d(117.021164f, 0.0f, 80.66721f));
		mVertices.add(new Vertex3d(19.931082f, 0.0f, 80.66721f));
		mVertices.add(new Vertex3d(16.455908f, 0.0f, 91.3627f));
		mVertices.add(new Vertex3d(6.2856035f, 0.0f, 98.75186f));
		mVertices.add(new Vertex3d(-6.2855835f, 0.0f, 98.75186f));
		mVertices.add(new Vertex3d(-16.455889f, 0.0f, 91.3627f));
		mVertices.add(new Vertex3d(-19.93106f, 0.0f, 80.66721f));
		mVertices.add(new Vertex3d(-117.021164f, 0.0f, 80.66721f));
		mVertices.add(new Vertex3d(-117.021164f, 0.0f, 13.185925f));
		mVertices.add(new Vertex3d(-125.644875f, 0.0f, 13.185925f));
		MyPolygon baseFootprint = new MyPolygon(mVertices);

		MyVector3f posTranslation = null;
		/*
		 * mOuterFootprint = new RectFootprint(new Plane(CoordinatePlane.XZ),
		 * buildingLength, buildingWidth); baseFootprint =
		 * mOuterFootprint.getFootprints().get(0);
		 * 
		 * 
		 * FloorComplex groundFloor = new FloorComplex(mBuilding.getParent(),
		 * baseFootprint.clone(), floorHeight,
		 * mBuilding.getNormalToDirectionMap(), FloorPosition.GROUND, 0, isTop);
		 * posTranslation = baseFootprint.getNormal();
		 * posTranslation.add(mPosition); groundFloor.create();
		 * groundFloor.translate(posTranslation); groundFloor.update();
		 * mBuilding.addComplex(groundFloor); currentHeight += floorHeight;
		 */
		FloorComplex secondFloor = new FloorComplex(mBuilding.getParent(),
				baseFootprint.clone(), floorHeight,
				mBuilding.getNormalToDirectionMap(), FloorPosition.TOP, 1,
				isTop);
		posTranslation = baseFootprint.getNormal();
		posTranslation.scale(currentHeight);
		posTranslation.add(mPosition);
		secondFloor.create();
		secondFloor.translate(posTranslation);
		secondFloor.update();
		mBuilding.addComplex(secondFloor);
		currentHeight += floorHeight;

		/*
		 * FasciaConfiguration fasciaConf = new FasciaConfiguration(groundFloor,
		 * ModelCategory.Moulding_Gotisch, 0.1f); Fascia fascia = new
		 * Fascia(mBuilding.getParent(), fasciaConf); fascia.createComponent();
		 * mBuilding.addComplex(fascia.getComponent());
		 * 
		 * AbstractQuad frontQuad = groundFloor.getQuadByDirection(Side.FRONT);
		 * assert frontQuad != null : "FEHLER: Kein Front-Quad gefunden!";
		 * 
		 * 
		 * 
		 * 
		 * MouldingConfig mouldConf = new MouldingConfig(
		 * ModelCategory.Moulding_Eckstein, frontQuad, VerticalAlignment.TOP,
		 * 0.5f, null, frontQuad.getPolygon()); Moulding moulding = new
		 * Moulding(mBuilding.getParent(), mouldConf);
		 * moulding.createComponent();
		 * mBuilding.addComplex(moulding.getComponent());
		 */
		ObjectPositioningService posService = ObjectPositioningService
				.getInstance();
		// posService.addDoor(mBuilding, "Door");
		// posService.addWindows(mBuilding, "Window_Jugendstil");
		// posService.addWindowLedges(mBuilding);

		/*
		 * ComponentLedgeConfiguration windowLedgeConfig = new
		 * ComponentLedgeConfiguration(mBuilding, VerticalAlignment.BOTTOM,
		 * ModelCategory.Window, ModelCategory.Moulding_Eckstein, false);
		 * ComponentLedge windowLedge = new
		 * ComponentLedge(mBuilding.getParent(), windowLedgeConfig);
		 * windowLedge.createComponent();
		 * mBuilding.addComplex(windowLedge.getComponent());
		 * 
		 * 
		 * ComponentLedgeConfiguration windowTopLedgeConfig = new
		 * ComponentLedgeConfiguration(mBuilding, VerticalAlignment.TOP,
		 * ModelCategory.Window, ModelCategory.Moulding_Gotisch, false);
		 * ComponentLedge windowTopLedge = new
		 * ComponentLedge(mBuilding.getParent(), windowTopLedgeConfig);
		 * windowTopLedge.createComponent();
		 * mBuilding.addComplex(windowTopLedge.getComponent());
		 */
		finalizeBuilding();

		/*
		 * FasciaConfiguration fasciaConf = new FasciaConfiguration(cube,
		 * ModelCategory.Moulding_Gotisch, 0.3f); Fascia fascia = new
		 * Fascia(mBuilding.getParent(), fasciaConf); fascia.createComponent();
		 * 
		 * mBuilding.addComplex(fascia.getComponent());
		 */
		/*
		 * // TEST List<AbstractQuad> quads = cube.getOutdoorQuads();
		 * AbstractQuad currentQuad = null; MouldingConfig mouldConf = null;
		 * Moulding moulding = null;
		 * 
		 * 
		 * AbstractQuad right = cube.getQuadByDirection(Side.RIGHT); mouldConf =
		 * new MouldingConfig(ModelCategory.Moulding_Gotisch, right,
		 * VerticalAlignment.TOP, 0.1f, null); moulding = new
		 * Moulding(mBuilding.getParent(), mouldConf);
		 * moulding.createComponent();
		 * mBuilding.addComplex(moulding.getComponent());
		 * 
		 * for (int i = 0; i < quads.size(); i++) { currentQuad = quads.get(i);
		 * if(currentQuad.getDirection().equals(Side.TOP) ||
		 * currentQuad.getDirection().equals(Side.BOTTOM)) continue; mouldConf =
		 * new MouldingConfig(ModelCategory.Moulding_Gotisch, currentQuad,
		 * VerticalAlignment.TOP, 0.1f, null); moulding = new
		 * Moulding(mBuilding.getParent(), mouldConf);
		 * moulding.createComponent();
		 * mBuilding.addComplex(moulding.getComponent()); }
		 */

	}

	// ------------------------------------------------------------------------------------------
	/**
	 * Methode fuehrt automatisiert verschiedene Berechnungsschritte durch, die
	 * bei allen Arten von Gebaeuden relevant sind Berechnung von Waenden,
	 * Daechern, Texturkoordinaten, Tesselation etc.
	 */
	protected void finalizeBuilding() {

		// Dach kann nicht hinzugefuegt werden, da keine Config-Datei verfuegbar
		// ist
		addRoof();

		// FUER DEBUGGING RAUSGENOMMEN
		insetWalls(mBuilding, 4.0f);

		// tesseliere das Gebaeude
		mBuilding.tesselate();

		// berechne durchgaengige Quads ueber Stockwerksgrenzen hinweg
		mBuilding.computePolyQuadsForContiguousBaseQuads();

		// setze Wandtexturen
		mBuilding.setTextureByCategory(TextureCategory.Wall);

	}
	// ------------------------------------------------------------------------------------------

}
