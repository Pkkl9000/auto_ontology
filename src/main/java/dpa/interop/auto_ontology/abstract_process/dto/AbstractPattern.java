package dpa.interop.auto_ontology.abstract_process.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AbstractPattern(
        @JsonProperty("abstract_pattern") String abstractPattern,
        @JsonProperty("category") List<String> category,
        @JsonProperty("confidence") double confidence,
        @JsonProperty("reason") String reason,
        @JsonProperty("is_new") boolean isNew,
        @JsonProperty(value = "is_approved", defaultValue = "false") boolean isApproved
) {
    public AbstractPattern {
        if (category == null) category = List.of();
        if (reason == null) reason = "";
    }
}