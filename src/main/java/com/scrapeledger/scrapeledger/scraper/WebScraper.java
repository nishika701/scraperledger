package com.scrapeledger.scrapeledger.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;

public class WebScraper {

    // Our Bright Data Scraper Studio collector
    private static final String COLLECTOR_ID = "c_mt6pl55urtgr3wyqj";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, String> scrape(String url) throws Exception {

        ProcessBuilder processBuilder = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-Command",
                "bdata scraper run " + COLLECTOR_ID + " '" + url + "'"
        );

        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException(
                    "Bright Data scraper failed.\n" + output
            );
        }

        String result = output.toString();

        System.out.println("Bright Data scrape completed.");
        //System.out.println(result);

        /*
         * Bright Data CLI prints some status/progress text before
         * the actual JSON. Find the JSON array.
         */
        int jsonStart = result.indexOf("[");

        if (jsonStart == -1) {
            throw new RuntimeException(
                    "Could not find JSON result from Bright Data.\n" + result
            );
        }

        String json = result.substring(jsonStart).trim();

        JsonNode root = objectMapper.readTree(json);

        if (!root.isArray() || root.isEmpty()) {
            throw new RuntimeException(
                    "Bright Data returned empty result: " + json
            );
        }

        JsonNode data = root.get(0);

        Map<String, String> scrapedData = new LinkedHashMap<>();

        scrapedData.put(
                "latest_version",
                data.path("latest_version").asText("")
        );

        scrapedData.put(
                "release_date",
                data.path("release_date").asText("")
        );

        scrapedData.put(
                "package_summary",
                data.path("package_summary").asText("")
        );

        return scrapedData;
    }
}