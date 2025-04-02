package com.greenpastures.tagq.service;

import com.greenpastures.tagq.repository.QuestionRepository;
import com.greenpastures.tagq.repository.TagRepository;
import com.greenpastures.tagq.repository.entity.QuestionEntity;
import com.greenpastures.tagq.repository.entity.TagEntity;
import com.greenpastures.tagq.service.exceptions.NotAcceptException;
import com.greenpastures.tagq.service.mapper.TagMapper;
import com.greenpastures.tagq.web.dto.TagBody;
import com.greenpastures.tagq.web.dto.TagDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TagService {
    private final TagRepository tagRepository;
    private final QuestionRepository questionRepository;
    private static final Logger logger = LoggerFactory.getLogger(TagService.class);


    public String createTag(TagBody tagBody) {
        boolean tagExists = tagRepository.existsByTag(tagBody.getTag());
        if (tagExists) {
            throw new NotAcceptException("Tag with the same name: " + tagBody.getTag() + " already exists.");
        }

        TagEntity tagEntity = TagMapper.INSTANCE.idAndTagBodyToTagEntity(null, tagBody);
        log.info(tagEntity.toString());
        TagEntity tagCreated = tagRepository.save(tagEntity);
        return "ID:"+ tagCreated.getTagId() + " "+tagCreated.getTag();
    }

    public List<String> createTags(List<TagBody> tagBodies) {
      // 이미 존재하는 태그 목록 가져오기
        Set<String> existingTags = tagRepository.findByTagIn(
                tagBodies.stream().map(TagBody::getTag).collect(Collectors.toSet())
                //tagBodies에서 각각의 태그 문자열을 얻기, 얻어서 tagRepository의 FindByTagIn을 통해 태그를 가진 tagEntitySet을 찾음
        ).stream().map(TagEntity::getTag).collect(Collectors.toSet());//반환한 값 결과가 Set<TagEntity>이니깐

        Set<TagEntity> newTagEntities = tagBodies.stream()
                .filter(tagBody -> !existingTags.contains(tagBody.getTag()))
                .map(tagBody -> TagMapper.INSTANCE.idAndTagBodyToTagEntity(null, tagBody))
                .collect(Collectors.toSet());

        //새로운 태그가 있을 경우에만 저장!
        if (!newTagEntities.isEmpty()) {
            tagRepository.saveAll(newTagEntities);
        }

        if (newTagEntities.isEmpty()) {
            return Collections.singletonList("All tags are already registered. No new tags were created.");
        } else {
            return newTagEntities.stream().map(TagEntity::getTag).toList();
        }
    }

    //질문과 연관된 전체 태그 반환
    public String getTagsByQuestionId(int questionId) {
        Optional<QuestionEntity> question = questionRepository.findById(questionId);

        if (question.isPresent()) {
            Set<TagEntity> tags = question.get().getTags();

            return "Tags: " + tags.stream()
                    .map(TagEntity::getTag)
                    .collect(Collectors.joining(", "));
        } else {
            return "No question found for ID: " + questionId;
        }
    }

    //태그와 연관된 전체 질문 리스트 반환
    @Transactional
    public String getQuestionsByTagId(int tagId) {
        Optional<TagEntity> tag = tagRepository.findById(tagId);
        if (tag.isPresent()) {
            List<QuestionEntity> questions = tag.get().getQuestions();
            // 질문 목록을 쉼표로 구분하여 반환
            return "Tag: " + tag.get().getTag() +"\n"+
                    "Questions related to this tag: \n" +
                    questions.stream()
                    .map(q -> q.getQuestion().replace("\n", " "))
                    .collect(Collectors.joining("\n"));
        } else {
            return "No tag found for ID: " + tagId;
        }
    }


    //전체 태그 반환 - 태그에 포함된 질문 수 내림차순 정렬
    //태그에 포함된 질문이 많아지면 나중에 해당 태그를 키워드로 넣을 수 있도록..!?
    public String getAllTags() {
        List<TagEntity> tagEntities = tagRepository.findAll();
        List<TagDTO> tagDTOs = TagMapper.INSTANCE.tagEntitiesToTagDTOs(tagEntities);
        tagDTOs.sort(Comparator.comparingInt(TagDTO::getQuestionCount).reversed());

        return "Tags: " + getTagNames(tagDTOs);
    }

    public List<TagDTO> getAllTagsAndSubtags() {
        List<TagEntity> tagEntities = tagRepository.findAll();
        List<TagDTO> tagDTOs = TagMapper.INSTANCE.tagEntitiesToTagDTOs(tagEntities);
        return tagDTOs;
    }


    private String getTagNames(List<TagDTO> tagDTOList) {
        return tagDTOList.stream()
                .map(tagDTO -> "Id:"+tagDTO.getTagId() +"-"+tagDTO.getTag() + "(" + tagDTO.getQuestionCount() + "questions) ")
                .collect(Collectors.joining(", "));
    }

    public String deleteTagById(int tagId) {
        tagRepository.deleteById(tagId);
        return "Tag Id: "+ tagId + " is deleted";
    }

    public String deleteAllTags() {
        tagRepository.deleteAll();
        return "All tags are deleted.";
    }
}
