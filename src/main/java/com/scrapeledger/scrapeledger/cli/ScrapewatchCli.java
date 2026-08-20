package com.scrapeledger.scrapeledger.cli;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ScrapewatchCli {

    private static final String BASE_URL = "http://localhost:8080/api";

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) {

        if (args == null || args.length == 0) {
            printUsageAndExit();
        }

        String subcommand = args[0].toLowerCase();

        // check and history require a repo ID
        if ((subcommand.equals("check") || subcommand.equals("history"))
                && args.length < 2) {
            printUsageAndExit();
        }

        switch (subcommand) {

            case "check":
                handleCheck(args);
                break;

            case "history":
                handleHistory(args);
                break;

            case "status":
                handleStatus();
                break;

            case "add":
                handleAdd(args);
                break;

            default:
                System.out.println("Unknown command: " + subcommand);
                printUsageAndExit();
        }
    }


    private static void handleCheck(String[] args) {

        String repoId = args[1];

        try {

            String json = fetch(
                    BASE_URL + "/repos/" + repoId + "/current"
            );

            JsonNode node = MAPPER.readTree(json);

            if (node == null || node.isNull()) {
                System.out.println("Repository not found.");
                return;
            }

            System.out.println();
            System.out.println("=================================");
            System.out.println("         REPOSITORY STATUS");
            System.out.println("=================================");

            System.out.println("Repo:          "
                    + getText(node, "repoName"));

            System.out.println("Open Issues:   "
                    + getText(node, "openIssues"));

            System.out.println("Open PRs:      "
                    + getText(node, "openPrs"));

            System.out.println("Last Commit:   "
                    + getText(node, "lastCommitDate"));

            System.out.println("Health Score:  "
                    + getInt(node, "healthScore"));

            System.out.println("=================================");
            System.out.println();

        } catch (Exception e) {

            System.out.println(
                    "Error fetching repo state: " + e.getMessage()
            );
        }
    }


    private static void handleHistory(String[] args) {

        String repoId = args[1];

        try {

            String json = fetch(
                    BASE_URL + "/repos/" + repoId + "/history"
            );

            JsonNode events = MAPPER.readTree(json);

            System.out.println();
            System.out.println("=================================");
            System.out.println("          REPOSITORY HISTORY");
            System.out.println("=================================");

            if (events == null || !events.isArray() || events.isEmpty()) {
                System.out.println("No history available.");
                return;
            }

            for (JsonNode event : events) {

                String field = getText(event, "field");

                String oldVal = event.get("oldValue") == null
                        || event.get("oldValue").isNull()
                        ? "null"
                        : event.get("oldValue").asText();

                String newVal = event.get("newValue") == null
                        || event.get("newValue").isNull()
                        ? "null"
                        : event.get("newValue").asText();

                String scrapedAt = getText(event, "scrapedAt");

                boolean anomaly = event.has("anomaly")
                        && event.get("anomaly").asBoolean();

                String flag = anomaly
                        ? "  [ANOMALY]"
                        : "";

                System.out.println(
                        "[" + scrapedAt + "] "
                                + field + ": "
                                + oldVal
                                + " -> "
                                + newVal
                                + flag
                );
            }

            System.out.println("=================================");
            System.out.println();

        } catch (Exception e) {

            System.out.println(
                    "Error fetching history: " + e.getMessage()
            );
        }
    }

    private static void handleStatus() {

        try {

            // First get ALL tracked repositories
            String json = fetch(BASE_URL + "/repos");

            JsonNode repos = MAPPER.readTree(json);

            if (repos == null || !repos.isArray() || repos.isEmpty()) {

                System.out.println();
                System.out.println("No repositories are currently tracked.");
                System.out.println();

                return;
            }

            System.out.println();
            System.out.println("=================================");
            System.out.println("       SCRAPEWATCH STATUS");
            System.out.println("=================================");

            // Dynamically loop through every repository
            for (JsonNode repo : repos) {

                long id = repo.get("id").asLong();

                String name = repo.has("name")
                        ? repo.get("name").asText()
                        : "Unknown repository";

                try {

                    String currentJson = fetch(
                            BASE_URL + "/repos/" + id + "/current"
                    );

                    JsonNode current = MAPPER.readTree(currentJson);

                    int healthScore = getInt(
                            current,
                            "healthScore"
                    );

                    System.out.println(
                            name + " — Health: " + healthScore
                    );

                } catch (Exception e) {

                    System.out.println(
                            name + " — unavailable"
                    );
                }
            }

            System.out.println("=================================");
            System.out.println();

        } catch (Exception e) {

            System.out.println(
                    "Error fetching repository list: "
                            + e.getMessage()
            );
        }
    }
    private static void handleAdd(String[] args) {
        if (args.length < 6) {

            System.out.println(
                    "Usage:"
            );

            System.out.println(
                    "scrapewatch add <repoName> <githubUrl> "
                            + "<openIssues> <openPrs> <lastCommitDate>"
            );

            return;
        }

        String repoName = args[1];
        String githubUrl = args[2];

        int openIssues;
        int openPrs;

        try {

            openIssues = Integer.parseInt(args[3]);
            openPrs = Integer.parseInt(args[4]);

        } catch (NumberFormatException e) {

            System.out.println(
                    "openIssues and openPrs must be numbers."
            );

            return;
        }

        String lastCommitDate = args[5];

        try {
            Map<String, Object> payload = new HashMap<>();

            payload.put("repoName", repoName);
            payload.put("repoUrl", githubUrl);
            payload.put("openIssues", openIssues);
            payload.put("openPrs", openPrs);
            payload.put("lastCommitDate", lastCommitDate);

            String jsonPayload =
                    MAPPER.writeValueAsString(payload);

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(
                                            BASE_URL
                                                    + "/scrape-events"
                                    )
                            )
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .POST(
                                    HttpRequest.BodyPublishers
                                            .ofString(jsonPayload)
                            )
                            .build();

            HttpResponse<String> response =
                    CLIENT.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );
            if (response.statusCode() >= 200
                    && response.statusCode() < 300) {

                System.out.println();
                System.out.println(
                        "Repository registered successfully!"
                );

                System.out.println(
                        "Repo: " + repoName
                );

                System.out.println(
                        "URL:  " + githubUrl
                );

                System.out.println();

            } else {

                System.out.println(
                        "Failed to register repository."
                );

                System.out.println(
                        "HTTP Status: "
                                + response.statusCode()
                );

                System.out.println(
                        "Response: "
                                + response.body()
                );
            }

        } catch (Exception e) {

            System.out.println(
                    "Error adding repo: "
                            + e.getMessage()
            );
        }
    }

    private static String fetch(String url) throws Exception {

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

        HttpResponse<String> response =
                CLIENT.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw new RuntimeException(
                    "HTTP " + response.statusCode()
            );
        }

        return response.body();
    }

    private static String getText(
            JsonNode node,
            String field
    ) {

        if (node == null
                || !node.has(field)
                || node.get(field).isNull()) {

            return "N/A";
        }

        return node.get(field).asText();
    }

    private static int getInt(
            JsonNode node,
            String field
    ) {

        if (node == null
                || !node.has(field)
                || node.get(field).isNull()) {

            return 0;
        }

        return node.get(field).asInt();
    }

    private static void printUsageAndExit() {

        System.out.println();
        System.out.println("ScrapeWatch CLI");
        System.out.println();

        System.out.println(
                "Usage:"
        );

        System.out.println(
                "  scrapewatch check <repoId>"
        );

        System.out.println(
                "  scrapewatch history <repoId>"
        );

        System.out.println(
                "  scrapewatch status"
        );

        System.out.println(
                "  scrapewatch add <repoName> <githubUrl> "
                        + "<openIssues> <openPrs> <lastCommitDate>"
        );

        System.out.println();

        System.exit(1);
    }
}