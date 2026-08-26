package com.scrapeledger.scrapeledger.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class RepoStateDto {

    private Long repoId;
    private String repoName;
    private Map<String, String> data;
    private int healthScore;
}