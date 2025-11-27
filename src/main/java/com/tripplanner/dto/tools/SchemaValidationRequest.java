package com.tripplanner.dto.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for schema validation tool
 */
public class SchemaValidationRequest {
    
    @JsonProperty("jsonOutput")
    private String jsonOutput;
    
    @JsonProperty("jsonSchema")
    private String jsonSchema;
    
    @JsonProperty("cleanBeforeValidation")
    private Boolean cleanBeforeValidation = true;
    
    public SchemaValidationRequest() {}
    
    public SchemaValidationRequest(String jsonOutput, String jsonSchema) {
        this.jsonOutput = jsonOutput;
        this.jsonSchema = jsonSchema;
    }
    
    // Getters and Setters
    public String getJsonOutput() {
        return jsonOutput;
    }
    
    public void setJsonOutput(String jsonOutput) {
        this.jsonOutput = jsonOutput;
    }
    
    public String getJsonSchema() {
        return jsonSchema;
    }
    
    public void setJsonSchema(String jsonSchema) {
        this.jsonSchema = jsonSchema;
    }
    
    public Boolean getCleanBeforeValidation() {
        return cleanBeforeValidation;
    }
    
    public void setCleanBeforeValidation(Boolean cleanBeforeValidation) {
        this.cleanBeforeValidation = cleanBeforeValidation;
    }
}
