package com.scrapeledger.scrapeledger.service;

import com.scrapeledger.scrapeledger.dto.RepoStateDto;
import com.scrapeledger.scrapeledger.model.Repo;
import com.scrapeledger.scrapeledger.model.RepoEvent;
import com.scrapeledger.scrapeledger.repository.RepoEventRepository;
import com.scrapeledger.scrapeledger.repository.RepoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectionService {

    private final RepoRepository repoRepository;
    private final RepoEventRepository repoEventRepository;

    public ProjectionService(RepoRepository repoRepository, RepoEventRepository repoEventRepository) {
        this.repoRepository = repoRepository;
        this.repoEventRepository = repoEventRepository;
    }

    public RepoStateDto getCurrentState(Long repoId) {
        Repo repo = repoRepository.findById(repoId)
                .orElseThrow(() -> new RuntimeException("Repo not found: " + repoId));

        String openIssues = latestValue(repoId, "open_issues");
        String openPrs = latestValue(repoId, "open_prs");
        String lastCommitDate = latestValue(repoId, "last_commit_date");

        int healthScore = computeHealthScore(openIssues);

        return new RepoStateDto(repo.getId(), repo.getName(), openIssues, openPrs, lastCommitDate, healthScore);
    }

    public List<RepoEvent> getHistory(Long repoId) {
        return repoEventRepository.findByRepoIdOrderByScrapedAtAsc(repoId);
    }

    public List<RepoEvent> getAnomalies(Long repoId) {
        return repoEventRepository.findByRepoIdAndIsAnomalyTrueOrderByScrapedAtAsc(repoId);
    }

    private String latestValue(Long repoId, String field) {
        RepoEvent event = repoEventRepository.findFirstByRepoIdAndFieldOrderByScrapedAtDesc(repoId, field);
        return event == null ? null : event.getNewValue();
    }

    // Simple formula: start at 100, subtract 1 point per open issue, floor at 0.
    // Explainable in one sentence for the demo — that's the point.
    private int computeHealthScore(String openIssuesStr) {
        if (openIssuesStr == null) return 0;
        try {
            int issues = Integer.parseInt(openIssuesStr);
            return Math.max(0, 100 - issues);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}