package com.candela.artemisapi.conversation;

import com.candela.artemisapi.agent.Intent;
import java.util.HashMap;
import java.util.Map;

public class ConversationState {
    
    private String conversationId;
    private Intent intent;
    private Map<String, Object> data = new HashMap<>();
    private String currentCapability;
    private String status; // Ex: IDLE, COLLECTING, VALIDATING, READY, EXECUTING, COMPLETED

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Intent getIntent() {
        return intent;
    }

    public void setIntent(Intent intent) {
        this.intent = intent;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getCurrentCapability() {
        return currentCapability;
    }

    public void setCurrentCapability(String currentCapability) {
        this.currentCapability = currentCapability;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
