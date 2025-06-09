package dpa.interop.auto_ontology.relation_process;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dpa.interop.auto_ontology.relation_process.dto.Relation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class RelationResponseParser {

    private final ObjectMapper objectMapper;
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{.*?\\}", Pattern.DOTALL);

    /**
     * Основной метод для обработки ответов LLM
     */
    public List<Relation> parseLlmResponse(String llmResponse) {
        String cleanedResponse = cleanFullResponse(llmResponse);

        try {
            return objectMapper.readValue(cleanedResponse, new TypeReference<List<Relation>>() {});
        } catch (JsonProcessingException e) {
            return extractJsonObjects(cleanedResponse).stream()
                    .map(this::parseSingleJson)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Очищает полный ответ LLM от лишних элементов
     */
    private String cleanFullResponse(String dirtyResponse) {
        return dirtyResponse
                .replaceAll("(?s)<think>.*?</think>", "") // Удаляем блоки <think>
                .replaceAll("/\\*.*?\\*/", "")            // Удаляем /* комментарии */
                .replaceAll("//.*", "")                   // Удаляем // комментарии
                .replaceAll("(?s)<.*?>", "")              // Удаляем другие HTML-теги
                .replaceAll("^\\s*\\[|\\]\\s*$", "")      // Удаляем окружающие квадратные скобки
                .trim();
    }

    /**
     * Извлекает отдельные JSON объекты из текста
     */
    private List<String> extractJsonObjects(String text) {
        List<String> jsonObjects = new ArrayList<>();
        Matcher matcher = JSON_OBJECT_PATTERN.matcher(text);

        while (matcher.find()) {
            jsonObjects.add(matcher.group());
        }

        return jsonObjects;
    }

    /**
     * Парсит один JSON объект с обработкой ошибок
     */
    private Relation parseSingleJson(String json) {
        try {
            String cleanJson = json.trim();
            if (cleanJson.endsWith(",")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 1);
            }
            return objectMapper.readValue(cleanJson, Relation.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse JSON: " + json, e);
        }
    }

    /**
     * Валидирует Relation DTO
     */
    private void validateRelation(Relation relation) {
        if (relation.source() == null || relation.source().isBlank()) {
            throw new IllegalArgumentException("Relation source cannot be empty");
        }
        if (relation.target() == null || relation.target().isBlank()) {
            throw new IllegalArgumentException("Relation target cannot be empty");
        }
    }
}