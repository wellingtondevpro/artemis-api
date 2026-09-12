package com.candela.artemisapi.business;

import org.springframework.stereotype.Component;

import com.candela.artemisapi.agent.Capability;
import com.candela.artemisapi.conversation.ConversationState;

@Component
public class BusinessPolicy {

    public PolicyResult evaluate(Capability capability, ConversationState state) {
        // Validações independentes da IA: elegibilidade, permissão, condições específicas.
        // Como o sistema não toma decisões de negócio via IA, a aplicação fará isso aqui.
        
        if ("COMMAND".equalsIgnoreCase(capability.type()) || "WORKFLOW".equalsIgnoreCase(capability.type())) {
            // Requer validação extra de autorização/elegibilidade
            return new PolicyResult(true, "Permissão concedida para execução.");
        }
        
        return new PolicyResult(true, "Consulta permitida.");
    }
    
    public record PolicyResult(boolean allowed, String reason) {}
}
