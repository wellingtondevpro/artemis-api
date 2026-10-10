package com.candela.artemisapi.orchestrator.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaChatOptions;

public class TestCompile {
    public void test(ChatClient chatClient) {
        chatClient.prompt().options(OllamaChatOptions.builder().temperature(0.0).format("json"));
    }
}
