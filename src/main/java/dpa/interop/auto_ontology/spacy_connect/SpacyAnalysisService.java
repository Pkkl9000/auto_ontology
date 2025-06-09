package dpa.interop.auto_ontology.spacy_connect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dpa.interop.auto_ontology.exception.AnalysisException;
import dpa.interop.auto_ontology.spacy_connect.dto.SimpleToken;
import dpa.interop.auto_ontology.spacy_connect.dto.SpacyFullResponse;
import dpa.interop.auto_ontology.spacy_connect.dto.SpacyShortResponse;
import dpa.interop.auto_ontology.spacy_connect.dto.SpacyTreeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class SpacyAnalysisService {
    private static final String SUCCESS_STATUS = "success";
    private static final int MAX_TEXT_LOG_LENGTH = 50;
    private static final int MAX_ERROR_TEXT_LOG_LENGTH = 100;

    private final SpacyClientOkHttp spacyClient;
    private final ObjectMapper objectMapper;

    /**
     * Performs quick text analysis using Spacy service
     *
     * @param text Text to analyze
     * @param verbose Flag to enable detailed logging of the response
     * @return Analysis result as SpacyShortResponse
     * @throws AnalysisException if analysis fails or service returns non-success status
     */
    @Cacheable("spacy-analyses")
    public SpacyShortResponse quickAnalyze(String text, boolean verbose) throws AnalysisException {
        try {
            log.debug("Starting quick analysis for text: {}", abbreviate(text, MAX_TEXT_LOG_LENGTH));

            SpacyShortResponse response = spacyClient.getShortAnalysis(text);

            if (!SUCCESS_STATUS.equalsIgnoreCase(response.getStatus())) {
                throw new AnalysisException("Spacy service returned non-success status: " + response.getStatus());
            }

            if (verbose) {
                logSpacyShortResponse(response);
            }

            log.debug("Analysis completed. Tokens found: {}", response.getTokens().size());
            return response;

        } catch (IOException ex) {
            log.error("Spacy analysis failed for text: {}", abbreviate(text, MAX_TEXT_LOG_LENGTH), ex);
            throw new AnalysisException("Spacy service unavailable", ex);
        }
    }

    /**
     * Logs detailed information about SpacyShortResponse
     *
     * @param response Response to log
     */
    private void logSpacyShortResponse(SpacyShortResponse response) {
        StringBuilder output = new StringBuilder("SpacyShortResponse:\n")
                .append("Status: ").append(response.getStatus()).append("\n")
                .append("Tokens:\n");

        response.getTokens().forEach(token ->
                output.append("  - Text: ").append(token.getText())
                        .append(", POS: ").append(token.getPartOfSpeech())
                        .append(", Dependency: ").append(token.getDependencyTag())
                        .append("\n")
        );

        log.info(output.toString());
    }

    /**
     * Performs full text analysis with maximum information
     *
     * @param text Text to analyze
     * @param verbose Flag to enable detailed logging of the response
     * @return Complete analysis result as SpacyFullResponse
     * @throws AnalysisException if analysis fails or response is invalid
     */
    @Cacheable("spacy-full-analyses")
    public SpacyFullResponse fullAnalyze(String text, boolean verbose) throws AnalysisException {
        try {
            log.debug("Starting full analysis for text: {}", abbreviate(text, MAX_TEXT_LOG_LENGTH));

            SpacyFullResponse response = spacyClient.getFullAnalysis(text);

            validateFullResponse(response);
            if (verbose) {
                logSpacyFullResponse(response);
            }

            return response;

        } catch (IOException ex) {
            handleAnalysisError(text, ex);
            throw new AnalysisException("Full analysis failed", ex);
        }
    }

    /**
     * Extracts named entities from text
     *
     * @param text Text to analyze
     * @return List of extracted entities
     * @throws AnalysisException if extraction fails
     */
    public List<SpacyFullResponse.Entity> extractEntities(String text) throws AnalysisException {
        try {
            SpacyFullResponse response = fullAnalyze(text, false);
            return response.getEntities();
        } catch (AnalysisException ex) {
            log.error("Entity extraction failed for text: {}", abbreviate(text, MAX_TEXT_LOG_LENGTH), ex);
            throw new AnalysisException("Entity extraction failed", ex);
        }
    }

    /**
     * Extracts noun chunks from text
     *
     * @param text Text to analyze
     * @return List of extracted noun chunks
     * @throws AnalysisException if extraction fails
     */
    public List<SpacyFullResponse.NounChunk> extractNounChunks(String text) throws AnalysisException {
        try {
            SpacyFullResponse response = fullAnalyze(text, false);
            return response.getNounChunks();
        } catch (AnalysisException ex) {
            log.error("Noun chunk extraction failed for text: {}", abbreviate(text, MAX_TEXT_LOG_LENGTH), ex);
            throw new AnalysisException("Noun chunk extraction failed", ex);
        }
    }

    /**
     * Extracts pure nouns from noun chunks in the response
     *
     * @param response Analysis response containing noun chunks
     * @return List of unique nouns in insertion order
     */
    public List<String> extractNouns(SpacyFullResponse response) {
        Set<String> result = new LinkedHashSet<>();
        Set<String> seenWords = new HashSet<>();

        List<SpacyFullResponse.Token> tokens = response.getTokens();

        for (SpacyFullResponse.NounChunk chunk : response.getNounChunks()) {
            String rootText = chunk.getRoot();

            tokens.stream()
                    .filter(token -> rootText.equals(token.getText()))
                    .filter(token -> "NOUN".equals(token.getPos()))
                    .findFirst()
                    .ifPresent(token -> {
                        if (seenWords.add(token.getText())) {
                            result.add(token.getText());
                        }
                    });
        }

        return new ArrayList<>(result);
    }

    /**
     * Validates the full analysis response
     *
     * @param response Response to validate
     * @throws AnalysisException if response is invalid
     */
    private void validateFullResponse(SpacyFullResponse response) throws AnalysisException {
        if (!SUCCESS_STATUS.equalsIgnoreCase(response.getStatus())) {
            throw new AnalysisException("Spacy service returned non-success status: " + response.getStatus());
        }
        if (response.getTokens() == null || response.getTokens().isEmpty()) {
            throw new AnalysisException("No tokens found in response");
        }
    }

    /**
     * Logs detailed information about SpacyFullResponse
     *
     * @param response Response to log
     */
    private void logSpacyFullResponse(SpacyFullResponse response) {
        StringBuilder output = new StringBuilder("\n=== SpaCy Analysis Result ===\n")
                .append(String.format("%-15s: %s%n", "Status", response.getStatus()))
                .append(String.format("%-15s: %d%n", "Tokens", response.getTokens().size()))
                .append(String.format("%-15s: %d%n", "Entities", response.getEntities().size()))
                .append(String.format("%-15s: %d%n", "Noun Chunks", response.getNounChunks().size()))
                .append(String.format("%-15s: %d%n", "Sentences", response.getSentenceCount()));

        appendTokenDetails(output, response);
        appendNounChunkDetails(output, response);
        appendEntityDetails(output, response);

        log.info(output.toString());
    }

    private void appendTokenDetails(StringBuilder output, SpacyFullResponse response) {
        output.append("\n=== Tokens ===\n")
                .append(String.format("%-10s %-10s %-10s %-15s %-10s %-15s %-10s %-10s %-15s %-15s %-20s %-15s %-15s %-30s%n",
                        "Text", "Lemma", "POS", "Tag", "Dep", "Head", "HeadPOS", "Alpha", "Stop", "Shape", "Morphology", "EntType", "EntIOB", "Children"));

        response.getTokens().forEach(token -> {
            String morph = token.getMorph() != null ?
                    token.getMorph().entrySet().stream()
                            .map(e -> e.getKey() + "=" + e.getValue())
                            .collect(Collectors.joining("|")) :
                    "N/A";

            String children = token.getChildren() != null && !token.getChildren().isEmpty() ?
                    String.join(", ", token.getChildren()) :
                    "N/A";

            output.append(String.format("%-10s %-10s %-10s %-15s %-10s %-15s %-10s %-10s %-10s %-15s %-20s %-15s %-15s %-30s%n",
                    token.getText(),
                    token.getLemma(),
                    token.getPos(),
                    token.getTag(),
                    token.getDep(),
                    token.getHead(),
                    token.getHeadPos() != null ? token.getHeadPos() : "N/A",
                    token.isAlpha() ? "Y" : "N",
                    token.isStop() ? "Y" : "N",
                    token.getShape() != null ? token.getShape() : "N/A",
                    morph,
                    token.getEntType() != null ? token.getEntType() : "N/A",
                    token.getEntIob() != null ? token.getEntIob() : "N/A",
                    children));
        });
    }

    private void appendNounChunkDetails(StringBuilder output, SpacyFullResponse response) {
        if (!response.getNounChunks().isEmpty()) {
            output.append("\n=== Noun Chunks ===\n")
                    .append(String.format("%-30s %-30s%n", "Text", "Root"));
            response.getNounChunks().forEach(chunk ->
                    output.append(String.format("%-30s %-30s%n", chunk.getText(), chunk.getRoot()))
            );
        }
    }

    private void appendEntityDetails(StringBuilder output, SpacyFullResponse response) {
        if (!response.getEntities().isEmpty()) {
            output.append("\n=== Named Entities ===\n")
                    .append(String.format("%-30s %-15s %-15s%n", "Text", "Label", "Span"));
            response.getEntities().forEach(entity ->
                    output.append(String.format("%-30s %-15s %d-%d%n",
                            entity.getText(),
                            entity.getLabel(),
                            entity.getStart(),
                            entity.getEnd()))
            );
        }
    }

    /**
     * Converts analysis result to JSON string
     *
     * @param text Text to analyze
     * @return JSON string representation of the analysis result
     * @throws AnalysisException if analysis or serialization fails
     */
    public String quickAnalyzeToJson(String text) throws AnalysisException {
        try {
            SpacyShortResponse response = quickAnalyze(text, false);
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize analysis result for text: {}", abbreviate(text, MAX_TEXT_LOG_LENGTH), ex);
            throw new AnalysisException("Failed to serialize analysis result", ex);
        }
    }

    /**
     * Extracts simplified token information from text
     *
     * @param text Text to analyze
     * @return List of simplified tokens
     * @throws AnalysisException if analysis fails
     */
    public List<SimpleToken> getSimpleTokens(String text) throws AnalysisException {
        return quickAnalyze(text, false).getTokens().stream()
                .map(token -> new SimpleToken(
                        token.getText(),
                        token.getPartOfSpeech(),
                        token.getDependencyTag()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Abbreviates text for logging purposes
     *
     * @param text Text to abbreviate
     * @param maxLength Maximum length before truncation
     * @return Abbreviated text
     */
    private String abbreviate(String text, int maxLength) {
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }

    /**
     * Handles analysis errors with proper logging
     *
     * @param text Text that caused the error
     * @param ex Exception that occurred
     */
    private void handleAnalysisError(String text, Exception ex) {
        log.error("Analysis failed for text: '{}'", abbreviate(text, MAX_ERROR_TEXT_LOG_LENGTH), ex);
    }

    /**
     * Gets the syntax tree for the given text.
     *
     * @param text The text to analyze
     * @param verbose Flag for detailed logging
     * @return Response containing the syntax tree
     * @throws AnalysisException if analysis fails
     */
    @Cacheable("spacy-tree-analyses")
    public SpacyTreeResponse getSyntaxTree(String text, boolean verbose) throws AnalysisException {
        try {
            log.debug("Getting syntax tree for text: {}", abbreviate(text, MAX_TEXT_LOG_LENGTH));

            SpacyTreeResponse response = spacyClient.getSyntaxTree(text);

            if (!SUCCESS_STATUS.equalsIgnoreCase(response.getStatus())) {
                throw new AnalysisException("Spacy service returned non-success status: " + response.getStatus());
            }

            if (verbose) {
                logSyntaxTree(response);
            }

            return response;

        } catch (IOException ex) {
            log.error("Syntax tree analysis failed for text: {}", abbreviate(text, MAX_TEXT_LOG_LENGTH), ex);
            throw new AnalysisException("Syntax tree analysis failed", ex);
        }
    }

    /**
     * Prints the syntax tree to the console in a readable format.
     *
     * @param response Response containing the syntax tree
     */
    public void printSyntaxTree(SpacyTreeResponse response) {
        if (response == null || response.getTree() == null) {
            log.warn("No syntax tree data available");
            return;
        }

        Map<String, SpacyTreeResponse.TreeNode> nodeMap = response.getTree().stream()
                .collect(Collectors.toMap(SpacyTreeResponse.TreeNode::getText, Function.identity()));

        List<SpacyTreeResponse.TreeNode> roots = response.getTree().stream()
                .filter(node -> node.getText().equals(node.getHead()))
                .collect(Collectors.toList());

        if (roots.isEmpty()) {
            roots = response.getTree().stream()
                    .filter(node -> "ROOT".equals(node.getDep()))
                    .collect(Collectors.toList());
        }

        if (roots.isEmpty() && !response.getTree().isEmpty()) {
            roots = List.of(response.getTree().get(0));
        }

        System.out.println("\n=== Syntax Tree ===");
        for (SpacyTreeResponse.TreeNode root : roots) {
            printTreeNode(root, nodeMap, "", true);
        }
    }

    /**
     * Recursively prints the tree node with indentation
     */
    private void printTreeNode(SpacyTreeResponse.TreeNode node,
                               Map<String, SpacyTreeResponse.TreeNode> nodeMap,
                               String indent,
                               boolean isLast) {
        String currentIndent = indent + (isLast ? "└─ " : "├─ ");
        System.out.println(currentIndent + formatTreeNode(node));

        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            String childIndent = indent + (isLast ? "   " : "│  ");
            for (int i = 0; i < node.getChildren().size(); i++) {
                boolean lastChild = i == node.getChildren().size() - 1;
                SpacyTreeResponse.TreeNode child = nodeMap.get(node.getChildren().get(i).getText());
                if (child != null) {
                    printTreeNode(child, nodeMap, childIndent, lastChild);
                }
            }
        }
    }

    /**
     * Formats node information for display
     */
    private String formatTreeNode(SpacyTreeResponse.TreeNode node) {
        return String.format("%s (%s → %s [%s])",
                node.getText(),
                node.getDep(),
                node.getHead(),
                node.getDepExplained() != null ? node.getDepExplained() : "");
    }

    /**
     * Logs syntax tree information
     */
    private void logSyntaxTree(SpacyTreeResponse response) {
        StringBuilder output = new StringBuilder("\n=== Syntax Tree Analysis ===\n")
                .append(String.format("%-15s: %s%n", "Status", response.getStatus()))
                .append(String.format("%-15s: %d%n", "Nodes", response.getTree().size()))
                .append(String.format("%-15s: %d%n", "Sentences", response.getSentenceCount()));

        output.append("\n=== Tree Nodes ===\n")
                .append(String.format("%-15s %-10s %-15s %-15s %-40s%n",
                        "Text", "Dep", "Head", "Head POS", "Dep Explained"));

        response.getTree().forEach(node ->
                output.append(String.format("%-15s %-10s %-15s %-15s %-40s%n",
                        node.getText(),
                        node.getDep(),
                        node.getHead(),
                        node.getHeadPos() != null ? node.getHeadPos() : "N/A",
                        node.getDepExplained() != null ? node.getDepExplained() : "N/A"))
        );

        log.info(output.toString());
    }
}