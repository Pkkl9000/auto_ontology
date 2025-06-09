package dpa.interop.auto_ontology.lmstudio_connect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dpa.interop.auto_ontology.lmstudio_connect.dto.ChatRequest;
import dpa.interop.auto_ontology.lmstudio_connect.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class LLMAssistant {

    private final ChatService chatService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String MODEL = "qwen3-8b";


    private final String LM_STUDIO_HEALTH_URL = "http://localhost:1234/health";

    private final String LM_STUDIO_URL = "http://localhost:1234/v1/chat/completions";


    public CompletableFuture<String> getLLMResponseAsync(String systemPrompt, String userPrompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ChatRequest request = new ChatRequest(
                        MODEL,
                        List.of(
                                new ChatRequest.Message("system", systemPrompt),
                                new ChatRequest.Message("user", userPrompt)
                        ),
                        0.7
                );

                ResponseEntity<ChatResponse> response = restTemplate.postForEntity(
                        LM_STUDIO_URL,
                        request,
                        ChatResponse.class
                );

                return cleanJsonContent(response.getBody().choices().get(0).message().content());
            } catch (Exception e) {
                log.error("LLM request failed", e);
                throw new CompletionException("LLM processing error: " + e.getMessage(), e);
            }
        });
    }

    public String getLLMResponseWithFallback(String systemPrompt, String userPrompt) {
        try {
            if (!isServerHealthy()) {
                log.warn("LM Studio server is not healthy, using fallback response");
                return getCachedResponse();
            }

            return getLLMResponse(systemPrompt, userPrompt);
        } catch (Exception e) {
            log.error("Error getting LLM response, using fallback", e);
            return getCachedResponse();
        }
    }

    public String getLLMResponse(String systemPrompt, String userPrompt) {
        long startTime = System.currentTimeMillis();
        try {
            ChatRequest request = createChatRequest(systemPrompt, userPrompt);
            ChatResponse response = chatService.sendChatRequest(request);

            String content = extractContentFromResponse(response);
            content = cleanJsonContent(content);
            validateJson(content);

            log.info("LLM request completed in {} ms", System.currentTimeMillis() - startTime);
            return content;
        } catch (Exception e) {
            log.error("LLM request failed after {} ms", System.currentTimeMillis() - startTime, e);
            throw new RuntimeException("LLM request failed", e);
        }
    }

    private boolean isServerHealthy() {
        try {
            ResponseEntity<String> healthResponse = restTemplate.getForEntity(LM_STUDIO_HEALTH_URL, String.class);
            return healthResponse.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Health check failed: {}", e.getMessage());
            return false;
        }
    }

    private String getCachedResponse() {
        return """
            {
                "status": "fallback",
                "message": "LLM service is currently unavailable"
            }
            """;
    }

    private ChatRequest createChatRequest(String systemPrompt, String userPrompt) {
        return new ChatRequest(
                MODEL,
                List.of(
                        new ChatRequest.Message("system", systemPrompt),
                        new ChatRequest.Message("user", userPrompt)
                ),
                0.7
        );
    }

    private String extractContentFromResponse(ChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new RuntimeException("Invalid LLM response structure");
        }
        return response.choices().get(0).message().content();
    }

    private String cleanJsonContent(String content) {
        if (content == null || content.isBlank()) return "";

        content = content.strip();
        if (content.startsWith("```json")) {
            content = content.substring("```json".length());
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }

        return content.replace("\\r\\n", "\n").replace("\\", "").strip();
    }

    private void validateJson(String json) throws JsonProcessingException {
        objectMapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        objectMapper.readTree(json);
    }
}