package dpa.interop.auto_ontology.prompt_process;

import dpa.interop.auto_ontology.relation_process.dto.Relation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptService {
    private final DynamicPromptService dynamicPromptService;

    /**
     * Генерация системного промпта
     * @param templateName Имя шаблона (без расширения)
     * @return Сгенерированный промпт
     */
    public String buildSystemPrompt(String templateName) throws IOException {
        PromptData promptData = new PromptData(
                true,
                templateName,
                null
        );
        return dynamicPromptService.buildPrompt(promptData);
    }

    /**
     * Генерация пользовательского промпта без параметров
     * @param templateName Имя шаблона (без расширения)
     * @return Сгенерированный промпт
     */
    public String buildUserPrompt(String templateName) throws IOException {
        return buildUserPrompt(templateName, Map.of());
    }

    /**
     * Генерация пользовательского промпта с параметрами
     * @param templateName Имя шаблона (без расширения)
     * @param placeholders Параметры для подстановки
     * @return Сгенерированный промпт
     */
    public String buildUserPrompt(String templateName, Map<String, Object> placeholders) throws IOException {
        PromptData promptData = new PromptData(
                false,
                templateName,
                placeholders
        );
        return dynamicPromptService.buildPrompt(promptData);
    }

    /**
     * Генерация пользовательского промпта с текстом и сущностью
     * (специализированный метод для частого сценария)
     * @param templateName Имя шаблона
     * @param text Текст для подстановки
     * @param subject Сущность для подстановки
     * @return Сгенерированный промпт
     */
    public String buildUserPromptWithTextAndEntity(String templateName, String text, String subject) throws IOException {
        return buildUserPrompt(templateName, Map.of(
                "text", text,
                "subject", subject
        ));
    }

    /**
     * Генерация пользовательского промпта для абстракции отношений
     * @param templateName Имя шаблона
     * @param text Текст для подстановки
     * @param relation Отношение для анализа
     * @param patternList Список абстрактных паттернов (через запятую)
     * @return Сгенерированный промпт
     */
    public String buildUserPromptForRelationAbstraction(
            String templateName,
            String text,
            Relation relation,
            String patternList) throws IOException {

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("text", text);
        placeholders.put("source_entity", relation.source());
        placeholders.put("target_entity", relation.target());
        placeholders.put("specific_verb", relation.relation());
        placeholders.put("attribute", relation.attribute() != null ? relation.attribute() : "");
        placeholders.put("category", relation.category());
        placeholders.put("existing_patterns", patternList);

        return buildUserPrompt(templateName, placeholders);
    }
}