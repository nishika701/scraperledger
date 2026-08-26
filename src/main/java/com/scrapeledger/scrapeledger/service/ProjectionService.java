package com.scrapeledger.scrapeledger.service;

import com.scrapeledger.scrapeledger.dto.RepoStateDto;
import com.scrapeledger.scrapeledger.model.Repo;
import com.scrapeledger.scrapeledger.model.RepoEvent;
import com.scrapeledger.scrapeledger.repository.RepoEventRepository;
import com.scrapeledger.scrapeledger.repository.RepoRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProjectionService {

    private final RepoRepository repoRepository;
    private final RepoEventRepository repoEventRepository;

    public ProjectionService(
            RepoRepository repoRepository,
            RepoEventRepository repoEventRepository) {

        this.repoRepository = repoRepository;
        this.repoEventRepository = repoEventRepository;
    }

    public RepoStateDto getCurrentState(Long repoId) {

        Repo repo = repoRepository.findById(repoId)
                .orElseThrow(() ->
                        new RuntimeException("Repo not found: " + repoId)
                );

        List<RepoEvent> events =
                repoEventRepository.findByRepoIdOrderByScrapedAtAsc(repoId);

        Map<String, String> currentData = new LinkedHashMap<>();

        for (RepoEvent event : events) {
            currentData.put(
                    event.getField(),
                    event.getNewValue()
            );
        }

        int healthScore = computeHealthScore(currentData);

        return new RepoStateDto(
                repo.getId(),
                repo.getName(),
                currentData,
                healthScore
        );
    }

    public List<RepoEvent> getHistory(Long repoId) {

        return repoEventRepository
                .findByRepoIdOrderByScrapedAtAsc(repoId);
    }

    public List<RepoEvent> getAnomalies(Long repoId) {

        return repoEventRepository
                .findByRepoIdAndIsAnomalyTrueOrderByScrapedAtAsc(repoId);
    }

    private int computeHealthScore(Map<String, String> data) {

        if (data == null || data.isEmpty()) {
            return 0;
        }

        int score = 100;

        if (!data.containsKey("version")) {
            score -= 25;
        }

        if (!data.containsKey("release_date")) {
            score -= 25;
        }

        if (!data.containsKey("summary")) {
            score -= 25;
        }

        return Math.max(0, score);
    }
}