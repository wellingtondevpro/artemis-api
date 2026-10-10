package com.candela.artemisapi.Presention.Controllers;

import com.candela.artemisapi.Application.Services.ChatService;
import com.candela.artemisapi.Presention.DTOs.ChatRequest;
import com.candela.artemisapi.orchestrator.model.AgentResponse;
import com.candela.artemisapi.orchestrator.model.Plan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private final ChatService chatService;
    private final com.candela.artemisapi.Application.Services.ReActAgentService reActAgentService;

    public ChatController(ChatService chatService, com.candela.artemisapi.Application.Services.ReActAgentService reActAgentService) {
        this.chatService = chatService;
        this.reActAgentService = reActAgentService;
    }

    /**
     * Passo 1: Recebe a mensagem livre do usuário e gera o Plano de Execução (Plan-and-Execute)
     */
    @PostMapping
    public AgentResponse chat(@RequestBody ChatRequest request) {
        logger.info("=== Nova requisição de Chat recebida do Front-end ===");
        logger.info("ConversationID: {}, useReAct: {}", request.conversationId(), request.useReAct());
        
        if (Boolean.TRUE.equals(request.useReAct())) {
            return reActAgentService.processChatReAct(request);
        }
        
        return chatService.processChat(request);
    }

    /**
     * Passo 2: Recebe a confirmação do usuário (via Angular) contendo o Plano já revisado.
     * Executa as ações reais nos servidores MCP.
     */
    @PostMapping("/confirm")
    public AgentResponse confirmPlan(@RequestBody Plan confirmedPlan) {
        logger.info("=== Requisição de Confirmação de Plano recebida ===");
        logger.info("PlanID: {}", confirmedPlan.getPlanId());
        
        try {
            logger.info("Payload recebido: {}", new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(confirmedPlan));
        } catch (Exception e) {}

        return chatService.executeConfirmedPlan(confirmedPlan);
    }

    /**
     * Passo 3: Retorna os provedores de IA disponíveis para o Front-end
     */
    @GetMapping("/models")
    public List<Map<String, String>> getAvailableModels() {
        return List.of(
            Map.of("id", "local", "name", "Modo Privado (Ollama Local)"),
            Map.of("id", "cloud", "name", "Modo Avançado (DeepSeek Cloud)")
        );
    }
}
