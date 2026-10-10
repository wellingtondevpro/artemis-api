package com.candela.artemisapi.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class SseMcpClient implements MCPClient {

    private static final Logger logger = LoggerFactory.getLogger(SseMcpClient.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AtomicInteger requestId = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();
    
    private String postEndpoint = null;
    private final String serverUrl;

    public SseMcpClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.httpClient = HttpClient.newBuilder().build();
        this.objectMapper = new ObjectMapper();
        connect();
    }

    private void connect() {
        try {
            logger.info("[API LOG] Tentando conectar no MCP Server em: " + serverUrl + "/sse");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/sse"))
                    .GET()
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                    .thenAccept(response -> {
                        response.body().forEach(line -> {
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6);
                                if (data.contains("/message?sessionId=")) {
                                    postEndpoint = serverUrl + data;
                                    logger.info("[API LOG] MCP SSE Conectado! Post Endpoint: {}", postEndpoint);
                                    sendInitialize();
                                } else {
                                    try {
                                        JsonNode node = objectMapper.readTree(data);
                                        if (node.has("id")) {
                                            int id = node.get("id").asInt();
                                            if (node.has("result")) {
                                                CompletableFuture<String> future = pendingRequests.remove(id);
                                                if (future != null) {
                                                    // Extract text content from result
                                                    JsonNode content = node.get("result").get("content");
                                                    if (content != null && content.isArray() && content.size() > 0) {
                                                        future.complete(content.get(0).get("text").asText());
                                                    } else {
                                                        future.complete(node.get("result").toString());
                                                    }
                                                }
                                            } else if (node.has("error")) {
                                                logger.error("[API LOG] MCP Server returned error: {}", node.get("error"));
                                                CompletableFuture<String> future = pendingRequests.remove(id);
                                                if (future != null) {
                                                    future.completeExceptionally(new RuntimeException(node.get("error").toString()));
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        logger.error("Error parsing SSE data: " + data, e);
                                    }
                                }
                            }
                        });
                    }).whenComplete((res, ex) -> {
                        logger.warn("[API LOG] SSE Connection closed for {}. Reconnecting...", serverUrl);
                        postEndpoint = null;
                    });
            
            int retries = 0;
            while (postEndpoint == null && retries < 50) {
                Thread.sleep(100);
                retries++;
            }
        } catch (Exception e) {
            logger.error("Error connecting to MCP server", e);
        }
    }

    private void sendInitialize() {
        try {
            Map<String, Object> params = Map.of(
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of(),
                "clientInfo", Map.of("name", "ArtemisClient", "version", "1.0")
            );
            Map<String, Object> req = Map.of(
                "jsonrpc", "2.0",
                "id", requestId.getAndIncrement(),
                "method", "initialize",
                "params", params
            );
            sendPost(objectMapper.writeValueAsString(req));
            
            Map<String, Object> notif = Map.of(
                "jsonrpc", "2.0",
                "method", "notifications/initialized"
            );
            sendPost(objectMapper.writeValueAsString(notif));
        } catch (Exception e) {
            logger.error("Error sending initialize", e);
        }
    }

    private void sendPost(String json) {
        if (postEndpoint == null) return;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(postEndpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            httpClient.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            logger.error("Error sending POST", e);
        }
    }

    @Override
    public Object callTool(String toolName, Map<String, Object> arguments) {
        if (postEndpoint == null) {
            return "Erro: MCP Client não conectado ao servidor na porta 8082.";
        }
        try {
            Map<String, Object> params = Map.of(
                "name", toolName,
                "arguments", arguments != null ? arguments : Map.of()
            );
            int id = requestId.getAndIncrement();
            Map<String, Object> req = Map.of(
                "jsonrpc", "2.0",
                "id", id,
                "method", "tools/call",
                "params", params
            );
            
            CompletableFuture<String> future = new CompletableFuture<>();
            pendingRequests.put(id, future);
            
            sendPost(objectMapper.writeValueAsString(req));
            
            // Aguarda resposta por até 5 segundos
            return future.get(5, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            logger.error("Timeout ao chamar ferramenta {}. Invalidando conexão.", toolName, e);
            postEndpoint = null;
            connect();
            return "Erro: Timeout na comunicação com MCP Server.";
        } catch (Exception e) {
            return "Erro ao chamar ferramenta " + toolName + ": " + e.getMessage();
        }
    }

    @Override
    public String getAvailableTools() {
        if (postEndpoint == null) {
            connect(); // Tenta reconectar se estiver desconectado
            if (postEndpoint == null) return "[]";
        }
        try {
            int id = requestId.getAndIncrement();
            Map<String, Object> req = Map.of(
                "jsonrpc", "2.0",
                "id", id,
                "method", "tools/list"
            );
            
            CompletableFuture<String> future = new CompletableFuture<>();
            pendingRequests.put(id, future);
            
            sendPost(objectMapper.writeValueAsString(req));
            
            // Aguarda resposta
            return future.get(5, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            logger.error("Timeout ao listar ferramentas. Invalidando conexão.", e);
            postEndpoint = null;
            connect();
            return "[]";
        } catch (Exception e) {
            logger.error("Erro ao listar ferramentas", e);
            return "[]";
        }
    }
}
