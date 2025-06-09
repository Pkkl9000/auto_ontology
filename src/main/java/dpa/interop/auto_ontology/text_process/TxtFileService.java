package dpa.interop.auto_ontology.text_process;

import dpa.interop.auto_ontology.txt_file_process.FileSystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class TxtFileService {
    private static final String CONTENT_DIRECTORY = "content/";
    private static final Pattern SENTENCE_END_PATTERN = Pattern.compile("[.!?]\\s*$");
    private static final int READ_AHEAD_LIMIT = 1024;
    private final FileSystemService fileSystemService;

    /**
     * Reads paragraphs from a text file in content directory
     *
     * @param fileName Name of the file without extension
     * @return List of paragraphs
     * @throws IOException If file doesn't exist or can't be read
     */
    public List<String> extractParagraphsFromFile(String fileName) throws IOException {
        String fileContent = fileSystemService.readFileContent(CONTENT_DIRECTORY, fileName);
        log.debug("Extracting paragraphs from file: {}", fileName);
        return extractParagraphs(fileContent);
    }

    public List<String> splitTextIntoSentences(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> sentences = new ArrayList<>();
        StringBuilder currentSentence = new StringBuilder();
        String[] words = text.split("\\s+");

        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }

            currentSentence.append(word).append(" ");

            if (SENTENCE_END_PATTERN.matcher(word).find()) {
                sentences.add(currentSentence.toString().trim());
                currentSentence.setLength(0);
            }
        }

        if (currentSentence.length() > 0) {
            sentences.add(currentSentence.toString().trim());
        }

        return sentences;
    }

    public List<String> extractParagraphs(String text) throws IOException {
        List<String> paragraphs = new ArrayList<>();
        StringBuilder currentParagraph = new StringBuilder();
        boolean previousLineEndedWithHyphen = false;

        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = processLine(line, previousLineEndedWithHyphen, currentParagraph);
                previousLineEndedWithHyphen = line.endsWith("-");

                if (isParagraphEnd(reader, line)) {
                    paragraphs.add(currentParagraph.toString());
                    currentParagraph.setLength(0);
                    previousLineEndedWithHyphen = false;
                }
            }

            addRemainingParagraph(paragraphs, currentParagraph);
        }

        return paragraphs;
    }

    private String processLine(String line, boolean previousLineEndedWithHyphen, StringBuilder currentParagraph) {
        boolean endsWithHyphen = line.endsWith("-");
        String processedLine = endsWithHyphen ? line.substring(0, line.length() - 1) : line;

        if (currentParagraph.length() > 0 && !previousLineEndedWithHyphen) {
            currentParagraph.append(" ");
        }

        currentParagraph.append(processedLine.trim());
        return line;
    }

    private boolean isParagraphEnd(BufferedReader reader, String currentLine) throws IOException {
        boolean sentenceEnd = SENTENCE_END_PATTERN.matcher(currentLine).find();
        if (!sentenceEnd || !reader.ready()) {
            return false;
        }

        reader.mark(READ_AHEAD_LIMIT);
        String nextLine = reader.readLine();
        reader.reset();

        return nextLine != null && !nextLine.trim().isEmpty();
    }

    private void addRemainingParagraph(List<String> paragraphs, StringBuilder currentParagraph) {
        if (currentParagraph.length() > 0) {
            paragraphs.add(currentParagraph.toString());
        }
    }

    public String listToCommaSeparatedString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return String.join(", ", list);
    }
}
