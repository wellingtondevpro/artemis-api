package com.candela.artemisapi.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Primary
@Component
public class MultiMcpClient implements MCPClient {

    private static final Logger logger = LoggerFactory.getLogger(MultiMcpClient.class);
    private final List<SseMcpClient> clients = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Mapeia o nome da ferramenta para o cliente que a possui
    private final Map<String, SseMcpClient> toolRoutingCache = new ConcurrentHashMap<>();

    @Value("${mcp.servers.urls}")
    private String mcpServerUrls;

    @PostConstruct
    public void init() {
        // Lemos as URLs dos servidores MCP a partir do application.properties
        for (String url : mcpServerUrls.split(",")) {
            String trimmedUrl = url.trim();
            if (!trimmedUrl.isEmpty()) {
                logger.info("[MultiMcpClient] Inicializando conexão para o servidor: {}", trimmedUrl);
                SseMcpClient client = new SseMcpClient(trimmedUrl);
                clients.add(client);
            }
        }
    }

    @Override
    public Object callTool(String toolName, Map<String, Object> arguments) {
        SseMcpClient targetClient = toolRoutingCache.get(toolName);
        
        if (targetClient != null) {
            logger.info("[MultiMcpClient] Roteando chamada da ferramenta '{}' para o servidor apropriado.", toolName);
            return targetClient.callTool(toolName, arguments);
        }
        
        // Fallback: se não achar no cache, tenta em todos (ou atualiza o cache)
        logger.warn("[MultiMcpClient] Ferramenta '{}' não encontrada no cache. Atualizando cache...", toolName);
        getAvailableTools(); // Força atualização do cache
        
        targetClient = toolRoutingCache.get(toolName);
        if (targetClient != null) {
            return targetClient.callTool(toolName, arguments);
        }
        
        return "Erro: Ferramenta " + toolName + " não foi encontrada em nenhum servidor MCP conectado.";
    }

    @Override
    public String getAvailableTools() {
        ArrayNode allTools = objectMapper.createArrayNode();
        
        for (SseMcpClient client : clients) {
            try {
                String rawResult = client.getAvailableTools();
                if (rawResult != null && !rawResult.equals("[]")) {
                    JsonNode root = objectMapper.readTree(rawResult);
                    JsonNode toolsNode = root.has("tools") ? root.get("tools") : root;
                    
                    if (toolsNode.isArray()) {
                        for (JsonNode toolNode : toolsNode) {
                            allTools.add(toolNode);
                            if (toolNode.has("name")) {
                                toolRoutingCache.put(toolNode.get("name").asText(), client);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("[MultiMcpClient] Erro ao listar ferramentas de um dos clientes", e);
            }
        }
        
        ObjectNode result = objectMapper.createObjectNode();
        result.set("tools", allTools);
        
        return result.toString();
    }
}
