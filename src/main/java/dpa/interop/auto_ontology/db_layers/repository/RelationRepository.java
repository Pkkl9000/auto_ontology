package dpa.interop.auto_ontology.db_layers.repository;

import dpa.interop.auto_ontology.db_layers.entity.RelationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RelationRepository extends JpaRepository<RelationEntity, Long> {

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM RelationEntity r WHERE " +
            "r.source = :source AND " +
            "r.target = :target AND " +
            "r.relation = :relation AND " +
            "r.statement = :statement")
    boolean existsBySourceAndTargetAndRelationAndStatement(
            @Param("source") String source,
            @Param("target") String target,
            @Param("relation") String relation,
            @Param("statement") String statement);

    List<RelationEntity> findBySource(String source);
    List<RelationEntity> findByCategory(String category);
}