package dpa.interop.auto_ontology.prompt_process;

import dpa.interop.auto_ontology.txt_file_process.FileSystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Сервис для генерации системных и пользовательских промптов
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicPromptService {
    private static final String SYSTEM_PROMPTS_DIR = "/prompts/system/";
    private static final String USER_PROMPTS_DIR = "/prompts/user/";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(\\w+)}");

    private final FileSystemService fileSystemService;

    /**
     * Генерирует промпт на основе переданных данных
     */
    public String buildPrompt(PromptData promptData) throws IOException {
        validatePromptData(promptData);

        String template = loadTemplate(promptData);

        return promptData.isSystem()
                ? template
                : replacePlaceholders(template, promptData.placeholders());
    }

    private void validatePromptData(PromptData promptData) {
        if (!StringUtils.hasText(promptData.templateName())) {
            throw new IllegalArgumentException("Template name must not be blank");
        }
    }

    private String loadTemplate(PromptData promptData) throws IOException {
        String dir = promptData.isSystem() ? SYSTEM_PROMPTS_DIR : USER_PROMPTS_DIR;
        try {
            return fileSystemService.readFileContent(dir, promptData.templateName());
        } catch (IOException e) {
            log.error("Failed to load {} template: {}",
                    promptData.isSystem() ? "system" : "user",
                    promptData.templateName(), e);
            throw new IOException(String.format(
                    "%s template loading failed: %s",
                    promptData.isSystem() ? "System" : "User",
                    promptData.templateName()), e);
        }
    }

    private String replacePlaceholders(String template, Map<String, Object> placeholders) {
        if (placeholders.isEmpty()) {
            return template;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);

        while (matcher.find()) {
            String placeholder = matcher.group(1);
            Object value = placeholders.getOrDefault(placeholder, "");
            matcher.appendReplacement(result, processValue(value));
        }

        return matcher.appendTail(result).toString();
    }

    private String processValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Iterable<?> iterable) {
            return String.join(", ", toStringList(iterable));
        }
        return value.toString();
    }

    private List<String> toStringList(Iterable<?> iterable) {
        List<String> result = new ArrayList<>();
        iterable.forEach(item -> result.add(item != null ? item.toString() : ""));
        return result;
    }
}