package com.mobimore.controller;

import com.google.gson.Gson;
import com.mobimore.model.Experiment;
import com.mobimore.model.ExperimentsData;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import jdk.nashorn.api.scripting.URLReader;
import org.hildan.fxgson.FxGson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class MainController {

    @FXML
    public Label loadDatalabel;

    @FXML
    public TabPane tabPane;

    @FXML
    public ScrollBar scrollBar;

    @FXML
    public ChoiceBox<Integer> sizeSelect;

    private Gson gsonFX = FxGson.create();

    private ExperimentsData data;

    private final Integer[] SIZES = {10, 25, 50, 100, 200};
    private ObservableList<Integer> sizesList= FXCollections.observableArrayList();

    @FXML
    private void initialize(){
        sizesList.addAll(SIZES);
        sizeSelect.setItems(sizesList);
        sizeSelect.getSelectionModel().select(2);
        //openSample();
    }

    @FXML
    private void openFile(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Experiments File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JSON Experiment Files", "*.json"));
        File selectedFile = fileChooser.showOpenDialog(tabPane.getScene().getWindow());
        if (selectedFile != null) {
            try {
                openJSONFile(selectedFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void openJSONFile(File file) throws FileNotFoundException {
        data = gsonFX.fromJson(new FileReader(file), ExperimentsData.class);
        loadDatalabel.setVisible(false);
        createExperimentsTabs();
    }

    private void openSample(){
        URL url = getClass().getResource("/t/test.json");
        data = gsonFX.fromJson(new URLReader(url), ExperimentsData.class);
        loadDatalabel.setVisible(false);
        createExperimentsTabs();
    }

    private void createExperimentsTabs(){
        if (data.getGen().size() < 10) {
            scrollBar.setDisable(true);
        }else{
            scrollBar.setDisable(false);
        }
        tabPane.getTabs().clear();
        List<Experiment> experiments = data.getExperiments();
        for (int i = 0, experimentsSize = experiments.size(); i < experimentsSize; i++) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/charts.fxml"));
                Parent root = loader.load();
                ChartsController controller = loader.getController();
                controller.setData(data);
                controller.setExperimentNumber(i);
                controller.displayGeneStartPercentProperty().bind(scrollBar.valueProperty());
                controller.displayGeneSizeProperty().bind(sizeSelect.getSelectionModel().selectedItemProperty());
                controller.displayPlot();
                Tab tab = new Tab(experiments.get(i).getName(), root);
                tabPane.getTabs().add(tab);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
