package com.signalling.message;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Layer preference message for streaming quality control.
 */
public class LayerPreferenceMessage extends BaseMessage {
    
    @JsonProperty("spatialLayer")
    private Integer spatialLayer;
    
    @JsonProperty("temporalLayer") 
    private Integer temporalLayer;
    
    public LayerPreferenceMessage() {
        super("layerPreference");
    }
    
    public Integer getSpatialLayer() {
        return spatialLayer;
    }
    
    public void setSpatialLayer(Integer spatialLayer) {
        this.spatialLayer = spatialLayer;
    }
    
    public Integer getTemporalLayer() {
        return temporalLayer;
    }
    
    public void setTemporalLayer(Integer temporalLayer) {
        this.temporalLayer = temporalLayer;
    }
}