package dpa.interop.auto_ontology.db_layers.mapper;

import dpa.interop.auto_ontology.db_layers.entity.RelationEntity;
import dpa.interop.auto_ontology.relation_process.dto.Relation;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Mapper(componentModel = "spring")
@Component
public interface RelationMapper {
    RelationEntity toEntity(Relation dto);
    Relation toDto(RelationEntity entity);

    default List<RelationEntity> toEntities(List<Relation> dtos) {
        return dtos.stream().map(this::toEntity).toList();
    }

    default List<Relation> toDtos(List<RelationEntity> entities) {
        return entities.stream().map(this::toDto).toList();
    }
}