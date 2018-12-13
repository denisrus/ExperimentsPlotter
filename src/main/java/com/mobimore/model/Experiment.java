package com.mobimore.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import javafx.beans.property.StringProperty;

import java.util.ArrayList;
import java.util.List;

public class Experiment {

    @SerializedName("name")
    @Expose
    private StringProperty name = null;

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
        List<List<List<Integer>>> clustersCopy = new ArrayList<>();
        for (List<List<Integer>> points : clusters) {
            List<List<Integer>> pointsCopy = new ArrayList<>();
            for (List<Integer> point : points) {
                pointsCopy.add(new ArrayList<>(point));
            }
            clustersCopy.add(new ArrayList<>(pointsCopy));
        }
        return clustersCopy;
    }

    public void setClusters(List<List<List<Integer>>> clusters) {
        this.clusters = clusters;
    }


}