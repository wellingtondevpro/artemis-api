package com.candela.artemisapi.Application.Services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.candela.artemisapi.agent.IntentResolver;
import com.candela.artemisapi.agent.Intent;
import com.candela.artemisapi.agent.CapabilityDiscovery;
import com.candela.artemisapi.agent.ToolSelector;
import com.candela.artemisapi.agent.Capability;
import com.candela.artemisapi.business.BusinessPolicy;
import com.candela.artemisapi.business.BusinessPolicy.PolicyResult;
import com.candela.artemisapi.conversation.ConversationState;
import com.candela.artemisapi.process.ProcessExecutor;
import com.candela.artemisapi.mcp.MCPClient;

import java.util.List;
import java.util.Optional;

@Service
public class AgentService {

    private static final Logger logger = LoggerFactory.getLogger(AgentService.class);
    private final IntentResolver intentResolver;
    private final CapabilityDiscovery discovery;
    private final ToolSelector toolSelector;
    private final BusinessPolicy businessPolicy;
    private final ProcessExecutor processExecutor;
    private final MCPClient mcpClient;

    public AgentService(IntentResolver intentResolver, 
                        CapabilityDiscovery discovery, 
                        ToolSelector toolSelector,
                        BusinessPolicy businessPolicy,
                        ProcessExecutor processExecutor,
                        MCPClient mcpClient) {
        this.intentResolver = intentResolver;
        this.discovery = discovery;
        this.toolSelector = toolSelector;
        this.businessPolicy = businessPolicy;
        this.processExecutor = processExecutor;
        this.mcpClient = mcpClient;
    }

    public String processAgentFlow(String userMessage, ConversationState state) {
        logger.info("AgentService processando mensagem. ConversationID: {}", state.getConversationId());
        
        // 1. Obter a intenção
        Intent intent = intentResolver.resolveIntent(userMessage);
        state.setIntent(intent);
        logger.info("Intenção identificada: domain={}, operation={}, target={}, parameters={}", 
                intent.domain(), intent.operation(), intent.target(), intent.parameters());

        // 2. Capability Discovery
        List<Capability> capabilities = discovery.discoverAll();
        
        // 3. Tool Selection
        Optional<Capability> selectedOpt = toolSelector.selectBestCapability(intent, capabilities);

        if (selectedOpt.isPresent()) {
            Capability cap = selectedOpt.get();
            state.setCurrentCapability(cap.id());
            logger.info("Capability selecionada: {} ({})", cap.id(), cap.type());
            
            // 4. Business Rules / Policies
            PolicyResult policy = businessPolicy.evaluate(cap, state);
            if (!policy.allowed()) {
                state.setStatus("BLOCKED_BY_POLICY");
                return "Ação não permitida: " + policy.reason();
            }

            // 5. Decidir execução com base no tipo
            if ("WORKFLOW".equalsIgnoreCase(cap.type())) {
                state.setStatus("EXECUTING_WORKFLOW");
                return processExecutor.executeWorkflow(cap.id());
            } else if ("COMMAND".equalsIgnoreCase(cap.type())) {
                state.setStatus("EXECUTING_COMMAND");
                // Chamar MCPClient simulado para execução do COMMAND
                Object result = mcpClient.callTool(cap.id(), intent.parameters());
                return String.format("Comando executado via MCP [%s].\nResultado:\n%s", cap.id(), result);
            } else {
                state.setStatus("EXECUTING_QUERY");
                // Chamar MCPClient simulado para execução da QUERY
                Object result = mcpClient.callTool(cap.id(), intent.parameters());
                return String.format("Consulta executada via MCP [%s].\nResultado:\n%s", cap.id(), result);
            }
            
        } else {
            state.setStatus("CAPABILITY_NOT_FOUND");
            logger.warn("Nenhuma capability encontrada para a intenção.");
            return String.format("Identifiquei o que você deseja (Domínio: %s, Operação: %s, Alvo: %s), mas ainda não há capacidade mapeada ou MCP tool disponível para realizá-la.", 
                    intent.domain(), intent.operation(), intent.target());
        }
    }
}
