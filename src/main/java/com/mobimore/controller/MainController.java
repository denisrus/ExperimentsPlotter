package com.mobimore.controller;

import com.google.gson.Gson;
import com.mobimore.model.Experiment;
import com.mobimore.model.ExperimentsData;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import jdk.nashorn.api.scripting.URLReader;
import org.hildan.fxgson.FxGson;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class MainController {

    @FXML
    public Label loadDatalabel;

    @FXML
    public TabPane tabPane;

    private Gson gsonFX = FxGson.create();

    @FXML
    private void initialize(){
        openFile(null);
    }

    private ExperimentsData data;

    @FXML
    private void openFile(ActionEvent actionEvent) {
        URL url = getClass().getResource("/t/test.json");
        data = gsonFX.fromJson(new URLReader(url), ExperimentsData.class);
        loadDatalabel.setVisible(false);
        createExperimentsTabs();
    }

    private void createExperimentsTabs(){
        List<Experiment> experiments = data.getExperiments();
        for (int i = 0, experimentsSize = experiments.size(); i < experimentsSize; i++) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/charts.fxml"));
                Parent root = loader.load();
                ChartsController controller = loader.getController();
                controller.buildPlot(data, i);
                Tab tab = new Tab(experiments.get(i).getName(), root);
                tabPane.getTabs().add(tab);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
