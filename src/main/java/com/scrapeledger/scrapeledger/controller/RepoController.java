package com.scrapeledger.scrapeledger.controller;

import com.scrapeledger.scrapeledger.dto.RepoStateDto;
import com.scrapeledger.scrapeledger.dto.ScrapeResultDto;
import com.scrapeledger.scrapeledger.model.Repo;
import com.scrapeledger.scrapeledger.model.RepoEvent;
import com.scrapeledger.scrapeledger.repository.RepoRepository;
import com.scrapeledger.scrapeledger.service.IngestionService;
import com.scrapeledger.scrapeledger.service.ProjectionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RepoController {

    private final IngestionService ingestionService;
    private final ProjectionService projectionService;
    private final RepoRepository repoRepository;

    public RepoController(IngestionService ingestionService, ProjectionService projectionService,RepoRepository repoRepository) {
        this.ingestionService = ingestionService;
        this.projectionService = projectionService;
        this.repoRepository = repoRepository;
    }

    @PostMapping("/scrape-events")
    public List<RepoEvent> receiveScrapeResult(@RequestBody ScrapeResultDto result) {
        return ingestionService.ingest(result);
    }

    @GetMapping("/repos/{id}/current")
    public RepoStateDto getCurrent(@PathVariable Long id) {
        return projectionService.getCurrentState(id);
    }

    @GetMapping("/repos/{id}/history")
    public List<RepoEvent> getHistory(@PathVariable Long id) {
        return projectionService.getHistory(id);
    }

    @GetMapping("/repos/{id}/anomalies")
    public List<RepoEvent> getAnomalies(@PathVariable Long id) {
        return projectionService.getAnomalies(id);
    }

    @GetMapping("/repos")
    public List<Repo> getAllRepos() {
        return repoRepository.findAll();
    }
}