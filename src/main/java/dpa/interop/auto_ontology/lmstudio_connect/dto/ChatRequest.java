package dpa.interop.auto_ontology.lmstudio_connect.dto;

import java.util.List;

public record ChatRequest(
        String model,
        List<Message> messages,
        double temperature
) {

    public record Message(
            String role,  // "system", "user", "assistant"
            String content
    ) {}
}