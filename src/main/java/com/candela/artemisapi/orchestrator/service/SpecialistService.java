package com.candela.artemisapi.orchestrator.service;

import com.candela.artemisapi.orchestrator.model.PlanStep;
import com.candela.artemisapi.orchestrator.model.ToolExecutionProposal;
import com.candela.artemisapi.mcp.MCPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import com.candela.artemisapi.Application.Services.LlmRouterService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Iterator;
import java.util.Map;

@Service
public class SpecialistService {

    private static final Logger logger = LoggerFactory.getLogger(SpecialistService.class);
    private final LlmRouterService llmRouterService;
    private final MCPClient mcpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SpecialistService(LlmRouterService llmRouterService, MCPClient mcpClient) {
        this.llmRouterService = llmRouterService;
        this.mcpClient = mcpClient;
    }

    /**
     * Simplifica o schema gigante do MCP para um formato de texto amigável para a LLM,
     * reduzindo o consumo de tokens e evitando alucinações.
     */
    private String simplifyMcpSchemas(String rawSchemas, String domain) {
        StringBuilder simplified = new StringBuilder();
        try {
            JsonNode root = objectMapper.readTree(rawSchemas);
            JsonNode tools = root.has("tools") ? root.get("tools") : root;
            
            if (tools.isArray()) {
                for (JsonNode tool : tools) {
                    String name = tool.has("name") ? tool.get("name").asText() : "unknown";
                    
                    // Filtro Simples por Domínio (ex: RH -> rh_...)
                    // Se o domínio for mapeado, só adiciona as ferramentas que combinam.
                    // Para evitar de não achar nada, colocamos um filtro permissivo.
                    boolean matchesAcesso = domain.equalsIgnoreCase("ACESSO") && name.toLowerCase().startsWith("userman_");
                    if (domain != null && !name.toLowerCase().startsWith(domain.toLowerCase()) && !matchesAcesso && !domain.equalsIgnoreCase("TI")) {
                        continue; // FILTRA: só passa para o LLM as ferramentas do domínio correto, reduzindo o contexto!
                    }

                    String desc = tool.has("description") ? tool.get("description").asText() : "";
                    simplified.append("- Ferramenta: ").append(name).append("\n");
                    simplified.append("  Descrição: ").append(desc).append("\n");
                    
                    if (tool.has("inputSchema") && tool.get("inputSchema").has("properties")) {
                        JsonNode props = tool.get("inputSchema").get("properties");
                        simplified.append("  Parâmetros esperados:\n");
                        Iterator<Map.Entry<String, JsonNode>> fields = props.fields();
                        while (fields.hasNext()) {
                            Map.Entry<String, JsonNode> field = fields.next();
                            String type = field.getValue().has("type") ? field.getValue().get("type").asText() : "string";
                            boolean isRequired = false;
                            if (tool.get("inputSchema").has("required")) {
                                JsonNode reqNode = tool.get("inputSchema").get("required");
                                if (reqNode.isArray()) {
                                    for (JsonNode r : reqNode) {
                                        if (r.asText().equals(field.getKey())) {
                                            isRequired = true;
                                            break;
                                        }
                                    }
                                }
                            }
                            simplified.append("    * ").append(field.getKey()).append(" (").append(type).append(isRequired ? " - OBRIGATÓRIO" : "").append(")\n");
                        }
                    }
                    simplified.append("\n");
                }
            }
        } catch (Exception e) {
            logger.warn("Erro ao simplificar schemas do MCP. Usando fallback bruto.", e);
            return rawSchemas;
        }
        
        if (simplified.length() == 0) {
            return "Nenhuma ferramenta encontrada.";
        }
        return simplified.toString();
    }

    private java.util.List<String> getMissingRequiredParameters(String toolName, Map<String, Object> arguments, String rawSchemas) {
        java.util.List<String> missing = new java.util.ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawSchemas);
            JsonNode tools = root.has("tools") ? root.get("tools") : root;
            if (tools.isArray()) {
                for (JsonNode tool : tools) {
                    if (tool.has("name") && toolName.equals(tool.get("name").asText())) {
                        if (tool.has("inputSchema") && tool.get("inputSchema").has("required")) {
                            JsonNode requiredNode = tool.get("inputSchema").get("required");
                            if (requiredNode.isArray()) {
                                for (JsonNode req : requiredNode) {
                                    String reqField = req.asText();
                                    Object val = arguments != null ? arguments.get(reqField) : null;
                                    if (val == null || val.toString().trim().isEmpty() || "null".equalsIgnoreCase(val.toString().trim())) {
                                        missing.add(reqField);
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao validar parametros obrigatórios", e);
        }
        return missing;
    }

    public void generateToolProposal(PlanStep step, String provider) {
        logger.info("[START] SpecialistService: Processando passo Action '{}' no domínio '{}'", step.getAction(), step.getDomain());

        String rawMcpSchemas = mcpClient.getAvailableTools();
        String cleanMcpSchemas = simplifyMcpSchemas(rawMcpSchemas, step.getDomain());

        BeanOutputConverter<ToolExecutionProposal> converter = new BeanOutputConverter<>(ToolExecutionProposal.class);
        String format = converter.getFormat();

        String systemPrompt = String.format("""
            Você é um Especialista de Integração de Sistemas do domínio: %s.
            Sua missão é mapear a intenção do usuário para uma das ferramentas disponíveis.
            
            Entidade ID Resolvida: %s
            Ação pretendida: %s
            Parâmetros informados pelo usuário: %s
            
            Ferramentas Disponíveis (Simplificadas):
            %s
            
            INSTRUÇÕES DE FORMATAÇÃO JSON OBRIGATÓRIAS:
            %s
            
            IMPORTANTE: IDs alfanuméricos ou hexadecimais (ex: 96e26b10) DEVEM sempre ser colocados entre aspas duplas como String no JSON final.
            CRÍTICO: RETORNE APENAS O JSON BRUTO! NÃO USE BLOCOS DE MARKDOWN. COMECE A RESPOSTA DIRETAMENTE COM { E TERMINE COM }.
            ATENÇÃO: O JSON DEVE SER ESTRITAMENTE VÁLIDO. NUNCA DEIXE VÍRGULAS SOBRANDO (TRAILING COMMAS) ANTES DE FECHAR CHAVES } OU COLCHETES ].
            """, 
            step.getDomain(), 
            step.getResolvedEntityId(), 
            step.getAction(), 
            step.getParameters(),
            cleanMcpSchemas,
            format
        );

        try {
            ToolExecutionProposal proposal = llmRouterService.prepareJsonPrompt(provider, systemPrompt, "Gere a proposta de execução da ferramenta. Retorne APENAS um JSON válido. Não adicione textos explicativos.")
                    .call()
                    .entity(ToolExecutionProposal.class);

            step.setToolProposal(proposal);
            
            java.util.List<String> missingParams = getMissingRequiredParameters(proposal.getToolName(), proposal.getArguments(), rawMcpSchemas);
            if (!missingParams.isEmpty()) {
                step.setStatus("MISSING_PARAMETERS");
                step.setMissingParameters(missingParams);
                logger.warn("[END] SpecialistService: Parametros obrigatorios ausentes: {}", missingParams);
                return;
            }
            
            logger.info("[END] SpecialistService: Sucesso! Especialista propôs usar a ferramenta MCP '{}' com argumentos: {}", proposal.getToolName(), proposal.getArguments());
            
        } catch (Exception e) {
            logger.warn("[END] SpecialistService: FALHA ao gerar proposta para action '{}'. Erro de parse JSON: {}", step.getAction(), e.getMessage());
            step.setStatus("FAILED");
        }
    }
}
