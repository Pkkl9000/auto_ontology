package dpa.interop.auto_ontology;

import dpa.interop.auto_ontology.abstract_process.AbstractPatternExtractionService;
import dpa.interop.auto_ontology.abstract_process.dto.AbstractPattern;
import dpa.interop.auto_ontology.db_layers.service.AbstractPatternDbService;
import dpa.interop.auto_ontology.db_layers.service.RelationService;
import dpa.interop.auto_ontology.relation_process.RelationExtractionService;
import dpa.interop.auto_ontology.relation_process.dto.Relation;
import dpa.interop.auto_ontology.spacy_connect.dto.SpacyFullResponse;
import dpa.interop.auto_ontology.spacy_connect.SpacyAnalysisService;
import dpa.interop.auto_ontology.text_process.TxtFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class AppRunner implements ApplicationRunner {

//  http://localhost:8084/h2-console

    private final SpacyAnalysisService spacyAnalysisService;
    private final TxtFileService txtFileService;
    private final RelationService relationService;
    private final AbstractPatternExtractionService abstractPatternExtractionService;
    private final AbstractPatternDbService abstractPatternDbService;
    private final RelationExtractionService relationExtractionService;


    @Override
    public void run(ApplicationArguments args) throws Exception {


//        Например, доступные методы (не все):

//        Получение текста из файла
//        List<String> content = txtFileService.extractParagraphsFromFile("text");
//        String paragraph = content.get(0);

//        Получение SpaСy-разбора для текста
//        SpacyFullResponse spacyFullResponse = spacyAnalysisService.fullAnalyze(paragraph, false);

//        Получение списка существительных из SpaСy-разбора
//        List<String> subjects = spacyAnalysisService.extractNouns(spacyFullResponse);

//        Определение связей для списка субъектов через LLM
//        List<Relation> relations = relationExtractionService.extractRelations(paragraph, subjects);

//        Получение всех сохранённых связей из базы
//        List<Relation> relationsFromDb = relationService.getAllRelations();

//        Определение абстрактных связей для списка простых связей через LLM
//        List<AbstractPattern> abstractRelations = abstractPatternExtractionService.extractPatterns(paragraph, relationsFromDb);

//        Сохранение абстрактных связей в базе
//        abstractPatternDbService.saveCandidatePatterns(abstractRelations);
    }
}