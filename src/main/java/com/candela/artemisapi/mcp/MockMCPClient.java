package com.candela.artemisapi.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MockMCPClient implements MCPClient {

    private static final Logger logger = LoggerFactory.getLogger(MockMCPClient.class);

    @Override
    public Object callTool(String toolName, Map<String, Object> arguments) {
        logger.info("Executando Tool via (Mock) MCP: toolName={}, arguments={}", toolName, arguments);
        
        return switch (toolName) {
            case "iam.list_user_systems" -> """
                    Sistemas que você possui acesso:
                    - SIAFI (Perfil: Consulta)
                    - SIGEPE (Perfil: Servidor)
                    - SEI (Perfil: Básico)
                    """;
                    
            case "beneficio.criar_solicitacao" -> 
                    "Sua solicitação de benefício foi criada com sucesso! Protocolo: " + (int)(Math.random() * 100000);
                    
            case "iam.change_password" -> 
                    "Um link para redefinição de senha foi enviado para o seu e-mail corporativo aprovado.";
                    
            case "rh.consultar_lotacao" -> 
                    "Sua lotação atual é: Departamento de Tecnologia da Informação (DTI) - Sede Regional.";
                    
            default -> "Execução simulada da ferramenta " + toolName + " concluída (Sem dados mockados).";
        };
    }
}
