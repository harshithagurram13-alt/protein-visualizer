package com.proteinviz.model;

public class MutationRequest {
    private String pdbId;
    private String proteinName;
    private String wildType;   // single-letter AA code, e.g. "V"
    private String mutant;     // single-letter AA code, e.g. "E"
    private int position;      // residue number in PDB numbering
    private String chain;      // chain identifier, e.g. "A"
    private String uniprotId;  // optional

    public String getPdbId() { return pdbId; }
    public void setPdbId(String pdbId) { this.pdbId = pdbId; }

    public String getProteinName() { return proteinName; }
    public void setProteinName(String proteinName) { this.proteinName = proteinName; }

    public String getWildType() { return wildType; }
    public void setWildType(String wildType) { this.wildType = wildType; }

    public String getMutant() { return mutant; }
    public void setMutant(String mutant) { this.mutant = mutant; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public String getChain() { return chain != null ? chain : "A"; }
    public void setChain(String chain) { this.chain = chain; }

    public String getUniprotId() { return uniprotId; }
    public void setUniprotId(String uniprotId) { this.uniprotId = uniprotId; }
}