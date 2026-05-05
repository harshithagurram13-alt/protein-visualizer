package com.proteinviz.controller;

import com.proteinviz.model.ProteinInfo;
import com.proteinviz.model.SearchResult;
import com.proteinviz.service.ProteinService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProteinController {

    private final ProteinService proteinService;

    public ProteinController(ProteinService proteinService) {
        this.proteinService = proteinService;
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

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Protein Viz API"
        ));
    }
}