package com.proteinviz.model;

import java.util.List;

public class SearchResult {

    private int totalCount;
    private List<SearchEntry> results;
    private String query;

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    public List<SearchEntry> getResults() { return results; }
    public void setResults(List<SearchEntry> results) { this.results = results; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public static class SearchEntry {

        private String pdbId;
        private String title;
        private String organism;
        private String method;
        private Double resolution;
        private String releaseDate;
        private String description;

        public String getPdbId() { return pdbId; }
        public void setPdbId(String pdbId) { this.pdbId = pdbId; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getOrganism() { return organism; }
        public void setOrganism(String organism) { this.organism = organism; }

        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }

        public Double getResolution() { return resolution; }
        public void setResolution(Double resolution) { this.resolution = resolution; }

        public String getReleaseDate() { return releaseDate; }
        public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}