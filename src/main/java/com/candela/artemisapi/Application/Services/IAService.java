package com.candela.artemisapi.Application.Services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IAService {

    private static final Logger logger = LoggerFactory.getLogger(IAService.class);
    private final LlmRouterService llmRouterService;

    public IAService(LlmRouterService llmRouterService) {
        this.llmRouterService = llmRouterService;
        logger.info("IAService inicializado! Pronto para se comunicar com as LLMs via Router.");
    }

    public <T> T getStructuredOutput(String systemPrompt, String userMessage, Class<T> targetClass, String provider) {
        logger.info("Chamando LLM para extração estruturada de tipo: {}", targetClass.getSimpleName());
        
        long startTime = System.currentTimeMillis();
        
        T response = this.llmRouterService.prepareJsonPrompt(provider, systemPrompt, userMessage)
                .call()
                .entity(targetClass);
                
        long timeTaken = System.currentTimeMillis() - startTime;
        logger.info("Resposta estruturada recebida com sucesso! Tempo de processamento: {} ms", timeTaken);
        
        return response;
    }
}
