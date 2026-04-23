package com.alis.backend;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentRepository documentRepository;

    public DocumentController(DocumentService documentService,
                               DocumentRepository documentRepository) {
        this.documentService = documentService;
        this.documentRepository = documentRepository;
    }

    // UPLOAD / ANALYZE DOCUMENT
    @PostMapping("/upload")
    public ResponseEntity<Object> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("user_id") String userId) {

        try {

            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Please upload a valid document."));
            }

            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User ID is required."));
            }

            Object result = documentService.analyzeDocument(file, userId);

            // FIX: safe null + type handling
            if (result == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Analysis returned null result");
                return ResponseEntity.status(500).body(error);
            }

            if (result instanceof Map<?, ?> map && map.containsKey("error")) {
                return ResponseEntity.status(500).body(result);
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();

            Map<String, Object> error = new HashMap<>();
            error.put("error", "Analysis failed");
            error.put("details", e.getMessage());

            return ResponseEntity.internalServerError().body(error);
        }
    }

    // GET ALL DOCUMENTS
    @GetMapping("/all")
    public ResponseEntity<Object> getAllDocuments() {

        try {
            return ResponseEntity.ok(documentRepository.findAll());
        } catch (Exception e) {

            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch documents");
            error.put("details", e.getMessage());

            return ResponseEntity.internalServerError().body(error);
        }
    }

    // DELETE DOCUMENT
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteDocument(@PathVariable Long id) {

        try {

            if (id == null || id <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid document ID"));
            }

            if (!documentRepository.existsById(id)) {
                return ResponseEntity.status(404)
                        .body(Map.of("error", "Document not found"));
            }

            documentRepository.deleteById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Document deleted successfully");
            response.put("deleted_id", id);

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to delete document");
            error.put("details", e.getMessage());

            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    // GET USER DOCUMENTS 
    @GetMapping("/user/{userIdentifier}")
    public ResponseEntity<List<Document>> getUserDocuments(@PathVariable String userIdentifier) {

        return ResponseEntity.ok(documentService.getUserDocuments(userIdentifier));
    }

    
}