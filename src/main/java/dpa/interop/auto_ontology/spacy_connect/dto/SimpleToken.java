package dpa.interop.auto_ontology.spacy_connect.dto;

import lombok.Value;

@Value
public class SimpleToken {
    String text;
    String pos;
    String dep;
}
