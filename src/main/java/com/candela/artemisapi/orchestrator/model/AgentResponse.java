package com.candela.artemisapi.orchestrator.model;

import java.util.List;

public class AgentResponse {
    
    public enum ResponseType {
        PLAN_PROPOSAL,
        EXECUTION_RESULT,
        CLARIFICATION_REQUIRED,
        ERROR
    }

    private ResponseType type;
    private String message;
    private Plan plan; // Preenchido se type for PLAN_PROPOSAL
    private Object executionData; // Preenchido se type for EXECUTION_RESULT

    // Construtores, Getters e Setters
    public AgentResponse() {}

    public AgentResponse(ResponseType type, String message, Plan plan) {
        this.type = type;
        this.message = message;
        this.plan = plan;
    }

    public ResponseType getType() { return type; }
    public void setType(ResponseType type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Plan getPlan() { return plan; }
    public void setPlan(Plan plan) { this.plan = plan; }
    
    public Object getExecutionData() { return executionData; }
    public void setExecutionData(Object executionData) { this.executionData = executionData; }
}
