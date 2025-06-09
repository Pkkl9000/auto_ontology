package dpa.interop.auto_ontology.spacy_connect.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class SpacyTreeResponse {
    private String status;
    @JsonProperty("syntactic_tree")
    private List<TreeNode> tree;
    @JsonProperty("sentence_count")
    private Integer sentenceCount;

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    public static class TreeNode {
        private String text;
        private String dep;
        @JsonProperty("dep_explained")
        private String depExplained;
        private String head;
        @JsonProperty("head_pos")
        private String headPos;
        private List<ChildNode> children;
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    public static class ChildNode {
        private String text;
        private String dep;
    }
}