package com.candela.artemisapi.conversation;

import java.util.HashMap;
import java.util.Map;

public class ConversationState {
    
    private String conversationId;
    private Map<String, Object> data = new HashMap<>();
    private String status; // Ex: IDLE, COLLECTING, VALIDATING, READY, EXECUTING, COMPLETED

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
