package com.dota2analyzer.core.model.opendota;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BenchmarkEntry {

    @JsonProperty("percentile")
    private double percentile;

    @JsonProperty("value")
    private double value;

    public BenchmarkEntry() {}

    public double getPercentile() { return percentile; }
    public void setPercentile(double percentile) { this.percentile = percentile; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
}
