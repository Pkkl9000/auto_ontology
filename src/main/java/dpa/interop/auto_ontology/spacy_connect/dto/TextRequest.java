package dpa.interop.auto_ontology.spacy_connect.dto;

public class TextRequest {
    private String text;
    public TextRequest(String text) {
        this.text = text;
    }
    public String getText() {
        return text;
    }
}
