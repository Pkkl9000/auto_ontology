package dpa.interop.auto_ontology.db_layers.service.impl;

import dpa.interop.auto_ontology.abstract_process.dto.AbstractPattern;
import dpa.interop.auto_ontology.db_layers.entity.AbstractPatternEntity;
import dpa.interop.auto_ontology.db_layers.mapper.AbstractPatternMapper;
import dpa.interop.auto_ontology.db_layers.repository.AbstractPatternRepository;
import dpa.interop.auto_ontology.db_layers.service.AbstractPatternDbService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AbstractPatternDbServiceImpl implements AbstractPatternDbService {

    private final AbstractPatternRepository repository;
    private final AbstractPatternMapper patternMapper;

    @Override
    @Transactional
    public List<AbstractPattern> saveCandidatePatterns(List<AbstractPattern> patterns) {
        log.info("Saving {} candidate patterns", patterns.size());

        List<AbstractPatternEntity> newEntities = patterns.stream()
                .filter(p -> !strictPatternExists(p))
                .peek(p -> log.debug("Saving new pattern: {}", p.abstractPattern()))
                .map(patternMapper::toEntity)
                .toList();

        if (!newEntities.isEmpty()) {
            log.info("Persisting {} new patterns", newEntities.size());
            return patternMapper.toDtos(repository.saveAll(newEntities));
        }

        log.info("No new patterns to save");
        return Collections.emptyList();
    }

    private boolean strictPatternExists(AbstractPattern pattern) {
        return repository.existsByAbstractPatternAndIsApproved(
                pattern.abstractPattern(),
                pattern.isApproved()
        ) || repository.existsSimilar(
                pattern.abstractPattern(),
                pattern.category(),
                pattern.isApproved()
        );
    }

    @Override
    @Transactional
    public List<AbstractPattern> approvePatterns(List<Long> ids) {
        log.info("Approving {} patterns", ids.size());

        List<AbstractPatternEntity> toApprove = repository.findAllById(ids);
        toApprove.forEach(e -> {
            e.setApproved(true);
            log.debug("Approving pattern ID: {}", e.getId());
        });

        return patternMapper.toDtos(repository.saveAll(toApprove));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AbstractPattern> getAllApproved() {
        log.debug("Fetching all approved patterns");
        return patternMapper.toDtos(repository.findByIsApproved(true));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AbstractPattern> getAllCandidates() {
        log.debug("Fetching all candidate patterns");
        return patternMapper.toDtos(repository.findByIsApproved(false));
    }

    @Override
    @Transactional
    public void purgeRejectedPatterns() {
        long count = repository.countByIsApproved(false);
        log.info("Purging {} rejected patterns", count);
        repository.deleteAllByApprovalStatus(false);
    }
}