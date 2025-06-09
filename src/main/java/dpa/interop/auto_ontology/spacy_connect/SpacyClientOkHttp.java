package dpa.interop.auto_ontology.spacy_connect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dpa.interop.auto_ontology.spacy_connect.dto.SpacyFullResponse;
import dpa.interop.auto_ontology.spacy_connect.dto.SpacyShortResponse;
import dpa.interop.auto_ontology.spacy_connect.dto.SpacyTreeResponse;
import dpa.interop.auto_ontology.spacy_connect.dto.TextRequest;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class SpacyClientOkHttp {
    private static final String BASE_URL = "http://127.0.0.1:5000";
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public SpacyClientOkHttp(ObjectMapper objectMapper) {
        this.client = new OkHttpClient.Builder()
//                .connectTimeout(10, TimeUnit.SECONDS)
//                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = objectMapper;
    }

    private <T> T executePost(String path, Object requestBody, Class<T> responseType) throws IOException {
        String requestJson = objectMapper.writeValueAsString(requestBody);
        log.debug("Sending request to {}{}", BASE_URL, path);
        log.trace("Request body: {}", requestJson); // Только для трассировки

        RequestBody body = RequestBody.create(requestJson, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(BASE_URL + path)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            log.trace("Raw response: {}", responseBody); // Только для трассировки

            if (!response.isSuccessful()) {
                log.error("Request failed. Code: {}, Response body: {}",
                        response.code(), abbreviate(responseBody, 200));
                throw new IOException("HTTP " + response.code());
            }

            log.debug("Request to {}{} completed successfully", BASE_URL, path);
            return objectMapper.readValue(responseBody, responseType);
        } catch (IOException e) {
            log.error("Error calling {}{}: {}", BASE_URL, path, e.getMessage());
            throw e;
        }
    }

    public SpacyShortResponse getShortAnalysis(String text) throws IOException {
        log.info("Starting short analysis for text: {}", abbreviate(text, 50));
        return executePost("/short", new TextRequest(text), SpacyShortResponse.class);
    }

    public SpacyFullResponse getFullAnalysis(String text) throws IOException {
        log.info("Starting full analysis for text: {}", abbreviate(text, 50));
        return executePost("/full", new TextRequest(text), SpacyFullResponse.class);
    }

    public List<SpacyFullResponse.Entity> getEntities(String text) throws IOException {
        log.debug("Extracting entities from text: {}", abbreviate(text, 100));
        SpacyFullResponse response = getFullAnalysis(text);
        return Optional.ofNullable(response.getEntities())
                .orElse(Collections.emptyList());
    }

    public List<SpacyFullResponse.NounChunk> getNounChunks(String text) throws IOException {
        log.debug("Extracting noun chunks from text: {}", abbreviate(text, 100));
        SpacyFullResponse response = getFullAnalysis(text);
        return Optional.ofNullable(response.getNounChunks())
                .orElse(Collections.emptyList());
    }

    public SpacyTreeResponse getSyntaxTree(String text) throws IOException {
        log.info("Getting syntax tree for text: {}", abbreviate(text, 50));

        String requestJson = objectMapper.writeValueAsString(new TextRequest(text));
        log.debug("Request JSON: {}", requestJson);

        RequestBody body = RequestBody.create(requestJson, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(BASE_URL + "/tree")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            log.debug("Raw response ({}): {}", response.code(), responseBody);

            if (!response.isSuccessful()) {
                log.error("Request failed. Code: {}, Body: {}", response.code(), responseBody);
                throw new IOException("HTTP " + response.code());
            }

            log.debug("Response headers:");
            for (String name : response.headers().names()) {
                log.debug("  {}: {}", name, response.header(name));
            }

            try {
                SpacyTreeResponse result = objectMapper.readValue(responseBody, SpacyTreeResponse.class);
                log.debug("Parsed response: {}", result);

                if (result == null) {
                    log.error("Parsed response is null");
                    throw new IOException("Parsed response is null");
                }

                if (result.getTree() == null) {
                    log.error("Tree data is null in response. Full response: {}", result);
                    throw new IOException("Tree data is null in response");
                }

                log.info("Successfully parsed syntax tree with {} nodes", result.getTree().size());
                return result;

            } catch (JsonProcessingException e) {
                log.error("Failed to parse response. Body: {}", responseBody, e);
                throw new IOException("Failed to parse response", e);
            }
        } catch (IOException e) {
            log.error("Request failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String abbreviate(String text, int maxLength) {
        return text.length() > maxLength ?
                text.substring(0, maxLength - 3) + "..." : text;
    }
}

