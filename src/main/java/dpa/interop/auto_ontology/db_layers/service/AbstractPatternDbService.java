package dpa.interop.auto_ontology.db_layers.service;

import dpa.interop.auto_ontology.abstract_process.dto.AbstractPattern;

import java.util.List;

public interface AbstractPatternDbService {
    List<AbstractPattern> saveCandidatePatterns(List<AbstractPattern> patterns);
    List<AbstractPattern> approvePatterns(List<Long> ids);
    List<AbstractPattern> getAllApproved();
    List<AbstractPattern> getAllCandidates();
    void purgeRejectedPatterns();
}