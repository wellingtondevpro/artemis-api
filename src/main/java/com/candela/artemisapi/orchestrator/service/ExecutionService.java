package com.candela.artemisapi.orchestrator.service;

import com.candela.artemisapi.orchestrator.model.Plan;
import com.candela.artemisapi.orchestrator.model.PlanStep;
import com.candela.artemisapi.mcp.MCPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionService.class);
    private final MCPClient mcpClient;

    public ExecutionService(MCPClient mcpClient) {
        this.mcpClient = mcpClient;
    }

    /**
     * Executa as propostas do plano que foram confirmadas pelo usuário.
     */
    public Map<String, Object> executePlan(Plan plan) {
        logger.info("[START] ExecutionService: Iniciando execução real do Plano ID '{}'", plan.getPlanId());
        Map<String, Object> resultados = new HashMap<>();

        if (plan.getSteps() == null) {
            logger.warn("[END] ExecutionService: A lista de passos recebida do frontend é nula!");
            return resultados;
        }

        logger.info("[ExecutionService] Quantidade de passos no plano recebido: {}", plan.getSteps().size());

        for (PlanStep step : plan.getSteps()) {
            String currentStatus = step.getStatus() != null ? step.getStatus().toUpperCase() : "PENDING";
            
            logger.info("[ExecutionService] Avaliando passo - Action: '{}', Status Atual: '{}', Tem ToolProposal? {}", 
                step.getAction(), currentStatus, step.getToolProposal() != null);
                
            if (step.getToolProposal() != null && (currentStatus.equals("PENDING") || currentStatus.equals("PENDENTE"))) {
                String toolName = step.getToolProposal().getToolName();
                Map<String, Object> args = step.getToolProposal().getArguments();
                
                logger.info("[ExecutionService] >>> DISPARANDO MCP TOOL: '{}' com argumentos: {}", toolName, args);
                
                try {
                    // Execução determinística sem LLM
                    Object result = mcpClient.callTool(toolName, args);
                    step.setStatus("EXECUTED");
                    resultados.put(toolName, result);
                    logger.info("[ExecutionService] <<< SUCESSO MCP TOOL '{}'. Resultado: {}", toolName, result);
                    
                } catch (Exception e) {
                    logger.error("[ExecutionService] <<< ERRO AO EXECUTAR MCP TOOL '{}'", toolName, e);
                    step.setStatus("FAILED");
                    resultados.put(toolName, "Erro: " + e.getMessage());
                }
            } else {
                logger.warn("[ExecutionService] Passo Ignorado. Condições não atendidas para execução.");
            }
        }
        
        logger.info("[END] ExecutionService: Execução do plano '{}' concluída. Resultados obtidos: {}", plan.getPlanId(), resultados.size());
        return resultados;
    }
}
