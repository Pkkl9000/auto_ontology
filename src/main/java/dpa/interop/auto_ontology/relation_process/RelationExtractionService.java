package dpa.interop.auto_ontology.relation_process;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dpa.interop.auto_ontology.lmstudio_connect.LLMAssistant;
import dpa.interop.auto_ontology.prompt_process.PromptService;
import dpa.interop.auto_ontology.relation_process.dto.Relation;
import dpa.interop.auto_ontology.lmstudio_connect.LLMResponseCleaner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RelationExtractionService {

    private final LLMAssistant llmAssistant;
    private final PromptService promptService;
    private final LLMResponseCleaner responseCleaner;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT_NAME = "system_deep3";
    private static final String USER_PROMPT_NAME = "user_qwen3";

    public List<Relation> extractRelations(String text, List<String> subjects) {
        List<Relation> allRelations = Collections.synchronizedList(new ArrayList<>());
        String systemPrompt = buildSystemPrompt();

        List<CompletableFuture<Void>> futures = subjects.stream()
                .map(subject -> processSubject(text, subject, systemPrompt, allRelations))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return filterUniqueRelations(allRelations);
    }

    private String buildSystemPrompt() {
        try {
            return promptService.buildSystemPrompt(SYSTEM_PROMPT_NAME);
        } catch (IOException e) {
            throw new RuntimeException("Failed to build system prompt", e);
        }
    }

    private CompletableFuture<Void> processSubject(String text, String subject,
                                                   String systemPrompt, List<Relation> resultCollector) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String userPrompt = promptService.buildUserPromptWithTextAndEntity(
                        USER_PROMPT_NAME, text, subject);
                return llmAssistant.getLLMResponseAsync(systemPrompt, userPrompt).get();
            } catch (Exception e) {
                log.error("Error processing subject '{}'", subject, e);
                return null;
            }
        }).thenAccept(response -> {
            if (response != null) {
                List<Relation> relations = parseResponseToRelations(response);
                synchronized (resultCollector) {
                    resultCollector.addAll(relations);
                }
            }
        });
    }

    private List<Relation> parseResponseToRelations(String llmResponse) {
        try {
            LLMResponseCleaner.CleanedResponse cleaned = responseCleaner.cleanAndParse(llmResponse);
            if (cleaned.hasError()) {
                log.warn("Response cleaning error: {}", cleaned.getError());
                return List.of();
            }

            String cleanJson = cleaned.getJson().toString();
            if (cleanJson.trim().startsWith("[")) {
                // Обработка массива JSON
                return objectMapper.readValue(cleanJson,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Relation.class));
            } else {
                // Обработка нескольких JSON объектов в одной строке
                return Arrays.stream(cleanJson.split("(?<=})"))
                        .filter(s -> s.trim().startsWith("{"))
                        .map(json -> parseSingleRelation(json))
                        .filter(Objects::nonNull)
                        .toList();
            }
        } catch (Exception e) {
            log.error("Failed to parse relations from response: {}", llmResponse, e);
            return List.of();
        }
    }

    private Relation parseSingleRelation(String json) {
        try {
            return objectMapper.readValue(json, Relation.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse relation from JSON: {}", json);
            return null;
        }
    }

    private List<Relation> filterUniqueRelations(List<Relation> relations) {
        return relations.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(r ->
                        r.source() + r.target() + r.relation() + r.category()))
                .values()
                .stream()
                .map(group -> group.get(0))
                .toList();
    }
}