package com.alis.backend;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "document",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "file_name"})
)
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;    

    @Column(name = "file_name")
    private String fileName;

    @Column(columnDefinition = "TEXT")
    private String content;

    public Document() {}

    public Document(Long id, String fileName, String content) {
        this.id = id;
        this.fileName = fileName;
        this.content = content;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public Long getUserId() {   
        return userId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContent() {
        return content;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setContent(String content) {
        this.content = content;
    }
}