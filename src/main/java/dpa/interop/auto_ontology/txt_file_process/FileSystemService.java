package dpa.interop.auto_ontology.txt_file_process;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class FileSystemService {
    private static final String FILE_EXTENSION = ".txt";

    /**
     * Reads file content from specified directory
     *
     * @param directory Directory path (relative to resources)
     * @param fileName Name of the file without extension
     * @return Content of the file as String
     * @throws IOException If file doesn't exist or can't be read
     */
    public String readFileContent(String directory, String fileName) throws IOException {
        String filePath = buildFilePath(directory, fileName);
        log.debug("Reading file from path: {}", filePath);

        ClassPathResource resource = new ClassPathResource(filePath);
        validateResourceExists(resource, filePath);

        return readResourceContent(resource);
    }

    private String buildFilePath(String directory, String fileName) {
        return directory + fileName + FILE_EXTENSION;
    }

    private void validateResourceExists(ClassPathResource resource, String filePath) throws IOException {
        if (!resource.exists()) {
            log.error("File not found: {}", filePath);
            throw new IOException("File not found: " + filePath);
        }
    }

    private String readResourceContent(ClassPathResource resource) throws IOException {
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
