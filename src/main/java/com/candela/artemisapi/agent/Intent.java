package com.candela.artemisapi.agent;

import java.util.Map;

public record Intent(
    String domain,
    String operation,
    String target,
    Map<String, Object> parameters
) {}
