package dpa.interop.auto_ontology.db_layers.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Getter;
import lombok.Setter;


import java.util.List;

@Entity
@Table(name = "abstract_patterns",
        indexes = @Index(columnList = "abstractPattern, isApproved"))
@Getter
@Setter
public class AbstractPatternEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String abstractPattern;

    @ElementCollection
    @CollectionTable(name = "pattern_categories")
    private List<String> category;

    private double confidence;
    private String reason;
    private boolean isNew;

    @Column(nullable = false)
    private boolean isApproved = false; // false=Candidate, true=Approved
}
