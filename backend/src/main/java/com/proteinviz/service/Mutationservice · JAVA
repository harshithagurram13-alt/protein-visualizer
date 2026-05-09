package com.proteinviz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proteinviz.model.MutationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * Mutation analysis service.
 * - Highlights the mutated residue in the 3D viewer
 * - Compares wild-type vs mutant amino acid properties
 * - Predicts functional effect using ClinVar / UniProt variant API + Claude AI
 */
@Service
public class MutationService {

    @Value("${anthropic.api.key:${ANTHROPIC_API_KEY:}}")
    private String anthropicApiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Amino acid property tables
    private static final Map<String, Map<String, Object>> AA_PROPERTIES = buildAaProperties();

    public Map<String, Object> analyzeMutation(MutationRequest req) throws Exception {
        String pdbId = req.getPdbId().toUpperCase();
        String wtAa = req.getWildType().toUpperCase();
        String mutAa = req.getMutant().toUpperCase();
        int position = req.getPosition();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pdbId", pdbId);
        result.put("position", position);
        result.put("wildType", wtAa);
        result.put("mutant", mutAa);
        result.put("notation", wtAa + position + mutAa);

        // 1. Residue properties comparison
        result.put("wildTypeProperties", AA_PROPERTIES.getOrDefault(wtAa, defaultProps(wtAa)));
        result.put("mutantProperties", AA_PROPERTIES.getOrDefault(mutAa, defaultProps(mutAa)));

        // 2. Property changes (charge, polarity, size, hydrophobicity)
        result.put("propertyChanges", computePropertyChanges(wtAa, mutAa));

        // 3. Query ClinVar / Ensembl Variant Effect Predictor if UniProt ID available
        Map<String, Object> dbResult = queryVariantDatabases(req);
        result.put("databasePrediction", dbResult);

        // 4. AI prediction using Claude
        Map<String, Object> aiPrediction = predictWithAi(req, dbResult);
        result.put("aiPrediction", aiPrediction);

        // 5. Viewer highlight instructions (for 3Dmol.js)
        result.put("highlightInstructions", buildHighlightInstructions(position, wtAa, mutAa, req.getChain()));

        // 6. Overall severity score (0-1)
        result.put("severity", computeSeverity(wtAa, mutAa, dbResult, aiPrediction));

        return result;
    }

