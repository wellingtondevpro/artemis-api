package com.candela.artemisapi.agent;

import org.springframework.stereotype.Component;
import com.candela.artemisapi.Application.Services.IAService;

@Component
public class IntentResolver {

    private final IAService iaService;

    public IntentResolver(IAService iaService) {
        this.iaService = iaService;
    }

    public Intent resolveIntent(String userMessage) {
        String systemPrompt = "Você é o analisador de intenções de um assistente de negócios corporativo. "
                + "Seu objetivo é classificar a intenção e extrair parâmetros de forma semântica e genérica. "
                + "A saída DEVE ser estritamente o JSON correspondente à estrutura solicitada, sem texto livre. "
                + "Identifique 'domain' (o contexto principal), 'operation' (a ação) e 'target' (o alvo específico da operação). "
                + "Use SEMPRE letras MAIÚSCULAS para domain, operation e target. "
                + "No objeto 'parameters', extraia informações adicionais passadas pelo usuário (ex: datas, valores). "
                + "Exemplo: Se o usuário quer saber a quais sistemas tem acesso, retorne domain='ACESSO', operation='CONSULTAR', target='SISTEMAS_USUARIO'.";

        return iaService.getStructuredOutput(systemPrompt, userMessage, Intent.class);
    }
}
