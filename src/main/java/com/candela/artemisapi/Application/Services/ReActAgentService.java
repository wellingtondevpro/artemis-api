package com.candela.artemisapi.Application.Services;

import org.springframework.stereotype.Service;
import org.springframework.ai.chat.client.ChatClient;
import com.candela.artemisapi.orchestrator.model.AgentResponse;
import com.candela.artemisapi.Presention.DTOs.ChatRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ReActAgentService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReActAgentService.class);
    private final ChatClient localClient;
    private final ChatClient cloudClient;
    private final ReActTools reActTools;
    private final com.candela.artemisapi.mcp.MCPClient mcpClient;

    public ReActAgentService(org.springframework.ai.ollama.OllamaChatModel ollamaModel, org.springframework.ai.openai.OpenAiChatModel openAiModel, ReActTools reActTools, com.candela.artemisapi.mcp.MCPClient mcpClient) {
        this.localClient = ChatClient.builder(ollamaModel).build();
        this.cloudClient = ChatClient.builder(openAiModel).build();
        this.reActTools = reActTools;
        this.mcpClient = mcpClient;
    }

    public AgentResponse processChatReAct(ChatRequest request) {
        logger.info("=== Iniciando execução autônoma (ReAct) ===");
        String availableToolsJson = mcpClient.getAvailableTools();
        
        String systemPrompt = """
            Você é o assistente autônomo Artemis. 
            Sua missão é ajudar o usuário atendendo a solicitação dele de forma autônoma.
            Você tem acesso à ferramenta Spring AI chamada 'executeMcpTool'.
            Sempre que precisar buscar dados ou realizar ações nos sistemas, USE a ferramenta 'executeMcpTool'.
            
            Lista de ferramentas (tools) MCP que você pode chamar:
            %s
            
            Você DEVE obrigatoriamente chamar 'executeMcpTool' passando o nome exato da ferramenta em 'toolName' (presente no JSON acima) e os parâmetros mapeados em 'argumentsJson' no formato JSON válido.
            """.formatted(availableToolsJson);
            
        ChatClient clientToUse = "cloud".equalsIgnoreCase(request.provider()) || "deepseek".equalsIgnoreCase(request.provider()) 
            ? cloudClient : localClient;

        try {
            String responseText = clientToUse.prompt()
                .system(systemPrompt)
                .user(request.message())
                .tools(reActTools)
                .call()
                .content();
                
            return new AgentResponse(
                AgentResponse.ResponseType.EXECUTION_RESULT,
                responseText,
                null
            );
        } catch(Exception e) {
             logger.error("Erro ReAct", e);
             return new AgentResponse(
                AgentResponse.ResponseType.ERROR,
                "Erro na execução autônoma ReAct: " + e.getMessage(),
                null
            );
        }
    }
}
