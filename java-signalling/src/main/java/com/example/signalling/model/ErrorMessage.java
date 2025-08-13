package com.example.signalling.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorMessage extends SignallingMessage {
    private final String error;

    @JsonCreator
    public ErrorMessage(@JsonProperty("error") String error) {
        this.error = error;
    }

    @Override
    public String getType() {
        return "error";
    }

    public String getError() {
        return error;
    }
}