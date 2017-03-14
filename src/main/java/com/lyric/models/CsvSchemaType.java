package com.lyric.models;

import io.vertx.core.json.JsonArray;

/**
 * Created by amymadden on 3/14/17.
 */
public enum CsvSchemaType {

    SonyatvSongSummary("songSummary", new JsonArray().add("lyric-csv-use-header" + "|" + "false").add("lyric-csv-date-format-string" + "|" + "yyyy-MM-dd HH:mm:ss")),
    SonyatvEarningsSummary("earningSummary", new JsonArray().add("lyric-csv-use-header" + "|" + "false").add("lyric-csv-date-format-string" + "|" + "yyyy-MM-dd HH:mm:ss")),
    SonyatvFinancialTransactions("financialTransactions", new JsonArray().add("lyric-csv-use-header" + "|" + "false").add("lyric-csv-date-format-string" + "|" + "yyyy-MM-dd HH:mm:ss")),
    SonyatvStatementSummary("statementSummary", new JsonArray().add("lyric-csv-use-header" + "|" + "false").add("lyric-csv-date-format-string" + "|" + "yyyy-MM-dd HH:mm:ss")),

    TunecoreDistributionSample("songSummary",  null);

    private final JsonArray additionalJweHeaders;
    private final String fileType;

    CsvSchemaType(String fileType, JsonArray additionalJweHeaders) {
        this.additionalJweHeaders = additionalJweHeaders;
        this.fileType = fileType;
    }

    public JsonArray getAdditionalJweHeaders(){
        return additionalJweHeaders;
    }

    public String getFileType() {
        return fileType;
    }
}
