package com.greenpastures.tagq.service;


import com.greenpastures.tagq.repository.QuestionRepository;
import com.greenpastures.tagq.repository.TagRepository;
import com.greenpastures.tagq.repository.entity.QuestionEntity;
import com.greenpastures.tagq.repository.entity.TagEntity;
import com.greenpastures.tagq.service.exceptions.NotAcceptException;
import com.greenpastures.tagq.service.exceptions.NotFoundException;
import com.greenpastures.tagq.service.mapper.QuestionMapper;
import com.greenpastures.tagq.web.dto.QuestionBody;
import com.greenpastures.tagq.web.dto.QuestionDTO;
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
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final TagRepository tagRepository;
    private final OpenAIService openAIService;
    private static final Logger logger = LoggerFactory.getLogger(QuestionService.class);


    @Transactional
    public QuestionDTO createQuestion(QuestionBody questionBody) {
        QuestionEntity questionEntity = QuestionMapper.INSTANCE.idAndQuestionBodyToQuestionEntity(null, questionBody);
        Set<String> generatedTags = new HashSet<>(Arrays.asList(openAIService.tagQuestion(questionBody.getQuestion()).split(",")));
        Set<TagEntity> tagEntities = new HashSet<>();

        // AI 태그를 확인하고 없으면 새로 저장
        for (String tagName : generatedTags) {
            tagName = tagName.trim().isEmpty() ? "기타" : tagName.trim();  // 빈 값이면 "기타"로 대체

            final String finalTagName = tagName;
            TagEntity tagEntity = tagRepository.findByTag(tagName).orElseGet(() -> {
                TagEntity newTag = new TagEntity();
                newTag.setTag(finalTagName);
                return tagRepository.save(newTag);
            });
            tagEntities.add(tagEntity);
        }
        // QuestionEntity에 태그 추가 (다대다 매핑)
        questionEntity.setTags(tagEntities);
        for (TagEntity tagEntity : tagEntities) {
            tagEntity.getQuestions().add(questionEntity);
        }

        QuestionEntity savedQuestion = questionRepository.save(questionEntity);
        return QuestionMapper.INSTANCE.questionEntityToQuestionDTO(savedQuestion);
    }

    @Transactional
    public List<QuestionDTO> createQuestions(List<QuestionBody> questionBodies) {
        List<QuestionDTO> savedQuestions = new ArrayList<>();

        for (QuestionBody questionBody : questionBodies) {
            QuestionEntity questionEntity = QuestionMapper.INSTANCE.idAndQuestionBodyToQuestionEntity(null, questionBody);
            Set<String> generatedTags = new HashSet<>(Arrays.asList(openAIService.tagQuestion(questionBody.getQuestion()).split(",")));
            Set<TagEntity> tagEntities = new HashSet<>();

            // AI 태그를 확인하고 없으면 새로 저장
            for (String tagName : generatedTags) {
                tagName = tagName.trim().isEmpty() ? "기타" : tagName.trim();  // 빈 값이면 "기타"로 대체

                final String finalTagName = tagName;
                TagEntity tagEntity = tagRepository.findByTag(tagName).orElseGet(() -> {
                    TagEntity newTag = new TagEntity();
                    newTag.setTag(finalTagName);
                    return tagRepository.save(newTag);
                });
                tagEntities.add(tagEntity);
            }

            // QuestionEntity에 태그 추가 (다대다 매핑)
            questionEntity.setTags(tagEntities);
            for (TagEntity tagEntity : tagEntities) {
                tagEntity.getQuestions().add(questionEntity);
            }

            // 질문 저장
            QuestionEntity savedQuestion = questionRepository.save(questionEntity);
            savedQuestions.add(QuestionMapper.INSTANCE.questionEntityToQuestionDTO(savedQuestion));
        }

        return savedQuestions;
    }

   //전체 질문 조회
    public List<QuestionDTO> getAllQuestions() {
        List<QuestionEntity> questionEntities = questionRepository.findAll();
        List<QuestionDTO> questionDTOs = QuestionMapper.INSTANCE.questionEntitiesToQuestionDTOs(questionEntities);
        return questionDTOs;
    }

    //질문 단건 조회
    public QuestionDTO getQuestionById(int questionId) {
        QuestionEntity existingQuestion = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotAcceptException("Question Id doesn't exist"));
        QuestionDTO questionDTO = QuestionMapper.INSTANCE.questionEntityToQuestionDTO(existingQuestion);
        return questionDTO;
    }

    public QuestionDTO getQuestionById(int questionId, String userTimeZone) {
        QuestionEntity existingQuestion = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotAcceptException("Question Id doesn't exist"));
        QuestionDTO questionDTO = QuestionMapper.INSTANCE.questionEntityToQuestionDTO(existingQuestion);
        //질문을 저장할 때는 UTC타임으로 저장이 되지만, 질문을 조회할 때는 사용자 위치에 맞게 시간대가 변환되어 조회됨.
        String formattedTime = TimeZoneConverter.convertToUserTimeZone(existingQuestion.getCreatedAt(), userTimeZone);
        questionDTO.setCreatedAt(formattedTime);
        return questionDTO;
    }


    public String deleteQuestion(int questionId) {
        QuestionEntity existingQuestion = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("No Question found for this Id" + questionId));
        questionRepository.deleteById(existingQuestion.getQuestionId());
        return String.format("Question Id: %d, Question: %s is deleted.", existingQuestion.getQuestionId(), existingQuestion.getQuestion());
    }

    public String deleteAllQuestion() {
        questionRepository.deleteAll();
        return "All questions are deleted.";
    }
}