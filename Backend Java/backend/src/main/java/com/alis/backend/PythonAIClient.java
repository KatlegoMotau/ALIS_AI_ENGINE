package com.alis.backend;

import java.util.Map;
import org.springframework.web.client.RestTemplate;

public class PythonAIClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public String analyzeDocument(String filePath) {

        //IP ADDRESS
        String url = "http://192.168.0.244:8000/analyze_document";

        Map<String, String> request = Map.of(
                "file_path", filePath
        );

        return restTemplate.postForObject(url, request, String.class);
    }
}
