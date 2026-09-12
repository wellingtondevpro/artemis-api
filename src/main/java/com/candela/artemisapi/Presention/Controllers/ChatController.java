package com.candela.artemisapi.Presention.Controllers;

import com.candela.artemisapi.Application.Services.ChatService;
import com.candela.artemisapi.Presention.DTOs.ChatRequest;
import com.candela.artemisapi.Presention.DTOs.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }


    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        logger.info("=== Nova requisição recebida do Front-end ===");
        logger.info("ConversationID: {}", request.conversationId());
        
        // Recebe a string processada
        String aiText = chatService.processChat(request);
        
        // Retorna como JSON {"response": "..."} em vez de texto puro
        return new ChatResponse(aiText);
    }
}
