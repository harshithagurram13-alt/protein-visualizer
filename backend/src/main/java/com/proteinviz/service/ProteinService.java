package com.proteinviz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proteinviz.model.ProteinInfo;
import com.proteinviz.model.SearchResult;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

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

        return p;
    }

    @Cacheable("search")
    public SearchResult searchProteins(String query, int limit) {

        SearchResult result = new SearchResult();
        result.setQuery(query);
        result.setTotalCount(1);

        List<SearchResult.SearchEntry> list = new ArrayList<>();

        SearchResult.SearchEntry entry = new SearchResult.SearchEntry();
        entry.setPdbId("1HHO");
        entry.setTitle("Hemoglobin");

        list.add(entry);
        result.setResults(list);

        return result;
    }

    // ✅ THIS is the method you were missing
    public List<SearchResult.SearchEntry> getFeaturedProteins() {
        List<SearchResult.SearchEntry> list = new ArrayList<>();

        SearchResult.SearchEntry entry = new SearchResult.SearchEntry();
        entry.setPdbId("1HHO");
        entry.setTitle("Hemoglobin");

        list.add(entry);

        return list;
    }
}