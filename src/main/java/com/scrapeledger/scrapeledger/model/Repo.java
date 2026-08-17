package com.scrapeledger.scrapeledger.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

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

    @Column(name = "github_url", nullable = false)
    private String githubUrl;

    @Column(name = "created_At")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Repo(String name, String githubUrl) {
        this.name = name;
        this.githubUrl = githubUrl;
    }
}
