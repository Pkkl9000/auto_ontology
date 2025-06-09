package dpa.interop.auto_ontology.spacy_connect.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class SpacyFullResponse {
    private String status;
    private List<Token> tokens;
    private List<Entity> entities;
    @JsonProperty("noun_chunks")
    private List<NounChunk> nounChunks;
    @JsonProperty("sentence_count")
    private int sentenceCount;

    @Data
    public static class Token {
        private String text;
        private String lemma;
        private String pos;
        private String tag;
        private String dep;
        private String shape;
        @JsonProperty("is_alpha")
        private boolean alpha;
        @JsonProperty("is_stop")
        private boolean stop;
        private String head;
        @JsonProperty("head_pos")
        private String headPos;
        private List<String> children;
        @JsonProperty("ent_type")
        private String entType;
        @JsonProperty("ent_iob")
        private String entIob;
        @JsonDeserialize(using = MorphologyDeserializer.class)
        private Map<String, String> morph;
        /**
         * Кастомный десериализатор для преобразования строки морфологии в Map
         */
        public static class MorphologyDeserializer extends JsonDeserializer<Map<String, String>> {
            @Override
            public Map<String, String> deserialize(JsonParser p, DeserializationContext ctxt)
                    throws IOException {

                String morphStr = p.getValueAsString();
                if (morphStr == null || morphStr.isEmpty() || "null".equals(morphStr)) {
                    return Collections.emptyMap();
                }

                try {
                    return Arrays.stream(morphStr.split("\\|"))
                            .map(entry -> entry.split("=", 2))
                            .filter(parts -> parts.length == 2)
                            .collect(Collectors.toMap(
                                    parts -> parts[0],
                                    parts -> parts[1],
                                    (existing, replacement) -> existing
                            ));
                } catch (Exception e) {
                    throw new IOException("Failed to parse morphology: " + morphStr, e);
                }
            }
        }
    }

    @Data
    public static class Entity {
        private String text;
        private String label;
        private int start;
        private int end;
    }

    @Data
    public static class NounChunk {
        private String text;
        private String root;
    }
}