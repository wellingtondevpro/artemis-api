package com.candela.artemisapi.Application.Services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class IAService {

    private static final Logger logger = LoggerFactory.getLogger(IAService.class);
    private final ChatClient chatClient;

    public IAService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
        logger.info("IAService inicializado! Pronto para se comunicar com o Ollama local.");
    }

    public <T> T getStructuredOutput(String systemPrompt, String userMessage, Class<T> targetClass) {
        logger.info("Chamando LLM para extração estruturada de tipo: {}", targetClass.getSimpleName());
        
        long startTime = System.currentTimeMillis();
        
        T response = this.chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .entity(targetClass);
                
        long timeTaken = System.currentTimeMillis() - startTime;
        logger.info("Resposta estruturada recebida com sucesso! Tempo de processamento: {} ms", timeTaken);
        
        return response;
    }
}
