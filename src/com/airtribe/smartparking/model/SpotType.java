package com.airtribe.smartparking.model;

// Physical size of a parking spot
public enum SpotType {
    SMALL('S'),
    MEDIUM('M'),
    LARGE('L');

    private final char code; // used later to build readable spot ids like F1-M002

    SpotType(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }
}