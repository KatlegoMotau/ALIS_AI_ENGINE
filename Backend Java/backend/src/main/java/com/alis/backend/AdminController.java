package com.alis.backend;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin("*")
public class AdminController {

    private final UserRepository userRepository;
    private final LawRepository lawRepository;
    private final LogRepository logRepository;
    private final DocumentRepository documentRepository;
    private final AdminRepository adminRepository;

    public AdminController(UserRepository userRepository,
                            LawRepository lawRepository,
                            LogRepository logRepository,
                            DocumentRepository documentRepository,
                            AdminRepository adminRepository) {

        this.userRepository = userRepository;
        this.lawRepository = lawRepository;
        this.logRepository = logRepository;
        this.documentRepository = documentRepository;
        this.adminRepository = adminRepository;
    }

    //Admin login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {

        String username = body.get("username");
        String password = body.get("password");

        Optional<Admin> admin = adminRepository.findByUsername(username);

        if (admin.isPresent() && admin.get().getPassword().equals(password)) {
            return ResponseEntity.ok(Map.of(
                "message", "Login successful",
                "adminId", admin.get().getId()
            ));
        }

        return ResponseEntity.status(401)
                .body(Map.of("error", "Invalid credentials"));
    }

    // DELETE USER
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {

        try {

            User user = userRepository.findById(id).orElse(null);

            if (user == null) {
                return ResponseEntity.status(404)
                        .body(Map.of("error", "User not found"));
            }

            documentRepository.deleteAll(
                    documentRepository.findAll().stream().filter(d -> d.getUserId() != null && d.getUserId().equals(id))
                            .toList());

            logRepository.deleteAll(
                    logRepository.findAll().stream().filter(l -> l.getUserId() != null && l.getUserId().equals(id))
                            .toList() );

            userRepository.delete(user);

            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Internal server error",
                            "details", e.getMessage()
                    ));
        }
    }

    // EDIT USER
    @PutMapping("/users/{id}")
    public Map<String, String> editUser(
            @PathVariable long id,
            @RequestBody Map<String, String> body) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newUserIdentifier = body.get("userIdentifier");
        String newUserType = body.get("userType");

        if (newUserIdentifier == null || newUserIdentifier.isBlank()) {
            return Map.of("error", "User Identifier cannot be empty");
        }

        user.setUserIdentifier(newUserIdentifier);

        if (newUserType != null && !newUserType.isBlank()) {
            user.setUserType(newUserType);
        }

        userRepository.save(user);

        return Map.of("message", "User updated");
    }

    // ADD LAW
    @PostMapping("/laws")
    public Map<String, String> addLaw(@RequestBody @NonNull Map<String, String> body) {

        String lawName = body.get("lawName");
        String section = body.get("section");
        String description = body.get("description");

        if (lawName == null || lawName.isBlank()) {
            return Map.of("error", "Law name is required");
        }

        if (description == null || description.isBlank()) {
            return Map.of("error", "Description is required");
        }

        Law law = new Law();
        law.setLawName(lawName);
        law.setSection(section);
        law.setDescription(description);

        lawRepository.save(law);

        return Map.of("message", "Law added");
    }

    // GET LOGS (SAFE + CLEAN)
    @GetMapping("/log")
    public Object getLogs() {

        return logRepository.findAll().stream().map(log -> {

            String user = "Unknown User";
            String file = "Unknown Document";

            Long userId = log.getUserId();
            Long docId = log.getDocumentId();

            if (userId != null) {
                user = userRepository.findById(userId)
                        .map(User::getUserIdentifier)
                        .orElse("Unknown User");
            }

            if (docId != null) {
                file = documentRepository.findById(docId)
                        .map(Document::getFileName)
                        .orElse("Unknown Document");
            }

            return new LogResponse(
                    user,
                    file,
                    log.getAction(),
                    log.getTimestamp()
            );
        }).toList();
    }

    // GET ALL USERS (FOR ADMIN VIEW)
    @GetMapping("/users")
    public Object getAllUsers() {

        return userRepository.findAll().stream().map(user -> {

            return Map.of(
                    "id", user.getId(),
                    "userIdentifier", user.getUserIdentifier(),
                    "name", user.getName(),
                    "userType", user.getUserType()
            );

        }).toList();
    }

    // GET ALL LAWS (FOR ADMIN VIEW)
    @GetMapping("/laws")
    public Object getAllLaws() {

        return lawRepository.findAll().stream().map(law -> {

            return Map.of(
                    "id", law.getId(),
                    "lawName", law.getLawName(),
                    "section", law.getSection(),
                    "description", law.getDescription()
            );

        }).toList();
    }

    //Delete laws
    @DeleteMapping("/laws/{id}")
    public ResponseEntity<?> deleteLaw(@PathVariable Long id) {

        try {

            if (!lawRepository.existsById(id)) {
                return ResponseEntity.status(404)
                        .body(Map.of("error", "Law not found"));
            }

            lawRepository.deleteById(id);

            return ResponseEntity.ok(Map.of("message", "Law deleted successfully"));

        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to delete law", "details", e.getMessage()));
        }
    }
}