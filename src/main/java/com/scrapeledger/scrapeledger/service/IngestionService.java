package com.scrapeledger.scrapeledger.service;

import com.scrapeledger.scrapeledger.dto.ScrapeResultDto;
import com.scrapeledger.scrapeledger.model.Repo;
import com.scrapeledger.scrapeledger.model.RepoEvent;
import com.scrapeledger.scrapeledger.repository.RepoEventRepository;
import com.scrapeledger.scrapeledger.repository.RepoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class IngestionService{

    private final RepoRepository repoRepository;
    private final RepoEventRepository repoEventRepository;

    public IngestionService(
            RepoRepository repoRepository,
            RepoEventRepository repoEventRepository) {

        this.repoRepository = repoRepository;
        this.repoEventRepository = repoEventRepository;
    }

    public List<RepoEvent> ingest(ScrapeResultDto result) {

        Repo repo = repoRepository.findByName(result.getRepoName())
                .orElseGet(() ->
                        repoRepository.save(
                                new Repo(
                                        result.getRepoName(),
                                        result.getRepoUrl()
                                )
                        )
                );

        LocalDateTime now = LocalDateTime.now();

        List<RepoEvent> written = new ArrayList<>();

        if (result.getData() == null || result.getData().isEmpty()) {
            return written;
        }

        for (Map.Entry<String, String> entry : result.getData().entrySet()) {

            String field = entry.getKey();
            String newValue = entry.getValue();

            written.addAll(
                    appendIfChanged(
                            repo,
                            field,
                            newValue,
                            now
                    )
            );
        }

        return written;
    }

    private List<RepoEvent> appendIfChanged(
            Repo repo,
            String field,
            String newValue,
            LocalDateTime scrapedAt) {

        RepoEvent last =
                repoEventRepository
                        .findFirstByRepoIdAndFieldOrderByScrapedAtDesc(
                                repo.getId(),
                                field
                        );

        String oldValue =
                last == null ? null : last.getNewValue();

        boolean unchanged =
                (oldValue == null && newValue == null)
                        ||
                        (oldValue != null && oldValue.equals(newValue));

        if (unchanged) {
            return List.of();
        }

        boolean anomaly = isAnomaly(field, newValue);

        RepoEvent event = new RepoEvent(
                repo,
                field,
                oldValue,
                newValue,
                anomaly,
                scrapedAt
        );

        repoEventRepository.save(event);

        return List.of(event);
    }

    private boolean isAnomaly(String field, String newValue) {

        if (newValue == null || newValue.isBlank()) {
            return true;
        }

        return false;
    }
}