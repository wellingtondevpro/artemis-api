package com.candela.artemisapi.Application.Services;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.ai.tool.annotation.ToolParam;

@Component
public class ReActTools {

    private final com.candela.artemisapi.mcp.MCPClient mcpClient;

    public ReActTools(com.candela.artemisapi.mcp.MCPClient mcpClient) {
        this.mcpClient = mcpClient;
    }

    @Tool(name="executeMcpTool", description="Executa uma ferramenta dinâmica no Model Context Protocol (MCP). Use esta função para acionar sistemas corporativos como RH, Frota, Acessos, etc.")
    public String executeMcpTool(
            @ToolParam(description="O nome da ferramenta a ser executada no MCP (ex: iam.list_user_systems)") String toolName,
            @ToolParam(description="Os argumentos da ferramenta em formato JSON (ex: {\"usuario\": \"joao\"}). Se não houver, mande '{}'") String argumentsJson) {
        try {
            java.util.Map<String, Object> map = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(argumentsJson, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>(){});
            return String.valueOf(mcpClient.callTool(toolName, map));
        } catch (Exception e) {
            return "Erro ao executar tool: " + e.getMessage();
        }
    }
}
