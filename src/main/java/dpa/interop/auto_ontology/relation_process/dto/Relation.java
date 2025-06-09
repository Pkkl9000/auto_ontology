package dpa.interop.auto_ontology.relation_process.dto;

import jakarta.annotation.Nullable;

public record Relation(
        String source,
        String target,
        String relation,
        @Nullable String attribute,
        String statement,
        String category
) {}
