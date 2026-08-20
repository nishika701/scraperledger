package com.scrapeledger.scrapeledger.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RepoStateDto {
    private Long repoId;
    private String repoName;
    private String openIssues;
    private String openPrs;
    private String lastCommitDate;
    private int healthScore;
}