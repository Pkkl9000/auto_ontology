package dpa.interop.auto_ontology.db_layers.mapper;

import dpa.interop.auto_ontology.abstract_process.dto.AbstractPattern;
import dpa.interop.auto_ontology.db_layers.entity.AbstractPatternEntity;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Mapper(componentModel = "spring")
@Component
public interface AbstractPatternMapper {

    AbstractPatternEntity toEntity(AbstractPattern dto);
    AbstractPattern toDto(AbstractPatternEntity entity);

    default List<AbstractPatternEntity> toEntities(List<AbstractPattern> dtos) {
        return dtos.stream().map(this::toEntity).toList();
    }

    default List<AbstractPattern> toDtos(List<AbstractPatternEntity> entities) {
        return entities.stream().map(this::toDto).toList();
    }
}