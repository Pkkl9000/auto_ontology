package dpa.interop.auto_ontology.db_layers.repository;

import dpa.interop.auto_ontology.db_layers.entity.AbstractPatternEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AbstractPatternRepository extends JpaRepository<AbstractPatternEntity, Long> {

    boolean existsByAbstractPatternAndIsApproved(String pattern, boolean isApproved);

    @Query("""
        SELECT COUNT(p) > 0 FROM AbstractPatternEntity p 
        WHERE p.abstractPattern = :pattern 
        AND p.isApproved = :isApproved
        AND (
            SELECT COUNT(c1) FROM p.category c1 
            WHERE c1 IN (SELECT c2 FROM p.category c2 WHERE c2 IN :categories)
        ) = (SELECT COUNT(DISTINCT c3) FROM p.category c3)
        AND (
            SELECT COUNT(DISTINCT c4) FROM p.category c4 
            WHERE c4 IN :categories
        ) = (SELECT COUNT(DISTINCT c5) FROM p.category c5)
    """)
    boolean existsSimilar(
            @Param("pattern") String pattern,
            @Param("categories") List<String> categories,
            @Param("isApproved") boolean isApproved
    );

    List<AbstractPatternEntity> findByIsApproved(boolean isApproved);

    @Modifying
    @Query("DELETE FROM AbstractPatternEntity p WHERE p.isApproved = :isApproved")
    void deleteAllByApprovalStatus(@Param("isApproved") boolean isApproved);

    long countByIsApproved(boolean isApproved);
}