package dpa.interop.auto_ontology.abstract_process;

import com.fasterxml.jackson.databind.ObjectMapper;
import dpa.interop.auto_ontology.abstract_process.dto.AbstractPattern;
import dpa.interop.auto_ontology.db_layers.service.AbstractPatternDbService;
import dpa.interop.auto_ontology.lmstudio_connect.LLMAssistant;
import dpa.interop.auto_ontology.prompt_process.PromptService;
import dpa.interop.auto_ontology.relation_process.dto.Relation;
import dpa.interop.auto_ontology.lmstudio_connect.LLMResponseCleaner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AbstractPatternExtractionService {

    private final LLMAssistant llmAssistant;
    private final PromptService promptService;
    private final LLMResponseCleaner responseCleaner;
    private final ObjectMapper objectMapper;
    private final AbstractPatternDbService patternService;

    private static final String SYSTEM_PROMPT_NAME = "abstract_system_deep";
    private static final String USER_PROMPT_NAME = "abstract_user_deep";

    public List<AbstractPattern> extractPatterns(String text, List<Relation> relations) {
        List<AbstractPattern> accumulatedPatterns = new ArrayList<>(
                patternService.getAllApproved()
        );

        String currentPatternList = buildPatternListString(accumulatedPatterns);
        String systemPrompt = buildSystemPrompt();

        log.info("Initial patterns loaded from DB: {}", currentPatternList);

        for (Relation relation : relations) {
            try {
                String userPrompt = promptService.buildUserPromptForRelationAbstraction(
                        USER_PROMPT_NAME,
                        text,
                        relation,
                        currentPatternList
                );

                log.info("Generated user prompt for relation {}:\n{}",
                        relation, userPrompt);

                String response = llmAssistant.getLLMResponseAsync(systemPrompt, userPrompt).get();

                log.debug("LLM raw response for relation {}:\n{}",
                        relation, response);

                AbstractPattern pattern = parseSinglePattern(response);

                if (pattern != null) {
                    if (!accumulatedPatterns.stream()
                            .anyMatch(p -> p.abstractPattern().equals(pattern.abstractPattern()))) {

                        accumulatedPatterns.add(pattern);
                        currentPatternList = buildPatternListString(accumulatedPatterns);

                        log.info("Added new pattern: {} (Total: {})",
                                pattern.abstractPattern(),
                                accumulatedPatterns.size());
                    } else {
                        log.debug("Pattern already exists: {}", pattern.abstractPattern());
                    }
                }
            } catch (Exception e) {
                log.error("Error processing relation {}: {}", relation, e.getMessage());
            }
        }

        return filterUniquePatterns(accumulatedPatterns);
    }

    private String buildSystemPrompt() {
        try {
            return promptService.buildSystemPrompt(SYSTEM_PROMPT_NAME);
        } catch (IOException e) {
            throw new RuntimeException("Failed to build system prompt", e);
        }
    }

    private AbstractPattern parseSinglePattern(String llmResponse) {
        try {
            LLMResponseCleaner.CleanedResponse cleaned = responseCleaner.cleanAndParse(llmResponse);
            if (cleaned.hasError()) {
                log.warn("Failed to clean response: {}", cleaned.getError());
                return null;
            }

            String json = cleaned.getJson().toString();
            return objectMapper.readValue(json, AbstractPattern.class);
        } catch (Exception e) {
            log.error("Failed to parse pattern from response: {}", llmResponse, e);
            return null;
        }
    }

    private String buildPatternListString(List<AbstractPattern> patterns) {
        return patterns.stream()
                .map(AbstractPattern::abstractPattern)
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private List<AbstractPattern> filterUniquePatterns(List<AbstractPattern> patterns) {
        return patterns.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(AbstractPattern::abstractPattern))
                .values()
                .stream()
                .map(group -> group.get(0))
                .toList();
    }
}