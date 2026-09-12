package com.candela.artemisapi.process;

/**
 * Abstração inicial para processos de múltiplas etapas.
 * Utilizado somente quando uma operação exigir um fluxo complexo.
 */
public interface ProcessDefinition {
    
    /**
     * Identificador único do processo.
     */
    String getProcessId();
    
    /**
     * Descrição do que o processo realiza.
     */
    String getDescription();
    
    // Futuras definições de etapas do fluxo serão inseridas aqui
}
