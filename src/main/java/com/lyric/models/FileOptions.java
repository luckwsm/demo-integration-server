package com.lyric.models;

import io.vertx.core.json.JsonObject;

/**
 * Created by amymadden on 3/10/17.
 */
public class FileOptions {
    private String vendorType;
    private int frequencyInDays;
    private int numberOfPeriods;
    private int numberOfRecordsPerPeriod;

    private String specifiedFileName;
    private String specifiedFileContentType;

    public FileOptions(JsonObject other){
        this(other.getString("vendorType"), other.getInteger("frequencyInDays", 0), other.getInteger("numberOfPeriods", 0),
                other.getInteger("numberOfRecordsPerPeriod", 0));
    }

    public FileOptions(String vendorType, int frequencyInDays, int numberOfPeriods, int numberOfRecordsPerPeriod) {
        this.vendorType = vendorType;
        this.frequencyInDays = frequencyInDays;
        this.numberOfPeriods = numberOfPeriods;
        this.numberOfRecordsPerPeriod = numberOfRecordsPerPeriod;
    }


    public String getVendorType() {
        return vendorType;
    }

    public int getFrequencyInDays() {
        return frequencyInDays;
    }

    public int getNumberOfPeriods() {
        return numberOfPeriods;
    }

    public int getNumberOfRecordsPerPeriod() {
        return numberOfRecordsPerPeriod;
    }

    public String getSpecifiedFileName() {
        return specifiedFileName;
    }

    public void setSpecifiedFileName(String specifiedFileName) {
        this.specifiedFileName = specifiedFileName;
    }

    public String getSpecifiedFileContentType() {
        return specifiedFileContentType;
    }

    public void setSpecifiedFileContentType(String specifiedFileContentType) {
        this.specifiedFileContentType = specifiedFileContentType;
    }
}
