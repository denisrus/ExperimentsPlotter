package com.mobimore.controller;

import com.mobimore.model.Experiment;
import com.mobimore.model.ExperimentsData;
import eu.mihosoft.jcsg.ext.openjfx.shape3d.PolygonMesh;
import eu.mihosoft.jcsg.ext.openjfx.shape3d.PolygonMeshView;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Shape3D;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import org.fxyz3d.scene.Crosshair3D;
import org.fxyz3d.shapes.composites.ScatterPlot;
import org.fxyz3d.utils.CameraTransformer;

import java.util.*;

public class ChartsController {

    @FXML
    public StackPane stackPaneWithGraph;

    @FXML
    public SubScene graphSubScene;

    @FXML
    public Label pointInformation;

    private ScatterPlot plot;

    private PerspectiveCamera camera;
    private final CameraTransformer cameraTransform = new CameraTransformer();

    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;

    private IntegerProperty displayGeneStartPercent = new SimpleIntegerProperty(1);
    private int displayGeneStart = 0;
    private IntegerProperty displayGeneSize = new SimpleIntegerProperty(50);

    private ExperimentsData data;
    private int experimentNumber = 0;

    private List<Color> clusterColorsList = new ArrayList<>();

    private static final int MULTIPLIER = 10;
    private static final String POINT_INFORMATION_TEXT = "Point information: ";
    private static final String POINT_INFORMATION_PLACEHOLDER_TEXT = "Select point to display information";
    private static final String POINT_CLUSTER_NUMBER_PROPERTY = "clusterNumber";
    private static final String POINT_CLUSTER_GEN_PROPERTY = "clusterGen";
    private static final String POINT_CLUSTER_SAMPLE_PROPERTY = "clusterSample";
    private static final String POINT_CLUSTER_TIME_PROPERTY = "clusterTime";

    private Crosshair3D crosshair3DSelection = null;

    public int getDisplayGeneStartPercent() {
        return displayGeneStartPercent.get();
    }
    public IntegerProperty displayGeneStartPercentProperty() {
        return displayGeneStartPercent;
    }
    public void setDisplayGeneStartPercent(int displayGeneStartPercent) {
        this.displayGeneStartPercent.set(displayGeneStartPercent);
    }

    public int getDisplayGeneSize() {
        return displayGeneSize.get();
    }
    public IntegerProperty displayGeneSizeProperty() {
        return displayGeneSize;
    }
    public void setDisplayGeneSize(int displayGeneSize) {
        this.displayGeneSize.set(displayGeneSize);
    }

    public ExperimentsData getData() {
        return data;
    }
    public void setData(ExperimentsData data) {
        this.data = data;
    }

    public int getExperimentNumber() {
        return experimentNumber;
    }
    public void setExperimentNumber(int experimentNumber) {
        this.experimentNumber = experimentNumber;
    }

