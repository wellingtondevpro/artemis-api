package com.candela.artemisapi.orchestrator.model;

import java.util.List;
import java.util.UUID;

public class Plan {
    private String planId = UUID.randomUUID().toString();
    private String summary;
    private List<PlanStep> steps;
    private String provider;
    private String conversationId;

    public Plan() {}

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public List<PlanStep> getSteps() { return steps; }
    public void setSteps(List<PlanStep> steps) { this.steps = steps; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
}
