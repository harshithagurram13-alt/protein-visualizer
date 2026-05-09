package com.proteinviz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * Generates AI-powered protein summaries using the Anthropic Claude API.
 * Covers: function, disease relevance, pathway involvement, drug targets.
 *
 * Set ANTHROPIC_API_KEY as an environment variable before running.
 */
@Service
public class AiSummaryService {

    @Value("${anthropic.api.key:${ANTHROPIC_API_KEY:}}")
    private String anthropicApiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProteinService proteinService;

    public AiSummaryService(ProteinService proteinService) {
        this.proteinService = proteinService;
    }

    @Cacheable("aiSummary")
    public Map<String, Object> generateSummary(String pdbId) throws Exception {
        String id = pdbId.toUpperCase();

        // 1. Gather structural metadata
        String title = "";
        String organism = "";
        try {
            var info = proteinService.getProteinInfo(id);
            title = info.getTitle() != null ? info.getTitle() : "";
            organism = info.getOrganism() != null ? info.getOrganism() : "";
        } catch (Exception ignored) {}

        // 2. Fetch UniProt functional annotation if available
        String uniprotSummary = fetchUniprotAnnotation(id);

        // 3. Build the Claude prompt
        String prompt = buildPrompt(id, title, organism, uniprotSummary);

        // 4. Call Claude API (or fallback to rule-based if no key)
        if (anthropicApiKey == null || anthropicApiKey.isBlank()) {
            return buildFallbackSummary(id, title, organism, uniprotSummary);
        }

        return callClaudeApi(id, title, organism, prompt);
    }

    private Map<String, Object> callClaudeApi(String pdbId, String title,
                                               String organism, String prompt) throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
            "model", "claude-opus-4-5",
            "max_tokens", 1024,
            "messages", List.of(
                Map.of("role", "user", "content", prompt)
            )
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.anthropic.com/v1/messages"))
                .header("Content-Type", "application/json")
                .header("x-api-key", anthropicApiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Claude API error: " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String text = root.path("content").path(0).path("text").asText("");

        // Parse JSON embedded in the Claude response
        return parseClaudeJsonResponse(pdbId, title, organism, text);
    }

    private Map<String, Object> parseClaudeJsonResponse(String pdbId, String title,
                                                          String organism, String text) {
        try {
            // Claude returns JSON within ```json ... ``` or raw
            String jsonStr = text;
            if (text.contains("```json")) {
                jsonStr = text.substring(text.indexOf("```json") + 7);
                jsonStr = jsonStr.substring(0, jsonStr.indexOf("```")).trim();
            } else if (text.contains("{")) {
                jsonStr = text.substring(text.indexOf("{"), text.lastIndexOf("}") + 1);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(jsonStr, Map.class);
            parsed.put("pdbId", pdbId);
            parsed.put("source", "claude-ai");
            return parsed;
        } catch (Exception e) {
            // If JSON parse fails, return structured text
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("pdbId", pdbId);
            result.put("title", title);
            result.put("organism", organism);
            result.put("summary", text);
            result.put("source", "claude-ai");
            return result;
        }
    }

    private String buildPrompt(String pdbId, String title, String organism, String uniprotInfo) {
        return """
            You are a structural bioinformatics expert. Analyze the protein with PDB ID %s.
            Title: %s
            Organism: %s
            UniProt annotations: %s

            Provide a comprehensive analysis in STRICT JSON format (no markdown, no preamble):
            {
              "title": "official protein name",
              "organism": "source organism",
              "function": "2-3 sentence description of the protein's biological function",
              "diseaseRelevance": [
                {"disease": "disease name", "role": "how this protein is involved", "clinicalSignificance": "high|medium|low"}
              ],
              "pathways": [
                {"name": "pathway name", "database": "KEGG|Reactome|WikiPathways", "role": "protein's role in pathway"}
              ],
              "drugTargets": [
                {"drug": "drug name or compound", "status": "approved|clinical_trial|experimental", "mechanism": "mechanism of action"}
              ],
              "keyResidues": [
                {"position": 123, "residue": "HIS", "role": "catalytic|binding|structural"}
              ],
              "structuralFeatures": "brief description of notable structural features",
              "confidence": "high|medium|low"
            }

            Base your answer on established knowledge. If information is unavailable, use empty arrays [].
            Return ONLY the JSON object.
            """.formatted(pdbId, title, organism, uniprotInfo);
    }

    private String fetchUniprotAnnotation(String pdbId) {
        try {
            String url = "https://www.ebi.ac.uk/pdbe/api/mappings/uniprot/" + pdbId.toLowerCase();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> resp =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(resp.body());
                // Extract first UniProt accession
                JsonNode entries = root.path(pdbId.toLowerCase());
                if (!entries.isMissingNode()) {
                    JsonNode uniprotNode = entries.path("UniProt");
                    if (!uniprotNode.isMissingNode()) {
                        String accession = uniprotNode.fieldNames().next();
                        return "UniProt accession: " + accession;
                    }
                }
            }
        } catch (Exception ignored) {}
        return "No UniProt mapping available";
    }

    private Map<String, Object> buildFallbackSummary(String pdbId, String title,
                                                       String organism, String uniprotInfo) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pdbId", pdbId);
        result.put("title", title);
        result.put("organism", organism);
        result.put("function", "Configure ANTHROPIC_API_KEY environment variable to enable AI-powered summaries. "
                + "This protein (" + title + ") can be analyzed using the Claude API for detailed functional information.");
        result.put("diseaseRelevance", List.of());
        result.put("pathways", List.of());
        result.put("drugTargets", List.of());
        result.put("keyResidues", List.of());
        result.put("structuralFeatures", "Set ANTHROPIC_API_KEY to enable AI analysis.");
        result.put("confidence", "low");
        result.put("source", "fallback-no-api-key");
        result.put("uniprotInfo", uniprotInfo);
        return result;
    }
}