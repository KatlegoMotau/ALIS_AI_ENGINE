package com.alis.backend;

import org.springframework.stereotype.Service;

@Service
public class LogService {

    private final LogRepository logRepository;

    public LogService(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    public void saveLog(Long userId, Long documentId, String action) {

        Log log = new Log();
        log.setUserId(userId);
        log.setDocumentId(documentId);
        log.setAction(action);
        log.setTimestamp(java.time.LocalDateTime.now());

        logRepository.save(log);
    }
}
