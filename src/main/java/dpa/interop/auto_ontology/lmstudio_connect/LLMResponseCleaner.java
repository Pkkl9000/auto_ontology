package dpa.interop.auto_ontology.lmstudio_connect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LLMResponseCleaner {

    private static final ObjectMapper mapper = new ObjectMapper();

    public CleanedResponse cleanAndParse(String llmResponse) {
        try {
            if (llmResponse == null || llmResponse.isBlank()) {
                return new CleanedResponse(null, "Empty response received");
            }

            String cleaned = llmResponse.replaceAll("(?s)<think>.*?</think>", "")
                    .replaceAll("//.*", "")
                    .trim();

            List<String> jsonBlocks = new ArrayList<>();
            Matcher matcher = Pattern.compile("\\{(?:[^{}]|\\{(?:[^{}]|\\{[^{}]*\\})*\\})*\\}").matcher(cleaned);

            while (matcher.find()) {
                jsonBlocks.add(matcher.group());
            }

            if (jsonBlocks.isEmpty()) {
                return new CleanedResponse(null,
                        "No JSON objects found in response. Full response:\n" + llmResponse);
            }

            if (jsonBlocks.size() == 1) {
                JsonNode jsonNode = mapper.readTree(jsonBlocks.get(0));
                return new CleanedResponse(jsonNode, null);
            }

            String combinedJson = "[" + String.join(",", jsonBlocks) + "]";
            JsonNode jsonNode = mapper.readTree(combinedJson);
            return new CleanedResponse(jsonNode, null);

        } catch (JsonProcessingException e) {
            return new CleanedResponse(null,
                    "Invalid JSON format: " + e.getMessage() + "\nOriginal response:\n" + llmResponse);
        } catch (Exception e) {
            return new CleanedResponse(null,
                    "Processing error: " + e.getMessage() + "\nOriginal response:\n" + llmResponse);
        }
    }

    public static class CleanedResponse {
        private final JsonNode json;
        private final String error;

        public CleanedResponse(JsonNode json, String error) {
            this.json = json;
            this.error = error;
        }

        public boolean hasError() {
            return error != null;
        }

        public JsonNode getJson() {
            return json;
        }

        public String getError() {
            return error;
        }
    }
}