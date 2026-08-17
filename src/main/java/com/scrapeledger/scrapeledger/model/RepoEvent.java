package com.scrapeledger.scrapeledger.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "repo_events")
@Data
@NoArgsConstructor
public class RepoEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repo_id", nullable = false)
    private Repo repo;

    @Column(nullable = false)
    private String field;

    @Column(name = "old_value")
    private String oldValue;

    @Column(name = "new_value")
    private String newValue;

    @Column(name = "is_anomaly")
    private boolean isAnomaly = false;

    @Column(name = "scraped_at", nullable = false)
    private LocalDateTime scrapedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public RepoEvent(Repo repo, String field, String oldValue, String newValue,
                     boolean isAnomaly, LocalDateTime scrapedAt) {
        this.repo = repo;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.isAnomaly = isAnomaly;
        this.scrapedAt = scrapedAt;
    }
}