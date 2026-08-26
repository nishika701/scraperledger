package com.scrapeledger.scrapeledger.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ScrapeResultDto {

    private String repoName;
    private String repoUrl;
    private Map<String, String> data;
}