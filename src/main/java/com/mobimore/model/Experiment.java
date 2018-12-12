package com.mobimore.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import javafx.beans.property.StringProperty;

import java.util.List;

public class Experiment {

    @SerializedName("name")
    @Expose
    private StringProperty name;

    @SerializedName("clusters")
    @Expose
    private List<List<List<Integer>>> clusters = null;

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public List<List<List<Integer>>> getClusters() {
        return clusters;
    }

    public void setClusters(List<List<List<Integer>>> clusters) {
        this.clusters = clusters;
    }

}