    private Map<String, Object> queryVariantDatabases(MutationRequest req) {
        Map<String, Object> dbResult = new LinkedHashMap<>();
        
        // Query UniProt natural variants via PDB mapping
        try {
            String pdbLower = req.getPdbId().toLowerCase();
            String url = "https://www.ebi.ac.uk/pdbe/graph-api/pdbe_pages/residue_mapping/" 
                        + pdbLower + "/" + req.getChain() + "/" + req.getPosition();
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(6))
                    .build();
            
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                dbResult.put("pdbeMapping", "available");
                
                // Extract UniProt residue info
                JsonNode residueData = root.path(pdbLower).path("residue_mapping");
                if (!residueData.isMissingNode() && residueData.isArray() && residueData.size() > 0) {
                    JsonNode first = residueData.get(0);
                    String uniprotAcc = first.path("uniprot_accession").asText("");
                    int uniprotPos = first.path("uniprot_residue_number").asInt(req.getPosition());
                    dbResult.put("uniprotAccession", uniprotAcc);
                    dbResult.put("uniprotPosition", uniprotPos);
                    
                    // Query UniProt variants
                    if (!uniprotAcc.isEmpty()) {
                        Map<String, Object> variantInfo = queryUniprotVariants(uniprotAcc, uniprotPos, req.getMutant());
                        dbResult.putAll(variantInfo);
                    }
                }
            }
        } catch (Exception e) {
            dbResult.put("pdbeMapping", "unavailable");
            dbResult.put("note", "Could not fetch database annotations: " + e.getMessage());
        }

        // BLOSUM62-based evolutionary conservation score
        dbResult.put("blosum62Score", computeBlosum62(req.getWildType(), req.getMutant()));
        dbResult.put("conservationClass", classifyConservation(req.getWildType(), req.getMutant()));

        return dbResult;
    }

    private Map<String, Object> queryUniprotVariants(String accession, int position, String mutant) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String url = "https://rest.uniprot.org/uniprotkb/" + accession + "/features?types=natural+variant";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(6))
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode features = root.path("features");
                List<Map<String, Object>> knownVariants = new ArrayList<>();
                
                if (features.isArray()) {
                    for (JsonNode feat : features) {
                        int start = feat.path("location").path("start").path("value").asInt(-1);
                        if (start == position) {
                            Map<String, Object> v = new LinkedHashMap<>();
                            v.put("type", feat.path("type").asText(""));
                            v.put("description", feat.path("description").asText(""));
                            v.put("clinicalSignificance", feat.path("featureCrossReferences")
                                    .path(0).path("properties").path(0).path("value").asText(""));
                            knownVariants.add(v);
                        }
                    }
                }
                result.put("knownVariants", knownVariants);
                result.put("hasKnownVariant", !knownVariants.isEmpty());
            }
        } catch (Exception ignored) {
            result.put("knownVariants", List.of());
            result.put("hasKnownVariant", false);
        }
        return result;
    }

    private Map<String, Object> predictWithAi(MutationRequest req, Map<String, Object> dbResult) throws Exception {
        if (anthropicApiKey == null || anthropicApiKey.isBlank()) {
            return buildFallbackAiPrediction(req);
        }

        String prompt = """
            Analyze this protein point mutation as a structural bioinformatics expert.

            Protein: %s (PDB ID: %s)
            Mutation: %s%d%s (chain %s)
            Database annotations: %s
            Wild-type AA properties: charge=%s, polarity=%s, size=%s
            Mutant AA properties: charge=%s, polarity=%s, size=%s

            Return ONLY a JSON object (no markdown, no preamble):
            {
              "effect": "benign|likely_benign|uncertain|likely_pathogenic|pathogenic",
              "confidence": "high|medium|low",
              "mechanismOfEffect": "brief explanation of structural/functional impact",
              "structuralImpact": "description of how this mutation changes structure",
              "functionalImpact": "description of effect on protein function",
              "conservationNote": "evolutionary conservation at this position",
              "clinicalNote": "any known clinical significance",
              "recommendations": ["list of follow-up analyses to perform"]
            }
            """.formatted(
                req.getProteinName() != null ? req.getProteinName() : "Unknown",
                req.getPdbId(),
                req.getWildType(), req.getPosition(), req.getMutant(), req.getChain(),
                objectMapper.writeValueAsString(dbResult).substring(0, Math.min(500, objectMapper.writeValueAsString(dbResult).length())),
                getProp(req.getWildType(), "charge"), getProp(req.getWildType(), "polarity"), getProp(req.getWildType(), "size"),
                getProp(req.getMutant(), "charge"), getProp(req.getMutant(), "polarity"), getProp(req.getMutant(), "size")
        );

        String requestBody = objectMapper.writeValueAsString(Map.of(
            "model", "claude-opus-4-5",
            "max_tokens", 800,
            "messages", List.of(Map.of("role", "user", "content", prompt))
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
            return buildFallbackAiPrediction(req);
        }

        JsonNode root = objectMapper.readTree(response.body());
        String text = root.path("content").path(0).path("text").asText("");

        try {
            String jsonStr = text;
            if (text.contains("```json")) {
                jsonStr = text.substring(text.indexOf("```json") + 7);
                jsonStr = jsonStr.substring(0, jsonStr.indexOf("```")).trim();
            } else if (text.contains("{")) {
                jsonStr = text.substring(text.indexOf("{"), text.lastIndexOf("}") + 1);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(jsonStr, Map.class);
            parsed.put("source", "claude-ai");
            return parsed;
        } catch (Exception e) {
            return Map.of("effect", "uncertain", "mechanismOfEffect", text, "source", "claude-ai");
        }
    }

    private Map<String, Object> buildFallbackAiPrediction(MutationRequest req) {
        int blosum = computeBlosum62(req.getWildType(), req.getMutant());
        String effect = blosum >= 0 ? "likely_benign" : blosum >= -2 ? "uncertain" : "likely_pathogenic";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("effect", effect);
        result.put("confidence", "low");
        result.put("mechanismOfEffect", "Based on BLOSUM62 substitution score (" + blosum + "). "
                + "Set ANTHROPIC_API_KEY for detailed AI analysis.");
        result.put("structuralImpact", describePropertyChanges(req.getWildType(), req.getMutant()));
        result.put("functionalImpact", "Configure Claude API for detailed functional prediction.");
        result.put("source", "blosum62-fallback");
        return result;
    }

    private Map<String, Object> buildHighlightInstructions(int position, String wtAa, String mutAa, String chain) {
        Map<String, Object> hl = new LinkedHashMap<>();
        hl.put("residueNumber", position);
        hl.put("chain", chain != null ? chain : "A");
        hl.put("wildTypeColor", "#39ffc8");   // teal for wild-type context
        hl.put("mutantColor", "#ff5e8a");     // red for mutation site
        hl.put("sphereRadius", 1.5);
        hl.put("labelText", wtAa + position + mutAa);
        
        // 3Dmol.js selection spec
        hl.put("selectionSpec", Map.of(
            "resi", position,
            "chain", chain != null ? chain : "A"
        ));
        return hl;
    }

    private List<Map<String, Object>> computePropertyChanges(String wt, String mut) {
        List<Map<String, Object>> changes = new ArrayList<>();
        Map<String, Object> wtProps = AA_PROPERTIES.getOrDefault(wt, defaultProps(wt));
        Map<String, Object> mutProps = AA_PROPERTIES.getOrDefault(mut, defaultProps(mut));

        String[] props = {"charge", "polarity", "size", "hydrophobicity", "aromaticity"};
        for (String prop : props) {
            Object wtVal = wtProps.get(prop);
            Object mutVal = mutProps.get(prop);
            if (wtVal != null && !wtVal.equals(mutVal)) {
                changes.add(Map.of(
                    "property", prop,
                    "from", wtVal,
                    "to", mutVal,
                    "changed", true
                ));
            }
        }
        return changes;
    }

    private double computeSeverity(String wt, String mut, Map<String, Object> dbResult, Map<String, Object> aiPrediction) {
        double score = 0.5;
        int blosum = computeBlosum62(wt, mut);
        
        // BLOSUM contribution
        if (blosum >= 1) score -= 0.2;
        else if (blosum == 0) score -= 0.1;
        else if (blosum == -1 || blosum == -2) score += 0.1;
        else score += 0.25;

        // AI prediction contribution
        String effect = String.valueOf(aiPrediction.getOrDefault("effect", "uncertain"));
        score = switch (effect) {
            case "benign" -> Math.max(0, score - 0.25);
            case "likely_benign" -> Math.max(0, score - 0.15);
            case "likely_pathogenic" -> Math.min(1, score + 0.2);
            case "pathogenic" -> Math.min(1, score + 0.35);
            default -> score;
        };

        return Math.round(score * 100.0) / 100.0;
    }

    private int computeBlosum62(String wt, String mut) {
        // Simplified BLOSUM62 diagonal + common substitution scores
        if (wt.equals(mut)) return 4;
        Map<String, Integer> scores = Map.ofEntries(
            Map.entry("A_G", 0), Map.entry("A_S", 1), Map.entry("A_T", 0), Map.entry("A_V", 0),
            Map.entry("R_K", 2), Map.entry("R_Q", 1), Map.entry("R_H", 0),
            Map.entry("N_D", 1), Map.entry("N_S", 1), Map.entry("N_T", 0),
            Map.entry("D_E", 2), Map.entry("D_N", 1),
            Map.entry("C_S", -1), Map.entry("C_A", 0),
            Map.entry("Q_E", 2), Map.entry("Q_K", 1), Map.entry("Q_R", 1),
            Map.entry("E_D", 2), Map.entry("E_Q", 2), Map.entry("E_K", 1),
            Map.entry("G_A", 0), Map.entry("G_S", 0),
            Map.entry("H_R", 0), Map.entry("H_Y", 2), Map.entry("H_N", 1),
            Map.entry("I_V", 3), Map.entry("I_L", 1), Map.entry("I_M", 1),
            Map.entry("L_I", 1), Map.entry("L_V", 1), Map.entry("L_M", 2), Map.entry("L_F", 0),
            Map.entry("K_R", 2), Map.entry("K_Q", 1), Map.entry("K_E", 1),
            Map.entry("M_L", 2), Map.entry("M_I", 1), Map.entry("M_V", 1),
            Map.entry("F_Y", 3), Map.entry("F_W", 1), Map.entry("F_L", 0),
            Map.entry("P_A", -1),
            Map.entry("S_T", 1), Map.entry("S_A", 1), Map.entry("S_N", 1),
            Map.entry("T_S", 1), Map.entry("T_A", 0), Map.entry("T_N", 0),
            Map.entry("W_F", 1), Map.entry("W_Y", 2),
            Map.entry("Y_F", 3), Map.entry("Y_H", 2), Map.entry("Y_W", 2),
            Map.entry("V_I", 3), Map.entry("V_L", 1), Map.entry("V_A", 0)
        );
        String key1 = wt + "_" + mut;
        String key2 = mut + "_" + wt;
        return scores.getOrDefault(key1, scores.getOrDefault(key2, -3));
    }

    private String classifyConservation(String wt, String mut) {
        int score = computeBlosum62(wt, mut);
        if (score >= 2) return "conservative";
        if (score >= 0) return "semi-conservative";
        if (score >= -2) return "non-conservative";
        return "highly-disruptive";
    }

    private String describePropertyChanges(String wt, String mut) {
        List<Map<String, Object>> changes = computePropertyChanges(wt, mut);
        if (changes.isEmpty()) return "No major property changes predicted.";
        StringBuilder sb = new StringBuilder("Property changes: ");
        for (Map<String, Object> c : changes) {
            sb.append(c.get("property")).append(" (").append(c.get("from"))
              .append("→").append(c.get("to")).append(") ");
        }
        return sb.toString().trim();
    }

    private String getProp(String aa, String prop) {
        return String.valueOf(AA_PROPERTIES.getOrDefault(aa, defaultProps(aa)).getOrDefault(prop, "unknown"));
    }

    private Map<String, Object> defaultProps(String aa) {
        return Map.of("name", aa, "charge", "unknown", "polarity", "unknown",
                "size", "unknown", "hydrophobicity", "unknown", "aromaticity", "non-aromatic");
    }

    private static Map<String, Map<String, Object>> buildAaProperties() {
        Map<String, Map<String, Object>> m = new LinkedHashMap<>();
        // One-letter code -> properties
        m.put("A", Map.of("name", "Alanine", "charge", "neutral", "polarity", "nonpolar",
                "size", "small", "hydrophobicity", "hydrophobic", "aromaticity", "non-aromatic",
                "mw", 89.09));
        m.put("R", Map.of("name", "Arginine", "charge", "positive", "polarity", "polar",
                "size", "large", "hydrophobicity", "hydrophilic", "aromaticity", "non-aromatic",
                "mw", 174.20));
        m.put("N", Map.of("name", "Asparagine", "charge", "neutral", "polarity", "polar",
                "size", "medium", "hydrophobicity", "hydrophilic", "aromaticity", "non-aromatic",
                "mw", 132.12));
        m.put("D", Map.of("name", "Aspartate", "charge", "negative", "polarity", "polar",
                "size", "small", "hydrophobicity", "hydrophilic", "aromaticity", "non-aromatic",
                "mw", 133.10));
        m.put("C", Map.of("name", "Cysteine", "charge", "neutral", "polarity", "polar",
                "size", "small", "hydrophobicity", "hydrophobic", "aromaticity", "non-aromatic",
                "mw", 121.16));
        m.put("Q", Map.of("name", "Glutamine", "charge", "neutral", "polarity", "polar",
                "size", "medium", "hydrophobicity", "hydrophilic", "aromaticity", "non-aromatic",
                "mw", 146.15));
        m.put("E", Map.of("name", "Glutamate", "charge", "negative", "polarity", "polar",
                "size", "medium", "hydrophobicity", "hydrophilic", "aromaticity", "non-aromatic",
                "mw", 147.13));
        m.put("G", Map.of("name", "Glycine", "charge", "neutral", "polarity", "nonpolar",
                "size", "tiny", "hydrophobicity", "hydrophobic", "aromaticity", "non-aromatic",
                "mw", 75.03));
        m.put("H", Map.of("name", "Histidine", "charge", "positive", "polarity", "polar",
                "size", "medium", "hydrophobicity", "hydrophilic", "aromaticity", "aromatic",
                "mw", 155.16));
        m.put("I", Map.of("name", "Isoleucine", "charge", "neutral", "polarity", "nonpolar",
                "size", "large", "hydrophobicity", "hydrophobic", "aromaticity", "non-aromatic",
                "mw", 131.17));
        m.put("L", Map.of("name", "Leucine", "charge", "neutral", "polarity", "nonpolar",
                "size", "large", "hydrophobicity", "hydrophobic", "aromaticity", "non-aromatic",
                "mw", 131.17));
        m.put("K", Map.of("name", "Lysine", "charge", "positive", "polarity", "polar",
                "size", "large", "hydrophobicity", "hydrophilic", "aromaticity", "non-aromatic",
                "mw", 146.19));
        m.put("M", Map.of("name", "Methionine", "charge", "neutral", "polarity", "nonpolar",
                "size", "large", "hydrophobicity", "hydrophobic", "aromaticity", "non-aromatic",
                "mw", 149.21));
        m.put("F", Map.of("name", "Phenylalanine", "charge", "neutral", "polarity", "nonpolar",
                "size", "large", "hydrophobicity", "hydrophobic", "aromaticity", "aromatic",
                "mw", 165.19));
        m.put("P", Map.of("name", "Proline", "charge", "neutral", "polarity", "nonpolar",
                "size", "small", "hydrophobicity", "hydrophobic", "aromaticity", "non-aromatic",
                "mw", 115.13));
        m.put("S", Map.of("name", "Serine", "charge", "neutral", "polarity", "polar",
                "size", "small", "hydrophobicity", "hydrophilic", "aromaticity", "non-aromatic",
                "mw", 105.09));
        m.put("T", Map.of("name", "Threonine", "charge", "neutral", "polarity", "polar",
                "size", "small", "hydrophobicity", "hydrophilic", "aromaticity", "non-aromatic",
                "mw", 119.12));
        m.put("W", Map.of("name", "Tryptophan", "charge", "neutral", "polarity", "nonpolar",
                "size", "large", "hydrophobicity", "hydrophobic", "aromaticity", "aromatic",
                "mw", 204.23));
        m.put("Y", Map.of("name", "Tyrosine", "charge", "neutral", "polarity", "polar",
                "size", "large", "hydrophobicity", "hydrophobic", "aromaticity", "aromatic",
                "mw", 181.19));
        m.put("V", Map.of("name", "Valine", "charge", "neutral", "polarity", "nonpolar",
                "size", "medium", "hydrophobicity", "hydrophobic", "aromaticity", "non-aromatic",
                "mw", 117.15));
        return Collections.unmodifiableMap(m);
    }
}