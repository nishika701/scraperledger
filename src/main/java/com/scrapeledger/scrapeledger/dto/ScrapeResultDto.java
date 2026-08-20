package com.scrapeledger.scrapeledger.dto;

import lombok.Data;

@Data
public class ScrapeResultDto {
    private String repoName;
    private String repoUrl;
    private Integer openIssues;
    private Integer openPrs;
    private String lastCommitDate;
}