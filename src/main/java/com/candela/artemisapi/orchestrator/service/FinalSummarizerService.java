package com.candela.artemisapi.orchestrator.service;

import com.candela.artemisapi.orchestrator.model.Plan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.candela.artemisapi.Application.Services.LlmRouterService;

import java.util.Map;

@Service
public class FinalSummarizerService {

    private static final Logger logger = LoggerFactory.getLogger(FinalSummarizerService.class);
    private final LlmRouterService llmRouterService;

    public FinalSummarizerService(LlmRouterService llmRouterService) {
        this.llmRouterService = llmRouterService;
    }

    public String summarize(Plan plan, Map<String, Object> results, String provider) {
        logger.info("[START] FinalSummarizerService: Gerando resposta natural baseada nos resultados.");
        
        String planSummary = plan != null && plan.getSummary() != null ? plan.getSummary() : "Sem resumo fornecido.";
        String resultsString = results != null ? results.toString() : "Nenhum resultado obtido.";

        String systemPrompt = """
            Você é o assistente virtual corporativo Artemis.
            
            O usuário solicitou inicialmente a seguinte tarefa (resumo do plano):
            %s
            
            As ferramentas do sistema executaram a tarefa e retornaram os seguintes dados/logs brutos:
            %s
            
            Sua tarefa:
            Leia os resultados brutos e escreva uma resposta natural e conversacional para o usuário.
            - A resposta deve ser concisa (máximo de 1000 caracteres), direta e amigável.
            - NÃO invente histórias, personagens ou cenários irreais. Apenas relate os dados retornados.
            - Se o resultado for uma lista de pessoas ou itens, formate de forma limpa.
            - Não mencione a palavra "JSON", IDs técnicos desnecessários, nem diga "A ferramenta retornou".
            - Fale como se você mesmo tivesse verificado os dados e feito a ação.
            """.formatted(planSummary, resultsString);

        try {
            String naturalResponse = llmRouterService.prepareTextPrompt(provider, systemPrompt, "Gere a resposta final para o usuário.")
                    .call()
                    .content();
                    
            logger.info("[END] FinalSummarizerService: Resposta natural gerada com sucesso.");
            return naturalResponse;
        } catch (Exception e) {
            logger.error("[ERROR] FinalSummarizerService: Falha ao gerar resposta natural. Fallback acionado.", e);
            return "Plano executado. (Não foi possível gerar um resumo natural, consulte os dados brutos abaixo)";
        }
    }
}
