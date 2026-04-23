package com.alis.backend;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

@Service
public class DocumentService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final DocumentRepository documentRepository;
    private final LogService logService;

    // FIX: used to fetch real user from DB
    @PersistenceContext
    private EntityManager entityManager;

    public DocumentService(DocumentRepository documentRepository,
                           LogService logService) {
        this.documentRepository = documentRepository;
        this.logService = logService;
    }

    public Object analyzeDocument(MultipartFile file, String userIdentifier) {

        try {

            System.out.println("STARTING ANALYSIS...");

            String originalName = file.getOriginalFilename();

            if (originalName == null || originalName.isBlank()) {
                originalName = "uploaded_document.pdf";
            }

            // FIXED ABSOLUTE PATH (IMPORTANT)
            String filePath = new java.io.File("ingestion_folder", originalName)
                    .getAbsolutePath();

            File savedFile = new File(filePath);
            savedFile.getParentFile().mkdirs();

            try (FileOutputStream fos = new FileOutputStream(savedFile)) {
                fos.write(file.getBytes());
            }

            System.out.println("FILE SAVED: " + filePath);

            // ================= FIX: SAFE USER LOOKUP =================
            Long userId;

            try {
                userId = (Long) entityManager.createQuery(
                        "SELECT u.id FROM User u WHERE u.userIdentifier = :uid"
                )
                .setParameter("uid", userIdentifier)
                .getSingleResult();

            } catch (NoResultException e) {
                throw new RuntimeException("User not found: " + userIdentifier);
            }

            // ================= CALL FASTAPI FIRST =================
            String analyzeUrl = "http://127.0.0.1:8000/analyze_document";

            Map<String, Object> analyzeRequest = new HashMap<>();
            analyzeRequest.put("file_path", filePath);
            analyzeRequest.put("user_identifier", userIdentifier);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity =
                    new HttpEntity<>(analyzeRequest, headers);

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(analyzeUrl, requestEntity, Map.class);

            Map<String, Object> aiResponse = response.getBody();

            if (aiResponse == null) {
                throw new RuntimeException("AI returned NULL response");
            }

            int clauses = aiResponse.get("clauses_found") != null
                    ? Integer.parseInt(aiResponse.get("clauses_found").toString())
                    : 0;

            Object risks = aiResponse.getOrDefault("risks", List.of());
            Object compliance = aiResponse.getOrDefault("compliance_issues", List.of());
            Object suggestions = aiResponse.getOrDefault("suggestions", List.of());

            double similarity = aiResponse.get("similarity_percentage") != null
                    ? Double.parseDouble(aiResponse.get("similarity_percentage").toString())
                    : 0;

            Object message = aiResponse.get("message");
            Object existingId = aiResponse.get("existing_document_id");

            Map<String, Object> result = new HashMap<>();

            // ================= FIX: HANDLE DUPLICATE =================
            if (message != null && existingId != null) {

                result.put("new_document_id", null);
                result.put("existing_document_id", existingId);
                result.put("clauses_found", clauses);
                result.put("risks", risks);
                result.put("compliance_issues", compliance);
                result.put("suggestions", suggestions);
                result.put("similarity_percentage", similarity);
                result.put("message", message);

                return result;
            }

            // ================= FIX: CHECK DB BEFORE INSERT =================
            Optional<Document> existingDoc =
                    documentRepository.findByUserIdAndFileName(userId, originalName);

            Document savedDoc;

            if (existingDoc.isPresent()) {

                // reuse existing document (NO INSERT)
                savedDoc = existingDoc.get();

            } else {

                Document doc = new Document();
                doc.setUserId(userId);
                doc.setFileName(originalName);
                doc.setContent("Uploaded file");

                savedDoc = documentRepository.save(doc);
            }

            System.out.println("DOCUMENT READY ID: " + savedDoc.getId());

            // LOG (only when new insert OR reuse is fine)
            logService.saveLog(
                    userId,
                    savedDoc.getId(),
                    "DOCUMENT UPLOADED"
            );

            // RETURN RESULT
            result.put("new_document_id", savedDoc.getId());
            result.put("existing_document_id", existingId);

            result.put("clauses_found", clauses);
            result.put("risks", risks);
            result.put("compliance_issues", compliance);
            result.put("suggestions", suggestions);
            result.put("similarity_percentage", similarity);
            result.put("message", message);

            return result;

        } catch (Exception e) {

            System.out.println("ERROR OCCURRED:");
            e.printStackTrace();

            Map<String, Object> error = new HashMap<>();
            error.put("document_id", null);
            error.put("clauses_found", 0);
            error.put("risks", List.of());
            error.put("compliance_issues", List.of());
            error.put("suggestions", List.of());
            error.put("similarity_percentage", 0);
            error.put("error", e.toString());

            return error;
        }
    }

    // GET USER DOCUMENTS
    public List<Document> getUserDocuments(String userIdentifier) {

        Long userId;

        try {
            userId = (Long) entityManager.createQuery(
                    "SELECT u.id FROM User u WHERE u.userIdentifier = :uid"
            )
            .setParameter("uid", userIdentifier)
            .getSingleResult();

        } catch (NoResultException e) {
            throw new RuntimeException("User not found: " + userIdentifier);
        }

        return documentRepository.findByUserId(userId);
    }
}