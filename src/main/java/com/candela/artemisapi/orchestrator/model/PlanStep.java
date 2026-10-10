package com.candela.artemisapi.orchestrator.model;

import java.util.Map;

public class PlanStep {
    private String domain; // Ex: RH, FROTA, ACESSO
    private String action; // Ex: CADASTRAR_FUNCIONARIO, CONSULTAR
    private Map<String, Object> parameters; 
    private String resolvedEntityId; // ID único após a resolução de entidade
    private String status; // PENDING, EXECUTED, FAILED
    private java.util.List<String> missingParameters;
    private ToolExecutionProposal toolProposal;

    public PlanStep() {
        this.status = "PENDING";
    }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    public String getResolvedEntityId() { return resolvedEntityId; }
    public void setResolvedEntityId(String resolvedEntityId) { this.resolvedEntityId = resolvedEntityId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public java.util.List<String> getMissingParameters() { return missingParameters; }
    public void setMissingParameters(java.util.List<String> missingParameters) { this.missingParameters = missingParameters; }
    public ToolExecutionProposal getToolProposal() { return toolProposal; }
    public void setToolProposal(ToolExecutionProposal toolProposal) { this.toolProposal = toolProposal; }
}
