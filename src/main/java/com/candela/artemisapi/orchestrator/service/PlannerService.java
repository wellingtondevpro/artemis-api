package com.candela.artemisapi.orchestrator.service;

import com.candela.artemisapi.orchestrator.model.Plan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import com.candela.artemisapi.Application.Services.LlmRouterService;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

@Service
public class PlannerService {

    private static final Logger logger = LoggerFactory.getLogger(PlannerService.class);
    private final LlmRouterService llmRouterService;

    public PlannerService(LlmRouterService llmRouterService) {
        this.llmRouterService = llmRouterService;
    }

    public Plan generatePlan(String userMessage, com.candela.artemisapi.conversation.ConversationState state) {
        logger.info("[START] PlannerService: Iniciando geração de plano para requisição: '{}'", userMessage);

        BeanOutputConverter<Plan> converter = new BeanOutputConverter<>(Plan.class);
        String format = converter.getFormat();

        String systemPrompt = """
            Você tem acesso ao Contexto da Conversa. Se houver uma intenção pendente ou parâmetros faltando no contexto, USE a nova mensagem do usuário para preencher os parâmetros que faltavam. Junte os parâmetros antigos com os novos!
            Você é o Arquiteto de Soluções de um sistema corporativo.
            Sua única função é ler a solicitação do usuário e gerar um Plano de Ação estruturado.
            Você não executa nada. Você apenas diz quais domínios e ações devem ser acionados.
            
            Domínios válidos: RH, FROTA, ACESSO, TI, FINANCEIRO, USERMAN, SYSTEM.
            
            REGRAS PARA OS PASSOS:
            - AJA EXATAMENTE SOBRE O PEDIDO DO USUÁRIO. NUNCA COPIE OS EXEMPLOS ABAIXO, CRIE OS SEUS DADOS BASEADO NO QUE O USUÁRIO DIGITOU.
            - Mantenha simples. Se for uma pergunta simples, gere APENAS UM PASSO.
            - A 'action' deve ser clara (ex: 'buscar', 'cadastrar', 'deletar').
            - Em 'parameters', extraia OS DADOS REAIS da pergunta (ex: nome, cargo, salario).
            - IMPORTANTE: Se o usuário apenas cumprimentar ("oi", "tudo bem?") ou fizer uma pergunta genérica/fora do escopo, use domain "SYSTEM" e action "chat", passando a intenção nos parameters.
            
            EXEMPLOS (FEW-SHOT):
            
            Usuário: "Quero cadastrar o João como desenvolvedor pleno com salário de 5000"
            Resposta:
            {
              "summary": "Cadastrar novo funcionário no sistema de RH",
              "steps": [
                { "domain": "RH", "action": "cadastrar_funcionario", "parameters": { "nome": "João", "cargo": "desenvolvedor pleno", "salario": "5000" } }
              ]
            }
            
            Usuário: "Quais os acessos do usuário maria?"
            Resposta:
            {
              "summary": "Buscar perfis de acesso de um usuário",
              "steps": [
                { "domain": "USERMAN", "action": "buscar_acessos", "parameters": { "usuario": "maria" } }
              ]
            }

            Usuário: "Olá, bom dia! O que você pode fazer?"
            Resposta:
            {
              "summary": "Responder saudação e explicar funcionalidades",
              "steps": [
                { "domain": "SYSTEM", "action": "chat", "parameters": { "intent": "greeting_and_help" } }
              ]
            }
            
            INSTRUÇÕES DE FORMATAÇÃO JSON OBRIGATÓRIAS:
            %s
            
            CRÍTICO: RETORNE APENAS O JSON BRUTO! NÃO USE BLOCOS DE MARKDOWN. COMECE A RESPOSTA DIRETAMENTE COM { E TERMINE COM }.
            """.formatted(format);

        String userContext = state != null && state.getData() != null && !state.getData().isEmpty() 
            ? "Contexto da Conversa (Memória):\n" + state.getData() + "\n\nNova Mensagem: " + userMessage 
            : userMessage;

        String provider = state != null ? (String) state.getData().getOrDefault("llm_provider", "local") : "local";

        Plan plan = llmRouterService.prepareJsonPrompt(provider, systemPrompt, userContext)
                .call()
                .entity(Plan.class);

        logger.info("[END] PlannerService: Plano gerado com sucesso. ID: {} | Passos identificados: {}", plan.getPlanId(), plan.getSteps().size());
        return plan;
    }
}
