package com.greenpastures.tagq.service.mapper;

import com.greenpastures.tagq.repository.entity.TagEntity;
import com.greenpastures.tagq.web.dto.TagBody;
import com.greenpastures.tagq.web.dto.TagDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;


@Mapper
public interface TagMapper {
    TagMapper INSTANCE = Mappers.getMapper(TagMapper.class);


    TagEntity idAndTagBodyToTagEntity(Integer id, TagBody tagBody);

    @Mapping(target = "questionCount", expression = "java(tag.getQuestions().size())")
    TagDTO tagEntityToTagDTO(TagEntity tag);

    List<TagDTO> tagEntitiesToTagDTOs(List<TagEntity> tagEntities);

    default String tagEntityToString(TagEntity tagEntity) {
        return tagEntity != null ? tagEntity.getTag() : null;
    }

}