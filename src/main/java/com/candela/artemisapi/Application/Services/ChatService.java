package com.candela.artemisapi.Application.Services;

import org.springframework.stereotype.Service;
import com.candela.artemisapi.Presention.DTOs.ChatRequest;
import com.candela.artemisapi.conversation.ConversationState;
import com.candela.artemisapi.conversation.ConversationStateService;
import com.candela.artemisapi.orchestrator.model.AgentResponse;
import com.candela.artemisapi.orchestrator.model.Plan;
import com.candela.artemisapi.orchestrator.model.PlanStep;
import com.candela.artemisapi.orchestrator.service.PlannerService;
import com.candela.artemisapi.orchestrator.service.EntityResolverService;
import com.candela.artemisapi.orchestrator.service.SpecialistService;
import com.candela.artemisapi.orchestrator.service.ExecutionService;

import java.util.Map;

@Service
public class ChatService {

    private final PlannerService plannerService;
    private final EntityResolverService entityResolver;
    private final SpecialistService specialistService;
    private final ExecutionService executionService;
    private final ConversationStateService stateService;
    private final com.candela.artemisapi.orchestrator.service.FinalSummarizerService finalSummarizerService;

    public ChatService(PlannerService plannerService, 
                       EntityResolverService entityResolver,
                       SpecialistService specialistService,
                       ExecutionService executionService,
                       ConversationStateService stateService,
                       com.candela.artemisapi.orchestrator.service.FinalSummarizerService finalSummarizerService) {
        this.plannerService = plannerService;
        this.entityResolver = entityResolver;
        this.specialistService = specialistService;
        this.executionService = executionService;
        this.stateService = stateService;
        this.finalSummarizerService = finalSummarizerService;
    }

    public AgentResponse processChat(ChatRequest request) {
        ConversationState state = stateService.getState(request.conversationId());
        
        String provider = request.provider();
        if (provider != null && !provider.isEmpty()) {
            state.getData().put("llm_provider", provider);
        } else {
            provider = (String) state.getData().getOrDefault("llm_provider", "local");
        }
        
        Plan plan;
        try {
            plan = plannerService.generatePlan(request.message(), state);
            plan.setProvider(provider);
            plan.setConversationId(request.conversationId());
        } catch (Exception e) {
            return new AgentResponse(
                AgentResponse.ResponseType.ERROR,
                "Erro na camada de Planejamento (LLM): Não foi possível compreender a solicitação estruturada. Detalhe: " + e.getMessage(),
                null
            );
        }

        try {
            // 2. O Resolutor de Entidades busca e limpa as ambiguidades globais
            boolean isResolved = entityResolver.resolveEntities(plan);
            if (!isResolved) {
                // Volta pro Angular perguntar qual João é
                return new AgentResponse(
                    AgentResponse.ResponseType.CLARIFICATION_REQUIRED, 
                    "Encontrei uma ambiguidade. Existem múltiplos registros para esse nome. Qual o sobrenome ou matrícula?",
                    plan
                );
            }
        } catch (Exception e) {
            return new AgentResponse(
                AgentResponse.ResponseType.ERROR,
                "Erro na camada de Resolução de Entidades (MCP/Domain API): " + e.getMessage(),
                plan
            );
        }

        try {
            // 3. Roteia cada passo do plano para o Especialista daquele Domínio
            for (PlanStep step : plan.getSteps()) {
                if ("SYSTEM".equalsIgnoreCase(step.getDomain()) && ("chat".equalsIgnoreCase(step.getAction()) || "clarify".equalsIgnoreCase(step.getAction()))) {
                    // Trata o Fallback (Small Talk, Saudações, Ambiguidades)
                    return new AgentResponse(
                        AgentResponse.ResponseType.EXECUTION_RESULT, 
                        "Olá! Sou o assistente Artemis. Como não encontrei uma ação específica de negócio na sua mensagem, me diga: como posso ajudar você hoje? Consigo auxiliar com RH, Frotas, Acessos e outros processos.",
                        null
                    );
                }

                specialistService.generateToolProposal(step, plan.getProvider());
                if ("MISSING_PARAMETERS".equals(step.getStatus())) {
                    // Salvar intenção no contexto para a próxima mensagem do usuário preencher
                    String missing = String.join(", ", step.getMissingParameters());
                    state.getData().put("pendingAction", step.getAction());
                    state.getData().put("pendingDomain", step.getDomain());
                    state.getData().put("pendingParameters", step.getParameters());
                    state.getData().put("missingParameters", step.getMissingParameters());
                    
                    return new AgentResponse(
                        AgentResponse.ResponseType.CLARIFICATION_REQUIRED, 
                        "Para continuar com a ação (" + step.getAction() + "), preciso dos seguintes dados adicionais: " + missing + ".",
                        plan
                    );
                }
                if ("FAILED".equals(step.getStatus())) {
                    return new AgentResponse(
                        AgentResponse.ResponseType.ERROR, 
                        "Erro na camada Especialista: Não consegui mapear uma ferramenta MCP para a ação solicitada no domínio " + step.getDomain(),
                        null
                    );
                }
            }
        } catch (Exception e) {
            return new AgentResponse(
                AgentResponse.ResponseType.ERROR,
                "Erro na camada Especialista ao gerar proposta de ferramenta: " + e.getMessage(),
                plan
            );
        }

        // 4. Retorna a Proposta do Plano para aprovação humana (HITL) no Angular
        return new AgentResponse(
            AgentResponse.ResponseType.PLAN_PROPOSAL,
            "Aqui está o plano das ações que preparei. Por favor, confira os passos e clique em Confirmar.",
            plan
        );
    }

    public AgentResponse executeConfirmedPlan(Plan plan) {
        try {
            // 5. Execução real das chamadas
            Map<String, Object> results = executionService.executePlan(plan);
            
            // 6. Geração de resposta natural com base nos resultados crus (LLM)
            String provider = plan.getProvider() != null ? plan.getProvider() : "local";
            String respostaNatural = finalSummarizerService.summarize(plan, results, provider);
            
            // Atualizar o contexto da conversa com o histórico desta interação
            if (plan.getConversationId() != null) {
                ConversationState state = stateService.getState(plan.getConversationId());
                java.util.List<String> history = (java.util.List<String>) state.getData().getOrDefault("history", new java.util.ArrayList<String>());
                history.add("Usuário pediu: " + plan.getSummary());
                history.add("Sistema respondeu: " + respostaNatural);
                // Manter apenas as últimas 10 interações para não estourar o limite de tokens
                if (history.size() > 10) {
                    history = history.subList(history.size() - 10, history.size());
                }
                state.getData().put("history", history);
                state.getData().remove("pendingAction");
                state.getData().remove("pendingDomain");
                state.getData().remove("pendingParameters");
                state.getData().remove("missingParameters");
            }
            
            AgentResponse response = new AgentResponse(
                AgentResponse.ResponseType.EXECUTION_RESULT,
                respostaNatural,
                null
            );
            response.setExecutionData(results); // Mantém os dados crus para o frontend caso queira exibir logs detalhados
            return response;
        } catch (Exception e) {
            return new AgentResponse(
                AgentResponse.ResponseType.ERROR,
                "Erro na camada de Execução (MCP/Domain API): Falha ao acionar ferramentas finais. Detalhe: " + e.getMessage(),
                plan
            );
        }
    }
}
