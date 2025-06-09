package dpa.interop.auto_ontology.db_layers.service;

import dpa.interop.auto_ontology.db_layers.entity.RelationEntity;
import dpa.interop.auto_ontology.relation_process.dto.Relation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RelationService {
    List<RelationEntity> processAndSaveRelations(List<Relation> relations);
    boolean strictRelationExists(Relation relation);
    List<Relation> getAllRelations();
    Page<Relation> getRelationsPage(Pageable pageable);
    List<Relation> getRelationsBySource(String source);
    List<Relation> getRelationsByCategory(String category);
}
