package com.candela.artemisapi.Presention.DTOs;

public record ChatRequest(String conversationId, String message, String provider, Boolean useReAct) {
}
