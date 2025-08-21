package AsteroidField;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.shape.TriangleMesh;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class CrateredAsteroidMeshProvider implements AsteroidMeshProvider {
    @Override
    public TriangleMesh generateMesh(AsteroidParameters baseParams) {
        // Downcast for crater params
        CrateredAsteroidParameters params = (CrateredAsteroidParameters) baseParams;

        IcosphereMesh mesh = new IcosphereMesh(params.getRadius(), params.getSubdivisions());
        float[] verts = mesh.getVertices();
        Random rng = new Random(params.getSeed());

        List<double[]> craters = new ArrayList<>();
        for (int i = 0; i < params.getCraterCount(); i++) {
            double theta = 2 * Math.PI * rng.nextDouble();
            double phi = Math.acos(2 * rng.nextDouble() - 1);
            double x = Math.sin(phi) * Math.cos(theta);
            double y = Math.sin(phi) * Math.sin(theta);
            double z = Math.cos(phi);
            craters.add(new double[]{x, y, z});
        }

        double craterWidth = params.getCraterWidth();
        double craterDepth = params.getCraterDepth();

        for (int i = 0; i < verts.length; i += 3) {
            double x = verts[i], y = verts[i+1], z = verts[i+2];
            double r = Math.sqrt(x*x + y*y + z*z);
            double vx = x / r, vy = y / r, vz = z / r;
            double maxCraterEffect = 0;
            for (double[] crater : craters) {
                double dot = vx*crater[0] + vy*crater[1] + vz*crater[2];
                double angle = Math.acos(dot);
                double normalized = angle / (craterWidth * Math.PI);
                if (normalized < 1.0) {
                    double effect = (1.0 - normalized*normalized);
                    if (effect > maxCraterEffect) maxCraterEffect = effect;
                }
            }
            if (maxCraterEffect > 0) {
                r -= craterDepth * params.getRadius() * maxCraterEffect;
            }
            verts[i]   = (float) (vx * r);
            verts[i+1] = (float) (vy * r);
            verts[i+2] = (float) (vz * r);
        }

        // Deformation (optional)
        Random deformerRng = new Random(params.getSeed() ^ 0x12345678);
        double deform = params.getDeformation();
        for (int i = 0; i < verts.length; i += 3) {
            double x = verts[i], y = verts[i+1], z = verts[i+2];
            double r = Math.sqrt(x*x + y*y + z*z);
            double bump = 1.0 + deform * (deformerRng.nextDouble() - 0.5) * 2.0;
            verts[i] = (float)((x / r) * r * bump);
            verts[i+1] = (float)((y / r) * r * bump);
            verts[i+2] = (float)((z / r) * r * bump);
        }

        TriangleMesh triMesh = new TriangleMesh();
        triMesh.getPoints().setAll(verts);
        triMesh.getTexCoords().addAll(0,0);
        triMesh.getFaces().setAll(mesh.getFaces());
        triMesh.getFaceSmoothingGroups().clear();
        int numFaces = mesh.getFaces().length / 6;
        for (int i = 0; i < numFaces; i++) triMesh.getFaceSmoothingGroups().addAll(1);

        return triMesh;
    }

    @Override
    public List<Node> createParameterControls(Consumer<AsteroidParameters> onChange, AsteroidParameters current) {
        CrateredAsteroidParameters cur = (current instanceof CrateredAsteroidParameters)
                ? (CrateredAsteroidParameters) current
                : new CrateredAsteroidParameters.Builder().build();
        List<Node> controls = new ArrayList<>();

        Label countLabel = new Label("Crater Count:");
        Spinner<Integer> countSpinner = new Spinner<>();
        countSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, cur.getCraterCount()));
        countSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            onChange.accept(new CrateredAsteroidParameters.Builder()
                    .radius(cur.getRadius())
                    .subdivisions(cur.getSubdivisions())
                    .deformation(cur.getDeformation())
                    .seed(cur.getSeed())
                    .familyName(cur.getFamilyName())
                    .craterCount(newV)
                    .craterDepth(cur.getCraterDepth())
                    .craterWidth(cur.getCraterWidth())
                    .build());
        });

        Label depthLabel = new Label("Crater Depth:");
        Slider depthSlider = new Slider(0.05, 0.5, cur.getCraterDepth());
        depthSlider.setShowTickLabels(true);
        depthSlider.valueProperty().addListener((obs, oldV, newV) -> {
            onChange.accept(new CrateredAsteroidParameters.Builder()
                    .radius(cur.getRadius())
                    .subdivisions(cur.getSubdivisions())
                    .deformation(cur.getDeformation())
                    .seed(cur.getSeed())
                    .familyName(cur.getFamilyName())
                    .craterCount(cur.getCraterCount())
                    .craterDepth(newV.doubleValue())
                    .craterWidth(cur.getCraterWidth())
                    .build());
        });

        Label widthLabel = new Label("Crater Width:");
        Slider widthSlider = new Slider(0.05, 0.6, cur.getCraterWidth());
        widthSlider.setShowTickLabels(true);
        widthSlider.valueProperty().addListener((obs, oldV, newV) -> {
            onChange.accept(new CrateredAsteroidParameters.Builder()
                    .radius(cur.getRadius())
                    .subdivisions(cur.getSubdivisions())
                    .deformation(cur.getDeformation())
                    .seed(cur.getSeed())
                    .familyName(cur.getFamilyName())
                    .craterCount(cur.getCraterCount())
                    .craterDepth(cur.getCraterDepth())
                    .craterWidth(newV.doubleValue())
                    .build());
        });

        controls.add(new VBox(5, countLabel, countSpinner));
        controls.add(new VBox(5, depthLabel, depthSlider));
        controls.add(new VBox(5, widthLabel, widthSlider));
        return controls;
    }

    @Override
    public String getDisplayName() { return "Cratered"; }
}