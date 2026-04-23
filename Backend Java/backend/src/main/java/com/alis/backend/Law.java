package com.alis.backend;

import jakarta.persistence.*;

@Entity
@Table(name = "law")
public class Law {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String lawName;
    private String section;

    @Column(columnDefinition = "TEXT")
    private String description;

    // getters + setters
    public Long getId() { return id; }

    public String getLawName() { return lawName; }
    public void setLawName(String lawName) { this.lawName = lawName; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}