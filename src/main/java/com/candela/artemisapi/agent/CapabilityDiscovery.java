package com.candela.artemisapi.agent;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstração para descobrir capabilities disponíveis de diferentes fontes
 * (locais, MCP, banco de dados, etc).
 */
@Component
public class CapabilityDiscovery {
    
    private final List<Capability> capabilities = new ArrayList<>();

    public CapabilityDiscovery() {
        // Exemplo: Simulando descobertas locais e de MCP em memória
        capabilities.add(new Capability(
            "beneficio.criar_solicitacao",
            "Cria uma solicitação de benefício",
            "COMMAND",
            "BENEFICIO", "SOLICITAR", "AUXILIO_ALIMENTACAO"
        ));
        
        capabilities.add(new Capability(
            "iam.list_user_systems",
            "Lista os sistemas aos quais o usuário possui acesso",
            "QUERY",
            "ACESSO", "CONSULTAR", "SISTEMAS_USUARIO"
        ));
        
        capabilities.add(new Capability(
            "iam.change_password",
            "Altera a senha do usuário",
            "COMMAND",
            "ACESSO", "ALTERAR", "SENHA"
        ));

        capabilities.add(new Capability(
            "rh.consultar_lotacao",
            "Consulta a lotação atual do usuário",
            "QUERY",
            "RH", "CONSULTAR", "LOTACAO"
        ));
    }

    public List<Capability> discoverAll() {

        return new ArrayList<>(capabilities);

    }
}
