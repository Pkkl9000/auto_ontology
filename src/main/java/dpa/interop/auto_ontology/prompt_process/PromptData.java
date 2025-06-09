package dpa.interop.auto_ontology.prompt_process;

import java.util.Map;
import java.util.Objects;


/**
 * Контейнер для данных промпта
 * @param isSystem Флаг системного промпта
 * @param templateName Имя шаблона (без расширения)
 * @param placeholders Значения для подстановки (только для пользовательских промптов)
 */
public record PromptData(
        boolean isSystem,
        String templateName,
        Map<String, Object> placeholders
) {
    public PromptData {
        Objects.requireNonNull(templateName, "Template name must not be null");
        placeholders = placeholders != null ? Map.copyOf(placeholders) : Map.of();
    }
}