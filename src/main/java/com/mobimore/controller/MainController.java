package com.mobimore.controller;

import com.google.gson.Gson;
import com.mobimore.model.Experiment;
import com.mobimore.model.ExperimentsData;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.*;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Shape3D;
import jdk.nashorn.api.scripting.URLReader;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.scene.Crosshair3D;
import org.fxyz3d.shapes.composites.ScatterPlot;
import org.fxyz3d.utils.CameraTransformer;
import org.hildan.fxgson.FxGson;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainController {

    @FXML
    public SubScene graphSubScene;

    @FXML
    public StackPane stackPaneWithGraph;

    @FXML
    public ListView<String> experimentNamesList;

    private PerspectiveCamera camera;
    private final CameraTransformer cameraTransform = new CameraTransformer();

    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;

    private ScatterPlot plot;
    private Gson gsonFX = FxGson.create();

    private List<Color> clusterColorsList = new ArrayList<>();

    private static final int MULTIPLIER = 100;
    private static final String CLUSTER_NUMBER_PROPERTY = "clusterNumber";
    private static final String CLUSTER_GEN_PROPERTY = "clusterGen";
    private static final String CLUSTER_SAMPLE_PROPERTY = "clusterSample";
    private static final String CLUSTER_TIME_PROPERTY = "clusterTime";

    private ObservableList<String> experimentNames = FXCollections.observableArrayList();

    @FXML
    private void initialize(){
        setupCameraAndLight();

        experimentNamesList.setItems(experimentNames);

        graphSubScene.widthProperty().bind(stackPaneWithGraph.widthProperty());
        graphSubScene.heightProperty().bind(stackPaneWithGraph.heightProperty());

        graphSubScene.setOnMousePressed((MouseEvent me) -> {
            graphSubScene.requestFocus();
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();

        });
        graphSubScene.setOnMouseClicked((MouseEvent me)->{

            if(me.getButton().equals(MouseButton.PRIMARY)){
                PickResult pickResult = me.getPickResult();

                if (pickResult != null && pickResult.getIntersectedNode() != null && pickResult.getIntersectedNode() instanceof Shape3D) { //shape selected
                    Shape3D pickedShape = (Shape3D) pickResult.getIntersectedNode();
                    Integer selectedClusterNumber = (Integer) pickedShape.getProperties().get(CLUSTER_NUMBER_PROPERTY);

                    if (selectedClusterNumber != null) {
                        resetClusterColors();

                        plot.scatterDataGroup.getChildren().stream().map(e -> (Shape3D) e)
                                .filter(shape3D -> !(shape3D.getProperties().get(CLUSTER_NUMBER_PROPERTY).equals(selectedClusterNumber)))
                                .forEach(shape3D -> {
                                    Color color = getOpaqueColor();
                                    ((PhongMaterial) shape3D.getMaterial()).setDiffuseColor(color);
                                });
                    }
                }

                if(me.getClickCount() == 2){
                    resetClusterColors();
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
                camera.setTranslateX(camera.getTranslateX() + mouseDeltaX * modifierFactor * modifier * 0.3);
                camera.setTranslateY(camera.getTranslateY() + mouseDeltaY * modifierFactor * modifier * 0.3);
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
            System.out.println(newZ);
        });

        openFile(null);
    }

    private void resetClusterColors(){
        plot.scatterDataGroup.getChildren().stream().map(e -> (Shape3D) e)
                .forEach(shape3D -> {
                    Integer clusterNumber = (Integer) shape3D.getProperties().get(CLUSTER_NUMBER_PROPERTY);
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
        camera.setTranslateZ(-3000);
        cameraTransform.ry.setAngle(-45.0);
        cameraTransform.rx.setAngle(180.0);
        //add a Point Light for better viewing of the grid coordinate system
        PointLight light = new PointLight(Color.WHITE);
        light.setDepthTest(DepthTest.DISABLE);
        cameraTransform.getChildren().add(light);
        cameraTransform.getChildren().add(new AmbientLight(Color.WHITE));
        light.setTranslateX(camera.getTranslateX());
        light.setTranslateY(camera.getTranslateY());
        light.setTranslateZ(camera.getTranslateZ());
        graphSubScene.setCamera(camera);
    }

    @FXML
    public void openFile(ActionEvent actionEvent) {
        URL url = getClass().getResource("/t/test.json");
        ExperimentsData data = gsonFX.fromJson(new URLReader(url), ExperimentsData.class);
        experimentNames.setAll(data.getExperiments().stream().map(Experiment::getName).collect(Collectors.toList()));
        ((Group) graphSubScene.getRoot()).getChildren().addAll(buildPlot(data, 0));
        graphSubScene.requestFocus();
    }

    private Group buildPlot(ExperimentsData data, int experimentNumber) {
        final int genesCount = data.getGen().size();

        /*CubeWorld world = new CubeWorld(genesCount*2*MULTIPLIER, 3*MULTIPLIER, false);
        world.setPickOnBounds(false);
        world.setDepthTest(DepthTest.DISABLE);*/

        Crosshair3D crosshair3D = new Crosshair3D(new Point3D(0, 0, 0), genesCount*2*MULTIPLIER, genesCount*2);
        final List<Double> dataX = new ArrayList<>();
        final List<Double> dataY = new ArrayList<>();
        final List<Double> dataZ = new ArrayList<>();
        final List<Color> colors = new ArrayList<>();

        final Experiment experiment = data.getExperiments().get(experimentNumber);
        final List<List<List<Integer>>> clusters = experiment.getClusters();

        clusterColorsList = generateColors(clusters.size());
        for (int i = 0, clustersSize = clusters.size(); i < clustersSize; i++) {
            List<List<Integer>> cluster = clusters.get(i);
            Color color = clusterColorsList.get(i);
            for (List<Integer> point : cluster) {
                int x = point.get(0);
                int y = point.get(1);
                int z = point.get(2);
                dataX.add((double) x * MULTIPLIER);
                dataY.add((double) y * MULTIPLIER);
                dataZ.add((double) z * MULTIPLIER);
                colors.add(color);
            }
        }

        plot = new ScatterPlot(10*MULTIPLIER, MULTIPLIER/5d, true);
        plot.setXYZData(dataX, dataY, dataZ, colors);

        final List<String> genes = data.getGen();
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
                    shape3D.getProperties().put(CLUSTER_NUMBER_PROPERTY, clusterNumber);
                    shape3D.getProperties().put(CLUSTER_GEN_PROPERTY, genes.get(((int) (dataX.get(pointNumber)/MULTIPLIER))-1));
                    shape3D.getProperties().put(CLUSTER_SAMPLE_PROPERTY, samples.get(((int) (dataY.get(pointNumber)/MULTIPLIER))-1));
                    shape3D.getProperties().put(CLUSTER_TIME_PROPERTY, times.get(((int) (dataZ.get(pointNumber)/MULTIPLIER))-1));
                }
                pointNumber++;
            }
        }

        final Bounds boundsInParent = plot.scatterDataGroup.getBoundsInParent();
        double x = (boundsInParent.getMaxX() + boundsInParent.getMinX())/2;
        double y = (boundsInParent.getMaxY() + boundsInParent.getMinY())/2;
        double z = (boundsInParent.getMaxZ() + boundsInParent.getMinZ())/2;
        cameraTransform.setTranslate(x,y,z);

        Group group = new Group();
        group.getChildren().addAll(/*world,*/ crosshair3D, plot);
        return group;
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
    private static Color getOpaqueColor() {
        return new Color(0, 0, 0, 0);
    }
}
