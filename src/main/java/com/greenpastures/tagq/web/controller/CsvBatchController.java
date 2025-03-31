package com.greenpastures.tagq.web.controller;

import com.greenpastures.tagq.service.CsvBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/batch")
public class CsvBatchController {

    private final CsvBatchService csvBatchService;
    @Value("${CSV_FILE_PATH}")
    private String filePath;

    @PostMapping("/upload")
    public String uploadCsv() throws IOException {
        csvBatchService.processCsvBatch(filePath);
        return "Batch processing started!";
    }

    @RequestMapping("/reset")
    public String resetQuestions() {
        csvBatchService.resetSentQuestions();
        return "질문 내역이 초기화되었습니다. 새로운 질문을 전송하세요.";
    }

}

