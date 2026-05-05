package com.proteinviz.model;

import java.util.List;

public class ProteinInfo {

    private String pdbId;
    private String title;
    private String organism;
    private String method;
    private Double resolution;
    private Integer atomCount;
    private Integer residueCount;
    private String releaseDate;
    private List<String> authors;
    private String description;
    private String formula;
    private String structureFileUrl;
    private String thumbnailUrl;

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

    public Integer getAtomCount() { return atomCount; }
    public void setAtomCount(Integer atomCount) { this.atomCount = atomCount; }

    public Integer getResidueCount() { return residueCount; }
    public void setResidueCount(Integer residueCount) { this.residueCount = residueCount; }

    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }

    public List<String> getAuthors() { return authors; }
    public void setAuthors(List<String> authors) { this.authors = authors; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFormula() { return formula; }
    public void setFormula(String formula) { this.formula = formula; }

    public String getStructureFileUrl() { return structureFileUrl; }
    public void setStructureFileUrl(String structureFileUrl) { this.structureFileUrl = structureFileUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
}