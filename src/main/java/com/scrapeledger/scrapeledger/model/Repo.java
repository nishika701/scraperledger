package com.scrapeledger.scrapeledger.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "repos")
@NoArgsConstructor
public class Repo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Column(name = "created_At")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Repo(String name, String sourceUrl) {
        this.name = name;
        this.sourceUrl = sourceUrl;
    }
}
