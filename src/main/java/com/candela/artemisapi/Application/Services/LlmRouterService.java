package com.candela.artemisapi.Application.Services;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

@Service
public class LlmRouterService {

    private final ChatClient localClient;
    private final ChatClient cloudClient;

    public LlmRouterService(OllamaChatModel ollamaModel, OpenAiChatModel openAiModel) {
        this.localClient = ChatClient.builder(ollamaModel).build();
        this.cloudClient = ChatClient.builder(openAiModel).build();
    }

    public ChatClient.ChatClientRequestSpec prepareJsonPrompt(String provider, String systemPrompt, String userMessage) {
        if ("cloud".equalsIgnoreCase(provider) || "deepseek".equalsIgnoreCase(provider)) {
            return cloudClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .options(OpenAiChatOptions.builder().temperature(0.0));
        }
        return localClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .options(OllamaChatOptions.builder().temperature(0.0).format("json"));
    }

    public ChatClient.ChatClientRequestSpec prepareTextPrompt(String provider, String systemPrompt, String userMessage) {
        if ("cloud".equalsIgnoreCase(provider) || "deepseek".equalsIgnoreCase(provider)) {
            return cloudClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .options(OpenAiChatOptions.builder().temperature(0.7));
        }
        return localClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .options(OllamaChatOptions.builder().temperature(0.7));
    }
}
