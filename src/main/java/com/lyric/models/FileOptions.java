package com.lyric.models;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Created by amymadden on 3/10/17.
 */
public class FileOptions {
    private int frequencyInDays;
    private int numberOfPeriods;
    private int numberOfRecordsPerPeriod;
    private String[] schemas;

    private String specifiedFileName;
    private String specifiedFileContentType;

    public FileOptions(JsonObject other){
        this(other.getInteger("frequencyInDays", 0), other.getInteger("numberOfPeriods", 0),
                other.getInteger("numberOfRecordsPerPeriod", 0), toStringArray(other.getJsonArray("schemas")));
    }

    public FileOptions(int frequencyInDays, int numberOfPeriods, int numberOfRecordsPerPeriod, String[] schemas) {
        this.frequencyInDays = frequencyInDays;
        this.numberOfPeriods = numberOfPeriods;
        this.numberOfRecordsPerPeriod = numberOfRecordsPerPeriod;
        this.schemas = schemas;
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

    public String[] getSchemas() {
        return schemas;
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

    public static String[] toStringArray(JsonArray array) {
        if(array==null)
            return null;

        String[] arr=new String[array.size()];
        for(int i=0; i<arr.length; i++) {
            arr[i]=array.getString(i);
        }
        return arr;
    }
}
