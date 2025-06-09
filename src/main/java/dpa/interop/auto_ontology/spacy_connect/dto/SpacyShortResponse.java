package dpa.interop.auto_ontology.spacy_connect.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SpacyShortResponse {
    private String status;
    private List<Token> tokens;

    @Getter
    @Setter
    public static class Token {
        @JsonProperty("children")
        private List<String> children;

        @JsonProperty("dep")
        private String dependencyTag;

        @JsonProperty("head")
        private String head;

        @JsonProperty("pos")
        private String partOfSpeech;

        @JsonProperty("text")
        private String text;

        @Override
        public String toString() {
            return "Token{" +
                    "text='" + text + '\'' +
                    ", pos='" + partOfSpeech + '\'' +
                    ", dep='" + dependencyTag + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "SpacyShortResponse{" +
                "status='" + status + '\'' +
                ", tokens=" + tokens +
                '}';
    }
}