    @FXML
    private void initialize(){
        pointInformation.setText(POINT_INFORMATION_TEXT+"\n"+POINT_INFORMATION_PLACEHOLDER_TEXT);

        setupCameraAndLight();

        graphSubScene.widthProperty().bind(stackPaneWithGraph.widthProperty());
        graphSubScene.heightProperty().bind(stackPaneWithGraph.heightProperty());

        graphSubScene.setOnMousePressed((MouseEvent me) -> {
            graphSubScene.requestFocus();
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();
            graphSubScene.requestFocus();
        });
        graphSubScene.setOnMouseClicked((MouseEvent me)->{

            if(me.getButton().equals(MouseButton.PRIMARY)){
                PickResult pickResult = me.getPickResult();

                if (pickResult != null && pickResult.getIntersectedNode() != null && pickResult.getIntersectedNode() instanceof Shape3D) { //shape selected
                    Shape3D pickedShape = (Shape3D) pickResult.getIntersectedNode();
                    Integer selectedClusterNumber = (Integer) pickedShape.getProperties().get(POINT_CLUSTER_NUMBER_PROPERTY);

                    if (selectedClusterNumber != null) {
                        String selectedPointGen = (String) pickedShape.getProperties().get(POINT_CLUSTER_GEN_PROPERTY);
                        String selectedPointSample = (String) pickedShape.getProperties().get(POINT_CLUSTER_SAMPLE_PROPERTY);
                        String selectedPointTime = (String) pickedShape.getProperties().get(POINT_CLUSTER_TIME_PROPERTY);

                        pointInformation.setText(POINT_INFORMATION_TEXT +
                                "\nGen: " + selectedPointGen +
                                "\nSample: "+selectedPointSample +
                                "\nTime: "+selectedPointTime);

                        Point3D center = getNodeCenter(pickedShape);

                        ((Group) graphSubScene.getRoot()).getChildren().remove(crosshair3DSelection);

                        crosshair3DSelection = new Crosshair3D(new org.fxyz3d.geometry.Point3D((float)center.getX(),
                                (float)center.getY(),(float)center.getZ()), 10, 2);

                        ((Group) graphSubScene.getRoot()).getChildren().add(crosshair3DSelection);
                        resetClusterColors();

                        plot.scatterDataGroup.getChildren().stream().map(e -> (Shape3D) e)
                                .filter(shape3D -> !(shape3D.getProperties().get(POINT_CLUSTER_NUMBER_PROPERTY).equals(selectedClusterNumber)))
                                .forEach(shape3D -> ((PhongMaterial) shape3D.getMaterial()).setDiffuseColor(Color.TRANSPARENT));
                    }
                }

                if(me.getClickCount() == 2){
                    resetClusterColors();
                    ((Group) graphSubScene.getRoot()).getChildren().remove(crosshair3DSelection);
                    pointInformation.setText(POINT_INFORMATION_TEXT+"\n"+POINT_INFORMATION_PLACEHOLDER_TEXT);
                }
            }
        });
        graphSubScene.setOnMouseDragged((MouseEvent me) -> {
            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseDeltaX = (mousePosX - mouseOldX);
            mouseDeltaY = (mousePosY - mouseOldY);

            double modifier = 10.0;
            double modifierFactor = 0.1;

            if (me.isControlDown()) {
                modifier = 0.1;
            }
            if (me.isShiftDown()) {
                modifier = 50.0;
            }
            if (me.isPrimaryButtonDown()) {
                cameraTransform.ry.setAngle(((cameraTransform.ry.getAngle() + mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);  // +
                cameraTransform.rx.setAngle(((cameraTransform.rx.getAngle() - mouseDeltaY * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);  // -
            } else if (me.isSecondaryButtonDown()) {
                camera.setTranslateX(camera.getTranslateX() - mouseDeltaX * modifierFactor * modifier * 0.3);
                camera.setTranslateY(camera.getTranslateY() - mouseDeltaY * modifierFactor * modifier * 0.3);
            }
        });
        graphSubScene.setOnScroll((ScrollEvent e)->{
            double modifier = 10.0;
            double modifierFactor = 0.1;

            if (e.isControlDown()) {
                modifier = 0.1;
            }
            if (e.isShiftDown()) {
                modifier = 50.0;
            }

            double z = camera.getTranslateZ();
            double newZ = z + e.getDeltaY() * modifierFactor * modifier;
            camera.setTranslateZ(newZ);
        });

        displayGeneStartPercent.addListener(observable -> {
            if (data == null) {
                return;
            }

            int genesCount = data.getGen().size() - displayGeneSize.intValue();
            if (genesCount <= 0) {
                genesCount = data.getGen().size();
            }
            int val = displayGeneStartPercent.getValue();
            if (val == 0) {
                val = 1;
            }
            displayGeneStart = mapRange(val, 1, 100, 0, genesCount);
            displayPlot();
        });
        displayGeneSize.addListener(observable -> {
            if (data == null) {
                return;
            }

            int genesCount = data.getGen().size() - displayGeneSize.intValue();
            if (genesCount <= 0) {
                genesCount = data.getGen().size();
            }
            int val = displayGeneStartPercent.getValue();
            if (val == 0) {
                val = 1;
            }
            displayGeneStart = mapRange(val, 1, 100, 0, genesCount);
            displayPlot();
        });
    }

    private void resetClusterColors(){
        plot.scatterDataGroup.getChildren().stream().map(e -> (Shape3D) e)
                .forEach(shape3D -> {
                    Integer clusterNumber = (Integer) shape3D.getProperties().get(POINT_CLUSTER_NUMBER_PROPERTY);
                    Color color = clusterColorsList.get(clusterNumber);
                    ((PhongMaterial) shape3D.getMaterial()).setDiffuseColor(color);
                });
    }

    private void setupCameraAndLight(){
        camera = new PerspectiveCamera(true);
        camera.setDepthTest(DepthTest.DISABLE);
        cameraTransform.setTranslate(0, 0, 0);
        cameraTransform.getChildren().add(camera);
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);
        camera.setTranslateZ(-100);
        /*cameraTransform.ry.setAngle(-45.0);*/
        cameraTransform.rx.setAngle(180.0);
        //add a Point Light for better viewing of the grid coordinate system
        PointLight light = new PointLight(Color.WHITE);
        cameraTransform.getChildren().add(light);
        cameraTransform.getChildren().add(new AmbientLight(Color.WHITE));
        light.setTranslateX(camera.getTranslateX());
        light.setTranslateY(camera.getTranslateY());
        light.setTranslateZ(camera.getTranslateZ());
        graphSubScene.setCamera(camera);
    }

    public void resetPlot(){
        setupCameraAndLight();
        ((Group) graphSubScene.getRoot()).getChildren().clear();
    }
    public void displayPlot() {
        ((Group) graphSubScene.getRoot()).getChildren().clear();
        if (data == null || experimentNumber >= data.getExperiments().size()) {
            return;
        }

        final List<Double> dataX = new ArrayList<>();
        final List<Double> dataY = new ArrayList<>();
        final List<Double> dataZ = new ArrayList<>();
        final List<Color> colors = new ArrayList<>();

        final Experiment experiment = data.getExperiments().get(experimentNumber);
        final List<List<List<Integer>>> clusters = experiment.getClusters();
        filterClusters(clusters);

        clusterColorsList = generateColors(clusters.size());

        Map<Point3D, Integer> pointsAtCoordinates = new HashMap<>();

        for (int i = 0, clustersSize = clusters.size(); i < clustersSize; i++) {
            List<List<Integer>> cluster = clusters.get(i);
            Color color = clusterColorsList.get(i);
            for (List<Integer> point : cluster) {
                double x = point.get(0) * MULTIPLIER;
                double y = point.get(1) * MULTIPLIER;
                double z = point.get(2) * MULTIPLIER;
                dataX.add(x);
                dataY.add(y);
                dataZ.add(z);
                Point3D point3D = new Point3D(x, y, z);
                if (!pointsAtCoordinates.containsKey(point3D)) {
                    pointsAtCoordinates.put(point3D, 1);
                }else{
                    Integer points = pointsAtCoordinates.get(point3D);
                    pointsAtCoordinates.put(point3D, points + 1);
                }
                colors.add(color);
            }
        }

        double nodeRadius = MULTIPLIER / 5d;
        plot = new ScatterPlot(10 * MULTIPLIER, nodeRadius, true);
        plot.setXYZData(dataX, dataY, dataZ, colors);

        List<String> genes = data.getGen();
        genes = genes.subList(displayGeneStart, Math.min(displayGeneStart + displayGeneSize.intValue()
                ,genes.size()));
        final int genesCount = genes.size();

        final List<String> samples = data.getSamples();
        final List<String> times = data.getTime();
        final List<Node> nodes = plot.scatterDataGroup.getChildren();

        int pointNumber = 0;
        for (int clusterNumber = 0, clustersSize = clusters.size(); clusterNumber < clustersSize; clusterNumber++) {
            List<List<Integer>> cluster = clusters.get(clusterNumber);
            for (int i1 = 0, pointsInCluster = cluster.size(); i1 < pointsInCluster; i1++) {
                Node node = nodes.get(pointNumber);
                if (node instanceof Shape3D) {
                    Shape3D shape3D = (Shape3D) node;
                    Point3D coordinates = getNodeCenter(shape3D);
                    Integer simillarPoints = pointsAtCoordinates.get(coordinates);
                    switch (simillarPoints) {
                        case 1:
                            shape3D.getTransforms().add(new Translate(nodeRadius / 2d, 0, 0));
                            break;
                        case 2:
                            shape3D.getTransforms().add(new Translate(-nodeRadius / 2d, 0, 0));
                            break;
                        case 3:
                            shape3D.getTransforms().add(new Translate(0, nodeRadius / 2d, 0));
                            break;
                        case 4:
                            shape3D.getTransforms().add(new Translate(0, -nodeRadius / 2d, 0));
                            break;
                        case 5:
                            shape3D.getTransforms().add(new Translate(0, 0, nodeRadius / 2d));
                            break;
                        case 6:
                            shape3D.getTransforms().add(new Translate(0, 0, -nodeRadius / 2d));
                            break;
                            default:
                                break;
                    }
                    if (simillarPoints > 1) {
                        pointsAtCoordinates.put(coordinates, simillarPoints - 1);
                    }
                    shape3D.getProperties().put(POINT_CLUSTER_NUMBER_PROPERTY, clusterNumber);
                    shape3D.getProperties().put(POINT_CLUSTER_GEN_PROPERTY, genes.get(((int) (dataX.get(pointNumber)/MULTIPLIER))-1));
                    shape3D.getProperties().put(POINT_CLUSTER_SAMPLE_PROPERTY, samples.get(((int) (dataY.get(pointNumber)/MULTIPLIER))-1));
                    shape3D.getProperties().put(POINT_CLUSTER_TIME_PROPERTY, times.get(((int) (dataZ.get(pointNumber)/MULTIPLIER))-1));
                }
                pointNumber++;
            }
        }

        //points center
        Point3D groupCenter = getNodeCenter(plot.scatterDataGroup);
        cameraTransform.setTranslate(groupCenter.getX(), groupCenter.getY(), groupCenter.getZ());

        /*final Group group = new Group();
        group.getChildren().addAll(world, crosshair3D, plot);*/
        final Group grid = createGrid(genesCount*MULTIPLIER+MULTIPLIER,
                samples.size()*MULTIPLIER+MULTIPLIER, times.size()*MULTIPLIER+MULTIPLIER,MULTIPLIER);

        final Group axes = createAxis(2, genesCount*MULTIPLIER+MULTIPLIER,
                samples.size()*MULTIPLIER+MULTIPLIER, times.size()*MULTIPLIER+MULTIPLIER);

        ((Group) graphSubScene.getRoot()).getChildren().addAll(grid, axes, plot);
        graphSubScene.requestFocus();
    }

    private static Point3D getNodeCenter(Node node) {
        //points center
        final Bounds boundsInParent = node.getBoundsInParent();
        double x = (boundsInParent.getMaxX() + boundsInParent.getMinX())/2;
        double y = (boundsInParent.getMaxY() + boundsInParent.getMinY())/2;
        double z = (boundsInParent.getMaxZ() + boundsInParent.getMinZ())/2;
        return new Point3D(x, y, z);
    }

    private static List<Color> generateColors(int count) {
        List<Color> colors = new ArrayList<>();
        float part = 350f / count;
        for (int i = 0; i < count; i++) {
            final float hue = i * part;
            final float saturation = 0.9f;//1.0 for brilliant, 0.0 for dull
            final float luminance = 1f; //1.0 for brighter, 0.0 for black
            colors.add(Color.hsb(hue, saturation, luminance));
        }
        return colors;
    }

    private static PolygonMesh createQuadrilateralMesh(float width, float height, int subDivX, int subDivY) {
        final float minX = -width / 2f;
        final float minY = -height / 2f;
        final float maxX = width / 2f;
        final float maxY = height / 2f;

        final int pointSize = 3;
        final int texCoordSize = 2;
        // 4 point indices and 4 texCoord indices per face
        final int faceSize = 8;
        int numDivX = subDivX + 1;
        int numVerts = (subDivY + 1) * numDivX;
        float[] points = new float[numVerts * pointSize];
        float[] texCoords = new float[numVerts * texCoordSize];
        int faceCount = subDivX * subDivY;
        int[][] faces = new int[faceCount][faceSize];

        // Create points and texCoords
        for (int y = 0; y <= subDivY; y++) {
            float dy = (float) y / subDivY;
            double fy = (1 - dy) * minY + dy * maxY;

            for (int x = 0; x <= subDivX; x++) {
                float dx = (float) x / subDivX;
                double fx = (1 - dx) * minX + dx * maxX;

                int index = y * numDivX * pointSize + (x * pointSize);
                points[index] = (float) fx;
                points[index + 1] = (float) fy;
                points[index + 2] = 0.0f;

                index = y * numDivX * texCoordSize + (x * texCoordSize);
                texCoords[index] = dx;
                texCoords[index + 1] = dy;
            }
        }

        // Create faces
        int index = 0;
        for (int y = 0; y < subDivY; y++) {
            for (int x = 0; x < subDivX; x++) {
                int p00 = y * numDivX + x;
                int p01 = p00 + 1;
                int p10 = p00 + numDivX;
                int p11 = p10 + 1;
                int tc00 = y * numDivX + x;
                int tc01 = tc00 + 1;
                int tc10 = tc00 + numDivX;
                int tc11 = tc10 + 1;

                faces[index][0] = p00;
                faces[index][1] = tc00;
                faces[index][2] = p10;
                faces[index][3] = tc10;
                faces[index][4] = p11;
                faces[index][5] = tc11;
                faces[index][6] = p01;
                faces[index++][7] = tc01;
            }
        }

        int[] smooth = new int[faceCount];

        PolygonMesh mesh = new PolygonMesh(points, texCoords, faces);
        mesh.getFaceSmoothingGroups().addAll(smooth);
        return mesh;
    }
    @SuppressWarnings("SuspiciousNameCombination")
    private static Group createGrid(float sizeX, float sizeY, float sizeZ, float delta) {
        if (delta < 1) {
            delta = 1;
        }
        final PolygonMesh plane = createQuadrilateralMesh(sizeX, sizeY, (int) (sizeX / delta), (int) (sizeY / delta));

        final PolygonMesh plane2 = createQuadrilateralMesh(sizeX, sizeZ, (int) (sizeX / delta), (int) (sizeZ / delta));

        final PolygonMesh plane3 = createQuadrilateralMesh(sizeY, sizeZ, (int) (sizeY / delta), (int) (sizeZ / delta));

        PolygonMeshView meshViewXY = new PolygonMeshView(plane);
        meshViewXY.setDrawMode(DrawMode.LINE);
        meshViewXY.setCullFace(CullFace.NONE);
        meshViewXY.getTransforms().add(new Translate(sizeX/2, sizeY/2, 0));

        PolygonMeshView meshViewXZ = new PolygonMeshView(plane2);
        meshViewXZ.setDrawMode(DrawMode.LINE);
        meshViewXZ.setCullFace(CullFace.NONE);
        meshViewXZ.getTransforms().add(new Rotate(90, Rotate.X_AXIS));
        meshViewXZ.setCullFace(CullFace.NONE);
        meshViewXZ.getTransforms().add(new Translate(sizeX/2, sizeY/2, 0));

        PolygonMeshView meshViewYZ = new PolygonMeshView(plane3);
        meshViewYZ.setDrawMode(DrawMode.LINE);
        meshViewYZ.setCullFace(CullFace.NONE);
        meshViewYZ.getTransforms().add(new Translate(0, sizeY/2, sizeZ/2));
        meshViewYZ.getTransforms().add(new Rotate(90, Rotate.Y_AXIS));
        meshViewYZ.setCullFace(CullFace.NONE);

        return new Group(meshViewXY,meshViewXZ, meshViewYZ);
    }
    @SuppressWarnings("SuspiciousNameCombination")
    private static Group createAxis(double radius, double xAxisLength, double yAxisLength, double zAxisLength) {
        Cylinder axisX = new Cylinder(radius, xAxisLength);
        axisX.getTransforms().addAll(new Rotate(90, Rotate.Z_AXIS), new Translate(0, -xAxisLength/2, 0));
        axisX.setMaterial(new PhongMaterial(Color.RED));

        Label xAxisLabel = new Label("Gen");
        xAxisLabel.getTransforms().addAll(new Rotate(180, Rotate.X_AXIS), new Translate(xAxisLength, 0, 0));
        xAxisLabel.setFont(new Font(MULTIPLIER));
        xAxisLabel.setTextFill(Color.RED);

        Cylinder axisY = new Cylinder(radius, yAxisLength);
        axisY.getTransforms().add(new Translate(0, yAxisLength/2, 0));
        axisY.setMaterial(new PhongMaterial(Color.GREEN));

        Label yAxisLabel = new Label("Sample");
        yAxisLabel.getTransforms().addAll(new Rotate(180, Rotate.X_AXIS), new Translate(0, -yAxisLength, 0));
        yAxisLabel.setFont(new Font(MULTIPLIER));
        yAxisLabel.setTextFill(Color.GREEN);

        Cylinder axisZ = new Cylinder(radius, zAxisLength);
        axisZ.setMaterial(new PhongMaterial(Color.BLUE));
        axisZ.getTransforms().addAll(new Rotate(90, Rotate.X_AXIS), new Translate(0, zAxisLength/2, 0));

        Label zAxisLabel = new Label("Time");
        zAxisLabel.getTransforms().addAll(new Rotate(180, Rotate.X_AXIS), new Translate(0, 0, -zAxisLength));
        zAxisLabel.setFont(new Font(MULTIPLIER));
        zAxisLabel.setTextFill(Color.BLUE);

        return new Group(axisX, xAxisLabel, axisY, yAxisLabel, axisZ, zAxisLabel);
    }

    private void filterClusters(List<List<List<Integer>>> clusters) {
        for (List<List<Integer>> points : clusters) {
            Iterator<List<Integer>> pointsIterator = points.iterator();
            while (pointsIterator.hasNext()) {
                List<Integer> point = pointsIterator.next();
                int geneNumber = point.get(0);
                int geneNumberNew = geneNumber - displayGeneStart;
                if (geneNumberNew < 1 || geneNumberNew > displayGeneSize.intValue()) {
                    pointsIterator.remove();
                }else{
                    point.set(0, geneNumberNew);
                }
            }
        }
    }

    private static int mapRange(int s, int a1, int a2, int b1, int b2) {
        return b1 + (s-a1)*(b2-b1)/(a2-a1);
    }

}