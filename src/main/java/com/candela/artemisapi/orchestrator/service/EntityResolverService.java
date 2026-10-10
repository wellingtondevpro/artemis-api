package com.candela.artemisapi.orchestrator.service;

import com.candela.artemisapi.mcp.MCPClient;
import com.candela.artemisapi.orchestrator.model.Plan;
import com.candela.artemisapi.orchestrator.model.PlanStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EntityResolverService {

    private static final Logger logger = LoggerFactory.getLogger(EntityResolverService.class);
    private final MCPClient mcpClient;

    public EntityResolverService(MCPClient mcpClient) {
        this.mcpClient = mcpClient;
    }

    /**
     * Tenta resolver entidades (ex: "Pedro" -> ID 1).
     * @param plan O plano gerado pelo PlannerService
     * @return true se todas as entidades foram resolvidas sem ambiguidade, false se precisar de esclarecimento.
     */
    public boolean resolveEntities(Plan plan) {
        logger.info("[START] EntityResolverService: Analisando entidades para o plano '{}'", plan.getPlanId());
        
        for (PlanStep step : plan.getSteps()) {
            if (step.getParameters() != null && step.getParameters().containsKey("nome")) {
                String nome = (String) step.getParameters().get("nome");
                logger.info("[EntityResolverService] Resolvendo matrícula/ID para o nome: '{}'", nome);
                
                // Chamada REAL para o MCP Server de RH para resolver o nome em ID
                try {
                    Object result = mcpClient.callTool("rh_resolver_id_por_nome", Map.of("nome", nome));
                    String resultStr = result != null ? result.toString() : "";
                    
                    // Verifica se retornou múltiplos (ambiguidade) ou não encontrado
                    if (resultStr.contains("AMBIGUIDADE")) {
                        logger.warn("[END] EntityResolverService: Ambiguidade encontrada para '{}'. Retornando false.", nome);
                        return false;
                    }
                    if (resultStr.contains("NÃO ENCONTRADO")) {
                        logger.warn("[EntityResolverService] Funcionário '{}' não encontrado.", nome);
                        step.setResolvedEntityId("NOT_FOUND");
                        continue;
                    }
                    
                    // Extrai o ID numérico da resposta (Assumindo que o MCP retorna algo como "ID: 1")
                    Matcher matcher = Pattern.compile("ID: (\\d+)").matcher(resultStr);
                    if (matcher.find()) {
                        String resolvedId = matcher.group(1);
                        step.setResolvedEntityId(resolvedId); 
                        logger.info("[EntityResolverService] Nome '{}' resolvido para ID real: '{}'", nome, resolvedId);
                    } else {
                        logger.warn("[EntityResolverService] Falha ao extrair ID da resposta: {}", resultStr);
                        // Fallback em caso de erro no parse para não quebrar o fluxo
                        step.setResolvedEntityId(resultStr);
                    }
                } catch (Exception e) {
                    logger.error("[EntityResolverService] Erro ao chamar MCP para resolver entidade", e);
                    return false;
                }
            } else {
                logger.info("[EntityResolverService] Passo '{}' não possui o parâmetro 'nome' para resolver.", step.getAction());
            }
        }
        
        logger.info("[END] EntityResolverService: Todas as entidades foram resolvidas com sucesso (ou não necessitaram resolução).");
        return true;
    }
}
