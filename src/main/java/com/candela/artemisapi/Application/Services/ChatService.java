package com.candela.artemisapi.Application.Services;

import org.springframework.stereotype.Service;
import com.candela.artemisapi.Presention.DTOs.ChatRequest;
import com.candela.artemisapi.conversation.ConversationState;
import com.candela.artemisapi.conversation.ConversationStateService;

@Service
public class ChatService {

    private final AgentService agentService;
    private final ConversationStateService stateService;

    public ChatService(AgentService agentService, ConversationStateService stateService) {
        this.agentService = agentService;
        this.stateService = stateService;
    }

    public String processChat(ChatRequest request) {
        // Recupera ou cria o estado da conversa
        ConversationState state = stateService.getState(request.conversationId());

        String userMessage = request.message();

        // Coordena a conversa e encaminha para o AgentService com estado associado
        String response = agentService.processAgentFlow(userMessage, state);
        
        // Salva/Atualiza o estado modificado pelas camadas abaixo
        stateService.saveState(state);

        return response;
    }
}
