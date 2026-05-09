package com.proteinviz.controller;

import com.proteinviz.model.ProteinInfo;
import com.proteinviz.model.SearchResult;
import com.proteinviz.model.MutationRequest;
import com.proteinviz.service.ProteinService;
import com.proteinviz.service.AiSummaryService;
import com.proteinviz.service.MutationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProteinController {

    private final ProteinService proteinService;
    private final AiSummaryService aiSummaryService;
    private final MutationService mutationService;

    public ProteinController(ProteinService proteinService,
                              AiSummaryService aiSummaryService,
                              MutationService mutationService) {
        this.proteinService = proteinService;
        this.aiSummaryService = aiSummaryService;
        this.mutationService = mutationService;
    }

    @GetMapping("/protein/{pdbId}")
    public ResponseEntity<?> getProtein(@PathVariable String pdbId) {
        try {
            return ResponseEntity.ok(proteinService.getProteinInfo(pdbId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String q,
                                    @RequestParam(defaultValue = "10") int limit) {
        try {
            return ResponseEntity.ok(proteinService.searchProteins(q, limit));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/featured")
    public ResponseEntity<?> getFeatured() {
        return ResponseEntity.ok(proteinService.getFeaturedProteins());
    }

    /**
     * AI-powered protein summary: function, disease relevance,
     * pathway involvement, and drug targets.
     */
    @GetMapping("/protein/{pdbId}/summary")
    public ResponseEntity<?> getAiSummary(@PathVariable String pdbId) {
        try {
            Map<String, Object> summary = aiSummaryService.generateSummary(pdbId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Mutation analysis: compare wild-type vs mutant,
     * predict effect, highlight changed residue.
     */
    @PostMapping("/mutation/analyze")
    public ResponseEntity<?> analyzeMutation(@RequestBody MutationRequest req) {
        try {
            Map<String, Object> result = mutationService.analyzeMutation(req);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Fetch UniProt sequence for a PDB entry (used by mutation analyzer).
     */
    @GetMapping("/protein/{pdbId}/sequence")
    public ResponseEntity<?> getSequence(@PathVariable String pdbId) {
        try {
            return ResponseEntity.ok(proteinService.getSequenceInfo(pdbId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Protein Viz API v2"
        ));
    }
}