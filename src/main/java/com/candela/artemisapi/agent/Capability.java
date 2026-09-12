package com.candela.artemisapi.agent;

public record Capability(
    String id,
    String description,
    String type, // QUERY, COMMAND, WORKFLOW
    String domain,
    String operation,
    String target
) {}
