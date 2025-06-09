package dpa.interop.auto_ontology.db_layers.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "relations")
@Getter
@Setter
public class RelationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private String target;

    @Column(nullable = false)
    private String relation;

    @Column
    private String attribute;

    @Column(nullable = false, length = 1000)
    private String statement;

    @Column(nullable = false)
    private String category;
}
