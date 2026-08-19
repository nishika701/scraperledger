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

@Service
public class IngestionService {

    private final RepoRepository repoRepository;
    private final RepoEventRepository repoEventRepository;

    IngestionService(RepoRepository repoRepository,RepoEventRepository repoEventRepository){
        this.repoEventRepository = repoEventRepository;
        this.repoRepository = repoRepository;
    }

    public List<RepoEvent> ingest(ScrapeResultDto result) {
        Repo repo = repoRepository.findByName(result.getRepoName())
                .orElseGet(() -> repoRepository.save(new Repo(result.getRepoName(), result.getRepoUrl())));

        LocalDateTime now = LocalDateTime.now();
        List<RepoEvent> written = new ArrayList<>();

        written.addAll(appendIfChanged(repo, "open_issues",
                result.getOpenIssues() == null ? null : result.getOpenIssues().toString(), now));
        written.addAll(appendIfChanged(repo, "open_prs",
                result.getOpenPrs() == null ? null : result.getOpenPrs().toString(), now));
        written.addAll(appendIfChanged(repo, "last_commit_date",
                result.getLastCommitDate(), now));

        return written;
    }

    private List<RepoEvent> appendIfChanged(Repo repo, String field, String newValue, LocalDateTime scrapedAt) {
        RepoEvent last = repoEventRepository.findFirstByRepoIdAndFieldOrderByScrapedAtDesc(repo.getId(), field);
        String oldValue = (last == null) ? null : last.getNewValue();

        boolean unchanged = (oldValue == null && newValue == null) ||
                (oldValue != null && oldValue.equals(newValue));
        if (unchanged) {
            return List.of();
        }

        boolean anomaly = isAnomaly(field, newValue);
        RepoEvent event = new RepoEvent(repo, field, oldValue, newValue, anomaly, scrapedAt);
        repoEventRepository.save(event);
        return List.of(event);
    }

    private boolean isAnomaly(String field, String newValue) {
        if (newValue == null) return true;
        if (field.equals("open_issues") || field.equals("open_prs")) {
            try {
                return Integer.parseInt(newValue) < 0;
            } catch (NumberFormatException e) {
                return true;
            }
        }
        return false;
    }

}