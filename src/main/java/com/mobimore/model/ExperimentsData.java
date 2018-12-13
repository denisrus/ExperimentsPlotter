package com.mobimore.model;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ExperimentsData {

    @SerializedName("experiments")
    @Expose
    private List<Experiment> experiments = null;

    @SerializedName("gen")
    @Expose
    private List<String> gen = null;

    @SerializedName("samples")
    @Expose
    private List<String> samples = null;

    @SerializedName("time")
    @Expose
    private List<String> time = null;

    public List<Experiment> getExperiments() {
        return experiments;
    }

    public void setExperiments(List<Experiment> experiments) {
        this.experiments = experiments;
    }

    public List<String> getGen() {
        return new ArrayList<>(gen);
    }

    public void setGen(List<String> gen) {
        this.gen = gen;
    }

    public List<String> getSamples() {
        return new ArrayList<>(samples);
    }

    public void setSamples(List<String> samples) {
        this.samples = samples;
    }

    public List<String> getTime() {
        return new ArrayList<>(time);
    }

    public void setTime(List<String> time) {
        this.time = time;
    }

}