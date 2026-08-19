package com.scrapeledger.scrapeledger.repository;

import com.scrapeledger.scrapeledger.model.RepoEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepoEventRepository extends JpaRepository<RepoEvent,Long> {
    RepoEvent findFirstByRepoIdAndFieldOrderByScrapedAtDesc(Long repoId,String field);
    List<RepoEvent> findByRepoIdOrderByScrapedAtAsc(Long repoId);
    List<RepoEvent> findByRepoIdAndFieldOrderByScrapedAtAsc(Long repoId, String field);
    List<RepoEvent> findByRepoIdAndIsAnomalyTrueOrderByScrapedAtAsc(Long repoId);
}