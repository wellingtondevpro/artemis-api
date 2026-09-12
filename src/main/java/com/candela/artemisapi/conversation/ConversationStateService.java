package com.candela.artemisapi.conversation;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationStateService {
    
    // Armazenamento em memória para o MVP.
    // Futuramente pode ser trocado por Redis ou Banco de Dados.
    private final Map<String, ConversationState> states = new ConcurrentHashMap<>();

    public ConversationState getState(String conversationId) {
        return states.computeIfAbsent(conversationId, id -> {
            ConversationState state = new ConversationState();
            state.setConversationId(id);
            state.setStatus("IDLE");
            return state;
        });
    }

    public void saveState(ConversationState state) {
        states.put(state.getConversationId(), state);
    }
}
