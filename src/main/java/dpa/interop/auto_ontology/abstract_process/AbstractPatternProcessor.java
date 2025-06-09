package dpa.interop.auto_ontology.abstract_process;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dpa.interop.auto_ontology.abstract_process.dto.AbstractPattern;
import dpa.interop.auto_ontology.lmstudio_connect.LLMAssistant;
import dpa.interop.auto_ontology.prompt_process.PromptService;
import dpa.interop.auto_ontology.relation_process.dto.Relation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


//  старая версия, с ошибкой
@Service
@Slf4j
public class AbstractPatternProcessor {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern JSON_PATTERN = Pattern.compile("\\{.*?\\}", Pattern.DOTALL);

    /**
     * Извлекает все JSON-ответы из списка результатов
     */
    public static List<AbstractPattern> extractPatterns(List<String> llmResponses) {
        return llmResponses.stream()
                .map(AbstractPatternProcessor::extractJsonFromResponse)
                .filter(Objects::nonNull)
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, AbstractPattern.class);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to parse pattern JSON: {}", json, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Формирует строку с уникальными шаблонами для следующего запроса
     */
    public static String buildPatternListString(List<AbstractPattern> patterns) {
        return patterns.stream()
                .map(AbstractPattern::abstractPattern)
                .distinct()
                .collect(Collectors.joining(", "));
    }

    /**
     * Извлекает JSON из ответа LLM (игнорируя текстовые комментарии)
     */
    private static String extractJsonFromResponse(String response) {
        Matcher matcher = JSON_PATTERN.matcher(response);
        return matcher.find() ? matcher.group() : null;
    }

    /**
     * Обновленный метод для выполнения итеративных запросов
     */
    public static List<AbstractPattern> processRelationsIteratively(
            List<Relation> relations,
            String paragraph,
            LLMAssistant llmAssistant,
            PromptService promptService) throws IOException {

        List<AbstractPattern> allPatterns = new ArrayList<>();
        String[] currentPatternsHolder = {""}; // Используем массив как контейнер

        for (int i = 0; i < 3; i++) {
            String sysPr = promptService.buildSystemPrompt("abstract_system_deep");
            List<String> responses = new ArrayList<>();

            // Создаем локальную копию для использования в лямбде
            final String currentPatternsForIteration = currentPatternsHolder[0];

            List<CompletableFuture<Void>> futures = relations.stream()
                    .map(relation -> {
                        String userPr = null;
                        try {
                            userPr = promptService.buildUserPromptForRelationAbstraction(
                                    "abstract_user_deep", paragraph, relation, currentPatternsForIteration);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        return llmAssistant.getLLMResponseAsync(sysPr, userPr)
                                .thenAccept(response -> {
                                    synchronized (responses) {
                                        responses.add(response);
                                        log.info("Processed relation: {}", relation);
                                    }
                                })
                                .exceptionally(e -> {
                                    log.error("Error processing relation: {}", relation, e);
                                    return null;
                                });
                    })
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            List<AbstractPattern> newPatterns = extractPatterns(responses);
            allPatterns.addAll(newPatterns);
            currentPatternsHolder[0] = buildPatternListString(allPatterns);

            log.info("Iteration {} complete. Current patterns: {}", i + 1, currentPatternsHolder[0]);
        }

        return allPatterns.stream()
                .collect(Collectors.groupingBy(AbstractPattern::abstractPattern))
                .values().stream()
                .map(patterns -> patterns.get(0))
                .toList();
    }

    public List<AbstractPattern> processRelationsWithAccumulatedPatterns(
            List<Relation> relations,
            String text,
            LLMAssistant llmAssistant,
            PromptService promptService) throws IOException {

        List<AbstractPattern> accumulatedPatterns = new ArrayList<>();
        String currentPatternList = "";
        String systemPrompt = promptService.buildSystemPrompt("abstract_system_deep");

        // Обрабатываем последовательно для накопления шаблонов
        for (Relation relation : relations) {
            String userPrompt = promptService.buildUserPromptForRelationAbstraction(
                    "abstract_user_deep",
                    text,
                    relation,
                    currentPatternList);

            try {
                String response = llmAssistant.getLLMResponseAsync(systemPrompt, userPrompt)
                        .get();

                AbstractPattern newPattern = extractSinglePattern(response);
                if (newPattern != null) {
                    accumulatedPatterns.add(newPattern);
                    currentPatternList = buildPatternListString(accumulatedPatterns);
                    log.info("Added pattern: {}", newPattern.abstractPattern());
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error("Failed to process relation: {}", relation, e);
            }
        }

        return accumulatedPatterns;
    }

    private static AbstractPattern extractSinglePattern(String llmResponse) {
        try {
            // Удаляем все не-JSON части
            String json = llmResponse.replaceAll("(?s)<think>.*?</think>", "")
                    .replaceAll("//.*", "")
                    .trim();

            // Извлекаем самый внутренний JSON объект
            Matcher matcher = Pattern.compile("\\{(?:[^{}]|\\{(?:[^{}]|\\{[^{}]*\\})*\\})").matcher(json);
            if (matcher.find()) {
                String pureJson = matcher.group();
                ObjectMapper mapper = new ObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

                return mapper.readValue(pureJson, AbstractPattern.class);
            }
        } catch (Exception e) {
            log.error("Pattern extraction failed from response:\n{}\nError: {}", llmResponse, e.getMessage());
        }
        return null;
    }
}