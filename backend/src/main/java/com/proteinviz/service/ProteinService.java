package com.proteinviz.service;
 
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proteinviz.model.ProteinInfo;
import com.proteinviz.model.SearchResult;
 
@Service
public class ProteinService {
 
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
 
    @Cacheable("proteins")
    public ProteinInfo getProteinInfo(String pdbId) throws Exception {
        String id = pdbId.toUpperCase();
        String url = "https://data.rcsb.org/rest/v1/core/entry/" + id;
 
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
 
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
 
        JsonNode root = objectMapper.readTree(response.body());
 
        ProteinInfo p = new ProteinInfo();
        p.setPdbId(id);
        p.setTitle(root.path("struct").path("title").asText(""));
        p.setMethod(root.path("exptl").path(0).path("method").asText(""));
        p.setReleaseDate(root.path("rcsb_accession_info")
                .path("initial_release_date").asText(""));
 
        // Resolution
        JsonNode refine = root.path("refine");
        if (refine.isArray() && refine.size() > 0) {
            double res = refine.get(0).path("ls_d_res_high").asDouble(0);
            if (res > 0) p.setResolution(res);
        }
 
        // Organism
        JsonNode entity = root.path("polymer_entities");
        if (entity.isArray() && entity.size() > 0) {
            String org = entity.get(0).path("rcsb_entity_source_organism")
                    .path(0).path("scientific_name").asText("");
            if (!org.isEmpty()) p.setOrganism(org);
        }
 
        // Authors
        JsonNode auditAuth = root.path("audit_author");
        if (auditAuth.isArray()) {
            List<String> authors = new ArrayList<>();
            auditAuth.forEach(a -> authors.add(a.path("name").asText("")));
            p.setAuthors(authors);
        }
 
        return p;
    }
 
    @Cacheable("search")
    public SearchResult searchProteins(String query, int limit) throws Exception {
        // RCSB full-text search API
        String searchPayload = buildRcsbSearchPayload(query, limit);
        String url = "https://search.rcsb.org/rcsbsearch/v2/query";
 
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(searchPayload))
                .build();
 
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
 
        SearchResult result = new SearchResult();
        result.setQuery(query);
 
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode hits = root.path("result_set");
 
        List<SearchResult.SearchEntry> list = new ArrayList<>();
        if (hits.isArray()) {
            for (JsonNode hit : hits) {
                SearchResult.SearchEntry entry = new SearchResult.SearchEntry();
                String pdbId = hit.path("identifier").asText("");
                entry.setPdbId(pdbId);
                // Fetch title asynchronously via batch — for now, use identifier as fallback
                entry.setTitle(hit.path("score").asText("") + " — " + pdbId);
                list.add(entry);
            }
        }
 
        // Enrich first 5 results with titles
        int toEnrich = Math.min(5, list.size());
        for (int i = 0; i < toEnrich; i++) {
            try {
                ProteinInfo info = getProteinInfo(list.get(i).getPdbId());
                list.get(i).setTitle(info.getTitle());
            } catch (Exception ignored) {}
        }
 
        result.setResults(list);
        result.setTotalCount(root.path("total_count").asInt(list.size()));
        return result;
    }
 
    public List<SearchResult.SearchEntry> getFeaturedProteins() {
        List<SearchResult.SearchEntry> list = new ArrayList<>();
        String[][] featured = {
            {"4HHB", "Deoxyhemoglobin"},
            {"1CRN", "Crambin"},
            {"1TIM", "Triosephosphate Isomerase"},
            {"1AKE", "Adenylate Kinase"},
            {"6LU7", "SARS-CoV-2 Main Protease"},
            {"3J3Q", "80S Ribosome"},
            {"1BNA", "B-DNA Dodecamer"},
            {"2HHB", "Oxyhemoglobin"}
        };
        for (String[] p : featured) {
            SearchResult.SearchEntry e = new SearchResult.SearchEntry();
            e.setPdbId(p[0]);
            e.setTitle(p[1]);
            list.add(e);
        }
        return list;
    }
 
    /**
     * Fetch UniProt accession and canonical sequence for a PDB entry.
     */
    @Cacheable("sequence")
    public Map<String, Object> getSequenceInfo(String pdbId) throws Exception {
        String id = pdbId.toUpperCase();
        String url = "https://data.rcsb.org/rest/v1/core/polymer_entity/" + id + "/1";
 
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
 
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
 
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pdbId", id);
 
        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            String sequence = root.path("entity_poly").path("pdbx_seq_one_letter_code_can").asText("");
            result.put("sequence", sequence.replaceAll("\\s+", ""));
            result.put("length", sequence.replaceAll("\\s+", "").length());
 
            // UniProt accession
            JsonNode uniprotRef = root.path("rcsb_polymer_entity_container_identifiers")
                    .path("uniprot_ids");
            if (uniprotRef.isArray() && uniprotRef.size() > 0) {
                result.put("uniprotId", uniprotRef.get(0).asText(""));
            }
        } else {
            // Fallback: try entity listing endpoint
            result.put("sequence", "");
            result.put("length", 0);
            result.put("note", "Sequence not available for this entry");
        }
 
        return result;
    }
 
    private String buildRcsbSearchPayload(String query, int limit) {
        return """
            {
              "query": {
                "type": "terminal",
                "service": "full_text",
                "parameters": {
                  "value": "%s"
                }
              },
              "return_type": "entry",
              "request_options": {
                "paginate": {
                  "start": 0,
                  "rows": %d
                },
                "results_content_type": ["experimental"],
                "sort": [
                  {"sort_by": "score", "direction": "desc"}
                ]
              }
            }
            """.formatted(query.replace("\"", "\\\""), limit);
    }
}
