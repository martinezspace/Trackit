package com.trackit.investmentservice.model;

public enum ImportStatus {
    PENDING,    //batch created, processing not started
    PROCESSING, //currently parsing and importing rows
    COMPLETED,  //all rows processed, can have errors
    FAILED,     //import failed entirely e.g. unreadable file
    CANCELLED   //user cancelled the import
}
