package com.candela.artemisapi.agent;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Responsável por selecionar a capability/tool correta com base na intenção.
 */
@Component
public class ToolSelector {
    
    public Optional<Capability> selectBestCapability(Intent intent, List<Capability> availableCapabilities) {
        if (intent.domain() == null || intent.operation() == null || intent.target() == null) {
            return Optional.empty();
        }

        // Lógica de matching simples. No futuro pode utilizar IA/Embeddings para matching semântico.
        return availableCapabilities.stream()
            .filter(c -> matches(c, intent))
            .findFirst();
    }
    
    private boolean matches(Capability c, Intent intent) {
        boolean matchDomain = c.domain().equalsIgnoreCase(intent.domain());
        boolean matchOp = c.operation().equalsIgnoreCase(intent.operation());
        boolean matchTarget = c.target().equalsIgnoreCase(intent.target());
        
        return matchDomain && matchOp && matchTarget;
    }
}
