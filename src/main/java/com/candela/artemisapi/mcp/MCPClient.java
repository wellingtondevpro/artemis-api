package com.candela.artemisapi.mcp;

import java.util.Map;

/**
 * Interface/Abstração para futura integração com ferramentas via Model Context Protocol (MCP).
 * Neste ponto entrarão as chamadas para os MCP Tools após a identificação da intenção e capability.
 */
public interface MCPClient {
    
    /**
     * Executa uma ferramenta no MCP Server conectado.
     * 
     * @param toolName O nome da ferramenta a ser executada
     * @param arguments Argumentos extraídos da intenção e contexto do usuário
     * @return O resultado retornado pela execução da ferramenta (futuramente poderá ser estruturado)
     */
    Object callTool(String toolName, Map<String, Object> arguments);

    /**
     * Requisita a lista de ferramentas disponíveis no servidor MCP (tools/list).
     */
    String getAvailableTools();
}
