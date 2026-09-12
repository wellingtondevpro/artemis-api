package com.candela.artemisapi.process;

import org.springframework.stereotype.Component;

/**
 * Responsável por gerenciar as etapas de WORKFLOWS complexos
 * quando uma Capability exigir coleta e validação multi-step.
 */
@Component
public class ProcessExecutor {
    
    public String executeWorkflow(String capabilityId) {
        // Futura engine de workflow com múltiplas etapas
        return "Fluxo complexo iniciado para a capability: " + capabilityId;
    }
}
