package com.greenpastures.tagq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
@RequiredArgsConstructor
@Slf4j
public class CsvBatchService {
    private static final String API_URL = "http://localhost:8080/api/questions/batch";
    private static final int BATCH_SIZE = 100; // 한 번에 보낼 데이터 개수
    private static final String SENT_QUESTIONS_FILE = "sent_questions.txt";

    // 이미 전송된 질문을 추적하기 위한 Set (Thread-safe 버전으로 변경)
    private final Set<String> sentQuestions = new HashSet<>(loadSentQuestionsFromFile());

    // 이미 보낸 질문을 파일에서 로드
    private Set<String> loadSentQuestionsFromFile() {
        Set<String> questions = new HashSet<>();
        Path path = Paths.get(SENT_QUESTIONS_FILE);

        if (Files.exists(path)) {
            try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
                lines.map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .forEach(questions::add);
            } catch (IOException e) {
                log.error("파일을 읽는 중 오류 발생", e);
            }
        }

        return questions;
    }


    //질문기록 초기화 할 때 사용
    public void resetSentQuestions() {
        sentQuestions.clear(); // Set 초기화
        try {
            Files.deleteIfExists(Paths.get(SENT_QUESTIONS_FILE)); // 파일 삭제
            log.info("기존 sent_questions.txt 삭제 완료. 새로운 질문을 전송합니다.");
        } catch (IOException e) {
            log.error("sent_questions.txt 삭제 중 오류 발생", e);
        }
    }


    // 보낸 질문을 파일에 저장
    private void saveSentQuestionsToFile() {
        Path path = Paths.get(SENT_QUESTIONS_FILE);

        try {
            // 기존 파일에서 읽어온 데이터와 현재 `sentQuestions` 차이를 구함
            Set<String> existingQuestions = loadSentQuestionsFromFile();
            Set<String> newQuestions = sentQuestions.stream()
                    .filter(q -> !existingQuestions.contains(q))
                    .collect(Collectors.toSet());

            if (!newQuestions.isEmpty()) {
                Files.write(path, newQuestions, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                log.info("새로운 질문 {}개가 파일에 저장되었습니다.", newQuestions.size());
            } else {
                log.info("새롭게 저장할 질문이 없습니다.");
            }
        } catch (IOException e) {
            log.error("파일을 저장하는 중 오류 발생", e);
        }
    }


    // CSV 파일에서 질문을 읽고, 중복되지 않은 질문만 추출
    public void processCsvBatch(String filePath) throws IOException {
        List<Map<String, String>> questions = readCsv(filePath);
        log.info("Total questions to send: {}", questions.size());
        sendToApiInBatches(questions, BATCH_SIZE); // 배치 전송
        saveSentQuestionsToFile(); // 보낸 질문 기록 저장
    }

    // CSV 파일을 읽어 Map 형태로 질문 목록 생성
    private List<Map<String, String>> readCsv(String filePath) throws IOException {
        List<Map<String, String>> questionList = new ArrayList<>();
        try (FileReader reader = new FileReader(filePath, StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .withHeader()
                    .withSkipHeaderRecord()
                    .parse(reader);

            for (CSVRecord record : records) {
                Map<String, String> questionMap = new HashMap<>();
                for (String header : record.toMap().keySet()) {
                    String value = record.get(header).trim();
                    String key = header.equals("context") ? "question" : header;
                    questionMap.put(key, value);
                }
                String question = questionMap.get("question");
                if (question != null && !sentQuestions.contains(question)) {
                    questionList.add(questionMap); // 새로운 질문만 추가
                    sentQuestions.add(question); // 전송한 질문으로 등록
                }
            }
        }
        return questionList;
    }

    // 배치로 API에 전송
    private void sendToApiInBatches(List<Map<String, String>> questions, int batchSize) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        int successCount = 0;
        List<List<Map<String, String>>> failedBatches = new ArrayList<>();

        for (int i = 0; i < questions.size(); i += batchSize) {
            int batchEnd = Math.min(i + batchSize, questions.size());
            List<Map<String, String>> batch = questions.subList(i, batchEnd);

            log.info("현재 처리 중: {} ~ {}", i, batchEnd);

            try {
                HttpEntity<List<Map<String, String>>> requestEntity = new HttpEntity<>(batch, headers);
                ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.POST, requestEntity, String.class);

                log.info("응답 본문: {}", response.getBody());

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("API 요청 성공! 응답 코드: {}", response.getStatusCode());
                    log.info("응답 본문: {}", response.getBody());
                    int addedCount = extractAddedCount(response.getBody());
                    log.info("추출된 추가된 개수: {}", addedCount); // 확인용 로그 추가
                    if (addedCount > 0) {
                        successCount += addedCount;
                        log.info("Batch 성공: {}개 추가됨 (누적: {})", addedCount, successCount);
                    } else {
                        log.warn("Batch 성공했지만 추가된 개수가 0입니다.");
                    }
                } else {
                    log.error("Batch 실패: {}", response.getStatusCode());
                    log.error("응답 본문: {}", response.getBody());
                    failedBatches.add(batch);  // 실패한 배치 기록
                }
            } catch (Exception e) {
                log.error("API 요청 중 오류 발생:", e);
                failedBatches.add(batch);  // 실패한 배치 기록
            }
        }

        log.info("총 {}개 질문이 성공적으로 추가됨", successCount);
        log.warn("처리되지 않은 배치가 있습니다: {}", failedBatches.size());
    }

    // 응답 본문에서 실제 추가된 개수 추출
    private int extractAddedCount(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            log.warn("응답 본문이 비어 있습니다.");
            return 0;
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<?> responseList = objectMapper.readValue(responseBody, List.class);
            return responseList.size();
        } catch (Exception e) {
            log.error("응답 본문 파싱 중 오류 발생", e);
            return 0;
        }
    }
}