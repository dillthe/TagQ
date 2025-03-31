package com.greenpastures.tagq.service;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenAIService {
    @Value("${OPENAI_API_KEY}")
    private String openAiApiKey;

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    public String tagQuestion(String question) {
        RestTemplate restTemplate = new RestTemplate();

        String prompt = buildPrompt(question);

        //max_token value값:40이면 태그가 3-4개 정도로 나옴, 토큰 갯수를 늘리면 태그가 더 많이 출력됨.
        Map<String, Object> request = Map.of(
                "model", "gpt-4o-mini",
                "messages", buildMessages(prompt),
                "max_tokens", 40
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        ResponseEntity<Map> response = restTemplate.exchange(OPENAI_URL, HttpMethod.POST, entity, Map.class);

        return parseResponse(response.getBody());
    }
    private String buildPrompt(String question) {
        return "질문: \"" + question + "\"\n\n" +
                "질문과 연관된 태그를 출력해. 태그이름은 공백없이 반환해.\n" +
                "관련있는 이름만 단어로 반환해. 관련있는 단어가 많다면 4개만 반환해. 관련있는 순서대로 반환해. \n" +
                "각 태그는 쉼표로 구분해";
    }

    private Object[] buildMessages(String prompt) {
        return new Object[]{
                Map.of("role", "system", "content", "질문을 잘 이해해서 연관된 태그를 정확하게 출력해주는 AI이야"),
                Map.of("role", "user", "content", prompt)
        };
    }

    private String parseResponse(Map<String, Object> responseBody) {
        // 응답 본문에서 "choices" 항목을 찾아 "content"를 추출합니다
        if (responseBody == null || !responseBody.containsKey("choices")) {
            return "General";
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        if (choices.isEmpty()) {
            return "General";
        }

        Map<String, Object> firstChoice = choices.get(0);
        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
        if (message == null || !message.containsKey("content")) {
            return "General";
        }

        // content 값을 가져오고 trim() 후 빈 문자열일 경우 "기타" 반환
        String content = message.get("content").toString().trim();
        return content.replace("'", "").
                replaceAll(" ","").trim();
    }
}