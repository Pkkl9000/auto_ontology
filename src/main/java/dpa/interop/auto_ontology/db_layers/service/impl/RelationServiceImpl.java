package dpa.interop.auto_ontology.db_layers.service.impl;

import dpa.interop.auto_ontology.db_layers.entity.RelationEntity;
import dpa.interop.auto_ontology.db_layers.mapper.RelationMapper;
import dpa.interop.auto_ontology.db_layers.repository.RelationRepository;
import dpa.interop.auto_ontology.db_layers.service.RelationService;
import dpa.interop.auto_ontology.relation_process.dto.Relation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RelationServiceImpl implements RelationService {
    private final RelationRepository repository;
    private final RelationMapper relationMapper;

    @Override
    @Transactional
    public List<RelationEntity> processAndSaveRelations(List<Relation> relations) {
        log.info("Processing {} relations", relations.size());

        List<RelationEntity> newEntities = relations.stream()
                .filter(dto -> !strictRelationExists(dto))
                .peek(dto -> log.debug("Will save new relation: {}", dto))
                .map(relationMapper::toEntity)
                .toList();

        if (!newEntities.isEmpty()) {
            log.info("Saving {} new relations", newEntities.size());
            return repository.saveAll(newEntities);
        }

        log.info("No new relations to save");
        return Collections.emptyList();
    }

    @Transactional
    public RelationEntity saveSingleRelation(Relation relation) {
        log.debug("Attempting to save relation: {}", relation);

        if (strictRelationExists(relation)) {
            log.warn("Relation already exists: {}", relation);
            throw new IllegalStateException("Relation already exists");
        }

        RelationEntity entity = relationMapper.toEntity(relation);
        RelationEntity saved = repository.save(entity);
        log.info("Saved new relation with id: {}", saved.getId());
        return saved;
    }

    @Override
    public boolean strictRelationExists(Relation relation) {
        return repository.existsBySourceAndTargetAndRelationAndStatement(
                relation.source(),
                relation.target(),
                relation.relation(),
                relation.statement()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Relation> getAllRelations() {
        log.debug("Fetching all relations from database");
        List<RelationEntity> entities = repository.findAll();
        return relationMapper.toDtos(entities); // Используем метод для преобразования списка
    }

    @Override
    @Transactional(readOnly = true)
    public List<Relation> getRelationsBySource(String source) {
        log.debug("Fetching relations by source: {}", source);
        List<RelationEntity> entities = repository.findBySource(source);
        return relationMapper.toDtos(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Relation> getRelationsByCategory(String category) {
        log.debug("Fetching relations by category: {}", category);
        List<RelationEntity> entities = repository.findByCategory(category);
        return relationMapper.toDtos(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Relation> getRelationsPage(Pageable pageable) {
        log.debug("Fetching relations page: {}", pageable);
        return repository.findAll(pageable)
                .map(relationMapper::toDto);
    }
}